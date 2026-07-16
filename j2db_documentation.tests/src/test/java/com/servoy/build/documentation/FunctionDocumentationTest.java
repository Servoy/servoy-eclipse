package com.servoy.build.documentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.j2db.documentation.IFunctionDocumentation;

@DisplayName("FunctionDocumentation")
class FunctionDocumentationTest
{
	private FunctionDocumentation createFunction(String name, Class<?>[] argTypes)
	{
		return new FunctionDocumentation(name, argTypes, IFunctionDocumentation.TYPE_FUNCTION, false, false,
			IFunctionDocumentation.STATE_DOCUMENTED);
	}

	@Nested
	@DisplayName("getJSTranslatedSignature with jsType")
	class JSTypeInSignature
	{
		@Test
		@DisplayName("uses jsType when set on parameter")
		void usesJsTypeWhenSet()
		{
			FunctionDocumentation func = createFunction("connectViaStreamableHTTP", new Class<?>[] { Map.class });
			ParameterDocumentation param = new ParameterDocumentation("headers", Map.class, "Object<String>",
				"any request headers", false);
			func.addArgument(param);

			String signature = func.getFullJSTranslatedSignature(false, true);

			assertNotNull(signature);
			assertTrue(signature.contains("Object<String>"), "Signature should contain 'Object<String>' but was: " + signature);
		}

		@Test
		@DisplayName("signature includes parameter jsType for multiple parameters")
		void multipleParamsWithJsType()
		{
			FunctionDocumentation func = createFunction("doSomething", new Class<?>[] { String.class, Map.class });
			ParameterDocumentation param1 = new ParameterDocumentation("name", String.class, null, "the name", false);
			ParameterDocumentation param2 = new ParameterDocumentation("headers", Map.class, "Object<String>",
				"any request headers", false);
			func.addArgument(param1);
			func.addArgument(param2);

			String signature = func.getFullJSTranslatedSignature(false, true);

			assertNotNull(signature);
			assertTrue(signature.contains("Object<String>"), "Should contain jsType 'Object<String>' but was: " + signature);
		}

		@Test
		@DisplayName("uses jsType with names and types")
		void usesJsTypeWithNamesAndTypes()
		{
			FunctionDocumentation func = createFunction("connectViaSTDIO", new Class<?>[] { Map.class });
			ParameterDocumentation param = new ParameterDocumentation("environment", Map.class, "Object<String>",
				"environment variables", false);
			func.addArgument(param);

			String signature = func.getFullJSTranslatedSignature(true, true);

			assertNotNull(signature);
			assertTrue(signature.contains("environment:Object<String>"),
				"Signature should contain 'environment:Object<String>' but was: " + signature);
		}

		@Test
		@DisplayName("optional parameter with jsType includes brackets")
		void optionalParamWithJsType()
		{
			FunctionDocumentation func = createFunction("connect", new Class<?>[] { Map.class });
			ParameterDocumentation param = new ParameterDocumentation("headers", Map.class, "Object<String>",
				"optional headers", true);
			func.addArgument(param);

			String signature = func.getFullJSTranslatedSignature(false, true);

			assertNotNull(signature);
			assertTrue(signature.contains("[Object<String>]"),
				"Signature should contain '[Object<String>]' but was: " + signature);
		}
	}

	@Nested
	@DisplayName("getJSTranslatedSignature fallback to class-based type")
	class FallbackToClassType
	{
		@ParameterizedTest
		@NullAndEmptySource
		@DisplayName("falls back to class type when jsType is null or empty")
		void fallsBackWhenJsTypeNullOrEmpty(String jsType)
		{
			FunctionDocumentation func = createFunction("doSomething", new Class<?>[] { String.class });
			ParameterDocumentation param = new ParameterDocumentation("name", String.class, jsType, "the name", false);
			func.addArgument(param);

			String signature = func.getFullJSTranslatedSignature(false, true);

			assertNotNull(signature);
			assertTrue(signature.contains("String"), "Signature should contain 'String' but was: " + signature);
		}

		@Test
		@DisplayName("returns null when parameter type is null")
		void returnsNullWhenParamTypeNull()
		{
			FunctionDocumentation func = createFunction("doSomething", new Class<?>[] { null });
			ParameterDocumentation param = new ParameterDocumentation("arg", null, null, "desc", false);
			func.addArgument(param);

			String signature = func.getFullJSTranslatedSignature(false, true);

			assertNull(signature);
		}
	}

	@Nested
	@DisplayName("getJSTranslatedSignature preserves other behaviors")
	class PreservesOtherBehaviors
	{
		@Test
		@DisplayName("varargs parameter still uses component type with ellipsis")
		void varargsStillWork()
		{
			FunctionDocumentation func = new FunctionDocumentation("doSomething", new Class<?>[] { String[].class },
				IFunctionDocumentation.TYPE_FUNCTION, false, true, IFunctionDocumentation.STATE_DOCUMENTED);
			ParameterDocumentation param = new ParameterDocumentation("args", String[].class, "Object<String>",
				"varargs", false);
			func.addArgument(param);

			String signature = func.getFullJSTranslatedSignature(false, true);

			assertNotNull(signature);
			assertTrue(signature.contains("..."), "Varargs should contain '...' but was: " + signature);
		}

		@Test
		@DisplayName("property type does not generate parentheses")
		void propertyTypeNoParens()
		{
			FunctionDocumentation func = new FunctionDocumentation("myProp", new Class<?>[] {},
				IFunctionDocumentation.TYPE_PROPERTY, false, false, IFunctionDocumentation.STATE_DOCUMENTED);

			String signature = func.getFullJSTranslatedSignature(false, true);

			assertNotNull(signature);
			assertEquals("myProp", signature);
		}
	}
}
