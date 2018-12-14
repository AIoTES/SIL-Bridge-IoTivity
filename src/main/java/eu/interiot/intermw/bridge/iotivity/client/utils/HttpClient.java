package eu.interiot.intermw.bridge.iotivity.client.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * HTTP client for requests
 *
 */
public class HttpClient {

	/**
	 * When the IoTivity server initializes, it uses a random assigned port. 
	 * Californium is java library for making COAP requests and retrieving this port
	 * When using Java 8 it does not work
	 * A web service that finds the port by using an IoTivity client has been implemented for this kind of situations. 
	 * This method makes a request to this web service 
	 * 
	 * @param proxyIP : the IP where the web service is deployed
	 * @param iotivityServerIP : the IP of the IoTivity server
	 * @return the port assigned to the IoTivity server
	 * @throws Exception in case the request to the service fails
	 */
	public static int getPort(String proxyIP, String iotivityServerIP) throws Exception {
		String url =  "http://"+ proxyIP +":8080/DiscoverPort-0.0.1-SNAPSHOT/service?ip=" + iotivityServerIP;
		String result = sendGet(url);
		JsonObject json = new JsonParser().parse(result).getAsJsonObject();
		return Integer.parseInt(json.get("port").getAsString());
	}
	
	/**
	 * Makes a GET HTTP request to given {@code url}
	 * 
	 * @param url : this is were the GET request is made
	 * @return the response in {@code String} format
	 * @throws Exception in case the request fails
	 */
	private static String sendGet(String url) throws Exception {		
		URL obj = new URL(url);
		System.out.println(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");

		BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		return response.toString();
	}
}
