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
package eu.interiot.intermw.translators.syntax.iotivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import eu.interiot.intermw.bridge.iotivity.IoTivityUtils;
import eu.interiot.intermw.ontology.OntologyHandler;
import eu.interiot.intermw.ontology.entities.Device;
import eu.interiot.translators.syntax.IllegalSyntaxException;
import eu.interiot.translators.syntax.SyntacticTranslator;

/**
 * 
 * Method that is used for translating messages in JENA model format to the
 * format used by the IoTivity
 *
 */
public class IotivityTranslator extends SyntacticTranslator<String> {

	public static String iotivityBaseURI = "http://inter-iot.eu/syntax/Iotivity#";
	public static String interIoT = "http://inter-iot.eu/";

	private Resource arrayType;
	private Resource valueType;
	private Resource instanceType;
	private Resource elementType;

	private Property hasValue;
	private Property hasElement;
	private Property hasNumber;



	/**
	 * Constructor
	 * 
	 * The needed resources and properties for representing IoTivity info to JENA
	 * model are created
	 * 
	 */
	public IotivityTranslator() {
		super(iotivityBaseURI, "Iotivity");

		Model jenaModel = ModelFactory.createDefaultModel();
		arrayType = jenaModel.createResource(getBaseURI() + "Array");
		valueType = jenaModel.createResource(getBaseURI() + "Value");
		instanceType = jenaModel.createResource(getBaseURI() + "Instance");
		elementType = jenaModel.createResource(getBaseURI() + "ArrayElement");

		hasValue = jenaModel.createProperty(getBaseURI() + "hasValue");
		hasElement = jenaModel.createProperty(getBaseURI() + "hasElement");
		hasNumber = jenaModel.createProperty(getBaseURI() + "hasNumber");
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see eu.interiot.translators.syntax.SyntacticTranslator#toFormatX(org.apache.jena.rdf.model.Model)
	 */
	public String toFormatX(Model jenaModelParam) throws Exception {
		Model jenaModel = ModelFactory.createDefaultModel().add(jenaModelParam);
		ObjectMapper mapper = new ObjectMapper();
		LinkedList<JsonNode> jsonNodeList = new LinkedList<JsonNode>();
		//jenaModel.write(System.out, "JSON-LD") ;

		// Find the top-level RDF Entity (the entity that does not appear in Object of
		// RDF triples)
		final String topLevelQuery = "SELECT DISTINCT ?top  WHERE { ?top ?y ?z MINUS {?a ?b ?top} }";
		ResultSet results = executeSelectToJenaModel(jenaModel, topLevelQuery);

		while (results.hasNext()) {
			QuerySolution qs = results.next();
			RDFNode resultNode = qs.get("top");
			StmtIterator typesIt = getTypesOfResource(jenaModel, resultNode.asResource());
			Set<String> types = typesIt.toSet().stream().map(x -> x.getObject().asResource().getURI()).collect(Collectors.toSet());
			Set<String> deviceTypes = OntologyHandler.gi().getAllDeviceUris();
			deviceTypes.retainAll(types);
			if (!deviceTypes.isEmpty()) {
				JsonNode someTopLevelNode = parseRDFEntityToJson(resultNode.asResource(), jenaModel, mapper);
				if (someTopLevelNode != null) {
					jsonNodeList.add(someTopLevelNode);
				}
			}
		}
		JsonNode topLevelNode = null;
		if (jsonNodeList.isEmpty()) {
			throw new IllegalSyntaxException("No top-level RDF entity found");
		} else if (jsonNodeList.size() == 1) {
			topLevelNode = jsonNodeList.getFirst();
		} else {
			topLevelNode = mapper.createArrayNode();
			for (JsonNode node : jsonNodeList) {
				((ArrayNode) topLevelNode).add(node);
			}
		}
		//System.out.println(topLevelNode);
		return topLevelNode.toString();
	}
	
	@Override
	/*
	 * (non-Javadoc)
	 * @see eu.interiot.translators.syntax.SyntacticTranslator#toJenaModel(java.lang.Object)
	 */
	public Model toJenaModel(String formatXString) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonParser parser = mapper.getFactory().createParser(formatXString);
		JsonNode topLevelNode = mapper.readTree(parser);
		Model jenaModel = ModelFactory.createDefaultModel();
		if (topLevelNode.isObject()) {
			Resource myEntity;
			if (topLevelNode.has("id")) {
				myEntity = jenaModel.createResource(instanceType +"/" + topLevelNode.get("id").asText());
			}
			else {
				myEntity = jenaModel.createResource();
			}
			parseJSONObjectToJena(myEntity, topLevelNode, jenaModel, null);
		} else if (topLevelNode.isArray()) {
			Resource arrayResource = jenaModel.createResource();
			arrayResource.addProperty(RDF.type, arrayType);
			parseArrayToJena(arrayResource, topLevelNode, jenaModel);
		} else if (topLevelNode.isValueNode()) {
			Resource valueResource = jenaModel.createResource();
			valueResource.addProperty(RDF.type, valueType);
			parseValueToJena(valueResource, topLevelNode, jenaModel);
		}
		//jenaModel.write(System.out, "TTL") ;
		jenaModel.write(System.out, "JSON-LD") ;

		return jenaModel;
	}
	
	/**
	 * Method that parses an RDF entity and creates the equivalent JSON node
	 * 
	 * @param entityResource : the RDF entity to be parsed
	 * @param jenaModel: the JENA model
	 * @param mapper : an object mapper for the creation of the JSON node
	 * @return a {@code JsonNode} instance
	 * @throws Exception 
	 */
	private JsonNode parseRDFEntityToJson(Resource entityResource, Model jenaModel, ObjectMapper mapper) throws Exception {
		Set<String> deviceTypes = OntologyHandler.gi().getAllDeviceUris();
		StmtIterator typesIt = getTypesOfResource(jenaModel, entityResource);
		while (typesIt.hasNext()) {
			Resource type = typesIt.next().getObject().asResource();
			if (type.equals(instanceType) || deviceTypes.contains(type.getURI())) {
				ObjectNode jsonNode = mapper.createObjectNode();
				parseAttributesToJson(entityResource, jsonNode, jenaModel, mapper);
				return jsonNode;
			} else if (type.equals(arrayType)) {
				ArrayNode jsonNode = parseArrayEntityToJsonArray(entityResource, jenaModel, mapper);
				return jsonNode;
			} else if (type.equals(valueType)) {
				StmtIterator valuesIt = jenaModel.listStatements(entityResource, hasValue, (String) null);
				while (valuesIt.hasNext()) {
					Statement valStmt = valuesIt.next();
					if (valStmt.getObject().isLiteral()) {
						ValueNode jsonNode = parseLiteralToValueNode(valStmt.getObject().asLiteral(), mapper);
						return jsonNode;
					}
				}
			}
		}
		ObjectNode jsonNode = mapper.createObjectNode();
		parseAttributesToJson(entityResource, jsonNode, jenaModel, mapper);
		return jsonNode;
	}

	/**
	 * Method that parses an RDF array entity and creates the equivalent JSON array
	 * 
	 * @param arrayResource : the RDF array entity to be parsed
	 * @param jenaModel: the JENA model
	 * @param mapper : an object mapper for the creation of the JSON array
	 * @return an {@code ArrayNode} instance
	 * @throws Exception 
	 */
	private ArrayNode parseArrayEntityToJsonArray(Resource arrayResource, Model jenaModel, ObjectMapper mapper) throws Exception {
		ArrayNode jsonArrayNode = mapper.createArrayNode();

		StmtIterator arrayElementsIt = getPropertyValuesOfResource(jenaModel, arrayResource, hasElement);
		TreeMap<Integer, JsonNode> arrayElements = new TreeMap<Integer, JsonNode>();
		while (arrayElementsIt.hasNext()) {
			Statement stmt = arrayElementsIt.next();
			if (stmt.getObject().isResource()) {
				Resource arrayElement = stmt.getObject().asResource();
				// Get index
				Optional<Integer> i = Optional.empty();
				StmtIterator indexIterator = getPropertyValuesOfResource(jenaModel, arrayElement, hasNumber);
				if (indexIterator.hasNext()) {
					RDFNode indexNode = indexIterator.next().getObject();
					if (indexNode.isLiteral()) {
						i = Optional.of(indexNode.asLiteral().getInt());
					}
				}
				// Get value
				Optional<JsonNode> elementValueNode = Optional.empty();
				StmtIterator valueIterator = getPropertyValuesOfResource(jenaModel, arrayElement, hasValue);
				if (valueIterator.hasNext()) {
					RDFNode valueNode = valueIterator.next().getObject();
					if (valueNode.isLiteral()) {
						elementValueNode = Optional.of(parseLiteralToValueNode(valueNode.asLiteral(), mapper));
					} else if (valueNode.isResource()) {
						elementValueNode = Optional.of(parseRDFEntityToJson(valueNode.asResource(), jenaModel, mapper));
					}
				}
				if (i.isPresent() && elementValueNode.isPresent()) {
					arrayElements.put(i.get(), elementValueNode.get());
				}
			}
		}

		Iterator<java.util.Map.Entry<Integer, JsonNode>> it = arrayElements.entrySet().iterator();
		while (it.hasNext()) {
			jsonArrayNode.add(it.next().getValue());
		}

		return jsonArrayNode;
	}

	/**
	 * Method that checks the given {@code entityResource} and parses all its attributes
	 * The name and the value of each attribute is retrieved and the given {@code entity} is
	 * appended with these values
	 * 
	 * @param entityResource : the RDF entity resource to be parsed
	 * @param entity : a JSON entity to be appended with the parsed values
	 * @param jenaModel : a JENA model
	 * @param mapper : an object mapper needed for the manipulation of the given JSON entity
	 */
	private void parseAttributesToJson(Resource entityResource, ObjectNode entity, Model jenaModel,
			ObjectMapper mapper) {
			StmtIterator x = jenaModel.listStatements();
			Map<String, List<RDFNode>> map = new HashMap<String, List<RDFNode>>();
			Set<String> types = new HashSet<String>();
			String typePropertyLabel = null;
			while (x.hasNext()) {
				Statement st = x.next();
				if (st.getSubject().asResource().equals(entityResource)) {
					Property property = st.getPredicate();
					String propertyLocalName = property.getLocalName();
					if (propertyLocalName.equals("type")) {
						Set<String> resourceTypes = OntologyHandler.gi().findResourceType(st.getObject().asResource().getURI());
						types.addAll(resourceTypes);
						typePropertyLabel = "rt";
					}
					if (!propertyLocalName.equals("type") && !propertyLocalName.equals("IsHostedBy")) {
						List<RDFNode> nodes = map.get(property.getURI());
						if (nodes == null) {
							nodes = new ArrayList<RDFNode>();
						}
						nodes.add(st.getObject());
						map.put(property.getURI(), nodes);
					}
				}
			}
			
			if (!types.isEmpty()) {
				ArrayNode node = mapper.getNodeFactory().arrayNode();
				entity.set(typePropertyLabel, node);
				for (String type : types) {
						node.add(type);
				}
			}
			
			for (Entry<String, List<RDFNode>> entry : map.entrySet()) {
				List<RDFNode> list = entry.getValue();
				if (list.size() > 1) {
					ArrayNode arrayNode = mapper.getNodeFactory().arrayNode();
					String key = OntologyHandler.gi().findLabelOfProperty(entry.getKey());				
					entity.set(key, arrayNode);
					for (RDFNode node : list) {
						if (node.isLiteral()) {
							ValueNode jsonValueNode = parseLiteralToValueNode(node.asLiteral(), mapper);
							arrayNode.add(jsonValueNode);
						}
						else if (node.isResource()) {
							ObjectNode jsonNode = mapper.createObjectNode();
							arrayNode.add(jsonNode);
							parseAttributesToJson(node.asResource(), jsonNode, jenaModel, mapper);
						}
					}
				}
				else {
					RDFNode node = list.get(0);
					if (node.isLiteral()) {
						ValueNode jsonValueNode = parseLiteralToValueNode(node.asLiteral(), mapper);
						String key = OntologyHandler.gi().findLabelOfProperty(entry.getKey());		
						entity.set(key, jsonValueNode);
					}
					else if (node.isResource()) {
						ObjectNode jsonNode = mapper.createObjectNode();
						String key = OntologyHandler.gi().findLabelOfProperty(entry.getKey());		
						entity.set(key, jsonNode);
						parseAttributesToJson(node.asResource(), jsonNode, jenaModel, mapper);
					}
				}
			}
	}

	/**
	 * Method that checks the type of the given {@code literal} and creates the 
	 * equivalent JSON value node
	 * 
	 * @param literal : the literal to be parsed
	 * @param mapper : an object mapper needed for the creation of JSON value node
	 * @return {@code ValueNode} instance
	 */
	private ValueNode parseLiteralToValueNode(Literal literal, ObjectMapper mapper) {
		RDFDatatype datatype = literal.getDatatype();
		if (datatype != null) {
			if (datatype.equals(XSDDatatype.XSDboolean)) {
				return mapper.getNodeFactory().booleanNode(literal.getBoolean());
			}

			if (datatype.equals(XSDDatatype.XSDint) || datatype.equals(XSDDatatype.XSDinteger)) {
				return mapper.getNodeFactory().numberNode(literal.getInt());
			}

			if (datatype.equals(XSDDatatype.XSDlong)) {
				return mapper.getNodeFactory().numberNode(literal.getLong());
			}

			if (datatype.equals(XSDDatatype.XSDfloat)) {
				return mapper.getNodeFactory().numberNode(literal.getFloat());
			}

			if (datatype.equals(XSDDatatype.XSDdouble)) {
				return mapper.getNodeFactory().numberNode(literal.getDouble());
			}
			return mapper.getNodeFactory().textNode(literal.getString());
		}
		return mapper.getNodeFactory().textNode(literal.getValue().toString());
	}

	/**
	 * Method used for parsing a JSON object (usually representing an IoTivity device) and creating
	 * the equivalent JENA resource
	 * 
	 * @param objectResource : the JENA resource to be appended with the  and values
	 * @param objectNode : the JSON node to be parsed
	 * @param jenaModel : the JENA model used
	 */
	private void parseJSONObjectToJena(Resource objectResource, JsonNode objectNode, Model jenaModel, String uri) {
		if (objectNode.has(OntologyHandler.RESOURCE_TYPE)) {
			JsonNode resourceTypeNode =  objectNode.get(OntologyHandler.RESOURCE_TYPE);
			Set<String> resourceTypes = new HashSet<String>();
			for (int i = 0; i < resourceTypeNode.size(); i++) {
				resourceTypes.add(resourceTypeNode.get(i).asText());
			}
			if (OntologyHandler.gi().isDevice(resourceTypes)) {
				try {
					for (String rt : resourceTypes) {
						Device d = OntologyHandler.gi().findDeviceByResourceType(rt);
						uri = d.getUri();
						objectResource.addProperty(RDF.type, uri);
					}
					objectResource.addProperty(RDF.type, IoTivityUtils.AiotesEntityTypeDevice);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if (eu.interiot.intermw.ontology.entities.Resource.isResource(resourceTypes)) {
				try {
					eu.interiot.intermw.ontology.entities.Resource r = OntologyHandler.gi().findResourceByResourceTypes(resourceTypes);
					uri = r.getUri();
					objectResource.addProperty(RDF.type, jenaModel.createResource(r.getUri()));
					objectResource.addProperty(RDF.type, jenaModel.createResource("http://inter-iot.eu/syntax/Iotivity.owl#Resource"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		Iterator<Map.Entry<String, JsonNode>> it = objectNode.fields();
		while (it.hasNext()) {
			Map.Entry<String, JsonNode> field = it.next();
			if (field.getKey().equals(OntologyHandler.RESOURCE_TYPE) || field.getKey().equals("if") || field.getKey().equals("id") || field.getKey().equals("href")) {
				continue;
			}
			JsonNode json = field.getValue();
			if (field.getKey().equals("rep")) {

				parseJSONObjectToJena(objectResource, json, jenaModel, uri);
			}
			else {

				String propertyUri = null;
				if (uri != null) {
					try {
						propertyUri = OntologyHandler.gi().findPropertyOfResourceByLabel(uri, field.getKey());
					} catch (Exception e) {
					}
				}
				if (propertyUri == null) {
					propertyUri = iotivityBaseURI + field.getKey();
				}			
				Property property = jenaModel.createProperty(propertyUri);
				parseJSONPropertyToJena(objectResource, field.getValue(), property, jenaModel, uri);
			}
		}
	}
	
	/**
	 * Method used for parsing a JSON field (usually representing an IoTivity property) and appending
	 * the equivalent JENA resource
	 * 
	 * @param objectResource : the JENA resource to be appended with the {@code property} and the {@code value}
	 * @param value : the value of the property
	 * @param property : the proeprty
	 * @param jenaModel : the JENA model used
	 */
	private void parseJSONPropertyToJena(Resource objectResource, JsonNode value, Property property, Model jenaModel, String uri) {
		if (value.isArray()) {
			for (JsonNode element : value) {
				parseJSONPropertyToJena(objectResource, element, property, jenaModel, uri);
			}
		}
		else if (value.isValueNode()) {
			parseLiteral(objectResource, property, value);		
		}
		else {
			Resource measurementResource = jenaModel.createResource();
			objectResource.addProperty(property, measurementResource);
			parseJSONObjectToJena(measurementResource, value,jenaModel, uri);
		}
	}

	
	/**
	 * Checks the type of the given value (that is literal) and updates accordingly the given resource
	 * @param objectResource : the JENA resource to be appended
	 * @param property : the property
	 * @param value: the value
	 */
	private void parseLiteral(Resource objectResource, Property property, JsonNode value) {
		if (value.canConvertToInt()) {
			objectResource.addLiteral(property, value.asInt());
		}
		else if (value.isLong()) {
			objectResource.addLiteral(property, value.asLong());
		}
		else if (value.isBoolean()) {
			objectResource.addLiteral(property, value.asBoolean());
		}
		else if (value.isTextual()) {
			objectResource.addLiteral(property, value.asText());
		}
		else if (value.isDouble()) {
			objectResource.addLiteral(property, value.asDouble());
		}
	}
	

	/**
	 * Method that parses the given {@code jsonNode}, identifies the type of its content
	 * and appends the given resource {@code res} with the equivalent values by using the 
	 * {@value #hasValue} property
	 * 
	 * @param res : the JENA resource to be appended with values
	 * @param jsonNode : the JSON node to be parsed
	 * @param jenaModel : the JENA model used
	 */
	private void parseValueToJena(Resource res, JsonNode jsonNode, Model jenaModel) {
		if (jsonNode.isValueNode()) {
			if (jsonNode.isNumber()) {
				if (jsonNode.isInt()) {
					res.addLiteral(hasValue, new Integer(jsonNode.asInt()));
				} else if (jsonNode.isLong()) {
					res.addLiteral(hasValue, jsonNode.asLong());
				} else if (jsonNode.isFloat() || jsonNode.isFloatingPointNumber()) {
					res.addLiteral(hasValue, new Float(jsonNode.asDouble()));
				} else {
					res.addLiteral(hasValue, jsonNode.asDouble());
				}
			} else if (jsonNode.isBoolean()) {
				res.addLiteral(hasValue, jsonNode.asBoolean());
			} else if (jsonNode.isTextual()) {
				res.addLiteral(hasValue, jsonNode.asText());
			} else {
				res.addProperty(hasValue, jsonNode.asText());
			}
		} else if (jsonNode.isArray()) {
			Resource arrayResource = jenaModel.createResource();
			arrayResource.addProperty(RDF.type, arrayType);
			res.addProperty(hasValue, arrayResource);
			parseArrayToJena(arrayResource, jsonNode, jenaModel);
		} else if (jsonNode.isObject()) {
			Resource objectResource;
			if (jsonNode.has("id")) {
				objectResource = jenaModel.createResource(instanceType +"/" + jsonNode.get("id").asText());
			}
			else {
				objectResource = jenaModel.createResource();
			}
			res.addProperty(hasValue, objectResource);
			parseJSONObjectToJena(objectResource, jsonNode, jenaModel, null);
		}
	}

	/**
	 * Method that is used in order to parse a {@code JsonNode} that is an array For
	 * each element of the array a {@code Resource} instance is created and is added
	 * to the given JENA model
	 * 
	 * @param arrayResource
	 * @param arrayNode
	 * @param jenaModel
	 */
	private void parseArrayToJena(Resource arrayResource, JsonNode arrayNode, Model jenaModel) {
		int counter = 1;
		Iterator<JsonNode> arrayIt = arrayNode.elements();
		while (arrayIt.hasNext()) {
			JsonNode element = arrayIt.next();
			Resource jenaElement = jenaModel.createResource();
			jenaElement.addProperty(RDF.type, elementType);
			arrayResource.addProperty(hasElement, jenaElement);
			jenaElement.addLiteral(hasNumber, new Integer(counter++));
			parseValueToJena(jenaElement, element, jenaModel);
		}
	}

	/**
	 * Method that executes the given {@code sparqlQuery} to the given {@code model}
	 * 
	 * @param model: a JENA model
	 * @param sparqlQuery: a SELECT SPARQL query to be executed
	 * @return the results of the SELECT query
	 */
	private ResultSet executeSelectToJenaModel(Model model, String sparqlQuery) {
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		return queryExecution.execSelect();
	}
	
	/**
	 * Uses the given {@code model} to find all types of the given {@code resource}
	 * 
	 * @param model : a JENA model
	 * @param resource : the resource we want to find its types
	 * @return a {@code StmtIterator} instance
	 */
	private StmtIterator getTypesOfResource(Model model, Resource resource) {
		return model.listStatements(new SimpleSelector(resource, RDF.type, (RDFNode) null));
	}
	
	/**
	 * Uses the given {@code model} to find all property values of the given {@code resource}
	 * 
	 * @param model : a JENA model
	 * @param resource : the resource we want to find its types
	 * @param property : the property we want to retrieve its values
	 * @return a {@code StmtIterator} instance
	 */
	private StmtIterator getPropertyValuesOfResource(Model model, Resource resource, Property property) {
		return model.listStatements(new SimpleSelector(resource, property, (RDFNode) null));
	}
}
