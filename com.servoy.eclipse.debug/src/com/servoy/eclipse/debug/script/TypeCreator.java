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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Parameter;
import org.eclipse.dltk.javascript.typeinfo.model.ParameterKind;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.dltk.javascript.typeinfo.model.TypeKind;
import org.eclipse.emf.common.util.EList;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.doc.IParameter;
import com.servoy.eclipse.core.doc.ITypedScriptObject;
import com.servoy.eclipse.core.doc.ScriptParameter;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerListContentProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public abstract class TypeCreator
{
	private final ConcurrentMap<String, Type> types = new ConcurrentHashMap<String, Type>();
	private final ConcurrentMap<String, Type> dynamicTypes = new ConcurrentHashMap<String, Type>();
	private final ConcurrentMap<String, Class< ? >> classTypes = new ConcurrentHashMap<String, Class< ? >>();
	private final ConcurrentMap<String, IScopeTypeCreator> scopeTypes = new ConcurrentHashMap<String, IScopeTypeCreator>();
	protected static final List<String> objectMethods = Arrays.asList(new String[] { "wait", "toString", "hashCode", "equals", "notify", "notifyAll", "getClass" });

	public TypeCreator()
	{
		super();
		registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSApplication.class));
		registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSSecurity.class));
		registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSSolutionModel.class));
		registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSDatabaseManager.class));
		registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(ServoyException.class));

		List<IClientPlugin> lst = Activator.getDefault().getDesignClient().getPluginManager().getPlugins(IClientPlugin.class);
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

		ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(true, new IPersistChangeListener()
		{
			public void persistChanges(Collection<IPersist> changes)
			{
				// TODO see if this will become a performance issue..
				clearDynamicTypes();
			}
		});
	}

	private void clearDynamicTypes()
	{
		dynamicTypes.clear();
	}

	protected Class< ? > getTypeClass(String name)
	{
		return classTypes.get(name);
	}


	public Set<String> getTypeNames(String prefix)
	{
		Set<String> names = new HashSet<String>(classTypes.keySet());
		names.addAll(scopeTypes.keySet());
		if (prefix != null && !"".equals(prefix.trim()))
		{
			Iterator<String> iterator = names.iterator();
			while (iterator.hasNext())
			{
				String name = iterator.next();
				if (!name.startsWith(prefix)) iterator.remove();
			}
		}
		return names;
	}

	public Type getType(ITypeInfoContext context, String typeName)
	{
		if (typeName.equals("Number") || typeName.equals("Array") || typeName.equals("String")) return null;
		Type type = types.get(typeName);
		if (type == null)
		{
			type = dynamicTypes.get(typeName);
			if (type == null)
			{
				type = createType(context, typeName, typeName);
				if (type != null)
				{
					Type previous = types.putIfAbsent(typeName, type);
					if (previous != null) return previous;
				}
				else
				{
					type = createDynamicType(context, typeName);
					if (type != null)
					{
						Type previous = dynamicTypes.putIfAbsent(typeName, type);
						if (previous != null) return previous;
					}
				}
			}
		}
		return type;
	}

	protected abstract boolean constantsOnly(String name);


	protected void registerConstantsForScriptObject(IReturnedTypesProvider scriptObject)
	{
		if (scriptObject == null) return;
		Class< ? >[] allReturnedTypes = scriptObject.getAllReturnedTypes();
		if (allReturnedTypes == null) return;

		for (Class< ? > element : allReturnedTypes)
		{
			if (!IDeprecated.class.isAssignableFrom(element))
			{
				if (IPrefixedConstantsObject.class.isAssignableFrom(element))
				{
					try
					{
						IPrefixedConstantsObject constants = (IPrefixedConstantsObject)element.newInstance();
						addType(constants.getPrefix(), element);
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
				else if (IConstantsObject.class.isAssignableFrom(element))
				{
					addType(element.getSimpleName(), element);
				}
				else if (!constantsOnly(element.getSimpleName()) && IJavaScriptType.class.isAssignableFrom(element))
				{
					addType(element.getSimpleName(), element);
				}
			}
		}
	}

	protected Type createDynamicType(ITypeInfoContext context, String typeName)
	{
		IScopeTypeCreator creator = scopeTypes.get(typeName);
		if (creator != null)
		{
			return creator.createType(context, typeName);
		}
		return null;
	}

	/**
	 * @param typeNameClassName
	 * @return
	 */
	protected Type createType(ITypeInfoContext context, String typeNameClassName, String fullTypeName)
	{
		Class< ? > cls = classTypes.get(typeNameClassName);
		if (cls != null)
		{
			return createType(context, fullTypeName, cls);
		}
		return null;
	}

	/**
	 * @param context
	 * @param typeName
	 * @param cls
	 * @return
	 */
	protected Type createType(ITypeInfoContext context, String typeName, Class< ? > cls)
	{
		Type type = TypeInfoModelFactory.eINSTANCE.createType();
		type.setName(typeName);
		type.setKind(TypeKind.JAVA);

		EList<Member> members = type.getMembers();

		if (cls == FoundSet.class)
		{
			Property alldataproviders = TypeInfoModelFactory.eINSTANCE.createProperty();
			alldataproviders.setName("alldataproviders");
			alldataproviders.setDescription("the dataproviders array of this foundset");
			members.add(alldataproviders);
		}

		fill(context, members, cls, typeName);
		return type;
	}

	/**
	 * @param typeName 
	 * @param members
	 * @param class1
	 */
	private void fill(ITypeInfoContext context, EList<Member> membersList, Class< ? > scriptObjectClass, String typeName)
	{
		ArrayList<String> al = new ArrayList<String>();
		JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(scriptObjectClass, null);
		if (javaMembers != null)
		{
			IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
			if (!constantsOnly(typeName))
			{
				Object[] members = javaMembers.getIds(false);
				for (Object element : members)
				{
					al.add((String)element);
				}
			}
			else if (IConstantsObject.class.isAssignableFrom(scriptObjectClass))
			{
				Object[] members = javaMembers.getIds(true);
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
							type = 3;
						}
					}
					else type = 2;
				}
				else type = 1;

				if (object != null)
				{
					Class< ? > returnType = FormDomProvider.getReturnType(object);
					if (type == 1)
					{
						Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
						method.setName(name);
						if (scriptObject != null && scriptObject.isDeprecated(name))
						{
							method.setDeprecated(true);
							method.setVisible(false);
						}
						method.setDescription(FormDomProvider.getDoc(name, scriptObjectClass, name)); // TODO name should be of parent.
						if (returnType != null)
						{
							method.setType(context.getType(getMemberTypeName(context, name, returnType, typeName)));
						}

						IParameter[] scriptParams = getParameters(name, scriptObjectClass);
						if (scriptParams.length > 0)
						{
							EList<Parameter> parameters = method.getParameters();
							for (IParameter param : scriptParams)
							{
								Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
								parameter.setName(param.getName());
								parameter.setType(context.getType(param.getType()));
								parameter.setKind(param.isOptional() ? ParameterKind.OPTIONAL : ParameterKind.NORMAL);
								parameters.add(parameter);
							}
						}
						membersList.add(method);
					}
					else
					{
						Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
						property.setName(name);
						if (scriptObject != null && scriptObject.isDeprecated(name))
						{
							property.setDeprecated(true);
							property.setVisible(false);
						}
						property.setDescription(FormDomProvider.getDoc(name, scriptObjectClass, name)); // TODO name should be of parent.
						if (returnType != null)
						{
							property.setType(context.getType(getMemberTypeName(context, name, returnType, typeName)));
						}
						membersList.add(property);
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	protected String getMemberTypeName(ITypeInfoContext context, String memberName, Class< ? > memberReturnType, String objectTypeName)
	{
		return memberReturnType.getSimpleName();
	}


	public void addType(String name, Class< ? > cls)
	{
		classTypes.put(name, cls);
	}

	public void addScopeType(String name, IScopeTypeCreator creator)
	{
		scopeTypes.put(name, creator);
	}


	protected interface IScopeTypeCreator
	{
		Type createType(ITypeInfoContext context, String fullTypeName);
	}

	public static IParameter[] getParameters(String key, Class< ? > scriptObjectClass)
	{
		if (scriptObjectClass == null) return null;
		IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
		IParameter[] parameters = null;
		String[] parameterNames = null;
		if (scriptObject instanceof ITypedScriptObject)
		{
			parameters = ((ITypedScriptObject)scriptObject).getParameters(key);
		}
		else if (scriptObject != null)
		{
			parameterNames = scriptObject.getParameterNames(key);
		}

		if (parameters == null && scriptObjectClass != null)
		{
			JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(scriptObjectClass, null);
			NativeJavaMethod method = javaMembers.getMethod(key, false);
			if (method != null)
			{
				MemberBox[] methods = method.getMethods();

				MemberBox selectedMethod = methods[0];
				for (int i = 1; i < methods.length; i++)
				{
					if (methods[i].getParameterTypes().length > selectedMethod.getParameterTypes().length)
					{
						selectedMethod = methods[i];
					}
				}
				if (parameterNames != null && selectedMethod.getParameterTypes().length != parameterNames.length)
				{
					parameters = new IParameter[parameterNames.length];
					for (int i = 0; i < parameterNames.length; i++)
					{
						parameters[i] = new ScriptParameter(parameterNames[i], null, false);
					}
				}
				else
				{
					parameters = new IParameter[selectedMethod.getParameterTypes().length];
					for (int i = 0; i < selectedMethod.getParameterTypes().length; i++)
					{
						Class< ? > paramClass = selectedMethod.getParameterTypes()[i];
						String name = null;
						String type = null;
						if (parameterNames != null)
						{
							type = SolutionExplorerListContentProvider.TYPES.get(paramClass.getName());
							name = parameterNames[i];
						}
						else if (paramClass.isArray())
						{
							type = "Array<" + SolutionExplorerListContentProvider.TYPES.get(paramClass.getComponentType().getName()) + '>';
							name = SolutionExplorerListContentProvider.TYPES.get(paramClass.getComponentType().getName()) + "[]";

						}
						else
						{
							type = SolutionExplorerListContentProvider.TYPES.get(paramClass.getName());
							name = SolutionExplorerListContentProvider.TYPES.get(paramClass.getName());
						}
						parameters[i] = new ScriptParameter(name, type, false);
					}
				}
			}
		}
		return parameters;
	}

	public static FlattenedSolution getFlattenedSolution(ITypeInfoContext context)
	{
		IResource resource = context.getModelElement().getResource();
		if (resource != null)
		{
			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject servoyProject = servoyModel.getServoyProject(resource.getProject().getName());

			return servoyProject.getEditingFlattenedSolution();
		}
		return null;
	}

	public static Form getForm(ITypeInfoContext context)
	{
		IResource resource = context.getModelElement().getResource();
		if (resource != null)
		{
			IPath path = resource.getProjectRelativePath();
			if (path.segmentCount() > 1 && path.segment(0).equals(SolutionSerializer.FORMS_DIR))
			{
				String formName = path.segment(1);
				if (formName.endsWith(SolutionSerializer.JS_FILE_EXTENSION))
				{
					formName = formName.substring(0, formName.length() - SolutionSerializer.JS_FILE_EXTENSION.length());
				}
				FlattenedSolution fs = getFlattenedSolution(context);
				if (fs != null)
				{
					return fs.getForm(formName);
				}
			}
		}
		return null;
	}

	public static Property createProperty(ITypeInfoContext context, String name, boolean readonly, String type, String description)
	{
		Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
		property.setName(name);
		property.setDescription(description);
		property.setReadOnly(readonly);
		if (type != null)
		{
			property.setType(context.getType(type));
		}
		return property;
	}

	/**
	 * @param sm
	 * @return
	 */
	protected static Member createMethod(ITypeInfoContext context, ScriptMethod sm)
	{
		Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
		method.setName(sm.getName());

		MethodArgument[] arguments = sm.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);
		if (arguments != null && arguments.length > 0)
		{
			EList<Parameter> parameters = method.getParameters();
			for (MethodArgument argument : arguments)
			{
				Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
				parameter.setKind(ParameterKind.NORMAL);
				parameter.setName(argument.getName());
				parameter.setType(context.getType(argument.getType().getName()));
				parameters.add(parameter);
			}
		}

		String type = sm.getSerializableRuntimeProperty(IScriptProvider.TYPE);
		if (type != null)
		{
			method.setType(context.getType(type));
		}
		String comment = sm.getRuntimeProperty(IScriptProvider.COMMENT);
		if (comment != null)
		{
			method.setDescription(comment);
		}
		return method;
	}


	public static Property createProperty(ITypeInfoContext context, String name, boolean readonly, String type)
	{
		Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
		property.setName(name);
		property.setReadOnly(readonly);
		if (type != null)
		{
			property.setType(context.getType(type));
		}
		return property;
	}

	public static Property createProperty(String name, boolean readonly, Type type)
	{
		Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
		property.setName(name);
		property.setReadOnly(readonly);
		if (type != null)
		{
			property.setType(type);
		}
		return property;
	}

	public static Property createProperty(String name, boolean readonly, Type type, String description)
	{
		Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
		property.setName(name);
		property.setReadOnly(readonly);
		property.setDescription(description);
		if (type != null)
		{
			property.setType(type);
		}
		return property;
	}

	public static boolean isLoginSolution(ITypeInfoContext context)
	{
		FlattenedSolution fs = getFlattenedSolution(context);
		if (fs != null)
		{
			return fs.getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION;
		}

		return false;
	}
}