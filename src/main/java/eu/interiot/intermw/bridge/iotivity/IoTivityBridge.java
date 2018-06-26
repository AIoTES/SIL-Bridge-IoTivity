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
package eu.interiot.intermw.bridge.iotivity;

import eu.interiot.intermw.bridge.abstracts.AbstractBridge;
import eu.interiot.intermw.bridge.annotations.Bridge;
import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.bridge.iotivity.client.IoTivityClient;
import eu.interiot.intermw.bridge.iotivity.client.IoTivityCoapHandler;
import eu.interiot.intermw.bridge.iotivity.client.impls.IoTivityCoapClientImpl;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.translators.syntax.iotivity.IotivityTranslator;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.exceptions.payload.PayloadException;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import eu.interiot.message.metadata.PlatformMessageMetadata;

import org.apache.jena.rdf.model.Model;

import org.eclipse.californium.core.CoapHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;
import java.util.Set;

@Bridge(platformType = "http://inter-iot.eu/IoTivity")
public class IoTivityBridge extends AbstractBridge {

	private final Logger logger = LoggerFactory.getLogger(IoTivityBridge.class);
	private IoTivityClient iotivityClient = null;
	private IotivityTranslator translator = new IotivityTranslator();
	private String rootURL;

	public IoTivityBridge(Configuration configuration, Platform platform) throws MiddlewareException {
		super(configuration, platform);
		logger.debug("Example bridge is initializing...");
		
		rootURL = configuration.getProperties().getProperty(IoTivityProperty.SERVER_ROOT_URL);
        if (rootURL == null) {
            throw new BridgeException("Invalid bridge configuration: property '"+IoTivityProperty.SERVER_ROOT_URL+"' is not set.");
        }
        iotivityClient = new IoTivityCoapClientImpl(configuration);
		logger.info("Example bridge has been initialized successfully.");
	}

	@Override
	public Message registerPlatform(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);
//		Set<String> entityIDs = IoTivityUtils.getEntityIDsFromPayload(message.getPayload(), IoTivityUtils.EntityTypePlatform);
//		if (entityIDs.size() != 1) {
//			throw new BridgeException("Missing platform ID.");
//		}
		String platformId = platform.getPlatformId();		
		logger.debug("Registering platform {}...", platformId);
		try {
			iotivityClient.discoverServer();
			logger.debug("Platform {} has been registered.", platformId);
		} catch (Exception e) {
			logger.error("Register Platform  " + e);
			e.printStackTrace();
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
		}
		return responseMessage;
	}

	@Override
	public Message unregisterPlatform(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);
//		Set<String> entityIDs = IoTivityUtils.getEntityIDsFromPayload(message.getPayload(), IoTivityUtils.EntityTypePlatform);
//		if (entityIDs.size() != 1) {
//			throw new BridgeException("Missing platform ID.");
//		}
		String platformId = platform.getPlatformId();	
		logger.debug("Unregistering platform {}...", platformId);
		try {
			logger.debug("Platform {} has been unregistered.", platformId);
			iotivityClient.setPort(0);
		} catch (Exception e) {
			logger.error("Unregister Platform  " + e);
			e.printStackTrace();
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
		}
		return responseMessage;
	}

	@Override
	public Message subscribe(Message message) throws Exception {	
		Message responseMessage = createResponseMessage(message);
		Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
		
		if (entities.isEmpty()) {
			throw new PayloadException("No entities of type Device found in the Payload.");
		} else if (entities.size() > 1) {
			throw new PayloadException("Only one device is supported by Subscribe operation.");
		}

		String thingId = entities.iterator().next();
		String conversationId = message.getMetadata().getConversationId().orElse(null);

		logger.debug("Subscribing to thing {} using conversationId {}...", thingId, conversationId);

		try {
			iotivityClient.isPlatformRegistered();
			PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
			metadata.initializeMetadata();
			metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
			metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);
			metadata.setSenderPlatformId(new EntityID(platform.getPlatformId()));
			metadata.setConversationId(conversationId);

			CoapHandler handler = new IoTivityCoapHandler(metadata, translator, publisher);
			String id = IoTivityUtils.getThingId(thingId);
			String resource = iotivityClient.findResourceURL(id, rootURL);
			iotivityClient.observeResource(resource, handler);
		} catch (Exception e) {
			logger.error("Error subscribing: " + e.getMessage());
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
		}
		return responseMessage;
	}

	@Override
	public Message unsubscribe(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);	
		Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
		try{
			for (String entityId : entities) {
				String id = IoTivityUtils.getThingId(entityId);
				logger.info("Unsubscribing from thing {}...", entityId);
				iotivityClient.stopObservingResource(iotivityClient.findResourceURL(id, rootURL));
			}
		} catch (Exception e){ 
			logger.error("Error unsubscribing: " + e.getMessage());
			e.printStackTrace();
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
		}		
		return responseMessage;
	}

	@Override
	public Message query(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);
		try{
			Set<String> deviceIds = IoTivityUtils.getDeviceIDsFromPayload(message);
			JsonArray array = new JsonArray();
			for (String deviceId : deviceIds){
				String id = IoTivityUtils.getThingId(deviceId);
				String resource = iotivityClient.findResourceURL(id, rootURL);
				JsonObject responseBody = iotivityClient.getResource(resource);
				array.add(responseBody);
			}
			if (array.size() > 0) {
				Model translatedModel = translator.toJenaModel(array.toString());
				MessagePayload responsePayload = new MessagePayload(translatedModel);
				responseMessage.setPayload(responsePayload);
			}
			responseMessage.getMetadata().setStatus("OK");
			publisher.publish(responseMessage);
		}
		catch (Exception e) {
			logger.error("Error in query: " + e.getMessage());
			e.printStackTrace();
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
		}
		return responseMessage;
	}

	@Override
	public Message listDevices(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);
		try{
			iotivityClient.isPlatformRegistered();
			String responseBody = iotivityClient.getResource(rootURL).toString();
			String responseList = IoTivityUtils.getDeviceList(new JsonParser().parse(responseBody).getAsJsonObject()).toString();
			Model translatedModel = translator.toJenaModel(responseList);
			MessagePayload responsePayload = new MessagePayload(translatedModel);
			//translatedModel.write(System.out, "TTL") ;
			responseMessage.setPayload(responsePayload);
			responseMessage.getMetadata().setStatus("OK");
		}
		catch (Exception e) {
			logger.error("Error in query: " + e.getMessage());
			e.printStackTrace();
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
		}
		return responseMessage;
	}

	@Override
	public Message platformCreateDevices(Message message) throws Exception {
		logger.debug("platformCreateDevices() started.");
		Message responseMessage = createResponseMessage(message);		
		try{			
			String body = translator.toFormatX(message.getPayload().getJenaModel());
			Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
			iotivityClient.isPlatformRegistered();

			for (String entityId : entities) {					
				String id = IoTivityUtils.getThingId(entityId);
				String resourceURL = null;
				try {
					resourceURL = iotivityClient.findResourceURL(id, rootURL);
				}
				catch (Exception ex) {};
				if (resourceURL != null) throw new Exception("There is already a device with given id : " + entityId);
				Map<String, Object> map = IoTivityUtils.jsonToMap(body);
				map.put("id", id);
				System.out.println(map);
				resourceURL = IoTivityUtils.getDeviceURLByType((String) map.get("type"), rootURL);
				map.remove("type");
				resourceURL = "/a/devices/bloodpressure";
				logger.debug("Registering thing {}...", id);
				iotivityClient.createResource(map, resourceURL);
	    		logger.debug("Success");
			}
    	}catch(Exception e){
    		logger.error("Error creating devices: " + e.getMessage());
			e.printStackTrace();
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
    	}
		return responseMessage;
	}

	@Override
	public Message platformUpdateDevices(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);
		try{
			String body = translator.toFormatX(message.getPayload().getJenaModel());
			Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
			iotivityClient.isPlatformRegistered();

			for (String entityId : entities) {
				String id = IoTivityUtils.getThingId(entityId);
				String resource = iotivityClient.findResourceURL(id, rootURL);
				logger.debug("Updating thing {}...", resource);
				iotivityClient.editResource(IoTivityUtils.jsonToMap(body), resource);
	    		logger.debug("Success");
			}
    	}catch(Exception e){
    		logger.error("Error updating device: " + e.getMessage());
			e.printStackTrace();
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
    	}
		return responseMessage;
	}

	@Override
	public Message platformDeleteDevices(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);
		try {
			iotivityClient.isPlatformRegistered();
			logger.debug("Removing devices...");
			Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
			for(String deviceId : entities){
				String id = IoTivityUtils.getThingId(deviceId);
				String resource = iotivityClient.findResourceURL(id, rootURL);
				iotivityClient.deleteResource(resource);
				logger.debug("Device {} has been removed.", id);
			}
			responseMessage.getMetadata().setStatus("OK");
		} 
		catch (Exception e) {
			logger.error("Error removing devices: " + e.getMessage());
			e.printStackTrace();
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
		}
		return responseMessage;
	}

	@Override
	public Message observe(Message message) throws Exception {
		return null;
	}

	@Override
	public Message actuate(Message message) throws Exception {
		return null;
	}

	@Override
	public Message error(Message message) throws Exception {
		logger.debug("Error occured in {}...", message);
		Message responseMessage = createResponseMessage(message);
		responseMessage.getMetadata().setStatus("KO");
		responseMessage.getMetadata().setMessageType(MessageTypesEnum.ERROR);
		return responseMessage;
	}

	@Override
	public Message unrecognized(Message message) throws Exception {
		logger.debug("Unrecognized message type.");
		Message responseMessage = createResponseMessage(message);
		responseMessage.getMetadata().setStatus("OK");
		return responseMessage;
	}
}
