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

import com.servoy.j2db.documentation.ServoyDocumented;

@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "JS Lib")
public class JSLib
{
	/**
	 * Numeric value representing infinity.
	 * 
	 * @sample Infinity
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Properties/Infinity
	 */
	public Number Infinity;

	/**
	 * Value representing Not-a-Number. 
	 * 
	 * @sample NaN
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Properties/NaN
	 */
	public Number NaN;

	/**
	 * The value undefined.
	 * 
	 * @sample undefined
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Properties/undefined
	 */
	public Object undefined;

	/**
	 * Decodes a URI previously encoded with encodeURI or another similar routine.
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Functions/decodeURI
	 */
	public String decodeURI(String encodedURI)
	{
		return null;
	}

	/**
	 * 
	 * @param encodedURI
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Functions/decodeURIComponent
	 */
	public String decodeURIComponent(String encodedURI)
	{
		return null;
	}

	/**
	 * Encodes a URI by replacing certain characters with escape sequences.
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Functions/encodeURI
	 */
	public String encodeURI(String URI)
	{
		return null;
	}

	/**
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Functions/encodeURIComponent
	 */
	public String encodeURIComponent(String str)
	{
		return null;
	}

	/**
	 * Returns the hexadecimal encoding of a given string.
	 * 
	 * @sample
	 * var encoded = escape("Hello World!");
	 * application.output(encoded); // prints: Hello%20World%21
	 * application.output(unescape(encoded)); // prints: Hello World!
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Guide/Predefined_Functions/escape_and_unescape_Functions
	 */
	@Deprecated
	public String escape(String str)
	{
		return null;
	}

	/**
	 * Evaluates JavaScript code passed as a string. Returns the value returned by the evaluated code.
	 * 
	 * @sample
	 * eval("var x = 2 + 3;");
	 * application.output(x); // prints: 5.0
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Functions/eval
	 */
	public Object eval(String s)
	{
		return null;
	}


	/**
	 * Returns true if the given number is a finite number.
	 * 
	 * @sample
	 * application.output(isFinite(1)); // prints: true
	 * application.output(isFinite(Infinity)); // prints: false
	 * application.output(isFinite(isNaN)); // prints: false
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Functions/isFinite
	 */
	public Boolean isFinite(Number n)
	{
		return null;
	}

	/**
	 * The NaN property indicates that a value is 'Not a Number'.
	 *
	 * @sample isNaN( value )
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Guide/Predefined_Functions/isNaN_Function
	 */
	public void isNaN()
	{
	}

	/**
	 * Returns true if the given name can be used as a valid name for an XML element or attribute.
	 * 
	 * @sample
	 * application.output(isXMLName("good_name")); // prints: true
	 * application.output(isXMLName("bad name")); // because of the space, prints: false
	 *
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean isXMLName(String name)
	{
		return null;
	}

	/**
	 * Makes a floating point number from the starting numbers in a given string.
	 *
	 * @sample parseFloat('string')
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Guide/Predefined_Functions/parseInt_and_parseFloat_Functions
	 */
	public void parseFloat()
	{
	}

	/**
	 * Makes a integer from the starting numbers in a given string in the base specified.
	 *
	 * @sample parseInt( 'string' [, base] )
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Guide/Predefined_Functions/parseInt_and_parseFloat_Functions
	 */
	public void parseInt()
	{
	}

	/**
	 * Returns the ASCII encoding of a string that was previously encoded with escape or another similar routine.
	 * 
	 * @sample
	 * var encoded = escape("Hello World!");
	 * application.output(encoded); // prints: Hello%20World%21
	 * application.output(unescape(encoded)); // prints: Hello World!
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Guide/Predefined_Functions/escape_and_unescape_Functions
	 */
	@Deprecated
	public String unescape(String str)
	{
		return null;
	}

	/**
	 * Returns the string representation behind a given object.
	 * 
	 * @sample
	 * application.output(uneval(isNaN)); // prints something like: function isNaN() { [native code for isNaN, arity=1] }
	 */
	public String uneval(Object obj)
	{
		return null;
	}
}
