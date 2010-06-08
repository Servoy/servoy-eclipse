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
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.debug.Activator;
import com.servoy.eclipse.ui.scripting.CalculationModeHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.DefaultScope;

/**
 * @author jcompagner
 * 
 */
class GlobalScope extends DefaultScope
{
	private final static URL GLOBAL_VAR_IMAGE = FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/global_variable.gif"), null); //$NON-NLS-1$
	private final static URL GLOBAL_METHOD_IMAGE = FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/global_method.gif"), null); //$NON-NLS-1$

	/**
	 * @param parent
	 */
	GlobalScope(Scriptable parent)
	{
		super(parent);
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getClassName()
	 */
	@Override
	public String getClassName()
	{
		return GlobalScope.class.getName();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		ArrayList<String> al = new ArrayList<String>();
		FlattenedSolution fs = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
		boolean isLoginSolution = fs.getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION;
		Iterator<ScriptVariable> scriptGlobalVariables = fs.getScriptVariables(false);
		while (scriptGlobalVariables.hasNext())
		{
			al.add(scriptGlobalVariables.next().getName());
		}
		if (!CalculationModeHandler.getInstance().isCalculationMode())
		{
			al.add("allmethods"); //$NON-NLS-1$
			al.add("allvariables"); //$NON-NLS-1$
			if (!isLoginSolution) al.add("allrelations"); //$NON-NLS-1$
			al.add("currentcontroller"); //$NON-NLS-1$
			Iterator<ScriptMethod> scriptMethods = fs.getScriptMethods(false);
			while (scriptMethods.hasNext())
			{
				al.add(scriptMethods.next().getName());
			}
		}
		if (!isLoginSolution)
		{
			// relations
			try
			{
				Iterator<Relation> relations = fs.getRelations(null, true, false); // returns only global relations
				while (relations.hasNext())
				{
					al.add(relations.next().getName());
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("error in codecompletion global relations", e);
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
		if (name.equals("allmethods") || name.equals("allrelations") || name.equals("allvariables"))
		{
			String doc = FormDomProvider.getDoc(name, com.servoy.eclipse.core.scripting.docs.Globals.class, ""); //$NON-NLS-1$
			return new ProposalHolder(null, null, "Array", doc, false, null, null); //$NON-NLS-1$
		}
		if (name.equals("currentcontroller"))
		{
			String doc = FormDomProvider.getDoc(name, com.servoy.eclipse.core.scripting.docs.Globals.class, ""); //$NON-NLS-1$
			return new ScriptObjectClassScope(this, JSForm.class, "currentcontroller", null, doc, false);
		}

		FlattenedSolution fs = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
		ScriptVariable sv = fs.getScriptVariable(name);
		if (sv != null)
		{
			String filename = SolutionSerializer.getScriptPath(sv, false);
			IFile file = null;
			if (filename != null)
			{
				file = ResourcesPlugin.getWorkspace().getRoot().getFile(Path.fromOSString(filename));
			}
			Object initValue = sv.getInitValue();
			if (initValue instanceof String && (sv.getDataProviderType() == IColumnTypes.MEDIA || sv.getDataProviderType() == IColumnTypes.TEXT))
			{
				// if it isnt really a string then set the value to null
				// let dltk handle its type else you get String completion
				if (!((String)initValue).startsWith("\"") && !((String)initValue).startsWith("'")) //$NON-NLS-1$ //$NON-NLS-2$
				{
					initValue = null;
				}
			}
			return new ProposalHolder(initValue, null, "Type: " + Column.getDisplayTypeString(sv.getVariableType()) + "<br/>Defaultvalue: " +
				sv.getDefaultValue() + "<br/>Solution: " + sv.getRootObject().getName(), false, GLOBAL_VAR_IMAGE, file);
		}


		ScriptMethod sm = fs.getScriptMethod(name);
		if (sm != null)
		{
			return new MethodScope(this, sm, "Solution: " + sm.getRootObject().getName(), true, GLOBAL_METHOD_IMAGE);
		}

		// relations
		Relation relation = fs.getRelation(name);
		if (relation != null && relation.isValid())
		{
			return new FoundSetScope(this, RelatedFoundSet.class, relation);
		}

		return Scriptable.NOT_FOUND;
	}
}
