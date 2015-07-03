//  LOTUSINDIDevice.java
// $HeadURL$
package ngat.lotus.ccd;

import java.io.*;
import java.lang.*;

import org.indilib.i4j.Constants;
import org.indilib.i4j.Constants.SwitchStatus;
import org.indilib.i4j.client.INDIProperty;
import org.indilib.i4j.client.INDIElement;
import org.indilib.i4j.client.INDINumberElement;
import org.indilib.i4j.client.INDISwitchElement;
import org.indilib.i4j.client.INDITextElement;
import org.indilib.i4j.client.INDIValueException;

import ngat.util.*;
import ngat.util.logging.*;

/**
 * This class allows us to set and get properties for an INDI Device via an INDI server.
 * @author Chris Mottram
 * @version $Revision: 57 $
 */
public class LOTUSINDIDevice
{
	/**
	 * Revision Control System id string, showing the version of the Class.
	 */
	public final static String RCSID = new String("$Id$");
	/**
	 * The default number of times to attempt to get an element from the connection before timing out.
	 */
	public final static int DEFAULT_TIMEOUT_COUNT = 10;
	/**
	 * The connection to use when communicating with the camera.
	 * @see LOTUSINDIConnection
	 */
	LOTUSINDIConnection connection = null;
	/**
	 * The number of times to attempt to get an element from the connection before timing out.
	 * @see #DEFAULT_TIMEOUT_COUNT
	 */
	int timeoutCount = DEFAULT_TIMEOUT_COUNT;
	/**
	 * Logger to use.
	 */
	Logger logger = null;

	/**
	 * Default constructor.
	 * @see #logger
	 */
	public LOTUSINDIDevice()
	{
		super();
		logger = LogManager.getLogger(this);
	}

	/**
	 * Set the connection to use for communication with the camera (via the INDI server).
	 * @param c The connection object.
	 * @see #connection
	 */
	public void setConnection(LOTUSINDIConnection c)
	{
		connection = c;
	}

	/**
	 * Set the number of times to attempt to get an element from the connection before timing out.
	 * @param t The number of times to attempt to get an element. Should be greater than 1.
	 * @see #DEFAULT_TIMEOUT_COUNT
	 * @see #timeoutCount
	 */
	public void setTimeoutCount(int t)
	{
		timeoutCount = t;
	}

	/**
	 * Open a connection to the INDI server.
	 * @exception IOException Thrown if the connection fails.
	 * @see #connection
	 */
	public void connect() throws Exception
	{
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+":Connecting.");
		connection.connect();	
	}

	/**
	 * Open a connection to the INDI server.
	 * @param deviceName The name of the device (controlled by the server) that we want to communicate with.
	 * @exception IOException Thrown if the connection fails.
	 * @see #connection
	 */
	public void connect(String deviceName) throws Exception
	{
		logger.log(Logging.VERBOSITY_INTERMEDIATE,this.getClass().getName()+":Connecting to device "+
			   deviceName+".");
		connection.connect(deviceName);	
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
	 * Get the current value of the specified switch property element, as a boolean.
	 * @param deviceName The INDI device the property belongs to.
	 * @param propertyName The INDI property to query.
	 * @param elementName The INDI property element to return the value of.
	 * @return The value of the property element as an object. If the property is a INDINumberProperty the value
	 *    will be a double.
	 * @exception Exception Thrown when the specified element cannot be found in timeoutCount attempts.
	 * @exception ClassCastException Thrown when connection.getElement does not return an INDIElement.
	 * @see #timeoutCount
	 * @see #connection
	 */
	public synchronized boolean getSwitchPropertyValue(String deviceName,String propertyName, String elementName) 
		throws ClassCastException, Exception
	{
		SwitchStatus switchValue = null;
		Object value = null;

		value = getPropertyValue(deviceName,propertyName,elementName);
		if((value instanceof SwitchStatus) == false)
		{
			throw new Exception(this.getClass().getName()+
					    ":getSwitchPropertyValue: Device: "+deviceName+
					    ": Property "+propertyName+": Element "+elementName+
					    " has value "+value+" of illegal class "+value.getClass().getName()+".");
		}
		switchValue = (SwitchStatus)value;
		if(switchValue == SwitchStatus.ON)
			return true;
		else if(switchValue == SwitchStatus.OFF)
			return false;
		else
		{
			throw new Exception(this.getClass().getName()+
					    ":getSwitchPropertyValue: Device: "+deviceName+
					    ": Property "+propertyName+": Element "+elementName+
					    " has illegal value "+value+".");
		}
	}

	/**
	 * Get the current value of the specified property element.
	 * @param deviceName The INDI device the property belongs to.
	 * @param propertyName The INDI property to query.
	 * @param elementName The INDI property element to return the value of.
	 * @return The value of the property element as an object. 
	 *    If the property is an INDINumberProperty the value will be a Double. 
	 *    If the property is an INDITextElement the value will be a String.
	 *    If the property is an  INDISwitchElement the value will be a org.indilib.i4j.Constants.SwitchStatus,
	 *    use getSwitchPropertyValue in this case.
	 * @exception Exception Thrown when the specified element cannot be found in timeoutCount attempts.
	 * @exception ClassCastException Thrown when connection.getElement does not return an INDIElement.
	 * @see #timeoutCount
	 * @see #connection
	 */
	public synchronized Object getPropertyValue(String deviceName,String propertyName, String elementName) throws 
		ClassCastException, Exception
	{
		INDIElement element = null;
		Object value = null;
		int timeoutIndex;

		timeoutIndex = 0;
		// Wait until the Server has a Device with a
		// property and element with the required name
		while((element == null) && (timeoutIndex < timeoutCount))
		{ 
			try 
			{
				Thread.sleep(Constants.WAITING_INTERVAL); // Wait 0.5 seconds.
			} 
			catch (InterruptedException e) 
			{
			}
			element = (INDIElement) connection.getElement(deviceName,propertyName,elementName);
			timeoutIndex++;
		}
		if(element == null)
		{
			throw new Exception(this.getClass().getName()+
					    ":getPropertyValue: element was null for Device: "+deviceName+
					    ": Property "+propertyName+": Element "+elementName+
					    " after "+timeoutIndex+" attempts.");
		}
		value = element.getValue();
		if(value != null)
		{
			logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+":Device: "+deviceName+
				   ": Property "+propertyName+": Element "+elementName+
				   " has value: "+value+" of class "+value.getClass().getName());
		}
		else
		{
			logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+":Device: "+deviceName+
				   ": Property "+propertyName+": Element "+elementName+" has null value.");
		}
		return value;
	}

	/**
	 * Set the current value of the specified property element (which should be a INDINumberProperty).
	 * @param deviceName The INDI device the property belongs to.
	 * @param propertyName The INDI property to set.
	 * @param elementName The INDI property element to set the value of.
	 * @param newValue The value of the property element as a Double object. 
	 * @exception Exception Thrown when the specified property or element cannot be found in timeoutCount attempts,
	 *            or property.sendChangesToDriver fails.
	 * @exception ClassCastException Thrown when property.getElement does not return an INDIElement,
	 *            or getPropertyFromConnection does not return a property.
	 * @exception IllegalArgumentException Thrown if the element value type is not a Double.
	 * @see #getPropertyFromConnection
	 * @see #getElementFromProperty
	 */
	public synchronized void setPropertyValue(String deviceName,String propertyName, String elementName,
						  Double newValue) throws IllegalArgumentException, 
									  ClassCastException, Exception
	{
		INDIElement element = null;
		INDIProperty property = null;
		Object value = null;

		property = getPropertyFromConnection(deviceName,propertyName);
		element = getElementFromProperty(property,elementName);
		value = element.getValue();
		logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+":Device: "+deviceName+
			   ": Property "+propertyName+"  Element: "+elementName+" has current value: "+value+
			   " of class "+value.getClass().getName());
		if(element instanceof INDINumberElement)
		{
			element.setDesiredValue(newValue);
		}
		else
		{
			throw new IllegalArgumentException(this.getClass().getName()+
							   ":setPropertyValue:element value type:"+
							   value.getClass().getName()+" does not match Double.");
		}
		// tell property to update server with new value
		try
		{
			logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				   ":Trying to update Property "+propertyName+"  Element: "+elementName+
				   " with new value: "+newValue+".");
			property.sendChangesToDriver();
		}
		catch(Exception e)
		{
			// catch the INDI library exceptions and throw a more generic exception here,
			// to stop INDI namespace pollution of the main robotic software
			throw new Exception(this.getClass().getName()+
					    ":setPropertyValue:Property.sendChangesToDriver failed with error:"+e,e);
		}
	}

	/**
	 * Set the current values of several specified property elements (which should be INDINumberPropertys),
	 * belonging to the same property.
	 * @param deviceName The INDI device the property belongs to.
	 * @param propertyName The INDI property to set.
	 * @param elementNameList An array of strings containing the INDI property elements to set the values of.
	 * @param newValueList An array of values of the property elements as a Double objects. 
	 * @exception Exception Thrown when the specified property or element cannot be found in timeoutCount attempts,
	 *            or property.sendChangesToDriver fails.
	 * @exception ClassCastException Thrown when property.getElement does not return an INDIElement,
	 *            or getPropertyFromConnection does not return a property.
	 * @exception IllegalArgumentException Thrown if the element value type is not a Double.
	 * @see #getPropertyFromConnection
	 * @see #getElementFromProperty
	 */
	public synchronized void setPropertyValues(String deviceName,String propertyName, 
						   String elementNameList[],Double newValueList[]) throws
		IllegalArgumentException, ClassCastException, Exception
	{
		INDIElement element = null;
		INDIProperty property = null;
		Object value = null;
		int elementNameCount, newValueCount;

		// ensure element and value arrays are not null
		if(elementNameList == null)
		{
			throw new IllegalArgumentException(this.getClass().getName()+
							   ":setPropertyValues:elementNameList was null.");
		}
		if(newValueList == null)
		{
			throw new IllegalArgumentException(this.getClass().getName()+
							   ":setPropertyValues:newValueList was null.");
		}
		// ensure element and value arrays are the same length
		elementNameCount = elementNameList.length;
		newValueCount = newValueList.length;
		if(elementNameCount != newValueCount)
		{
			throw new IllegalArgumentException(this.getClass().getName()+
			      ":setPropertyValues:elementNameList and newValueList were not the same length:"+
							   elementNameCount+" vs "+newValueCount+".");
		}
		// now we can just use elementNameCount :- newValueCount is the same number
		if(elementNameCount < 1)
		{
			throw new IllegalArgumentException(this.getClass().getName()+
			      ":setPropertyValues:elementNameList and newValueList are empty.");
		}
		// get property
		property = getPropertyFromConnection(deviceName,propertyName);
		for(int i = 0; i < elementNameCount; i++)
		{
			element = getElementFromProperty(property,elementNameList[i]);
			value = element.getValue();
			logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+":Device: "+deviceName+
				   ": Property "+propertyName+"  Element: "+elementNameList[i]+
				   " has current value: "+value+" of class "+value.getClass().getName());
			if(element instanceof INDINumberElement)
			{
				logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
					   ":Setting  Element: "+elementNameList[i]+
					   " with new value: "+newValueList[i]+".");
				element.setDesiredValue(newValueList[i]);
			}
			else
			{
				throw new IllegalArgumentException(this.getClass().getName()+
								   ":setPropertyValue:element value type:"+
								 value.getClass().getName()+" does not match Double.");
			}
		}
		// tell property to update server with new values
		try
		{
			logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				   ":Trying to update Property "+propertyName+" Elements with new values.");
			property.sendChangesToDriver();
		}
		catch(Exception e)
		{
			// catch the INDI library exceptions and throw a more generic exception here,
			// to stop INDI namespace pollution of the main robotic software
			throw new Exception(this.getClass().getName()+
					    ":setPropertyValue:Property.sendChangesToDriver failed with error:"+e,e);
		}
	}

	/**
	 * Set the current value of the specified property element (which should be a INDITextElement).
	 * @param deviceName The INDI device the property belongs to.
	 * @param propertyName The INDI property to set.
	 * @param elementName The INDI property element to set the value of.
	 * @param newValue The value of the property element as a String object. 
	 * @exception Exception Thrown when the specified property or element cannot be found in timeoutCount attempts,
	 *            or property.sendChangesToDriver fails.
	 * @exception ClassCastException Thrown when property.getElement does not return an INDIElement,
	 *            or getPropertyFromConnection does not return a property.
	 * @exception IllegalArgumentException Thrown if the element value type is not a Double.
	 * @see #getPropertyFromConnection
	 * @see #getElementFromProperty
	 */
	public synchronized void setPropertyValue(String deviceName,String propertyName, String elementName,
						  String newValue) throws
		IllegalArgumentException, ClassCastException, Exception
	{
		INDIElement element = null;
		INDIProperty property = null;
		Object value = null;

		property = getPropertyFromConnection(deviceName,propertyName);
		element = getElementFromProperty(property,elementName);
		value = element.getValue();
		logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+":Device: "+deviceName+
			   ": Property "+propertyName+"  Element: "+elementName+" has current value: "+value+
			   " of class "+value.getClass().getName());
		if(element instanceof INDITextElement)
		{
			element.setDesiredValue(newValue);
		}
		else
		{
			throw new IllegalArgumentException(this.getClass().getName()+
							   ":setPropertyValue:element value type:"+
							   value.getClass().getName()+" does not match String.");
		}
		// tell property to update server with new value
		try
		{
			logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				   ":Trying to update Property "+propertyName+"  Element: "+elementName+
				   " with new value: "+newValue+".");
			property.sendChangesToDriver();
		}
		catch(Exception e)
		{
			// catch the INDI library exceptions and throw a more generic exception here,
			// to stop INDI namespace pollution of the main robotic software
			throw new Exception(this.getClass().getName()+
					    ":setPropertyValue:Property.sendChangesToDriver failed with error:"+e,e);
		}
	}

	/**
	 * Set the current value of the specified property element (which should be a INDISwitchElement).
	 * @param deviceName The INDI device the property belongs to.
	 * @param propertyName The INDI property to set.
	 * @param elementName The INDI property element to set the value of.
	 * @param newValue This boolean should be true to set the  INDISwitchElement to On, and false to set the
	 *        INDISwitchElement to Off.
	 * @exception Exception Thrown when the specified property or element cannot be found in timeoutCount attempts,
	 *            or property.sendChangesToDriver fails.
	 * @exception ClassCastException Thrown when getElementFromProperty does not return an INDIElement,
	 *            or getPropertyFromConnection does not return a property.
	 * @exception IllegalArgumentException Thrown if the element value type is not a SwitchStatus.
	 * @see #getPropertyFromConnection
	 * @see #getElementFromProperty
	 */
	public synchronized void setPropertyValue(String deviceName,String propertyName, String elementName,
						  boolean newValue) throws
		IllegalArgumentException, ClassCastException, Exception
	{
		INDIElement element = null;
		INDIProperty property = null;
		Object value = null;

		property = getPropertyFromConnection(deviceName,propertyName);
		element = getElementFromProperty(property,elementName);
		value = element.getValue();
		logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+":Device: "+deviceName+
			   ": Property "+propertyName+"  Element: "+elementName+" has current value: "+value+
			   " of class "+value.getClass().getName());
		if(element instanceof INDISwitchElement)
		{
			if(newValue)
				element.setDesiredValue(SwitchStatus.ON);
			else
				element.setDesiredValue(SwitchStatus.OFF);
		}
		else
		{
			throw new IllegalArgumentException(this.getClass().getName()+
							   ":setPropertyValue:element value type:"+
							   value.getClass().getName()+" does not match SwitchStatus.");
		}
		// tell property to update server with new value
		try
		{
			logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				   ":Trying to update Property "+propertyName+"  Element: "+elementName+
				   " with new value: "+newValue+".");
			property.sendChangesToDriver();
		}
		catch(Exception e)
		{
			// catch the INDI library exceptions and throw a more generic exception here,
			// to stop INDI namespace pollution of the main robotic software
			throw new Exception(this.getClass().getName()+
					    ":setPropertyValue:Property.sendChangesToDriver failed with error:"+e,e);
		}
	}

	/**
	 * Get the specified property from the connection.
	 * @param deviceName The INDI device the property belongs to.
	 * @param propertyName The INDI property name to retrieve.
	 * @return The instance of INDIProperty from the specified device with the specified propertyName.
	 * @exception Exception Thrown when the specified property cannot be found in timeoutCount attempts.
	 * @exception ClassCastException Thrown when connection.getProperty does not return an INDIProperty.
	 * @see #timeoutCount
	 * @see #connection
	 * @see #logger
	 */
	protected synchronized INDIProperty getPropertyFromConnection(String deviceName,String propertyName) 
		throws Exception, ClassCastException
	{
		INDIProperty property = null;
		int timeoutIndex;

		// Wait until the Server has a Device with a
		// property with the required name
		timeoutIndex = 0;
		while((property == null) && (timeoutIndex < timeoutCount))
		{ 
			logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				   ":getPropertyFromConnection:Trying to retrieve property Device: "+deviceName+
				   ": Property "+propertyName+".");
			try 
			{
				Thread.sleep(Constants.WAITING_INTERVAL); // Wait 0.5 seconds.
			} 
			catch (InterruptedException e) 
			{
			}
			property = (INDIProperty) connection.getProperty(deviceName,propertyName);
			timeoutIndex++;
		}
		if(property == null)
		{
			throw new Exception(this.getClass().getName()+
					    ":getPropertyFromConnection: property was null for Device: "+deviceName+
					    ": Property "+propertyName+" after "+timeoutIndex+" attempts.");
		}
		return property;
	}

	/**
	 * Get the specified element from the property.
	 * @param property The INDI property to query.
	 * @param elementName The INDI property element to retrieve.
	 * @return The instance of INDIElement from the specified property with the specified elementName.
	 * @exception Exception Thrown when the specified element cannot be found in timeoutCount attempts.
	 * @exception ClassCastException Thrown when property.getElement does not return an INDIElement.
	 * @see #timeoutCount
	 * @see #connection
	 * @see #logger
	 */
	protected synchronized INDIElement getElementFromProperty(INDIProperty property,
								  String elementName) throws Exception, 
											     ClassCastException
	{
		INDIElement element = null;
		int timeoutIndex;

		// Wait until the Server has a Device with a
		// property and element with the required name
		timeoutIndex = 0;
		while ((element == null)  && (timeoutIndex < timeoutCount))
		{ 
			logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				   ":getElementFromProperty:Trying to retrieve Element: "+elementName+
				   " from property :"+property+".");
			try 
			{
				Thread.sleep(Constants.WAITING_INTERVAL); // Wait 0.5 seconds.
			} 
			catch (InterruptedException e) 
			{
			}
			element = (INDIElement) property.getElement(elementName);
			timeoutIndex++;
		}
		if(element == null)
		{
			throw new Exception(this.getClass().getName()+
					    ":getElementFromProperty: element was null for Property "+property+
					    ": Element "+elementName+" after "+timeoutIndex+" attempts.");
		}
		return element;
	}
}
