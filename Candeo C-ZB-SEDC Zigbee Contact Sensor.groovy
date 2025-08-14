/**
 *    Candeo C-ZB-SEDC Zigbee Contact Sensor
 *    Reports Contact Events
 *    Reports Battery Events
 *    Has Setting For Battery Reporting
 */

metadata {
    definition(name: 'Candeo C-ZB-SEDC Zigbee Contact Sensor', namespace: 'Candeo', author: 'Candeo', importUrl: 'https://raw.githubusercontent.com/candeosmart/hubitat-zigbee/refs/heads/main/Candeo%20C-ZB-SEDC%20Zigbee%20Contact%20Sensor.groovy', singleThreaded: true) {
        capability 'ContactSensor'
        capability 'Battery'
        capability 'Sensor'
        capability 'Configuration'

        fingerprint profileId: '0104', endpointId: '01', inClusters: '0000,0003,0500,0001', manufacturer: 'Candeo', model: 'C-ZB-SEDC', deviceJoinName: 'Candeo C-ZB-SEDC Zigbee Contact Sensor'
    }
    preferences {
        input name: 'deviceDriverOptions', type: 'hidden', title: '<strong>Device Driver Options</strong>', description: '<small>The following options change the behaviour of the device driver, they take effect after hitting "<strong>Save Preferences</strong> below."</small>'
        input name: 'loggingOption', type: 'enum', title: 'Logging Option', description: '<small>Sets the logging level cumulatively, for example "Driver Trace Logging" will include all logging levels below it.</small><br><br>', options: PREFLOGGING, defaultValue: '5'
        input name: 'deviceConfigurationOptions', type: 'hidden', title: '<strong>Device Configuration Options</strong>', description: '<small>The following options change the behaviour of the device itself, they take effect after hitting "<strong>Save Preferences</strong> below", followed by "<strong>Configure</strong>" above.<br><br>For a battery powered device, you may also need to wake it up manually!</small>'
        input name: 'batteryPercentageReportTime', type: 'enum', title: 'Battery Percentage Time (hours)', description: '<small>Adjust the period that the battery percentage is reported to suit your requirements.</small><br><br>', options: PREFBATTERYREPORTTIME, defaultValue: '28800'
        input name: 'platformOptions', type: 'hidden', title: '<strong>Platform Options</strong>', description: '<small>The following options are relevant to the Hubitat platform and UI itself.</small>'
    }
}

import groovy.transform.Field

private @Field final String CANDEO = 'Candeo C-ZB-SEDC Device Driver'
private @Field final Boolean DEBUG = false
private @Field final Integer LOGSOFF = 1800
private @Field final Integer ZIGBEEDELAY = 1000
private @Field final Map PREFFALSE = [value: 'false', type: 'bool']
private @Field final Map PREFTRUE = [value: 'true', type: 'bool']
private @Field final Map PREFBATTERYREPORTTIME = ['3600': '1h', '5400': '1.5h', '7200': '2h', '10800': '3h', '21600': '6h', '28800': '8h', '43200': '12h', '64800': '18h']
private @Field final Map PREFLOGGING = ['0': 'Device Event Logging', '1': 'Driver Informational Logging', '2': 'Driver Warning Logging', '3': 'Driver Error Logging', '4': 'Driver Debug Logging', '5': 'Driver Trace Logging' ]

void installed() {
    logsOn()
    logTrace('installed called')
    device.updateSetting('batteryPercentageReportTime', [value: '28800', type: 'enum'])
    logInfo("batteryPercentageReportTime setting is: ${PREFBATTERYREPORTTIME[batteryPercentageReportTime]}")
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
    logDebug("battery percentage time is: ${batteryPercentageReportTime.toBigDecimal()}")
    Integer batteryTime = batteryPercentageReportTime.toInteger()
    List<String> cmds = ["zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0001 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",
                    "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0021 0x20 3600 ${batteryTime} {${intTo16bitUnsignedHex(2)}}", "delay ${ZIGBEEDELAY}",
                    "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0001 {10 00 08 00 2100}", "delay ${ZIGBEEDELAY}",
                    "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0021 {}"]
    logDebug("sending ${cmds}")
    return cmds
}

List<Map<String,?>> parse(String description) {
    logTrace('parse called')
    if (description) {
        logDebug("got description: ${description}")
        if (description.startsWith('enroll') || description.startsWith('zone')) {
            List<Map<String,?>> events = processIASEvents(description)
            if (events) {
                logDebug("parse returning events: ${events}")
                return events
            }
            logDebug("unhandled description: ${description}")
        }
        else {
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
    }
    else {
        logWarn('empty description!')
    }
}

private List<Map> processIASEvents(String description, List<Map> events = []) {
    logDebug('processIASEvents called')
    logDebug("got description: ${description}")
    if (description.startsWith('zone status')) {
        hubitat.zigbee.clusters.iaszone.ZoneStatus zoneStatus = zigbee.parseZoneStatus(description)
        if (zoneStatus != null) {
            logDebug('got zoneStatus')
            logDebug("alarm1: ${zoneStatus.alarm1}")
            logDebug("alarm2: ${zoneStatus.alarm2}")
            logDebug("tamper: ${zoneStatus.tamper}")
            logDebug("battery: ${zoneStatus.battery}")
            logDebug("supervisionReports: ${zoneStatus.supervisionReports}")
            logDebug("restoreReports: ${zoneStatus.restoreReports}")
            logDebug("trouble: ${zoneStatus.trouble}")
            logDebug("ac: ${zoneStatus.ac}")
            logDebug("test: ${zoneStatus.test}")
            logDebug("batteryDefect: ${zoneStatus.batteryDefect}")
            Boolean alarmState = zoneStatus.alarm1 || zoneStatus.alarm2
            logDebug("alarm state is ${alarmState}")
            String contactState = alarmState ? 'open' : 'closed'
            String descriptionText = "${device.displayName} contact is ${contactState}"
            logEvent(descriptionText)
            events.add(processEvent([name: 'contact', value: contactState, descriptionText: descriptionText]))
        }
        else {
            logDebug('could not parse zoneStatus')
        }
    }
    else if (description.startsWith('enroll request')) {
        logDebug('got enrollRequest, not supported by device, ignoring')
    }
    return events
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

private String intTo16bitUnsignedHex(Integer value) {
    String hexStr = zigbee.convertToHexString(value.toInteger(), 4)
    return new String(hexStr.substring(2, 4) + hexStr.substring(0, 2))
}
