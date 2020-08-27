# IoTivity Bridge

Bridge for IoTivity platform

## Build bridge


To build the IoTivity bridge:
1. Clone repository
2. Change directory to cloned repository folder
3. Build using maven. Skip all unitary tests in the compilation and use the jar obtained. For compiling the bridge you must use a maven version >= 3.6.0.

```
mvn clean package -DskipTests
```

## Testing

Tests are provided with the bridge code, in ``src/test`` folder. These tests can be adapted to test new functionalities.

## License

Copyright, by CERTH/ITI, http://www.iti.gr . Licensed under the  Eclipse Public License v1.0.