// ABORTImplementation.java
// $HeadURL$
package ngat.lotus;

import java.lang.*;
import ngat.lotus.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.net.*;
import ngat.util.*;
import ngat.util.logging.*;

/**
 * This class provides the implementation for the ABORT command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 30 $
 */
public class ABORTImplementation extends INTERRUPTImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");

	/**
	 * Constructor.
	 */
	public ABORTImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.ABORT&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.ABORT";
	}

	/**
	 * This method gets the ABORT command's acknowledge time. This takes the default acknowledge time to implement.
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
	 * This method implements the ABORT command. 
	 * <ul>
	 * <li>It tells the currently executing thread to abort itself.
	 * </ul>
	 * An object of class ABORT_DONE is returned.
	 * @see LOTUSStatus#getCurrentThread
	 * @see LOTUSTCPServerConnectionThread#setAbortProcessCommand
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		ngat.message.INST_DP.ABORT dprtAbort = new ngat.message.INST_DP.ABORT(command.getId());
		ABORT_DONE abortDone = new ABORT_DONE(command.getId());
		LOTUSTCPServerConnectionThread thread = null;
		double remainingExposureLength;

		lotus.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":processCommand:Started.");
	// tell the thread itself to abort at a suitable point
		lotus.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+":processCommand:Tell thread to abort.");
		thread = (LOTUSTCPServerConnectionThread)status.getCurrentThread();
		if(thread != null)
			thread.setAbortProcessCommand();
		// are we currently exposing? If so stop the acquisition
		// note getting the exposure length can fail with no property existing during readout
		lotus.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+
			   ":processCommand:Getting elapsed exposure time.");
		remainingExposureLength = ccd.getRemainingExposureLength();
		if(remainingExposureLength > 0.0)
		{
			try
			{
				ccd.abort();
			}
			catch(Exception e)
			{
				lotus.error(this.getClass().getName()+
					  ":processCommand:Sending Abort command failed:",e);
				abortDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+2402);
				abortDone.setErrorString("processCommand:Sending Abort command failed:"+e);
				abortDone.setSuccessful(false);
				return abortDone;
			}
		}
		else
		{
			lotus.log(Logging.VERBOSITY_TERSE,this.getClass().getName()+
				":processCommand:Exposure NOT in progress:NOT Sending Abort command.");
		}
	// return done object.
		abortDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_NO_ERROR);
		abortDone.setErrorString("");
		abortDone.setSuccessful(true);
		return abortDone;
	}
}
