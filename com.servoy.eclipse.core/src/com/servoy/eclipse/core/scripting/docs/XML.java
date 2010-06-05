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

@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "XML", scriptingName = "XML")
public class XML
{
	/**
	 * If set to true, then comments in the XML is ignored when constructing new XML objects.
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean ignoreComments;

	/**
	 * If set to true, then processing instructions are ignored when constructing new XML objects.
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean ignoreProcessingInstructions;

	/**
	 * If set to true, then whitespace in the XML is ignored when constructing new XML objects.
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean ignoreWhitespace;

	/**
	 * If set to true, then toString() and toXMLString() methods will normalize the output
	 * to achieve a uniform appearance.
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean prettyPrinting;

	/**
	 * The amount of positions used when indenting child nodes relative to their parent
	 * if prettyPrinting is enabled.
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean prettyIndent;

	/**
	 * Takes one argument which can be a string with a namespace URI or a Namespace object and adds the 
	 * argument to the in scope namespaces of this XML object.
	 *
	 * @sample xml.addNamespace(namespaceToAdd)
	 * 
	 * @param namespaceToAdd 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML addNamespace(String namespaceToAdd)
	{
		return null;
	}

	/**
	 * Appends a new child at the end of this XML object's properties, the changed XML object is then returned.
	 *
	 * @sample xml.appendChild(childToAppend)
	 * 
	 * @param childToAppend 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML appendChild(XML childToAppend)
	{
		return null;
	}

	/**
	 * Takes a single argument with the attribute name and returns an XMLList with attributes 
	 * matching the argument.
	 *
	 * @sample xml.attribute(attributeName)
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
	 * Returns an XMLList with the attributes of this XML object which are in no namespace.
	 *
	 * @sample xml.attributes()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList attributes()
	{
		return null;
	}

	/**
	 * Returns an XMLList with children matching the property name.
	 *
	 * @sample xml.child(childPropertyName)
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
	 * If the XML object has no parent then the special number NaN is returned, otherwise the ordinal 
	 * position the object has in the context of its parent is returned.
	 *
	 * @sample xml.childIndex()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Number childIndex()
	{
		return null;
	}

	/**
	 * Returns an XMLList with the child nodes of this XML object.
	 *
	 * @sample xml.children()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList children()
	{
		return null;
	}

	/**
	 * Returns an XMLList with the comment nodes which are children of this XML object.
	 *
	 * @sample xml.comments()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList comments()
	{
		return null;
	}

	/**
	 * Calling xmlObject.contains(value) yields the same result as the equality comparison xmlObject == value
	 *
	 * @sample xml.contains(value)
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
	 * Returns a deep copy of the XML object it is called on where the internal parent property is set to null
	 *
	 * @sample xml.copy()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML copy()
	{
		return null;
	}

	/**
	 * Returns an object containing the default XML settings.
	 * 
	 * @sample xml.defaultSettings()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Object defaultSettings()
	{
		return null;
	}

	/**
	 * Returns an XMLList with the descendants matching the passed name argument or with all descendants 
	 * if no argument is passed.
	 *
	 * @sample xml.descendants([name])
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
	 * Takes one optional argument, the name of elements you are looking for, and returns an XMLList with 
	 * all matching child elements.
	 *
	 * @sample xml.elements([name])
	 * 
	 * @param name optional 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList elements(String name)
	{
		return null;
	}

	/**
	 * Returns false for XML objects of node kind 'text', 'attribute', 'comment', and 'processing-instruction'.
	 * For objects of kind 'element' it checks whether the element has at least one child element.
	 *
	 * @sample xml.hasComplexContent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean hasComplexContent()
	{
		return null;
	}

	/**
	 * Returns true if the XML object the method is called on has a property of that name.
	 *
	 * @sample xml.hasOwnProperty(propertyName)
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
	 * Returns true for XML objects of node kind text or attribute. For XML objects of node kind 
	 * element it returns true if the element has no child elements and false otherwise.
	 * For other node kinds (comment, processing instruction) the method always returns false.
	 *
	 * @sample xml.hasSimpleContent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Boolean hasSimpleContent()
	{
		return null;
	}

	/**
	 * Returns an array of Namespace objects representing the namespace that are in scope for this XML object.
	 *
	 * @sample xml.inScopeNamespaces()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Array inScopeNamespaces()
	{
		return null;
	}

	/**
	 * Takes two arguments, an existing child to insert after and the new child to be inserted.
	 * If the first argument is null then the second argument is inserted as the first child of this XML.
	 * 
	 * @sample xml.insertChildAfter(childToInsertAfter, childToInsert)
	 * 
	 * @param childToInserAfter 
	 * @param childToInsert 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML insertChildAfter(XML childToInserAfter, XML childToInsert)
	{
		return null;
	}

	/**
	 * Takes two arguments, an existing child to insert before and the new child to be inserted.
	 * If the first argument is null then the child is inserted as the last child.
	 *
	 * @sample xml.insertChildBefore(childToInsertBefore, childToInsert)
	 * 
	 * @param childToInsertBefore 
	 * @param childToInsert 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML insertChildBefore(XML childToInsertBefore, XML childToInsert)
	{
		return null;
	}

	/**
	 * This always returns 1. This is done to blur the distinction between an XML object and an XMLList 
	 * containing exactly one value.
	 *
	 * @sample xml.length()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Number length()
	{
		return null;
	}

	/**
	 * returns the local name part if the XML object has a name.
	 *
	 * @sample xml.localName()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public String localName()
	{
		return null;
	}

	/**
	 * Returns the qualified name (a QName object) of the XML object it is called
	 *
	 * @sample xml.name()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public QName name()
	{
		return null;
	}

	/**
	 * If no argument is passed to the method then it returns the namespace associated with the qualified 
	 * name of this XML object. If a prefix is passed to the method then it looks for a matching namespace 
	 * in the in scope namespace of this XML object and returns it when found, otherwise undefined is returned.
	 *
	 * @sample xml.namespace([prefix])
	 * 
	 * @param prefix optional
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Namespace namespace(String prefix)
	{
		return null;
	}

	/**
	 * Returns an array with the namespace declarations associated with the XML object it is called on.
	 *
	 * @sample xml.namespaceDeclarations()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Array namespaceDeclarations()
	{
		return null;
	}

	/**
	 * Returns a string denoting the kind of node this XML object represents. Possible values: 'element', 
	 * 'attribute', 'text', 'comment', 'processing-instruction'.
	 *
	 * @sample xml.nodeKind()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public String nodeKind()
	{
		return null;
	}

	/**
	 * Returns this XML object after normalizing all text content.
	 *
	 * @sample xml.normalize()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML normalize()
	{
		return null;
	}

	/**
	 * Returns the parent XML object of this XML object or null if there is no parent.
	 *
	 * @sample xml.parent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML parent()
	{
		return null;
	}

	/**
	 * Iinserts the given value as the first child of the XML object and returns the XML object.
	 *
	 * @sample xml.prependChild(childToPrepend)
	 * 
	 * @param childToPrepend 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML prependChild(XML childToPrepend)
	{
		return null;
	}

	/**
	 * If no argument is passed in then the method returns an XMLList with all the children of the XML 
	 * object which are processing instructions. If an argument is passed in then the method returns an 
	 * XMLList with all children of the XML object which are processing instructions where the name 
	 * matches the argument.
	 *
	 * @sample xml.processingInstructions([name])
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
	 * Returns true if the property name is '0' and false otherwise.
	 *
	 * @sample xml.propertyIsEnumerable(propertyName)
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
	 * Removes the namespace from the in scope namespaces of this XML object if the namespace 
	 * is not used for the qualified name of the object or its attributes.
	 *
	 * @sample xml.removeNamespace(namespace)
	 * 
	 * @param namespace 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML removeNamespace(Namespace namespace)
	{
		return null;
	}

	/**
	 * Takes two arguments, the property name of the property / properties to be replaced, and the 
	 * value to replace the properties.
	 *
	 * @sample xml.replace(propertyName, replacementValue)
	 * 
	 * @param propertyName 
	 * @param replacementValue 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML replace(String propertyName, XML replacementValue)
	{
		return null;
	}

	/**
	 * Replaces all children of the XML object with this value. The method returns the XML object it 
	 * is called on.
	 *
	 * @sample xml.setChildren(value)
	 * 
	 * @param value 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML setChildren(Object value)
	{
		return null;
	}

	/**
	 * Changes the local name of this XML object to the name passed in.
	 *
	 * @sample xml.setLocalName(name)
	 * 
	 * @param name 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public void setLocalName(String name)
	{
	}

	/**
	 * Replaces the name of this XML object with the name passed in.
	 *
	 * @sample xml.setName(name)
	 * 
	 * @param name 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public void setName(String name)
	{
	}

	/**
	 * Changes the namespace associated with the name of this XML object to the new namespace.
	 *
	 * @sample xml.setNamespace(namespace)
	 * 
	 * @param namespace 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public void setNamespace(Namespace namespace)
	{
	}

	/**
	 * Returns an object containing the global XML settings.
	 * 
	 * @sample xml.settings()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public Object settings()
	{
		return null;
	}

	/**
	 * Allows the global XML settings to be adjusted or restored to their default values.
	 * 
	 * @param settings optional The new settings that should be applied globally to the XML object.

	 * @sample xml.setSettings(settings)
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public void setSettings(Object settings)
	{
	}

	/**
	 * Returns an XMLList with all the children of this XML object that represent text nodes.
	 *
	 * @sample xml.text()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XMLList text()
	{
		return null;
	}

	/**
	 * Returns a convenient string value of this XML object.
	 *
	 * @sample xml.toString()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	@Override
	public java.lang.String toString()
	{
		return null;
	}

	/**
	 * Returns a string with the serialized XML markup for this XML object. XML.prettyPrinting 
	 * and XML.prettyIndent settings affect the returned string.
	 *
	 * @sample xml.toXMLString()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public String toXMLString()
	{
		return null;
	}

	/**
	 * The method simply returns the XML object it is called on.
	 *
	 * @sample xml.valueOf()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-357.pdf
	 */
	public XML valueOf()
	{
		return null;
	}
}
