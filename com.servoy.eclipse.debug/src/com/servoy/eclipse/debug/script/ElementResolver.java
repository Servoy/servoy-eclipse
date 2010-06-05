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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dltk.javascript.typeinfo.IElementResolver;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.dltk.javascript.typeinfo.model.TypeKind;
import org.eclipse.emf.common.util.EList;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;

@SuppressWarnings("nls")
public class ElementResolver extends TypeCreator implements IElementResolver
{
	public static final String FOUNDSET_TABLE_CONFIG = "table:";

	private static final List<String> noneConstantTypes = Arrays.asList(new String[] { "application", "security", "solutionModel", "databaseManager", "controller", "currentcontroller", "foundset", "forms", "elements", "globals" });

	public ElementResolver()
	{
		addType("application", JSApplication.class);
		addType("security", JSSecurity.class);
		addType("solutionModel", JSSolutionModel.class);
		addType("databaseManager", JSDatabaseManager.class);
		addType("controller", JSForm.class);
		addType("currentcontroller", JSForm.class);

		addScopeType("foundset", new FoundsetScopeCreator());
		addScopeType("forms", new FormsScopeCreator());
		addScopeType("elements", new ElementsScopeCreator());
		addScopeType("globals", new GlobalScopeCreator());
	}

	@Override
	protected boolean constantsOnly(String name)
	{
		return !noneConstantTypes.contains(name);
	}

	public Set<String> listGlobals(ITypeInfoContext context, String prefix)
	{
		Set<String> typeNames = getTypeNames(prefix);
		Form form = getForm(context);
		if (form != null)
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			Form formToUse = form;
			typeNames.remove("currentcontroller");
			if (form.getExtendsFormID() > 0)
			{
				typeNames.add("_super");
				try
				{
					formToUse = fs.getFlattenedForm(form);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Cant get super flattened form for " + form, e);
				}
			}
// TODO is this needed? should already be done
//			Iterator<ScriptMethod> scriptMethods = formToUse.getScriptMethods(false);
//			while (scriptMethods.hasNext())
//			{
//				ScriptMethod sm = scriptMethods.next();
//			}
//			formToUse.getScriptVariables(false)
// data providers
			try
			{
				Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(formToUse.getTable());
				if (allDataProvidersForTable != null)
				{
					typeNames.addAll(allDataProvidersForTable.keySet());
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Cant get dataproviders of " + form, e);
			}

			try
			{
				Iterator<Relation> relations = fs.getRelations(formToUse.getTable(), true, false);
				while (relations.hasNext())
				{
					typeNames.add(relations.next().getName());
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Cant get relations of " + form, e);
			}
		}
		else
		{
			// global
			typeNames.remove("controller");
			typeNames.remove("globals");
			typeNames.remove("foundset");
			// or calculation???
		}
		return typeNames;
	}

	public Element resolveElement(ITypeInfoContext context, String name)
	{
		Type type = getType(context, name);
		boolean readOnly = true;
		if (type == null)
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs != null)
			{
				Relation relation = fs.getRelation(name);
				if (relation != null)
				{
					type = context.getType(FoundSet.JS_FOUNDSET + "<" + relation.getName() + ">");
				}
				else
				{
					Form form = getForm(context);
					if (form != null)
					{
						IDataProvider provider = form.getScriptVariable(name);
						if (provider == null)
						{
							try
							{
								Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(form.getTable());
								if (allDataProvidersForTable != null)
								{
									provider = allDataProvidersForTable.get(name);
								}
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
							}
						}
						if (provider != null)
						{
							readOnly = false;
							type = getDataPRoviderType(context, provider);
						}
					}
				}

			}
		}

		if (type != null)
		{
			return createProperty(name, readOnly, type);
		}
		return null;
	}

	/**
	 * @param context
	 * @param type
	 * @param provider
	 * @return
	 */
	private Type getDataPRoviderType(ITypeInfoContext context, IDataProvider provider)
	{
		Type type = null;
		switch (provider.getDataProviderType())
		{
			case IColumnTypes.DATETIME :
				type = context.getType("Date");
				break;
			case IColumnTypes.INTEGER :
			case IColumnTypes.NUMBER :
				type = context.getType("Number");
				break;
			case IColumnTypes.TEXT :
				type = context.getType("String");
				break;
		}
		return type;
	}

	private class FormsScopeCreator implements IScopeTypeCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(ITypeInfoContext context, String fullTypeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName("forms");
			type.setKind(TypeKind.JAVA);

			EList<Member> members = type.getMembers();
			members.add(createProperty(context, "allnames", true, "Array", "All form names as an array"));
			members.add(createProperty(context, "length", true, "Number", "Number of forms"));

			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs != null)
			{
				Iterator<Form> forms = fs.getForms(false);

				while (forms.hasNext())
				{
					Form form = forms.next();
					members.add(createProperty(context, form.getName(), true, "Form<" + form.getName() + '>',
						"Form based on datasource: " + form.getDataSource()));
				}
			}
			return type;
		}

	}

	private class GlobalScopeCreator implements IScopeTypeCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(ITypeInfoContext context, String fullTypeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName("globals");
			type.setKind(TypeKind.JAVA);

			EList<Member> members = type.getMembers();

			members.add(createProperty(context, "allmethods", true, "Array"));
			members.add(createProperty(context, "allvariables", true, "Array"));
			members.add(createProperty(context, "allrelations", true, "Array"));
			members.add(createProperty(context, "currentcontroller", true, "controller"));

			FlattenedSolution fs = getFlattenedSolution(context);

			if (fs != null)
			{
				Iterator<ScriptVariable> scriptVariables = fs.getScriptVariables(false);
				while (scriptVariables.hasNext())
				{
					ScriptVariable sv = scriptVariables.next();
					members.add(createProperty(sv.getName(), false, getDataPRoviderType(context, sv), sv.getComment()));
				}

				Iterator<ScriptMethod> scriptMethods = fs.getScriptMethods(false);
				while (scriptMethods.hasNext())
				{
					ScriptMethod sm = scriptMethods.next();
					members.add(createMethod(context, sm));
				}
			}
			return type;

		}
	}

	private class ElementsScopeCreator implements IScopeTypeCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(ITypeInfoContext context, String fullTypeName)
		{
			Form form = getForm(context);
			if (form != null)
			{
				return context.getType("Elements<" + form.getName() + '>');
			}
			return context.getType("Elements");
		}
	}

	private class FoundsetScopeCreator implements IScopeTypeCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(ITypeInfoContext context, String fullTypeName)
		{
			Form form = getForm(context);
			if (form != null)
			{
				try
				{
					Table table = form.getTable();
					if (table != null) return context.getType(FoundSet.JS_FOUNDSET + '<' + FOUNDSET_TABLE_CONFIG + table.getServerName() + '.' +
						table.getName() + '>');
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}

			}
			return context.getType(FoundSet.JS_FOUNDSET);
		}
	}
}
