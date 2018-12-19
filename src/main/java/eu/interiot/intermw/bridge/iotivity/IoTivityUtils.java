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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import eu.interiot.intermw.bridge.exceptions.BridgeException;
import eu.interiot.intermw.commons.model.Platform;
import eu.interiot.intermw.translators.syntax.iotivity.IotivityTranslator;
import eu.interiot.message.MessagePayload;
import eu.interiot.message.ID.EntityID;
import eu.interiot.message.ID.PropertyID;
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
	public static final String EntityTypeDevice = "http://inter-iot.eu/syntax/Iotivity.owl#Device";
	public static final String AiotesEntityTypeDevice = "http://inter-iot.eu/GOIoTP#IoTDevice";

	
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
		Set<String> entityIDs = getEntityIDsFromPayload(message.getPayload(), EntityTypeDevice);
		entityIDs.addAll(getEntityIDsFromPayload(message.getPayload(), AiotesEntityTypeDevice));
		return entityIDs;
	}
	
	/**
	 * Parses a JSON and returns the all devices value
	 * @param allDevices : the {@code JsonObject} to be parsed
	 * @return
	 */
	public static JsonArray getDeviceList(JsonElement allDevices){
		JsonArray result = new JsonArray();
		if (allDevices.isJsonNull()) return result;
		else if (allDevices.isJsonArray()) {
			JsonArray array = allDevices.getAsJsonArray();
			for (int i = 0 ; i < array.size(); i++) {
				JsonObject platform = array.get(i).getAsJsonObject();
				handlePlatform(platform, result);
			}
		}
		else if (allDevices.isJsonObject()) {
			JsonObject platform = allDevices.getAsJsonObject();
			handlePlatform(platform, result);
		}
		return result;
	}
	
	private static void handlePlatform(JsonObject platform, JsonArray result) {
		JsonArray devices  = platform.get("links").getAsJsonArray();
		for (JsonElement d: devices) {
			String href = getHref(d);
			if (href == null || href.equals("/oic/d")) {
				continue;
			}
			result.add(d);
		}
	}
	
	public static String getHref(JsonElement jsonElement) {
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		if (jsonObject.has("href")) {
			return jsonObject.get("href").getAsString();
		}
		return null;
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
	
	@SuppressWarnings("unchecked")
	public static String findResourceTypeOfRequest(Map<String, Object> map){
		ArrayList<String> resourceTypes = (ArrayList<String>) map.get("rt");
		resourceTypes.remove("oic.wk.d");
		return resourceTypes.get(0);
	}
	
	public static String findHrefByResourceType(JsonElement json, String rt) {
		JsonArray array = json.getAsJsonArray();
		for (int i = 0; i < array.size(); i++) {
			JsonArray linksArray = array.get(i).getAsJsonObject().get("links").getAsJsonArray();
			for (int j = 0; j < linksArray.size(); j++) {
				JsonObject link = linksArray.get(j).getAsJsonObject();
				String href = getHref(link);
				JsonArray resourceTypes = link.get("rt").getAsJsonArray();
				for (int k = 0; k < resourceTypes.size(); k++) {
					String resourceType = resourceTypes.get(k).getAsString();
					if (resourceType.equals(rt)) {
						return href;
					}
				}
			}
		}
		return null;
	}
	
	public static Resource getAuthenticationData(Model model, String platformId) {
		StmtIterator it = model.listStatements();
		Resource userUri = null;
		while (it.hasNext()) {
			Statement st = it.next();
			if (st.getSubject().getURI() != null && st.getSubject().getURI().equals(platformId) && st.getPredicate().getURI().equals("http://inter-iot.eu/GOIoTP#hasUser")) {
				userUri = st.getObject().asResource();
				break;
			}
		}
		if (userUri == null) return null;
		it = model.listStatements();
		Resource authenticationData = null;
		while (it.hasNext()) {
			Statement st = it.next();
			if (st.getSubject().equals(userUri) && st.getPredicate().getURI().equals("http://inter-iot.eu/GOIoTPex#hasAuthenticationData")) {
				authenticationData = st.getObject().asResource();
				return authenticationData;
			}
		}
		return null;
	}
	
	public static Resource getUpstreamChannelConfig(Model model, String platformId) {
		return getChannelConfig(model, platformId, "http://inter-iot.eu/INTERMW#hasUpstreamChannelConfig");	
	}

	public static Resource getDownstreamChannelConfig(Model model, String platformId) {
		return getChannelConfig(model, platformId, "http://inter-iot.eu/INTERMW#hasDownstreamChannelConfig");	
	}
	
	
	
	public static Resource getChannelConfig(Model model, String platformId, String propertyUri) {
		StmtIterator it = model.listStatements();
		while (it.hasNext()) {
			Statement st = it.next();
			if (st.getSubject().getURI() != null && st.getSubject().getURI().equals(platformId) && st.getPredicate().getURI().equals(propertyUri)) {
				return st.getObject().asResource();
			}
		}
		return null;
	}
	
	public static Resource getInputAllignment(Resource channelConfigResource, Model model) {
		return getAllignment(channelConfigResource, model, "http://inter-iot.eu/INTERMW#hasInputAlignment");
	}
	
	public static Resource getOutputAllignment(Resource channelConfigResource, Model model) {
		return getAllignment(channelConfigResource, model, "http://inter-iot.eu/INTERMW#hasOutputAlignment");
	}
	
	private static Resource getAllignment(Resource channelConfigResource, Model model, String propertyUri) {
		StmtIterator it = model.listStatements();
		while (it.hasNext()) {
			Statement st = it.next();
			if (st.getSubject().equals(channelConfigResource) && st.getPredicate().getURI().equals(propertyUri)) {
				return st.getObject().asResource();
			}
		}
		return null;
	}
	
	public static String getUsername(Model model, Resource authenticationData) {
		return getAuthenticationDataPropertyValue(model, authenticationData, "http://inter-iot.eu/INTERMW#username");
	}
	
	public static String getEncryptedPassord(Model model, Resource authenticationData) {
		return getAuthenticationDataPropertyValue(model, authenticationData, "http://inter-iot.eu/INTERMW#encryptedPassword");
	}
	
	public static String getEncryptionAlgorithm(Model model, Resource authenticationData) {
		return getAuthenticationDataPropertyValue(model, authenticationData, "http://inter-iot.eu/INTERMW#encryptionAlgorithm");
	}
	
	public static String getPlatformLocation(Model model, String platformId) {
		StmtIterator it = model.listStatements();
		while (it.hasNext()) {
			Statement st = it.next();
			if (st.getSubject().getURI() != null && st.getSubject().getURI().equals(platformId) && st.getPredicate().getURI().equals("http://inter-iot.eu/GOIoTP#hasLocation")) {
				return st.getObject().asResource().getURI();
			}
		}
		return null;
	}
	
	
	/**
	 * Method that returns from an authentication data resource the value of the given property
	 * @param model
	 * @param authenticationData
	 * @param propertyUri
	 * @return
	 */
	private static String getAuthenticationDataPropertyValue(Model model, Resource authenticationData, String propertyUri) {
		StmtIterator it = model.listStatements();
		while (it.hasNext()) {
			Statement st = it.next();
			if (st.getSubject().equals(authenticationData) && st.getPredicate().getURI().equals(propertyUri)) {
				return st.getObject().asLiteral().getString();
			}
		}
		return null;
	}
	
	public static Platform updatePlatform(Platform platform, MessagePayload messagePayload) throws MalformedURLException {
		EntityID platformEntity = new EntityID(platform.getPlatformId());
		Map<PropertyID, Object> map = messagePayload.getAllDataPropertyAssertionsForEntity(platformEntity);
		String baseEndPoint = (String) map.get(new PropertyID("http://inter-iot.eu/GOIoTP#hasBaseEndpoint"));
		if (baseEndPoint != null) {
			platform.setBaseEndpoint(new URL(baseEndPoint));
		}
		String name = (String) map.get(new PropertyID("http://inter-iot.eu/GOIoTP#hasName"));
		if (name != null) {
			platform.setName(name);
		}
		Model payloadModel = messagePayload.getJenaModel();
		String location = IoTivityUtils.getPlatformLocation(payloadModel, platform.getPlatformId());
		if (location != null) {
			platform.setLocation(location);
		}
		Resource authenticationData = IoTivityUtils.getAuthenticationData(payloadModel, platform.getPlatformId());
		if (authenticationData != null) {
			String username = IoTivityUtils.getUsername(payloadModel, authenticationData);
			if (username != null) {
				platform.setUsername(username);

			}
			String password = IoTivityUtils.getEncryptedPassord(payloadModel, authenticationData);
			if (password != null) {
				platform.setEncryptedPassword(password);
			}
			String encryptionAlgorithm = IoTivityUtils.getEncryptionAlgorithm(payloadModel, authenticationData);
			if (encryptionAlgorithm != null) {
				platform.setEncryptionAlgorithm(encryptionAlgorithm);
			}
		}
		Resource upstreamChannelConfig = IoTivityUtils.getUpstreamChannelConfig(payloadModel, platform.getPlatformId());
		if (upstreamChannelConfig != null) {
			Resource inputAllignement = IoTivityUtils.getInputAllignment(upstreamChannelConfig, payloadModel);
			String inputAllignementName = getAllignmentName(inputAllignement, payloadModel);
			if (inputAllignementName != null) {
				platform.setUpstreamInputAlignmentName(inputAllignementName);
			}	
			String inputAllignementVersion = getAllignmentVersion(inputAllignement, payloadModel);
			if (inputAllignementVersion != null) {
				platform.setUpstreamInputAlignmentVersion(inputAllignementVersion);
			}
			Resource outputAllignement = IoTivityUtils.getOutputAllignment(upstreamChannelConfig, payloadModel);
			String outputAllignementName = getAllignmentName(outputAllignement, payloadModel);
			if (outputAllignementName != null) {
				platform.setUpstreamOutputAlignmentName(outputAllignementName);
			}	
			String outputAllignementVersion = getAllignmentVersion(outputAllignement, payloadModel);
			if (outputAllignementVersion != null) {
					platform.setUpstreamOutputAlignmentVersion(outputAllignementVersion);
			}
		}
		
		Resource downstreamChannelConfig = IoTivityUtils.getDownstreamChannelConfig(payloadModel, platform.getPlatformId());
		if (downstreamChannelConfig != null) {
			Resource inputAllignement = IoTivityUtils.getInputAllignment(downstreamChannelConfig, payloadModel);
			String inputAllignementName = getAllignmentName(inputAllignement, payloadModel);
			if (inputAllignementName != null) {
				platform.setDownstreamInputAlignmentName(inputAllignementName);
			}	
			String inputAllignementVersion = getAllignmentVersion(inputAllignement, payloadModel);
			if (inputAllignementVersion != null) {
				platform.setDownstreamInputAlignmentVersion(inputAllignementVersion);
			}
			Resource outputAllignement = IoTivityUtils.getOutputAllignment(downstreamChannelConfig, payloadModel);
			String outputAllignementName = getAllignmentName(outputAllignement, payloadModel);
			if (outputAllignementName != null) {
				platform.setDownstreamOutputAlignmentName(outputAllignementName);
			}	
			String outputAllignementVersion = getAllignmentVersion(outputAllignement, payloadModel);
			if (outputAllignementVersion != null) {
					platform.setDownstreamOutputAlignmentVersion(outputAllignementVersion);
			}
		}
		return platform;
	}
	
	private static String getAllignmentName(Resource allignment, Model model) {
		return getAllignmentProperty(allignment, model, "http://inter-iot.eu/GOIoTP#hasName");
	}
	
	private static String getAllignmentVersion(Resource allignment, Model model) {
		return getAllignmentProperty(allignment, model, "http://inter-iot.eu/GOIoTP#hasVersion");
	}
	
	private static String getAllignmentProperty(Resource allignment, Model model, String propertyUri) {
		if (allignment != null) {
			Statement st = allignment.getProperty(model.getProperty(propertyUri));
			if (st != null) {
				return st.getObject().asLiteral().getString();			
			}
		}
		return null;
	}
}
