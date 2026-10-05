package com.servoy.eclipse.cypress.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

public class FormSpecGeneratorTest {
	/**
	 * Builds a FormSpecGenerator.FormMetadata instance via reflection (the type is
	 * package-private) with the given data source and element definitions,
	 * bypassing buildMetadata(Form) so these tests don't need a live Servoy
	 * model/Form persist.
	 *
	 * Each element definition is {name, typeName, dataProviderID, isWebComponent,
	 * isButton, isLabel}.
	 */
	private Object buildMetadata(String formName, String dataSource, Object[][] elements) throws Exception {
		Class<?> metadataClass = Class.forName("com.servoy.eclipse.cypress.services.FormSpecGenerator$FormMetadata");
		Constructor<?> metadataCtor = metadataClass.getDeclaredConstructor();
		metadataCtor.setAccessible(true);
		Object metadata = metadataCtor.newInstance();

		setField(metadata, "formName", formName);
		setField(metadata, "dataSource", dataSource);

		Class<?> elementClass = Class.forName("com.servoy.eclipse.cypress.services.FormSpecGenerator$ElementInfo");
		Constructor<?> elementCtor = elementClass.getDeclaredConstructor();
		elementCtor.setAccessible(true);

		@SuppressWarnings("unchecked")
		List<Object> namedElements = (List<Object>) getField(metadata, "namedElements");
		for (Object[] def : elements) {
			Object elem = elementCtor.newInstance();
			setField(elem, "name", def[0]);
			setField(elem, "typeName", def[1]);
			setField(elem, "dataProviderID", def[2]);
			setField(elem, "isWebComponent", def[3]);
			setField(elem, "isButton", def[4]);
			setField(elem, "isLabel", def[5]);
			namedElements.add(elem);
		}

		return metadata;
	}

	private Object buildMetadata(String formName, String dataSource) throws Exception {
		return buildMetadata(formName, dataSource, new Object[0][]);
	}

	private void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private Object getField(Object target, String fieldName) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(target);
	}

	private Method getGenerateCypressSpecContentMethod() throws Exception {
		Class<?> metadataClass = Class.forName("com.servoy.eclipse.cypress.services.FormSpecGenerator$FormMetadata");
		Method m = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent", metadataClass);
		m.setAccessible(true);
		return m;
	}

	private Method getGenerateSetupContentMethod() throws Exception {
		Class<?> metadataClass = Class.forName("com.servoy.eclipse.cypress.services.FormSpecGenerator$FormMetadata");
		Method m = FormSpecGenerator.class.getDeclaredMethod("generateSetupContent", metadataClass);
		m.setAccessible(true);
		return m;
	}

	@Test
	public void testFormSpecGenerator_isCreatable() {
		assertNotNull("FormSpecGenerator must have @Creatable annotation",
				FormSpecGenerator.class.getAnnotation(org.eclipse.e4.core.di.annotations.Creatable.class));
	}

	@Test
	public void testFormSpecGenerator_hasGenerateSpecMethod() throws NoSuchMethodException {
		assertNotNull("FormSpecGenerator must have generateSpec(String) method",
				FormSpecGenerator.class.getMethod("generateSpec", String.class));
	}

	@Test
	public void testFormSpecGenerator_hasSpecExistsMethod() throws NoSuchMethodException {
		assertNotNull("FormSpecGenerator must have specExists(String) method",
				FormSpecGenerator.class.getMethod("specExists", String.class));
	}

	@Test
	public void testFormSpecGenerator_generateSpecReturnType() throws NoSuchMethodException {
		assertEquals("generateSpec must return String", String.class,
				FormSpecGenerator.class.getMethod("generateSpec", String.class).getReturnType());
	}

	@Test
	public void testFormSpecGenerator_specExistsReturnType() throws NoSuchMethodException {
		assertEquals("specExists must return boolean", boolean.class,
				FormSpecGenerator.class.getMethod("specExists", String.class).getReturnType());
	}

	@Test
	public void testFormSpecGenerator_hasNoArgConstructor() throws NoSuchMethodException {
		assertNotNull("FormSpecGenerator must have a no-arg constructor", FormSpecGenerator.class.getConstructor());
	}

	@Test
	public void testFormSpecGenerator_canBeInstantiated() {
		FormSpecGenerator gen = new FormSpecGenerator();
		assertNotNull("FormSpecGenerator must be instantiable", gen);
	}

	@Test
	public void testFormSpecGenerator_doesNotHaveParseFrmFileMethod() {
		// SVY-21514: form metadata is now read from the resolved Form persist via
		// buildMetadata(Form), not by re-reading and regex-parsing the .frm file, so
		// parseFrmFile should no longer exist.
		boolean found = false;
		for (Method m : FormSpecGenerator.class.getDeclaredMethods()) {
			if ("parseFrmFile".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue("FormSpecGenerator should no longer have a parseFrmFile method", !found);
	}

	@Test
	public void testFormSpecGenerator_hasBuildMetadataMethod() {
		// SVY-21514: replaces parseFrmFile as the way FormMetadata is produced, now
		// reading directly from a resolved Form persist instead of .frm file text.
		boolean found = false;
		for (Method m : FormSpecGenerator.class.getDeclaredMethods()) {
			if ("buildMetadata".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue("FormSpecGenerator must have a buildMetadata method", found);
	}

	@Test
	public void testFormSpecGenerator_hasGenerateCypressSpecContentMethod() {
		Method[] methods = FormSpecGenerator.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods) {
			if ("generateCypressSpecContent".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue("FormSpecGenerator must have a generateCypressSpecContent method", found);
	}

	@Test
	public void testFormSpecGenerator_hasGenerateSetupContentMethod() {
		Method[] methods = FormSpecGenerator.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods) {
			if ("generateSetupContent".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue("FormSpecGenerator must have a generateSetupContent method", found);
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_containsSetUp() throws Exception {
		Object metadata = buildMetadata("testForm", "db:/test/t1");
		String setup = (String) getGenerateSetupContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated setup must contain spec_setUp function", setup.contains("function spec_setUp()"));
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_containsTearDown() throws Exception {
		Object metadata = buildMetadata("testForm", "db:/test/t1");
		String setup = (String) getGenerateSetupContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated setup must contain spec_tearDown function", setup.contains("function spec_tearDown()"));
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_containsUuid() throws Exception {
		Object metadata = buildMetadata("testForm", null);
		String setup = (String) getGenerateSetupContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated setup must contain @properties with uuid",
				setup.contains("@properties={typeid:24,uuid:\""));
	}

	@Test
	public void testFormSpecGenerator_generateSetupContent_mentionsCypress() throws Exception {
		Object metadata = buildMetadata("testForm", null);
		String setup = (String) getGenerateSetupContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated setup must mention Cypress", setup.contains("Cypress"));
	}

	// --- Cypress spec content generation tests ---

	@Test
	public void testFormSpecGenerator_generateCypressSpec_noPropertiesAnnotation() throws Exception {
		Object metadata = buildMetadata("myForm", null, new Object[][] { { "btn1", null, null, false, true, false } });
		String spec = (String) getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must NOT contain @properties annotation", !spec.contains("@properties"));
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesCyVisit() throws Exception {
		Object metadata = buildMetadata("myForm", null);
		String spec = (String) getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must use cy.visit()", spec.contains("cy.visit("));
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesCyGet() throws Exception {
		Object metadata = buildMetadata("myForm", null, new Object[][] { { "btn1", null, null, false, true, false } });
		String spec = (String) getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must use cy.get() with data-cy selectors",
				spec.contains("cy.get('[data-cy=\"myForm.btn1\"]')"));
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesDescribeAndIt() throws Exception {
		Object metadata = buildMetadata("myForm", null, new Object[][] { { "lbl1", null, null, false, false, true } });
		String spec = (String) getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must use describe()", spec.contains("describe('myForm"));
		assertTrue("Generated spec must use it()", spec.contains("it('"));
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_usesRelativeUrl() throws Exception {
		Object metadata = buildMetadata("myForm", null);
		String spec = (String) getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must use relative URL with formpreview param",
				spec.contains("?formpreview=myForm&svy_testmode=true"));
		assertTrue("Generated spec must NOT contain hardcoded localhost URL", !spec.contains("http://localhost"));
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_checksErrorOverlay() throws Exception {
		Object metadata = buildMetadata("myForm", null, new Object[][] { { "lbl1", null, null, false, false, true } });
		String spec = (String) getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must check for error overlay", spec.contains(".svy-error, .error-overlay"));
	}

	@Test
	public void testFormSpecGenerator_generateCypressSpec_buttonsUseBeEnabled() throws Exception {
		Object metadata = buildMetadata("myForm", null,
				new Object[][] { { "btn_save", null, null, false, true, false } });
		String spec = (String) getGenerateCypressSpecContentMethod().invoke(new FormSpecGenerator(), metadata);

		assertTrue("Generated spec must have buttons are clickable test", spec.contains("buttons are clickable"));
		assertTrue("Generated spec must check button is enabled", spec.contains("'be.enabled'"));
	}

	@Test
	public void testFormSpecGenerator_hasGetSpecFilePathMethod() throws NoSuchMethodException {
		assertNotNull("FormSpecGenerator must have getSpecFilePath(String) method",
				FormSpecGenerator.class.getMethod("getSpecFilePath", String.class));
	}

	@Test
	public void testFormSpecGenerator_hasGetFormSpecDirMethod() throws NoSuchMethodException {
		assertNotNull("FormSpecGenerator must have getFormSpecDir() method",
				FormSpecGenerator.class.getMethod("getFormSpecDir"));
	}

	@Test
	public void testFormSpecGenerator_doesNotHaveGetFormsDirMethod() {
		// SVY-21171: getFormsDir() was renamed to getFormSpecDir() when the Cypress
		// form specs moved out of medias/tests to the workspace-relative cy-form dir.
		boolean found = false;
		for (Method m : FormSpecGenerator.class.getDeclaredMethods()) {
			if ("getFormsDir".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue("FormSpecGenerator should no longer have getFormsDir() method (renamed to getFormSpecDir)", !found);
	}

	@Test
	public void testFormSpecGenerator_doesNotHaveEnsureBuildpathExclusionMethod() {
		// SVY-21171: ensureBuildpathExclusion was removed because the moved specs
		// live outside the solution and no longer need a .buildpath exclusion.
		Method[] methods = FormSpecGenerator.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods) {
			if ("ensureBuildpathExclusion".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue("FormSpecGenerator should no longer have ensureBuildpathExclusion method", !found);
	}
}
