package ngat.lotus.command.test;
import java.io.IOException;
import java.util.Date;

import org.indilib.i4j.Constants;
import org.indilib.i4j.client.INDIProperty;
import org.indilib.i4j.client.INDIElement;
import org.indilib.i4j.client.INDINumberElement;
import org.indilib.i4j.client.INDIServerConnection;
import org.indilib.i4j.client.INDIValueException;
import org.indilib.i4j.protocol.INDIProtocol;
import org.indilib.i4j.protocol.url.INDIURLStreamHandlerFactory;

class SetCCDTemperature
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
	public SetCCDTemperature(String host, int port) 
	{
		connection = new INDIServerConnection(host, port);
	}

	public void connect() throws IOException
	{
		connection.connect();
		connection.askForDevices(CAMERA_DEVICE_NAME); 
	}

	public void setPropertyValue(String deviceName,String propertyName, String elementName,Double newValue) throws
		IllegalArgumentException, INDIValueException
	{
		INDIElement element = null;
		INDIProperty property = null;

		// Wait until the Server has a Device with a
		// property and element with the required name
		while (property == null) 
		{ 
			try 
			{
				System.out.println("Trying to retrieve property Device: "+deviceName+": Property "+
						   propertyName+".");
				try 
				{
					Thread.sleep(Constants.WAITING_INTERVAL); // Wait 0.5
					// seconds.
				} 
				catch (InterruptedException e) 
				{
				}
				property = (INDIProperty) connection.getProperty(deviceName,propertyName);
			} 
			catch (ClassCastException e) 
			{
				System.err.println("The Property is not an property:"+e);
				System.exit(-1);
			}
		}
		// Wait until the Server has a Device with a
		// property and element with the required name
		while (element == null) 
		{ 
			try 
			{
				System.out.println("Trying to retrieve element Device: "+deviceName+": Property "+propertyName+" Element: "+elementName+".");
				try 
				{
					Thread.sleep(Constants.WAITING_INTERVAL); // Wait 0.5
					// seconds.
				} 
				catch (InterruptedException e) 
				{
				}
				element = (INDIElement) property.getElement(elementName);
			} 
			catch (ClassCastException e) 
			{
				System.err.println("The Element is not an element:"+e);
				System.exit(-1);
			}
		}
		Object value = element.getValue();
		System.out.println("Device: "+deviceName+": Property "+propertyName+" has current value: "+value+" of class "+value.getClass().getName());
		if(element instanceof INDINumberElement)
		{
			element.setDesiredValue(newValue);
		}
		else
			throw new IllegalArgumentException(this.getClass().getName()+":setPropertyValue:element value type:"+value.getClass().getName()+" does not match Double.");
		// tell property to update server with new value
		try
		{
			System.out.println("Trying to update property with new value: "+newValue+".");
			property.sendChangesToDriver();
		}
		catch(Exception e)
		{
			System.err.println("Property.sendChangesToDriver failed with error:"+e);
			e.printStackTrace(System.err);
			System.exit(-1);
		}
		// see if value updates
		//while(element.isChanged() == false)
		//{
		//	System.out.println("Waiting for value to change.");
		//	try 
		//	{
		//		Thread.sleep(Constants.WAITING_INTERVAL); // Wait 0.5
				// seconds.
		//	} 
		//	catch (InterruptedException e) 
		//	{
		//	}
		//}
		Object checkValue = getPropertyValue(deviceName,propertyName,elementName);
		System.out.println("Device: "+deviceName+": Property "+propertyName+" has new value: "+checkValue+" of class "+checkValue.getClass().getName());
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
		if (args.length != 3) 
		{
			System.err.println("ngat.lotus.command.test.SetCCDTemperature <host> <port> <temperature>");
			System.exit(1);
		}
		
		String host = args[0];
		int port = Constants.INDI_DEFAULT_PORT;
		double targetTemperature = 0.0;

		try 
		{
			port = Integer.parseInt(args[1]);
		} 
		catch (NumberFormatException e) 
		{
			System.err.println("Failed to parse port number:"+args[1]);
			System.exit(1);
		}
		try 
		{
			targetTemperature = Double.parseDouble(args[2]);
		} 
		catch (NumberFormatException e) 
		{
			System.err.println("Failed to parse target temperature:"+args[2]);
			System.exit(1);
		}

		SetCCDTemperature setCCDTemp = new SetCCDTemperature(host,port);
		try {
			setCCDTemp.connect();
		} catch (IOException e) {
			System.err.println("Problem with the connection: " + setCCDTemp.connection.toString()+":"+e);
		}
		try
		{
			setCCDTemp.setPropertyValue(CAMERA_DEVICE_NAME,CCD_TEMPERATURE_PROPERTY_NAME,
						    CCD_TEMPERATURE_ELEMENT_NAME,new Double(targetTemperature));
		}
		catch (Exception e) 
		{
			System.err.println("Failed to set target temperature:"+e);
			e.printStackTrace(System.err);
			System.exit(1);
		}

		System.exit(0);
	}
}
