// HardwareImplementation.java
// $HeadURL$
package ngat.lotus;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;

import ngat.fits.*;
import ngat.lotus.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.util.logging.*;

/**
 * This class provides a base implementation for commands sent to a server using the
 * Java Message System that communicate with a piece of hardware.
 * @author Chris Motram
 * @version $Revision: 59 $
 */
public class HardwareImplementation extends CommandImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");
	/**
	 * Object to control the CCD camera, a Starlight Express Trius SX-35.
	 */
	protected StarlightExpressTriusSX35 ccd = null;

	/**
	 * <ul>
	 * <li>This method calls the super-classes method. 
	 * <li>It then retrieves the CCD interface object from the main LOTUS object.
	 * </ul>
	 * @param command The command to be implemented.
	 * @see #lotus
	 * @see #ccd
	 * @see ngat.lotus.LOTUS#getCCD
	 */
	public void init(COMMAND command)
	{
		super.init(command);
		if(lotus != null)
		{
			ccd = lotus.getCCD();
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
	 * This routine tries to move the mirror fold to a certain location, by issuing a MOVE_FOLD command
	 * to the ISS. The position to move the fold to is specified by the LOTUS property file.
	 * If an error occurs the done objects field's are set accordingly.
	 * @param command The command being implemented that made this call to the ISS. This is used
	 * 	for error logging.
	 * @param done A COMMAND_DONE subclass specific to the command being implemented. If an
	 * 	error occurs the relevant fields are filled in with the error.
	 * @return The routine returns a boolean to indicate whether the operation was completed
	 *  	successfully.
	 * @see LOTUSStatus#getPropertyInteger
	 * @see LOTUS#sendISSCommand
	 */
	public boolean moveFold(COMMAND command,COMMAND_DONE done)
	{
		INST_TO_ISS_DONE instToISSDone = null;
		MOVE_FOLD moveFold = null;
		int mirrorFoldPosition = 0;

		moveFold = new MOVE_FOLD(command.getId());
		try
		{
			mirrorFoldPosition = status.getPropertyInteger("lotus.mirror_fold_position");
		}
		catch(NumberFormatException e)
		{
			mirrorFoldPosition = 0;
			lotus.error(this.getClass().getName()+":moveFold:"+command.getClass().getName(),e);
			done.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+500);
			done.setErrorString("moveFold:"+e);
			done.setSuccessful(false);
			return false;
		}
		moveFold.setMirror_position(mirrorFoldPosition);
		instToISSDone = lotus.sendISSCommand(moveFold,serverConnectionThread);
		if(instToISSDone.getSuccessful() == false)
		{
			lotus.error(this.getClass().getName()+":moveFold:"+
				    command.getClass().getName()+":"+instToISSDone.getErrorString());
			done.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+501);
			done.setErrorString(instToISSDone.getErrorString());
			done.setSuccessful(false);		
			return false;
		}
		return true;
	}

	/**
	 * Routine to set the telescope focus offset.The offset sent is based on:
	 * <ul>
	 * <li>The instrument's offset with respect to the telescope's natural offset (in the configuration
	 *     property 'lotus.focus.offset'.
	 * <ul>
	 * This method sends a OFFSET_FOCUS command to the ISS. 
	 * @param id The Id is used as the OFFSET_FOCUS command's id.
	 * @exception Exception Thrown if the return value of the OFFSET_FOCUS ISS command is false.
	 */
	protected void setFocusOffset(String id) throws Exception
	{
		OFFSET_FOCUS offsetFocusCommand = null;
		INST_TO_ISS_DONE instToISSDone = null;
		String instrumentName = null;
		float focusOffset = 0.0f;

		lotus.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+":setFocusOffset:Started.");
		focusOffset = 0.0f;
	// get default focus offset
		focusOffset += status.getPropertyFloat("lotus.focus.offset");
		lotus.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+":setFocusOffset:Master offset is "+
			  focusOffset+".");
	// send the overall focusOffset to the ISS using  OFFSET_FOCUS
		offsetFocusCommand = new OFFSET_FOCUS(id);
		offsetFocusCommand.setFocusOffset(focusOffset);
		lotus.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+":setFocusOffset:Total offset for "+
			  instrumentName+" is "+focusOffset+".");
		instToISSDone = lotus.sendISSCommand(offsetFocusCommand,serverConnectionThread);
		if(instToISSDone.getSuccessful() == false)
		{
			throw new Exception(this.getClass().getName()+":focusOffset failed:"+focusOffset+":"+
					    instToISSDone.getErrorString());
		}
		lotus.log(Logging.VERBOSITY_VERY_TERSE,this.getClass().getName()+":setFocusOffset:Finished.");
	}

	/**
	 * Method to send an instance of ACK back to the client. This stops the client timing out, whilst we
	 * work out what to attempt next.
	 * @param command The instance of COMMAND we are currently running.
	 * @param done The instance of COMMAND_DONE to fill in with errors we receive.
	 * @param timeToComplete The time it will take to complete the next set of operations
	 *	before the next ACK or DONE is sent to the client. The time is in milliseconds. 
	 * 	The server connection thread's default acknowledge time is added to the value before it
	 * 	is sent to the client, to allow for network delay etc.
	 * @return The method returns true if the ACK was sent successfully, false if an error occured.
	 * @see #serverConnectionThread
	 * @see ngat.message.base.ACK
	 * @see LOTUSTCPServerConnectionThread#sendAcknowledge
	 */
	protected boolean sendBasicAck(COMMAND command,COMMAND_DONE done,int timeToComplete)
	{
		ACK acknowledge = null;

		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(timeToComplete+serverConnectionThread.getDefaultAcknowledgeTime());
		try
		{
			serverConnectionThread.sendAcknowledge(acknowledge,true);
		}
		catch(IOException e)
		{
			String errorString = new String(command.getId()+":sendBasicAck:Sending ACK failed:");
			lotus.error(this.getClass().getName()+":"+errorString,e);
			done.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+502);
			done.setErrorString(errorString+e);
			done.setSuccessful(false);
			return false;
		}
		return true;
	}
}

