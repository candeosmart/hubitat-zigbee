/**
 *    Candeo C-ZB-SETE Zigbee Temperature & Humidity Sensor
 *    Reports Temperature Events
 *    Reports Humidity Events
 *    Reports Battery Events
 *    Has Setting For Battery Reporting
 *    Has Setting For Temperature Reporting
 *    Has Setting For Humidity Reporting
 */

metadata {
    definition(name: 'Candeo C-ZB-SETE Zigbee Temperature & Humidity Sensor', namespace: 'Candeo', author: 'Candeo', importUrl: 'https://raw.githubusercontent.com/candeosmart/hubitat-zigbee/refs/heads/main/Candeo%20C-ZB-SETE%20Zigbee%20Temperature%20%26%20Humidity%20Sensor.groovy', singleThreaded: true) {
        capability 'TemperatureMeasurement'
        capability 'RelativeHumidityMeasurement'
        capability 'Battery'
        capability 'Sensor'
        capability 'Configuration'

        fingerprint profileId: '0104', endpointId: '01', inClusters: '0000,0003,0402,0405,0001', outClusters:'0003', manufacturer: 'Candeo', model: 'C-ZB-SETE', deviceJoinName: 'Candeo C-ZB-SETE Zigbee Temperature & Humidity Sensor'
    }
    preferences {
        input name: 'deviceDriverOptions', type: 'hidden', title: '<strong>Device Driver Options</strong>', description: '<small>The following options change the behaviour of the device driver, they take effect after hitting "<strong>Save Preferences</strong> below."</small>'
        input name: 'loggingOption', type: 'enum', title: 'Logging Option', description: '<small>Sets the logging level cumulatively, for example "Driver Trace Logging" will include all logging levels below it.</small><br><br>', options: PREFLOGGING, defaultValue: '5'
        input name: 'deviceConfigurationOptions', type: 'hidden', title: '<strong>Device Configuration Options</strong>', description: '<small>The following options change the behaviour of the device itself, they take effect after hitting "<strong>Save Preferences</strong> below", followed by "<strong>Configure</strong>" above.<br><br>For a battery powered device, you may also need to wake it up manually!</small>'
        input name: 'temperatureReportChange', type: 'enum', title: 'Temperature Change (°C)', options: PREFTEMPERATUREREPORTCHANGE, defaultValue: '1'
        input name: 'temperatureReportTime', type: 'enum', title: 'Temperature Time (minutes)', options: PREFTEMPERATUREHUMIDITYREPORTTIME, defaultValue: '1800'
        input name: 'humidityReportChange', type: 'enum', title: 'Humidity Change (%)', options: PREFHUMIDITYREPORTCHANGE, defaultValue: '5'
        input name: 'humidityReportTime', type: 'enum', title: 'Humidity Time (minutes)', options: PREFTEMPERATUREHUMIDITYREPORTTIME, defaultValue: '3600'
        input name: 'batteryPercentageReportTime', type: 'enum', title: 'Battery Percentage Time (hours)', description: '<small>Adjust the period that the battery percentage is reported to suit your requirements.</small><br><br>', options: PREFBATTERYREPORTTIME, defaultValue: '28800'
        input name: 'platformOptions', type: 'hidden', title: '<strong>Platform Options</strong>', description: '<small>The following options are relevant to the Hubitat platform and UI itself.</small>'
    }
}

import groovy.transform.Field

private @Field final String CANDEO = 'Candeo C-ZB-SETE Device Driver'
private @Field final Boolean DEBUG = false
private @Field final Integer LOGSOFF = 1800
private @Field final Integer ZIGBEEDELAY = 1000
private @Field final Map PREFFALSE = [value: 'false', type: 'bool']
private @Field final Map PREFTRUE = [value: 'true', type: 'bool']
private @Field final Map PREFBATTERYREPORTTIME = ['3600': '1h', '5400': '1.5h', '7200': '2h', '10800': '3h', '21600': '6h', '28800': '8h', '43200': '12h', '64800': '18h']
private @Field final Map PREFTEMPERATUREHUMIDITYREPORTTIME = ['60': '1m', '90': '1.5m', '120': '2m', '240': '4m', '300': '5m', '600': '10m', '1200': '20m', '1800': '30m', '2400': '40m', '3000': '50m', '3600': '60m', '5400': '90m', '7200': '120m']
private @Field final Map PREFTEMPERATUREREPORTCHANGE = ['0.1': '0.1°C', '0.2': '0.2°C', '0.3': '0.3°C', '0.4': '0.4°C', '0.5': '0.5°C', '0.6': '0.6°C', '0.7': '0.7°C', '0.8': '0.8°C', '0.9': '0.9°C', '1': '1°C', '1.5': '1.5°C', '2': '2°C', '2.5': '2.5°C', '3': '3°C', '3.5': '3.5°C', '4': '4°C', '4.5': '4.5°C', '5': '5°C']
private @Field final Map PREFHUMIDITYREPORTCHANGE = ['1': '1%', '2': '2%', '3': '3%', '4': '4%', '5': '5%', '6': '6%', '7': '7%', '8': '8%', '9': '9%', '10': '10%']
private @Field final Map PREFLOGGING = ['0': 'Device Event Logging', '1': 'Driver Informational Logging', '2': 'Driver Warning Logging', '3': 'Driver Error Logging', '4': 'Driver Debug Logging', '5': 'Driver Trace Logging' ]

void installed() {
    logsOn()
    logTrace('installed called')
    device.updateSetting('batteryPercentageReportTime', [value: '28800', type: 'enum'])
    logInfo("batteryPercentageReportTime setting is: ${PREFBATTERYREPORTTIME[batteryPercentageReportTime]}")
    device.updateSetting('temperatureReportChange', [value: '1', type: 'enum'])
    logInfo("temperatureReportChange setting is: ${PREFTEMPERATUREREPORTCHANGE[temperatureReportChange]}")
    device.updateSetting('temperatureReportTime', [value: '1800', type: 'enum'])
    logInfo("temperatureReportTime setting is: ${PREFTEMPERATUREHUMIDITYREPORTTIME[temperatureReportTime]}")
    device.updateSetting('humidityReportChange', [value: '5', type: 'enum'])
    logInfo("humidityReportChange setting is: ${PREFHUMIDITYREPORTCHANGE[humidityReportChange]}")
    device.updateSetting('humidityReportTime', [value: '3600', type: 'enum'])
    logInfo("humidityReportTime setting is: ${PREFTEMPERATUREHUMIDITYREPORTTIME[humidityReportTime]}")
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
    logInfo("batteryPercentageReportTime setting is: ${PREFBATTERYREPORTTIME[batteryPercentageReportTime ?: '28800']}", true)
    logInfo("temperatureReportChange setting is: ${PREFTEMPERATUREREPORTCHANGE[temperatureReportChange ?: '1']}", true)
    logInfo("temperatureReportTime setting is: ${PREFTEMPERATUREHUMIDITYREPORTTIME[temperatureReportTime ?: '1800']}", true)
    logInfo("humidityReportChange setting is: ${PREFHUMIDITYREPORTCHANGE[humidityReportChange ?: '5']}", true)
    logInfo("humidityReportTime setting is: ${PREFTEMPERATUREHUMIDITYREPORTTIME[humidityReportTime ?: '3600']}", true)
    logInfo("logging level is: ${PREFLOGGING[loggingOption]}", true)
    clearAll()
    if (logMatch('debug')) {
        logInfo("logging level will reduce to Driver Error Logging after ${LOGSOFF} seconds", true)
        runIn(LOGSOFF, logsOff)
    }
    logInfo('if you have changed any Device Configuration Options, make sure that you hit Configure above!', true)
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
    logDebug('battery powered device requires manual wakeup to accept configuration commands')
    logDebug("battery percentage time is: ${batteryPercentageReportTime ?: '28800'}")
    Integer batteryTime = batteryPercentageReportTime ? batteryPercentageReportTime.toInteger() : 28800
    logDebug("temperature change is: ${temperatureReportChange ?: '1'}°C")
    logDebug("temperature time is: ${temperatureReportTime ?: '1800'}s")
    Integer temperatureChange = ((temperatureReportChange ? temperatureReportChange.toBigDecimal() : 1) * 100).toInteger()
    logDebug("temperatureChange: ${temperatureChange}")
    if (temperatureChange == 0) {
        logDebug('temperatureChange is ZERO, protecting against report flooding!')
        temperatureChange = 1000
    }
    Integer temperatureTime = temperatureReportTime ? temperatureReportTime.toInteger() : 1800
    logDebug("humidity change is: ${humidityReportChange ?: '5'}%")
    logDebug("humidity time is: ${humidityReportTime ?: '3600'}s")
    Integer humidityChange = ((humidityReportChange ? humidityReportChange.toBigDecimal() : 5) * 100).toInteger()
    logDebug("humidityChange: ${humidityChange}")
    if (humidityChange == 0) {
        logDebug('humidityChange is ZERO, protecting against report flooding!')
        humidityChange = 1000
    }
    Integer humidityTime = humidityReportTime ? humidityReportTime.toInteger() : 3600
    List<String> cmds = ["zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0402 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",
                        "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0402 0x0000 ${DataType.INT16} 60 ${temperatureTime} {${convertToHexString(temperatureChange)}}", "delay ${ZIGBEEDELAY}",
                        "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0402 {10 00 08 00 0000}", "delay ${ZIGBEEDELAY}",
                        "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0402 0 {}", "delay ${ZIGBEEDELAY}",
                        "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0405 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",
                        "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0405 0x0000 ${DataType.UINT16} 60 ${humidityTime} {${convertToHexString(humidityChange, 2, true)}}", "delay ${ZIGBEEDELAY}",
                        "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0405 {10 00 08 00 0000}", "delay ${ZIGBEEDELAY}",
                        "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0405 0 {}", "delay ${ZIGBEEDELAY}",
                        "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0001 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",
                        "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0021 ${DataType.UINT8} 3600 ${batteryTime} {${intTo16bitUnsignedHex(2)}}", "delay ${ZIGBEEDELAY}",
                        "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0001 {10 00 08 00 2100}", "delay ${ZIGBEEDELAY}",
                        "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0021 {}"]
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
        if (descriptionMap.cluster == '0001' || descriptionMap.clusterId == '0001' || descriptionMap.clusterInt == 1) {
            processPowerConfigurationCluster(descriptionMap, events)
        }
        else if (descriptionMap.cluster == '0402' || descriptionMap.clusterId == '0402' || descriptionMap.clusterInt == 1026) {
            processTemperatureMeasurementCluster(descriptionMap, events)
        }
        else if (descriptionMap.cluster == '0405' || descriptionMap.clusterId == '0405' || descriptionMap.clusterInt == 1029) {
            processRelativeHumidityMeasurementCluster(descriptionMap, events)
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

private void processPowerConfigurationCluster(Map descriptionMap, List<Map> events) {
    logTrace('processPowerConfigurationCluster called')
    switch (descriptionMap.command) {
        case '0A':
        case '01':
            if (descriptionMap.attrId == '0021' || descriptionMap.attrInt == 33) {
                logDebug('power configuration (0001) battery percentage report (0021)')
                Integer batteryValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("battery percentage report is ${batteryValue}")
                batteryValue = batteryValue.intdiv(2)
                logDebug("calculated battery percentage is ${batteryValue}")
                String descriptionText = "${device.displayName} battery percent is ${batteryValue}%"
                logEvent(descriptionText)
                events.add(processEvent([name: 'battery', value: batteryValue, unit: '%', descriptionText: descriptionText, isStateChange: true]))
            }
            else {
                logDebug('power configuration (0001) attribute skipped')
            }
            break
        case '04':
            logDebug('power configuration (0001) write attribute response (04) skipped')
            break
        case '07':
            logDebug('power configuration (0001) configure reporting response (07) skipped')
            break
        case '0B':
            logDebug('power configuration (0001) default response (0B) skipped')
            break
        default:
            logDebug('power configuration (0001) command skipped')
            break
    }
}

private void processTemperatureMeasurementCluster(Map descriptionMap, List<Map> events) {
    logTrace('processTemperatureMeasurementCluster called')
    switch (descriptionMap.command) {
        case '0A':
        case '01':
            if (descriptionMap.attrId == '0000' || descriptionMap.attrInt == 0) {
                logDebug('temperature measurement (0402) measured value report (0000)')
                BigDecimal temperatureValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("temperature measurement measured value report is ${temperatureValue}")
                temperatureValue = temperatureValue / 100
                logDebug("calculated temperature is ${temperatureValue}")
                String descriptionText = "${device.displayName} temperature is ${temperatureValue}°C"
                logEvent(descriptionText)
                events.add(processEvent([name: 'temperature', value: temperatureValue, unit: '°C', descriptionText: descriptionText]))
            }
            else {
                logDebug('temperature measurement (0402) attribute skipped')
            }
            break
        case '04':
            logDebug('temperature measurement (0402) write attribute response (04) skipped')
            break
        case '07':
            logDebug('temperature measurement (0402) configure reporting response (07) skipped')
            break
        case '0B':
            logDebug('temperature measurement (0402) default response (0B) skipped')
            break
        default:
            logDebug('temperature measurement (0402) command skipped')
            break
    }
}

private void processRelativeHumidityMeasurementCluster(Map descriptionMap, List<Map> events) {
    logTrace('processRelativeHumidityMeasurementCluster called')
    switch (descriptionMap.command) {
        case '0A':
        case '01':
            if (descriptionMap.attrId == '0000' || descriptionMap.attrInt == 0) {
                logDebug('relative humidity measurement (0405) measured value report (0000)')
                BigDecimal humidityValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("relative humidity measurement measured value report is ${humidityValue}")
                humidityValue = humidityValue / 100
                logDebug("calculated humidity is ${humidityValue}")
                String descriptionText = "${device.displayName} humidity is ${humidityValue}%"
                logEvent(descriptionText)
                events.add(processEvent([name: 'humidity', value: humidityValue, unit: '%', descriptionText: descriptionText]))
            }
            else {
                logDebug('relative humidity measurement (0405) attribute skipped')
            }
            break
        case '04':
            logDebug('relative humidity measurement (0405) write attribute response (04) skipped')
            break
        case '07':
            logDebug('relative humidity measurement (0405) configure reporting response (07) skipped')
            break
        case '0B':
            logDebug('relative humidity measurement (0405) default response (0B) skipped')
            break
        default:
            logDebug('relative humidity measurement (0405) command skipped')
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

private String intTo16bitUnsignedHex(Integer value, Boolean reverse = true) {
    String hexStr = zigbee.convertToHexString(value.toInteger(), 4)
    if (reverse) {
        return new String(hexStr.substring(2, 4) + hexStr.substring(0, 2))
    }
    return hexStr
}

private String convertToHexString(String value, Integer minBytes = 1, Boolean reverse = false) {
    return convertToHexString(convertToInteger(value), minBytes, reverse)
}

private List<String> convertToHexString(List<?> values, Integer minBytes = 1, Boolean reverse = false) {
    return values.collect { value -> convertToHexString(value, minBytes, reverse) }
}

private String convertToHexString(Integer value, Integer minBytes = 1, Boolean reverse = false) {
    logTrace("convertToHexString called value: ${value} minBytes: ${minBytes} reverse: ${reverse}")
    String hexString = hubitat.helper.HexUtils.integerToHexString(value, minBytes)
    if (reverse) {
        return reverseStringOfBytes(hexString)
    }
    return hexString
}

private String reverseStringOfBytes(String value) {
    logTrace("reverseStringOfBytes called value: ${value}")
    return value.split('(?<=\\G..)').reverse().join()
}
