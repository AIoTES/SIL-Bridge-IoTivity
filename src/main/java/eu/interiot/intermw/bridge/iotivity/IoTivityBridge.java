/**
 * ACTIVAGE. ACTivating InnoVative IoT smart living environments for AGEing well.
 * ACTIVAGE is a R&D project which has received funding from the European 
 * Union�s Horizon 2020 research and innovation programme under grant 
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
import eu.interiot.message.utils.MessageUtils;

import org.apache.jena.rdf.model.Model;

import org.eclipse.californium.core.CoapHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Set;

@eu.interiot.intermw.bridge.annotations.Bridge(platformType = "IoTivity")
public class IoTivityBridge extends AbstractBridge {

	private final Logger logger = LoggerFactory.getLogger(IoTivityBridge.class);
	private IoTivityClient iotivityClient = null;
	private IotivityTranslator translator = new IotivityTranslator();
	private String roolURL;

	public IoTivityBridge(Configuration configuration, Platform platform) throws MiddlewareException {
		super(configuration, platform);
		logger.debug("Example bridge is initializing...");
		
		roolURL = configuration.getProperties().getProperty(IoTivityProperty.SERVER_ROOT_URL);
        if (roolURL == null) {
            throw new BridgeException("Invalid bridge configuration: property '"+IoTivityProperty.SERVER_ROOT_URL+"' is not set.");
        }
		iotivityClient = new IoTivityCoapClientImpl(configuration);
		logger.info("Example bridge has been initialized successfully.");
	}

	@Override
	public Message registerPlatform(Message message) throws Exception {
		Message responseMessage = MessageUtils.createResponseMessage(message);
		Set<String> entityIDs = IoTivityUtils.getEntityIDsFromPayload(message.getPayload(), IoTivityUtils.EntityTypePlatform);
		if (entityIDs.size() != 1) {
			throw new BridgeException("Missing platform ID.");
		}
		String platformId = entityIDs.iterator().next();
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
		Message responseMessage = MessageUtils.createResponseMessage(message);
		Set<String> entityIDs = IoTivityUtils.getEntityIDsFromPayload(message.getPayload(), IoTivityUtils.EntityTypePlatform);
		if (entityIDs.size() != 1) {
			throw new BridgeException("Missing platform ID.");
		}
		String platformId = entityIDs.iterator().next();
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
		Message responseMessage = MessageUtils.createResponseMessage(message);
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
			metadata.setSenderPlatformId(new EntityID(platform.getId().getId()));
			metadata.setConversationId(conversationId);

			CoapHandler handler = new IoTivityCoapHandler(metadata, translator, publisher);
			iotivityClient.observeResource(IoTivityUtils.getThingUrl(thingId), handler);
		} catch (Exception e) {
			logger.error("Error subscribing: " + e.getMessage());
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
		}
		return responseMessage;
	}

	@Override
	public Message unsubscribe(Message message) throws Exception {
		Message responseMessage = MessageUtils.createResponseMessage(message);	
		Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
		try{
			for (String entityId : entities) {
				logger.info("Unsubscribing from thing {}...", entityId);
				iotivityClient.stopObservingResource(IoTivityUtils.getThingUrl(entityId));
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
		Message responseMessage = MessageUtils.createResponseMessage(message);
		try{
			Set<String> deviceIds = IoTivityUtils.getDeviceIDsFromPayload(message);
			JsonArray array = new JsonArray();
			for (String deviceId : deviceIds){
				String resource = IoTivityUtils.getThingUrl(deviceId);
				JsonObject responseBody = iotivityClient.getResource(resource);
				array.add(responseBody);
			}
			if (array.size() > 0) {
				Model translatedModel = translator.toJenaModel(array.toString());
//				translatedModel.write(System.out, "TTL") ;

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
		Message responseMessage = MessageUtils.createResponseMessage(message);
		try{
			iotivityClient.isPlatformRegistered();
			String responseBody = iotivityClient.getResource(roolURL).toString();
			Model translatedModel = translator.toJenaModel(responseBody);
			MessagePayload responsePayload = new MessagePayload(translatedModel);
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
	public Message platformCreateDevice(Message message) throws Exception {
		Message responseMessage = MessageUtils.createResponseMessage(message);
		try{
			String body = translator.toFormatX(message.getPayload().getJenaModel());	
			Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
			iotivityClient.isPlatformRegistered();

			for (String entityId : entities) {
				String resource = IoTivityUtils.getThingUrl(entityId);
				logger.debug("Registering thing {}...", resource);
				iotivityClient.createResource(IoTivityUtils.jsonToMap(body), resource);
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
	public Message platformUpdateDevice(Message message) throws Exception {
		Message responseMessage = MessageUtils.createResponseMessage(message);
		try{
			String body = translator.toFormatX(message.getPayload().getJenaModel());
			Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
			iotivityClient.isPlatformRegistered();

			for (String entityId : entities) {
				String resource = IoTivityUtils.getThingUrl(entityId);
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
	public Message platformDeleteDevice(Message message) throws Exception {
		Message responseMessage = MessageUtils.createResponseMessage(message);
		try {
			iotivityClient.isPlatformRegistered();
			logger.debug("Removing devices...");
			Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
			for(String deviceId : entities){
				String transformedId = IoTivityUtils.getThingUrl(deviceId);
				iotivityClient.deleteResource(IoTivityUtils.getThingUrl(deviceId));
				logger.debug("Device {} has been removed.", transformedId);
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
		Message responseMessage = MessageUtils.createResponseMessage(message);
		responseMessage.getMetadata().setStatus("KO");
		responseMessage.getMetadata().setMessageType(MessageTypesEnum.ERROR);
		return responseMessage;
	}

	@Override
	public Message unrecognized(Message message) throws Exception {
		logger.debug("Unrecognized message type.");
		Message responseMessage = MessageUtils.createResponseMessage(message);
		responseMessage.getMetadata().setStatus("OK");
		return responseMessage;
	}
}
