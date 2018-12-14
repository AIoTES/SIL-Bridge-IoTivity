package eu.interiot.intermw.bridge.testing.module.coapclient;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import eu.interiot.intermw.bridge.iotivity.client.IoTivityClient;
import eu.interiot.intermw.bridge.iotivity.client.impls.IoTivityCoapClientImpl;

@FixMethodOrder
public class CoapClientTest {
	
	private IoTivityClient iotivityClient = null;
	private Map<String, Object> map;
	
	private static final String SERVER_IP = "160.40.48.65";
	private static final String PROXY_IP = "160.40.49.115";
	private static final String SERVER_PORT = "50014";
	
	private static final String RESOURCE_HREF = "/ocf/dimming";
	
	private static final String TEST_FOLDER =  System.getProperty("user.dir") + "/src/test/resources/coapClientTest/";
	private static final String INPUT_FOLDER = TEST_FOLDER + "inputs/";
	private static final String EXPECTED_OUTCOME_FOLDER = TEST_FOLDER + "expectedOutcomes/";
	
    @Before
    public void setUp() throws Exception {
    	iotivityClient = new IoTivityCoapClientImpl(SERVER_IP, PROXY_IP, SERVER_PORT);
    	map = new HashMap<String, Object>();
    }
    
    @Test
    public void testDiscover() throws Exception {
    	//DISCOVER
    	JsonElement discoverResult = iotivityClient.discoverServer();
    	JsonElement expectedOutcome = new JsonParser().parse(readFile(EXPECTED_OUTCOME_FOLDER + "discover"));
    	assertEquals(discoverResult, expectedOutcome);
    }
    
    @Test
    public void testGet() throws Exception {
    	//GET
    	iotivityClient.discoverServer();
    	JsonElement response = iotivityClient.getResource(RESOURCE_HREF);
    	JsonElement expectedOutcome = new JsonParser().parse(readFile(EXPECTED_OUTCOME_FOLDER + "getResource"));
    	assertEquals(response, expectedOutcome);
    }
    
    
    @Test
    public void testPut() throws Exception { 	
    	//PUT
    	iotivityClient.discoverServer();
    	JsonObject input = new JsonParser().parse(readFile(INPUT_FOLDER + "putResource")).getAsJsonObject();
    	map = new HashMap<String, Object>();
    	for (String key : input.keySet()) {
    		map.put(key, input.get(key).getAsInt());
    	}
    	iotivityClient.editResource(map, RESOURCE_HREF);
    	JsonObject response = iotivityClient.getResource(RESOURCE_HREF).getAsJsonObject();
    	JsonObject valueResponse = new JsonObject();
    	for (String key : response.keySet()) {
    		if (key.equals("if") || key.equals("rt")) continue;
    		valueResponse.addProperty(key, response.get(key).getAsInt());
    	}
    	assertEquals(valueResponse, input);
    }
    
    @Test
    public void testPost() throws Exception { 	
    	//POST
    	testPut();
    }
    
    @Test
    public void testDelete() throws Exception { 	
    	//DELETE
    	testPut();
    }
    
    @Test
    public void testQuery() throws Exception { 	
    	//QUERY
    	testPut();
    }
    
    @Test
    public void testObserve() throws Exception { 	
    	//OBSERVE
    	testPut();
    	testPut();
    	testPut();
    }

//    	response = iotivityClient.getResource(RESOURCE_HREF);
//    	assertTrue(randomValue == response.getAsJsonObject().get(RESOURCE_ATTIBUTE).getAsInt());
//    	
//    	iotivityClient.deleteResource(RESOURCE_HREF);
//    	iotivityClient.getResource(RESOURCE_HREF);
    	
//    	map = new HashMap<String, Object>();
//    	randomValue = random.nextInt(100 + 1 - 0) + 0;
//    	map.put(RESOURCE_ATTIBUTE, randomValue);
//    	iotivityClient.createResource(map, RESOURCE_HREF);
//    	JsonElement response = iotivityClient.getResource(RESOURCE_HREF);
//    	assertTrue(randomValue == response.getAsJsonObject().get(RESOURCE_ATTIBUTE).getAsInt());
    
    
    private String readFile(String fileName) throws IOException {
    	BufferedReader br = new BufferedReader(new FileReader(fileName));
    	try {
    	    StringBuilder sb = new StringBuilder();
    	    String line = br.readLine();

    	    while (line != null) {
    	        sb.append(line);
    	        sb.append(System.lineSeparator());
    	        line = br.readLine();
    	    }
    	    String everything = sb.toString();
    	    return everything;
    	} finally {
    	    br.close();
    	}
    }

}
