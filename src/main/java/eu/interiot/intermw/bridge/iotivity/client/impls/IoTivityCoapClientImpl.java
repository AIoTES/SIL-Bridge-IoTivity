/**
 * ACTIVAGE. ACTivating InnoVative IoT smart living environments for AGEing well.
 * ACTIVAGE is a R&D project which has received funding from the European 
 * Union’s Horizon 2020 research and innovation programme under grant 
 * agreement No 732679.
 * 
 * Copyright (C) 2016-2018, by (Author's company of this file):
 * - CERTH/ITI, http://www.iti.gr/
 * Centre for Research & Technology Hellas - Information Technologies Institute
 * 
 *
 * For more information, contact:
 * - @author <a href="mailto:nkak@iti.gr">Nikolaos Kaklanis</a>  
 * - @cauthor <a href="mailto:stavrotheodoros@iti.gr">Stefanos Stavrotheodoros</a> 
 * - Project coordinator:  <a href="mailto:coordinator@activage.eu"></a>
 *  
 *
 * This code is licensed under the EPL license, available at the root
 * application directory.
 */
package eu.interiot.intermw.bridge.iotivity.client.impls;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import com.google.gson.JsonObject;

import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.bridge.iotivity.IoTivityProperty;
import eu.interiot.intermw.bridge.iotivity.client.IoTivityClient;
import eu.interiot.intermw.bridge.iotivity.client.utils.EncodingUtils;
import eu.interiot.intermw.bridge.iotivity.client.utils.HttpClient;
import eu.interiot.intermw.commons.interfaces.Configuration;

/**
 * 
 * An implementation of the IoTivityClient interface
 * It uses the Californium library and provides the needed functionalities
 * by utilizing the COAP protocol
 *
 */
public class IoTivityCoapClientImpl implements IoTivityClient{
	
	private static final String SCHEME = "coap";
	private static final String SERVER_DISCOVERY_URL = "/oic/res";
	private static final int SERVER_DISCOVERY_PORT = 5683;
	private int hardCodedPort;
	private static double JAVA_VERSION = getVersion();

	private String ip;
	private String proxyIp;
	private int port;
	private Map<String, CoapObserveRelation> observationMap = new HashMap<String, CoapObserveRelation>();
	
	/**
	 * Constructor
	 * 
	 * @param configuration : the configuration parameters of the bridge
	 * @throws BridgeException in case there is a missing configuration property
	 */
	public IoTivityCoapClientImpl(Configuration configuration) throws BridgeException {
		super();
		Properties properties = configuration.getProperties();
		ip = properties.getProperty(IoTivityProperty.SERVER_IP);
		proxyIp = properties.getProperty(IoTivityProperty.PROXY_IP);
		String iotivityServerPort = properties.getProperty(IoTivityProperty.SERVER_PORT);
		
        if (ip == null) {
            throw new BridgeException("Invalid bridge configuration: property '"+IoTivityProperty.SERVER_IP+"' is not set.");
        }
        if (proxyIp == null && iotivityServerPort == null) {
            throw new BridgeException("Invalid bridge configuration: define '"+IoTivityProperty.PROXY_IP+"' or '"+IoTivityProperty.SERVER_PORT+"'.");
        }
		if (iotivityServerPort != null) {
			hardCodedPort = Integer.parseInt(iotivityServerPort);
		}
	}

	/**
	 * Method that edits an existing resource to the IoTivity server
	 * 
	 * @param attributesMap : a {@code Map} that contains the names and the values of the resource to be edited
	 * @param resource : the URL of the resource
	 * @throws Exception
	 */
	public void editResource(Map<String, Object> attributesMap, final String resource) throws Exception{
		CoapClient client = createCoapClient(createResourceURL(resource));
		byte[] resourceEncoded = EncodingUtils.encodeResourceToCbor(attributesMap);
		CoapResponse response = client.put(resourceEncoded, MediaTypeRegistry.APPLICATION_CBOR);
		EncodingUtils.printCborEncodedBytes(response.getPayload());
	}
	
	/**
	 * Method that creates a new resource to the IoTivity server
	 * 
	 * @param attributesMap : a {@code Map} that contains the names and the values of the new resource
	 * @param resource : the URL of the resource
	 * @throws Exception
	 */
	public void createResource(Map<String, Object> attributesMap, final String resource) throws Exception{
		CoapClient client = createCoapClient(createResourceURL(resource));
		byte[] resourceEncoded = EncodingUtils.encodeResourceToCbor(attributesMap);
		CoapResponse response = client.post(resourceEncoded, MediaTypeRegistry.APPLICATION_CBOR);
		EncodingUtils.printCborEncodedBytes(response.getPayload());
	}
 	

	
	/**
	 * Deletes a resource from the IoTivity server
	 * 
	 * @param resource : the URL of the resource to be deleted
	 * @throws Exception
	 */
	public void deleteResource(final String resource) throws Exception{
		CoapClient client = createCoapClient(createResourceURL(resource));
		client.delete();
	}
	
	/**
	 * This method makes a COAP GET request to the given {@code resource} of the IoTivity server
	 * 
	 * @param resource : the URL of the resource to be retrieved
	 * @return a JSON representation of the given resource
	 * @throws Exception
	 */
	public JsonObject getResource(final String resource) throws Exception{
		CoapClient client = createCoapClient(createResourceURL(resource));
		CoapResponse response = client.get();
		if (response != null) {
			JsonObject json = EncodingUtils.coapResponseToJson(response.getPayload()).getAsJsonObject();
			System.out.println(json);
			return json;
		}
		throw new Exception("Did not receive any data for the resource: " + resource);
	}
	
	/**
	 * This method makes a unicast discovery request to the given IP and
	 * discovers the port used by the IoTivity server
	 * 
	 * @throws Exception
	 */
	public void discoverServer() throws Exception{
		if (JAVA_VERSION < 1.8) {
			CoapClient client = createCoapClient(createDiscoveryURL());
			CoapResponse response = client.discoverPort();
			port = response.advanced().getSourcePort();
		}
		else {
			if (hardCodedPort != 0) {
				port = hardCodedPort;
			}
			else {
				port = HttpClient.getPort(proxyIp, ip);
			}
			
		}
		System.out.println("PORT: " + port);
	}
	
	/**
	 * Makes a subscription to a resource in order to receive notifications 
	 * for possible changes
	 * 
	 * @param resource : the URL of the resource to be observed
	 * @throws Exception
	 */
	public void observeResource(final String resource, final CoapHandler handler) throws Exception{
		final CoapClient client = new CoapClient(createResourceURL(resource));
		Thread thread = new Thread(){
		    public void run(){
		    	if (handler != null) {
			    	CoapObserveRelation relation = client.observe(handler);
					observationMap.put(resource, relation);
		    	}
		    	else {
		    		CoapObserveRelation relation = client.observe(createDefaultObservationHandler());
					observationMap.put(resource, relation);
		    	}
		      }
		};
		thread.start();
	}
	
	/**
	 * Terminates the subscription for observation of a resource
	 * 
	 * @param resource : the URL of the resource to terminate subscription
	 */
	public void stopObservingResource(final String resource){
		CoapObserveRelation relation = observationMap.get(resource);
		if (relation != null){
			relation.proactiveCancel();
			observationMap.remove(resource);
		}
	}
	
	/**
	 * Creates a listener for the received events while observing a resource
	 * The method defines the actions on receiving a notification or error
	 * 
	 * @return a {@code CoapHandler} implementation
	 */
	private CoapHandler createDefaultObservationHandler(){
		return new CoapHandler() {
			@Override public void onLoad(CoapResponse response) {
				try {
					if (response.getPayload() != null){
						System.out.println(EncodingUtils.coapResponseToJson(response.getPayload()));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			@Override public void onError() {
				System.err.println("OBSERVING FAILED");
			}
		};
	}
	
	/**
	 * @return : the URL for unicast IoTivity server discovery through COAP
	 * @throws Exception
	 */
	private String createDiscoveryURL() throws Exception{
		return SCHEME + "://" + getIp() + ":" + SERVER_DISCOVERY_PORT + SERVER_DISCOVERY_URL;
	}
	
	/**
	 * @param resource : the URL of the resource (e.g. /a/light1)
	 * @return the COAP URL of the given resource
	 * @throws Exception
	 */
	private String createResourceURL(String resource) throws Exception{
		return SCHEME + "://" + getIp() + ":" + getPort() + resource;
	}
	
	/**
	 * @return the port of the IoTivity server
	 * @throws Exception in case the port is not known
	 */
	private int getPort() throws Exception{
		if (port == 0){
			throw new Exception("The port of the server is not known. You must execute a discovery first");
		} 
		return port;
	}
	
	/**
	 * This method checks if there is a port assigned to this instance
	 * If not, that means that there was not platform registration and an exception is thrown
	 */
	public void isPlatformRegistered() throws Exception {
		if (port == 0) throw new Exception("The platform is not registered");
	}
	
	/**
	 * @return the IP of the IoTivity server
	 * @throws Exception in case the IP is null or empty
	 */
	private String getIp() throws Exception{
		if (ip == null || ip.isEmpty()){
			throw new Exception("IP is null or empty");
		} 
		return ip;
	}
	
	/**
	 * Creates and returns a {@code CoapClient} that timeouts after 5 seconds
	 * 
	 * @param resource : the COAP URL the client is going to use
	 * @return
	 */
	private CoapClient createCoapClient(String resource){
		CoapClient client = new CoapClient(resource);
		client.setTimeout(5000);
		return client;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * Finds the Java version of the system 
	 * 
	 * @return the Java version in a double format with one decimal place
	 */
	private static double getVersion () {
	    String version = System.getProperty("java.version");
	    int pos = version.indexOf('.');
	    pos = version.indexOf('.', pos+1);
	    return Double.parseDouble (version.substring (0, pos));
	}
}
