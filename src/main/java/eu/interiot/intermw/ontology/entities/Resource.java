package eu.interiot.intermw.ontology.entities;

import java.util.Set;

public class Resource extends OCFObject{
	
	private static final String RT_PREFIX = "oic.r";

	
	protected String comment;
	protected Set<String> interfaces;
	
	public Resource(String uri) {
		super(uri);
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Set<String> getInterfaces() {
		return interfaces;
	}

	public void setInterfaces(Set<String> interfaces) {
		this.interfaces = interfaces;
	}

	public void addInterface(String interf) {
		interfaces.add(interf);
	}
	
	public void removeInterface(String interf) {
		interfaces.remove(interf);
	}
	
	public boolean hasInterface(String interf) {
		return interfaces.contains(interf);
	}
	
	public static boolean isResource(Set<String> resourceTypes) {
		if (resourceTypes == null || resourceTypes.isEmpty()) return false;
		for (String s : resourceTypes) {
			if (s.startsWith(RT_PREFIX)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "Resource [comment=" + comment + ", interfaces=" + interfaces + ", resourceType=" + resourceType
				+ ", uri=" + uri + "]";
	}

}
