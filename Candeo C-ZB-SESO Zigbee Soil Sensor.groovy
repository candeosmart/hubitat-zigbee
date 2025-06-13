/**
 *    Candeo C-ZB-SESO Zigbee Soil Sensor
 *    Reports Temperature Events
 *    Reports Humidity Events
 *    Reports Battery Events
 */

metadata {
    definition(name: 'Candeo C-ZB-SESO Zigbee Soil Sensor', namespace: 'Candeo', author: 'Candeo', importUrl: '', singleThreaded: true) {
        capability 'TemperatureMeasurement'
        capability 'RelativeHumidityMeasurement'
        capability 'Battery'
        capability 'Sensor'
        capability 'Configuration'

        fingerprint profileId: '0104', endpointId: '01', inClusters: '0004,0005,EF00,0000,ED00', outClusters: '0019,000A', manufacturer: '_TZE284_aao3yzhs', model: 'TS0601', deviceJoinName: 'Candeo C-ZB-SESO Zigbee Soil Sensor'
        fingerprint profileId: '0104', endpointId: '01', inClusters: '0004,0005,EF00,0000,ED00', outClusters: '0019,000A', manufacturer: 'Candeo', model: 'C-ZB-SESO', deviceJoinName: 'Candeo C-ZB-SESO Zigbee Soil Sensor'
    }
    preferences {
        input name: 'deviceDriverOptions', type: 'hidden', title: '<strong>Device Driver Options</strong>', description: '<small>The following options change the behaviour of the device driver, they take effect after hitting "<strong>Save Preferences</strong> below."</small>'
        input name: 'loggingOption', type: 'enum', title: 'Logging Option', description: '<small>Sets the logging level cumulatively, for example "Driver Trace Logging" will include all logging levels below it.</small><br><br>', options: PREFLOGGING, defaultValue: '5'
        input name: 'platformOptions', type: 'hidden', title: '<strong>Platform Options</strong>', description: '<small>The following options are relevant to the Hubitat platform and UI itself.</small>'
    }
}

import groovy.transform.Field

private @Field final String CANDEO = 'Candeo C-ZB-SESO Device Driver'
private @Field final Boolean DEBUG = false
private @Field final Integer LOGSOFF = 1800
private @Field final String TUYACLUSTER = 'EF00'
private @Field final List<Map<String,String,String>> TUYADATAPOINTS = [ ['name': 'Soil Moisture', 'id': '03', 'type': 'VALUE'], ['name': 'Temperature', 'id': '05', 'type': 'VALUE'], ['name': 'Battery', 'id': '0F', 'type': 'VALUE'], ['name': 'Temperature Unit', 'id': '09', 'type': 'ENUM'], ['name': 'Battery State', 'id': '0E', 'type': 'STRING'] ]
private @Field final Map<String,String> TUYADATAPOINTTYPES = ['RAW': '00', 'BOOLEAN': '01', 'VALUE': '02', 'STRING': '03', 'ENUM': '04', 'BITMAP': '05']
private @Field final Map<String,String> TUYACOMMANDS = ['DataRequest': '00', 'DataQuery': '03', 'SendData': '04', 'McuVersionRequest': '10', 'McuSyncTime': '24']
private @Field final Map<String,String> TUYACOMMANDSRESPONSE = ['01': 'DataResponse', '02': 'DataReport', '05': 'ActiveStatusReportAlt', '06': 'ActiveStatusReport', '11': 'McuVersionResponse', '24': 'McuSyncTime', '0B': 'DefaultResponse']
private @Field final Map<String,String> PREFLOGGING = ['0': 'Device Event Logging', '1': 'Driver Informational Logging', '2': 'Driver Warning Logging', '3': 'Driver Error Logging', '4': 'Driver Debug Logging', '5': 'Driver Trace Logging' ]

void installed() {
    logsOn()
    logTrace('installed called')
    logInfo('logging level is: Driver Trace Logging')
    logInfo("logging level will reduce to Driver Error Logging after ${LOGSOFF} seconds")
}

void uninstalled() {
    logTrace('uninstalled called')
    clearAll()
}

void updated() {
    logTrace('updated called')
    logTrace("settings: ${settings}")
    logInfo("logging level is: ${PREFLOGGING[loggingOption]}", true)
    clearAll()
}

void logsOff() {
    logTrace('logsOff called')
    if (DEBUG) {
        logDebug('DEBUG field variable is set, not disabling logging automatically!', true)
    }
    else {
        logInfo('automatically reducing logging level to Driver Error Logging', true)
        device.updateSetting('loggingOption', [value: '3', type: 'enum'])
    }
}

List<String> configure() {
    logTrace('configure called')
    List<String> cmds = tuyaMagicPacket()
    logDebug("sending ${cmds}")
    return cmds
}

List<Map<String,?>> parse(String description) {
    logTrace('parse called')
    if (description) {
        logDebug("got description: ${description}")
        Map<String,?> descriptionMap = null
        try {
            descriptionMap = zigbee.parseDescriptionAsMap(description)
        }
        catch (Exception ex) {
            logError("could not parse the description as platform threw error: ${ex}")
        }
        if (descriptionMap == [:]) {
            logWarn("descriptionMap is empty, can't continue!")
        }
        else if (descriptionMap) {
            List<Map<String,?>> events = processEvents(descriptionMap)
            if (events) {
                logDebug("parse returning events: ${events}")
                return events
            }
            logDebug("unhandled descriptionMap: ${descriptionMap}")
        }
        else {
            logWarn('no descriptionMap available!')
        }
    }
    else {
        logWarn('empty description!')
    }
}

private List<Map> processEvents(Map descriptionMap, List<Map> events = []) {
    logTrace('processEvents called')
    logDebug("got descriptionMap: ${descriptionMap}")
    if (descriptionMap.profileId && descriptionMap.profileId == '0000') {
        logTrace('skipping ZDP profile message')
    }
    else if (!(descriptionMap.profileId) || (descriptionMap.profileId && descriptionMap.profileId == '0104')) {
        if (descriptionMap.cluster == TUYACLUSTER || descriptionMap.clusterId == TUYACLUSTER || descriptionMap.clusterInt == zigbee.convertHexToInt(TUYACLUSTER)) {
            processTuyaCluster(descriptionMap, events)
        }
        else if (descriptionMap.cluster == '0000' || descriptionMap.clusterId == '0000' || descriptionMap.clusterInt == 0) {
            processBasicCluster(descriptionMap, events)
        }
        else {
            logDebug("skipped descriptionMap.cluster: ${descriptionMap.cluster ?: 'unknown'} descriptionMap.clusterId: ${descriptionMap.clusterId ?: 'unknown'} descriptionMap.clusterInt: ${descriptionMap.clusterInt ?: 'unknown'}")
        }
        if (descriptionMap.additionalAttrs) {
            logDebug("got additionalAttrs: ${descriptionMap.additionalAttrs}")
            descriptionMap.additionalAttrs.each { Map attribute ->
                attribute.clusterInt = descriptionMap.clusterInt
                attribute.cluster = descriptionMap.cluster
                attribute.clusterId = descriptionMap.clusterId
                attribute.command = descriptionMap.command
                processEvents(attribute, events)
            }
        }
    }
    return events
}

private List<String> tuyaMagicPacket() {
    logTrace('tuyaMagicPacket called')
    List<String> cmds = [
                        "he raw 0x${device.deviceNetworkId} 1 0x${device.endpointId} 0x0000 {10 00 00 04 00 00 00 01 00 05 00 07 00 FE FF}", 'delay 1000',
                        "he wattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0000 0xFFDE 0x20 {0D} {}", 'delay 1000',
                        tuyaDataQuery(), 'delay 1000'
                        ]
    return cmds
}

private String tuyaDataPointRequest(Map<String,?> dataPoints) {
    logTrace("tuyaDataPointRequest called dataPoints: ${dataPoints}")
    String tuyaDataPoints = ''
    dataPoints.each { key, val ->
        logTrace("dataPoint: ${key} value: ${val}")
        tuyaDataPoints += tuyaDataPoint(key, val)
    }
    return tuyaDataPointRequest(tuyaDataPoints)
}

private String tuyaDataPointRequest(String dataPoint, def value) {
    logTrace("tuyaDataPointRequest called dataPoint: ${dataPoint} value: ${value}")
    String tuyaDataPoint = tuyaDataPoint(dataPoint, value)
    return tuyaDataPointRequest(tuyaDataPoint)
}

private String tuyaDataPointRequest(String tuyaDataPoint) {
    logTrace("tuyaDataPointRequest called tuyaDataPoint: ${tuyaDataPoint}")
    return tuyaDataPacket(TUYACOMMANDS['DataRequest'], tuyaDataPoint)
}

private String tuyaDataPoint(String dataPoint, def value) {
    logTrace("tuyaDataPoint called dataPoint: ${dataPoint} value: ${value}")
    String dataPointId = lookupTuyaDataPointDetail(dataPoint, 'name', 'id')
    logDebug("dataPointId: ${dataPointId}")
    String dataPointType = lookupTuyaDataPointDetail(dataPoint, 'name', 'type')
    logDebug("dataPointType: ${dataPointType}")
    String dataPointTypeId = lookupTuyaDataPointType(dataPointType)
    logDebug("dataPointTypeId: ${dataPointTypeId}")
    String dataPointValue = encodeTuyaDataPoint(value, dataPointType)
    logDebug("dataPointValue: ${dataPointValue}")
    Integer dataPointLength = (dataPointValue.length() / 2)
    logDebug("dataPointLength: ${dataPointLength}")
    String tuyaDataPoint = dataPointId + dataPointTypeId + zigbee.convertToHexString(dataPointLength, 4) + dataPointValue
    logDebug("tuyaDataPoint: ${tuyaDataPoint}")
    return tuyaDataPoint
}

private String tuyaDataQuery() {
    logTrace('tuyaDataQuery called')
    return tuyaDataPacket(TUYACOMMANDS['DataQuery'])
}

private String tuyaDataPacket(String tuyaCommand, String tuyaData = null, Boolean tuyaSequence = true) {
    logTrace("tuyaDataPacket called tuyaCommand: ${tuyaCommand} tuyaData: ${tuyaData} tuyaSequence: ${tuyaSequence}")
    String cmds = "he raw 0x${device.deviceNetworkId} 1 0x${device.endpointId} 0x${TUYACLUSTER} {1100${tuyaCommand}"
    if (tuyaData) {
        if (tuyaSequence) {
            String tuyaSequenceNumber = zigbee.convertToHexString(new Random().nextInt(65536), 4)
            cmds += tuyaSequenceNumber
        }
        cmds += tuyaData
    }
    cmds += '}'
    logDebug("returning cmds: ${cmds}")
    return cmds
}

private String tuyaMcuSyncTime() {
    logTrace('tuyaMcuSyncTime called')
    Integer tzOffset = 0
    try {
        tzOffset = location.getTimeZone().getOffset(new Date().getTime())
        logDebug("tzOffset: ${tzOffset}")
    }
    catch (e) {
        logInfo('********** unable to retrieve current location to calculate timezone offset, please set your location in the Hubitat settings area! **********', true)
    }
    Long cts = currentTimeStamp
    Integer now = (cts / 1000).intValue()
    logDebug("now: ${now}")
    Integer nowOffset = ((cts + tzOffset) / 1000).intValue()
    logDebug("nowOffset: ${nowOffset}")
    return tuyaDataPacket(TUYACOMMANDS['McuSyncTime'], zigbee.convertToHexString(now, 8) + zigbee.convertToHexString(nowOffset, 8))
}

private String lookupTuyaDataPointDetail(String searchData, String searchField, String valueField = null) {
    logTrace("lookupTuyaDataPointDetail called searchData: ${searchData} searchField: ${searchField} valueField: ${valueField}")
    String field = valueField != null ? valueField : searchField
    String value = 'unknown'
    TUYADATAPOINTS.each { Map datapoint ->
        if (datapoint[searchField] == searchData) {
            value = datapoint[field]
        }
    }
    return value
}

private String lookupTuyaDataPointType(String searchData, String searchField = 'name') {
    logTrace("lookupTuyaDataPointType called searchData: ${searchData} searchField: ${searchField}")
    switch (searchField) {
        case 'name':
            return TUYADATAPOINTTYPES[searchData]
        case 'id':
            return TUYADATAPOINTTYPES.find { dataPointType ->
                dataPointType.value == searchData
            }?.key
    }
    return 'unknown'
}

private decodeTuyaDataPoint(List<String> data, String dataPointType) {
    logTrace("decodeTuyaDataPoint called data: ${data} dataPointType: ${dataPointType}")
    switch (dataPointType) {
        case 'RAW': // bytes
            logDebug('raw not yet decoded!')
            return data
        case 'BOOLEAN': // 0/1
            Integer state = zigbee.convertHexToInt(data[0])
            logDebug("state: ${state}")
            return state == 1
        case 'VALUE': // 4 byte value
            Integer value = 0
            try {
                Integer length = data.size()
                Integer power = 1
                for (Integer i in length..1) {
                    value = value + power * zigbee.convertHexToInt(data[i - 1])
                    power = power * 256
                }
            }
            catch (e) {
                logError("could not decode tuya datapoint: ${data} error: ${e}")
            }
            logDebug("value: ${value}")
            return value
        case 'STRING': // N byte string
            String value = ''
            try {
                Integer length = data.size()
                for (Integer i in 1..length) {
                    value = value + zigbee.convertHexToInt(data[i - 1]) as char
                }
            }
            catch (e) {
                logError("could not decode tuya datapoint: ${data} error: ${e}")
            }
            logDebug("value: ${value}")
            return value
        case 'ENUM': // 0 - 255
            Integer value = zigbee.convertHexToInt(data[0])
            logDebug("value: ${value}")
            return value
        case 'BITMAP': //bytes as bits
            logDebug('bitmap not yet decoded!')
            return data
        default:
            logDebug("skipped decoding of unknown dataPointType: ${dataPointType}!")
            return data
    }
}

private String encodeTuyaDataPoint(def data, String dataPointType) {
    logTrace("encodeTuyaDataPoint called data: ${data} dataPointType: ${dataPointType}")
    switch (dataPointType) {
        case 'RAW': // bytes
            logDebug('raw not yet encoded!')
            return data
        case 'BOOLEAN': // 0/1
            if (data instanceof Boolean) {
                return data ? '01' : '00'
            }
            logDebug('data did not match type, could not encode!')
            return data
        case 'VALUE': // 4 byte value
            if (data instanceof Integer) {
                return zigbee.convertToHexString(data as Integer, 8)
            }
            logDebug('data did not match type, could not encode!')
            return data
        case 'STRING': // N byte string
            logDebug('string not yet encoded!')
            return data
        case 'ENUM': // 0 - 255
            if (data instanceof Integer) {
                return zigbee.convertToHexString(data as Integer, 2)
            }
            logDebug('data did not match type, could not encode!')
            return data
        case 'BITMAP': //bytes as bits
            logDebug('bitmap not yet encoded!')
            return data
        default:
            logDebug("skipped encoding of unknown dataPointType: ${dataPointType}!")
            return data
    }
}

private void processTuyaCluster(Map descriptionMap, List<Map> events) {
    logTrace('processTuyaCluster called')
    String tuyaCommand = 'unknown'
    if (TUYACOMMANDSRESPONSE[descriptionMap.command]) {
        tuyaCommand = TUYACOMMANDSRESPONSE[descriptionMap.command]
    }
    logDebug("tuyaCommand: ${tuyaCommand}")
    switch (tuyaCommand) {
        case 'DataResponse':
        case 'DataReport':
            if (tuyaCommand == 'DataResponse') {
                logDebug('tuya cluster (EF00) DataResponse (01)')
            }
            else if (tuyaCommand == 'DataReport') {
                logDebug('tuya cluster (EF00) DataReport (02) skipped')
            }
            String dataPoint = lookupTuyaDataPointDetail(descriptionMap.data[2], 'id', 'name')
            logDebug("dataPoint: ${dataPoint}")
            String dataPointType = lookupTuyaDataPointType(descriptionMap.data[3], 'id')
            logDebug("dataPointType: ${dataPointType}")
            Integer length = zigbee.convertHexToInt(descriptionMap.data[5])
            logDebug("length: ${length}")
            List<String> data = descriptionMap.data[6 .. (length + 5)]
            logDebug("data: ${data}")
            def dataPointValue = decodeTuyaDataPoint(data, dataPointType)
            logDebug("dataPointValue: ${dataPointValue}")
            switch (dataPoint) {
                case 'Battery':
                    logDebug("battery percent: ${dataPointValue}")
                    String descriptionText = "${device.displayName} battery percentage is ${dataPointValue}%"
                    logEvent(descriptionText)
                    events.add(processEvent([name: 'battery', value: dataPointValue, unit: '%', descriptionText: descriptionText]))
                    break
                case 'Temperature':
                    logDebug("temperature: ${dataPointValue}")
                    BigDecimal temperatureValue = dataPointValue / 10
                    logDebug("calculated temperature: ${temperatureValue}")
                    String descriptionText = "${device.displayName} temperature is ${temperatureValue}°C"
                    logEvent(descriptionText)
                    events.add(processEvent([name: 'temperature', value: temperatureValue, unit: '°C', descriptionText: descriptionText]))
                    break
                case 'Soil Moisture':
                    logDebug("humidity: ${dataPointValue}")
                    BigDecimal humidityValue = dataPointValue 
                    logDebug("calculated humidity: ${humidityValue}")
                    String descriptionText = "${device.displayName} humidity is ${humidityValue}%"
                    logEvent(descriptionText)
                    events.add(processEvent([name: 'humidity', value: humidityValue, unit: '%', descriptionText: descriptionText]))
                    break
                default:
                    logDebug("skipped unknown dataPoint: ${descriptionMap.data[2]}")
                    break
            }
            break
        case 'DefaultResponse':
            logDebug('tuya cluster (EF00) DefaultResponse (0B) skipped')
            break
        case 'McuSyncTime':
            logDebug('tuya cluster (EF00) McuSyncTime (24)')
            doZigBeeCommand([tuyaMcuSyncTime()])
            break
        default:
            logDebug("tuya cluster (EF00) command ${descriptionMap.command} skipped")
            break
    }
}

private void processBasicCluster(Map descriptionMap, List<Map> events) {
    logTrace('processBasicluster called')
    switch (descriptionMap.command) {
        case '01':
        case '0A':
            if (descriptionMap.attrId == 'FFE2' || descriptionMap.attrInt == 65506) {
                logDebug("basic (0000) tuya specific attribute FFE2 reported value: ${descriptionMap.value}")
            }
            else if (descriptionMap.attrId == 'FFE4' || descriptionMap.attrInt == 65508) {
                logDebug("basic (0000) tuya specific attribute FFE4 reported value: ${descriptionMap.value}")
            }
            else if (descriptionMap.attrId == 'FFCF' || descriptionMap.attrInt == 65487) {
                logDebug("basic (0000) tuya specific attribute FFCF reported value: ${descriptionMap.value}")
            }
            else if (descriptionMap.attrId == 'FFDF' || descriptionMap.attrInt == 65503) {
                logDebug("basic (0000) tuya specific attribute FFDF reported value: ${descriptionMap.value}")
            }
            else if (descriptionMap.attrId == '0000' || descriptionMap.attrInt == 0) {
                logDebug("basic (0000) ZCLVersion attribute 0000 reported value: ${descriptionMap.value}")
            }
            else if (descriptionMap.attrId == '0001' || descriptionMap.attrInt == 1) {
                logDebug("basic (0000) ApplicationVersion reported value: ${descriptionMap.value}")
            }
            else if (descriptionMap.attrId == '0004' || descriptionMap.attrInt == 4) {
                logDebug("basic (0000) ManufacturerName attribute 0004 reported value: ${descriptionMap.value}")
            }
            else if (descriptionMap.attrId == '0005' || descriptionMap.attrInt == 5) {
                logDebug("basic (0000) ModelIdentifier attribute 0005 reported value: ${descriptionMap.value}")
            }
            else {
                logDebug('basic (0000) attribute skipped')
            }
            break
        case '04':
            logDebug('basic (0000) write attribute response (04) skipped')
            break
        case '07':
            logDebug('basic (0000) configure reporting response (07) skipped')
            break
        case '0B':
            logDebug('basic (0000) default response (0B) skipped')
            break
        default:
            logDebug("basic (0000) command ${descriptionMap.command} skipped")
            break
    }
}

private Map processEvent(Map event) {
    logTrace("processEvent called data: ${event}")
    return createEvent(event)
}

private Boolean logMatch(String logLevel) {
    Map<String, String> logLevels = ['event': '0', 'info': '1', 'warn': '2', 'error': '3', 'debug': '4', 'trace': '5' ]
    return loggingOption ? loggingOption.toInteger() >= logLevels[logLevel].toInteger() : false
}

private String logTrace(String msg, Boolean override = false) {
    if (logMatch('trace') || override) {
        log.trace(logMsg(msg))
    }
}

private String logDebug(String msg, Boolean override = false) {
    if (logMatch('debug') || override) {
        log.debug(logMsg(msg))
    }
}

private String logError(String msg, Boolean override = false) {
    if (logMatch('error') || override) {
        log.error(logMsg(msg))
    }
}

private String logWarn(String msg, Boolean override = false) {
    if (logMatch('warn') || override) {
        log.warn(logMsg(msg))
    }
}

private String logInfo(String msg, Boolean override = false) {
    if (logMatch('info') || override) {
        log.info(logMsg(msg))
    }
}

private String logEvent(String msg, Boolean override = false) {
    if (logMatch('event') || override) {
        log.info(logMsg(msg))
    }
}

private String logMsg(String msg) {
    String log = "candeo logging for ${CANDEO} -- "
    log += msg
    return log
}

private void logsOn() {
    logTrace('logsOn called', true)
    device.updateSetting('loggingOption', [value: '5', type: 'enum'])
    runIn(LOGSOFF, logsOff)
}

private void clearAll() {
    logTrace('clearAll called')
    state.clear()
    atomicState.clear()
    unschedule()
}

private void doZigBeeCommand(List<String> cmds) {
    logTrace('doZigBeeCommand called')
    logDebug("sending ${cmds}")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

private Long getCurrentTimeStamp() {
    logTrace('getCurrentTimeStamp called')
    Long timeStamp = java.time.Instant.now().toEpochMilli()
    logDebug("currentTimeStamp: ${timeStamp}")
    return timeStamp
}
