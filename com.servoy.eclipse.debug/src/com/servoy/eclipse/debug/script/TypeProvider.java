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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.dltk.javascript.typeinfo.model.TypeKind;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.mozilla.javascript.JavaMembers;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.IPluginManager;
import com.servoy.j2db.scripting.GroupScriptObject;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.smart.dataui.DataButton;
import com.servoy.j2db.smart.dataui.DataCalendar;
import com.servoy.j2db.smart.dataui.DataCheckBox;
import com.servoy.j2db.smart.dataui.DataChoice;
import com.servoy.j2db.smart.dataui.DataComboBox;
import com.servoy.j2db.smart.dataui.DataField;
import com.servoy.j2db.smart.dataui.DataImgMediaField;
import com.servoy.j2db.smart.dataui.DataLabel;
import com.servoy.j2db.smart.dataui.DataLookupField;
import com.servoy.j2db.smart.dataui.DataPassword;
import com.servoy.j2db.smart.dataui.DataTextArea;
import com.servoy.j2db.smart.dataui.DataTextEditor;
import com.servoy.j2db.smart.dataui.PortalComponent;
import com.servoy.j2db.smart.dataui.ScriptButton;
import com.servoy.j2db.smart.dataui.ScriptLabel;
import com.servoy.j2db.smart.dataui.SpecialSplitPane;
import com.servoy.j2db.smart.dataui.SpecialTabPanel;
import com.servoy.j2db.smart.dataui.SwingItemFactory;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

@SuppressWarnings("nls")
public class TypeProvider extends TypeCreator implements ITypeProvider
{
	private final ConcurrentMap<String, DynamicTypeFiller> dynamicTypeFillers = new ConcurrentHashMap<String, DynamicTypeFiller>();
	private final Set<String> constantOnly = new HashSet<String>();

	public TypeProvider()
	{
		addType(Record.JS_RECORD, Record.class);
		addType("JSDataSet", JSDataSet.class);
		addType(IExecutingEnviroment.TOPLEVEL_SERVOY_EXCEPTION, ServoyException.class);
		addType("controller", JSForm.class);

		addScopeType(FoundSet.JS_FOUNDSET, new FoundSetCreator());
		addScopeType("Form", new FormScopeCreator());
		addScopeType("Elements", new ElementsScopeCreator());
		addScopeType("Plugins", new PluginsScopeCreator());
		addScopeType("Super", new SuperScopeCreator());

		dynamicTypeFillers.put(FoundSet.JS_FOUNDSET, new DataProviderFiller());
		dynamicTypeFillers.put(Record.JS_RECORD, new DataProviderFiller());
		dynamicTypeFillers.put("Form", new FormScopeFiller());
		dynamicTypeFillers.put("Super", new SuperScopeFiller());
		dynamicTypeFillers.put("Elements", new ElementsScopeFiller());

	}

	@Override
	protected synchronized void initalize()
	{
		super.initalize();
		if (constantOnly.size() == 0)
		{
			Set<String> typeNames = getTypeNames(null);
			for (String name : typeNames)
			{
				Class< ? > cls = getTypeClass(name);
				if (cls != null)
				{
					ArrayList<String> al = new ArrayList<String>();
					JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(cls, null);
					if (javaMembers != null)
					{
						Object[] members = javaMembers.getIds(false);
						for (Object element : members)
						{
							al.add((String)element);
						}
						if (javaMembers instanceof InstanceJavaMembers)
						{
							al.removeAll(((InstanceJavaMembers)javaMembers).getGettersAndSettersToHide());
						}
						else
						{
							al.removeAll(objectMethods);
						}
						if (al.size() == 0) constantOnly.add(name);
					}
				}
			}
		}
	}

	@Override
	protected boolean constantsOnly(String name)
	{
		return false;
	}

	public Set<String> listTypes(ITypeInfoContext context, String prefix)
	{
		Set<String> names = getTypeNames(prefix);
		names.remove("Elements");
		names.remove("controller");
		names.removeAll(constantOnly);
		return names;
	}

	/**
	 * @see org.eclipse.dltk.javascript.typeinfo.ITypeProvider#getType(org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext, java.lang.String)
	 */
	@Override
	public Type getType(ITypeInfoContext context, String typeName)
	{
		return super.getType(context, getRealName(typeName));
	}

	/**
	 * @param typeName
	 * @return
	 */
	private String getRealName(String typeName)
	{
		if ("Record".equals(typeName)) return Record.JS_RECORD;
		if ("FoundSet".equals(typeName)) return FoundSet.JS_FOUNDSET;
		return typeName;
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
			if (type == null)
			{
				// TODO better support for Array<Object> or Array<byte>
				type = context.getType(classType);
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
							Form superForm = fs.getForm(form.getExtendsFormID());
							if (superForm != null)
							{
								members.add(createProperty(context, "_super", true, "Super<" + superForm.getName() + '>', FORM_IMAGE));
							}
						}
						Table table = formToUse.getTable();
						if (table != null)
						{
							// first adjust the foundset member.
							for (Member member : members)
							{
								if (member.getName().equals("foundset"))
								{
									member.setType(context.getType(FoundSet.JS_FOUNDSET + '<' + table.getServerName() + '.' + table.getName() + '>'));
									break;
								}
							}
						}

						Iterator<ScriptMethod> scriptMethods = formToUse.getScriptMethods(false);
						while (scriptMethods.hasNext())
						{
							ScriptMethod sm = scriptMethods.next();
							members.add(createMethod(context, sm, FORM_METHOD_IMAGE, sm.getSerializableRuntimeProperty(IScriptProvider.FILENAME)));
						}

						// form variables
						addDataProviders(formToUse.getScriptVariables(false), members, context);

						// data providers
						Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(formToUse.getTable());

						if (allDataProvidersForTable != null)
						{
							addDataProviders(allDataProvidersForTable.values().iterator(), members, context);
						}

						// relations
						addRelations(context, fs, members, fs.getRelations(formToUse.getTable(), true, false));

						// element scope
						members.add(createProperty(context, "elements", true, "Elements<" + formToUse.getName() + '>', PROPERTY));
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
	}


	private class SuperScopeFiller implements DynamicTypeFiller
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
						}

						Iterator<ScriptMethod> scriptMethods = formToUse.getScriptMethods(false);
						while (scriptMethods.hasNext())
						{
							ScriptMethod sm = scriptMethods.next();
							members.add(createMethod(context, sm, FORM_METHOD_IMAGE, sm.getSerializableRuntimeProperty(IScriptProvider.FILENAME)));
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

	private class ElementsScopeFiller implements DynamicTypeFiller
	{
		private final ConcurrentHashMap<String, Type> elementTypes = new ConcurrentHashMap<String, Type>();

		private final Map<String, String> typeNames = new HashMap<String, String>();

		private ElementsScopeFiller()
		{
			typeNames.put(SpecialTabPanel.class.getSimpleName(), "TabPanel");
			typeNames.put(SpecialSplitPane.class.getSimpleName(), "SplitPane");
			typeNames.put(ScriptButton.class.getSimpleName(), "Button");
			typeNames.put(DataButton.class.getSimpleName(), "Button");
			typeNames.put(ScriptLabel.class.getSimpleName(), "Label");
			typeNames.put(DataLabel.class.getSimpleName(), "Label");
			typeNames.put(DataPassword.class.getSimpleName(), "Password");
			typeNames.put(DataTextEditor.class.getSimpleName(), "HtmlArea");
			typeNames.put(DataTextArea.class.getSimpleName(), "TextArea");
			typeNames.put(DataChoice.class.getSimpleName(), "Checks");
			typeNames.put(DataCheckBox.class.getSimpleName(), "CheckBox");
			typeNames.put(DataComboBox.class.getSimpleName(), "ComboBox");
			typeNames.put(DataCalendar.class.getSimpleName(), "Calendar");
			typeNames.put(DataImgMediaField.class.getSimpleName(), "MediaField");
			typeNames.put(DataLookupField.class.getSimpleName(), "TypeAhead");
			typeNames.put(DataField.class.getSimpleName(), "TextField");
			typeNames.put(PortalComponent.class.getSimpleName(), "Portal");

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
									Class< ? > persistClass = SwingItemFactory.getPersistClass(application, persist);
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
				Debug.error("no element name found for " + cls.getSimpleName());
				name = cls.getSimpleName();
			}
			Type type = elementTypes.get(name);
			if (type == null)
			{
				type = createType(context, name, cls);
				Type t = elementTypes.putIfAbsent(name, type);
				if (t != null) return t;
			}
			return type;
		}

	}

	private class DataProviderFiller implements DynamicTypeFiller
	{
		/**
		 * @see com.servoy.eclipse.debug.script.TypeProvider.DynamicTypeFiller#fillType(org.eclipse.dltk.javascript.typeinfo.model.Type, org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext, java.lang.String)
		 */
		public void fillType(Type type, ITypeInfoContext context, String config)
		{
			Table table = null;
			FlattenedSolution fs = getFlattenedSolution(context);

			int index = config.indexOf('.');
			if (index != -1)
			{
				// table foundset
				String serverName = config.substring(0, index);
				String tableName = config.substring(index + 1);

				if (fs != null)
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
			}
			else
			{
				// relation
				try
				{
					Relation relation = fs.getRelation(config);
					if (relation != null) table = relation.getForeignTable();
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
						addDataProviders(allDataProvidersForTable.values().iterator(), members, context);
					}

					// relations
					addRelations(context, fs, members, fs.getRelations(table, true, false));
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

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
				IScriptObject scriptObject = clientPlugin.getScriptObject();
				if (scriptObject != null)
				{
					ScriptObjectRegistry.registerScriptObjectForClass(scriptObject.getClass(), scriptObject);
					Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
					property.setName(clientPlugin.getName());
					property.setReadOnly(true);
					property.setType(TypeProvider.this.createType(context, clientPlugin.getName(), scriptObject.getClass()));

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

			// controller and foundset
			members.add(createProperty(context, "controller", true, "controller", PROPERTY));
			members.add(createProperty(context, "foundset", true, FoundSet.JS_FOUNDSET, FOUNDSET_IMAGE));
			type.setAttribute(IMAGE_DESCRIPTOR, FORM_IMAGE);
			return type;
		}
	}

	private static class SuperScopeCreator implements IScopeTypeCreator
	{

		public Type createType(ITypeInfoContext context, String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);
			type.setDescription(getDoc("_super", com.servoy.j2db.documentation.scripting.docs.Form.class, "", null));
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

			type.setAttribute(IMAGE_DESCRIPTOR, PROPERTY);
			return type;
		}
	}

	private static void addDataProviders(Iterator< ? extends IDataProvider> dataproviders, EList<Member> members, ITypeInfoContext context)
	{
		while (dataproviders.hasNext())
		{
			IDataProvider provider = dataproviders.next();
			Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
			property.setName(provider.getDataProviderID());
			property.setAttribute(RESOURCE, provider);
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
			String variableType = "Column";
			if (provider instanceof AggregateVariable)
			{
				image = COLUMN_AGGR_IMAGE;
				variableType = "Aggregate (" + ((AggregateVariable)provider).getRootObject().getName() + ")";
			}
			else if (provider instanceof ScriptCalculation)
			{
				image = COLUMN_CALC_IMAGE;
				variableType = "Calculation (" + ((ScriptCalculation)provider).getRootObject().getName() + ")";
			}
			else if (provider instanceof ScriptVariable)
			{
				image = FORM_VARIABLE_IMAGE;
				variableType = getParsedComment(((ScriptVariable)provider).getComment());
				property.setAttribute(RESOURCE, ((ScriptVariable)provider).getSerializableRuntimeProperty(IScriptProvider.FILENAME));
			}
			property.setAttribute(IMAGE_DESCRIPTOR, image);
			property.setDescription(variableType);
			members.add(property);
		}
	}

	/**
	 * @param context
	 * @param fs
	 * @param members
	 * @param relations
	 * @throws RepositoryException
	 */
	private static void addRelations(ITypeInfoContext context, FlattenedSolution fs, EList<Member> members, Iterator<Relation> relations)
		throws RepositoryException
	{
		while (relations.hasNext())
		{
			Relation relation = relations.next();
			Property property = createProperty(relation.getName(), true, context.getType(FoundSet.JS_FOUNDSET + "<" + relation.getName() + ">"),
				getRelationDescription(relation, relation.getPrimaryDataProviders(fs), relation.getForeignColumns()), RELATION_IMAGE, relation);
			members.add(property);
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
