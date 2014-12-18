package com.ziftr.android.ziftrwallet.network;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ziftr.android.ziftrwallet.ZWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWCurrency;
import com.ziftr.android.ziftrwallet.crypto.ZWECDSASignature;
import com.ziftr.android.ziftrwallet.crypto.ZWECKey;
import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.crypto.ZWTransaction;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWDataSyncHelper {

	
	public static String sendCoins(ZWCoin coin, BigInteger fee, BigInteger amount, List<String> inputs, String output, final String passphrase){
		String message = "";
		ZLog.log("Sending " + amount + " coins to " + output);
		ZiftrNetworkManager.networkStarted();
		
		//find (or create) an address for change that hasn't been spent from
		List<String> changeAddresses = ZWWalletManager.getInstance().getHiddenAddressList(coin, false);
		String changeAddress;
		if (changeAddresses.size() <= 0){
			//create new address for change
			changeAddress = ZWWalletManager.getInstance().createChangeAddress(passphrase, coin).getAddress();
			ZLog.log(changeAddress);
		} else {
			//or reuse one that hasnt been spent from
			changeAddress = changeAddresses.get(0);
		}
		
		JSONObject spendJson = buildSpendPostData(inputs, output, amount, fee.toString(), changeAddress);
		
		ZLog.log("Spending coins with json: ", spendJson.toString());
		
		ZiftrNetRequest request = ZWApi.buildSpendRequest(coin.getType(), coin.getChain(), spendJson);
		
		String response = request.sendAndWait();
		ZLog.forceFullLog(response);		
		
		switch (request.getResponseCode()){
			case 200:
				try {
					JSONObject jsonRes = new JSONObject(response);
					
					ZLog.log("Spend transaction data to sign: ", jsonRes.toString());
					
					message = signSentCoins(coin, jsonRes, inputs, passphrase);
				}
				catch(Exception e) {
					ZLog.log("Exception send coin request: ", e);
				}
				break;
			case 0:
				ZLog.log("Unable to connect to server. Please check your connection.");
				message = "Unable to connect to server. Please check your connection.";
				break;
			default:
				message = "Error, something went wrong! " + request.getResponseCode() + " " + request.getResponseMessage();
				break;
		}

		ZiftrNetworkManager.networkStopped();
		return message;
	}
	
	
	private static String signSentCoins(ZWCoin coin, JSONObject serverResponse, List<String> inputs, String passphrase) {
		Set<String> addressesSpentFrom = new HashSet<String>();
		ZLog.log("signSentCoins called");
		try {
			JSONArray toSignList = serverResponse.getJSONArray("to_sign");
			for (int i=0; i< toSignList.length(); i++){
				JSONObject toSign = toSignList.getJSONObject(i);
	
				String signingAddressPublic = toSign.optString("address");
				addressesSpentFrom.add(signingAddressPublic);
				
				ZLog.log("Signing a transaction with address: ", signingAddressPublic);
				
				ZWECKey key = ZWWalletManager.getInstance().getKeyForAddress(coin, signingAddressPublic, passphrase);
				toSign.put("pubkey", ZiftrUtils.bytesToHexString(key.getPubKey()));
	
				String stringToSign = toSign.getString("tosign");
				ZWECDSASignature signature = key.sign(stringToSign);
				
				String rHex = ZiftrUtils.bigIntegerToString(signature.r, 32);
				toSign.put("r", rHex);
				
				String sHex = ZiftrUtils.bigIntegerToString(signature.s, 32);
				toSign.put("s", sHex);
				
				ZLog.log("Sending this signed send request to server: " + toSign);
			}
		}
		catch(Exception e) {
			ZLog.log("Exeption attemting to sign transactions: ", e);
		}
		ZLog.log("sending  this signed txn to server :" + serverResponse);
		//now that we've packed all the signing into a json object send it off to the server
		ZiftrNetRequest signingRequest = ZWApi.buildSpendSigningRequest(coin.getType(), coin.getChain(), serverResponse);
		
		String response = signingRequest.sendAndWait();
		ZLog.log(signingRequest.getResponseCode());
		ZLog.log("Response from signing: ", response);
		if(signingRequest.getResponseCode() == 202){
			try {
				//flag any addresses we spent from as having been spent from (so we don't reuse change addresses)
				if (addressesSpentFrom.size() <= 0){
					ZLog.log("addresses we spent from weren't in our db ERROR!");
				} else {
					for (String addr : addressesSpentFrom){
						ZLog.log(addr + "changed to spent");
						ZWAddress address = ZWWalletManager.getInstance().getAddress(coin, addr, true);
						address.setSpentFrom(true);
						ZWWalletManager.getInstance().updateAddress(address);
					}
				}
				
				//update the transactions table immediatley with the data from this transaction
				try {
					JSONObject responseJson = new JSONObject(response);
					ZLog.log("Response from completed signing: ", responseJson);
					createTransaction(coin, responseJson, inputs, new HashMap<String, JSONObject>());
					
					//TODO -once create transaction works properly remove this
					updateTransactionHistory(coin);
				}
				catch(Exception e2) {
					ZLog.log("Exception parsing successful spend response: ", e2);
					
					//if the server said the send was successful, but we couldn't handle the response, 
					//then we just update our history and hope it's there
					updateTransactionHistory(coin);
				}
				
				return "";
			}
			catch(Exception e) {
				ZLog.log("Exception saving spend transaction: ", e);
			}
		}
		return "error: " + signingRequest.getResponseMessage();
		
	}


	public static void getBlockChainWallets(){
		ZiftrNetworkManager.networkStarted();
		
		ZiftrNetRequest request = ZWApi.buildGenericApiRequest(false, "/blockchains");
		String response = request.sendAndWait();
		if (request.getResponseCode() == 200){
			JSONArray res;
			try {
				res = new JSONArray((new JSONObject(response)).getString("blockchains"));
				for (int i=0; i< res.length(); i++){
					JSONObject coinJson = res.getJSONObject(i);
					ZWCoin coinToUpdate;
					if (coinJson.getString("chain").contains("test")){
						coinToUpdate = ZWCoin.getCoin(coinJson.getString("type").toUpperCase(Locale.getDefault()) + "_TEST");
					} else {
						coinToUpdate = ZWCoin.getCoin(coinJson.getString("type").toUpperCase(Locale.getDefault()));
					}
					
					if(coinToUpdate != null) {
						int defaultFee = coinJson.getInt("default_fee_per_kb");
						int pubKeyPrefix = coinJson.getInt("p2pkh_byte");
						int scriptHashPrefix = coinJson.getInt("p2sh_byte");
						int privateBytePrefix = coinJson.getInt("priv_byte");
						int blockTime = coinJson.getInt("seconds_per_block_generated");
						int confirmationsNeeded = coinJson.getInt("recommended_confirmations");
						int blockNum = coinJson.getInt("height");
						String chain = coinJson.getString("chain");
						String type = coinJson.getString("type");
						//commented out for testing since server returns false for everything
						//boolean isEnabled = coinJson.getBoolean("is_enabled");
						boolean isEnabled = true;
						ZLog.log("CoinJSON " + coinJson);
						ZWWalletManager.getInstance().updateCoinDb(coinToUpdate, blockNum, defaultFee, pubKeyPrefix, scriptHashPrefix, privateBytePrefix, confirmationsNeeded, blockTime, chain, type, isEnabled);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			ZLog.log("error getting /blockchains wallets: " + response);
		}
		for (ZWCoin coin : ZWWalletManager.getInstance().getEnabledCoins()){
			ZWWalletManager.getInstance().updateCoin(coin);
		}
		ZiftrNetworkManager.networkStopped();
	}
	
	public static void getMarketValue(){
		ZiftrNetworkManager.networkStarted();
		ZiftrNetRequest request = ZWApi.buildMarketValueRequest();
		String response = request.sendAndWait();
		if (request.getResponseCode() == 200){
			ZLog.log("Market Value Response: ", response);
			try {
				JSONArray marketRes = new JSONArray(response);
				for (int i=0; i< marketRes.length(); i++){
					JSONObject marketVal = new JSONObject(marketRes.getString(i));
					ZWCurrency convertingFrom = parseCurrencyHelper(marketVal.getString("currency_from"));
					ZWCurrency convertingTo = parseCurrencyHelper(marketVal.getString("currency_to"));

					BigDecimal rate = new BigDecimal(marketVal.getLong("exchange_rate")).divide(new BigDecimal(marketVal.getLong("exchange_rate_divisor")), MathContext.DECIMAL64);
					
					ZLog.log("Market value of " + convertingFrom.getShortTitle() + " is : " + rate.toString() + " " + convertingTo.getShortTitle());
					ZWWalletManager.getInstance().upsertExchangeValue(convertingFrom, convertingTo, rate.toString());
				}
			} catch (JSONException e) {
				ZLog.log("Market Value Response wasn't json array?!");
				e.printStackTrace();
			}
		} else {
			ZLog.log("Market Value Error: ", response);
		}
		ZiftrNetworkManager.networkStopped();
	}
	
	//helper method to convert server market value string to ZWCurrency
	private static ZWCurrency parseCurrencyHelper(String str){
		if (str.split("/").length > 1){
			//we are a coin
			String coinType = str.split("/")[0].toUpperCase(Locale.ENGLISH);
			if (str.split("/")[1].contains("test")){
				coinType += "_TEST";
			}
			return ZWCoin.getCoin(coinType);
		} else {
			//we are a fiat
			return ZWFiat.valueOf(str);
		}
	}

	public static void updateTransactionHistory(ZWCoin coin) {
		List<String> addresses = ZWWalletManager.getInstance().getAddressList(coin, true);
		if (addresses.size() <= 0){
			ZLog.log("No addresses to get transaction history for");
			return;
		}
		ZiftrNetworkManager.networkStarted();

		ZLog.log("Getting transaction history for addresses: ", addresses);

		ZiftrNetRequest request = ZWApi.buildTransactionsRequest(coin.getType(), coin.getChain(), addresses);
		String response = request.sendAndWait();

		ZLog.forceFullLog("Transaction history response(" + request.getResponseCode() + "): " + response);
		if(request.getResponseCode() == 200) {
			try {
				
				HashMap<String, JSONObject> parsedTransactions = new HashMap<String, JSONObject>();
				
				JSONObject responseJson = new JSONObject(response);
				JSONArray transactions = responseJson.getJSONArray("transactions");

				//we now have an array of transactions with a transaction hash and a url to get more data about the transaction
				for(int x = transactions.length() -1; x >= 0; x--) {
					JSONObject transactionJson = transactions.getJSONObject(x);
					String txid = transactionJson.getString("txid");
					parsedTransactions.put(txid, transactionJson);
					createTransaction(coin, transactionJson, addresses, parsedTransactions);
				}

				//if we got this far without any exceptions, everything went good, so let the UI know the database has changed, so it can update
				ZiftrNetworkManager.dataUpdated();
			}
			catch(Exception e) {
				ZLog.log("Exception downloading transactions: ", e);
			}
		}//end if response code = 200

		ZiftrNetworkManager.networkStopped();
		
		ZiftrNetworkManager.dataUpdated();
	}

	
	//creates transaction in local database from json response
	private static void createTransaction(ZWCoin coin, JSONObject json, List<String> addresses, HashMap<String, JSONObject> parsedTransactions) throws JSONException {
		
		String hash = json.getString("txid");
		ZWTransaction transaction = new ZWTransaction(coin, hash);
		
		long time = json.optLong("time");
		if(time == 0) {
			//this happens when a transaction hasn't even been confirmed once
			//so just use the current time
			time = System.currentTimeMillis() / 1000; //server sends us seconds, so our hold over time should be formatted the same way
		}
		transaction.setTxTime(time * 1000);
		transaction.setConfirmationCount(json.optLong("confirmations"));
		
		//TODO -just setting this as zero for now, need to either get server to send this, or calculate it
		//BigInteger fees = new BigInteger(json.getString("fee"));
		BigInteger fees = new BigInteger("0");
		
		BigInteger value = new BigInteger("0"); //calculate this by scanning inputs and outputs		
		ArrayList<String> displayAddresses = new ArrayList<String>();
		
		//list to display if no non-hidden addresses received coins
		ArrayList<String> hiddenReceivedOn = new ArrayList<String>();
		
		//read through all inputs
		JSONArray inputs = json.getJSONArray("vin");
		for(int x = 0; x < inputs.length(); x++) {
			JSONObject input = inputs.getJSONObject(x);
			
			boolean usedValue = false;
			//the json doesn't have an input address or value, so we have to look at previously parsed transactions
			//and see if this input is the output of another, which would indicate that it is us spending coins
			String inputTxid = input.getString("txid");
			
			JSONObject matchingTransaction = parsedTransactions.get(inputTxid);
			if(matchingTransaction != null) {
				//this means we're spending coins in this transaction
				int voutIndex = input.getInt("vout");
				JSONArray vouts = matchingTransaction.getJSONArray("vout");
				JSONObject vout = vouts.optJSONObject(voutIndex);
				if(vout != null) {
					JSONObject scriptPubKey = vout.getJSONObject("scriptPubKey");
					JSONArray inputAddresses = scriptPubKey.getJSONArray("addresses");
					for(int y = 0; y < inputAddresses.length(); y++) {
						String inputAddress = inputAddresses.getString(y);
						if(addresses.contains(inputAddress)) {
							String outputValue = vout.getString("value");
							if(!usedValue) {
								usedValue = true;
								BigInteger outputValueInt = convertToBigIntSafe(coin, outputValue);
								value = value.subtract(outputValueInt);
							}
						}
					}//end for y
				}
			}
		}//end for x

		
		//read through all outputs
		JSONArray outputs = json.getJSONArray("vout");
		for(int x = 0; x < outputs.length(); x++) {
			JSONObject output = outputs.getJSONObject(x);
			JSONObject scriptPubKey = output.getJSONObject("scriptPubKey");
			JSONArray outputAddresses = scriptPubKey.getJSONArray("addresses");
			boolean usedValue = false;
			for(int y = 0; y < outputAddresses.length(); y++) {
				String outputAddress = outputAddresses.getString(y);
				if(addresses.contains(outputAddress)) {
					if(!usedValue) {
						usedValue = true;
						String outputValue = output.getString("value");
						BigInteger outputValueInt = convertToBigIntSafe(coin, outputValue); 
						value = value.add(outputValueInt);
						
						//add the receiving address to the display if not hidden
						ZWAddress receivedOn = ZWWalletManager.getInstance().getAddress(coin, outputAddress,true);
						if(receivedOn != null){
							if (!receivedOn.isHidden()) {
								displayAddresses.add(receivedOn.getAddress());
								if(transaction.getNote() == null || transaction.getNote().length() == 0){
									transaction.setNote(receivedOn.getLabel());
								}
							} else {
								hiddenReceivedOn.add(receivedOn.getAddress());
							}
						}
					}
				}
				else {
					//if we didn't receive we may have sent to an address we have stored
					ZWAddress sentTo = ZWWalletManager.getInstance().getAddress(coin, outputAddress, false);
					if(sentTo != null) {
						//if we have the address in our local db
						displayAddresses.add(sentTo.getAddress());
						if(transaction.getNote() == null || transaction.getNote().length() == 0){
							transaction.setNote(sentTo.getLabel());
						}
					}
				}

			}
		}

		
		transaction.setAmount(value);
		transaction.setFee(fees);
		if (displayAddresses.size() > 0){
			transaction.setDisplayAddresses(displayAddresses);
		} else {
			transaction.setDisplayAddresses(hiddenReceivedOn);
			//should only have one hidden address we received on
			ZWAddress hiddenAddress = ZWWalletManager.getInstance().getAddress(coin, hiddenReceivedOn.get(0), true);
			hiddenAddress.setHidden(false);
			hiddenAddress.setLabel("Previously hidden address");
			ZWWalletManager.getInstance().updateAddress(hiddenAddress);
			transaction.setNote("Previously hidden address");
		}
		
		ZWWalletManager.getInstance().addTransaction(transaction);
		//return transaction;
	}
	

	
	private static JSONObject buildSpendPostData(List<String> inputs, String output, BigInteger amount, String fee, String changeAddress) {
		JSONObject postData = new JSONObject();
		try {
			postData.put("fee_per_kb", fee);
			postData.put("surplus_refund_address", changeAddress);
			JSONArray inputAddresses = new JSONArray();
			for (String addr : inputs){
				inputAddresses.put(addr);
			}
			
			postData.put("input_addresses", inputAddresses);
			JSONObject outputAddresses = new JSONObject();
			
			outputAddresses.put(output, amount.longValue());
			
			postData.put("output_addresses", outputAddresses);
		}
		catch(Exception e) {
			ZLog.log("Exception build spend JSON: ", e);
		}
		
		return postData;
	}

	
	/**
	 * There seems to be a server bug where sometimes it's sending values as decimals instead of whole satoshis
	 * @param amount a string representing a number in either satoshis or coins
	 * @return a {@link BigInteger} for the string in satoshis
	 */
	private static BigInteger convertToBigIntSafe(ZWCoin coin, String amount) {
		
		if(amount.contains(".")) {
			return coin.getAtomicUnits(new BigDecimal(amount));
		}
		
		return new BigInteger(amount);
	}
	
	
}





