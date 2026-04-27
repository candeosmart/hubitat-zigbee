/**
 *    Candeo C-RFZB-HUB Kinetic RF To Zigbee Gateway
 *    Exposes up to 10 Kinetic RF switches as buttons
 *    Has setting for how many multi press events to detect and raise
 *    Has setting for the detection time window for detecting them
 */

metadata {
    definition(name: 'Candeo C-RFZB-HUB Kinetic RF To Zigbee Gateway', namespace: 'Candeo', author: 'Candeo', importUrl: 'TBA', singleThreaded: true) {
        capability 'PushableButton'
        capability 'DoubleTapableButton'

        attribute 'tripleTapped', 'number'
        attribute 'quadrupleTapped', 'number'
        attribute 'quintupleTapped', 'number'

        command 'push', [ [ name: 'Button number to push *', type: 'ENUM', constraints: BUTTONS ] ]
        command 'doubleTap', [ [ name: 'Button number to double tap *', type: 'ENUM', constraints: BUTTONS ] ]
        command 'tripleTap', [ [ name: 'Button number to triple tap *', type: 'ENUM', constraints: BUTTONS ] ]
        command 'quadrupleTap', [ [ name: 'Button number to quadruple tap *', type: 'ENUM', constraints: BUTTONS ] ]
        command 'quintupleTap', [ [ name: 'Button number to quintuple tap *', type: 'ENUM', constraints: BUTTONS ] ]
        command 'resetPreferencesToDefault'

        fingerprint profileId: '0104', endpointId: '01', inClusters: '0000,0006,0003,0004,0005', manufacturer: 'Candeo', model: 'C-RFZB-HUB', deviceJoinName: 'Candeo C-RFZB-HUB Kinetic RF To Zigbee Gateway'
    }
    preferences {
        input name: 'deviceDriverOptions', type: 'hidden', title: '<strong>Device Driver Options</strong>', description: '<small>The following options change the behaviour of the device driver, they take effect after hitting "<strong>Save Preferences</strong> below."</small>'
        input name: 'multiPressEventDetectionActions', type: 'enum', title: 'Multi Press Event Detection Actions', description: '<small>Create events for these actions.  Adjust to suit your requirements.</small><br><br>', options: PREFMULTIPRESSEVENTDETECTIONACTIONS, defaultValue: '1'
        input name: 'multiPressEventDetectionWindow', type: 'enum', title: 'Multi Press Event Detection Window (ms)', description: '<small>Detect multiple actions within this time window.  Adjust to suit your environment.</small><br><br>', options: PREFMULTIPRESSEVENTDETECTIONWINDOW, defaultValue: '500'
        input name: 'loggingOption', type: 'enum', title: 'Logging Option', description: '<small>Sets the logging level cumulatively, for example "Driver Trace Logging" will include all logging levels below it.</small><br><br>', options: PREFLOGGING, defaultValue: '5'
        input name: 'platformOptions', type: 'hidden', title: '<strong>Platform Options</strong>', description: '<small>The following options are relevant to the Hubitat platform and UI itself.</small>'
    }
}

import groovy.transform.Field

private @Field final String CANDEO = 'Candeo C-RFZB-HUB Device Driver'
private @Field final Boolean DEBUG = false
private @Field final Integer LOGSOFF = 1800
private @Field final Integer ZIGBEEDELAY = 200
private @Field final List<String> BUTTONS = [ '1', '2', '3', '4', '5', '6', '7', '8', '9', '10' ]
private @Field final Map<Integer, String> BUTTONEVENTS = [ 1: 'pushed', 2: 'doubleTapped', 3: 'tripleTapped', 4: 'quadrupleTapped', 5: 'quintupleTapped' ]
private @Field final Map PREFMULTIPRESSEVENTDETECTIONACTIONS = [ '1': 'single', '2': 'single & double', '3': 'single, double & triple', '4': 'single, double, triple & quadruple', '5': 'single, double, triple, quadruple & quintuple' ]
private @Field final Map PREFMULTIPRESSEVENTDETECTIONWINDOW = [ '200': '200ms', '250': '250ms', '300': '300ms', '350': '350ms', '400': '400ms', '450': '450ms', '500': '500ms', '550': '550ms', '600': '600ms', '650': '650ms', '700': '700ms', '750': '750ms', '800': '800ms', '850': '850ms', '900': '900ms', '950': '950ms', '1000': '1000ms', '1500': '1500ms', '2000': '2000ms', '2500': '2500ms', '3000': '3000ms' ]
private @Field final Map PREFLOGGING = [ '0': 'Device Event Logging', '1': 'Driver Informational Logging', '2': 'Driver Warning Logging', '3': 'Driver Error Logging', '4': 'Driver Debug Logging', '5': 'Driver Trace Logging' ]

void installed() {
    logTrace('installed called', true)
    setPreferencesToDefault()
    sendEvent(processEvent(name: 'numberOfButtons', value: 10, displayed: false))
    for (Integer buttonNumber : 1..10) {
        sendEvent(buttonAction('pushed', buttonNumber, 'digital'))
        state["button${buttonNumber}MultiPressEventDetectionClickCount"] = 0
    }
}

void uninstalled() {
    logTrace('uninstalled called')
    clearAll()
}

void updated() {
    logTrace('updated called')
    logTrace("settings: ${settings}")
    logInfo("multiPressEventDetectionActions setting is: ${PREFMULTIPRESSEVENTDETECTIONACTIONS[multiPressEventDetectionActions ?: '1']}", true)
    logInfo("multiPressEventDetectionWindow setting is: ${PREFMULTIPRESSEVENTDETECTIONWINDOW[multiPressEventDetectionWindow ?: '500']}", true)
    logInfo("logging level is: ${PREFLOGGING[loggingOption]}", true)
    clearAll()
    for (Integer buttonNumber : 1..10) {
        state["button${buttonNumber}MultiPressEventDetectionClickCount"] = 0
    }
    if (logMatch('debug')) {
        logInfo("logging level will reduce to Driver Error Logging after ${LOGSOFF} seconds", true)
        runIn(LOGSOFF, logsOff)
    }
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

void resetPreferencesToDefault() {
    logTrace('resetPreferencesToDefault called')
    setPreferencesToDefault()
}

void push(String button) {
    logTrace('push called')
    buttonCommand('pushed', button.toInteger())
}

void doubleTap(String button) {
    logTrace('doubleTap called')
    buttonCommand('doubleTapped', button.toInteger())
}

void tripleTap(String button) {
    logTrace('tripleTap called')
    buttonCommand('tripleTapped', button.toInteger())
}

void quadrupleTap(String button) {
    logTrace('quadrupleTap called')
    buttonCommand('quadrupleTapped', button.toInteger())
}

void quintupleTap(String button) {
    logTrace('quintupleTap called')
    buttonCommand('quintupleTapped', button.toInteger())
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

void delayedButton1Event(Map data) {
    delayedButtonEvent(data)
}

void delayedButton2Event(Map data) {
    delayedButtonEvent(data)
}

void delayedButton3Event(Map data) {
    delayedButtonEvent(data)
}

void delayedButton4Event(Map data) {
    delayedButtonEvent(data)
}

void delayedButton5Event(Map data) {
    delayedButtonEvent(data)
}

void delayedButton6Event(Map data) {
    delayedButtonEvent(data)
}

void delayedButton7Event(Map data) {
    delayedButtonEvent(data)
}

void delayedButton8Event(Map data) {
    delayedButtonEvent(data)
}

void delayedButton9Event(Map data) {
    delayedButtonEvent(data)
}

void delayedButton10Event(Map data) {
    delayedButtonEvent(data)
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

private void delayedButtonEvent(Map data) {
    logTrace("delayedButtonEvent called data: ${data}")
    String buttonMultiPressEventDetectionClickCount = "button${data.buttonNumber}MultiPressEventDetectionClickCount"
    Integer buttonClickCount = (state[buttonMultiPressEventDetectionClickCount]).toInteger()
    logDebug("buttonClickCount: ${buttonClickCount}")
    state[buttonMultiPressEventDetectionClickCount] = 0
    Integer multiPressEventDetectionAction = multiPressEventDetectionActions ? multiPressEventDetectionActions.toInteger() : 1
    logDebug("multiPressEventDetectionAction: ${multiPressEventDetectionAction}")
    if (buttonClickCount <= multiPressEventDetectionAction) {
        String buttonEvent = BUTTONEVENTS[buttonClickCount] ? BUTTONEVENTS[buttonClickCount] : 'unknown'
        if (buttonEvent != 'unknown') {
            sendEvent(buttonAction(buttonEvent, data.buttonNumber, 'physical'))
        }
    }
    else {
        logWarn("device driver preference disables events for this combination, change the preference if you wish for events to be raised for it - buttonClickCount: ${buttonClickCount} multiPressEventDetectionActions: ${PREFMULTIPRESSEVENTDETECTIONACTIONS[multiPressEventDetectionActions ?: '1']}")
    }
}

private void processSwitchEvent(Map descriptionMap, List<Map> events) {
    logTrace('processSwitchEvent called')
    switch (descriptionMap.command) {
        case '0A':
        case '01':
            if (descriptionMap.attrId == '0000' || descriptionMap.attrInt == 0) {
                logDebug('on off (0006) on off report (0000)')
                Integer onOffValue = zigbee.convertHexToInt(descriptionMap.value)
                if (onOffValue == 1 || onOffValue == 0) {
                    logDebug("on off report is ${onOffValue}")
                    String endpoint = descriptionMap.sourceEndpoint ?: descriptionMap.endpoint ?: 'unknown'
                    if (endpoint != 'unknown') {
                        logDebug("endpoint is ${endpoint}")
                        Integer buttonNumber = zigbee.convertHexToInt(endpoint)
                        logDebug("buttonNumber: ${buttonNumber}")
                        String buttonMultiPressEventDetectionClickCount = "button${buttonNumber}MultiPressEventDetectionClickCount"
                        if (state[buttonMultiPressEventDetectionClickCount] != null) {
                            Integer buttonClickCount = (state[buttonMultiPressEventDetectionClickCount]).toInteger()
                            logDebug("current buttonClickCount: ${buttonClickCount}")
                            buttonClickCount++
                            logDebug("new buttonClickCount: ${buttonClickCount}")
                            state[buttonMultiPressEventDetectionClickCount] = buttonClickCount
                            Integer multiPressEventDetectionAction = multiPressEventDetectionActions ? multiPressEventDetectionActions.toInteger() : 1
                            logDebug("multiPressEventDetectionAction: ${multiPressEventDetectionAction}")
                            if (multiPressEventDetectionAction == 1) {
                                logDebug('multi press event detection limited to single by setting or default, skipping timer!')
                                delayedButtonEvent(['buttonNumber': buttonNumber])
                            }
                            else {
                                Integer multiPressEventDetectionTime = multiPressEventDetectionWindow ? multiPressEventDetectionWindow.toInteger() : 500
                                logDebug("multiPressEventDetectionTime: ${multiPressEventDetectionTime}")
                                runInMillis(multiPressEventDetectionTime, "delayedButton${buttonNumber}Event", [data: ['buttonNumber': buttonNumber]])
                            }
                        }
                        else {
                            logWarn('could not retrieve state for button')
                        }
                    }
                    else {
                        logDebug('endpoint not available!')
                    }
                }
                else {
                    logDebug("skipping onOffValue: ${onOffValue}")
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
    return loggingOption ? loggingOption.toInteger() >= logLevels[logLevel].toInteger() : true
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
private void setPreferencesToDefault() {
    logsOn()
    logTrace('setPreferencesToDefault called')
    settings.keySet().each { String setting ->
        device.removeSetting(setting)
    }
    device.updateSetting('multiPressEventDetectionActions', [value: '1', type: 'enum'])
    logInfo("multiPressEventDetectionActions setting is: ${PREFMULTIPRESSEVENTDETECTIONACTIONS[multiPressEventDetectionActions]}")
    device.updateSetting('multiPressEventDetectionWindow', [value: '500', type: 'enum'])
    logInfo("multiPressEventDetectionWindow setting is: ${PREFMULTIPRESSEVENTDETECTIONWINDOW[multiPressEventDetectionWindow]}")
    logInfo('logging level is: Driver Trace Logging')
    logInfo("logging level will reduce to Driver Error Logging after ${LOGSOFF} seconds")
}
private Map buttonAction(String action, Integer button, String type) {
    logTrace("buttonAction called button: ${button} action: ${action} type: ${type}")
    String descriptionText = "${device.displayName} button ${button} is ${action}"
    logEvent(descriptionText)
    return processEvent([name: action, value: button, descriptionText: descriptionText, isStateChange: true, type: type])
}

private void buttonCommand(String action, Integer button) {
    logTrace("buttonCommand called button: ${button} action: ${action}")
    if (button >= 1 && button <= 10) {
        sendEvent(buttonAction(action, button, 'digital'))
    }
}
