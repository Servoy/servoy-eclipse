package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.servoy.eclipse.cypress.services.FormSpecGenerator;

public class FormSpecGeneratorTest
{
	/**
	 * Builds a FormSpecGenerator.FormMetadata instance via reflection (the type is
	 * private) with the given data source and element definitions, bypassing
	 * buildMetadata(Form) so these tests don't need a live Servoy model/Form persist.
	 *
	 * Each element definition is {name, typeName, isWebComponent, isButton,
	 * isLabel}.
	 */
	private Object buildMetadata(String formName, String dataSource, Object[][] elements) throws Exception
	{
		Class< ? > metadataClass = Class.forName("com.servoy.eclipse.cypress.services.FormSpecGenerator$FormMetadata");
		Constructor< ? > metadataCtor = metadataClass.getDeclaredConstructor();
		metadataCtor.setAccessible(true);
		Object metadata = metadataCtor.newInstance();

		setField(metadata, "formName", formName);
		setField(metadata, "dataSource", dataSource);

		Class< ? > elementClass = Class.forName("com.servoy.eclipse.cypress.services.FormSpecGenerator$ElementInfo");
		Constructor< ? > elementCtor = elementClass.getDeclaredConstructor();
		elementCtor.setAccessible(true);

		@SuppressWarnings("unchecked")
		List<Object> namedElements = (List<Object>)getField(metadata, "namedElements");
		for (Object[] def : elements)
		{
			Object elem = elementCtor.newInstance();
			setField(elem, "name", def[0]);
			setField(elem, "typeName", def[1]);
			setField(elem, "isWebComponent", def[2]);
			setField(elem, "isButton", def[3]);
			setField(elem, "isLabel", def[4]);
			namedElements.add(elem);
		}

		return metadata;
	}

	private Object buildMetadata(String formName, String dataSource) throws Exception
	{
		return buildMetadata(formName, dataSource, new Object[0][]);
	}

	private void setField(Object target, String fieldName, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private Object getField(Object target, String fieldName) throws Exception
	{
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(target);
	}

	private Method getGenerateCypressSpecContentMethod() throws Exception
	{
		Class< ? > metadataClass = Class.forName("com.servoy.eclipse.cypress.services.FormSpecGenerator$FormMetadata");
		Method m = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent", metadataClass);
		m.setAccessible(true);
		return m;
	}

	private Method getGenerateSetupContentMethod() throws Exception
	{
		Class< ? > metadataClass = Class.forName("com.servoy.eclipse.cypress.services.FormSpecGenerator$FormMetadata");
		Method m = FormSpecGenerator.class.getDeclaredMethod("generateSetupContent", metadataClass);
		m.setAccessible(true);
		return m;
	}

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
	public void testFormSpecGenerator_doesNotHaveParseFrmFileMethod()
	{
		// SVY-21514: form metadata is now read from the resolved Form persist via
		// buildMetadata(Form), not by re-reading and regex-parsing the .frm file, so
		// parseFrmFile should no longer exist.
		boolean found = false;
		for (Method m : FormSpecGenerator.class.getDeclaredMethods())
		{
			if ("parseFrmFile".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue(!found, "FormSpecGenerator should no longer have a parseFrmFile method");
	}

	@Test
	public void testFormSpecGenerator_hasBuildMetadataMethod()
	{
		// SVY-21514: replaces parseFrmFile as the way FormMetadata is produced, now
		// reading directly from a resolved Form persist instead of .frm file text.
		boolean found = false;
		for (Method m : FormSpecGenerator.class.getDeclaredMethods())
		{
			if ("buildMetadata".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue(found, "FormSpecGenerator must have a buildMetadata method");
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
	public void testFormSpecGenerator_generateSetupContent_containsSetUp() throws Exception
	{
		Object metadata = buildMetadata("testForm", "db:/test/t1");
		String setup = (String)getGenerateSetupContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue(setup.contains("function spec_setUp()"), "Generated setup must contain spec_setUp function");
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_containsTearDown() throws Exception
	{
		Object metadata = buildMetadata("testForm", "db:/test/t1");
		String setup = (String)getGenerateSetupContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue(setup.contains("function spec_tearDown()"), "Generated setup must contain spec_tearDown function");
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_containsUuid() throws Exception
	{
		Object metadata = buildMetadata("testForm", null);
		String setup = (String)getGenerateSetupContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue(setup.contains("@properties={typeid:24,uuid:\""),
			"Generated setup must contain @properties with uuid");
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_mentionsCypress() throws Exception
	{
		Object metadata = buildMetadata("testForm", null);
		String setup = (String)getGenerateSetupContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue(setup.contains("Cypress"), "Generated setup must mention Cypress");
	}

	// --- Cypress spec content generation tests ---

	@Test
	public void testFormSpecGenerator_generateCypressSpec_noPropertiesAnnotation() throws Exception
	{
		Object metadata = buildMetadata("myForm", null, new Object[][] { { "btn1", null, false, true, false } });
		String spec = (String)getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue(!spec.contains("@properties"), "Generated spec must NOT contain @properties annotation");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesCyVisit() throws Exception
	{
		Object metadata = buildMetadata("myForm", null);
		String spec = (String)getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue(spec.contains("cy.visit("), "Generated spec must use cy.visit()");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesCyGet() throws Exception
	{
		Object metadata = buildMetadata("myForm", null, new Object[][] { { "btn1", null, false, true, false } });
		String spec = (String)getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue(spec.contains("cy.get('[data-cy=\"myForm.btn1\"]')"),
			"Generated spec must use cy.get() with data-cy selectors");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_beforeEachWaitSelectorIsClosed() throws Exception
	{
		// SVY-21516: the beforeEach wait gate for a form with named elements must emit a
		// valid, correctly-closed CSS attribute selector [data-cy^="<formName>."] (with the
		// closing ]). A missing ] made querySelectorAll throw a SyntaxError, aborting the
		// whole spec before any it() block ran.
		Object metadata = buildMetadata("myForm", null, new Object[][] { { "elem1", null, false, false, true } });
		String spec = (String)getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue(spec.contains("cy.get('[data-cy^=\"myForm.\"]'"),
			"beforeEach wait selector must be the correctly-closed [data-cy^=\"myForm.\"] attribute selector");
		assertTrue(!spec.contains("cy.get('[data-cy^=\"myForm.\"'"),
			"beforeEach wait selector must NOT contain the malformed selector missing the closing ]");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesDescribeAndIt() throws Exception
	{
		Object metadata = buildMetadata("myForm", null, new Object[][] { { "lbl1", null, false, false, true } });
		String spec = (String)getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue(spec.contains("describe('myForm"), "Generated spec must use describe()");
		assertTrue(spec.contains("it('"), "Generated spec must use it()");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesRelativeUrl() throws Exception
	{
		Object metadata = buildMetadata("myForm", null);
		String spec = (String)getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue(spec.contains("?formpreview=myForm&svy_testmode=true"),
			"Generated spec must use relative URL with formpreview param");
		assertTrue(!spec.contains("http://localhost"), "Generated spec must NOT contain hardcoded localhost URL");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_checksErrorOverlay() throws Exception
	{
		Object metadata = buildMetadata("myForm", null, new Object[][] { { "lbl1", null, false, false, true } });
		String spec = (String)getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue(spec.contains(".svy-error, .error-overlay"), "Generated spec must check for error overlay");
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_buttonsUseBeEnabled() throws Exception
	{
		Object metadata = buildMetadata("myForm", null,
			new Object[][] { { "btn_save", null, false, true, false } });
		String spec = (String)getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

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
