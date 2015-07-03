package ngat.lotus.command.test;
import java.io.IOException;
import java.util.Date;

import org.indilib.i4j.Constants;
import org.indilib.i4j.client.INDIElement;
import org.indilib.i4j.client.INDIElementListener;
import org.indilib.i4j.client.INDIServerConnection;
import org.indilib.i4j.protocol.INDIProtocol;
import org.indilib.i4j.protocol.url.INDIURLStreamHandlerFactory;

class GetCCDTemperature2 
{
	public final static String CAMERA_DEVICE_NAME = "SX CCD SXVR-H35";
	public final static String CCD_TEMPERATURE_PROPERTY_NAME = "CCD_TEMPERATURE";
	public final static String CCD_TEMPERATURE_ELEMENT_NAME = "CCD_TEMPERATURE_VALUE";
	
	static 
	{
		INDIURLStreamHandlerFactory.init();
	}

	/**
	 * The connection to a INDI server.
	 */
	private INDIServerConnection connection;

	/**
	 * Creates aSimpleINDIClient that will connect to a particular INDI Server.
	 * 
	 * @param host
	 *            The host of the server
	 * @param port
	 *            The port of the server
	 */
	public GetCCDTemperature2(String host, int port) 
	{
		connection = new INDIServerConnection(host, port);
	}

	public void connect() throws IOException
	{
		connection.connect();
		connection.askForDevices(); // Ask for all the devices.
	}

	public Object getPropertyValue(String deviceName,String propertyName, String elementName)
	{
		INDIElement element = null;

		// Wait until the Server has a Device with a
		// property and element with the required name
		while (element == null) 
		{ 
			try 
			{
				try 
				{
					Thread.sleep(Constants.WAITING_INTERVAL); // Wait 0.5
					// seconds.
				} 
				catch (InterruptedException e) 
				{
				}
				element = (INDIElement) connection.getElement(deviceName,propertyName,elementName);
			} 
			catch (ClassCastException e) 
			{
				System.err.println("The Element is not an element:"+e);
				System.exit(-1);
			}
		}
		Object value = element.getValue();
		System.out.println("Device: "+deviceName+": Property "+propertyName+" has value: "+value+" of class "+value.getClass().getName());
		return value;
	}

	/**
	 * Parses the arguments and creates the Client if they are correct.
	 * 
	 * @param args
	 *            The arguments of the application
	 */
	public static void main(String[] args)
	{
		if (args.length < 1 || args.length > 2) 
		{
			System.err.println("ngat.lotus.command.test.GetCCDTemperature2 <host> <port>");
			System.exit(1);
		}
		
		String host = args[0];
		int port = Constants.INDI_DEFAULT_PORT;
		
		if (args.length > 1) 
		{
			try 
			{
				port = Integer.parseInt(args[1]);
			} 
			catch (NumberFormatException e) 
			{
				System.err.println("Failed to parse port number:"+args[1]);
				System.exit(1);
			}
		}

		GetCCDTemperature2 getCCDTemp = new GetCCDTemperature2(host,port);
		try {
			getCCDTemp.connect();
		} catch (IOException e) {
			System.err.println("Problem with the connection: " + getCCDTemp.connection.toString()+":"+e);
		}
		Object value = getCCDTemp.getPropertyValue(CAMERA_DEVICE_NAME,CCD_TEMPERATURE_PROPERTY_NAME,
							   CCD_TEMPERATURE_ELEMENT_NAME);
		if(value instanceof Double )
		{
			Double ccdTemp = (Double)value;
			System.out.println("Current CCD Temperature:"+ccdTemp);
		}
		else
			System.out.println("Returned value: "+value+" has class "+value.getClass().getName());
		System.exit(0);
	}
}
