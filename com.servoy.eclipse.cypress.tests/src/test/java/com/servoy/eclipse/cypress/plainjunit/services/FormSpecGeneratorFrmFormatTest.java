package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.servoy.eclipse.cypress.services.FormSpecGenerator;

/**
 * SVY-21517: Servoy writes .frm files with unquoted keys (dataSource:"...", name:"...", typeid:47), the older tests only used JSON style quoted keys.
 * Both notations must give the same metadata.
 */
class FormSpecGeneratorFrmFormatTest
{
	/** Trimmed copy of a real .frm as written by Servoy (unquoted keys). */
	private static final String UNQUOTED_FRM = """
		customProperties:"useCssPosition:true",
		dataSource:"db:/servoy_test/orders",
		encapsulation:108,
		items:[
		{
		cssPosition:"326,-1,-1,353,140,25",
		json:{
		cssPosition:{
		bottom:"-1",
		height:"25"
		},
		dataProviderID:"terms",
		tabSeq:2
		},
		name:"fld_terms",
		typeName:"bootstrapcomponents-textbox",
		typeid:47,
		uuid:"2E559573-224C-41AA-BD0B-0DEE656CD314"
		},
		{
		height:480,
		partType:5,
		typeid:19,
		uuid:"768A9CA1-B45F-4B59-8274-83DD8638AA11"
		},
		{
		cssPosition:"26,-1,-1,634,80,19",
		json:{
		cssPosition:{
		bottom:"-1",
		height:"19"
		},
		onActionMethodID:"8ACE47E0-9370-4EF4-A226-DF6DD1DACF76",
		tabSeq:5,
		text:"Next"
		},
		name:"btn_next",
		typeName:"bootstrapcomponents-button",
		typeid:47,
		uuid:"BD893E12-1005-4AD6-AFB5-0949A98A3910"
		},
		{
		name:"legacy_btn",
		onActionMethodID:"11111111-2222-3333-4444-555555555555",
		typeid:7,
		uuid:"AAAAAAAA-2222-3333-4444-555555555555"
		},
		{
		name:"legacy_label",
		typeid:7,
		uuid:"BBBBBBBB-2222-3333-4444-555555555555"
		}
		],
		name:"f1_test",
		navigatorID:"-1",
		typeid:3,
		uuid:"1CCDE048-2885-4B37-B63F-764B84EFD659"
		""";

	private static Object parse(String content) throws Exception
	{
		Method parseFrmFile = FormSpecGenerator.class.getDeclaredMethod("parseFrmFile", String.class, String.class);
		parseFrmFile.setAccessible(true);
		return parseFrmFile.invoke(new FormSpecGenerator(), content, "f1_test");
	}

	private static Object field(Object target, String name) throws Exception
	{
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(target);
	}

	private static List<Object> elements(Object metadata) throws Exception
	{
		return new ArrayList<>((List< ? >)field(metadata, "namedElements"));
	}

	private static Object element(Object metadata, String name) throws Exception
	{
		for (Object e : elements(metadata))
		{
			if (name.equals(field(e, "name"))) return e;
		}
		throw new AssertionError("element '" + name + "' was not found");
	}

	@Test
	void unquotedKeys_dataSourceIsExtracted() throws Exception
	{
		assertEquals("db:/servoy_test/orders", field(parse(UNQUOTED_FRM), "dataSource"));
	}

	@Test
	void unquotedKeys_elementNamesAreExtractedInSourceOrderWithoutTheFormName() throws Exception
	{
		Object metadata = parse(UNQUOTED_FRM);
		List<String> names = new ArrayList<>();
		for (Object e : elements(metadata))
		{
			names.add((String)field(e, "name"));
		}
		assertEquals(List.of("fld_terms", "btn_next", "legacy_btn", "legacy_label"), names);
	}

	@Test
	void unquotedKeys_typeNameIsExtracted() throws Exception
	{
		Object metadata = parse(UNQUOTED_FRM);
		assertAll(() -> assertEquals("bootstrapcomponents-textbox", field(element(metadata, "fld_terms"), "typeName")),
			() -> assertEquals("bootstrapcomponents-button", field(element(metadata, "btn_next"), "typeName")));
	}

	@Test
	void unquotedKeys_webComponentIsRecognisedByTypeid47() throws Exception
	{
		Object metadata = parse(UNQUOTED_FRM);
		assertAll(() -> assertEquals(Boolean.TRUE, field(element(metadata, "fld_terms"), "isWebComponent")),
			() -> assertEquals(Boolean.TRUE, field(element(metadata, "btn_next"), "isWebComponent")),
			() -> assertEquals(Boolean.FALSE, field(element(metadata, "legacy_btn"), "isWebComponent")));
	}

	@Test
	void unquotedKeys_legacyButtonAndLabelAreRecognisedByTypeid7() throws Exception
	{
		Object metadata = parse(UNQUOTED_FRM);
		assertAll(() -> assertEquals(Boolean.TRUE, field(element(metadata, "legacy_btn"), "isButton")),
			() -> assertEquals(Boolean.FALSE, field(element(metadata, "legacy_btn"), "isLabel")),
			() -> assertEquals(Boolean.FALSE, field(element(metadata, "legacy_label"), "isButton")),
			() -> assertEquals(Boolean.TRUE, field(element(metadata, "legacy_label"), "isLabel")));
	}

	@Test
	void unquotedKeys_setupFileMentionsTheDataSource() throws Exception
	{
		Object metadata = parse(UNQUOTED_FRM);
		Method generateSetupContent = FormSpecGenerator.class.getDeclaredMethod("generateSetupContent", metadata.getClass());
		generateSetupContent.setAccessible(true);
		String setup = (String)generateSetupContent.invoke(new FormSpecGenerator(), metadata);

		assertAll(() -> assertTrue(setup.contains(" * DataSource: db:/servoy_test/orders\n"), "header must name the data source"),
			() -> assertTrue(setup.contains("\t// DataSource: db:/servoy_test/orders\n"), "spec_setUp must name the data source"),
			() -> assertFalse(setup.contains("No dataSource on this form"), "a form with a data source must not be reported as having none"));
	}

	@Test
	void unquotedKeys_legacySpecAssertsTheFormElements() throws Exception
	{
		Object metadata = parse(UNQUOTED_FRM);
		Field solutionName = metadata.getClass().getDeclaredField("solutionName");
		solutionName.setAccessible(true);
		solutionName.set(metadata, "mysol");
		Method generateCypressSpecContent = FormSpecGenerator.class.getDeclaredMethod("generateCypressSpecContent", metadata.getClass());
		generateCypressSpecContent.setAccessible(true);
		String spec = (String)generateCypressSpecContent.invoke(new FormSpecGenerator(), metadata);

		assertAll(() -> assertTrue(spec.contains("    cy.get('[data-cy=\"f1_test.fld_terms\"]').should('be.visible');\n")),
			() -> assertTrue(spec.contains("    cy.get('[data-cy=\"f1_test.btn_next\"]').should('be.visible');\n")),
			() -> assertTrue(spec.contains("    cy.get('[data-cy=\"f1_test.legacy_btn\"]').should('be.visible').and('be.enabled');\n")),
			() -> assertFalse(spec.contains("cy.get('.svy-form'"), "a form with elements must wait on its data-cy elements, not on .svy-form"));
	}

	@Test
	void quotedKeys_stillWork() throws Exception
	{
		String quoted = "{\n\"dataSource\":\"mem:test_grid\",\n\"items\":[\n{\n\"name\":\"groupingtable\",\n\"typeName\":\"aggrid-groupingtable\",\n\"typeid\":47\n}\n],\n\"name\":\"f1_test\",\n\"typeid\":3\n}";
		Object metadata = parse(quoted);
		assertAll(() -> assertEquals("mem:test_grid", field(metadata, "dataSource")),
			() -> assertEquals("aggrid-groupingtable", field(element(metadata, "groupingtable"), "typeName")),
			() -> assertEquals(Boolean.TRUE, field(element(metadata, "groupingtable"), "isWebComponent")));
	}

	@Test
	void keyNamesInsideLongerNamesAreNotTakenForTheKey() throws Exception
	{
		// "typeName" must not satisfy the name pattern and "nodataSource" must not satisfy the dataSource pattern
		String content = "nodataSource:\"db:/x/y\",\nitems:[\n{\nsubname:\"nope\",\ntypeName:\"t\",\ntypeid:47\n}\n],\nname:\"f1_test\",\ntypeid:3\n";
		Object metadata = parse(content);
		assertAll(() -> assertEquals(null, field(metadata, "dataSource")), () -> assertEquals(0, elements(metadata).size()));
	}
}
