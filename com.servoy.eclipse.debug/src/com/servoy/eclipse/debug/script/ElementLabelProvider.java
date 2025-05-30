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

import org.eclipse.dltk.core.CompletionProposal;
import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Parameter;
import org.eclipse.dltk.javascript.typeinfo.model.ParameterKind;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.ui.typeinfo.IElementLabelProvider;
import org.eclipse.dltk.javascript.ui.typeinfo.IElementLabelProviderExtension;
import org.eclipse.dltk.ui.text.completion.ScriptCompletionProposalCollector;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.resource.ImageDescriptor;

import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.IExecutingEnviroment;

/**
 * Extension point implementation of {@link IElementLabelProvider} that returns the {@link TypeCreator#IMAGE_DESCRIPTOR} attribute
 * if set from a element
 *
 * @author jcompagner
 * @since 6.0
 */
public class ElementLabelProvider implements IElementLabelProviderExtension
{
	private final Set<String> propertyNames = new HashSet<String>();

	public ElementLabelProvider()
	{
		propertyNames.add("controller");
		propertyNames.add("currentcontroller");
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_APPLICATION);
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_I18N);
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_HISTORY);
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_MENUS);
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_EVENTTYPES);
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_JSPERMISSION);
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_UTILS);
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_CLIENTUTILS);
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_JSUNIT);
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER);
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER);
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_DATASOURCES);
		propertyNames.add("servoyDeveloper");
		propertyNames.add("developerBridge");
		propertyNames.add(IExecutingEnviroment.TOPLEVEL_SECURITY);
		propertyNames.add("elements");
		propertyNames.add(ScriptVariable.SCOPES);
	}

	public ImageDescriptor getImageDescriptor(Element element)
	{
		return (ImageDescriptor)element.getAttribute(TypeCreator.IMAGE_DESCRIPTOR);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.dltk.javascript.ui.typeinfo.IElementLabelProviderExtension#getLabel(org.eclipse.dltk.javascript.typeinfo.model.Element,
	 * org.eclipse.dltk.javascript.ui.typeinfo.IElementLabelProvider.Mode, java.lang.Object)
	 */
	public String getLabel(Element element, Mode mode, Object context)
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

			int paramCount = Integer.MAX_VALUE;
			if (context instanceof CompletionProposal)
			{
				Integer paramLimit = (Integer)((CompletionProposal)context).getAttribute(ScriptCompletionProposalCollector.ATTR_PARAM_LIMIT);
				if (paramLimit != null)
				{
					paramCount = paramLimit.intValue();
				}
			}
			// parameters
			nameBuffer.append('(');
			if (paramCount != Integer.MAX_VALUE)
			{
				EList<Parameter> parameters = method.getParameters();
				for (int i = 0; i < paramCount; i++)
				{
					Parameter parameter = parameters.get(i);
					if (nameBuffer.charAt(nameBuffer.length() - 1) != '(') nameBuffer.append(", ");
					nameBuffer.append(parameter.getName());
					if (parameter.getKind() == ParameterKind.VARARGS)
					{
						nameBuffer.append("...");
					}
				}
			}
			else
			{
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
			}
			nameBuffer.append(')');

			if (method.getType() != null)
			{
				nameBuffer.append(": ");
				nameBuffer.append(method.getType().getName());
			}
			return nameBuffer.toString();
		}
		else if (element instanceof Type)
		{
			// for custom types that can be packagename.typename it should not change the name based on .
			if (element.getName().startsWith(ElementUtil.CUSTOM_TYPE)) return null;
			int lastDotIndex = element.getName().lastIndexOf('.');
			if (lastDotIndex != -1)
			{
				String name = element.getName().substring(lastDotIndex + 1);
				return name + " - " + element.getName().substring(0, lastDotIndex);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.dltk.javascript.ui.typeinfo.IElementLabelProvider#getLabel(org.eclipse.dltk.javascript.typeinfo.model.Element,
	 * org.eclipse.dltk.javascript.ui.typeinfo.IElementLabelProvider.Mode)
	 */
	public String getLabel(Element element, Mode mode)
	{
		return getLabel(element, mode, null);
	}
}
