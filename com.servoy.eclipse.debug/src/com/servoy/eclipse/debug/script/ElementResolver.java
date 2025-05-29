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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.javascript.typeinfo.IElementResolver;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.ITypeNames;
import org.eclipse.dltk.javascript.typeinfo.ReferenceSource;
import org.eclipse.dltk.javascript.typeinfo.TypeMemberQuery;
import org.eclipse.dltk.javascript.typeinfo.TypeUtil;
import org.eclipse.dltk.javascript.typeinfo.model.JSType;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Parameter;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.SimpleType;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.dltk.javascript.typeinfo.model.Visibility;
import org.eclipse.jface.resource.ImageDescriptor;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyDeveloperProject;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.ViewFoundSet;
import com.servoy.j2db.documentation.scripting.docs.Globals;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.solutionmodel.developer.IJSDeveloperBridge;
import com.servoy.j2db.scripting.solutionmodel.developer.IJSDeveloperSolutionModel;
import com.servoy.j2db.util.DataSourceUtils;

/**
 * Class that resolves names in javascript like application or controller to a {@link Property} with a reference to the {@link Type} of that name.
 * It also lists all possible global names for the current context for the code completion to use.
 *
 * @author jcompagner
 * @since 6.0
 */
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
	private final Map<String, String> serversideScriptingNamesAndDoc = new HashMap<>();

	public ElementResolver()
	{
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_APPLICATION, new SimpleNameTypeNameCreator("JSApplication"));
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_EVENTS_MANAGER, new SimpleNameTypeNameCreator("JSEventsManager"));
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_SECURITY, new SimpleNameTypeNameCreator("JSSecurity"));
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_I18N, new SimpleNameTypeNameCreator("JSI18N"));
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_HISTORY, new SimpleNameTypeNameCreator("HistoryProvider"));
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_MENUS, new SimpleNameTypeNameCreator("MenuManager"));
//		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_EVENTTYPES, new SimpleNameTypeNameCreator("EventType"));
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_UTILS, new SimpleNameTypeNameCreator("JSUtils"));
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_CLIENTUTILS, new SimpleNameTypeNameCreator("JSClientUtils"));
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_JSUNIT, new SimpleNameTypeNameCreator("JSUnit"));
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER, new SimpleNameTypeNameCreator("JSSolutionModel"));
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER, new SimpleNameTypeNameCreator("JSDatabaseManager"));
		typeNameCreators.put("controller", new SimpleNameTypeNameCreator("Controller"));
		typeNameCreators.put("currentcontroller", new SimpleNameTypeNameCreator("Controller"));

		typeNameCreators.put("foundset", new FoundsetTypeNameCreator());
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_PLUGINS, new SimpleNameTypeNameCreator("Plugins"));
		typeNameCreators.put("elements", new ElementsTypeNameCreator());
		typeNameCreators.put("containers", new ContainersTypeNameCreator());
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_FORMS, new FormsNameCreator());
		typeNameCreators.put(ScriptVariable.SCOPES, new ScopesTypeNameCreator());
		typeNameCreators.put("alldataproviders", new SimpleNameTypeNameCreator("Array"));
		typeNameCreators.put(IExecutingEnviroment.TOPLEVEL_DATASOURCES, new SimpleNameTypeNameCreator("JSDataSources"));

		formOnlyNames.add("controller");
		formOnlyNames.add("alldataproviders");
		formOnlyNames.add("foundset");
		formOnlyNames.add("elements");
		formOnlyNames.add("containers");

		noneFoundsetNames.add("currentcontroller");
		noneFoundsetNames.add(IExecutingEnviroment.TOPLEVEL_HISTORY);
		noneFoundsetNames.add(IExecutingEnviroment.TOPLEVEL_JSUNIT);
		noneFoundsetNames.add(IExecutingEnviroment.TOPLEVEL_FORMS);

		noneCalcNames.addAll(noneFoundsetNames); // all filtered out for foundset methods is also filtered out for calcs
		noneCalcNames.add(IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER);

		serversideScriptingNamesAndDoc.put("servoyApi",
			"Provides utility methods for web object server side scripting to interact with the Servoy environment.");
		serversideScriptingNamesAndDoc.put("console", "Supports console logging in the serverside scripting code of web objects.");

		deprecated.add("alldataproviders");
		deprecated.add("currentcontroller");

		constantTypeNames.put("EventType", "EventType");
		constantTypeNames.put("JSPermission", "JSPermission");
	}


	public Set<String> listGlobals(ITypeInfoContext context, String prefix)
	{
		Set<String> typeNames = Collections.emptySet();
		FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
		IResource resource = context.getModelElement().getResource();
		String projectName = getProjectName(context);
		if (projectName != null &&
			ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(projectName) instanceof ServoyDeveloperProject)
		{
			typeNames = new HashSet<String>();
			typeNames.add("developerBridge");
		}
		else if (resource != null && fs != null)
		{
			typeNames = getTypeNames(prefix);

			// we only want currently to show code completion for servoyDeveloper inside Interactive Scripting Console
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
				// only add the globals. prefix when it is a scope but no the globals itself.
				if (!path.lastSegment().equals("globals.js")) typeNames.add(ScriptVariable.GLOBAL_SCOPE);
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
				Form form = getForm(context);
				if (form != null)
				{
					Form formToUse = fs.getFlattenedForm(form);
					if (!form.isResponsiveLayout() && !formToUse.containsResponsiveLayout())
					{
						typeNames.remove("containers");
					}
					typeNames.add(ScriptVariable.GLOBAL_SCOPE);

					if (form.getExtendsID() > 0)
					{
						typeNames.add("_super");
					}
					try
					{
						Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(fs.getTable(formToUse.getDataSource()));
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
						Iterator<Relation> relations = fs.getRelations(fs.getTable(formToUse.getDataSource()), true, false);
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
				ITable table = getDatasourceTable(context, fs);
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
						Map<String, IDataProvider> allDataProvidersForTable = null;
						try
						{
							allDataProvidersForTable = fs.getAllDataProvidersForTable(table);
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError("Cant get dataproviders of " + table + " for file " + context.getModelElement().getResource(), e);
						}
						for (Member member : new TypeMemberQuery(type))
						{
							IDataProvider dataProvider = allDataProvidersForTable.get(member.getName());
							if (dataProvider == null || dataProvider instanceof AggregateVariable)
							{
								typeNames.add(member.getName());
							}
						}
					}
				}
			}
		}
		try
		{
			if (resource != null && resource.getProject() != null && resource.getProject().hasNature(ServoyNGPackageProject.NATURE_ID) &&
				resource.getName().endsWith("_server.js"))
			{
				if (typeNames.isEmpty()) typeNames = new HashSet<>();
				typeNames.addAll(serversideScriptingNamesAndDoc.keySet());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
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
				if (name.startsWith(TypeCreator.PLUGIN_TYPE_PREFIX)) name = name.substring(name.lastIndexOf(".") + 1);
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
	private ITable getDatasourceTable(ITypeInfoContext context, FlattenedSolution fs)
	{
		ITable table = null;
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
				IServer server = DataSourceUtils.INMEM_DATASOURCE.equals(serverTablename[0])
					? ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(resource.getProject().getName()).getMemServer()
					: fs.getSolution().getServer(serverTablename[0]);
				if (server != null) table = server.getTable(serverTablename[1]);
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
		Set<Member> members = resolveElements(context, name);
		return members != null && !members.isEmpty() ? members.iterator().next() : null;
	}

	@Override
	public Set<Member> resolveElements(ITypeInfoContext context, String name)
	{
		Set<Member> members = new HashSet<Member>();
		if (TypeCreator.BASE_TYPES.contains(name)) return null;

		FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
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
			String desc = TypeCreator.getTopLevelDoc(Globals.class);
			if (desc != null) property.setDescription(desc);
			members.add(property);
		}

		if ("_super".equals(name))
		{
			Form form = getForm(context);
			if (form != null && form.getExtendsID() > 0 && fs != null)
			{
				Form superForm = fs.getForm(form.getExtendsID());
				if (superForm != null)
				{
					Property property = TypeCreator.createProperty("_super", true, (JSType)null, null,
						TypeCreator.getImageDescriptorForFormEncapsulation(superForm.getEncapsulation()));
					property.setDescription(TypeCreator.getDoc("_super", com.servoy.j2db.documentation.scripting.docs.Form.class, null));
					property.setAttribute(TypeCreator.LAZY_VALUECOLLECTION, superForm);
					property.setAttribute(ValueCollectionProvider.SUPER_SCOPE, Boolean.TRUE);
					members.add(property);
				}
			}
			return members;
		}
		// some stuff that should just not report anything.
		if ("document".equals(name)) // dom model
		{
			Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
			property.setName(name);
			property.setReadOnly(true);
			property.setType(TypeInfoModelFactory.eINSTANCE.createAnyType());
			members.add(property);
		}
		else if ("$".equals(name))
		{
			Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
			Parameter param = TypeInfoModelFactory.eINSTANCE.createParameter();
			param.setName("selector");
			method.getParameters().add(param);
			method.setType(TypeInfoModelFactory.eINSTANCE.createAnyType());
			method.setName("$");
			members.add(method);
		}

		IResource ctxResource = context.getModelElement().getResource();
		try
		{
			if (ctxResource != null && ctxResource.exists() && ctxResource.getProject() != null &&
				ctxResource.getProject().hasNature(ServoyNGPackageProject.NATURE_ID) && ctxResource.getName().endsWith("_server.js") &&
				serversideScriptingNamesAndDoc.containsKey(name))
			{
				if (ServoyModelFinder.getServoyModel().getActiveProject() == null) return null; // in this case TypeCreator would not create the needed type; avoid generating a resolve stack overflow
				Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
				property.setName(name);
				property.setReadOnly(true);
				property.setType(context.getTypeRef(name));
				property.setDescription(serversideScriptingNamesAndDoc.get(name));
				members.add(property);
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Cannot resolve " + name, e);
		}
		// try to resolve first based on existing types as defined by TypeCreator (so that deprecated & other things are in sync)
		if (ctxResource != null && ctxResource.exists() && fs != null && fs.getSolution() != null)
		{
			IPath path = ctxResource.getProjectRelativePath();
			if (path.segmentCount() == 1)
			{
				// globals.js (or other scope)
				Type type = context.getType("Scope<" + fs.getSolution().getName() + "/" +
					path.segment(0).substring(0, path.segment(0).length() - SolutionSerializer.JS_FILE_EXTENSION.length()) + '>');
				members.addAll(TypeCreator.getMembers(name, type));
			}
			else if (path.segmentCount() == 2 && path.segment(0).equals(SolutionSerializer.FORMS_DIR))
			{
				// forms/formname.js
				Form form = getForm(context);
				if (form != null)
				{
					Type type = context.getType("RuntimeForm<" + form.getName() + '>');
					Set<Member> set = TypeCreator.getMembers(name, type);
					for (Member m : set)
					{
						if (m != null)
						{
							boolean memberTypeVisible = true;
							JSType memberType = m.getType();
							if (memberType instanceof SimpleType && ((SimpleType)memberType).getTarget() != null)
							{
								memberTypeVisible = ((SimpleType)memberType).getTarget().isVisible();
							}
							if (!m.isVisible() && memberTypeVisible)
							{
								m = TypeCreator.clone(m, null);
								m.setVisible(true);
								m.setVisibility(Visibility.PUBLIC);
							}
							else if (!memberTypeVisible && m.isVisible())
							{
								m = TypeCreator.clone(m, null);
								m.setVisible(false);
								m.setVisibility(Visibility.INTERNAL);
							}
							members.add(m);
						}
					}
				}
			}
			else if (path.segmentCount() == 3 && path.segment(0).equals(SolutionSerializer.DATASOURCES_DIR_NAME))
			{
				// datasources/server/table_foundset.js or datasources/server/table_calculations.js
				ITable table = getDatasourceTable(context, fs);
				if (table != null)
				{
					if (path.segment(2).endsWith(SolutionSerializer.FOUNDSET_POSTFIX))
					{
						// datasources/server/table_foundset.js
						Type type = context.getType(FoundSet.JS_FOUNDSET + '<' + table.getDataSource() + '>');
						if (type != null)
						{
							// TODO do we still need this code
							members.addAll(TypeCreator.getMembers(name, type));
						}
					}
				}
			}
			if (!members.isEmpty())
			{
				//return members if found in existing types defined by TypeCreator
				return members;
			}
		}

		String typeName;
		String description = null;
		if ("servoyDeveloper".equals(name))
		{
			if (ServoyModelFinder.getServoyModel().getActiveProject() == null) return null; // in this case TypeCreator would not create the needed type; avoid generating a resolve stack overflow
			typeName = name;
			description = TypeCreator.getTopLevelDoc(IJSDeveloperSolutionModel.class);
		}
		else if ("developerBridge".equals(name))
		{
			String projectName = getProjectName(context);
			if (projectName != null &&
				ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(projectName) instanceof ServoyDeveloperProject)
			{
				typeName = name;
				description = TypeCreator.getTopLevelDoc(IJSDeveloperBridge.class);
			}
			else
			{
				typeName = null;
			}
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
			if (SolutionSerializer.getFormNameFromFile(context.getModelElement().getResource()) == null && formOnlyNames.contains(name))
			{
				// hide in other then forms
				return null;
			}
			type = context.getType(typeName);
		}
		boolean readOnly = true;
		ImageDescriptor image = null;
		Object resource = null;
		String deprecatedText = null;
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
					deprecatedText = relation.getDeprecated();
					try
					{
						description = TypeCreator.getRelationDescription(relation, relation.getPrimaryDataProviders(fs), relation.getForeignColumns(fs));
					}
					catch (RepositoryException e)
					{
//						ServoyLog.logInfo("Type creator - cannot read relation; problem markers probably already exist for this it: " + e.getMessage());
						description = "Relation with errors (check for problem markers):" + e.getMessage();
					}
				}
				else
				{
					Form form = getForm(context);
					ITable table = null;
					if (form != null)
					{
						table = fs.getTable(form.getDataSource());
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
								Property property = TypeCreator.createProperty(name, readOnly, (JSType)null, null, image, resource);
								property.setHideAllowed(hideAllowed);
								members.add(property);
							}
						}
						else if (isCalculationResource(context) && name.equals("getDataSource"))
						{
							Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
							method.setName(name);
							method.setType(TypeUtil.ref(ITypeNames.STRING));
							method.setAttribute(TypeCreator.IMAGE_DESCRIPTOR, TypeCreator.METHOD);
							members.add(method);
						}
						else if (isFoundsetResource(context))
						{
							Type foundsetType = context.getType(FoundSet.JS_FOUNDSET + '<' + table.getDataSource() + '>');
							Member member = new TypeMemberQuery(foundsetType).findMember(name);
							if (member != null)
							{
								// TODO do we still need this code
								members.add(member);
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
			else if (type.getName().equals("Any"))
			{
				typeRef = TypeInfoModelFactory.eINSTANCE.createAnyType();
			}
			else
			{
				typeRef = TypeUtil.ref(type);
			}
			if (description == null) description = type.getDescription();
			if ("currentcontroller".equals(name))
			{
				description = TypeCreator.getDoc("currentcontroller", com.servoy.j2db.documentation.scripting.docs.Globals.class, null);
			}
			Property property = TypeCreator.createProperty(name, readOnly, typeRef, description, image, resource, deprecatedText);
			property.setHideAllowed(hideAllowed);
			if (deprecated.contains(name))
			{
				property.setDeprecated(true);
				property.setVisible(false);
			}
			else
			{
				property.setDeprecated(property.isDeprecated() || type.isDeprecated());
				property.setVisible(type.isVisible());
			}
			members.add(property);
		}
		return members;
	}

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
			Form form = getForm(context);
			if (form != null)
			{
				return "Elements<" + form.getName() + '>';
			}
			return "Elements";
		}
	}

	private class ContainersTypeNameCreator implements ITypeNameCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			Form form = getForm(context);
			if (form != null)
			{
				return "RuntimeContainers<" + form.getName() + '>';
			}
			return "RuntimeContainers";
		}
	}

	private class ScopesTypeNameCreator implements ITypeNameCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public String getTypeName(ITypeInfoContext context, String fullTypeName)
		{
			FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
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
			FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
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
			Form form = getForm(context);
			if (form != null && form.getDataSource() != null)
			{
				if (form.getDataSource().startsWith(DataSourceUtils.VIEW_DATASOURCE_SCHEME_COLON))
				{
					return ViewFoundSet.VIEW_FOUNDSET + '<' + form.getDataSource() + '>';
				}
				return FoundSet.JS_FOUNDSET + '<' + form.getDataSource() + '>';
			}
			return FoundSet.JS_FOUNDSET;
		}
	}

	public interface ITypeNameCreator
	{
		public String getTypeName(ITypeInfoContext context, String typeName);
	}

	public static String getProjectName(ITypeInfoContext context)
	{
		IResource resource = null;
		ReferenceSource rs = context.getSource();
		if (rs != null)
		{
			resource = rs.getSourceModule().getResource();
		}
		if (resource == null && context.getModelElement() != null)
		{
			resource = context.getModelElement().getResource();
		}
		if (resource == null)
		{
			return null;
		}
		return resource.getProject().getName();
	}

	public static FlattenedSolution getFlattenedSolution(ITypeInfoContext context)
	{
		return getFlattenedSolution(getProjectName(context));
	}


	/**
	 * @param name
	 */
	public static FlattenedSolution getFlattenedSolution(String name)
	{
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		if (name != null && servoyModel.isSolutionActive(name))
		{
			ServoyProject servoyProject = servoyModel.getServoyProject(name);
			if (servoyProject != null)
			{
				return servoyProject.getEditingFlattenedSolution();
			}
		}
		return null;
	}

	/**
	 * @param context
	 */
	public static Form getForm(ITypeInfoContext context)
	{
		String formName = SolutionSerializer.getFormNameFromFile(context.getModelElement().getResource());
		if (formName != null)
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs != null)
			{
				return fs.getForm(formName);
			}
		}
		return null;
	}
}
