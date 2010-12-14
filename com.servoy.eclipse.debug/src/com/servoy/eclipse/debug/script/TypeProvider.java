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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.Icon;

import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.ITypeProvider;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Parameter;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.dltk.javascript.typeinfo.model.TypeKind;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.util.IconProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDisplayDependencyData;
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
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.IPluginManager;
import com.servoy.j2db.scripting.GroupScriptObject;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.ui.IDepricatedScriptTabPanelMethods;
import com.servoy.j2db.ui.IScriptCheckBoxMethods;
import com.servoy.j2db.ui.IScriptChoiceMethods;
import com.servoy.j2db.ui.IScriptDataButtonMethods;
import com.servoy.j2db.ui.IScriptDataCalendarMethods;
import com.servoy.j2db.ui.IScriptDataComboboxMethods;
import com.servoy.j2db.ui.IScriptDataLabelMethods;
import com.servoy.j2db.ui.IScriptDataPasswordMethods;
import com.servoy.j2db.ui.IScriptFieldMethods;
import com.servoy.j2db.ui.IScriptMediaInputFieldMethods;
import com.servoy.j2db.ui.IScriptPortalComponentMethods;
import com.servoy.j2db.ui.IScriptScriptButtonMethods;
import com.servoy.j2db.ui.IScriptScriptLabelMethods;
import com.servoy.j2db.ui.IScriptSplitPaneMethods;
import com.servoy.j2db.ui.IScriptTextAreaMethods;
import com.servoy.j2db.ui.IScriptTextEditorMethods;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

@SuppressWarnings("nls")
public class TypeProvider extends TypeCreator implements ITypeProvider
{
	private final ConcurrentMap<String, DynamicTypeFiller> dynamicTypeFillers = new ConcurrentHashMap<String, DynamicTypeFiller>();

	private static final ConcurrentHashMap<String, Type> classTypes = new ConcurrentHashMap<String, Type>();
	private static final Type NO_CLASS = TypeInfoModelFactory.eINSTANCE.createType();

	public TypeProvider()
	{
		addType(Record.JS_RECORD, Record.class);
		addType("JSDataSet", JSDataSet.class);
		addType(IExecutingEnviroment.TOPLEVEL_SERVOY_EXCEPTION, ServoyException.class);
		addAnonymousClassType("Controller", JSForm.class);

		addScopeType(FoundSet.JS_FOUNDSET, new FoundSetCreator());
		addScopeType("Form", new FormScopeCreator());
		addScopeType("Elements", new ElementsScopeCreator());
		addScopeType("Plugins", new PluginsScopeCreator());
		addScopeType("Forms", new FormsScopeCreator());
		addScopeType("Globals", new GlobalScopeCreator());

		dynamicTypeFillers.put(FoundSet.JS_FOUNDSET, new DataProviderFiller());
		dynamicTypeFillers.put(Record.JS_RECORD, new DataProviderFiller());
		dynamicTypeFillers.put("Form", new FormScopeFiller());
		dynamicTypeFillers.put("Elements", new ElementsScopeFiller());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.debug.script.TypeCreator#getType(org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext, java.lang.String)
	 */
	@Override
	public Type getType(ITypeInfoContext context, String typeName)
	{
		if (typeName.startsWith("Packages."))
		{
			String name = typeName.substring("Packages.".length());
			Type type = classTypes.get(name);
			if (type == null)
			{
				try
				{
					ClassLoader cl = Activator.getDefault().getDesignClient().getBeanManager().getClassLoader();
					if (cl == null) cl = Thread.currentThread().getContextClassLoader();
					Class< ? > clz = Class.forName(name, false, cl);
					type = getClassType(clz, name, context);
				}
				catch (ClassNotFoundException e)
				{
					// ignore
					classTypes.put(name, NO_CLASS);
				}
			}
			else if (type == NO_CLASS)
			{
				type = null;
			}
			return type;
		}
		else if (typeName.equals("Continuation"))
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVASCRIPT);
			type.setSuperType(context.getKnownType("Function"));
			return type;
		}
		return super.getType(context, typeName);
	}

	private static Type getClassType(Class< ? > clz, String name, ITypeInfoContext context)
	{
		Type type = classTypes.get(name);
		if (type != null) return type;

		type = TypeInfoModelFactory.eINSTANCE.createType();
		type.setName(name);
		type.setKind(TypeKind.JAVA);

		classTypes.put(name, type);

		Method[] methods = clz.getMethods();
		Field[] fields = clz.getFields();

		EList<Member> members = type.getMembers();

		for (Field field : fields)
		{
			Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
			property.setName(field.getName());
			Class< ? > fieldType = field.getType();
			if (fieldType != null) property.setType(context.getKnownType("Packages." + fieldType.getName()));
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
			Class< ? > methodType = method.getReturnType();
			if (methodType != null) m.setType(context.getKnownType("Packages." + methodType.getName()));

			EList<Parameter> parameters = m.getParameters();
			Class< ? >[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++)
			{
				Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
				parameter.setName(parameterTypes[i].getSimpleName() + " arg" + i);
				parameter.setType(context.getKnownType("Packages." + parameterTypes[i].getName()));
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


	@Override
	protected void initalize()
	{
		synchronized (this)
		{
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSApplication.class));
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSSecurity.class));
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSSolutionModel.class));
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSDatabaseManager.class));
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(ServoyException.class));

			List<IClientPlugin> lst = com.servoy.eclipse.core.Activator.getDefault().getDesignClient().getPluginManager().getPlugins(IClientPlugin.class);
			for (IClientPlugin clientPlugin : lst)
			{
				try
				{
					registerConstantsForScriptObject(clientPlugin.getScriptObject());
				}
				catch (Throwable e)
				{
					Debug.error("error registering constants for client plugin ", e); //$NON-NLS-1$
				}
			}
		}
		super.initalize();
	}

	@Override
	protected boolean constantsOnly(String name)
	{
		return false;
	}

	public Set<String> listTypes(ITypeInfoContext context, String prefix)
	{
		Set<String> names = getTypeNames(prefix);
		//remove types that are only elements
		names.remove("Elements");
		names.remove("Super");
		names.remove("Forms");
		names.remove("Plugins");
		// add the special rhinoe Continuation type.
		names.add("Continuation");
		return names;
	}

	@Override
	protected Type createDynamicType(ITypeInfoContext context, String typeNameClassName, String fullTypeName)
	{
		// is it a 'generified' type
		int index = typeNameClassName.indexOf('<');
		int index2;
		if (index != -1 && (index2 = typeNameClassName.indexOf('>', index)) != -1)
		{
			String classType = typeNameClassName.substring(0, index);
			Type type = createType(context, classType, typeNameClassName);
			if (type == null) type = createDynamicType(context, classType, typeNameClassName);
			DynamicTypeFiller filler = dynamicTypeFillers.get(classType);
			if (type != null && filler != null)
			{
				filler.fillType(type, context, typeNameClassName.substring(index + 1, index2));
			}
			return type;
		}
		return super.createDynamicType(context, typeNameClassName, fullTypeName);
	}

	private interface DynamicTypeFiller
	{
		public void fillType(Type type, ITypeInfoContext context, String config);
	}

	private class FormScopeFiller implements DynamicTypeFiller
	{
		public void fillType(Type type, ITypeInfoContext context, String config)
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs != null)
			{
				Form form = fs.getForm(config);
				if (form != null)
				{
					try
					{
						EList<Member> members = type.getMembers();
						Form formToUse = form;
						if (form.getExtendsFormID() > 0)
						{
							formToUse = fs.getFlattenedForm(form);
//							Form superForm = fs.getForm(form.getExtendsFormID());
//							if (superForm != null)
//							{
//								Property property = createProperty(context, "_super", true, null, FORM_IMAGE);
//								property.setDescription(getDoc("_super", com.servoy.j2db.documentation.scripting.docs.Form.class, "", null));
//								property.setAttribute(LAZY_VALUECOLLECTION, superForm);
//								members.add(property);
//							}
						}
						String ds = formToUse.getDataSource();
						if (ds != null)
						{
							// first adjust the foundset member.
							for (Member member : members)
							{
								if (member.getName().equals("foundset"))
								{
									member.setVisible(!FormEncapsulation.hideFoundset(formToUse));
									member.setType(context.getType(FoundSet.JS_FOUNDSET + '<' + ds + '>'));
								}
								else if (member.getName().equals("controller"))
								{
									member.setVisible(!FormEncapsulation.hideController(formToUse));
								}
								else if (member.getName().equals("alldataproviders"))
								{
									member.setVisible(!FormEncapsulation.hideDataproviders(formToUse));
								}
								else if (member.getName().equals("allrelations"))
								{
									member.setVisible(!FormEncapsulation.hideDataproviders(formToUse));
								}
								else if (member.getName().equals("elements"))
								{
									member.setVisible(!FormEncapsulation.hideElements(formToUse));
									member.setType(context.getType("Elements<" + ds + '>'));
								}
							}
						}

						// data providers
						Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(formToUse.getTable());

						boolean dataprovidersVisible = !FormEncapsulation.hideDataproviders(formToUse);
						if (allDataProvidersForTable != null)
						{
							addDataProviders(allDataProvidersForTable.values().iterator(), members, context, dataprovidersVisible);
						}

						// relations
						addRelations(context, fs, members, fs.getRelations(formToUse.getTable(), true, false), dataprovidersVisible);
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
	}


	private class ElementsScopeFiller implements DynamicTypeFiller
	{
		private final Map<String, String> typeNames = new HashMap<String, String>();

		private ElementsScopeFiller()
		{
			typeNames.put(IScriptScriptButtonMethods.class.getSimpleName(), "Button");
			typeNames.put(IScriptDataButtonMethods.class.getSimpleName(), "Button");
			typeNames.put(IScriptScriptLabelMethods.class.getSimpleName(), "Label");
			typeNames.put(IScriptDataLabelMethods.class.getSimpleName(), "Label");
			typeNames.put(IScriptDataPasswordMethods.class.getSimpleName(), "Password");
			typeNames.put(IScriptTextEditorMethods.class.getSimpleName(), "HtmlArea");
			typeNames.put(IScriptTextAreaMethods.class.getSimpleName(), "TextArea");
			typeNames.put(IScriptChoiceMethods.class.getSimpleName(), "Checks");
			typeNames.put(IScriptCheckBoxMethods.class.getSimpleName(), "CheckBox");
			typeNames.put(IScriptChoiceMethods.class.getSimpleName(), "Radios");
			typeNames.put(IScriptDataComboboxMethods.class.getSimpleName(), "ComboBox");
			typeNames.put(IScriptDataCalendarMethods.class.getSimpleName(), "Calendar");
			typeNames.put(IScriptMediaInputFieldMethods.class.getSimpleName(), "MediaField");
			typeNames.put(IDisplayDependencyData.class.getSimpleName(), "TypeAhead");
			typeNames.put(IScriptFieldMethods.class.getSimpleName(), "TextField");
			typeNames.put(IDepricatedScriptTabPanelMethods.class.getSimpleName(), "TabPanel");
			typeNames.put(IScriptSplitPaneMethods.class.getSimpleName(), "SplitPane");
			typeNames.put(IScriptPortalComponentMethods.class.getSimpleName(), "Portal");

		}

		public void fillType(Type type, ITypeInfoContext context, String config)
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs != null)
			{
				Form form = fs.getForm(config);
				if (form != null)
				{
					try
					{
						EList<Member> members = type.getMembers();
						Form formToUse = form;
						if (form.getExtendsFormID() > 0)
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
									if (persistClass != null && formElement instanceof Bean && ((Bean)formElement).getBeanClassName() != null)
									{
										int index = ((Bean)formElement).getBeanClassName().lastIndexOf('.');
										if (index != -1)
										{
											typeNames.put(persistClass.getSimpleName(), ((Bean)formElement).getBeanClassName().substring(index + 1));
										}
									}
									members.add(createProperty(formElement.getName(), true, getElementType(context, persistClass), null, PROPERTY));
								}
								else if (formElement.getGroupID() != null)
								{
									String groupName = FormElementGroup.getName(formElement.getGroupID());
									if (groupName != null)
									{
										members.add(createProperty(groupName, true, getElementType(context, GroupScriptObject.class), null, PROPERTY));
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

		private Type getElementType(ITypeInfoContext context, Class< ? > cls)
		{
			if (cls == null) return null;
			String name = typeNames.get(cls.getSimpleName());
			if (name == null)
			{
				Debug.log("no element name found for " + cls.getSimpleName()); // TODO make trace, this will always be hit by beans.
				name = cls.getSimpleName();
			}
			addAnonymousClassType(name, cls);
			return context.getType(name);
		}

	}

	private class DataProviderFiller implements DynamicTypeFiller
	{
		/**
		 * @see com.servoy.eclipse.debug.script.TypeProvider.DynamicTypeFiller#fillType(org.eclipse.dltk.javascript.typeinfo.model.Type, org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext, java.lang.String)
		 */
		public void fillType(Type type, ITypeInfoContext context, String config)
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs == null) return;

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
					if (relation != null && relation.isValid()) table = relation.getForeignTable();
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

			if (table != null)
			{
				try
				{
					EList<Member> members = type.getMembers();
					Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(table);
					if (allDataProvidersForTable != null)
					{
						addDataProviders(allDataProvidersForTable.values().iterator(), members, context, true);
					}

					// relations
					addRelations(context, fs, members, fs.getRelations(table, true, false), true);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

		}
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
				try
				{
					addRelations(context, fs, members, fs.getRelations(null, true, false), true);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
			return type;

		}
	}


	private class FormsScopeCreator implements IScopeTypeCreator
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
				type.setName("Forms<" + fs.getMainSolutionMetaData().getName() + '>');
			}
			else
			{
				type.setName("Forms");
			}
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, FORMS);

			EList<Member> members = type.getMembers();
			members.add(createProperty(context, "allnames", true, "Array", "All form names as an array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "length", true, "Number", "Number of forms", PROPERTY));

			// special array lookup property so that forms[xxx]. does code complete.
			Property arrayProp = createProperty(context, "[]", true, "Form", PROPERTY);
			arrayProp.setVisible(false);
			members.add(arrayProp);

			if (fs != null)
			{
				Iterator<Form> forms = fs.getForms(false);

				while (forms.hasNext())
				{
					Form form = forms.next();
					Property formProperty = createProperty(form.getName(), true, context.getType("Form<" + form.getName() + '>'), "Form based on datasource: " +
						form.getDataSource(), FORM_IMAGE);
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

	}

	private class FoundSetCreator implements IScopeTypeCreator
	{

		public Type createType(ITypeInfoContext context, String fullTypeName)
		{
			Type type = TypeProvider.this.createType(context, fullTypeName, FoundSet.class);
			type.setAttribute(IMAGE_DESCRIPTOR, FOUNDSET_IMAGE);

			Property alldataproviders = TypeInfoModelFactory.eINSTANCE.createProperty();
			alldataproviders.setName("alldataproviders");
			alldataproviders.setDescription("the dataproviders array of this foundset");
			alldataproviders.setAttribute(IMAGE_DESCRIPTOR, SPECIAL_PROPERTY);
			type.getMembers().add(alldataproviders);

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
			type.setName("plugins");
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, PLUGINS);

			EList<Member> members = type.getMembers();
			members.add(createProperty(context, "allnames", true, "Array", "All form names as an array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "length", true, "Number", "Number of forms", PROPERTY));


			IPluginManager pluginManager = com.servoy.eclipse.core.Activator.getDefault().getDesignClient().getPluginManager();
			List<IClientPlugin> clientPlugins = pluginManager.getPlugins(IClientPlugin.class);
			for (IClientPlugin clientPlugin : clientPlugins)
			{
				IScriptObject scriptObject = null;
				try
				{
					scriptObject = clientPlugin.getScriptObject();
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
					property.setType(context.getType("Plugin<" + clientPlugin.getName() + '>'));

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

			return type;
		}
	}

	private static class FormScopeCreator implements IScopeTypeCreator
	{

		public Type createType(ITypeInfoContext context, String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);

			EList<Member> members = type.getMembers();

			members.add(createProperty(context, "allnames", true, "Array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "alldataproviders", true, "Array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "allmethods", true, "Array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "allrelations", true, "Array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "allvariables", true, "Array", SPECIAL_PROPERTY));

			// controller and foundset and elements
			members.add(createProperty(context, "controller", true, "Controller", IconProvider.instance().descriptor(JSForm.class)));
			members.add(createProperty(context, "foundset", true, FoundSet.JS_FOUNDSET, FOUNDSET_IMAGE));
			members.add(createProperty(context, "elements", true, "Elements", ELEMENTS));

			type.setAttribute(IMAGE_DESCRIPTOR, FORM_IMAGE);
			return type;
		}
	}

	public static class ElementsScopeCreator implements IScopeTypeCreator
	{

		public Type createType(ITypeInfoContext context, String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);

			EList<Member> members = type.getMembers();

			members.add(createProperty(context, "allnames", true, "Array", SPECIAL_PROPERTY));
			members.add(createProperty(context, "length", true, "Number", PROPERTY));

			type.setAttribute(IMAGE_DESCRIPTOR, ELEMENTS);
			return type;
		}
	}

	private static void addDataProviders(Iterator< ? extends IDataProvider> dataproviders, EList<Member> members, ITypeInfoContext context, boolean visible)
	{
		while (dataproviders.hasNext())
		{
			IDataProvider provider = dataproviders.next();
			Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
			property.setName(provider.getDataProviderID());
			property.setAttribute(RESOURCE, provider);
			property.setVisible(visible);
			switch (provider.getDataProviderType())
			{
				case IColumnTypes.DATETIME :
					property.setType(context.getType("Date"));
					break;
				case IColumnTypes.INTEGER :
				case IColumnTypes.NUMBER :
					property.setType(context.getType("Number"));
					break;
				case IColumnTypes.TEXT :
					property.setType(context.getType("String"));
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
			else if (provider instanceof ScriptVariable)
			{
				image = FORM_VARIABLE_IMAGE;
				description = getParsedComment(((ScriptVariable)provider).getComment());
				property.setAttribute(RESOURCE, ((ScriptVariable)provider).getSerializableRuntimeProperty(IScriptProvider.FILENAME));
			}
			property.setAttribute(IMAGE_DESCRIPTOR, image);
			property.setDescription(description);
			members.add(property);
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
					Property property = createProperty(relation.getName(), true, context.getType(FoundSet.JS_FOUNDSET + "<" + relation.getName() + ">"),
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

	private static String getRelationDescription(Relation relation, IDataProvider[] primaryDataProviders, Column[] foreignColumns)
	{
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
		return sb.toString();
	}


}
