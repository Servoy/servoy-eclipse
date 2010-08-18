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
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.dlkt.javascript.dom.support.IProposalHolder;
import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.debug.Activator;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.DefaultScope;

/**
 * @author jcompagner
 * 
 */
class FormScope extends DefaultScope implements IProposalHolder
{
	private final static URL FORM_IMAGE = FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/designer.gif"), null); //$NON-NLS-1$
	final static URL FORM_METHOD_IMAGE = FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/form_method.gif"), null); //$NON-NLS-1$
	final static URL FORM_VARIABLE_IMAGE = FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/form_variable.gif"), null); //$NON-NLS-1$

	private final Form form;
	private final String[] parameterNames;
	private final String proposalInfo;
	private final boolean isFunctionRef;
	private final URL imageURL;

	/**
	 * @param parent
	 */
	FormScope(Scriptable parent, Form form)
	{
		this(parent, form, null, form != null ? "Form based on datasource: " + form.getDataSource() : null, false, FORM_IMAGE); //$NON-NLS-1$
	}

	FormScope(Scriptable parent, Form form, String[] parameterNames, String proposalInfo, boolean isFunctionRef, URL imageURL)
	{
		super(parent);
		this.form = form;
		this.parameterNames = parameterNames;
		this.proposalInfo = proposalInfo;
		this.isFunctionRef = isFunctionRef;
		this.imageURL = imageURL;
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getClassName()
	 */
	@Override
	public String getClassName()
	{
		return FormScope.class.getName();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		FlattenedSolution fs = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
		ArrayList<String> al = new ArrayList<String>();

		// first the fixed.
		al.add("allnames"); //$NON-NLS-1$
		al.add("alldataproviders"); //$NON-NLS-1$
		al.add("allmethods"); //$NON-NLS-1$
		al.add("allrelations"); //$NON-NLS-1$
		al.add("allvariables"); //$NON-NLS-1$

		// controller, elements and foundset
		al.add("controller"); //$NON-NLS-1$
		al.add("elements"); //$NON-NLS-1$
		al.add("foundset"); //$NON-NLS-1$

		if (form != null)
		{
			try
			{
				// all methods of this form.
				Form formToUse = form;
				if (form.getExtendsFormID() > 0)
				{
					formToUse = fs.getFlattenedForm(form);
					al.add("_super"); //$NON-NLS-1$
				}
				Iterator<ScriptMethod> scriptMethods = formToUse.getScriptMethods(false);
				while (scriptMethods.hasNext())
				{
					al.add(scriptMethods.next().getDataProviderID());
				}

				// form variables
				Iterator<ScriptVariable> scriptVariables = formToUse.getScriptVariables(false);
				while (scriptVariables.hasNext())
				{
					al.add(scriptVariables.next().getDataProviderID());
				}

				// data providers
				Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(form.getTable());

				if (allDataProvidersForTable != null) al.addAll(allDataProvidersForTable.keySet());

				// relations
				Iterator<Relation> relations = fs.getRelations(form.getTable(), true, false);
				while (relations.hasNext())
				{
					al.add(relations.next().getName());
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error in codecompleton form: " + form, e); //$NON-NLS-1$
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
		if (name.equals("allnames") || name.equals("alldataproviders") || name.equals("allmethods") || name.equals("allrelations") || //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			name.equals("allvariables")) //$NON-NLS-1$
		{
			String doc = FormDomProvider.getDoc(name, com.servoy.j2db.documentation.scripting.docs.Form.class, ""); //$NON-NLS-1$
			return new ProposalHolder(null, null, "Array", doc, false, null, null); //$NON-NLS-1$
		}
		if (name.equals("controller")) //$NON-NLS-1$
		{
			String doc = FormDomProvider.getDoc(name, com.servoy.j2db.documentation.scripting.docs.Form.class, ""); //$NON-NLS-1$
			return new ScriptObjectClassScope(this, JSForm.class, "controller", null, doc, false); //$NON-NLS-1$
		}
		if (name.equals("elements")) //$NON-NLS-1$
		{
			return new ElementScope(this, form);
		}
		if (form != null && name.equals("_super") && form.getExtendsFormID() > 0) //$NON-NLS-1$
		{
			String doc = FormDomProvider.getDoc(name, com.servoy.j2db.documentation.scripting.docs.Form.class, ""); //$NON-NLS-1$
			return new SuperScope(this, form, doc);
		}
		if (name.equals("foundset")) //$NON-NLS-1$
		{
			if (form != null)
			{
				try
				{
					return new FoundSetScope(this, FoundSet.class, form.getTable());
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
					return null;
				}
			}
			return new FoundSetScope(this, FoundSet.class, (ITable)null);
		}
		if (form != null)
		{
			FlattenedSolution fs = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
			try
			{
				Object o = RecordScope.testForDataProvider(this, fs, form.getTable(), name);
				if (o != null) return o;
			}
			catch (RepositoryException e1)
			{
				ServoyLog.logError("error getting dataproviders from table", e1); //$NON-NLS-1$
			}
			Form formToUse;
			try
			{
				formToUse = fs.getFlattenedForm(form);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
				return null;
			}
			ScriptMethod method = formToUse.getScriptMethod(name);
			if (method != null)
			{
				String proposalInfo = null;
				if (method.getParent() != form)
				{
					proposalInfo = "Method of form: " + ((Form)method.getParent()).getName(); //$NON-NLS-1$
				}
				return new MethodScope(this, method, proposalInfo, true, FORM_METHOD_IMAGE);
			}

			// form variables
			ScriptVariable variable = formToUse.getScriptVariable(name);
			if (variable != null)
			{
				IFile file = null;
				String filename = SolutionSerializer.getScriptPath(variable, false);
				if (filename != null)
				{
					file = ResourcesPlugin.getWorkspace().getRoot().getFile(Path.fromOSString(filename));
				}
				Object initValue = variable.getInitValue();
				if (initValue instanceof String &&
					(variable.getDataProviderType() == IColumnTypes.MEDIA || variable.getDataProviderType() == IColumnTypes.TEXT))
				{
					// if it isnt really a string then set the value to null
					// let dltk handle its type else you get String completion
					if (!((String)initValue).startsWith("\"") && !((String)initValue).startsWith("'")) //$NON-NLS-1$ //$NON-NLS-2$
					{
						initValue = null;
					}
				}

				return new ProposalHolder(initValue, null, "Type: " + Column.getDisplayTypeString(variable.getVariableType()) + "<br/>Defaultvalue: " + //$NON-NLS-1$ //$NON-NLS-2$
					variable.getDefaultValue() + "<br/>Form: " + ((Form)variable.getAncestor(IRepository.FORMS)).getName(), false, FORM_VARIABLE_IMAGE, file); //$NON-NLS-1$
			}

			// relations
			Relation relation = fs.getRelation(name);
			if (relation != null && relation.isValid())
			{
				return new FoundSetScope(this, RelatedFoundSet.class, relation);
			}
		}
		return Scriptable.NOT_FOUND;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof FormScope)
		{
			return ((FormScope)obj).form.equals(form);
		}
		return false;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getImageURL()
	 */
	public URL getImageURL()
	{
		return imageURL;
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
		if (parameterNames != null && parameterNames.length > 0)
		{
			char[][] nms = new char[parameterNames.length][];
			for (int i = 0; i < parameterNames.length; i++)
			{
				nms[i] = parameterNames[i].toCharArray();
			}
			return nms;
		}
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
		return proposalInfo;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getSourceFile()
	 */
	public IFile getSourceFile()
	{
		if (form == null) return null;
		String path = SolutionSerializer.getRelativePath(form, false);
		String persistBaseFilename = SolutionSerializer.getFileName(form, false);
		return ResourcesPlugin.getWorkspace().getRoot().getFile(Path.fromOSString(path + persistBaseFilename));
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#isFunctionRef()
	 */
	public boolean isFunctionRef()
	{
		return isFunctionRef;
	}
}
