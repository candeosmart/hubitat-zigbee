/**
 *    Candeo C-ZB-DM201-2G Zigbee 2-Gang Dimmer Module Child Switch
 *    Supports Momentary Switches
 *    Supports on / off / setLevel / startLevelChange / stopLevelChange / flash
 *    Reports switch / level events
 *    Has Setting For Level Transition Time (use device setting, as fast as possible or set an explicit time)
 *    Has Setting For Level Change Rate (as fast as possible or set an explicit rate)
 *    Has Setting For Flash Time
 *    Has Setting For Flash Timeout
 *    Has Setting For Power On Default
 *    Has Setting For Power On Default Level
 *    Has Setting For Default On Level
 *    Has Setting For Explicit State After Hub Startup
 */

metadata {
    definition(name: 'Candeo C-ZB-DM201-2G Zigbee 2-Gang Dimmer Module Child Switch', namespace: 'Candeo', author: 'Candeo', component: true, importUrl: 'https://raw.githubusercontent.com/candeosmart/hubitat-zigbee/refs/heads/main/Candeo%20C-ZB-DM201-2G%20Zigbee%202-Gang%20Dimmer%20Module%20Child%20Switch.groovy', singleThreaded: true) {
        capability 'Switch'
        capability 'SwitchLevel'
        capability 'ChangeLevel'
        capability 'Flash'
        capability 'Actuator'
        capability 'Initialize'
        capability 'Refresh'
        capability 'Configuration'

        command 'resetPreferencesToDefault'
    }
    preferences {
        input name: 'deviceDriverOptions', type: 'hidden', title: '<strong>Device Driver Options</strong>', description: '<small>The following options change the behaviour of the device driver, they take effect after hitting "<strong>Save Preferences</strong> below."</small>'
        input name: 'levelTransitionTime', type: 'enum', title: 'Transition Time (s)', description: '<small>When setting the device to a specific level, use this as the default transition time if one is not specified explicitly.</small><br><br>', options: PREFLEVELTRANSITIONTIME, defaultValue: 'device'
        input name: 'levelChangeRate', type: 'enum', title: 'Level Change Rate (%)', description: '<small>When carrying out a start level change command, move at this percentage per second.</small><br><br>', options: PREFLEVELCHANGERATE, defaultValue: 'none'
        input name: 'flashTime', type: 'enum', title: 'Flash Time (ms)', description: '<small>When carrying out a flash command, use this as the on and off time.</small><br><br>', options: PREFFLASHTIME, defaultValue: '750'
        input name: 'flashTimeout', type: 'enum', title: 'Flash Timeout (m)', description: '<small>When carrying out a flash command, automatically cancel after this amount of time.</small><br><br>', options: PREFFLASHTIMEOUT, defaultValue: '10'
        input name: 'hubStartupDefaultCommand', type: 'enum', title: 'Explicit Command After Hub Has Restarted', description: '<small>After the hub restarts, carry out this command on the device.</small><br><br>', options: PREFHUBRESTART, defaultValue: 'refresh'
        input name: 'loggingOption', type: 'enum', title: 'Logging Option', description: '<small>Sets the logging level cumulatively, for example "Driver Trace Logging" will include all logging levels below it.</small><br><br>', options: PREFLOGGING, defaultValue: '5'
        input name: 'deviceConfigurationOptions', type: 'hidden', title: '<strong>Device Configuration Options</strong>', description: '<small>The following options change the behaviour of the device itself, they take effect after hitting "<strong>Save Preferences</strong> below", followed by "<strong>Configure</strong>" above.<br><br>For a battery powered device, you may also need to wake it up manually!</small>'
        input name: 'deviceConfigDefaultPowerOnBehaviour', type: 'enum', title: 'Default State After Return From Power Failure', description: '<small>After a power failure, set the device to this state when the power is restored.</small><br><br>', options: PREFPOWERON, defaultValue: 'previous'
        input name: 'deviceConfigDefaultPowerOnLevel', type: 'enum', title: 'Default Level After Return From Power Failure', description: '<small>After a power failure, set the device to this level when the power is restored.</small><br><br>', options: PREFLEVEL, defaultValue: 'previous'
        input name: 'deviceConfigDefaultOnLevel', type: 'enum', title: 'Default Level When Turned On', description: '<small>When turned on, go immediately to this level.</small><br><br>', options: PREFLEVEL, defaultValue: 'previous'
        input name: 'platformOptions', type: 'hidden', title: '<strong>Platform Options</strong>', description: '<small>The following options are relevant to the Hubitat platform and UI itself.</small>'
    }
}

import groovy.transform.Field

private @Field final String CANDEO = 'Candeo C-ZB-DM201-2G Device Driver'
private @Field final Boolean DEBUG = false
private @Field final Integer LOGSOFF = 1800
private @Field final Integer DEVICEMINLEVEL = 1
private @Field final Integer DEVICEMAXLEVEL = 254
private @Field final Map PREFFALSE = [value: 'false', type: 'bool']
private @Field final Map PREFTRUE = [value: 'true', type: 'bool']
private @Field final Map PREFPREVIOUS = [value: 'previous', type: 'enum']
private @Field final Map PREFDEVICE = [value: 'device', type: 'enum']
private @Field final Map PREFNONE = [value: 'none', type: 'enum']
private @Field final Map PREF1000 = [value: '1000', type: 'enum']
private @Field final Map PREF10 = [value: '10', type: 'enum']
private @Field final Map PREF5 = [value: '5', type: 'enum']
private @Field final Map PREFPOWERON = [ 'off': 'Off', 'on': 'On', 'opposite': 'Opposite', 'previous': 'Previous' ]
private @Field final Map PREFLEVEL = ['5': '5%', '10': '10%', '15': '15%', '20': '20%', '25': '25%', '30': '30%', '35': '35%', '40': '40%', '45': '45%', '50': '50%', '55': '55%', '65': '65%', '70': '70%', '75': '75%', '80': '85%', '90': '95%', '100': '100%', 'previous': 'Previous']
private @Field final Map PREFHUBRESTART = [ 'off': 'Off', 'on': 'On', 'refresh': 'Refresh State Only', 'nothing': 'Do Nothing' ]
private @Field final Map PREFLEVELTRANSITIONTIME = ['device': 'use device setting', 'none': 'as fast as possible', '500': '0.5s', '1000': '1s', '1500': '1.5s', '2000': '2s', '2500': '2.5s', '3000': '3s', '3500': '3.5s', '4000': '4s', '4500': '4.5s', '5000': '5s']
private @Field final Map PREFLEVELCHANGERATE = ['none': 'as fast as possible', '1': '1%', '2': '2%', '3': '3%', '4': '4%', '5': '5%', '6': '6%', '7': '6%', '8': '8%', '9': '9%', '10': '10%', '15': '15%', '20': '20%', '25': '25%', '30': '30%', '35': '35%', '40': '40%', '45': '45%', '50': '50%']
private @Field final Map PREFTRANSITIONTIME = ['none': 'as fast as possible', '500': '0.5s', '1000': '1s', '1500': '1.5s', '2000': '2s', '2500': '2.5s', '3000': '3s', '3500': '3.5s', '4000': '4s', '4500': '4.5s', '5000': '5s', '5500': '5.5s', '6000': '6s', '6500': '6.5s', '7000': '7s', '7500': '7.5s', '8000': '8s', '8500': '8.5s', '9000': '9s', '9500': '9.5s', '10000': '10s']
private @Field final Map PREFFLASHTIME = ['500': '500ms', '750': '750ms', '1000': '1000ms', '1500': '1500ms', '2000': '2000ms', '2500': '2500ms', '3000': '3000ms', '4000': '4000ms', '5000': '5000ms']
private @Field final Map PREFFLASHTIMEOUT = ['0': 'never', '1': '1m', '2': '2m', '3': '3m', '4': '4m', '5': '5m', '10': '10m', '15': '15m', '30': '30m', '60': '60m', '90': '90m', '120': '120m', '180': '180m']
private @Field final Map PREFLOGGING = ['0': 'Device Event Logging', '1': 'Driver Informational Logging', '2': 'Driver Warning Logging', '3': 'Driver Error Logging', '4': 'Driver Debug Logging', '5': 'Driver Trace Logging' ]

void installed() {
    logTrace('installed called', true)
    resetPreferencesToDefault()
}

void resetPreferencesToDefault() {
    logsOn()
    logTrace('resetPreferencesToDefault called')
    settings.keySet().each { String setting ->
        device.removeSetting(setting)
    }
    device.updateSetting('deviceConfigDefaultPowerOnBehaviour', PREFPREVIOUS)
    logInfo("deviceConfigDefaultPowerOnBehaviour setting is: ${PREFPOWERON[deviceConfigDefaultPowerOnBehaviour]}")
    device.updateSetting('deviceConfigDefaultPowerOnLevel', PREFPREVIOUS)
    logInfo("deviceConfigDefaultPowerOnLevel setting is: ${PREFLEVEL[deviceConfigDefaultPowerOnLevel]}")
    device.updateSetting('deviceConfigDefaultOnLevel', PREFPREVIOUS)
    logInfo("deviceConfigDefaultOnLevel setting is: ${PREFLEVEL[deviceConfigDefaultOnLevel]}")
    device.updateSetting('hubStartupDefaultCommand', [value: 'refresh', type: 'enum'])
    logInfo("hubStartupDefaultCommand setting is: ${PREFHUBRESTART[hubStartupDefaultCommand]}")
    device.updateSetting('levelTransitionTime', PREFDEVICE)
    logInfo("levelTransitionTime setting is: ${PREFLEVELTRANSITIONTIME[levelTransitionTime]}")
    device.updateSetting('levelChangeRate', PREFNONE)
    logInfo("levelChangeRate setting is: ${PREFLEVELCHANGERATE[levelChangeRate]}")
    device.updateSetting('flashTime', [value: '750', type: 'enum'])
    logInfo("flashTime setting is: ${PREFFLASHTIME[flashTime]}")
    device.updateSetting('flashTimeout', PREF10)
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
            off()
            break
        case 'on':
            on()
            break
        case 'refresh':
            refresh()
            break
        default:
            break
    }
}

List<String> updated() {
    logTrace('updated called')
    logTrace("settings: ${settings}")
    logInfo("deviceConfigDefaultPowerOnBehaviour setting is: ${PREFPOWERON[deviceConfigDefaultPowerOnBehaviour ?: 'previous']}", true)
    logInfo("deviceConfigDefaultPowerOnLevel setting is: ${PREFLEVEL[deviceConfigDefaultPowerOnLevel ?: 'previous']}", true)
    logInfo("deviceConfigDefaultOnLevel setting is: ${PREFLEVEL[deviceConfigDefaultOnLevel ?: 'previous']}", true)
    logInfo("hubStartupDefaultCommand setting is: ${PREFHUBRESTART[hubStartupDefaultCommand ?: 'refresh']}", true)
    logInfo("levelTransitionTime setting is: ${PREFLEVELTRANSITIONTIME[levelTransitionTime ?: 'device']}", true)
    logInfo("levelChangeRate setting is: ${PREFLEVELCHANGERATE[levelChangeRate ?: 'none']}", true)
    logInfo("flashTime setting is: ${PREFFLASHTIME[flashTime ?: '750']}", true)
    logInfo("flashTimeout setting is: ${PREFFLASHTIMEOUT[flashTimeout ?: '10']}", true)
    logInfo("logging level is: ${PREFLOGGING[loggingOption]}", true)
    clearAll()
    if (logMatch('debug')) {
        logInfo("logging level will reduce to Driver Error Logging after ${LOGSOFF} seconds", true)
        runIn(LOGSOFF, logsOff)
    }
    if (checkPreferences()) {
        logInfo('Device Configuration Options have been changed, will now configure the device!', true)
        return configure()
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

void configure() {
    logTrace('configure called')
    parent?.componentConfigure(this.device)
}

void refresh() {
    logTrace('refresh called')
    parent?.componentRefresh(this.device)
}

void on() {
    logTrace('on called')
    flashStop(false)
    parent?.componentOn(this.device)
    state['action'] = 'digitalon'
}

void off() {
    logTrace('off called')
    flashStop(false)
    parent?.componentOff(this.device)
    state['action'] = 'digitaloff'
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
        if (flashRate > 5000 || flashRate < 500) {
            flashRate = flashRate > 5000 ? 5000 : flashRate < 500 ? 500 : flashRate
            logWarn('flashRate outside safe range (500 - 5000), resetting to safe value!')
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
        String action = 'digitalon'
        if (data.on) {
            logDebug('turning on')
            parent?.componentOn(this.device)
        }
        else {
            logDebug('turning off')
            parent?.componentOff(this.device)
            action = 'digitaloff'
        }
        state['action'] = action
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
        String action = 'digitalon'
        if (state['flashPrevious']) {
            logDebug('reinstate device to on')
            parent?.componentOn(this.device)
        }
        else {
            logDebug('reinstate device to off')
            parent?.componentOff(this.device)
            action = 'digitaloff'
        }
        state['action'] = action
    }
}

void setLevel(BigDecimal level) {
    logTrace("setLevel called level: ${level}")
    logDebug("levelTransitionTime: ${levelTransitionTime ?: 'device'}")
    BigDecimal levelTime = levelTransitionTime ? levelTransitionTime == 'none' ? 0 : levelTransitionTime == 'device' ? 65535 : levelTransitionTime.toBigDecimal() / 1000 : 65535
    setLevel(level, levelTime)
}

void setLevel(BigDecimal level, BigDecimal transition) {
    logTrace("setLevel called level: ${level} rate: ${transition}")
    Integer scaledTransition = transition == 0 ? 0 : transition == 65535 ? 65535 : (transition * 10).toInteger()
    logDebug("scaledTransition: ${scaledTransition}")
    Integer scaledLevel = percentageValueToLevel(level)
    logDebug("scaledLevel: ${scaledLevel}")
    parent?.componentSetLevel(this.device, scaledLevel, scaledTransition)
    state['action'] = 'digitalsetlevel'
}

void startLevelChange(String direction) {
    logTrace("startLevelChange called direction: ${direction}")
    Integer upDown = direction == 'down' ? 1 : 0
    logDebug("upDown: ${upDown} levelChangeRate: ${levelChangeRate ?: 'device'}")
    Integer scaledRate = levelChangeRate ? levelChangeRate == 'none' ? 254 : levelChangeRate == 'device' ? 255 : percentageValueToLevel(levelChangeRate.toBigDecimal()) : 255
    logDebug("scaledRate: ${scaledRate}")
    parent?.componentStartLevelChange(this.device, upDown, scaledRate)
}

void stopLevelChange() {
    logTrace('stopLevelChange called')
    parent?.componentStopLevelChange(this.device)
}

void parse(String description) {
    logTrace('parse called')
    logWarn("parse(String description) not implemented description: ${description}")
}

void parse(Map event) {
    logTrace('parse called')
    if (event) {
        logDebug("got event: ${event}")
        switch (event.name) {
            case 'switch':
                Integer onOffValue = zigbee.convertHexToInt(event.value)
                String onOffState = onOffValue == 0 ? 'off' : 'on'
                String descriptionText = "${device.displayName} was turned ${onOffState}"
                String currentValue = device.currentValue('switch')
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
                sendEvent(processEvent([name: 'switch', value: onOffState, type: type, descriptionText: descriptionText]))
                break
            case 'level':
                Integer levelValue = zigbee.convertHexToInt(event.value)
                logDebug("current level report is ${levelValue}")
                levelValue = levelValueToPercentage(levelValue)
                String descriptionText = "${device.displayName} was set to ${levelValue}%"
                Integer currentValue = device.currentValue('level') ? (device.currentValue('level')).toInteger() : -1
                if (levelValue == currentValue) {
                    descriptionText = "${device.displayName} is ${levelValue}%"
                }
                String type = 'physical'
                String action = state['action'] ?: 'standby'
                if (action == 'digitalsetlevel') {
                    logDebug("action is ${action}")
                    type = 'digital'
                    state['action'] = 'standby'
                    logDebug('action set to standby')
                }
                logEvent(descriptionText)
                sendEvent(processEvent([name: 'level', value: levelValue, unit: '%', type: type, descriptionText: descriptionText]))
                break
            case 'startupBehaviour':
                Map<String, String> startUpOnOff = ['00': 'off', '01': 'on', '02': 'opposite', '03': 'previous', 'FF': 'previous']
                String startupBehaviour = PREFPOWERON[startUpOnOff[event.value]] ?: "${event.value} (unknown}"
                logDebug("deviceConfigDefaultPowerOnBehaviour is currently set to: ${PREFPOWERON[deviceConfigDefaultPowerOnBehaviour ?: 'previous']} and device reports it is set to: ${startupBehaviour}")
                break
            case 'startUpLevelBehaviour':
                Integer startUpLevelValue = zigbee.convertHexToInt(event.value)
                logDebug("startUpLevelValue is ${startUpLevelValue}")
                String startUpLevelBehaviour = 'Previous'
                if (startUpLevelValue < 255) {
                    startUpLevelValue = levelValueToPercentage(startUpLevelValue)
                    startUpLevelBehaviour = startUpLevelValue.toString()
                }
                logDebug("deviceConfigDefaultPowerOnLevel is currently set to: ${PREFLEVEL[deviceConfigDefaultPowerOnLevel ?: 'previous']} and device reports it is set to: ${startUpLevelBehaviour}")
                break
            case 'onLevelBehaviour':
                Integer onLevelValue = zigbee.convertHexToInt(event.value)
                logDebug("onLevelValue is ${onLevelValue}")
                String onLevelBehaviour = 'Previous'
                if (onLevelValue < 255) {
                    onLevelValue = levelValueToPercentage(onLevelValue)
                    onLevelBehaviour = onLevelValue.toString()
                }
                logDebug("deviceConfigDefaultOnLevel is currently set to: ${PREFLEVEL[deviceConfigDefaultOnLevel ?: 'previous']} and device reports it is set to: ${onLevelBehaviour}")
                break
            default:
                logWarn('unexpected event type!')
        }
    }
    else {
        logWarn('empty event!')
    }
}

String lookupData(String dataType, String dataName) {
    logTrace("lookupData called dataType: ${dataType} dataName: ${dataName}")
    String lookupDataValue = null
    switch (dataType) {
        case 'state':
            lookupDataValue = state[dataName]
            break
        case 'setting':
            lookupDataValue = settings[dataName]
            break
        case 'data':
            lookupDataValue = getDataValue(dataName)
            break
        default:
            break
    }
    logDebug("${dataType} ${dataName}: ${lookupDataValue}")
    return lookupDataValue
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
    device.updateSetting('loggingOption', PREF5)
    runIn(LOGSOFF, logsOff)
}

private void clearAll() {
    logTrace('clearAll called')
    state.clear()
    atomicState.clear()
    unschedule()
}
private Integer levelValueToPercentage(Integer levelValue) {
    logTrace("levelValueToPercentage called levelValue: ${levelValue}")
    Integer validateLevel = levelValue
    if (validateLevel >= DEVICEMAXLEVEL || validateLevel >= 255) {
        logTrace('returning 100')
        return 100
    }
    if (validateLevel <= 0) {
        logTrace('returning 0')
        return 0
    }
    if (validateLevel < DEVICEMINLEVEL ) {
        validateLevel = DEVICEMINLEVEL
    }
    validateLevel = Math.round(validateLevel / levelDivisor)
    if (validateLevel < 1) {
        return 1
    }
    logTrace("returning ${validateLevel}")
    return validateLevel
}

private Integer percentageValueToLevel(BigDecimal percentageValue) {
    return percentageValueToLevel(percentageValue.toInteger())
}

private Integer percentageValueToLevel(String percentageValue) {
    return percentageValueToLevel(percentageValue.toInteger())
}

private Integer percentageValueToLevel(Integer percentageValue) {
    logTrace("percentageValueToLevel called percentageValue: ${percentageValue}")
    Integer validatePercentage = percentageValue
    if (validatePercentage >= 100) {
        logTrace("returning ${DEVICEMAXLEVEL}")
        return DEVICEMAXLEVEL
    }
    if (validatePercentage <= 0) {
        logTrace('returning 0')
        return 0
    }
    logTrace("levelDivisor: ${levelDivisor}")
    Integer level = (validatePercentage * levelDivisor).setScale(0, BigDecimal.ROUND_HALF_UP)
    if (level < 1) {
        return 1
    }
    logTrace("returning ${level}")
    return level
}

private BigDecimal getLevelDivisor() {
    return (DEVICEMAXLEVEL - DEVICEMINLEVEL) / 100
}

private Boolean checkPreference(String preference) {
    logTrace("checkPreference called preference: ${preference}")
    String oldPreference = preference + '_OLD'
    String newPreferenceValue = settings.containsKey(preference) ? settings[preference].toString() : 'unknown'
    logDebug("newPreferenceValue: ${newPreferenceValue}")
    String oldPreferenceValue = settings.containsKey(oldPreference) ? settings[oldPreference].toString() : 'unknown'
    logDebug("oldPreferenceValue: ${oldPreferenceValue}")
    if (oldPreferenceValue != newPreferenceValue) {
        device.updateSetting(oldPreference, newPreferenceValue)
        return true
    }
    return false
}

private Boolean checkPreferences() {
    logTrace('checkPreferences called')
    Set deviceConfig = settings.keySet().findAll { String preference ->
        preference.startsWith('deviceConfig') && preference.indexOf('_OLD') == -1 && checkPreference(preference)
    }
    return (deviceConfig.size() > 0)
}
