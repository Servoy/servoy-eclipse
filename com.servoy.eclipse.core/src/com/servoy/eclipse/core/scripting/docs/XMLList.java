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

@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "XMLList")
public class XMLList
{
	/**
	 * It calls the method attribute of each object in this XMLList and returns the results in order 
	 * in an XMLList.
	 *
	 * @sample xmlList.attribute(attributeName)
	 * 
	 * @param attributeName 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList attribute(String attributeName)
	{
		return null;
	}

	/**
	 * Calls the method attributes of each object in this XMLList and returns an XMLList with 
	 * the results in order.
	 *
	 * @sample xmlList.attributes()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList attributes()
	{
		return null;
	}

	/**
	 * Calls the method child of each XML object in this XMLList object to return an XMLList 
	 * with the matching children in order.
	 *
	 * @sample xmlList.child(propertyName)
	 * 
	 * @param propertyName 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList child(String propertyName)
	{
		return null;
	}

	/**
	 * Returns an XMLList with the children of all XML objects in this XMLList.
	 *
	 * @sample xmlList.children()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList children()
	{
		return null;
	}

	/**
	 * Returns an XMLList with all the comment child nodes of XML objects in this XMLList in order.
	 *
	 * @sample xmlList.comments()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList comments()
	{
		return null;
	}

	/**
	 * Returns true if there is (at least) one XML object in the list that compares equal to the value
	 *
	 * @sample xmlList.contains(value)
	 * 
	 * @param value 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean contains(Object value)
	{
		return null;
	}

	/**
	 * Returns a deep copy of the XMLList it is called on.
	 *
	 * @sample xmlList.copy()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList copy()
	{
		return null;
	}

	/**
	 * Returns an XMLList with all of the matching descendants of all XML objects.
	 *
	 * @sample xmlList.descendants([name])
	 * 
	 * @param name optional 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList descendants(String name)
	{
		return null;
	}

	/**
	 * Returns an XMLList with the matching element children of all XML objects in this XMLList.
	 *
	 * @sample xmlList.elements([name])
	 * 
	 * @param name 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList elements(String name)
	{
		return null;
	}

	/**
	 * Returns true if the XMLList contains exactly one XML object which has complex content or if 
	 * the XMLList contains several XML objects.
	 *
	 * @sample xmlList.hasComplexContent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean hasComplexContent()
	{
		return null;
	}

	/**
	 * Returns true if the XMLList object has a property of that name and false otherwise.
	 *
	 * @sample xmlList.hasOwnProperty(propertyName)
	 * 
	 * @param propertyName 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean hasOwnProperty(String propertyName)
	{
		return null;
	}

	/**
	 * Returns true if the XMLList is empty or contains exactly one XML object which has simple 
	 * content or contains no elements at all.
	 *
	 * @sample xmlList.hasSimpleContent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean hasSimpleContent()
	{
		return null;
	}

	/**
	 * Returns the number of XML objects this XMLList contains.
	 *
	 * @sample xmlList.length()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Number length()
	{
		return null;
	}

	/**
	 * Returns the XMLList object it is called on after joining adjacent text nodes 
	 * and removing empty text nodes.
	 *
	 * @sample xmlList.normalize()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList normalize()
	{
		return null;
	}

	/**
	 * Returns the common parent of all XML objects in this XMLList if all those objects 
	 * have the same parent.
	 *
	 * @sample xmlList.parent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML parent()
	{
		return null;
	}

	/**
	 * Returns an XMLList with all the matching processing instruction child nodes of all 
	 * XML objects in this XMLList.
	 *
	 * @sample xmlList.processingInstructions([name])
	 * 
	 * @param name optional
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList processingInstructions(String name)
	{
		return null;
	}

	/**
	 * Returns true if the property name converted to a number is greater than or equal to 
	 * 0 and less than the length of this XMLList.
	 *
	 * @sample xmlList.propertyIsEnumerable(propertyName)
	 * 
	 * @param propertyName 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean propertyIsEnumerable(String propertyName)
	{
		return null;
	}

	/**
	 * Returns an XMLList containing all the text child nodes of all the XML objects contained in this XMLList.
	 *
	 * @sample xmlList.text()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList text()
	{
		return null;
	}

	/**
	 * Returns a string representation of the XMLList
	 *
	 * @sample xmlList.toString()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	@Override
	public java.lang.String toString()
	{
		return null;
	}

	/**
	 * Returns the concatenation of toXMLString called on each XML object. The result for each XML 
	 * object is put on a separate line if XML.prettyPrinting is true.
	 *
	 * @sample xmlList.toXMLString()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public String toXMLString()
	{
		return null;
	}

	/**
	 * Simply returns the XMLList object it is called on.
	 *
	 * @sample xmlList.valueOf()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList valueOf()
	{
		return null;
	}
}
