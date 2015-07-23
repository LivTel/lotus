// MULTRUNImplementation.java
// $HeadURL$
// $Revision: 62 $
package ngat.lotus;

import java.lang.*;
import java.io.*;

import ngat.fits.*;
import ngat.lotus.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.net.*;
import ngat.util.logging.*;

/**
 * This class provides the implementation for the MULTRUN command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 62 $
 */
public class MULTRUNImplementation extends EXPOSEImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");

	/**
	 * Constructor.
	 */
	public MULTRUNImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.MULTRUN&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.MULTRUN";
	}

	/**
	 * This method returns the MULTRUN command's acknowledge time. Each frame in the MULTRUN takes 
	 * the exposure time plus the status's max readout time plus the default acknowledge time to complete. 
	 * The default acknowledge time
	 * allows time to setup the camera, get information about the telescope and save the frame to disk.
	 * This method returns the time for the first frame in the MULTRUN only, as a MULTRUN_ACK message
	 * is returned to the client for each frame taken.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see #serverConnectionThread
	 * @see #status
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see LOTUSTCPServerConnectionThread#getDefaultAcknowledgeTime
	 * @see MULTRUN#getExposureTime
	 * @see MULTRUN#getNumberExposures
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		MULTRUN multRunCommand = (MULTRUN)command;
		ACK acknowledge = null;

		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(multRunCommand.getExposureTime()+
			serverConnectionThread.getDefaultAcknowledgeTime());
		return acknowledge;
	}

	/**
	 * This method implements the MULTRUN command. 
	 * <ul>
	 * <li>We initialise exposure status variables (<b>setExposureCount</b>/<b>setExposureNumber</b>/
	 *     <b>setExposureLength</b>).
	 * <li>It moves the fold mirror to the correct location (<b>moveFold</b>).
	 * <li>We setup the OBSTYPE and configure the fitsFilename instance based on whether we are doing
	 *     an exposure or standard.
	 * <li>For each exposure we do the following:
	 *	<ul>
	 *      <li>We call <b>clearFitsHeaders</b> to reset the FITS headers information.
	 *      <li>We call <b>setFitsHeaders</b> to set FITS header data based on the current config.
	 *      <li>Calls <b>getFitsHeadersFromISS</b> to get FITS headers (incorporating
	 *              the latest  offset) from the ISS.
	 *      <li>Adds the returned FITS headers to lotusFitsHeader.
	 *      <li>We call fitsFilename to generate a FITS filename to save data into,
	 *          a create a leafFilename fromthis to send to the INDI server.
	 *      <li>We call <b>ccd.expose</b> to do the exposure and save it in the specified filename.
	 *      <li>We call <b>lotusFitsHeader.writeFitsHeader</b> to append the constructed FITS headers to the
	 *          INDI generated FITS file.
	 *      </ul>
	 * <li>We set up the return values to return to the client.
	 * </ul>
	 * The resultant filenames or the relevant error code is put into the an object of class MULTRUN_DONE and
	 * returned. During execution of these operations the abort flag is tested to see if we need to
	 * stop the implementation of this command.
	 * @see #sendACK
	 * @see #testAbort
	 * @see #moveFold
	 * @see #clearFitsHeaders
	 * @see #setFitsHeaders
	 * @see #getFitsHeadersFromISS
	 * @see #lotus
	 * @see #ccd
	 * @see #lotusFitsHeader
	 * @see LOTUS#log
	 * @see LOTUS#error
	 * @see LOTUS#getFitsFilename
	 * @see LOTUSStatus#setCurrentMode
	 * @see LOTUSStatus#setExposureCount
	 * @see LOTUSStatus#setExposureNumber
	 * @see LOTUSStatus#setExposureLength
	 * @see LOTUSStatus#setExposureStartTime
	 * @see ngat.lotus.ccd.StarlightExpressTriusSX35#expose
	 * @see ngat.fits.FitsFilename#nextMultRunNumber
	 * @see ngat.fits.FitsFilename#setExposureCode
	 * @see ngat.fits.FitsFilename#nextRunNumber
	 * @see ngat.fits.FitsFilename#getFilename
	 * @see ngat.fits.FitsHeader#writeFitsHeader
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		MULTRUN multRunCommand = (MULTRUN)command;
		MULTRUN_DP_ACK multRunDpAck = null;
		MULTRUN_DONE multRunDone = new MULTRUN_DONE(command.getId());
		FitsFilename fitsFilename = null;
		File fitsFile = null;
		String obsType = null;
		String filename = null;
		String leafname = null;
		double exposureLengthSeconds;
		int index;
		boolean retval = false;
		boolean fitsFilenameRename;
		boolean done;

		lotus.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+
			":processCommand:Starting MULTRUN with exposure length "+multRunCommand.getExposureTime()+
			" ms and number of exposures "+multRunCommand.getNumberExposures()+".");
		if(testAbort(multRunCommand,multRunDone) == true)
			return multRunDone;
		// send an initiali ACK, actually getDefaultAcknowledgeTime long
		if(sendACK(multRunCommand,multRunDone,0) == false)
			return multRunDone;
	// setup exposure status.
		status.setExposureCount(multRunCommand.getNumberExposures());
		status.setExposureNumber(0);
		status.setExposureLength(multRunCommand.getExposureTime());
		status.setCurrentMode(GET_STATUS_DONE.MODE_IDLE);
	// increment multrun number
		fitsFilename = lotus.getFitsFilename();
		fitsFilename.nextMultRunNumber();
	// move the fold mirror to the correct location
		if(moveFold(multRunCommand,multRunDone) == false)
			return multRunDone;
		if(testAbort(multRunCommand,multRunDone) == true)
			return multRunDone;
		try
		{
			if(multRunCommand.getStandard())
			{
				obsType = FitsHeaderDefaults.OBSTYPE_VALUE_STANDARD;
				fitsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_STANDARD);
			}
			else
			{
				obsType = FitsHeaderDefaults.OBSTYPE_VALUE_EXPOSURE;
				fitsFilename.setExposureCode(FitsFilename.EXPOSURE_CODE_EXPOSURE);
			}
		}
		catch(Exception e)
		{
			lotus.error(this.getClass().getName()+
				  ":processCommand:Failed to set Exposure Code:",e);
			multRunDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+1203);
			multRunDone.setErrorString("processCommand:Failed to set Exposure Code:"+e);
			multRunDone.setSuccessful(false);
			return multRunDone;
		}
		// configure the array 
		exposureLengthSeconds = ((double)(multRunCommand.getExposureTime())/1000.0);
		// send an ACK, actually getDefaultAcknowledgeTime long
		if(sendACK(multRunCommand,multRunDone,0) == false)
			return multRunDone;
		if(testAbort(multRunCommand,multRunDone) == true)
			return multRunDone;
		// do exposures
		index = 0;
		retval = true;
		while(retval&&(index < multRunCommand.getNumberExposures()))
		{
			// send an ACK, actually at least one exposure length +readout long
			// diddly 4000
			if(sendACK(multRunCommand,multRunDone,
				   multRunCommand.getExposureTime()+4000) == false)
			{
				lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
					":processCommand:sendACK failed for index "+index+".");
				return multRunDone;
			}
			if(testAbort(multRunCommand,multRunDone) == true)
			{
				status.setCurrentMode(GET_STATUS_DONE.MODE_IDLE);
				return multRunDone;
			}
			lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
				":processCommand:Starting exposure "+index+" of length "+exposureLengthSeconds+"s.");
			lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
				":processCommand:Clear FITS headers.");
			clearFitsHeaders();
			// set FITS header
			if(setFitsHeaders(multRunCommand,multRunDone,obsType,multRunCommand.getExposureTime(),
					  multRunCommand.getNumberExposures())== false)
			{
				lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
					":processCommand:setFitsHeaders failed for index "+index+".");
				return multRunDone;
			}
			// get FITS headers from ISS
			if(getFitsHeadersFromISS(multRunCommand,multRunDone)== false)
			{
				lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
					":processCommand:getFitsHeadersFromISS failed for index "+index+".");
				return multRunDone;
			}
			try
			{
				// increment run number in Multrun
				fitsFilename.nextRunNumber();
				// get filenane
				filename = fitsFilename.getFilename();
				// get leaf name of fits filename
				fitsFile = new File(filename);
				leafname = fitsFile.getName();
				// now remove '.fits' as INDI driver does not want this in the filename leafname
				leafname = leafname.substring(0,leafname.lastIndexOf('.'));
			}
			catch(Exception e)
			{
				lotus.error(this.getClass().getName()+
					  ":processCommand:Processing FITS filename failed:",e);
				multRunDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+1204);
				multRunDone.setErrorString("processCommand:Processing FITS filename failed:"+e);
				multRunDone.setSuccessful(false);
				return multRunDone;
			}
			if(testAbort(multRunCommand,multRunDone) == true)
			{
				status.setCurrentMode(GET_STATUS_DONE.MODE_IDLE);
				return multRunDone;
			}
			// take exposure
			lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
				  ":processCommand:Exposing CCD for "+exposureLengthSeconds+" seconds.");
			status.setExposureStartTime(System.currentTimeMillis());
			try
			{
				status.setCurrentMode(GET_STATUS_DONE.MODE_EXPOSING);
				ccd.expose(exposureLengthSeconds,leafname);
				status.setCurrentMode(GET_STATUS_DONE.MODE_READING_OUT);
			}
			catch(Exception e)
			{
				status.setCurrentMode(GET_STATUS_DONE.MODE_IDLE);
				lotus.error(this.getClass().getName()+
					  ":processCommand:Taking exposure failed:",e);
				multRunDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+1201);
				multRunDone.setErrorString("processCommand:Taking exposure failed:"+e);
				multRunDone.setSuccessful(false);
				return multRunDone;
			}
			if(testAbort(multRunCommand,multRunDone) == true)
			{
				status.setCurrentMode(GET_STATUS_DONE.MODE_IDLE);
				return multRunDone;
			}
			// update FITS headers needing timestamp information
			if(setFitsHeaderTimestamps(multRunCommand,multRunDone)== false)
			{
				lotus.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
					":processCommand:setFitsHeaderTimestamps failed for index "+index+".");
				return multRunDone;
			}
			// append FITS headers to created FITS image
			try
			{
				lotusFitsHeader.writeFitsHeader(filename);
			}
			catch(Exception e)
			{
				status.setCurrentMode(GET_STATUS_DONE.MODE_IDLE);
				lotus.error(this.getClass().getName()+
					  ":processCommand:Adding FITS headers to "+filename+" failed:",e);
				multRunDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+1205);
				multRunDone.setErrorString("processCommand:Adding FITS headers to "+filename
							   +" failed:"+e);
				multRunDone.setSuccessful(false);
				return multRunDone;
			}
			// flip images?
			// increment exposure number
			status.setExposureNumber(index+1);
			// test whether an abort has occured.
			if(testAbort(multRunCommand,multRunDone) == true)
			{
				retval = false;
			}
			index++;
		}// end while
		status.setCurrentMode(GET_STATUS_DONE.MODE_IDLE);
		// no pipeline processing, set return value to something bland.
		// set filename to last filename exposed.
		multRunDone.setFilename(filename);
		multRunDone.setCounts(0.0f);
		multRunDone.setSeeing(0.0f);
		multRunDone.setXpix(0.0f);
		multRunDone.setYpix(0.0f);
		multRunDone.setPhotometricity(0.0f);
		multRunDone.setSkyBrightness(0.0f);
		multRunDone.setSaturation(false);
	// if a failure occurs, return now
		if(!retval)
			return multRunDone;
	// setup return values.
	// setCounts,setFilename,setSeeing,setXpix,setYpix 
	// setPhotometricity, setSkyBrightness, setSaturation set by reduceExpose for last image reduced.
		multRunDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_NO_ERROR);
		multRunDone.setErrorString("");
		multRunDone.setSuccessful(true);
	// return done object.
		lotus.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+
			":processCommand:MULTRUN command completed.");
		return multRunDone;
	}

	/**
	 * Method to send an ACK containing the filename (could actually be a directory) just taken back to the
	 * client, and to ensure the client connection is kept open.
	 * @param multRunCommand The MULTRUN command we are implementing.
	 * @param multRunDone The MULTRUN_DONE command object that will be returned to the client. We set
	 *       a sensible error message in this object if this method fails.
	 * @return We return true if the method succeeds, and false if an error occurs.
	 * @see #lotus
	 * @see #serverConnectionThread
	 * @see ngat.lotus.LOTUS#log
	 * @see ngat.lotus.LOTUS#error
	 * @see ngat.message.ISS_INST.MULTRUN_ACK
	 */
	protected boolean sendMultrunACK(MULTRUN multRunCommand,MULTRUN_DONE multRunDone,String filename)
	{
		MULTRUN_ACK multRunAck = null;

		// send acknowledge to say frames are completed.
		// diddly 4000
		lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			":sendMultrunACK:Sending ACK with exposure time "+multRunCommand.getExposureTime()+
			" plus ramp overhead "+4000+
			" plus default ACK time "+serverConnectionThread.getDefaultAcknowledgeTime()+".");
		multRunAck = new MULTRUN_ACK(multRunCommand.getId());
		multRunAck.setTimeToComplete(multRunCommand.getExposureTime()+4000+
					     serverConnectionThread.getDefaultAcknowledgeTime());
		multRunAck.setFilename(filename);
		try
		{
			serverConnectionThread.sendAcknowledge(multRunAck);
		}
		catch(IOException e)
		{
			lotus.error(this.getClass().getName()+":sendMultrunACK:sendAcknowledge:",e);
			multRunDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+1202);
			multRunDone.setErrorString("sendMultrunACK:sendAcknowledge:"+e.toString());
			multRunDone.setSuccessful(false);
			return false;
		}
		return true;
	}
}
