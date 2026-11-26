package com.servoy.eclipse.ngclient.startup.resourceprovider.plainjunit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;

import com.servoy.eclipse.ngclient.startup.resourceprovider.ComponentResourcesExporter;
import com.servoy.eclipse.ngclient.startup.resourceprovider.ResourceProvider;

class ComponentResourcesExporterTest
{
	@TempDir
	File tempDir;

	private Supplier<Bundle> originalBundleSupplier;
	private Bundle bundle;

	@BeforeEach
	void setUp()
	{
		originalBundleSupplier = ResourceProvider.nClientBundleSupplier;
		bundle = mock(Bundle.class);
		ResourceProvider.nClientBundleSupplier = () -> bundle;
	}

	@AfterEach
	void tearDown()
	{
		ResourceProvider.nClientBundleSupplier = originalBundleSupplier;
	}

	private void entries(String path, String... children)
	{
		when(bundle.getEntryPaths(path)).thenAnswer(inv -> Collections.enumeration(List.of(children)));
	}

	private void bundleFile(String bundlePath, String content) throws IOException
	{
		File source = new File(new File(tempDir, "bundle"), bundlePath);
		Files.createDirectories(source.getParentFile().toPath());
		Files.writeString(source.toPath(), content);
		when(bundle.getEntry(bundlePath)).thenReturn(source.toURI().toURL());
	}

	private String read(File file) throws IOException
	{
		return Files.readString(file.toPath());
	}

	@Test
	@DisplayName("component directory names list only exported packages, not js/css/templates/services, in bundle order")
	void componentDirectoryNames()
	{
		entries("/war/", "war/js/", "war/css/", "war/templates/", "war/button/", "war/table/", "war/ngservices/", "war/notexported/", "war/index.html");

		String result = ComponentResourcesExporter.getDefaultComponentDirectoryNames(Set.of("button", "table", "ngservices", "js"));

		assertEquals("/button/;/table/;", result);
	}

	@Test
	@DisplayName("component directory names are empty when nothing is exported")
	void componentDirectoryNamesNothingExported()
	{
		entries("/war/", "war/button/", "war/table/");

		assertEquals("", ComponentResourcesExporter.getDefaultComponentDirectoryNames(Set.of()));
	}

	@Test
	@DisplayName("service directory names list only exported packages ending in services/")
	void serviceDirectoryNames()
	{
		entries("/war/", "war/button/", "war/ngservices/", "war/otherservices/", "war/js/");

		String result = ComponentResourcesExporter.getDefaultServicesDirectoryNames(Set.of("ngservices", "button"));

		assertEquals("/ngservices/;", result);
	}

	@Test
	@DisplayName("exported packages, templates and top level files are copied, others are skipped, html files are registered as templates")
	void copiesExportedPackagesAndTemplates() throws IOException
	{
		entries("/war/", "war/readme.txt", "war/button/", "war/other/", "war/templates/");
		entries("war/button/", "war/button/button.js", "war/button/button.html", "war/button/sub/");
		entries("war/button/sub/", "war/button/sub/deep.txt");
		entries("war/templates/", "war/templates/t.html");
		bundleFile("war/readme.txt", "readme");
		bundleFile("war/button/button.js", "js");
		bundleFile("war/button/button.html", "<b/>");
		bundleFile("war/button/sub/deep.txt", "deep");
		bundleFile("war/templates/t.html", "<t/>");
		File dest = new File(tempDir, "out");
		Map<String, File> templates = new HashMap<>();

		ComponentResourcesExporter.copyDefaultComponentsAndServices(dest, Set.of("button"), templates);

		assertAll(
			() -> assertEquals("readme", read(new File(dest, "readme.txt"))),
			() -> assertEquals("js", read(new File(dest, "button/button.js"))),
			() -> assertEquals("<b/>", read(new File(dest, "button/button.html"))),
			() -> assertEquals("deep", read(new File(dest, "button/sub/deep.txt"))),
			() -> assertEquals("<t/>", read(new File(dest, "templates/t.html"))),
			() -> verify(bundle, never()).getEntryPaths("war/other/"),
			() -> assertEquals(Set.of("button/button.html", "templates/t.html"), templates.keySet()),
			() -> assertEquals(new File(dest, "button/button.html"), templates.get("button/button.html")),
			() -> assertEquals(new File(dest, "templates/t.html"), templates.get("templates/t.html")));
	}

	@Test
	@DisplayName("an existing template in the destination is kept, an existing other file is overwritten")
	void templateKeptOtherOverwritten() throws IOException
	{
		entries("/war/", "war/button/", "war/templates/");
		entries("war/button/", "war/button/button.js");
		entries("war/templates/", "war/templates/t.html");
		bundleFile("war/button/button.js", "new");
		bundleFile("war/templates/t.html", "bundle version");
		File dest = new File(tempDir, "out");
		Files.createDirectories(new File(dest, "button").toPath());
		Files.createDirectories(new File(dest, "templates").toPath());
		Files.writeString(new File(dest, "button/button.js").toPath(), "old");
		Files.writeString(new File(dest, "templates/t.html").toPath(), "customized");
		Map<String, File> templates = new HashMap<>();

		ComponentResourcesExporter.copyDefaultComponentsAndServices(dest, Set.of("button"), templates);

		assertAll(
			() -> assertEquals("new", read(new File(dest, "button/button.js"))),
			() -> assertEquals("customized", read(new File(dest, "templates/t.html"))),
			() -> assertTrue(templates.containsKey("templates/t.html")));
	}

	@Test
	@DisplayName("a bundle without /war/ entries copies nothing")
	void noEntries() throws IOException
	{
		File dest = new File(tempDir, "out");
		Map<String, File> templates = new HashMap<>();

		ComponentResourcesExporter.copyDefaultComponentsAndServices(dest, Set.of("button"), templates);

		assertAll(
			() -> assertFalse(dest.exists()),
			() -> assertTrue(templates.isEmpty()));
	}
}
