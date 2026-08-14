package com.servoy.eclipse.cypress.services;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Test;

public class FormSpecRunnerShutdownTest
{
	@Test
	public void testHasShutdownFormPreviewClientsMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("shutdownFormPreviewClients".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must have shutdownFormPreviewClients method", found);
	}

	@Test
	public void testShutdownFormPreviewClientsIsPrivate()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		for (Method m : methods)
		{
			if ("shutdownFormPreviewClients".equals(m.getName()))
			{
				assertTrue("shutdownFormPreviewClients must be private",
					java.lang.reflect.Modifier.isPrivate(m.getModifiers()));
				return;
			}
		}
		assertTrue("shutdownFormPreviewClients method not found", false);
	}

	@Test
	public void testShutdownFormPreviewClientsReturnsVoid()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		for (Method m : methods)
		{
			if ("shutdownFormPreviewClients".equals(m.getName()))
			{
				assertTrue("shutdownFormPreviewClients must return void",
					m.getReturnType() == void.class);
				return;
			}
		}
		assertTrue("shutdownFormPreviewClients method not found", false);
	}

	@Test
	public void testShutdownFormPreviewClientsHasNoParams()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		for (Method m : methods)
		{
			if ("shutdownFormPreviewClients".equals(m.getName()))
			{
				assertTrue("shutdownFormPreviewClients must have zero parameters",
					m.getParameterCount() == 0);
				return;
			}
		}
		assertTrue("shutdownFormPreviewClients method not found", false);
	}

	@Test
	public void testFormPreviewUserUidConstant() throws Exception
	{
		java.lang.reflect.Field field = FormSpecRunner.class.getDeclaredField("FORMPREVIEW_USER_UID");
		field.setAccessible(true);
		assertNotNull("FORMPREVIEW_USER_UID constant must exist", field);
		assertTrue("FORMPREVIEW_USER_UID must be static",
			java.lang.reflect.Modifier.isStatic(field.getModifiers()));
		assertTrue("FORMPREVIEW_USER_UID must be final",
			java.lang.reflect.Modifier.isFinal(field.getModifiers()));
		Object value = field.get(null);
		assertTrue("FORMPREVIEW_USER_UID must be 'formpreview_user'",
			"formpreview_user".equals(value));
	}

	@Test
	public void testShutdownFormPreviewClientsDoesNotThrowWithNoServer()
	{
		FormSpecRunner runner = new FormSpecRunner();
		try
		{
			Method m = FormSpecRunner.class.getDeclaredMethod("shutdownFormPreviewClients");
			m.setAccessible(true);
			m.invoke(runner);
		}
		catch (java.lang.reflect.InvocationTargetException e)
		{
			assertTrue("shutdownFormPreviewClients should not throw even without a server: " + e.getCause(),
				false);
		}
		catch (Exception e)
		{
			assertTrue("Expected graceful handling: " + e.getMessage(), true);
		}
	}
}
