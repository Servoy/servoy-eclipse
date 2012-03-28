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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.internal.javascript.ti.IReferenceAttributes;
import org.eclipse.dltk.javascript.typeinfo.IElementResolver;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.ITypeNames;
import org.eclipse.dltk.javascript.typeinfo.TypeMemberQuery;
import org.eclipse.dltk.javascript.typeinfo.TypeUtil;
import org.eclipse.dltk.javascript.typeinfo.model.JSType;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.jface.resource.ImageDescriptor;

import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptVariable;
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
	private static final Map<String, String> constantTypeNames = new HashMap<String, String>();

	public static void registerConstantType(String name, String typeName)
	{
		constantTypeNames.put(name, typeName);
	}

	private final Map<String, ITypeNameCreator> typeNameCreators = new HashMap<String, ElementResolver.ITypeNameCreator>();
	private final Set<String> formOnlyNames = new HashSet<String>();
	private final Set<String> noneCalcNames = new HashSet<String>();
	private final Set<String> noneFoundsetNames = new HashSet<String>();
	private final Set<String> deprecated = new HashSet<String>();

	public ElementResolver()
	{
		typeNameCreators.put("application", new SimpleNameTypeNameCreator("JSApplication"));
		typeNameCreators.put("security", new SimpleNameTypeNameCreator("JSSecurity"));
		typeNameCreators.put("i18n", new SimpleNameTypeNameCreator("JSI18N"));
		typeNameCreators.put("history", new SimpleNameTypeNameCreator("HistoryProvider"));
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
		typeNameCreators.put(ScriptVariable.SCOPES, new ScopesTypeNameCreator());
		typeNameCreators.put("alldataproviders", new SimpleNameTypeNameCreator("Array"));

		formOnlyNames.add("controller");
		formOnlyNames.add("alldataproviders");
		formOnlyNames.add("foundset");
		formOnlyNames.add("elements");

		noneFoundsetNames.add("currentcontroller");
		noneFoundsetNames.add("history");
		noneFoundsetNames.add("jsunit");
		noneFoundsetNames.add("forms");

		noneCalcNames.addAll(noneFoundsetNames); // all filtered out for foundset methods is also filtered out for calcs
		noneCalcNames.add("databaseManager");

		deprecated.add("alldataproviders");
	}


	public Set<String> listGlobals(ITypeInfoContext context, String prefix)
	{
		Set<String> typeNames = getTypeNames(prefix);
		FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
		IResource resource = context.getModelElement().getResource();
		if (resource != null && fs != null)
		{
			if (ValueCollectionProvider.getGenerateFullGlobalCollection())
			{
				typeNames.add("servoyDeveloper");
			}

			IPath path = resource.getProjectRelativePath();
			if (path.segmentCount() == 1)
			{
				// globals.js (or other scope)
				// global, remove the form only things.
				typeNames.removeAll(formOnlyNames);
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
					ServoyLog.logError("Can't get global relations", e);
				}
			}

			else if (path.segmentCount() == 2 && path.segment(0).equals(SolutionSerializer.FORMS_DIR))
			{
				// forms/formname.js
				Form form = TypeCreator.getForm(context);
				if (form != null)
				{
					typeNames.add(ScriptVariable.GLOBAL_SCOPE);
					Form formToUse = form;
					if (form.getExtendsID() > 0)
					{
						formToUse = fs.getFlattenedForm(form);
						typeNames.add("_super");
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
			}

			else if (path.segmentCount() == 3 && path.segment(0).equals(SolutionSerializer.DATASOURCES_DIR_NAME))
			{
				// datasources/server/table_foundset.js or datasources/server/table_calculations.js
				Table table = getDatasourceTable(context, fs);
				if (table != null)
				{
					typeNames.add(ScriptVariable.GLOBAL_SCOPE);
					if (path.segment(2).endsWith(SolutionSerializer.CALCULATIONS_POSTFIX))
					{
						// datasources/server/table_calculations.js
						typeNames.removeAll(noneCalcNames);
						typeNames.add("getDataSource");
						try
						{
							Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(table);
							if (allDataProvidersForTable != null)
							{
								typeNames.addAll(allDataProvidersForTable.keySet());
							}
							Iterator<Relation> relations = fs.getRelations(table, true, false);
							while (relations.hasNext())
							{
								typeNames.add(relations.next().getName());
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError("Cant get dataproviders of " + table + " for file " + context.getModelElement().getResource(), e);
						}
					}
					else if (path.segment(2).endsWith(SolutionSerializer.FOUNDSET_POSTFIX))
					{
						// datasources/server/table_foundset.js
						typeNames.removeAll(noneFoundsetNames);
						Type type = context.getType(FoundSet.JS_FOUNDSET + '<' + table.getDataSource() + '>');
						for (Member member : new TypeMemberQuery(type))
						{
							typeNames.add(member.getName());
						}
					}
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
		names.addAll(constantTypeNames.keySet());
		if (prefix != null && !"".equals(prefix.trim()))
		{
			String lowerCasePrefix = prefix.toLowerCase();
			Iterator<String> iterator = names.iterator();
			while (iterator.hasNext())
			{
				String name = iterator.next();
				if (name.startsWith(TypeCreator.TYPE_PREFIX)) name = name.substring(name.lastIndexOf(".") + 1);
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
	private Table getDatasourceTable(ITypeInfoContext context, FlattenedSolution fs)
	{
		Table table = null;
		IResource resource = context.getModelElement().getResource();
		String[] serverTablename = SolutionSerializer.getDataSourceForCalculationJSFile(resource);
		if (serverTablename == null)
		{
			serverTablename = SolutionSerializer.getDataSourceForFoundsetJSFile(resource);
		}
		if (serverTablename != null)
		{
			try
			{
				IServer server = fs.getSolution().getServer(serverTablename[0]);
				if (server != null) table = (Table)server.getTable(serverTablename[1]);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Cant get table " + serverTablename[1] + " for " + serverTablename[0] + " for file " + resource, e);
			}
		}
		return table;
	}

	@SuppressWarnings("restriction")
	public Member resolveElement(ITypeInfoContext context, String name)
	{
		if (TypeCreator.BASE_TYPES.contains(name)) return null;

		FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
		if (ScriptVariable.GLOBAL_SCOPE.equals(name))
		{
			if (fs == null || fs.getSolution() == null)
			{
				return null;
			}
			Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
			property.setName(name);
			property.setReadOnly(true);
			property.setAttribute(TypeCreator.IMAGE_DESCRIPTOR, TypeCreator.GLOBALS);
			property.setType(context.getTypeRef("Scope<" + fs.getSolution().getName() + "/globals>"));
			return property;
		}

		if ("_super".equals(name))
		{
			Form form = TypeCreator.getForm(context);
			if (form != null && form.getExtendsID() > 0 && fs != null)
			{
				Form superForm = fs.getForm(form.getExtendsID());
				Property property = TypeCreator.createProperty(context, "_super", true, null, TypeCreator.FORM_IMAGE);
				property.setDescription(TypeCreator.getDoc("_super", com.servoy.j2db.documentation.scripting.docs.Form.class, "", null));
				property.setAttribute(TypeCreator.LAZY_VALUECOLLECTION, superForm);
				property.setAttribute(IReferenceAttributes.SUPER_SCOPE, Boolean.TRUE);
				return property;
			}
			return null;
		}

		String typeName;
		if ("servoyDeveloper".equals(name))
		{
			typeName = "JSDeveloperSolutionModel";
		}
		else
		{
			typeName = getTypeName(context, name);
		}
		Type type = null;
		if (typeName != null)
		{
			if (SolutionSerializer.getDataSourceForCalculationJSFile(context.getModelElement().getResource()) != null &&
				(noneCalcNames.contains(name) || formOnlyNames.contains(name)))
			{
				// hide in calc
				return null;
			}
			if (SolutionSerializer.getDataSourceForFoundsetJSFile(context.getModelElement().getResource()) != null &&
				(noneFoundsetNames.contains(name) || formOnlyNames.contains(name)))
			{
				// hide in foundset method
				return null;
			}
			if (SolutionSerializer.getFormNameForJSFile(context.getModelElement().getResource()) == null && formOnlyNames.contains(name))
			{
				// hide in other then forms
				return null;
			}
			type = context.getType(typeName);
		}
		boolean readOnly = true;
		ImageDescriptor image = null;
		Object resource = null;
		String description = null;
		boolean hideAllowed = false;
		if (type == null && name.indexOf('.') == -1)
		{
			if (fs != null)
			{
				Relation relation = fs.getRelation(name);
				if (relation != null)
				{
					type = context.getType(FoundSet.JS_FOUNDSET + '<' + relation.getName() + '>');
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
						table = getDatasourceTable(context, fs);
					}
					if (table != null)
					{
						IDataProvider provider = null; //form.getScriptVariable(name);
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
									description = "Aggregate (" + ((AggregateVariable)provider).getRootObject().getName() + ")".intern();
								}
								else if (provider instanceof ScriptCalculation)
								{
									image = TypeCreator.COLUMN_CALC_IMAGE;
									hideAllowed = true;
									description = "Calculation (" + ((ScriptCalculation)provider).getRootObject().getName() + ")".intern();
								}
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}

						if (provider != null)
						{
							readOnly = false;
							type = TypeCreator.getDataProviderType(context, provider);
							resource = provider;
							if (provider instanceof Column && ((Column)provider).getColumnInfo() != null)
							{
								String columnDesc = ((Column)provider).getColumnInfo().getDescription();
								if (columnDesc == null) description = "Column";
								else description = "Column<br/>" + columnDesc.replace("\n", "<br/>");
							}
							if (type == null)
							{
								// for now type can be null for media 
								Property property = TypeCreator.createProperty(name, readOnly, null, null, image, resource);
								property.setHideAllowed(hideAllowed);
								return property;
							}
						}
						else if (isCalculationResource(context) && name.equals("getDataSource"))
						{
							Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
							method.setName(name);
							method.setType(TypeUtil.ref(ITypeNames.STRING));
							method.setAttribute(TypeCreator.IMAGE_DESCRIPTOR, TypeCreator.METHOD);
							return method;
						}
						else if (isFoundsetResource(context))
						{
							Type foundsetType = context.getType(FoundSet.JS_FOUNDSET + '<' + table.getDataSource() + '>');
							Member member = new TypeMemberQuery(foundsetType).findMember(name);
							if (member != null)
							{
								return member;
							}
						}
					}
				}

			}
		}

		if (type != null)
		{
			JSType typeRef = null;
			if (constantTypeNames.containsKey(name))
			{
				typeRef = TypeUtil.classType(type);
				image = TypeCreator.CONSTANT;
			}
			else
			{
				typeRef = TypeUtil.ref(type);
			}
			if (description == null) description = type.getDescription();
			Property property = TypeCreator.createProperty(name, readOnly, typeRef, description, image, resource);
			property.setHideAllowed(hideAllowed);
			if (deprecated.contains(name))
			{
				property.setDeprecated(true);
				property.setVisible(false);
			}
			else
			{
				property.setDeprecated(type.isDeprecated());
				property.setVisible(type.isVisible());
			}
			return property;
		}
		return null;
	}

	/**
	 * @param context
	 * @return
	 */
	private boolean isCalculationResource(ITypeInfoContext context)
	{
		return isTableNodeResource(context, SolutionSerializer.CALCULATIONS_POSTFIX);
	}

	private boolean isFoundsetResource(ITypeInfoContext context)
	{
		return isTableNodeResource(context, SolutionSerializer.FOUNDSET_POSTFIX);
	}

	private boolean isTableNodeResource(ITypeInfoContext context, String postfix)
	{
		IResource resource = context.getModelElement().getResource();
		if (resource != null)
		{
			IPath path = resource.getProjectRelativePath();
			return path.segmentCount() == 3 && path.segment(0).equals(SolutionSerializer.DATASOURCES_DIR_NAME) && path.segment(2).endsWith(postfix);
		}
		return false;
	}

	/**
	 * @param file
	 * @return
	 */


	private String getTypeName(ITypeInfoContext context, String name)
	{
		ITypeNameCreator nameCreator = typeNameCreators.get(name);
		if (nameCreator != null) return nameCreator.getTypeName(context, name);
		return constantTypeNames.get(name);
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

	private class ScopesTypeNameCreator implements ITypeNameCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
			if (fs != null)
			{
				return "Scopes<" + fs.getMainSolutionMetaData().getName() + '>';
			}
			return "Scopes";
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
