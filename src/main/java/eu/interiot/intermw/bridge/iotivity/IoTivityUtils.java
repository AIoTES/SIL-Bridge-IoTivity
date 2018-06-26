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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.vocabulary.RDF;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.translators.syntax.iotivity.IotivityTranslator;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.managers.URI.URIManagerMessageMetadata.MessageTypesEnum;
import eu.interiot.message.Message;

/**
 * 
 * Utility class that contains methods for manipulating incoming messages
 * Used by the IoTivity bridge
 *
 */
public class IoTivityUtils {
	
	public static final String URIsosa = "http://www.w3.org/ns/sosa/";;
	public static final String EntityTypeInstance = IotivityTranslator.iotivityBaseURI + "Instance";
	public static final String EntityTypePlatform = URIsosa + "Platform";
	public static final String EntityTypeDevice = "http://purl.oclc.org/NET/ssnx/ssn#Device";
	
	/**
	 * Retrieves the id of the given {@code platform}
	 * 
	 * @param platform : the platform to get processed
	 * @return the id
	 */
	public static String getPlatformId(Platform platform){
		return platform.getPlatformId();
	}
	
	/**
	 * Method that checks the {@code payload} and retreives the ids of the entities that have type equal to {@code entityType}
	 * 
	 * @param payload : the payload that contain the info
	 * @param entityType : the type of the wanted entities
	 * @return a set of entity ids
	 */
	public static Set<String> getEntityIDsFromPayload(MessagePayload payload, String entityType) {
        Model model = payload.getJenaModel();
        return model.listStatements(new SimpleSelector(null, RDF.type, model.createResource(entityType))).toSet().stream().map(x -> x.getSubject().toString()).collect(Collectors.toSet());
    }
	
	/**
	 * Method that appends the {@code responseMessage} in order to create an error response message
	 * 
	 * @param responseMessage : the response message to be appended
	 * @param e : the exception that was created. Needed for identifing the type of the error
	 */
	public static void createErrorResponseMessage(Message responseMessage, Exception e) {
		responseMessage.getMetadata().setStatus("KO");
		responseMessage.getMetadata().setMessageType(MessageTypesEnum.ERROR);
		responseMessage.getMetadata().asErrorMessageMetadata().setExceptionStackTrace(e);
	}
	
	/**
	 * It processes the given resource URI ({@code thingId}) and retrieves the equivalent id
	 * 
	 * @param thingId : the URI of an instance
	 * @return the URL of the given resource
	 * @throws BridgeException in case the given {@code thingId} is null or empty
	 */
	public static String getThingId(String thingId) throws BridgeException {
		if (thingId == null || thingId.isEmpty()) {
			throw new BridgeException("The thing id is null or empty");
		}
		return thingId.replace(EntityTypeInstance+"/", "");
	}
	
	/**
	 * Converts a JSON in {@code String} format to {@code Map}
	 * @param json
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static Map<String, Object> jsonToMap(String json) throws JsonParseException, JsonMappingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = new HashMap<String, Object>();
		map = mapper.readValue(json, new TypeReference<Map<String, Object>>(){});
		return map;
	}
	
	/**
	 * Method that checks the payload of the given {@code message} and retrieves the ids of the entities that have
	 * type {@value #EntityTypeDevice}
	 * @param message the message that contains the payload
	 * @return a set of device ids
	 */
	public static Set<String> getDeviceIDsFromPayload(Message message) {
		return getEntityIDsFromPayload(message.getPayload(), EntityTypeDevice);
	}
	
	/**
	 * Parses a JSON and returns the all devices value
	 * @param allDevices : the {@code JsonObject} to be parsed
	 * @return
	 */
	public static JsonElement getDeviceList(JsonObject allDevices){
		return allDevices.get("alldevices");
	}
	
	/**
	 * Returns the equivalent URL according to the given device type
	 * @param type : the type of the device
	 * @param rootURL : the root URL that contains all devices
	 * @return
	 * @throws Exception in case the given type is not equivalent to one of the supported
	 */
	public static String getDeviceURLByType(String type, String rootURL) throws Exception {
		if (type.equals("bloodpressure") || type.equals("oximeter")) {
			return rootURL+"/"+type;
		}
		throw new Exception("The given type ("+type+") is not supported");
	}
}
