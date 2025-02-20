/**
 *    Candeo C-ZB-SM205-2G Zigbee 2-Gang Switch Module (L+N)
 *    Supports Momentary & Toggle Switches
 *    Via Parent Device:
 *    Supports on / off / flash (operates both gangs)
 *    Reports switch events (combines child device states into one based on state determination setting)
 *    Reports power / energy / current / voltage events (whole device)
 *    Has Setting For Determination Of Master State
 *    Has Settings For Power Reporting
 *    Has Settings For Voltage Reporting
 *    Has Settings For Current Reporting
 *    Has Settings For Energy Reporting
 *    Has Setting For Switch Input Type
 *    Via Child Devices:
 *    Supports on / off / flash
 *    Reports switch events
 *    Has Setting For Flash Time
 *    Has Setting For Flash Timeout
 *    Has Setting For Power On Default
 *    Has Setting For Explicit State After Hub Startup
 */

metadata {
    definition(name: 'Candeo C-ZB-SM205-2G Zigbee 2-Gang Switch Module (L+N)', namespace: 'Candeo', author: 'Candeo', importUrl: 'https://raw.githubusercontent.com/candeosmart/hubitat-zigbee/main/Candeo%20C-ZB-SM205-2G%20Zigbee%202-Gang%20Switch%20Module%20(L%2BN).groovy', singleThreaded: true) {
        capability 'Switch'
        capability 'Flash'
        capability 'PowerMeter'
        capability 'EnergyMeter'
        capability 'VoltageMeasurement'
        capability 'CurrentMeter'
        capability 'Sensor'
        capability 'Actuator'
        capability 'Initialize'
        capability 'Refresh'
        capability 'Configuration'

        command 'resetPreferencesToDefault'

        fingerprint profileId: '0104', endpointId: '01', inClusters: '0000,0003,0004,0005,0006', manufacturer: 'Candeo', model: 'C-ZB-SM205-2G', deviceJoinName: 'Candeo C-ZB-SM205-2G Zigbee 2-Gang Switch Module (L+N)'
    }
    preferences {
        input name: 'deviceDriverOptions', type: 'hidden', title: '<strong>Device Driver Options</strong>', description: '<small>The following options change the behaviour of the device driver, they take effect after hitting "<strong>Save Preferences</strong> below."</small>'
        input name: 'masterStateDetermination', type: 'enum', title: 'Choose How Master State Is Determined', description: '<small>The master on / off control operates both channels so its state can be determined from the following options.</small><br><br>', options: PREFMASTERSTATEDETERMINATION, defaultValue: 'allSame'
        input name: 'loggingOption', type: 'enum', title: 'Logging Option', description: '<small>Sets the logging level cumulatively, for example "Driver Trace Logging" will include all logging levels below it.</small><br><br>', options: PREFLOGGING, defaultValue: '5'
        input name: 'deviceConfigurationOptions', type: 'hidden', title: '<strong>Device Configuration Options</strong>', description: '<small>The following options change the behaviour of the device itself, they take effect after hitting "<strong>Save Preferences</strong> below", followed by "<strong>Configure</strong>" above.<br><br>For a battery powered device, you may also need to wake it up manually!</small>'
        input name: 'deviceConfigPowerReportEnable', type: 'bool', title: 'Enable Power Reporting', description: '<small>Enable the device to report instantaneous power readings.</small><br><br>', defaultValue: false
        input name: 'deviceConfigPowerReportChange', type: 'enum', title: 'Power Change (W)', description: '<small>Report instantaneous power readings that change by at least this value.</small><br><br>', options: PREFPOWERCHANGE, defaultValue: '10'
        input name: 'deviceConfigPowerReportTime', type: 'enum', title: 'Power Time (s)', description: '<small>Periodically report instantaneous power reading according to this timing.</small><br><br>', options: PREFREPORTTIME, defaultValue: '300'
        input name: 'deviceConfigVoltageReportEnable', type: 'bool', title: 'Enable Voltage Reporting', description: '<small>Enable the device to report voltage readings.</small><br><br>', defaultValue: false
        input name: 'deviceConfigVoltageReportChange', type: 'enum', title: 'Voltage Change (V)', description: '<small>Report voltage readings that change by at least this value.</small><br><br>', options: PREFVOLTAGECHANGE, defaultValue: '5'
        input name: 'deviceConfigVoltageReportTime', type: 'enum', title: 'Voltage Time (s)', description: '<small>Periodically report voltage reading according to this timing.</small><br><br>', options: PREFREPORTTIME, defaultValue: '600'
        input name: 'deviceConfigCurrentReportEnable', type: 'bool', title: 'Enable Current Reporting', description: '<small>Enable the device to report current readings.</small><br><br>', defaultValue: false
        input name: 'deviceConfigCurrentReportChange', type: 'enum', title: 'Current Change (A)', description: '<small>Report current readings that change by at least this value.</small><br><br>', options: PREFCURRENTCHANGE, defaultValue: '0.1'
        input name: 'deviceConfigCurrentReportTime', type: 'enum', title: 'Current Time (s)', description: '<small>Periodically report current reading according to this timing.</small><br><br>', options: PREFREPORTTIME, defaultValue: '900'
        input name: 'deviceConfigEnergyReportEnable', type: 'bool', title: 'Enable Energy Reporting', description: '<small>Enable the device to report eneergy readings.</small><br><br>', defaultValue: false
        input name: 'deviceConfigEnergyReportChange', type: 'enum', title: 'Energy Change (kWh)', description: '<small>Report energy readings that change by at least this value.</small><br><br>', options: PREFENERGYCHANGE, defaultValue: '0.5'
        input name: 'deviceConfigEnergyReportTime', type: 'enum', title: 'Energy Time (s)', description: '<small>Periodically report energy reading according to this timing.</small><br><br>', options: PREFREPORTTIME, defaultValue: '3600'
        input name: 'deviceConfigSwitchInputType', type: 'enum', title: 'Switch Input Type', description: '<small>Select the type of switch connected to the inputs. <strong>Power cycle the module for the setting to take effect.</strong></small><br><br>', options: PREFSWITCHINPUTTYPE, defaultValue: 'toggle'
        input name: 'platformOptions', type: 'hidden', title: '<strong>Platform Options</strong>', description: '<small>The following options are relevant to the Hubitat platform and UI itself.</small>'
    }
}

import groovy.transform.Field
import com.hubitat.app.ChildDeviceWrapper
import com.hubitat.app.DeviceWrapper

private @Field final String CANDEO = 'Candeo C-ZB-SM205-2G Device Driver'
private @Field final Boolean DEBUG = false
private @Field final Integer LOGSOFF = 1800
private @Field final Integer ZIGBEEDELAY = 1000
private @Field final Map PREFFALSE = [value: 'false', type: 'bool']
private @Field final Map PREFTRUE = [value: 'true', type: 'bool']
private @Field final Map PREF1000 = [value: '1000', type: 'enum']
private @Field final Map PREF10 = [value: '10', type: 'enum']
private @Field final Map PREF5 = [value: '5', type: 'enum']
private @Field final Map PREFSWITCHINPUTTYPE = [ 'momentary': 'Momentary', 'toggle': 'Toggle' ]
private @Field final Map PREFHUBRESTART = [ 'off': 'Off', 'on': 'On', 'refresh': 'Refresh State Only', 'nothing': 'Do Nothing' ]
private @Field final Map PREFMASTERSTATEDETERMINATION = [ 'anyOn': 'If Any Child Is On, Set Master On', 'anyOff': 'If Any Child Is Off, Set Master Off', 'allSame': 'Only Change The State If All Children Are In The Same State']
private @Field final Map PREFREPORTTIME = ['10': '10s', '20': '20s', '30': '30s', '40': '40s', '50': '50s', '60': '60s', '90': '90s', '120': '120s', '240': '240s', '300': '300s', '600': '600s', '900': '900s', '1800': '1800s', '3600': '3600s']
private @Field final Map PREFPOWERCHANGE = ['1': '1W', '2': '2W', '3': '3W', '4': '4W', '5': '5W', '6': '6W', '7': '7W', '8': '8W', '9': '9W', '10': '10W', '15': '15W', '20': '20W']
private @Field final Map PREFVOLTAGECHANGE = ['1': '1V', '2': '2V', '3': '3V', '4': '4V', '5': '5V', '6': '6V', '7': '7V', '8': '8V', '9': '9V', '10': '10V', '15': '15V', '20': '20V']
private @Field final Map PREFCURRENTCHANGE = ['0.1': '0.1A', '0.2': '0.2A', '0.3': '0.3A', '0.4': '0.4A', '0.5': '0.5A', '0.6': '0.6A', '0.7': '0.7A', '0.8': '0.8A', '0.9': '0.9A', '1': '1A', '1.5': '1.5A', '2': '2A']
private @Field final Map PREFENERGYCHANGE = ['0.1': '0.1kWh', '0.2': '0.2kWh', '0.3': '0.3kWh', '0.4': '0.4kWh', '0.5': '0.5kWh', '0.6': '0.6kWh', '0.7': '0.7kWh', '0.8': '0.8kWh', '0.9': '0.9kWh', '1': '1kWh']
private @Field final Map PREFLOGGING = ['0': 'Device Event Logging', '1': 'Driver Informational Logging', '2': 'Driver Warning Logging', '3': 'Driver Error Logging', '4': 'Driver Debug Logging', '5': 'Driver Trace Logging' ]
private @Field final String PARENTEP = '0B'
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
    device.updateSetting('masterStateDetermination', [value: 'allSame', type: 'enum'])
    logInfo("masterStateDetermination setting is: ${PREFMASTERSTATEDETERMINATION[masterStateDetermination]}")
    device.updateSetting('deviceConfigSwitchInputType', [value: 'toggle', type: 'enum'])
    logInfo("deviceConfigSwitchInputType setting is: ${PREFSWITCHINPUTTYPE[deviceConfigSwitchInputType]}")
    device.updateSetting('deviceConfigPowerReportEnable', PREFFALSE)
    logInfo('deviceConfigPowerReportEnable setting is: false')
    device.updateSetting('deviceConfigPowerReportChange', PREF10)
    logInfo("deviceConfigPowerReportChange setting is: ${PREFPOWERCHANGE[deviceConfigPowerReportChange]}")
    device.updateSetting('deviceConfigPowerReportTime', [value: '300', type: 'enum'])
    logInfo("deviceConfigPowerReportTime setting is: ${PREFREPORTTIME[deviceConfigPowerReportTime]}")
    device.updateSetting('deviceConfigVoltageReportEnable', PREFFALSE)
    logInfo('deviceConfigVoltageReportEnable setting is: false')
    device.updateSetting('deviceConfigVoltageReportChange', PREF5)
    logInfo("deviceConfigVoltageReportChange setting is: ${PREFVOLTAGECHANGE[deviceConfigVoltageReportChange]}")
    device.updateSetting('deviceConfigVoltageReportTime', [value: '600', type: 'enum'])
    logInfo("deviceConfigVoltageReportTime setting is: ${PREFREPORTTIME[deviceConfigVoltageReportTime]}")
    device.updateSetting('deviceConfigCurrentReportEnable', PREFFALSE)
    logInfo('deviceConfigCurrentReportEnable setting is: false')
    device.updateSetting('deviceConfigCurrentReportChange', [value: '0.1', type: 'enum'])
    logInfo("deviceConfigCurrentReportChange setting is: ${PREFCURRENTCHANGE[deviceConfigCurrentReportChange]}")
    device.updateSetting('deviceConfigCurrentReportTime', [value: '900', type: 'enum'])
    logInfo("deviceConfigCurrentReportTime setting is: ${PREFREPORTTIME[deviceConfigCurrentReportTime]}")
    device.updateSetting('deviceConfigEnergyReportEnable', PREFFALSE)
    logInfo('deviceConfigEnergyReportEnable setting is: false')
    device.updateSetting('deviceConfigEnergyReportChange', [value: '0.5', type: 'enum'])
    logInfo("deviceConfigEnergyReportChange setting is: ${PREFENERGYCHANGE[deviceConfigEnergyReportChange]}")
    device.updateSetting('deviceConfigEnergyReportTime', [value: '3600', type: 'enum'])
    logInfo("deviceConfigEnergyReportTime setting is: ${PREFREPORTTIME[deviceConfigEnergyReportTime]}")
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
    logInfo("deviceConfigSwitchInputType setting is: ${PREFSWITCHINPUTTYPE[deviceConfigSwitchInputType ?: 'toggle']}", true)
    logInfo("deviceConfigPowerReportEnable setting is: ${deviceConfigPowerReportEnable == true}", true)
    if (deviceConfigPowerReportEnable) {
        logInfo("deviceConfigPowerReportChange setting is: ${PREFPOWERCHANGE[deviceConfigPowerReportChange ?: '10']}", true)
        logInfo("deviceConfigPowerReportTime setting is: ${PREFREPORTTIME[deviceConfigPowerReportTime ?: '300']}", true)
    }
    logInfo("deviceConfigVoltageReportEnable setting is: ${deviceConfigVoltageReportEnable == true}", true)
    if (deviceConfigVoltageReportEnable) {
        logInfo("deviceConfigVoltageReportChange setting is: ${PREFVOLTAGECHANGE[deviceConfigVoltageReportChange ?: '5']}", true)
        logInfo("deviceConfigVoltageReportTime setting is: ${PREFREPORTTIME[deviceConfigVoltageReportTime ?: '600']}", true)
    }
    logInfo("deviceConfigCurrentReportEnable setting is: ${deviceConfigCurrentReportEnable == true}", true)
    if (deviceConfigCurrentReportEnable) {
        logInfo("deviceConfigCurrentReportChange setting is: ${PREFCURRENTCHANGE[deviceConfigCurrentReportChange ?: '0.1']}", true)
        logInfo("deviceConfigCurrentReportTime setting is: ${PREFREPORTTIME[deviceConfigCurrentReportTime ?: '900']}", true)
    }
    logInfo("deviceConfigEnergyReportEnable setting is: ${deviceConfigEnergyReportEnable == true}", true)
    if (deviceConfigEnergyReportEnable) {
        logInfo("deviceConfigEnergyReportChange setting is: ${PREFENERGYCHANGE[deviceConfigEnergyReportChange ?: '0.5']}", true)
        logInfo("deviceConfigEnergyReportTime setting is: ${PREFREPORTTIME[deviceConfigEnergyReportTime ?: '3600']}", true)
    }
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
    logDebug("switch input type is ${deviceConfigSwitchInputType ?: 'toggle'}")
    Map<String, String> deviceConfigSwitchInputTypes = ['momentary': 0x00, 'toggle': 0x01]
    logDebug("switchInputType: ${deviceConfigSwitchInputTypes[deviceConfigSwitchInputType ?: 'toggle']}")
    List<String> cmds = [//switchinputtype
                         "he raw 0x${device.deviceNetworkId} 0x01 0x${PARENTEP} 0x0000 {04 24 12 00 00 03 88}", "delay ${ZIGBEEDELAY}",
                         "he wattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0000 0x8803 ${convertToHexString(DataType.UINT8)} {${deviceConfigSwitchInputTypes[deviceConfigSwitchInputType ?: 'toggle']}} {1224}", "delay ${ZIGBEEDELAY}",
                         "he raw 0x${device.deviceNetworkId} 0x01 0x${PARENTEP} 0x0000 {04 24 12 00 00 03 88}", "delay ${ZIGBEEDELAY}"]
    if (deviceConfigPowerReportEnable || deviceConfigVoltageReportEnable || deviceConfigCurrentReportEnable) {
        cmds += ["zdo bind 0x${device.deviceNetworkId} 0x${PARENTEP} 0x01 0x0B04 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}"]
    }
    else {
        cmds += ["zdo unbind 0x${device.deviceNetworkId} 0x${PARENTEP} 0x01 0x0B04 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}"]
    }
    if (deviceConfigPowerReportEnable) {
        logDebug("power change is: ${deviceConfigPowerReportChange ?: '10'}W") //10 = 1W
        logDebug("power time is: ${deviceConfigPowerReportTime ?: '300'}s")
        Integer powerChange = ((deviceConfigPowerReportChange ?: 10).toBigDecimal() / 1 * 10).toInteger()
        logDebug("powerChange: ${powerChange}")
        if (powerChange == 0) {
            logDebug('powerChange is ZERO, protecting against report flooding!')
            powerChange = 100
        }
        Integer powerTime = deviceConfigPowerReportTime ? deviceConfigPowerReportTime.toInteger() : 300
        cmds += ["he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0604 {}", "delay ${ZIGBEEDELAY}", //1
                "he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0605 {}", "delay ${ZIGBEEDELAY}", //10
                "he cr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x050B ${DataType.INT16} 5 ${powerTime} {${convertToHexString(powerChange, 2, true)}}", "delay ${ZIGBEEDELAY}",
                "he raw 0x${device.deviceNetworkId} 1 0x${PARENTEP} 0x0B04 {10 00 08 00 0B 05}", "delay ${ZIGBEEDELAY}",
                "he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x050B {}", "delay ${ZIGBEEDELAY}"]
    }
    else {
        cmds += ["he cr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x050B ${DataType.INT16} 0 65535 {0} {}", "delay ${ZIGBEEDELAY}",
                "he raw 0x${device.deviceNetworkId} 1 0x${PARENTEP} 0x0B04 {10 00 08 00 0B 05}", "delay ${ZIGBEEDELAY}"]
    }
    if (deviceConfigVoltageReportEnable) {
        logDebug("voltage change is: ${deviceConfigVoltageReportChange ?: '5'}V") //10 = 1V
        logDebug("voltage time is: ${deviceConfigVoltageReportTime ?: '600'}s")
        Integer voltageChange = ((deviceConfigVoltageReportChange ?: 5).toBigDecimal() / 1 * 10).toInteger()
        logDebug("voltageChange: ${voltageChange}")
        if (voltageChange == 0) {
            logDebug('voltageChange is ZERO, protecting against report flooding!')
            voltageChange = 50
        }
        Integer voltageTime = deviceConfigVoltageReportTime ? deviceConfigVoltageReportTime.toInteger() : 600
        cmds += ["he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0600 {}", "delay ${ZIGBEEDELAY}", //1
                "he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0601 {}", "delay ${ZIGBEEDELAY}", //10
                "he cr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0505 ${DataType.UINT16} 5 ${voltageTime} {${convertToHexString(voltageChange, 2, true)}}", "delay ${ZIGBEEDELAY}",
                "he raw 0x${device.deviceNetworkId} 1 0x${PARENTEP} 0x0B04 {10 00 08 00 05 05}", "delay ${ZIGBEEDELAY}",
                "he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0505 {}", "delay ${ZIGBEEDELAY}"]
    }
    else {
        cmds += ["he cr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0505 ${DataType.UINT16} 0 65535 {0} {}", "delay ${ZIGBEEDELAY}",
                "he raw 0x${device.deviceNetworkId} 1 0x${PARENTEP} 0x0B04 {10 00 08 00 05 05}", "delay ${ZIGBEEDELAY}"]
    }
    if (deviceConfigCurrentReportEnable) {
        logDebug("current change is: ${deviceConfigCurrentReportChange ?: '0.1'}A") //1000 = 1A
        logDebug("current time is: ${deviceConfigCurrentReportTime ?: '900'}s")
        Integer currentChange = ((deviceConfigCurrentReportChange ?: 0.1).toBigDecimal() / 1 * 1000).toInteger()
        logDebug("currentChange: ${currentChange}")
        if (currentChange == 0) {
            logDebug('currentChange is ZERO, protecting against report flooding!')
            currentChange = 100
        }
        Integer currentTime = deviceConfigCurrentReportTime ? deviceConfigCurrentReportTime.toInteger() : 900
        cmds += ["he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0602 {}", "delay ${ZIGBEEDELAY}", //1
                "he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0603 {}", "delay ${ZIGBEEDELAY}", //1000
                "he cr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0508 ${DataType.UINT16} 5 ${currentTime} {${convertToHexString(currentChange, 2, true)}}", "delay ${ZIGBEEDELAY}",
                "he raw 0x${device.deviceNetworkId} 1 0x${PARENTEP} 0x0B04 {10 00 08 00 08 05}", "delay ${ZIGBEEDELAY}",
                "he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0508 {}", "delay ${ZIGBEEDELAY}"]
    }
    else {
        cmds += ["he cr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0508 ${DataType.UINT16} 0 65535 {0} {}", "delay ${ZIGBEEDELAY}",
                "he raw 0x${device.deviceNetworkId} 1 0x${PARENTEP} 0x0B04 {10 00 08 00 08 05}", "delay ${ZIGBEEDELAY}"]
    }
    if (deviceConfigEnergyReportEnable) {
        logDebug("energy change is: ${deviceConfigEnergyReportChange ?: '0.5'}kWh") //3600000 = 1kWh
        logDebug("energy time is: ${deviceConfigEnergyReportTime ?: '3600'}s")
        Integer energyChange = ((deviceConfigEnergyReportChange ?: 0.5).toBigDecimal() / 1 * 3600000).toInteger()
        logDebug("energyChange: ${energyChange}")
        if (energyChange == 0) {
            logDebug('energyChange is ZERO, protecting against report flooding!')
            energyChange = 1800000
        }
        Integer energyTime = deviceConfigEnergyReportTime ? deviceConfigEnergyReportTime.toInteger() : 3600
        cmds += ["zdo bind 0x${device.deviceNetworkId} 0x${PARENTEP} 0x01 0x0702 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",
                "he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0702 0x0300 {}", "delay ${ZIGBEEDELAY}",
                "he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0702 0x0301 {}", "delay ${ZIGBEEDELAY}", //1
                "he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0702 0x0302 {}", "delay ${ZIGBEEDELAY}", //3600000
                "he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0702 0x0303 {}", "delay ${ZIGBEEDELAY}",
                "he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0702 0x0304 {}", "delay ${ZIGBEEDELAY}",
                "he cr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0702 0x0000 ${DataType.UINT48} 5 ${energyTime} {${convertToHexString(energyChange, 4, true)}}", "delay ${ZIGBEEDELAY}",
                "he raw 0x${device.deviceNetworkId} 1 0x${PARENTEP} 0x0702 {10 00 08 00 00 00}", "delay ${ZIGBEEDELAY}",
                "he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0702 0x0000 {}", "delay ${ZIGBEEDELAY}"]
    }
    else {
        cmds += ["zdo unbind 0x${device.deviceNetworkId} 0x${PARENTEP} 0x01 0x0702 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",
                 "he cr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0702 0x0000 ${DataType.UINT48} 0 65535 {0} {}", "delay ${ZIGBEEDELAY}",
                 "he raw 0x${device.deviceNetworkId} 1 0x${PARENTEP} 0x0702 {10 00 08 00 00 00}", "delay ${ZIGBEEDELAY}"]
    }
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
    if (deviceConfigPowerReportEnable) {
        cmds += ["he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x050B {}", "delay ${ZIGBEEDELAY}"]
    }
    if (deviceConfigVoltageReportEnable) {
        cmds += ["he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0505 {}", "delay ${ZIGBEEDELAY}"]
    }
    if (deviceConfigCurrentReportEnable) {
        cmds += ["he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0B04 0x0508 {}", "delay ${ZIGBEEDELAY}"]
    }
    if (deviceConfigEnergyReportEnable) {
        cmds += ["he rattr 0x${device.deviceNetworkId} 0x${PARENTEP} 0x0702 0x0000 {}", "delay ${ZIGBEEDELAY}"]
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

void componentOn(DeviceWrapper childDevice) {
    logTrace('componentOn called')
    logDebug("got childDevice: ${childDevice.displayName}")
    String endpoint = childDevice.deviceNetworkId.split('-EP')[1]
    List<String> cmds = ["he cmd 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x01 {}", "delay ${ZIGBEEDELAY}"]
    doZigBeeCommand(cmds)
}

void componentOff(DeviceWrapper childDevice) {
    logTrace('componentOff called')
    logDebug("got childDevice: ${childDevice.displayName}")
    String endpoint = childDevice.deviceNetworkId.split('-EP')[1]
    List<String> cmds = ["he cmd 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x00 {}", "delay ${ZIGBEEDELAY}"]
    doZigBeeCommand(cmds)
}

void componentRefresh(DeviceWrapper childDevice) {
    logTrace('componentRefresh called')
    logDebug("got childDevice: ${childDevice.displayName}")
    String endpoint = childDevice.deviceNetworkId.split('-EP')[1]
    List<String> cmds = ["he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x0000 {}", "delay ${ZIGBEEDELAY}"]
    doZigBeeCommand(cmds)
}

void componentConfigure(DeviceWrapper childDevice) {
    logTrace('componentConfigure called')
    logDebug("got childDevice: ${childDevice.displayName}")
    String endpoint = childDevice.deviceNetworkId.split('-EP')[1]
    String deviceConfigDefaultPowerOnBehaviour = getChildDeviceSettingItem("EP${endpoint}", 'deviceConfigDefaultPowerOnBehaviour') ?: 'previous'
    logDebug("startup on off is ${deviceConfigDefaultPowerOnBehaviour}")
    Map<String, String> startUpOnOff = ['on': 0x01, 'off': 0x0, 'opposite': 0x02, 'previous': 0x03]
    logDebug("startUpOnOff: ${startUpOnOff[deviceConfigDefaultPowerOnBehaviour ?: 'previous']}")
    List<String> cmds = [//onoff
                         "zdo bind 0x${device.deviceNetworkId} 0x${endpoint} 0x01 0x0006 {${device.zigbeeId}} {}", "delay ${ZIGBEEDELAY}",
                         "he cr 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x0000 ${DataType.BOOLEAN} 0 3600 {}", "delay ${ZIGBEEDELAY}",
                         "he raw 0x${device.deviceNetworkId} 1 0x${endpoint} 0x0006 {10 00 08 00 00 00}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x0000 {}", "delay ${ZIGBEEDELAY}",
                         //startupbehaviour
                         "he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x4003 {}", "delay ${ZIGBEEDELAY}",
                         "he wattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x4003 ${convertToHexString(DataType.ENUM8)} {${startUpOnOff[deviceConfigDefaultPowerOnBehaviour ?: 'previous']}} {}", "delay ${ZIGBEEDELAY}",
                         "he rattr 0x${device.deviceNetworkId} 0x${endpoint} 0x0006 0x4003 {}", "delay ${ZIGBEEDELAY}"]
    doZigBeeCommand(cmds)
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
        else if (endpoint == PARENTEP && (descriptionMap.cluster == '0000' || descriptionMap.clusterId == '0000' || descriptionMap.clusterInt == 0)) {
            processBasicEvent(descriptionMap, events)
        }
        else if (endpoint == PARENTEP && (descriptionMap.cluster == '0B04' || descriptionMap.clusterId == '0B04' || descriptionMap.clusterInt == 2820)) {
            processElectricalMeasurementEvent(descriptionMap, events)
        }
        else if (endpoint == PARENTEP && (descriptionMap.cluster == '0702' || descriptionMap.clusterId == '0702' || descriptionMap.clusterInt == 1794)) {
            processSimpleMeteringEvent(descriptionMap, events)
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
            String endpoint = descriptionMap.sourceEndpoint ?: descriptionMap.endpoint ?: 'unknown'
            if (descriptionMap.attrId == '0000' || descriptionMap.attrInt == 0) {
                logDebug('on off (0006) on off report (0000)')
                Integer onOffValue = zigbee.convertHexToInt(descriptionMap.value)
                if (onOffValue == 1 || onOffValue == 0) {
                    logDebug("on off report is ${onOffValue}")
                    String onOffState = onOffValue == 0 ? 'off' : 'on'
                    String currentValue = device.currentValue('switch')
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
                        if (masterState == currentValue) {
                            descriptionText = "${device.displayName} is ${masterState}"
                        }
                        logEvent(descriptionText)
                        String type = 'physical'
                        String action = getChildDeviceStateItem("EP${endpoint}", 'action') ?: 'standby'
                        if (action == 'digitalon' || action == 'digitaloff') {
                            logDebug("action is ${action}")
                            type = 'digital'
                        }
                        events.add(processEvent([name: 'switch', value: masterState, type: type, descriptionText: descriptionText]))
                    }
                    sendEventToChildDevice("EP${endpoint}", 'switch', onOffState)
                }
                else {
                    logDebug("skipping onOffValue: ${onOffValue}")
                }
            }
            else if (descriptionMap.attrId == '4003' || descriptionMap.attrInt == 16387) {
                logDebug('on off (0006) startup on off report (4003)')
                String startUpOnOffValue = descriptionMap.value
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

private void processElectricalMeasurementEvent(Map descriptionMap, List<Map> events) {
    logTrace('processElectricalMeasurementEvent called')
    switch (descriptionMap.command) {
        case '0A':
        case '01':
            if (descriptionMap.attrId == '050B' || descriptionMap.attrInt == 1291) {
                logDebug('electrical measurement (0B04) power report (050B)')
                BigDecimal powerValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("power report is ${powerValue}") //10 = 1W
                powerValue = powerValue / 10
                String descriptionText = "${device.displayName} is ${powerValue} W"
                logEvent(descriptionText)
                if (deviceConfigPowerReportEnable) {
                    events.add(processEvent([name: 'power', value: powerValue, unit: 'W', descriptionText: descriptionText]))
                }
                else {
                    logDebug('skipped raising event for unsolicited power report since the preference is disabled')
                }
            }
            else if (descriptionMap.attrId == '0505' || descriptionMap.attrInt == 1285) {
                logDebug('electrical measurement (0B04) voltage report (0505)')
                BigDecimal voltageValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("voltage report is ${voltageValue}") //10 = 1V
                voltageValue = voltageValue / 10
                String descriptionText = "${device.displayName} is ${voltageValue} V"
                logEvent(descriptionText)
                if (deviceConfigVoltageReportEnable) {
                    events.add(processEvent([name: 'voltage', value: voltageValue, unit: 'V', descriptionText: descriptionText]))
                }
                else {
                    logDebug('skipped raising event for unsolicited voltage report since the preference is disabled')
                }
            }
            else if (descriptionMap.attrId == '0508' || descriptionMap.attrInt == 1288) {
                logDebug('electrical measurement (0B04) current report (0508)')
                BigDecimal currentValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("current report is ${currentValue}") //1000 = 1A
                currentValue = currentValue / 1000
                String descriptionText = "${device.displayName} is ${currentValue} A"
                logEvent(descriptionText)
                if (deviceConfigCurrentReportEnable) {
                    events.add(processEvent([name: 'amperage', value: currentValue, unit: 'A', descriptionText: descriptionText]))
                }
                else {
                    logDebug('skipped raising event for unsolicited current report since the preference is disabled')
                }
            }
            else {
                logDebug('electrical measurement (0B04) attribute skipped')
            }
            break
        case '04':
            logDebug('electrical measurement (0B04) write attribute response (04) skipped')
            break
        case '07':
            logDebug('electrical measurement (0B04) configure reporting response (07) skipped')
            break
        case '0B':
            logDebug('electrical measurement (0B04) default response (0B) skipped')
            break
        default:
            logDebug('electrical measurement (0B04) command skipped')
            break
    }
}

private void processSimpleMeteringEvent(Map descriptionMap, List<Map> events) {
    logTrace('processSimpleMeteringEvent called')
    switch (descriptionMap.command) {
        case '0A':
        case '01':
            if (descriptionMap.attrId == '0000' || descriptionMap.attrInt == 0) {
                logDebug('simple metering (0702) current summation delivered report (0000)')
                BigDecimal energyValue = zigbee.convertHexToInt(descriptionMap.value)
                logDebug("energy report is ${energyValue}") //3600000 = 1kWh
                energyValue = (energyValue / 3600000).setScale(3, BigDecimal.ROUND_HALF_UP)
                String descriptionText = "${device.displayName} is ${energyValue} kWh"
                logEvent(descriptionText)
                if (deviceConfigEnergyReportEnable) {
                    events.add(processEvent([name: 'energy', value: energyValue, unit: 'kWh', descriptionText: descriptionText]))
                }
                else {
                    logDebug('skipped raising event for unsolicited energy report since the preference is disabled')
                }
            }
            else {
                logDebug('simple metering (0702) attribute skipped')
            }
            break
        case '04':
            logDebug('simple metering (0702) write attribute response (04) skipped')
            break
        case '07':
            logDebug('simple metering (0702) configure reporting response (07) skipped')
            break
        case '0B':
            logDebug('simple metering (0702) default response (0B) skipped')
            break
        default:
            logDebug('simple metering (0702) command skipped')
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
        currentValue = childDevice.currentValue(valueItem) ?: 'unknown'
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
        this.addChildDevice('Candeo', 'Candeo C-ZB-SM205-2G Zigbee 2-Gang Switch Module (L+N) Child Switch', "${device.id}-${address}", [name: "${device.displayName} ${address}", label: "${device.displayName} ${address}", isComponent: true])
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
