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

import org.apache.jena.rdf.model.Model;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import eu.interiot.intermw.bridge.iotivity.client.utils.EncodingUtils;
import eu.interiot.intermw.comm.broker.Publisher;
import eu.interiot.intermw.translators.syntax.iotivity.IotivityTranslator;
import eu.interiot.message.Message;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.metadata.PlatformMessageMetadata;
/**
 * 
 * This class provides the functionalities of a handler needed for observing a resource
 * It defines the procedure to be followed whenever a new observation is sent by the platform
 * and the procedure in case of an error
 *
 */
public class IoTivityCoapHandler implements CoapHandler {

	private final Logger logger = LoggerFactory.getLogger(IoTivityCoapHandler.class);
	private PlatformMessageMetadata metadata;
	private IotivityTranslator translator;
	private Publisher<Message> publisher;

	public IoTivityCoapHandler(PlatformMessageMetadata metadata, IotivityTranslator translator,
			Publisher<Message> publisher) {
		super();
		this.metadata = metadata;
		this.translator = translator;
		this.publisher = publisher;
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.californium.core.CoapHandler#onLoad(org.eclipse.californium.core.CoapResponse)
	 */
	public void onLoad(CoapResponse response) {
		try {
			if (response.getPayload() != null) {
				logger.debug("Received observation from the platform.");
				System.out.println("Received observation from the platform");
				JsonElement responseJson = EncodingUtils.coapResponseToJson(response.getPayload());
				System.out.println("---> " + responseJson);
				JsonParser parser = new JsonParser();
				JsonObject observationObject = parser.parse(responseJson.toString()).getAsJsonObject();
				Model translatedModel = translator.toJenaModel(observationObject.toString());
				MessagePayload responsePayload = new MessagePayload(translatedModel);

				Message observationMessage = new Message();
				observationMessage.setMetadata(metadata);
				observationMessage.setPayload(responsePayload);

				publisher.publish(observationMessage);
				logger.debug("Observation message has been published upstream.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.debug("Error while handling the observation message");
		}
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.californium.core.CoapHandler#onError()
	 */
	public void onError() {
		logger.debug("Received observation from the platform using the coap client failed.");
		System.out.println("Received observation from the platform using the coap client failed.");
	}
}
