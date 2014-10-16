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


import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.Icon;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.internal.javascript.ti.TypeSystemImpl;
import org.eclipse.dltk.javascript.scriptdoc.JavaDoc2HTMLTextReader;
import org.eclipse.dltk.javascript.typeinfo.DefaultMetaType;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.ITypeNames;
import org.eclipse.dltk.javascript.typeinfo.MetaType;
import org.eclipse.dltk.javascript.typeinfo.TypeCache;
import org.eclipse.dltk.javascript.typeinfo.TypeMemberQuery;
import org.eclipse.dltk.javascript.typeinfo.TypeUtil;
import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.typeinfo.model.JSType;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Parameter;
import org.eclipse.dltk.javascript.typeinfo.model.ParameterKind;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.SimpleType;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.dltk.javascript.typeinfo.model.TypeKind;
import org.eclipse.dltk.javascript.typeinfo.model.Visibility;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.JavaMembers.BeanProperty;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.BooleanPropertyType;
import org.sablo.specification.property.types.DatePropertyType;
import org.sablo.specification.property.types.DoublePropertyType;
import org.sablo.specification.property.types.FloatPropertyType;
import org.sablo.specification.property.types.IntPropertyType;
import org.sablo.specification.property.types.LongPropertyType;
import org.sablo.specification.property.types.StringPropertyType;

import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.JSDeveloperSolutionModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.Activator;
import com.servoy.eclipse.debug.IWebResourceChangedListener;
import com.servoy.eclipse.model.DesignApplication;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.util.IconProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;
import com.servoy.j2db.BasicFormController.JSForm;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormManager.HistoryProvider;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServoyBeanFactory;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.DataException;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.dataprocessing.datasource.DBDataSource;
import com.servoy.j2db.dataprocessing.datasource.DBDataSourceServer;
import com.servoy.j2db.dataprocessing.datasource.JSDataSource;
import com.servoy.j2db.dataprocessing.datasource.JSDataSources;
import com.servoy.j2db.dataprocessing.datasource.MemDataSource;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.DocumentationUtil;
import com.servoy.j2db.documentation.IParameter;
import com.servoy.j2db.documentation.ScriptParameter;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.documentation.scripting.docs.FormElements;
import com.servoy.j2db.documentation.scripting.docs.Forms;
import com.servoy.j2db.documentation.scripting.docs.Globals;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LiteralDataprovider;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.plugins.IBeanClassProvider;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.IPluginManager;
import com.servoy.j2db.querybuilder.impl.QBAggregate;
import com.servoy.j2db.querybuilder.impl.QBColumn;
import com.servoy.j2db.querybuilder.impl.QBColumns;
import com.servoy.j2db.querybuilder.impl.QBCondition;
import com.servoy.j2db.querybuilder.impl.QBFactory;
import com.servoy.j2db.querybuilder.impl.QBFunction;
import com.servoy.j2db.querybuilder.impl.QBFunctions;
import com.servoy.j2db.querybuilder.impl.QBGroupBy;
import com.servoy.j2db.querybuilder.impl.QBJoin;
import com.servoy.j2db.querybuilder.impl.QBJoins;
import com.servoy.j2db.querybuilder.impl.QBLogicalCondition;
import com.servoy.j2db.querybuilder.impl.QBParameter;
import com.servoy.j2db.querybuilder.impl.QBParameters;
import com.servoy.j2db.querybuilder.impl.QBPart;
import com.servoy.j2db.querybuilder.impl.QBResult;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.querybuilder.impl.QBSort;
import com.servoy.j2db.querybuilder.impl.QBSorts;
import com.servoy.j2db.querybuilder.impl.QBTableClause;
import com.servoy.j2db.querybuilder.impl.QBWhereCondition;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.IJavaScriptType;
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
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.scripting.annotations.JSSignature;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.TagStringPropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.ui.IScriptAccordionPanelMethods;
import com.servoy.j2db.ui.IScriptDataLabelMethods;
import com.servoy.j2db.ui.IScriptInsetListComponentMethods;
import com.servoy.j2db.ui.IScriptMobileBean;
import com.servoy.j2db.ui.IScriptPortalComponentMethods;
import com.servoy.j2db.ui.IScriptScriptLabelMethods;
import com.servoy.j2db.ui.IScriptSplitPaneMethods;
import com.servoy.j2db.ui.IScriptTabPanelMethods;
import com.servoy.j2db.ui.runtime.IRuntimeButton;
import com.servoy.j2db.ui.runtime.IRuntimeCalendar;
import com.servoy.j2db.ui.runtime.IRuntimeCheck;
import com.servoy.j2db.ui.runtime.IRuntimeChecks;
import com.servoy.j2db.ui.runtime.IRuntimeCombobox;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.ui.runtime.IRuntimeDataButton;
import com.servoy.j2db.ui.runtime.IRuntimeHtmlArea;
import com.servoy.j2db.ui.runtime.IRuntimeImageMedia;
import com.servoy.j2db.ui.runtime.IRuntimeListBox;
import com.servoy.j2db.ui.runtime.IRuntimePassword;
import com.servoy.j2db.ui.runtime.IRuntimeRadio;
import com.servoy.j2db.ui.runtime.IRuntimeRadios;
import com.servoy.j2db.ui.runtime.IRuntimeRtfArea;
import com.servoy.j2db.ui.runtime.IRuntimeSpinner;
import com.servoy.j2db.ui.runtime.IRuntimeTextArea;
import com.servoy.j2db.ui.runtime.IRuntimeTextField;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class TypeCreator extends TypeCache
{
	static final String HIDDEN_IN_RELATED = "HIDDEN_IN_RELATED";

	private static final String SCOPE_TABLES = "scope:tables";

	private static final int INSTANCE_METHOD = 1;
	private static final int STATIC_METHOD = 2;
	private static final int INSTANCE_FIELD = 3;
	private static final int STATIC_FIELD = 4;

	public static final String PLUGIN_TYPE_PREFIX = "plugins.";

	protected final static ImageDescriptor METHOD = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/function.gif"), null));
	protected final static ImageDescriptor PROPERTY = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/properties_icon.gif"), null));
	protected final static ImageDescriptor CONSTANT = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/constant.gif"), null));

	protected final static ImageDescriptor ELEMENTS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/elements.gif"), null));

	protected final static ImageDescriptor SPECIAL_PROPERTY = ImageDescriptor.createFromURL(FileLocator.find(
		com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/special_properties_icon.gif"), null));

	protected final static ImageDescriptor PUBLIC_GLOBAL_VAR_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(),
		new Path("/icons/variable_public.gif"), null));
	protected final static ImageDescriptor PRIVATE_GLOBAL_VAR_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(),
		new Path("/icons/variable_private.gif"), null));
	protected final static ImageDescriptor PUBLIC_GLOBAL_METHOD_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(),
		new Path("/icons/public_method.gif"), null));
	protected final static ImageDescriptor PROTECTED_GLOBAL_METHOD_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(),
		new Path("/icons/protected_method.gif"), null));
	protected final static ImageDescriptor PRIVATE_GLOBAL_METHOD_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(),
		new Path("/icons/private_method.gif"), null));

	protected final static ImageDescriptor FORM_PUBLIC_METHOD_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(),
		new Path("/icons/public_method.gif"), null));
	protected final static ImageDescriptor FORM_PROTECTED_METHOD_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(),
		new Path("/icons/protected_method.gif"), null));
	protected final static ImageDescriptor FORM_PRIVATE_METHOD_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(),
		new Path("/icons/private_method.gif"), null));
	protected final static ImageDescriptor FORM_PUBLIC_VARIABLE_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(),
		new Path("/icons/variable_public.gif"), null));
	protected final static ImageDescriptor FORM_PRIVATE_VARIABLE_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(),
		new Path("/icons/variable_private.gif"), null));

	protected final static ImageDescriptor FOUNDSET_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/foundset.gif"), null));
	protected final static ImageDescriptor RELATION_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/relation.gif"), null));
	protected final static ImageDescriptor RELATION_PROTECTED_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(
		com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/relation_protected.gif"), null));
	protected final static ImageDescriptor RELATION_PRIVATE_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(
		com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/relation_private.gif"), null));

	protected final static ImageDescriptor COLUMN_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/column.gif"), null));
	protected final static ImageDescriptor COLUMN_AGGR_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/columnaggr.gif"), null));
	protected final static ImageDescriptor COLUMN_CALC_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/columncalc.gif"), null));

	protected final static ImageDescriptor GLOBALS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/globe.gif"), null));
	protected final static ImageDescriptor SCOPES = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/scopes.gif"), null));
	protected final static ImageDescriptor FORMS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/forms.gif"), null));

	protected final static ImageDescriptor PLUGINS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/plugin.gif"), null));

	protected final static ImageDescriptor PLUGIN_DEFAULT = ImageDescriptor.createFromURL(FileLocator.find(
		com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/plugin_conn.gif"), null));

	public static final String ARRAY_INDEX_PROPERTY_PREFIX = "array__indexedby_";

	public static final String IMAGE_DESCRIPTOR = "servoy.IMAGEDESCRIPTOR";
	public static final String RESOURCE = "servoy.RESOURCE";
	public static final String VALUECOLLECTION = "servoy.VALUECOLLECTION";
	public static final String LAZY_VALUECOLLECTION = "servoy.LAZY_VALUECOLLECTION";

	public final static Set<String> BASE_TYPES = new HashSet<String>(128);

	static
	{
		BASE_TYPES.add("Object");
		BASE_TYPES.add("Number");
		BASE_TYPES.add("Array");
		BASE_TYPES.add("String");
		BASE_TYPES.add("Date");
		BASE_TYPES.add("Function");
		BASE_TYPES.add("Boolean");
		BASE_TYPES.add("RegExp");
		BASE_TYPES.add("Error");
		BASE_TYPES.add("Math");
	}

	/*
	 * JAVASCRIPT TYPE NAME (not java class name for those that have a different name in JS! See
	 * DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(cls)) -> java class
	 */
	private final ConcurrentMap<String, Class< ? >> classTypes = new ConcurrentHashMap<String, Class< ? >>();
	private final ConcurrentMap<String, Class< ? >> anonymousClassTypes = new ConcurrentHashMap<String, Class< ? >>();
	private final ConcurrentMap<String, String> wcTypeNames = new ConcurrentHashMap<String, String>();
	private final ConcurrentMap<String, WebComponentSpecification> wcServices = new ConcurrentHashMap<String, WebComponentSpecification>();


	private final ConcurrentMap<String, IScopeTypeCreator> scopeTypes = new ConcurrentHashMap<String, IScopeTypeCreator>();
	protected final ConcurrentMap<Class< ? >, Class< ? >[]> linkedTypes = new ConcurrentHashMap<Class< ? >, Class< ? >[]>();
	protected final ConcurrentMap<Class< ? >, String> prefixedTypes = new ConcurrentHashMap<Class< ? >, String>();
	private final ConcurrentMap<String, ClientSupport> typesClientSupport = new ConcurrentHashMap<String, ClientSupport>();
	private volatile boolean initialized;
	protected static final List<String> objectMethods = Arrays.asList(new String[] { "wait", "toString", "hashCode", "equals", "notify", "notifyAll", "getClass" });


	private final TypeSystemImpl servoyStaticTypeSystem = new TypeSystemImpl()
	{
		@Override
		protected Type doResolveType(Type type)
		{
			Type resolved = super.doResolveType(type);
			if (resolved.isProxy())
			{
				final String typeName = URI.decode(((InternalEObject)type).eProxyURI().fragment());
				resolved = findType(null, typeName);
			}
			return resolved;
		}
	};

	private final ServoyStaticMetaType staticMetaType = new ServoyStaticMetaType(servoyStaticTypeSystem);

	private final MetaType javaMetaType = new JavaRuntimeMetaType(servoyStaticTypeSystem);


	public TypeCreator()
	{
		super("servoy", "javascript");
		addType("JSDataSet", JSDataSet.class);
		addType(IExecutingEnviroment.TOPLEVEL_SERVOY_EXCEPTION, ServoyException.class);
		addType(DataException.class.getSimpleName(), DataException.class);

		addAnonymousClassType("Controller", JSForm.class);
		addAnonymousClassType(JSApplication.class);
		addAnonymousClassType(JSI18N.class);
		addAnonymousClassType(HistoryProvider.class);
		addAnonymousClassType(JSUtils.class);
		addAnonymousClassType("JSUnit", JSUnitAssertFunctions.class);
		addAnonymousClassType(JSSolutionModel.class);
		addAnonymousClassType(JSDatabaseManager.class);
		addAnonymousClassType(JSDeveloperSolutionModel.class);
		addAnonymousClassType(JSSecurity.class);
		ElementResolver.registerConstantType("JSSecurity", "JSSecurity");


		addScopeType(Record.JS_RECORD, new RecordCreator());
		addScopeType(FoundSet.JS_FOUNDSET, new FoundSetCreator());
		addScopeType("JSDataSet", new JSDataSetCreator());
		addScopeType("Form", new FormScopeCreator());
		addScopeType("RuntimeForm", new FormScopeCreator());
		addScopeType("Elements", new ElementsScopeCreator());
		addScopeType("Plugins", new PluginsScopeCreator());
		addScopeType("Forms", new FormsScopeCreator());
		addScopeType("Relations", new RelationsScopeCreator());
		addScopeType("Dataproviders", new DataprovidersScopeCreator());
		addScopeType("InvisibleRelations", new InvisibleRelationsScopeCreator());
		addScopeType("InvisibleDataproviders", new InvisibleDataprovidersScopeCreator());
		addScopeType("Scopes", new ScopesScopeCreator());
		addScopeType("Scope", new ScopeScopeCreator());
		addScopeType(QBAggregate.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBColumn.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBCondition.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBFactory.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBFunction.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBGroupBy.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBJoin.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBJoins.class.getSimpleName(), new QueryBuilderJoinsCreator());
		addScopeType(QBLogicalCondition.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBWhereCondition.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBResult.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBSelect.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBSort.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBSorts.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBTableClause.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBPart.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBParameter.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBParameters.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBColumns.class.getSimpleName(), new QueryBuilderColumnsCreator());
		addScopeType(QBFunctions.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(MemDataSource.class.getSimpleName(), new MemDataSourceCreator());
		addScopeType(DBDataSource.class.getSimpleName(), new DBDataSourceCreator());
		addScopeType(DBDataSourceServer.class.getSimpleName(), new DBDataSourceServerCreator());
		addScopeType(JSDataSource.class.getSimpleName(), new TypeWithConfigCreator(JSDataSource.class, ClientSupport.wc_sc));
		addScopeType(JSDataSources.class.getSimpleName(), new JSDataSourcesCreator());
		createSpecTypeDefinitions();
	}

	private void createSpecTypeDefinitions()
	{
		WebComponentSpecification[] webComponentSpecifications = WebComponentSpecProvider.getInstance().getWebComponentSpecifications();
		WebComponentSpecification[] webServiceSpecifications = WebServiceSpecProvider.getInstance().getWebServiceSpecifications();
		Collection<WebComponentSpecification> specs = new ArrayList<WebComponentSpecification>();
		Collections.addAll(specs, webComponentSpecifications);
		Collections.addAll(specs, webServiceSpecifications);
		for (WebComponentSpecification webComponentSpecification : specs)
		{
			Map<String, IPropertyType< ? >> foundTypes = webComponentSpecification.getFoundTypes();
			for (String typeName : foundTypes.keySet())
			{
				IPropertyType< ? > iPropertyType = foundTypes.get(typeName);
				Type type = TypeInfoModelFactory.eINSTANCE.createType();
				type.setName(iPropertyType.getName());
				type.setKind(TypeKind.JAVA);
				EList<Member> members = type.getMembers();
				addType(null, type);
				if (iPropertyType instanceof ICustomType< ? >)
				{
					ICustomType< ? > customType = (ICustomType< ? >)iPropertyType;
					PropertyDescription customJSONTypeDefinition = customType.getCustomJSONTypeDefinition();
					Map<String, PropertyDescription> properties = customJSONTypeDefinition.getProperties();
					for (PropertyDescription pd : properties.values())
					{
						if ("design".equals(pd.getScope()))
						{
							// skip design properties
							continue;
						}
						String name = pd.getName();
						// skip the default once added by servoy, see WebComponentPackage.getWebComponentDescriptions()
						// and skip the dataprovider properties (those are not accesable through scripting)
						if (!name.equals("location") && !name.equals("size") && !name.equals("anchors") && pd.getType() != DataproviderPropertyType.INSTANCE)
						{
							JSType memberType = getType(null, pd);
							if (memberType == null) memberType = getTypeRef(null, pd.getType().getName());
							if (pd.getType() instanceof CustomJSONArrayType< ? , ? >)
							{
								memberType = TypeUtil.arrayOf(memberType);
							}
							members.add(createProperty(name, false, memberType, "", null));
						}
					}
				}

			}
		}
	}

	private final ConcurrentHashMap<String, Boolean> ignorePackages = new ConcurrentHashMap<String, Boolean>();

	@Override
	protected String[] getAccessibleBuckets(String context)
	{
		return new String[] { null, SCOPE_TABLES };
	}

	@Override
	protected Type createType(String context, String typeName)
	{
		if (BASE_TYPES.contains(typeName) || typeName.startsWith("Array<")) return null;
		if (!initialized) initalize();
		Type type = null;
		if (typeName.startsWith("Packages.") || typeName.startsWith("java.") || typeName.startsWith("javax."))
		{
			String name = typeName;
			if (name.startsWith("Packages."))
			{
				name = name.substring("Packages.".length());
				type = findType(context, name);
				if (type != null)
				{
					return type;
				}
			}
			if (ignorePackages.containsKey(name)) return null;
			try
			{
				int lastDot = name.lastIndexOf('.');
				if (lastDot != -1)
				{
					if (findType(context, name.substring(0, lastDot)) != null)
					{
						// type found, so this is a inner class
						name = name.substring(0, lastDot) + '$' + name.substring(lastDot + 1);
					}
				}
				Class< ? > clz = null;
				try
				{
					clz = Class.forName(name);
				}
				catch (Exception e)
				{
				}
				if (clz == null)
				{
					ClassLoader cl = com.servoy.eclipse.core.Activator.getDefault().getDesignClient().getBeanManager().getClassLoader();
					if (cl == null) cl = Thread.currentThread().getContextClassLoader();
					clz = Class.forName(name, false, cl);
				}
				type = getClassType(context, clz, name);
			}
			catch (ClassNotFoundException e)
			{
				ignorePackages.put(name, Boolean.FALSE);
			}
		}
		else if (typeName.equals("Continuation"))
		{
			type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVASCRIPT);
			type.setSuperType(getType(context, "Function"));
		}
		else if (typeName.equals("byte"))
		{
			// special support for byte type (mostly used in Array<byte>)
			type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVASCRIPT);
			type.setSuperType(getType(context, ITypeNames.NUMBER));
		}
		if (type != null)
		{
			return addType(null, type);
		}

		String realTypeName = typeName;
		if (realTypeName.equals("JSFoundset")) realTypeName = FoundSet.JS_FOUNDSET;
		type = createClassType(context, realTypeName, realTypeName);
		if (type != null)
		{
			if (type.eResource() != null) return type;
			return addType(null, type);
		}

		FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
		if (fs != null)
		{
			type = createDynamicType(context, realTypeName, realTypeName);
			if (type != null && realTypeName.indexOf('<') != -1 && type.eResource() == null && !type.isProxy())
			{
				return addType(fs.getSolution().getName(), type);
			}
		}
		else
		{
			type = createDynamicType(context, realTypeName, realTypeName);
		}
		return type;
	}

	private Type getClassType(String context, Class< ? > clz, String name)
	{
		Type type = TypeInfoModelFactory.eINSTANCE.createType();
		type.setName(name);
		type.setKind(TypeKind.JAVA);
		type.setAttribute(JavaRuntimeType.JAVA_CLASS, clz);
		type.setMetaType(javaMetaType);

		java.lang.reflect.Method[] methods = clz.getMethods();
		Field[] fields = clz.getFields();

		EList<Member> members = type.getMembers();

		for (Field field : fields)
		{
			Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
			property.setName(field.getName());
			property.setType(getJSType(context, field.getType()));
			if (Modifier.isStatic(field.getModifiers()))
			{
				property.setStatic(true);
			}
			members.add(property);
		}
		for (java.lang.reflect.Method method : methods)
		{
			org.eclipse.dltk.javascript.typeinfo.model.Method m = TypeInfoModelFactory.eINSTANCE.createMethod();
			m.setName(method.getName());
			m.setType(getJSType(context, method.getReturnType()));

			EList<Parameter> parameters = m.getParameters();
			Class< ? >[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++)
			{
				Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
				parameter.setName("arg" + i);
				JSType jsType;
				if (i == (parameterTypes.length - 1) && method.isVarArgs() && parameterTypes[i].isArray())
				{
					jsType = getJSType(context, parameterTypes[i].getComponentType());
					parameter.setKind(ParameterKind.VARARGS);
				}
				else
				{
					jsType = getJSType(context, parameterTypes[i]);
				}
				// don't set any type of it is just Object, so that everything is excepted
				if (!jsType.getName().equals("java.lang.Object")) parameter.setType(jsType);
				parameters.add(parameter);
			}
			if (Modifier.isStatic(method.getModifiers()))
			{
				m.setStatic(true);
			}
			members.add(m);
		}
		return type;
	}

	private JSType getJSType(String context, Class< ? > type)
	{
		if (type != null && type != Void.class && type != void.class)
		{
			if (type == Object.class)
			{
				return getTypeRef(context, "java.lang.Object");
			}
			if (type.isArray())
			{
				Class< ? > componentType = type.getComponentType();
				JSType componentJSType = getJSType(context, componentType);
				if (componentJSType != null)
				{
					return TypeUtil.arrayOf(componentJSType);
				}
				return getTypeRef(context, ITypeNames.ARRAY);
			}
			if (type == Boolean.class || type == boolean.class)
			{
				return getTypeRef(context, ITypeNames.BOOLEAN);
			}
			if (type == Byte.class || type == byte.class)
			{
				return getTypeRef(context, "byte");
			}
			if (Number.class.isAssignableFrom(type) || type.isPrimitive())
			{
				return getTypeRef(context, ITypeNames.NUMBER);
			}
			if (type == String.class || type == CharSequence.class)
			{
				return getTypeRef(context, ITypeNames.STRING);
			}

			return getTypeRef(context, "Packages." + type.getName());
		}
		return null;
	}


	protected void initalize()
	{
		DesignApplication application = com.servoy.eclipse.core.Activator.getDefault().getDesignClient();
		synchronized (this)
		{
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSApplication.class), null);
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSSecurity.class), null);
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSSolutionModel.class), null);
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSDatabaseManager.class), null);
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSDataSources.class), null);
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(ServoyException.class), null);

			List<IClientPlugin> lst = application.getPluginManager().getPlugins(IClientPlugin.class);
			for (IClientPlugin clientPlugin : lst)
			{
				// for now cast to deprecated interface
				try
				{
					java.lang.reflect.Method method = clientPlugin.getClass().getMethod("getScriptObject", (Class[])null);
					if (method != null)
					{
						IScriptable scriptObject = (IScriptable)method.invoke(clientPlugin, (Object[])null);
						if (scriptObject instanceof IReturnedTypesProvider)
						{
							String prefix = "plugins." + clientPlugin.getName() + ".";
							registerConstantsForScriptObject((IReturnedTypesProvider)scriptObject, prefix);
							if (((IReturnedTypesProvider)scriptObject).getAllReturnedTypes() != null &&
								((IReturnedTypesProvider)scriptObject).getAllReturnedTypes().length > 0)
							{
								linkedTypes.put(scriptObject.getClass(), ((IReturnedTypesProvider)scriptObject).getAllReturnedTypes());
							}
						}
					}
				}
				catch (Throwable e)
				{
					Debug.error("error registering constants for client plugin ", e);
				}
			}
		}
		IBeanClassProvider beanManager = (IBeanClassProvider)application.getBeanManager();
		Class< ? >[] allBeanClasses = beanManager.getAllBeanClasses();
		for (Class< ? > beanClass : allBeanClasses)
		{
			if (IServoyBeanFactory.class.isAssignableFrom(beanClass))
			{
				try
				{
					IServoyBeanFactory beanFactory = (IServoyBeanFactory)beanClass.newInstance();
					Object beanInstance = beanFactory.getBeanInstance(application.getApplicationType(), (IClientPluginAccess)application.getPluginAccess(),
						new Object[] { "developer", "developer", null });
					addType(beanClass.getSimpleName(), beanInstance.getClass());
				}
				catch (Exception e)
				{
					ServoyLog.logError("error creating bean for in the js type provider", e);
				}
				catch (NoClassDefFoundError e)
				{
					ServoyLog.logError("error creating bean for in the js type provider", e);
				}
			}
			else
			{
				addType(beanClass.getSimpleName(), beanClass);
			}
		}

		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		synchronized (this)
		{
			if (!initialized)
			{
				initialized = true;
				if (servoyModel instanceof ServoyModel)
				{
					((ServoyModel)servoyModel).addPersistChangeListener(true, new IPersistChangeListener()
					{
						public void persistChanges(Collection<IPersist> changes)
						{
							Job job = new Job("clearing cache")
							{

								@Override
								public IStatus run(IProgressMonitor monitor)
								{
									flushCache();
									return Status.OK_STATUS;
								}
							};
							job.setRule(ResourcesPlugin.getWorkspace().getRoot());
							job.schedule();
						}
					});
					((ServoyModel)servoyModel).addActiveProjectListener(new IActiveProjectListener()
					{
						public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
						{
							return true;
						}

						public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
						{
							// todo maybe fush on certain things?
						}

						public void activeProjectChanged(ServoyProject activeProject)
						{
							Job job = new Job("clearing cache")
							{
								@Override
								public IStatus run(IProgressMonitor monitor)
								{
									for (IScopeTypeCreator creator : scopeTypes.values())
									{
										creator.flush();
									}
									servoyStaticTypeSystem.reset();
									clear(null);
									flushCache();
									return Status.OK_STATUS;
								}
							};
							job.setRule(ResourcesPlugin.getWorkspace().getRoot());
							job.schedule();
						}
					});
				}

				Activator.getDefault().addWebComponentChangedListener(new IWebResourceChangedListener()
				{
					@Override
					public void changed()
					{
						Job job = new Job("clearing cache")
						{

							@Override
							public IStatus run(IProgressMonitor monitor)
							{
								flushCache();
								return Status.OK_STATUS;
							}
						};
						job.setRule(ResourcesPlugin.getWorkspace().getRoot());
						job.schedule();
					}
				});
			}
		}
	}

	protected final Class< ? > getTypeClass(String name)
	{
		Class< ? > clz = classTypes.get(name);
		if (clz == null)
		{
			clz = anonymousClassTypes.get(name);
		}
		return clz;
	}


	public final Set<String> getTypeNames(String prefix)
	{
		Set<String> names = new HashSet<String>(classTypes.keySet());
		if (prefix != null && !"".equals(prefix.trim()))
		{
			String lowerCasePrefix = prefix.toLowerCase();
			Iterator<String> iterator = names.iterator();
			while (iterator.hasNext())
			{
				String name = iterator.next();
				if (!PLUGIN_TYPE_PREFIX.startsWith(prefix) && name.startsWith(PLUGIN_TYPE_PREFIX)) name = name.substring(name.lastIndexOf(".") + 1);
				if (!name.toLowerCase().startsWith(lowerCasePrefix)) iterator.remove();
			}
		}
		return names;
	}

	protected final void registerConstantsForScriptObject(IReturnedTypesProvider scriptObject, String prefix)
	{
		if (scriptObject == null) return;
		Class< ? >[] allReturnedTypes = scriptObject.getAllReturnedTypes();
		if (allReturnedTypes == null) return;

		for (Class< ? > element : allReturnedTypes)
		{
			boolean constant = false;
			JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(element, null);
			if (javaMembers != null)
			{
				Object[] members = javaMembers.getIds(false);
				ArrayList<String> al = new ArrayList<String>(members.length);
				for (Object el : members)
				{
					al.add((String)el);
				}
				if (javaMembers instanceof InstanceJavaMembers)
				{
					al.removeAll(((InstanceJavaMembers)javaMembers).getGettersAndSettersToHide());
				}
				else
				{
					al.removeAll(objectMethods);
				}
				// skip constants only classes
				constant = al.size() == 0;
			}

			boolean add = false;
			if (IPrefixedConstantsObject.class.isAssignableFrom(element))
			{
				add = true;
				try
				{
					IPrefixedConstantsObject constants = (IPrefixedConstantsObject)element.newInstance();
					if (constant)
					{
						addAnonymousClassType(constants.getPrefix(), element);
						ElementResolver.registerConstantType(constants.getPrefix(), constants.getPrefix());
						if (prefix != null)
						{
							addAnonymousClassType(prefix + constants.getPrefix(), element);
							ElementResolver.registerConstantType(prefix + constants.getPrefix(), prefix + constants.getPrefix());
						}
					}
					else
					{
						addType(constants.getPrefix(), element);
						if (prefix != null)
						{
							addType(prefix + constants.getPrefix(), element);
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
			else if (IConstantsObject.class.isAssignableFrom(element) || IJavaScriptType.class.isAssignableFrom(element))
			{
				add = true;
				if (constant)
				{
					addAnonymousClassType(element.getSimpleName(), element);
					ElementResolver.registerConstantType(element.getSimpleName(), element.getSimpleName());
					if (prefix != null)
					{
						addAnonymousClassType(prefix + element.getSimpleName(), element);
						ElementResolver.registerConstantType(prefix + element.getSimpleName(), prefix + element.getSimpleName());
					}
				}
				else
				{
					String name = element.getSimpleName();
					ServoyDocumented sd = element.getAnnotation(ServoyDocumented.class);
					if (sd != null && sd.scriptingName() != null && sd.scriptingName().trim().length() > 0)
					{
						// documentation has overridden scripting name
						name = sd.scriptingName().trim();
					}

					addType(name, element);
					if (prefix != null)
					{
						addType(prefix + name, element);
					}
				}

			}
			if (prefix != null && add)
			{
				prefixedTypes.put(element, prefix);
			}
		}
	}

	protected Type createDynamicType(String context, String typeNameClassName, String fullTypeName)
	{
		// is it a 'generified' type
		int index = typeNameClassName.indexOf('<');
		if (index != -1 && (typeNameClassName.indexOf('>', index)) != -1)
		{
			String fullClassName = typeNameClassName;
			String classType = fullClassName.substring(0, index);
			if (classType.equals("JSFoundset"))
			{
				classType = FoundSet.JS_FOUNDSET;
				fullClassName = classType + fullClassName.substring(index);
			}
			Type type = createDynamicType(context, classType, fullClassName);
			if (type == null) type = createClassType(context, classType, fullClassName);
			return type;
		}

		IScopeTypeCreator creator = scopeTypes.get(typeNameClassName);
		if (creator != null)
		{
			return creator.createType(context, fullTypeName);
		}
		return null;
	}

	/**
	 * @param typeNameClassName
	 * @return
	 */
	protected final Type createClassType(String context, String typeNameClassName, String fullTypeName)
	{
		Class< ? > cls = getTypeClass(typeNameClassName);
		if (cls != null)
		{
			return createType(context, fullTypeName, cls);
		}
		if (wcTypeNames.get(typeNameClassName) != null)
		{
			WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(typeNameClassName);
			if (spec != null)
			{
				return createWebComponentType(context, fullTypeName, spec);
			}
		}
		WebComponentSpecification webComponentSpecification = wcServices.get(typeNameClassName);
		if (webComponentSpecification != null)
		{
			return createWebComponentType(context, fullTypeName, webComponentSpecification);
		}
		return null;
	}

	/**
	 * @param context
	 * @param fullTypeName
	 * @param spec
	 * @return
	 */
	private Type createWebComponentType(String context, String fullTypeName, WebComponentSpecification spec)
	{
		Type type = TypeInfoModelFactory.eINSTANCE.createType();
		type.setName(fullTypeName);
		type.setKind(TypeKind.JAVA);
		EList<Member> members = type.getMembers();
		Map<String, PropertyDescription> properties = spec.getProperties();
		for (PropertyDescription pd : properties.values())
		{
			if ("design".equals(pd.getScope()))
			{
				// skip design properties
				continue;
			}
			String name = pd.getName();
			// skip the default once added by servoy, see WebComponentPackage.getWebComponentDescriptions()
			// and skip the dataprovider properties (those are not accesable through scripting)
			if (!name.equals("location") && !name.equals("size") && !name.equals("anchors") && pd.getType() != DataproviderPropertyType.INSTANCE)
			{
				JSType memberType = getType(context, pd);
				if (memberType == null) memberType = getTypeRef(null, pd.getType().getName());
				if (pd.getType() instanceof CustomJSONArrayType< ? , ? >)
				{
					memberType = TypeUtil.arrayOf(memberType);
				}
				members.add(createProperty(name, false, memberType, "", null));
			}
		}
		Map<String, WebComponentApiDefinition> apis = spec.getApiFunctions();
		for (WebComponentApiDefinition api : apis.values())
		{
			Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
			method.setName(api.getName());
			JSType returnType = getType(context, api.getReturnType());
			if (returnType == null && api.getReturnType() != null)
			{
				returnType = getTypeRef(null, api.getReturnType().getType().getName());
				if (api.getReturnType().getConfig() instanceof Boolean)
				{
					if ((Boolean)api.getReturnType().getConfig()) returnType = TypeUtil.arrayOf(returnType);
				}
			}
			method.setType(returnType);
			EList<Parameter> parameters = method.getParameters();
			for (PropertyDescription paramDesc : api.getParameters())
			{
				Parameter param = TypeInfoModelFactory.eINSTANCE.createParameter();
				param.setName(paramDesc.getName());
				if (paramDesc.isOptional()) param.setKind(ParameterKind.OPTIONAL);

				JSType paramType = getType(context, paramDesc);
				if (paramType == null)
				{
					paramType = getTypeRef(null, paramDesc.getType().getName());
					if (paramDesc.getConfig() instanceof Boolean)
					{
						if ((Boolean)paramDesc.getConfig()) paramType = TypeUtil.arrayOf(paramType);
					}
				}
				param.setType(paramType);
				parameters.add(param);
			}

			members.add(method);
		}
		return addType("WEB:COMPONENTS", type);
	}

	/**
	 * @param type
	 * @return
	 */
	private JSType getType(String context, PropertyDescription pd)
	{
		if (pd == null) return null;
		IPropertyType< ? > type = pd.getType();
		if (type == BooleanPropertyType.INSTANCE) return getTypeRef(context, ITypeNames.BOOLEAN);
		if (type == IntPropertyType.INSTANCE || type == LongPropertyType.INSTANCE || type == FloatPropertyType.INSTANCE || type == DoublePropertyType.INSTANCE) return getTypeRef(
			context, ITypeNames.NUMBER);
		if (type == StringPropertyType.INSTANCE || type == TagStringPropertyType.INSTANCE) return getTypeRef(context, ITypeNames.STRING);
		if (DatePropertyType.TYPE_NAME.equals(type.getName())) return getTypeRef(context, ITypeNames.DATE);
		return null;
	}

	/**
	 * @param context
	 * @param typeName
	 * @param cls
	 * @return
	 */
	protected final Type createType(String context, String typeName, Class< ? > cls)
	{
		Type type = TypeInfoModelFactory.eINSTANCE.createType();
		type.setName(typeName);
		type.setKind(TypeKind.JAVA);
		EList<Member> members = type.getMembers();
		fill(context, members, cls, typeName);

		if (cls != ServoyException.class && !IFoundSet.class.isAssignableFrom(cls))
		{
			ImageDescriptor desc = IconProvider.instance().descriptor(cls);
			type.setAttribute(IMAGE_DESCRIPTOR, desc);
		}
		if (IDeprecated.class.isAssignableFrom(cls) || cls.isAnnotationPresent(Deprecated.class) ||
			(prefixedTypes.containsKey(cls) && typeName.equals(cls.getSimpleName())))
		{
			makeDeprecated(type);
		}

		Type superT = null;
		ServoyDocumented anno = cls.getAnnotation(ServoyDocumented.class);
		if (anno != null && anno.extendsComponent() != null && !anno.extendsComponent().trim().equals(""))
		{
			superT = getType(context, anno.extendsComponent().trim());
			if (superT == null) ServoyLog.logWarning(
				"@ServoyDocumented.extendsComponent for type '" + typeName + "' was not found. Value: " + anno.extendsComponent(), null);
		}
		else if (cls != IRuntimeComponent.class && IRuntimeComponent.class.isAssignableFrom(cls))
		{
			superT = getType(context, "RuntimeComponent");
		}
		else if (cls.getSuperclass() != null)
		{
			Class< ? > superCls = classTypes.get(DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(cls.getSuperclass()));
			if (superCls != null)
			{
				JavaMembers superClassMembers = ScriptObjectRegistry.getJavaMembers(superCls, null);
				JavaMembers classMembers = ScriptObjectRegistry.getJavaMembers(cls, null);
				// only add the super type if both are of the same javamembers class (instance or not) or the super class is a specific js class.
				if (classMembers.getClass() == superClassMembers.getClass() || superClassMembers instanceof InstanceJavaMembers)
				{
					superT = getType(context, cls.getSuperclass().getSimpleName());
				}
			}
		}
		if (superT != null)
		{
			type.setSuperType(superT);
		}

		Class< ? >[] returnTypes = linkedTypes.get(cls);
		if (returnTypes != null)
		{
			int index = typeName.indexOf('<');
			int index2;
			String config = typeName;
			if (index != -1 && (index2 = typeName.indexOf('>', index)) != -1)
			{
				config = typeName.substring(index + 1, index2);
			}
			for (Class< ? > returnTypeClass : returnTypes)
			{
				String name = returnTypeClass.getSimpleName();
				if (IPrefixedConstantsObject.class.isAssignableFrom(returnTypeClass))
				{
					try
					{
						IPrefixedConstantsObject constants = (IPrefixedConstantsObject)returnTypeClass.newInstance();
						name = constants.getPrefix();
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
				else if (!(IConstantsObject.class.isAssignableFrom(returnTypeClass) || IJavaScriptType.class.isAssignableFrom(returnTypeClass)))
				{
					continue;
				}
				String prefix = PLUGIN_TYPE_PREFIX + config + ".";
				Property property = createProperty(name, true, TypeUtil.classType(getType(context, prefix + name)), null, null);
				if (IDeprecated.class.isAssignableFrom(returnTypeClass) || returnTypeClass.isAnnotationPresent(Deprecated.class))
				{
					makeDeprecated(property);
				}
				members.add(property);
			}
		}
		return type;
	}

	/**
	 * @param typeName
	 * @param members
	 * @param class1
	 */
	@SuppressWarnings("deprecation")
	private final void fill(String context, EList<Member> membersList, Class< ? > scriptObjectClass, String typeName)
	{
		ArrayList<String> al = new ArrayList<String>();
		JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(scriptObjectClass, null);
		if (Scriptable.class.isAssignableFrom(scriptObjectClass) && !(javaMembers instanceof InstanceJavaMembers))
		{
			// if the class is a scriptable an the javamembers is not a instance java members, just return nothing.
			return;
		}
		if (javaMembers != null)
		{
			IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
			Object[] members = javaMembers.getIds(false);
			for (Object element : members)
			{
				al.add((String)element);
			}
			if (IConstantsObject.class.isAssignableFrom(scriptObjectClass))
			{
				members = javaMembers.getIds(true);
				for (Object element : members)
				{
					al.add((String)element);
				}
			}
			if (javaMembers instanceof InstanceJavaMembers)
			{
				al.removeAll(((InstanceJavaMembers)javaMembers).getGettersAndSettersToHide());
			}
			else
			{
				al.removeAll(objectMethods);
			}

			List<Member> newMembers = new ArrayList<Member>();
			for (String name : al)
			{
				int type = 0;
				Object object = javaMembers.getMethod(name, false);
				if (object == null)
				{
					object = javaMembers.getField(name, false);
					if (object == null)
					{
						object = javaMembers.getField(name, true);
						if (object != null)
						{
							type = STATIC_FIELD;
						}
						else
						{
							object = javaMembers.getMethod(name, true);
							type = STATIC_METHOD;
						}
					}
					else type = INSTANCE_FIELD;
				}
				else type = INSTANCE_METHOD;

				if (object != null)
				{
					if (type == INSTANCE_METHOD || type == STATIC_METHOD)
					{
						MemberBox[] memberbox = null;
						if (object instanceof NativeJavaMethod)
						{
							memberbox = ((NativeJavaMethod)object).getMethods();
						}
						int membersSize = memberbox == null ? 0 : memberbox.length;
						for (int i = 0; i < membersSize; i++)
						{
							Class< ? > returnTypeClz = getReturnType(memberbox[i]);
							Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
							method.setName(name);
							Class< ? >[] parameterTypes = memberbox[i].getParameterTypes();

							JSSignature annotation = memberbox[i].method().getAnnotation(JSSignature.class);
							if (annotation != null)
							{
								if (annotation.arguments().length > 0) parameterTypes = annotation.arguments();
								if (annotation.returns() != Object.class) returnTypeClz = annotation.returns();

							}
							if (scriptObject instanceof ITypedScriptObject)
							{
								if (((ITypedScriptObject)scriptObject).isDeprecated(name, parameterTypes))
								{
									makeDeprecated(method);
								}
							}
							else if (scriptObject != null && scriptObject.isDeprecated(name))
							{
								makeDeprecated(method);
							}

							ClientSupport clientType = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveSolutionClientType();
							ClientSupport csp = typesClientSupport.get(typeName);
							if ((csp != null && !csp.hasSupport(clientType)) ||
								!AnnotationManagerReflection.getInstance().hasSupportForClientType(memberbox[i].method(), scriptObjectClass, clientType,
									ClientSupport.Default))
							{
								method.setVisibility(Visibility.INTERNAL);
							}

							method.setDescription(getDoc(name, scriptObjectClass, parameterTypes)); // TODO name should be of parent.
							if (returnTypeClz != null)
							{
								method.setType(getMemberTypeName(context, name, returnTypeClz, typeName));
							}
							method.setAttribute(IMAGE_DESCRIPTOR, METHOD);
							method.setStatic(type == STATIC_METHOD);

							IParameter[] scriptParams = getParameters(name, scriptObjectClass, memberbox[i]);
							if (scriptParams != null && scriptParams.length > 0)
							{
								EList<Parameter> parameters = method.getParameters();
								for (IParameter param : scriptParams)
								{
									Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
									parameter.setName(param.getName());
									if (param.getType() != null)
									{
										Class< ? > paramType = param.getRealType();
										if (paramType != null && paramType.isArray())
										{
											Class< ? > componentType = paramType.getComponentType();
											if (param.isVarArgs())
											{
												parameter.setType(getMemberTypeName(context, name, componentType, typeName));
											}
											else if (componentType == Object.class)
											{
												parameter.setType(getTypeRef(context, ITypeNames.ARRAY));
											}
											else
											{
												parameter.setType(TypeUtil.arrayOf(getMemberTypeName(context, name, componentType, typeName)));
											}
										}
										else if (paramType != null)
										{
											parameter.setType(getMemberTypeName(context, name, paramType, typeName));
										}
										else
										{
											parameter.setType(getTypeRef(context, param.getType()));
										}
									}
									parameter.setKind(param.isVarArgs() ? ParameterKind.VARARGS : param.isOptional() ? ParameterKind.OPTIONAL
										: ParameterKind.NORMAL);
									parameters.add(parameter);
								}
							}
							else if (parameterTypes != null && parameterTypes.length > 0)
							{
								EList<Parameter> parameters = method.getParameters();
								for (Class< ? > paramClass : parameterTypes)
								{
									Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
									if (paramClass.isArray())
									{
										Class< ? > componentType = paramClass.getComponentType();
										String jsTypeName = DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(componentType);
										parameter.setType(TypeUtil.arrayOf(getTypeRef(context, jsTypeName)));
										int index = jsTypeName.lastIndexOf('.');
										parameter.setName(index == -1 ? jsTypeName : jsTypeName.substring(index + 1));
									}
									else
									{
										String jsTypeName = DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(paramClass);
										parameter.setType(getTypeRef(context, jsTypeName));
										int index = jsTypeName.lastIndexOf('.');
										parameter.setName(index == -1 ? jsTypeName : jsTypeName.substring(index + 1));
									}
									parameter.setKind(ParameterKind.NORMAL);
									parameters.add(parameter);
								}
							}
							newMembers.add(method);
						}
					}
					else
					{
						Class< ? > returnTypeClz = getReturnType(object);
						JSType returnType = null;
						if (returnTypeClz != null)
						{
							returnType = getMemberTypeName(context, name, returnTypeClz, typeName);
						}

						boolean xmlDocumentedProperty = (scriptObject instanceof ITypedScriptObject);
						String propertyName = name;
						boolean visible = true;
						if (name.toLowerCase().startsWith(ARRAY_INDEX_PROPERTY_PREFIX)) // see also MethodStoragePlace.ARRAY_INDEX_PROPERTY_PREFIX which serves the same purpose
						{
							propertyName = "[]"; // it must be resolved to an array access in this case
							if (xmlDocumentedProperty) name = "[" + name.substring(ARRAY_INDEX_PROPERTY_PREFIX.length()) + "]"; // in the doc xmls this property's name is converted to "[something]"; use that when trying to get info
							visible = false;
						}

						ImageDescriptor descriptor = IconProvider.instance().descriptor(returnTypeClz);
						if (descriptor == null)
						{
							boolean isSpecial = false;
							if (xmlDocumentedProperty)
							{
								isSpecial = ((ITypedScriptObject)scriptObject).isSpecial(name);
							}
							descriptor = (type == STATIC_FIELD) ? CONSTANT : (isSpecial ? SPECIAL_PROPERTY : PROPERTY);
						}

						boolean readOnly = false;
						if (object instanceof BeanProperty)
						{
							readOnly = AnnotationManagerReflection.getInstance().isAnnotationPresent(((BeanProperty)object).getGetter(), scriptObjectClass,
								JSReadonlyProperty.class);
						}

						Property property = createProperty(propertyName, readOnly, returnType, getDoc(name, scriptObjectClass, null), descriptor);
						if (!visible) property.setVisible(false);
						property.setStatic(type == STATIC_FIELD);

						if (object instanceof BeanProperty || (object instanceof Field && property.isStatic()))
						{
							ClientSupport clientType = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveSolutionClientType();
							ClientSupport csp = typesClientSupport.get(typeName);
							boolean visibility = (csp == null ? ClientSupport.Default : csp).hasSupport(clientType);
							if (visibility)
							{
								if (object instanceof BeanProperty)
								{
									visibility = AnnotationManagerReflection.getInstance().hasSupportForClientType(((BeanProperty)object).getGetter(),
										scriptObjectClass, clientType, ClientSupport.Default);
								}
								else if (object instanceof Field && descriptor == CONSTANT)
								{
									visibility = AnnotationManagerReflection.getInstance().hasSupportForClientType(((Field)object), clientType,
										ClientSupport.Default);
								}
							}
							if (!visibility)
							{
								property.setVisibility(Visibility.INTERNAL);
							}
						}

						if (scriptObject != null && scriptObject.isDeprecated(name))
						{
							makeDeprecated(property);
						}
						newMembers.add(property);
					}
				}
			}

			// Make sure that deprecated and mobile-hidden methods are added at the end, when multiple methods match, the non-deprecated ones should first be considered.
			Collections.sort(newMembers, new Comparator<Member>()
			{
				public int compare(Member member1, Member member2)
				{
					if (member1.isDeprecated() == member2.isDeprecated())
					{
						if (member1.getVisibility() == member2.getVisibility())
						{
							return 0;
						}
						return member1.getVisibility() == Visibility.INTERNAL ? 1 : -1;
					}

					return member1.isDeprecated() ? 1 : -1;
				}
			});

			membersList.addAll(newMembers);
		}
	}

	protected final JSType getMemberTypeName(String context, String memberName, Class< ? > memberReturnType, String objectTypeName)
	{
		int index = objectTypeName.indexOf('<');
		int index2;
		// skip plugins that return Record or Foundset, the object type shouldnt be used to generate JSFoundset<pluginname>
		if (!objectTypeName.startsWith("Plugin<") && index != -1 && (index2 = objectTypeName.indexOf('>', index)) != -1)
		{
			String config = objectTypeName.substring(index + 1, index2);

			if (memberReturnType == Record.class)
			{
				return getTypeRef(context, Record.JS_RECORD + '<' + config + '>');
			}
			if (memberReturnType == FoundSet.class)
			{
				if (memberName.equals("unrelated"))
				{
					if (config.indexOf('.') == -1)
					{
						// its really a relation, unrelate it.
						FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
						if (fs != null)
						{
							Relation relation = fs.getRelation(config);
							if (relation != null)
							{
								return getTypeRef(context, FoundSet.JS_FOUNDSET + '<' + relation.getForeignDataSource() + '>');
							}
						}
						return getTypeRef(context, FoundSet.JS_FOUNDSET);
					}
				}
				return getTypeRef(context, FoundSet.JS_FOUNDSET + '<' + config + '>');
			}
		}
		if (memberReturnType.isArray())
		{
			Class< ? > returnType = getReturnType(memberReturnType.getComponentType());
			if (returnType != null && returnType != Object.class)
			{
				JSType componentJSType = getMemberTypeName(context, memberName, returnType, objectTypeName);
				if (componentJSType != null)
				{
					return TypeUtil.arrayOf(componentJSType);
				}
			}
			return TypeUtil.arrayOf(TypeInfoModelFactory.eINSTANCE.createAnyType());
		}

		String typeName = null;
		if (prefixedTypes.containsKey(memberReturnType))
		{
			typeName = prefixedTypes.get(memberReturnType) + memberReturnType.getSimpleName();
		}
		else
		{
			typeName = DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(memberReturnType);
			// always just convert plain Object to Any so that it will map on both js and java Object
			if ("Object".equals(typeName))
			{
				return TypeInfoModelFactory.eINSTANCE.createAnyType();
			}
			else addAnonymousClassType(typeName, memberReturnType);
		}
		return getTypeRef(context, typeName);
	}

	public final void addType(String name, Class< ? > cls)
	{
		classTypes.put(name, cls);
		typesClientSupport.put(name, AnnotationManagerReflection.getInstance().getClientSupport(cls, ClientSupport.Default));
	}

	protected void addAnonymousClassType(Class< ? > cls)
	{
		addAnonymousClassType(cls.getSimpleName(), cls);
	}

	protected void addAnonymousClassType(String name, Class< ? > cls)
	{
		if (!classTypes.containsKey(name) && !scopeTypes.containsKey(name) && !BASE_TYPES.contains(name))
		{
			anonymousClassTypes.put(name, cls);
			typesClientSupport.put(name, AnnotationManagerReflection.getInstance().getClientSupport(cls, ClientSupport.Default));
		}
	}

	public final void addScopeType(String name, IScopeTypeCreator creator)
	{
		scopeTypes.put(name, creator);
		typesClientSupport.put(name, creator.getClientSupport());
	}

	/**
	 * @param context
	 * @param type
	 * @param provider
	 * @return
	 */
	protected static final Type getDataProviderType(ITypeInfoContext context, IDataProvider provider)
	{
		if (provider instanceof Column)
		{
			ColumnInfo columnInfo = ((Column)provider).getColumnInfo();
			if (columnInfo != null)
			{
				if (columnInfo.hasFlag(Column.UUID_COLUMN))
				{
					return context.getType("UUID");
				}
			}
		}
		ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, provider, com.servoy.eclipse.core.Activator.getDefault().getDesignClient());
		switch (componentFormat.dpType)
		{
			case IColumnTypes.DATETIME :
				return context.getType("Date");

			case IColumnTypes.INTEGER :
			case IColumnTypes.NUMBER :
				return context.getType("Number");

			case IColumnTypes.TEXT :
				return context.getType("String");

			default :
				// Return the Any type because Media can be anything.
				// should be in sync with TypeProvider.DataprovidersScopeCreator
				return context.getType("Any");
		}
	}


	protected interface IScopeTypeCreator
	{
		Type createType(String context, String fullTypeName);

		ClientSupport getClientSupport();

		void flush();
	}

	@SuppressWarnings("deprecation")
	public IParameter[] getParameters(String key, Class< ? > scriptObjectClass, MemberBox member)
	{
		if (scriptObjectClass == null) return null;
		IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
		IParameter[] parameters = null;
		String[] parameterNames = null;
		if (scriptObject instanceof ITypedScriptObject)
		{
			parameters = ((ITypedScriptObject)scriptObject).getParameters(key, member.getParameterTypes());
		}
		else if (scriptObject != null)
		{
			parameterNames = scriptObject.getParameterNames(key);
		}

		if (parameterNames != null && parameters == null)
		{
			// so this is old type docs - not xml based; can have optional args.
			int memberParamLength = member.getParameterTypes().length; // parameters used in script (editor)
			if (memberParamLength < parameterNames.length)
			{
				boolean removeOptional;
				// if parameterNames bigger then the members parameter types and it is not a vararg, just get the first names.
				if (memberParamLength == 1 && member.getParameterTypes()[0].isArray())
				{
					parameters = new IParameter[parameterNames.length];
					removeOptional = false;
				}
				else
				{
					parameters = new IParameter[memberParamLength];
					removeOptional = true;
				}
				for (int i = 0; i < parameters.length; i++)
				{
					String name = parameterNames[i];
					boolean vararg = false;
					boolean optional = name.startsWith("[") && name.endsWith("]");
					if (optional && removeOptional)
					{
						optional = false;
						name = name.substring(1, name.length() - 1);
						if (name.startsWith("..."))
						{
							vararg = true;
							name = name.substring(3);
						}
					}
					else if (name.startsWith("[..."))
					{
						vararg = true;
					}
					else if (optional)
					{
						name = name.substring(1, name.length() - 1);
					}
					String typePrefix = null;
					Class< ? > realType = null;
					if (removeOptional && i < memberParamLength)
					{
						realType = member.getParameterTypes()[i];
						typePrefix = getTypePrefix(realType);
					}
					parameters[i] = new ScriptParameter(name, typePrefix, realType, optional, vararg);
				}
			}
			else if (memberParamLength == parameterNames.length)
			{
				parameters = new IParameter[memberParamLength];
				for (int i = 0; i < memberParamLength; i++)
				{
					Class< ? > paramClass = member.getParameterTypes()[i];
					String name = null;
					String typePrefix = null;
					boolean optional = false;

					name = parameterNames[i];
					if (name.startsWith("[") && name.endsWith("]"))
					{
						name = name.substring(1, name.length() - 1);
						optional = true;
					}
					else
					{
						typePrefix = getTypePrefix(paramClass);
					}

					parameters[i] = new ScriptParameter(name, typePrefix, paramClass, optional, false);
				}
			}
		}
		return parameters;
	}

	/**
	 * Gets the prefix for a class of an array with a prefixed type elements.
	 * If given class does not use a prefix, it returns null.
	 */
	private String getTypePrefix(Class< ? > realType)
	{
		return prefixedTypes.get((realType.isArray()) ? realType.getComponentType() : realType);
	}

	private final ConcurrentMap<String, String> buckets = new ConcurrentHashMap<String, String>();

	/**
	 *
	 */
	protected void flushCache()
	{
		// TODO, maybe only flush solution buckets and SCOPE_TABLES only when a table change?
		for (String bucket : buckets.keySet())
		{
			clear(bucket);
		}
		relationCache.clear();
		ValueCollectionProvider.clear();
	}

//	final Set<String> staticTypes = Collections.synchronizedSet(new TreeSet<String>());
//	final Set<String> javaTypes = Collections.synchronizedSet(new TreeSet<String>());
//	final ConcurrentHashMap<String, Set<String>> dynamicTypes = new ConcurrentHashMap<String, Set<String>>();

	@Override
	protected Type addType(String bucket, Type type)
	{
		if (bucket != null && !bucket.equals(""))
		{
			buckets.put(bucket, bucket);
			type.setMetaType(ServoyDynamicMetaType.META_TYPE);
//			Set<String> set = dynamicTypes.get(bucket);
//			if (set == null)
//			{
//				set = Collections.synchronizedSet(new TreeSet<String>());
//				Set<String> realValue = dynamicTypes.putIfAbsent(bucket, set);
//				if (realValue != null) set = realValue;
//			}
//			set.add(type.getName());
		}
		else if (type.getMetaType() == null || type.getMetaType() == DefaultMetaType.DEFAULT)
		{
			type.setMetaType(staticMetaType);
//			staticTypes.add(type.getName());
		}
//		else
//		{
//			javaTypes.add(type.getName());
//		}

		ClientSupport clientType = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveSolutionClientType();
		if (clientType == ClientSupport.mc && type.getMetaType() != null && type.getMetaType() == javaMetaType)
		{
			type.setVisible(false);
		}
		else
		{
			String properTypeName = type.getName();
			int index = properTypeName.indexOf("<");
			if (index != -1)
			{
				properTypeName = properTypeName.substring(0, index);
			}
			ClientSupport csp = typesClientSupport.get(properTypeName);
			if (!(csp == null ? ClientSupport.Default : csp).hasSupport(clientType)) type.setVisible(false);
		}
		return super.addType(bucket, type);
	}

//	private void log()
//	{
//		StringBuilder sb = new StringBuilder();
//		sb.append("\njava types: ");
//		sb.append(javaTypes.size());
//		sb.append("\n\n");
//		for (String type : javaTypes)
//		{
//			sb.append(type);
//			sb.append("\n");
//		}
//		sb.append("\nstatic types: ");
//		sb.append(staticTypes.size());
//		sb.append("\n\n");
//		for (String type : staticTypes)
//		{
//			sb.append(type);
//			sb.append("\n");
//		}
//		sb.append("dynamic types: ");
//		sb.append(dynamicTypes.size());
//		sb.append("\n");
//		int size = 0;
//		for (Entry<String, Set<String>> entry : dynamicTypes.entrySet())
//		{
//			sb.append("\nbucket: ");
//			sb.append(entry.getKey());
//			sb.append(": ");
//			sb.append(entry.getValue().size());
//			sb.append("\n");
//			size += entry.getValue().size();
//			for (String type : entry.getValue())
//			{
//				sb.append(type);
//				sb.append("\n");
//			}
//		}
//		sb.append("\nTotal Dynamic Size: ");
//		sb.append(size);
//		Debug.error(sb.toString());
//	}

	public Property createProperty(String context, String name, boolean readonly, String typeName, String description, ImageDescriptor image)
	{
		return createProperty(context, name, readonly, typeName, description, image, null);
	}

	public Property createProperty(String context, String name, boolean readonly, String typeName, String description, ImageDescriptor image, Object resource)
	{
		SimpleType type = null;
		if (typeName != null)
		{
			type = getTypeRef(context, typeName);
		}
		return createProperty(name, readonly, type, description, image, resource);
	}


	public Property createProperty(String context, String name, boolean readonly, String typeName, ImageDescriptor image)
	{
		SimpleType type = null;
		if (typeName != null)
		{
			type = getTypeRef(context, typeName);
		}
		return createProperty(name, readonly, type, null, image);
	}

	public static Property createProperty(String name, boolean readonly, JSType type, String description, ImageDescriptor image)
	{
		return createProperty(name, readonly, type, description, image, null);
	}

	public static Property createProperty(String name, boolean readonly, JSType type, String description, ImageDescriptor image, Object resource)
	{
		return createProperty(name, readonly, type, description, image, resource, null);
	}

	public static Property createProperty(String name, boolean readonly, JSType type, String description, ImageDescriptor image, Object resource,
		String deprecated)
	{
		Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
		property.setName(name);
		property.setReadOnly(readonly);

		String propertyDescription = null;
		if (deprecated != null)
		{
			deprecated = "<b>Deprecated:</b>" + deprecated;
			propertyDescription = description != null ? description + "<br>" + deprecated : deprecated;
			property.setDeprecated(true);
		}
		else propertyDescription = description;

		if (propertyDescription != null)
		{
			property.setDescription(propertyDescription);
		}
		if (type != null)
		{
			property.setType(type);
		}
		if (image != null)
		{
			property.setAttribute(IMAGE_DESCRIPTOR, image);
		}
		else if (type instanceof SimpleType && ((SimpleType)type).getTarget().getAttribute(IMAGE_DESCRIPTOR) != null)
		{
			property.setAttribute(IMAGE_DESCRIPTOR, ((SimpleType)type).getTarget().getAttribute(IMAGE_DESCRIPTOR));
		}
		if (resource != null)
		{
			property.setAttribute(RESOURCE, resource);
		}
		return property;
	}

	public static String getParsedComment(String comment)
	{
		int currPos = 0;
		int endPos = comment.length();
		boolean newLine = true;
		StringBuilder sb = new StringBuilder(comment.length());
		outer : while (currPos < endPos)
		{
			char ch;
			if (newLine)
			{
				do
				{
					ch = comment.charAt(currPos++);
					if (currPos >= endPos) break outer;
					if (ch == '\n' || ch == '\r') break;
				}
				while (Character.isWhitespace(ch) || ch == '*' || ch == '/');
			}
			else
			{
				ch = comment.charAt(currPos++);
			}
			newLine = ch == '\n' || ch == '\r';

			if (newLine)
			{
				if (sb.length() != 0) sb.append("<br/>\n");
			}
			else
			{
				sb.append(ch);
			}
		}

		JavaDoc2HTMLTextReader reader = new JavaDoc2HTMLTextReader(new StringReader(sb.toString()));
		try
		{
			return reader.getString();
		}
		catch (IOException e)
		{
			return comment;
		}
	}

	private static final ConcurrentMap<MethodSignature, String> docCache = new ConcurrentHashMap<MethodSignature, String>(64, 0.9f, 16);

	/**
	 * @param key
	 * @param scriptObject
	 * @param name
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static String getDoc(String name, Class< ? > scriptObjectClass, Class< ? >[] parameterTypes)
	{
		if (scriptObjectClass == null) return null;

		MethodSignature cacheKey = new MethodSignature(scriptObjectClass, name, parameterTypes);
		String doc = docCache.get(cacheKey);
		if (doc == null)
		{
			doc = name;
			IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
			if (scriptObject != null)
			{
				StringBuilder docBuilder = new StringBuilder(200);
				String sampleDoc = null;
				IParameter[] parameters = null;
				String returnText = null;
				ClientSupport clientType = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveSolutionClientType();

				if (scriptObject instanceof ITypedScriptObject)
				{
					if (((ITypedScriptObject)scriptObject).isDeprecated(name, parameterTypes))
					{
						String deprecatedText = ((ITypedScriptObject)scriptObject).getDeprecatedText(name, parameterTypes);
						if (deprecatedText != null)
						{
							docBuilder.append("<br/><b>@deprecated</b>&nbsp;");
							docBuilder.append(deprecatedText);
							docBuilder.append("<br/>");
						}
						else
						{
							docBuilder.append("<br/><b>@deprecated</b><br/>");
						}
					}
					String toolTip = ((ITypedScriptObject)scriptObject).getToolTip(name, parameterTypes, clientType);
					if (toolTip != null && toolTip.trim().length() != 0)
					{
						docBuilder.append("<br/>");
						docBuilder.append(toolTip);
						docBuilder.append("<br/>");
					}
					sampleDoc = ((ITypedScriptObject)scriptObject).getSample(name, parameterTypes, clientType);
					if (parameterTypes != null)
					{
						parameters = ((ITypedScriptObject)scriptObject).getParameters(name, parameterTypes);
					}


					Class< ? > returnedType = ((ITypedScriptObject)scriptObject).getReturnedType(name, parameterTypes);
					String returnDescription = ((ITypedScriptObject)scriptObject).getReturnDescription(name, parameterTypes);
					if ((returnedType != Void.class && returnedType != void.class && returnedType != null) || returnDescription != null)
					{
						returnText = "<b>@return</b> ";
						if (returnedType != null) returnText += DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(returnedType) + ' ';
						if (returnDescription != null) returnText += returnDescription;
					}
				}
				else
				{
					if (scriptObject.isDeprecated(name))
					{
						docBuilder.append("<br/><b>@deprecated</b><br/>");
					}
					String toolTip = scriptObject.getToolTip(name);
					if (toolTip != null && toolTip.trim().length() != 0)
					{
						docBuilder.append("<br/>");
						docBuilder.append(toolTip);
						docBuilder.append("<br/>");
					}
					sampleDoc = scriptObject.getSample(name);
				}

				if (sampleDoc != null && sampleDoc.trim().length() != 0)
				{
					docBuilder.append("<pre>");
					docBuilder.append(HtmlUtils.escapeMarkup(sampleDoc));
					docBuilder.append("</pre><br/>");
				}
				if (docBuilder.length() > 0)
				{
					if (parameters != null)
					{
						StringBuilder sb = new StringBuilder(parameters.length * 30);
						for (IParameter parameter : parameters)
						{
							sb.append("<b>@param</b> ");
							if (parameter.getType() != null)
							{
								sb.append("{");
								sb.append(parameter.getType());
								sb.append("} ");
							}
							sb.append(parameter.getName());
							if (parameter.getDescription() != null)
							{
								sb.append(" ");
								sb.append(parameter.getDescription());
							}
							sb.append("<br/>");
						}
						docBuilder.append(sb);
					}
					if (returnText != null)
					{
						docBuilder.append("<br/>");
						docBuilder.append(returnText);
					}
					doc = Utils.stringReplace(docBuilder.toString(), "%%prefix%%", "");
					doc = Utils.stringReplace(doc, "%%elementName%%", "elements.elem");
				}
			}
			docCache.putIfAbsent(cacheKey, doc);
		}
		return doc;
	}

	public static Class< ? > getReturnType(Object object)
	{
		Class< ? > returnType = null;
		if (object instanceof NativeJavaMethod)
		{
			NativeJavaMethod method = (NativeJavaMethod)object;
			MemberBox[] methods = method.getMethods();
			if (methods != null && methods.length > 0)
			{
				returnType = methods[0].getReturnType();
			}
		}
		else if (object instanceof MemberBox)
		{
			returnType = ((MemberBox)object).getReturnType();
		}
		else if (object instanceof BeanProperty)
		{
			returnType = ((BeanProperty)object).getGetter().getReturnType();
		}
		else if (object instanceof Field)
		{
			returnType = ((Field)object).getType();
		}
		return getReturnType(returnType);
	}

	/**
	 * @param returnType
	 */
	private static Class< ? > getReturnType(Class< ? > returnType)
	{
		if (returnType == null) return null;
		if (returnType == Object.class || returnType.isArray()) return returnType;
		if (!returnType.isAssignableFrom(Void.class) && !returnType.isAssignableFrom(void.class))
		{
			if (returnType.isAssignableFrom(Record.class))
			{
				return Record.class;
			}
			else if (returnType.isAssignableFrom(JSDataSet.class))
			{
				return JSDataSet.class;
			}
			else if (returnType.isAssignableFrom(FoundSet.class))
			{
				return FoundSet.class;
			}
			else if (returnType.isPrimitive() || Number.class.isAssignableFrom(returnType))
			{
				if (returnType.isAssignableFrom(boolean.class)) return Boolean.class;
				if (returnType.isAssignableFrom(byte.class) || returnType == Byte.class)
				{
					return byte.class;
				}
				return Number.class;
			}
			else if (returnType == Object.class || returnType == String.class || Date.class.isAssignableFrom(returnType))
			{
				return returnType;
			}
			JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(returnType, null);
			if (javaMembers != null)
			{
				return returnType;
			}
		}
		return null;
	}

	private final static class MethodSignature
	{
		private final Class< ? > scriptObjectClass;
		private final String name;
		private final Class< ? >[] parameterTypes;

		/**
		 * @param scriptObjectClass
		 * @param name
		 * @param parameterTypes
		 */
		public MethodSignature(Class< ? > scriptObjectClass, String name, Class< ? >[] parameterTypes)
		{
			this.scriptObjectClass = scriptObjectClass;
			this.name = name;
			this.parameterTypes = parameterTypes;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + Arrays.hashCode(parameterTypes);
			result = prime * result + ((scriptObjectClass == null) ? 0 : scriptObjectClass.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			MethodSignature other = (MethodSignature)obj;
			if (name == null)
			{
				if (other.name != null) return false;
			}
			else if (!name.equals(other.name)) return false;
			if (!Arrays.equals(parameterTypes, other.parameterTypes)) return false;
			if (scriptObjectClass == null)
			{
				if (other.scriptObjectClass != null) return false;
			}
			else if (!scriptObjectClass.equals(other.scriptObjectClass)) return false;
			return true;
		}
	}


	/**
	 * @param recordType
	 * @return
	 */
	private static Type getRecordType(String type)
	{
		String recordType = type;
		if (recordType.startsWith("{") && recordType.endsWith("}"))
		{
			recordType = recordType.substring(1, recordType.length() - 1);
		}
		Type t = TypeInfoModelFactory.eINSTANCE.createType();
		t.setKind(TypeKind.JAVA);

		EList<Member> members = t.getMembers();
		StringTokenizer st = new StringTokenizer(recordType, ",");
		while (st.hasMoreTokens())
		{
			String typeName = "Object";
			String propertyName = st.nextToken();
			int typeSeparator = propertyName.indexOf(':');
			if (typeSeparator != -1)
			{
				typeName = propertyName.substring(typeSeparator + 1);
				propertyName = propertyName.substring(0, typeSeparator);
			}

			Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
			property.setName(propertyName.trim());
			property.setType(TypeUtil.ref(typeName.trim()));
			members.add(property);
		}
		return t;
	}


	private class ScopesScopeCreator implements IScopeTypeCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(String context, String fullTypeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, SCOPES);
			type.setName(fullTypeName);

			FlattenedSolution fs;
			if ("Scopes".equals(fullTypeName) || (fs = ElementResolver.getFlattenedSolution(context)) == null)
			{
				// special array lookup property so that scopes[xxx]. does code complete.
				Property arrayProp = createProperty(context, "[]", true, "Scope", PROPERTY);
				arrayProp.setVisible(false);
				type.getMembers().add(arrayProp);
				// quickly add this one to the static types. context.markInvariant(type);
				return addType(null, type);
			}
			else
			{
				type.setSuperType(getType(context, "Scopes"));
				EList<Member> members = type.getMembers();

				for (Pair<String, IRootObject> scope : fs.getScopes())
				{
					IFile resource = ResourcesPlugin.getWorkspace().getRoot().getFile(
						Path.fromPortableString(scope.getRight().getName() + '/' + scope.getLeft() + ".js"));
					Property scopeProperty = createProperty(scope.getLeft(), true,
						getTypeRef(context, "Scope<" + scope.getRight().getRootObject().getName() + '/' + scope.getLeft() + '>'),
						"Global scope " + scope.getLeft() + " defined in solution " + scope.getRight().getRootObject().getName(), SCOPES, resource);
//					scopeProperty.setAttribute(LAZY_VALUECOLLECTION, persist); // currently not needed, solution name from config is used
					members.add(scopeProperty);
				}
			}
			return type;
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.All;
		}

		@Override
		public void flush()
		{
		}
	}

	private class ScopeScopeCreator implements IScopeTypeCreator
	{
		private Type docGlobalsType = null;

		public Type createType(String context, String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);

			if (typeName.endsWith(">"))
			{
				// Scope<solutionname/scopeName>
				FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
				String config = typeName.substring(typeName.indexOf('<') + 1, typeName.length() - 1);
				String[] split = config.split("/");
				String solutionName = split[0];
				String scopeName = split[1];

				EList<Member> members = type.getMembers();

				if (docGlobalsType == null) docGlobalsType = TypeCreator.this.createType(context, "globals", Globals.class);
				members.add(TypeCreator.clone(getMember("allmethods", docGlobalsType), null));
				members.add(TypeCreator.clone(getMember("allvariables", docGlobalsType), null));
				members.add(TypeCreator.clone(getMember("allrelations", docGlobalsType), null));

				if (fs != null && (fs.getMainSolutionMetaData().getName().equals(solutionName) || fs.hasModule(solutionName)))
				{
					type.setSuperType(getType(context, "Relations<" + config + '>')); // Relations<solutionName/scopeName>
				}
			}
			else
			{
				return addType(null, type);
			}

			return type;
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.All;
		}

		@Override
		public void flush()
		{
			docGlobalsType = null;
		}

	}

	private class FormsScopeCreator implements IScopeTypeCreator
	{
		private final ConcurrentMap<String, String> descriptions = new ConcurrentHashMap<String, String>();

		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(String context, String fullTypeName)
		{
			Type type = null;
			if ("Forms".equals(fullTypeName))
			{
				type = TypeCreator.this.createType(context, "Forms", Forms.class);
				type.setAttribute(IMAGE_DESCRIPTOR, FORMS);
				// quickly add this one to the static types.
				return addType(null, type);
			}
			else
			{
				FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
				if (fs == null) return getType(context, "Forms");
				type = TypeInfoModelFactory.eINSTANCE.createType();
				type.setSuperType(getType(context, "Forms"));
				type.setName("Forms<" + fs.getMainSolutionMetaData().getName() + '>');
				type.setKind(TypeKind.JAVA);
				type.setAttribute(IMAGE_DESCRIPTOR, FORMS);
				EList<Member> members = type.getMembers();
				Iterator<Form> forms = fs.getForms(false);
				while (forms.hasNext())
				{
					Form form = forms.next();
					Property formProperty = createProperty(form.getName(), true, getTypeRef(context, "RuntimeForm<" + form.getName() + '>'),
						getDescription(form.getDataSource()), getImageDescriptorForFormEncapsulation(form.getEncapsulation()), null, form.getDeprecated());
					formProperty.setAttribute(LAZY_VALUECOLLECTION, form);
					if (PersistEncapsulation.isHideInScriptingModuleScope(form, fs))
					{
						formProperty.setVisible(false);
					}
					members.add(formProperty);
				}
			}
			return type;
		}

		/**
		 * @param dataSource
		 * @return
		 */
		private String getDescription(String ds)
		{
			String datasource = ds;
			if (datasource == null) datasource = "<no datasource>";
			String description = descriptions.get(datasource);
			if (description == null)
			{
				description = "Form based on datasource: " + datasource;
				descriptions.putIfAbsent(datasource, description);
			}
			return description;
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.All;
		}

		@Override
		public void flush()
		{
		}

	}

	private Member createOverrideMember(Member member, String context, String config)
	{
		JSType memberType = member.getType();
		if (memberType != null)
		{
			if (memberType.getName().equals("Array<" + Record.JS_RECORD + '>'))
			{
				return TypeCreator.clone(member, TypeUtil.arrayOf(Record.JS_RECORD + '<' + config + '>'));
			}
			if (memberType.getName().equals(Record.JS_RECORD) || QUERY_BUILDER_CLASSES.containsKey(memberType.getName()) ||
				memberType.getName().equals(FoundSet.JS_FOUNDSET) || memberType.getName().equals(DBDataSourceServer.class.getSimpleName()))
			{
				return TypeCreator.clone(member, getTypeRef(context, memberType.getName() + '<' + config + '>'));
			}
		}

		return null;
	}

	private class FoundSetCreator implements IScopeTypeCreator
	{
		private Type cachedSuperTypeTemplateTypeForFoundSet = null;
		private Type cachedSuperTypeTemplateTypeForRelatedFoundSet = null;

		public Type createType(String context, String fullTypeName)
		{
			if (fullTypeName.equals(FoundSet.JS_FOUNDSET))
			{
				// quickly add this one to the static types.
				return addType(null, createBaseType(context, fullTypeName, FoundSet.class));
			}

			FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
			String config = fullTypeName.substring(fullTypeName.indexOf('<') + 1, fullTypeName.length() - 1);
			EList<Member> members;
			if (fs != null && fs.getRelation(config) != null)
			{
				// related foundset
				if (cachedSuperTypeTemplateTypeForRelatedFoundSet == null)
				{
					cachedSuperTypeTemplateTypeForRelatedFoundSet = createBaseType(context, FoundSet.JS_FOUNDSET, RelatedFoundSet.class);
				}
				members = cachedSuperTypeTemplateTypeForRelatedFoundSet.getMembers();
			}
			else
			{
				if (cachedSuperTypeTemplateTypeForFoundSet == null)
				{
					cachedSuperTypeTemplateTypeForFoundSet = createBaseType(context, FoundSet.JS_FOUNDSET, FoundSet.class);
				}
				members = cachedSuperTypeTemplateTypeForFoundSet.getMembers();
			}

			List<Member> overwrittenMembers = new ArrayList<Member>();
			for (Member member : members)
			{
				Member overridden = null;
				if (member.getVisibility() == Visibility.INTERNAL)
				{
					if (fs != null && fs.getRelation(config) != null)
					{
						// the special internal once (like clear() of related) should be also added
						// because they should override the normal super JSFoundSet clear
						overridden = TypeCreator.clone(member, member.getType());
						overridden.setAttribute(HIDDEN_IN_RELATED, Boolean.TRUE);
					}
				}
				else
				{
					String memberConfig = config;
					if (fs != null && member.getType() != null && member.getType().getName().equals(FoundSet.JS_FOUNDSET) &&
						member.getName().equals("unrelate"))
					{
						// its really a relation, unrelate it.
						Relation relation = fs.getRelation(config);
						if (relation != null)
						{
							memberConfig = relation.getForeignDataSource();
						}
					}

					overridden = createOverrideMember(member, context, memberConfig);
				}

				if (overridden != null)
				{
					overwrittenMembers.add(overridden);
				}
			}
			return getCombinedTypeWithRelationsAndDataproviders(fs, context, fullTypeName, config, overwrittenMembers, getType(context, FoundSet.JS_FOUNDSET),
				FOUNDSET_IMAGE, true);
		}

		/**
		 * @param context
		 * @param fullTypeName
		 * @param foudsetClass
		 * @return
		 */
		private Type createBaseType(String context, String fullTypeName, Class< ? > foundsetClass)
		{
			Type type = TypeCreator.this.createType(context, fullTypeName, foundsetClass);
			//type.setAttribute(IMAGE_DESCRIPTOR, FOUNDSET_IMAGE);

			Property maxRecordIndex = TypeInfoModelFactory.eINSTANCE.createProperty();
			maxRecordIndex.setName("maxRecordIndex");
			type.getMembers().add(makeDeprecated(maxRecordIndex));

			Property selectedIndex = TypeInfoModelFactory.eINSTANCE.createProperty();
			selectedIndex.setName("selectedIndex");
			type.getMembers().add(makeDeprecated(selectedIndex));
			return type;
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.All;
		}

		@Override
		public void flush()
		{
			cachedSuperTypeTemplateTypeForFoundSet = null;
			cachedSuperTypeTemplateTypeForRelatedFoundSet = null;
		}
	}

	private class JSDataSetCreator implements IScopeTypeCreator
	{
		public Type createType(String context, String fullTypeName)
		{
			Type type = getType(context, "JSDataSet");
			int index = fullTypeName.indexOf('<');
			if (index != -1 && fullTypeName.endsWith(">"))
			{
				String recordType = fullTypeName.substring(index + 1, fullTypeName.length() - 1);
				Type t = getRecordType(recordType);
				t.setName(fullTypeName);
				t.setSuperType(type);
				type = t;
			}
			return type;
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.wc_sc;
		}

		@Override
		public void flush()
		{
		}
	}

	private class RecordCreator extends FoundSetCreator
	{
		private Type cachedSuperTypeTemplateType;

		@Override
		public Type createType(String context, String fullTypeName)
		{
			if (fullTypeName.equals(Record.JS_RECORD))
			{
				Type type = TypeCreator.this.createType(context, fullTypeName, Record.class);
				ImageDescriptor desc = IconProvider.instance().descriptor(Record.class);
				type.setAttribute(IMAGE_DESCRIPTOR, desc);
				// quickly add this one to the static types.
				return addType(null, type);
			}

			String config = fullTypeName.substring(fullTypeName.indexOf('<') + 1, fullTypeName.length() - 1);
			if (cachedSuperTypeTemplateType == null)
			{
				cachedSuperTypeTemplateType = TypeCreator.this.createType(context, Record.JS_RECORD, Record.class);
			}
			EList<Member> members = cachedSuperTypeTemplateType.getMembers();
			List<Member> overwrittenMembers = new ArrayList<Member>();
			for (Member member : members)
			{
				Member overridden = createOverrideMember(member, context, config);
				if (overridden != null)
				{
					overwrittenMembers.add(overridden);
				}
			}
			return getCombinedTypeWithRelationsAndDataproviders(ElementResolver.getFlattenedSolution(context), context, fullTypeName, config,
				overwrittenMembers, getType(context, Record.JS_RECORD), IconProvider.instance().descriptor(Record.class), true);
		}

		@Override
		public void flush()
		{
			super.flush();
			cachedSuperTypeTemplateType = null;
		}

	}

	private class PluginsScopeCreator implements IScopeTypeCreator
	{
		private final Map<String, Image> images = new ConcurrentHashMap<String, Image>();

		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(String context, String fullTypeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(fullTypeName);
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, PLUGINS);

			EList<Member> members = type.getMembers();
			members.add(createProperty("allnames", true, TypeUtil.arrayOf("String"), "All plugin names as an array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "length", true, "Number", "Number of plugins", PROPERTY));

			IPluginManager pluginManager = com.servoy.eclipse.core.Activator.getDefault().getDesignClient().getPluginManager();
			List<IClientPlugin> clientPlugins = pluginManager.getPlugins(IClientPlugin.class);
			for (IClientPlugin clientPlugin : clientPlugins)
			{
				IScriptable scriptObject = null;
				try
				{
					java.lang.reflect.Method method = clientPlugin.getClass().getMethod("getScriptObject", (Class[])null);
					if (method != null)
					{
						scriptObject = (IScriptable)method.invoke(clientPlugin, (Object[])null);
					}
				}
				catch (Throwable t)
				{
					Debug.error("Could not get scriptobject from plugin " + clientPlugin.getName());
				}
				if (scriptObject != null)
				{
					ScriptObjectRegistry.registerScriptObjectForClass(scriptObject.getClass(), scriptObject);
					Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
					property.setName(clientPlugin.getName());
					property.setReadOnly(true);
					addAnonymousClassType("Plugin<" + clientPlugin.getName() + '>', scriptObject.getClass());
					property.setType(getTypeRef(context, "Plugin<" + clientPlugin.getName() + '>'));

					if (!AnnotationManagerReflection.getInstance().hasSupportForClientType(scriptObject.getClass(),
						ServoyModelManager.getServoyModelManager().getServoyModel().getActiveSolutionClientType(), ClientSupport.Default))
					{
						property.setVisibility(Visibility.INTERNAL);
					}

					if (clientPlugin.getName().equals("window"))
					{
						members.add(makeDeprecated(createProperty("kioskmode", true, getTypeRef(context, "Plugin<" + clientPlugin.getName() + '>'),
							"Window plugin", null)));
						members.add(makeDeprecated(createProperty("popupmenu", true, getTypeRef(context, "Plugin<" + clientPlugin.getName() + '>'),
							"Window plugin", null)));
						members.add(makeDeprecated(createProperty("menubar", true, getTypeRef(context, "Plugin<" + clientPlugin.getName() + '>'),
							"Window plugin", null)));
						members.add(makeDeprecated(createProperty("it2be_menubar", true, getTypeRef(context, "Plugin<" + clientPlugin.getName() + '>'),
							"Window plugin", null)));
					}

					Image clientImage = null;
					Icon icon = clientPlugin.getImage();
					if (icon != null)
					{
						clientImage = images.get(clientPlugin.getName());
						if (clientImage == null)
						{
							clientImage = UIUtils.getSWTImageFromSwingIcon(icon, Display.getDefault(), 16, 16);
						}
						if (clientImage != null)
						{
							com.servoy.eclipse.debug.Activator.getDefault().registerImage(clientImage);
							images.put(clientPlugin.getName(), clientImage);
						}
					}
					if (clientImage == null)
					{
						property.setAttribute(IMAGE_DESCRIPTOR, PLUGIN_DEFAULT);
					}
					else
					{
						property.setAttribute(IMAGE_DESCRIPTOR, ImageDescriptor.createFromImage(clientImage));
					}

					members.add(property);
				}
			}
			ClientSupport clientSupport = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveSolutionClientType();
			if (clientSupport != null && clientSupport.supports(ClientSupport.ng))
			{
				WebComponentSpecification[] serviceSpecifications = WebServiceSpecProvider.getInstance().getWebServiceSpecifications();
				for (WebComponentSpecification spec : serviceSpecifications)
				{
					if (spec.getApiFunctions().size() != 0 || spec.getAllPropertiesNames().size() != 0)
					{
						Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
						property.setName(spec.getName());
						property.setReadOnly(true);
						wcServices.put("WebService<" + spec.getName() + '>', spec);
						property.setType(getTypeRef(context, "WebService<" + spec.getName() + '>'));
						members.add(property);
					}
				}
			}

			// quickly add this one to the static types.
			return addType(null, type);
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.All;
		}

		@Override
		public void flush()
		{
		}
	}

	private class FormScopeCreator implements IScopeTypeCreator
	{
		public Type createType(String context, String typeName)
		{
			if (typeName.equals("Form") || typeName.equals("RuntimeForm"))
			{
				Type type = TypeCreator.this.createType(context, "RuntimeForm", com.servoy.j2db.documentation.scripting.docs.Form.class);
				type.setKind(TypeKind.JAVA);
				Member superMember = getMember("_super", type);
				superMember.setVisible(false);

				//type.setAttribute(IMAGE_DESCRIPTOR, FORM_IMAGE);
				// quickly add this one to the static types.
				return addType(null, type);
			}

			FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
			if (fs == null) return null;
			String config = typeName.substring(typeName.indexOf('<') + 1, typeName.length() - 1);
			Form form = fs.getForm(config);
			if (form == null) return null;
			Form formToUse = fs.getFlattenedForm(form);
			Type baseType = findType(context, "RuntimeForm");
			Type superForm = baseType;
			Form extendsForm = null;
			if (form.getExtendsID() > 0)
			{
				extendsForm = fs.getForm(form.getExtendsID());
				if (extendsForm != null) superForm = getType(context, "RuntimeForm<" + extendsForm.getName() + '>');
			}

			String ds = formToUse.getDataSource();
			List<Member> overwrittenMembers = new ArrayList<Member>();

			if (ds != null || PersistEncapsulation.hideFoundset(formToUse))
			{
				String foundsetType = FoundSet.JS_FOUNDSET;
				if (ds != null) foundsetType += '<' + ds + '>';
				Member clone = TypeCreator.clone(getMember("foundset", baseType), getTypeRef(context, foundsetType));
				overwrittenMembers.add(clone);
				clone.setVisible(!PersistEncapsulation.hideFoundset(formToUse));
			}
			if (PersistEncapsulation.hideController(formToUse))
			{
				Member clone = TypeCreator.clone(getMember("controller", baseType), null);
				overwrittenMembers.add(clone);
				clone.setVisible(false);
			}
			if (PersistEncapsulation.hideDataproviders(formToUse))
			{
				Member clone = TypeCreator.clone(getMember("alldataproviders", baseType), TypeUtil.arrayOf("String"));
				overwrittenMembers.add(clone);
				clone.setVisible(false);

				clone = TypeCreator.clone(getMember("allrelations", baseType), TypeUtil.arrayOf("String"));
				overwrittenMembers.add(clone);
				clone.setVisible(false);
			}

			// currently _super is hardcoded in ElementResolver for when in the form's JS... hide it in other places
			Member clone = TypeCreator.clone(getMember("_super", baseType), null);
			clone.setType(null);
			if (extendsForm != null)
			{
				clone.setAttribute(TypeCreator.LAZY_VALUECOLLECTION, extendsForm);
				clone.setAttribute(ValueCollectionProvider.SUPER_SCOPE, Boolean.TRUE);
			}
			clone.setVisible(false);
			overwrittenMembers.add(clone);

			clone = TypeCreator.clone(getMember("elements", baseType), getTypeRef(context, "Elements<" + config + '>'));
			overwrittenMembers.add(clone);
			clone.setVisible(!PersistEncapsulation.hideElements(formToUse));

			Type type = getCombinedTypeWithRelationsAndDataproviders(fs, context, typeName, ds, overwrittenMembers, superForm,
				getImageDescriptorForFormEncapsulation(form.getEncapsulation()), !PersistEncapsulation.hideDataproviders(formToUse));
			if (type != null) type.setAttribute(LAZY_VALUECOLLECTION, form);

			return type;
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.All;
		}

		@Override
		public void flush()
		{
		}
	}

	private final static Map<String, Class< ? >> QUERY_BUILDER_CLASSES = new ConcurrentHashMap<String, Class< ? >>();
	static
	{
		addClass(QBAggregate.class);
		addClass(QBColumn.class);
		addClass(QBColumns.class);
		addClass(QBCondition.class);
		addClass(QBFactory.class);
		addClass(QBFunction.class);
		addClass(QBGroupBy.class);
		addClass(QBJoin.class);
		addClass(QBJoins.class);
		addClass(QBLogicalCondition.class);
		addClass(QBWhereCondition.class);
		addClass(QBResult.class);
		addClass(QBSelect.class);
		addClass(QBSort.class);
		addClass(QBSorts.class);
		addClass(QBPart.class);
		addClass(QBTableClause.class);
		addClass(QBParameter.class);
		addClass(QBParameters.class);
		addClass(QBFunctions.class);
	}

	private static void addClass(Class< ? > clazz)
	{
		QUERY_BUILDER_CLASSES.put(clazz.getSimpleName(), clazz);
	}

	private class QueryBuilderCreator implements IScopeTypeCreator
	{
		private Type cachedSuperTypeTemplateType = null;

		public Type createType(String context, String fullTypeName)
		{
			int indexOf = fullTypeName.indexOf('<');
			if (indexOf == -1)
			{
				// quickly add this one to the static types.
				return addType(null, createBaseType(context, fullTypeName));
			}

			String config = fullTypeName.substring(indexOf + 1, fullTypeName.length() - 1);
			String superTypeName = fullTypeName.substring(0, indexOf);
			if (cachedSuperTypeTemplateType == null)
			{
				cachedSuperTypeTemplateType = createBaseType(context, superTypeName);
			}
			EList<Member> members = cachedSuperTypeTemplateType.getMembers();
			List<Member> overwrittenMembers = new ArrayList<Member>();
			for (Member member : members)
			{
				Member overridden = createOverrideMember(member, context, config);
				if (overridden != null)
				{
					overwrittenMembers.add(overridden);
				}
			}

			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.getMembers().addAll(overwrittenMembers);
			type.setName(fullTypeName);
			type.setKind(TypeKind.JAVA);
//			type.setAttribute(IMAGE_DESCRIPTOR, imageDescriptor);
			type.setSuperType(getType(context, superTypeName));
			return type;
		}

		/**
		 * @param context
		 * @param fullTypeName
		 * @return
		 */
		private Type createBaseType(String context, String fullTypeName)
		{
			Class< ? > cls = QUERY_BUILDER_CLASSES.get(fullTypeName);
			Type type = TypeCreator.this.createType(context, fullTypeName, cls);
			String superclass = cls.getSuperclass().getSimpleName();
			if (QUERY_BUILDER_CLASSES.containsKey(superclass))
			{
				type.setSuperType(getType(context, superclass));
			}
			return type;
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.wc_sc;
		}

		@Override
		public void flush()
		{
			cachedSuperTypeTemplateType = null;
		}

	}

	private class QueryBuilderColumnsCreator implements IScopeTypeCreator
	{
		public Type createType(String context, String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);

			TypeConfig fsAndTable = getFlattenedSolutonAndTable(typeName);
			Table table = null;
			if (fsAndTable != null && (fsAndTable.table != null || typeName.indexOf('<') <= 0))
			{
				table = fsAndTable.table;
			}
			else if (typeName.indexOf('<') > 0)
			{
				// relation name
				FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
				if (fs != null)
				{
					Relation relation = fs.getRelation(typeName.substring(typeName.indexOf('<') + 1, typeName.length() - 1));
					if (relation != null)
					{
						try
						{
							table = relation.getForeignTable();
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}
			if (table != null)
			{
				addDataProviders(context, table.getColumns().iterator(), type.getMembers(), table.getDataSource());
			}

			return type;
		}

		private void addDataProviders(String context, Iterator< ? extends IDataProvider> dataproviders, EList<Member> members, String dataSource)
		{
			while (dataproviders.hasNext())
			{
				IDataProvider provider = dataproviders.next();
				Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
				property.setName(provider.getDataProviderID());
				property.setAttribute(RESOURCE, provider);
				property.setVisible(true);
				property.setType(getTypeRef(context, QBColumn.class.getSimpleName() + '<' + dataSource + '>'));
				ImageDescriptor image = COLUMN_IMAGE;
				String description = "Column";
				if (provider instanceof AggregateVariable)
				{
					image = COLUMN_AGGR_IMAGE;
					description = "Aggregate (" + ((AggregateVariable)provider).getRootObject().getName() + ")";
				}
				else if (provider instanceof ScriptCalculation)
				{
					image = COLUMN_CALC_IMAGE;
					description = "Calculation (" + ((ScriptCalculation)provider).getRootObject().getName() + ")";
				}
				property.setAttribute(IMAGE_DESCRIPTOR, image);
				property.setDescription(description.intern());
				members.add(property);
			}
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.wc_sc;
		}

		@Override
		public void flush()
		{
		}
	}

	private class QueryBuilderJoinsCreator extends QueryBuilderCreator
	{
		@Override
		public Type createType(String context, String fullTypeName)
		{
			Type type = super.createType(context, fullTypeName);

			TypeConfig fsAndTable = getFlattenedSolutonAndTable(fullTypeName);
			FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
			if (fsAndTable != null && fs != null && fsAndTable.table != null)
			{
				if (fsAndTable.table.isMarkedAsHiddenInDeveloper())
				{
					type.setDescription("<b>Based on a table that is marked as HIDDEN in developer</b>");
					type.setDeprecated(true);
				}
				try
				{
					Iterator<Relation> relations = fs.getRelations(fsAndTable.table, true, false, false, false, false);
					while (relations.hasNext())
					{
						try
						{
							Relation relation = relations.next();
							if (relation.isValid())
							{
								Property property = createProperty(relation.getName(), true,
									getTypeRef(context, QBJoin.class.getSimpleName() + '<' + relation.getForeignDataSource() + '>'),
									getRelationDescription(relation, relation.getPrimaryDataProviders(fs), relation.getForeignColumns()), RELATION_IMAGE,
									relation);
								property.setVisible(true);
								type.getMembers().add(property);
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

			return type;
		}
	}

	private class DBDataSourceCreator implements IScopeTypeCreator
	{
		public Type createType(String context, String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);
			type.setSuperType(createArrayLookupType(context, DBDataSourceServer.class));

			IServerManagerInternal servermanager = ServoyModel.getServerManager();
			for (String serverName : servermanager.getServerNames(false, false, true, true))
			{
				IServerInternal server = (IServerInternal)servermanager.getServer(serverName, false, false);
				if (server != null)
				{
					Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
					property.setName(serverName);
					property.setAttribute(RESOURCE, server.getConfig());
					property.setVisible(true);
					property.setType(getTypeRef(context,
						DBDataSourceServer.class.getSimpleName() + '<' + DataSourceUtils.createDBTableDataSource(serverName, null) + '>'));
					property.setAttribute(
						IMAGE_DESCRIPTOR,
						com.servoy.eclipse.ui.Activator.loadImageDescriptorFromBundle(SolutionExplorerTreeContentProvider.getServerImageName(serverName, server)));
					property.setDescription("Server");
					type.getMembers().add(property);
				}
			}
			return addType(context, type);
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.wc_sc;
		}

		@Override
		public void flush()
		{
		}
	}
	private class MemDataSourceCreator implements IScopeTypeCreator
	{
		public Type createType(String context, String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);
			type.setSuperType(createArrayLookupType(context, JSDataSource.class));

			return type;
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.wc_sc;
		}

		@Override
		public void flush()
		{
		}
	}

	private class DBDataSourceServerCreator implements IScopeTypeCreator
	{
		private Type cachedSuperTypeTemplateType = null;

		public Type createType(String context, String fullTypeName)
		{
			if (fullTypeName.equals(DBDataSourceServer.class.getSimpleName()))
			{
				Type type = TypeCreator.this.createType(context, fullTypeName, DBDataSourceServer.class);
				type.setSuperType(createArrayLookupType(context, JSDataSource.class));
				// quickly add this one to the static types.
				return addType(null, type);
			}

			if (cachedSuperTypeTemplateType == null)
			{
				cachedSuperTypeTemplateType = TypeCreator.this.createType(context, DBDataSourceServer.class.getSimpleName(), DBDataSourceServer.class);
			}

			String config = fullTypeName.substring(fullTypeName.indexOf('<') + 1, fullTypeName.length() - 1);

			EList<Member> members = cachedSuperTypeTemplateType.getMembers();

			List<Member> overwrittenMembers = new ArrayList<Member>();
			for (Member member : members)
			{
				Member overridden = createOverrideMember(member, context, config);
				if (overridden != null)
				{
					overwrittenMembers.add(overridden);
				}
			}

			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.getMembers().addAll(overwrittenMembers);
			type.setSuperType(getType(context, DBDataSourceServer.class.getSimpleName()));
			type.setName(fullTypeName);
			type.setKind(TypeKind.JAVA);

			// add tables
			int index = fullTypeName.indexOf('<');
			if (index > 0)
			{
				String[] stn = DataSourceUtils.getDBServernameTablename(fullTypeName.substring(index + 1, fullTypeName.length() - 1));
				if (stn != null)
				{
					IServer server = ServoyModel.getServerManager().getServer(stn[0]);
					if (server != null)
					{
						try
						{
							for (String name : server.getTableAndViewNames(true))
							{
								ITable table = server.getTable(name);
								if (table != null)
								{
									Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
									property.setName(name);
									property.setAttribute(RESOURCE, table);
									property.setVisible(true);
									property.setType(getTypeRef(context, JSDataSource.class.getSimpleName() + '<' + table.getDataSource() + '>'));
									property.setAttribute(IMAGE_DESCRIPTOR, com.servoy.eclipse.ui.Activator.loadImageDescriptorFromBundle("portal.gif"));
									property.setDescription(Table.getTableTypeAsString(table.getTableType()));
									type.getMembers().add(property);
								}
							}
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError(e);
						}
						catch (RemoteException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}

			return type;
		}

		@Override
		public ClientSupport getClientSupport()
		{
			return ClientSupport.wc_sc;
		}

		@Override
		public void flush()
		{
			cachedSuperTypeTemplateType = null;
		}

	}

	private class TypeWithConfigCreator implements IScopeTypeCreator
	{
		protected final Class< ? > cls;
		protected final ClientSupport csp;

		private Type cachedSuperTypeTemplateType = null;

		TypeWithConfigCreator(Class< ? > cls, ClientSupport csp)
		{
			this.cls = cls;
			this.csp = csp;
		}

		public Type createType(String context, String fullTypeName)
		{
			if (fullTypeName.equals(cls.getSimpleName()))
			{
				// quickly add this one to the static types.
				return addType(null, createBaseType(context, fullTypeName));
			}

			if (cachedSuperTypeTemplateType == null)
			{
				cachedSuperTypeTemplateType = createBaseType(context, cls.getSimpleName());
			}

			String config = fullTypeName.substring(fullTypeName.indexOf('<') + 1, fullTypeName.length() - 1);

			EList<Member> members = cachedSuperTypeTemplateType.getMembers();

			List<Member> overwrittenMembers = new ArrayList<Member>();
			for (Member member : members)
			{
				Member overridden = createOverrideMember(member, context, config);
				if (overridden != null)
				{
					overwrittenMembers.add(overridden);
				}
			}

			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.getMembers().addAll(overwrittenMembers);
			type.setSuperType(getType(context, cls.getSimpleName()));
			type.setName(fullTypeName);
			type.setKind(TypeKind.JAVA);

			return type;
		}

		protected Type createBaseType(String context, String typeName)
		{
			return TypeCreator.this.createType(context, typeName, cls);
		}

		@Override
		public ClientSupport getClientSupport()
		{
			return csp;
		}

		@Override
		public void flush()
		{
			cachedSuperTypeTemplateType = null;
		}
	}

	private class JSDataSourcesCreator extends DynamicJavaClassCreator
	{
		JSDataSourcesCreator()
		{
			super(JSDataSources.class, ClientSupport.wc_sc);
		}
	}

	private class DynamicJavaClassCreator implements IScopeTypeCreator
	{
		private final Class< ? > cls;
		private final ClientSupport csp;

		DynamicJavaClassCreator(Class< ? > cls, ClientSupport csp)
		{
			this.cls = cls;
			this.csp = csp;
		}

		public final Type createType(String context, String fullTypeName)
		{
			// Make sure the type is dynamic, created in solution context
			return addType(context, doCreateType(context, fullTypeName));
		}

		protected Type doCreateType(String context, String fullTypeName)
		{
			return TypeCreator.this.createType(context, fullTypeName, cls);
		}

		@Override
		public ClientSupport getClientSupport()
		{
			return csp;
		}

		@Override
		public void flush()
		{
		}
	}

	private class InvisibleRelationsScopeCreator extends RelationsScopeCreator
	{
		@Override
		protected boolean isVisible()
		{
			return false;
		}
	}

	private class RelationsScopeCreator implements IScopeTypeCreator
	{
		public Type createType(String context, String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);

			TypeConfig fsAndTable = getFlattenedSolutonAndTable(typeName);
			if (fsAndTable != null && fsAndTable.flattenedSolution != null)
			{
				// do not set Relations<solutionName> as supertype when table is not null, if you do then in a table
				// context (like forms.x.foundset) global relations show up.
				try
				{
					addRelations(context, fsAndTable.flattenedSolution, fsAndTable.scopeName, type.getMembers(),
						fsAndTable.flattenedSolution.getRelations(fsAndTable.table, true, false, fsAndTable.table == null, false, false), isVisible());
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

			return type;
		}

		protected boolean isVisible()
		{
			return true;
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.All;
		}

		@Override
		public void flush()
		{
		}
	}

	private class InvisibleDataprovidersScopeCreator extends DataprovidersScopeCreator
	{
		@Override
		protected boolean isVisible()
		{
			return false;
		}
	}

	private class DataprovidersScopeCreator implements IScopeTypeCreator
	{
		public Type createType(String context, String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);

			TypeConfig fsAndTable = getFlattenedSolutonAndTable(typeName);
			if (fsAndTable != null && fsAndTable.table != null)
			{
				if (fsAndTable.flattenedSolution != null)
				{
					type.setSuperType(getType(context, typeName.substring(0, typeName.indexOf('<') + 1) + fsAndTable.table.getDataSource() + '>'));
					try
					{
						Map<String, IDataProvider> allDataProvidersForTable = fsAndTable.flattenedSolution.getAllDataProvidersForTable(fsAndTable.table);
						if (allDataProvidersForTable != null)
						{
							addDataProviders(context, allDataProvidersForTable.values().iterator(), type.getMembers(),
								fsAndTable.table.isMarkedAsHiddenInDeveloper(), isVisible(), false);
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
				else
				{
					addDataProviders(context, fsAndTable.table.getColumns().iterator(), type.getMembers(), false, isVisible(), true);
					return addType(SCOPE_TABLES, type);

				}
			}

			return type;
		}

		private void addDataProviders(String context, Iterator< ? extends IDataProvider> dataproviders, EList<Member> members, boolean hiddenTable,
			boolean visible, boolean columnsOnly)
		{
			while (dataproviders.hasNext())
			{
				IDataProvider provider = dataproviders.next();
				boolean uuid = false;
				if (columnsOnly)
				{
					if (provider instanceof AggregateVariable || provider instanceof ScriptCalculation) continue;
					if (provider instanceof Column)
					{
						ColumnInfo ci = ((Column)provider).getColumnInfo();
						if (ci != null && ci.isExcluded()) continue;
						if (ci != null && ci.hasFlag(Column.UUID_COLUMN))
						{
							uuid = true;
						}
					}
				}
				else if (provider instanceof Column) continue;

				Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
				property.setName(provider.getDataProviderID());
				property.setAttribute(RESOURCE, provider);
				property.setVisible(visible);

				ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, provider,
					com.servoy.eclipse.core.Activator.getDefault().getDesignClient());
				switch (componentFormat.dpType)
				{
					case IColumnTypes.DATETIME :
						property.setType(getTypeRef(context, "Date"));
						break;

					case IColumnTypes.INTEGER :
					case IColumnTypes.NUMBER :
						property.setType(getTypeRef(context, "Number"));
						break;

					case IColumnTypes.TEXT :
						property.setType(getTypeRef(context, "String"));
						break;

					case IColumnTypes.MEDIA :
						// Just return the Any type, because a media can hold anything.
						// should be in sync with TypeCreater.getDataProviderType
						property.setType(TypeInfoModelFactory.eINSTANCE.createAnyType());
						break;
				}
				if (uuid)
				{
					property.setType(getTypeRef(context, "UUID"));
				}
				ImageDescriptor image = COLUMN_IMAGE;
				String description = "Column";
				if (provider instanceof AggregateVariable)
				{
					image = COLUMN_AGGR_IMAGE;
					description = "Aggregate (" + ((AggregateVariable)provider).getRootObject().getName() + ")".intern();
				}
				else if (provider instanceof ScriptCalculation)
				{
					image = COLUMN_CALC_IMAGE;
					description = "Calculation (" + ((ScriptCalculation)provider).getRootObject().getName() + ")".intern();
				}
				if (provider instanceof Column && ((Column)provider).getColumnInfo() != null)
				{
					String columnDesc = ((Column)provider).getColumnInfo().getDescription();
					if (columnDesc != null)
					{
						description += "<br/>" + columnDesc.replace("\n", "<br/>");
					}
				}
				property.setAttribute(IMAGE_DESCRIPTOR, image);
				property.setDescription(description);
				if (hiddenTable)
				{
					property.setDeprecated(true);
					property.setDescription(property.getDescription() + " <b>of table marked as HIDDEN in developer</b>");
				}
				members.add(property);
			}
		}

		protected boolean isVisible()
		{
			return true;
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.All;
		}

		@Override
		public void flush()
		{
		}
	}

	/**
	 * Parse the config for a type. Possible combinations:
	 *
	 * <br>* Typename&lt;solutionName;dataSource&gt;
	 *
	 * <br>* Typename&lt;dataSource&gt;
	 *
	 * <br>* Typename&lt;solutionName&gt;
	 *
	 * <br>* Typename&lt;solutionName/scopeName&gt;
	 */
	private static TypeConfig getFlattenedSolutonAndTable(String typeName)
	{
		if (typeName.endsWith(">"))
		{
			IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
			int index = typeName.indexOf('<');
			if (index > 0)
			{
				String config = typeName.substring(index + 1, typeName.length() - 1);
				int sep = config.indexOf(';');
				if (sep > 0)
				{
					// solutionName;dataSource
					ServoyProject servoyProject = servoyModel.getServoyProject(config.substring(0, sep));
					if (servoyProject != null && servoyModel.isSolutionActive(servoyProject.getProject().getName()) &&
						servoyModel.getFlattenedSolution().getSolution() != null)
					{
						try
						{
							FlattenedSolution fs = servoyProject.getEditingFlattenedSolution();
							String[] dbServernameTablename = DataSourceUtilsBase.getDBServernameTablename(config.substring(sep + 1));
							if (dbServernameTablename != null)
							{
								IServer server = fs.getSolution().getRepository().getServer(dbServernameTablename[0]);
								if (server != null)
								{
									Table table = (Table)server.getTable(dbServernameTablename[1]);
									if (table != null)
									{
										return new TypeConfig(fs, table);
									}
								}
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				}
				else
				{
					// this is only dataSource or solutionname[/scope]
					if (servoyModel.getFlattenedSolution().getSolution() != null)
					{
						String[] dbServernameTablename = DataSourceUtilsBase.getDBServernameTablename(config);
						if (dbServernameTablename != null)
						{
							try
							{
								IServer server = servoyModel.getFlattenedSolution().getSolution().getRepository().getServer(dbServernameTablename[0]);
								if (server != null)
								{
									Table table = (Table)server.getTable(dbServernameTablename[1]);
									if (table != null)
									{
										return new TypeConfig(table);
									}
								}
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
							}
						}
						else
						{
							// solutionName[/scopeName]
							String[] split = config.split("/");
							ServoyProject servoyProject = servoyModel.getServoyProject(split[0]);
							if (servoyProject != null && servoyModel.isSolutionActive(servoyProject.getProject().getName()) &&
								servoyModel.getFlattenedSolution().getSolution() != null)
							{
								return new TypeConfig(servoyProject.getEditingFlattenedSolution(), split.length == 1 ? null : split[1]);
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Gets a member by name from a type.
	 */
	public static Member getMember(String memberName, Type existingType)
	{
		Member result = null;
		Iterator<Member> members = new TypeMemberQuery(existingType).ignoreDuplicates().iterator();
		while (result == null && members.hasNext())
		{
			Member member = members.next();
			if (memberName.equals(member.getName()))
			{
				result = member;
			}
		}
		return result;
	}

	private static <T extends Element> T makeDeprecated(T element)
	{
		element.setDeprecated(true);
		element.setVisible(false);
		return element;
	}

	public class ElementsScopeCreator implements IScopeTypeCreator
	{
		private final Map<String, String> typeNames = new ConcurrentHashMap<String, String>();

		private ElementsScopeCreator()
		{
			typeNames.put(IRuntimeButton.class.getSimpleName(), "RuntimeButton");
			addType("RuntimeButton", IRuntimeButton.class);
			typeNames.put(IRuntimeDataButton.class.getSimpleName(), "RuntimeDataButton");
			addType("RuntimeDataButton", IRuntimeDataButton.class);
			typeNames.put(IScriptScriptLabelMethods.class.getSimpleName(), "RuntimeLabel");
			addType("RuntimeLabel", IScriptScriptLabelMethods.class);
			typeNames.put(IScriptDataLabelMethods.class.getSimpleName(), "RuntimeDataLabel");
			addType("RuntimeDataLabel", IScriptDataLabelMethods.class);
			typeNames.put(IRuntimePassword.class.getSimpleName(), "RuntimePassword");
			addType("RuntimePassword", IRuntimePassword.class);
			typeNames.put(IRuntimeHtmlArea.class.getSimpleName(), "RuntimeHtmlArea");
			addType("RuntimeHtmlArea", IRuntimeHtmlArea.class);
			typeNames.put(IRuntimeRtfArea.class.getSimpleName(), "RuntimeRtfArea");
			addType("RuntimeRtfArea", IRuntimeRtfArea.class);
			typeNames.put(IRuntimeTextArea.class.getSimpleName(), "RuntimeTextArea");
			addType("RuntimeTextArea", IRuntimeTextArea.class);
			typeNames.put(IRuntimeChecks.class.getSimpleName(), "RuntimeChecks");
			addType("RuntimeChecks", IRuntimeChecks.class);
			typeNames.put(IRuntimeCheck.class.getSimpleName(), "RuntimeCheck");
			addType("RuntimeCheck", IRuntimeCheck.class);
			typeNames.put(IRuntimeRadios.class.getSimpleName(), "RuntimeRadios");
			addType("RuntimeRadios", IRuntimeRadios.class);
			typeNames.put(IRuntimeRadio.class.getSimpleName(), "RuntimeRadio");
			addType("RuntimeRadio", IRuntimeRadio.class);
			typeNames.put(IRuntimeCombobox.class.getSimpleName(), "RuntimeComboBox");
			addType("RuntimeComboBox", IRuntimeCombobox.class);
			typeNames.put(IRuntimeCalendar.class.getSimpleName(), "RuntimeCalendar");
			addType("RuntimeCalendar", IRuntimeCalendar.class);
			typeNames.put(IRuntimeImageMedia.class.getSimpleName(), "RuntimeImageMedia");
			addType("RuntimeImageMedia", IRuntimeImageMedia.class);
			typeNames.put(IRuntimeTextField.class.getSimpleName(), "RuntimeTypeAhead");
			typeNames.put(IRuntimeTextField.class.getSimpleName(), "RuntimeTextField");
			addType("RuntimeTextField", IRuntimeTextField.class);
			typeNames.put(IScriptTabPanelMethods.class.getSimpleName(), "RuntimeTabPanel");
			addType("RuntimeTabPanel", IScriptTabPanelMethods.class);
			typeNames.put(IScriptSplitPaneMethods.class.getSimpleName(), "RuntimeSplitPane");
			addType("RuntimeSplitPane", IScriptSplitPaneMethods.class);
			typeNames.put(IScriptPortalComponentMethods.class.getSimpleName(), "RuntimePortal");
			addType("RuntimePortal", IScriptPortalComponentMethods.class);
			typeNames.put(IScriptInsetListComponentMethods.class.getSimpleName(), "RuntimeInsetList");
			addType("RuntimeInsetList", IScriptInsetListComponentMethods.class);
			typeNames.put(IRuntimeListBox.class.getSimpleName(), "RuntimeListBox");
			addType("RuntimeListBox", IRuntimeListBox.class);
			typeNames.put(IScriptAccordionPanelMethods.class.getSimpleName(), "RuntimeAccordionPanel");
			addType("RuntimeAccordionPanel", IScriptAccordionPanelMethods.class);
			typeNames.put(IRuntimeSpinner.class.getSimpleName(), "RuntimeSpinner");
			addType("RuntimeSpinner", IRuntimeSpinner.class);
			typeNames.put(IScriptMobileBean.class.getSimpleName(), "RuntimeBean");
			addType("RuntimeBean", IScriptMobileBean.class);
			addType("RuntimeComponent", IRuntimeComponent.class);
		}

		public Type createType(String context, String typeName)
		{
			if (typeName.equals("Elements"))
			{
				Type type = TypeCreator.this.createType(context, "Elements", FormElements.class);
				type.setAttribute(IMAGE_DESCRIPTOR, ELEMENTS);

				// quickly add this one to the static types.
				return addType(null, type);
			}

			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, ELEMENTS);
			FlattenedSolution fs = ElementResolver.getFlattenedSolution(context);
			if (fs != null)
			{
				String config = typeName.substring(typeName.indexOf('<') + 1, typeName.length() - 1);
				Form form = fs.getForm(config);
				if (form != null)
				{
					type.setSuperType(getType(context, "Elements"));
					try
					{
						EList<Member> members = type.getMembers();
						Form formToUse = form;
						if (form.getExtendsID() > 0)
						{
							formToUse = fs.getFlattenedForm(form);
						}
						IApplication application = com.servoy.eclipse.core.Activator.getDefault().getDesignClient();
						Iterator<IPersist> formObjects = formToUse.getAllObjects();
						createFormElementProperties(context, application, members, formObjects);
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
			}
			return type;
		}

		/**
		 * @param application
		 * @param context
		 * @param members
		 * @param formObjects
		 */
		private void createFormElementProperties(String context, IApplication application, EList<Member> members, Iterator<IPersist> formObjects)
		{
			while (formObjects.hasNext())
			{
				IPersist persist = formObjects.next();
				if (persist instanceof IFormElement)
				{
					IFormElement formElement = (IFormElement)persist;
					if (!Utils.stringIsEmpty(formElement.getName()))
					{
						SimpleType elementType = null;
						if (FormTemplateGenerator.isWebcomponentBean(formElement))
						{
							String typeName = FormTemplateGenerator.getComponentTypeName(formElement);
							wcTypeNames.put(typeName, typeName);
							elementType = getTypeRef(context, typeName);
						}
						else
						{
							Class< ? > persistClass = ElementUtil.getPersistScriptClass(application, persist);
							if (persistClass != null && formElement instanceof Bean)
							{
								String beanClassName = ((Bean)formElement).getBeanClassName();
								if (persistClass != IScriptMobileBean.class && beanClassName != null)
								{
									// map the persist class that is registered in the initialize() method under the beanclassname under that same name.
									// So SwingDBTreeView class/name points to "DBTreeView" which points to that class again of the class types
									typeNames.put(persistClass.getSimpleName(), beanClassName.substring(beanClassName.lastIndexOf('.') + 1));
								}
							}
							elementType = getElementType(context, persistClass);
						}
						members.add(createProperty(formElement.getName(), true, elementType, null, PROPERTY));
					}
					if (formElement.getGroupID() != null)
					{
						String groupName = FormElementGroup.getName(formElement.getGroupID());
						if (groupName != null)
						{
							members.add(createProperty(groupName, true, getElementType(context, RuntimeGroup.class), null, PROPERTY));
						}
					}
					if (formElement instanceof Portal && !((Portal)formElement).isMobileInsetList())
					{
						createFormElementProperties(context, application, members, ((Portal)formElement).getAllObjects());
					}
				}
			}
		}

		private SimpleType getElementType(String context, Class< ? > cls)
		{
			if (cls == null) return null;
			String name = typeNames.get(cls.getSimpleName());
			if (name == null)
			{
				Debug.log("no element name found for " + cls.getSimpleName()); // TODO make trace, this will always be hit by beans.
				name = cls.getSimpleName();
				addAnonymousClassType(name, cls);
			}
			return getTypeRef(context, name);
		}

		public ClientSupport getClientSupport()
		{
			return ClientSupport.All;
		}

		@Override
		public void flush()
		{
		}
	}

	/**
	 * @param context
	 * @param fs
	 * @param members
	 * @param relations
	 * @param visible
	 * @throws RepositoryException
	 */
	private void addRelations(String context, FlattenedSolution fs, String scopeName, EList<Member> members, Iterator<Relation> relations, boolean visible)
	{
		while (relations.hasNext())
		{
			try
			{
				Relation relation = relations.next();
				// show only relations that are valid and defined for this scope
				if (relation.isValid() && (scopeName == null || relation.usesScope(scopeName)))
				{
					ImageDescriptor relationImage = RELATION_IMAGE;
					if (PersistEncapsulation.hasEncapsulation(relation, PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE))
					{
						relationImage = RELATION_PRIVATE_IMAGE;
					}
					else if (PersistEncapsulation.isModuleScope(relation, null))
					{
						relationImage = RELATION_PROTECTED_IMAGE;
					}
					Property property = createProperty(relation.getName(), true, getTypeRef(context, FoundSet.JS_FOUNDSET + '<' + relation.getName() + '>'),
						getRelationDescription(relation, relation.getPrimaryDataProviders(fs), relation.getForeignColumns()), relationImage, relation,
						relation.getDeprecated());
					if (visible)
					{
						IServerInternal sp = ((IServerInternal)relation.getPrimaryServer());
						IServerInternal sf = ((IServerInternal)relation.getForeignServer());
						if ((sp != null && sp.isTableMarkedAsHiddenInDeveloper(relation.getPrimaryTableName())) ||
							(sf != null && sf.isTableMarkedAsHiddenInDeveloper(relation.getForeignTableName())))
						{
							property.setDeprecated(true);
							property.setDescription(property.getDescription() + "<br><b>This relation is based on a table marked as HIDDEN in developer</b>.");
						}
					}
					if (PersistEncapsulation.isHideInScriptingModuleScope(relation, fs))
					{
						property.setVisible(false);
					}
					else
					{
						property.setVisible(visible);
					}
					members.add(property);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private static final ConcurrentMap<Relation, String> relationCache = new ConcurrentHashMap<Relation, String>(64, 0.9f, 16);

	static String getRelationDescription(Relation relation, IDataProvider[] primaryDataProviders, Column[] foreignColumns)
	{
		String str = relationCache.get(relation);
		if (str != null) return str;
		StringBuilder sb = new StringBuilder(150);
		if (relation.isGlobal())
		{
			sb.append("Global relation defined in solution: ");
		}
		else if (primaryDataProviders.length == 0)
		{
			sb.append("Self referencing relation defined in solution:");
		}
		else
		{
			sb.append("Relation defined in solution: ");
		}
		sb.append(relation.getRootObject().getName());
		if (relation.isGlobal() || primaryDataProviders.length == 0)
		{
			sb.append("<br/>On table: ");
			sb.append(relation.getForeignServerName() + "->" + relation.getForeignTableName());
		}
		else
		{
			sb.append("<br/>From: ");
//			sb.append(relation.getPrimaryDataSource());
			sb.append(relation.getPrimaryServerName() + " -> " + relation.getPrimaryTableName());
			sb.append("<br/>To: ");
			sb.append(relation.getForeignServerName() + " -> " + relation.getForeignTableName());
		}
		sb.append("<br/>");
		if (primaryDataProviders.length != 0)
		{
			for (int i = 0; i < foreignColumns.length; i++)
			{
				sb.append("&nbsp;&nbsp;");
				if (primaryDataProviders[i] instanceof LiteralDataprovider)
				{
					sb.append(((LiteralDataprovider)primaryDataProviders[i]).getValue());
					sb.append("&nbsp;");
					sb.append(RelationItem.getOperatorAsString(relation.getOperators()[i]));
					sb.append("&nbsp;");
				}
				else
				{
					sb.append((primaryDataProviders[i] != null) ? primaryDataProviders[i].getDataProviderID() : "unresolved");
					sb.append(" -> ");
				}
				sb.append((foreignColumns[i] != null) ? foreignColumns[i].getDataProviderID() : "unresolved");
				sb.append("<br/>");
			}
		}
		str = sb.toString();
		relationCache.put(relation, str);
		return str;
	}


	/**
	 * @param member
	 * @param config
	 * @return
	 */
	public static Member clone(Member member, JSType type)
	{
		Member clone;
		if (member instanceof Property)
		{
			Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
			property.setReadOnly(((Property)member).isReadOnly());
			clone = property;
		}
		else
		{
			org.eclipse.dltk.javascript.typeinfo.model.Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
			EList<Parameter> cloneParameters = method.getParameters();
			EList<Parameter> parameters = ((org.eclipse.dltk.javascript.typeinfo.model.Method)member).getParameters();
			for (Parameter parameter : parameters)
			{
				cloneParameters.add(clone(parameter));
			}
			clone = method;
		}

		EMap<String, Object> attributes = member.getAttributes();
		for (Entry<String, Object> entry : attributes)
		{
			clone.setAttribute(entry.getKey(), entry.getValue());
		}
		clone.setDeprecated(member.isDeprecated());
		clone.setStatic(member.isStatic());
		clone.setVisible(member.isVisible());
		clone.setVisibility(member.getVisibility());
		clone.setDescription(member.getDescription());
		clone.setName(member.getName());
		if (type == null)
		{
			if (member.getDirectType() != null)
			{
				SimpleType typeRef = TypeInfoModelFactory.eINSTANCE.createSimpleType();
				typeRef.setTarget(member.getDirectType());
				clone.setType(typeRef);
			}
		}
		else
		{
			clone.setType(type);
		}

		return clone;
	}

	/**
	 * @param parameter
	 * @return
	 */
	private static Parameter clone(Parameter parameter)
	{
		Parameter clone = TypeInfoModelFactory.eINSTANCE.createParameter();
		clone.setKind(parameter.getKind());
		clone.setName(parameter.getName());
		if (parameter.getDirectType() != null)
		{
			SimpleType typeRef = TypeInfoModelFactory.eINSTANCE.createSimpleType();
			typeRef.setTarget(parameter.getDirectType());
			clone.setType(typeRef);
		}
		return clone;
	}

	private Type getCombinedTypeWithRelationsAndDataproviders(FlattenedSolution fs, String context, String fullTypeName, String config, List<Member> members,
		Type superType, ImageDescriptor imageDescriptor, boolean visible)
	{
		if (config == null)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(fullTypeName);
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, imageDescriptor);
			type.setSuperType(superType);
			type.getMembers().addAll(members);
			return type;
		}

		if (fs == null) return null;

		String serverName = null;
		String tableName = null;
		String[] serverAndTableName = DataSourceUtilsBase.getDBServernameTablename(config);
		if (serverAndTableName != null)
		{
			serverName = serverAndTableName[0];
			tableName = serverAndTableName[1];
		}
		else
		{
			int index = config.indexOf('.');
			if (index != -1)
			{
				// table foundset
				serverName = config.substring(0, index);
				tableName = config.substring(index + 1);
			}
		}

		Type type = null;
		Table table = null;
		if (serverName == null && config.startsWith("{") && config.endsWith("}"))
		{
			type = getRecordType(config);
		}
		else
		{
			if (serverName != null)
			{
				try
				{
					IServer server = fs.getSolution().getRepository().getServer(serverName);
					if (server != null)
					{
						table = (Table)server.getTable(tableName);
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
				if (table == null) return null;
			}
			else
			{
				// relation
				try
				{
					Relation relation = fs.getRelation(config);
					if (relation != null && relation.isValid())
					{
						table = relation.getForeignTable();
						superType = getType(context, superType.getName() + '<' + table.getDataSource() + '>');
						table = null;
					}
					else return null;
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
		}

		if (type == null)
		{
			type = TypeInfoModelFactory.eINSTANCE.createType();
		}
		type.getMembers().addAll(members);
		type.setAttribute(IMAGE_DESCRIPTOR, imageDescriptor);
		type.setSuperType(superType);
		type.setName(fullTypeName);
		type.setKind(TypeKind.JAVA);

		if (table != null)
		{
			String traitsConfig = fs.getSolution().getName() + ';' + table.getDataSource();
			String relationsType = "Relations<" + traitsConfig + '>';
			String dataproviderType = "Dataproviders<" + traitsConfig + '>';
			if (!visible)
			{
				relationsType = "Invisible" + relationsType;
				dataproviderType = "Invisible" + dataproviderType;
			}

			EList<Type> traits = type.getTraits();
			traits.add(getType(context, dataproviderType));
			traits.add(getType(context, relationsType));

			if (table.isMarkedAsHiddenInDeveloper())
			{
				type.setDescription("<b>Based on a table that is marked as HIDDEN in developer</b>");
				type.setDeprecated(true);
			}
		}

		return type;
	}

	public Type createArrayLookupType(String context, Class< ? > arrayClassType)
	{
		Type superType = TypeInfoModelFactory.eINSTANCE.createType();
		Property arrayType = TypeInfoModelFactory.eINSTANCE.createProperty();
		arrayType.setName("[]");
		arrayType.setVisible(false);
		arrayType.setReadOnly(true);
		arrayType.setType(getTypeRef(context, arrayClassType.getSimpleName()));
		superType.getMembers().add(arrayType);
		return superType;
	}


	public static class TypeConfig
	{
		public final FlattenedSolution flattenedSolution;
		public final Table table;
		public final String scopeName;

		public TypeConfig(FlattenedSolution flattenedSolution, String scopeName, Table table)
		{
			this.flattenedSolution = flattenedSolution;
			this.scopeName = scopeName;
			this.table = table;
		}

		public TypeConfig(FlattenedSolution flattenedSolution, Table table)
		{
			this(flattenedSolution, null, table);
		}

		public TypeConfig(FlattenedSolution flattenedSolution, String scopeName)
		{
			this(flattenedSolution, scopeName, null);
		}

		public TypeConfig(Table table)
		{
			this(null, null, table);
		}
	}

	protected static ImageDescriptor getImageDescriptorForFormEncapsulation(int encapsulation)
	{
		String imgPath = "/icons/designer.gif";
		if ((encapsulation & PersistEncapsulation.MODULE_SCOPE) == PersistEncapsulation.MODULE_SCOPE) imgPath = "/icons/designer_protected.gif";
		else if ((encapsulation & PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE) == PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE) imgPath = "/icons/designer_private.gif";
		else if ((encapsulation & DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL) == DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL) imgPath = "/icons/designer_public.gif";
		return ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path(imgPath), null));
	}
}
