// EXPOSEImplementation.java
// $HeadURL$
package ngat.lotus;

import java.io.*;

import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.message.INST_DP.*;
import ngat.util.logging.*;

/**
 * This class provides the generic implementation for EXPOSE commands sent to a server using the
 * Java Message System. It extends FITSImplementation, as EXPOSE commands needs access to
 * resources to make FITS files.
 * @see FITSImplementation
 * @author Chris Mottram
 * @version $Revision: 43 $
 */
public class EXPOSEImplementation extends FITSImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");

	/**
	 * This method gets the EXPOSE command's acknowledge time. It returns the server connection 
	 * threads min acknowledge time. This method should be over-written in sub-classes.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set.
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see LOTUSTCPServerConnectionThread#getMinAcknowledgeTime
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		ACK acknowledge = null;

		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(serverConnectionThread.getMinAcknowledgeTime());
		return acknowledge;
	}

	/**
	 * This method is a generic implementation for the EXPOSE command, that does nothing.
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
	       	// do nothing 
		EXPOSE_DONE exposeDone = new EXPOSE_DONE(command.getId());

		exposeDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_NO_ERROR);
		exposeDone.setErrorString("");
		exposeDone.setSuccessful(true);
		return exposeDone;
	}

	/**
	 * Method to send an ACK to the to ensure the client connection is kept open.
	 * @param command The command we are implementing.
	 * @param done The COMMAND_DONE command object that will be returned to the client. We set
	 *       a sensible error message in this object if this method fails.
	 * @param timeToComplete The length of time before the command is due to finish,
	 *      or before the next ACK is to be sent, in milliseconds. The client should hold open the
	 *      socket connection for the command for at least this length of time before giving up.
	 * @return We return true if the method succeeds, and false if an error occurs.
	 * @see #lotus
	 * @see #serverConnectionThread
	 * @see ngat.lotus.LOTUS#log
	 * @see ngat.lotus.LOTUS#error
	 * @see ngat.message.base.ACK
	 */
	protected boolean sendACK(COMMAND command,COMMAND_DONE done,int timeToComplete)
	{
		ACK ack = null;

		// send acknowledge to say frames are completed.
		lotus.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			":sendACK:Sending ACK with timeToComplete "+timeToComplete+" and default ACK time "+
			(long)(timeToComplete+serverConnectionThread.getDefaultAcknowledgeTime()));
		ack = new ACK(command.getId());
		ack.setTimeToComplete(timeToComplete+serverConnectionThread.getDefaultAcknowledgeTime());
		try
		{
			serverConnectionThread.sendAcknowledge(ack);
		}
		catch(IOException e)
		{
			lotus.error(this.getClass().getName()+":sendACK:sendAcknowledge failed:",e);
			done.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+601);
			done.setErrorString("sendACK:sendAcknowledge failed:"+e.toString());
			done.setSuccessful(false);
			return false;
		}
		return true;
	}
}

