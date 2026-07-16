package com.servoy.eclipse.docgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.eclipse.docgenerator.metamodel.MetaModelHolder;

@DisplayName("DocumentedParameterData")
class DocumentedParameterDataTest
{
	private MetaModelHolder holder;
	private TypeMapper typeMapper;

	@BeforeEach
	void setUp()
	{
		holder = new MetaModelHolder();
		typeMapper = new TypeMapper(false);
	}

	@Nested
	@DisplayName("checkIfHasType with {Type<Generic>} pattern")
	class GenericTypePattern
	{
		@Test
		@DisplayName("extracts Object<String> from {Object<String>} description")
		void extractsObjectString()
		{
			DocumentedParameterData param = new DocumentedParameterData("headers", false,
				"{Object<String>} any request headers");

			param.checkIfHasType(holder, typeMapper);

			assertEquals("Object<String>", param.getJSType());
			assertEquals("any request headers", param.getDescription());
		}

		@Test
		@DisplayName("extracts Array<Number> from {Array<Number>} description")
		void extractsArrayNumber()
		{
			DocumentedParameterData param = new DocumentedParameterData("values", false,
				"{Array<Number>} list of values");

			param.checkIfHasType(holder, typeMapper);

			assertEquals("Array<Number>", param.getJSType());
			assertEquals("list of values", param.getDescription());
		}

		@Test
		@DisplayName("extracts CustomType<Param> from {CustomType<Param>} description")
		void extractsCustomGenericType()
		{
			DocumentedParameterData param = new DocumentedParameterData("data", false,
				"{CustomType<Param>} the data object");

			param.checkIfHasType(holder, typeMapper);

			assertEquals("CustomType<Param>", param.getJSType());
			assertEquals("the data object", param.getDescription());
		}

		@ParameterizedTest
		@CsvSource({
			"'{Object<String>} desc', 'Object<String>', 'desc'",
			"'{Object<String>}', 'Object<String>', ''",
			"'{Map<String>} some map data', 'Map<String>', 'some map data'"
		})
		@DisplayName("handles various {Type<Generic>} patterns")
		void handlesVariousPatterns(String description, String expectedJsType, String expectedDesc)
		{
			DocumentedParameterData param = new DocumentedParameterData("param", false, description);

			param.checkIfHasType(holder, typeMapper);

			assertEquals(expectedJsType, param.getJSType());
			assertEquals(expectedDesc, param.getDescription());
		}
	}

	@Nested
	@DisplayName("checkIfHasType without generic pattern")
	class NoGenericPattern
	{
		@Test
		@DisplayName("does not set jsType for plain type description")
		void noJsTypeForPlainDescription()
		{
			DocumentedParameterData param = new DocumentedParameterData("name", false,
				"the name of the entity");

			param.checkIfHasType(holder, typeMapper);

			assertNull(param.getJSType());
		}

		@Test
		@DisplayName("does not set jsType for braces without angle brackets")
		void noJsTypeForBracesWithoutAngleBrackets()
		{
			DocumentedParameterData param = new DocumentedParameterData("name", false,
				"{String} the name");

			param.checkIfHasType(holder, typeMapper);

			assertNull(param.getJSType());
		}

		@Test
		@DisplayName("does not set jsType when description is null")
		void noJsTypeForNullDescription()
		{
			DocumentedParameterData param = new DocumentedParameterData("name", false, null);

			param.checkIfHasType(holder, typeMapper);

			assertNull(param.getJSType());
		}

		@Test
		@DisplayName("does not set jsType when description is empty")
		void noJsTypeForEmptyDescription()
		{
			DocumentedParameterData param = new DocumentedParameterData("name", false, "");

			param.checkIfHasType(holder, typeMapper);

			assertNull(param.getJSType());
		}

		@ParameterizedTest
		@ValueSource(strings = {
			"Object<String> without braces",
			"<String> no type before angle",
			"{Object} no angle brackets",
			"just a regular description"
		})
		@DisplayName("does not set jsType for non-matching patterns")
		void noJsTypeForNonMatchingPatterns(String description)
		{
			DocumentedParameterData param = new DocumentedParameterData("param", false, description);

			param.checkIfHasType(holder, typeMapper);

			assertNull(param.getJSType());
		}
	}

	@Nested
	@DisplayName("checkIfHasType idempotency")
	class Idempotency
	{
		@Test
		@DisplayName("calling checkIfHasType twice does not change result")
		void callingTwiceIsSafe()
		{
			DocumentedParameterData param = new DocumentedParameterData("headers", false,
				"{Object<String>} any request headers");

			param.checkIfHasType(holder, typeMapper);
			String firstJsType = param.getJSType();
			String firstDesc = param.getDescription();

			param.checkIfHasType(holder, typeMapper);

			assertEquals(firstJsType, param.getJSType());
			assertEquals(firstDesc, param.getDescription());
		}
	}

	@Nested
	@DisplayName("checkIfHasType description stripping")
	class DescriptionStripping
	{
		@Test
		@DisplayName("strips {Object<String>} prefix and trims remaining description")
		void stripsAndTrims()
		{
			DocumentedParameterData param = new DocumentedParameterData("headers", false,
				"{Object<String>}   extra spaces before desc");

			param.checkIfHasType(holder, typeMapper);

			assertEquals("Object<String>", param.getJSType());
			assertEquals("extra spaces before desc", param.getDescription());
		}

		@Test
		@DisplayName("handles description that is only the type token")
		void onlyTypeToken()
		{
			DocumentedParameterData param = new DocumentedParameterData("headers", false,
				"{Object<String>}");

			param.checkIfHasType(holder, typeMapper);

			assertEquals("Object<String>", param.getJSType());
			assertEquals("", param.getDescription());
		}
	}

	@Nested
	@DisplayName("toXML includes jstype attribute when set")
	class ToXml
	{
		@Test
		@DisplayName("jsType is accessible after checkIfHasType")
		void jsTypeAccessibleAfterCheck()
		{
			DocumentedParameterData param = new DocumentedParameterData("headers", false,
				"{Object<String>} any request headers");

			param.checkIfHasType(holder, typeMapper);

			assertNotNull(param.getJSType());
			assertEquals("Object<String>", param.getJSType());
		}

		@Test
		@DisplayName("setJSType directly works")
		void setJsTypeDirectly()
		{
			DocumentedParameterData param = new DocumentedParameterData("headers", false, "some desc");

			param.setJSType("Object<String>");

			assertEquals("Object<String>", param.getJSType());
		}
	}
}
