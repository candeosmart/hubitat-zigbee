/**
 *    Candeo C-ZB-SR5BR Smart Scene Remote
 *    Reports button 1 - 4 pushed, double tapped, held & released events for buttons 1 - 4
 *    Reports button 5 pushed, double tapped, held & released events for center button
 *    Reports button 6 held events for starting & continuing clockwise rotation of the ring
 *    Reports button 6 released events for stopping clockwise rotation of the ring
 *    Reports button 7 held events for starting & continuing counter-clockwise rotation of the ring
 *    Reports button 7 released events for stopping counter-clockwise rotation of the ring
 *    Reports Battery Events
 *    Has Setting For Battery Reporting
 */

metadata {
    definition(name: 'Candeo C-ZB-SR5BR Smart Scene Remote', namespace: 'Candeo', author: 'Candeo', importUrl: 'https://raw.githubusercontent.com/candeosmart/hubitat-zigbee/ea628d2e0590d782a611671c30b054feb8857be3/Candeo%20C-ZB-SR5BR%20Smart%20Scene%20Remote.groovy', singleThreaded: true) {
        capability 'PushableButton'
        capability 'DoubleTapableButton'
        capability 'ReleasableButton'
        capability 'HoldableButton'
        capability 'Battery'
        capability 'Sensor'
        capability 'Configuration'

        fingerprint profileId: '0104', endpointId: '01', inClusters: '0000,0001,0003,0B05,1000', outClusters: '0003,0004,0005,0006,0008,0019,1000', manufacturer: 'Candeo', model: 'C-ZB-SR5BR', deviceJoinName: 'Candeo C-ZB-SR5BR Smart Scene Remote'
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

private @Field final String CANDEO = 'Candeo C-ZB-SR5BR Device Driver'
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
    sendEvent(processEvent(name: 'numberOfButtons', value: 7, displayed: false))
    for (Integer buttonNumber : 1..7) {
        sendEvent(buttonAction('held', buttonNumber, 'digital'))
    }
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
    logDebug("battery percentage time is: ${batteryPercentageReportTime ?: '28800'}")
    Integer batteryTime = batteryPercentageReportTime ? batteryPercentageReportTime.toInteger() : 28800
    List<String> cmds = [
                        "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0001 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",
                        "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0021 ${DataType.UINT8} 3600 ${batteryTime} {${intTo16bitUnsignedHex(2)}}", "delay ${ZIGBEEDELAY}",
                        "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0001 {10 00 08 00 2100}", "delay ${ZIGBEEDELAY}",
                        "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0021 {}"
                        ]
    if (!(isZigbee30())) {
        logDebug('older zigbee version detected, binding endpoint')
        cmds += ["zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0xFF03 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",]
    }
    logDebug("sending ${cmds}")
    return cmds
}

void push(BigDecimal button) {
    logTrace('push called')
    buttonCommand('pushed', button.intValue())
}

void doubleTap(BigDecimal button) {
    logTrace('doubleTap called')
    buttonCommand('doubleTapped', button.intValue())
}

void hold(BigDecimal button) {
    logTrace('hold called')
    buttonCommand('held', button.intValue())
}

void release(BigDecimal button) {
    logTrace('release called')
    buttonCommand('released', button.intValue())
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
        else if (descriptionMap.cluster == 'FF03' || descriptionMap.clusterId == 'FF03' || descriptionMap.clusterInt == 65283) {
            processManufacturerSpecificCluster(descriptionMap, events)
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

private void processManufacturerSpecificCluster(Map descriptionMap, List<Map> events) {
    logTrace('processManufacturerSpecificCluster called')
    switch (descriptionMap.command) {
        case '01':
            logDebug('manufacturer specific (FF03) command (01)')
            List<String> commandData = descriptionMap.data
            logDebug("data is: ${commandData}")
            Integer buttonNumber = 0
            List<String> buttonActions = []
            if (commandData[0] == '01') {
                logDebug('type is button')
                Map<String,Integer> buttonNumbers = ['01': 1, '02': 2, '04': 3, '08': 4, '10': 5]
                String commandNumber = commandData[2]
                logDebug("commandNumber: ${commandNumber}")
                buttonNumber = buttonNumbers[commandNumber] ? buttonNumbers[commandNumber] : 0
                Map<String,String> buttonEvents = ['01': 'pushed', '02': 'doubleTapped', '03': 'held', '04': 'released']
                String commandEvent = commandData[3]
                logDebug("commandEvent: ${commandEvent}")
                buttonActions += buttonEvents[commandEvent] ? buttonEvents[commandEvent] : 'unknown'
            }
            else if (commandData[0] == '03') {
                logDebug('type is ring')
                Map<String,String> ringEvents = ['01': 'held', '02': 'released']
                String commandEvent = commandData[2]
                logDebug("commandEvent: ${commandEvent}")
                String ringEvent = ringEvents[commandEvent] ? ringEvents[commandEvent] : 'unknown'
                logDebug("ringEvent: ${ringEvent}")
                if (ringEvent == 'released') {
                    Integer previousButtonNumber = device.currentValue('held', true) ?: 0
                    if (previousButtonNumber != 0) {
                        logDebug("previous button number was ${previousButtonNumber}")
                        buttonNumber = previousButtonNumber
                    }
                    else {
                        logDebug('could not determine previous button number')
                    }
                }
                else {
                    Map<String,Integer> ringNumbers = ['01': 6, '02': 7]
                    String commandNumber = commandData[1]
                    logDebug("commandNumber: ${commandNumber}")
                    buttonNumber = ringNumbers[commandNumber] ? ringNumbers[commandNumber] : 0
                    String ringClicks = commandData[3]
                    logDebug("ringClicks: ${ringClicks}")
                    if (ringClicks != '00' && ringClicks != '01') {
                        Integer extraRingEvents = zigbee.convertHexToInt(ringClicks) - 1
                        logDebug("need to generate ${extraRingEvents} additional ring events")
                        for (Integer extraRingEvent : 1..extraRingEvents) {
                            logDebug("adding ${extraRingEvent} additional ring event")
                            buttonActions += ringEvent
                        }
                    }
                }
                buttonActions += ringEvent
            }
            else {
                logDebug('type is unknown')
            }
            if (buttonNumber != 0 && buttonActions != []) {
                logDebug("buttonNumber: ${buttonNumber}")
                logDebug("buttonActions: ${buttonActions}")
                buttonActions.each { String button ->
                    events.add(buttonAction(button, buttonNumber, 'physical'))
                }
            }
            else {
                logDebug('could not determine button number and button actions')
            }
            break
        default:
            logDebug('manufacturer specific (FF03) command skipped')
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

private Map buttonAction(String action, Integer button, String type) {
    logTrace("buttonAction called button: ${button} action: ${action} type: ${type}")
    String descriptionText = "${device.displayName} button ${button} is ${action}"
    logEvent(descriptionText)
    return processEvent([name: action, value: button, descriptionText: descriptionText, isStateChange: true, type: type])
}

private void buttonCommand(String action, Integer button) {
    logTrace("buttonCommand called button: ${button} action: ${action}")
    if (button >= 1 && button <= 3) {
        sendEvent(buttonAction(action, button, 'digital'))
    }
}

private boolean isZigbee30() {
    logTrace('isZigbee30 called')
    String model = getHubVersion()
    logDebug("model: ${model}")
    String revision = model.split('-').last()
    revision = revision.contains('Pro') ? '9' : revision
    logDebug("revision: ${revision}")
    return (Integer.parseInt(revision) >= 8)
}
