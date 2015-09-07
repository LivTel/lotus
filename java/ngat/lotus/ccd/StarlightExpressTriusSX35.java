// StarlightExpressTriusSX35.java
// $HeadURL$
package ngat.lotus.ccd;

import java.io.*;
import java.lang.*;

import ngat.util.*;
import ngat.util.logging.*;

/**
 * This class allows us to control the Starlight Express Trius SX-35 via an INDI server.
 * @author Chris Mottram
 * @version $Revision: 57 $
 */
public class StarlightExpressTriusSX35 extends LOTUSINDIDevice
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");
	/**
	 * The camera device name (INDI device) the Starlight Express CCD driver uses for the SX-35.
	 */
	public final static String CAMERA_DEVICE_NAME = "SX CCD SXVR-H35";
	/**
	 * The camera property name (INDI property) the Starlight Express CCD driver uses for binning.
	 */
	public final static String BINNING_PROPERTY_NAME = "CCD_BINNING";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for 
	 *  horizontal binning.
	 */
	public final static String HOR_BIN_ELEMENT_NAME = "HOR_BIN";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for 
	 *  vertical binning.
	 */
	public final static String VER_BIN_ELEMENT_NAME = "VER_BIN";
	/**
	 * The camera property name (INDI property) the Starlight Express CCD driver uses for Connection data.
	 */
	public final static String CONNECTION_PROPERTY_NAME = "CONNECTION";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for connection.
	 */
	public final static String CONNECTION_ELEMENT_NAME = "CONNECT";
	/**
	 * The camera property name (INDI property) the Starlight Express CCD driver uses for CCD temperature.
	 */
	public final static String CCD_TEMPERATURE_PROPERTY_NAME = "CCD_TEMPERATURE";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for CCD temperature.
	 */
	public final static String CCD_TEMPERATURE_ELEMENT_NAME = "CCD_TEMPERATURE_VALUE";
	/**
	 * The camera property name (INDI property) the Starlight Express CCD driver uses for seting upload mode.
	 */
	public final static String UPLOAD_MODE_PROPERTY_NAME = "UPLOAD_MODE";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for setting 
	 * upload mode.
	 */
	public final static String UPLOAD_MODE_ELEMENT_NAME = "UPLOAD_LOCAL";
	/**
	 * The camera property name (INDI property) the Starlight Express CCD driver uses for setting upload settings.
	 */
	public final static String UPLOAD_SETTINGS_PROPERTY_NAME = "UPLOAD_SETTINGS";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for setting 
	 * upload directory.
	 */
	public final static String UPLOAD_DIR_ELEMENT_NAME = "UPLOAD_DIR";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for setting 
	 * upload filename.
	 */
	public final static String UPLOAD_PREFIX_ELEMENT_NAME = "UPLOAD_PREFIX";
	/**
	 * The camera property property name (INDI property) the Starlight Express CCD driver uses for setting debugging.
	 */
	public final static String DEBUG_PROPERTY_NAME = "DEBUG";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for setting debugging.
	 */
	public final static String ENABLE_ELEMENT_NAME = "ENABLE";
	/**
	 * The camera property property name (INDI property) the Starlight Express CCD driver uses for 
	 * setting the exposure length (and starting an exposure).
	 */
	public final static String CCD_EXPOSURE_PROPERTY_NAME = "CCD_EXPOSURE";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for 
	 * setting the exposure length (and starting an exposure).
	 */
	public final static String CCD_EXPOSURE_VALUE_ELEMENT_NAME = "CCD_EXPOSURE_VALUE";
	/**
	 * The camera property property name (INDI property) the Starlight Express CCD driver uses for 
	 * setting the exposure length (and starting an exposure).
	 */
	public final static String CCD_FRAME_TYPE_PROPERTY_NAME = "CCD_FRAME_TYPE";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for 
	 * setting the exposure frame type to light (normal exposure).
	 */
	public final static String FRAME_LIGHT_ELEMENT_NAME = "FRAME_LIGHT";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for 
	 * setting the exposure frame type to bias.
	 */
	public final static String FRAME_BIAS_ELEMENT_NAME = "FRAME_BIAS";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for 
	 * setting the exposure frame type to dark.
	 */
	public final static String FRAME_DARK_ELEMENT_NAME = "FRAME_DARK";
	/**
	 * The camera property property name (INDI property) the Starlight Express CCD driver uses for 
	 * aborting an exposure.
	 */
	public final static String CCD_ABORT_EXPOSURE_PROPERTY_NAME = "CCD_ABORT_EXPOSURE";
	/**
	 * The camera property element name (INDI element) the Starlight Express CCD driver uses for 
	 * aborting an exposure.
	 */
	public final static String ABORT_ELEMENT_NAME = "ABORT";
	/**
	 * The number of times round the exposure loop to request the remaining exposure length, and not get it, 
	 * before throwing an exception.
	 */
	public final static int EXPOSURE_TIMEOUT_COUNT = 10;
	/**
	 * The Starlight Express SX35 appears to under-expose by 1.5s. This offset is used to correct this.
	 * See fault #2335 .
	 */
	public final static double SX35_EXPOSURE_LENGTH_OFFSET = 1.5;

	/**
	 * Logger to use.
	 */
	Logger logger = null;
	/**
	 * A timestamp taken when setting the exposure length property, and therefore starting the exposure.
	 */
	long startExposureTimestamp;

	/**
	 * Default constructor.
	 * @see #logger
	 */
	public StarlightExpressTriusSX35()
	{
		super();
		logger = LogManager.getLogger(this);
	}

	/**
	 * Open a connection to the INDI server, and then connect to the camera.
	 * @exception IOException Thrown if the connection fails.
	 * @exception IllegalArgumentException Thrown if setting the connection property to On fails.
	 * @exception ClassCastException Thrown if setting the connection property to On fails.
	 * @exception Exception Thrown if setting the connection property to On fails.
	 * @see #CAMERA_DEVICE_NAME
	 * @see #CONNECTION_PROPERTY_NAME
	 * @see #CONNECTION_ELEMENT_NAME
	 * @see #setPropertyValue
	 * @see LOTUSINDIDevice#connect(java.lang.String)
	 */
	public void connect() throws IOException, IllegalArgumentException, ClassCastException, Exception
	{
		// connect to INDI server
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":connect:Connecting to INDI server.");
		super.connect(CAMERA_DEVICE_NAME);
		// NOW connect to camera
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":connect:Telling INDI server to connect to camera.");
		setPropertyValue(CAMERA_DEVICE_NAME,CONNECTION_PROPERTY_NAME,CONNECTION_ELEMENT_NAME,true);
		// Add callbacks for temperature, connection status, exposure status?
	}

	/**
	 * Tell the INDI server to disconnect from the camera, and close the connection to the INDI server.
	 * @exception IllegalArgumentException Thrown if setting the connection property to Off fails.
	 * @exception ClassCastException Thrown if setting the connection property to Off fails.
	 * @exception Exception Thrown if setting the connection property to Off fails.
	 * @see #CAMERA_DEVICE_NAME
	 * @see #CONNECTION_PROPERTY_NAME
	 * @see #CONNECTION_ELEMENT_NAME
	 * @see #setPropertyValue
	 * @see LOTUSINDIDevice#disconnect
	 */
	public void disconnect()  throws IOException, IllegalArgumentException, ClassCastException, Exception
	{
		// tell camera to disconnect
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":disconnect:Telling INDI server to disconnect from camera.");
		setPropertyValue(CAMERA_DEVICE_NAME,CONNECTION_PROPERTY_NAME,CONNECTION_ELEMENT_NAME,false);
		// close connection to INDI server
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":disconnect:Disconnecting from INDI server.");
		super.disconnect();
	}

	/**
	 * Get whether the INDI server thinks it has an open connection to the camera.
	 * This is <b>NOT</b> whether we are connected to the INDI server, but whether the INDI server
	 * has an opena ctive connection to the camera.
	 * This should have opened by the setPropertyValue in the connect method, but if the camera driver indi_sx_ccd
	 * crashes it can be reset to off.
	 * @return A boolean, true if the INDI server is connected to the camera, and FALSE if it is not.
	 * @exception IllegalArgumentException Thrown if getting the property fails.
	 * @exception ClassCastException Thrown if getting the property fails.
	 * @exception Exception Thrown if getting the property fails.
	 * @see #CAMERA_DEVICE_NAME
	 * @see #CONNECTION_PROPERTY_NAME
	 * @see #CONNECTION_ELEMENT_NAME
	 * @see #getSwitchPropertyValue
	 */
	public boolean getConnected() throws IOException, IllegalArgumentException, ClassCastException, Exception
	{
		return getSwitchPropertyValue(CAMERA_DEVICE_NAME,CONNECTION_PROPERTY_NAME,CONNECTION_ELEMENT_NAME);
	}

	/**
	 * Whether to turn driver debugging on or off.
	 * @param onff If true turn driver debugging on, otherwise turn driver debugging off.
	 * @exception IllegalArgumentException Thrown if setting the property fails.
	 * @exception ClassCastException Thrown if setting the property fails.
	 * @exception Exception Thrown if setting the property fails.
	 * @see #CAMERA_DEVICE_NAME
	 * @see #DEBUG_PROPERTY_NAME
	 * @see #ENABLE_ELEMENT_NAME
	 * @see #setPropertyValue
	 * @see #logger
	 */
	public void setDebug(boolean onff) throws IOException, IllegalArgumentException, ClassCastException, Exception
	{
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+":setDebug:"+onff);
		setPropertyValue(CAMERA_DEVICE_NAME,DEBUG_PROPERTY_NAME,ENABLE_ELEMENT_NAME,onff);
	}

	/**
	 * Set the CCD temperature.
	 * @param targetTemperature The CCD temperature, in degrees celcius.
	 * @exception IllegalArgumentException Thrown by setPropertyValue if it fails.
	 * @exception ClassCastException Thrown by setPropertyValue if it fails.
	 * @exception Exception Thrown by setPropertyValue if it fails.
	 * @see #CAMERA_DEVICE_NAME
	 * @see #CCD_TEMPERATURE_PROPERTY_NAME
	 * @see #CCD_TEMPERATURE_ELEMENT_NAME
	 * @see #setPropertyValue
	 * @see #logger
	 */
	public void setTemperature(double targetTemperature) throws IllegalArgumentException, ClassCastException, 
								    Exception
	{
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":setTemperature:Setting CCD temperature to:"+targetTemperature);
		setPropertyValue(CAMERA_DEVICE_NAME,CCD_TEMPERATURE_PROPERTY_NAME,CCD_TEMPERATURE_ELEMENT_NAME,
				 targetTemperature);
	}

	/**
	 * Get the CCD temperature.
	 * @return The current CCD temperature, in degrees celcius.
	 * @exception IllegalArgumentException Thrown by getPropertyValue if it fails.
	 * @exception ClassCastException Thrown by getPropertyValue if it fails.
	 * @exception Exception Thrown by getPropertyValue if it fails, or returns a value that is not a double.
	 * @see #CAMERA_DEVICE_NAME
	 * @see #CCD_TEMPERATURE_PROPERTY_NAME
	 * @see #CCD_TEMPERATURE_ELEMENT_NAME
	 * @see #getPropertyValue
	 * @see #logger
	 */
	public double getTemperature()  throws IllegalArgumentException, ClassCastException, Exception
	{
		Object value = null;
		Double doubleValue = null;

		value = getPropertyValue(CAMERA_DEVICE_NAME,CCD_TEMPERATURE_PROPERTY_NAME,
					 CCD_TEMPERATURE_ELEMENT_NAME);
		if((value instanceof Double) == false)
		{
			throw new Exception(this.getClass().getName()+
					    ":getTemperature:Returned temperature value had illegal class:"+
					    value.getClass().getName());
		}
		doubleValue = (Double)value;
		return doubleValue.doubleValue();
	}

	/**
	 * Set the CCD binning.
	 * @param binX The X binning factor, should be 1 or greater.
	 * @param binY The Y binning factor, should be 1 or greater.
	 * @exception IllegalArgumentException Thrown by setPropertyValue if it fails.
	 * @exception ClassCastException Thrown by setPropertyValue if it fails.
	 * @exception Exception Thrown by setPropertyValue if it fails.
	 * @see #CAMERA_DEVICE_NAME
	 * @see #BINNING_PROPERTY_NAME
	 * @see #HOR_BIN_ELEMENT_NAME
	 * @see #VER_BIN_ELEMENT_NAME
	 * @see #setPropertyValues
	 * @see #logger
	 */
	public void setBinning(int binX,int binY) throws IllegalArgumentException, ClassCastException, 
							 Exception
	{
		String elementNameList[] = new String[2];
		Double valueList[] = new Double[2];

		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":setBinning:Setting binning to:"+binX+"x"+binY);
		// looks like the INDI driver crashes unless you set horizontal and vertical binning at the same time
		// If you set the horizontal binning by itself the server reads the vertical binning as zero and then 
		// The SX35 driver EOFs and restarts (probably due to a division by zero error)
		elementNameList[0] = HOR_BIN_ELEMENT_NAME;
		valueList[0] = new Double(binX);
		elementNameList[1] = VER_BIN_ELEMENT_NAME;
		valueList[1] = new Double(binY);
		setPropertyValues(CAMERA_DEVICE_NAME,BINNING_PROPERTY_NAME,elementNameList,valueList);
		//setPropertyValue(CAMERA_DEVICE_NAME,BINNING_PROPERTY_NAME,HOR_BIN_ELEMENT_NAME,new Double(binX));
		//setPropertyValue(CAMERA_DEVICE_NAME,BINNING_PROPERTY_NAME,VER_BIN_ELEMENT_NAME,new Double(binY));
	}

	/**
	 * Set the upload mode to local, and the directory to use for storing FITS images.
	 * @param directoryName The directory to use for storing data, this does not need to be terminated in a '/'.
	 * @exception IllegalArgumentException Thrown by setPropertyValue if it fails.
	 * @exception ClassCastException Thrown by setPropertyValue if it fails.
	 * @exception Exception Thrown by setPropertyValue if it fails.
	 * @see #CAMERA_DEVICE_NAME
	 * @see #UPLOAD_MODE_PROPERTY_NAME
	 * @see #UPLOAD_MODE_ELEMENT_NAME
	 * @see #UPLOAD_SETTINGS_PROPERTY_NAME
	 * @see #UPLOAD_DIR_ELEMENT_NAME
	 * @see #setPropertyValue
	 * @see #logger
	 */
	public void setDataDirectory(String directoryName) throws IllegalArgumentException, ClassCastException, 
							 Exception
	{
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":setDataDirectory:Setting upload local.");
		setPropertyValue(CAMERA_DEVICE_NAME,UPLOAD_MODE_PROPERTY_NAME,UPLOAD_MODE_ELEMENT_NAME,true);
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":setDataDirectory:Setting upload directory to:"+directoryName);
		setPropertyValue(CAMERA_DEVICE_NAME,UPLOAD_SETTINGS_PROPERTY_NAME,UPLOAD_DIR_ELEMENT_NAME,
				 directoryName);
	}

	/**
	 * Take an exposure.
	 * <ul>
	 * <li>Set the filename to save the data into, using setFilename.
	 * <li>We set the frame type to light (normal exposure).
	 * <li>Save a timestamp for the start of the exposure.
	 * <li>We modify the exposure length by SX35_EXPOSURE_LENGTH_OFFSET as the CCD driver appears to underexpose.
	 * <li>Set the exposure length property. This will cause the driver to start an exposure.
	 * <li>Enter a loop, whilst the remaining exposure length is greater than zero 
	 *     and the timeoutCount is less than EXPOSURE_TIMEOUT_COUNT.
	 *     <ul>
	 *     <li>Attempt to read the remaining exposure length. This can fail if the driver 
	 *         is reading out the camera.
	 *     <li>If the returned value is a double, update the remaining exposure length.
	 *     <li>If the returned value is not a double, increment the timeoutCount and print a message.
	 *     <li>If the read of the remaining exposure length failed, increment the timeoutCount and print a message.
	 *     <li>Sleep for a second.
	 *     </ul>
	 * <li>If the timeoutCount is greater than EXPOSURE_TIMEOUT_COUNT throw an exception.
	 * </ul>
	 * @param exposureLength The exposure length in seconds as a double.
	 * @param filename A string representing the FITS filename to save the acquired data into.
	 *        This should be the leaf filename only, the directory should be set using setDataDirectory first.
	 * @exception Exception Thrown if setting the exposure length fails, or a timeout occurs when
	 *            trying to get the remaining exposure length.
	 * @see #CAMERA_DEVICE_NAME
	 * @see #CCD_FRAME_TYPE_PROPERTY_NAME
	 * @see #FRAME_LIGHT_ELEMENT_NAME
	 * @see #CCD_EXPOSURE_PROPERTY_NAME
	 * @see #CCD_EXPOSURE_VALUE_ELEMENT_NAME
	 * @see #SX35_EXPOSURE_LENGTH_OFFSET
	 * @see #setFilename
	 * @see #startExposureTimestamp
	 */
	public void expose(double exposureLength,String filename) throws Exception
	{
		Object retval;
		Double remainingExposureLengthObject;
		double modifiedExposureLength,remainingExposureLength = 0;
		int timeoutCount = 0;

		logger.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+":expose:Started.");
		// set filename
		logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":expose:Setting filename to "+filename+
			   ".");
		setFilename(filename);
		// set the frame type to light
		logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":expose:Setting frame type to light.");
		setPropertyValue(CAMERA_DEVICE_NAME,CCD_FRAME_TYPE_PROPERTY_NAME,FRAME_LIGHT_ELEMENT_NAME,true);
		// Add 1.5s to exposure length
		// See fault #2335
		logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":expose:Modifying exposure length of "+
			   exposureLength+" seconds to take account of SX35 underexposing.");
		modifiedExposureLength = exposureLength+SX35_EXPOSURE_LENGTH_OFFSET;
		logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":expose:Starting exposure of modified length "+
			   modifiedExposureLength+" seconds.");
		// save start exposure timestamp
		startExposureTimestamp = System.currentTimeMillis();
		// start exposure
		setPropertyValue(CAMERA_DEVICE_NAME,CCD_EXPOSURE_PROPERTY_NAME,CCD_EXPOSURE_VALUE_ELEMENT_NAME,
				 modifiedExposureLength);
		remainingExposureLength = modifiedExposureLength;
		timeoutCount = 0;
		while((remainingExposureLength > 0.0) && (timeoutCount < EXPOSURE_TIMEOUT_COUNT))
		{
			// note getting the exposure length can fail with no property existing during readout
			try
			{
				retval = getPropertyValue(CAMERA_DEVICE_NAME,CCD_EXPOSURE_PROPERTY_NAME,
							  CCD_EXPOSURE_VALUE_ELEMENT_NAME);
				if(retval instanceof Double)
				{
					remainingExposureLengthObject = (Double)retval;
					remainingExposureLength = remainingExposureLengthObject.doubleValue();
					logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+
						   ":expose:Remaining exposure length:"+remainingExposureLength);
					timeoutCount = 0;
				}
				else
				{
					logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+
						   ":expose:Returned exposure length was not a double.");
					timeoutCount++;
				}
			}
			catch(Exception e)
			{
				logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+
					   ":expose:Failed to retrieve remaining exposure length.");
				timeoutCount++;
			}
			// sleep a bit before trying again
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e)
			{
				logger.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
					   ":expose:Sleep interrupted.");
			}
			// diddly check still connected to the camera OK?
		}
		if(timeoutCount >= EXPOSURE_TIMEOUT_COUNT)
		{
			throw new Exception(this.getClass().getName()+
					    ":expose:Timeout when trying to retrieve remaining exposure length.");
		}
		logger.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+":expose:Finished.");
	}

	/**
	 * Take a bias frame.
	 * <ul>
	 * <li>Set the filename to save the data into, using setFilename.
	 * <li>We set the frame type to bias (normal.
	 * <li>Save a timestamp for the start of the exposure.
	 * <li>Set the exposure length property (to some very small value). 
	 * <li>Enter a loop, whilst the remaining exposure length is greater than zero 
	 *     and the timeoutCount is less than EXPOSURE_TIMEOUT_COUNT.
	 *     <ul>
	 *     <li>Attempt to read the remaining exposure length. This can fail if the driver 
	 *         is reading out the camera.
	 *     <li>If the returned value is a double, update the remaining exposure length.
	 *     <li>If the returned value is not a double, increment the timeoutCount and print a message.
	 *     <li>If the read of the remaining exposure length failed, increment the timeoutCount and print a message.
	 *     <li>Sleep for a second.
	 *     </ul>
	 * <li>If the timeoutCount is greater than EXPOSURE_TIMEOUT_COUNT throw an exception.
	 * </ul>
	 * @param filename A string representing the FITS filename to save the acquired data into.
	 *        This should be the leaf filename only, the directory should be set using setDataDirectory first.
	 * @exception Exception Thrown if setting the exposure length fails, or a timeout occurs when
	 *            trying to get the remaining exposure length.
	 * @see #CAMERA_DEVICE_NAME
	 * @see #CCD_FRAME_TYPE_PROPERTY_NAME
	 * @see #FRAME_BIAS_ELEMENT_NAME
	 * @see #CCD_EXPOSURE_PROPERTY_NAME
	 * @see #CCD_EXPOSURE_VALUE_ELEMENT_NAME
	 * @see #setFilename
	 * @see #startExposureTimestamp
	 */
	public void bias(String filename) throws Exception
	{
		Object retval;
		Double remainingExposureLengthObject;
		double remainingExposureLength = 0;
		int timeoutCount = 0;

		logger.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+":bias:Started.");
		// set filename
		logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":bias:Setting filename to "+filename+
			   ".");
		setFilename(filename);
		// set the frame type to bias
		logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":bias:Setting frame type to bias.");
		setPropertyValue(CAMERA_DEVICE_NAME,CCD_FRAME_TYPE_PROPERTY_NAME,FRAME_BIAS_ELEMENT_NAME,true);
		logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":bias:Starting readout.");
		// save start exposure timestamp
		startExposureTimestamp = System.currentTimeMillis();
		// start exposure
		setPropertyValue(CAMERA_DEVICE_NAME,CCD_EXPOSURE_PROPERTY_NAME,CCD_EXPOSURE_VALUE_ELEMENT_NAME,
				 new Double(0.001));
		remainingExposureLength = 0.001;
		timeoutCount = 0;
		while((remainingExposureLength > 0.0) && (timeoutCount < EXPOSURE_TIMEOUT_COUNT))
		{
			// note getting the exposure length can fail with no property existing during readout
			try
			{
				retval = getPropertyValue(CAMERA_DEVICE_NAME,CCD_EXPOSURE_PROPERTY_NAME,
							  CCD_EXPOSURE_VALUE_ELEMENT_NAME);
				if(retval instanceof Double)
				{
					remainingExposureLengthObject = (Double)retval;
					remainingExposureLength = remainingExposureLengthObject.doubleValue();
					logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+
						   ":bias:Remaining exposure length:"+remainingExposureLength);
					timeoutCount = 0;
				}
				else
				{
					logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+
						   ":bias:Returned exposure length was not a double.");
					timeoutCount++;
				}
			}
			catch(Exception e)
			{
				logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+
					   ":bias:Failed to retrieve remaining exposure length.");
				timeoutCount++;
			}
			// sleep a bit before trying again
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e)
			{
				logger.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
					   ":bias:Sleep interrupted.");
			}
			// diddly check still connected to the camera OK?
		}
		if(timeoutCount >= EXPOSURE_TIMEOUT_COUNT)
		{
			throw new Exception(this.getClass().getName()+
					    ":bias:Timeout when trying to retrieve remaining exposure length.");
		}
		logger.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+":bias:Finished.");
	}

	/**
	 * Take an dark frame.
	 * <ul>
	 * <li>Set the filename to save the data into, using setFilename.
	 * <li>We set the frame type to dark.
	 * <li>Save a timestamp for the start of the exposure.
	 * <li>We modify the exposure length by SX35_EXPOSURE_LENGTH_OFFSET as the CCD driver appears to underexpose.
	 * <li>Set the exposure length property. This will cause the driver to start an exposure.
	 * <li>Enter a loop, whilst the remaining exposure length is greater than zero 
	 *     and the timeoutCount is less than EXPOSURE_TIMEOUT_COUNT.
	 *     <ul>
	 *     <li>Attempt to read the remaining exposure length. This can fail if the driver 
	 *         is reading out the camera.
	 *     <li>If the returned value is a double, update the remaining exposure length.
	 *     <li>If the returned value is not a double, increment the timeoutCount and print a message.
	 *     <li>If the read of the remaining exposure length failed, increment the timeoutCount and print a message.
	 *     <li>Sleep for a second.
	 *     </ul>
	 * <li>If the timeoutCount is greater than EXPOSURE_TIMEOUT_COUNT throw an exception.
	 * </ul>
	 * @param exposureLength The exposure length in seconds as a double.
	 * @param filename A string representing the FITS filename to save the acquired data into.
	 *        This should be the leaf filename only, the directory should be set using setDataDirectory first.
	 * @exception Exception Thrown if setting the exposure length fails, or a timeout occurs when
	 *            trying to get the remaining exposure length.
	 * @see #CAMERA_DEVICE_NAME
	 * @see #CCD_FRAME_TYPE_PROPERTY_NAME
	 * @see #FRAME_DARK_ELEMENT_NAME
	 * @see #CCD_EXPOSURE_PROPERTY_NAME
	 * @see #CCD_EXPOSURE_VALUE_ELEMENT_NAME
	 * @see #SX35_EXPOSURE_LENGTH_OFFSET
	 * @see #setFilename
	 * @see #startExposureTimestamp
	 */
	public void dark(double exposureLength,String filename) throws Exception
	{
		Object retval;
		Double remainingExposureLengthObject;
		double modifiedExposureLength,remainingExposureLength = 0;
		int timeoutCount = 0;

		logger.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+":dark:Started.");
		// set filename
		logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":dark:Setting filename to "+filename+
			   ".");
		setFilename(filename);
		// set the frame type to light
		logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":dark:Setting frame type to dark.");
		setPropertyValue(CAMERA_DEVICE_NAME,CCD_FRAME_TYPE_PROPERTY_NAME,FRAME_DARK_ELEMENT_NAME,true);
		// Add 1.5s to exposure length
		// See fault #2335
		logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":dark:Modifying dark exposure length of "+
			   exposureLength+" seconds to take account of SX35 underexposing.");
		modifiedExposureLength = exposureLength+SX35_EXPOSURE_LENGTH_OFFSET;
		logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":dark:Starting dark of modified length "+
			   modifiedExposureLength+" seconds.");
		// save start exposure timestamp
		startExposureTimestamp = System.currentTimeMillis();
		// start exposure
		setPropertyValue(CAMERA_DEVICE_NAME,CCD_EXPOSURE_PROPERTY_NAME,CCD_EXPOSURE_VALUE_ELEMENT_NAME,
				 modifiedExposureLength);
		remainingExposureLength = modifiedExposureLength;
		timeoutCount = 0;
		while((remainingExposureLength > 0.0) && (timeoutCount < EXPOSURE_TIMEOUT_COUNT))
		{
			// note getting the exposure length can fail with no property existing during readout
			try
			{
				retval = getPropertyValue(CAMERA_DEVICE_NAME,CCD_EXPOSURE_PROPERTY_NAME,
							  CCD_EXPOSURE_VALUE_ELEMENT_NAME);
				if(retval instanceof Double)
				{
					remainingExposureLengthObject = (Double)retval;
					remainingExposureLength = remainingExposureLengthObject.doubleValue();
					logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+
						   ":dark:Remaining exposure length:"+remainingExposureLength);
					timeoutCount = 0;
				}
				else
				{
					logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+
						   ":dark:Returned exposure length was not a double.");
					timeoutCount++;
				}
			}
			catch(Exception e)
			{
				logger.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+
					   ":dark:Failed to retrieve remaining exposure length.");
				timeoutCount++;
			}
			// sleep a bit before trying again
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e)
			{
				logger.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
					   ":dark:Sleep interrupted.");
			}
			// diddly check still connected to the camera OK?
		}
		if(timeoutCount >= EXPOSURE_TIMEOUT_COUNT)
		{
			throw new Exception(this.getClass().getName()+
					    ":dark:Timeout when trying to retrieve remaining exposure length.");
		}
		logger.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+":dark:Finished.");
	}

	/**
	 * Abort a running exposure.
	 * @see #CAMERA_DEVICE_NAME
	 * @see #CCD_ABORT_EXPOSURE_PROPERTY_NAME
	 * @see #ABORT_ELEMENT_NAME
	 * @see #setPropertyValue
	 */
	public void abort()throws Exception
	{
		logger.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+":abort:Started.");
		setPropertyValue(CAMERA_DEVICE_NAME,CCD_ABORT_EXPOSURE_PROPERTY_NAME,ABORT_ELEMENT_NAME,true);
		logger.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+":abort:Finished.");
	}

	/**
	 * Return the timestamp saved just before the exposure length was set (and the exposure started).
	 * @return The timestamp saved just before the exposure was started, in milliseconds since the epoch 
	 * (1st Jan 1970).
	 * @see #startExposureTimestamp
	 */
	public long getStartExposureTimestamp()
	{
		return startExposureTimestamp;
	}

	/**
	 * Method to get the remaining exposure length in seconds.
	 * @return The remaining exposure length, in seconds, or -1.0 if 
	 *         the remaining exposure length was not retrieved.
	 * @see #logger
	 * @see #getPropertyValue
	 * @see #CAMERA_DEVICE_NAME
	 * @see #CCD_EXPOSURE_PROPERTY_NAME
 	 * @see #CCD_EXPOSURE_VALUE_ELEMENT_NAME
	 */
	public double getRemainingExposureLength()
	{
		Object retval = null;
		Double remainingExposureLengthObject = null;
		double remainingExposureLength;

		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":getRemainingExposureLength:Started.");
		// note getting the exposure length can fail with no property existing during readout
		try
		{
			retval = getPropertyValue(CAMERA_DEVICE_NAME,CCD_EXPOSURE_PROPERTY_NAME,
						  CCD_EXPOSURE_VALUE_ELEMENT_NAME);
			if(retval instanceof Double)
			{
				remainingExposureLengthObject = (Double)retval;
				remainingExposureLength = remainingExposureLengthObject.doubleValue();
				logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
					   ":getRemainingExposureLength:Remaining exposure length:"+
					   remainingExposureLength);
			}
			else
			{
				logger.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
					   ":getRemainingExposureLength:Returned exposure length was not a double.");
				remainingExposureLength = -1.0;
			}
		}
		catch(Exception e)
		{
			logger.log(Logging.VERBOSITY_VERBOSE,this.getClass().getName()+
				   ":getRemainingExposureLength:Failed to retrieve remaining exposure length.");
			remainingExposureLength = -1.0;
		}
		return remainingExposureLength;
	}

	/**
	 * Set the filename to save an exposure into.
	 * @param filename A string representing the FITS filename to save the acquired data into.
	 *        This should be the leaf filename only, the directory should be set using setDataDirectory first.
	 * @see #setPropertyValue
	 * @see #CAMERA_DEVICE_NAME
	 * @see #UPLOAD_SETTINGS_PROPERTY_NAME
	 * @see #UPLOAD_PREFIX_ELEMENT_NAME
	 */
	protected void setFilename(String filename) throws Exception
	{
		setPropertyValue(CAMERA_DEVICE_NAME,UPLOAD_SETTINGS_PROPERTY_NAME,UPLOAD_PREFIX_ELEMENT_NAME,filename);
	}
}
