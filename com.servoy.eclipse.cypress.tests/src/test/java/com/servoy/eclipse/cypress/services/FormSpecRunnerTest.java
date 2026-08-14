package com.servoy.eclipse.cypress.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Test;

public class FormSpecRunnerTest
{
	@Test
	public void testFormSpecRunner_isCreatable()
	{
		assertNotNull("FormSpecRunner must have @Creatable annotation",
			FormSpecRunner.class.getAnnotation(
				org.eclipse.e4.core.di.annotations.Creatable.class));
	}

	@Test
	public void testFormSpecRunner_hasRunFormCypressTestsMethod() throws NoSuchMethodException
	{
		assertNotNull("FormSpecRunner must have runFormCypressTests(String, boolean) method",
			FormSpecRunner.class.getMethod("runFormCypressTests", String.class, boolean.class));
	}

	@Test
	public void testFormSpecRunner_runFormCypressTestsReturnType() throws NoSuchMethodException
	{
		assertTrue("runFormCypressTests must return String",
			FormSpecRunner.class.getMethod("runFormCypressTests", String.class, boolean.class)
				.getReturnType() == String.class);
	}

	@Test
	public void testFormSpecRunner_runFormCypressTestsHasHeadlessParam() throws NoSuchMethodException
	{
		Method m = FormSpecRunner.class.getMethod("runFormCypressTests", String.class, boolean.class);
		assertEquals("Second param should be boolean for headless",
			boolean.class, m.getParameterTypes()[1]);
	}

	@Test
	public void testFormSpecRunner_hasEnsureCypressInstalledMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("ensureCypressInstalled".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must have ensureCypressInstalled method", found);
	}

	@Test
	public void testFormSpecRunner_hasEnsureCypressConfigMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("ensureCypressConfig".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must have ensureCypressConfig method", found);
	}

	@Test
	public void testFormSpecRunner_hasGetCypressDirMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("getCypressDir".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must have getCypressDir method", found);
	}

	@Test
	public void testFormSpecRunner_hasGetNodePathMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("getNodePath".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must have getNodePath method", found);
	}

	@Test
	public void testFormSpecRunner_canBeInstantiated()
	{
		FormSpecRunner runner = new FormSpecRunner();
		assertNotNull("FormSpecRunner should be instantiable", runner);
	}

	@Test
	public void testFormSpecRunner_hasRunE2ECypressTestsMethod() throws NoSuchMethodException
	{
		assertNotNull("FormSpecRunner must have runE2ECypressTests(String, boolean) method",
			FormSpecRunner.class.getMethod("runE2ECypressTests", String.class, boolean.class));
	}

	@Test
	public void testFormSpecRunner_runE2ECypressTestsReturnType() throws NoSuchMethodException
	{
		assertTrue("runE2ECypressTests must return String",
			FormSpecRunner.class.getMethod("runE2ECypressTests", String.class, boolean.class)
				.getReturnType() == String.class);
	}

	@Test
	public void testFormSpecRunner_runE2ECypressTestsHasHeadlessParam() throws NoSuchMethodException
	{
		Method m = FormSpecRunner.class.getMethod("runE2ECypressTests", String.class, boolean.class);
		assertEquals("Second param should be boolean for headless",
			boolean.class, m.getParameterTypes()[1]);
	}

	@Test
	public void testFormSpecRunner_doesNotHaveRunSpecMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("runSpec".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must NOT have runSpec method (renamed to runFormCypressTests)", !found);
	}

	@Test
	public void testFormSpecRunner_doesNotHaveRunE2ESpecMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("runE2ESpec".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must NOT have runE2ESpec method (renamed to runE2ECypressTests)", !found);
	}
}

