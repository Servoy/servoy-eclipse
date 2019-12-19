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
package com.servoy.eclipse.ui.views.solutionexplorer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.wicket.util.io.IOUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.javascript.ast.AbstractNavigationVisitor;
import org.eclipse.dltk.javascript.ast.BinaryOperation;
import org.eclipse.dltk.javascript.ast.CallExpression;
import org.eclipse.dltk.javascript.ast.Comment;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.ObjectInitializer;
import org.eclipse.dltk.javascript.ast.PropertyExpression;
import org.eclipse.dltk.javascript.ast.PropertyInitializer;
import org.eclipse.dltk.javascript.ast.ReturnStatement;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.parser.JavaScriptParser;
import org.eclipse.dltk.javascript.scriptdoc.JavaDoc2HTMLTextReader;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.specification.property.types.StyleClassPropertyType;

import com.servoy.base.persistence.constants.IRepositoryConstants;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.base.util.ITagResolver;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.inmemory.MemServer;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.util.DataSourceWrapperFactory;
import com.servoy.eclipse.model.util.IDataSourceWrapper;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.model.view.ViewFoundsetsServer;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.labelproviders.RelationLabelProvider;
import com.servoy.eclipse.ui.node.IDeveloperFeedback;
import com.servoy.eclipse.ui.node.IImageLookup;
import com.servoy.eclipse.ui.node.SimpleDeveloperFeedback;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.TreeBuilder;
import com.servoy.eclipse.ui.node.UserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.scripting.CalculationModeHandler;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.j2db.BasicFormController.JSForm;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormManager.HistoryProvider;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.dataprocessing.datasource.JSDataSource;
import com.servoy.j2db.dataprocessing.datasource.JSDataSources;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.DocumentationUtil;
import com.servoy.j2db.documentation.IParameter;
import com.servoy.j2db.documentation.XMLScriptObjectAdapter;
import com.servoy.j2db.documentation.scripting.docs.FormElements;
import com.servoy.j2db.documentation.scripting.docs.Forms;
import com.servoy.j2db.documentation.scripting.docs.RuntimeContainer;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IItemChangeListener;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Procedure;
import com.servoy.j2db.persistence.ProcedureColumn;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.DeclaringClassJavaMembers;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.ITypedScriptObject;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSI18N;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.JSUnitAssertFunctions;
import com.servoy.j2db.scripting.JSUtils;
import com.servoy.j2db.scripting.RuntimeGroup;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.annotations.AnnotationManagerReflection;
import com.servoy.j2db.scripting.annotations.JSSignature;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.ui.runtime.HasRuntimeClientProperty;
import com.servoy.j2db.ui.runtime.HasRuntimeDesignTimeProperty;
import com.servoy.j2db.ui.runtime.HasRuntimeElementType;
import com.servoy.j2db.ui.runtime.HasRuntimeFormName;
import com.servoy.j2db.ui.runtime.HasRuntimeName;
import com.servoy.j2db.ui.runtime.HasRuntimeStyleClass;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

public class SolutionExplorerListContentProvider implements IStructuredContentProvider, IImageLookup, IPersistChangeListener, IItemChangeListener<IColumn>
{
	private final SolutionExplorerView view;

	private final Map<Object, Object> leafList = new HashMap<Object, Object>();

	private static final SimpleUserNode[] EMPTY_LIST = new SimpleUserNode[0];

	public static Set<String> ignoreMethods = new TreeSet<String>();

	public static Set<String> ignoreMethodsFromPrefixedConstants = new TreeSet<String>();

	private final static com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();

	private final Image propertiesIcon;
	private final Image specialPropertiesIcon;
	private final Image functionIcon;

	private boolean includeModules = false;
	private boolean showInheritedMethods = true;

	private final Map<ITable, List<Object>> usedTables = new HashMap<ITable, List<Object>>();

	static
	{
		Method[] methods = Object.class.getMethods();
		for (Method method : methods)
		{
			ignoreMethods.add(method.getName());
		}
		methods = IPrefixedConstantsObject.class.getDeclaredMethods();
		for (Method method : methods)
		{
			ignoreMethodsFromPrefixedConstants.add(method.getName());
		}
	}

	public static final String PLUGIN_PREFIX = "plugins";

	public static final FieldComparator fieldComparator = new FieldComparator();

	public static final MethodComparator methodComparator = new MethodComparator();

	static class FieldComparator implements Comparator<Field>
	{
		private FieldComparator()
		{
		}

		public int compare(Field o1, Field o2)
		{
			return o1.getName().compareToIgnoreCase(o2.getName());
		}
	}

	static class MethodComparator implements Comparator<Method>
	{
		private MethodComparator()
		{
		}

		public int compare(Method o1, Method o2)
		{
			return o1.getName().compareToIgnoreCase(o2.getName());
		}
	}

	SolutionExplorerListContentProvider(SolutionExplorerView v)
	{
		view = v;
		propertiesIcon = loadImage("properties.png");
		specialPropertiesIcon = loadImage("special_properties.png");
		functionIcon = uiActivator.loadImageFromBundle("function.png");
	}

	public void dispose()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel().removePersistChangeListener(true, this);
		Set<ITable> keySet = usedTables.keySet();
		for (ITable table : keySet)
		{
			table.removeIColumnListener(this);
		}
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
	}

	public Image loadImage(String name)
	{
		Image img = uiActivator.loadImageFromBundle(name);
		if (img == null)
		{
			img = uiActivator.loadImageFromOldLocation(name);
		}
		return img;
	}

	public Object[] getElements(Object inputElement)
	{
		if (inputElement instanceof SimpleUserNode)
		{
			SimpleUserNode un = ((SimpleUserNode)inputElement);
			try
			{
				CalculationModeHandler cm = CalculationModeHandler.getInstance();
				Object[] list = getList(un);
				for (Object node : list)
				{
					if (node instanceof SimpleUserNode)
					{
						((SimpleUserNode)node).parent = un;
					}
				}
				if (cm.hasPartialList(un.getName()))
				{
					ArrayList<Object> al = new ArrayList<Object>();
					for (Object node : list)
					{
						if (node instanceof SimpleUserNode)
						{
							if (cm.hide(un.getName(), ((SimpleUserNode)node).getName())) continue;
						}
						al.add(node);
					}
					list = al.toArray();
				}
				return list;
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
		return EMPTY_LIST;
	}

	protected Object[] getList(SimpleUserNode un) throws RepositoryException
	{
		UserNodeType type = un.getType();
		if (type == null) return EMPTY_LIST;

		// see if the data for this node is cached (data that is unlikely to
		// change or does not change during runtime)
		Object key = type;
		Object mapKey = type;
		if (type == UserNodeType.PLUGIN)
		{
			key = key + un.getName();
		}
		else if (type == UserNodeType.BEAN)
		{
			Bean b = (Bean)un.getRealObject();
			key = key + b.getBeanClassName();
		}
		else if (type == UserNodeType.SERVER)
		{
			key = key + un.getName();
		}
		else if (type == UserNodeType.RETURNTYPE)
		{
			Object real = un.getRealObject();
			Class cls = null;
			if (real instanceof Class)
			{
				cls = (Class)real;
			}
			else
			{
				cls = real.getClass();
			}
			key = key + cls.getName();
		}
		else if (type == UserNodeType.CURRENT_FORM || type == UserNodeType.FORM_CONTROLLER || type == UserNodeType.MEDIA)
		{
			key = type;
		}
		else if (un.getRealObject() instanceof IPersist)
		{
			key = ((IPersist)un.getRealObject()).getUUID();
		}
		else if (un.getRealObject() instanceof Object[] && ((Object[])un.getRealObject())[0] instanceof Bean)
		{
			key = ((IPersist)((Object[])un.getRealObject())[0]).getUUID();
			Object beanClass = ((Object[])un.getRealObject())[1];
			mapKey = (beanClass == null ? "null" : beanClass.toString()); // tostring of the class
		}
		else if (un.getRealObject() instanceof Object[] && ((Object[])un.getRealObject())[0] instanceof IPersist)
		{
			key = ((IPersist)((Object[])un.getRealObject())[0]).getUUID();
		}
		else if (type == UserNodeType.TABLE_COLUMNS)
		{
			key = un.getRealObject();
		}
		else if (!(type == UserNodeType.PLUGINS || type == UserNodeType.STRING || type == UserNodeType.NUMBER || type == UserNodeType.DATE ||
			type == UserNodeType.ARRAY || type == UserNodeType.OBJECT || type == UserNodeType.STATEMENTS || type == UserNodeType.SPECIAL_OPERATORS ||
			type == UserNodeType.XML_METHODS || type == UserNodeType.XML_LIST_METHODS || type == UserNodeType.FUNCTIONS || type == UserNodeType.FORM_ELEMENTS ||
			type == UserNodeType.JSON))
		// if (type !=  UserNodeType.OTHER_CACHED_RETURN_TYPES_THAT_DO_NOT_MODIFY_THE_KEY)
		{
			// THE DATA FOR THIS TYPE OF NODES IS NOT CACHED
			key = null;
		}

		Object lst = leafList.get(key);
		Object[] lm = null;
		Map<Object, Object[]> parentMap = null;
		if (lst instanceof Map)
		{
			parentMap = (Map<Object, Object[]>)lst;
			lm = parentMap.get(mapKey);
		}
		else
		{
			if (key instanceof UUID)
			{
				parentMap = new HashMap<Object, Object[]>(3);
				leafList.put(key, parentMap);
			}
			lm = (Object[])lst;
		}
		if (lm == null)
		{
			if (type == UserNodeType.STYLES)
			{
				un.setRealObject(ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject());
				lm = createStyles();
			}
			else if (type == UserNodeType.I18N_FILES)
			{
				lm = createI18NFiles();
			}
			else if (type == UserNodeType.COMPONENT || type == UserNodeType.SERVICE || type == UserNodeType.LAYOUT)
			{
				lm = createComponentFileList(un);
				key = null;
			}
			else if (type == UserNodeType.WEB_OBJECT_FOLDER)
			{
				lm = createWebObjectFileList(un);
				key = null;
			}
			else if (type == UserNodeType.TEMPLATES)
			{
				un.setRealObject(ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject());
				lm = createTemplates();
			}
			else if (type == UserNodeType.TABLE_COLUMNS)
			{
				lm = createTableColumns((ITable)un.getRealObject(), un);
			}
			else if (type == UserNodeType.PROCEDURES)
			{
				lm = createProcedures((IServerInternal)un.getRealObject(), UserNodeType.PROCEDURE);
			}
			else if (type == UserNodeType.SERVER && ServoyModel.isClientRepositoryAccessAllowed(((IServerInternal)un.getRealObject()).getName()))
			{
				lm = createTables((IServerInternal)un.getRealObject(), UserNodeType.TABLE);
			}
			else if (type == UserNodeType.INMEMORY_DATASOURCES)
			{
				lm = createInMemTables(((MemServer)un.getRealObject()).getServoyProject(), includeModules);
			}
			else if (type == UserNodeType.VIEW_FOUNDSETS)
			{
				lm = createViewFoundsets(((ViewFoundsetsServer)un.getRealObject()).getServoyProject(), includeModules);
			}
			else if (type == UserNodeType.VIEWS && ServoyModel.isClientRepositoryAccessAllowed(((IServerInternal)un.getRealObject()).getName()))
			{
				lm = createViews((IServerInternal)un.getRealObject());
			}
			else if (type == UserNodeType.GLOBALS_ITEM)
			{
				lm = createGlobalScripts(un);
			}
			else if (type == UserNodeType.GLOBAL_VARIABLES)
			{
				lm = createGlobalVariables(un);
			}
			else if (type == UserNodeType.FORM_VARIABLES)
			{
				lm = createFormVariables(un);
			}
			else if (type == UserNodeType.VALUELISTS)
			{
				lm = createValueLists(un);
			}
			else if (type == UserNodeType.MEDIA || type == UserNodeType.MEDIA_FOLDER)
			{
				lm = createMedia(un);
			}
			else if (type == UserNodeType.FORM)
			{
				Form currentForm = (Form)un.getRealObject();
				lm = createFormScripts(currentForm);
			}
			else if (type == UserNodeType.RELATIONS)
			{
				// allrelations DEPRECATED in favour of solutionModel
			}
			else if (type == UserNodeType.RELATION)
			{
				Relation r = (Relation)un.getRealObject();
				lm = createRelation(r, false);
			}
			else if (type == UserNodeType.GLOBALRELATIONS)
			{
				// allrelations DEPRECATED in favour of solutionModel
			}
			else if (type == UserNodeType.ALL_RELATIONS)
			{
				Solution sol = (Solution)un.getRealObject();
				lm = createAllRelations(sol);
			}
			else if (type == UserNodeType.APPLICATION)
			{
				lm = getJSMethods(JSApplication.class, "application", null, UserNodeType.APPLICATION_ITEM, null, null);
			}
			else if (type == UserNodeType.HISTORY)
			{
				lm = getJSMethods(HistoryProvider.class, "history", null, UserNodeType.HISTORY_ITEM, null, null);
			}
			else if (type == UserNodeType.SOLUTION_MODEL)
			{
				lm = getJSMethods(JSSolutionModel.class, IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER, null, UserNodeType.SOLUTION_MODEL_ITEM, null, null);
			}
			else if (type == UserNodeType.I18N)
			{
				lm = getJSMethods(JSI18N.class, "i18n", null, UserNodeType.I18N_ITEM, null, null);
			}
			else if (type == UserNodeType.EXCEPTIONS)
			{
				lm = getJSMethods(ServoyException.class, ".", null, UserNodeType.EXCEPTIONS_ITEM, null, null);
			}
			else if (type == UserNodeType.UTILS)
			{
				lm = getJSMethods(JSUtils.class, "utils", null, UserNodeType.UTIL_ITEM, null, null);
			}
			else if (type == UserNodeType.JSUNIT)
			{
				lm = getJSMethods(JSUnitAssertFunctions.class, IExecutingEnviroment.TOPLEVEL_JSUNIT, null, UserNodeType.JSUNIT_ITEM, null, null);
			}
			else if (type == UserNodeType.SECURITY)
			{
				lm = getJSMethods(JSSecurity.class, "security", null, UserNodeType.SECURITY_ITEM, null, null);
			}
			else if (type == UserNodeType.FUNCTIONS)
			{
				lm = TreeBuilder.createJSMathFunctions(this);
			}
			else if (type == UserNodeType.JSON)
			{
				lm = TreeBuilder.createJSONFunctions(this);
			}
			else if (type == UserNodeType.XML_METHODS)
			{
				lm = TreeBuilder.createXMLMethods(this);
			}
			else if (type == UserNodeType.XML_LIST_METHODS)
			{
				lm = TreeBuilder.createXMLListMethods(this);
			}
			else if (type == UserNodeType.CURRENT_FORM)
			{
				lm = getJSMethods(JSForm.class, "currentcontroller.", "current", UserNodeType.CURRENT_FORM_ITEM, null, null);
			}
			else if (type == UserNodeType.FORM_CONTROLLER)
			{
				lm = getJSMethods(JSForm.class, "controller.", null, UserNodeType.FORM_CONTROLLER_FUNCTION_ITEM, null, null);
			}
			else if (type == UserNodeType.FORMS)
			{
				lm = TreeBuilder.docToNodes(Forms.class, this, UserNodeType.ARRAY, "forms.", null);
			}
			else if (type == UserNodeType.PLUGINS)
			{
				lm = TreeBuilder.createLengthAndArray(this, PLUGIN_PREFIX);
			}
			else if (type == UserNodeType.STRING)
			{
				lm = TreeBuilder.createJSString(this);
			}
			else if (type == UserNodeType.NUMBER)
			{
				lm = TreeBuilder.createTypedArray(this, com.servoy.j2db.documentation.scripting.docs.Number.class, UserNodeType.NUMBER, null);
			}
			else if (type == UserNodeType.DATE)
			{
				lm = TreeBuilder.createJSDate(this);
			}
			else if (type == UserNodeType.ARRAY)
			{
				lm = TreeBuilder.createJSArray(this);
			}
			else if (type == UserNodeType.OBJECT)
			{
				lm = TreeBuilder.createJSObject(this);
			}
			else if (type == UserNodeType.REGEXP)
			{
				lm = TreeBuilder.createJSRegexp(this);
			}
			else if (type == UserNodeType.STATEMENTS)
			{
				lm = TreeBuilder.createFlows(this);
			}
			else if (type == UserNodeType.SPECIAL_OPERATORS)
			{
				lm = TreeBuilder.createTypedArray(this, com.servoy.j2db.documentation.scripting.docs.SpecialOperators.class, UserNodeType.SPECIAL_OPERATORS,
					null);
			}
			else if (type == UserNodeType.FOUNDSET_MANAGER)
			{
				lm = getJSMethods(JSDatabaseManager.class, IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER, null, UserNodeType.FOUNDSET_MANAGER_ITEM, null,
					null);
			}
			else if (type == UserNodeType.DATASOURCES)
			{
				lm = getJSMethods(JSDataSources.class, IExecutingEnviroment.TOPLEVEL_DATASOURCES, null, UserNodeType.FOUNDSET_MANAGER_ITEM, null, null);
			}
			else if (type == UserNodeType.INMEMORY_DATASOURCE)
			{
				String prefix = '.' + DataSourceUtils.INMEM_DATASOURCE + '.' + ((IDataSourceWrapper)un.getRealObject()).getTableName();
				lm = getJSMethods(JSDataSource.class, IExecutingEnviroment.TOPLEVEL_DATASOURCES + prefix, null, UserNodeType.FOUNDSET_MANAGER_ITEM, null, null);
			}
			else if (type == UserNodeType.TABLE)
			{
				String prefix = '.' + DataSourceUtilsBase.DB_DATASOURCE_SCHEME + '.' + ((TableWrapper)un.getRealObject()).getServerName() + '.' +
					((TableWrapper)un.getRealObject()).getTableName();
				lm = getJSMethods(JSDataSource.class, IExecutingEnviroment.TOPLEVEL_DATASOURCES + prefix, null, UserNodeType.FOUNDSET_MANAGER_ITEM, null, null);

				ITable table = null;
				try
				{
					table = ApplicationServerRegistry.get().getServerManager().getServer(((TableWrapper)un.getRealObject()).getServerName()).getTable(
						((TableWrapper)un.getRealObject()).getTableName());
				}
				catch (RemoteException e)
				{
					ServoyLog.logError(e);
				}
				if (table != null)
				{
					Object[] tableColumns = createTableColumns(table, un);
					Object[] newElements = new Object[lm.length + tableColumns.length];

					System.arraycopy(tableColumns, 0, newElements, 0, tableColumns.length);
					System.arraycopy(lm, 0, newElements, tableColumns.length, lm.length);

					lm = newElements;
				}
			}
			else if (type == UserNodeType.FORM_ELEMENTS)
			{
				lm = TreeBuilder.docToNodes(FormElements.class, this, UserNodeType.ARRAY, "elements.", null);
			}
			else if (type == UserNodeType.FORM_ELEMENTS_GROUP)
			{
				Object[] real = (Object[])un.getRealObject();
				lm = getJSMethods(RuntimeGroup.class, "elements." + ((FormElementGroup)real[0]).getGroupID(), null, UserNodeType.FORM_ELEMENTS_ITEM_METHOD,
					null, null);// TODO fix multiple anonymous groups
			}
			else if (type == UserNodeType.FORM_ELEMENTS_ITEM)
			{
				Object[] real = (Object[])un.getRealObject(); // [IPersist, Class]
				Object model = real[0];
				Class specificClass = (Class)real[1];
				String prefix = "elements.";
				if (model instanceof ISupportName)
				{
					prefix += ((ISupportName)model).getName();
				}

				if (model instanceof IBasicWebComponent && FormTemplateGenerator.isWebcomponentBean((IBasicWebComponent)model))
				{
					lm = getWebComponentMembers(prefix, (IBasicWebComponent)model);
					key = null; // for now don't cache this.
				}
				else if (specificClass == null)
				{
					try
					{
						lm = getJSMethods(ElementUtil.getPersistScriptClass(Activator.getDefault().getDesignClient(), model), prefix, null,
							UserNodeType.FORM_ELEMENTS_ITEM_METHOD, null, null);
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}
				else
				{
					// this is a sub-type (for example, the JComponent sub-type of a
					// JSplitPane element)
					try
					{
						Class beanClass = specificClass;
						if (model instanceof Bean)
						{
							IApplication application = Activator.getDefault().getDesignClient();
							beanClass = application.getBeanManager().getClassLoader().loadClass(((Bean)model).getBeanClassName());
						}
						lm = getAllMethods(beanClass, specificClass, prefix, null, UserNodeType.FORM_ELEMENTS_ITEM_METHOD);
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}
			}
			// else if (type == UserNodeType.CALCULATIONS)
			// {
			// Table t = (Table)un.getRealObject();
			// key = type.toString() + t.getName();
			// lm = createCalculation(t);
			// }
			// else if (type == UserNodeType.BEAN)
			// {
			// Bean b = (Bean)un.getRealObject();
			// lm = createBean(b);
			// }
			else if (type == UserNodeType.FORM_CONTAINERS_ITEM)
			{
				lm = TreeBuilder.docToNodes(RuntimeContainer.class, this, UserNodeType.ARRAY, "containers." + un.getName() + ".", null);
			}
			else if (type == UserNodeType.CALC_RELATION)
			{
				Relation r = (Relation)un.getRealObject();
				lm = createRelation(r, true);
			}
			else if (type == UserNodeType.PLUGIN)
			{
				try
				{
					lm = getJSMethods(un.getRealObject(), PLUGIN_PREFIX + "." + un.getName(), null, UserNodeType.PLUGINS_ITEM, null, null);
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
			else if (type == UserNodeType.RETURNTYPE)
			{
				Object real = un.getRealObject();

				if (real instanceof IScriptObject)
				{
					ScriptObjectRegistry.registerScriptObjectForClass(real.getClass(), (IScriptObject)real);
				}
				else if (real instanceof IReturnedTypesProvider)
				{
					ScriptObjectRegistry.registerReturnedTypesProviderForClass(real.getClass(), (IReturnedTypesProvider)real);
				}

				Class cls = null;
				if (real instanceof Class)
				{
					cls = (Class)real;
				}
				else
				{
					cls = real.getClass();
				}
				String elementName = ".";
				if (un.parent.parent != null && un.parent.parent.getType() == UserNodeType.PLUGIN)
				{
					elementName = PLUGIN_PREFIX + "." + un.parent.parent.getName() + elementName;
				}
				lm = getJSMethods(cls, elementName, null, UserNodeType.RETURNTYPE_ELEMENT, null, null);
			}
			else if (type == UserNodeType.JSLIB)
			{
				lm = TreeBuilder.createTypedArray(this, com.servoy.j2db.documentation.scripting.docs.JSLib.class, UserNodeType.JSLIB, null);
			}
			if (lm != null && key != null)
			{
				if (parentMap != null)
				{
					parentMap.put(mapKey, lm);
				}
				else
				{
					leafList.put(key, lm);
				}
			}
		}
		if (lm == null)
		{
			lm = EMPTY_LIST;
		}
		return lm;
	}

	/**
	 * @param un
	 * @return
	 */
	private SimpleUserNode[] createComponentFileList(SimpleUserNode un)
	{
		WebObjectSpecification spec = (WebObjectSpecification)un.getRealObject();
		IProject resources = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getProject();
		if ("file".equals(spec.getSpecURL().getProtocol()))
		{
			try
			{
				IFile[] specFile = resources.getWorkspace().getRoot().findFilesForLocationURI(spec.getSpecURL().toURI());
				if (specFile.length == 1)
				{
					List<SimpleUserNode> list = createComponentList("", specFile[0].getParent());
					return list.toArray(new SimpleUserNode[list.size()]);
				}
			}
			catch (URISyntaxException e)
			{
				ServoyLog.logError(e);
			}
		}
		else
		{
			String folderName = SolutionExplorerTreeContentProvider.getFolderNameFromSpec(spec);
			if (folderName != null)
			{
				folderName += "/";
				List<SimpleUserNode> list = new ArrayList<SimpleUserNode>();
				IPackageReader reader = WebComponentSpecProvider.getSpecProviderState().getPackageReader(spec.getPackageName());
				if (reader == null) reader = WebServiceSpecProvider.getSpecProviderState().getPackageReader(spec.getPackageName());
				try (ZipFile zip = new ZipFile(reader.getResource().toURI().toURL().getFile()))
				{
					Enumeration< ? extends ZipEntry> e = zip.entries();
					while (e.hasMoreElements())
					{
						ZipEntry entry = e.nextElement();
						if (entry.getName().startsWith(folderName) && !entry.isDirectory())
						{
							PlatformSimpleUserNode node = new PlatformSimpleUserNode(entry.getName().replaceFirst(folderName, ""), UserNodeType.ZIP_RESOURCE,
								null, getIcon(entry.getName()));
							list.add(node);
						}
					}
				}
				catch (IOException e)
				{
					ServoyLog.logError(e);
				}
				return list.toArray(new SimpleUserNode[list.size()]);
			}
		}
		return new SimpleUserNode[0];
	}

	public static Image getIcon(String name)
	{
		if (name != null && name.contains("."))
		{
			String suffix = name.substring(name.lastIndexOf("."));
			if (suffix.toLowerCase().endsWith("js"))
			{
				return uiActivator.loadImageFromBundle("js_file.png");
			}
			if (suffix.toLowerCase().endsWith("css"))
			{
				return uiActivator.loadImageFromBundle("style.png");
			}
			if (suffix.toLowerCase().endsWith("png") || suffix.toLowerCase().endsWith("jpeg") || suffix.toLowerCase().endsWith("gif"))
			{
				return uiActivator.loadImageFromBundle("media.png");
			}
		}
		return uiActivator.loadImageFromBundle("file_obj.png");
	}

	private SimpleUserNode[] createWebObjectFileList(SimpleUserNode un)
	{
		IFolder folder = (IFolder)un.getRealObject();
		List<SimpleUserNode> children = new ArrayList<>();
		try
		{
			for (IResource res : folder.members(false))
			{
				if (res instanceof IFile)
				{
					children.add(new PlatformSimpleUserNode(res.getName(), UserNodeType.COMPONENT_RESOURCE, res, getIcon(res.getName())));
				}
			}
			return children.toArray(new PlatformSimpleUserNode[children.size()]);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return new SimpleUserNode[0];
	}

	/**
	 * @param folder
	 * @param resources
	 * @return
	 */
	private List<SimpleUserNode> createComponentList(String path, IContainer folder)
	{
		List<SimpleUserNode> list = new ArrayList<SimpleUserNode>();
		try
		{
			if (!folder.exists()) return list;
			IResource[] members = folder.members();
			for (IResource resource : members)
			{
				if (resource.isHidden() || resource.isTeamPrivateMember() || resource.isDerived()) continue;
				if (resource.getType() == IResource.FILE)
				{
					PlatformSimpleUserNode node = new PlatformSimpleUserNode(path + resource.getName(), UserNodeType.COMPONENT_RESOURCE, resource,
						getIcon(resource.getName()));
					list.add(node);
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		return list;
	}


	private List<IPersist> getPersists(Solution solution, UserNodeType type, String scopeName)
	{
		List<IPersist> persists = new ArrayList<IPersist>();
		try
		{
			List<Solution> modules = new ArrayList<Solution>();
			if (includeModules)
			{
				modules.addAll(solution.getReferencedModulesRecursive(new HashMap<String, Solution>()).values());
			}
			if (!modules.contains(solution))
			{
				modules.add(solution);
			}

			for (Solution module : modules)
			{
				Iterator< ? extends IPersist> it;
				switch (type)
				{
					case RELATIONS :
						it = module.getRelations(false);
						break;
					case VALUELISTS :
						it = module.getValueLists(false);
						break;
					case GLOBAL_VARIABLES :
						it = module.getScriptVariables(scopeName, false);
						break;
					case GLOBALS_ITEM :
						it = module.getScriptMethods(scopeName, false);
						break;

					default :
						it = null;
				}
				while (it != null && it.hasNext())
				{
					persists.add(it.next());
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		Collections.sort(persists, NameComparator.INSTANCE);
		return persists;
	}

	private Object[] createAllRelations(Solution solution)
	{
		// global
		List<SimpleUserNode> rels = new ArrayList<SimpleUserNode>();
		try
		{
			List<IPersist> relations = getPersists(solution, UserNodeType.RELATIONS, null);
			for (IPersist persist : relations)
			{
				Relation relation = (Relation)persist;
				SimpleUserNode un = new UserNode(getDisplayName(relation, solution), UserNodeType.RELATION,
					new SimpleDeveloperFeedback(relation.getName(), null, null), relation, RelationLabelProvider.INSTANCE_ALL.getImage(relation));
				rels.add(un);
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return rels.toArray();
	}

	private String getDisplayName(IPersist persist, Solution context)
	{
		String displayName;
		Solution persistSolution = (Solution)persist.getAncestor(IRepository.SOLUTIONS);
		if (persistSolution == null || persistSolution.equals(context))
		{
			displayName = ((ISupportName)persist).getName();
		}
		else
		{
			displayName = ((ISupportName)persist).getName() + " [" + persistSolution.getName() + ']';
		}
		return displayName;
	}

	private Object[] createStyles()
	{
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();
		List<IRootObject> styles = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.STYLES);
		Collections.sort(styles, NameComparator.INSTANCE);

		Iterator<IRootObject> stylesIterator = styles.iterator();
		while (stylesIterator.hasNext())
		{
			IRootObject style = stylesIterator.next();
			UserNode node = new UserNode(style.getName(), UserNodeType.STYLE_ITEM, style, uiActivator.loadImageFromBundle("style.png"));
			dlm.add(node);
		}
		return dlm.toArray();
	}

	private Object[] createI18NFiles()
	{
		ArrayList<String> activeI18NFileNames = new ArrayList<String>();
		ServoyProject[] modules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		for (ServoyProject module : modules)
		{
			String i18nDatasource = module.getSolution().getI18nDataSource();
			if (i18nDatasource != null)
			{
				String[] i18nServerTable = DataSourceUtilsBase.getDBServernameTablename(i18nDatasource);
				if (i18nServerTable[0] != null && i18nServerTable[1] != null) activeI18NFileNames.add(i18nServerTable[0] + '.' + i18nServerTable[1]);
			}
		}


		List<String> messagesFiles = Arrays.asList(EclipseMessages.getDefaultMessageFileNames());
		Collections.sort(messagesFiles);
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();
		Iterator<String> messagesFilesIte = messagesFiles.iterator();
		while (messagesFilesIte.hasNext())
		{
			String i18nFileItemName = messagesFilesIte.next();
			i18nFileItemName = i18nFileItemName.substring(0, i18nFileItemName.indexOf(EclipseMessages.MESSAGES_EXTENSION));
			boolean isActive = activeI18NFileNames.indexOf(i18nFileItemName) != -1;
			UserNode node = new UserNode(i18nFileItemName, UserNodeType.I18N_FILE_ITEM, null, isActive ? null : "Not referenced");
			node.setEnabled(isActive);
			node.setAppearenceFlags(isActive ? 0 : (SimpleUserNode.TEXT_GRAYED_OUT | SimpleUserNode.TEXT_ITALIC));
			dlm.add(node);
		}

		return dlm.toArray();
	}

	private Object[] createTemplates()
	{
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();
		List<IRootObject> templates = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.TEMPLATES);
		Collections.sort(templates, NameComparator.INSTANCE);

		Iterator<IRootObject> templatesIterator = templates.iterator();
		while (templatesIterator.hasNext())
		{
			IRootObject template = templatesIterator.next();
			UserNode node = new UserNode(template.getName(), UserNodeType.TEMPLATE_ITEM, template, null);
			dlm.add(node);
		}
		return dlm.toArray();
	}

	private Object[] createViews(IServerInternal s)
	{
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();
		if (s.isValid() && s.getConfig().isEnabled())
		{
			try
			{
				List<String> viewNames = s.getViewNames(true);
				if (viewNames != null && viewNames.size() > 0)
				{
					List<String> hiddenViews = null;
					Iterator<String> it = viewNames.iterator();
					while (it.hasNext())
					{
						String name = it.next();
						if (s.isTableMarkedAsHiddenInDeveloper(name))
						{
							if (hiddenViews == null) hiddenViews = new ArrayList<String>();
							hiddenViews.add(name);
						}
						else
						{
							dlm.add(
								new UserNode(name, UserNodeType.VIEW, new TableWrapper(s.getName(), name, true), uiActivator.loadImageFromBundle("view.png")));
						}
					}
					if (hiddenViews != null)
					{
						// tables and views that are marked by user as "hiddenInDeveloper" will only be shown in this sol. ex. list and grayed-out + at the bottom of this list
						for (String name : hiddenViews)
						{
							UserNode node = new UserNode(name, UserNodeType.VIEW, new TableWrapper(s.getName(), name, true),
								uiActivator.loadImageFromBundle("view.png", true));
							node.setAppearenceFlags(SimpleUserNode.TEXT_GRAYED_OUT);
							node.setToolTipText(Messages.SolutionExplorerListContentProvider_hidden);
							dlm.add(node);
						}
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		return dlm.toArray();
	}

	public static SimpleUserNode[] createTables(IServerInternal s, UserNodeType type)
	{
		return createTables(s, type, new ArrayList<SimpleUserNode>());
	}

	public static SimpleUserNode[] createTables(IServerInternal server, UserNodeType type, List<SimpleUserNode> dlm)
	{
		if (server.isValid() && server.getConfig().isEnabled())
		{
			try
			{
				List<String> tableNames = server.getTableNames(true);
				Collections.sort(tableNames);

				List<String> hiddenTables = null;
				List<String> invalidBecauseNoPK = null;
				List<String> metadataTables = null;
				for (String tableName : tableNames)
				{
					final ITable tabel = server.getTable(tableName);
					if (server.isTableMarkedAsHiddenInDeveloper(tableName))
					{
						if (hiddenTables == null) hiddenTables = new ArrayList<String>();
						hiddenTables.add(tableName);
					}
					else if (server.isTableMarkedAsMetaData(tableName))
					{
						if (metadataTables == null) metadataTables = new ArrayList<String>();
						metadataTables.add(tableName);
					}
					else if (isTableInvalidInDeveloperBecauseNoPk(tabel) && !server.isTableMarkedAsHiddenInDeveloper(tableName))
					{
						if (invalidBecauseNoPK == null) invalidBecauseNoPK = new ArrayList<String>();
						invalidBecauseNoPK.add(tableName);
					}
					else
					{
						// The table may not have been initialized yet (i.e. loaded columns).
						// Do not get the table object here because that may cause loading all columns of all tables from
						// developer rendering developer unresponsive for a long time.
						String dataSource = server.getTableDatasource(tableName);
						UserNode node = new UserNode(tableName, type, new DataSourceFeedback(dataSource), DataSourceWrapperFactory.getWrapper(dataSource),
							uiActivator.loadImageFromBundle("table.png"));
						node.setClientSupport(ClientSupport.All);
						dlm.add(node);
					}
				}
				if (metadataTables != null)
				{
					for (String name : metadataTables)
					{
						String dataSource = server.getTableDatasource(name);

						Image image = uiActivator.loadImageFromBundle("metadata_table.png");
						if (image == null)
						{
							//we don't have a metadata icon, for now we add some decorator over the existing table icon
							image = uiActivator.loadImageFromBundle("table.png");
							image = new DecorationOverlayIcon(image,
								com.servoy.eclipse.ui.Activator.loadDefaultImageDescriptorFromBundle("layout_decorator.png"),
								IDecoration.TOP_LEFT).createImage();
							uiActivator.putImageInCache("metadata_table.png", image);
						}
						UserNode node = new UserNode(name, type, new DataSourceFeedback(dataSource, true), DataSourceWrapperFactory.getWrapper(dataSource),
							image);
						dlm.add(node);
					}
				}
				if (hiddenTables != null)
				{
					// tables and views that are marked by user as "hiddenInDeveloper" will only be shown in this sol. ex. list and grayed-out + at the bottom of this list
					for (String name : hiddenTables)
					{
						String dataSource = server.getTable(name).getDataSource();
						UserNode node = new UserNode(name, type, DataSourceWrapperFactory.getWrapper(dataSource),
							uiActivator.loadImageFromBundle("portal.png", true));
						node.setAppearenceFlags(SimpleUserNode.TEXT_GRAYED_OUT);
						node.setToolTipText(Messages.SolutionExplorerListContentProvider_hidden);
						dlm.add(node);
					}
				}
				if (invalidBecauseNoPK != null)
				{
					// tables and views that are marked by user as "hiddenInDeveloperBecauseNoPK" will only be shown in this sol. ex. list and grayed-out + at the bottom of this list
					// the icon will be rendered with an error icon on the bottom left
					for (String name : invalidBecauseNoPK)
					{
						String dataSource = server.getTable(name).getDataSource();
						UserNode node = new UserNode(name, type, DataSourceWrapperFactory.getWrapper(dataSource), loadImageForTableNode());
						//node.setAppearenceFlags(SimpleUserNode.TEXT_GRAYED_OUT);
						node.setToolTipText(Messages.SolutionExplorerListContentProvider_hiddenBecauseNoPK);
						dlm.add(node);
					}
				}

			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("error creating tables nodes for server: " + server, e);
			}
		}
		return dlm.toArray(new SimpleUserNode[dlm.size()]);
	}


	/**
	 * @param tabel
	 * @return
	 */
	private static boolean isTableInvalidInDeveloperBecauseNoPk(final ITable tabel)
	{
		if (tabel != null)
		{
			return tabel.isTableInvalidInDeveloperBecauseNoPk();
		}
		return false;
	}

	private static Image loadImageForTableNode()
	{
		final String TABLE_ERROR_IMAGE = "table.png_IMG_DEC_FIELD_ERROR";

		Image errorIcon = uiActivator.loadImageFromCache(TABLE_ERROR_IMAGE);
		if (errorIcon == null)
		{
			Image IMG_ERROR = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage();
			if (IMG_ERROR != null)
			{
				errorIcon = new DecorationOverlayIcon(uiActivator.loadImageFromBundle("table.png"), ImageDescriptor.createFromImage(IMG_ERROR),
					IDecoration.BOTTOM_LEFT).createImage();
				uiActivator.putImageInCache(TABLE_ERROR_IMAGE, errorIcon);
			}
		}

		return errorIcon;
	}

	public static SimpleUserNode[] createProcedures(IServerInternal s, UserNodeType type)
	{
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();
		try
		{
			List<Procedure> procedures = new ArrayList<>(((IServer)s).getProcedures());
			Collections.sort(procedures, new Comparator<Procedure>()
			{
				@Override
				public int compare(Procedure o1, Procedure o2)
				{
					return NameComparator.INSTANCE.compare(o1.getName(), o2.getName());
				}
			});

			for (Procedure procedure : procedures)
			{
				ProcedureFeedback procedureFeedback = new ProcedureFeedback(procedure, s.getName());
				UserNode node = new UserNode(procedureFeedback.getCall(), type, procedureFeedback, procedure, uiActivator.loadImageFromBundle("function.png"));
				node.setClientSupport(ClientSupport.All);
				dlm.add(node);
			}
		}
		catch (RemoteException | RepositoryException e)
		{
			ServoyLog.logError("error creating tables nodes for server: " + s, e);
		}
		return dlm.toArray(new SimpleUserNode[dlm.size()]);
	}

	public static SimpleUserNode[] createInMemTables(ServoyProject servoyProject, boolean bIncludeModules)
	{
		return createTables(servoyProject, bIncludeModules, UserNodeType.INMEMORY_DATASOURCE);
	}

	public static SimpleUserNode[] createViewFoundsets(ServoyProject servoyProject, boolean bIncludeModules)
	{
		return createTables(servoyProject, bIncludeModules, UserNodeType.VIEW_FOUNDSET);
	}

	private static SimpleUserNode[] createTables(ServoyProject servoyProject, boolean bIncludeModules, UserNodeType nodeType)
	{
		ArrayList<SimpleUserNode> serverNodeChildren = new ArrayList<SimpleUserNode>();
		try
		{
			List<IProject> allReferencedProjects;
			if (bIncludeModules)
			{
				allReferencedProjects = servoyProject.getSolutionAndModuleReferencedProjects();
			}
			else
			{
				allReferencedProjects = new ArrayList<IProject>(1);
				allReferencedProjects.add(servoyProject.getProject());
			}
			for (IProject module : allReferencedProjects)
			{
				if (module.isOpen() && module.hasNature(ServoyProject.NATURE_ID))
				{
					SimpleUserNode[] moduleTables = SolutionExplorerListContentProvider.createTables(
						nodeType == UserNodeType.INMEMORY_DATASOURCE ? ((ServoyProject)module.getNature(ServoyProject.NATURE_ID)).getMemServer()
							: ((ServoyProject)module.getNature(ServoyProject.NATURE_ID)).getViewFoundsetsServer(),
						nodeType);

					for (SimpleUserNode moduleTable : moduleTables)
					{
						if (module != servoyProject.getProject())
							moduleTable.setDisplayName(SolutionExplorerTreeContentProvider.appendModuleName(moduleTable.getName(), module.getName()));
						serverNodeChildren.add(moduleTable);
					}
				}
			}
		}
		catch (CoreException ex)
		{
			ServoyLog.logError(ex);
		}

		return serverNodeChildren.toArray(new SimpleUserNode[serverNodeChildren.size()]);
	}

	private Object[] createTableColumns(ITable table, SimpleUserNode un) throws RepositoryException
	{
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();

		if (table != null)
		{
			if (un.getSolution() == null)
			{
				genTableColumns(table, dlm, UserNodeType.TABLE_COLUMNS_ITEM,
					ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getSolution(), null);
			}
			else genTableColumns(table, dlm, UserNodeType.TABLE_COLUMNS_ITEM, un.getSolution(), null);
		}

		return dlm.toArray();
	}

	private void genTableColumns(ITable table, List<SimpleUserNode> dlm, UserNodeType type, Solution solution, Relation relation) throws RepositoryException
	{
		List<Object> tableUsers = usedTables.get(table);
		if (tableUsers == null)
		{
			tableUsers = new ArrayList<Object>();
			table.addIColumnListener(this);
			usedTables.put(table, tableUsers);
		}
		if (relation != null)
		{
			tableUsers.add(relation.getUUID());
		}
		String prefix = relation == null ? "" : relation.getName() + '.';
		HashMap<String, Solution> modulesOfSolution = new HashMap<String, Solution>();
		if (solution != null)
		{
			solution.getReferencedModulesRecursive(modulesOfSolution);
			modulesOfSolution.put(solution.getName(), solution);
		}

		Iterator<Column> cols = EditorUtil.getTableColumns(table);
		while (cols.hasNext())
		{
			IColumn c = cols.next();
			ColumnInfo ci = c.getColumnInfo();
			if (ci != null && ci.isExcluded())
			{
				continue;
			}
			Object real = relation == null ? c : new ColumnWrapper(c, new Relation[] { relation });
			dlm.add(new UserNode(c.getDataProviderID(), type, new ColumnFeedback(prefix, c), real, uiActivator.loadImageFromBundle("column.png")));
		}
		Iterator<Solution> modules = modulesOfSolution.values().iterator();
		SortedList<UserNode> sl = new SortedList<UserNode>(NameComparator.INSTANCE);
		while (modules.hasNext())
		{
			Solution sol = modules.next();
			Iterator<AggregateVariable> aggs = sol.getAggregateVariables(table, false);
			while (aggs.hasNext())
			{
				AggregateVariable av = aggs.next();
				Object real = relation == null ? av : new ColumnWrapper(av, relation);
				sl.add(new UserNode(av.getDataProviderID(), type, new ColumnFeedback(prefix, av), real, uiActivator.loadImageFromBundle("columnaggr.png")));
			}
		}
		dlm.addAll(sl);
		sl.clear();
		modules = modulesOfSolution.values().iterator();
		while (modules.hasNext())
		{
			Solution sol = modules.next();
			Iterator<ScriptCalculation> calcs = sol.getScriptCalculations(table, false);
			while (calcs.hasNext())
			{
				ScriptCalculation sc = calcs.next();
				Object real = relation == null ? sc : new ColumnWrapper(sc, relation);
				sl.add(new UserNode(sc.getDataProviderID(), UserNodeType.CALCULATIONS_ITEM, new ColumnFeedback(prefix, sc), real,
					uiActivator.loadImageFromBundle("columncalc.png")));
			}
		}
		dlm.addAll(sl);
	}

	private String getScriptMethodSignature(ScriptMethod sm, String methodName, boolean showParam, boolean showParamType, boolean showReturnType,
		boolean showReturnTypeAtEnd)
	{
		MethodArgument[] args = sm.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);

		StringBuilder methodSignatureBuilder = new StringBuilder();
		methodSignatureBuilder.append(methodName != null ? methodName : sm.getName()).append('(');
		for (int i = 0; i < args.length; i++)
		{
			if ((showParam || showParamType) && args[i].isOptional()) methodSignatureBuilder.append('[');
			if (showParam)
			{
				methodSignatureBuilder.append(args[i].getName());
				if (showParamType) methodSignatureBuilder.append(':');
			}
			if (showParamType) methodSignatureBuilder.append(args[i].getType());
			if ((showParam || showParamType) && args[i].isOptional()) methodSignatureBuilder.append(']');
			if (i < args.length - 1) methodSignatureBuilder.append(", ");
		}
		methodSignatureBuilder.append(')');

		if (showReturnType)
		{
			MethodArgument returnTypeArgument = sm.getRuntimeProperty(IScriptProvider.METHOD_RETURN_TYPE);
			String returnType = "void";
			if (returnTypeArgument != null)
			{
				if ("*".equals(returnTypeArgument.getType().getName())) returnType = "Any";
				else returnType = returnTypeArgument.getType().getName();
			}
			if (showReturnTypeAtEnd)
			{
				methodSignatureBuilder.append(" - ").append(returnType);
			}
			else
			{
				methodSignatureBuilder.insert(0, ' ').insert(0, returnType);
			}
		}

		return methodSignatureBuilder.toString();
	}

	private Object[] createFormScripts(Form f)
	{
		Form form = f;
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();
		try
		{
			dlm.add(
				new UserNode(form.getName(), UserNodeType.FORM_FOUNDSET, form.getName(), form.getName(), form, ElementUtil.getImageForFormEncapsulation(form)));
			TreeBuilder.docToOneNode(com.servoy.j2db.documentation.scripting.docs.Form.class, this, UserNodeType.FOUNDSET_ITEM, null, dlm, "foundset", form,
				uiActivator.loadImageFromBundle("foundset.png"));
			FlattenedSolution flatSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
			if (flatSolution != null)
			{
				Form flatForm = flatSolution.getFlattenedForm(f);
				if (flatForm != null)
				{
					form = flatForm;
				}
			}
			Iterator<ScriptMethod> it = form.getScriptMethods(true);
			String nodeText;
			while (it.hasNext())
			{
				ScriptMethod sm = it.next();
				if (sm.getParent() == f)
				{
					nodeText = getScriptMethodSignature(sm, null, false, true, true, true);
				}
				else
				{
					if (sm.isPrivate() || !showInheritedMethods) continue;

					nodeText = getScriptMethodSignature(sm, null, false, true, true, true) + " [" + ((Form)sm.getParent()).getName() + "]";
				}

				Image icon = getImageForMethodEncapsulation(sm);
				dlm.add(new UserNode(nodeText, UserNodeType.FORM_METHOD, new ScriptMethodFeedback(sm), sm, icon));
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return dlm.toArray();
	}

	private Object[] createGlobalVariables(SimpleUserNode un)
	{
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();
		Pair<Solution, String> pair = (Pair<Solution, String>)un.getRealObject();
		List<IPersist> persists = getPersists(pair.getLeft(), UserNodeType.GLOBAL_VARIABLES, pair.getRight());
		for (IPersist persist : persists)
		{
			ScriptVariable var = (ScriptVariable)persist;
			SimpleUserNode node = new UserNode(getDisplayName(var, pair.getLeft()), UserNodeType.GLOBAL_VARIABLE_ITEM, var.getDataProviderID(),
				Column.getDisplayTypeString(var.getDataProviderType()) + " " + var.getDataProviderID(), var, getImageForVariableEncapsulation(var));
			dlm.add(node);
		}
		return dlm.toArray();
	}

	private Object[] createFormVariables(SimpleUserNode un)
	{
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();
		Form form = (un != null) ? (Form)un.getRealObject() : null;
		if (form != null)
		{
			Form originalForm = form;
			FlattenedSolution flatSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
			if (flatSolution != null)
			{
				Form flatForm = flatSolution.getFlattenedForm(form);
				if (flatForm != null)
				{
					form = flatForm;
				}
			}

			Iterator<ScriptVariable> it = form.getScriptVariables(true);
			String nodeText;
			while (it.hasNext())
			{
				ScriptVariable var = it.next();
				if (var.getParent() == originalForm)
				{
					nodeText = var.getName();
				}
				else
				{
					if (var.isPrivate()) continue;

					nodeText = var.getName() + " [" + ((Form)var.getParent()).getName() + "]";
				}
				dlm.add(new UserNode(nodeText, UserNodeType.FORM_VARIABLE_ITEM, var.getDataProviderID(), "%%prefix%%" + var.getDataProviderID(),
					Column.getDisplayTypeString(var.getDataProviderType()) + " " + var.getDataProviderID(), var, getImageForVariableEncapsulation(var)));
			}
		}

		return dlm.toArray();
	}

	private Image getImageForVariableEncapsulation(ScriptVariable sv)
	{
		if (sv.isPrivate()) return uiActivator.loadImageFromBundle("variable_private.png");
		if (sv.isPublic()) uiActivator.loadImageFromBundle("variable_public.png");
		return uiActivator.loadImageFromBundle("variable_default.png");
	}

	private Image getImageForMethodEncapsulation(ScriptMethod sm)
	{
		if (sm.isPrivate()) return uiActivator.loadImageFromBundle("method_private.png");
		if (sm.isProtected()) return uiActivator.loadImageFromBundle("method_protected.png");
		return uiActivator.loadImageFromBundle("method_public.png");
	}

	private Object[] createGlobalScripts(SimpleUserNode un)
	{
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();

		Pair<Solution, String> pair = (Pair<Solution, String>)un.getRealObject();
		List<IPersist> persists = getPersists(pair.getLeft(), UserNodeType.GLOBALS_ITEM, pair.getRight());
		for (IPersist persist : persists)
		{
			ScriptMethod sm = (ScriptMethod)persist;
			String nodeName;
			Solution persistSolution = (Solution)persist.getAncestor(IRepository.SOLUTIONS);
			if (persistSolution == null || persistSolution.equals(pair.getLeft()))
			{
				nodeName = getScriptMethodSignature(sm, ((ISupportName)persist).getName(), false, true, true, true);
			}
			else
			{
				nodeName = getScriptMethodSignature(sm, ((ISupportName)persist).getName(), false, true, true, true) + " [" + persistSolution.getName() + ']';
			}

			SimpleUserNode node = new UserNode(nodeName, UserNodeType.GLOBAL_METHOD_ITEM, new ScriptMethodFeedback(sm), sm, getImageForMethodEncapsulation(sm));
			dlm.add(node);
		}
		return dlm.toArray();
	}

	private Object[] createValueLists(SimpleUserNode un)
	{
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();
		Solution s = (Solution)un.getRealObject();

		List<IPersist> valuelists = getPersists(s, UserNodeType.VALUELISTS, null);
		for (IPersist persist : valuelists)
		{
			ValueList var = (ValueList)persist;
			SimpleUserNode node = new UserNode(getDisplayName(var, s), UserNodeType.VALUELIST_ITEM, null, var.getName(), var,
				uiActivator.loadImageFromBundle(PersistEncapsulation.isModuleScope(var, null) ? "valuelist_protected.png" : "valuelist.png"));
			dlm.add(node);
		}
		return dlm.toArray();
	}

	private SimpleUserNode[] createMedia(SimpleUserNode un)
	{
		if (un != null)
		{
			MediaNode mediaFolder = null;
			if (un.getType() == UserNodeType.MEDIA)
			{
				mediaFolder = new MediaNode(null, null, MediaNode.TYPE.FOLDER, ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
					((Solution)un.getRealObject()).getName()).getEditingSolution());
			}
			else if (un.getType() == UserNodeType.MEDIA_FOLDER)
			{
				mediaFolder = (MediaNode)un.getRealObject();
			}

			if (mediaFolder != null)
			{
				return view.createMediaFolderChildrenNodes(mediaFolder, uiActivator, EnumSet.of(MediaNode.TYPE.IMAGE));
			}
		}

		return new SimpleUserNode[0];
	}

	private Object[] createRelation(Relation r, boolean calcMode)
	{
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();
		try
		{
			String[] exludeMethods = null;
			if (calcMode)
			{
				exludeMethods = new String[] { "clearFoundSet", "clear", "addFoundSetFilterParam", "deleteRecord", "deleteAllRecords", "duplicateRecord", "getCurrentSort", "newRecord", "sort", "unrelate" };
			}
			else
			{
				exludeMethods = new String[] { "clearFoundSet", "clear", "addFoundSetFilterParam" };
			}
			dlm.add(new UserNode(r.getName(), UserNodeType.RELATION, r.getName(), r.getName(), r, uiActivator.loadImageFromBundle("foundset.png")));
			SimpleUserNode[] methods = getJSMethods(RelatedFoundSet.class, r.getName(), null, UserNodeType.RELATION_METHODS, null, exludeMethods);

			genTableColumns(ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(r.getForeignDataSource()), dlm,
				UserNodeType.RELATION_COLUMN, (Solution)r.getRootObject(), r);
			dlm.addAll(Arrays.asList(methods));
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return dlm.toArray();
	}

	private static class DummyScope extends ScriptableObject
	{
		@Override
		public String getClassName()
		{
			return "DummyScope";
		}
	}

	private final class TagResolver implements ITagResolver
	{
		private final String elementName;
		private final String prefix;

		private TagResolver(String elementName, String prefix)
		{
			this.elementName = elementName;
			this.prefix = prefix;
		}

		/**
		 * @see com.servoy.base.util.Text.ITagResolver#getStringValue(java.lang.String)
		 */
		public String getStringValue(String tagname)
		{
			if (tagname.equals("elementName"))
			{
				return elementName;
			}
			else if (tagname.equals("prefix"))
			{
				return prefix;
			}
			return null;
		}
	}

	public boolean isNonEmptyPlugin(SimpleUserNode un)
	{
		Object[] lm = getJSMethods(un.getRealObject(), PLUGIN_PREFIX + "." + un.getName(), null, UserNodeType.PLUGINS_ITEM, null, null);
		if (lm != null && lm.length > 0) return true;
		return false;
	}

	private SimpleUserNode[] getAllMethods(final Class beanClazz, Class specificClazz, String elementName, String prefix, UserNodeType actionType)
	{
		boolean current = (Context.getCurrentContext() != null);
		JavaMembers jm = null;
		try
		{
			if (!current)
			{
				Context.enter();
			}
			jm = new DeclaringClassJavaMembers(null, specificClazz, specificClazz);
		}
		finally
		{
			if (!current)
			{
				Context.exit();
			}
		}
		return getJSMethodsViaJavaMembers(jm, beanClazz, null, elementName, prefix, actionType, null, null);
	}

	/**
	 * Extract the docs for angular client side apis.
	 * @param readTextFile
	 */
	public static void extractApiDocs(WebObjectSpecification spec)
	{
		if (spec.getApiFunctions().size() > 0)
		{
			extractDocsFromJsFile(spec, spec.getDefinitionURL());
			extractDocsFromJsFile(spec, spec.getServerScript());
		}
	}

	private static void extractDocsFromJsFile(WebObjectSpecification spec, URL url)
	{
		if (spec != null && url != null)
		{
			final Map<String, WebObjectFunctionDefinition> apis = spec.getApiFunctions();
			try
			{
				URLConnection openConnection = url.openConnection();
				openConnection.setUseCaches(false);
				InputStream is = openConnection.getInputStream();
				String source = IOUtils.toString(is);
				is.close();
				if (source != null)
				{
					JavaScriptParser parser = new JavaScriptParser();
					Script script = parser.parse(source, null);
					script.visitAll(new AbstractNavigationVisitor<ASTNode>()
					{
						@Override
						public ASTNode visitBinaryOperation(BinaryOperation node)
						{
							if (node.getOperationText().trim().equals("=") && node.getLeftExpression() instanceof PropertyExpression)
							{
								String expr = ((PropertyExpression)node.getLeftExpression()).toString();
								if (expr.startsWith("$scope.api") || expr.startsWith("scope.api"))
								{
									WebObjectFunctionDefinition api = apis.get(((PropertyExpression)node.getLeftExpression()).getProperty().toString());
									Comment doc = node.getDocumentation();
									if (api != null && doc != null && doc.isDocumentation())
									{
										api.setDocumentation(doc.getText());
									}
								}
							}
							return super.visitBinaryOperation(node);
						}


						/*
						 * (non-Javadoc)
						 *
						 * @see org.eclipse.dltk.javascript.ast.AbstractNavigationVisitor#visitObjectInitializer(org.eclipse.dltk.javascript.ast.
						 * ObjectInitializer)
						 */
						@Override
						public ASTNode visitObjectInitializer(ObjectInitializer node)
						{
							CallExpression call = null;
							try
							{
								ReturnStatement ret = node.getParent() != null ? node.getAncestor(ReturnStatement.class) : null;
								if (ret != null)
								{
									call = ret.getAncestor(CallExpression.class);
								}
							}
							catch (Exception e)
							{
								//ignore, why is this getAncestor throwing error if ancestor doesn't exist
							}

							if (call != null && call.getExpression().toString().endsWith(".factory"))
							{
								PropertyInitializer[] initializers = node.getPropertyInitializers();
								for (PropertyInitializer initializer : initializers)
								{
									WebObjectFunctionDefinition api = apis.get(initializer.getNameAsString());
									Comment doc = initializer.getName().getDocumentation();
									if (api != null && initializer.getValue() instanceof FunctionStatement && doc != null && doc.isDocumentation())
									{
										api.setDocumentation(doc.getText());
									}
								}
							}
							return super.visitObjectInitializer(node);
						}

					});
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	public static String getParsedComment(String comment, String elementName, boolean toHTML)
	{
		if (comment == null) return null;
		String c = comment.replaceAll("/\\*\\*|\\*/", "");
		c = c.replaceAll("\\n(\\s*)\\*", "\n").trim();
		if (!toHTML)
		{
			String separator = System.getProperty("line.separator");
			String[] inputArray = c.split(separator);
			StringBuilder stringBuilder = new StringBuilder();
			for (String value : inputArray)
			{
				stringBuilder.append(value.trim());
				stringBuilder.append(separator);
			}
			c = stringBuilder.toString();
			c = c.replaceAll(separator, "<br/>");
		}
		if (elementName != null) c = c.replaceAll("%%elementName%%", elementName);
		if (!toHTML) return c;

		JavaDoc2HTMLTextReader reader = new JavaDoc2HTMLTextReader(new StringReader(c));
		try
		{
			return reader.getString().replaceAll(System.getProperty("line.separator"), "<br/>");
		}
		catch (IOException e)
		{
			return comment;
		}
	}

	private static String getParsedSample(final String name, final WebObjectFunctionDefinition api)
	{
		if (api.getDocumentation() != null && api.getDocumentation().contains("@example"))
		{
			String description = getParsedComment(api.getDocumentation(), name, false);
			String example = description.split("@example")[1].split("@")[0];
			example = example.replaceAll("<br>|<br/>", "\n");
			example = example.replaceAll("\\<.*?\\>", "");
			return example;
		}
		return null;
	}

	private SimpleUserNode[] getJSMethods(Object o, final String elementName, String prefix, UserNodeType actionType, Object real, String[] excludeMethodNames)
	{
		if (o == null) return EMPTY_LIST;
		if (o instanceof WebObjectSpecification)
		{
			WebObjectSpecification spec = ((WebObjectSpecification)o);
			extractApiDocs(spec);
			Map<String, PropertyDescription> properties = spec.getProperties();
			List<String> ids = new ArrayList<String>();
			for (PropertyDescription pd : properties.values())
			{
				if (pd.isDeprecated() || WebFormComponent.isDesignOnlyProperty(pd) || WebFormComponent.isPrivateProperty(pd))
				{
					// skip deprecated, design and private properties
					continue;
				}
				ids.add(pd.getName());
			}
			ids.addAll(spec.getApiFunctions().keySet());
			if (ids != null)
			{
				List<SimpleUserNode> serviceIds = new ArrayList<SimpleUserNode>();
				for (Object element : ids)
				{
					Image icon = propertiesIcon;
					String pluginsPrefix = PLUGIN_PREFIX + "." + ((WebObjectSpecification)o).getScriptingName() + ".";
					IDeveloperFeedback feedback = new FieldFeedback((String)element, pluginsPrefix, null, null, null);
					if (spec.getApiFunction((String)element) != null)
					{
						final WebObjectFunctionDefinition api = spec.getApiFunction((String)element);
						if (api.isDeprecated()) continue;
						icon = functionIcon;
						final List<String> parNames = new ArrayList<String>();
						List<String> parTypes = new ArrayList<String>();
						for (PropertyDescription pd : api.getParameters())
						{
							parNames.add(pd.getName());
							parTypes.add(pd.getType().getName());
						}
						feedback = new MethodFeedback((String)element, parTypes.toArray(new String[0]), pluginsPrefix, null, new IScriptObject()
						{

							@Override
							public Class< ? >[] getAllReturnedTypes()
							{
								return null;
							}

							@Override
							public String getSample(String methodName)
							{
								return getParsedSample(elementName, api);
							}

							@Override
							public String getToolTip(String methodName)
							{
								return getParsedComment(api.getDocumentation(), elementName, false);
							}

							@Override
							public String[] getParameterNames(String methodName)
							{
								return parNames.toArray(new String[0]);
							}

							@Override
							public boolean isDeprecated(String methodName)
							{
								return api.getDocumentation() != null && api.getDocumentation().contains("@deprecated");
							}

						}, null, api.getReturnType() != null ? api.getReturnType().getType().getName() : "void");
					}
					UserNode node = new UserNode((String)element, actionType, feedback, real, icon);
					node.setClientSupport(ClientSupport.ng);
					serviceIds.add(node);
				}
				return serviceIds.toArray(new SimpleUserNode[serviceIds.size()]);
			}

		}
		boolean current = (Context.getCurrentContext() != null);
		InstanceJavaMembers ijm = null;
		try
		{
			if (!current)
			{
				Context.enter();
			}
			ijm = new InstanceJavaMembers(new DummyScope(), o.getClass());
		}
		finally
		{
			if (!current)
			{
				Context.exit();
			}
		}
		IScriptObject so = null;
		if (o instanceof IScriptObject)
		{
			so = (IScriptObject)o;
		}
		else if (o instanceof IScriptable)
		{
			so = ScriptObjectRegistry.getScriptObjectForClass(o.getClass());
		}
		return getJSMethodsViaJavaMembers(ijm, o.getClass(), so, elementName, prefix, actionType, real, excludeMethodNames);
	}

	private SimpleUserNode[] getJSMethods(Class clz, String elementName, String prefix, UserNodeType actionType, Object real, String[] excludeMethodNames)
	{
		if (clz == null)
		{
			return null;
		}
		IScriptObject o = ScriptObjectRegistry.getScriptObjectForClass(clz);
		if (o == null && IScriptObject.class.isAssignableFrom(clz))
		{
			try
			{
				// just try to make it.
				o = (IScriptObject)clz.newInstance();
				ScriptObjectRegistry.registerScriptObjectForClass(clz, o);
			}
			catch (Exception e)
			{
				ServoyLog.logWarning("Class " + clz +
					" did implement IScriptObject but doesnt have a default constructor, it should have that or use ScriptObjectRegistry.registerScriptObjectForClass()",
					e);
			}
		}
		JavaMembers ijm = ScriptObjectRegistry.getJavaMembers(clz, null);
		if (real == null)
		{
			if (IPrefixedConstantsObject.class.isAssignableFrom(clz))
			{
				try
				{
					// just try to make it.
					real = clz.newInstance();
				}
				catch (Exception e)
				{
					ServoyLog.logWarning("Constants object couldnt be created: " + clz, e);
				}
			}
			else if (o instanceof IConstantsObject)
			{
				real = o;
			}
			else
			{
				real = clz;
			}
		}
		if (Scriptable.class.isAssignableFrom(clz) && !(ijm instanceof InstanceJavaMembers))
		{
			// if the class is a scriptable an the javamembers is not a instance java members, just return nothing.
			return new SimpleUserNode[0];
		}
		return getJSMethodsViaJavaMembers(ijm, clz, o, elementName, prefix, actionType, real, excludeMethodNames);
	}

	private SimpleUserNode[] getJSMethodsViaJavaMembers(JavaMembers ijm, Class< ? > originalClass, IScriptObject scriptObject, String elementName,
		String prefix, UserNodeType actionType, Object real, String[] excludeMethodNames)
	{
		List<SimpleUserNode> dlm = new ArrayList<SimpleUserNode>();
		IScriptObject adapter = ScriptObjectRegistry.getAdapterIfAny(scriptObject);
		if (real instanceof IConstantsObject || (real instanceof Class< ? > && IConstantsObject.class.isAssignableFrom((Class< ? >)real)))
		{
			String constantsElementName = null;
			if (real instanceof IPrefixedConstantsObject)
			{
				constantsElementName = ((IPrefixedConstantsObject)real).getPrefix() + ".";
			}
			else if (real instanceof IConstantsObject)
			{
				constantsElementName = real.getClass().getSimpleName() + ".";
			}
			else
			{
				constantsElementName = ((Class< ? >)real).getSimpleName() + ".";
			}
			ITagResolver resolver = new TagResolver(constantsElementName, (prefix == null ? "" : prefix));
			if (!constantsElementName.endsWith(".")) constantsElementName = constantsElementName + ".";
			if (elementName.startsWith(PLUGIN_PREFIX))
			{
				constantsElementName = elementName + constantsElementName;
			}

			List<String> fields = ijm.getFieldIds(true);
			Object[] arrays = fields.toArray(new Object[fields.size()]);
			Arrays.sort(arrays);

			UserNode node;
			for (Object element : arrays)
			{
				if (adapter != null)
				{
					if (adapter.isDeprecated((String)element)) continue;
					if (adapter.isDeprecated(constantsElementName + (String)element)) continue;

					node = new UserNode((String)element, actionType, new FieldFeedback((String)element, constantsElementName, resolver, scriptObject, ijm),
						real, uiActivator.loadImageFromBundle("constant.png"));

					// this field is a constant
					Field field = (Field)ijm.getField((String)element, true);
					node.setClientSupport(AnnotationManagerReflection.getInstance().getClientSupport(field, ClientSupport.Default));
				}
				else
				{
					node = new UserNode((String)element, actionType, constantsElementName + element, null, null, real,
						uiActivator.loadImageFromBundle("constant.png"));
				}
				dlm.add(node);
			}

		}

		ITagResolver resolver = new TagResolver(elementName, (prefix == null ? "" : prefix));
		if (!elementName.endsWith(".")) elementName = elementName + ".";


		List fields = ijm.getFieldIds(false);

		if (excludeMethodNames != null) fields.removeAll(Arrays.asList(excludeMethodNames));

		Object[] arrays = new Object[fields.size()];
		arrays = fields.toArray(arrays);
		Arrays.sort(arrays);

		for (Object element : arrays)
		{
			String name = (String)element;

			Image pIcon = propertiesIcon;
			if (adapter != null)
			{
				if (adapter.isDeprecated(name)) continue;
				if (adapter.isDeprecated(elementName + name)) continue;
				if (adapter instanceof ITypedScriptObject)
				{
					if (((ITypedScriptObject)adapter).isSpecial(name))
					{
						pIcon = specialPropertiesIcon;
					}
				}
			}
			Object bp = ijm.getField(name, false);
			if (bp == null) continue;
			String codePrefix = "";
			if (actionType != UserNodeType.RETURNTYPE_ELEMENT)
			{
				codePrefix = elementName;
			}

			UserNode node = new UserNode(name, actionType, new FieldFeedback(name, codePrefix, resolver, scriptObject, ijm), real, pIcon);
			if (bp instanceof JavaMembers.BeanProperty)
			{
				node.setClientSupport(AnnotationManagerReflection.getInstance().getClientSupport(((JavaMembers.BeanProperty)bp).getGetter(), originalClass,
					ClientSupport.Default));
			}
			dlm.add(node);
		}

		List names = ijm.getMethodIds(false);

		if (ijm instanceof InstanceJavaMembers)
		{
			names.removeAll(((InstanceJavaMembers)ijm).getGettersAndSettersToHide());
		}

		arrays = new Object[names.size()];
		arrays = names.toArray(arrays);
		Arrays.sort(arrays);

		for (Object element : arrays)
		{
			String id = (String)element;

			if (!(ijm instanceof InstanceJavaMembers))
			{
				// check if method from Object itself..
				if (ignoreMethods.contains(id)) continue;

				// don't list the methods from IPrefixedConstantsObject
				if ((real instanceof IPrefixedConstantsObject) && ignoreMethodsFromPrefixedConstants.contains(id))
				{
					continue;
				}
			}

			NativeJavaMethod njm = ijm.getMethod(id, false);
			if (njm == null) continue;

			for (MemberBox method : njm.getMethods())
			{
				String displayName = null;

				Class[] parameterTypes = method.getParameterTypes();
				Class returnType = method.getReturnType();
				JSSignature annotation = method.method().getAnnotation(JSSignature.class);
				if (annotation != null)
				{
					if (annotation.arguments().length > 0) parameterTypes = annotation.arguments();
					if (annotation.returns() != Object.class) returnType = annotation.returns();
				}

				if (adapter != null)
				{
					if (adapter instanceof ITypedScriptObject)
					{
						if (((ITypedScriptObject)adapter).isDeprecated(id, parameterTypes)) continue;
						displayName = ((ITypedScriptObject)adapter).getJSTranslatedSignature(id, parameterTypes);
					}
					else
					{
						if (adapter.isDeprecated(id)) continue;
						if (adapter.isDeprecated(elementName + id)) continue;
					}
				}

				if (displayName == null)
				{
					String paramTypes = "";
					for (Class param : parameterTypes)
					{
						paramTypes += DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(param) + ", ";
					}
					paramTypes = "(" + (parameterTypes.length > 0 ? paramTypes.substring(0, paramTypes.length() - 2) : "") + ")";
					displayName = id + paramTypes + " - " + DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(returnType);
				}
				String codePrefix = "";
				if (actionType != UserNodeType.RETURNTYPE_ELEMENT)
				{
					codePrefix = elementName;
				}

				SimpleUserNode node = new UserNode(displayName, actionType,
					new MethodFeedback(id, parameterTypes, codePrefix, resolver, scriptObject, njm, null), (Object)null, functionIcon);

				node.setClientSupport(AnnotationManagerReflection.getInstance().getClientSupport(method.method(), originalClass, ClientSupport.Default));

				dlm.add(node);
			}
		}
		SimpleUserNode[] nodes = new SimpleUserNode[dlm.size()];
		return dlm.toArray(nodes);
	}

	private SimpleUserNode[] getWebComponentMembers(String prefix, final IBasicWebComponent webcomponent)
	{
		String prefixForWebComponentMembers = prefix + ".";
		if (webcomponent == null)
		{
			return null;
		}
		List<SimpleUserNode> nodes = new ArrayList<SimpleUserNode>();
		SortedList<SimpleUserNode> sortedApis = new SortedList<SimpleUserNode>(NameComparator.INSTANCE);

		String webComponentClassName = FormTemplateGenerator.getComponentTypeName(webcomponent);

		WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(webComponentClassName);
		if (spec != null)
		{
			extractApiDocs(spec);

			Map<String, PropertyDescription> properties = spec.getProperties();
			SortedList<PropertyDescription> sortedProperties = new SortedList<PropertyDescription>(new Comparator<PropertyDescription>()
			{

				@Override
				public int compare(PropertyDescription o1, PropertyDescription o2)
				{
					return o1.getName().toString().compareToIgnoreCase(o2.getName().toString());
				}
			}, properties.values());
			for (PropertyDescription pd : sortedProperties)
			{
				if (WebFormComponent.isDesignOnlyProperty(pd) || WebFormComponent.isPrivateProperty(pd)) continue;

				String name = pd.getName();
				// skip the default once added by servoy, see WebComponentPackage.getWebComponentDescriptions()
				// and skip the dataprovider properties (those are not accesable through scripting)
				if (!name.equals("location") && !name.equals("size") && !name.equals("anchors") && !(pd.getType() instanceof DataproviderPropertyType))
				{
					nodes.add(new UserNode(name, UserNodeType.FORM_ELEMENTS, prefixForWebComponentMembers + name, name, webcomponent, propertiesIcon));
				}
			}
			Map<String, WebObjectFunctionDefinition> apis = spec.getApiFunctions();
			for (final WebObjectFunctionDefinition api : apis.values())
			{
				String name = api.getName();
				String displayParams = "(";
				List<PropertyDescription> parameters = api.getParameters();
				final List<String> parNames = new ArrayList<String>();
				List<String> parTypes = new ArrayList<String>();

				for (int i = 0; i < parameters.size(); i++)
				{
					displayParams += parameters.get(i).getName();
					parNames.add(parameters.get(i).getName());
					parTypes.add(parameters.get(i).getType().getName());
					if (i < parameters.size() - 1) displayParams += ", ";
				}
				displayParams += ")";

				MethodFeedback feedback = new MethodFeedback(name, parTypes.toArray(new String[0]), prefixForWebComponentMembers, null, new IScriptObject()
				{

					@Override
					public Class< ? >[] getAllReturnedTypes()
					{
						return null;
					}

					@Override
					public String getSample(String methodName)
					{
						return getParsedSample(webcomponent.getName(), api);
					}

					@Override
					public String getToolTip(String methodName)
					{
						return getParsedComment(api.getDocumentation(), webcomponent.getName(), false);
					}

					@Override
					public String[] getParameterNames(String methodName)
					{
						return parNames.toArray(new String[0]);
					}

					@Override
					public boolean isDeprecated(String methodName)
					{
						return api.getDocumentation() != null && api.getDocumentation().contains("@deprecated");
					}

				}, null, api.getReturnType() != null ? api.getReturnType().getType().getName() : "void");

				sortedApis.add(new UserNode(name + displayParams, UserNodeType.FORM_ELEMENTS, feedback, webcomponent, functionIcon));
			}
			if (spec.getProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName()) != null ||
				spec.getTaggedProperties("mainStyleClass", StyleClassPropertyType.INSTANCE).size() > 0)
			{
				sortedApis.addAll(getJSMethodsFromClass(prefix, webcomponent, HasRuntimeStyleClass.class));
			}
		}
		sortedApis.addAll(getJSMethodsFromClass(prefix, webcomponent, HasRuntimeFormName.class));
		sortedApis.addAll(getJSMethodsFromClass(prefix, webcomponent, HasRuntimeName.class));
		sortedApis.addAll(getJSMethodsFromClass(prefix, webcomponent, HasRuntimeElementType.class));
		sortedApis.addAll(getJSMethodsFromClass(prefix, webcomponent, HasRuntimeDesignTimeProperty.class));
		sortedApis.addAll(getJSMethodsFromClass(prefix, webcomponent, HasRuntimeClientProperty.class));
		nodes.addAll(sortedApis);
		return nodes.toArray(new SimpleUserNode[nodes.size()]);

	}

	private List<SimpleUserNode> getJSMethodsFromClass(String prefix, final IBasicWebComponent webcomponent, Class< ? > clazz)
	{
		return Arrays.asList(getJSMethodsViaJavaMembers(new InstanceJavaMembers(new DummyScope(), clazz), clazz,
			ScriptObjectRegistry.getScriptObjectForClass(clazz), webcomponent.getName(), prefix, UserNodeType.FORM_ELEMENTS, webcomponent, null));
	}

	private SimpleUserNode[] getScriptableMethods(Scriptable scriptable, String elementName, String prefix)
	{
		if (scriptable == null)
		{
			return null;
		}

		Object[] ids = scriptable.getIds();
		Arrays.sort(ids);
		SimpleUserNode[] nodes = new SimpleUserNode[ids.length];

		for (int i = 0; i < ids.length; i++)
		{
			nodes[i] = new UserNode(ids[i].toString(), UserNodeType.FORM_ELEMENTS, prefix + '.' + ids[i].toString(), prefix + '.' + ids[i].toString(), null,
				propertiesIcon);
		}
		return nodes;
	}

	public void clearCache()
	{
		leafList.clear();
	}

	public void clearMediaCache()
	{
		leafList.remove(UserNodeType.MEDIA);
	}

	public void refreshContent()
	{
		view.refreshList();
	}

	public void refreshServer(String serverName)
	{
		String key = UserNodeType.SERVER.toString() + serverName;
		Object previousValue = leafList.remove(key);

		if (previousValue != null)
		{
			view.refreshList();
		} // else the data for this server was not loaded - no use refreshing the list
	}

	public void setIncludeModules(boolean includeModules)
	{
		if (this.includeModules != includeModules)
		{
			this.includeModules = includeModules;
			leafList.clear();
		}
	}

	public void setShowInheritedMethods(boolean showInheritedMethods)
	{
		if (this.showInheritedMethods != showInheritedMethods)
		{
			this.showInheritedMethods = showInheritedMethods;
			leafList.clear();
		}
	}

	/**
	 * @see com.com.servoy.j2db.persistence.IPersistChangeListener#persistChanges(java.util.Collection)
	 */
	public void persistChanges(Collection<IPersist> changes)
	{
		Set<IPersist> processed = new HashSet<IPersist>();
		for (IPersist persist : changes)
		{
			while (persist != null && processed.add(persist))
			{
				leafList.remove(persist.getUUID());
				if (persist instanceof TableNode)
				{
					String dataSource = ((TableNode)persist).getDataSource();
					flushTable(ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(dataSource));
				}
				if (persist instanceof Form)
				{
					Form form = (Form)persist;
					if (ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution() != null)
					{
						List<Form> formHierarchy = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getDirectlyInheritingForms(
							form);
						for (Form f : formHierarchy)
						{
							if (f != form)
							{
								leafList.remove(f.getUUID());
							}
						}
					}
				}
				if (persist instanceof Media)
				{
					clearMediaCache();
				}
				persist = persist.getParent();
			}
		}
	}

	/**
	 * @param column
	 */
	private void flushTable(ITable table)
	{
		leafList.remove(table);
		List<Object> list = usedTables.get(table);
		if (list != null)
		{
			for (Object object : list)
			{
				leafList.remove(object);
			}
		}
	}

	public void itemChanged(IColumn column)
	{
		itemChanged(Collections.singletonList(column));
	}

	public void itemChanged(Collection<IColumn> columns)
	{
		if (columns != null && columns.size() > 0)
		{
			try
			{
				flushTable(columns.iterator().next().getTable());
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	public void itemCreated(IColumn column)
	{
		try
		{
			flushTable(column.getTable());
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
	}

	public void itemRemoved(IColumn column)
	{
		try
		{
			flushTable(column.getTable());
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
	}

	private static class DataSourceFeedback implements IDeveloperFeedback
	{
		private final String dataSource;
		private boolean isMetadata = false;

		/**
		 * @param name
		 * @param tableName
		 */
		public DataSourceFeedback(String dataSource)
		{
			this.dataSource = dataSource;
		}

		public DataSourceFeedback(String datasource, boolean isMetadata)
		{
			this(datasource);
			this.isMetadata = isMetadata;
		}

		public String getSample()
		{
			return "/** @type {JSFoundSet<" + getCode() + ">} */";
		}

		public String getCode()
		{
			return dataSource;
		}

		public String getToolTipText()
		{
			return "<pre>" + (isMetadata ? "Metadata table " : "Table") + " with datasource: '<b>" + getCode() + "\'</b><pre";
		}

	}
	private static class ProcedureFeedback implements IDeveloperFeedback
	{
		private final Procedure procedure;
		private final String serverName;

		public ProcedureFeedback(Procedure procedure, String serverName)
		{
			this.procedure = procedure;
			this.serverName = serverName;
		}

		public String getCall()
		{
			StringBuilder sb = new StringBuilder(procedure.getName());
			sb.append('(');
			List<ProcedureColumn> parameters = procedure.getParameters();
			for (int i = 0; i < parameters.size(); i++)
			{
				if (i > 0)
				{
					sb.append(", ");
				}
				sb.append(parameters.get(i).getName());
			}
			sb.append(')');
			return sb.toString();
		}

		public String getSample()
		{
			return getCall();
		}

		public String getCode()
		{
			return "datasources.sp." + serverName + '.' + getCall() + ';';
		}

		public String getToolTipText()
		{
			return "<pre>Procedure with signature: '<b>" + getCode() + "\'</b><pre";
		}
	}


	private static class MethodFeedback implements IDeveloperFeedback
	{
		private final ITagResolver resolver;
		private final IScriptObject scriptObject;
		private final String name;
		private final Object[] parameterTypes;
		private final NativeJavaMethod njm;
		private final String prefix;
		private final String returnTypeString;

		MethodFeedback(String name, Object[] parameterTypes, String prefix, ITagResolver resolver, IScriptObject scriptObject, NativeJavaMethod njm,
			String returnTypeString)
		{
			this.name = name;
			this.parameterTypes = parameterTypes;
			this.prefix = prefix;
			this.resolver = resolver;
			this.scriptObject = ScriptObjectRegistry.getAdapterIfAny(scriptObject);
			this.njm = njm;
			this.returnTypeString = returnTypeString;
		}

		/**
		 * @see com.servoy.eclipse.ui.node.IDeveloperFeedback#getSample()
		 */
		public String getSample()
		{
			String sample = null;
			if (scriptObject != null)
			{
				if (parameterTypes instanceof Class[] && scriptObject instanceof XMLScriptObjectAdapter)
				{
					sample = ((XMLScriptObjectAdapter)scriptObject).getSample(name, (Class[])parameterTypes,
						ServoyModelManager.getServoyModelManager().getServoyModel().getActiveSolutionClientType());
					if (sample == null) sample = ((XMLScriptObjectAdapter)scriptObject).getSample(name, (Class[])parameterTypes);
				}
				else
				{
					if (scriptObject instanceof XMLScriptObjectAdapter)
					{
						sample = ((XMLScriptObjectAdapter)scriptObject).getSample(name,
							ServoyModelManager.getServoyModelManager().getServoyModel().getActiveSolutionClientType());
					}
					if (sample == null || "".equals(sample))
					{
						sample = scriptObject.getSample(name);
					}
				}
				sample = Text.processTags(sample, resolver);
			}
			return HtmlUtils.escapeMarkup(sample != null ? sample : "").toString();
		}

		/**
		 * @see com.servoy.eclipse.ui.node.IDeveloperFeedback#getCode()
		 */
		public String getCode()
		{
			String[] paramNames = null;
			if (scriptObject != null)
			{
				if (scriptObject instanceof XMLScriptObjectAdapter && parameterTypes instanceof Class[])
				{
					IParameter[] parameters = ((XMLScriptObjectAdapter)scriptObject).getParameters(name, (Class[])parameterTypes);
					if (parameters != null)
					{
						paramNames = new String[parameters.length];
						for (int i = 0; i < parameters.length; i++)
						{
							paramNames[i] = parameters[i].getName();
						}
					}
				}
				else
				{
					paramNames = scriptObject.getParameterNames(name);
				}
			}
			return prefix + name + "(" + getPrettyParameterTypesString(paramNames, true) + ")";
		}

		/**
		 * @see com.servoy.eclipse.ui.node.IDeveloperFeedback#getToolTipText()
		 */
		public String getToolTipText()
		{
			String tooltip = null;
			Class< ? > returnType = null;
			String returnDescription = null;
			String[] paramNames = null;
			boolean namesOnly = false;
			if (scriptObject != null)
			{
				String description = "";
				if (scriptObject instanceof XMLScriptObjectAdapter && parameterTypes instanceof Class[])
				{
					ClientSupport csp = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveSolutionClientType();
					description = ((XMLScriptObjectAdapter)scriptObject).getToolTip(name, (Class[])parameterTypes, csp);
					returnType = ((XMLScriptObjectAdapter)scriptObject).getReturnedType(name, (Class[])parameterTypes);
					returnDescription = ((XMLScriptObjectAdapter)scriptObject).getReturnDescription(name, (Class[])parameterTypes);
					IParameter[] parameters = ((XMLScriptObjectAdapter)scriptObject).getParameters(name, (Class[])parameterTypes);
					tooltip = ((XMLScriptObjectAdapter)scriptObject).getToolTip(name, (Class[])parameterTypes, csp);
					String sample = ((XMLScriptObjectAdapter)scriptObject).getSample(name, (Class[])parameterTypes);
					if (sample != null)
					{
						tooltip += "\n<i>" + Text.processTags(HtmlUtils.escapeMarkup(sample).toString(), resolver).toString() + "</i>";
					}
					if (parameters != null)
					{
						paramNames = new String[parameters.length];
						tooltip += "\n";
						for (int i = 0; i < parameters.length; i++)
						{
							paramNames[i] = parameters[i].getName();
							tooltip = tooltip + "\n <b>@param</b> {" + parameters[i].getType() + "} " + parameters[i].getName() + " " +
								parameters[i].getDescription();
						}
					}
					if (returnType != null)
					{
						tooltip = tooltip + "\n <b>@return</b> {" + getReturnTypeString(returnType) + "} ";
						if (returnDescription != null) tooltip += returnDescription;
					}
				}
				else
				{
					namesOnly = true;
					description = scriptObject.getToolTip(name);
					paramNames = scriptObject.getParameterNames(name);
					returnType = getMethodReturnType();
					tooltip = Text.processTags(description, resolver);
					tooltip = tooltip != null ? tooltip.replaceAll("@param|@return|@example", "<b>$0</b>") : "";
				}
			}
			else
			{
				returnType = getMethodReturnType();
			}
			if (tooltip == null) tooltip = "";

			StringBuilder tmp = new StringBuilder("<b>" + (returnTypeString != null ? returnTypeString : getReturnTypeString(returnType)) + " " + name + "(" +
				getPrettyParameterTypesString(paramNames, namesOnly) + ")</b>");
			if ("".equals(tooltip))
			{
				tooltip = tmp.toString();
			}
			else
			{
				tooltip = tmp.toString() + "<pre>" + tooltip + "</pre>";
			}
			return tooltip;
		}

		private Class getMethodReturnType()
		{
			if (njm != null)
			{
				MemberBox method = njm.getMethods()[0];
				for (MemberBox mthd : njm.getMethods())
				{
					if (Utils.equalObjects(mthd.getParameterTypes(), parameterTypes))
					{
						method = mthd;
					}
				}
				return method.getReturnType();
			}
			return null;
		}

		private String getPrettyParameterTypesString(String[] names, boolean namesOnly)
		{
			if (parameterTypes == null || parameterTypes.length == 0) return "";

			StringBuilder paramTypes = new StringBuilder(32);
			if (names == null || names.length != parameterTypes.length)
			{
				if (parameterTypes instanceof Class[])
				{
					//Object[] varargs backward compatibility
					if (parameterTypes.length == 1 && ((Class[])parameterTypes)[0].isArray() &&
						(((Class[])parameterTypes)[0].getComponentType() == Object.class))
					{
						if (names == null || names.length == 0) paramTypes.append("Object[]");
						else
						{
							for (int k = 0; k < names.length; k++)
							{
								paramTypes.append(names[k]);
								if (k < names.length - 1) paramTypes.append(", ");
							}
						}
						return paramTypes.toString();
					}
					else for (Class param : (Class[])parameterTypes)
					{
						paramTypes.append(DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(param));
						paramTypes.append(", ");
					}
				}
			}
			else if (names != null)
			{
				for (int i = 0; i < parameterTypes.length; i++)
				{
					paramTypes.append(names[i]);
					if (!namesOnly)
					{
						paramTypes.append(':');
						if (parameterTypes instanceof Class[])
						{
							if (i == parameterTypes.length - 1 && ((Class[])parameterTypes)[i].isArray() && scriptObject instanceof XMLScriptObjectAdapter)
							{
								IParameter[] parameters = ((XMLScriptObjectAdapter)scriptObject).getParameters(this.name, (Class[])parameterTypes);
								if (parameters != null && parameters[i].isVarArgs())
								{
									paramTypes.append(DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(
										((Class[])parameterTypes)[i].getComponentType()));
									paramTypes.append("...");
								}
								else
								{
									paramTypes.append(
										DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(((Class[])parameterTypes)[i]));
								}
							}
							else
							{
								paramTypes.append(DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(((Class[])parameterTypes)[i]));
							}
						}
						else
						{
							paramTypes.append(parameterTypes[i].toString());
						}
					}
					paramTypes.append(", ");
				}
			}
			return paramTypes.substring(0, paramTypes.length() - 2);
		}
	}

	private static class ScriptMethodFeedback extends MethodFeedback
	{
		private enum PARAM
		{
			NAME, TYPE
		}

		ScriptMethodFeedback(final ScriptMethod sm)
		{
			super(sm.getName(), getParameters(sm, PARAM.TYPE), getPrefix(sm), null, new IScriptObject()
			{
				@Override
				public Class< ? >[] getAllReturnedTypes()
				{
					return null;
				}

				@Override
				public String getSample(String methodName)
				{
					String comment = getParsedComment(sm.getRuntimeProperty(IScriptProvider.COMMENT), null, false);
					if (comment != null)
					{
						String[] commentSplitByExample = comment.split("@example");
						if (commentSplitByExample.length > 1)
						{
							String example = commentSplitByExample[1].trim();
							if (example.startsWith("<pre>"))
							{
								int preEndIdx = example.indexOf("</pre>");
								example = example.substring("<pre>".length(), preEndIdx != -1 ? preEndIdx : example.length());
							}
							else
							{
								example = example.split("@")[0];
							}
							example = example.replaceAll("&#47;", "/");
							example = example.replaceAll("<br>|<br/>", "\n");
							example = example.replaceAll("\\<.*?\\>", "");
							return example;
						}
					}
					return null;
				}

				private String findDocInHierarchy(Form methodForm, String methodName)
				{
					Iterator<ScriptMethod> methods = methodForm.getObjects(IRepositoryConstants.METHODS);

					while (methods.hasNext())
					{
						ScriptMethod method = methods.next();
						if (method.getName().equals(methodName))
						{
							return method.getRuntimeProperty(IScriptProvider.COMMENT);
						}
					}
					return null;
				}

				private boolean containsInheritDoc(String value)
				{
					return (value != null) && (value.indexOf("@inheritDoc") >= 0);
				}

				@Override
				public String getToolTip(String methodName)
				{
					String comment = sm.getRuntimeProperty(IScriptProvider.COMMENT);
					Form extendsForm = null;
					ISupportChilds parent = sm.getParent();
					if (parent instanceof Form)
					{
						extendsForm = (Form)parent;
						while (containsInheritDoc(comment))
						{
							comment = findDocInHierarchy(extendsForm, methodName);
							extendsForm = extendsForm.getExtendsForm();
						}
					}
					return getParsedComment(comment, null, false);
				}

				@Override
				public String[] getParameterNames(String methodName)
				{
					return getParameters(sm, PARAM.NAME);
				}

				@Override
				public boolean isDeprecated(String methodName)
				{
					return sm.isDeprecated();
				}

			}, null, getReturnTypeString(sm));
		}

		private static String[] getParameters(ScriptMethod sm, PARAM p)
		{
			MethodArgument[] args = sm.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);
			String[] parameters = new String[args.length];
			for (int i = 0; i < args.length; i++)
			{
				parameters[i] = p == PARAM.NAME ? args[i].getName() : args[i].getType().getName();
			}
			return parameters;
		}

		private static String getReturnTypeString(ScriptMethod sm)
		{
			MethodArgument returnTypeArgument = sm.getRuntimeProperty(IScriptProvider.METHOD_RETURN_TYPE);
			String returnTypeString = "void";
			if (returnTypeArgument != null)
			{
				if ("*".equals(returnTypeArgument.getType().getName())) returnTypeString = "Any";
				else returnTypeString = returnTypeArgument.getType().getName();
			}
			return returnTypeString;
		}

		private static String getPrefix(ScriptMethod sm)
		{
			String prefixedName = sm.getPrefixedName();
			int lastIdxOfName = prefixedName.lastIndexOf(sm.getName());
			if (lastIdxOfName != -1)
			{
				return prefixedName.substring(0, lastIdxOfName);
			}
			else
			{
				return "";
			}
		}
	}

	private static class FieldFeedback implements IDeveloperFeedback
	{
		private final ITagResolver resolver;
		private final IScriptObject scriptObject;
		private final String name;
		private final JavaMembers ijm;
		private final String prefix;

		FieldFeedback(String name, String prefix, ITagResolver resolver, IScriptObject scriptObject, JavaMembers ijm)
		{
			this.name = name;
			this.prefix = prefix;
			this.resolver = resolver;
			this.scriptObject = scriptObject;
			this.ijm = ijm;

		}

		/**
		 * @see com.servoy.eclipse.ui.node.IDeveloperFeedback#getCode()
		 */
		public String getCode()
		{
			return prefix + name;
		}

		/**
		 * @see com.servoy.eclipse.ui.node.IDeveloperFeedback#getSample()
		 */
		public String getSample()
		{
			if (scriptObject != null)
			{
				String sample = null;
				if (scriptObject instanceof XMLScriptObjectAdapter)
				{
					sample = ((XMLScriptObjectAdapter)scriptObject).getSample(name,
						ServoyModelManager.getServoyModelManager().getServoyModel().getActiveSolutionClientType());
				}
				if (sample == null || "".equals(sample))
				{
					sample = scriptObject.getSample(name);
				}
				CharSequence escapedSample = HtmlUtils.escapeMarkup(Text.processTags(sample, resolver));
				return escapedSample != null ? escapedSample.toString() : null;
			}
			return null;
		}

		/**
		 * @see com.servoy.eclipse.ui.node.IDeveloperFeedback#getToolTipText()
		 */
		public String getToolTipText()
		{
			String toolTip = null;
			if (scriptObject != null)
			{
				if (scriptObject instanceof XMLScriptObjectAdapter)
				{
					toolTip = ((XMLScriptObjectAdapter)scriptObject).getToolTip(name,
						ServoyModelManager.getServoyModelManager().getServoyModel().getActiveSolutionClientType());
				}
				else
				{
					toolTip = scriptObject.getToolTip(name);
				}
				toolTip = Text.processTags(toolTip, resolver);
			}
			if (toolTip == null) toolTip = "";

			String tmp = "";
			if (ijm != null)
			{
				Object bp = ijm.getField(name, false);
				if (bp instanceof JavaMembers.BeanProperty)
				{
					tmp = "<b>" + getReturnTypeString(((JavaMembers.BeanProperty)bp).getGetter().getReturnType()) + " " + name + "</b>";
				}
				else if (bp instanceof Field)
				{
					tmp = "<b>" + DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(((Field)bp).getType()) + " " + name + "</b>";
				}
				else if (bp == null)
				{
					// test if it is a Constant.
					bp = ijm.getField(name, true);
					if (bp instanceof Field)
					{
						tmp = "<b>" + prefix + name + "</b>";
					}
				}
			}
			if ("".equals(toolTip))
			{
				toolTip = tmp;
			}
			else
			{
				toolTip = tmp + "<br><pre>" + toolTip + "</pre>";
			}
			return toolTip;
		}
	}

	private static class ColumnFeedback implements IDeveloperFeedback
	{
		private final String prefix;
		private final IColumn c;

		ColumnFeedback(String prefix, IColumn c)
		{
			this.prefix = prefix;
			this.c = c;

		}

		/**
		 * @see com.servoy.eclipse.ui.node.IDeveloperFeedback#getCode()
		 */
		public String getCode()
		{
			return prefix + c.getDataProviderID();
		}

		/**
		 * @see com.servoy.eclipse.ui.node.IDeveloperFeedback#getSample()
		 */
		public String getSample()
		{
			return null;
		}

		/**
		 * @see com.servoy.eclipse.ui.node.IDeveloperFeedback#getToolTipText()
		 */
		public String getToolTipText()
		{
			return "<pre><b>" + c.getTypeAsString() + " " + c.getDataProviderID() + "</b></pre>";
		}

	}

	public static String getReturnTypeString(Class returnType)
	{
		if (returnType == null) return "*unknown*";
		StringBuilder sb = new StringBuilder();
		while (returnType.isArray())
		{
			sb.append("[]");
			returnType = returnType.getComponentType();
		}
		sb.insert(0, DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(returnType));
		return sb.toString();
	}

}
