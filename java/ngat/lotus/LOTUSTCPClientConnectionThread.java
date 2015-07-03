// LOTUSTCPClientConnectionThread.java
// $HeadURL$
package ngat.lotus;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.Date;

import ngat.net.*;
import ngat.message.base.*;

/**
 * The LOTUSTCPClientConnectionThread extends TCPClientConnectionThread. 
 * It implements the generic ISS/DP(RT) instrument command protocol with multiple acknowledgements. 
 * The instrument starts one of these threads each time
 * it wishes to send a message to the ISS/DP(RT).
 * @author Chris Mottram
 * @version $Revision: 61 $
 */
public class LOTUSTCPClientConnectionThread extends TCPClientConnectionThreadMA
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");
	/**
	 * The commandThread was spawned by the LOTUS to deal with a LOTUS command request. 
	 * As part of the running of
	 * the commandThread, this client connection thread was created. We need to know the server thread so
	 * that we can pass back any acknowledge times from the ISS/DpRt back to the LOTUS client (ISS/IcsGUI etc).
	 */
	private LOTUSTCPServerConnectionThread commandThread = null;
	/**
	 * The LOTUS object.
	 */
	private LOTUS lotus = null;

	/**
	 * A constructor for this class. Currently just calls the parent class's constructor.
	 * @param address The internet address to send this command to.
	 * @param portNumber The port number to send this command to.
	 * @param c The command to send to the specified address.
	 * @param ct The LOTUS command thread, the implementation of which spawned this command.
	 */
	public LOTUSTCPClientConnectionThread(InetAddress address,int portNumber,COMMAND c,
					      LOTUSTCPServerConnectionThread ct)
	{
		super(address,portNumber,c);
		commandThread = ct;
		setName(c.getClass().getName());
	}

	/**
	 * Routine to set this objects pointer to the lotus object.
	 * @param o The lotus object.
	 */
	public void setLOTUS(LOTUS o)
	{
		this.lotus = o;
	}

	/**
	 * This routine processes the acknowledge object returned by the server. It
	 * prints out a message, giving the time to completion if the acknowledge was not null.
	 * It sends the acknowledgement to the LOTUS client for this sub-command of the command,
	 * so that the LOTUS's client does not time out if,say, a zero is returned.
	 * @see LOTUSTCPServerConnectionThread#sendAcknowledge
	 * @see #commandThread
	 */
	protected void processAcknowledge()
	{
		if(acknowledge == null)
		{
			lotus.error(this.getClass().getName()+":processAcknowledge:"+
				    command.getClass().getName()+":acknowledge was null.");
			return;
		}
	// send acknowledge to LOTUS client.
		try
		{
			commandThread.sendAcknowledge(acknowledge);
		}
		catch(IOException e)
		{
			lotus.error(this.getClass().getName()+":processAcknowledge:"+
				    command.getClass().getName()+":sending acknowledge to client failed:",e);
		}
	}

	/**
	 * This routine processes the done object returned by the server. 
	 * It prints out the basic return values in done.
	 */
	protected void processDone()
	{
		ACK acknowledge = null;

		if(done == null)
		{
			lotus.error(this.getClass().getName()+":processDone:"+
				  command.getClass().getName()+":done was null.");
			return;
		}
	// construct an acknowledgement to sent to the LOTUS client to tell it how long to keep waiting
	// it currently returns the time the LOTUS origianally asked for to complete this command
	// This is because the LOTUS assumed zero time for all sub-commands.
		acknowledge = new ACK(command.getId());
		acknowledge.setTimeToComplete(commandThread.getAcknowledgeTime());
		try
		{
			commandThread.sendAcknowledge(acknowledge);
		}
		catch(IOException e)
		{
			lotus.error(this.getClass().getName()+":processDone:"+
				    command.getClass().getName()+":sending acknowledge to client failed:",e);
		}
	}
}
