// LOTUSREBOOTQuitThread.java
// $HeadURL$
package ngat.lotus;

import java.lang.*;
import java.io.*;

/**
 * This class is a thread that is started when the LOTUS is to terminate.
 * A thread is passed in, which must terminate before System.exit is called.
 * This is used in, for instance, the REBOOTImplementation, so that the 
 * REBOOT's DONE mesage is returned to the client before LOTUS is terminated.
 * @author Chris Mottram
 * @version $Revision: 28 $
 */
public class LOTUSREBOOTQuitThread extends Thread
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");
	/**
	 * The Thread, that has to terminatre before this thread calls System.exit
	 */
	private Thread waitThread = null;
	/**
	 * Field holding the instance of the LOTUS currently executing, used to access error handling routines etc.
	 */
	private LOTUS lotus = null;
	/**
	 * The exit value to exit the JVM with. Normally (and by default) 0.
	 */
	private int exitValue = 0;

	/**
	 * The constructor.
	 * @param name The name of the thread.
	 */
	public LOTUSREBOOTQuitThread(String name)
	{
		super(name);
	}

	/**
	 * Routine to set this objects pointer to the LOTUS object.
	 * @param o The LOTUS object.
	 */
	public void setLOTUS(LOTUS o)
	{
		this.lotus = o;
	}

	/**
	 * Method to set a thread, such that this thread will not call System.exit until
	 * that thread has terminated.
	 * @param t The thread to wait for.
	 * @see #waitThread
	 */
	public void setWaitThread(Thread t)
	{
		waitThread = t;
	}

	/**
	 * Method to set the exit value to pass into System.exit.
	 * @param i The exit value to pass into System.exit
	 * @see #exitValue
	 */
	public void setExitValue(int i)
	{
		exitValue = i;
	}

	/**
	 * Run method, called when the thread is started.
	 * If the waitThread is non-null, we try to wait until it has terminated.
	 * System.exit(exitValue) is then called.
	 * @see #waitThread
	 * @see #exitValue
	 */
	public void run()
	{
		if(waitThread != null)
		{
			try
			{
				waitThread.join();
			}
			catch (InterruptedException e)
			{
				lotus.error(this.getClass().getName()+":run:",e);
			}
		}
		System.exit(exitValue);
	}
}
