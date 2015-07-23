// FITSImplementation.java
// $HeadURL$
package ngat.lotus;

import java.lang.*;
import java.io.*;
import java.text.*;
import java.util.*;

import ngat.astrometry.NGATAstro;
import ngat.fits.*;
import ngat.lotus.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.net.*;
import ngat.util.*;
import ngat.util.logging.*;

/**
 * This class provides the generic implementation of commands that write FITS files. It extends those that
 * use the hardware libraries as this is needed to generate FITS files.
 * @see HardwareImplementation
 * @author Chris Mottram
 * @version $Revision: 53 $
 */
public class FITSImplementation extends HardwareImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");
	/**
	 * Internal constant used when the order number offset defined in the property
	 * 'lotus.get_fits.order_number_offset' is not found or is not a valid number.
	 * @see #getFitsHeadersFromISS
	 */
	private final static int DEFAULT_ORDER_NUMBER_OFFSET = 255;
	/**
	 * A local reference to the FitsHeader object held in LOTUS. This is used for writing FITS headers to disk
	 * and setting the values of card images within the headers.
	 */
	protected FitsHeader lotusFitsHeader = null;
	/**
	 * A local reference to the FitsHeaderDefaults object held in LOTUS. 
	 * This is used to supply default values, 
	 * units and comments for FITS header card images.
	 */
	protected FitsHeaderDefaults lotusFitsHeaderDefaults = null;

	/**
	 * This method calls the super-classes method, and tries to fill in the reference to the
	 * FITS filename object, the FITS header object and the FITS default value object.
	 * @param command The command to be implemented.
	 * @see #lotus
	 * @see #lotusFitsHeader
	 * @see LOTUS#getFitsHeader
	 * @see #lotusFitsHeaderDefaults
	 * @see LOTUS#getFitsHeaderDefaults
	 */
	public void init(COMMAND command)
	{
		super.init(command);
		if(lotus != null)
		{
			lotusFitsHeader = lotus.getFitsHeader();
			lotusFitsHeaderDefaults = lotus.getFitsHeaderDefaults();
		}
	}

	/**
	 * This method is used to calculate how long an implementation of a command is going to take, so that the
	 * client has an idea of how long to wait before it can assume the server has died.
	 * @param command The command to be implemented.
	 * @return The time taken to implement this command, or the time taken before the next acknowledgement
	 * is to be sent.
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		return super.calculateAcknowledgeTime(command);
	}

	/**
	 * This routine performs the generic command implementation.
	 * @param command The command to be implemented.
	 * @return The results of the implementation of this command.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		return super.processCommand(command);
	}

	/**
	 * This routine clears the current set of FITS headers. The FITS headers are held in the main LOTUS
	 * object. This is retrieved and the relevant method called.
	 * @see #lotusFitsHeader
	 * @see ngat.fits.FitsHeader#clearKeywordValueList
	 */
	public void clearFitsHeaders()
	{
		lotusFitsHeader.clearKeywordValueList();
	}

	/**
	 * This routine sets up the Fits Header objects with some keyword value pairs.
	 * It calls the more complicated method below, assuming exposureCount is 1.
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @param obsTypeString The type of image taken by the camera. This string should be
	 * 	one of the OBSTYPE_VALUE_* defaults in ngat.fits.FitsHeaderDefaults.
	 * @param exposureTime The exposure time,in milliseconds, to put in the EXPTIME keyword. It
	 * 	is converted into decimal seconds (a double).
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see #setFitsHeaders(COMMAND,COMMAND_DONE,String,int,int)
	 */
	public boolean setFitsHeaders(COMMAND command,COMMAND_DONE done,String obsTypeString,int exposureTime)
	{
		return setFitsHeaders(command,done,obsTypeString,exposureTime,1);
	}

	/**
	 * This routine sets up the Fits Header objects with some keyword value pairs.
	 * <p>The following mandatory keywords are assumed to exist in the INDI server generated data: 
	 * SIMPLE,BITPIX,NAXIS,NAXIS1,NAXIS2. </p>
	 * <p> A complete list of keywords is constructed from the LOTUS FITS defaults file. Some of the values of
	 * these keywords are overwritten by real data obtained from the camera controller, 
	 * or internal LOTUS status.
	 * These are:
	 * OBSTYPE, RUNNUM, EXPNUM, EXPTOTAL, DATE, DATE-OBS, UTSTART, MJD, EXPTIME, 
	 * FILTER1, FILTERI1, FILTER2, FILTERI2, CONFIGID, CONFNAME, 
	 * PRESCAN, POSTSCAN, GAIN, READNOIS, EPERDN, CCDXBIN, CCDYBIN, CCDXIMSI, CCDYIMSI, CCDSCALE, CCDRDOUT,
	 * CCDSTEMP, CCDATEMP, CCDWMODE, CALBEFOR, CALAFTER, INSTDFOC, FILTDFOC, MYDFOCUS.
	 * Windowing keywords CCDWXOFF, CCDWYOFF, CCDWXSIZ, CCDWYSIZ are not implemented at the moment.
	 * Note the DATE, DATE-OBS, UTSTART and MJD keywords are given the value of the current
	 * system time, this value is updated to the exposure start time when the image has been exposed. </p>
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @param obsTypeString The type of image taken by the camera. This string should be
	 * 	one of the OBSTYPE_VALUE_* defaults in ngat.fits.FitsHeaderDefaults.
	 * @param exposureTime The exposure time,in milliseconds, to put in the EXPTIME keyword. It
	 * 	is converted into decimal seconds (a double).
	 * @param exposureCount The number of exposures to put in the EXPTOTAL keyword.
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see #status
	 * @see #lotusFitsHeader
	 * @see #lotusFitsHeaderDefaults
	 * @see LOTUSStatus#getPropertyBoolean
	 * @see LOTUSStatus#getPropertyDouble
	 * @see LOTUSStatus#getBinX
	 * @see LOTUSStatus#getBinY
	 * @see LOTUSStatus#getExposureStartTime
	 * @see ngat.fits.FitsHeaderDefaults#getCardImageList
	 */
	public boolean setFitsHeaders(COMMAND command,COMMAND_DONE done,String obsTypeString,
				      int exposureTime,int exposureCount)
	{
		double actualTemperature = 0.0;
		FitsHeaderCardImage cardImage = null;
		Date date = null;
		String filterWheelString = null;
		String filterWheelIdString = null;
		Vector defaultFitsHeaderList = null;
		int iValue,xBin,yBin,windowFlags,preScan, postScan;
		double doubleValue = 0.0;
		double instDFoc,myDFoc;
		boolean filterWheelEnable,tempControlEnable;
		char tempInput;

		lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+":setFitsHeaders:Started.");
		// filter wheel and dfocus data
		try
		{
			// instrument defocus
			instDFoc = status.getPropertyDouble("lotus.focus.offset");
			// defocus settings
			myDFoc = instDFoc;
		}
		catch(Exception e)
		{
			String s = new String("Command "+command.getClass().getName()+
				":Setting Fits Headers failed:");
			lotus.error(s,e);
			done.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+309);
			done.setErrorString(s+e);
			done.setSuccessful(false);
			return false;
		}
		try
		{
			lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
				  ":setFitsHeaders:Loading defaults.");
		// load all the FITS header defaults and put them into the lotusFitsHeader object
			defaultFitsHeaderList = lotusFitsHeaderDefaults.getCardImageList();
			lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				  ":setFitsHeaders:Adding "+defaultFitsHeaderList.size()+" defaults to list.");
			lotusFitsHeader.addKeywordValueList(defaultFitsHeaderList,0);
		// NAXIS1
			//cardImage = lotusFitsHeader.get("NAXIS1");
			//cardImage.setValue(new Integer(ccd.getBinnedNCols()));
		// NAXIS2
			//cardImage = lotusFitsHeader.get("NAXIS2");
			//cardImage.setValue(new Integer(ccd.getBinnedNRows()));
		// OBSTYPE
			cardImage = lotusFitsHeader.get("OBSTYPE");
			cardImage.setValue(obsTypeString);
		// The current MULTRUN number and runNumber are used for these keywords at the moment.
		// They are updated in saveFitsHeaders, when the retrieved values are more likely 
		// to be correct.
			// diddly not sure how to calculate these now
		// RUNNUM
			//cardImage = lotusFitsHeader.get("RUNNUM");
			//cardImage.setValue(new Integer(oFilename.getMultRunNumber()));
		// EXPNUM
			//cardImage = lotusFitsHeader.get("EXPNUM");
			//cardImage.setValue(new Integer(oFilename.getRunNumber()));
		// EXPTOTAL
			lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				  ":setFitsHeaders:EXPTOTAL = "+exposureCount+".");
			cardImage = lotusFitsHeader.get("EXPTOTAL");
			cardImage.setValue(new Integer(exposureCount));
		// EXPTIME
			lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				":setFitsHeaders:EXPTIME = "+(((double)exposureTime)/1000.0)+".");
			cardImage = lotusFitsHeader.get("EXPTIME");
			cardImage.setValue(new Double(((double)exposureTime)/1000.0));
		// CCDXBIN
			cardImage = lotusFitsHeader.get("CCDXBIN");
			cardImage.setValue(new Integer(status.getBinX()));
		// CCDYBIN
			cardImage = lotusFitsHeader.get("CCDYBIN");
			cardImage.setValue(new Integer(status.getBinY()));
		// FILTER1
			// diddly these don't exist at the moment
			//cardImage = lotusFitsHeader.get("FILTER1");
			//cardImage.setValue(filterWheelString);
		// FILTERI1
			//cardImage = lotusFitsHeader.get("FILTERI1");
			//cardImage.setValue(filterWheelIdString);
		// CONFIGID
			lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				  ":setFitsHeaders:CONFIGID = "+status.getConfigId()+".");
			cardImage = lotusFitsHeader.get("CONFIGID");
			cardImage.setValue(new Integer(status.getConfigId()));
		// CONFNAME
			cardImage = lotusFitsHeader.get("CONFNAME");
			cardImage.setValue(status.getConfigName());
		// CCDSTEMP
			doubleValue = status.getPropertyDouble("lotus.indi.temperature.target");
			// convert to kelvin
			doubleValue += LOTUS.CENTIGRADE_TO_KELVIN;
			lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				  ":setFitsHeaders:CCDSTEMP = "+(doubleValue)+".");
			cardImage = lotusFitsHeader.get("CCDSTEMP");
			cardImage.setValue(new Integer((int)(doubleValue)));
			actualTemperature = ccd.getTemperature();
			// convert to kelvin
			actualTemperature += LOTUS.CENTIGRADE_TO_KELVIN;
		// CCDATEMP
			lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				  ":setFitsHeaders:CCDATEMP = "+(actualTemperature)+".");
			cardImage = lotusFitsHeader.get("CCDATEMP");
			cardImage.setValue(new Integer((int)(actualTemperature)));
		// windowing keywords
		// CCDWMODE
			//windowFlags = ccd.getSetupWindowFlags();
			//cardImage = lotusFitsHeader.get("CCDWMODE");
			//cardImage.setValue(new Boolean((boolean)(windowFlags>0)));
		// CALBEFOR
			//lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+":setFitsHeaders:CALBEFOR.");
			//cardImage = lotusFitsHeader.get("CALBEFOR");
			// diddly cardImage.setValue(new Boolean(status.getCachedConfigCalibrateBefore()));
		// CALAFTER
			//cardImage = lotusFitsHeader.get("CALAFTER");
			// diddly cardImage.setValue(new Boolean(status.getCachedConfigCalibrateAfter()));
		// INSTDFOC
			lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				":setFitsHeaders:INSTDFOC = "+instDFoc+".");
			cardImage = lotusFitsHeader.get("INSTDFOC");
			cardImage.setValue(new Double(instDFoc));
		// FILTDFOC
			lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				":setFitsHeaders:FILTDFOC = "+0.0+".");
			cardImage = lotusFitsHeader.get("FILTDFOC");
			cardImage.setValue(new Double(0.0));
		// MYDFOCUS
			lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				":setFitsHeaders:MYDFOCUS = "+myDFoc+".");
			cardImage = lotusFitsHeader.get("MYDFOCUS");
			cardImage.setValue(new Double(myDFoc));
			lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
				  ":setFitsHeaders:Finished.");
		}// end try
		// ngat.fits.FitsHeaderException thrown by lotusFitsHeaderDefaults.getValue
		// ngat.util.FileUtilitiesNativeException thrown by LOTUSStatus.getConfigId
		// NumberFormatException thrown by LOTUSStatus.getFilterWheelName/LOTUSStatus.getConfigId
		// Exception thrown by LOTUSStatus.getConfigId
		catch(Exception e)
		{
			lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
				  ":setFitsHeaders:An error occured whilst setting headers.");
			String s = new String("Command "+command.getClass().getName()+
				":Setting Fits Headers failed:");
			lotus.error(s,e);
			done.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+304);
			done.setErrorString(s+e);
			done.setSuccessful(false);
			return false;
		}
		return true;
	}

	/**
	 * This routine tries to get a set of FITS headers for an exposure, by issuing a GET_FITS command
	 * to the ISS. 
	 * If an error occurs the done objects field's can be set to record the error.
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see #lotusFitsHeader
	 * @see #DEFAULT_ORDER_NUMBER_OFFSET
	 * @see LOTUS#sendISSCommand
	 * @see LOTUS#getStatus
	 * @see LOTUSStatus#getPropertyInteger
	 */
	public boolean getFitsHeadersFromISS(COMMAND command,COMMAND_DONE done)
	{
		INST_TO_ISS_DONE instToISSDone = null;
		ngat.message.ISS_INST.GET_FITS getFits = null;
		ngat.message.ISS_INST.GET_FITS_DONE getFitsDone = null;
		FitsHeaderCardImage cardImage = null;
		Object value = null;
		Vector list = null;
		int orderNumberOffset;

		lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			  ":getFitsHeadersFromISS:Started.");
		getFits = new ngat.message.ISS_INST.GET_FITS(command.getId());
		instToISSDone = lotus.sendISSCommand(getFits,serverConnectionThread);
		if(instToISSDone.getSuccessful() == false)
		{
			lotus.error(this.getClass().getName()+":getFitsHeadersFromISS:"+
				    command.getClass().getName()+":"+instToISSDone.getErrorString());
			done.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+302);
			done.setErrorString(instToISSDone.getErrorString());
			done.setSuccessful(false);
			return false;
		}
	// Get the returned FITS header information into the FitsHeader object.
		getFitsDone = (ngat.message.ISS_INST.GET_FITS_DONE)instToISSDone;
	// extract specific FITS headers 
		list = getFitsDone.getFitsHeader();
		// get an ordernumber offset
		try
		{
			orderNumberOffset = status.getPropertyInteger("lotus.get_fits.iss.order_number_offset");
		}
		catch(NumberFormatException e)
		{
			orderNumberOffset = DEFAULT_ORDER_NUMBER_OFFSET;
			lotus.error(this.getClass().getName()+
				    ":getFitsHeadersFromISS:Getting order number offset failed.",e);
		}
		// Add the list, which is a Vector containing FitsHeaderCardImage objects, 
		// to lotusFitsHeader
		lotusFitsHeader.addKeywordValueList(list,orderNumberOffset);
		lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":getFitsHeadersFromISS:finished.");
		return true;
	}

	/**
	 * Set the timestamp keywords in the FITS headers, to the exposure start time saved in the 
	 * LOTUSStatus object. This must be done after the exposure but before the FITS headers
	 * are appended to the INDI generated ones, so is a separate method to setFitsHeaders.
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @see #lotus
	 * @see LOTUS#log
	 * @see LOTUS#error
	 * @see #status
	 * @see LOTUSStatus#getExposureStartTime
	 * @see
	 */
	public boolean setFitsHeaderTimestamps(COMMAND command,COMMAND_DONE done)
	{
		FitsHeaderCardImage cardImage = null;
		DecimalFormat twoDigits = null;
		DecimalFormat threeDigits = null;
		Date date = null;
		TimeZone timeZone = null;
		Calendar calendar = null;
		String valueString = null;
		double mjd;

		lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			  ":setFitsHeaderTimestamps:Started.");
		try
		{
			// get the saved exposure start time
			date = new Date(status.getExposureStartTime());
			timeZone = TimeZone.getTimeZone("UTC");
			calendar = Calendar.getInstance(timeZone);
			calendar.setTime(date);
			// add three seconds to allow for array clear time.
			// See fault #2312
			// INDI timestamp:
			// DATE-OBS= '2015-07-22T14:52:42' / UTC start date of observation
			// My timestamps:
			// DATE    = '2015-07-22T15:52:39.528' / [UTC] The start date of the observation
			calendar.add(Calendar.SECOND,3);
		// DATE
			twoDigits = new DecimalFormat("00");
			threeDigits = new DecimalFormat("000");
			valueString = new String(calendar.get(Calendar.YEAR)+"-"+
						 twoDigits.format(calendar.get(Calendar.MONTH)+1)+"-"+
						 twoDigits.format(calendar.get(Calendar.DAY_OF_MONTH)));
			cardImage = lotusFitsHeader.get("DATE");
			cardImage.setValue(valueString);
		// DATE-OBS
			// Currently using INDI generated version
			//cardImage = lotusFitsHeader.get("DATE-OBS");
			//cardImage.setValue(date);
		// UTSTART
			valueString = new String(twoDigits.format(calendar.get(Calendar.HOUR_OF_DAY))+":"+
						 twoDigits.format(calendar.get(Calendar.MINUTE))+":"+
						 twoDigits.format(calendar.get(Calendar.SECOND))+"."+
						 threeDigits.format(calendar.get(Calendar.MILLISECOND)));
			cardImage = lotusFitsHeader.get("UTSTART");
			cardImage.setValue(valueString);
		// MJD
			mjd = NGATAstro.getMJD(calendar.getTimeInMillis());
			cardImage = lotusFitsHeader.get("MJD");
			cardImage.setValue(new Double(mjd));
		}// end try
		catch(Exception e)
		{
			lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
				  ":setFitsHeaderTimestamps:An error occured whilst setting headers.");
			String s = new String("Command "+command.getClass().getName()+
				":Setting Fits Header Timestamps failed:");
			lotus.error(s,e);
			done.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+301);
			done.setErrorString(s+e);
			done.setSuccessful(false);
			return false;
		}
		return true;
	}
}
