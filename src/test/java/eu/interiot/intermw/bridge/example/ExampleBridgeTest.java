/**
 * INTER-IoT. Interoperability of IoT Platforms.
 * INTER-IoT is a R&D project which has received funding from the European
 * Union’s Horizon 2020 research and innovation programme under grant
 * agreement No 687283.
 * <p>
 * Copyright (C) 2016-2018, by (Author's company of this file):
 * - XLAB d.o.o.
 * <p>
 * This code is licensed under the EPL license, available at the root
 * application directory.
 */
package eu.interiot.intermw.bridge.example;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.bridge.iotivity.IoTivityBridge;
import eu.interiot.intermw.commons.DefaultConfiguration;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.exceptions.MessageException;
import eu.interiot.message.Message;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import spark.Spark;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ExampleBridgeTest {
    private ExamplePlatformEmulator platformEmulator;

    @Before
    public void setUp() throws Exception {
        platformEmulator = new ExamplePlatformEmulator(4569, 5);
        platformEmulator.start();
    }

    @After
    public void tearDown() {
        platformEmulator.stop();
    }

    @Test
    public void testBridge() throws Exception {
        Configuration configuration = new DefaultConfiguration("*.bridge.properties");

        URL callbackUrl = new URL(configuration.getProperty("bridge.callback.address"));
        int callbackPort = callbackUrl.getPort();
        Spark.port(callbackPort);
        
        boolean observe = false;

        // create Message objects from serialized messages
        Message platformRegisterMsg = createMessage("messages/platform-register.json");

        // create Platform object using platform-register message
        EntityID platformId = platformRegisterMsg.getMetadata().asPlatformMessageMetadata().getReceivingPlatformIDs().iterator().next();
        Platform platform = new Platform(platformId.toString(), platformRegisterMsg.getPayload());

        IoTivityBridge exampleBridge = new IoTivityBridge(configuration, platform);
        PublisherMock<Message> publisher = new PublisherMock<>();
        exampleBridge.setPublisher(publisher);
        Set<MessageTypesEnum> messageTypesEnumSet;

//        // register platform
        System.out.println("REGISTER PLATFORM");
        messageTypesEnumSet = sendMessage(exampleBridge,  createMessage("messages/platform-register.json"), publisher);
        assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.RESPONSE));
        assertTrue(messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.PLATFORM_REGISTER));
        System.out.println("***************************************************************");
        
//        // unregister platform
//        System.out.println("UNREGISTER PLATFORM");
//        messageTypesEnumSet = sendMessage(exampleBridge,  createMessage("messages/platform-unregister.json"), publisher);
//        assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.RESPONSE));
//        assertTrue(messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.PLATFORM_UNREGISTER));
//        System.out.println("***************************************************************");
        
//	      // list devices
//	      System.out.println("LIST DEVICES");
//	      messageTypesEnumSet = sendMessage(exampleBridge,  createMessage("messages/list-devices.json"), publisher);
//	      assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.RESPONSE));
//	      assertTrue(messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.LIST_DEVICES));
//	      System.out.println("***************************************************************");
        
//          // query
//	      System.out.println("QUERY");
//	      messageTypesEnumSet = sendMessage(exampleBridge,  createMessage("messages/query.json"), publisher);
//	      assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.RESPONSE));
//	      assertTrue(messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.QUERY));
//	      System.out.println("***************************************************************");
        
//        //delete thing
//        System.out.println("DELETE THING");
//        messageTypesEnumSet = sendMessage(exampleBridge, createMessage("messages/thing-delete.json"), publisher);
//        assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.RESPONSE));
//        assertTrue(messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.DEVICE_REMOVE));
//        System.out.println("***************************************************************");
        
//        //register thing
//        System.out.println("REGISTER THING");
//        messageTypesEnumSet = sendMessage(exampleBridge, createMessage("messages/thing-register.json"), publisher);
//        assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.RESPONSE));
//        assertTrue(messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.DEVICE_ADD_OR_UPDATE));
//        System.out.println("***************************************************************");
        
//        //register thing
//        System.out.println("EDIT THING");
//        messageTypesEnumSet = sendMessage(exampleBridge, createMessage("messages/thing-update.json"), publisher);
//        assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.RESPONSE));
//        assertTrue(messageTypesEnumSet.contains(URIManagerMessageMetadata.MessageTypesEnum.DEVICE_ADD_OR_UPDATE));
//        System.out.println("***************************************************************");

//        // subscribe to thing
//        System.out.println("SUBSCRIBE TO THING");
//        messageTypesEnumSet = sendMessage(exampleBridge, createMessage("messages/thing-subscribe.json"), publisher);
//        assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.RESPONSE));
//        assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.SUBSCRIBE));
//    	  observe = true;
//        System.out.println("***************************************************************");
      
        
//      // unsubscribe from thing
//      Thread.sleep(2000);
//      System.out.println("UNSUBSCRIBE FROM THING");
//      messageTypesEnumSet = sendMessage(exampleBridge, createMessage("messages/thing-unsubscribe.json"), publisher);
//      assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.RESPONSE));
//      assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.UNSUBSCRIBE));
//      System.out.println("***************************************************************");

       if (observe) {
    	   waitForObervationMessage(publisher, 20);
       }
    }
    
    /**
     * This method waits fin order to retrieve an observation message and is used only if a subscription message has 
     * been sent
     * @param publisher : the publisher used for retrieving observation messages
     * @param seconds : the amount of time in seconds that this method waits
     * @throws InterruptedException : if any thread has interrupted the current thread. 
     * The interrupted status of the current thread is cleared when this exception is thrown.
     */
    private void waitForObervationMessage(PublisherMock<Message> publisher, int seconds) throws InterruptedException {
        Long startTime = new Date().getTime();
        Message observationMsg = null;
        do {
            Thread.sleep(1000);
            observationMsg = publisher.retrieveMessage();
        } while (new Date().getTime() - startTime < seconds * 1000);

        if (observationMsg != null) {
        	Set<MessageTypesEnum>  messageTypesEnumSet = observationMsg.getMetadata().getMessageTypes();
            assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.RESPONSE));
            assertTrue(messageTypesEnumSet.contains(MessageTypesEnum.OBSERVATION));
        } else {
            fail("Timeout waiting for observation messages.");
        }
    }
    
    /**
     * This method sends {@code msg} to {@code bridge} and receives a response
     * @param bridge : this is where the message is sent
     * @param msg : the message that is sent
     * @param publisher : the publisher used for retrieving responses
     * @return : the message types of the response
     * @throws BridgeException
     */
    private  Set<MessageTypesEnum> sendMessage(IoTivityBridge bridge, Message msg, PublisherMock<Message> publisher) throws BridgeException{
    	bridge.send(msg);
        Message responseMsg = publisher.retrieveMessage();
        return responseMsg.getMetadata().getMessageTypes();
    }
    
    /**
     * Method that uses the {@code messagePath} to load the equivalent message from the resources
     * and creates a {@code Message} instance
     * @param messagePath : the relative path of the file that contains the message in the resources
     * @return a {@code Message} instance
     * @throws IOException - if an I/O error occurs.
     * @throws MessageException - in case the format of the loaded message is not correct and it is 
     * not possible to create a {@code Message} instance
     */
    private Message createMessage(String messagePath) throws IOException, MessageException {
        URL url = Resources.getResource(messagePath);
        String resourceString = Resources.toString(url, Charsets.UTF_8);
        return new Message(resourceString);
    }
}
