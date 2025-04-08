/**
 *    Candeo C-RFZB-SM1 Zigbee & RF Switch Module
 *    Supports on / off / flash
 *    Reports switch events
 *    Has Setting For Flash Time
 *    Has Setting For Flash Timeout
 *    Has Setting For Power On Default
 *    Has Setting For Explicit State After Hub Startup
 */

metadata {
    definition(name: 'Candeo C-RFZB-SM1 Zigbee & RF Switch Module', namespace: 'Candeo', author: 'Candeo', importUrl: '', singleThreaded: true) {
        capability 'Switch'
        capability 'Flash'
        capability 'Actuator'
        capability 'Initialize'
        capability 'Refresh'
        capability 'Configuration'

        fingerprint profileId: '0104', endpointId: '01', inClusters: '0000,0006,0003,0004,0005,E001', outClusters: '000A', manufacturer: 'Candeo', model: 'C-RFZB-SM1', deviceJoinName: 'Candeo C-RFZB-SM1 Zigbee & RF Switch Module'
        fingerprint profileId: '0104', endpointId: '01', inClusters: '0000,0003,0004,0005,0006,E001', outClusters: '000A', manufacturer: 'Candeo', model: 'C-RFZB-SM1', deviceJoinName: 'Candeo C-RFZB-SM1 Zigbee & RF Switch Module'
    }
    preferences {
        input name: 'deviceDriverOptions', type: 'hidden', title: '<strong>Device Driver Options</strong>', description: '<small>The following options change the behaviour of the device driver, they take effect after hitting "<strong>Save Preferences</strong> below."</small>'
        input name: 'flashTime', type: 'enum', title: 'Flash Time (ms)', description: '<small>When carrying out a flash command, use this as the on and off time.</small><br><br>', options: PREFFLASHTIME, defaultValue: '750'
        input name: 'flashTimeout', type: 'enum', title: 'Flash Timeout (m)', description: '<small>When carrying out a flash command, automatically cancel after this amount of time.</small><br><br>', options: PREFFLASHTIMEOUT, defaultValue: '10'
        input name: 'hubStartupDefaultCommand', type: 'enum', title: 'Explicit Command After Hub Has Restarted', description: '<small>After the hub restarts, carry out this command on the device.</small><br><br>', options: PREFHUBRESTART, defaultValue: 'refresh'
        input name: 'loggingOption', type: 'enum', title: 'Logging Option', description: '<small>Sets the logging level cumulatively, for example "Driver Trace Logging" will include all logging levels below it.</small><br><br>', options: PREFLOGGING, defaultValue: '5'
        input name: 'deviceConfigurationOptions', type: 'hidden', title: '<strong>Device Configuration Options</strong>', description: '<small>The following options change the behaviour of the device itself, they take effect after hitting "<strong>Save Preferences</strong> below", followed by "<strong>Configure</strong>" above.<br><br>For a battery powered device, you may also need to wake it up manually!</small>'
        input name: 'powerOnDefault', type: 'enum', title: 'Default State After Return From Power Failure', description: '<small>After a power failure, set the device to this state when the power is restored.</small><br><br>', options: PREFPOWERON, defaultValue: 'previous'
        input name: 'platformOptions', type: 'hidden', title: '<strong>Platform Options</strong>', description: '<small>The following options are relevant to the Hubitat platform and UI itself.</small>'
    }
}

import groovy.transform.Field

private @Field final String CANDEO = 'Candeo C-RFZB-SM1 Device Driver'
private @Field final Boolean DEBUG = false
private @Field final Integer LOGSOFF = 1800
private @Field final Map PREFFALSE = [value: 'false', type: 'bool']
private @Field final Map PREFTRUE = [value: 'true', type: 'bool']
private @Field final Map PREFPOWERON = [ 'off': 'Off', 'on': 'On', 'opposite': 'Opposite', 'previous': 'Previous' ]
private @Field final Map PREFHUBRESTART = [ 'off': 'Off', 'on': 'On', 'refresh': 'Refresh State Only', 'nothing': 'Do Nothing' ]
private @Field final Map PREFFLASHTIME = ['500': '500ms', '750': '750ms', '1000': '1000ms', '1500': '1500ms', '2000': '2000ms', '2500': '2500ms', '3000': '3000ms', '4000': '4000ms', '5000': '5000ms']
private @Field final Map PREFFLASHTIMEOUT = ['0': 'never', '1': '1m', '2': '2m', '3': '3m', '4': '4m', '5': '5m', '10': '10m', '15': '15m', '30': '30m', '60': '60m', '90': '90m', '120': '120m', '180': '180m']
private @Field final Map PREFLOGGING = ['0': 'Device Event Logging', '1': 'Driver Informational Logging', '2': 'Driver Warning Logging', '3': 'Driver Error Logging', '4': 'Driver Debug Logging', '5': 'Driver Trace Logging' ]

void installed() {
    logsOn()
    logTrace('installed called')
    device.updateSetting('powerOnDefault', [value: 'previous', type: 'enum'])
    logInfo("powerOnDefault setting is: ${PREFPOWERON[powerOnDefault]}")
    device.updateSetting('hubStartupDefaultCommand', [value: 'refresh', type: 'enum'])
    logInfo("hubStartupDefaultCommand setting is: ${PREFHUBRESTART[hubStartupDefaultCommand]}")
    device.updateSetting('flashTime', [value: '750', type: 'enum'])
    logInfo("flashTime setting is: ${PREFFLASHTIME[flashTime]}")
    device.updateSetting('flashTimeout', [value: '10', type: 'enum'])
    logInfo("flashTimeout setting is: ${PREFFLASHTIMEOUT[flashTimeout]}")
    logInfo('logging level is: Driver Trace Logging')
    logInfo("logging level will reduce to Driver Error Logging after ${LOGSOFF} seconds")
}

void uninstalled() {
    logTrace('uninstalled called')
    clearAll()
}

void initialize() {
    logTrace('initialize called')
    String startupDefaultCommand = hubStartupDefaultCommand ?: 'refresh'
    switch (startupDefaultCommand) {
        case 'off':
            doZigBeeCommand(off())
            break
        case 'on':
            doZigBeeCommand(on())
            break
        case 'refresh':
            doZigBeeCommand(refresh())
            break
        default:
            break
    }
}

void updated() {
    logTrace('updated called')
    logTrace("settings: ${settings}")
    logInfo("powerOnDefault setting is: ${PREFPOWERON[powerOnDefault ?: 'previous']}", true)
    logInfo("hubStartupDefaultCommand setting is: ${PREFHUBRESTART[hubStartupDefaultCommand ?: 'refresh']}", true)
    logInfo("flashTime setting is: ${PREFFLASHTIME[flashTime ?: '750']}", true)
    logInfo("flashTimeout setting is: ${PREFFLASHTIMEOUT[flashTimeout ?: '10']}", true)
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
    Map<String, String> startUpOnOff = ['on': 0x01, 'off': 0x0, 'opposite': 0x02, 'previous': 0x03]
    List<String> cmds = ["zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}", 'delay 2000',
                         "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x0000 ${DataType.BOOLEAN} 0 3600 {}", 'delay 2000',
                         "he wattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x4003 0x30 {${startUpOnOff[powerOnDefault ?: 'previous']}} {}", 'delay 2000',
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x4003 {}", 'delay 2000']
    logDebug("sending ${cmds}")
    return cmds
}

List<String> refresh() {
    logTrace('refresh called')
    List<String> cmds = ["he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x0000 {}", 'delay 2000',
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x4003 {}", 'delay 2000']
    logDebug("sending ${cmds}")
    return cmds
}

List<String> on() {
    logTrace('on called')
    flashStop(false)
    List<String> cmds = ["he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 1 {}"]
    logDebug("sending ${cmds}")
    state['action'] = 'digitalon'
    return cmds
}

List<String> off() {
    logTrace('off called')
    flashStop(false)
    List<String> cmds = ["he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}"]
    logDebug("sending ${cmds}")
    state['action'] = 'digitaloff'
    return cmds
}

void flash(BigDecimal rate = null) {
    logTrace("flash called rate: ${rate ?: 'no rate specified'}")
    if (state['flashing']) {
        logDebug('state["flashing"] is true, stopping flasher')
        state['flashing'] = false
        flashStop()
    }
    else {
        logDebug('state["flashing"] is false, starting flasher')
        state['flashing'] = true
        String currentState = device.currentValue('switch')
        logDebug("device state is currently: ${currentState}")
        state['flashPrevious'] = currentState == 'on' ?: false
        Integer flashRate = rate
        if (!flashRate) {
            logDebug("no rate specified, using flashTime: ${flashTime ?: '750'}")
            flashRate = flashTime ? flashTime.toInteger() : 750
        }
        logDebug("flashRate: ${flashRate}")
        if (flashRate < 500) {
            logWarn('flashRate is lower than 500ms, resetting to safe value')
            flashRate = 500
        }
        runInMillis(flashRate, flasher, [data: ['on': !(state['flashPrevious']), 'rate': flashRate]])
        logDebug("flashTimeout: ${flashTimeout ?: '10'}")
        Integer flashEnd = (flashTimeout ? flashTimeout.toInteger() : 10) * 60 * 1000
        logDebug("flashEnd: ${flashEnd}")
        if (flashEnd > 0) {
            logDebug('setting flashing timeout')
            runInMillis(flashEnd, flashStop)
        }
        else {
            logDebug('no timeout requested')
        }
    }
}

void flasher(Map data) {
    logTrace("flasher called data: ${data}")
    if (state['flashing']) {
        String cmd = '1'
        String action = 'digitalon'
        if (data.on) {
            logDebug('turning on')
        }
        else {
            logDebug('turning off')
            cmd = '0'
            action = 'digitaloff'
        }
        state['action'] = action
        doZigBeeCommand(["he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 ${cmd} {}"])
        runInMillis(data.rate, flasher, [data: ['on': !(data.on), 'rate': data.rate]])
    }
    else {
        logDebug('state["flashing"] is false, skipping!')
    }
}

void flashStop(Boolean reinstate = true) {
    logTrace('flashStop called')
    state['flashing'] = false
    unschedule('flasher')
    unschedule('flashStop')
    if (reinstate) {
        logDebug("reinstate is true, reinstating device previous state: ${state['flashPrevious'] ? 'on' : 'off'}")
        String cmd = '1'
        String action = 'digitalon'
        if (state['flashPrevious']) {
            logDebug('reinstate device to on')
        }
        else {
            logDebug('reinstate device to off')
            cmd = '0'
            action = 'digitaloff'
        }
        state['action'] = action
        doZigBeeCommand(["he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 ${cmd} {}"])
    }
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
        case '0A':
        case '01':
            if (descriptionMap.attrId == '0000' || descriptionMap.attrInt == 0) {
                logDebug('on off (0006) on off report (0000)')
                Integer onOffValue = zigbee.convertHexToInt(descriptionMap.value)
                if (onOffValue == 1 || onOffValue == 0) {
                    logDebug("on off report is ${onOffValue}")
                    String onOffState = onOffValue == 0 ? 'off' : 'on'
                    String descriptionText = "${device.displayName} was turned ${onOffState}"
                    String currentValue = device.currentValue('switch') ?: 'unknown'
                    if (onOffState == currentValue) {
                        descriptionText = "${device.displayName} is ${onOffState}"
                    }
                    String type = 'physical'
                    String action = state['action'] ?: 'standby'
                    if (action == 'digitalon' || action == 'digitaloff') {
                        logDebug("action is ${action}")
                        type = 'digital'
                        state['action'] = 'standby'
                        logDebug('action set to standby')
                    }
                    logEvent(descriptionText)
                    events.add(processEvent([name: 'switch', value: onOffState, type: type, descriptionText: descriptionText]))
                }
                else {
                    logDebug("skipping onOffValue: ${onOffValue}")
                }
            }
            else if (descriptionMap.attrId == '4003' || descriptionMap.attrInt == 16387) {
                logDebug('on off (0006) startup on off report (4003)')
                String startUpOnOffValue = descriptionMap.value
                Map<String, String> startUpOnOff = ['00': 'off', '01': 'on', '02': 'opposite', '03': 'previous', 'FF': 'previous']
                String startupBehaviour = PREFPOWERON[startUpOnOff[startUpOnOffValue]] ?: "${startUpOnOffValue} (unknown}"
                logDebug("powerOnDefault is currently set to: ${PREFPOWERON[powerOnDefault ?: 'previous']} and device reports it is set to: ${startupBehaviour}")
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
