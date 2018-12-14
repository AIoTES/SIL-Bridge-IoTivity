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

import eu.interiot.intermw.bridge.BridgeConfiguration;
import eu.interiot.intermw.bridge.abstracts.AbstractBridge;
import eu.interiot.intermw.bridge.annotations.Bridge;
import eu.interiot.intermw.bridge.iotivity.client.IoTivityClient;
import eu.interiot.intermw.bridge.iotivity.client.IoTivityCoapHandler;
import eu.interiot.intermw.bridge.iotivity.client.impls.IoTivityCoapClientImpl;
import eu.interiot.intermw.bridge.iotivity.scheduler.DeviceCheckScheduler;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
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

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Bridge(platformType = "http://inter-iot.eu/IoTivity")
public class IoTivityBridge extends AbstractBridge {

	private final Logger logger = LoggerFactory.getLogger(IoTivityBridge.class);
	private IotivityTranslator translator = new IotivityTranslator();
	private BridgeConfiguration configuration;
    private String url;
	private IoTivityClient iotivityClient = null;
	private DeviceCheckScheduler scheduler = null;

	public IoTivityBridge(BridgeConfiguration configuration, Platform platform) throws MiddlewareException {
		super(configuration, platform);
		logger.debug("Example bridge is initializing...");
		if (platform.getBaseEndpoint() != null) {
			url = platform.getBaseEndpoint().toString();
		}
	    if (Strings.isNullOrEmpty(url)) {
	    	url = configuration.getProperties().getProperty(IoTivityProperty.SERVER_IP);
	    }
		String proxyIp = configuration.getProperties().getProperty(IoTivityProperty.PROXY_IP);
		String iotivityServerPort = configuration.getProperties().getProperty(IoTivityProperty.SERVER_PORT);
        if (Strings.isNullOrEmpty(url)) {
            throw new MiddlewareException("Invalid bridge configuration: property '"+IoTivityProperty.SERVER_IP+"' is not set.");
        }
        if (Strings.isNullOrEmpty(proxyIp) && Strings.isNullOrEmpty(iotivityServerPort)) {
        	proxyIp = url;
            //throw new MiddlewareException("Invalid bridge configuration: define '"+IoTivityProperty.PROXY_IP+"' or '"+IoTivityProperty.SERVER_PORT+"'.");
        }
        if (!Strings.isNullOrEmpty(iotivityServerPort) && !iotivityServerPort.isEmpty()) {
			Integer.parseInt(iotivityServerPort);
		}
		this.configuration = configuration;
		logger.info("Bridge has been initialized successfully.");
	}

	@Override
	//DONE
	public Message registerPlatform(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);
		try {
			logger.debug("Registering platform {}...", platform.getPlatformId());
			iotivityClient  = new IoTivityCoapClientImpl(url, configuration);			
			updatePlatform(message.getPayload());
			iotivityClient.discoverServer();
			logger.debug("Platform {} has been registered.", platform.getPlatformId());
		} catch (Exception e) {
			logger.error("Register Platform  " + e);
			e.printStackTrace();
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
		}
		return responseMessage;
	}
	
	@Override
	//DONE
	public Message updatePlatform(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);
		if (iotivityClient == null) {
			IoTivityUtils.createErrorResponseMessage(responseMessage, getPlatformUnregisterException());
			return responseMessage;
		}
		updatePlatform(message.getPayload());
		return responseMessage;
	}
	
	/**
	 * Method that parses the given payload, updates the platform instance and the iotivity client
	 * @param messagePayload
	 * @throws MalformedURLException
	 */
	private void updatePlatform(MessagePayload messagePayload) throws MalformedURLException {
		platform = IoTivityUtils.updatePlatform(platform, messagePayload);
		if (platform.getBaseEndpoint() != null) {
			url = platform.getBaseEndpoint().toString();
		}
		iotivityClient.setIp(url);
	}

	@Override
	//DONE
	public Message unregisterPlatform(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);
		String platformId = platform.getPlatformId();	
		logger.debug("Unregistering platform {}...", platformId);
		try {
			iotivityClient = null;
			logger.debug("Platform {} has been unregistered.", platformId);
		} catch (Exception e) {
			logger.error("Unregister Platform  " + e);
			e.printStackTrace();
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
		}
		return responseMessage;
	}

	@Override
	//DONE
	public Message subscribe(Message message) throws Exception {	
		Message responseMessage = createResponseMessage(message);
		if (iotivityClient == null) {
			IoTivityUtils.createErrorResponseMessage(responseMessage, getPlatformUnregisterException());
			return responseMessage;
		}	
		Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
		
		if (entities.isEmpty()) {
			throw new PayloadException("No entities of type Device found in the Payload.");
		} else if (entities.size() > 1) {
			throw new PayloadException("Only one device is supported by Subscribe operation.");
		}

		String thingId = entities.iterator().next();
		String conversationId = message.getMetadata().getConversationId().orElse(null);

		logger.debug("Subscribing to thing {} using conversationId {}...", thingId, conversationId);
		Map<String, JsonElement> deviceMap = iotivityClient.listDevices();

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
			JsonElement deviceJsonElement = deviceMap.get(id);
			if (deviceJsonElement == null) {
				throw new Exception("There is no device with id '"+id+"' to sunscribe to");
			}
			String href = deviceJsonElement.getAsJsonObject().get("href").getAsString();
			iotivityClient.observeResource(href, handler);
		} catch (Exception e) {
			logger.error("Error subscribing: " + e.getMessage());
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
		}
		return responseMessage;
	}

	@Override
	//DONE
	public Message unsubscribe(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);	
		if (iotivityClient == null) {
			IoTivityUtils.createErrorResponseMessage(responseMessage, getPlatformUnregisterException());
			return responseMessage;
		}	
		Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
		Map<String, JsonElement> deviceMap = iotivityClient.listDevices();
		try{
			for (String entityId : entities) {
				String id = IoTivityUtils.getThingId(entityId);
				logger.info("Unsubscribing from thing {}...", entityId);
				JsonElement deviceJsonElement = deviceMap.get(id);
				if (deviceJsonElement == null) {
					return responseMessage;
				}
				String href = deviceJsonElement.getAsJsonObject().get("href").getAsString();
				iotivityClient.stopObservingResource(href);
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
		if (iotivityClient == null) {
			IoTivityUtils.createErrorResponseMessage(responseMessage, getPlatformUnregisterException());
			return responseMessage;
		}	
		try{
			Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);			
			//get a map that contains all registered devices on the server
			Map<String, JsonElement> deviceMap = iotivityClient.listDevices();
			if (entities.size() == 1) {
				for (String entityId : entities) {
					responseMessage = createResponseMessage(message);
					String id = IoTivityUtils.getThingId(entityId);
					JsonElement deviceJsonElement = deviceMap.get(id);
					if (deviceJsonElement == null) {
						throw new Exception("There is no device with given id : " + entityId);
					}
					String href = deviceJsonElement.getAsJsonObject().get("href").getAsString();
					logger.debug("Querying thing {}...", entityId);				
					JsonElement resource = iotivityClient.getResource(href);
					Model translatedModel = translator.toJenaModel(resource.toString());
					MessagePayload responsePayload = new MessagePayload(translatedModel);
					responseMessage.setPayload(responsePayload);
				}
			}
			else if (entities.size() == 0) {
				JsonArray allDeviceList = new JsonArray();
				for (JsonElement d : deviceMap.values()) {
						allDeviceList.add(d);
				}
				Model translatedModel = translator.toJenaModel(allDeviceList.toString());
				MessagePayload responsePayload = new MessagePayload(translatedModel);
				responseMessage.setPayload(responsePayload);
			}
			else {
				IoTivityUtils.createErrorResponseMessage(responseMessage, new Exception("Current version does not support querying with multiple ids"));
				return responseMessage;
			}
			responseMessage.getMetadata().setStatus("OK");
    		logger.debug("Success");
    	}catch(Exception e){
    		logger.error("Error querying device: " + e.getMessage());
			e.printStackTrace();
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
    	}
		return responseMessage;
	}

	@Override
	//DONE
	public Message listDevices(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);
		if (iotivityClient == null) {
			IoTivityUtils.createErrorResponseMessage(responseMessage, getPlatformUnregisterException());
			return responseMessage;
		}	
		
		try {
			deviceRegistryInitialization(message);			
			scheduler = new DeviceCheckScheduler(iotivityClient, new HashMap<String, JsonElement>(), message, publisher, platform.getPlatformId());
			scheduler.check();
			
			responseMessage.getMetadata().setStatus("OK");
			logger.info("Completed listDevices");			
		}
		catch (Exception e) {
			logger.error("Error in query: " + e.getMessage());
			e.printStackTrace();
			IoTivityUtils.createErrorResponseMessage(responseMessage, e);
		}
		return responseMessage;
	}
	
	private Message deviceRegistryInitialization(Message original) throws Exception {
		try{
		    Message deviceRegistryInitializeMessage = new Message();
		    PlatformMessageMetadata metadata = createMessageMetadata(original, URIManagerMessageMetadata.MessageTypesEnum.DEVICE_REGISTRY_INITIALIZE, platform.getPlatformId());
		    deviceRegistryInitializeMessage.setMetadata(metadata);
		    deviceRegistryInitializeMessage.setPayload(new MessagePayload());
		    publisher.publish(deviceRegistryInitializeMessage);
		    logger.debug("Device_Registry_Initialize message has been published upstream.");
		    original.getMetadata().setStatus("OK");
		    return original;
		}catch(Exception ex){
		    return error(original);
		}
	}

	@Override
	//DONE
	public Message platformCreateDevices(Message message) throws Exception {
		logger.debug("platformCreateDevices() started.");
		Message responseMessage = createResponseMessage(message);	
		if (iotivityClient == null) {
			IoTivityUtils.createErrorResponseMessage(responseMessage, getPlatformUnregisterException());
			return responseMessage;
		}	
		try{			
			String body = translator.toFormatX(message.getPayload().getJenaModel());
			Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
			
			//get a map that contains all registered devices on the server
			Map<String, JsonElement> deviceMap = iotivityClient.listDevices();

			for (String entityId : entities) {		
				//check if there is already registered a device with the given id
				String id = IoTivityUtils.getThingId(entityId);
				JsonElement deviceJsonElement = deviceMap.get(id);
				if (deviceJsonElement != null) {
					throw new Exception("There is already a device with given id : " + entityId);
				}
				
				//create map that will be used as payload
				Map<String, Object> map = IoTivityUtils.jsonToMap(body);
				map.put("id", id);
				logger.debug("Registering thing {}...", id);
				
				//find uri
				JsonElement result = iotivityClient.getResource("/oic/res?rt=oic.wk.res");
				String resourceType = IoTivityUtils.findResourceTypeOfRequest(map);
				String url = IoTivityUtils.findHrefByResourceType(result, resourceType);
				
				iotivityClient.createResource(map, url);
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
		if (iotivityClient == null) {
			IoTivityUtils.createErrorResponseMessage(responseMessage, getPlatformUnregisterException());
			return responseMessage;
		}	
		try{
			String body = translator.toFormatX(message.getPayload().getJenaModel());
			Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
			
			//get a map that contains all registered devices on the server
			Map<String, JsonElement> deviceMap = iotivityClient.listDevices();

			for (String entityId : entities) {
				String id = IoTivityUtils.getThingId(entityId);
				JsonElement deviceJsonElement = deviceMap.get(id);
				if (deviceJsonElement == null) {
					throw new Exception("There is no device with given id : " + entityId);
				}
				String href = deviceJsonElement.getAsJsonObject().get("href").getAsString();
				logger.debug("Updating thing {}...", href);
				Map<String, Object> map = IoTivityUtils.jsonToMap(body);
				iotivityClient.editResource(map, href);
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
	//DONE
	public Message platformDeleteDevices(Message message) throws Exception {
		Message responseMessage = createResponseMessage(message);
		if (iotivityClient == null) {
			IoTivityUtils.createErrorResponseMessage(responseMessage, getPlatformUnregisterException());
			return responseMessage;
		}	
		try {
			logger.debug("Removing devices...");
			Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
			Map<String, JsonElement> map = iotivityClient.listDevices();
			for(String deviceId : entities){
				String id = IoTivityUtils.getThingId(deviceId);

				JsonElement deviceJsonElement = map.get(id);
				if (deviceJsonElement == null) {
					throw new Exception("There is no device with id '"+id+"' to be deleted");
				}
				String deviceHref = IoTivityUtils.getHref(deviceJsonElement);
				iotivityClient.deleteResource(deviceHref);
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
		Message responseMessage = createResponseMessage(message);
		if (iotivityClient == null) {
			IoTivityUtils.createErrorResponseMessage(responseMessage, getPlatformUnregisterException());
			return responseMessage;
		}	
		try{
			String body = translator.toFormatX(message.getPayload().getJenaModel());
			Set<String> entities = IoTivityUtils.getDeviceIDsFromPayload(message);
			iotivityClient.isPlatformRegistered();
			
			//get a map that contains all registered devices on the server
			Map<String, JsonElement> deviceMap = iotivityClient.listDevices();

			for (String entityId : entities) {
				String id = IoTivityUtils.getThingId(entityId);
				JsonElement deviceJsonElement = deviceMap.get(id);
				if (deviceJsonElement == null) {
					throw new Exception("There is no device with given id : " + entityId);
				}
				String href = deviceJsonElement.getAsJsonObject().get("href").getAsString();
				logger.debug("Updating thing {}...", href);
				Map<String, Object> map = IoTivityUtils.jsonToMap(body);
				iotivityClient.editResource(map, href);
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
	
	/**
	 * Creates an exception that contains a message indicating that there is no registered platform
	 */
	private Exception getPlatformUnregisterException() {
		return new Exception("There is no registered platform");
	}
	
	/**
	 * Creates a message metadata instance according to the given original request (the conversation id is retreived) and message type
	 */
	public static PlatformMessageMetadata createMessageMetadata(Message originalMessage, MessageTypesEnum messageType, String platformId) {
	    PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
	    metadata.initializeMetadata();
	    metadata.addMessageType(messageType);
	    metadata.setSenderPlatformId(new EntityID(platformId));
	    String conversationId = originalMessage.getMetadata().getConversationId().orElse(null);
	    metadata.setConversationId(conversationId);
	    return metadata;
	}
}
