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
	public void testFormPreviewNGClientHasShutdownExistingMethod() throws Exception
	{
		Method method = com.servoy.eclipse.ngclient.startup.FormPreviewNGClient.class.getDeclaredMethod("shutdownExisting");
		assertNotNull("FormPreviewNGClient must have shutdownExisting method", method);
		assertTrue("shutdownExisting must be public",
			java.lang.reflect.Modifier.isPublic(method.getModifiers()));
		assertTrue("shutdownExisting must be static",
			java.lang.reflect.Modifier.isStatic(method.getModifiers()));
		assertTrue("shutdownExisting must return void",
			method.getReturnType() == void.class);
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
