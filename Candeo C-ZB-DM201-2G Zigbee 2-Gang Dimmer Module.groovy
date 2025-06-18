/**
 *    Candeo C-ZB-DM201-2G Zigbee 2-Gang Dimmer Module
 *    Supports Momentary Switches
 *    Via Parent Device:
 *    Supports on / off / setLevel / startLevelChange / stopLevelChange / flash (operates both gangs)
 *    Reports switch events (combines child device states into one based on state determination setting)
 *    Reports level events (combines child device levels into one based on level determination setting)
 *    Has Setting For Determination Of Master State
 *    Has Setting For Determination Of Master Level
 *    Via Child Devices:
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
    definition(name: 'Candeo C-ZB-DM201-2G Zigbee 2-Gang Dimmer Module', namespace: 'Candeo', author: 'Candeo', importUrl: 'https://raw.githubusercontent.com/candeosmart/hubitat-zigbee/refs/heads/main/Candeo%20C-ZB-DM201-2G%20Zigbee%202-Gang%20Dimmer%20Module.groovy', singleThreaded: true) {
        capability 'Switch'
        capability 'SwitchLevel'
        capability 'ChangeLevel'
        capability 'Flash'
        capability 'Actuator'
        capability 'Initialize'
        capability 'Refresh'
        capability 'Configuration'

        command 'resetPreferencesToDefault'

        fingerprint profileId: '0104', endpointId: '01', inClusters: '0000,0003,0004,0005,0006,0008,1000', outClusters: '0019', manufacturer: 'HZC', model: 'DimmerSwitch-2Gang-ZB3.0', deviceJoinName: 'Candeo C-ZB-DM201-2G Zigbee 2-Gang Dimmer Module'
        fingerprint profileId: '0104', endpointId: '01', inClusters: '0000,0003,0004,0005,0006,0008,1000', outClusters: '0019', manufacturer: 'Candeo', model: 'C-ZB-DM201-2G', deviceJoinName: 'Candeo C-ZB-DM201-2G Zigbee 2-Gang Dimmer Module'
    }
    preferences {
        input name: 'deviceDriverOptions', type: 'hidden', title: '<strong>Device Driver Options</strong>', description: '<small>The following options change the behaviour of the device driver, they take effect after hitting "<strong>Save Preferences</strong> below."</small>'
        input name: 'masterStateDetermination', type: 'enum', title: 'Choose How Master State Is Determined', description: '<small>The master on / off control operates both channels so its state can be determined from the following options.</small><br><br>', options: PREFMASTERSTATEDETERMINATION, defaultValue: 'allSame'
        input name: 'masterLevelDetermination', type: 'enum', title: 'Choose How Master Level Is Determined', description: '<small>The master level control operates both channels so its level can be determined from the following options.</small><br><br>', options: PREFMASTERLEVELDETERMINATION, defaultValue: 'allSame'
        input name: 'loggingOption', type: 'enum', title: 'Logging Option', description: '<small>Sets the logging level cumulatively, for example "Driver Trace Logging" will include all logging levels below it.</small><br><br>', options: PREFLOGGING, defaultValue: '5'
        input name: 'deviceConfigurationOptions', type: 'hidden', title: '<strong>Device Configuration Options</strong>', description: '<small>The following options change the behaviour of the device itself, they take effect after hitting "<strong>Save Preferences</strong> below", followed by "<strong>Configure</strong>" above.<br><br>For a battery powered device, you may also need to wake it up manually!</small>'
    }
}

import groovy.transform.Field
import com.hubitat.app.ChildDeviceWrapper
import com.hubitat.app.DeviceWrapper

private @Field final String CANDEO = 'Candeo C-ZB-DM201-2G Device Driver'
private @Field final Boolean DEBUG = false
private @Field final Integer LOGSOFF = 1800
private @Field final Integer ZIGBEEDELAY = 1000
private @Field final Integer DEVICEMINLEVEL = 1
private @Field final Integer DEVICEMAXLEVEL = 254
private @Field final Map PREFFALSE = [value: 'false', type: 'bool']
private @Field final Map PREFTRUE = [value: 'true', type: 'bool']
private @Field final Map PREF5 = [value: '5', type: 'enum']
private @Field final Map PREFALLSAME = [value: 'allSame', type: 'enum']
private @Field final Map PREFHUBRESTART = [ 'off': 'Off', 'on': 'On', 'refresh': 'Refresh State Only', 'nothing': 'Do Nothing' ]
private @Field final Map PREFMASTERSTATEDETERMINATION = [ 'anyOn': 'If Any Child Is On, Set Master On', 'anyOff': 'If Any Child Is Off, Set Master Off', 'allSame': 'Only Change The State If All Children Are In The Same State']
private @Field final Map PREFMASTERLEVELDETERMINATION = [ '01': 'Use Level From Child 01 As Master Level', '02': 'Use Level From Child 02 As Master Level', 'allSame': 'Only Change The Level If All Children Are At The Same Level']
private @Field final Map PREFLOGGING = ['0': 'Device Event Logging', '1': 'Driver Informational Logging', '2': 'Driver Warning Logging', '3': 'Driver Error Logging', '4': 'Driver Debug Logging', '5': 'Driver Trace Logging' ]
private @Field final List<String> CHILDEPS = ['01', '02']

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
    device.updateSetting('masterStateDetermination', PREFALLSAME)
    logInfo("masterStateDetermination setting is: ${PREFMASTERSTATEDETERMINATION[masterStateDetermination]}")
    device.updateSetting('masterLevelDetermination', PREFALLSAME)
    logInfo("masterLevelDetermination setting is: ${PREFMASTERLEVELDETERMINATION[masterLevelDetermination]}")
    logInfo('logging level is: Driver Trace Logging')
    logInfo("logging level will reduce to Driver Error Logging after ${LOGSOFF} seconds")
}

void uninstalled() {
    logTrace('uninstalled called')
    clearAll()
}

void initialize() {
    logTrace('initialize called')
}

List<String> updated() {
    logTrace('updated called')
    logTrace("settings: ${settings}")
    logInfo("masterStateDetermination setting is: ${PREFMASTERSTATEDETERMINATION[masterStateDetermination]}", true)
    logInfo("masterLevelDetermination setting is: ${PREFMASTERLEVELDETERMINATION[masterLevelDetermination]}", true)
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
    List<String> cmds = []
    CHILDEPS.each { String childEP ->
        String address = "EP${childEP}"
        ChildDeviceWrapper childDevice = findChildDevice(address)
        if (childDevice) {
            logDebug("got child device name: ${childDevice.name} displayName: ${childDevice.displayName}, sending configure")
            childDevice.configure()
        }
        else {
            logWarn('could not find child device, skipping command!')
        }
    }
    cmds += refresh()
    logDebug("sending ${cmds}")
    return cmds
}

List<String> refresh() {
    logTrace('refresh called')
    List<String> cmds = []
    CHILDEPS.each { String childEP ->
        String address = "EP${childEP}"
        ChildDeviceWrapper childDevice = findChildDevice(address)
        if (childDevice) {
            logDebug("got child device name: ${childDevice.name} displayName: ${childDevice.displayName}, sending refresh")
            childDevice.refresh()
        }
        else {
            logWarn('could not find child device, skipping command!')
        }
    }
    logDebug("sending ${cmds}")
    return cmds
}

void on() {
    logTrace('on called')
    CHILDEPS.each { String childEP ->
        String address = "EP${childEP}"
        ChildDeviceWrapper childDevice = findChildDevice(address)
        if (childDevice) {
            logDebug("got child device name: ${childDevice.name} displayName: ${childDevice.displayName}, sending on")
            childDevice.on()
        }
        else {
            logWarn('could not find child device, skipping command!')
        }
    }
}

void off() {
    logTrace('off called')
    CHILDEPS.each { String childEP ->
        String address = "EP${childEP}"
        ChildDeviceWrapper childDevice = findChildDevice(address)
        if (childDevice) {
            logDebug("got child device name: ${childDevice.name} displayName: ${childDevice.displayName}, sending off")
            childDevice.off()
        }
        else {
            logWarn('could not find child device, skipping command!')
        }
    }
}

void flash(BigDecimal rate = null) {
    logTrace("flash called rate: ${rate ?: 'no rate specified'}")
    CHILDEPS.each { String childEP ->
        String address = "EP${childEP}"
        ChildDeviceWrapper childDevice = findChildDevice(address)
        if (childDevice) {
            logDebug("got child device name: ${childDevice.name} displayName: ${childDevice.displayName}, sending flash")
            childDevice.flash(rate)
        }
        else {
            logWarn('could not find child device, skipping command!')
        }
    }
}

void setLevel(BigDecimal level) {
    logTrace("setLevel called level: ${level}")
    CHILDEPS.each { String childEP ->
        String address = "EP${childEP}"
        ChildDeviceWrapper childDevice = findChildDevice(address)
        if (childDevice) {
            logDebug("got child device name: ${childDevice.name} displayName: ${childDevice.displayName}, sending setLevel")
            childDevice.setLevel(level)
        }
        else {
            logWarn('could not find child device, skipping command!')
        }
    }
}

void setLevel(BigDecimal level, BigDecimal rate) {
    logTrace("setLevel called level: ${level} rate: ${rate}")
    CHILDEPS.each { String childEP ->
        String address = "EP${childEP}"
        ChildDeviceWrapper childDevice = findChildDevice(address)
        if (childDevice) {
            logDebug("got child device name: ${childDevice.name} displayName: ${childDevice.displayName}, sending setLevel")
            childDevice.setLevel(level, rate)
        }
        else {
            logWarn('could not find child device, skipping command!')
        }
    }
}

void startLevelChange(String direction) {
    logTrace("startLevelChange called direction: ${direction}")
    CHILDEPS.each { String childEP ->
        String address = "EP${childEP}"
        ChildDeviceWrapper childDevice = findChildDevice(address)
        if (childDevice) {
            logDebug("got child device name: ${childDevice.name} displayName: ${childDevice.displayName}, sending startLevelChange")
            childDevice.startLevelChange(direction)
        }
        else {
            logWarn('could not find child device, skipping command!')
        }
    }
}

void stopLevelChange() {
    logTrace('stopLevelChange called')
    CHILDEPS.each { String childEP ->
        String address = "EP${childEP}"
        ChildDeviceWrapper childDevice = findChildDevice(address)
        if (childDevice) {
            logDebug("got child device name: ${childDevice.name} displayName: ${childDevice.displayName}, sending stopLevelChange")
            childDevice.stopLevelChange()
        }
        else {
            logWarn('could not find child device, skipping command!')
        }
    }
}

void componentOn(DeviceWrapper childDevice) {
    logTrace('componentOn called')
    logDebug("got childDevice: ${childDevice.displayName}")
    String endpoint = childDevice.deviceNetworkId.split('-EP')[1]
    List<String> cmds = ["he cmd 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x01 {}"]
    doZigBeeCommand(cmds)
}

void componentOff(DeviceWrapper childDevice) {
    logTrace('componentOff called')
    logDebug("got childDevice: ${childDevice.displayName}")
    String endpoint = childDevice.deviceNetworkId.split('-EP')[1]
    List<String> cmds = ["he cmd 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x00 {}"]
    doZigBeeCommand(cmds)
}

void componentSetLevel(DeviceWrapper childDevice, Integer scaledLevel, Integer scaledTransition) {
    logTrace("componentSetLevel called scaledLevel: ${scaledLevel} scaledTransition: ${scaledTransition}")
    logDebug("got childDevice: ${childDevice.displayName}")
    String endpoint = childDevice.deviceNetworkId.split('-EP')[1]
    List<String> cmds = ["he cmd 0x${device.deviceNetworkId} 0x${endpoint} 0x0008 4 {0x${intTo8bitUnsignedHex(scaledLevel)} 0x${intTo16bitUnsignedHex(scaledTransition)}}"]
    doZigBeeCommand(cmds)
}

void componentStartLevelChange(DeviceWrapper childDevice, Integer upDown, Integer scaledRate) {
    logTrace("componentStartLevelChange called upDown: ${upDown} scaledRate: ${scaledRate}")
    logDebug("got childDevice: ${childDevice.displayName}")
    String endpoint = childDevice.deviceNetworkId.split('-EP')[1]
    List<String> cmds = ["he cmd 0x${device.deviceNetworkId} 0x${endpoint} 0x0008 5 {0x${intTo8bitUnsignedHex(upDown)} 0x${intTo16bitUnsignedHex(scaledRate)}}"]
    doZigBeeCommand(cmds)
}

void componentStopLevelChange(DeviceWrapper childDevice) {
    logTrace('componentStopLevelChange called')
    logDebug("got childDevice: ${childDevice.displayName}")
    String endpoint = childDevice.deviceNetworkId.split('-EP')[1]
    List<String> cmds = ["he cmd 0x${device.deviceNetworkId} 0x${endpoint} 0x0008 3 {}"]
    doZigBeeCommand(cmds)
}

void componentRefresh(DeviceWrapper childDevice) {
    logTrace('componentRefresh called')
    logDebug("got childDevice: ${childDevice.displayName}")
    String endpoint = childDevice.deviceNetworkId.split('-EP')[1]
    List<String> cmds = ["he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x0000 {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0008 0x0000 {}", "delay ${ZIGBEEDELAY}"]
    doZigBeeCommand(cmds)
}

void componentConfigure(DeviceWrapper childDevice) {
    logTrace('componentConfigure called')
    logDebug("got childDevice: ${childDevice.displayName}")
    String endpoint = childDevice.deviceNetworkId.split('-EP')[1]
    String deviceConfigDefaultPowerOnBehaviour = getChildDeviceSettingItem("EP${endpoint}", 'deviceConfigDefaultPowerOnBehaviour') ?: 'previous'
    logDebug("startup on off is ${deviceConfigDefaultPowerOnBehaviour ?: 'previous'}")
    Map<String, String> startUpOnOff = ['on': 0x01, 'off': 0x0, 'opposite': 0x02, 'previous': 0x03]
    logDebug("startUpOnOff: ${startUpOnOff[deviceConfigDefaultPowerOnBehaviour ?: 'previous']}")
    String deviceConfigDefaultPowerOnLevel = getChildDeviceSettingItem("EP${endpoint}", 'deviceConfigDefaultPowerOnLevel') ?: 'previous'
    logDebug("startup current level is: ${deviceConfigDefaultPowerOnLevel ?: 'previous'}")
    Integer startUpLevel = deviceConfigDefaultPowerOnLevel ? deviceConfigDefaultPowerOnLevel == 'previous' ? 255 : percentageValueToLevel(deviceConfigDefaultPowerOnLevel) : 255
    logDebug("startUpLevel: ${startUpLevel}")
    String deviceConfigDefaultOnLevel = getChildDeviceSettingItem("EP${endpoint}", 'deviceConfigDefaultOnLevel') ?: 'previous'
    logDebug("on level is: ${deviceConfigDefaultOnLevel ?: 'previous'}")
    Integer onLevel = deviceConfigDefaultOnLevel ? deviceConfigDefaultOnLevel == 'previous' ? 255 : percentageValueToLevel(deviceConfigDefaultOnLevel) : 255
    logDebug("onLevel: ${onLevel}")
    List<String> cmds = ["zdo bind 0x${device.deviceNetworkId} 0x${endpoint} 0x01 0x0006 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",
                         "he cr 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x0000 ${DataType.BOOLEAN} 0 3600 {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x0000 {}", "delay ${ZIGBEEDELAY}",
                         "zdo bind 0x${device.deviceNetworkId} 0x${endpoint} 0x01 0x0008 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",
                         "he cr 0x${device.deviceNetworkId} 0x${endpoint} 0x0008 0x0000 ${DataType.UINT8} 1 3600 {01}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0008 0x0000 {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x4003 {}", "delay ${ZIGBEEDELAY}",
                         "he wattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x4003 ${convertToHexString(DataType.ENUM8)} {${startUpOnOff[deviceConfigDefaultPowerOnBehaviour ?: 'previous']}} {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x4003 {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0008 0x0011 {}", "delay ${ZIGBEEDELAY}",
                         "he wattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0008 0x0011 ${convertToHexString(DataType.UINT8)} {${convertToHexString(onLevel)}} {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0008 0x0011 {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0008 0x4000 {}", "delay ${ZIGBEEDELAY}",
                         "he wattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0008 0x4000 ${convertToHexString(DataType.UINT8)} {${convertToHexString(startUpLevel)}} {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0008 0x4000 {}", "delay ${ZIGBEEDELAY}"]
    doZigBeeCommand(cmds)
}

void checkAndSetMasterLevel() {
    logTrace('checkAndSetMasterLevel called')
    List<Integer> childLevels = []
    CHILDEPS.each { String childEP ->
        String address = "EP${childEP}"
        Integer childDeviceLevel = getChildDeviceCurrentValue(address, 'level') == 'unknown' ? 9999 : getChildDeviceCurrentValue(address, 'level').toInteger()
        childLevels.add(childDeviceLevel)
    }
    logDebug("childLevels: ${childLevels}")
    childLevels = childLevels.toUnique()
    Boolean levelsMatch = false
    if (childLevels.size() == 1) {
        levelsMatch = true
    }
    logDebug("levelsMatch: ${levelsMatch}")
    logDebug("masterLevelDetermination setting is: ${masterLevelDetermination}")
    Integer masterLevel = 9999
    if (levelsMatch && masterLevelDetermination == 'allSame') {
        Integer levelValue = childLevels.pop()
        masterLevel = levelValue
        logDebug("masterLevel set to levelValue: ${levelValue}")
    }
    else if (masterLevelDetermination != 'allSame') {
        String address = "EP${masterLevelDetermination}"
        Integer childDeviceLevel = getChildDeviceCurrentValue(address, 'level') == 'unknown' ? 9999 : getChildDeviceCurrentValue(address, 'level').toInteger()
        masterLevel = childDeviceLevel
        logDebug("masterLevel set to childDeviceLevel: ${childDeviceLevel}")
    }
    if (masterLevel == 9999) {
        logDebug('masterLevel could not be determined, skipping this event!')
    }
    else {
        logDebug("masterLevel determined to be ${masterLevel}")
        String descriptionText = "${device.displayName} was set to ${masterLevel}%"
        logEvent(descriptionText)
        sendEvent(processEvent([name: 'level', value: masterLevel, unit: '%', type: 'digital', descriptionText: descriptionText]))
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
        String endpoint = descriptionMap.sourceEndpoint ?: descriptionMap.endpoint ?: 'unknown'
        if (CHILDEPS.contains(endpoint) && (descriptionMap.cluster == '0006' || descriptionMap.clusterId == '0006' || descriptionMap.clusterInt == 6)) {
            processSwitchEvent(descriptionMap, events)
        }
        else if (CHILDEPS.contains(endpoint) && (descriptionMap.cluster == '0008' || descriptionMap.clusterId == '0008' || descriptionMap.clusterInt == 8)) {
            processLevelEvent(descriptionMap, events)
        }
        else {
            logDebug("skipped endpoint: ${endpoint} descriptionMap.cluster: ${descriptionMap.cluster ?: 'unknown'} descriptionMap.clusterId: ${descriptionMap.clusterId ?: 'unknown'} descriptionMap.clusterInt: ${descriptionMap.clusterInt ?: 'unknown'}")
        }
        if (descriptionMap.additionalAttrs) {
            logDebug("got additionalAttrs: ${descriptionMap.additionalAttrs}")
            descriptionMap.additionalAttrs.each { Map attribute ->
                attribute.endpoint = endpoint
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
            String endpoint = descriptionMap.sourceEndpoint ?: descriptionMap.endpoint ?: 'unknown'
            logDebug("endpoint is: ${endpoint}")
            if (descriptionMap.attrId == '0000' || descriptionMap.attrInt == 0) {
                logDebug('on off (0006) on off report (0000)')
                Integer onOffValue = zigbee.convertHexToInt(descriptionMap.value)
                if (onOffValue == 1 || onOffValue == 0) {
                    logDebug("on off report is ${onOffValue}")
                    String onOffState = onOffValue == 0 ? 'off' : 'on'
                    Integer childEPCount = CHILDEPS.size()
                    Integer childEPSOn = 0
                    Integer childEPSOff = 0
                    CHILDEPS.each { String childEP ->
                        if (endpoint == childEP) {
                            switch (onOffState) {
                                case 'on':
                                    childEPSOn++
                                    break
                                case 'off':
                                    childEPSOff++
                                    break
                                default:
                                    logDebug('got unknown state for device')
                                    break
                            }
                        }
                        else {
                            String address = "EP${childEP}"
                            String childDeviceState = getChildDeviceCurrentValue(address, 'switch')
                            switch (childDeviceState) {
                                case 'on':
                                    childEPSOn++
                                    break
                                case 'off':
                                    childEPSOff++
                                    break
                                default:
                                    logDebug('got unknown state for child device')
                                    break
                            }
                        }
                    }
                    logDebug("there are ${childEPCount} child endpoints and ${childEPSOn} of them are on and ${childEPSOff} of them are off")
                    logDebug("masterStateDetermination setting is: ${masterStateDetermination}")
                    String masterState = 'unknown'
                    if (childEPSOn == childEPCount) {
                        masterState = 'on'
                    }
                    else if (childEPSOff == childEPCount) {
                        masterState = 'off'
                    }
                    else if (childEPSOn > 0 && masterStateDetermination == 'anyOn') {
                        masterState = 'on'
                    }
                    else if (childEPSOff > 0 && masterStateDetermination == 'anyOff') {
                        masterState = 'off'
                    }
                    if (masterState == 'unknown') {
                        logDebug('masterState could not be determined, skipping this event!')
                    }
                    else {
                        logDebug("masterState determined to be ${masterState}")
                        String descriptionText = "${device.displayName} was turned ${masterState}"
                        events.add(processEvent([name: 'switch', value: masterState, type: 'digital', descriptionText: descriptionText]))
                    }
                    sendEventToChildDevice("EP${endpoint}", 'switch', descriptionMap.value)
                }
                else {
                    logDebug("skipping onOffValue: ${descriptionMap.value}")
                }
            }
            else if (descriptionMap.attrId == '4003' || descriptionMap.attrInt == 16387) {
                logDebug('on off (0006) startup on off report (4003)')
                String startUpOnOffValue = descriptionMap.value
                logDebug("startUpOnOffValue: ${startUpOnOffValue}")
                sendEventToChildDevice("EP${endpoint}", 'startupBehaviour', startUpOnOffValue)
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
            String endpoint = descriptionMap.sourceEndpoint ?: descriptionMap.endpoint ?: 'unknown'
            logDebug("endpoint is: ${endpoint}")
            if (descriptionMap.attrId == '0000' || descriptionMap.attrInt == 0) {
                logDebug('level control (0008) current level report (0000)')
                Integer levelValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("current level report is ${levelValue}")
                levelValue = levelValue <= 254 ? levelValue >= 0 ? levelValue : 0 : 254
                levelValue = Math.round(levelValue / 2.55)
                logDebug("levelValue: ${levelValue}")
                sendEventToChildDevice("EP${endpoint}", 'level', descriptionMap.value)
                runInMillis(1000, checkAndSetMasterLevel)
            }
            else if (descriptionMap.attrId == '4000' || descriptionMap.attrInt == 16384) {
                logDebug('level control (0008) startup level report (4000)')
                logDebug("startUpLevelValue: ${descriptionMap.value}")
                sendEventToChildDevice("EP${endpoint}", 'startUpLevelBehaviour', descriptionMap.value)
            }
            else if (descriptionMap.attrId == '0011' || descriptionMap.attrInt == 11) {
                logDebug('level control (0008) on level report (0011)')
                logDebug("onLevelValue: ${descriptionMap.value}")
                sendEventToChildDevice("EP${endpoint}", 'onLevelBehaviour', descriptionMap.value)
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

private void sendEventToChildDevice(String address, String eventType, String eventValue) {
    logDebug("sendEventToChildDevice called address: ${address} eventType: ${eventType} eventValue: ${eventValue}")
    ChildDeviceWrapper childDevice = findChildDevice(address)
    if (childDevice) {
        logDebug("got child device name: ${childDevice.name} displayName: ${childDevice.displayName}, sending event")
        Map<String> childEvent = [name: eventType, value: eventValue]
        childDevice.parse(childEvent)
    }
    else {
        logWarn('could not find child device, skipping event!')
    }
}

private String getChildDeviceCurrentValue(String address, String valueItem) {
    logDebug("getChildDeviceCurrentValue called address: ${address} valueItem: ${valueItem}")
    String currentValue = 'unknown'
    ChildDeviceWrapper childDevice = findChildDevice(address)
    if (childDevice) {
        logDebug("got child device name: ${childDevice.name} displayName: ${childDevice.displayName}, getting value")
        String childDeviceValue = childDevice.currentValue(valueItem)
        logDebug("got childDeviceValue: ${childDeviceValue}")
        currentValue = childDeviceValue ?: 'unknown'
        logDebug("got currentValue: ${currentValue}")
    }
    else {
        logWarn('could not find child device!')
    }
    return currentValue
}

private String getChildDeviceStateItem(String address, String stateItem) {
    logDebug("getChildDeviceStateItem called address: ${address} stateItem: ${stateItem}")
    return getChildDeviceData(address, 'state', stateItem)
}

private String getChildDeviceSettingItem(String address, String settingItem) {
    logDebug("getChildDeviceSettingItem called address: ${address} settingItem: ${settingItem}")
    return getChildDeviceData(address, 'setting', settingItem)
}

private String getChildDeviceDataItem(String address, String dataItem) {
    logDebug("getChildDeviceDataItem called address: ${address} dataItem: ${dataItem}")
    return getChildDeviceData(address, 'data', dataItem)
}

private String getChildDeviceData(String address, String dataType, String dataName) {
    logDebug("getChildDeviceData called address: ${address} dataType: ${dataType} dataName: ${dataName}")
    String dataNameValue = 'unknown'
    ChildDeviceWrapper childDevice = findChildDevice(address)
    if (childDevice) {
        logDebug("got child device name: ${childDevice.name} displayName: ${childDevice.displayName}, getting value")
        dataNameValue = childDevice.lookupData(dataType, dataName) ?: 'unknown'
        logDebug("got dataNameValue: ${dataNameValue}")
    }
    else {
        logWarn('could not find child device!')
    }
    return dataNameValue
}

private ChildDeviceWrapper findChildDevice(String address) {
    logTrace("findChildDevice called address: ${address}")
    ChildDeviceWrapper childDevice = this.getChildDevice("${device.id}-${address}")
    if (childDevice == null) {
        logDebug("creating child device for address: ${address}")
        this.addChildDevice('Candeo', 'Candeo C-ZB-DM201-2G Zigbee 2-Gang Dimmer Module Child Switch', "${device.id}-${address}", [name: "${device.displayName} ${address}", label: "${device.displayName} ${address}", isComponent: true])
        childDevice = this.getChildDevice("${device.id}-${address}")
    }
    return childDevice
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
    if (validateLevel >= DEVICEMAXLEVEL) {
        logTrace("device reported level greater than its reported maximum ${DEVICEMAXLEVEL}, returning 100")
        return 100
    }
    if (validateLevel >= 255) {
        logTrace('device reported level greater than or equal to 255, returning 100')
        return 100
    }
    if (validateLevel <= 0) {
        logTrace('device reported less than or equal to 0, returning 0')
        return 0
    }
    if (validateLevel < DEVICEMINLEVEL ) {
        logTrace("device reported level lower than its reported minimum ${DEVICEMINLEVEL}, using ${DEVICEMINLEVEL}")
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
