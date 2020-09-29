# IoTivity Bridge

Bridge for IoTivity platform.

## Build bridge


To build the IoTivity bridge:
1. Download this repository
2. Change directory to downloaded repository folder
3. Build using maven. Skip all unitary tests in the compilation and use the jar obtained. For compiling the bridge you must use a maven version >= 3.6.0.

```
mvn clean package -DskipTests
```

## Bridge configuration 

To configure the SIL with the IoTivity bridge properly the following files must be added inside a mounted docker volume. Specifically, at first all needed jar files must be copied to the ``lib`` directory of the INTER-MW.
1. ``mw.bridge.iotivity-2.3.0-SNAPSHOT.jar``: This is the jar file acquired after building through maven the IoTivity bridge project, as described above.
2. ``syntactic-translators-1.0.jar``: This is one of the runtime dependencies needed by any bridge. It can be found in the following link: https://git.activageproject.eu/Bridges_Binaries/Libraries
3. element-connector-2.0.0-M9.jar: This is a local dependency of the IoTivity bridge, that can be found [libs/element-connector-2.0.0-M9.jar](libs/element-connector-2.0.0-M9.jar)
4. ``californium-core-2.0.0-M9.jar``: Another local dependency of the IoTivity bridge, that can be found in [libs/californium-core-2.0.0-M9.jar](libs/californium-core-2.0.0-M9.jar)

In addition, the bridge requires a configuration file named IoTivityBridge.properties, which must be copied to the INTER-MW configuration directory. An example of such a property file can be found in [src/main/resources/IoTivityBridge.properties](src/main/resources/IoTivityBridge.properties)

The property id must define the id of the IoTivity platform we intent to use, that is ``http://inter-iot.eu/platforms/IoTivity``. Moreover, the IP of the IoTivity instance must be defined through the property named ``iotivity.server.ip``. The following is an example of such a  property file:

```
platforms=demoA
demoA.id=http://inter-iot.eu/platforms/IoTivity
demoA.iotivity.server.ip=160.40.48.40
```

## Bridge deployment

In order to deploy the bridge to INTERMW docker container the syntactic translator, bridge jar and its depedencies and properties files should be copied to the ``lib`` directory. The following commands summarize the process.
```
docker cp mw.bridge.iotivity-2.3.0-SNAPSHOT.jar intermw-ipsm-deployment_intermw_1:/usr/local/tomcat/webapps/ROOT/WEB-INF/lib
docker cp syntactic-translators-1.0.jar intermw-ipsm-deployment_intermw_1:/usr/local/tomcat/webapps/ROOT/WEB-INF/lib
docker cp element-connector-2.0.0-M9.jar intermw-ipsm-deployment_intermw_1:/usr/local/tomcat/webapps/ROOT/WEB-INF/lib
docker cp californium-core-2.0.0-M9.jar intermw-ipsm-deployment_intermw_1:/usr/local/tomcat/webapps/ROOT/WEB-INF/lib

docker cp iotivity.properties intermw-ipsm-deployment_intermw_1:/etc/inter-iot/intermw
docker cp intermw.properties intermw-ipsm-deployment_intermw_1:/etc/inter-iot/intermw
```


### How to register an IoTivity platform instance?

Platforms are registered in the SIL using the POST /mw2mw/platforms operation. In the case of IoTivity, the parameters to be provided are:

- **platformId**: the id that will be assigned to the registered platform. It has to conform to the format "http://{DS_CODE}.inter-iot.eu/platforms/{id}", where DS_CODE is the acronym for the deployment site in Activage (e.g., 'dsgreece') and 'id' is an internal identifier for the platform (eg, 'IoTivity')
- **type**: this is the bridge type to use (http://inter-iot.eu/IoTivity) . This label can be obtained from /get platform-types or from the Table of platform types on the main/general guide. Check that the expected platform type is shown using GET platform-types in the API to confirm that the IoTivity bridge has been integrated correctly.
- **baseEndpoint**: it refers to IoTivity's address. It should be an URL (e.g., http://{iotivity_ipaddress})
- **location**: internal attribute used by Inter-Iot to give the geographic location of the platform. This field is optional, but in case it is provided, it has to be an URL.
- **name**: a label to identify the platform
- **downstreamXXX/upstreamXXX**: these fields are used to create the IPSM channels using the registered alignments. Use an empty string ("") to indicate that no alignment is used. 
 

Example JSON object:
```json
{
  "platformId": "http://dsgreece.inter-iot.eu/platforms/IoTivity",
  "type": "http://inter-iot.eu/IoTivity",
  "baseEndpoint": "160.40.48.40",
  "location": "http://dsgreece.inter-iot.eu/activage-iotivity-server",
  "name": "GR IoTivity production platform",
  "downstreamInputAlignmentName": "",
  "downstreamInputAlignmentVersion": "",
  "downstreamOutputAlignmentName": "",
  "downstreamOutputAlignmentVersion": "",
  "upstreamInputAlignmentName": "",
  "upstreamInputAlignmentVersion": "",
  "upstreamOutputAlignmentName": "",
  "upstreamOutputAlignmentVersion": ""
}
```
The REST API operation returns 202 (Accepted) response code. To make sure the process has executed successfully, check the response message and the logs.

Registering devices
The devices can be manually registered in the SIL. This can be done using the operation POST /mw2mw/devices. This operation also allows the creation of virtual devices.The convention used to generate device ids in IoTivity is the following:

"http://inter-iot.eu/syntax/Iotivity#Instance/{id}"

If the entity is given by the previous parameters already exists, then the following message is returned: "There is already a device with given id : {id}"
.

Example of POST data:
```json
{
  "devices": [
	{
  	"deviceId": " http://inter-iot.eu/syntax/Iotivity#Instance/test13",
  	"hostedBy": "http://ds_gr.inter-iot.eu/platforms/IoTivity",
  	"location": "http://ds_gr.inter-iot.eu/activage-iotivity-server ",
  	"name": "Blood pressure device"
	}
  
}
```

The REST API operation returns 202 (Accepted) response code. To make sure the process has been executed successfully, check the response messages.


## Testing

Tests are provided with the bridge code, in ``src/test`` folder. These tests can be adapted to test new functionalities.

## License

Apache 2.0
(C) CERTH/ITI Visual Analytics Lab, 2020 
