// LOTUSTCPServer.java
// $HeadURL$
package ngat.lotus;

import java.lang.*;
import java.io.*;
import java.net.*;

import ngat.net.*;

/**
 * This class extends the TCPServer class for the LOTUS application.
 * @author Chris Mottram
 * @version $Revision: 28 $
 */
public class LOTUSTCPServer extends TCPServer
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");
	/**
	 * Field holding the instance of LOTUS currently executing, so we can pass this to spawned threads.
	 */
	private LOTUS lotus = null;

	/**
	 * The constructor.
	 */
	public LOTUSTCPServer(String name,int portNumber)
	{
		super(name,portNumber);
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
	 * This routine spawns threads to handle connection to the server. This routine
	 * spawns LOTUSTCPServerConnectionThread threads.
	 * The routine also sets the new threads priority to higher than normal. This makes the thread
	 * reading it's command a priority so we can quickly determine whether the thread should
	 * continue to execute at a higher priority.
	 * @see LOTUSTCPServerConnectionThread
	 */
	public void startConnectionThread(Socket connectionSocket)
	{
		LOTUSTCPServerConnectionThread thread = null;

		thread = new LOTUSTCPServerConnectionThread(connectionSocket);
		thread.setLOTUS(lotus);
		thread.setPriority(lotus.getStatus().getThreadPriorityInterrupt());
		thread.start();
	}
}
