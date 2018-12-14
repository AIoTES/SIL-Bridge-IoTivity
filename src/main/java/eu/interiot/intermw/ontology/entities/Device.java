package eu.interiot.intermw.ontology.entities;


public class Device extends OCFObject{
	
	private static final String RT_PREFIX = "oic.d";
	private static final String RT = "oic.wk.d";

	public Device(String uri) {
		super(uri);
	}

	@Override
	public String toString() {
		return "Device [resourceType=" + resourceType + ", uri=" + uri + "]";
	}
	
	public static boolean isDevice(String resourceType) {
		if (resourceType == null) return false;
		if (resourceType.equals(RT) || resourceType.startsWith(RT_PREFIX)) {
			return true;
		}
		return false;
	}
}
