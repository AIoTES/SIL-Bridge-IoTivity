package eu.interiot.intermw.ontology.entities;

import java.util.Set;

public abstract class OCFObject extends OntologyClass{
	
	protected Set<String> resourceType;

	public OCFObject(String uri) {
		super(uri);
	}

	public Set<String> getResourceType() {
		return resourceType;
	}

	public void setResourceType(Set<String> resourceType) {
		this.resourceType = resourceType;
	}	
}
