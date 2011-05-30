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
package com.servoy.eclipse.model.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.FixedStyleSheet;

public class ModelUtils
{
	public static String[] getTokenElements(String value, String delim, boolean trim)
	{
		if (value == null)
		{
			return new String[] { };
		}

		List<String> lst = new ArrayList<String>();
		StringTokenizer tokemizer = new StringTokenizer(value, delim);
		while (tokemizer.hasMoreElements())
		{
			String token = tokemizer.nextToken();
			if (trim)
			{
				lst.add(token.trim());
			}
			else
			{
				lst.add(token);
			}
		}
		return lst.toArray(new String[lst.size()]);
	}

	public static String getTokenValue(Object[] value, String delim)
	{
		if (value == null || value.length == 0)
		{
			return null;
		}

		StringBuffer sb = new StringBuffer();
		for (Object o : value)
		{
			if (sb.length() > 0)
			{
				sb.append(delim);
			}
			sb.append(o instanceof String ? (String)o : "");
		}
		return sb.toString();
	}

	public static String getStyleLookupname(IPersist persist)
	{
		if (persist instanceof BaseComponent)
		{
			return ComponentFactory.getLookupName((BaseComponent)persist);
		}
		if (persist instanceof Form)
		{
			return "form"; //$NON-NLS-1$
		}
		return null;
	}

	public static String[] getStyleClasses(Style style, String lookupName, String formStyleClass)
	{
		if (lookupName == null || lookupName.length() == 0 || style == null)
		{
			return new String[0];
		}

		FixedStyleSheet styleSheet = ComponentFactory.getCSSStyle(style);
		String[] styleClassess = styleSheet.getCachedStyleClasses(lookupName, formStyleClass);
		if (styleClassess == null)
		{
			styleClassess = calculateStyleClasses(styleSheet, lookupName, formStyleClass);
			styleSheet.setCachedStyleClasses(lookupName, formStyleClass, styleClassess);
		}
		return styleClassess;
	}

	private static String[] calculateStyleClasses(FixedStyleSheet styleSheet, String lookupName, String formStyleClass)
	{
		List<String> styleClasses = new ArrayList<String>();
		boolean matchedFormPrefix = false;
		String formPrefix = "form"; //$NON-NLS-1$
		if (formStyleClass != null)
		{
			formPrefix += "." + formStyleClass; //$NON-NLS-1$
		}

		if (!lookupName.equals("form"))
		{
			boolean styleExist = false;
			boolean matchedFormPrefixField = false;
			Enumeration< ? > selectors = styleSheet.getStyleNames();
			while (selectors.hasMoreElements())
			{
				String[] styleParts = ((String)selectors.nextElement()).split("\\p{Space}+?"); //$NON-NLS-1$
				if (styleParts.length <= 2 && (styleParts.length == 1 || styleParts[0].equals(formPrefix)))
				{
					String styleName = styleParts[styleParts.length - 1];
					if (styleName.equals(lookupName))
					{
						matchedFormPrefix |= styleParts.length == 2; // found a match with form prefix, skip root matches 
						styleExist = true;
					}
					else if (styleName.startsWith(lookupName + '.'))
					{
						styleExist = true;
					}
					else if (styleName.equals("field"))
					{
						matchedFormPrefixField |= styleParts.length == 2;
					}
				}
			}
			if (!styleExist && lookupName.equals("check") || lookupName.equals("combobox") || lookupName.equals("radio"))
			{
				lookupName = "field"; //$NON-NLS-1$
				matchedFormPrefix = matchedFormPrefixField;
			}
		}

		Enumeration< ? > selectors = styleSheet.getStyleNames();
		while (selectors.hasMoreElements())
		{
			String selector = (String)selectors.nextElement();
			String[] styleParts = selector.split("\\p{Space}+?"); //$NON-NLS-1$
			String styleName = styleParts[styleParts.length - 1];
			if ((matchedFormPrefix && styleParts.length == 1) // found a match with form prefix, skip root matches 
				||
				styleParts.length > 2 || !styleName.startsWith(lookupName) || (styleParts.length == 2 && !styleParts[0].equals(formPrefix)))
			{
				continue;
			}

			int index = styleName.indexOf('.');
			if (index == lookupName.length() && styleName.startsWith(lookupName))
			{
				styleClasses.add(styleName.substring(index + 1));
			}
		}

		Collections.sort(styleClasses);

		return styleClasses.toArray(new String[styleClasses.size()]);
	}

	public static FlattenedSolution getEditingFlattenedSolution(IPersist persist)
	{
		if (persist == null) return null;

		Solution solution = (Solution)persist.getAncestor(IRepository.SOLUTIONS);
		if (solution == null) return null;

		ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solution.getName());
		if (servoyProject == null) return null;

		return servoyProject.getEditingFlattenedSolution();
	}

	public static FlattenedSolution getEditingFlattenedSolution(IPersist persist, IPersist context)
	{
		return getEditingFlattenedSolution(isInheritedFormElement(persist, context) ? context : persist);
	}

	/**
	 * Get a script method by id.
	 * 
	 * @param persist
	 * @param context
	 * @param table
	 * @param methodId
	 * @return
	 */
	public static IScriptProvider getScriptMethod(IPersist persist, IPersist context, Table table, int methodId)
	{
		if (methodId <= 0)
		{
			return null;
		}

		// is it a global method?
		FlattenedSolution editingFlattenedSolution = getEditingFlattenedSolution(persist);
		ScriptMethod sm = editingFlattenedSolution.getScriptMethod(methodId);
		if (sm != null)
		{
			return sm;
		}

		if (table != null)
		{
			try
			{
				Iterator<TableNode> tableNodes = editingFlattenedSolution.getTableNodes(table);
				while (tableNodes.hasNext())
				{
					TableNode tableNode = tableNodes.next();
					ScriptCalculation calc = AbstractBase.selectById(tableNode.getScriptCalculations().iterator(), methodId);
					if (calc != null)
					{
						return calc;
					}
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}

		// find the form method
		Form formBase = (Form)(context == null ? persist : context).getAncestor(IRepository.FORMS); // search via context if provided
		if (formBase == null)
		{
			// not a form method
			return null;
		}

		List<Form> formHierarchy = editingFlattenedSolution.getFormHierarchy(formBase);
		for (int i = 0; sm == null && i < formHierarchy.size(); i++)
		{
			sm = formHierarchy.get(i).getScriptMethod(methodId);
		}

		if (sm != null && !sm.getParent().equals(formBase))
		{
			// found form method by id, now find the actual implementation based on name (respecting form hierarchy)
			for (Form f : formHierarchy)
			{
				ScriptMethod formSm = f.getScriptMethod(sm.getName());
				if (formSm != null)
				{
					return formSm;
				}
			}
		}

		return sm;
	}

	public static <T> List<T> getExtensions(String extensionID)
	{
		List<T> ts = new ArrayList<T>();
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint(extensionID);
		IExtension[] extensions = ep.getExtensions();

		for (IExtension extension : extensions)
		{
			IConfigurationElement[] ces = extension.getConfigurationElements();
			for (IConfigurationElement ce : ces)
			{
				try
				{
					T t = (T)ce.createExecutableExtension("class"); //$NON-NLS-1$
					if (t != null)
					{
						ts.add(t);
					}
				}
				catch (CoreException e)
				{
					ServoyLog.logError("Could not load extension (extension point " + extensionID + ", " + ce.getAttribute("class") + ")", e);
				}
				catch (ClassCastException e)
				{
					ServoyLog.logError("Extension class has wrong type (extension point " + extensionID + ", " + ce.getAttribute("class") + ")", e);
				}
			}
		}
		return ts;
	}

	private static boolean uiRunning = true;

	public static boolean isUIRunning()
	{
		return uiRunning;
	}

	public static void setUIRunning(boolean running)
	{
		uiRunning = running;
	}

	public static boolean isInheritedFormElement(Object element, IPersist context)
	{
		if (element instanceof Form)
		{
			return false;
		}
		if (context instanceof Form && element instanceof IPersist && (((IPersist)element).getAncestor(IRepository.FORMS) != context))
		{
			if (element instanceof IPersist && (((IPersist)element).getAncestor(IRepository.FORMS) != context))
			{
				// child of super-form, readonly
				return true;
			}
		}
		if (element instanceof FormElementGroup)
		{
			Iterator<IFormElement> elements = ((FormElementGroup)element).getElements();
			while (elements.hasNext())
			{
				if (isInheritedFormElement(elements.next(), context))
				{
					return true;
				}
			}
		}
		if (element instanceof AbstractBase)
		{
			return ((AbstractBase)element).isOverrideElement();
		}
		// child of this form, not of a inherited form
		return false;
	}
}
