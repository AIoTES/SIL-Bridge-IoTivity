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

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.interiot.intermw.bridge.abstracts.AbstractBridge;
import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.commons.exceptions.MiddlewareException;
import eu.interiot.intermw.commons.interfaces.Configuration;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.Message;
import eu.interiot.message.MessageMetadata;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.exceptions.payload.PayloadException;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata;
import eu.interiot.message.metadata.PlatformMessageMetadata;
import eu.interiot.message.utils.INTERMWDemoUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@eu.interiot.intermw.bridge.annotations.Bridge(platformType = "ExamplePlatform")
public class ExampleBridge extends AbstractBridge {
    private final Logger logger = LoggerFactory.getLogger(ExampleBridge.class);
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    public ExampleBridge(Configuration configuration, Platform platform) throws MiddlewareException {
        super(configuration, platform);
        logger.debug("Example bridge is initializing...");
        Properties properties = configuration.getProperties();

        httpClient = HttpClientBuilder.create().build();
        objectMapper = new ObjectMapper();

        logger.info("Example bridge has been initialized successfully.");
    }

    @Override
    public Message registerPlatform(Message message) throws Exception {
        Set<String> entityIDs = INTERMWDemoUtils.getEntityIDsFromPayload(
                message.getPayload(), INTERMWDemoUtils.EntityTypePlatform);
        if (entityIDs.size() != 1) {
            throw new BridgeException("Missing platform ID.");
        }
        String platformId = entityIDs.iterator().next();
        logger.debug("Registering platform {}...", platformId);
        return createResponseMessage(message);
    }

    @Override
    public Message unregisterPlatform(Message message) throws Exception {
        Set<String> entityIDs = INTERMWDemoUtils.getEntityIDsFromPayload(
                message.getPayload(), INTERMWDemoUtils.EntityTypePlatform);
        if (entityIDs.size() != 1) {
            throw new BridgeException("Missing platform ID.");
        }
        String platformId = entityIDs.iterator().next();
        logger.debug("Unregistering platform {}...", platformId);
        return createResponseMessage(message);
    }

    @Override
    public Message subscribe(Message message) throws Exception {
        Set<String> entities = INTERMWDemoUtils.getEntityIDsFromPayload(message.getPayload(),
                INTERMWDemoUtils.EntityTypeDevice);
        if (entities.isEmpty()) {
            throw new PayloadException("No entities of type Device found in the Payload.");
        } else if (entities.size() > 1) {
            throw new PayloadException("Only one device is supported by Subscribe operation.");
        }

        String thingId = entities.iterator().next();
        String conversationId = message.getMetadata().getConversationId().orElse(null);

        logger.debug("Subscribing to thing {} using conversationId {}...", thingId, conversationId);

        URL callbackUrl = new URL(bridgeCallbackUrl, conversationId);

        HttpPost httpPost = new HttpPost(platform.getBaseURL() + "/things/subscribe");
        Map<String, Object> data = new HashMap<>();
        data.put("thingId", thingId);
        data.put("conversationId", conversationId);
        data.put("callbackUrl", callbackUrl.toString());
        String json = objectMapper.writeValueAsString(data);
        HttpEntity httpEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
        httpPost.setEntity(httpEntity);
        HttpResponse resp = httpClient.execute(httpPost);
        logger.debug("Received response from the platform: {}", resp.getStatusLine());

        logger.debug("Creating callback listener listening at {}...", callbackUrl);
        Spark.post(conversationId, (request, response) -> {
            logger.debug("Received observation from the platform.");
            PlatformMessageMetadata metadata = new MessageMetadata().asPlatformMessageMetadata();
            metadata.initializeMetadata();
            metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.OBSERVATION);
            metadata.addMessageType(URIManagerMessageMetadata.MessageTypesEnum.RESPONSE);
            metadata.setSenderPlatformId(new EntityID(platform.getId().getId()));
            metadata.setConversationId(conversationId);

            String observationN3 = request.body();
            Model m = ModelFactory.createDefaultModel();
            InputStream inStream = new ByteArrayInputStream(observationN3.getBytes());
            RDFDataMgr.read(m, inStream, Lang.N3);
            MessagePayload messagePayload = new MessagePayload(m);

            Message observationMessage = new Message();
            observationMessage.setMetadata(metadata);
            observationMessage.setPayload(messagePayload);

            publisher.publish(observationMessage);
            logger.debug("Observation message has been published upstream.");

            response.status(204);
            return "";
        });
        logger.debug("Successfully subscribed to thing {}.", thingId);
        return createResponseMessage(message);
    }

    @Override
    public Message unsubscribe(Message message) throws Exception {
        String conversationID = message.getMetadata().getConversationId().orElse(null);
        logger.debug("Unsubscribing from thing {}...", conversationID);
        return createResponseMessage(message);
    }

    @Override
    public Message query(Message message) throws Exception {
        return null;
    }

    @Override
    public Message listDevices(Message message) throws Exception {
        return null;
    }

    @Override
    public Message platformCreateDevice(Message message) throws Exception {
        return null;
    }

    @Override
    public Message platformUpdateDevice(Message message) throws Exception {
        return null;
    }

    @Override
    public Message platformDeleteDevice(Message message) throws Exception {
        return null;
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
        return null;
    }

    @Override
    public Message unrecognized(Message message) throws Exception {
        return null;
    }
}
