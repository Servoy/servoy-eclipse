package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.servoy.eclipse.cypress.services.FormSpecGenerator;

public class FormSpecGeneratorTest
{
	@Test
	public void testFormSpecGenerator_isCreatable()
	{
		assertNotNull(FormSpecGenerator.class.getAnnotation(org.eclipse.e4.core.di.annotations.Creatable.class),
			"FormSpecGenerator must have @Creatable annotation");
	}

	@Test
	public void testFormSpecGenerator_hasGenerateSpecMethod() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getMethod("generateSpec", String.class),
			"FormSpecGenerator must have generateSpec(String) method");
	}

	@Test
	public void testFormSpecGenerator_hasOldSpecExistsMethod() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getMethod("oldSpecExists", String.class),
			"FormSpecGenerator must have oldSpecExists(String) method");
	}

	@Test
	public void testFormSpecGenerator_hasSpecExistsMethod() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getMethod("specExists", String.class, String.class),
			"FormSpecGenerator must have specExists(String) method");
	}

	@Test
	public void testFormSpecGenerator_generateSpecReturnType() throws NoSuchMethodException
	{
		assertEquals(String.class, FormSpecGenerator.class.getMethod("generateSpec", String.class).getReturnType(),
			"generateSpec must return String");
	}

	@Test
	public void testFormSpecGenerator_oldSpecExistsReturnType() throws NoSuchMethodException
	{
		assertEquals(boolean.class, FormSpecGenerator.class.getMethod("oldSpecExists", String.class).getReturnType(),
			"specExists must return boolean");
	}

	@Test
	public void testFormSpecGenerator_specExistsReturnType() throws NoSuchMethodException
	{
		assertEquals(boolean.class, FormSpecGenerator.class.getMethod("specExists", String.class, String.class).getReturnType(),
			"specExists must return boolean");
	}

	@Test
	public void testFormSpecGenerator_hasNoArgConstructor() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getConstructor(), "FormSpecGenerator must have a no-arg constructor");
	}

	@Test
	public void testFormSpecGenerator_canBeInstantiated()
	{
		FormSpecGenerator gen = new FormSpecGenerator();
		assertNotNull(gen, "FormSpecGenerator must be instantiable");
	}

	@Test
	public void testFormSpecGenerator_hasParseFrmFileMethod()
	{
		Method[] methods = FormSpecGenerator.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("parseFrmFile".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue(found, "FormSpecGenerator must have a parseFrmFile method");
	}

	@Test
	public void testFormSpecGenerator_hasGenerateCypressSpecContentMethod()
	{
		Method[] methods = FormSpecGenerator.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("generateCypressSpecContent".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue(found, "FormSpecGenerator must have a generateCypressSpecContent method");
	}

	@Test
	public void testFormSpecGenerator_hasGenerateSetupContentMethod()
	{
		Method[] methods = FormSpecGenerator.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("generateSetupContent".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue(found, "FormSpecGenerator must have a generateSetupContent method");
	}

	@Test
	public void testFormSpecGenerator_parseFrmFile_extractsDataSource() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);

		String frmContent = "{\n\"dataSource\":\"db:/servoy_test/people\",\n\"items\":[\n{\n\"name\":\"field1\",\n\"typeName\":\"bootstrapcomponents-textbox\",\n\"typeid\":47\n}\n],\n\"name\":\"testForm\",\n\"typeid\":3\n}";

		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "testForm");
		assertNotNull(metadata, "parseFrmFile must return non-null metadata");

		java.lang.reflect.Field dsField = metadata.getClass().getDeclaredField("dataSource");
		dsField.setAccessible(true);
		assertEquals("db:/servoy_test/people", dsField.get(metadata));
	}

	@Test
	public void testFormSpecGenerator_parseFrmFile_extractsElementNames() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);

		String frmContent = "{\n\"dataSource\":\"db:/test/t1\",\n\"items\":[\n" +
			"{\n\"name\":\"btn_save\",\n\"onActionMethodID\":\"abc\",\n\"typeid\":7\n},\n" +
			"{\n\"name\":\"name_field\",\n\"typeName\":\"bootstrapcomponents-textbox\",\n\"typeid\":47,\n\"json\":{\"dataProviderID\":\"name\"}\n}\n" +
			"],\n\"name\":\"myForm\",\n\"typeid\":3\n}";

		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");

		java.lang.reflect.Field elementsField = metadata.getClass().getDeclaredField("namedElements");
		elementsField.setAccessible(true);
		List< ? > elements = (List< ? >)elementsField.get(metadata);

		assertTrue(elements.size() >= 2, "parseFrmFile must extract at least 2 elements");
	}

	@Test
	public void testFormSpecGenerator_parseFrmFile_formNameNotInElements() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);

		String frmContent = "{\n\"dataSource\":\"db:/test/t1\",\n\"items\":[\n" + "{\n\"name\":\"field1\",\n\"typeid\":47\n}\n" +
			"],\n\"name\":\"myForm\",\n\"typeid\":3\n}";

		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");

		java.lang.reflect.Field elementsField = metadata.getClass().getDeclaredField("namedElements");
		elementsField.setAccessible(true);
		List< ? > elements = (List< ? >)elementsField.get(metadata);

		for (Object elem : elements)
		{
			java.lang.reflect.Field nameField = elem.getClass().getDeclaredField("name");
			nameField.setAccessible(true);
			String name = (String)nameField.get(elem);
			assertTrue(!"myForm".equals(name), "Form name 'myForm' should not appear as an element");
		}
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_containsSetUp() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSetupContent = FormSpecGenerator.class.getDeclaredMethod("generateSetupContent",
			parseFrmFile.getReturnType());
		generateSetupContent.setAccessible(true);

		String frmContent = "{\n\"dataSource\":\"db:/test/t1\",\n\"items\":[],\n\"name\":\"testForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "testForm");
		String setup = (String)generateSetupContent.invoke(new FormSpecGenerator(), metadata);

		assertTrue(setup.contains("function spec_setUp()"), "Generated setup must contain spec_setUp function");
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_containsTearDown() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSetupContent = FormSpecGenerator.class.getDeclaredMethod("generateSetupContent",
			parseFrmFile.getReturnType());
		generateSetupContent.setAccessible(true);

		String frmContent = "{\n\"dataSource\":\"db:/test/t1\",\n\"items\":[],\n\"name\":\"testForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "testForm");
		String setup = (String)generateSetupContent.invoke(new FormSpecGenerator(), metadata);

		assertTrue(setup.contains("function spec_tearDown()"), "Generated setup must contain spec_tearDown function");
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_containsUuid() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSetupContent = FormSpecGenerator.class.getDeclaredMethod("generateSetupContent",
			parseFrmFile.getReturnType());
		generateSetupContent.setAccessible(true);

		String frmContent = "{\n\"items\":[],\n\"name\":\"testForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "testForm");
		String setup = (String)generateSetupContent.invoke(new FormSpecGenerator(), metadata);

		assertTrue(setup.contains("@properties={typeid:24,uuid:\""),
			"Generated setup must contain @properties with uuid");
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_mentionsCypress() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSetupContent = FormSpecGenerator.class.getDeclaredMethod("generateSetupContent",
			parseFrmFile.getReturnType());
		generateSetupContent.setAccessible(true);

		String frmContent = "{\n\"items\":[],\n\"name\":\"testForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "testForm");
		String setup = (String)generateSetupContent.invoke(new FormSpecGenerator(), metadata);

		assertTrue(setup.contains("Cypress"), "Generated setup must mention Cypress");
	}

	// --- Cypress spec content generation tests ---

	@Test
	public void testFormSpecGenerator_generateCypressSpec_noPropertiesAnnotation() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent",
			parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[{\"name\":\"btn1\",\"typeid\":7,\"onActionMethodID\":\"x\"}],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue(!spec.contains("@properties"), "Generated spec must NOT contain @properties annotation");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesCyVisit() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent",
			parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue(spec.contains("cy.visit("), "Generated spec must use cy.visit()");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesCyGet() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent",
			parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[{\"name\":\"btn1\",\"typeid\":7,\"onActionMethodID\":\"x\"}],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue(spec.contains("cy.get('[data-cy=\"myForm.btn1\"]')"),
			"Generated spec must use cy.get() with data-cy selectors");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesDescribeAndIt() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent",
			parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[{\"name\":\"lbl1\",\"typeid\":7}],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue(spec.contains("describe('myForm"), "Generated spec must use describe()");
		assertTrue(spec.contains("it('"), "Generated spec must use it()");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesRelativeUrl() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent",
			parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue(spec.contains("?formpreview=myForm&svy_testmode=true"),
			"Generated spec must use relative URL with formpreview param");
		assertTrue(!spec.contains("http://localhost"), "Generated spec must NOT contain hardcoded localhost URL");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_checksErrorOverlay() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent",
			parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[{\"name\":\"lbl1\",\"typeid\":7}],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue(spec.contains(".svy-error, .error-overlay"), "Generated spec must check for error overlay");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_buttonsUseBeEnabled() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent",
			parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[{\"name\":\"btn_save\",\"onActionMethodID\":\"abc\",\"typeid\":7}],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue(spec.contains("buttons are clickable"), "Generated spec must have buttons are clickable test");
		assertTrue(spec.contains("'be.enabled'"), "Generated spec must check button is enabled");
	}

	@Test
	public void testFormSpecGenerator_hasGetOldSpecFilePathMethod() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getMethod("getOldSpecFilePath", String.class),
			"FormSpecGenerator must have getOldSpecFilePath(String) method");
	}

	@Test
	public void testFormSpecGenerator_hasGetSpecFilePathMethod() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getMethod("getSpecFilePath", String.class, String.class),
			"FormSpecGenerator must have getSpecFilePath(String) method");
	}

	@Test
	public void testFormSpecGenerator_hasGetFormSpecDirMethod() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getMethod("getFormSpecDir"),
			"FormSpecGenerator must have getFormSpecDir() method");
	}

	@Test
	public void testFormSpecGenerator_doesNotHaveGetFormsDirMethod()
	{
		// SVY-21171: getFormsDir() was renamed to getFormSpecDir() when the Cypress
		// form specs moved out of medias/tests to the workspace-relative cy-form dir.
		boolean found = false;
		for (Method m : FormSpecGenerator.class.getDeclaredMethods())
		{
			if ("getFormsDir".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue(!found, "FormSpecGenerator should no longer have getFormsDir() method (renamed to getFormSpecDir)");
	}

	@Test
	public void testFormSpecGenerator_doesNotHaveEnsureBuildpathExclusionMethod()
	{
		// SVY-21171: ensureBuildpathExclusion was removed because the moved specs
		// live outside the solution and no longer need a .buildpath exclusion.
		Method[] methods = FormSpecGenerator.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("ensureBuildpathExclusion".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue(!found, "FormSpecGenerator should no longer have ensureBuildpathExclusion method");
	}
}
