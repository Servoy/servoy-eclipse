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
package com.servoy.eclipse.core.scripting.docs;

import com.servoy.j2db.annotations.ServoyDocumented;

@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Number", scriptingName = "Number")
public class Number
{
	/**
	 * The largest representable number. 
	 * 
	 * @sample
	 * application.output("Largest number: " + Number.MAX_VALUE);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/MAX_VALUE
	 */
	public Number MAX_VALUE;

	/**
	 * The smallest representable number. 
	 * 
	 * @sample
	 * application.output("Smallest number: " + Number.MIN_VALUE);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/MIN_VALUE
	 */
	public Number MIN_VALUE;

	/**
	 * Special "not a number" value.
	 * 
	 * @sample
	 * application.output("NaN: " + Number.NaN);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/NaN
	 */
	public Object NaN;

	/**
	 * Special value representing negative infinity; returned on overflow.
	 * 
	 * @sample
	 * application.output("Negative infinity: " + Number.NEGATIVE_INFINITY);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/NEGATIVE_INFINITY
	 */
	public Number NEGATIVE_INFINITY;

	/**
	 * Special value representing infinity; returned on overflow.
	 * 
	 * @sample
	 * application.output("Positive infinity: " + Number.POSITIVE_INFINITY);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/POSITIVE_INFINITY
	 */
	public Number POSITIVE_INFINITY;

	/**
	 * Returns a string representing the number in fixed-point notation. 
	 *
	 * @sample
	 * var n = 123.45678;
	 * application.output(n.toFixed(3));
	 * 
	 * @param digits optional The number of digits to appear after the decimal point. Defaults to 0.
	 * 
	 * @return A string representing the number in fixed-point notation.
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/toFixed
	 */
	public String toFixed(Number digits)
	{
		return null;
	}

	/**
	 * Returns a string representing the number in exponential notation. 
	 * 
	 * @sample
	 * var n = 123.45678;
	 * application.output(n.toExponential(3));
	 * 
	 * @param fractionDigits optional An integer specifying the number of digits after the decimal point. Defaults to as many digits as necessary to specify the number. 
	 * 
	 * @return A string representing the number in exponential notation. 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/toExponential
	 */
	public String toExponential(Number fractionDigits)
	{
		return null;
	}

	/**
	 * Returns a string representing the number to a specified precision in fixed-point or exponential notation. 
	 * 
	 * @sample
	 * var n = 123.45678;
	 * application.output(n.toPrecision(5));
	 * 
	 * @param precision optional An integer specifying the number of significant digits.
	 * 
	 * @return A string representing the number to a specified precision in fixed-point or exponential notation.
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/toPrecision
	 */
	public String toPrecision(Number precision)
	{
		return null;
	}
}
