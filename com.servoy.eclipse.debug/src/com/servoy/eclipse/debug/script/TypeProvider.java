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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.Icon;

import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.ITypeNames;
import org.eclipse.dltk.javascript.typeinfo.ITypeProvider;
import org.eclipse.dltk.javascript.typeinfo.TypeMode;
import org.eclipse.dltk.javascript.typeinfo.TypeUtil;
import org.eclipse.dltk.javascript.typeinfo.model.JSType;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Parameter;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.SimpleType;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.dltk.javascript.typeinfo.model.TypeKind;
import org.eclipse.dltk.javascript.typeinfo.model.TypeRef;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.DesignApplication;
import com.servoy.eclipse.core.JSDeveloperSolutionModel;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.util.IconProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.FormManager.HistoryProvider;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServoyBeanFactory;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.FormEncapsulation;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.plugins.IBeanClassProvider;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.IPluginManager;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSI18N;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.JSUnitAssertFunctions;
import com.servoy.j2db.scripting.JSUtils;
import com.servoy.j2db.scripting.RuntimeGroup;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.ui.IDepricatedScriptTabPanelMethods;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptCheckBoxMethods;
import com.servoy.j2db.ui.IScriptChoiceMethods;
import com.servoy.j2db.ui.IScriptDataButtonMethods;
import com.servoy.j2db.ui.IScriptDataCalendarMethods;
import com.servoy.j2db.ui.IScriptDataComboboxMethods;
import com.servoy.j2db.ui.IScriptDataLabelMethods;
import com.servoy.j2db.ui.IScriptDataPasswordMethods;
import com.servoy.j2db.ui.IScriptFieldMethods;
import com.servoy.j2db.ui.IScriptListBoxMethods;
import com.servoy.j2db.ui.IScriptMediaInputFieldMethods;
import com.servoy.j2db.ui.IScriptPortalComponentMethods;
import com.servoy.j2db.ui.IScriptRadioMethods;
import com.servoy.j2db.ui.IScriptScriptButtonMethods;
import com.servoy.j2db.ui.IScriptScriptLabelMethods;
import com.servoy.j2db.ui.IScriptSplitPaneMethods;
import com.servoy.j2db.ui.IScriptTextAreaMethods;
import com.servoy.j2db.ui.IScriptTextEditorMethods;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

@SuppressWarnings("nls")
public class TypeProvider extends TypeCreator implements ITypeProvider
{
	public static final String JAVA_CLASS = "JAVA_CLASS";


	private final ConcurrentHashMap<String, Boolean> ignorePackages = new ConcurrentHashMap<String, Boolean>();

	public TypeProvider()
	{
		addType("JSDataSet", JSDataSet.class);
		addType(IExecutingEnviroment.TOPLEVEL_SERVOY_EXCEPTION, ServoyException.class);

		addAnonymousClassType("Controller", JSForm.class);
		addAnonymousClassType("JSApplication", JSApplication.class);
		addAnonymousClassType("JSI18N", JSI18N.class);
		addAnonymousClassType("HistoryProvider", HistoryProvider.class);
		addAnonymousClassType("JSUtils", JSUtils.class);
		addAnonymousClassType("JSUnit", JSUnitAssertFunctions.class);
		addAnonymousClassType("JSSolutionModel", JSSolutionModel.class);
		addAnonymousClassType("JSDatabaseManager", JSDatabaseManager.class);
		addAnonymousClassType("JSDeveloperSolutionModel", JSDeveloperSolutionModel.class);
		addAnonymousClassType("JSSecurity", JSSecurity.class);
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
		addScopeType("Globals", new GlobalScopeCreator());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.ITypeProvider#initialize(org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext)
	 */
	public boolean initialize(ITypeInfoContext context)
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.debug.script.TypeCreator#getType(org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext, java.lang.String)
	 */
	public Type getType(ITypeInfoContext context, TypeMode mode, String typeName)
	{
		if (typeName.startsWith("Packages.") || typeName.startsWith("java.") || typeName.startsWith("javax."))
		{
			String name = typeName;
			if (typeName.startsWith("P"))
			{
				name = typeName.substring("Packages.".length());
				Type type = context.getInvariantType(name, null);
				if (type != null) return type;
			}
			if (ignorePackages.containsKey(name)) return null;
			try
			{
				ClassLoader cl = Activator.getDefault().getDesignClient().getBeanManager().getClassLoader();
				if (cl == null) cl = Thread.currentThread().getContextClassLoader();
				Class< ? > clz = Class.forName(name, false, cl);
				return getClassType(clz, name, context);
			}
			catch (ClassNotFoundException e)
			{
				ignorePackages.put(name, Boolean.FALSE);
			}
			return null;
		}
		else if (typeName.equals("Continuation"))
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVASCRIPT);
			type.setSuperType(context.getKnownType("Function", mode));
			context.markInvariant(type);
			return type;
		}
		else if (typeName.equals("byte"))
		{
			// special support for byte type (mostly used in Array<byte>)
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVASCRIPT);
			type.setSuperType(context.getKnownType("Object", mode));
			context.markInvariant(type);
			return type;
		}
		return super.getType(context, typeName);
	}

	private static JSType getJSType(Class< ? > type, ITypeInfoContext context)
	{
		if (type != null && type != Void.class && type != void.class)
		{
			if (type == Object.class) return context.getTypeRef(ITypeNames.OBJECT);
			if (type.isArray())
			{
				Class< ? > componentType = type.getComponentType();
				JSType componentJSType = getJSType(componentType, context);
				if (componentJSType != null)
				{
					return TypeUtil.arrayOf(componentJSType);
				}
				return context.getTypeRef(ITypeNames.ARRAY);
			}
			else if (type == Boolean.class || type == boolean.class)
			{
				return context.getTypeRef(ITypeNames.BOOLEAN);
			}
			else if (Number.class.isAssignableFrom(type) || type.isPrimitive())
			{
				return context.getTypeRef(ITypeNames.NUMBER);
			}
			else if (type == String.class || type == CharSequence.class)
			{
				return context.getTypeRef(ITypeNames.STRING);
			}
			else
			{

				return context.getTypeRef("Packages." + type.getName());
			}
		}
		return null;
	}

	private static Type getClassType(Class< ? > clz, String name, ITypeInfoContext context)
	{
		Type type = TypeInfoModelFactory.eINSTANCE.createType();
		type.setName(name);
		type.setKind(TypeKind.JAVA);
		type.setAttribute(JAVA_CLASS, clz);

		Method[] methods = clz.getMethods();
		Field[] fields = clz.getFields();

		EList<Member> members = type.getMembers();

		for (Field field : fields)
		{
			Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
			property.setName(field.getName());
			property.setType(getJSType(field.getType(), context));
			if (Modifier.isStatic(field.getModifiers()))
			{
				property.setStatic(true);
			}
			members.add(property);
		}
		for (Method method : methods)
		{
			org.eclipse.dltk.javascript.typeinfo.model.Method m = TypeInfoModelFactory.eINSTANCE.createMethod();
			m.setName(method.getName());
			m.setType(getJSType(method.getReturnType(), context));

			EList<Parameter> parameters = m.getParameters();
			Class< ? >[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++)
			{
				Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
				parameter.setName("arg" + i);
				parameter.setType(getJSType(parameterTypes[i], context));
				parameters.add(parameter);
			}
			if (Modifier.isStatic(method.getModifiers()))
			{
				m.setStatic(true);
			}
			members.add(m);
		}
		context.markInvariant(type);
		return type;
	}


	@Override
	protected void initalize()
	{
		DesignApplication application = com.servoy.eclipse.core.Activator.getDefault().getDesignClient();
		synchronized (this)
		{
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSApplication.class));
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSSecurity.class));
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSSolutionModel.class));
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSDatabaseManager.class));
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(ServoyException.class));

			List<IClientPlugin> lst = application.getPluginManager().getPlugins(IClientPlugin.class);
			for (IClientPlugin clientPlugin : lst)
			{
				// for now cast to deprecated interface
				try
				{
					Method method = clientPlugin.getClass().getMethod("getScriptObject", (Class[])null);
					if (method != null)
					{
						IScriptable scriptObject = (IScriptable)method.invoke(clientPlugin, (Object[])null);
						if (scriptObject instanceof IReturnedTypesProvider)
						{
							registerConstantsForScriptObject((IReturnedTypesProvider)scriptObject);
						}
					}
				}
				catch (Throwable e)
				{
					Debug.error("error registering constants for client plugin ", e); //$NON-NLS-1$
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
			}
			else
			{
				addType(beanClass.getSimpleName(), beanClass);
			}
		}
		super.initalize();
	}

	public Set<String> listTypes(ITypeInfoContext context, TypeMode mode, String prefix)
	{
		Set<String> names = getTypeNames(prefix);
		if (prefix != null)
		{
			String prefixLower = prefix.toLowerCase();
			if (Record.JS_RECORD.toLowerCase().startsWith(prefixLower)) names.add(Record.JS_RECORD);
			if (FoundSet.JS_FOUNDSET.toLowerCase().startsWith(prefixLower)) names.add(FoundSet.JS_FOUNDSET);
			if ("form".startsWith(prefixLower)) names.add("Form");
			if ("runtimeform".startsWith(prefixLower)) names.add("RuntimeForm");
			if ("continuation".startsWith(prefixLower)) names.add("Continuation");
		}
		else
		{
			names.add(Record.JS_RECORD);
			names.add(FoundSet.JS_FOUNDSET);
			names.add("Form");
			names.add("RuntimeForm");
			names.add("Continuation");
		}
		return names;
	}

	@Override
	protected Type createDynamicType(ITypeInfoContext context, String typeNameClassName, String fullTypeName)
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
			if (type == null) type = createType(context, classType, fullClassName);
			return type;
		}
		return super.createDynamicType(context, typeNameClassName, fullTypeName);
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
			property.setName(propertyName);
			property.setType(TypeUtil.ref(typeName));
			members.add(property);
		}
		return t;
	}


	private class GlobalScopeCreator implements IScopeTypeCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(ITypeInfoContext context, String fullTypeName)
		{
			FlattenedSolution fs = getFlattenedSolution(context);

			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			if (fs != null)
			{
				type.setName("Globals<" + fs.getMainSolutionMetaData().getName() + '>');
			}
			else
			{
				type.setName("Globals");
			}
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, GLOBALS);

			EList<Member> members = type.getMembers();

			members.add(createProperty(context, "allmethods", true, "Array", "Returns all global method names in an Array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "allvariables", true, "Array", "Returns all global variable names in an Array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "allrelations", true, "Array", "Returns all global relation names in an Array", SPECIAL_PROPERTY));

			if (fs != null)
			{
				type.setSuperType(context.getType("Relations<" + fs.getSolution().getName() + '>'));
			}
			return type;

		}
	}


	private class FormsScopeCreator implements IScopeTypeCreator
	{
		private final ConcurrentMap<String, String> descriptions = new ConcurrentHashMap<String, String>();

		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(ITypeInfoContext context, String fullTypeName)
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			Type type = null;
			if ("Forms".equals(fullTypeName))
			{
				type = TypeInfoModelFactory.eINSTANCE.createType();
				type.setKind(TypeKind.JAVA);
				type.setAttribute(IMAGE_DESCRIPTOR, FORMS);

				EList<Member> members = type.getMembers();
				members.add(createProperty("allnames", true, TypeUtil.arrayOf("String"), "All form names as an array", SPECIAL_PROPERTY));
				members.add(createProperty(context, "length", true, "Number", "Number of forms", PROPERTY));

				// special array lookup property so that forms[xxx]. does code complete.
				Property arrayProp = createProperty(context, "[]", true, "Form", PROPERTY);
				arrayProp.setVisible(false);
				members.add(arrayProp);
				type.setName("Forms");
				// quickly add this one to the static types.
				context.markInvariant(type);
			}
			else if (fs != null)
			{
				type = TypeInfoModelFactory.eINSTANCE.createType();
				type.setSuperType(context.getType("Forms"));
				type.setName("Forms<" + fs.getMainSolutionMetaData().getName() + '>');
				type.setKind(TypeKind.JAVA);
				type.setAttribute(IMAGE_DESCRIPTOR, FORMS);
				EList<Member> members = type.getMembers();
				Iterator<Form> forms = fs.getForms(false);
				while (forms.hasNext())
				{
					Form form = forms.next();
					Property formProperty = createProperty(form.getName(), true, context.getTypeRef("Form<" + form.getName() + '>'),
						getDescription(form.getDataSource()), FORM_IMAGE);
					formProperty.setAttribute(LAZY_VALUECOLLECTION, form);
					if (FormEncapsulation.isPrivate(form, fs))
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

	}

	private class FoundSetCreator implements IScopeTypeCreator
	{
		private Type cachedSuperTypeTemplateType = null;

		public Type createType(ITypeInfoContext context, String fullTypeName)
		{
			Type type;
			if (fullTypeName.equals(FoundSet.JS_FOUNDSET))
			{
				type = createBaseType(context, fullTypeName);

				// quickly add this one to the static types.
				context.markInvariant(type);
			}
			else
			{
				FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
				String config = fullTypeName.substring(fullTypeName.indexOf('<') + 1, fullTypeName.length() - 1);
				if (cachedSuperTypeTemplateType == null)
				{
					cachedSuperTypeTemplateType = createBaseType(context, FoundSet.JS_FOUNDSET);
				}
				EList<Member> members = cachedSuperTypeTemplateType.getMembers();
				List<Member> overwrittenMembers = new ArrayList<Member>();
				for (Member member : members)
				{
					JSType memberType = member.getType();
					if (memberType != null)
					{
						if (memberType.getName().equals(Record.JS_RECORD))
						{
							overwrittenMembers.add(TypeProvider.clone(member, context.getTypeRef(Record.JS_RECORD + '<' + config + '>')));
						}
						else if (memberType.getName().equals("Array<" + Record.JS_RECORD + '>'))
						{
							overwrittenMembers.add(TypeProvider.clone(member, TypeUtil.arrayOf(Record.JS_RECORD + '<' + config + '>')));
						}
						else if (memberType.getName().equals(FoundSet.JS_FOUNDSET))
						{
							if (member.getName().equals("unrelate"))
							{
								// its really a relation, unrelate it.
								if (fs != null)
								{
									Relation relation = fs.getRelation(config);
									if (relation != null)
									{
										overwrittenMembers.add(TypeProvider.clone(member,
											context.getTypeRef(FoundSet.JS_FOUNDSET + '<' + relation.getForeignDataSource() + '>')));
									}
									else
									{
										overwrittenMembers.add(TypeProvider.clone(member, context.getTypeRef(FoundSet.JS_FOUNDSET + '<' + config + '>')));
									}
								}
							}
							else
							{
								overwrittenMembers.add(TypeProvider.clone(member, context.getTypeRef(FoundSet.JS_FOUNDSET + '<' + config + '>')));
							}
						}
					}
				}
				type = getCombinedType(context, fullTypeName, config, overwrittenMembers, context.getType(FoundSet.JS_FOUNDSET), FOUNDSET_IMAGE, true);
			}
			return type;
		}

		/**
		 * @param context
		 * @param fullTypeName
		 * @return
		 */
		private Type createBaseType(ITypeInfoContext context, String fullTypeName)
		{
			Type type;
			type = TypeProvider.this.createType(context, fullTypeName, FoundSet.class);
			//type.setAttribute(IMAGE_DESCRIPTOR, FOUNDSET_IMAGE);

			Property alldataproviders = TypeInfoModelFactory.eINSTANCE.createProperty();
			alldataproviders.setName("alldataproviders");
			alldataproviders.setDescription("the dataproviders array of this foundset");
			alldataproviders.setAttribute(IMAGE_DESCRIPTOR, SPECIAL_PROPERTY);
			type.getMembers().add(alldataproviders);

			Property maxRecordIndex = TypeInfoModelFactory.eINSTANCE.createProperty();
			maxRecordIndex.setName("maxRecordIndex");
			maxRecordIndex.setDeprecated(true);
			maxRecordIndex.setVisible(false);
			type.getMembers().add(maxRecordIndex);

			Property selectedIndex = TypeInfoModelFactory.eINSTANCE.createProperty();
			selectedIndex.setName("selectedIndex");
			selectedIndex.setDeprecated(true);
			selectedIndex.setVisible(false);
			type.getMembers().add(selectedIndex);
			return type;
		}


	}

	private class JSDataSetCreator implements IScopeTypeCreator
	{

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.eclipse.debug.script.TypeCreator.IScopeTypeCreator#createType(org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext,
		 * java.lang.String)
		 */
		public Type createType(ITypeInfoContext context, String fullTypeName)
		{
			Type type = context.getType("JSDataSet");
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

	}


	private class RecordCreator extends FoundSetCreator
	{
		private Type cachedSuperTypeTemplateType;

		@Override
		public Type createType(ITypeInfoContext context, String fullTypeName)
		{
			Type type;
			if (fullTypeName.equals(Record.JS_RECORD))
			{
				type = TypeProvider.this.createType(context, fullTypeName, Record.class);
				ImageDescriptor desc = IconProvider.instance().descriptor(Record.class);
				type.setAttribute(IMAGE_DESCRIPTOR, desc);
				// quickly add this one to the static types.
				context.markInvariant(type);
			}
			else
			{
				String config = fullTypeName.substring(fullTypeName.indexOf('<') + 1, fullTypeName.length() - 1);
				if (cachedSuperTypeTemplateType == null)
				{
					cachedSuperTypeTemplateType = createType(context, Record.JS_RECORD);
				}
				EList<Member> members = cachedSuperTypeTemplateType.getMembers();
				List<Member> overwrittenMembers = new ArrayList<Member>();
				for (Member member : members)
				{
					JSType memberType = member.getType();
					if (memberType != null)
					{
						if (memberType.getName().equals(FoundSet.JS_FOUNDSET))
						{
							overwrittenMembers.add(TypeProvider.clone(member, context.getTypeRef(FoundSet.JS_FOUNDSET + '<' + config + '>')));
						}
					}
				}
				type = getCombinedType(context, fullTypeName, config, overwrittenMembers, context.getType(Record.JS_RECORD),
					IconProvider.instance().descriptor(Record.class), true);
			}
			return type;
		}
	}

	private class PluginsScopeCreator implements IScopeTypeCreator
	{
		private final Map<String, Image> images = new HashMap<String, Image>();

		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(ITypeInfoContext context, String fullTypeName)
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
					Method method = clientPlugin.getClass().getMethod("getScriptObject", (Class[])null);
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
					property.setType(context.getTypeRef("Plugin<" + clientPlugin.getName() + '>'));

					Image clientImage = null;
					Icon icon = clientPlugin.getImage();
					if (icon != null)
					{
						clientImage = images.get(clientPlugin.getName());
						if (clientImage == null)
						{
							clientImage = UIUtils.getSWTImageFromSwingIcon(icon, Display.getDefault());
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
			// quickly add this one to the static types.
			context.markInvariant(type);
			return type;
		}
	}

	private class FormScopeCreator implements IScopeTypeCreator
	{
		public Type createType(ITypeInfoContext context, String typeName)
		{
			Type type;
			if (typeName.equals("Form") || typeName.equals("RuntimeForm"))
			{
				type = TypeInfoModelFactory.eINSTANCE.createType();
				type.setName(typeName);
				type.setKind(TypeKind.JAVA);

				EList<Member> members = type.getMembers();

				members.add(createProperty("allnames", true, TypeUtil.arrayOf("String"), "Array with all the names in this form scope", SPECIAL_PROPERTY));
				members.add(createProperty("alldataproviders", true, TypeUtil.arrayOf("String"), "Array with all the dataprovider names", SPECIAL_PROPERTY));
				members.add(createProperty("allmethods", true, TypeUtil.arrayOf("String"), "Array with all the method names", SPECIAL_PROPERTY));
				members.add(createProperty("allrelations", true, TypeUtil.arrayOf("String"), "Array with all the relation names", SPECIAL_PROPERTY));
				members.add(createProperty("allvariables", true, TypeUtil.arrayOf("String"), "Array with all the variable names", SPECIAL_PROPERTY));

				// controller and foundset and elements
				members.add(createProperty(context, "controller", true, "Controller", IconProvider.instance().descriptor(JSForm.class)));
				members.add(createProperty(context, "foundset", true, FoundSet.JS_FOUNDSET, FOUNDSET_IMAGE));
				members.add(createProperty(context, "elements", true, "Elements", ELEMENTS));

				//type.setAttribute(IMAGE_DESCRIPTOR, FORM_IMAGE);
				// quickly add this one to the static types.
				context.markInvariant(type);
			}
			else
			{
				FlattenedSolution fs = getFlattenedSolution(context);
				String config = typeName.substring(typeName.indexOf('<') + 1, typeName.length() - 1);
				Form form = fs.getForm(config);
				if (form == null) return context.getKnownType("Form", TypeMode.CODE);
				Form formToUse = fs.getFlattenedForm(form);
				Type superForm = context.getType("Form");
				if (form.getExtendsID() > 0)
				{
					Form extendsForm = fs.getForm(form.getExtendsID());
					if (extendsForm != null) superForm = context.getType("Form<" + extendsForm.getName() + '>');
				}

				String ds = formToUse.getDataSource();
				List<Member> overwrittenMembers = new ArrayList<Member>();

				if (ds != null || FormEncapsulation.hideFoundset(formToUse))
				{
					String foundsetType = FoundSet.JS_FOUNDSET;
					if (ds != null) foundsetType += '<' + ds + '>';
					Member clone = createProperty(context, "foundset", true, foundsetType, FOUNDSET_IMAGE);
					overwrittenMembers.add(clone);
					clone.setVisible(!FormEncapsulation.hideFoundset(formToUse));
				}
				if (FormEncapsulation.hideController(formToUse))
				{
					Member clone = createProperty(context, "controller", true, "Controller", IconProvider.instance().descriptor(JSForm.class));
					overwrittenMembers.add(clone);
					clone.setVisible(false);
				}
				if (FormEncapsulation.hideDataproviders(formToUse))
				{
					Member clone = createProperty("alldataproviders", true, TypeUtil.arrayOf("String"), "Array with all the dataprovider names",
						SPECIAL_PROPERTY);
					overwrittenMembers.add(clone);
					clone.setVisible(false);

					clone = createProperty("allrelations", true, TypeUtil.arrayOf("String"), "Array with all the relation names", SPECIAL_PROPERTY);
					overwrittenMembers.add(clone);
					clone.setVisible(false);
				}

				Member clone = createProperty(context, "elements", true, "Elements<" + config + '>', ELEMENTS);
				overwrittenMembers.add(clone);
				clone.setVisible(!FormEncapsulation.hideElements(formToUse));

				type = getCombinedType(context, typeName, ds, overwrittenMembers, superForm, FORM_IMAGE, !FormEncapsulation.hideDataproviders(formToUse));
				if (type != null) type.setAttribute(LAZY_VALUECOLLECTION, form);
			}
			return type;
		}
	}
	private static class InvisibleRelationsScopeCreator extends RelationsScopeCreator
	{
		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.eclipse.debug.script.TypeProvider.RelationsScopeCreator#isVisible()
		 */
		@Override
		protected boolean isVisible()
		{
			return false;
		}
	}

	private static class RelationsScopeCreator implements IScopeTypeCreator
	{
		public Type createType(ITypeInfoContext context, String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);

			Pair<FlattenedSolution, Table> fsAndTable = getFlattenedSolutonAndTable(typeName);
			if (fsAndTable != null && fsAndTable.getLeft() != null)
			{
				FlattenedSolution fs = fsAndTable.getLeft();
				Table table = fsAndTable.getRight();
				if (table != null)
				{
					type.setSuperType(context.getType("Relations<" + fs.getSolution().getName() + '>'));
				}
				try
				{
					addRelations(context, fs, type.getMembers(), fs.getRelations(table, true, false, table == null), isVisible());
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

		public Type createType(ITypeInfoContext context, String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);

			Pair<FlattenedSolution, Table> fsAndTable = getFlattenedSolutonAndTable(typeName);
			if (fsAndTable != null && fsAndTable.getRight() != null)
			{
				if (fsAndTable.getLeft() != null)
				{
					FlattenedSolution fs = fsAndTable.getLeft();
					type.setSuperType(context.getType(typeName.substring(0, typeName.indexOf('<') + 1) + fsAndTable.getRight().getDataSource() + '>'));
					try
					{
						Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(fsAndTable.getRight());
						if (allDataProvidersForTable != null)
						{
							addDataProviders(allDataProvidersForTable.values().iterator(), type.getMembers(), context, isVisible(), false);
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
				else
				{
					Table table = fsAndTable.getRight();
					addDataProviders(table.getColumns().iterator(), type.getMembers(), context, isVisible(), true);
					context.markInvariant(type, "scope:tables");
				}
			}

			return type;
		}

		private void addDataProviders(Iterator< ? extends IDataProvider> dataproviders, EList<Member> members, ITypeInfoContext context, boolean visible,
			boolean columnsOnly)
		{
			while (dataproviders.hasNext())
			{
				IDataProvider provider = dataproviders.next();
				if (columnsOnly)
				{
					if (provider instanceof AggregateVariable || provider instanceof ScriptCalculation) continue;
				}
				else
				{
					if (provider instanceof Column) continue;
				}
				Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
				property.setName(provider.getDataProviderID());
				property.setAttribute(RESOURCE, provider);
				property.setVisible(visible);
				switch (provider.getDataProviderType())
				{
					case IColumnTypes.DATETIME :
						property.setType(context.getTypeRef("Date"));
						break;
					case IColumnTypes.INTEGER :
					case IColumnTypes.NUMBER :
						property.setType(context.getTypeRef("Number"));
						break;
					case IColumnTypes.TEXT :
						property.setType(context.getTypeRef("String"));
						break;
					case IColumnTypes.MEDIA :
						// for now don't return a type (so that anything is valid)
						// mamybe we should return Array<byte> but then we also have to check column converters.
						// should be in sync with TypeCreater.getDataProviderType
//						property.setType(TypeUtil.arrayOf("byte"));
						break;
				}
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


		protected boolean isVisible()
		{
			return true;
		}
	}

	private static Pair<FlattenedSolution, Table> getFlattenedSolutonAndTable(String typeName)
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
					if (servoyProject != null && servoyModel.getFlattenedSolution().getSolution() != null)
					{
						try
						{
							FlattenedSolution fs = servoyProject.getEditingFlattenedSolution();
							String[] dbServernameTablename = DataSourceUtils.getDBServernameTablename(config.substring(sep + 1));
							if (dbServernameTablename != null)
							{
								IServer server = fs.getSolution().getRepository().getServer(dbServernameTablename[0]);
								if (server != null)
								{
									Table table = (Table)server.getTable(dbServernameTablename[1]);
									if (table != null)
									{
										return new Pair<FlattenedSolution, Table>(fs, table);
									}
								}
							}
							else
							{
								// only solutionName
								return new Pair<FlattenedSolution, Table>(fs, null);
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
					// this is only dataSource
					if (servoyModel.getFlattenedSolution().getSolution() != null)
					{
						String[] dbServernameTablename = DataSourceUtils.getDBServernameTablename(config);
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
										return new Pair<FlattenedSolution, Table>(null, table);
									}
								}
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
							}
						}
					}
				}
			}
		}
		return null;
	}

	public class ElementsScopeCreator implements IScopeTypeCreator
	{
		private final Map<String, String> typeNames = new HashMap<String, String>();

		private ElementsScopeCreator()
		{
			typeNames.put(IScriptScriptButtonMethods.class.getSimpleName(), "RuntimeButton");
			addType("RuntimeButton", IScriptScriptButtonMethods.class);
			typeNames.put(IScriptDataButtonMethods.class.getSimpleName(), "RuntimeDataButton");
			addType("RuntimeDataButton", IScriptDataButtonMethods.class);
			typeNames.put(IScriptScriptLabelMethods.class.getSimpleName(), "RuntimeLabel");
			addType("RuntimeLabel", IScriptScriptLabelMethods.class);
			typeNames.put(IScriptDataLabelMethods.class.getSimpleName(), "RuntimeDataLabel");
			addType("RuntimeDataLabel", IScriptDataLabelMethods.class);
			typeNames.put(IScriptDataPasswordMethods.class.getSimpleName(), "RuntimePassword");
			addType("RuntimePassword", IScriptDataPasswordMethods.class);
			typeNames.put(IScriptTextEditorMethods.class.getSimpleName(), "RuntimeHtmlArea");
			addType("RuntimeHtmlArea", IScriptTextEditorMethods.class);
			addType("RuntimeRtfArea", IScriptTextEditorMethods.class);
			typeNames.put(IScriptTextAreaMethods.class.getSimpleName(), "RuntimeTextArea");
			addType("RuntimeTextArea", IScriptTextAreaMethods.class);
			typeNames.put(IScriptChoiceMethods.class.getSimpleName(), "RuntimeChecks");
			addType("RuntimeChecks", IScriptChoiceMethods.class);
			typeNames.put(IScriptCheckBoxMethods.class.getSimpleName(), "RuntimeCheck");
			addType("RuntimeCheck", IScriptCheckBoxMethods.class);
			typeNames.put(IScriptChoiceMethods.class.getSimpleName(), "RuntimeRadios");
			addType("RuntimeRadios", IScriptChoiceMethods.class);
			typeNames.put(IScriptRadioMethods.class.getSimpleName(), "RuntimeRadio");
			addType("RuntimeRadio", IScriptRadioMethods.class);
			typeNames.put(IScriptDataComboboxMethods.class.getSimpleName(), "RuntimeComboBox");
			addType("RuntimeComboBox", IScriptDataComboboxMethods.class);
			typeNames.put(IScriptDataCalendarMethods.class.getSimpleName(), "RuntimeCalendar");
			addType("RuntimeCalendar", IScriptDataCalendarMethods.class);
			typeNames.put(IScriptMediaInputFieldMethods.class.getSimpleName(), "RuntimeImageMedia");
			addType("RuntimeImageMedia", IScriptMediaInputFieldMethods.class);
			typeNames.put(IScriptFieldMethods.class.getSimpleName(), "RuntimeTypeAhead");
			typeNames.put(IScriptFieldMethods.class.getSimpleName(), "RuntimeTextField");
			addType("RuntimeTextField", IScriptFieldMethods.class);
			typeNames.put(IDepricatedScriptTabPanelMethods.class.getSimpleName(), "RuntimeTabPanel");
			addType("RuntimeTabPanel", IDepricatedScriptTabPanelMethods.class);
			typeNames.put(IScriptSplitPaneMethods.class.getSimpleName(), "RuntimeSplitPane");
			addType("RuntimeSplitPane", IScriptSplitPaneMethods.class);
			typeNames.put(IScriptPortalComponentMethods.class.getSimpleName(), "RuntimePortal");
			addType("RuntimePortal", IScriptPortalComponentMethods.class);
			typeNames.put(IScriptListBoxMethods.class.getSimpleName(), "RuntimeListBox");
			addType("RuntimeListBox", IScriptListBoxMethods.class);

			addType("RuntimeComponent", IScriptBaseMethods.class);
		}

		public Type createType(ITypeInfoContext context, String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, ELEMENTS);
			if (typeName.equals("Elements"))
			{
				EList<Member> members = type.getMembers();
				members.add(createProperty("allnames", true, TypeUtil.arrayOf("String"), "Array with all the element names", SPECIAL_PROPERTY));
				members.add(createProperty(context, "length", true, "Number", PROPERTY));
				Property arrayProp = createProperty(context, "[]", true, "RuntimeComponent", PROPERTY);
				arrayProp.setVisible(false);
				members.add(arrayProp);
				// quickly add this one to the static types.
				context.markInvariant(type);
			}
			else
			{
				FlattenedSolution fs = getFlattenedSolution(context);
				if (fs != null)
				{
					String config = typeName.substring(typeName.indexOf('<') + 1, typeName.length() - 1);
					Form form = fs.getForm(config);
					if (form != null)
					{
						type.setSuperType(context.getType("Elements"));
						try
						{
							EList<Member> members = type.getMembers();
							Form formToUse = form;
							if (form.getExtendsID() > 0)
							{
								formToUse = fs.getFlattenedForm(form);
							}
							IApplication application = Activator.getDefault().getDesignClient();
							Iterator<IPersist> formObjects = formToUse.getAllObjects();
							while (formObjects.hasNext())
							{
								IPersist persist = formObjects.next();
								if (persist instanceof IFormElement)
								{
									IFormElement formElement = (IFormElement)persist;
									if (!Utils.stringIsEmpty(formElement.getName()))
									{
										Class< ? > persistClass = ElementUtil.getPersistScriptClass(application, persist);
										if (persistClass != null && formElement instanceof Bean)
										{
											String beanClassName = ((Bean)formElement).getBeanClassName();
											if (beanClassName != null)
											{
												// map the persist class that is registered in the initialize() method under the beanclassname under that same name.
												// So SwingDBTreeView class/name points to "DBTreeView" which points to that class again of the class types 
												typeNames.put(persistClass.getSimpleName(), beanClassName.substring(beanClassName.lastIndexOf('.') + 1));
											}
										}
										members.add(createProperty(formElement.getName(), true, getElementType(context, persistClass), null, PROPERTY));
									}
									else if (formElement.getGroupID() != null)
									{
										String groupName = FormElementGroup.getName(formElement.getGroupID());
										if (groupName != null)
										{
											members.add(createProperty(groupName, true, getElementType(context, RuntimeGroup.class), null, PROPERTY));
										}
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
			}
			return type;
		}

		private SimpleType getElementType(ITypeInfoContext context, Class< ? > cls)
		{
			if (cls == null) return null;
			String name = typeNames.get(cls.getSimpleName());
			if (name == null)
			{
				Debug.log("no element name found for " + cls.getSimpleName()); // TODO make trace, this will always be hit by beans.
				name = cls.getSimpleName();
				addAnonymousClassType(name, cls);
			}
			return context.getTypeRef(name);
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
	private static void addRelations(ITypeInfoContext context, FlattenedSolution fs, EList<Member> members, Iterator<Relation> relations, boolean visible)
	{
		while (relations.hasNext())
		{
			try
			{
				Relation relation = relations.next();
				if (relation.isValid())
				{
					Property property = createProperty(relation.getName(), true, context.getTypeRef(FoundSet.JS_FOUNDSET + '<' + relation.getName() + '>'),
						getRelationDescription(relation, relation.getPrimaryDataProviders(fs), relation.getForeignColumns()), RELATION_IMAGE, relation);
					property.setVisible(visible);
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

	private static String getRelationDescription(Relation relation, IDataProvider[] primaryDataProviders, Column[] foreignColumns)
	{
		String str = relationCache.get(relation);
		if (str != null) return str;
		StringBuilder sb = new StringBuilder(150);
		if (relation.isGlobal())
		{
			sb.append("Global relation defined in solution: "); //$NON-NLS-1$
		}
		else if (primaryDataProviders.length == 0)
		{
			sb.append("Self referencing relation defined in solution:"); //$NON-NLS-1$
		}
		else
		{
			sb.append("Relation defined in solution: "); //$NON-NLS-1$
		}
		sb.append(relation.getRootObject().getName());
		if (relation.isGlobal() || primaryDataProviders.length == 0)
		{
			sb.append("<br/>On table: "); //$NON-NLS-1$
			sb.append(relation.getForeignServerName() + "->" + relation.getForeignTableName()); //$NON-NLS-1$
		}
		else
		{
			sb.append("<br/>From: "); //$NON-NLS-1$
//			sb.append(relation.getPrimaryDataSource());
			sb.append(relation.getPrimaryServerName() + "->" + relation.getPrimaryTableName()); //$NON-NLS-1$
			sb.append("<br/>To: "); //$NON-NLS-1$
			sb.append(relation.getForeignServerName() + "->" + relation.getForeignTableName()); //$NON-NLS-1$
		}
		sb.append("<br/>"); //$NON-NLS-1$
		if (primaryDataProviders.length != 0)
		{
			for (int i = 0; i < foreignColumns.length; i++)
			{
				sb.append("&nbsp;&nbsp;"); //$NON-NLS-1$
				sb.append((primaryDataProviders[i] != null) ? primaryDataProviders[i].getDataProviderID() : "unresolved");
				sb.append("->"); //$NON-NLS-1$
				sb.append((foreignColumns[i] != null) ? foreignColumns[i].getDataProviderID() : "unresolved");
				sb.append("<br/>"); //$NON-NLS-1$
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
	private static Member clone(Member member, JSType type)
	{
		Member clone = null;
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
		clone.setDescription(member.getDescription());
		clone.setName(member.getName());
		if (type == null)
		{
			if (member.getDirectType() != null)
			{
				TypeRef typeRef = TypeInfoModelFactory.eINSTANCE.createTypeRef();
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
			TypeRef typeRef = TypeInfoModelFactory.eINSTANCE.createTypeRef();
			typeRef.setTarget(parameter.getDirectType());
			clone.setType(typeRef);
		}
		return clone;
	}

	private static Type getCombinedType(ITypeInfoContext context, String fullTypeName, String config, List<Member> members, Type superType,
		ImageDescriptor imageDescriptor, boolean visible)
	{
		if (config == null)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(fullTypeName);
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, imageDescriptor);
			type.setSuperType(superType);

			EList<Member> typeMembers = type.getMembers();
			for (Member member : members)
			{
				typeMembers.add(member);
			}
			return type;
		}
		FlattenedSolution fs = getFlattenedSolution(context);
		if (fs == null) return superType;

		Table table = null;

		String serverName = null;
		String tableName = null;
		String[] serverAndTableName = DataSourceUtils.getDBServernameTablename(config);
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
					superType = context.getType(superType.getName() + '<' + table.getDataSource() + '>');
					table = null;
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}

		if (table != null)
		{
			Type relationsType;
			Type dataproviderType;
			if (visible)
			{
				relationsType = context.getType("Relations<" + fs.getSolution().getName() + ';' + table.getDataSource() + '>');
				dataproviderType = context.getType("Dataproviders<" + fs.getSolution().getName() + ';' + table.getDataSource() + '>');
			}
			else
			{
				relationsType = context.getType("InvisibleRelations<" + fs.getSolution().getName() + ';' + table.getDataSource() + '>');
				dataproviderType = context.getType("InvisibleDataproviders<" + fs.getSolution().getName() + ';' + table.getDataSource() + '>');
			}
			Type compositeType = TypeInfoModelFactory.eINSTANCE.createType();
			compositeType.setName(fullTypeName);
			compositeType.setKind(TypeKind.JAVA);
			compositeType.setAttribute(IMAGE_DESCRIPTOR, imageDescriptor);
			compositeType.setSuperType(superType);

			compositeType.getMembers().addAll(members);
			EList<Type> traits = compositeType.getTraits();
			traits.add(dataproviderType);
			traits.add(relationsType);
			return compositeType;
		}
		else if (config.startsWith("{") && config.endsWith("}"))
		{
			Type type = getRecordType(config);
			type.setName(fullTypeName);
			type.setKind(TypeKind.JAVA);
			type.getMembers().addAll(members);
			type.setAttribute(IMAGE_DESCRIPTOR, imageDescriptor);
			type.setSuperType(superType);
			return type;
		}
		else
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.getMembers().addAll(members);
			type.setName(fullTypeName);
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, imageDescriptor);
			type.setSuperType(superType);
			return type;

		}
	}


}
