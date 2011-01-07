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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.javascript.scriptdoc.JavaDoc2HTMLTextReader;
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.JavaMembers.BeanProperty;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.doc.IParameter;
import com.servoy.eclipse.core.doc.ITypedScriptObject;
import com.servoy.eclipse.core.doc.ScriptParameter;
import com.servoy.eclipse.debug.Activator;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.IconProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerListContentProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.ui.IScriptRenderMethods;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public abstract class TypeCreator
{
	private static final int INSTANCE_METHOD = 1;
	private static final int STATIC_METHOD = 2;
	private static final int INSTANCE_FIELD = 3;
	private static final int STATIC_FIELD = 4;

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

	protected final static ImageDescriptor GLOBAL_VAR_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/global_variable.gif"), null));
	protected final static ImageDescriptor GLOBAL_METHOD_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/global_method.gif"), null));

	protected final static ImageDescriptor FORM_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(
		com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/designer.gif"), null));
	protected final static ImageDescriptor FORM_METHOD_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/form_method.gif"), null));
	protected final static ImageDescriptor FORM_VARIABLE_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/form_variable.gif"), null));

	protected final static ImageDescriptor FOUNDSET_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/foundset.gif"), null));
	protected final static ImageDescriptor RELATION_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/relation.gif"), null));

	protected final static ImageDescriptor COLUMN_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/column.gif"), null));
	protected final static ImageDescriptor COLUMN_AGGR_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/columnaggr.gif"), null));
	protected final static ImageDescriptor COLUMN_CALC_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/columncalc.gif"), null));

	protected final static ImageDescriptor GLOBALS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/globe.gif"), null));
	protected final static ImageDescriptor FORMS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/forms.gif"), null));

	protected final static ImageDescriptor PLUGINS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/plugin.gif"), null));

	protected final static ImageDescriptor PLUGIN_DEFAULT = ImageDescriptor.createFromURL(FileLocator.find(
		com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/plugin_conn.gif"), null));

	public static final String IMAGE_DESCRIPTOR = "servoy.IMAGEDESCRIPTOR";
	public static final String RESOURCE = "servoy.RESOURCE";
	public static final String VALUECOLLECTION = "servoy.VALUECOLLECTION";
	public static final String LAZY_VALUECOLLECTION = "servoy.LAZY_VALUECOLLECTION";

	protected final static Set<String> BASE_TYPES = new HashSet<String>(128);

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

	protected final ConcurrentMap<String, Type> types = new ConcurrentHashMap<String, Type>();
	private final ConcurrentMap<String, ConcurrentMap<String, Type>> dynamicTypes = new ConcurrentHashMap<String, ConcurrentMap<String, Type>>();
	protected final ConcurrentMap<String, Type> directDynamicTypes = new ConcurrentHashMap<String, Type>();
	private final ConcurrentMap<String, Class< ? >> classTypes = new ConcurrentHashMap<String, Class< ? >>();
	private final ConcurrentMap<String, Class< ? >> anonymousClassTypes = new ConcurrentHashMap<String, Class< ? >>();
	private final ConcurrentMap<String, IScopeTypeCreator> scopeTypes = new ConcurrentHashMap<String, IScopeTypeCreator>();
	private volatile boolean initialized;
	protected static final List<String> objectMethods = Arrays.asList(new String[] { "wait", "toString", "hashCode", "equals", "notify", "notifyAll", "getClass" });

	public TypeCreator()
	{
		super();
	}

	protected void initalize()
	{
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
							// TODO see if this will become a performance issue..
							clearDynamicTypes();
						}
					});
				}
			}
		}
	}

	private void clearDynamicTypes()
	{
		dynamicTypes.clear();
		directDynamicTypes.clear();
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
		names.addAll(scopeTypes.keySet());
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


	public Type getType(ITypeInfoContext context, String typeName)
	{
		if (BASE_TYPES.contains(typeName) || typeName.startsWith("Array<")) return null;
		if (!initialized) initalize();
		String realTypeName = typeName;
		Type type = types.get(realTypeName);
		if (type == null)
		{
			type = directDynamicTypes.get(realTypeName);
			if (type != null)
			{
				return type;
			}
			FlattenedSolution fs = getFlattenedSolution(context);
			ConcurrentMap<String, Type> flattenedSolutionTypeMap = null;
			if (fs != null && fs.getSolution() != null)
			{
				flattenedSolutionTypeMap = dynamicTypes.get(fs.getSolution().getName());
				if (flattenedSolutionTypeMap == null)
				{
					flattenedSolutionTypeMap = new ConcurrentHashMap<String, Type>();
					dynamicTypes.put(fs.getSolution().getName(), flattenedSolutionTypeMap);
				}
				else type = flattenedSolutionTypeMap.get(realTypeName);
			}
			if (type == null)
			{
				type = createType(context, realTypeName, realTypeName);
				if (type != null)
				{
					Type previous = types.putIfAbsent(realTypeName, type);
					if (previous != null) return previous;
				}
				else if (flattenedSolutionTypeMap != null)
				{
					type = createDynamicType(context, realTypeName, realTypeName);
					if (type != null)
					{
						Type previous = flattenedSolutionTypeMap.putIfAbsent(realTypeName, type);
						if (previous != null) return previous;
					}
				}
			}
		}
		return type;
	}

	/**
	 * @param typeName
	 * @return
	 */
	private String getRealName(String typeName)
	{
		if ("Record".equals(typeName)) return Record.JS_RECORD;
		if ("FoundSet".equals(typeName)) return FoundSet.JS_FOUNDSET;
		if (IScriptRenderMethods.class.getSimpleName().equals(typeName)) return IScriptRenderMethods.JS_RENDERABLE;
		return typeName;
	}


	protected final void registerConstantsForScriptObject(IReturnedTypesProvider scriptObject)
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
				else if (IJavaScriptType.class.isAssignableFrom(element))
				{
					addType(element.getSimpleName(), element);
				}
			}
		}
	}

	protected Type createDynamicType(ITypeInfoContext context, String typeNameClassName, String fullTypeName)
	{
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
	protected final Type createType(ITypeInfoContext context, String typeNameClassName, String fullTypeName)
	{
		Class< ? > cls = getTypeClass(typeNameClassName);
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
	protected final Type createType(ITypeInfoContext context, String typeName, Class< ? > cls)
	{
		Type type = TypeInfoModelFactory.eINSTANCE.createType();
		type.setName(typeName);
		type.setKind(TypeKind.JAVA);
		EList<Member> members = type.getMembers();

		fill(context, members, cls, typeName);

		ImageDescriptor desc = IconProvider.instance().descriptor(cls);
		type.setAttribute(IMAGE_DESCRIPTOR, desc);
		return type;
	}

	/**
	 * @param typeName 
	 * @param members
	 * @param class1
	 */
	private final void fill(ITypeInfoContext context, EList<Member> membersList, Class< ? > scriptObjectClass, String typeName)
	{
		ArrayList<String> al = new ArrayList<String>();
		JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(scriptObjectClass, null);
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
					Class< ? > returnTypeClz = getReturnType(object);
					if (type == INSTANCE_METHOD || type == STATIC_METHOD)
					{
						MemberBox[] memberbox = null;
						if (object instanceof NativeJavaMethod)
						{
							memberbox = ((NativeJavaMethod)object).getMethods();
						}
						int membersSize = memberbox == null ? 1 : memberbox.length;
						for (int i = 0; i < membersSize; i++)
						{
							Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
							method.setName(name);
							Class< ? >[] parameterTypes = memberbox[i].getParameterTypes();

							if (scriptObject instanceof ITypedScriptObject && ((ITypedScriptObject)scriptObject).isDeprecated(name, parameterTypes))
							{
								method.setDeprecated(true);
								method.setVisible(false);
							}
							else if (scriptObject != null && scriptObject.isDeprecated(name))
							{
								method.setDeprecated(true);
								method.setVisible(false);
							}
							method.setDescription(getDoc(name, scriptObjectClass, name, parameterTypes)); // TODO name should be of parent.
							if (returnTypeClz != null)
							{
								method.setType(context.getType(getMemberTypeName(context, name, returnTypeClz, typeName)));
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
									if (param.getType() != null) parameter.setType(context.getType(SolutionExplorerListContentProvider.TYPES.get(param.getType())));
									parameter.setKind(param.isOptional() ? param.isVarArgs() ? ParameterKind.VARARGS : ParameterKind.OPTIONAL
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
									parameter.setName(SolutionExplorerListContentProvider.TYPES.get(paramClass.getName()));
									parameter.setType(context.getType(SolutionExplorerListContentProvider.TYPES.get(paramClass.getName())));
									parameter.setKind(ParameterKind.NORMAL);
									parameters.add(parameter);
								}
							}
							membersList.add(method);
						}
					}
					else
					{
						Type returnType = null;
						if (returnTypeClz != null)
						{
							returnType = context.getType(getMemberTypeName(context, name, returnTypeClz, typeName));
						}
						ImageDescriptor descriptor = IconProvider.instance().descriptor(returnTypeClz);
						if (descriptor == null)
						{
							descriptor = type == STATIC_FIELD ? CONSTANT : PROPERTY;
						}
						Property property = createProperty(name, false, returnType, getDoc(name, scriptObjectClass, name, null), descriptor);
						property.setStatic(type == STATIC_FIELD);
						if (scriptObject != null && scriptObject.isDeprecated(name))
						{
							property.setDeprecated(true);
							property.setVisible(false);
						}
						membersList.add(property);
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	protected final String getMemberTypeName(ITypeInfoContext context, String memberName, Class< ? > memberReturnType, String objectTypeName)
	{
		int index = objectTypeName.indexOf('<');
		int index2;
		if (index != -1 && (index2 = objectTypeName.indexOf('>', index)) != -1)
		{
			String config = objectTypeName.substring(index + 1, index2);

			if (memberReturnType == Record.class)
			{
				return Record.JS_RECORD + '<' + config + '>';
			}
			if (memberReturnType == FoundSet.class)
			{
				if (memberName.equals("unrelated"))
				{
					if (config.indexOf('.') == -1)
					{
						// its really a relation, unrelate it.
						FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
						if (fs != null)
						{
							Relation relation = fs.getRelation(config);
							if (relation != null)
							{
								return FoundSet.JS_FOUNDSET + '<' + relation.getForeignDataSource() + '>';
							}
						}
						return FoundSet.JS_FOUNDSET;
					}
				}
				return FoundSet.JS_FOUNDSET + '<' + config + '>';
			}
		}
		if (memberReturnType.isArray())
		{
			Class< ? > returnType = getReturnType(memberReturnType.getComponentType());
			if (returnType != null)
			{
				return "Array<" + getMemberTypeName(context, memberName, returnType, objectTypeName) + '>';
			}
			return "Array";
		}

		String typeName = getRealName(memberReturnType.getSimpleName());
		addAnonymousClassType(typeName, memberReturnType);
		return typeName;
	}

	public final void addType(String name, Class< ? > cls)
	{
		classTypes.put(name, cls);
	}

	protected final void addAnonymousClassType(String name, Class< ? > cls)
	{
		if (!classTypes.containsKey(name) && !scopeTypes.containsKey(name) && !BASE_TYPES.contains(name))
		{
			anonymousClassTypes.put(name, cls);
		}
	}

	public final void addScopeType(String name, IScopeTypeCreator creator)
	{
		scopeTypes.put(name, creator);
	}

	/**
	 * @param context
	 * @param type
	 * @param provider
	 * @return
	 */
	protected static final Type getDataProviderType(ITypeInfoContext context, IDataProvider provider)
	{
		Type type = null;
		switch (provider.getDataProviderType())
		{
			case IColumnTypes.DATETIME :
				type = context.getType("Date");
				break;
			case IColumnTypes.INTEGER :
			case IColumnTypes.NUMBER :
				type = context.getType("Number");
				break;
			case IColumnTypes.TEXT :
				type = context.getType("String");
				break;
			case IColumnTypes.MEDIA :
				break;
		}
		return type;
	}


	protected interface IScopeTypeCreator
	{
		Type createType(ITypeInfoContext context, String fullTypeName);
	}

	public static IParameter[] getParameters(String key, Class< ? > scriptObjectClass, MemberBox member)
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
			int memberParamLength = member.getParameterTypes().length;
			if (memberParamLength < parameterNames.length)
			{
				boolean removeOptional = false;
				// if parameterNames bigger then the members parameter types and it is not a vararg, just get the first names.
				if (memberParamLength == 1 && member.getParameterTypes()[0].isArray())
				{
					parameters = new IParameter[parameterNames.length];
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
					parameters[i] = new ScriptParameter(name, null, optional, vararg);
				}
			}
			else if (memberParamLength == parameterNames.length)
			{
				parameters = new IParameter[memberParamLength];
				for (int i = 0; i < memberParamLength; i++)
				{
					Class< ? > paramClass = member.getParameterTypes()[i];
					String name = null;
					String type = null;
					if (parameterNames != null)
					{
						type = SolutionExplorerListContentProvider.TYPES.get(paramClass.getName());
						name = parameterNames[i];

						if (name.startsWith("[") && name.endsWith("]"))
						{
							name = name.substring(1, name.length() - 1);
						}
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
					parameters[i] = new ScriptParameter(name, type, false, false);
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
			IServoyModel servoyModel = ServoyModelFinder.getServoyModel();

			String name = resource.getProject().getName();
			//if (servoyModel.isActiveProject(name))
			{
				ServoyProject servoyProject = servoyModel.getServoyProject(name);
				if (servoyProject != null)
				{
					return servoyProject.getEditingFlattenedSolution();
				}
			}
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

	protected static Member createMethod(ITypeInfoContext context, ScriptMethod sm, ImageDescriptor image)
	{
		return createMethod(context, sm, image, null);
	}

	/**
	 * @param sm
	 * @return
	 */
	protected static Member createMethod(ITypeInfoContext context, ScriptMethod sm, ImageDescriptor image, String fileName)
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
		if (comment == null)
		{
			String declaration = sm.getDeclaration();
			int commentStart = declaration.indexOf("/**");
			if (commentStart != -1)
			{
				int commentEnd = declaration.indexOf("*/", commentStart);
				comment = declaration.substring(commentStart, commentEnd);
			}
		}
		if (comment != null)
		{
			if (comment.lastIndexOf("@deprecated") != -1)
			{
				method.setDeprecated(true);
			}

			method.setDescription(getParsedComment(comment));
		}
		if (image != null)
		{
			method.setAttribute(IMAGE_DESCRIPTOR, image);
		}
		if (fileName != null)
		{
			method.setAttribute(RESOURCE, fileName);
		}
		return method;
	}

	public static Property createProperty(ITypeInfoContext context, String name, boolean readonly, String typeName, String description, ImageDescriptor image)
	{
		return createProperty(context, name, readonly, typeName, description, image, null);
	}

	public static Property createProperty(ITypeInfoContext context, String name, boolean readonly, String typeName, String description, ImageDescriptor image,
		Object resource)
	{
		Type type = null;
		if (typeName != null)
		{
			type = context.getType(typeName);
		}
		return createProperty(name, readonly, type, description, image, resource);
	}


	public static Property createProperty(ITypeInfoContext context, String name, boolean readonly, String typeName, ImageDescriptor image)
	{
		Type type = null;
		if (typeName != null)
		{
			type = context.getType(typeName);
		}
		return createProperty(name, readonly, type, null, image);
	}

	public static Property createProperty(String name, boolean readonly, Type type, String description, ImageDescriptor image)
	{
		return createProperty(name, readonly, type, description, image, null);
	}

	public static Property createProperty(String name, boolean readonly, Type type, String description, ImageDescriptor image, Object resource)
	{
		Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
		property.setName(name);
		property.setReadOnly(readonly);
		if (description != null)
		{
			property.setDescription(description);
		}
		if (type != null)
		{
			property.setType(type);
		}
		if (image != null)
		{
			property.setAttribute(IMAGE_DESCRIPTOR, image);
		}
		else if (type != null && type.getAttribute(IMAGE_DESCRIPTOR) != null)
		{
			property.setAttribute(IMAGE_DESCRIPTOR, type.getAttribute(IMAGE_DESCRIPTOR));
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

	private static final ConcurrentMap<Pair<Class< ? >, String>, String> docCache = new ConcurrentHashMap<Pair<Class< ? >, String>, String>(64, 0.9f, 16);

	/**
	 * @param key
	 * @param scriptObject
	 * @param name
	 * @return
	 */
	public static String getDoc(String key, Class< ? > scriptObjectClass, String name, Class[] parameterTypes)
	{
		if (scriptObjectClass == null) return null;

		Pair<Class< ? >, String> cacheKey = new Pair<Class< ? >, String>(scriptObjectClass, name);
		String doc = docCache.get(cacheKey);
		if (doc == null)
		{
			doc = key;
			IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
			if (scriptObject != null)
			{
				String sample = null;
				if (scriptObject instanceof ITypedScriptObject)
				{
					String toolTip = ((ITypedScriptObject)scriptObject).getToolTip(key, parameterTypes);
					if (toolTip != null) doc = toolTip;
					sample = ((ITypedScriptObject)scriptObject).getSample(key, parameterTypes);
				}
				else
				{
					String toolTip = scriptObject.getToolTip(name);
					if (toolTip != null) doc = toolTip;
					sample = scriptObject.getSample(key);
				}
				if (sample != null)
				{
					doc = doc + "<br/>" + HtmlUtils.escapeMarkup(sample); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (doc != null)
				{
					doc = Utils.stringReplace(doc, "\n", "<br/>"); //$NON-NLS-1$ //$NON-NLS-2$
					doc = Utils.stringReplace(doc, "%%prefix%%", ""); //$NON-NLS-1$ //$NON-NLS-2$
					doc = Utils.stringReplace(doc, "%%elementName%%", name); //$NON-NLS-1$
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
		else if (object instanceof BeanProperty)
		{
			returnType = ((BeanProperty)object).getGetter().getReturnType();
		}
		return getReturnType(returnType);
	}

	/**
	 * @param returnType
	 */
	private static Class< ? > getReturnType(Class< ? > returnType)
	{
		if (returnType == null || returnType.isArray()) return returnType;
		if (!returnType.isAssignableFrom(Void.class) && !returnType.isAssignableFrom(void.class))
		{
			if (returnType.isAssignableFrom(Record.class))
			{
				return Record.class;
			}
			else if (returnType.isAssignableFrom(FoundSet.class))
			{
				return FoundSet.class;
			}
			else if (returnType.isPrimitive() || Number.class.isAssignableFrom(returnType))
			{
				if (returnType.isAssignableFrom(boolean.class)) return Boolean.class;
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
}