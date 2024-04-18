/**
 *    Candeo Modmote Zigbee Scene Switch
 *    Reports pushed, double tapped and held button events
 *    Supresses duplicate events received from device
 *    Has setting for debounce timer for filtering duplicate events
*/

metadata {
    definition(name: 'Candeo Modmote Zigbee Scene Switch', namespace: 'Candeo', author: 'Candeo', importUrl: 'https://github.com/candeosmart/hubitat-zigbee/blob/main/Candeo%20Modmote%20Zigbee%20Scene%20Switch.groovy', singleThreaded: true) {
        capability 'PushableButton'
        capability 'DoubleTapableButton'
        capability 'HoldableButton'
        capability 'Configuration'

        fingerprint profileId: '0104', endpointId: '01', inClusters: '0000,0001,0003,0004,0006,1000', outClusters: '0019,000A,0003,0004,0005,0006,0008,1000', manufacturer: '_TZ3000_czuyt8lz', model: 'TS004F', deviceJoinName: 'Candeo Modmote Zigbee Scene Switch'
        fingerprint profileId: '0104', endpointId: '01', inClusters: '0001,0003,0004,0006,1000,0000', outClusters: '0003,0004,0005,0006,0008,1000,0019,000A', manufacturer: '_TZ3000_b3mgfu0d', model: 'TS004F', deviceJoinName: 'Candeo Modmote Zigbee Scene Switch'
    }
    preferences {
        input name: 'deviceDriverOptions', type: 'hidden', title: '<strong>Device Driver Options</strong>', description: '<small>The following options change the behaviour of the device driver, they take effect after hitting "<strong>Save Preferences</strong> below."</small>'
        input name: 'debounceTimer', type: 'enum', title: 'Debounce Timer (s)', description: '<small>Helps to suppress duplicate events received from the device.  We think that the default of 2 seconds is the best option, but you can adjust it to suit your environment.</small><br><br>', options: PREFDEBOUNCETIMER, defaultValue: '2000'
        input name: 'loggingOption', type: 'enum', title: 'Logging Option', description: '<small>Sets the logging level cumulatively, for example "Driver Trace Logging" will include all logging levels below it.</small><br><br>', options: PREFLOGGING, defaultValue: '5'
        input name: 'platformOptions', type: 'hidden', title: '<strong>Platform Options</strong>', description: '<small>The following options are relevant to the Hubitat platform and UI itself.</small>'
    }
}

import groovy.transform.Field

private @Field final String CANDEO = 'Candeo Modmote Device Driver'
private @Field final Boolean DEBUG = false
private @Field final Integer LOGSOFF = 1800
private @Field final Map PREFFALSE = [value: 'false', type: 'bool']
private @Field final Map PREFTRUE = [value: 'true', type: 'bool']
private @Field final Map<String,Map> BUTTON_MAPPING = [ 'TS004F': ['numberOfButtons': 4,
                                                                   'endpointIds': [ '01': [ 'on': ['event': 'pushed', 'button': 1], 'off': ['event': 'doubleTapped', 'button': 1], 'toggle': ['event': 'held', 'button': 1] ],
                                                                                    '02': [ 'on': ['event': 'pushed', 'button': 2], 'off': ['event': 'doubleTapped', 'button': 2], 'toggle': ['event': 'held', 'button': 2] ],
                                                                                    '03': [ 'on': ['event': 'pushed', 'button': 3], 'off': ['event': 'doubleTapped', 'button': 3], 'toggle': ['event': 'held', 'button': 3] ],
                                                                                    '04': [ 'on': ['event': 'pushed', 'button': 4], 'off': ['event': 'doubleTapped', 'button': 4], 'toggle': ['event': 'held', 'button': 4] ] ] ] ]
private @Field final Integer DEBOUNCETIMER = 2000
private @Field final Map PREFDEBOUNCETIMER = ['0': '0s', '500': '0.5s', '1000': '1s', '1500': '1.5s', '2000': '2s', '3000': '3s', '4000': '4s', '5000': '5s']
private @Field final Map PREFLOGGING = ['0': 'Device Event Logging', '1': 'Driver Informational Logging', '2': 'Driver Warning Logging', '3': 'Driver Error Logging', '4': 'Driver Debug Logging', '5': 'Driver Trace Logging' ]

void installed() {
    logsOn()
    logTrace('installed called')
    device.updateSetting('debounceTimer', [value: '2000', type: 'enum'])
    logInfo("debounceTimer setting is: ${PREFDEBOUNCETIMER[debounceTimer]}")
    logInfo('logging level is: Driver Trace Logging')
    logInfo("logging level will reduce to Driver Error Logging after ${LOGSOFF} seconds")
    logDebug("modelNumberOfButtons: ${modelNumberOfButtons}")
    sendEvent(processEvent(name: 'numberOfButtons', value: modelNumberOfButtons, displayed: false))
    for (Integer buttonNumber : 1..modelNumberOfButtons) {
        sendEvent(buttonAction('pushed', buttonNumber, 'digital'))
        String buttonEvent = "button${buttonNumber}Event"
        state[buttonEvent] = 'standby'
        String buttonTimer = "button${buttonNumber}Timer"
        state[buttonTimer] = currentTimeStamp
    }
}

void uninstalled() {
    logTrace('uninstalled called')
    clearAll()
}

void initialize() {
    logTrace('initialize called')
}

void updated() {
    logTrace('updated called')
    logTrace("settings: ${settings}")
    logInfo("debounceTimer setting is: ${PREFDEBOUNCETIMER[debounceTimer]}", true)
    logInfo("logging level is: ${PREFLOGGING[loggingOption]}", true)
    String deviceMode = state['currentMode'] ?: 'unknown'
    clearAll()
    for (Integer buttonNumber : 1..modelNumberOfButtons) {
        String buttonEvent = "button${buttonNumber}Event"
        state[buttonEvent] = 'standby'
        String buttonTimer = "button${buttonNumber}Timer"
        state[buttonTimer] = currentTimeStamp
    }
    state['currentMode'] = deviceMode
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
    List<String> cmds = ["he raw 0x${device.deviceNetworkId} 1 0x${device.endpointId} 0x0000 {10 00 00 04 00 00 00 01 00 05 00 07 00 FE FF}", 'delay 200',
                         "he raw 0x${device.deviceNetworkId} 1 0x${device.endpointId} {10 00 00 04 80}", 'delay 50',
                         "he raw 0x${device.deviceNetworkId} 1 0x${device.endpointId} {10 00 00 11 D0}", 'delay 50',
                         "he wattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x8004 0x30 {01} {}", 'delay 50',
                         "he raw 0x${device.deviceNetworkId} 1 0x${device.endpointId} 0x0006 {10 00 00 04 80}", 'delay 50']
    logDebug("sending ${cmds}")
    return cmds
}

void push(BigDecimal button) {
    logTrace('push called')
    sendEvent(buttonAction('pushed', button.intValue(), 'digital'))
}

void doubleTap(BigDecimal button) {
    logTrace('doubleTap called')
    sendEvent(buttonAction('doubleTapped', button.intValue(), 'digital'))
}

void hold(BigDecimal button) {
    logTrace('hold called')
    sendEvent(buttonAction('held', button.intValue(), 'digital'))
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
        if (descriptionMap.cluster == '0006' || descriptionMap.clusterId == '0006' || descriptionMap.clusterInt == 6) {
            processSwitchEvent(descriptionMap, events)
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

private void processSwitchEvent(Map descriptionMap, List<Map> events) {
    logTrace('processSwitchEvent called')
    switch (descriptionMap.command) {
        case '01':
        case '0A':
            if (descriptionMap.attrId == '8004' || descriptionMap.attrInt == 32772) {
                logDebug('on off (0006) mode report (8004)')
                Integer modeValue = zigbee.convertHexToInt(descriptionMap.value)
                Map<Integer, String> modes = [0: 'dimming mode', 1: 'scene switch mode']
                String deviceMode = modes[modeValue]
                logDebug("device mode is currently set to: ${deviceMode}")
                state['currentMode'] = deviceMode
                if (deviceMode != 'scene switch mode') {
                    logInfo('********** your device mode is currently set to "dimmer mode", it wil now be switched to "scene mode" to work with this device driver **********', true)
                    List<String> cmds = ["he wattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x8004 0x30 {01} {}", 'delay 50',
                                         "he raw 0x${device.deviceNetworkId} 1 0x${device.endpointId} 0x0006 {10 00 00 04 80}", 'delay 50']
                    doZigBeeCommand(cmds)
                }
            }
            else {
                logDebug('on off (0006) attribute skipped')
            }
            break
        case '04':
            logDebug('on off (0006) write attribute response (04) skipped')
            break
        case '07':
            logDebug('on off (0006) configure reporting response (07) skipped')
            break
        case '0B':
            logDebug('on off (0006) default response (0B) skipped')
            break
        case 'FD':
            logDebug('on off (0006) switch command data (FD)')
            if (descriptionMap.sourceEndpoint && descriptionMap.data) {
                if (BUTTON_MAPPING && BUTTON_MAPPING[device.getDataValue('model')]) {
                    Map buttonMapping = BUTTON_MAPPING[device.getDataValue('model')]
                    if (buttonMapping['endpointIds']) {
                        Map<String,?> endpointIds = buttonMapping['endpointIds']
                        logTrace("using endpointIds: ${endpointIds}")
                        String endpointId = descriptionMap.sourceEndpoint
                        if (endpointIds[endpointId]) {
                            Map<String,?> commands = endpointIds[endpointId]
                            logTrace("using commands: ${commands}")
                            Map<Integer, String> switchCommandData = [0: 'on', 1: 'off', 2: 'toggle']
                            Integer commandData = zigbee.convertHexToInt(descriptionMap.data[0])
                            if (switchCommandData[commandData]) {
                                String switchCommand = switchCommandData[commandData]
                                if (commands[switchCommand]) {
                                    Map<String,?> commandDataMapping = commands[switchCommand]
                                    logTrace("commandDataMapping: ${commandDataMapping}")
                                    String event = commandDataMapping['event']
                                    Integer buttonNumber = commandDataMapping['button']
                                    logTrace("action: ${event}")
                                    if (state["button${buttonNumber}Event"] && state["button${buttonNumber}Timer"]) {
                                        logDebug("state is now: ${state}")
                                        String buttonEvent = state["button${buttonNumber}Event"]
                                        Long buttonTimer = Long.valueOf(state["button${buttonNumber}Timer"])
                                        logDebug("buttonEvent: ${buttonEvent}")
                                        logDebug("buttonTimer: ${buttonTimer}")
                                        Long timeDifference = currentTimeStamp - buttonTimer
                                        logDebug("timeDifference: ${timeDifference}")
                                        Integer debounceTime = debounceTimer ? debounceTimer.toInteger() : DEBOUNCETIMER
                                        logDebug("debounceTime: ${debounceTime}")
                                        if (buttonEvent != event || (buttonEvent == event && timeDifference >= debounceTime)) {
                                            logDebug('actioning this event!')
                                            events.add(buttonAction(event, buttonNumber, 'physical'))
                                        }
                                        else {
                                            logDebug('duplicate event received, skipping!')
                                        }
                                        state["button${buttonNumber}Event"] = event
                                        state["button${buttonNumber}Timer"] = currentTimeStamp
                                        state['currentMode'] = 'scene switch mode'
                                    }
                                    else {
                                        logWarn('could not retrieve state for button')
                                    }
                                }
                                else {
                                    logDebug("skipping switchCommand: ${switchCommand}")
                                }
                            }
                            else {
                                logDebug("skipping commandData: ${commandData}")
                            }
                        }
                        else {
                            logDebug("skipping endpointId: ${endpointId}")
                        }
                    }
                    else {
                        logWarn('BUTTON_MAPPING map not set correctly!')
                    }
                }
                else {
                    logWarn('BUTTON_MAPPING map not set correctly!')
                }
            }
            else {
                logWarn('descriptonMap did not contain sourceEndpoint or data fields!')
            }
            break
        default:
            logDebug('on off (0006) command skipped')
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

private Integer getModelNumberOfButtons() {
    logTrace('getModelNumberOfButtons called')
    Integer numberOfButtons = 1
    if (BUTTON_MAPPING && BUTTON_MAPPING[device.getDataValue('model')]) {
        Map buttonMapping = BUTTON_MAPPING[device.getDataValue('model')]
        numberOfButtons = buttonMapping['numberOfButtons']
    }
    else {
        logWarn('BUTTON_MAPPING map not set correctly!')
    }
    return numberOfButtons ?: 1
}

private Long getCurrentTimeStamp() {
    logTrace('getCurrentTimeStamp called')
    Long timeStamp = java.time.Instant.now().toEpochMilli()
    logDebug("currentTimeStamp: ${timeStamp}")
    return timeStamp
}

private Map buttonAction(String action, Integer button, String type) {
    logTrace("buttonAction called button: ${button} action: ${action} type: ${type}")
    String descriptionText = "${device.displayName} button ${button} is ${action}"
    logEvent(descriptionText)
    return processEvent([name: action, value: button, descriptionText: descriptionText, isStateChange: true, type: type])
}
