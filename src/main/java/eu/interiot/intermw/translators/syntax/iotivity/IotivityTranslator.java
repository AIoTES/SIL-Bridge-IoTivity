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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
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
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
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
	private Resource attributeType;
	private Resource elementType;
	private Resource deviceType;

	private Property hasAttribute;
	private Property hasName;
	private Property hasValue;
	private Property hasElement;
	private Property hasNumber;
	
	private Property deviceHasName;
	private Property hasLocation;
	
	private Property hasDiastolic;
	private Property hasSystolic;
	private Property hasPulse;
	private Property hasGlucose;


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
		attributeType = jenaModel.createResource(getBaseURI() + "Attribute");
		elementType = jenaModel.createResource(getBaseURI() + "ArrayElement");
		deviceType = jenaModel.createResource(IoTivityUtils.EntityTypeDevice);

		hasAttribute = jenaModel.createProperty(getBaseURI() + "hasAttribute");
		hasName = jenaModel.createProperty(getBaseURI() + "hasName");
		hasValue = jenaModel.createProperty(getBaseURI() + "hasValue");
		hasElement = jenaModel.createProperty(getBaseURI() + "hasElement");
		hasNumber = jenaModel.createProperty(getBaseURI() + "hasNumber");
		deviceHasName = jenaModel.createProperty(interIoT + "GOIoTP#hasName");
		hasLocation = jenaModel.createProperty(interIoT + "GOIoTP#hasLocation");
		
		hasDiastolic = jenaModel.createProperty(iotivityBaseURI + "hasDiastolic");
		hasSystolic = jenaModel.createProperty(iotivityBaseURI + "hasSystolic");
		hasPulse = jenaModel.createProperty(iotivityBaseURI + "hasPulse");
		hasGlucose = jenaModel.createProperty(iotivityBaseURI + "hasGlucose");
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

		// Find the top-level RDF Entity (the entity that does not appear in Object of
		// RDF triples)
		final String topLevelQuery = "SELECT DISTINCT ?top WHERE { ?top ?y ?z MINUS {?a ?b ?top} }";
		ResultSet results = executeSelectToJenaModel(jenaModel, topLevelQuery);

		while (results.hasNext()) {
			RDFNode resultNode = results.next().get("top");
			StmtIterator typesIt = getTypesOfResource(jenaModel, resultNode.asResource());
			Set<Resource> types = typesIt.toSet().stream().map(x -> x.getObject().asResource()).collect(Collectors.toSet());
			if (types.contains(deviceType)) {
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
			Resource myEntity = jenaModel.createResource(instanceType +"/" + topLevelNode.get("id").asText());
			myEntity.addProperty(RDF.type, interIoT+"GOIoTP#IoTDevice");
			parseJSONObjectToJena(myEntity, topLevelNode, jenaModel);
		} else if (topLevelNode.isArray()) {
			Resource arrayResource = jenaModel.createResource();
			arrayResource.addProperty(RDF.type, arrayType);
			parseArrayToJena(arrayResource, topLevelNode, jenaModel);
		} else if (topLevelNode.isValueNode()) {
			Resource valueResource = jenaModel.createResource();
			valueResource.addProperty(RDF.type, valueType);
			parseValueToJena(valueResource, topLevelNode, jenaModel);
		}
		// jenaModel.write(System.out, "TTL") ;
		return jenaModel;
	}
	
	/**
	 * Method that parses an RDF entity and creates the equivalent JSON node
	 * 
	 * @param entityResource : the RDF entity to be parsed
	 * @param jenaModel: the JENA model
	 * @param mapper : an object mapper for the creation of the JSON node
	 * @return a {@code JsonNode} instance
	 */
	private JsonNode parseRDFEntityToJson(Resource entityResource, Model jenaModel, ObjectMapper mapper) {
		StmtIterator typesIt = getTypesOfResource(jenaModel, entityResource);
		while (typesIt.hasNext()) {
			Resource type = typesIt.next().getObject().asResource();
			if (type.equals(instanceType) || type.equals(deviceType)) {
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
	 */
	private ArrayNode parseArrayEntityToJsonArray(Resource arrayResource, Model jenaModel, ObjectMapper mapper) {
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
		NodeIterator it = jenaModel.listObjectsOfProperty(entityResource, hasAttribute);
		if (it.hasNext()) {
			while (it.hasNext()) {
				RDFNode attribute = it.next();
				if (attribute.isResource() && attribute.asResource().hasProperty(RDF.type, attributeType)) {
	
					String attributeName = "";
					NodeIterator names = jenaModel.listObjectsOfProperty(attribute.asResource(), hasName);
					if (names.hasNext()) {
						attributeName = names.next().toString();
					}
					if (attributeName.isEmpty())
						continue;
	
					NodeIterator valIterator = jenaModel.listObjectsOfProperty(attribute.asResource(), hasValue);
					if (valIterator.hasNext()) {
						RDFNode valueNode = valIterator.next();
						if (valueNode.isLiteral()) {
							ValueNode jsonValueNode = parseLiteralToValueNode(valueNode.asLiteral(), mapper);
							entity.set(attributeName, jsonValueNode);
						}
					} else {
						ObjectNode attributeNode = mapper.createObjectNode();
						entity.set(attributeName, attributeNode);
						parseAttributesToJson(attribute.asResource(), attributeNode, jenaModel, mapper);
					}
				}
			}
		}
		else {
			parseLiteral(entityResource, jenaModel, deviceHasName, entity, mapper, "name");
			parseLiteral(entityResource, jenaModel, hasDiastolic, entity, mapper, "diastolic");
			parseLiteral(entityResource, jenaModel, hasSystolic, entity, mapper, "systolic");
			parseLiteral(entityResource, jenaModel, hasPulse, entity, mapper, "pulse");
			parseLiteral(entityResource, jenaModel, hasGlucose, entity, mapper, "glucose");
			parseResource(entityResource, jenaModel, hasLocation, entity, mapper, "location");
			
			//add default values
			entity.set("type", mapper.getNodeFactory().textNode("bloodpressure"));
			entity.set("model", mapper.getNodeFactory().textNode("Duo ultima"));
			entity.set("MAC", mapper.getNodeFactory().textNode("00:12:A1:B0:78:14"));
			entity.set("password", mapper.getNodeFactory().textNode("111111"));
			entity.set("manufacturer", mapper.getNodeFactory().textNode("Fibaro"));
		}
	}
	
	/**
	 * 
	 * @param entityResource : the RDF entity resource to be parsed
	 * @param jenaModel: a JENA model
	 * @param p : the property we want to parse
	 * @param entity : a JSON entity to be appended with the parsed values
	 * @param mapper : an object mapper needed for the manipulation of the given JSON entity
	 * @param fieldName : the name that will ne used as json field 
	 */
	private void parseResource(Resource entityResource, Model jenaModel, Property p, ObjectNode entity, ObjectMapper mapper, String fieldName) {
		NodeIterator it = jenaModel.listObjectsOfProperty(entityResource, p);
		while (it.hasNext()) {
			RDFNode attribute = it.next();
			if (attribute.isResource()) {
				ValueNode jsonValueNode = mapper.getNodeFactory().textNode(attribute.asResource().toString());
				entity.set(fieldName, jsonValueNode);
			}
		}
	}
	
	/**
	 * 
	 * @param entityResource : the RDF entity resource to be parsed
	 * @param jenaModel: a JENA model
	 * @param p : the property we want to parse
	 * @param entity : a JSON entity to be appended with the parsed values
	 * @param mapper : an object mapper needed for the manipulation of the given JSON entity
	 * @param fieldName : the name that will ne used as json field 
	 */
	private void parseLiteral(Resource entityResource, Model jenaModel, Property p, ObjectNode entity, ObjectMapper mapper, String fieldName) {
		NodeIterator it = jenaModel.listObjectsOfProperty(entityResource, p);
		while (it.hasNext()) {
			RDFNode attribute = it.next();
			if (attribute.isLiteral()) {
				ValueNode jsonValueNode = parseLiteralToValueNode(attribute.asLiteral(), mapper);
				entity.set(fieldName, jsonValueNode);
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
	 * Method used for parsing a JSON object (usually representing an IoTivity resource) and creating
	 * the equivalent JENA resource
	 * 
	 * @param objectResource : the JENA resource to be appended with properties and values
	 * @param objectNode : the JSON node to be parsed
	 * @param jenaModel : the JENA model used
	 */
	private void parseJSONObjectToJena(Resource objectResource, JsonNode objectNode, Model jenaModel) {
		Iterator<Map.Entry<String, JsonNode>> it = objectNode.fields();
		while (it.hasNext()) {
			Map.Entry<String, JsonNode> field = it.next();
			if (field.getKey().equals("diastolic")) {
				objectResource.addLiteral(hasDiastolic, field.getValue().asInt());
			}
			else if (field.getKey().equals("glucose")) {
				objectResource.addLiteral(hasGlucose, field.getValue().asInt());
			}
			else if (field.getKey().equals("systolic")) {
				objectResource.addLiteral(hasSystolic, field.getValue().asInt());
			}
			else if (field.getKey().equals("pulse")) {
				objectResource.addLiteral(hasPulse, field.getValue().asInt());
			}
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
			Resource objectResource = jenaModel.createResource(instanceType +"/" + jsonNode.get("id").asText());
			objectResource.addProperty(RDF.type, interIoT+"GOIoTP#IoTDevice");
			res.addProperty(hasValue, objectResource);
			parseJSONObjectToJena(objectResource, jsonNode, jenaModel);
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
