// REBOOTImplementation.java
// $HeadURL$
package ngat.lotus;

import java.io.*;
import java.lang.*;
import ngat.lotus.ccd.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.REBOOT;
import ngat.util.ICSDRebootCommand;
import ngat.util.ICSDShutdownCommand;
import ngat.util.logging.*;

/**
 * This class provides the implementation for the REBOOT command sent to a server using the
 * Java Message System.
 * @author Chris Mottram
 * @version $Revision: 28 $
 */
public class REBOOTImplementation extends INTERRUPTImplementation implements JMSCommandImplementation
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");
	/**
	 * Class constant used in calculating acknowledge times, when the acknowledge time connot be found in the
	 * configuration file.
	 */
	public final static int DEFAULT_ACKNOWLEDGE_TIME = 		300000;
	/**
	 * String representing the root part of the property key used to get the acknowledge time for 
	 * a certain level of reboot.
	 */
	public final static String ACK_TIME_PROPERTY_KEY_ROOT =	    "lotus.reboot.acknowledge_time.";
	/**
	 * String representing the root part of the property key used to decide whether a certain level of reboot
	 * is enabled.
	 */
	public final static String ENABLE_PROPERTY_KEY_ROOT =       "lotus.reboot.enable.";
	/**
	 * Set of constant strings representing levels of reboot. The levels currently start at 1, so index
	 * 0 is currently "NONE". These strings need to be kept in line with level constants defined in
	 * ngat.message.ISS_INST.REBOOT.
	 */
	public final static String REBOOT_LEVEL_LIST[] =  {"NONE","REDATUM","SOFTWARE","HARDWARE","POWER_OFF"};

	/**
	 * Constructor.
	 */
	public REBOOTImplementation()
	{
		super();
	}

	/**
	 * This method allows us to determine which class of command this implementation class implements.
	 * This method returns &quot;ngat.message.ISS_INST.REBOOT&quot;.
	 * @return A string, the classname of the class of ngat.message command this class implements.
	 */
	public static String getImplementString()
	{
		return "ngat.message.ISS_INST.REBOOT";
	}

	/**
	 * This method gets the REBOOT command's acknowledge time. This time is dependant on the level.
	 * This is calculated as follows:
	 * <ul>
	 * <li>If the level is LEVEL_REDATUM, the number stored in &quot; 
	 * lotus.reboot.acknowledge_time.REDATUM &quot; in the LOTUS properties file is the timeToComplete.
	 * <li>If the level is LEVEL_SOFTWARE, the number stored in &quot; 
	 * lotus.reboot.acknowledge_time.SOFTWARE &quot; in the LOTUS properties file is the timeToComplete.
	 * <li>If the level is LEVEL_HARDWARE, the number stored in &quot; 
	 * lotus.reboot.acknowledge_time.HARDWARE &quot; in the LOTUS properties file is the timeToComplete.
	 * <li>If the level is LEVEL_POWER_OFF, the number stored in &quot; 
	 * lotus.reboot.acknowledge_time.POWER_OFF &quot; in the LOTUS properties file is the timeToComplete.
	 * </ul>
	 * If these numbers cannot be found, the default number DEFAULT_ACKNOWLEDGE_TIME is used instead.
	 * <br>Note, this return value is irrelevant in the SOFTWARE,HARDWARE and POWER_OFF cases, 
	 * the client does not expect a DONE message back from
	 * the process as LOTUS should restart in the implementation of this command.
	 * However, the value returned here will be how long the client waits before trying to restart communications
	 * with the LOTUS server, so a reasonable value here may be useful.
	 * @param command The command instance we are implementing.
	 * @return An instance of ACK with the timeToComplete set to a time (in milliseconds).
	 * @see #DEFAULT_ACKNOWLEDGE_TIME
	 * @see #ACK_TIME_PROPERTY_KEY_ROOT
	 * @see #REBOOT_LEVEL_LIST
	 * @see ngat.message.base.ACK#setTimeToComplete
	 * @see LOTUSStatus#getPropertyInteger
	 */
	public ACK calculateAcknowledgeTime(COMMAND command)
	{
		ngat.message.ISS_INST.REBOOT rebootCommand = (ngat.message.ISS_INST.REBOOT)command;
		ACK acknowledge = null;
		int timeToComplete = 0;

		acknowledge = new ACK(command.getId()); 
		try
		{
			timeToComplete = lotus.getStatus().getPropertyInteger(ACK_TIME_PROPERTY_KEY_ROOT+
								   REBOOT_LEVEL_LIST[rebootCommand.getLevel()]);
		}
		catch(Exception e)
		{
			lotus.error(this.getClass().getName()+":calculateAcknowledgeTime:"+
				  rebootCommand.getLevel(),e);
			timeToComplete = DEFAULT_ACKNOWLEDGE_TIME;
		}
	//set time and return
		acknowledge.setTimeToComplete(timeToComplete);
		return acknowledge;
	}

	/**
	 * This method implements the REBOOT command. 
	 * An object of class REBOOT_DONE is returned.
	 * The <i>lotus.reboot.enable.&lt;level&gt;</i> property is checked to see to whether to really
	 * do the specified level of reboot. Thsi enables us to say, disbale to POWER_OFF reboot, if the
	 * instrument control computer is not connected to an addressable power supply.
	 * The following four levels of reboot are recognised:
	 * <ul>
	 * <li>REDATUM. This powers down the ASIC, and shuts down the connection to the controller, and then
	 * 	restarts it.
	 * <li>SOFTWARE. This powers down the ASIC, and shuts down the connection to the IDL Socket server 
	 *      and closes the server socket using the LOTUS close method. It then exits LOTUS.
	 * <li>HARDWARE. This powers down the ASIC, and shuts down the connection to the IDL Socket server 
	 *      using the LOTUS shutdownController method. It then issues a reboot
	 * 	command to the underlying operating system, to restart the instrument computer.
	 * <li>POWER_OFF. This powers down the ASIC, and shuts down the connection to the IDL Socket server 
	 *      using the LOTUS shutdownController method. It then issues a shutdown
	 * 	command to the underlying operating system, to put the instrument computer into a state
	 * 	where power can be switched off.
	 * </ul>
	 * Note: You need to perform at least a SOFTWARE level reboot to re-read the LOTUS configuration file,
	 * as it contains information such as server ports.
	 * @param command The command instance we are implementing.
	 * @return An instance of REBOOT_DONE. Note this is only returned on a REDATUM level reboot,
	 * all other levels cause the LOTUS to terminate (either directly or indirectly) and a DONE
	 * message cannot be returned.
	 * @see ngat.message.ISS_INST.REBOOT#LEVEL_REDATUM
	 * @see ngat.message.ISS_INST.REBOOT#LEVEL_SOFTWARE
	 * @see ngat.message.ISS_INST.REBOOT#LEVEL_HARDWARE
	 * @see ngat.message.ISS_INST.REBOOT#LEVEL_POWER_OFF
	 * @see #ENABLE_PROPERTY_KEY_ROOT
	 * @see #REBOOT_LEVEL_LIST
	 * @see LOTUS#close
	 * @see LOTUS#shutdownController
	 * @see LOTUS#startupController
	 */
	public COMMAND_DONE processCommand(COMMAND command)
	{
		ngat.message.ISS_INST.REBOOT rebootCommand = (ngat.message.ISS_INST.REBOOT)command;
		ngat.message.ISS_INST.REBOOT_DONE rebootDone = new ngat.message.ISS_INST.REBOOT_DONE(command.getId());
		ngat.message.INST_DP.REBOOT dprtReboot = new ngat.message.INST_DP.REBOOT(command.getId());
		ICSDRebootCommand icsdRebootCommand = null;
		ICSDShutdownCommand icsdShutdownCommand = null;
		LOTUSREBOOTQuitThread quitThread = null;
		boolean enable;

		try
		{
			// is reboot enabled at this level
			enable = lotus.getStatus().getPropertyBoolean(ENABLE_PROPERTY_KEY_ROOT+
							   REBOOT_LEVEL_LIST[rebootCommand.getLevel()]);
			// if not enabled return OK
			if(enable == false)
			{
				lotus.log(Logging.VERBOSITY_VERY_TERSE,"Command:"+
					   rebootCommand.getClass().getName()+":Level:"+rebootCommand.getLevel()+
					   " is not enabled.");
				rebootDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_NO_ERROR);
				rebootDone.setErrorString("");
				rebootDone.setSuccessful(true);
				return rebootDone;
			}
			// do relevent reboot based on level
			switch(rebootCommand.getLevel())
			{
				case REBOOT.LEVEL_REDATUM:
					lotus.shutdownController();
					lotus.reInit();
					lotus.startupController();
					break;
				case REBOOT.LEVEL_SOFTWARE:
					lotus.close();
					quitThread = new LOTUSREBOOTQuitThread("quit:"+rebootCommand.getId());
					quitThread.setLOTUS(lotus);
					quitThread.setWaitThread(serverConnectionThread);
					quitThread.start();
					break;
				case REBOOT.LEVEL_HARDWARE:
					// Set temperature to ambient, and close connection to INDI server
					lotus.shutdownController();
				// send reboot to the icsd_inet
					icsdRebootCommand = new ICSDRebootCommand();
					icsdRebootCommand.send();
					break;
				case REBOOT.LEVEL_POWER_OFF:
					// Set temperature to ambient, and close connection to INDI server
					lotus.shutdownController();
				// send shutdown to the icsd_inet
					icsdShutdownCommand = new ICSDShutdownCommand();
					icsdShutdownCommand.send();
					break;
				default:
					lotus.error(this.getClass().getName()+
						":processCommand:"+command+":Illegal level:"+rebootCommand.getLevel());
					rebootDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+1400);
					rebootDone.setErrorString("Illegal level:"+rebootCommand.getLevel());
					rebootDone.setSuccessful(false);
					return rebootDone;
			};// end switch
		}
		catch(IOException e)
		{
			lotus.error(this.getClass().getName()+
					":processCommand:"+command+":",e);
			rebootDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+1402);
			rebootDone.setErrorString(e.toString());
			rebootDone.setSuccessful(false);
			return rebootDone;
		}
		catch(InterruptedException e)
		{
			lotus.error(this.getClass().getName()+
				":processCommand:"+command+":",e);
			rebootDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+1403);
			rebootDone.setErrorString(e.toString());
			rebootDone.setSuccessful(false);
			return rebootDone;
		}
		catch(Exception e)
		{
			lotus.error(this.getClass().getName()+
					":processCommand:"+command+":",e);
			rebootDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_BASE+1404);
			rebootDone.setErrorString(e.toString());
			rebootDone.setSuccessful(false);
			return rebootDone;
		}
	// return done object.
		rebootDone.setErrorNum(LOTUSConstants.LOTUS_ERROR_CODE_NO_ERROR);
		rebootDone.setErrorString("");
		rebootDone.setSuccessful(true);
		return rebootDone;
	}
}
