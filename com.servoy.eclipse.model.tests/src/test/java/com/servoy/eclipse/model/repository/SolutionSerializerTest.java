package com.servoy.eclipse.model.repository;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.util.UUID;

public class SolutionSerializerTest
{
	private Method generateParamsMethod;

	@Before
	public void setUp() throws Exception
	{
		generateParamsMethod = SolutionSerializer.class.getDeclaredMethod("generateParams", StringBuilder.class,
			AbstractScriptProvider.class);
		generateParamsMethod.setAccessible(true);
	}

	@Test
	public void testReturnsFalseForNull() throws Exception
	{
		StringBuilder sb = new StringBuilder();
		boolean result = (boolean)generateParamsMethod.invoke(null, sb, null);
		assertFalse(result);
	}

	@Test
	public void testDoesNotThrowForScriptCalculation() throws Exception
	{
		ScriptCalculation calc = createScriptCalculation();
		StringBuilder sb = new StringBuilder();
		generateParamsMethod.invoke(null, sb, calc);
	}

	@Test
	public void testReturnsFalseForCalcWithNoArgs() throws Exception
	{
		ScriptCalculation calc = createScriptCalculation();
		StringBuilder sb = new StringBuilder();
		boolean result = (boolean)generateParamsMethod.invoke(null, sb, calc);
		assertFalse(result);
	}

	@Test
	public void testGeneratesParamsForCalcWithArgs() throws Exception
	{
		ScriptCalculation calc = createScriptCalculation();
		MethodArgument arg = new MethodArgument("myParam", null, null);
		calc.setRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS, new MethodArgument[] { arg });

		StringBuilder sb = new StringBuilder();
		boolean result = (boolean)generateParamsMethod.invoke(null, sb, calc);
		assertTrue(result);
		assertTrue(sb.toString().contains("@param"));
		assertTrue(sb.toString().contains("myParam"));
	}

	@Test
	public void testGeneratesParamsForScriptMethod() throws Exception
	{
		ScriptMethod method = createScriptMethod();
		MethodArgument arg = new MethodArgument("testParam", null, null);
		method.setRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS, new MethodArgument[] { arg });

		StringBuilder sb = new StringBuilder();
		boolean result = (boolean)generateParamsMethod.invoke(null, sb, method);
		assertTrue(result);
		assertTrue(sb.toString().contains("@param"));
		assertTrue(sb.toString().contains("testParam"));
	}

	@Test
	public void testReturnsFalseForScriptMethodWithNoArgs() throws Exception
	{
		ScriptMethod method = createScriptMethod();
		StringBuilder sb = new StringBuilder();
		boolean result = (boolean)generateParamsMethod.invoke(null, sb, method);
		assertFalse(result);
	}

	@SuppressWarnings("unchecked")
	private ScriptCalculation createScriptCalculation() throws Exception
	{
		Constructor<?>[] ctors = ScriptCalculation.class.getDeclaredConstructors();
		Constructor<ScriptCalculation> ctor = (Constructor<ScriptCalculation>)ctors[0];
		ctor.setAccessible(true);
		return ctor.newInstance((Object)null, UUID.randomUUID());
	}

	@SuppressWarnings("unchecked")
	private ScriptMethod createScriptMethod() throws Exception
	{
		Constructor<?>[] ctors = ScriptMethod.class.getDeclaredConstructors();
		Constructor<ScriptMethod> ctor = (Constructor<ScriptMethod>)ctors[0];
		ctor.setAccessible(true);
		return ctor.newInstance((Object)null, UUID.randomUUID());
	}
}
