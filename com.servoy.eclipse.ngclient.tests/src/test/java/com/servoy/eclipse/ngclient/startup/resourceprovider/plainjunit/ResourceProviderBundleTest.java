package com.servoy.eclipse.ngclient.startup.resourceprovider.plainjunit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.osgi.framework.Bundle;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.websocket.IWebsocketSessionFactory;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.eclipse.ngclient.startup.resourceprovider.ResourceProvider;
import com.servoy.j2db.server.ngclient.DefaultComponentPropertiesProvider;
import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;
import com.servoy.j2db.server.ngclient.less.LessCompiler;
import com.servoy.j2db.server.ngclient.property.types.Types;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class ResourceProviderBundleTest
{
	private static final String MANIFEST_PATH = "/META-INF/MANIFEST.MF";

	@TempDir
	File tempDir;

	private Supplier<Bundle> originalNClientSupplier;
	private Supplier<Bundle> originalSabloSupplier;
	private Supplier<String> originalDirSupplier;

	private Bundle nclient;
	private Bundle sablo;
	private MockedStatic<WebComponentSpecProvider> componentProviderStatic;
	private MockedStatic<WebServiceSpecProvider> serviceProviderStatic;
	private SpecProviderState componentState;
	private SpecProviderState serviceState;

	@BeforeEach
	void setUp() throws IOException
	{
		originalNClientSupplier = ResourceProvider.nClientBundleSupplier;
		originalSabloSupplier = ResourceProvider.sabloBundleSupplier;
		originalDirSupplier = ResourceProvider.servoyApplicationServerDirSupplier;
		nclient = mock(Bundle.class);
		sablo = mock(Bundle.class);
		ResourceProvider.nClientBundleSupplier = () -> nclient;
		ResourceProvider.sabloBundleSupplier = () -> sablo;
		ResourceProvider.servoyApplicationServerDirSupplier = () -> tempDir.getAbsolutePath();

		when(nclient.findEntries(anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean())).thenAnswer(inv -> Collections.emptyEnumeration());
		when(sablo.getEntry("META-INF/MANIFEST.MF")).thenReturn(manifest("sablo", "Bundle-SymbolicName: sablo").toURI().toURL());

		componentState = mock(SpecProviderState.class);
		serviceState = mock(SpecProviderState.class);
		componentProviderStatic = mockStatic(WebComponentSpecProvider.class);
		serviceProviderStatic = mockStatic(WebServiceSpecProvider.class);
		componentProviderStatic.when(WebComponentSpecProvider::getInstance).thenReturn(mock(WebComponentSpecProvider.class));
		serviceProviderStatic.when(WebServiceSpecProvider::getInstance).thenReturn(mock(WebServiceSpecProvider.class));
		componentProviderStatic.when(WebComponentSpecProvider::getSpecProviderState).thenReturn(componentState);
		serviceProviderStatic.when(WebServiceSpecProvider::getSpecProviderState).thenReturn(serviceState);
	}

	@AfterEach
	void tearDown()
	{
		ResourceProvider.setPackages(List.of());
		ResourceProvider.setRemovedPackages(List.of());
		serviceProviderStatic.close();
		componentProviderStatic.close();
		ResourceProvider.nClientBundleSupplier = originalNClientSupplier;
		ResourceProvider.sabloBundleSupplier = originalSabloSupplier;
		ResourceProvider.servoyApplicationServerDirSupplier = originalDirSupplier;
	}

	/** writes tempDir/relativeDir/META-INF/MANIFEST.MF and returns that file */
	private File manifest(String relativeDir, String attributes) throws IOException
	{
		File file = new File(new File(tempDir, relativeDir), "META-INF/MANIFEST.MF");
		Files.createDirectories(file.getParentFile().toPath());
		Files.writeString(file.toPath(), "Manifest-Version: 1.0\n" + attributes + "\n\n");
		return file;
	}

	private File file(String relativePath, String content) throws IOException
	{
		File file = new File(tempDir, relativePath);
		Files.createDirectories(file.getParentFile().toPath());
		Files.writeString(file.toPath(), content);
		return file;
	}

	private static IPackageReader reader(String packageName)
	{
		IPackageReader reader = mock(IPackageReader.class);
		when(reader.getPackageName()).thenReturn(packageName);
		return reader;
	}

	@Nested
	class DefaultPackageNames
	{
		@Test
		@DisplayName("only package directories of /war/ are returned, not js, css, templates or plain files")
		void onlyPackageDirectories()
		{
			when(nclient.getEntryPaths("/war/")).thenAnswer(
				inv -> Collections.enumeration(List.of("war/js/", "war/css/", "war/templates/", "war/index.html", "war/button/", "war/ngservices/")));

			assertEquals(java.util.Set.of("button", "ngservices"), ResourceProvider.getDefaultPackageNames());
		}
	}

	@Nested
	class ComputeUrl
	{
		@Test
		@DisplayName("when the nclient bundle has no entry the sablo bundle META-INF/resources is used, with or without leading slash")
		void fallsBackToSablo() throws Exception
		{
			URL url = new File(tempDir, "sablo.js").toURI().toURL();
			when(sablo.getEntry("/META-INF/resources/js/x.js")).thenReturn(url);

			assertAll(
				() -> assertSame(url, ResourceProvider.computeURL("/js/x.js", nclient)),
				() -> assertSame(url, ResourceProvider.computeURL("js/x.js", nclient)));
		}

		@Test
		@DisplayName("a bundle that throws on getEntry does not stop the lookup in sablo")
		void bundleThrows() throws Exception
		{
			URL url = new File(tempDir, "sablo.js").toURI().toURL();
			when(nclient.getEntry(anyString())).thenThrow(new IllegalStateException("bundle is gone"));
			when(sablo.getEntry("/META-INF/resources/js/x.js")).thenReturn(url);

			assertSame(url, ResourceProvider.computeURL("/js/x.js", nclient));
		}

		@Test
		@DisplayName("a component package reader is asked for the path below the package name, which is url decoded")
		void componentPackageReader() throws Exception
		{
			URL url = new File(tempDir, "a.png").toURI().toURL();
			IPackageReader reader = mock(IPackageReader.class);
			when(reader.getUrlForPath("/images/a.png")).thenReturn(url);
			when(componentState.getPackageReader("my pkg")).thenReturn(reader);

			assertSame(url, ResourceProvider.computeURL("/my%20pkg/images/a.png", nclient));
			verify(serviceState, never()).getPackageReader(anyString());
		}

		@Test
		@DisplayName("a service package reader is used when no component package has that name")
		void servicePackageReader() throws Exception
		{
			URL url = new File(tempDir, "svc.js").toURI().toURL();
			IPackageReader reader = mock(IPackageReader.class);
			when(reader.getUrlForPath("/svc.js")).thenReturn(url);
			when(serviceState.getPackageReader("mysvc")).thenReturn(reader);

			assertSame(url, ResourceProvider.computeURL("/mysvc/svc.js", nclient));
		}

		@Test
		@DisplayName("a package that does not contain the path gives no url")
		void pathNotInPackage() throws Exception
		{
			IPackageReader reader = mock(IPackageReader.class);
			when(componentState.getPackageReader("mypkg")).thenReturn(reader);

			assertNull(ResourceProvider.computeURL("/mypkg/missing.js", nclient));
			verify(reader).getUrlForPath("/missing.js");
		}

		@Test
		@DisplayName("a service package that does not contain the path gives no url")
		void pathNotInServicePackage() throws Exception
		{
			IPackageReader reader = mock(IPackageReader.class);
			when(serviceState.getPackageReader("mysvc")).thenReturn(reader);

			assertNull(ResourceProvider.computeURL("/mysvc/missing.js", nclient));
			verify(reader).getUrlForPath("/missing.js");
		}

		@Test
		@DisplayName("a single path segment or a package directory only is not looked up in the packages")
		void notAPackagePath() throws Exception
		{
			assertAll(
				() -> assertNull(ResourceProvider.computeURL("/single", nclient)),
				() -> assertNull(ResourceProvider.computeURL("/pkg/", nclient)));
			verify(componentState, never()).getPackageReader(anyString());
			verify(serviceState, never()).getPackageReader(anyString());
		}
	}

	@Nested
	class DoFilter
	{
		private final HttpServletRequest request = mock(HttpServletRequest.class);
		private final HttpServletResponse response = mock(HttpServletResponse.class);
		private final FilterChain chain = mock(FilterChain.class);
		private final ByteArrayOutputStream body = new ByteArrayOutputStream();
		private final StringWriter written = new StringWriter();

		@BeforeEach
		void stubOutput() throws IOException
		{
			when(response.getOutputStream()).thenReturn(new ServletOutputStream()
			{
				@Override
				public void write(int b)
				{
					body.write(b);
				}

				@Override
				public boolean isReady()
				{
					return true;
				}

				@Override
				public void setWriteListener(WriteListener listener)
				{
				}
			});
			when(response.getWriter()).thenReturn(new PrintWriter(written));
		}

		private void serve(String uri) throws IOException, ServletException
		{
			when(request.getRequestURI()).thenReturn(uri);
			new ResourceProvider().doFilter(request, response, chain);
		}

		@Test
		@DisplayName("a file in the nclient bundle is streamed with its content type and length")
		void servesFromBundle() throws Exception
		{
			File css = file("bundle/a.css", "body{}");
			when(nclient.getEntry("/war/css/a.css")).thenReturn(css.toURI().toURL());

			serve("/css/a.css");

			assertAll(
				() -> verify(response).setContentType("text/css"),
				() -> verify(response).setContentLength(6),
				() -> assertArrayEquals("body{}".getBytes(UTF_8), body.toByteArray()),
				() -> verify(chain, never()).doFilter(any(), any()));
		}

		@Test
		@DisplayName("a missing template in the application server folder is served from the bundle")
		void templateFallsBackToBundle() throws Exception
		{
			File html = file("bundle/t.html", "<i/>");
			when(nclient.getEntry("/war/templates/t.html")).thenReturn(html.toURI().toURL());

			serve("/templates/t.html");

			assertAll(
				() -> verify(response).setContentType("text/html"),
				() -> assertArrayEquals("<i/>".getBytes(UTF_8), body.toByteArray()));
		}

		@Test
		@DisplayName("an entry inside a jar is streamed with the size and content type of the jar entry")
		void servesJarEntry() throws Exception
		{
			File jar = new File(tempDir, "res.jar");
			try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar.toPath())))
			{
				out.putNextEntry(new JarEntry("war/css/in.css"));
				out.write("p{margin:0}".getBytes(UTF_8));
				out.closeEntry();
			}
			when(nclient.getEntry("/war/css/in.css")).thenReturn(java.net.URI.create("jar:" + jar.toURI().toURL() + "!/war/css/in.css").toURL());

			serve("/css/in.css");

			assertAll(
				() -> verify(response).setContentType("text/css"),
				() -> verify(response).setContentLength(11),
				() -> assertArrayEquals("p{margin:0}".getBytes(UTF_8), body.toByteArray()));
		}

		@Test
		@DisplayName("a .less file is compiled and the css is written with text/css and its length")
		void compilesLess() throws Exception
		{
			File less = file("bundle/a.less", "@c: red; a{color:@c}");
			when(nclient.getEntry("/war/css/a.less")).thenReturn(less.toURI().toURL());

			try (MockedStatic<LessCompiler> compiler = mockStatic(LessCompiler.class))
			{
				compiler.when(() -> LessCompiler.compileLess(any(InputStream.class))).thenReturn("a{color:red}");

				serve("/css/a.less");
			}

			assertAll(
				() -> verify(response, atLeastOnce()).setContentType("text/css"),
				() -> verify(response).setContentLength(12),
				() -> assertEquals("a{color:red}", written.toString()),
				() -> assertEquals(0, body.size()));
		}

		@Test
		@DisplayName("a .less file that cannot be compiled is streamed as is")
		void lessNotCompiled() throws Exception
		{
			File less = file("bundle/b.less", "a{b:c}");
			when(nclient.getEntry("/war/css/b.less")).thenReturn(less.toURI().toURL());

			try (MockedStatic<LessCompiler> compiler = mockStatic(LessCompiler.class))
			{
				compiler.when(() -> LessCompiler.compileLess(any(InputStream.class))).thenReturn(null);

				serve("/css/b.less");
			}

			assertAll(
				() -> assertArrayEquals("a{b:c}".getBytes(UTF_8), body.toByteArray()),
				() -> assertEquals("", written.toString()));
		}

		@Test
		@DisplayName("a resource of a package reader is served when the bundles do not have it")
		void servesFromPackageReader() throws Exception
		{
			File css = file("pkg/p.css", "i{}");
			IPackageReader reader = mock(IPackageReader.class);
			when(reader.getUrlForPath("/p.css")).thenReturn(css.toURI().toURL());
			when(componentState.getPackageReader("mypkg")).thenReturn(reader);

			serve("/mypkg/p.css");

			assertArrayEquals("i{}".getBytes(UTF_8), body.toByteArray());
		}

		@Test
		@DisplayName("an unknown resource is passed on to the filter chain and the response stays untouched")
		void missPassesThrough() throws Exception
		{
			serve("/nothing/here.txt");

			assertAll(
				() -> verify(chain).doFilter(request, response),
				() -> verify(response, never()).setContentType(anyString()),
				() -> assertEquals(0, body.size()));
		}

		@Test
		@DisplayName("a path that climbs out of /templates is not served from the templates folder")
		void templateTraversalIsNotServed() throws Exception
		{
			file("server/webapps/ROOT/secret.txt", "secret");

			serve("/templates/../secret.txt");

			assertAll(
				() -> verify(chain).doFilter(request, response),
				() -> assertEquals(0, body.size()));
		}
	}

	@Nested
	class SpecProviderInit
	{
		private MockedStatic<WebsocketSessionManager> sessionManager;
		private MockedStatic<Types> types;
		private Types typesInstance;

		@BeforeEach
		void mockStatics()
		{
			sessionManager = mockStatic(WebsocketSessionManager.class);
			types = mockStatic(Types.class);
			typesInstance = mock(Types.class);
			types.when(Types::getTypesInstance).thenReturn(typesInstance);
		}

		@AfterEach
		void closeStatics()
		{
			types.close();
			sessionManager.close();
		}

		private URL packageManifest(String dir, String attributes) throws IOException
		{
			return manifest(dir, attributes).toURI().toURL();
		}

		private void bundleHasManifests(URL... urls)
		{
			when(nclient.findEntries("/war/", "MANIFEST.MF", true)).thenAnswer(inv -> Collections.enumeration(Arrays.asList(urls)));
		}

		private IPackageReader[] initAndCaptureComponents() throws ServletException
		{
			new ResourceProvider().init(null);
			ArgumentCaptor<IPackageReader[]> captor = ArgumentCaptor.forClass(IPackageReader[].class);
			componentProviderStatic.verify(() -> WebComponentSpecProvider.init(captor.capture(), eq(DefaultComponentPropertiesProvider.instance)));
			return captor.getValue();
		}

		private IPackageReader[] captureServices()
		{
			ArgumentCaptor<IPackageReader[]> captor = ArgumentCaptor.forClass(IPackageReader[].class);
			serviceProviderStatic.verify(() -> WebServiceSpecProvider.init(captor.capture()));
			return captor.getValue();
		}

		private String[] names(IPackageReader[] readers)
		{
			return Arrays.stream(readers).map(IPackageReader::getPackageName).toArray(String[]::new);
		}

		@Test
		@DisplayName("init registers the types, splits the bundle packages in components and services and adds sablo to both")
		void initSplitsPackages() throws Exception
		{
			bundleHasManifests(packageManifest("war/button", "Bundle-SymbolicName: button"),
				packageManifest("war/ngservices", "Bundle-SymbolicName: ngservices;singleton:=true"));

			IPackageReader[] components = initAndCaptureComponents();
			IPackageReader[] services = captureServices();

			assertAll(
				() -> verify(typesInstance).registerTypes(),
				() -> assertArrayEquals(new String[] { "button", "sablo" }, names(components)),
				() -> assertArrayEquals(new String[] { "ngservices", "sablo" }, names(services)),
				() -> assertSame(components[1], services[1]));
		}

		@Test
		@DisplayName("packages marked as removed are left out of both spec providers")
		void removedPackagesAreSkipped() throws Exception
		{
			bundleHasManifests(packageManifest("war/button", "Bundle-SymbolicName: button"),
				packageManifest("war/table", "Bundle-SymbolicName: table"),
				packageManifest("war/oldservices", "Bundle-SymbolicName: oldservices"));
			ResourceProvider.setRemovedPackages(List.of("table", "oldservices"));

			IPackageReader[] components = initAndCaptureComponents();
			IPackageReader[] services = captureServices();

			assertAll(
				() -> assertArrayEquals(new String[] { "button", "sablo" }, names(components)),
				() -> assertArrayEquals(new String[] { "sablo" }, names(services)));
		}

		@Test
		@DisplayName("a session factory is registered for the client endpoint when there is none")
		void registersSessionFactory() throws Exception
		{
			sessionManager.when(() -> WebsocketSessionManager.getWebsocketSessionFactory(WebsocketSessionFactory.CLIENT_ENDPOINT)).thenReturn(null);

			new ResourceProvider().init(null);

			sessionManager.verify(() -> WebsocketSessionManager.setWebsocketSessionFactory(eq(WebsocketSessionFactory.CLIENT_ENDPOINT),
				any(WebsocketSessionFactory.class)));
		}

		@Test
		@DisplayName("an already registered session factory is not replaced")
		void keepsExistingSessionFactory() throws Exception
		{
			sessionManager.when(() -> WebsocketSessionManager.getWebsocketSessionFactory(WebsocketSessionFactory.CLIENT_ENDPOINT))
				.thenReturn(mock(IWebsocketSessionFactory.class));

			new ResourceProvider().init(null);

			sessionManager.verify(() -> WebsocketSessionManager.setWebsocketSessionFactory(anyString(), any()), never());
		}

		@Test
		@DisplayName("setPackages without initialized spec providers initializes them including the given packages")
		void setPackagesInitializesProviders() throws Exception
		{
			componentProviderStatic.when(WebComponentSpecProvider::getInstance).thenReturn(null);
			serviceProviderStatic.when(WebServiceSpecProvider::getInstance).thenReturn(null);
			bundleHasManifests(packageManifest("war/button", "Bundle-SymbolicName: button"));
			IPackageReader extraComponent = reader("extra");
			when(extraComponent.getPackageType()).thenReturn(IPackageReader.WEB_COMPONENT);
			IPackageReader extraService = reader("extraservice");
			when(extraService.getPackageType()).thenReturn(IPackageReader.WEB_SERVICE);

			ResourceProvider.setPackages(List.of(List.of(extraComponent, extraService)));

			ArgumentCaptor<IPackageReader[]> components = ArgumentCaptor.forClass(IPackageReader[].class);
			componentProviderStatic.verify(() -> WebComponentSpecProvider.init(components.capture(), any()));
			assertAll(
				() -> assertArrayEquals(new String[] { "extra", "button", "sablo" }, names(components.getValue())),
				() -> assertArrayEquals(new String[] { "extraservice", "sablo" }, names(captureServices())));
		}

		@Test
		@DisplayName("updatePackageResources without initialized spec providers initializes them including the added packages")
		void updateInitializesProviders() throws Exception
		{
			componentProviderStatic.when(WebComponentSpecProvider::getInstance).thenReturn(null);
			IPackageReader added = reader("added");

			ResourceProvider.updatePackageResources(List.of(), List.of(added), List.of(), List.of());

			ArgumentCaptor<IPackageReader[]> components = ArgumentCaptor.forClass(IPackageReader[].class);
			componentProviderStatic.verify(() -> WebComponentSpecProvider.init(components.capture(), any()));
			assertArrayEquals(new String[] { "added", "sablo" }, names(components.getValue()));
		}

		@Test
		@DisplayName("the bundle package reader gets name, display name, version and type from its manifest")
		void readerManifestValues() throws Exception
		{
			File manifest = manifest("war/button",
				"Bundle-SymbolicName: button;singleton:=true\nBundle-Name: Button Package\nBundle-Version: 1.2.3\nPackage-Type: " +
					IPackageReader.WEB_COMPONENT);
			bundleHasManifests(manifest.toURI().toURL());

			IPackageReader reader = initAndCaptureComponents()[0];

			assertAll(
				() -> assertEquals("button", reader.getPackageName()),
				() -> assertEquals("Button Package", reader.getPackageDisplayname()),
				() -> assertEquals("1.2.3", reader.getVersion()),
				() -> assertEquals(IPackageReader.WEB_COMPONENT, reader.getPackageType()),
				() -> assertEquals("Button Package", reader.getManifest().getMainAttributes().getValue("Bundle-Name")),
				() -> assertEquals(manifest.toURI().toURL().toExternalForm(), reader.getName()),
				() -> assertTrue(reader.toString().contains(reader.getName())),
				() -> assertNull(reader.getPackageURL()),
				() -> assertNull(reader.getResource()));
		}

		@Test
		@DisplayName("without a Bundle-Name the display name is the package name")
		void displayNameFallsBackToPackageName() throws Exception
		{
			bundleHasManifests(packageManifest("war/button", "Bundle-SymbolicName: button"));

			IPackageReader reader = initAndCaptureComponents()[0];

			assertEquals("button", reader.getPackageDisplayname());
		}

		@Test
		@DisplayName("a manifest that cannot be read gives the directory name, no version and no package type")
		void unreadableManifest() throws Exception
		{
			File missing = new File(new File(tempDir, "war/ghost"), "META-INF/MANIFEST.MF");
			bundleHasManifests(missing.toURI().toURL());

			IPackageReader reader = initAndCaptureComponents()[0];

			assertAll(
				() -> assertEquals("ghost", reader.getPackageName()),
				() -> assertEquals("ghost", reader.getPackageDisplayname()),
				() -> assertNull(reader.getVersion()),
				() -> assertNull(reader.getPackageType()));
		}

		@Test
		@DisplayName("paths of the bundle package reader are resolved below its directory in the bundle, without adding the prefix twice")
		void readerUrlForPath() throws Exception
		{
			File manifest = manifest("war/button", "Bundle-SymbolicName: button");
			String prefix = manifest.toURI().toURL().getPath();
			prefix = prefix.substring(0, prefix.indexOf(MANIFEST_PATH));
			URL entry = new File(tempDir, "war/button/js/a.js").toURI().toURL();
			when(nclient.getEntry(prefix + "/js/a.js")).thenReturn(entry);
			bundleHasManifests(manifest.toURI().toURL());
			IPackageReader reader = initAndCaptureComponents()[0];

			String fullPath = prefix + "/js/a.js";
			assertAll(
				() -> assertSame(entry, reader.getUrlForPath("js/a.js")),
				() -> assertSame(entry, reader.getUrlForPath("/js/a.js")),
				() -> assertSame(entry, reader.getUrlForPath(fullPath)),
				() -> assertNull(reader.getUrlForPath("/js/other.js")));
		}

		@Test
		@DisplayName("readTextFile returns the text of the bundle entry and null for a missing entry")
		void readerReadTextFile() throws Exception
		{
			File manifest = manifest("war/button", "Bundle-SymbolicName: button");
			String prefix = manifest.toURI().toURL().getPath();
			prefix = prefix.substring(0, prefix.indexOf(MANIFEST_PATH));
			File text = file("war/button/spec.txt", "h\u00e9llo");
			when(nclient.getEntry(prefix + "/spec.txt")).thenReturn(text.toURI().toURL());
			bundleHasManifests(manifest.toURI().toURL());
			IPackageReader reader = initAndCaptureComponents()[0];

			assertAll(
				() -> assertEquals("h\u00e9llo", reader.readTextFile("/spec.txt", UTF_8)),
				() -> assertNull(reader.readTextFile("/missing.txt", UTF_8)));
		}

		@Test
		@DisplayName("the sablo reader resolves its paths below META-INF/resources of the sablo bundle")
		void sabloReaderUrlForPath() throws Exception
		{
			URL entry = new File(tempDir, "sablo/x.js").toURI().toURL();
			when(sablo.getEntry("META-INF/resources/x.js")).thenReturn(entry);

			IPackageReader sabloReader = initAndCaptureComponents()[0];

			assertSame(entry, sabloReader.getUrlForPath("x.js"));
		}

		@Test
		@DisplayName("the component and service packages that were set are handed to the spec providers on init, before the bundle packages")
		void setPackagesKeptOnInit() throws Exception
		{
			IPackageReader extra = reader("extra");
			when(extra.getPackageType()).thenReturn(IPackageReader.WEB_COMPONENT);
			ResourceProvider.setPackages(List.of(List.of(extra)));
			bundleHasManifests(packageManifest("war/button", "Bundle-SymbolicName: button"));

			IPackageReader[] components = initAndCaptureComponents();

			assertArrayEquals(new String[] { "extra", "button", "sablo" }, names(components));
		}
	}
}
