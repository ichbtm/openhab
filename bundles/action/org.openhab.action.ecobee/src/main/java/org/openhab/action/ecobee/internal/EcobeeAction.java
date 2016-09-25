/**
 * Copyright (c) 2010-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.ecobee.internal;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openhab.binding.ecobee.EcobeeActionProvider;
import org.openhab.binding.ecobee.messages.AbstractFunction;
import org.openhab.binding.ecobee.messages.AcknowledgeFunction;
import org.openhab.binding.ecobee.messages.ControlPlugFunction;
import org.openhab.binding.ecobee.messages.CreateVacationFunction;
import org.openhab.binding.ecobee.messages.DeleteVacationFunction;
import org.openhab.binding.ecobee.messages.FanMode;
import org.openhab.binding.ecobee.messages.HoldType;
import org.openhab.binding.ecobee.messages.ResetPreferencesFunction;
import org.openhab.binding.ecobee.messages.ResumeProgramFunction;
import org.openhab.binding.ecobee.messages.SendMessageFunction;
import org.openhab.binding.ecobee.messages.SetHoldFunction;
import org.openhab.binding.ecobee.messages.SetOccupiedFunction;
import org.openhab.binding.ecobee.messages.Temperature;
import org.openhab.binding.ecobee.messages.Thermostat.Event;
import org.openhab.binding.ecobee.messages.Thermostat.VentilatorMode;
import org.openhab.binding.ecobee.messages.UpdateSensorFunction;
import org.openhab.core.scriptengine.action.ActionDoc;
import org.openhab.core.scriptengine.action.ParamDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides static methods for invocation of Ecobee functions, for use in automation rules.
 *
 * @see <a href="https://www.ecobee.com/home/developer/api/documentation/v1/functions/using-functions.shtml">Using
 *      Functions</a>
 * @author John Cocula
 * @since 1.8.0
 */
public class EcobeeAction {

    private static final Logger logger = LoggerFactory.getLogger(EcobeeAction.class);

    /**
     * The acknowledge function allows an alert to be acknowledged.
     *
     * @see <a
     *      href="https://www.ecobee.com/home/developer/api/documentation/v1/functions/Acknowledge.shtml">Acknowledge
     *      </a>
     */
    @ActionDoc(text = "The acknowledge function allows an alert to be acknowledged.")
    public static boolean ecobeeAcknowledge(
            @ParamDoc(name = "selection", text = "The thermostat selection to acknowledge the alert for.") String selection,
            @ParamDoc(name = "thermostatIdentifier", text = "The thermostat identifier to acknowledge the alert for.") String thermostatIdentifier,
            @ParamDoc(name = "ackRef", text = "The acknowledge ref of alert.") String ackRef,
            @ParamDoc(name = "ackType", text = "The type of acknowledgement. Valid values: accept, decline, defer, unacknowledged.") String ackType,
            @ParamDoc(name = "remindMeLater", text = "(opt) Whether to remind at a later date, if this is a defer acknowledgement.") Boolean remindMeLater) {
        AcknowledgeFunction function = new AcknowledgeFunction(thermostatIdentifier, ackRef,
                AcknowledgeFunction.AckType.forValue(ackType), remindMeLater);
        return callEcobeeInternal(selection, function);
    }

    /**
     * Control the on/off state of a plug by setting a hold on the plug. Creates a hold for the on or off state of the
     * plug for the specified duration. Note that an event is created regardless of whether the program is in the same
     * state as the requested state.
     *
     * @see <a href="https://www.ecobee.com/home/developer/api/documentation/v1/functions/ControlPlug.shtml">Control
     *      Plug</a>
     */
    @ActionDoc(text = "Control the on/off state of a plug by setting a hold on the plug.")
    public static boolean ecobeeControlPlug(
            @ParamDoc(name = "selection", text = "The thermostat selection controlling the plug.") String selection,
            @ParamDoc(name = "plugName", text = "The name of the plug. Ensure each plug has a unique name.") String plugName,
            @ParamDoc(name = "plugState", text = "The state to put the plug into. Valid values: on, off, resume.") String plugState,
            @ParamDoc(name = "startDateTime", text = "(opt) The start date/time in thermostat time.") Date startDateTime,
            @ParamDoc(name = "endDateTime", text = "(opt) The end date/time in thermostat time.") Date endDateTime,
            @ParamDoc(name = "holdType", text = "(opt) The hold duration type. Valid values: dateTime, nextTransition, indefinite, holdHours.") String holdType,
            @ParamDoc(name = "holdHours", text = "(opt) The number of hours to hold for, used and required if holdType='holdHours'.") Integer holdHours) {
        ControlPlugFunction function = new ControlPlugFunction(plugName,
                ControlPlugFunction.PlugState.forValue(plugState), startDateTime, endDateTime,
                (holdType == null) ? null : HoldType.forValue(holdType), holdHours);
        return callEcobeeInternal(selection, function);
    }

    /**
     * The create vacation function creates a vacation event on the thermostat. If the start/end date/times are not
     * provided for the vacation event, the vacation event will begin immediately and last 14 days.
     *
     * If both the coolHoldTemp and heatHoldTemp parameters provided to this function have the same value, and the
     * Thermostat is in auto mode, then the two values will be adjusted during processing to be separated by the value
     * stored in thermostat.settings.heatCoolMinDelta.
     *
     * @see <a href="https://www.ecobee.com/home/developer/api/documentation/v1/functions/CreateVacation.shtml">Create
     *      Vacation</a>
     */
    @ActionDoc(text = "The create vacation function creates a vacation event on the thermostat.")
    public static boolean ecobeeCreateVacation(
            @ParamDoc(name = "selection", text = "The thermostat selection for creating the vacation.") String selection,
            @ParamDoc(name = "name", text = "The vacation event name. It must be unique.") String name,
            @ParamDoc(name = "coolHoldTemp", text = "The temperature at which to set the cool vacation hold.") Number coolHoldTemp,
            @ParamDoc(name = "heatHoldTemp", text = "The temperature at which to set the heat vacation hold.") Number heatHoldTemp,
            @ParamDoc(name = "startDateTime", text = "(opt) The start date/time in thermostat time.") Date startDateTime,
            @ParamDoc(name = "endDateTime", text = "(opt) The end date in thermostat time.") Date endDateTime,
            @ParamDoc(name = "fan", text = "(opt) The fan mode during the vacation. Values: auto, on Default: auto") String fan,
            @ParamDoc(name = "fanMinOnTime", text = "(opt) The minimum number of minutes to run the fan each hour. Range: 0-60, Default: 0") Number fanMinOnTime) {
        CreateVacationFunction function = new CreateVacationFunction(name,
                Temperature.fromLocalTemperature(new BigDecimal(coolHoldTemp.toString())),
                Temperature.fromLocalTemperature(new BigDecimal(heatHoldTemp.toString())), startDateTime, endDateTime,
                FanMode.forValue(fan), (fanMinOnTime == null) ? null : fanMinOnTime.intValue());
        return callEcobeeInternal(selection, function);
    }

    /**
     * The delete vacation function deletes a vacation event from a thermostat. This is the only way to cancel a
     * vacation event. This method is able to remove vacation events not yet started and scheduled in the future.
     *
     * @see <a href="https://www.ecobee.com/home/developer/api/documentation/v1/functions/DeleteVacation.shtml">Delete
     *      Vacation</a>
     */
    @ActionDoc(text = "The delete vacation function deletes a vacation event from a thermostat.")
    public static boolean ecobeeDeleteVacation(
            @ParamDoc(name = "selection", text = "The thermostat selection to delete vacation.") String selection,
            @ParamDoc(name = "name", text = "The vacation event name to delete.") String name) {
        return callEcobeeInternal(selection, new DeleteVacationFunction(name));
    }

    /**
     * The reset preferences function sets all of the user configurable settings back to the factory default values.
     * This function call will not only reset the top level thermostat settings such as hvacMode, lastServiceDate and
     * vent, but also all of the user configurable fields of the thermostat.settings and thermostat.program objects.
     *
     * Note that this does not reset all values. For example, the installer settings and wifi details remain untouched.
     *
     * @see <a href="https://www.ecobee.com/home/developer/api/documentation/v1/functions/ResetPreferences.shtml">Reset
     *      Preferences</a>
     */
    @ActionDoc(text = "The reset preferences function sets all of the user configurable settings back to the factory default values.")
    public static boolean ecobeeResetPreferences(
            @ParamDoc(name = "selection", text = "The thermostat selection to reset preferences.") String selection) {
        return callEcobeeInternal(selection, new ResetPreferencesFunction());
    }

    /**
     * The resume program function removes the currently running event providing the event is not a mandatory demand
     * response event. If resumeAll parameter is not set, top active event is removed from the stack and the thermostat
     * resumes its program, or enters the next event in the stack if one exists.
     *
     * If resumeAll parameter set to true, the function resumes all events and returns the thermostat to its program.
     *
     * @see <a href="https://www.ecobee.com/home/developer/api/documentation/v1/functions/ResumeProgram.shtml">Resume
     *      Program</a>
     */
    @ActionDoc(text = "The resume program function removes the currently running event providing the event is not a mandatory demand response event.")
    public static boolean ecobeeResumeProgram(
            @ParamDoc(name = "selection", text = "The thermostat selection to resume program.") String selection,
            @ParamDoc(name = "resumeAll", text = "(opt) Should the thermostat be resumed to next event (false) or to its program (true).") Boolean resumeAll) {
        return callEcobeeInternal(selection, new ResumeProgramFunction(resumeAll));
    }

    /**
     * The send message function allows an alert message to be sent to the thermostat. The message properties are same
     * as those of the Alert Object.
     *
     * @see <a href="https://www.ecobee.com/home/developer/api/documentation/v1/functions/SendMessage.shtml">Send
     *      Message</a>
     */
    @ActionDoc(text = "The send message function allows an alert message to be sent to the thermostat.")
    public static boolean ecobeeSendMessage(
            @ParamDoc(name = "selection", text = "The thermostat selection to send message.") String selection,
            @ParamDoc(name = "text", text = "The message text to send. Text will be truncated to 500 characters if longer.") String text) {
        return callEcobeeInternal(selection, new SendMessageFunction(text));
    }

    /**
     * The set hold function sets the thermostat into a hold with the specified temperature. Creates a hold for the
     * specified duration. Note that an event is created regardless of whether the program is in the same state as the
     * requested state.
     *
     * There is also support for creating a hold by passing a holdClimateRef request parameter/value pair to this
     * function (See Event). When an existing and valid Climate.climateRef value is passed to this function, the
     * coolHoldTemp, heatHoldTemp and fan mode from that Climate are used in the creation of the hold event. The values
     * from that Climate will take precedence over any coolHoldTemp, heatHoldTemp and fan mode parameters passed into
     * this function separately.
     *
     * To resume from a hold and return to the program, use the ResumeProgram function.
     *
     * @see <a href="https://www.ecobee.com/home/developer/api/documentation/v1/functions/SetHold.shtml">Set Hold</a>
     */
    @ActionDoc(text = "The set hold function sets the thermostat into a hold with the specified temperature or climate ref.")
    public static boolean ecobeeSetHold(
            @ParamDoc(name = "selection", text = "The thermostat selection to set hold.") String selection,
            @ParamDoc(name = "coolHoldTemp", text = "(opt) The temperature at which to set the cool hold.") Number coolHoldTemp,
            @ParamDoc(name = "heatHoldTemp", text = "(opt) The temperature at which to set the heat hold.") Number heatHoldTemp,
            @ParamDoc(name = "holdClimateRef", text = "(opt) The Climate to use as reference for setting the coolHoldTemp, heatHoldTemp and fan settings for this hold. If this value is passed the coolHoldTemp and heatHoldTemp are not required.") String holdClimateRef,
            @ParamDoc(name = "startDateTime", text = "(opt) The start date in thermostat time.") Date startDateTime,
            @ParamDoc(name = "endDateTime", text = "(opt) The end date in thermostat time.") Date endDateTime,
            @ParamDoc(name = "holdType", text = "(opt) The hold duration type. Valid values: dateTime, nextTransition, indefinite, holdHours.") String holdType,
            @ParamDoc(name = "holdHours", text = "(opt) The number of hours to hold for, used and required if holdType='holdHours'.") Number holdHours) {
        Map<String, Object> params = new HashMap<String, Object>();
        if (coolHoldTemp != null) {
            params.put("coolHoldTemp", coolHoldTemp);
        }
        if (heatHoldTemp != null) {
            params.put("heatHoldTemp", heatHoldTemp);
        }
        if (holdClimateRef != null) {
            params.put("holdClimateRef", holdClimateRef);
        }
        return ecobeeSetHold(selection, params, holdType, holdHours, startDateTime, endDateTime);
    }

    /**
     * The set hold function sets the thermostat into a hold with the specified temperature. Creates a hold for the
     * specified duration. Note that an event is created regardless of whether the program is in the same state as the
     * requested state.
     *
     * There is also support for creating a hold by passing a holdClimateRef request parameter/value pair to this
     * function (See Event). When an existing and valid Climate.climateRef value is passed to this function, the
     * coolHoldTemp, heatHoldTemp and fan mode from that Climate are used in the creation of the hold event. The values
     * from that Climate will take precedence over any coolHoldTemp, heatHoldTemp and fan mode parameters passed into
     * this function separately.
     *
     * To resume from a hold and return to the program, use the ResumeProgram function.
     *
     * @see <a href="https://www.ecobee.com/home/developer/api/documentation/v1/functions/SetHold.shtml">Set Hold</a>
     */
    @ActionDoc(text = "The set hold function sets the thermostat into a hold with the specified event parameters.")
    public static boolean ecobeeSetHold(
            @ParamDoc(name = "selection", text = "The thermostat selection to set hold.") String selection,
            @ParamDoc(name = "params", text = "The map of hold parameters.") Map<String, Object> params,
            @ParamDoc(name = "holdType", text = "(opt) The hold duration type. Valid values: dateTime, nextTransition, indefinite, holdHours.") String holdType,
            @ParamDoc(name = "holdHours", text = "(opt) The number of hours to hold for, used and required if holdType='holdHours'.") Number holdHours,
            @ParamDoc(name = "startDateTime", text = "(opt) The start date in thermostat time.") Date startDateTime,
            @ParamDoc(name = "endDateTime", text = "(opt) The end date in thermostat time.") Date endDateTime) {
        Event event = new Event();
        for (String key : params.keySet()) {
            Object value = params.get(key);
            switch (key) {
                case "isOccupied":
                    event.setOccupied((Boolean) value);
                    break;
                case "isCoolOff":
                    event.setCoolOff((Boolean) value);
                    break;
                case "isHeatOff":
                    event.setHeatOff((Boolean) value);
                    break;
                case "coolHoldTemp":
                    event.setCoolHoldTemp(Temperature.fromLocalTemperature(new BigDecimal(value.toString())));
                    break;
                case "heatHoldTemp":
                    event.setHeatHoldTemp(Temperature.fromLocalTemperature(new BigDecimal(value.toString())));
                    break;
                case "fan":
                    event.setFan(FanMode.forValue((String) value));
                    break;
                case "vent":
                    event.setVent(VentilatorMode.forValue((String) value));
                    break;
                case "ventilatorMinOnTime":
                    event.setVentilatorMinOnTime((Integer) value);
                    break;
                case "isOptional":
                    event.setOptional((Boolean) value);
                    break;
                case "isTemperatureRelative":
                    event.setTemperatureRelative((Boolean) value);
                    break;
                case "coolRelativeTemp":
                    event.setCoolRelativeTemp(Temperature.fromLocalTemperature(new BigDecimal(value.toString())));
                    break;
                case "heatRelativeTemp":
                    event.setHeatRelativeTemp(Temperature.fromLocalTemperature(new BigDecimal(value.toString())));
                    break;
                case "isTemperatureAbsolute":
                    event.setTemperatureAbsolute((Boolean) value);
                    break;
                case "fanMinOnTime":
                    event.setFanMinOnTime((Integer) value);
                    break;
                case "holdClimateRef":
                    event.setHoldClimateRef((String) value);
                    break;
                default:
                    logger.warn("Unrecognized event field '{}' with value '{}' ignored.", key, value);
                    break;
            }
        }
        SetHoldFunction function = new SetHoldFunction(event, (holdType == null) ? null : HoldType.forValue(holdType),
                (holdHours == null) ? null : holdHours.intValue(), startDateTime, endDateTime);
        return callEcobeeInternal(selection, function);
    }

    /**
     * The set occupied function may only be used by EMS thermostats. The function switches a thermostat from occupied
     * mode to unoccupied, or vice versa. If used on a Smart thermostat, the function will throw an error. Switch
     * occupancy events are treated as Holds. There may only be one Switch Occupancy at one time, and the new event will
     * replace any previous event.
     *
     * Note that an occupancy event is created regardless what the program on the thermostat is set to. For example, if
     * the program is currently unoccupied and you set occupied=false, an occupancy event will be created using the
     * heat/cool settings of the unoccupied program climate. If your intent is to go back to the program and remove the
     * occupancy event, use resumeProgram instead.
     *
     * @see <a href="https://www.ecobee.com/home/developer/api/documentation/v1/functions/SetOccupied.shtml">Set
     *      Occupied</a>
     */
    @ActionDoc(text = "The function switches a thermostat from occupied mode to unoccupied, or vice versa (EMS MODELS ONLY).")
    public static boolean ecobeeSetOccupied(
            @ParamDoc(name = "selection", text = "The selection of EMS model thermostat to set occupied.") String selection,
            @ParamDoc(name = "occupied", text = "The climate to use for the temperature, occupied (true) or unoccupied (false).") Boolean occupied,
            @ParamDoc(name = "startDateTime", text = "(opt) The start date in thermostat time.") Date startDateTime,
            @ParamDoc(name = "endDateTime", text = "(opt) The end date in thermostat time.") Date endDateTime,
            @ParamDoc(name = "holdType", text = "(opt) The hold duration type. Valid values: dateTime, nextTransition, indefinite, holdHours.") String holdType,
            @ParamDoc(name = "holdHours", text = "(opt) The number of hours to hold for, used and required if holdType='holdHours'.") Number holdHours) {
        SetOccupiedFunction function = new SetOccupiedFunction(occupied, startDateTime, endDateTime,
                (holdType == null) ? null : HoldType.forValue(holdType),
                (holdHours == null) ? null : holdHours.intValue());
        return callEcobeeInternal(selection, function);
    }

    /**
     * The update sensor function allows the caller to update the name of an ecobee3 remote sensor.
     *
     * Each ecobee3 remote sensor "enclosure" contains two distinct sensors types temperature and occupancy. Only one of
     * the sensors is required in the request. Both of the sensors' names will be updated to ensure consistency as they
     * are part of the same remote sensor enclosure. This also reflects accurately what happens on the Thermostat
     * itself.
     *
     * @see <a href="https://www.ecobee.com/home/developer/api/documentation/v1/functions/UpdateSensor.shtml">Update
     *      Sensor</a>
     */
    @ActionDoc(text = "The update sensor function allows the caller to update the name of an ecobee3 remote sensor.")
    public static boolean ecobeeUpdateSensor(
            @ParamDoc(name = "selection", text = "The selection of EMS model thermostat to update the sensor name.") String selection,
            @ParamDoc(name = "name", text = "The updated name to give the sensor. Has a max length of 32, but shorter is recommended.") String name,
            @ParamDoc(name = "deviceId", text = "The deviceId for the sensor, typically this indicates the enclosure and corresponds to the ThermostatRemoteSensor.id field. For example: rs:100") String deviceId,
            @ParamDoc(name = "sensorId", text = "The identifier for the sensor within the enclosure. Corresponds to the RemoteSensorCapability.id. For example: 1") String sensorId) {
        return callEcobeeInternal(selection, new UpdateSensorFunction(name, deviceId, sensorId));
    }

    /**
     * Return the EcobeeActionProvider so we can perform Ecobee actions.
     *
     * @param selection
     *            unused
     * @return the Ecobee action provider
     * @throws Exception
     *             if no Ecobee action service or provider has been set
     */
    private static EcobeeActionProvider getActionProvider(String selection) throws Exception {
        EcobeeActionService service = EcobeeActionService.getEcobeeActionService();
        if (service == null) {
            throw new Exception(String.format("Ecobee Service is not configured, Action for selection %1$s not queued.",
                    selection));
        }

        EcobeeActionProvider actionProvider = service.getEcobeeActionProvider();
        if (actionProvider == null) {
            throw new Exception(String.format(
                    "Ecobee Action Provider is not configured, Action for selection %1$s not queued.", selection));
        }

        return actionProvider;
    }

    /**
     * This internal method sends function objects to the binding to invoke against thermostat(s).
     *
     * @param selection
     *            the selection of thermostat(s) against which to perform the function.
     * @param function
     *            the function to perform
     * @return true if the request to perform the function was successful.
     */
    private static boolean callEcobeeInternal(String selection, AbstractFunction function) {
        try {
            logger.debug("Attempting to call Ecobee function '{}' against selection '{}'", function, selection);

            EcobeeActionProvider actionProvider = getActionProvider(selection);

            return actionProvider.callEcobee(selection, function);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }
}
