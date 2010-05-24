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
import java.util.Iterator;
import java.util.Set;

import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.scripting.GroupScriptObject;
import com.servoy.j2db.smart.dataui.SwingItemFactory;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * 
 */
public class ElementScope extends DefaultScope
{
	private final ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

	private final Form form;

	/**
	 * @param parent
	 * @param form
	 */
	public ElementScope(Scriptable parent, Form form)
	{
		super(parent);
		this.form = form;
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getClassName()
	 */
	@Override
	public String getClassName()
	{
		return ElementScope.class.getName();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		Set<String> al = new HashSet<String>();

		al.add("allnames"); //$NON-NLS-1$
		al.add("length"); //$NON-NLS-1$

		if (form != null)
		{
			Form formToUse = form;
			try
			{
				formToUse = servoyModel.getFlattenedSolution().getFlattenedForm(form);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
			Iterator<IPersist> formObjects = formToUse.getAllObjects();
			while (formObjects.hasNext())
			{
				IPersist persist = formObjects.next();
				if (persist instanceof IFormElement)
				{
					IFormElement formElement = (IFormElement)persist;
					if (!Utils.stringIsEmpty(formElement.getName()))
					{
						al.add(formElement.getName());
					}
					if (formElement.getGroupID() != null)
					{
						String groupName = FormElementGroup.getName(formElement.getGroupID());
						if (groupName != null)
						{
							al.add(groupName);
						}
					}
				}
			}
		}
		return al.toArray();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		if (form != null)
		{
			Form formToUse = form;
			try
			{
				formToUse = servoyModel.getFlattenedSolution().getFlattenedForm(form);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
			IApplication application = Activator.getDefault().getDesignClient();
			Iterator<IPersist> formObjects = formToUse.getAllObjects();
			while (formObjects.hasNext())
			{
				IPersist persist = formObjects.next();
				if (persist instanceof IFormElement)
				{
					IFormElement formElement = ((IFormElement)persist);
					if (name.equals(formElement.getName()))
					{
						return new ScriptObjectClassScope(this, SwingItemFactory.getPersistClass(application, persist), "elements." + name); //$NON-NLS-1$
					}
					if (name.equals(formElement.getGroupID()))
					{
						return new ScriptObjectClassScope(this, GroupScriptObject.class, "elements." + name); //$NON-NLS-1$
					}
				}
			}
		}
		return super.get(name, start);
	}
}
