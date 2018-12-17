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
package eu.interiot.intermw.bridge.iotivity.client.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.builder.MapBuilder;
import co.nstant.in.cbor.model.DataItem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * 
 * Class that contains utility methods for manipulating data in CBOR @ format
 * CBOR format is used by the COAP protocol and the implemented methods of this class
 * provide encoding/decoding functionalities
 *
 */
public class EncodingUtils {
	
	/**
	 * The payload of a COAP response is in CBOR format and needs to get parsed accordingly in order
	 * to create a more friendly JSON representation
	 * 
	 * @param payload : the bytes of the payload of a COAP response
	 * @return : the bytes received transformed to JsonElement
	 * @throws IOException
	 */
	public static JsonElement coapResponseToJson(byte[] payload) throws IOException{
		CBORFactory f = new CBORFactory();
		CBORParser parser = f.createParser(payload);
		ObjectMapper mapper = new ObjectMapper(f);
		JsonParser jsonParser = new JsonParser();
		try {
			List<Map<String, Object>> list = mapper.readValue(parser, new TypeReference<List<Map<String, Object>>>() { });
			parser.close();	
			try {
				return jsonParser.parse(new Gson().toJson(list)).getAsJsonObject();
			}
			catch (Exception e){
				return jsonParser.parse(new Gson().toJson(list)).getAsJsonArray();
			}		
		}
		catch(Exception ex){
			Map<String, Object> map = mapper.readValue(parser, new TypeReference<Map<String, Object>>() { });
			parser.close();	
			return jsonParser.parse(new Gson().toJson(map)).getAsJsonObject();
		}	
	}
	
	/**
	 * Method that encodes the given {@code map} of key - values of a resource to a CBOR and returns
	 * the equivalent bytes
	 * 
	 * @param map :  a {@code Map} that contains key and values of a resource
	 * @return a byte array
	 * @throws Exception
	 */
	public static byte[] encodeResourceToCbor(Map<String, Object> map) throws Exception{
		List<DataItem> resource = createDataItemList(map);
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		CborEncoder encoder = new CborEncoder(byteOutputStream);
		encoder.encode(resource);
		return byteOutputStream.toByteArray();
	}

	/**
	 * Method that uses the entries of a given {@code map} to create the equivalent CBOR data items
	 * 
	 * @param map : a {@code Map} that contains key and values of a resource
	 * @return : a {@code List} of CBOR {@code DataItem} objects
	 * @throws Exception
	 */
	private static List<DataItem> createDataItemList(Map<String, Object> map) throws Exception{
		MapBuilder<CborBuilder> mapBuilder = new CborBuilder().startMap();
		for (Entry<String, Object> entry : map.entrySet()){
			mapBuilder = addEntryToMapBuilder(entry, mapBuilder);
		}
		return mapBuilder.end().build();
	}
	
	/**
	 * 
	 * @param entry
	 * @param mapBuilder
	 * @return
	 * @throws Exception
	 */
	private static MapBuilder<CborBuilder> addEntryToMapBuilder(Entry<String, Object> entry, MapBuilder<CborBuilder> mapBuilder) throws Exception{
		try { return mapBuilder.put(entry.getKey(), (boolean) entry.getValue());} catch (Exception e){};
		try { return mapBuilder.put(entry.getKey(), (byte[]) entry.getValue());} catch (Exception e){};
		try { return mapBuilder.put(entry.getKey(), (double) entry.getValue());} catch (Exception e){};
		try { return mapBuilder.put(entry.getKey(), (float) entry.getValue());} catch (Exception e){};
		try { return mapBuilder.put(entry.getKey(), (long) entry.getValue());} catch (Exception e){};
		try { return mapBuilder.put(entry.getKey(), (String) entry.getValue());} catch (Exception e){};
		try { return mapBuilder.put(entry.getKey(), (int) entry.getValue());} catch (Exception e){};
		if (entry.getValue() instanceof ArrayList) {
			ArrayBuilder<MapBuilder<CborBuilder>> array = mapBuilder.putArray(entry.getKey());
			ArrayList list = (ArrayList) entry.getValue();
			Iterator it = list.iterator();
			while (it.hasNext()) {
				Object obj = it.next();
				if (obj instanceof LinkedHashMap) {
					Map listElement = (LinkedHashMap) obj;
					List<DataItem> dataItems = createDataItemList(listElement);
					for (DataItem d : dataItems) {
						array.add(d);
					}
				}
				else {
					array.add((String) obj);
				}
			}
			return mapBuilder;
		}
		if (entry.getValue() instanceof LinkedHashMap) {
			handleMap(mapBuilder, entry.getValue(), entry.getKey());
			return mapBuilder;
		}
		throw new Exception("Unsupported type of the value of key: " + entry.getKey() + " - " + entry.getValue());	
	}
	
	private static void handleMap(MapBuilder<CborBuilder> mapBuilder, Object obj, String entryKey) throws Exception{
		Map listElement = (LinkedHashMap) obj;
		List<DataItem> dataItems = createDataItemList(listElement);
		ArrayBuilder<MapBuilder<CborBuilder>> array = mapBuilder.putArray(entryKey);
		for (DataItem d : dataItems) {
			array.add(d);
		}
	}
	
	/**
	 * Method for printing bytes encoded to CBOR
	 * 
	 * @see <a href="https://github.com/c-rack/cbor-java">lib used</a>
	 * @param bytes : the bytes to be printed
	 * @throws CborException
	 */
	public static void printCborEncodedBytes(byte[] bytes) throws CborException{
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		List<DataItem> dataItems = new CborDecoder(bais).decode();
		for(DataItem dataItem : dataItems) {
		    System.out.println(dataItem);
		}
	}
}
