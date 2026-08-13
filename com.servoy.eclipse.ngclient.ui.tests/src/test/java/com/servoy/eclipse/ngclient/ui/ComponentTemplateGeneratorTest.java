package com.servoy.eclipse.ngclient.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sablo.InMemPackageReader;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.Package.IPackageReader;

import com.servoy.j2db.server.ngclient.DefaultComponentPropertiesProvider;
import com.servoy.j2db.server.ngclient.property.types.Types;
import com.servoy.j2db.util.Pair;

class ComponentTemplateGeneratorTest
{
	private static final String MANIFEST = "Manifest-Version: 1.0\n" +
		"Bundle-Name: Test Package\n" +
		"Bundle-SymbolicName: testpkg\n" +
		"NPM-PackageName: @test/testpkg\n" +
		"NG2-Components: TestButton\n" +
		"Entry-Point: projects/testpkg\n" +
		"\n" +
		"Name: button/button.spec\n" +
		"Web-Component: True\n" +
		"\n" +
		"Name: formcomponent/formcomponent.spec\n" +
		"Web-Component: True\n" +
		"\n" +
		"Name: navigator/navigator.spec\n" +
		"Web-Component: True\n" +
		"\n" +
		"Name: portal/portal.spec\n" +
		"Web-Component: True\n";

	private static final String BUTTON_SPEC = "{\n" +
		"  \"name\": \"testpkg-button\",\n" +
		"  \"displayName\": \"Button\",\n" +
		"  \"version\": 1,\n" +
		"  \"definition\": \"testpkg/button/button.js\",\n" +
		"  \"libraries\": [],\n" +
		"  \"model\": {\n" +
		"    \"text\": { \"type\": \"tagstring\", \"initialValue\": \"button\" },\n" +
		"    \"enabled\": { \"type\": \"enabled\", \"blockingOn\": false, \"default\": true },\n" +
		"    \"visible\": \"visible\",\n" +
		"    \"styleClass\": { \"type\": \"styleclass\", \"tags\": { \"scope\": \"design\" } },\n" +
		"    \"dataProviderID\": { \"type\": \"dataprovider\", \"pushToServer\": \"allow\" }\n" +
		"  },\n" +
		"  \"handlers\": {\n" +
		"    \"onActionMethodID\": {\n" +
		"      \"parameters\": [{ \"name\": \"event\", \"type\": \"JSEvent\" }]\n" +
		"    }\n" +
		"  },\n" +
		"  \"api\": {}\n" +
		"}";

	private static final String FORMCOMPONENT_SPEC = "{\n" +
		"  \"name\": \"servoycore-formcomponent\",\n" +
		"  \"displayName\": \"Form Component\",\n" +
		"  \"version\": 1,\n" +
		"  \"definition\": \"servoycore/formcomponent/formcomponent.js\",\n" +
		"  \"libraries\": [],\n" +
		"  \"model\": { \"containedForm\": \"form\" },\n" +
		"  \"handlers\": {},\n" +
		"  \"api\": {}\n" +
		"}";

	private static final String NAVIGATOR_SPEC = "{\n" +
		"  \"name\": \"servoycore-navigator\",\n" +
		"  \"displayName\": \"Navigator\",\n" +
		"  \"version\": 1,\n" +
		"  \"definition\": \"servoycore/navigator/navigator.js\",\n" +
		"  \"libraries\": [],\n" +
		"  \"model\": { \"currentIndex\": \"int\" },\n" +
		"  \"handlers\": {},\n" +
		"  \"api\": {}\n" +
		"}";

	private static final String PORTAL_SPEC = "{\n" +
		"  \"name\": \"servoycore-portal\",\n" +
		"  \"displayName\": \"Portal\",\n" +
		"  \"version\": 1,\n" +
		"  \"definition\": \"servoycore/portal/portal.js\",\n" +
		"  \"libraries\": [],\n" +
		"  \"model\": { \"relatedFoundset\": \"string\" },\n" +
		"  \"handlers\": {},\n" +
		"  \"api\": {}\n" +
		"}";

	@BeforeAll
	static void setUp()
	{
		Types.getTypesInstance().registerTypes();

		HashMap<String, String> components = new HashMap<>();
		components.put("button/button.spec", BUTTON_SPEC);
		components.put("formcomponent/formcomponent.spec", FORMCOMPONENT_SPEC);
		components.put("navigator/navigator.spec", NAVIGATOR_SPEC);
		components.put("portal/portal.spec", PORTAL_SPEC);
		IPackageReader reader = new InMemPackageReader(MANIFEST, components);

		WebComponentSpecProvider.init(new IPackageReader[] { reader }, DefaultComponentPropertiesProvider.instance);
	}

	@AfterAll
	static void tearDown()
	{
		WebComponentSpecProvider.disposeInstance();
	}

	@Test
	void generatesCorrectNgTemplateForButton()
	{
		ComponentTemplateGenerator generator = new ComponentTemplateGenerator();
		Pair<StringBuilder, StringBuilder> result = generator.generateHTMLTemplate(null);
		String template = result.getLeft().toString();

		assertTrue(template.contains("<ng-template #testpkgButton"));
		assertTrue(template.contains("<testpkg-button "));
		assertTrue(template.contains("</testpkg-button>"));
		assertTrue(template.contains("</ng-template>"));
	}

	@Test
	void internalPropertiesNotInOutput()
	{
		ComponentTemplateGenerator generator = new ComponentTemplateGenerator();
		Pair<StringBuilder, StringBuilder> result = generator.generateHTMLTemplate(null);
		String template = result.getLeft().toString();

		assertFalse(template.contains("[location]=\""));
		assertFalse(template.contains("[size]=\""));
		assertFalse(template.contains("[cssPosition]=\""));
		assertFalse(template.contains("[anchors]=\""));
		assertFalse(template.contains("[formIndex]=\""));
	}

	@Test
	void skipsFormcomponentNavigatorPortal()
	{
		ComponentTemplateGenerator generator = new ComponentTemplateGenerator();
		Pair<StringBuilder, StringBuilder> result = generator.generateHTMLTemplate(null);
		String template = result.getLeft().toString();

		assertFalse(template.contains("<servoycore-formcomponent"));
		assertFalse(template.contains("<servoycore-navigator"));
		assertFalse(template.contains("<servoycore-portal"));
	}

	@Test
	void generatesViewChildReferences()
	{
		ComponentTemplateGenerator generator = new ComponentTemplateGenerator();
		Pair<StringBuilder, StringBuilder> result = generator.generateHTMLTemplate(null);
		String viewChild = result.getRight().toString();

		assertTrue(viewChild.contains("readonly testpkgButton = viewChild<TemplateRef<any>>('testpkgButton');"));
	}

	@Test
	void servoyApiAndNameBindingsAlwaysPresent()
	{
		ComponentTemplateGenerator generator = new ComponentTemplateGenerator();
		Pair<StringBuilder, StringBuilder> result = generator.generateHTMLTemplate(null);
		String template = result.getLeft().toString();

		assertTrue(template.contains("[servoyApi]=\"callback.getServoyApi(state)\""));
		assertTrue(template.contains("[name]=\"state.name\""));
	}

	@Test
	void handlerBindingsPresent()
	{
		ComponentTemplateGenerator generator = new ComponentTemplateGenerator();
		Pair<StringBuilder, StringBuilder> result = generator.generateHTMLTemplate(null);
		String template = result.getLeft().toString();

		assertTrue(template.contains("[onActionMethodID]=\"callback.getHandler(state,'onActionMethodID')\""));
	}

	@Test
	void datachangeEmitterForPushToServerProperty()
	{
		ComponentTemplateGenerator generator = new ComponentTemplateGenerator();
		Pair<StringBuilder, StringBuilder> result = generator.generateHTMLTemplate(null);
		String template = result.getLeft().toString();

		assertTrue(template.contains("(dataProviderIDChange)=\"callback.datachange(state,'dataProviderID',$event, true)\""));
	}

	@Test
	void testableOverloadProducesSameOutputAsOriginal()
	{
		ComponentTemplateGenerator generator = new ComponentTemplateGenerator();
		Pair<StringBuilder, StringBuilder> original = generator.generateHTMLTemplate(null);

		WebObjectSpecification[] specs = WebComponentSpecProvider.getSpecProviderState().getAllWebObjectSpecifications();
		Map<String, PackageSpecification<WebObjectSpecification>> packageSpecs = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecifications();
		Pair<StringBuilder, StringBuilder> overloaded = generator.generateHTMLTemplate(specs, packageSpecs, null);

		assertEquals(original.getLeft().toString(), overloaded.getLeft().toString());
		assertEquals(original.getRight().toString(), overloaded.getRight().toString());
	}
}
