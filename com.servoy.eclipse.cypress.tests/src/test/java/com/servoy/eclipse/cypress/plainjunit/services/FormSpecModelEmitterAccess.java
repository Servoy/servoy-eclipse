package com.servoy.eclipse.cypress.plainjunit.services;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.servoy.eclipse.cypress.services.FormSpecModel;

/**
 * Reflection bridge to the package-private {@code FormSpecModelEmitter}. The tests live in a different package than the emitter, so the
 * package-private statics are reached by reflection (same technique as {@code FormSpecGeneratorTest}).
 */
final class FormSpecModelEmitterAccess
{
	private static final String EMITTER_CLASS = "com.servoy.eclipse.cypress.services.FormSpecModelEmitter";

	private FormSpecModelEmitterAccess()
	{
	}

	static String emit(String formUrl, FormSpecModel model)
	{
		return (String)call("emit", new Class< ? >[] { String.class, FormSpecModel.class }, formUrl, model);
	}

	static int countVisibilityAssertions(FormSpecModel model)
	{
		return (int)call("countVisibilityAssertions", new Class< ? >[] { FormSpecModel.class }, model);
	}

	static int countHandlerCases(FormSpecModel model)
	{
		return (int)call("countHandlerCases", new Class< ? >[] { FormSpecModel.class }, model);
	}

	private static Object call(String name, Class< ? >[] parameterTypes, Object... arguments)
	{
		try
		{
			Method method = Class.forName(EMITTER_CLASS).getDeclaredMethod(name, parameterTypes);
			method.setAccessible(true);
			return method.invoke(null, arguments);
		}
		catch (InvocationTargetException e)
		{
			if (e.getCause() instanceof RuntimeException runtime)
			{
				throw runtime;
			}
			throw new IllegalStateException(e.getCause());
		}
		catch (ReflectiveOperationException e)
		{
			throw new IllegalStateException("Cannot reach " + EMITTER_CLASS + "." + name, e);
		}
	}
}
