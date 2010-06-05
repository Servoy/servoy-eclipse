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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dlkt.javascript.dom.support.IDesignTimeDOMProvider;
import org.eclipse.dlkt.javascript.dom.support.IProposalHolder;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.javascript.typeinference.ReferenceFactory;
import org.eclipse.dltk.javascript.typeinference.IScriptableTypeProvider;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.JavaMembers.BeanProperty;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerListContentProvider;
import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.FormManager.HistoryProvider;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSI18N;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.JSUnitAssertFunctions;
import com.servoy.j2db.scripting.JSUtils;
import com.servoy.j2db.scripting.PluginScope;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.info.ARGUMENTS;
import com.servoy.j2db.scripting.info.JSMathScriptInfo;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

public class FormDomProvider implements IDesignTimeDOMProvider
{
	public static final ThreadLocal<ServoyProject> CURRENT_PROJECT = new ThreadLocal<ServoyProject>();

	/**
	 * @author jcompagner
	 * 
	 */
	private static class IdAndTimeStamp
	{
		IdAndTimeStamp(Object[] ids)
		{
			this.ids = ids;
		}

		final long cachedTime = System.currentTimeMillis();
		final Object[] ids;
	}

	private static List<String> filters = new ArrayList<String>();

	static
	{
		filters.add("_formname_"); //$NON-NLS-1$
		filters.add("form"); //$NON-NLS-1$
		filters.add("_methodname_"); //$NON-NLS-1$
		filters.add("isPrototypeOf"); //$NON-NLS-1$
		filters.add("constructor"); //$NON-NLS-1$
		filters.add("hasOwnProperty"); //$NON-NLS-1$
		filters.add("propertyIsEnumerable"); //$NON-NLS-1$
		filters.add("__lookupSetter__"); //$NON-NLS-1$
		filters.add("__lookupGetter__"); //$NON-NLS-1$
		filters.add("__defineSetter__"); //$NON-NLS-1$
		filters.add("__defineGetter__"); //$NON-NLS-1$
	}

	private FormsScope formsScope;
	private GlobalScope globalScope;

	private DefaultScope topLevelTableScope;
	private final Map<Scriptable, IdAndTimeStamp> cachedIds;
	private CustomTypeProvider customTypeProvider;

	public FormDomProvider()
	{
		cachedIds = new HashMap<Scriptable, IdAndTimeStamp>();

		IScriptableTypeProvider[] scriptTypeProviders = ReferenceFactory.getScriptTypeProviders();

		for (IScriptableTypeProvider scriptTypeProvider : scriptTypeProviders)
		{
			if (scriptTypeProvider instanceof CustomTypeProvider)
			{
				customTypeProvider = (CustomTypeProvider)scriptTypeProvider;
				break;
			}
		}

		Context cx = Context.enter();
		try
		{
			Scriptable toplevelScope = new ImporterTopLevel(cx);
			toplevelScope.setPrototype(null);
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_SERVOY_EXCEPTION, toplevelScope, new ServoyException(0));//to be able to map finals in JS

			toplevelScope.put(FoundSet.JS_FOUNDSET, toplevelScope, new Object());
			toplevelScope.put(Record.JS_RECORD, toplevelScope, new Object());
			toplevelScope.put("JSDataSet", toplevelScope, new Object()); //$NON-NLS-1$
			toplevelScope.put("Form", toplevelScope, new Object()); //$NON-NLS-1$

			topLevelTableScope = new SimpleScope(null);

			SimpleScope solutionScope = new SimpleScope(toplevelScope);
			solutionScope.put("currentcontroller", solutionScope, new ScriptObjectClassScope(solutionScope, JSForm.class, "currentcontroller")); //$NON-NLS-1$ //$NON-NLS-2$
			globalScope = new GlobalScope(solutionScope);
			solutionScope.put(ScriptVariable.GLOBAL_PREFIX, solutionScope, globalScope);

			topLevelTableScope.put(ScriptVariable.GLOBAL_PREFIX, topLevelTableScope, globalScope);

			//put FormManager as 'history' in top level scope
			Scriptable history = new ScriptObjectClassScope(toplevelScope, HistoryProvider.class, IExecutingEnviroment.TOPLEVEL_HISTORY);
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_HISTORY, toplevelScope, history);

			// plugin scope.
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_PLUGINS, toplevelScope, new PluginScope(toplevelScope, Activator.getDefault().getDesignClient()));
			List<IClientPlugin> lst = Activator.getDefault().getDesignClient().getPluginManager().getPlugins(IClientPlugin.class);
			for (IClientPlugin clientPlugin : lst)
			{
				try
				{
					registerConstantsForScriptObject(toplevelScope, clientPlugin.getScriptObject());
				}
				catch (Throwable e)
				{
					Debug.error("error registering constants for client plugin ", e); //$NON-NLS-1$
				}
			}


			topLevelTableScope.put(IExecutingEnviroment.TOPLEVEL_PLUGINS, topLevelTableScope, new PluginScope(toplevelScope,
				Activator.getDefault().getDesignClient()));

			//add application variable to toplevel scope
//			ScriptObjectRegistry.registerScriptObjectForClass(JSApplication.class, JSApplication.DEVELOPER_JS_INSTANCE);
			registerConstantsForScriptObject(toplevelScope, ScriptObjectRegistry.getScriptObjectForClass(JSApplication.class));
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_APPLICATION, toplevelScope, new ScriptObjectClassScope(toplevelScope, JSApplication.class,
				IExecutingEnviroment.TOPLEVEL_APPLICATION));
			topLevelTableScope.put(IExecutingEnviroment.TOPLEVEL_APPLICATION, topLevelTableScope, new ScriptObjectClassScope(topLevelTableScope,
				JSApplication.class, IExecutingEnviroment.TOPLEVEL_APPLICATION));

			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_JSUNIT, toplevelScope, new ScriptObjectClassScope(toplevelScope, JSUnitAssertFunctions.class,
				IExecutingEnviroment.TOPLEVEL_JSUNIT));

//			ScriptObjectRegistry.registerScriptObjectForClass(JSUtils.class, JSUtils.DEVELOPER_JS_INSTANCE);
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_UTILS, toplevelScope, new ScriptObjectClassScope(toplevelScope, JSUtils.class,
				IExecutingEnviroment.TOPLEVEL_UTILS));
			topLevelTableScope.put(IExecutingEnviroment.TOPLEVEL_UTILS, topLevelTableScope, new ScriptObjectClassScope(topLevelTableScope, JSUtils.class,
				IExecutingEnviroment.TOPLEVEL_UTILS));

//			ScriptObjectRegistry.registerScriptObjectForClass(JSSecurity.class, JSSecurity.DEVELOPER_JS_INSTANCE);
			registerConstantsForScriptObject(toplevelScope, ScriptObjectRegistry.getScriptObjectForClass(JSSecurity.class));
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_SECURITY, toplevelScope, new ScriptObjectClassScope(toplevelScope, JSSecurity.class,
				IExecutingEnviroment.TOPLEVEL_SECURITY));
			topLevelTableScope.put(IExecutingEnviroment.TOPLEVEL_SECURITY, topLevelTableScope, new ScriptObjectClassScope(topLevelTableScope, JSSecurity.class,
				IExecutingEnviroment.TOPLEVEL_SECURITY));

			Scriptable solutionModifier = new ScriptObjectClassScope(toplevelScope, JSSolutionModel.class, IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER);
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER, toplevelScope, solutionModifier);
			registerConstants(toplevelScope, JSSolutionModel.class);

			toplevelScope.put(ARGUMENTS.getArgPrefix(), toplevelScope, new ScriptObjectClassScope(toplevelScope, ARGUMENTS.class, ARGUMENTS.getArgPrefix()));

			//add application variable to toplevel scope
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER, toplevelScope, new ScriptObjectClassScope(toplevelScope, JSDatabaseManager.class,
				IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER));
			registerConstants(toplevelScope, JSDatabaseManager.class);


//			ScriptObjectRegistry.registerScriptObjectForClass(JSI18N.class, JSI18N.DEVELOPER_JS_INSTANCE);
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_I18N, toplevelScope, new ScriptObjectClassScope(toplevelScope, JSI18N.class,
				IExecutingEnviroment.TOPLEVEL_I18N));
			topLevelTableScope.put(IExecutingEnviroment.TOPLEVEL_I18N, topLevelTableScope, new ScriptObjectClassScope(topLevelTableScope, JSI18N.class,
				IExecutingEnviroment.TOPLEVEL_I18N));

			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_SERVOY_EXCEPTION, toplevelScope, new ScriptObjectClassScope(toplevelScope, ServoyException.class,
				IExecutingEnviroment.TOPLEVEL_SERVOY_EXCEPTION, true));
			registerConstants(toplevelScope, ServoyException.class);
			if (customTypeProvider != null) customTypeProvider.addType(IExecutingEnviroment.TOPLEVEL_SERVOY_EXCEPTION, ServoyException.class);

			formsScope = new FormsScope(solutionScope);
			formsScope.setPrototype(null);

			solutionScope.put(IExecutingEnviroment.TOPLEVEL_FORMS, solutionScope, formsScope);
		}
		catch (Exception ex)
		{
			Debug.error("ScriptEngine init not completely successful ", ex); //$NON-NLS-1$
		}
		finally
		{
			Context.exit();
		}
	}

	private void registerConstants(Scriptable toplevelScope, Class< ? > cls) throws InstantiationException, IllegalAccessException
	{
		IScriptObject scriptObjectForClass = ScriptObjectRegistry.getScriptObjectForClass(cls);
		registerConstantsForScriptObject(toplevelScope, scriptObjectForClass);
	}

	private void registerConstantsForScriptObject(Scriptable toplevelScope, IReturnedTypesProvider scriptObject) throws InstantiationException,
		IllegalAccessException
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
					IPrefixedConstantsObject constants = (IPrefixedConstantsObject)element.newInstance();
					toplevelScope.put(constants.getPrefix(), toplevelScope, new ScriptObjectClassScope(toplevelScope, element, constants.getPrefix(), true));
					if (customTypeProvider != null) customTypeProvider.addType(constants.getPrefix(), element);
				}
				else if (IConstantsObject.class.isAssignableFrom(element))
				{
					toplevelScope.put(element.getSimpleName(), toplevelScope, new ScriptObjectClassScope(toplevelScope, element, element.getSimpleName(), true));
					if (customTypeProvider != null) customTypeProvider.addType(element.getSimpleName(), element);
				}
				else if (IJavaScriptType.class.isAssignableFrom(element))
				{
					toplevelScope.put(element.getSimpleName(), toplevelScope, new Object());
					if (customTypeProvider != null) customTypeProvider.addType(element.getSimpleName(), element);
				}
			}
		}
	}

	private final WeakHashMap<ISourceModule, IProject> projectCache = new WeakHashMap<ISourceModule, IProject>();

	/**
	 * 
	 * @param module
	 * @return true if this provider works for given source module
	 */
	public boolean canResolve(ISourceModule module)
	{
		try
		{
			if (module == null) return false;

			IProject project = projectCache.get(module);
			if (project == null)
			{
				if (module.getUnderlyingResource() == null) return false;

				project = module.getUnderlyingResource().getProject();
				projectCache.put(module, project);
			}

			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject servoyProject = servoyModel.getServoyProject(project.getName());

			if (servoyProject != null)
			{
				CURRENT_PROJECT.set(servoyProject);

				return true;
			}
		}
		catch (ModelException e)
		{
			ServoyLog.logError(e);
		}
		return false;
	}

	/**
	 * 
	 * @param module
	 * @return top level DOM object for given module
	 */
	public Scriptable resolveTopLevelScope(ISourceModule module)
	{
		try
		{
			if (module == null) return null;

			IProject project = projectCache.get(module);
			if (project == null)
			{
				if (module.getUnderlyingResource() == null) return null;

				project = module.getUnderlyingResource().getProject();
				projectCache.put(module, project);
			}

			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject servoyProject = servoyModel.getServoyProject(project.getName());

			if (servoyProject != null)
			{
				CURRENT_PROJECT.set(servoyProject);
				IPath path = module.getUnderlyingResource().getProjectRelativePath();
				String[] segments = path.segments();
				if (segments.length > 0)
				{
					if (segments[0].equals(SolutionSerializer.FORMS_DIR) && segments.length > 1)
					{
						String formName = segments[1];
						if (formName.endsWith(SolutionSerializer.JS_FILE_EXTENSION)) formName = formName.substring(0, formName.length() -
							SolutionSerializer.JS_FILE_EXTENSION.length());
						Object object = formsScope.get(formName, formsScope);
						if (object instanceof FormScope) return (FormScope)object;
					}
					else if (segments[0].equals(SolutionSerializer.GLOBALS_FILE))
					{
						return globalScope;
					}
					else if (segments.length == 3 && segments[0].equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
						segments[2].endsWith(SolutionSerializer.CALCULATIONS_POSTFIX_WITH_EXT))
					{
						String server = segments[1];
						String table = segments[2].substring(0, segments[2].length() - SolutionSerializer.CALCULATIONS_POSTFIX_WITH_EXT.length());
						IServer s = ServoyModel.getServerManager().getServer(server);
						if (s != null)
						{
							ITable t = s.getTable(table);
							if (t != null)
							{
								return new TableScope(topLevelTableScope, t);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	/**
	 * 
	 * @param module
	 * @return set of classes which are defined for given module
	 */
	public Class<Scriptable>[] resolveHostObjectClasses(ISourceModule module)
	{
		return null;
	}

	/**
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String,
	 *      java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException
	{
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IDesignTimeDOMProvider#filter(org.mozilla.javascript.Scriptable, java.lang.Object[])
	 */
	public Object[] resolveIds(Scriptable scope, String key)
	{
		if ("[]".equals(key)) //$NON-NLS-1$
		{
			return new Object[] { "[]" }; //$NON-NLS-1$
		}
		Object[] ids = null;
		if (key != null)
		{
			Object value = scope.get(key, scope);
			if (value == null || value == Scriptable.NOT_FOUND) return new Object[0];
			ids = new Object[] { key };
		}
		else
		{
			IdAndTimeStamp idAndTimeStamp = cachedIds.get(scope);
			if (idAndTimeStamp != null && System.currentTimeMillis() < (idAndTimeStamp.cachedTime + 5 * 1000))
			{
				ids = idAndTimeStamp.ids;
			}
			else
			{
				// if this scope was a cache time out and there are more then 100 in the cache just clear the cache.
				if (idAndTimeStamp != null && cachedIds.size() > 100) cachedIds.clear();
				if (scope instanceof ScriptableObject)
				{
					ids = ((ScriptableObject)scope).getAllIds();
				}
				else
				{
					ids = scope.getIds();
				}
				cachedIds.put(scope, new IdAndTimeStamp(ids));
			}
		}
		ArrayList<Object> filtered = new ArrayList<Object>();
		filtered.addAll(Arrays.asList(ids));
		filtered.removeAll(filters);
		if (scope instanceof Wrapper)
		{
			Object real = ((Wrapper)scope).unwrap();
			if (real instanceof IScriptObject)
			{
				IScriptObject scriptObject = (IScriptObject)real;
				for (Object element : ids)
				{
					if (scriptObject.isDeprecated((String)element))
					{
						filtered.remove(element);
					}
				}
			}
		}
		if (scope instanceof NativeJavaObject)
		{
			JavaMembers members = ScriptObjectRegistry.getJavaMembers(((NativeJavaObject)scope).unwrap().getClass(), null);
			if (members instanceof InstanceJavaMembers)
			{
				filtered.removeAll(((InstanceJavaMembers)members).getGettersAndSettersToHide());
			}
		}
		return filtered.toArray();
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IDesignTimeDOMProvider#getProposal(org.mozilla.javascript.Scriptable, java.lang.String)
	 */
	public IProposalHolder getProposal(Scriptable scope, String key)
	{
		if ("[]".equals(key)) //$NON-NLS-1$
		{
			if (scope instanceof FormsScope)
			{
				return new EmptyFormScope(scope);
			}
			return null;
		}

		Object object = null;
		Class< ? > scriptObjectClass = null;
		String name = ""; //$NON-NLS-1$
		if (scope instanceof IScriptObject)
		{
			IScriptObject scriptObject = (IScriptObject)scope;
			scriptObjectClass = scriptObject.getClass();
		}
		else if (scope instanceof NativeJavaObject)
		{
			Object real = ((NativeJavaObject)scope).unwrap();
			if (real instanceof IScriptObject)
			{
				IScriptObject scriptObject = (IScriptObject)real;
				scriptObjectClass = scriptObject.getClass();
				object = scope.get(key, scope);
			}
		}
		else if (scope instanceof ScriptObjectClassScope)
		{
			object = scope.get(key, scope);
			if (object instanceof IProposalHolder) return (IProposalHolder)object;
			name = ((ScriptObjectClassScope)scope).getName();
		}

		String sample = getDoc(key, scriptObjectClass, name);
		String[] parameterNames = getParameterNames(key, scriptObjectClass);
		Class< ? > returnType = getReturnType(object);
		if (returnType != null)
		{
			return new ScriptObjectClassScope(scope, returnType, key, parameterNames, sample, object instanceof NativeJavaMethod);
		}

		// special case for Math
		if (parameterNames == null && sample == null && ("parseInt".equals(key) || "parseFloat".equals(key) || "Math".equals(scope.getClassName()))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		{
			Object myObj = scope.get(key, scope);
			JSMathScriptInfo mathScriptInfo = new JSMathScriptInfo();

			ProposalHolder holder = new ProposalHolder(myObj, mathScriptInfo.getParameterNames(key), mathScriptInfo.getToolTip(key), false, null);
			return holder;
		}

		if (parameterNames != null || sample != null)
		{
			return new ProposalHolder(object, parameterNames, sample, object == null || object instanceof NativeJavaMethod, null);
		}
		return null;
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
		if (returnType != null && returnType != Object.class)
		{
			if (returnType.isAssignableFrom(Record.class))
			{
				returnType = Record.class;
			}
			else if (returnType.isAssignableFrom(FoundSet.class))
			{
				returnType = FoundSet.class;
			}
			JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(returnType, null);
			if (javaMembers != null)
			{
				return returnType;
			}
		}
		return null;
	}

	public static String getFormattedType(Class< ? > cls)
	{
		if (cls == null) return null;

		if (cls.isArray())
		{
			String componentType = getFormattedType(cls.getComponentType());
			if (componentType != null && !"Object".equals(componentType)) //$NON-NLS-1$
			{
				return componentType + "[]"; //$NON-NLS-1$ 
			}
			else
			{
				return "Array"; //$NON-NLS-1$
			}
		}
		else if (cls == int.class || cls == Integer.class || cls == Long.class || cls == long.class)
		{
			return "Integer"; //$NON-NLS-1$
		}
		else if (cls == float.class || cls == Float.class || cls == Double.class || cls == double.class)
		{
			return "Number"; //$NON-NLS-1$
		}
		else if (cls == boolean.class || cls == Boolean.class)
		{
			return "Boolean"; //$NON-NLS-1$
		}
		if (IFoundSet.class.isAssignableFrom(cls)) return FoundSet.JS_FOUNDSET;
		if (IRecord.class.isAssignableFrom(cls)) return Record.JS_RECORD;
		if (cls == JSForm.class) return "Controller"; //$NON-NLS-1$
		return cls.getSimpleName();
	}

	/**
	 * @param key
	 * @param scriptObject
	 * @param scriptObjectClass
	 * @return
	 */
	public static String[] getParameterNames(String key, Class< ? > scriptObjectClass)
	{
		if (scriptObjectClass == null) return null;
		IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
		String[] parameterNames = null;
		if (scriptObject != null)
		{
			parameterNames = scriptObject.getParameterNames(key);
		}

		if (parameterNames == null && scriptObjectClass != null)
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
				parameterNames = new String[selectedMethod.getParameterTypes().length];
				for (int i = 0; i < selectedMethod.getParameterTypes().length; i++)
				{
					Class< ? > paramClass = selectedMethod.getParameterTypes()[i];
					if (paramClass.isArray())
					{
						parameterNames[i] = SolutionExplorerListContentProvider.TYPES.get(paramClass.getComponentType().getName()) + "[]"; //$NON-NLS-1$
					}
					else
					{
						parameterNames[i] = SolutionExplorerListContentProvider.TYPES.get(paramClass.getName());
					}
				}
			}
		}
		return parameterNames;
	}

	/**
	 * @param key
	 * @param scriptObject
	 * @param name
	 * @return
	 */
	public static String getDoc(String key, Class< ? > scriptObjectClass, String name)
	{
		if (scriptObjectClass == null) return null;
		String doc = key;
		IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
		if (scriptObject != null)
		{
			String toolTip = scriptObject.getToolTip(key);
			if (toolTip != null)
			{
				doc = toolTip;
			}
			String sample = scriptObject.getSample(key);

			if (sample != null)
			{
				doc = doc + "\n<pre>" + HtmlUtils.escapeMarkup(sample) + "</pre>"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (doc != null)
			{
				doc = Utils.stringReplace(doc, "\n", "<br/>"); //$NON-NLS-1$ //$NON-NLS-2$
				doc = Utils.stringReplace(doc, "%%prefix%%", ""); //$NON-NLS-1$ //$NON-NLS-2$
				doc = Utils.stringReplace(doc, "%%elementName%%", name); //$NON-NLS-1$
				doc = "<html><body><font size='2'>" + doc + "</font></body></html>"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return doc;
	}
}
