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
package eu.interiot.intermw.bridge.iotivity.client;

import java.util.Map;

import org.eclipse.californium.core.CoapHandler;

import com.google.gson.JsonObject;
/**
 * 
 * Interface that contains that contains all needed methods by the bridge
 * This interface must be implemented by an IoTivity client that will communicate remotely 
 * with the platform
 *
 */
public interface IoTivityClient {
	
	/**
	 * Method that identifies the randomly assigned port of the IoTivity server.
	 * This port is needed for executing requests
	 * 
	 * @throws Exception in case the client was not able to connect to the server
	 */
	public void discoverServer() throws Exception;
	
	/**
	 * Method for creating a new resource on the IoTivity server
	 * 
	 * @param attributesMap : a {@code Map} that contains key-value pairs equivalent to the 
	 * resource's attributes
	 * @param resource : the URL of the resource to be created (e.g. /a/light)
	 * @throws Exception in case the POST request was not successful
	 */
	public void createResource(Map<String, Object> attributesMap, final String resource) throws Exception;
	
	/**
	 * Method for editing a registered resource of an IoTivity server
	 * 
	 * @param attributesMap : the attributes to be edited along with the new values
	 * @param resource: the URL of the resource to be edited (e.g. /a/light)
	 * @throws Exception in case the PUT request was not successful
	 */
	public void editResource(Map<String, Object> attributesMap, final String resource) throws Exception;
	
	/**
	 * Method for deleting a registered resource of an IoTivity server
	 * 
	 * @param resource: the URL of the resource to be deleted (e.g. /a/light)
	 * @throws Exception in case the DELETE request was not successful
	 */
	public void deleteResource(final String resource) throws Exception;
	
	/**
	 * Method for retrieving a registered resource of an IoTivity server
	 * 
	 * @param resource: the URL of the resource to be retrieved (e.g. /a/light)
	 * @throws Exception in case the GET request was not successful
	 */
	public JsonObject getResource(final String resource) throws Exception;
	
	/**
	 * Method for assigning an observer on a resource on an IoTivity server
	 * 
	 * @param resource: the URL of the resource to be observed (e.g. /a/light)
	 * @param handler : the observer of the resource
	 * @throws Exception
	 */
	public void observeResource(final String resource, final CoapHandler handler) throws Exception;
	
	/**
	 * Method for removing an already registered observer on an IoTivity resource
	 * 
	 * @param resource: the URL of the resource to be observed (e.g. /a/light)
	 */
	public void stopObservingResource(final String resource);
	
	/**
	 * Method for assigning the port used by the IoTivity server
	 * 
	 * @param port
	 */
	public void setPort(int port);
	
	/**
	 * Method that checks if platform is registered. If not an exception is thrown
	 */
	public void isPlatformRegistered() throws Exception;
}