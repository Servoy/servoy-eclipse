package com.servoy.eclipse.cypress.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

public class FormSpecGeneratorTest
{
	@Test
	public void testFormSpecGenerator_isCreatable()
	{
		assertNotNull("FormSpecGenerator must have @Creatable annotation",
			FormSpecGenerator.class.getAnnotation(
				org.eclipse.e4.core.di.annotations.Creatable.class));
	}

	@Test
	public void testFormSpecGenerator_hasGenerateSpecMethod() throws NoSuchMethodException
	{
		assertNotNull("FormSpecGenerator must have generateSpec(String) method",
			FormSpecGenerator.class.getMethod("generateSpec", String.class));
	}

	@Test
	public void testFormSpecGenerator_hasSpecExistsMethod() throws NoSuchMethodException
	{
		assertNotNull("FormSpecGenerator must have specExists(String) method",
			FormSpecGenerator.class.getMethod("specExists", String.class));
	}

	@Test
	public void testFormSpecGenerator_generateSpecReturnType() throws NoSuchMethodException
	{
		assertEquals("generateSpec must return String", String.class,
			FormSpecGenerator.class.getMethod("generateSpec", String.class).getReturnType());
	}

	@Test
	public void testFormSpecGenerator_specExistsReturnType() throws NoSuchMethodException
	{
		assertEquals("specExists must return boolean", boolean.class,
			FormSpecGenerator.class.getMethod("specExists", String.class).getReturnType());
	}

	@Test
	public void testFormSpecGenerator_hasNoArgConstructor() throws NoSuchMethodException
	{
		assertNotNull("FormSpecGenerator must have a no-arg constructor",
			FormSpecGenerator.class.getConstructor());
	}

	@Test
	public void testFormSpecGenerator_canBeInstantiated()
	{
		FormSpecGenerator gen = new FormSpecGenerator();
		assertNotNull("FormSpecGenerator must be instantiable", gen);
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
		assertTrue("FormSpecGenerator must have a parseFrmFile method", found);
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
		assertTrue("FormSpecGenerator must have a generateCypressSpecContent method", found);
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
		assertTrue("FormSpecGenerator must have a generateSetupContent method", found);
	}

	@Test
	public void testFormSpecGenerator_parseFrmFile_extractsDataSource() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);

		String frmContent = "{\n\"dataSource\":\"db:/servoy_test/people\",\n\"items\":[\n{\n\"name\":\"field1\",\n\"typeName\":\"bootstrapcomponents-textbox\",\n\"typeid\":47\n}\n],\n\"name\":\"testForm\",\n\"typeid\":3\n}";

		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "testForm");
		assertNotNull("parseFrmFile must return non-null metadata", metadata);

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
		List<?> elements = (List<?>)elementsField.get(metadata);

		assertTrue("parseFrmFile must extract at least 2 elements", elements.size() >= 2);
	}

	@Test
	public void testFormSpecGenerator_parseFrmFile_formNameNotInElements() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);

		String frmContent = "{\n\"dataSource\":\"db:/test/t1\",\n\"items\":[\n" +
			"{\n\"name\":\"field1\",\n\"typeid\":47\n}\n" +
			"],\n\"name\":\"myForm\",\n\"typeid\":3\n}";

		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");

		java.lang.reflect.Field elementsField = metadata.getClass().getDeclaredField("namedElements");
		elementsField.setAccessible(true);
		List<?> elements = (List<?>)elementsField.get(metadata);

		for (Object elem : elements)
		{
			java.lang.reflect.Field nameField = elem.getClass().getDeclaredField("name");
			nameField.setAccessible(true);
			String name = (String)nameField.get(elem);
			assertTrue("Form name 'myForm' should not appear as an element", !"myForm".equals(name));
		}
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_containsSetUp() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSetupContent = FormSpecGenerator.class.getDeclaredMethod("generateSetupContent", parseFrmFile.getReturnType());
		generateSetupContent.setAccessible(true);

		String frmContent = "{\n\"dataSource\":\"db:/test/t1\",\n\"items\":[],\n\"name\":\"testForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "testForm");
		String setup = (String)generateSetupContent.invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated setup must contain spec_setUp function",
			setup.contains("function spec_setUp()"));
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_containsTearDown() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSetupContent = FormSpecGenerator.class.getDeclaredMethod("generateSetupContent", parseFrmFile.getReturnType());
		generateSetupContent.setAccessible(true);

		String frmContent = "{\n\"dataSource\":\"db:/test/t1\",\n\"items\":[],\n\"name\":\"testForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "testForm");
		String setup = (String)generateSetupContent.invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated setup must contain spec_tearDown function",
			setup.contains("function spec_tearDown()"));
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_containsUuid() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSetupContent = FormSpecGenerator.class.getDeclaredMethod("generateSetupContent", parseFrmFile.getReturnType());
		generateSetupContent.setAccessible(true);

		String frmContent = "{\n\"items\":[],\n\"name\":\"testForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "testForm");
		String setup = (String)generateSetupContent.invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated setup must contain @properties with uuid",
			setup.contains("@properties={typeid:24,uuid:\""));
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_mentionsCypress() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSetupContent = FormSpecGenerator.class.getDeclaredMethod("generateSetupContent", parseFrmFile.getReturnType());
		generateSetupContent.setAccessible(true);

		String frmContent = "{\n\"items\":[],\n\"name\":\"testForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "testForm");
		String setup = (String)generateSetupContent.invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated setup must mention Cypress",
			setup.contains("Cypress"));
	}

	// --- Cypress spec content generation tests ---

	@Test
	public void testFormSpecGenerator_generateCypressSpec_noPropertiesAnnotation() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent", parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[{\"name\":\"btn1\",\"typeid\":7,\"onActionMethodID\":\"x\"}],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must NOT contain @properties annotation",
			!spec.contains("@properties"));
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesCyVisit() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent", parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must use cy.visit()",
			spec.contains("cy.visit("));
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesCyGet() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent", parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[{\"name\":\"btn1\",\"typeid\":7,\"onActionMethodID\":\"x\"}],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must use cy.get() with data-cy selectors",
			spec.contains("cy.get('[data-cy=\"myForm.btn1\"]')"));
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesDescribeAndIt() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent", parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[{\"name\":\"lbl1\",\"typeid\":7}],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must use describe()",
			spec.contains("describe('myForm"));
		assertTrue("Generated spec must use it()",
			spec.contains("it('"));
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesRelativeUrl() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent", parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must use relative URL with formpreview param",
			spec.contains("?formpreview=myForm&svy_testmode=true"));
		assertTrue("Generated spec must NOT contain hardcoded localhost URL",
			!spec.contains("http://localhost"));
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_checksErrorOverlay() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent", parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[{\"name\":\"lbl1\",\"typeid\":7}],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must check for error overlay",
			spec.contains(".svy-error, .error-overlay"));
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_buttonsUseBeEnabled() throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		Method generateSpec = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent", parseFrmFile.getReturnType());
		generateSpec.setAccessible(true);

		String frmContent = "{\n\"items\":[{\"name\":\"btn_save\",\"onActionMethodID\":\"abc\",\"typeid\":7}],\n\"name\":\"myForm\",\n\"typeid\":3\n}";
		Object metadata = parseFrmFile.invoke(new FormSpecGenerator(), frmContent, "myForm");
		String spec = (String)generateSpec.invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must have buttons are clickable test",
			spec.contains("buttons are clickable"));
		assertTrue("Generated spec must check button is enabled",
			spec.contains("'be.enabled'"));
	}

	@Test
	public void testFormSpecGenerator_hasGetSpecFilePathMethod() throws NoSuchMethodException
	{
		assertNotNull("FormSpecGenerator must have getSpecFilePath(String) method",
			FormSpecGenerator.class.getMethod("getSpecFilePath", String.class));
	}

	@Test
	public void testFormSpecGenerator_hasGetFormSpecDirMethod() throws NoSuchMethodException
	{
		assertNotNull("FormSpecGenerator must have getFormSpecDir() method",
			FormSpecGenerator.class.getMethod("getFormSpecDir"));
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
		assertTrue("FormSpecGenerator should no longer have getFormsDir() method (renamed to getFormSpecDir)", !found);
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
		assertTrue("FormSpecGenerator should no longer have ensureBuildpathExclusion method", !found);
	}
}

