// GET_STATUSImplementation.java
// $HeadURL$
package ngat.lotus;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;

import ngat.fits.*;
import ngat.lotus.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.ISS_TO_INST;
import ngat.message.ISS_INST.GET_STATUS;
import ngat.message.ISS_INST.GET_STATUS_DONE;
import ngat.net.*;
import ngat.util.*;
import ngat.util.logging.*;

/**
 * This class provides the implementation for the GET_STATUS command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 44 $
 * @see INTERRUPTImplementation
 */
public class GET_STATUSImplementation extends INTERRUPTImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");

	/**
	 * This hashtable is created in processCommand, and filled with status data,
	 * and is returned in the GET_STATUS_DONE object.
	 */
	private Hashtable hashTable = null;
	/**
	 * Standard status string passed back in the hashTable, 
	 * describing the detector temperature status health,
	 * using the standard keyword KEYWORD_DETECTOR_TEMPERATURE_INSTRUMENT_STATUS. 
	 * Initialised to VALUE_STATUS_UNKNOWN.
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#KEYWORD_DETECTOR_TEMPERATURE_INSTRUMENT_STATUS
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#VALUE_STATUS_UNKNOWN
	 */
	private String detectorTemperatureInstrumentStatus = GET_STATUS_DONE.VALUE_STATUS_UNKNOWN;
	/**
	 * Standard status string passed back in the hashTable, 
	 * describing the CCD comms status.
	 */
	private String commsInstrumentStatus = GET_STATUS_DONE.VALUE_STATUS_UNKNOWN;

	/**
	 * Constructor.
	 */
	public GET_STATUSImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.GET_STATUS&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.GET_STATUS";
	}

	/**
	 * This method gets the GET_STATUS command's acknowledge time. 
	 * This takes the default acknowledge time to implement.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see LOTUSTCPServerConnectionThread#getDefaultAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		ACK acknowledge = null;

		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(serverConnectionThread.getDefaultAcknowledgeTime());
		return acknowledge;
	}

	/**
	 * This method implements the GET_STATUS command. 
	 * The local hashTable is setup (returned in the done object) and a local copy of status setup.
	 * <ul>
	 * </ul>
	 * An object of class GET_STATUS_DONE is returned, with the information retrieved.
	 * @param command The GET_STATUS command.
	 * @return An object of class GET_STATUS_DONE is returned.
	 * @see #lotus
	 * @see #status
	 * @see #hashTable
	 * @see #detectorTemperatureInstrumentStatus
	 * @see LOTUSStatus#getProperty
	 * @see LOTUSStatus#getCurrentCommand
	 * @see LOTUSStatus#getCurrentMode
	 * @see LOTUSStatus#getExposureLength
	 * @see LOTUSStatus#getExposureStartTime
	 * @see LOTUSStatus#getExposureCount
	 * @see LOTUSStatus#getExposureNumber
	 * @see GET_STATUS#getLevel
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#KEYWORD_INSTRUMENT_STATUS
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#KEYWORD_DETECTOR_TEMPERATURE_INSTRUMENT_STATUS
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		GET_STATUS getStatusCommand = (GET_STATUS)command;
		GET_STATUS_DONE getStatusDone = new GET_STATUS_DONE(command.getId());
		ISS_TO_INST currentCommand = null;

		try
		{
			// Create new hashtable to be returned
			// v1.5 generic typing of collections:<String, Object>, 
			// can't be used due to v1.4 compatibility
			hashTable = new Hashtable();
			// current mode, derived from exposure status
			getStatusDone.setCurrentMode(status.getCurrentMode());
			// What instrument is this?
			hashTable.put("Instrument",status.getProperty("lotus.get_status.instrument_name"));
			// Initialise Standard status to UNKNOWN
			detectorTemperatureInstrumentStatus = GET_STATUS_DONE.VALUE_STATUS_UNKNOWN;
			hashTable.put(GET_STATUS_DONE.KEYWORD_DETECTOR_TEMPERATURE_INSTRUMENT_STATUS,
				      detectorTemperatureInstrumentStatus);
			hashTable.put(GET_STATUS_DONE.KEYWORD_INSTRUMENT_STATUS,
				      GET_STATUS_DONE.VALUE_STATUS_UNKNOWN);
			// current command
			currentCommand = status.getCurrentCommand();
			if(currentCommand == null)
				hashTable.put("currentCommand","");
			else
				hashTable.put("currentCommand",currentCommand.getClass().getName());
		}
		catch(Exception e)
		{
			lotus.error(this.getClass().getName()+
				    ":processCommand:Retrieving basic status failed.",e);
			getStatusDone.setDisplayInfo(hashTable);
			getStatusDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+2500);
			getStatusDone.setErrorString("processCommand:Retrieving basic status failed:"+e);
			getStatusDone.setSuccessful(false);
			return getStatusDone;
		}
		// exposure data
		hashTable.put("Exposure Length",new Integer(status.getExposureLength()));
		hashTable.put("Exposure Start Time",new Long(status.getExposureStartTime()));
		hashTable.put("Exposure Count",new Integer(status.getExposureCount()));
		hashTable.put("Exposure Number",new Integer(status.getExposureNumber()));
	// intermediate level information - basic plus controller calls.
		if(getStatusCommand.getLevel() >= GET_STATUS.LEVEL_INTERMEDIATE)
		{
			getIntermediateStatus();
		}// end if intermediate level status
	// Get full status information.
		if(getStatusCommand.getLevel() >= GET_STATUS.LEVEL_FULL)
		{
			getFullStatus();
		}
	// set hashtable and return values.
		getStatusDone.setDisplayInfo(hashTable);
		getStatusDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_NO_ERROR);
		getStatusDone.setErrorString("");
		getStatusDone.setSuccessful(true);
	// return done object.
		return getStatusDone;
	}

	/**
	 * Get intermediate level status. This is temperature information from the CCD.
	 * The overall health and well-being statii are then computed using setInstrumentStatus.
	 * @see #getCCDTemperature
	 * @see #getCCDConnected
	 * @see #commsInstrumentStatus
	 * @see #setInstrumentStatus
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#VALUE_STATUS_OK
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#VALUE_STATUS_FAIL
	 */
	private void getIntermediateStatus()
	{
		double temperature;

		try
		{
			commsInstrumentStatus = GET_STATUS_DONE.VALUE_STATUS_OK;
			getCCDConnected();
			getCCDTemperature();
			getRemainingExposureLength();
		}
		catch(Exception e)
		{
			lotus.error(this.getClass().getName()+
				      ":getIntermediateStatus:Retrieving temperature status failed.",e);
			commsInstrumentStatus = GET_STATUS_DONE.VALUE_STATUS_FAIL;
		}
		hashTable.put("CCD.Comms.Status",commsInstrumentStatus);
	// Standard status
		setInstrumentStatus();
	}

	/**
	 * Get whether the CCD is connected in the INDI server.
	 * @return A booolean, true if connected and false if not connected.
	 * @see #hashTable
	 * @see #ccd
	 * @see #setDetectorTemperatureInstrumentStatus
	 * @see ngat.lotus.ccd.StarlightExpressTriusSX35#getConnected
	 */
	protected boolean getCCDConnected() throws Exception
	{
		boolean connected = false;

		lotus.log(Logging.VERBOSITY_INTERMEDIATE,"getCCDConnected:started.");
		connected = ccd.getConnected();
		hashTable.put("CCD.Connected",new Boolean(connected));
		if(connected == false)
		{
			lotus.log(Logging.VERBOSITY_INTERMEDIATE,
				  "getCCDConnected:CCD is not connected to INDI server:Setting comms status to FAIL.");
			commsInstrumentStatus = GET_STATUS_DONE.VALUE_STATUS_FAIL;
		}
		return connected;
	}

	/**
	 * Get the current CCD temperature.
	 * @return A double representing the CCD temperature in Kelvin.
	 * @exception Exception Thrown if an error occurs.
	 * @see #hashTable
	 * @see #ccd
	 * @see #setDetectorTemperatureInstrumentStatus
	 * @see ngat.lotus.ccd.StarlightExpressTriusSX35#getTemperature
	 * @see ngat.lotus.LOTUS#CENTIGRADE_TO_KELVIN
	 */
	protected double getCCDTemperature() throws Exception
	{
		double temperature;

		lotus.log(Logging.VERBOSITY_INTERMEDIATE,"getCCDTemperature:started.");
		temperature = ccd.getTemperature();
		hashTable.put("Temperature",new Double(temperature+LOTUS.CENTIGRADE_TO_KELVIN));
		setDetectorTemperatureInstrumentStatus(temperature+LOTUS.CENTIGRADE_TO_KELVIN);
		lotus.log(Logging.VERBOSITY_INTERMEDIATE,"getCCDTemperature:finished with temperature:"+
			   temperature+".");
		return temperature+LOTUS.CENTIGRADE_TO_KELVIN;
	}

	/**
	 * Get the remaining exposure legnth returned by the INDI server. If the returned value is greater than
	 * zero (and therefore valid), the "Remaining Exposure Time" and "Elapsed Exposure Time" are computed
	 * (using the saved exposure length in the status object) and saved in the Hashtable.
	 * @see #hashTable
	 * @see #ccd
	 * @see LOTUSStatus#getExposureLength
	 * @see ngat.lotus.ccd.StarlightExpressTriusSX35#getRemainingExposureLength
	 */
	protected void getRemainingExposureLength() throws Exception
	{
		double remainingExposureLength;
		int remainingExposureLengthMs,exposureLengthMs,elapsedExposureLengthMs;

		lotus.log(Logging.VERBOSITY_INTERMEDIATE,"getRemainingExposureLength:started.");
		// get remaining exposure length from INDI driver
		remainingExposureLength = ccd.getRemainingExposureLength();
		lotus.log(Logging.VERBOSITY_INTERMEDIATE,"getRemainingExposureLength: "+remainingExposureLength+" s.");
		if(remainingExposureLength > 0.0)
		{
			// convert to milliseconds and save in "Remaining Exposure Time" (not used by icsgui)
			remainingExposureLengthMs = (int)(remainingExposureLength*1000.0);
			hashTable.put("Remaining Exposure Time",new Integer(remainingExposureLengthMs));
			// get status's saved exposure length in ms
			exposureLengthMs = status.getExposureLength();
			// compute elapsed exposure legnth in ms, and add to status
			elapsedExposureLengthMs = exposureLengthMs-remainingExposureLengthMs;
			hashTable.put("Elapsed Exposure Time",new Integer(elapsedExposureLengthMs));
		}
		else
		{
			hashTable.put("Remaining Exposure Time",new Integer(0));
			hashTable.put("Elapsed Exposure Time",new Integer(0));
		}
	}

	/**
	 * Set the standard entry for detector temperature in the hashtable based upon the 
	 * current temperature.
	 * Reads the folowing config:
	 * <ul>
	 * <li>lotus.get_status.detector.temperature.warm.warn
	 * <li>lotus.get_status.detector.temperature.warm.fail
	 * <li>lotus.get_status.detector.temperature.cold.warn
	 * <li>lotus.get_status.detector.temperature.cold.fail
	 * </ul>
	 * @param currentTemperature The current temperature in degrees C.
	 * @exception NumberFormatException Thrown if the config is not a valid double.
	 * @see #hashTable
	 * @see #status
	 * @see #detectorTemperatureInstrumentStatus
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#KEYWORD_DETECTOR_TEMPERATURE_INSTRUMENT_STATUS
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#VALUE_STATUS_OK
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#VALUE_STATUS_WARN
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#VALUE_STATUS_FAIL
	 */
	protected void setDetectorTemperatureInstrumentStatus(double currentTemperature) 
		throws NumberFormatException
	{
		double warmWarnTemperature,warmFailTemperature,coldWarnTemperature,coldFailTemperature;

		// get config for warn and fail temperatures
		warmWarnTemperature = status.getPropertyDouble("lotus.get_status.detector.temperature.warm.warn");
		warmFailTemperature = status.getPropertyDouble("lotus.get_status.detector.temperature.warm.fail");
		coldWarnTemperature = status.getPropertyDouble("lotus.get_status.detector.temperature.cold.warn");
		coldFailTemperature = status.getPropertyDouble("lotus.get_status.detector.temperature.cold.fail");
		// set status
		if(currentTemperature > warmFailTemperature)
			detectorTemperatureInstrumentStatus = GET_STATUS_DONE.VALUE_STATUS_FAIL;
		else if(currentTemperature > warmWarnTemperature)
			detectorTemperatureInstrumentStatus = GET_STATUS_DONE.VALUE_STATUS_WARN;
		else if(currentTemperature < coldFailTemperature)
			detectorTemperatureInstrumentStatus = GET_STATUS_DONE.VALUE_STATUS_FAIL;
		else if(currentTemperature < coldWarnTemperature)
			detectorTemperatureInstrumentStatus = GET_STATUS_DONE.VALUE_STATUS_WARN;
		else
			detectorTemperatureInstrumentStatus = GET_STATUS_DONE.VALUE_STATUS_OK;
		// set hashtable entry
		hashTable.put(GET_STATUS_DONE.KEYWORD_DETECTOR_TEMPERATURE_INSTRUMENT_STATUS,
			      detectorTemperatureInstrumentStatus);
		lotus.log(Logging.VERBOSITY_VERY_VERBOSE,"setDetectorTemperatureInstrumentStatus:temperature:"+
			  currentTemperature+" produced keyword "+
			  GET_STATUS_DONE.KEYWORD_DETECTOR_TEMPERATURE_INSTRUMENT_STATUS+" with value "+
			  detectorTemperatureInstrumentStatus);
	}

	/**
	 * Set the overall instrument status keyword in the hashtable. 
	 * This is derived from sub-system keyword values,
	 * the detector temperature and comms status data. HashTable entry KEYWORD_INSTRUMENT_STATUS)
	 * should be set to the worst of OK/WARN/FAIL. If sub-systems are UNKNOWN, OK is returned.
	 * @see #hashTable
	 * @see #status
	 * @see #detectorTemperatureInstrumentStatus
	 * @see #commsInstrumentStatus
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#KEYWORD_INSTRUMENT_STATUS
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#VALUE_STATUS_OK
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#VALUE_STATUS_WARN
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#VALUE_STATUS_FAIL
	 */
	protected void setInstrumentStatus()
	{
		String instrumentStatus;

		// default to OK
		instrumentStatus = GET_STATUS_DONE.VALUE_STATUS_OK;
		// if a sub-status is in warning, overall status is in warning
		if(detectorTemperatureInstrumentStatus.equals(GET_STATUS_DONE.VALUE_STATUS_WARN))
			instrumentStatus = GET_STATUS_DONE.VALUE_STATUS_WARN;
		if(commsInstrumentStatus.equals(GET_STATUS_DONE.VALUE_STATUS_WARN))
			instrumentStatus = GET_STATUS_DONE.VALUE_STATUS_WARN;
		// if a sub-status is in fail, overall status is in fail. This overrides a previous warn
	        if(detectorTemperatureInstrumentStatus.equals(GET_STATUS_DONE.VALUE_STATUS_FAIL))
			instrumentStatus = GET_STATUS_DONE.VALUE_STATUS_FAIL;
		if(commsInstrumentStatus.equals(GET_STATUS_DONE.VALUE_STATUS_FAIL))
			instrumentStatus = GET_STATUS_DONE.VALUE_STATUS_FAIL;
		// set standard status in hashtable
		hashTable.put(GET_STATUS_DONE.KEYWORD_INSTRUMENT_STATUS,instrumentStatus);
	}

	/**
	 * Method to get misc status, when level FULL has been selected.
	 * The following data is put into the hashTable:
	 * <ul>
	 * <li><b>Log Level</b> The current logging level LOTUS is using.
	 * <li><b>Disk Usage</b> The results of running a &quot;df -k&quot;, to get the disk usage.
	 * <li><b>Process List</b> The results of running a 
	 *       &quot;ps -e -o pid,pcpu,vsz,ruser,stime,time,args&quot;, 
	 * 	to get the processes running on this machine.
	 * <li><b>TCP Socket List</b> The results of running a 
	 *       &quot;netstat -t&quot;, 
	 * 	to get the open TCP sockets on this machine.
	 * <li><b>Uptime</b> The results of running a &quot;uptime&quot;, 
	 * 	to get system load and time since last reboot.
	 * <li><b>Total Memory, Free Memory</b> The total and free memory in the Java virtual machine.
	 * <li><b>java.version, java.vendor, java.home, java.vm.version, java.vm.vendor, java.class.path</b>
	 * 	Java virtual machine version, classpath and type.
	 * <li><b>os.name, os.arch, os.version</b> The operating system type/version.
	 * <li><b>user.name, user.home, user.dir</b> Data about the user the process is running as.
	 * <li><b>thread.list</b> A list of threads the LOTUS process is running.
	 * </ul>
	 * @see #serverConnectionThread
	 * @see #hashTable
	 * @see ExecuteCommand#run
	 * @see LOTUSStatus#getLogLevel
	 */
	private void getFullStatus()
	{
		ExecuteCommand executeCommand = null;
		Runtime runtime = null;
		StringBuffer sb = null;
		Thread threadList[] = null;
		int threadCount;

		// log level
		hashTable.put("Log Level",new Integer(status.getLogLevel()));
		// execute 'df -k' on instrument computer
		executeCommand = new ExecuteCommand("df -k");
		executeCommand.run();
		if(executeCommand.getException() == null)
			hashTable.put("Disk Usage",new String(executeCommand.getOutputString()));
		else
			hashTable.put("Disk Usage",new String(executeCommand.getException().toString()));
		// execute "ps -e -o pid,pcpu,vsz,ruser,stime,time,args" on instrument computer
		executeCommand = new ExecuteCommand("ps -e -o pid,pcpu,vsz,ruser,stime,time,args");
		executeCommand.run();
		if(executeCommand.getException() == null)
			hashTable.put("Process List",new String(executeCommand.getOutputString()));
		else
			hashTable.put("Process List",new String(executeCommand.getException().toString()));
		// execute "netstat -t" on instrument computer
		executeCommand = new ExecuteCommand("netstat -t");
		executeCommand.run();
		if(executeCommand.getException() == null)
			hashTable.put("TCP Socket List",new String(executeCommand.getOutputString()));
		else
			hashTable.put("TCP Socket List",new String(executeCommand.getException().toString()));
		// execute "uptime" on instrument computer
		executeCommand = new ExecuteCommand("uptime");
		executeCommand.run();
		if(executeCommand.getException() == null)
			hashTable.put("Uptime",new String(executeCommand.getOutputString()));
		else
			hashTable.put("Uptime",new String(executeCommand.getException().toString()));
		// get vm memory situation
		runtime = Runtime.getRuntime();
		hashTable.put("Free Memory",new Long(runtime.freeMemory()));
		hashTable.put("Total Memory",new Long(runtime.totalMemory()));
		// get some java vm information
		hashTable.put("java.version",new String(System.getProperty("java.version")));
		hashTable.put("java.vendor",new String(System.getProperty("java.vendor")));
		hashTable.put("java.home",new String(System.getProperty("java.home")));
		hashTable.put("java.vm.version",new String(System.getProperty("java.vm.version")));
		hashTable.put("java.vm.vendor",new String(System.getProperty("java.vm.vendor")));
		hashTable.put("java.class.path",new String(System.getProperty("java.class.path")));
		hashTable.put("os.name",new String(System.getProperty("os.name")));
		hashTable.put("os.arch",new String(System.getProperty("os.arch")));
		hashTable.put("os.version",new String(System.getProperty("os.version")));
		hashTable.put("user.name",new String(System.getProperty("user.name")));
		hashTable.put("user.home",new String(System.getProperty("user.home")));
		hashTable.put("user.dir",new String(System.getProperty("user.dir")));
	}
}

