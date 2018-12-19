package eu.interiot.intermw.ontology;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;

import eu.interiot.intermw.ontology.entities.Device;
import eu.interiot.intermw.ontology.entities.Resource;
/**
 * 
 * @author stavrotheodoros
 *
 * Method for retreiving information from the Ontology related to the IoTivity platform
 * 
 */
public class OntologyHandler {
	
	private OntModel model;
	//private static final String ONTOLOGY_FILENAME = "./src/main/resources/ontology/iotivity data model.owl";
	private static final String ONTOLOGY_FILENAME = "ontology/iotivity_data_model.owl";
	public static final String IOTIVITY_PREFIX = "http://inter-iot.eu/syntax/Iotivity.owl#";
	public static final String RESOURCE_TYPE = "rt";
	private static OntologyHandler instance = new OntologyHandler();
	
	private Set<String> deviceResourceTypes = new HashSet<String>();

	
	private OntologyHandler() {
		try {
			loadOntology();
			initializeStructures();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static OntologyHandler gi() {
		return instance;
	}
	
	/**
	 * Load the Ontology located in the resource/ontology folder
	 * @throws Exception
	 */
	public void loadOntology() throws Exception {
		System.out.println("LOAD ONTOLOGY");
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File file = new File(classLoader.getResource(ONTOLOGY_FILENAME).getFile());
		//File file = new File(ONTOLOGY_FILENAME);
		readOntologyModel(file);
	}
	
	private void initializeStructures() {
		getAllDeviceResourceTypes();
	}
	
	private void getAllDeviceResourceTypes(){
		try {
			Set<OntClass> allDevices = getAllDevices();
			for (OntClass cl : allDevices) {
				deviceResourceTypes.addAll(getResourceTypes(cl));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Set<String> getAllDeviceUris() throws Exception{
		Set<String> set = new HashSet<String>();
		Set<OntClass> allDevices = getAllDevices();
		for (OntClass cl : allDevices) {
			set.add(cl.getURI());
		}
		return set;
	}
	
	private Set<OntClass> getAllDevices() throws Exception{
		Set<OntClass> set = new HashSet<OntClass>();
		OntClass d = findByResourceType("oic.wk.d");
		set.add(d);
		ExtendedIterator<OntClass> it = d.listSubClasses();
		while (it.hasNext()) {
			set.add(it.next());
		}
		return set;
	}
	
	/**
	 * loads the ontology from the file
	 * 
	 * @throws IOException
	 */
	private void readOntologyModel(File file) throws Exception {
		if (file == null) {
			throw new Exception("File null");
		}
		model = ModelFactory.createOntologyModel();
		InputStream in = new FileInputStream(file);
		if (file.getCanonicalPath().endsWith(".owl")) {
			model = (OntModel) model.read(in, null, "RDF/XML");
		}
		else {
			model = (OntModel) model.read(in, null, "");
		}
	}
	
	/**
	 * Parses all ontology classes and finds the one with the given resource type
	 * @throws Exception in case no class was found
	 */
	public OntClass findByResourceType(String rt) throws Exception {
		ExtendedIterator<OntClass> it = model.listClasses();
		while (it.hasNext()) {
			OntClass cl = it.next();
			Set<String> resourceType = getResourceTypes(cl);
			if (resourceType != null && resourceType.contains(rt)) {
					return cl;
			}
		}
		throw new Exception("No class was found with rt=" + rt);
	}
	
	/**
	 * Parses all ontology classes and finds the one with the given resource type
	 * @throws Exception in case no class was found
	 */
	public OntClass findByResourceTypes(Set<String> resourceTypes) throws Exception {
		if (resourceTypes.size() > 1) {
			resourceTypes.remove("oic.wk.d");
		}
		return findByResourceType(resourceTypes.iterator().next());		
	}
	
	public Device findDeviceByResourceType(String rt) throws Exception {
		return ontClassToDevice(findByResourceType(rt));
	}
	
	public Device findDeviceByResourceTypes(Set<String> rt) throws Exception {
		return ontClassToDevice(findByResourceTypes(rt));
	}
	
	public Resource findResourceByResourceType(String rt) throws Exception {
		return ontClassToResource(findByResourceType(rt));
	}
	
	public Resource findResourceByResourceTypes(Set<String> rt) throws Exception {
		return ontClassToResource(findByResourceTypes(rt));
	}
	
	/**
	 * Creates from the given ontology class the equivalent Device instance
	 * @throws Exception in case the resource type is wrong
	 */
	private Device ontClassToDevice(OntClass cl) throws Exception {
		Device d = new Device(cl.getURI());
		d.setResourceType(getResourceTypes(cl));
		return d;
	}
	
	/**
	 * Creates from the given ontology class the equivalent Resource instance
	 * @throws Exception in case the resource type is wrong
	 */
	private Resource ontClassToResource(OntClass cl) throws Exception {
		Resource r = new Resource(cl.getURI());
		r.setResourceType(getResourceTypes(cl));
		r.setComment(getComment(cl));
		r.setInterfaces(getInterfaces(cl));
		return r;
	}
	
	/**
	 * Finds and returns the property value of the comment property
	 * (http://www.w3.org/2000/01/rdf-schema#comment)
	 */
	private String getComment(OntClass cl) {
		return getStringDataPropertyOfClass(cl, "http://www.w3.org/2000/01/rdf-schema#comment");
	}
	
	/**
	 * Finds and returns the property value of the resource type property
	 * (http://inter-iot.eu/syntax/Iotivity.owl#rt)
	 */
	private Set<String> getResourceTypes(OntClass cl) {
		Set<String> resourceTypes = new HashSet<String>();
		StmtIterator it = cl.listProperties();
		while (it.hasNext()) {
			Statement st = it.next();
			Property p = st.getPredicate();
			if (p.getURI().equals( IOTIVITY_PREFIX + RESOURCE_TYPE)) {
				resourceTypes.add(st.getObject().asLiteral().getString());
			}
		}
		return resourceTypes;
	}
	
	/**
	 * Finds and returns all the property values of the interface property
	 * (http://inter-iot.eu/syntax/Iotivity.owl#if)
	 */
	private Set<String> getInterfaces(OntClass cl){
		Set<String> interfaces = new HashSet<String>();
		StmtIterator it = cl.listProperties();
		while (it.hasNext()) {
			Statement st = it.next();
			Property p = st.getPredicate();
			if (p.getURI().equals( IOTIVITY_PREFIX + "if")) {
				interfaces.add(st.getObject().asLiteral().getString());
			}
		}
		return interfaces;
	}
	
	/**
	 * Method that parses the given ontology class, and find the value of the given property
	 * @param cl : the ontology class
	 * @param propertyName : the name of the property (full name)
	 * @return
	 */
	private String getStringDataPropertyOfClass(OntClass cl, String propertyName) {
		Statement st = cl.getProperty(model.getProperty(propertyName));
		if (st != null) {
			return st.getObject().asLiteral().getString();
		}
		return null;
	}
	
	/**
	 * Method that lists all properties and finds those with the given label
	 * Then it checks the domain of the found properties
	 * If the given resource belongs to the domain then the property is returned
	 * @param resourceUri : the uri of a device or resource
	 * @param propertyLabel : the label of a property
	 * @return the uri of the property with the given label that has in its domain the given resource
	 * @throws Exception
	 */
	public String findPropertyOfResourceByLabel(String resourceUri, String propertyLabel) throws Exception {
		OntClass resource = model.getOntClass(resourceUri);
		ExtendedIterator<OntProperty> it = model.listAllOntProperties();
		while (it.hasNext()) {
			OntProperty p = it.next();
			try {
				String label = findLabelOfProperty(p);
				if (label.equals(propertyLabel)) {
					ExtendedIterator<? extends OntResource> d = p.listDomain();
					while (d.hasNext()) {
						OntResource r = d.next();
						if (r.getURI().equals(resourceUri) || isSubclass(resource, r.getURI())) {
							return p.getURI();
						}
					}
				}
			}
			catch (Exception e) {
				
			}
		}
		throw new Exception("The resource "+resourceUri+" has not a property with label="+propertyLabel);
	}
	
	private boolean isSubclass(OntClass child, String parentUri) {
		ExtendedIterator<OntClass> it = child.listSuperClasses();
		while (it.hasNext()) {
			OntClass superClass = it.next();
			if (superClass.getURI().equals(parentUri)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Method that finds and returns the label of the given property
	 * @param property
	 * @return
	 * @throws Exception
	 */
	public String findLabelOfProperty(Property property) throws Exception {
		StmtIterator it = property.listProperties();
		while (it.hasNext()) {
			Statement st = it.next();
			Property p = st.getPredicate();
			if (p.getURI().equals("http://www.w3.org/2000/01/rdf-schema#label")) {
				return st.getObject().asLiteral().getString();
			}
		}
		throw new Exception("The given property ("+property.getURI()+") has not a label");
	}
	
	public String findLabelOfProperty(String propertyUri) {
		Property p = model.getProperty(propertyUri);
		try {
			return findLabelOfProperty(p);
		}
		catch (Exception e) {};
		return p.getLocalName();
	}
	
	public Set<String> findResourceType(String typeUri){
		OntClass cl = model.getOntClass(typeUri);
		if (cl == null) return null;
		return getResourceTypes(cl);
	}
	
	public boolean isDevice(String resourceType) {
		return deviceResourceTypes.contains(resourceType);
	}
	
	public boolean isDevice(Set<String> resourceTypes) {
		Set<String> clone = new HashSet<String>(resourceTypes);
		clone.retainAll(deviceResourceTypes);
		return clone.size() > 0;
	}
	
}
