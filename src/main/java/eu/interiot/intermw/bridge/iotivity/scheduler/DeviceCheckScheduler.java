package eu.interiot.intermw.bridge.iotivity.scheduler;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;

import eu.interiot.intermw.bridge.iotivity.client.IoTivityClient;
import eu.interiot.intermw.comm.broker.Publisher;
import eu.interiot.message.Message;

/**
 * 
 * @author stavrotheodoros
 *
 * This scheduler uses a fix number of threads to periodically check if there are any changes in the registered devices
 * (new addition or removal)
 * 
 */
public class DeviceCheckScheduler {
	
	//number of threads used by this scheduler
	private static final int NUM_OF_THREADS = 1;
	
	//the time between each execution
	private static final int SECONDS_INTERVAL = 60;
		
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(NUM_OF_THREADS);
	
	protected IoTivityClient iotivityClient;
	protected Map<String, JsonElement> initialDeviceMap;
	protected Message message;
	protected Publisher<Message> publisher;
	protected String platformId;
	
	public DeviceCheckScheduler(IoTivityClient iotivityClient, Map<String, JsonElement> initialDeviceMap, Message message, Publisher<Message> publisher, String platformId){
		this.iotivityClient = iotivityClient;
		this.initialDeviceMap = initialDeviceMap;
		this.message = message;
		this.publisher = publisher;
		this.platformId = platformId;
	}
	
	public void check() {
	       final Runnable retreive = new CheckDevicesTask(iotivityClient, initialDeviceMap, message, publisher, platformId);
	       scheduler.scheduleAtFixedRate(retreive, 0 , SECONDS_INTERVAL, TimeUnit.SECONDS);
	};	
}
