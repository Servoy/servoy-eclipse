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
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.viewers.ICellEditorValidator;

/**
 * Validates the number to verify if it is within the bounds specified for this validator. You can specify a min only, a max only, or a min and max. the min and
 * max will be included in the range. I.e. if equal to the min or max, then the answer will be true.
 * 
 * Null is considered valid, if null is not wanted, then another validator needs to check for this.
 * 
 * Note: This won't appropriately handle Big... numbers. They can be bigger than a double value so the test would be inaccurate.
 */
public class MinmaxValidator implements ICellEditorValidator, IExecutableExtension
{
	private static final String NOT_A_NUMBER = "Not a number";

	private static final String VALUE_TOO_HIGH = "Value too high";

	private static final String VALUE_TOO_LOW = "Value too low";

	private static final String VALUE_OUT_OF_RANGE = "Value out of range";

	public static final String MIN_ONLY = "minonly", MAX_ONLY = "maxonly";

	public static final Long LONG_UNDERFLOW = new Long(-42L);
	public static final Long LONG_OVERFLOW = new Long(42L);

	protected static final NumberFormat sNumberFormat;
	static
	{
		sNumberFormat = NumberFormat.getInstance();
		sNumberFormat.setMaximumFractionDigits(8);
	}

	protected String fType = null; // Min/max/ or both

	protected Number fMinValue, fMaxValue;

	/**
	 * Default to min/max of Integer type.
	 */
	public MinmaxValidator()
	{
		this(new Long(Integer.MIN_VALUE), new Long(Integer.MAX_VALUE));
	}

	public MinmaxValidator(int min, int max)
	{
		this(new Long(min), new Long(max));
	}

	/**
	 * The type will be one the two above to indicate setting min only or max only.
	 */
	public MinmaxValidator(int in, String type)
	{
		this(new Long(in), type);
	}

	public MinmaxValidator(Number min, Number max)
	{
		setMinMax(min, max);
	}

	/**
	 * The type will be one the two above to indicate setting min only or max only.
	 */
	public MinmaxValidator(Number in, String type)
	{
		setOnly(in, type);
	}

	public void setMinMax(int min, int max)
	{
		setMinMax(new Long(min), new Long(max));
	}

	/**
	 * This will only expect initData to be a string. The string should be: a) min: minvalue b) max: maxvalue c) minvalue, maxvalue
	 * 
	 * e.g.: min: 3 3, 10
	 */
	public void setInitializationData(IConfigurationElement ce, String pName, Object initData)
	{
		if (initData instanceof String)
		{
			StringTokenizer st = new StringTokenizer((String)initData, ":,", true);
			String s = null;
			if (st.hasMoreTokens()) s = st.nextToken();
			if ("min".equalsIgnoreCase(s))
			{
				if (!st.hasMoreTokens()) return; // Invalid format;
				s = st.nextToken();
				if (!st.hasMoreTokens()) return; // Invalid format;
				s = st.nextToken();
				try
				{
					Number min = sNumberFormat.parse(s);
					setOnly(min, MIN_ONLY);
				}
				catch (ParseException e)
				{
				}
			}
			else if ("max".equalsIgnoreCase(s))
			{
				try
				{
					if (!st.hasMoreTokens()) return; // Invalid format;
					s = st.nextToken();
					if (!st.hasMoreTokens()) return; // Invalid format;
					s = st.nextToken();
					Number max = sNumberFormat.parse(s);
					setOnly(max, MAX_ONLY);
				}
				catch (ParseException e)
				{
				}
			}
			else
			{
				try
				{
					// Should be number, number
					Number min = sNumberFormat.parse(s);
					if (!st.hasMoreTokens()) return; // Invalid format;				
					s = st.nextToken();
					if (!st.hasMoreTokens()) return; // Invalid format;
					s = st.nextToken();
					Number max = sNumberFormat.parse(s);
					setMinMax(min, max);
				}
				catch (ParseException e)
				{
				}
			}
		}
	}

	public void setMinMax(Number min, Number max)
	{
		fType = null;
		fMinValue = min;
		fMaxValue = max;
	}

	/**
	 * The type will be one the two above to indicate setting min only or max only.
	 */
	public void setOnly(int in, String type)
	{
		setOnly(new Long(in), type);
	}

	public void setOnly(Number in, String type)
	{
		if (MIN_ONLY.equals(type))
		{
			fType = MIN_ONLY;
			fMinValue = in;
		}
		else
		{
			fType = MAX_ONLY;
			fMaxValue = in;
		}
	}

	public String isValid(Object value)
	{
		if (value instanceof Number)
		{
			// Check for out of bounds Long values
			if (value == LONG_UNDERFLOW || value == LONG_OVERFLOW)
			{
				if (fType == null) return VALUE_OUT_OF_RANGE;
				else if (fType == MIN_ONLY) return (value != LONG_UNDERFLOW) ? null : VALUE_TOO_LOW;
				else return (value != LONG_OVERFLOW) ? null : VALUE_TOO_HIGH;
			}

			// We need to know whether a floating or integer because long has more precision
			// than double so we need to do comparisons as either double or longs.
			if (!(value instanceof Double || value instanceof Float))
			{
				// It is an integer type value
				long l = ((Number)value).longValue();
				if (fType == null) return (fMinValue.longValue() <= l && l <= fMaxValue.longValue()) ? null : VALUE_OUT_OF_RANGE;
				else if (fType == MIN_ONLY) return (fMinValue.longValue() <= l) ? null : VALUE_TOO_LOW;
				else return (l <= fMaxValue.longValue()) ? null : VALUE_TOO_HIGH;
			}
			else
			{
				// It is a floating type value
				double d = ((Number)value).doubleValue();
				if (fType == null) return (fMinValue.doubleValue() <= d && d <= fMaxValue.doubleValue()) ? null : VALUE_OUT_OF_RANGE;
				else if (fType == MIN_ONLY) return (fMinValue.doubleValue() <= d) ? null : VALUE_TOO_LOW;
				else return (d <= fMaxValue.doubleValue()) ? null : VALUE_TOO_HIGH;
			}
		}
		else if (value != null) return NOT_A_NUMBER;
		else return null;
	}

}
