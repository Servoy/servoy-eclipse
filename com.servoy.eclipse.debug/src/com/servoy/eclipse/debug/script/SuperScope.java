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

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.dlkt.javascript.dom.support.IProposalHolder;
import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.scripting.DefaultScope;

/**
 * @author jcompagner
 * 
 */
public class SuperScope extends DefaultScope implements IProposalHolder
{
	private final ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
	private final Form form;

	/**
	 * @param parent
	 */
	public SuperScope(Scriptable parent, Form form)
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
		return SuperScope.class.getName();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		ArrayList<String> al = new ArrayList<String>();
		Form f = getFormToUse();
		Iterator<ScriptMethod> scriptMethods = f.getScriptMethods(true);
		while (scriptMethods.hasNext())
		{
			al.add(scriptMethods.next().getDataProviderID());
		}
		return al.toArray();
	}

	/**
	 * @return
	 */
	private Form getFormToUse()
	{
		Form f = servoyModel.getFlattenedSolution().getForm(form.getExtendsFormID());
		try
		{
			f = servoyModel.getFlattenedSolution().getFlattenedForm(f);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return f;
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		Form f = getFormToUse();
		Iterator<ScriptMethod> scriptMethods = f.getScriptMethods(true);
		while (scriptMethods.hasNext())
		{
			ScriptMethod method = scriptMethods.next();
			if (method.getDataProviderID().equals(name))
			{
				String proposalInfo = null;
				proposalInfo = "Method of form: " + ((Form)method.getParent()).getName(); //$NON-NLS-1$
				return new MethodScope(this, method, proposalInfo, true, FormScope.FORM_METHOD_IMAGE);
			}
		}
		return super.get(name, start);
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getImageURL()
	 */
	public URL getImageURL()
	{
		return null;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getObject()
	 */
	public Object getObject()
	{
		return this;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getParameterNames()
	 */
	public char[][] getParameterNames()
	{
		return null;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getReturnType()
	 */
	public String getReturnType()
	{
		return null;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getProposalInfo()
	 */
	public String getProposalInfo()
	{
		return "super form: " + getFormToUse().getName(); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getSourceFile()
	 */
	public IFile getSourceFile()
	{
		Form f = servoyModel.getFlattenedSolution().getForm(form.getExtendsFormID());
		String filename = SolutionSerializer.getScriptPath(f, false);
		if (filename != null)
		{
			return ResourcesPlugin.getWorkspace().getRoot().getFile(Path.fromOSString(filename));
		}
		return null;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#isFunctionRef()
	 */
	public boolean isFunctionRef()
	{
		return false;
	}
}
