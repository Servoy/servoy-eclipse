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
package com.servoy.eclipse.debug.script;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Parameter;
import org.eclipse.dltk.javascript.typeinfo.model.ParameterKind;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.ui.typeinfo.IElementLabelProvider;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Extension point implementation of {@link IElementLabelProvider} that returns the {@link TypeCreator#IMAGE_DESCRIPTOR} attribute
 * if set from a element
 * 
 * @author jcompagner
 * @since 6.0
 */
@SuppressWarnings("nls")
public class ElementLabelProvider implements IElementLabelProvider
{
	private final Set<String> propertyNames = new HashSet<String>();

	public ElementLabelProvider()
	{
		propertyNames.add("controller");
		propertyNames.add("currentcontroller");
		propertyNames.add("application");
		propertyNames.add("i18n");
		propertyNames.add("history");
		propertyNames.add("utils");
		propertyNames.add("jsunit");
		propertyNames.add("solutionModel");
		propertyNames.add("databaseManager");
		propertyNames.add("servoyDeveloper");
		propertyNames.add("security");
		propertyNames.add("elements");
	}

	public ImageDescriptor getImageDescriptor(Element element)
	{
		return (ImageDescriptor)element.getAttribute(TypeCreator.IMAGE_DESCRIPTOR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.ui.typeinfo.IElementLabelProvider#getLabel(org.eclipse.dltk.javascript.typeinfo.model.Element,
	 * org.eclipse.dltk.javascript.ui.typeinfo.IElementLabelProvider.Mode)
	 */
	public String getLabel(Element element, Mode mode)
	{
		if (element instanceof Property)
		{
			if (propertyNames.contains(element.getName())) return element.getName();
			Property property = (Property)element;
			if (property.getType() != null)
			{
				final StringBuilder sb = new StringBuilder();
				sb.append(property.getName());
				sb.append(": ");
				sb.append(property.getType().getName());
				return sb.toString();
			}
		}
		else if (element instanceof Method)
		{
			Method method = (Method)element;
			StringBuilder nameBuffer = new StringBuilder();

			// method name
			nameBuffer.append(method.getName());

			// parameters
			nameBuffer.append('(');
			for (Parameter parameter : method.getParameters())
			{
				if (nameBuffer.charAt(nameBuffer.length() - 1) != '(') nameBuffer.append(", ");
				if (parameter.getKind() == ParameterKind.OPTIONAL)
				{
					nameBuffer.append('[');
				}
				nameBuffer.append(parameter.getName());
				if (parameter.getKind() == ParameterKind.VARARGS)
				{
					nameBuffer.append("...");
				}

				if (parameter.getKind() == ParameterKind.OPTIONAL)
				{
					nameBuffer.append(']');
				}
			}
			nameBuffer.append(')');

			if (method.getType() != null)
			{
				nameBuffer.append(": ");
				nameBuffer.append(method.getType().getName());
			}
			return nameBuffer.toString();
		}
		return null;
	}
}
