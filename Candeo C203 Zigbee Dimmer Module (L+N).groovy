/**
 *    Candeo C203 Zigbee Dimmer Module (L+N)
 *    Supports Momentary & Toggle Switches
 *    Supports on / off / setLevel / startLevelChange / stopLevelChange / flash
 *    Reports switch / level events
 *    Has Setting For Level Transition Time (use device setting, as fast as possible or set an explicit time)
 *    Has Setting For Level Change Rate (as fast as possible or set an explicit rate)
 *    Has Setting For Flash Time
 *    Has Setting For Flash Timeout
 *    Has Setting For Power On Default
 *    Has Setting For Power On Default Level
 *    Has Setting For Default On Level
 *    Has Setting For On / Off Transition Time
 *    Has Setting For Switch Input Type
 *    Has Setting For Explicit State After Hub Startup
 */

metadata {
    definition(name: 'Candeo C203 Zigbee Dimmer Module (L+N)', namespace: 'Candeo', author: 'Candeo', importUrl: 'https://raw.githubusercontent.com/candeosmart/hubitat-zigbee/main/Candeo%20C203%20Zigbee%20Dimmer%20Module%20(L%2BN).groovy', singleThreaded: true) {
        capability 'Switch'
        capability 'SwitchLevel'
        capability 'ChangeLevel'
        capability 'Flash'
        capability 'Actuator'
        capability 'Initialize'
        capability 'Refresh'
        capability 'Configuration'

        command 'resetPreferencesToDefault'

        fingerprint profileId: '0104', endpointId: '01', inClusters: '0000,0003,0004,0005,0006,0008,0B05,1000', outClusters: '0019', manufacturer: 'Candeo', model: 'HK-LN-DIM-A', deviceJoinName: 'Candeo C203 Zigbee Dimmer Module (L+N)'
        fingerprint profileId: '0104', endpointId: '01', inClusters: '0000,0003,0004,0005,0006,0008,0B05,1000', outClusters: '0019', manufacturer: 'Candeo', model: 'C203', deviceJoinName: 'Candeo C203 Zigbee Dimmer Module (L+N)'
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
        input name: 'deviceConfigDefaultOnOffTransitionTime', type: 'enum', title: 'On / Off Transition Time (s) When Turned On Or Off', description: '<small>When turned on or off, use this as the transition time to fade up (from off to on) or down (from on to off).</small><br><br>', options: PREFTRANSITIONTIME, defaultValue: '1000'
        input name: 'deviceConfigSwitchInputType', type: 'enum', title: 'Switch Input Type', description: '<small>Select the type of switch connected to the input. <strong>Power cycle the module for the setting to take effect.</strong></small><br><br>', options: PREFSWITCHINPUTTYPE, defaultValue: 'momentary'
        input name: 'platformOptions', type: 'hidden', title: '<strong>Platform Options</strong>', description: '<small>The following options are relevant to the Hubitat platform and UI itself.</small>'
    }
}

import groovy.transform.Field

private @Field final String CANDEO = 'Candeo C203 Device Driver'
private @Field final Boolean DEBUG = false
private @Field final Integer LOGSOFF = 1800
private @Field final Integer ZIGBEEDELAY = 1000
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
private @Field final Map PREFSWITCHINPUTTYPE = [ 'momentary': 'Momentary', 'toggle': 'Toggle' ]
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
    device.updateSetting('deviceConfigDefaultOnOffTransitionTime', PREF1000)
    logInfo("deviceConfigDefaultOnOffTransitionTime setting is: ${PREFTRANSITIONTIME[deviceConfigDefaultOnOffTransitionTime]}")
    device.updateSetting('deviceConfigSwitchInputType', [value: 'momentary', type: 'enum'])
    logInfo("deviceConfigSwitchInputType setting is: ${PREFSWITCHINPUTTYPE[deviceConfigSwitchInputType]}")
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

List<String> updated() {
    logTrace('updated called')
    logTrace("settings: ${settings}")
    logInfo("deviceConfigDefaultPowerOnBehaviour setting is: ${PREFPOWERON[deviceConfigDefaultPowerOnBehaviour ?: 'previous']}", true)
    logInfo("deviceConfigDefaultPowerOnLevel setting is: ${PREFLEVEL[deviceConfigDefaultPowerOnLevel ?: 'previous']}", true)
    logInfo("deviceConfigDefaultOnLevel setting is: ${PREFLEVEL[deviceConfigDefaultOnLevel ?: 'previous']}", true)
    logInfo("deviceConfigDefaultOnOffTransitionTime setting is: ${PREFTRANSITIONTIME[deviceConfigDefaultOnOffTransitionTime ?: '1000']}", true)
    logInfo("deviceConfigSwitchInputType setting is: ${PREFSWITCHINPUTTYPE[deviceConfigSwitchInputType ?: 'momentary']}", true)
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

List<String> configure() {
    logTrace('configure called')
    logDebug("startup on off is ${deviceConfigDefaultPowerOnBehaviour ?: 'previous'}")
    Map<String, String> startUpOnOff = ['on': 0x01, 'off': 0x0, 'opposite': 0x02, 'previous': 0x03]
    logDebug("startUpOnOff: ${startUpOnOff[deviceConfigDefaultPowerOnBehaviour ?: 'previous']}")
    logDebug("startup current level is: ${deviceConfigDefaultPowerOnLevel ?: 'previous'}")
    Integer startUpLevel = deviceConfigDefaultPowerOnLevel ? deviceConfigDefaultPowerOnLevel == 'previous' ? 255 : percentageValueToLevel(deviceConfigDefaultPowerOnLevel) : 255
    logDebug("startUpLevel: ${startUpLevel}")
    logDebug("on level is: ${deviceConfigDefaultOnLevel ?: 'previous'}")
    Integer onLevel = deviceConfigDefaultOnLevel ? deviceConfigDefaultOnLevel == 'previous' ? 255 : percentageValueToLevel(deviceConfigDefaultOnLevel) : 255
    logDebug("onLevel: ${onLevel}")
    logDebug("on / off transition time is: ${deviceConfigDefaultOnOffTransitionTime ?: '1000'}")
    Integer onOffTransitionTime = deviceConfigDefaultOnOffTransitionTime ? deviceConfigDefaultOnOffTransitionTime == 'none' ? 65535 : (deviceConfigDefaultOnOffTransitionTime.toInteger() / 100).toInteger() : 10
    logDebug("onOffTransitionTime: ${onOffTransitionTime}")
    logDebug("switch input type is ${deviceConfigSwitchInputType ?: 'momentary'}")
    Map<String, String> deviceConfigSwitchInputTypes = ['momentary': 0x00, 'toggle': 0x01]
    logDebug("switchInputType: ${deviceConfigSwitchInputTypes[deviceConfigSwitchInputType ?: 'momentary']}")
    List<String> cmds = [//onoff
                         "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0006 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",
                         "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x0000 ${DataType.BOOLEAN} 0 3600 {}", "delay ${ZIGBEEDELAY}",
                         "he raw 0x${device.deviceNetworkId} 1 0x${device.endpointId} 0x0006 {10 00 08 00 00 00}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x0000 {}", "delay ${ZIGBEEDELAY}",
                         //level
                         "zdo bind 0x${device.deviceNetworkId} 0x${device.endpointId} 0x01 0x0008 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",
                         "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0000 ${DataType.UINT8} 1 3600 {01}", "delay ${ZIGBEEDELAY}",
                         "he raw 0x${device.deviceNetworkId} 1 0x${device.endpointId} 0x0008 {10 00 08 00 00 00}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0000 {}", "delay ${ZIGBEEDELAY}",
                         //startupbehaviour
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x4003 {}", "delay ${ZIGBEEDELAY}",
                         "he wattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x4003 ${convertToHexString(DataType.ENUM8)} {${startUpOnOff[deviceConfigDefaultPowerOnBehaviour ?: 'previous']}} {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x4003 {}", "delay ${ZIGBEEDELAY}",
                         //minlevel
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0002 {}", "delay ${ZIGBEEDELAY}",
                         //maxlevel
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0003 {}", "delay ${ZIGBEEDELAY}",
                         //onofftransitiontime
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0010 {}", "delay ${ZIGBEEDELAY}",
                         "he wattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0010 ${convertToHexString(DataType.UINT16)} {${convertToHexString(onOffTransitionTime, 2)}} {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0010 {}", "delay ${ZIGBEEDELAY}",
                         //onlevel
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0011 {}", "delay ${ZIGBEEDELAY}",
                         "he wattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0011 ${convertToHexString(DataType.UINT8)} {${convertToHexString(onLevel)}} {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0011 {}", "delay ${ZIGBEEDELAY}",
                         //ontransitiontime
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0012 {}", "delay ${ZIGBEEDELAY}",
                         //offtransitiontime
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0013 {}", "delay ${ZIGBEEDELAY}",
                         //defaultmoverate
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0014 {}", "delay ${ZIGBEEDELAY}",
                         //startupcurrentlevel
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x4000 {}", "delay ${ZIGBEEDELAY}",
                         "he wattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x4000 ${convertToHexString(DataType.UINT8)} {${convertToHexString(startUpLevel)}} {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x4000 {}", "delay ${ZIGBEEDELAY}",
                         //switchinputtype
                         "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0000 {04 24 12 00 00 03 88}", "delay ${ZIGBEEDELAY}",
                         "he wattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0000 0x8803 ${convertToHexString(DataType.UINT8)} {${deviceConfigSwitchInputTypes[deviceConfigSwitchInputType ?: 'momentary']}} {1224}", "delay ${ZIGBEEDELAY}",
                         "he raw 0x${device.deviceNetworkId} 0x01 0x${device.endpointId} 0x0000 {04 24 12 00 00 03 88}", "delay ${ZIGBEEDELAY}"]
    logDebug("sending ${cmds}")
    return cmds
}

List<String> refresh() {
    logTrace('refresh called')
    List<String> cmds = ["he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x0000 {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0x0000 {}", "delay ${ZIGBEEDELAY}"]
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

List<String> setLevel(BigDecimal level) {
    logTrace("setLevel called level: ${level}")
    logDebug("levelTransitionTime: ${levelTransitionTime ?: 'device'}")
    BigDecimal levelTime = levelTransitionTime ? levelTransitionTime == 'none' ? 0 : levelTransitionTime == 'device' ? 65535 : levelTransitionTime.toBigDecimal() / 1000 : 65535
    return setLevel(level, levelTime)
}

List<String> setLevel(BigDecimal level, BigDecimal transition) {
    logTrace("setLevel called level: ${level} rate: ${transition}")
    Integer scaledTransition = transition == 0 ? 0 : transition == 65535 ? 65535 : (transition * 10).toInteger()
    logDebug("scaledTransition: ${scaledTransition}")
    Integer scaledLevel = percentageValueToLevel(level)
    logDebug("scaledLevel: ${scaledLevel}")
    List<String> cmds = ["he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 4 {0x${intTo8bitUnsignedHex(scaledLevel)} 0x${intTo16bitUnsignedHex(scaledTransition)}}"]
    logDebug("sending ${cmds}")
    state['action'] = 'digitalsetlevel'
    return cmds
}

List<String> startLevelChange(String direction) {
    logTrace("startLevelChange called direction: ${direction}")
    Integer upDown = direction == 'down' ? 1 : 0
    logDebug("upDown: ${upDown} levelChangeRate: ${levelChangeRate ?: 'none'}")
    Integer scaledRate = levelChangeRate ? levelChangeRate == 'none' ? 255 : percentageValueToLevel(levelChangeRate.toBigDecimal()) : 255
    logDebug("scaledRate: ${scaledRate}")
    List<String> cmds = ["he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 5 {0x${intTo8bitUnsignedHex(upDown)} 0x${intTo16bitUnsignedHex(scaledRate)}}"]
    logDebug("sending ${cmds}")
    state['action'] = 'digitalsetlevel'
    return cmds
}

List<String> stopLevelChange() {
    logTrace('stopLevelChange called')
    List<String> cmds = ["he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 3 {}"]
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
        if (descriptionMap.cluster == '0000' || descriptionMap.clusterId == '0000' || descriptionMap.clusterInt == 0) {
            processBasicEvent(descriptionMap, events)
        }
        else if (descriptionMap.cluster == '0006' || descriptionMap.clusterId == '0006' || descriptionMap.clusterInt == 6) {
            processSwitchEvent(descriptionMap, events)
        }
        else if (descriptionMap.cluster == '0008' || descriptionMap.clusterId == '0008' || descriptionMap.clusterInt == 8) {
            processLevelEvent(descriptionMap, events)
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

private void processBasicEvent(Map descriptionMap, List<Map> events) {
    logTrace('processBasicEvent called')
    switch (descriptionMap.command) {
        case '0A':
        case '01':
            if (descriptionMap.attrId == '8803' || descriptionMap.attrInt == 34819) {
                logDebug('basic (0000) switch type report (8803)')
                String deviceConfigSwitchInputTypeValue = descriptionMap.value
                Map<String, String> deviceConfigSwitchInputTypes = ['00': 'momentary', '01': 'toggle']
                String deviceConfigSwitchInputTypeBehaviour = PREFSWITCHINPUTTYPE[deviceConfigSwitchInputTypes[deviceConfigSwitchInputTypeValue]] ?: "${deviceConfigSwitchInputTypeValue} (unknown}"
                logDebug("deviceConfigSwitchInputType is currently set to: ${PREFSWITCHINPUTTYPE[deviceConfigSwitchInputType ?: 'previous']} and device reports it is set to: ${deviceConfigSwitchInputTypeBehaviour}")
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
            logDebug('basic (0000) command skipped')
            break
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
                logDebug("deviceConfigDefaultPowerOnBehaviour is currently set to: ${PREFPOWERON[deviceConfigDefaultPowerOnBehaviour ?: 'previous']} and device reports it is set to: ${startupBehaviour}")
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

private void processLevelEvent(Map descriptionMap, List<Map> events) {
    logTrace('processLevelEvent called')
    switch (descriptionMap.command) {
        case '0A':
        case '01':
            if (descriptionMap.attrId == '0000' || descriptionMap.attrInt == 0) {
                logDebug('level control (0008) current level report (0000)')
                Integer levelValue = zigbee.convertHexToInt(descriptionMap.value)
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
                events.add(processEvent([name: 'level', value: levelValue, unit: '%', type: type, descriptionText: descriptionText]))
            }
            else if (descriptionMap.attrId == '4000' || descriptionMap.attrInt == 16384) {
                logDebug('level control (0008) startup level report (4000)')
                Integer startUpLevelValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("startUpLevelValue is ${startUpLevelValue}")
                String startUpLevelBehaviour = 'Previous'
                if (startUpLevelValue < 255) {
                    startUpLevelValue = levelValueToPercentage(startUpLevelValue)
                    startUpLevelBehaviour = startUpLevelValue.toString()
                }
                logDebug("deviceConfigDefaultPowerOnLevel is currently set to: ${PREFLEVEL[deviceConfigDefaultPowerOnLevel ?: 'previous']} and device reports it is set to: ${startUpLevelBehaviour}")
            }
            else if (descriptionMap.attrId == '0011' || descriptionMap.attrInt == 11) {
                logDebug('level control (0008) on level report (0011)')
                Integer onLevelValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("onLevelValue is ${onLevelValue}")
                String onLevelBehaviour = 'Previous'
                if (onLevelValue < 255) {
                    onLevelValue = levelValueToPercentage(onLevelValue)
                    onLevelBehaviour = onLevelValue.toString()
                }
                logDebug("deviceConfigDefaultOnLevel is currently set to: ${PREFLEVEL[deviceConfigDefaultOnLevel ?: 'previous']} and device reports it is set to: ${onLevelBehaviour}")
            }
            else if (descriptionMap.attrId == '0002' || descriptionMap.attrInt == 2) {
                logDebug('level control (0008) min level report (0002)')
                Integer minLevelValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("minLevelValue is ${minLevelValue}")
            }
            else if (descriptionMap.attrId == '0003' || descriptionMap.attrInt == 3) {
                logDebug('level control (0008) max level report (0003)')
                Integer maxLevelValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("maxLevelValue is ${maxLevelValue}")
            }
            else if (descriptionMap.attrId == '0010' || descriptionMap.attrInt == 10) {
                logDebug('level control (0008) on off transition time report (0010)')
                Integer onOffTransitionTimeValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("onOffTransitionTimeValue is ${onOffTransitionTimeValue}")
                logDebug("deviceConfigDefaultOnOffTransitionTime is currently set to: ${PREFTRANSITIONTIME[deviceConfigDefaultOnOffTransitionTime ?: '1000']} and device reports it is set to: ${PREFTRANSITIONTIME[(onOffTransitionTimeValue * 100).toString()]}")
            }
            else if (descriptionMap.attrId == '0012' || descriptionMap.attrInt == 12) {
                logDebug('level control (0008) on transition time report (0012)')
                Integer onTransitionTimeValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("onTransitionTimeValue is ${onTransitionTimeValue}")
            }
            else if (descriptionMap.attrId == '0013' || descriptionMap.attrInt == 13) {
                logDebug('level control (0008) on off transition time report (0013)')
                Integer offTransitionTimeValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("offTransitionTimeValue is ${offTransitionTimeValue}")
            }
            else if (descriptionMap.attrId == '0014' || descriptionMap.attrInt == 14) {
                logDebug('level control (0008) default move rate report (0014)')
                Integer defaultMoveRateValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("defaultMoveRateValue is ${defaultMoveRateValue}")
            }
            else {
                logDebug('level control (0008) attribute skipped')
            }
            break
        case '04':
            logDebug('level control (0008) write attribute response (04) skipped')
            break
        case '07':
            logDebug('level control (0008) configure reporting response (07) skipped')
            break
        case '0B':
            logDebug('level control (0008) default response (0B) skipped')
            break
        default:
            logDebug('level control (0008) command skipped')
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
    device.updateSetting('loggingOption', PREF5)
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

private String intTo8bitUnsignedHex(Integer value) {
    return zigbee.convertToHexString(value.toInteger(), 2)
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
