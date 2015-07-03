// LOTUSINDIConnection.java
// $HeadURL$
package ngat.lotus.ccd;

import java.io.*;
import java.lang.*;

import org.indilib.i4j.client.INDIServerConnection;
import org.indilib.i4j.client.INDIProperty;
import org.indilib.i4j.client.INDIElement;
import org.indilib.i4j.protocol.INDIProtocol;
import org.indilib.i4j.protocol.url.INDIURLStreamHandlerFactory;

import ngat.util.*;
import ngat.util.logging.*;

/**
 * This class encapsulates an INDIServerConnection, which holds connection information to the INDI server
 * allowing access to the CCD (a Starlight Express Trius SX35).
 * @author Chris Mottram
 * @version $Revision: 57 $
 */
public class LOTUSINDIConnection
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");

	static 
	{
		INDIURLStreamHandlerFactory.init();
	}

	/**
	 * The connection to a INDI server.
	 */
	protected INDIServerConnection connection = null;
	/**
	 * Logger to use.
	 */
	Logger logger = null;

	/**
	 * Constructor.
	 * @param host The hostname string of the machine where the INDI server is running on.
	 * @param portNumber The port number the INDI server is listening on.
	 * @see #logger
	 * @see #connection
	 */
	public LOTUSINDIConnection(String host, int portNumber)
	{
		super();
		logger = LogManager.getLogger(this);
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":Constructing INDIServerConnection on host:"+host+" port:"+portNumber);
		connection = new INDIServerConnection(host, portNumber);
	}

	/**
	 * Open a connection to the INDI server.
	 * @exception IOException Thrown if the connection fails.
	 * @see #connection
	 */
	public void connect() throws IOException
	{
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":Connecting to INDI server and asking for ALL devices.");
		connection.connect();
		connection.askForDevices(); 
	}

	/**
	 * Open a connection to the INDI server.
	 * @param deviceName The name of the device (controlled by the server) that we want to communicate with.
	 * @exception IOException Thrown if the connection fails.
	 * @see #connection
	 */
	public void connect(String deviceName) throws IOException
	{
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":Connecting to INDI server and asking for device:"+deviceName);
		connection.connect();
		connection.askForDevices(deviceName); 
	}

	/**
	 * Disconnect from the INDI server.
	 * @exception Exception Not thrown here, but could be thrown by subclasses.
	 * @see #connection
	 */
	public void disconnect() throws Exception
	{
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+
			   ":Disconnecting from INDI server.");
		connection.disconnect();
	}

	/**
	 * A convenience method to get the Property of a Device by specifiying their names.
	 * This is a pass-through method for INDIServerConnection.getProperty.
	 * @param deviceName the name of the device containing the required property.
	 * @param propertyName  The name of the Property containing the required Element.
	 * @return The Property with propertyName as name of the device with deviceName as name.
	 */
	public INDIProperty getProperty(String deviceName,String propertyName)
	{
		return connection.getProperty(deviceName,propertyName);
	}

	/**
	 * A convenience method to get the Element of a Property of a Device by specifiying their names.
	 * This is a pass-through method for INDIServerConnection.getElement
	 * @param deviceName the name of the device containing the required property.
	 * @param propertyName  The name of the Property containing the required Element.
	 * @param elementName The name of the Element.
	 * @return The Element with a elementName as a name of a Property with propertyName as name of the 
	 *          device with deviceName as name.
	 */
	public INDIElement getElement(String deviceName,String propertyName,String elementName)
	{
		return connection.getElement(deviceName,propertyName,elementName);
	}

}
