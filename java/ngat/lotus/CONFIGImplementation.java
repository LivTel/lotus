// CONFIGImplementation.java
// $HeadURL$
package ngat.lotus;

import java.lang.*;
import java.io.*;

import ngat.lotus.ccd.*;
import ngat.message.base.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.phase2.*;
import ngat.net.*;
import ngat.util.logging.*;

/**
 * This class provides the implementation for the CONFIG command sent to a server using the
 * Java Message System. It extends SETUPImplementation.
 * @see SETUPImplementation
 * @author Chris Mottram
 * @version $Revision: 28 $
 */
public class CONFIGImplementation extends SETUPImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");

	/**
	 * Constructor. 
	 */
	public CONFIGImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.CONFIG&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.CONFIG";
	}

	/**
	 * This method gets the CONFIG command's acknowledge time.
	 * This method returns an ACK with timeToComplete set to the &quot; lotus.config.acknowledge_time &quot;
	 * held in the LOTUS configuration file. 
	 * If this cannot be found/is not a valid number the default acknowledge time is used instead.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set to a time (in milliseconds).
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see LOTUSTCPServerConnectionThread#getDefaultAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		ACK acknowledge = null;
		int timeToComplete = 0;

		acknowledge = new ACK(command.getId());
		try
		{
			timeToComplete += lotus.getStatus().getPropertyInteger("lotus.config.acknowledge_time");
		}
		catch(NumberFormatException e)
		{
			lotus.error(this.getClass().getName()+":calculateAcknowledgeTime:"+e);
			timeToComplete += serverConnectionThread.getDefaultAcknowledgeTime();
		}
		acknowledge.setTimeToComplete(timeToComplete);
		return acknowledge;
	}

	/**
	 * This method implements the CONFIG command. 
	 * <ul>
	 * <li>It checks the message contains a suitable LOTUSConfig object to configure the controller.
	 * <li>We set the CCD binning.
	 * <li>If the slit is wide, we offset the telescope from the nominal narrow position to the target
	 *     should be over the wide part of the slit.
	 * <li>We call setFocusOffset to send a focus offset to the ISS.
	 * <li>It increments the unique configuration ID.
	 * <li>We store the config name used, and the slit width, in the status object, for later retrieval to create FITS headers.
	 * </ul>
	 * @see ngat.phase2.LOTUSConfig
	 * @see LOTUSStatus#setCurrentMode
	 * @see LOTUSStatus#setSlitWidth
	 * @see FITSImplementation#setFocusOffset
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#MODE_IDLE
	 * @see ngat.message.ISS_INST.GET_STATUS_DONE#MODE_CONFIGURING
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		CONFIG configCommand = null;
		LOTUSConfig config = null;
		Detector detector = null;
		CONFIG_DONE configDone = null;
		double xArcsecOffset,yArcsecOffset;

	// test contents of command.
		configCommand = (CONFIG)command;
		configDone = new CONFIG_DONE(command.getId());
		if(testAbort(configCommand,configDone) == true)
			return configDone;
		if(configCommand.getConfig() == null)
		{
			lotus.error(this.getClass().getName()+":processCommand:"+command+":Config was null.");
			configDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+800);
			configDone.setErrorString(":Config was null.");
			configDone.setSuccessful(false);
			return configDone;
		}
		if((configCommand.getConfig() instanceof LOTUSConfig) == false)
		{
			lotus.error(this.getClass().getName()+":processCommand:"+command+":Config has wrong class:"+
				    configCommand.getConfig().getClass().getName());
			configDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+801);
			configDone.setErrorString(":Config has wrong class:"+
				configCommand.getConfig().getClass().getName());
			configDone.setSuccessful(false);
			return configDone;
		}
	// get LOTUS Config from configCommand.
		config = (LOTUSConfig)configCommand.getConfig();
	// get local detector copy
		detector = config.getDetector(0);
	// test abort
		if(testAbort(configCommand,configDone) == true)
			return configDone;
		status.setCurrentMode(GET_STATUS_DONE.MODE_CONFIGURING);
		// keep a copy of the binning used to populate the FITS headers
		status.setBinning(detector.getXBin(),detector.getYBin());
		// set binning
		try
		{
			ccd.setBinning(detector.getXBin(),detector.getYBin());
		}
		catch(Exception e)
		{
			lotus.error(this.getClass().getName()+":processCommand:"+command+":Setting binning failed:",e);
			status.setCurrentMode(GET_STATUS_DONE.MODE_IDLE);
			configDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+803);
			configDone.setErrorString("Setting binning failed:"+e.toString());
			configDone.setSuccessful(false);
			return configDone;
		}
		// move telescope is slit is wide.
		try
		{
			if(config.getSlitWidth() == LOTUSConfig.SLIT_WIDTH_WIDE)
			{
				xArcsecOffset = status.getPropertyDouble("lotus.config.slit.wide.offset.x");
				yArcsecOffset = status.getPropertyDouble("lotus.config.slit.wide.offset.y");
				doXYPixelOffset(configCommand.getId(),xArcsecOffset,yArcsecOffset);
			}
		}
		catch(Exception e)
		{
			lotus.error(this.getClass().getName()+":processCommand:"+command+
				    ":Configuring wide slit failed:",e);
			status.setCurrentMode(GET_STATUS_DONE.MODE_IDLE);
			configDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+802);
			configDone.setErrorString("Configuring wide slit failed:"+e.toString());
			configDone.setSuccessful(false);
			return configDone;
		}
	// send focus offset 
		try
		{
			setFocusOffset(configCommand.getId());
		}
		catch(Exception e)
		{
			lotus.error(this.getClass().getName()+":processCommand:"+
				command+":setFocusOffset failed:",e);
			status.setCurrentMode(GET_STATUS_DONE.MODE_IDLE);
			configDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+805);
			configDone.setErrorString("setFocusOffset failed:"+e.toString());
			configDone.setSuccessful(false);
			return configDone;
		}
	// Increment unique config ID.
	// This is queried when saving FITS headers to get the CONFIGID value.
		try
		{
			status.incConfigId();
		}
		catch(Exception e)
		{
			lotus.error(this.getClass().getName()+":processCommand:"+
				    command+":Incrementing configuration ID:"+e.toString());
			status.setCurrentMode(GET_STATUS_DONE.MODE_IDLE);
			configDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+806);
			configDone.setErrorString("Incrementing configuration ID:"+e.toString());
			configDone.setSuccessful(false);
			return configDone;
		}
	// Store name of configuration used in status object.
	// This is queried when saving FITS headers to get the CONFNAME value.
		status.setConfigName(config.getId());
	// store slit width - this is needed for constructing the FITS headers
		status.setSlitWidth(config.getSlitWidth());
		status.setCurrentMode(GET_STATUS_DONE.MODE_IDLE);
	// setup return object.
		configDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_NO_ERROR);
		configDone.setErrorString("");
		configDone.setSuccessful(true);
	// return done object.
		return configDone;
	}

	/**
	 * This method offsets the telescope in X and Y (focal plane geometry) using the 
	 * xArcsecOffset and yArcsecOffset parameters. 
	 * These are sent to the RCS using the OFFSET_X_Y command.
	 * @param id The string id of the command instance we are implementing. Used for generating ISS command id's.
	 * @param xArcsecOffset The offset in X in the focal plane of the acquisition instrument in arcseeconds.
	 * @param yArcsecOffset The offset in Y in the focal plane of the acquisition instrument in arcseeconds.
	 * @exception Exception Thrown if the ISS command OFFSET_RA_DEC fails.
	 * @see #lotus
	 * @see #serverConnectionThread
	 * @see LOTUS#sendISSCommand
	 * @see ngat.message.ISS_INST.OFFSET_X_Y
	 */
	protected void doXYPixelOffset(String id,double xArcsecOffset,double yArcsecOffset) throws Exception
	{
		OFFSET_X_Y offsetXYCommand = null;
		INST_TO_ISS_DONE instToISSDone = null;
		
		// log telescope offset
		lotus.log(Logging.VERBOSITY_VERBOSE,"Attempting telescope position XY offset x(arcsec):"+
			  xArcsecOffset+":y(arcsec):"+yArcsecOffset+".");
		// tell telescope of offset RA and DEC
		offsetXYCommand = new OFFSET_X_Y(id);
		offsetXYCommand.setXOffset((float)xArcsecOffset);
		offsetXYCommand.setYOffset((float)yArcsecOffset);
		offsetXYCommand.setRotation(0.0f);
		instToISSDone = lotus.sendISSCommand(offsetXYCommand,serverConnectionThread);
		if(instToISSDone.getSuccessful() == false)
		{
			throw new Exception(this.getClass().getName()+"ACQUIRE:"+id+"Offset X Y failed:x = "+
					    xArcsecOffset+", y = "+yArcsecOffset+":"+instToISSDone.getErrorString());
		}
	}
}
