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
package com.servoy.build.documentation;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.servoy.j2db.documentation.DocumentationUtil;
import com.servoy.j2db.documentation.IParameterDocumentation;


public class ParameterDocumentation implements IParameterDocumentation
{
	// top level tags
	private static final String TAG_PARAMETER = "parameter";

	// top level attributes
	private static final String ATTR_OPTIONAL = "optional";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_TYPECODE = "typecode";

	private static final String TAG_DESCRIPTION = "description";

	private final String name;
	private final Class< ? > type;
	private final String description;
	private final boolean optional;

	public ParameterDocumentation(String name, Class< ? > type, String description, boolean optional)
	{
		this.name = name;
		this.type = type;
		this.description = description;
		this.optional = optional;
	}

	public String getName()
	{
		return name;
	}

	public Class< ? > getType()
	{
		return type;
	}

	public String getDescription()
	{
		return description;
	}

	public boolean isOptional()
	{
		return optional;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("[pardoc:");
		sb.append("name=").append(name);
		sb.append(",type=").append(type != null ? type.getCanonicalName() : "N/A");
		sb.append(",desc=").append(description);
		sb.append(",opt=").append(optional);
		sb.append("]");
		return sb.toString();
	}

	public Element toXML(boolean pretty)
	{
		Element argumentElement = DocumentHelper.createElement(TAG_PARAMETER);

		argumentElement.addAttribute(ATTR_NAME, getName());
		if (isOptional()) argumentElement.addAttribute(ATTR_OPTIONAL, Boolean.TRUE.toString());
		if (getType() != null)
		{
			argumentElement.addAttribute(ATTR_TYPE, emitClass(getType()));
			if (!pretty)
			{
				argumentElement.addAttribute(ATTR_TYPECODE, getType().getName());
			}
		}

		if (getDescription() != null && getDescription().trim().length() > 0) argumentElement.addElement(TAG_DESCRIPTION).addCDATA(getDescription());

		return argumentElement;
	}

	public static ParameterDocumentation fromXML(Element argumentElement, ClassLoader loader)
	{
		if (!TAG_PARAMETER.equals(argumentElement.getName())) return null;

		String name = argumentElement.attributeValue(ATTR_NAME);
		String typeCode = argumentElement.attributeValue(ATTR_TYPECODE);
		Class< ? > type = null;
		try
		{
			type = DocumentationUtil.loadClass(loader, typeCode);
		}
		catch (Throwable e)
		{
			System.out.println("Cannot decode class from '" + typeCode + "' at parameter " + name + ".");
		}
		boolean optional = Boolean.TRUE.toString().equals(argumentElement.attributeValue(ATTR_OPTIONAL));

		String description = argumentElement.elementText(TAG_DESCRIPTION);

		ParameterDocumentation pd = new ParameterDocumentation(DocumentationManager.getInternedText(name), type,
			DocumentationManager.getInternedText(description), optional);
		return pd;
	}

	public static String emitClass(Class< ? > clazz)
	{
		return DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSDocumentedJavaClassName(clazz);
	}

	public static void main(String[] args)
	{
		String s = Integer.TYPE.getName();
		System.out.println(s);
		Class< ? > c;
		try
		{
			c = Class.forName(s);
			System.out.println(c.getCanonicalName());
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
	}
}
