/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.designer.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.servoy.eclipse.developersolution.DeveloperNGClient;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.scripting.solutionmodel.IJSParent;
import com.servoy.j2db.scripting.solutionmodel.JSBean;
import com.servoy.j2db.scripting.solutionmodel.JSComponent;
import com.servoy.j2db.scripting.solutionmodel.JSField;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent;
import com.servoy.j2db.scripting.solutionmodel.JSPortal;
import com.servoy.j2db.scripting.solutionmodel.JSTabPanel;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * @author gabi
 *
 */
public class JSMethodCallInterceptor
{
	private static final JSMethodCallInterceptor instance = new JSMethodCallInterceptor();

	private static final Map<Class< ? >, Class< ? >> CLASS_MAP;
	static
	{
		Map<Class< ? >, Class< ? >> map = new HashMap<>();
		map.put(WebComponent.class, JSWebComponent.class);
		map.put(GraphicalComponent.class, JSGraphicalComponent.class);
		map.put(Bean.class, JSBean.class);
		map.put(Field.class, JSField.class);
		map.put(Portal.class, JSPortal.class);
		map.put(TabPanel.class, JSTabPanel.class);
		CLASS_MAP = Collections.unmodifiableMap(map);
	}

	private final Map<Class< ? >, Class< ? >> proxyClassComponent = new HashMap<>();
	private JSComponentMethodCallListener jsComponentMethodCallListener;

	private Class< ? extends JSForm> proxyClassForm;
	private JSFormMethodCallListener jsFormMethodCallListener;

	public static JSMethodCallInterceptor getInstance()
	{
		return instance;
	}


	public JSComponent createComponent(JSForm jsForm, BaseComponent component) throws Exception
	{
		JSComponent jsComponent = null;
		Class baseClass = component.getClass(), jsClass = CLASS_MAP.get(baseClass);

		if (jsClass != null)
		{
			Class proxyClass = proxyClassComponent.get(baseClass);
			if (proxyClass == null)
			{
				proxyClass = new ByteBuddy()
					.subclass(jsClass)
					.method(net.bytebuddy.matcher.ElementMatchers.any())
					.intercept(MethodDelegation.to(this))
					.make()
					.load(JSMethodCallInterceptor.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
					.getLoaded();
				proxyClassComponent.put(baseClass, proxyClass);
			}


			Constructor ctor;
			if (baseClass == Bean.class)
			{
				ctor = proxyClass.getDeclaredConstructor(
					IJSParent.class, baseClass, boolean.class);
				jsComponent = (JSComponent)ctor.newInstance(jsForm, component, true);
			}
			else
			{
				ctor = proxyClass.getDeclaredConstructor(
					IJSParent.class, baseClass, IApplication.class, boolean.class);
				jsComponent = (JSComponent)ctor.newInstance(jsForm, component, DeveloperNGClient.INSTANCE, true);
			}


		}

		return jsComponent;
	}


	public void setJSComponentMethodCallListener(JSComponentMethodCallListener listener)
	{
		this.jsComponentMethodCallListener = listener;
	}

	public JSForm createForm(Form form) throws Exception
	{
		if (proxyClassForm == null)
		{
			proxyClassForm = new ByteBuddy()
				.subclass(JSForm.class)
				.method(net.bytebuddy.matcher.ElementMatchers.any())
				.intercept(MethodDelegation.to(this))
				.make()
				.load(JSMethodCallInterceptor.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
				.getLoaded();
		}
		Constructor< ? extends JSForm> ctor = proxyClassForm.getDeclaredConstructor(
			IApplication.class, Form.class, boolean.class);
		JSForm jsForm = ctor.newInstance(DeveloperNGClient.INSTANCE, form, true);
		return jsForm;
	}

	public void setJSFormMethodCallListener(JSFormMethodCallListener listener)
	{
		this.jsFormMethodCallListener = listener;
	}

	@RuntimeType
	public Object intercept(@This Object targetInstance,
		@Origin Method method,
		@SuperCall Callable< ? > superMethod) throws Throwable
	{
		String methodName = method.getName();
		if (methodName.startsWith("set") ||
			methodName.startsWith("put") ||
			methodName.startsWith("new") ||
			methodName.startsWith("remove"))
		{
			if (targetInstance instanceof JSComponent)
			{
				if (jsComponentMethodCallListener != null) jsComponentMethodCallListener.onJSComponentMethodCall((JSComponent)targetInstance);
			}
			else if (targetInstance instanceof JSForm)
			{
				if (jsFormMethodCallListener != null) jsFormMethodCallListener.onJSFormMethodCall((JSForm)targetInstance);
			}
		}
		return superMethod.call(); // Call the original method on the target instance

	}

	public interface JSComponentMethodCallListener
	{
		void onJSComponentMethodCall(JSComponent component);
	}

	public interface JSFormMethodCallListener
	{
		void onJSFormMethodCall(JSForm form);
	}

}