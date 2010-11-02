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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.dltk.javascript.typeinference.IValueCollection;
import org.eclipse.dltk.javascript.typeinference.ValueCollectionFactory;
import org.eclipse.dltk.javascript.typeinfo.IElementResolver;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.jface.resource.ImageDescriptor;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.FormManager.HistoryProvider;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSI18N;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.JSUnitAssertFunctions;
import com.servoy.j2db.scripting.JSUtils;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;

/**
 * Class that resolves names in javascript like application or controller to a {@link Property} with a reference to the {@link Type} of that name.
 * It also lists all possible global names for the current context for the code completion to use.
 * 
 * @author jcompagner
 * @since 6.0
 */
@SuppressWarnings("nls")
public class ElementResolver extends TypeCreator implements IElementResolver
{
	private static final List<String> noneConstantTypes = Arrays.asList(new String[] { "application", "security", "jsunit", "solutionModel", "databaseManager", "controller", "currentcontroller", "i18n", "history", "utils", "foundset", "forms", "elements" });

	private final Map<String, ITypeNameCreator> typeNameCreators = new HashMap<String, ElementResolver.ITypeNameCreator>();

	public ElementResolver()
	{
		addType("application", JSApplication.class);
		addType("security", JSSecurity.class);
		addType("i18n", JSI18N.class);
		addType("history", HistoryProvider.class);
		addType("utils", JSUtils.class);
		addType("jsunit", JSUnitAssertFunctions.class);
		addType("solutionModel", JSSolutionModel.class);
		addType("databaseManager", JSDatabaseManager.class);
		addType("controller", JSForm.class);
		addType("currentcontroller", JSForm.class);

		typeNameCreators.put("foundset", new FoundsetTypeNameCreator());
		typeNameCreators.put("plugins", new PluginsTypeNameCreator());
		typeNameCreators.put("elements", new ElementsTypeNameCreator());
		typeNameCreators.put("forms", new FormsNameCreator());

	}

	@Override
	protected boolean constantsOnly(String name)
	{
		return !noneConstantTypes.contains(name);
	}

	public Set<String> listGlobals(ITypeInfoContext context, String prefix)
	{
		Set<String> typeNames = getTypeNames(prefix);
		FlattenedSolution fs = getFlattenedSolution(context);
		Form form = getForm(context);
		if (form != null)
		{
			typeNames.add("forms");
			typeNames.add("globals");
			typeNames.addAll(typeNameCreators.keySet());
			Form formToUse = form;
			if (form.getExtendsFormID() > 0)
			{
				try
				{
					formToUse = fs.getFlattenedForm(form);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Cant get super flattened form for " + form, e);
				}
			}
			else
			{
				typeNames.remove("_super");
			}
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
			// global, remove the form only things.
			typeNames.remove("controller");

			typeNames.add("forms");
			typeNames.add("plugins");

			try
			{
				Iterator<Relation> relations = fs.getRelations(null, true, false);
				while (relations.hasNext())
				{
					typeNames.add(relations.next().getName());
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Cant get relations of " + form, e);
			}


			// or calculation???
		}
		return typeNames;
	}

	public Member resolveElement(ITypeInfoContext context, String name)
	{
		if (BASE_TYPES.contains(name)) return null;

		if ("globals".equals(name))
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs == null || fs.getSolution() == null)
			{
				return null;
			}
			ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(fs.getSolution().getName());
			IFile file = project.getProject().getFile("globals.js");
			IValueCollection globalsValueCollection = ValueCollectionProvider.getValueCollection(context, file);
			if (globalsValueCollection != null)
			{
				IValueCollection collection = ValueCollectionFactory.createScopeValueCollection();
				ValueCollectionFactory.copyInto(collection, globalsValueCollection);
				collection = ValueCollectionProvider.getGlobalModulesValueCollection(context, fs, collection);
				if (collection != null)
				{
					Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
					property.setName(name);
					property.setReadOnly(true);
					property.setAttribute(VALUECOLLECTION, collection);
					property.setAttribute(IMAGE_DESCRIPTOR, GLOBALS);
					property.setType(context.getType("Globals<" + fs.getSolution().getName() + '>'));
					return property;
				}
			}
			return null;
		}
		else if ("_super".equals(name))
		{
			Form form = getForm(context);
			if (form != null && form.getExtendsFormID() > 0)
			{
				FlattenedSolution fs = getFlattenedSolution(context);
				if (fs != null)
				{
					Form superForm = fs.getForm(form.getExtendsFormID());
					Property property = createProperty(context, "_super", true, null, FORM_IMAGE);
					property.setDescription(getDoc("_super", com.servoy.j2db.documentation.scripting.docs.Form.class, "", null));
					property.setAttribute(LAZY_VALUECOLLECTION, superForm);
					return property;
				}
			}
			return null;
		}
		Type type = null;
		String typeName = getTypeName(context, name);
		if (typeName != null)
		{
			type = context.getType(typeName);
		}
		else
		{
			type = getType(context, name);
		}
		boolean readOnly = true;
		ImageDescriptor image = null;
		Object resource = null;
		if (type == null && name.indexOf('.') == -1)
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs != null)
			{
				Relation relation = fs.getRelation(name);
				if (relation != null)
				{
					type = context.getType(FoundSet.JS_FOUNDSET + "<" + relation.getName() + ">");
					image = RELATION_IMAGE;
					resource = relation;
				}
				else
				{
					Form form = getForm(context);
					if (form != null)
					{
						IDataProvider provider = null; //form.getScriptVariable(name);
						if (provider == null)
						{
							try
							{
								Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(form.getTable());
								if (allDataProvidersForTable != null)
								{
									provider = allDataProvidersForTable.get(name);
									image = COLUMN_IMAGE;
									if (provider instanceof AggregateVariable)
									{
										image = COLUMN_AGGR_IMAGE;
									}
									else if (provider instanceof ScriptCalculation)
									{
										image = COLUMN_CALC_IMAGE;
									}
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
							type = getDataProviderType(context, provider);
							resource = provider;
						}
					}
				}

			}
		}
		else if (type != null)
		{
			image = (ImageDescriptor)type.getAttribute(IMAGE_DESCRIPTOR);
		}

		if (type != null)
		{
			return createProperty(name, readOnly, type, type.getDescription(), image, resource);
		}
		return null;
	}

	/**
	 * @param file
	 * @return
	 */


	private String getTypeName(ITypeInfoContext context, String name)
	{
		ITypeNameCreator nameCreator = typeNameCreators.get(name);
		if (nameCreator != null) return nameCreator.getTypeName(context, name);
		return null;
	}

	private class ElementsTypeNameCreator implements ITypeNameCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			Form form = getForm(context);
			if (form != null)
			{
				return "Elements<" + form.getName() + '>';
			}
			return "Elements";
		}
	}

	private class FormsNameCreator implements ITypeNameCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs != null)
			{
				return "Forms<" + fs.getMainSolutionMetaData().getName() + '>';
			}
			return "Forms";
		}
	}

	private class PluginsTypeNameCreator implements ITypeNameCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			return "Plugins";
		}
	}

	private class FoundsetTypeNameCreator implements ITypeNameCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			Form form = getForm(context);
			if (form != null && form.getDataSource() != null)
			{
				return FoundSet.JS_FOUNDSET + '<' + form.getDataSource() + '>';
			}
			return FoundSet.JS_FOUNDSET;
		}
	}

	public interface ITypeNameCreator
	{
		public String getTypeName(ITypeInfoContext context, String typeName);
	}
}
