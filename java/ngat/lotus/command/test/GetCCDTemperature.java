package ngat.lotus.command.test;
import java.io.IOException;
import java.util.Date;

import org.indilib.i4j.Constants;
import org.indilib.i4j.client.INDIDevice;
import org.indilib.i4j.client.INDIDeviceListener;
import org.indilib.i4j.client.INDIProperty;
import org.indilib.i4j.client.INDIPropertyListener;
import org.indilib.i4j.client.INDIElement;
import org.indilib.i4j.client.INDIServerConnection;
import org.indilib.i4j.client.INDIServerConnectionListener;
import org.indilib.i4j.protocol.INDIProtocol;
import org.indilib.i4j.protocol.url.INDIURLStreamHandlerFactory;

class GetCCDTemperature implements INDIServerConnectionListener, INDIDeviceListener, INDIPropertyListener 
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
    private INDIServerConnection currentConnection;


	/**
	 * Creates aSimpleINDIClient that will connect to a particular INDI Server.
	 * 
	 * @param host
	 *            The host of the server
	 * @param port
	 *            The port of the server
	 */
	public GetCCDTemperature(String host, int port) 
	{
		currentConnection = new INDIServerConnection(host, port);

		// Listen to all server events
		currentConnection.addINDIServerConnectionListener(this);

		try 
		{
			currentConnection.connect();
			currentConnection.askForDevices(); // Ask for all the devices.
		} catch (IOException e) {
		       System.err.println("Problem with the connection: " + host + ":" + port + ":"+ e);
		}
	}

    @Override
    public void newDevice(INDIServerConnection connection, INDIDevice device) {
        // We just simply listen to this Device
        System.out.println("New device: " + device.getName());
        try {
            // Enable receiving BLOBs from this Device
            device.blobsEnable(Constants.BLOBEnables.ALSO);
        } catch (IOException e) {
            System.err.println("Problem asking for BLOBs:"+e);
        }
	if(device.getName().equals(CAMERA_DEVICE_NAME))
		device.addINDIDeviceListener(this);
    }

    @Override
    public void removeDevice(INDIServerConnection connection, INDIDevice device) {
        // We just remove ourselves as a listener of the removed device
        System.out.println("Device Removed: " + device.getName());
        device.removeINDIDeviceListener(this);
    }

    @Override
    public void connectionLost(INDIServerConnection connection) {
        System.out.println("Connection lost. Bye");

        System.exit(-1);
    }

    @Override
    public void newMessage(INDIServerConnection connection, Date timestamp, String message) {
        System.out.println("New Server Message: " + timestamp + " - " + message);
    }

    @Override
    public void messageChanged(INDIDevice device) {
        System.out.println("New Device Message: " + device.getName() + " - " + device.getTimestamp() + " - " + device.getLastMessage());
    }

    @Override
    public void newProperty(INDIDevice device, INDIProperty<?> property) {
        // We just simply listen to this Property
        System.out.println("New Property (" + property.getName() + ") added to device " + device.getName());
	if(property.getName().equals(CCD_TEMPERATURE_PROPERTY_NAME))
		property.addINDIPropertyListener(this);
    }

    @Override
    public void propertyChanged(INDIProperty<?> property) {
	    INDIElement element = null;
        System.out.println("Property Changed: " + property.getNameStateAndValuesAsString());
	element = property.getElement(CCD_TEMPERATURE_ELEMENT_NAME);
	if(element != null)
	{
		Object value = element.getValue();
		System.out.println("Temperature is currently: "+value+" of class "+value.getClass().getName());
		System.exit(0);
	}
	

    }

    @Override
    public void removeProperty(INDIDevice device, INDIProperty<?> property) {

        // We just remove ourselves as a listener of the removed property
        System.out.println("Property (" + property.getName() + ") removed from device " + device.getName());
        property.removeINDIPropertyListener(this);
    }
    /**
     * Parses the arguments and creates the Client if they are correct.
     * 
     * @param args
     *            The arguments of the application
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
		System.err.println("ngat.lotus.command.test.GetCCDTemperature <host> <port>");
		    System.exit(1);
        }

        String host = args[0];
        int port = Constants.INDI_DEFAULT_PORT;

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
		    System.err.println("Failed to parse port number:"+args[1]);
		    System.exit(1);
            }
        }

        new GetCCDTemperature(host, port);
    }

}
