package eu.interiot.intermw.bridge.iotivity.scheduler;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import eu.interiot.intermw.bridge.iotivity.IoTivityBridge;
import eu.interiot.intermw.bridge.iotivity.client.IoTivityClient;
import eu.interiot.intermw.comm.broker.Publisher;
import eu.interiot.intermw.translators.syntax.iotivity.IotivityTranslator;
import eu.interiot.message.Message;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import eu.interiot.message.metadata.PlatformMessageMetadata;

/**
 * 
 * @author stavrotheodoros
 * 
 * Runnable that checks if there is any change in the registered devices of iotivity
 *
 */
public class CheckDevicesTask implements Runnable{
	
	protected IoTivityClient iotivityClient;
	protected Map<String, JsonElement> initialDeviceMap;
	protected Message message;
	protected Publisher<Message> publisher;
	protected String platformId;
	protected IotivityTranslator translator = new IotivityTranslator();
	private final Logger logger = LoggerFactory.getLogger(CheckDevicesTask.class);


		
	public CheckDevicesTask(IoTivityClient iotivityClient, Map<String, JsonElement> initialDeviceMap, Message message, Publisher<Message> publisher, String platformId){
		this.iotivityClient = iotivityClient;
		this.initialDeviceMap = initialDeviceMap;
		this.message = message;
		this.publisher = publisher;
		this.platformId = platformId;
	}

	@Override
	public void run() {
		try {
			Map<String, JsonElement> map = iotivityClient.listDevices();		
			for (Entry<String, JsonElement> x : map.entrySet()) {
				if (!initialDeviceMap.containsKey(x.getKey())) {
					deviceAdd(x.getKey(), x.getValue(), message);
					initialDeviceMap.put(x.getKey(), x.getValue());
				}
			}
			Set<String> deviceIdsForRemoval = new HashSet<String>();
			for (Entry<String, JsonElement> x : initialDeviceMap.entrySet()) {
				if (!map.containsKey(x.getKey())) {
					deviceRemove(x.getKey(), x.getValue(), message);
					deviceIdsForRemoval.add(x.getKey());
				}
			}
			for (String deviceId : deviceIdsForRemoval) {
				initialDeviceMap.remove(deviceId);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Message deviceAdd(String deviceId, JsonElement deviceRepresentation, Message message) throws Exception {
		sendDeviceMessage(deviceId, deviceRepresentation, message, URIManagerMessageMetadata.MessageTypesEnum.DEVICE_ADD_OR_UPDATE);
	    logger.debug("Device_Add message for device with id="+deviceId+" has been published upstream.");
	    return message;
	}
	
	private Message deviceRemove(String deviceId, JsonElement deviceRepresentation, Message message) throws Exception {
		sendDeviceMessage(deviceId, deviceRepresentation, message, URIManagerMessageMetadata.MessageTypesEnum.DEVICE_REMOVE);
	    logger.debug("Device_remove message for device with id="+deviceId+" has been published upstream.");
	    return message;
	}
	
	private Message sendDeviceMessage(String deviceId, JsonElement deviceRepresentation, Message message, MessageTypesEnum messageType) throws Exception {
		Model translatedModel = translator.toJenaModel(deviceRepresentation.toString());
	    Message deviceMessage = new Message();
	    PlatformMessageMetadata metadata = IoTivityBridge.createMessageMetadata(message, messageType, platformId);
	    MessagePayload devicePayload = new MessagePayload(translatedModel);
	    deviceMessage.setMetadata(metadata);
	    deviceMessage.setPayload(devicePayload);
	    publisher.publish(deviceMessage);
	    message.getMetadata().setStatus("OK");
	    return message;
	}
}
