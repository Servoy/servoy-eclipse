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
package com.servoy.eclipse.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.FixedStyleSheet;

public class CoreUtils
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

	public static String[] getStyleClasses(Style style, String lookupName)
	{
		List<String> styleClasses = new ArrayList<String>();
		if (style != null)
		{
			FixedStyleSheet styleSheet = ComponentFactory.getCSSStyle(style);

			if (lookupName != null && (lookupName.equals("check") || lookupName.equals("combobox") || lookupName.equals("radio")))
			{
				boolean styleExist = false;
				Enumeration selectors = styleSheet.getStyleNames();
				while (selectors.hasMoreElements())
				{
					String styleName = (String)selectors.nextElement();
					if (styleName.startsWith(lookupName))
					{
						styleExist = true;
						break;
					}
				}
				if (!styleExist) lookupName = "field";
			}

			Enumeration< ? > selectors = styleSheet.getStyleNames();
			while (selectors.hasMoreElements())
			{
				String styleName = (String)selectors.nextElement();
				int index = styleName.indexOf(".");
				if (index > 0)
				{
					if (lookupName == null || styleName.startsWith(lookupName))
					{
						styleName = styleName.substring(index + 1);
						styleClasses.add(styleName);
					}
				}
			}
			Collections.sort(styleClasses);
		}
		return styleClasses.toArray(new String[styleClasses.size()]);
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
		FlattenedSolution editingFlattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(persist);
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

}
