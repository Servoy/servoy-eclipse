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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.javascript.typeinference.IValueCollection;
import org.eclipse.dltk.javascript.typeinference.ValueCollectionFactory;
import org.eclipse.dltk.javascript.typeinfo.IElementResolver;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.jface.resource.ImageDescriptor;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.Table;

/**
 * Class that resolves names in javascript like application or controller to a {@link Property} with a reference to the {@link Type} of that name.
 * It also lists all possible global names for the current context for the code completion to use.
 * 
 * @author jcompagner
 * @since 6.0
 */
@SuppressWarnings("nls")
public class ElementResolver implements IElementResolver
{
	private final Map<String, ITypeNameCreator> typeNameCreators = new HashMap<String, ElementResolver.ITypeNameCreator>();

	public ElementResolver()
	{
		typeNameCreators.put("application", new SimpleNameTypeNameCreator("JSApplication"));
		typeNameCreators.put("security", new SimpleNameTypeNameCreator("JSSecurity"));
		typeNameCreators.put("i18n", new SimpleNameTypeNameCreator("JSI18N"));
		typeNameCreators.put("history", new SimpleNameTypeNameCreator("JSHistory"));
		typeNameCreators.put("utils", new SimpleNameTypeNameCreator("JSUtils"));
		typeNameCreators.put("jsunit", new SimpleNameTypeNameCreator("JSUnit"));
		typeNameCreators.put("solutionModel", new SimpleNameTypeNameCreator("JSSolutionModel"));
		typeNameCreators.put("databaseManager", new SimpleNameTypeNameCreator("JSDatabaseManager"));
		typeNameCreators.put("controller", new SimpleNameTypeNameCreator("Controller"));
		typeNameCreators.put("currentcontroller", new SimpleNameTypeNameCreator("Controller"));

		typeNameCreators.put("foundset", new FoundsetTypeNameCreator());
		typeNameCreators.put("plugins", new SimpleNameTypeNameCreator("Plugins"));
		typeNameCreators.put("elements", new ElementsTypeNameCreator());
		typeNameCreators.put("forms", new FormsNameCreator());

	}

	public Set<String> listGlobals(ITypeInfoContext context, String prefix)
	{
		Set<String> typeNames = getTypeNames(prefix);
		FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
		Form form = TypeCreator.getForm(context);

		if (ValueCollectionProvider.getGenerateFullGlobalCollection())
		{
			typeNames.add("devSolutionModel");
		}


		if (form != null)
		{
			typeNames.add("globals");
			typeNames.addAll(typeNameCreators.keySet());
			Form formToUse = form;
			if (form.getExtendsFormID() > 0)
			{
				try
				{
					formToUse = fs.getFlattenedForm(form);
					typeNames.add("_super");
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Cant get super flattened form for " + form, e);
				}
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
		else if (fs != null)
		{
			// global, remove the form only things.
			typeNames.remove("controller");
			typeNames.remove("foundset");
			typeNames.remove("elements");

			Table calcTable = getCalculationTable(context, fs);
			if (calcTable != null)
			{
				typeNames.remove("currentcontroller");
				typeNames.remove("databaseManager");
				typeNames.remove("history");
				typeNames.remove("jsunit");
				try
				{
					Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(calcTable);
					if (allDataProvidersForTable != null)
					{
						typeNames.addAll(allDataProvidersForTable.keySet());
					}
					Iterator<Relation> relations = fs.getRelations(calcTable, true, false);
					while (relations.hasNext())
					{
						typeNames.add(relations.next().getName());
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError("Cant get dataproviders of " + calcTable + " for calculations " + context.getModelElement().getResource(), e);
				}

			}
			else
			{
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
			}
		}
		return typeNames;
	}

	/**
	 * @param prefix
	 * @return
	 */
	public final Set<String> getTypeNames(String prefix)
	{
		Set<String> names = new HashSet<String>(typeNameCreators.keySet());
		names.addAll(typeNameCreators.keySet());
		if (prefix != null && !"".equals(prefix.trim()))
		{
			String lowerCasePrefix = prefix.toLowerCase();
			Iterator<String> iterator = names.iterator();
			while (iterator.hasNext())
			{
				String name = iterator.next();
				if (!name.toLowerCase().startsWith(lowerCasePrefix)) iterator.remove();
			}
		}
		return names;
	}

	/**
	 * @param context
	 * @param fs
	 * @return
	 */
	private Table getCalculationTable(ITypeInfoContext context, FlattenedSolution fs)
	{
		Table calcTable = null;
		IResource resource = context.getModelElement().getResource();
		if (resource != null)
		{
			IPath path = resource.getProjectRelativePath();
			if (path != null && path.segmentCount() == 3 && path.segment(0).equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
				path.segment(2).endsWith(SolutionSerializer.CALCULATIONS_POSTFIX_WITH_EXT))
			{
				String calcServerName = path.segment(1);
				String calcTableName = path.segment(2).substring(0, path.segment(2).length() - SolutionSerializer.CALCULATIONS_POSTFIX_WITH_EXT.length());
				try
				{
					IServer server = fs.getSolution().getServer(calcServerName);
					if (server != null) calcTable = (Table)server.getTable(calcTableName);
				}
				catch (Exception e)
				{
					ServoyLog.logError("Cant get table " + calcTableName + " for " + calcServerName + " for calculations " + resource, e);
				}
			}
		}
		return calcTable;
	}

	public Member resolveElement(ITypeInfoContext context, String name)
	{
		if (TypeCreator.BASE_TYPES.contains(name)) return null;

		if ("globals".equals(name))
		{
			FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
			if (fs == null || fs.getSolution() == null)
			{
				return null;
			}
			ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(fs.getSolution().getName());
			IFile file = project.getProject().getFile("globals.js");
			IValueCollection globalsValueCollection = ValueCollectionProvider.getValueCollection(file);
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
					property.setAttribute(TypeCreator.VALUECOLLECTION, collection);
					property.setAttribute(TypeCreator.IMAGE_DESCRIPTOR, TypeCreator.GLOBALS);
					property.setType(context.getType("Globals<" + fs.getSolution().getName() + '>'));
					return property;
				}
			}
			return null;
		}
		else if ("_super".equals(name))
		{
			Form form = TypeCreator.getForm(context);
			if (form != null && form.getExtendsFormID() > 0)
			{
				FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
				if (fs != null)
				{
					Form superForm = fs.getForm(form.getExtendsFormID());
					Property property = TypeCreator.createProperty(context, "_super", true, null, TypeCreator.FORM_IMAGE);
					property.setDescription(TypeCreator.getDoc("_super", com.servoy.j2db.documentation.scripting.docs.Form.class, "", null));
					property.setAttribute(TypeCreator.LAZY_VALUECOLLECTION, superForm);
					return property;
				}
			}
			return null;
		}
		Type type = null;
		String typeName = getTypeName(context, name);
		if ("developerSolutionModel".equals(name))
		{
			typeName = "JSDeveloperSolutionModel";
		}
		if (typeName != null)
		{
			type = context.getType(typeName);
		}
		boolean readOnly = true;
		ImageDescriptor image = null;
		Object resource = null;
		if (type == null && name.indexOf('.') == -1)
		{
			FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
			if (fs != null)
			{
				Relation relation = fs.getRelation(name);
				if (relation != null)
				{
					type = context.getType(FoundSet.JS_FOUNDSET + "<" + relation.getName() + ">");
					image = TypeCreator.RELATION_IMAGE;
					resource = relation;
				}
				else
				{
					Form form = TypeCreator.getForm(context);
					Table table = null;
					if (form != null)
					{
						try
						{
							table = form.getTable();
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError(e);
						}
					}
					else
					{
						table = getCalculationTable(context, fs);
					}
					if (table != null)
					{
						IDataProvider provider = null; //form.getScriptVariable(name);
						if (provider == null)
						{
							try
							{
								Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(table);
								if (allDataProvidersForTable != null)
								{
									provider = allDataProvidersForTable.get(name);
									image = TypeCreator.COLUMN_IMAGE;
									if (provider instanceof AggregateVariable)
									{
										image = TypeCreator.COLUMN_AGGR_IMAGE;
									}
									else if (provider instanceof ScriptCalculation)
									{
										image = TypeCreator.COLUMN_CALC_IMAGE;
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
							type = TypeCreator.getDataProviderType(context, provider);
							resource = provider;
						}
					}
				}

			}
		}
		else if (type != null)
		{
			image = (ImageDescriptor)type.getAttribute(TypeCreator.IMAGE_DESCRIPTOR);
		}

		if (type != null)
		{
			return TypeCreator.createProperty(name, readOnly, type, type.getDescription(), image, resource);
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
			Form form = TypeCreator.getForm(context);
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
			FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
			if (fs != null)
			{
				return "Forms<" + fs.getMainSolutionMetaData().getName() + '>';
			}
			return "Forms";
		}
	}

	private class SimpleNameTypeNameCreator implements ITypeNameCreator
	{
		private final String name;

		public SimpleNameTypeNameCreator(String name)
		{
			this.name = name;
		}

		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			return name;
		}
	}

	private class FoundsetTypeNameCreator implements ITypeNameCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			Form form = TypeCreator.getForm(context);
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
