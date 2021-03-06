/**
 * Add and remove multiple user codes for locks with Scheduling and notification options
 *
 * Copyright RBoy
 * Redistribution of any changes or code is not allowed without permission
 * Version 2.4.2
 *
 */
definition(
		name: "Lock multi user code management with notifications and automatic relock",
		namespace: "rboy",
		author: "RBoy",
		description: "Add and Delete Multiple User Codes for Locks with Scheduling and Notifications",
		category: "Safety & Security",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Allstate/lock_it_when_i_leave.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Allstate/lock_it_when_i_leave@2x.png"
	  )

import groovy.json.JsonSlurper

preferences {
	page(name: "setupApp")
    page(name: "usersPage")
    page(name: "unlockActionsPage")
    page(name: "relockDoorPage")
}

def setupApp() {
	log.trace "$settings"
    
    dynamicPage(name: "setupApp", title: "Lock User Management", install: false, uninstall: true, nextPage: "usersPage") {    
        section("Select Lock(s)") {
            input "locks","capability.lock", title: "Lock", multiple: true,  submitOnChange: true
        }

		section("How many Users do you want to manage (common to all selected locks)?") {
        	input name: "maxUserNames", title: "Max users", type: "number", required: true, multiple: false
        }

        section {
            href(name: "unlockActions", title: "Click here to define actions when the user unlocks the door successfully", page: "unlockActionsPage", description: "", required: false)
            href(name: "relockDoor", title: "Click here to automatically relock the door after closing", page: "relockDoorPage", description: "", required: false)
        }

        section([mobileOnly:true]) {
			label title: "Assign a name for this SmartApp", required: false
		}

		section("Code Programming Option (optional)") {
            paragraph "Change this setting if all the user codes aren't being programmed on the lock correctly. This settings determines the time gap between sending each user code to the lock. If the codes are sent too fast, they may fail to be set properly."
            input name: "sendDelay", title: "Delay between codes (seconds):", type: "number", defaultValue: "15", required: false
        }
    }
}

def relockDoorPage() {
    dynamicPage(name:"relockDoorPage", title: "Select door open/close sensor for each door and configure the automatic relock time after the door is closed", uninstall: false, install: false) {
        section {
            for (lock in locks) {
                def priorRelockDoor = settings."relockDoor${lock}"
                def priorRelockImmediate = settings."relockImmediate${lock}"
                def priorRelockAfter = settings."relockAfter${lock}"
                def priorRetractDeadbolt = settings."retractDeadbolt${lock}"

                paragraph "\r\n"
                paragraph "Configure ${lock}"
                if (priorRelockDoor || priorRetractDeadbolt) {
                    input "sensor${lock}", "capability.contactSensor", title: "Door open/close sensor", required: true
                }
                
                input name: "relockDoor${lock}", type: "bool", title: "Relock door automatically after closing", defaultValue: priorRelockDoor, required: true, submitOnChange: true
                if (priorRelockDoor) {
                    input name: "relockImmediate${lock}", type: "bool", title: "Relock immediately", defaultValue: priorRelockImmediate, required: true, submitOnChange: true
                    if (!priorRelockImmediate) {
                        input name: "relockAfter${lock}", type: "number", title: "Relock after (minutes)", defaultValue: priorRelockAfter, required: true                   
                    }
                }
                
                paragraph "Use this if you want to automatically retract the deadbolt (unlock) if it accidentally extends (locks) while the door is still open. This can avoid damage to the door frame."
                input name: "retractDeadbolt${lock}", type: "bool", title: "Unlock door if locked while open", defaultValue: priorRetractDeadbolt, required: true, submitOnChange: true
            }
        }
    }
}

def unlockActionsPage() {
    dynamicPage(name:"unlockActionsPage", title: "Setup unlock actions for each door", uninstall: false, install: false) {
        def phrases = location.helloHome?.getPhrases()*.label
        if (phrases) {
            phrases.sort()
            section {
                if  (locks?.size() > 1) {
                    input name: "individualDoorActions", title: "Separate unlock actions for each door", type: "bool", required: true,  submitOnChange: true
                }
            }
            if (individualDoorActions) {
                for (lock in locks) {
                    section ("Door Unlock Actions for $lock (optional)") {
                        def priorHomePhrase = settings."homePhrase${lock}"
                        def priorHomeMode = settings."homeMode${lock}"
                        def priorHomeDisarm = settings."homeDisarm${lock}"

                        paragraph "Run these routines and/or change the mode when a user successfully unlocks the door $lock"
                        input name: "homePhrase${lock}", type: "enum", title: "Run Routine", required: false, options: phrases, defaultValue: priorHomePhrase
                        input name: "homeMode${lock}", type: "mode", title: "Change Mode To", required: false, multiple: false, defaultValue: priorHomeMode
                        input name: "homeDisarm${lock}", type: "bool", title: "Disarm Smart Home Monitor", required: false, defaultValue: priorHomeDisarm

                        paragraph "Turn on these lights after dark when a user successfully unlocks the door $lock"
                        input "turnOnSwitchesAfterSunset${lock}", "capability.switch", title: "Turn on light(s) after dark", required: false, multiple: true

                        paragraph "Turn on and/or off these switches/lights when a user successfully unlocks the door $lock"
                        input "turnOnSwitches${lock}", "capability.switch", title: "Turn on switch(s)", required: false, multiple: true
                        input "turnOffSwitches${lock}", "capability.switch", title: "Turn off switch(s)", required: false, multiple: true
                    }
                }
            } else {
                section("Door Unlock Actions (optional)") {
                    paragraph "Run these routines and/or change the mode when a user successfully unlocks the door"
                    input name: "homePhrase", type: "enum", title: "Run Routine", required: false, options: phrases
                    input name: "homeMode", type: "mode", title: "Change Mode To", required: false, multiple: false
                    input name: "homeDisarm", type: "bool", title: "Disarm Smart Home Monitor", required: false

                    paragraph "Turn on these lights after dark when a user successfully unlocks the door"
                    input name: "turnOnSwitchesAfterSunset", type: "capability.switch", title: "Turn on light(s) after dark", required: false, multiple: true

                    paragraph "Turn on and/or off these switches/lights when a user successfully unlocks the door"
                    input name: "turnOnSwitches", type: "capability.switch", title: "Turn on switch(s)", required: false, multiple: true
                    input name: "turnOffSwitches", type: "capability.switch", title: "Turn off switch(s)", required: false, multiple: true
                }
            }        
        }
    }
}

def usersPage() {
	dynamicPage(name:"usersPage", title: "User Names, Codes and Notification Setup", uninstall: true, install: true) {

	section("Notification Options") {
        input name: "sms", title: "Send SMS notification to (optional):", type: "phone", required: false
        paragraph "Enable the below option if you DON'T want push notifications on your SmartThings phone app. This does not impact the SMS notifications."
        input name: "disableAllNotify", title: "Disable all push notifications", type: "bool", defaultValue: "false", required: true
    }
	
    section("Jammed Lock") {
    	input name: "jamNotify", title: "Notify on Lock Jam/Stuck", type: "bool"
    }

    section("Manual Unlock") {
    	paragraph "Get notifications when the door is unlocked manually (inside or outside)"
    	input name: "manualNotify", title: "Notify on Manual Unlock", type: "bool"
        input name: "manualNotifyModes", type: "mode", title: "Only when in this mode(s) (optional)", required: false, multiple: true
    }

    section("Lock") {
    	paragraph "Get notifications when the door is locked (manually or automatically)"
    	input name: "lockNotify", title: "Notify on Lock", type: "bool"
        input name: "lockNotifyModes", type: "mode", title: "Only when in this mode(s) (optional)", required: false, multiple: true
    }

	for (int i = 1; i <= settings.maxUserNames; i++) {
            def priorName = settings."userNames${i}"
            def priorCode = settings."userCodes${i}"
            def priorNotify = settings."userNotify${i}"
            def priorExpireDate = settings."userExpireDate${i}"
            def priorExpireTime = settings."userExpireTime${i}"
            def priorStartDate = settings."userStartDate${i}"
            def priorStartTime = settings."userStartTime${i}"
            def priorUserType = settings."userType${i}"
            def priorUserDayOfWeek = settings."userDayOfWeekA${i}"
            def priorUserStartTime = settings."userStartTimeA${i}"
            def priorUserEndTime = settings."userEndTimeA${i}"
            log.debug "Initial $i Name: $priorName, Code: $priorCode, Notify: $priorNotify, ExpireDate: $priorExpireDate, ExpireTime: $priorExpireTime, StartDate: $priorStartDate, StartTime: $priorStartTime, UserType: $priorUserType, UserDayOfWeek: $priorUserDayOfWeek, UserStartTime: $userStartTime, UserEndTime: $userEndTime"

			// Check for errors and display messages
            section("User Management Slot #${i}") {
                switch (priorUserType) {
                	case 'Expire on':
                    	def invalidDate = true
                        
                        if (priorExpireDate) {
                            log.debug "Found expiry date in setup"
                            try {
                                Date.parse("yyyy-MM-dd", priorExpireDate)
                                invalidDate = false
                            }
                            catch (Exception e) {
                                log.debug "Invalid expiry date in setup"
                                invalidDate = true
                            }
                        }
                        if (priorStartDate) {
                            log.debug "Found start date in setup"
                            try {
                                Date.parse("yyyy-MM-dd", priorStartDate)
                                invalidDate = false
                            }
                            catch (Exception e) {
                                log.debug "Invalid start date in setup"
                                invalidDate = true
                            }
                        }
                        
                        if (invalidDate == true) {
                            paragraph "INVALID DATE - PLEASE CHECK YOUR DATE FORMAT"
                        } else {
                            if (priorExpireDate) {
                            	if (priorExpireTime) {
                                    def midnightToday = timeToday("2000-01-01T00:00:00.000-0000", location.timeZone)
                                    String dst = location.timeZone.getDisplayName(location.timeZone.inDaylightTime(new Date(now())), TimeZone.SHORT) // Keep current timezone
                                    def expT = (timeToday(priorExpireTime, location.timeZone).time - midnightToday.time)
                                    def expD = Date.parse("yyyy-MM-dd Z", priorExpireDate + " " + dst).toCalendar()
                                    def exp = expD.getTimeInMillis() + expT
                                    def expStr = (new Date(exp)).format("EEE MMM dd yyyy HH:mm z", location.timeZone)
                                    if (exp < now()) {
                                        paragraph "Code EXPIRED!"
                                    } else {
		                                paragraph "Code will expire on ${expStr}"
                                    }
                                } else {
	                                paragraph "PLEASE ENTER EXPIRE TIME"
                                }
                                if (priorStartDate) {
                                    def sd = Date.parse("yyyy-MM-dd", priorStartDate)
                                    paragraph "Code will be active on ${sd.format("EEE MMM dd yyyy")}"
                                    if (!priorStartTime) {
                                        paragraph "PLEASE ENTER START TIME"
                                    }
                                }
                            } else {
                                paragraph "PLEASE ENTER EXPIRE DATE"
                            }
                        }
                        break
                        
                	case 'One time':
                        if (state.trackUsedOneTimeCodes?.contains(i as String)) {
                            paragraph "One time code USED!"
                        }
                        break
                        
                    default:
                    	break
                }

				if (priorCode) {
                    input name: "userNames${i}", description: "${priorName}", title: "Name", defaultValue: priorName, type: "text", multiple: false, required: false
                    input name: "userCodes${i}", description: "${priorCode}", title: "Code", defaultValue: priorCode, type: "text", multiple: false, required: false
                    input name: "userNotify${i}", title: "Notify", defaultValue: priorNotify, type: "bool"
                    input name: "userType${i}",
                        type: "enum",
                        title: "Select User Type",
                        required: true,
                        multiple: false,
                        options: codeOptions(),
                        defaultValue: priorUserType,
                        submitOnChange: true
                } else {
                    input name: "userNames${i}", description: "Tap to set", title: "Name", type: "text", multiple: false, required: false
                    input name: "userCodes${i}", description: "Tap to set", title: "Code", type: "text", multiple: false, required: false
                    input name: "userNotify${i}", title: "Notify", type: "bool"
                    input name: "userType${i}",
                        type: "enum",
                        title: "Select User Type",
                        required: true,
                        multiple: false,
                        options: codeOptions(),
                        defaultValue: 'Permanent',
                        submitOnChange: true
                }
                
                switch (priorUserType) {
                	case 'Expire on':
                        if (priorStartDate) { // Start Date/Time is optional
                            input name: "userStartDate${i}", title: "Code start date (YYYY-MM-DD) (optional)", description: "Date on which the code should be enabled",  defaultValue: priorStartDate, type: "date", required: false,  submitOnChange: true
                        } else {
                            input name: "userStartDate${i}", title: "Code start date (YYYY-MM-DD) (optional)", description: "Date on which the code should be enabled", type: "date", required: false,  submitOnChange: true
                        }

                        if (priorStartTime) {
                            input name: "userStartTime${i}", title: "Code start time (optional)", description: "(Touch here to set time) The code would be enabled within 5 minutes of this time", defaultValue: priorStartTime, type: "time", required: false,  submitOnChange: true
                        } else {
                            input name: "userStartTime${i}", title: "Code start time (optional)", description: "(Touch here to set time) The code would be enabled within 5 minutes of this time", type: "time", required: false,  submitOnChange: true
                        }

                    	if (priorExpireDate) {
                            input name: "userExpireDate${i}", title: "Code expiration date (YYYY-MM-DD)", description: "Date on which the code should be deleted",  defaultValue: priorExpireDate, type: "date", required: true,  submitOnChange: true
                        } else {
                            input name: "userExpireDate${i}", title: "Code expiration date (YYYY-MM-DD)", description: "Date on which the code should be deleted", type: "date", required: true,  submitOnChange: true
                        }

                        if (priorExpireTime) {
                            input name: "userExpireTime${i}", title: "Code expiration time", description: "(Touch here to set time) The code would be deleted within 5 minutes of this time", defaultValue: priorExpireTime, type: "time", required: true,  submitOnChange: true
                        } else {
                            input name: "userExpireTime${i}", title: "Code expiration time", description: "(Touch here to set time) The code would be deleted within 5 minutes of this time", type: "time", required: true,  submitOnChange: true
                        }
                        break
                        
                    case 'Scheduled':
                        input "userStartTimeA${i}", "time", title: "Start Time", required: false
                        input "userEndTimeA${i}", "time", title: "End Time", required: false
                        input name: "userDayOfWeekA${i}",
                            type: "enum",
                            title: "Which day of the week?",
                            required: true,
                            multiple: true,
                            options: [
                                'All Week',
                                'Monday to Friday',
                                'Saturday & Sunday',
                                'Monday',
                                'Tuesday',
                                'Wednesday',
                                'Thursday',
                                'Friday',
                                'Saturday',
                                'Sunday'
                            ],
                            defaultValue: priorUserDayOfWeek
                    	break
                    
                    default:
	                    break
                }
            }
        } 
	} 
}

def codeOptions() {
    def ret = [
        "Permanent",
        "One time",
        "Expire on",
        "Scheduled",
        "Inactive"
    ]

    return ret
}

def installed()
{
	log.debug "Install Settings: $settings"
	appTouch()
}

def updated()
{
	log.debug "Update Settings: $settings"
	appTouch()
}

def appTouch() {
	unschedule() // clear all pending updates
    unsubscribe()
	subscribe(location, modeChangeHandler)
    subscribe(locks, "lock", doorHandler)
    locks.each { lock -> // check each lock individually
        if (settings."sensor${lock}") {
        	log.trace "Subscribing to sensor ${settings."sensor${lock}"} for ${lock}"
        	subscribe(settings."sensor${lock}", "contact.closed", sensorHandler)
        }
        if (lock.hasAttribute('invalidCode')) {
            log.trace "Found attribute 'invalidCode' on lock $lock, enabled support for invalid code detection"
            subscribe(lock, "invalidCode", doorHandler)
        }
    }
    
    state.usedOneTimeCodes = [:]
    state.trackUsedOneTimeCodes = [] // Track for reporting purposes
    state.activeScheduledCodes = [:]
    state.expiredCodes = [:]
    state.startCodes = [:]

    atomicState.reLocks = [] // List of lock to relock
	state.updateLockList = []
    state.expiredLockList = []
    for (lock in locks) {
    	state.expiredCodes[lock.id] = [] // List of expired codes for this lock
        state.startCodes[lock.id] = [] // List of start codes for this lock
        state.usedOneTimeCodes[lock.id] = [] // List of used one time codes for this lock
        state.activeScheduledCodes[lock.id] = [] // List of active scheduled codes for this lock
        state.updateLockList.add(lock.id) // reset the state for each lock to be processed with update
        state.expiredLockList.add(lock.id) // reset the state for each lock to be processed with expired
        log.trace "Added $lock id ${lock.id} to unprocessed locks update list ${state.updateLockList} and expire list ${state.expiredLockList}"
    }
    state.updateNextCode = 1 // set next code to be set for the update loop
    state.expiredNextCode = 1 // set next code to be set for the expired loop
    
    runIn(1, updateCodes) // Updates codes
    
    log.debug "Initialization complete, scheduling code updates starting with code $state.updateNextCode in 1 second"
}

def sensorHandler(evt)
{
	def data = []
    def sensor = evt.device

	log.trace "Event name $evt.name, value $evt.value, device $evt.displayName"
    
    if (evt.value == "closed") { // Door was closed
        def reLock = locks.find { settings."sensor${it}"?.id == sensor.id } // Find the lock for this sensor, match by ID and not objects
        log.debug "Sensor ${sensor} belongs to Lock ${reLock}"
        
        if (reLock && settings."relockDoor${reLock}") { // Are we asked to reLock this door
        	if (settings."relockImmediate${reLock}") {
            	log.info "Relocking ${reLock} immediately"
                reLock.lock()
            } else if (settings."relockAfter${reLock}") {
            	log.debug "Scheduling ${reLock} to lock in ${settings."relockAfter${reLock}"} minutes"
                def relocks = atomicState.reLocks // We need to deference the atomicState object each time, https://community.smartthings.com/t/atomicstate-not-working/27827/6?u=rboy
                if (!relocks.contains(reLock.id)) { // Don't re add the same lock again
                	log.trace "Adding ${reLock.id} to the list of relocks"
                	relocks.add(reLock.id) // Atomic to ensure we get upto date info here
                    atomicState.reLocks = relocks // Set it back, we can't work direct on atomicState
                }
                runIn(60 * settings."relockAfter${reLock}", reLockDoor) // Schedule the relock in minutes
            } else {
            	log.error "Invalid configuration, no relock timeout defined"
            }
        }
    }
}

def reLockDoor() {
    def reLocksIDs = atomicState.reLocks // We need to deference the atomicState object each time, https://community.smartthings.com/t/atomicstate-not-working/27827/6?u=rboy
	log.trace "Checking door sensor state and relocking ${reLocksIDs}"
    
    reLocksIDs?.each { lockid ->
    	def lock = locks.find { it.id == lockid } // find the lock
        def lockSensor = settings."sensor${lock}" // Get the sensor for the lock
    	if (lockSensor.latestValue("contact") == "closed") {
        	log.info "Sensor ${lockSensor} is reporting door ${lock} is closed, locking the door"
            lock.lock() // lock it
            def relocks = atomicState.reLocks // We need to deference the atomicState object each time, https://community.smartthings.com/t/atomicstate-not-working/27827/6?u=rboy
            log.trace "Removing ${lockid} from the list of relocks"
            relocks.remove(lockid) // We are done with this lock, remove it from the list
            atomicState.reLocks = relocks // set it back to atomicState
        } else {
        	log.debug "Sensor ${lockSensor} is reporting door ${lock} is not closed, will check again in 1 minute"
            runIn(60, reLockDoor) // Check back again in a minute
        }
    }
}

def doorHandler(evt)
{
	def data = []
    def lock = evt.device

	log.debug "Event name $evt.name, value $evt.value, device $evt.displayName, data $evt.data"
    
	if (evt.name == "lock") {
    	if (evt.value == "unlocked") {
            def isManual = false
	    	if ((evt.data == "") || (evt.data == null)) {  				// No extended data, must be a manual/keyed unlock
            	isManual = true
            }
            else {														// We have extended data, should be a coded unlock           	
	    		data = new JsonSlurper().parseText(evt.data) 
            	if ((data.usedCode == "") || (data.usedCode == null)) {	// If no usedCode data, treat as manual unlock
                	log.debug "Unknown extended data (${data}), treating as manual unlock"
                	isManual = true
           		 }
            }
            
            if (isManual) {
            	log.debug "$evt.displayName was unlocked manually"

                if (manualNotify && (manualNotifyModes ? manualNotifyModes.find{it == location.currentMode} : true)) {
                    if (!disableAllNotify) {
                        sendPush "$evt.displayName was unlocked manually"
                    } else {
                        sendNotificationEvent("$evt.displayName was unlocked manually")
                    }
                    if (sms) {
                        sendSms(sms, "$evt.displayName was unlocked manually")
                    }
                }
            }
            else {
            	Integer i = data.usedCode as Integer
                def userName = settings."userNames${i}"
                def notify = settings."userNotify${i}"
                
                log.debug "Lock $evt.displayName unlocked by $userName, notify $notify"
                
                // First disarm SHM since it goes off due to other events
                if (individualDoorActions) {
                    if (settings."homeDisarm${lock}") {
                        log.info "Disarming Smart Home Monitor"
                        sendLocationEvent(name: "alarmSystemStatus", value: "off") // First do this to avoid false alerts from a slow platform
                        sendNotificationEvent("$evt.displayName was unlocked successfully,  disarming Smart Home Monitor")
                    }

                    if (settings."homeMode${lock}") {
                        log.info "Changing mode to ${settings."homeMode${lock}"}"
                        if (location.modes?.find{it.name == settings."homeMode${lock}"}) {
                            setLocationMode(settings."homeMode${lock}") // First do this to avoid false alerts from a slow platform
                        }  else {
                            log.warn "Tried to change to undefined mode '${settings."homeMode${lock}"}'"
                        }
                        sendNotificationEvent("$evt.displayName was unlocked successfully, changing mode to ${settings."homeMode${lock}"}")
                    }

                    if (settings."homePhrase${lock}") {
                        log.info "Running unlock Phrase ${settings."homePhrase${lock}"}"
                        location.helloHome.execute(settings."homePhrase${lock}") // First do this to avoid false alerts from a slow platform
                        sendNotificationEvent("$evt.displayName was unlocked successfully, running ${settings."homePhrase${lock}"}")
                    }

                    if (settings."turnOnSwitchesAfterSunset${lock}") {
                        def cdt = new Date(now())
                        def sunsetSunrise = getSunriseAndSunset(sunsetOffset: "-00:30") // Turn on 30 minutes before sunset (dark)
                        log.trace "Current DT: $cdt, Sunset $sunsetSunrise.sunset, Sunrise $sunsetSunrise.sunrise"
                        if ((cdt >= sunsetSunrise.sunset) || (cdt <= sunsetSunrise.sunrise)) {
                            log.info "$evt.displayName was unlocked successfully, turning on lights ${settings."turnOnSwitchesAfterSunset${lock}"} since it's after sunset but before sunrise"
                            settings."turnOnSwitchesAfterSunset${lock}"?.on()
                        }
                    }

                    if (settings."turnOnSwitches${lock}") {
                        log.info "$evt.displayName was unlocked successfully, turning on switches ${settings."turnOnSwitches${lock}"}"
                        settings."turnOnSwitches${lock}"?.on()
                    }

                    if (settings."turnOffSwitches${lock}") {
                        log.info "$evt.displayName was unlocked successfully, turning off switches ${settings."turnOffSwitches${lock}"}"
                        settings."turnOffSwitches${lock}"?.off()
                    }
                } else {
                    if (homeDisarm) {
                        log.info "Disarming Smart Home Monitor"
                        sendLocationEvent(name: "alarmSystemStatus", value: "off") // First do this to avoid false alerts from a slow platform
                        sendNotificationEvent("$evt.displayName was unlocked successfully,  disarming Smart Home Monitor")
                    }

                    if (homeMode) {
                        log.info "Changing mode to ${settings.homeMode}"
                        if (location.modes?.find{it.name == homeMode}) {
                            setLocationMode(homeMode) // First do this to avoid false alerts from a slow platform
                        }  else {
                            log.warn "Tried to change to undefined mode '${homeMode}'"
                        }
                        sendNotificationEvent("$evt.displayName was unlocked successfully, changing mode to ${settings.homeMode}")
                    }

                    if (homePhrase) {
                        log.info "Running unlock Phrase ${settings.homePhrase}"
                        location.helloHome.execute(settings.homePhrase) // First do this to avoid false alerts from a slow platform
                        sendNotificationEvent("$evt.displayName was unlocked successfully, running ${settings.homePhrase}")
                    }

                    if (turnOnSwitchesAfterSunset) {
                        def cdt = new Date(now())
                        def sunsetSunrise = getSunriseAndSunset(sunsetOffset: "-00:30") // Turn on 30 minutes before sunset (dark)
                        log.trace "Current DT: $cdt, Sunset $sunsetSunrise.sunset, Sunrise $sunsetSunrise.sunrise"
                        if ((cdt >= sunsetSunrise.sunset) || (cdt <= sunsetSunrise.sunrise)) {
                            log.info "$evt.displayName was unlocked successfully, turning on lights $turnOnSwitchesAfterSunset since it's after sunset but before sunrise"
                            turnOnSwitchesAfterSunset?.on()
                        }
                    }

                    if (turnOnSwitches) {
                        log.info "$evt.displayName was unlocked successfully, turning on switches $turnOnSwitches"
                        turnOnSwitches?.on()
                    }

                    if (turnOffSwitches) {
                        log.info "$evt.displayName was unlocked successfully, turning off switches $turnOffSwitches"
                        turnOffSwitches?.off()
                    }
                }
                                
				// Check for one time codes and disable them if required
                state.usedOneTimeCodes[lock.id].add(i as String) // mark the user slot used
                expireCodeCheck() // Check the expired code and remove from lock

                if (notify) {
                    if (userName == null) {
                    	if (!disableAllNotify) {
                        	sendPush "$evt.displayName was unlocked by Unknown User"
                        } else {
                            sendNotificationEvent("$evt.displayName was unlocked by Unknown User")
                        }
                        if (sms) {
                        	sendSms(sms, "$evt.displayName was unlocked by Unknown User")
                        }
                    }
                    else {
                    	if (!disableAllNotify) {
                        	sendPush "$evt.displayName was unlocked by $userName"
                        } else {
                            sendNotificationEvent("$evt.displayName was unlocked by $userName")
                        }
                        if (sms) {
                        	sendSms(sms, "$evt.displayName was unlocked by $userName")
                        }
                    }
                }
            }
        }
        else if (evt.value == "locked") {
            log.debug "$evt.displayName was locked with description: $evt.descriptionText"
            
            def lockMode = evt.descriptionText?.contains("manually") ? "manually" : "electronically"
            
            // Check if we need to retract a deadbolt lock it was locked while the door was still open
            if (settings."retractDeadbolt${lock}") {
            	def sensor = settings."sensor${lock}"
                if (sensor.latestValue("contact") == "open") {
                	log.info "$lock was locked while the door was still open, unlocking it"
                    lock.unlock() // unlock it
                } else {
                	log.trace "$lock was locked while the door was closed, we're good"
                }
            }

            if (lockNotify && (lockNotifyModes ? lockNotifyModes.find{it == location.currentMode} : true)) {
                if (!disableAllNotify) {
                    sendPush "$evt.displayName was locked $lockMode"
                } else {
                    sendNotificationEvent("$evt.displayName was locked $lockMode")
                }
                if (sms) {
                    sendSms(sms, "$evt.displayName was locked $lockMode")
                }
            }
        }
        else if (evt.value == "unknown") {
            log.debug "Lock $evt.displayName Jammed!"
            if (notify) {
                if (!disableAllNotify) {
                    sendPush "$evt.displayName lock is Jammed!"
                } else {
                    sendNotificationEvent("$evt.displayName lock is Jammed!")
                }
                if (sms) {
                    sendSms(sms, "$evt.displayName lock is Jammed!")
                }
            }        	
        }
    }
    else if (evt.name == "invalidCode") {
        log.debug "Lock $evt.displayName, invalid user code: $evt.value"
        if (notify) {
            if (!disableAllNotify) {
                sendPush "Too many invalid user codes detected on lock $evt.displayName"
            } else {
                sendNotificationEvent("Too many invalid user codes detected on lock $evt.displayName")
            }
            if (sms) {
                sendSms(sms, "Too many invalid user codes detected on lock $evt.displayName")
            }
        }        	
    }
}
                
def updateCodes() {
	for (lock in locks) {
		if (state.updateLockList.contains(lock.id)) { // this lock codes hasn't been completely initiated
        	log.trace "Check for pending code updates for $lock"
	        if (state.updateNextCode <= settings.maxUserNames) {
            	log.debug "Updating code $state.updateNextCode on $lock"
                def i = state.updateNextCode // Next code we are checking
                def name = settings."userNames${i}" // Get the name for the slot
                def code = settings."userCodes${i}" // Get the code for the slot
                def notify = settings."userNotify${i}" // Notification setting
                def userType = settings."userType${i}" // User type
                def expDate = settings."userExpireDate${i}" // Get the expiration date
                def expTime = settings."userExpireTime${i}" // Get the expiration time
                def startDate = settings."userStartDate${i}" // Get the start date
                def startTime = settings."userStartTime${i}" // Get the start time
                def userDayOfWeekA = settings."userDayOfWeekA${i}" // Scheduling Days of week A
                def userStartTimeA = settings."userStartTimeA${i}" ? (new Date(timeToday(settings."userStartTimeA${i}", location.timeZone).time)).format("HH:mm z", location.timeZone) : "" // Scheduling start time A
                def userEndTimeA = settings."userEndTimeA${i}" ? (new Date(timeToday(settings."userEndTimeA${i}", location.timeZone).time)).format("HH:mm z", location.timeZone) : "" // Scheduling end time A
                def user = i as Integer // which user slot are we using 
				def doAdd = false // by default we dont' add codes
                
                // Check the code expiration
                switch (userType) {
                    case 'Expire on':
                    	if (code != null) {
                            if (expDate && expTime) {
                                def midnightToday = timeToday("2000-01-01T00:00:00.000-0000", location.timeZone)
                                String dst = location.timeZone.getDisplayName(location.timeZone.inDaylightTime(new Date(now())), TimeZone.SHORT) // Keep current timezone
                                def expT = (timeToday(expTime, location.timeZone).time - midnightToday.time)
                                def expD = Date.parse("yyyy-MM-dd Z", expDate + " " + dst).toCalendar()
                                def exp = expD.getTimeInMillis() + expT
                                def expStr = (new Date(exp)).format("EEE MMM dd yyyy HH:mm z", location.timeZone)
                                if (exp > now()) {
                                    if (startDate && startTime) {
                                        def startT = (timeToday(startTime, location.timeZone).time - midnightToday.time)
                                        def startD = Date.parse("yyyy-MM-dd Z", startDate + " " + dst).toCalendar()
                                        def start = startD.getTimeInMillis() + startT
                                        def startStr = (new Date(start.value)).format("EEE MMM dd yyyy HH:mm z", location.timeZone)
                                        if (start <= now()) {
                                            if (!state.startCodes[lock.id].contains(i as String)) {
                                                state.startCodes[lock.id].add(i as String)
                                                sendNotificationEvent("$lock user $user $name's code is set to start on $startStr")
                                                log.debug "$lock user $user $name's code is set to start on $startStr"
                                                doAdd = true // we need to add the code
                                            } else {
                                                log.error "$lock User $i $name is already tracked as start added"
                                            }
                                        } else {
                                            if (!state.startCodes[lock.id].contains(i as String)) {
                                                sendNotificationEvent("$lock user $user $name's code is set to start in future on $startStr")
                                                log.debug "$lock user $user $name's code is set to start in future on $startStr"
                                            } else {
                                                log.error "$lock user $user $name is already tracked as start added"
                                            }
                                        }
                                    } else {
                                        sendNotificationEvent("$lock user $user $name's code is set to expire on $expStr")
                                        log.debug "$lock user $user $name's code is set to expire on $expStr"
                                        doAdd = true // we need to add the code
                                    }
                                } else {
                                    if (!state.expiredCodes[lock.id].contains(user as String)) {
                                        state.expiredCodes[lock.id].add(user as String)
                                        sendNotificationEvent("$lock user $user $name's code expired on $expStr")
                                        log.debug "$lock user $user $name expired on $expStr"
                                    } else {
                                        log.error "$lock user $user $name is already tracked as expired"
                                    }
                                }
                            } else {
                                log.error "$lock User $user $name set to Expire but does not have a Expiration Date: $expDate or Time: $expTime"
                            }
                        }
                        break

                    case 'One time':
                    	if (code != null) {
                            log.info "$lock user $user $name is a one time code"
                            sendNotificationEvent("$lock user $user $name is a one time code")
                            doAdd = true // the code
                        }
                    	break
                        
                    case 'Scheduled':
                    	if (code != null) {
                            if (checkSchedule(user, "A")) { // Within operating schedule
                                if (state.activeScheduledCodes[lock.id].contains(user as String)) {
                                    log.error "$lock scheduled user $user $name is already tracked. It is scheduled to work between $userDayOfWeekA: $userStartTimeA to $userEndTimeA"
                                } else {
                                    log.info "$lock user $user $name is scheduled to work between $userDayOfWeekA: $userStartTimeA to $userEndTimeA"
                                    state.activeScheduledCodes[lock.id].add(user as String) // track scheduled users who are added
                                }
                                doAdd = true // Add the user
                                sendNotificationEvent("$lock user $user $name is scheduled to work between $userDayOfWeekA: $userStartTimeA to $userEndTimeA")
                            } else {
                                state.activeScheduledCodes[lock.id].remove(user as String) // track scheduled users who are added
                                log.info "$lock user $user $name is outside operating schedule between $userDayOfWeekA: $userStartTimeA-$userEndTimeA"
                                sendNotificationEvent("$lock user $user $name is outside operating schedule between $userDayOfWeekA: $userStartTimeA to $userEndTimeA")
                            }
                        }
                        break
                        
                    case 'Permanent':
                    	if (code != null) {
                    		doAdd = true // add the code
                        }
                        break
                        
                    case 'Inactive':
                    	doAdd = false // delete the code
                        break
                        
                    default:
                    	log.error "Invalid user type $userType, user $user $name"
                    	sendNotificationEvent("Invalid user type $userType, user $user $name")
                        break
                }

                if ((code != null) && doAdd) { // We have a code and it is not expired/within schedule, update or set the code in the slot
                    lock.setCode(user, code)
                    log.info "$lock added user: $user, code: $code, name: $name"
                    sendNotificationEvent("$lock added $name to user $user")
                } else { // Delete the slot
                    lock.deleteCode(user)
                    log.info "$lock deleted user: $user"
                    sendNotificationEvent("$lock deleted user: $user")
                }
                
				state.updateNextCode = state.updateNextCode + 1 // move onto the next code
				log.trace "Scheduled next code update in ${sendDelay > 0 ? sendDelay : 15} seconds"
                startTimer((sendDelay > 0 ? sendDelay : 15), updateCodes) // schedule the next code update after a few seconds otherwise it overloads locks and doesn't work
                return // We are done here, exit out as we've scheduled the next update
            }
            
            state.updateLockList.remove(lock.id) // we are done with this lock
            state.updateNextCode = 1 // reset back to 1 for the next lock
            log.trace "$lock id $lock.id code updates complete, unprocessed locks ${state.updateLockList}, reset next code update to $state.updateNextCode"
        }
    }
    
    expireCodeCheck() // start the expired code check routine
}

def expireCodeCheck() {
    log.trace "ExpireCodeCheck called"

    for (lock in locks) {
        if (state.expiredLockList.contains(lock.id)) { // this lock codes hasn't been completely initiated
            log.trace "Check for pending expired code updates for $lock"
            while (state.expiredNextCode <= settings.maxUserNames) { // cycle through all the codes
                log.trace "Checking expired updates for code $state.expiredNextCode on $lock"
                def i = state.expiredNextCode
                def name = settings."userNames${i}" // Get the name for the slot
                def code = settings."userCodes${i}" // Get the code for the slot
                def notify = settings."userNotify${i}" // Notification setting
                def userType = settings."userType${i}" // User type
                def expDate = settings."userExpireDate${i}" // Get the expiration date
                def expTime = settings."userExpireTime${i}" // Get the expiration time
                def startDate = settings."userStartDate${i}" // Get the start date
                def startTime = settings."userStartTime${i}" // Get the start time
                def userDayOfWeekA = settings."userDayOfWeekA${i}" // Scheduling Days of week A
                def userStartTimeA = settings."userStartTimeA${i}" ? (new Date(timeToday(settings."userStartTimeA${i}", location.timeZone).time)).format("HH:mm z", location.timeZone) : "" // Scheduling start time A
                def userEndTimeA = settings."userEndTimeA${i}" ? (new Date(timeToday(settings."userEndTimeA${i}", location.timeZone).time)).format("HH:mm z", location.timeZone) : "" // Scheduling end time A

                switch (userType) {
                    case 'Expire on':
                    	if (code != null) {
                            if (startDate && startTime) {
                                def midnightToday = timeToday("2000-01-01T00:00:00.000-0000", location.timeZone)
                                String dst = location.timeZone.getDisplayName(location.timeZone.inDaylightTime(new Date(now())), TimeZone.SHORT) // Keep current timezone
                                def startT = (timeToday(startTime, location.timeZone).time - midnightToday.time)
                                def startD = Date.parse("yyyy-MM-dd Z", startDate + " " + dst).toCalendar()
                                def start = startD.getTimeInMillis() + startT
                                def startStr = (new Date(start.value)).format("EEE MMM dd yyyy HH:mm z", location.timeZone)
                                def expT = (timeToday(expTime, location.timeZone).time - midnightToday.time)
                                def expD = Date.parse("yyyy-MM-dd Z", expDate + " " + dst).toCalendar()
                                def exp = expD.getTimeInMillis() + expT
                                def expStr = (new Date(exp.value)).format("EEE MMM dd yyyy HH:mm z", location.timeZone)
                                if ((start <= now()) && (exp > now())) {
                                    if (!state.startCodes[lock.id].contains(i as String)) {
                                        state.startCodes[lock.id].add(i as String)
                                        def user = i as Integer // Convert to integer to be sure
                                        lock.setCode(user, code)
                                        log.info "$lock added $name to user $user, code: $code, because it is scheduled to start at $startStr"
                                        sendNotificationEvent("$lock added $name to user $user, because it is scheduled to start at $startStr")

                                        state.expiredNextCode = state.expiredNextCode + 1 // move onto the next code
                                        log.trace "Scheduled next expired code check in ${sendDelay > 0 ? sendDelay : 15} seconds"
                                        startTimer((sendDelay > 0 ? sendDelay : 15), expireCodeCheck) // schedule the next code update after a few seconds otherwise it overloads locks and doesn't work
                                        return // We are done here, exit out as we've scheduled the next update
                                    } else {
                                        log.debug "$lock User $i $name is already tracked as start added"
                                    }
                                }
                            } else if ((startDate && !startTime) || (!startDate && startTime)) {
                                log.error "User $i $name set to Start but does not have a valid Start Date/Time: $startDate or Time: $startTime"
                            }

                            if (expDate && expTime) {
                                def midnightToday = timeToday("2000-01-01T00:00:00.000-0000", location.timeZone)
                                String dst = location.timeZone.getDisplayName(location.timeZone.inDaylightTime(new Date(now())), TimeZone.SHORT) // Keep current timezone
                                def expT = (timeToday(expTime, location.timeZone).time - midnightToday.time)
                                def expD = Date.parse("yyyy-MM-dd Z", expDate + " " + dst).toCalendar()
                                def exp = expD.getTimeInMillis() + expT
                                def expStr = (new Date(exp.value)).format("EEE MMM dd yyyy HH:mm z", location.timeZone)
                                if (exp < now()) {
                                    if (!state.expiredCodes[lock.id].contains(i as String)) {
                                        state.expiredCodes[lock.id].add(i as String)
                                        def user = i as Integer // Convert to integer to be sure
                                        lock.deleteCode(user)
                                        log.info "$lock deleted expired user $user $name"
                                        sendNotificationEvent("$lock deleted expired user $user $name")

                                        state.expiredNextCode = state.expiredNextCode + 1 // move onto the next code
                                        log.trace "Scheduled next expired code check in ${sendDelay > 0 ? sendDelay : 15} seconds"
                                        startTimer((sendDelay > 0 ? sendDelay : 15), expireCodeCheck) // schedule the next code update after a few seconds otherwise it overloads locks and doesn't work
                                        return // We are done here, exit out as we've scheduled the next update
                                    } else {
                                        log.debug "$lock User $i $name is already tracked as expired"
                                    }
                                }
                            } else {
                                log.error "User $i $name set to Expire but does not have a valid Expiration Date/Time: $expDate or Time: $expTime"
                            }
                        }
                        break

                    case 'One time':
                    	if (code != null) {
                            if (state.usedOneTimeCodes[lock.id].contains(i as String)) {
                                def user = i as Integer // Convert to integer to be sure
                                state.usedOneTimeCodes[lock.id].remove(i as String)
                                state.trackUsedOneTimeCodes.add(i as String) // track it for reporting purposes
                                lock.deleteCode(user)
                                log.info "$lock deleted one time user $user $name"
                                sendNotificationEvent("$lock deleted one time user $user $name")

                                state.expiredNextCode = state.expiredNextCode + 1 // move onto the next code
                                log.trace "Scheduled next expired code check in ${sendDelay > 0 ? sendDelay : 15} seconds"
                                startTimer((sendDelay > 0 ? sendDelay : 15), expireCodeCheck) // schedule the next code update after a few seconds otherwise it overloads locks and doesn't work
                                return // We are done here, exit out as we've scheduled the next update
                            } else {
                                log.debug "$lock User $i $name is a one time code but it has not been used yet"
                            }
                        }
                        break

                    case 'Scheduled':
                    	if (code != null) {
                            if (checkSchedule(i, "A")) { // Within operating schedule
                                if (state.activeScheduledCodes[lock.id].contains(i as String)) {
                                    log.debug "$lock Scheduled user $i $name is already active, not adding again"
                                } else {
                                    def user = i as Integer // Convert to integer to be sure
                                    state.activeScheduledCodes[lock.id].add(i as String) // track scheduled users who are added
                                    lock.setCode(user, code)
                                    log.info "$lock added $name to user $user, code: $code, because it is scheduled to work between $userDayOfWeekA: $userStartTimeA to $userEndTimeA"
                                    sendNotificationEvent("$lock added $name to user $user because it is scheduled to work between $userDayOfWeekA: $userStartTimeA to $userEndTimeA")

                                    state.expiredNextCode = state.expiredNextCode + 1 // move onto the next code
                                    log.trace "Scheduled next expired code check in ${sendDelay > 0 ? sendDelay : 15} seconds"
                                    startTimer((sendDelay > 0 ? sendDelay : 15), expireCodeCheck) // schedule the next code update after a few seconds otherwise it overloads locks and doesn't work
                                    return // We are done here, exit out as we've scheduled the next update
                                }
                            } else { // Outside operating schedule
                                if (!state.activeScheduledCodes[lock.id].contains(i as String)) {
                                    log.debug "$lock Scheduled user $i $name is already inactive, not removing again"
                                } else {
                                    def user = i as Integer // Convert to integer to be sure
                                    state.activeScheduledCodes[lock.id].remove(i as String) // track scheduled users who are added
                                    lock.deleteCode(user)
                                    log.info "$lock deleted expired user $user $name because it is scheduled to work between $userDayOfWeekA: $userStartTimeA to $userEndTimeA"
                                    sendNotificationEvent("$lock deleted expired user $user $name because it is scheduled to work between $userDayOfWeekA: $userStartTimeA to $userEndTimeA")

                                    state.expiredNextCode = state.expiredNextCode + 1 // move onto the next code
                                    log.trace "Scheduled next expired code check in ${sendDelay > 0 ? sendDelay : 15} seconds"
                                    startTimer((sendDelay > 0 ? sendDelay : 15), expireCodeCheck) // schedule the next code update after a few seconds otherwise it overloads locks and doesn't work
                                    return // We are done here, exit out as we've scheduled the next update
                                }
                            }
                        }
                        break

                    case 'Permanent':
                    case 'Inactive':
                    	default:
                        break
                }

                state.expiredNextCode = state.expiredNextCode + 1 // move onto the next code
            }

            state.expiredLockList.remove(lock.id) // we are done with this lock
            state.expiredNextCode = 1 // reset back to 1 for the next lock
            log.trace "$lock id $lock.id code expire check complete, unprocessed locks ${state.expiredLockList}, reset next code update to $state.expiredNextCode"
        }
    }

    // All done now reset the lock list and add the locks back for next expired check cycle
    state.expiredNextCode = 1 // reset back to 1 for the next lock
    for (lock in locks) {
        state.expiredLockList.add(lock.id) // reset the state for each lock to be processed with expired
        log.trace "Added $lock id ${lock.id} back to unprocessed locks expire list ${state.expiredLockList}"
    }

    startTimer(5*60, expireCodeCheck) // schedule the next code check in 5 minutes
}

def startTimer(seconds, function) {
	def runTime = new Date(now() + (seconds * 1000)) // for runOnce
    log.trace "Scheduled to run $function at $runTime"
	runOnce(runTime, function, [overwrite: true]) // runIn isn't reliable, runOnce is more reliable, but only runIn supports overwrite:false to preserve existing function calls pending from runEveryXMinutes
}

private checkSchedule(def i, def x) {
	log.debug("Checking operating schedule $x for user $i")
    
    def doChange = false
    Calendar localCalendar = Calendar.getInstance(location.timeZone);
    int currentDayOfWeek = localCalendar.get(Calendar.DAY_OF_WEEK);
    def currentTime = now()

    // some debugging in order to make sure things are working correclty
    log.debug "Current time: ${(new Date(currentTime)).format("EEE MMM dd yyyy HH:mm z", location.timeZone)}"

	// Check if we are within operating times
    if (settings."userStartTime${x}${i}" != null && settings."userEndTime${x}${i}" != null) {
        def scheduledStart = timeToday(settings."userStartTime${x}${i}", location.timeZone).time
        def scheduledEnd = timeToday(settings."userEndTime${x}${i}", location.timeZone).time

    	log.debug("Operating ${settings."userStartTime${x}${i}"} ${(new Date(scheduledStart)).format("HH:mm z", location.timeZone)}, ${settings."userEndTime${x}${i}"} ${(new Date(scheduledEnd)).format("HH:mm z", location.timeZone)}")

		if (currentTime < scheduledStart || currentTime > scheduledEnd) {
            log.info("Outside operating time schedule")
            return false
        }
    }

	// Check the condition under which we want this to run now
    // This set allows the most flexibility.
    log.debug("Operating DOW(s): ${settings."userDayOfWeek${x}${i}"}")

    if(settings."userDayOfWeek${x}${i}" == null) {
    	log.error "Invalid Day of week ${settings."userDayOfWeek${x}${i}"}"
    } else if(settings."userDayOfWeek${x}${i}".contains('All Week')) {
            doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Monday') || settings."userDayOfWeek${x}${i}".contains('Monday to Friday')) && currentDayOfWeek == Calendar.instance.MONDAY) {
            doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Tuesday') || settings."userDayOfWeek${x}${i}".contains('Monday to Friday')) && currentDayOfWeek == Calendar.instance.TUESDAY) {
            doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Wednesday') || settings."userDayOfWeek${x}${i}".contains('Monday to Friday')) && currentDayOfWeek == Calendar.instance.WEDNESDAY) {
            doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Thursday') || settings."userDayOfWeek${x}${i}".contains('Monday to Friday')) && currentDayOfWeek == Calendar.instance.THURSDAY) {
            doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Friday') || settings."userDayOfWeek${x}${i}".contains('Monday to Friday')) && currentDayOfWeek == Calendar.instance.FRIDAY) {
            doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Saturday') || settings."userDayOfWeek${x}${i}".contains('Saturday & Sunday')) && currentDayOfWeek == Calendar.instance.SATURDAY) {
            doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Sunday') || settings."userDayOfWeek${x}${i}".contains('Saturday & Sunday')) && currentDayOfWeek == Calendar.instance.SUNDAY) {
            doChange = true
    }


    // If we have hit the condition to schedule this then lets do it
    if(doChange == true){
    	log.info("Within operating schedule")
        return true
    }
    else {
        log.info("Outside operating schedule")
        return false
    }
}

// Handle mode changes, reinitialize the current temperature and timers after a mode change, this is to workaround the issue of the last timer firing while in a non running mode, resume operations when supported modes are set
def modeChangeHandler(evt) {
	log.debug "Lock Mgmt reinitializing on mode change notification, new mode $evt.value"
	//sendNotificationEvent("$Lock Mgmt reinitializing on mode change notification, new mode $evt.value")
	expireCodeCheck() // kick start the expireCodeCheck sequence
}