/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.eclipse.ui.property;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.swt.widgets.Composite;

/**
 * Cell editor for numbers.
 * 
 * @author rgansevles
 */

public class NumberCellEditor extends ObjectCellEditor implements IExecutableExtension
{

	protected static final Short MAX_SHORT = new Short(Short.MAX_VALUE);

	protected static final Short MIN_SHORT = new Short(Short.MIN_VALUE);

	protected static final Long MAX_LONG = new Long(Long.MAX_VALUE);

	protected static final Long MIN_LONG = new Long(Long.MIN_VALUE);

	protected static final Integer MAX_INTEGER = new Integer(Integer.MAX_VALUE);

	protected static final Integer MIN_INTEGER = new Integer(Integer.MIN_VALUE);

	protected static final Float MAX_FLOAT = new Float(Float.MAX_VALUE);

	protected static final Float MIN_FLOAT = new Float(Float.MIN_VALUE);

	protected static final Double MAX_DOUBLE = new Double(Double.MAX_VALUE);

	protected static final Double MIN_DOUBLE = new Double(Double.MIN_VALUE);

	protected static final Byte MAX_BYTE = new Byte(Byte.MAX_VALUE);

	protected static final Byte MIN_BYTE = new Byte(Byte.MIN_VALUE);

	public static final int
	// Type of number to be returned.
		NUMBER = 0, // Whatever it produces
		BYTE = 1, DOUBLE = 2, FLOAT = 3, INTEGER = 4, LONG = 5, SHORT = 6;

	protected static final MinmaxValidator[] sMinMaxValidators = { null, new MinmaxValidator(MIN_BYTE, MAX_BYTE), new MinmaxValidator(MIN_DOUBLE, MAX_DOUBLE), new MinmaxValidator(
		MIN_FLOAT, MAX_FLOAT), new MinmaxValidator(MIN_INTEGER, MAX_INTEGER), new MinmaxValidator(MIN_LONG, MAX_LONG), new MinmaxValidator(MIN_SHORT, MAX_SHORT) };

	protected static final String sNotNumberError, sNotIntegerError, sMinValue, sMaxValue;
	static
	{
		sNotNumberError = "Not a number";
		sNotIntegerError = "Not an integer";
		sMinValue = "Value too low";
		sMaxValue = "Value too high";
	}

	protected NumberFormat fFormatter;
	{
		fFormatter = NumberFormat.getInstance();
		fFormatter.setMaximumFractionDigits(20);
		fFormatter.setMaximumIntegerDigits(20);
	}

	protected int fNumberType = NUMBER;

	public NumberCellEditor(Composite parent)
	{
		super(parent);
	}

	/**
	 * This will only expect initData to be a string. The string should be the type, a) integer b) long c) etc.
	 * 
	 * number is the default.
	 */
	public void setInitializationData(IConfigurationElement ce, String pName, Object initData)
	{
		if (initData instanceof String)
		{
			String type = ((String)initData).trim();
			if ("byte".equalsIgnoreCase(type)) //$NON-NLS-1$
			setType(BYTE);
			else if ("double".equalsIgnoreCase(type)) //$NON-NLS-1$
			setType(DOUBLE);
			else if ("float".equalsIgnoreCase(type)) //$NON-NLS-1$
			setType(FLOAT);
			else if ("integer".equalsIgnoreCase(type)) //$NON-NLS-1$
			setType(INTEGER);
			else if ("long".equalsIgnoreCase(type)) //$NON-NLS-1$
			setType(LONG);
			else if ("short".equalsIgnoreCase(type)) //$NON-NLS-1$
			setType(SHORT);
		}
	}

	public void setType(int type)
	{
		switch (type)
		{
			case NUMBER :
			case DOUBLE :
			case FLOAT :
				fFormatter.setParseIntegerOnly(false);
				break;
			case BYTE :
			case INTEGER :
			case LONG :
			case SHORT :
				fFormatter.setParseIntegerOnly(true);
				break;
			default :
				return; // Invalid type, do nothing
		}

		fNumberType = type;
	}

	@Override
	protected String isCorrectObject(Object value)
	{
		return (value == null || value instanceof Number) ? null : (fFormatter.isParseIntegerOnly() ? sNotIntegerError : sNotNumberError);
	}

	@Override
	protected String isCorrectString(String value)
	{
		String text = value.trim();
		if (sMinValue.equalsIgnoreCase(text) || sMaxValue.equalsIgnoreCase(text)) return null;

		Number result = null;
		if ((fNumberType == DOUBLE || fNumberType == FLOAT) && (text.indexOf('e') != -1 || text.indexOf('E') != -1))
		{
			// We have a double/float with an exponent. This is scientific notation. Formatter handles them badly, so use parse instead.
			try
			{
				if (fNumberType == DOUBLE) result = new Double(Double.parseDouble(text));
				else result = new Float(Float.parseFloat(text));
			}
			catch (NumberFormatException e)
			{
			}
		}
		else
		{
			// integral or not scientific notation. Let formatter handle it.
			ParsePosition parsePosition = new ParsePosition(0);
			result = fFormatter.parse(text, parsePosition);
			if (parsePosition.getErrorIndex() != -1 || parsePosition.getIndex() != text.length()) result = null; // Some error
			// Check for out of bounds with long type
			if (fNumberType == LONG && result instanceof Double)
			{
				result = (result.doubleValue() < 0) ? MinmaxValidator.LONG_UNDERFLOW : MinmaxValidator.LONG_OVERFLOW;
			}
		}

		if (result != null)
		{
			// Now see if it is valid for the requested type.
			MinmaxValidator v = sMinMaxValidators[fNumberType];

			// Double/Float are special because the min/MIN are on the absolute value, not signed value.
			if (fNumberType == DOUBLE || fNumberType == FLOAT)
			{
				double d = result.doubleValue();
				if (d == 0.0 || d == -0.0) return null; // +/- zero are valid values.
				result = new Double(Math.abs(d));
			}
			if (v != null)
			{
				String e = v.isValid(result);
				if (e == null || e.length() == 0) return null;
				return e; // It didn't fit in a the number type.
			}
		}
		return (fFormatter.isParseIntegerOnly() ? sNotIntegerError : sNotNumberError);
	}


	/**
	 * Return the object that the string represents.
	 */
	@Override
	protected Object doGetObject(String v)
	{
		try
		{
			if (v == null) return v;
			Number n = null;
			if (sMaxValue.equalsIgnoreCase(v))
			{
				switch (fNumberType)
				{
					case BYTE :
						return MAX_BYTE;
					case DOUBLE :
						return MAX_DOUBLE;
					case FLOAT :
						return MAX_FLOAT;
					case INTEGER :
						return MAX_INTEGER;
					case LONG :
						return MAX_LONG;
					case SHORT :
						return MAX_SHORT;
					default :
						return null;
				}
			}
			else if (sMinValue.equalsIgnoreCase(v))
			{
				switch (fNumberType)
				{
					case BYTE :
						return MIN_BYTE;
					case DOUBLE :
						return MIN_DOUBLE;
					case FLOAT :
						return MIN_FLOAT;
					case INTEGER :
						return MIN_INTEGER;
					case LONG :
						return MIN_LONG;
					case SHORT :
						return MIN_SHORT;
					default :
						return null;
				}
			}

			// Float and Double are done separately below because parseFloat and parseDouble can
			// result in different values when casting a double back to a float.
			switch (fNumberType)
			{
				case BYTE :
					n = new Byte(fFormatter.parse(v).byteValue());
					break;
				case DOUBLE :
					if (v.indexOf('E') == -1 && v.indexOf('e') == -1) n = new Double(fFormatter.parse(v).doubleValue());
					else n = new Double(Double.parseDouble(v)); // It has scientific notation. The formatter just doesn't handle that very well.
					break;
				case FLOAT :
					if (v.indexOf('E') == -1 && v.indexOf('e') == -1) n = new Float(fFormatter.parse(v).floatValue());
					else n = new Float(Float.parseFloat(v)); // It has scientific notation. The formatter just doesn't handle that very well.
					break;
				case INTEGER :
					n = new Integer(fFormatter.parse(v).intValue());
					break;
				case LONG :
					n = new Long(fFormatter.parse(v).longValue());
					break;
				case SHORT :
					n = new Short(fFormatter.parse(v).shortValue());
					break;
			}
			return n;
		}
		catch (ParseException exc)
		{
			// Shouldn't occur because we already tested validity, and this wouldn't be called if invalid.
		}
		return null;
	}

	/**
	 * Return the string for the object passed in.
	 */
	@Override
	protected String doGetString(Object value)
	{
		if (value instanceof Number)
		{
			Number num = (Number)value;
			switch (fNumberType)
			{
				case BYTE :
					if (num.byteValue() == Byte.MAX_VALUE) return sMaxValue;
					else if (num.byteValue() == Byte.MIN_VALUE) return sMinValue;
					else break;

				case DOUBLE :
					if (num.doubleValue() == Double.MAX_VALUE) return sMaxValue;
					else if (num.doubleValue() == Double.MIN_VALUE) return sMinValue;
				case FLOAT :
					if (fNumberType == FLOAT)
					{
						if (num.floatValue() == Float.MAX_VALUE) return sMaxValue;
						else if (num.floatValue() == Float.MIN_VALUE) return sMinValue;
					}
					// The formatter doesn't handle big/small floats. (i.e. more than the MIN digits we set).
					// It doesn't go to scientific notation as necessary. The only way to test this is to
					// roundtrip the number. If they come up to be the same, then it is ok. Else format using
					// toString.
					String result = fFormatter.format(value);
					try
					{
						Number roundTrip = fFormatter.parse(result);
						if (roundTrip.doubleValue() != num.doubleValue()) result = value.toString();
					}
					catch (ParseException e)
					{
						result = value.toString();
					}
					return result;
				case INTEGER :
					if (num.intValue() == Integer.MAX_VALUE) return sMaxValue;
					else if (num.intValue() == Integer.MIN_VALUE) return sMinValue;
					else break;
				case LONG :
					if (num.longValue() == Long.MAX_VALUE) return sMaxValue;
					else if (num.longValue() == Long.MIN_VALUE) return sMinValue;
					else break;
				case SHORT :
					if (num.shortValue() == Short.MAX_VALUE) return sMaxValue;
					else if (num.shortValue() == Short.MIN_VALUE) return sMinValue;
					else break;
				default :
					break;
			}
			return fFormatter.format(value);
		}
		else return null; // Invalid or null. No string to display.
	}
}
