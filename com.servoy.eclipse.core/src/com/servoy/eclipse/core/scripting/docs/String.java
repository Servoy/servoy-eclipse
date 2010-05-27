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

@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "String", scriptingName = "String")
public class String
{
	/**
	 * Gives the length of the string.
	 *
	 * @sample string.length;
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/length
	 */
	public Number length;

	/**
	 * returns a copy of the string embedded within an anchor &lt;A&gt; tag set.
	 *
	 * @sample string.anchor();
	 * 
	 * @param nameAttribute 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/anchor
	 */
	public String anchor(String nameAttribute)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;BIG&gt; tag set.
	 *
	 * @sample string.big();
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/big
	 */
	public String big()
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;BLINK&gt; tag set.
	 *
	 * @sample string.blink();
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/blink
	 */
	public String blink()
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;B&gt; tag set.
	 *
	 * @sample string.bold();
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/bold
	 */
	public String bold()
	{
		return null;
	}

	/**
	 * returns a character of the string.
	 *
	 * @sample string.charAt(integer position);
	 * 
	 * @param index 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/charAt
	 */
	public Number charAt(Number index)
	{
		return null;
	}

	/**
	 * returns a decimal code of the char in the string.
	 *
	 * @sample string.charCodeAt(integer position);
	 * 
	 * @param index 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/charCodeAt
	 */
	public Number charCodeAt(Number index)
	{
		return null;
	}

	/**
	 * returns a string that appends the parameter string to the string.
	 *
	 * @sample string.concat(string);
	 * 
	 * @param string2 
	 * @param stringN optional 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/concat
	 */
	public String concat(String string2, String stringN)
	{
		return null;
	}

	/**
	 * returns a boolean that checks if the given string is equal to the string
	 *
	 * @sample string.equals(string);
	 * 
	 * @param other 
	 */
	public Boolean equals(String other)
	{
		return null;
	}

	/**
	 * returns a boolean that checks if the given string is equal to the string ignoring case
	 *
	 * @sample string.equalsIgnoreCase(string);
	 * 
	 * @param other 
	 */
	public Boolean equalsIgnoreCase(String other)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an anchor &lt;TT&gt; tag set.
	 *
	 * @sample string.fixed();
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/fixed
	 */
	public String fixed()
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;FONT&gt; tag set, the color param is assigned the the color attribute.
	 *
	 * @sample string.fontcolor(color);
	 * 
	 * @param color 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/fontcolor
	 */
	public String fontcolor(String color)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;FONT&gt; tag set, The size param is set to the SIZE attribute
	 *
	 * @sample string.fontsize(size);
	 * 
	 * @param size 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/fontsize
	 */
	public String fontsize(Number size)
	{
		return null;
	}

	/**
	 * returns the found index of the given string in string.
	 *
	 * @sample string.indexOf(string,startPosition);
	 * 
	 * @param searchValue 
	 * @param fromIndex 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/indexOf
	 */
	public Number indexOf(String searchValue, Number fromIndex)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;I&gt; tag set
	 *
	 * @sample string.italics();
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/italics
	 */
	public String italics()
	{
		return null;
	}

	/**
	 * returns the found index of the given string in string from the end.
	 *
	 * @sample string.lastIndexOf(string,startPosition);
	 * 
	 * @param searchValue 
	 * @param fromIndex 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/lastIndexOf
	 */
	public Number lastIndexOf(String searchValue, Number fromIndex)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;A&gt; tag set.
	 *
	 * @sample string.link(url);
	 * 
	 * @param hrefAttribute 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/link
	 */
	public String link(String hrefAttribute)
	{
		return null;
	}

	/**
	 * returns an array of strings within the current string that matches the regexp.
	 *
	 * @sample string.match(regexpr);
	 * 
	 * @param regexp 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/match
	 */
	public Array match(RegExp regexp)
	{
		return null;
	}

	/**
	 * returns a strings where all matches of the regexp are replaced.
	 *
	 * @sample string.replace(regexpr,replacestring);
	 * 
	 * @param regexp 
	 * @param newSubStr 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/replace
	 */
	public String replace(RegExp regexp, String newSubStr)
	{
		return null;
	}

	/**
	 * returns a index where the first match is found of the regexp
	 *
	 * @sample string.search(regexpr);
	 * 
	 * @param regexp 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/search
	 */
	public Number search(RegExp regexp)
	{
		return null;
	}

	/**
	 * returns a substring of the string.
	 *
	 * @sample string.slice(start [,end]);
	 * 
	 * @param beginSlice 
	 * @param endSlice optional
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/slice
	 */
	public String slice(Number beginSlice, Number endSlice)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;SMALL&gt; tag set.
	 *
	 * @sample string.small();
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/small
	 */
	public String small()
	{
		return null;
	}

	/**
	 * returns an array of objects whose elements are segments of the current string.
	 *
	 * @sample string.split(delimiter [,limitInteger]);
	 * 
	 * @param separator 
	 * @param limit 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/split
	 */
	public String split(String separator, Number limit)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;STRIKE&gt; tag set.
	 *
	 * @sample string.strike();
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/strike
	 */
	public String strike()
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;SUB&gt; tag set.
	 *
	 * @sample string.sub();
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/sub
	 */
	public String sub()
	{
		return null;
	}

	/**
	 * returns a substring of the string from the start with the number of chars specified.
	 *
	 * @sample string.substr(start [,number of chars]);
	 * 
	 * @param start 
	 * @param length optional
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/substr
	 */
	public String substr(Number start, Number length)
	{
		return null;
	}

	/**
	 * Returns a substring of the string from the start index until the end index.
	 *
	 * @sample string.substring(start [,end]);
	 * 
	 * @param indexA 
	 * @param indexB optional
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/substring
	 */
	public String substring(Number indexA, Number indexB)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;SUP&gt; tag set.
	 *
	 * @sample string.sup();
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/sup
	 */
	public String sup()
	{
		return null;
	}

	/**
	 * returns a string with all lowercase letters of the current string.
	 *
	 * @sample string.toLowerCase();
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/toLowerCase
	 */
	public String toLowerCase()
	{
		return null;
	}

	/**
	 * returns a string with all uppercase letters of the current string.
	 *
	 * @sample string.toUpperCase();
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/toUpperCase
	 */
	public String toUpperCase()
	{
		return null;
	}
}
