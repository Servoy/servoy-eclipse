package com.servoy.eclipse.ngclient.startup.resourceprovider.plainjunit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.ngclient.startup.resourceprovider.ResourceProvider;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class ResourceProviderTest
{
	@TempDir
	File tempDir;

	private MockedStatic<ApplicationServerRegistry> registry;
	private MockedStatic<WebComponentSpecProvider> componentProviderStatic;
	private MockedStatic<WebServiceSpecProvider> serviceProviderStatic;
	private WebComponentSpecProvider componentProvider;
	private WebServiceSpecProvider serviceProvider;

	@BeforeEach
	void setUp()
	{
		registry = mockStatic(ApplicationServerRegistry.class);
		IApplicationServerSingleton server = mock(IApplicationServerSingleton.class);
		when(server.getServoyApplicationServerDirectory()).thenReturn(tempDir.getAbsolutePath());
		registry.when(ApplicationServerRegistry::get).thenReturn(server);

		componentProvider = mock(WebComponentSpecProvider.class);
		serviceProvider = mock(WebServiceSpecProvider.class);
		componentProviderStatic = mockStatic(WebComponentSpecProvider.class);
		serviceProviderStatic = mockStatic(WebServiceSpecProvider.class);
		componentProviderStatic.when(WebComponentSpecProvider::getInstance).thenReturn(componentProvider);
		serviceProviderStatic.when(WebServiceSpecProvider::getInstance).thenReturn(serviceProvider);
	}

	@AfterEach
	void tearDown()
	{
		// the reader maps are static: empty them so no test leaks state into another
		ResourceProvider.setPackages(List.of());
		serviceProviderStatic.close();
		componentProviderStatic.close();
		registry.close();
	}

	private static IPackageReader reader(String packageName, String type, File resource)
	{
		IPackageReader reader = mock(IPackageReader.class);
		when(reader.getPackageName()).thenReturn(packageName);
		when(reader.getName()).thenReturn(packageName);
		when(reader.getPackageType()).thenReturn(type);
		when(reader.getResource()).thenReturn(resource);
		return reader;
	}

	@SuppressWarnings("unchecked")
	private static Collection<IPackageReader> captureRemoved(org.sablo.specification.BaseSpecProvider provider, int invocation)
	{
		ArgumentCaptor<Collection<IPackageReader>> removed = ArgumentCaptor.forClass(Collection.class);
		verify(provider, org.mockito.Mockito.atLeast(invocation)).updatePackages(removed.capture(), any());
		return removed.getAllValues().get(invocation - 1);
	}

	@Nested
	class SetPackages
	{
		@Test
		@DisplayName("web services and components are filed separately and found by their resource file")
		void splitByPackageType()
		{
			File componentFile = new File(tempDir, "comp.zip");
			File serviceFile = new File(tempDir, "svc.zip");
			IPackageReader component = reader("comp", IPackageReader.WEB_COMPONENT, componentFile);
			IPackageReader service = reader("svc", IPackageReader.WEB_SERVICE, serviceFile);

			ResourceProvider.setPackages(List.of(List.of(component, service)));

			assertSame(component, ResourceProvider.getComponentPackageReader(componentFile));
			assertSame(service, ResourceProvider.getServicePackageReader(serviceFile));
			assertNull(ResourceProvider.getComponentPackageReader(serviceFile));
			assertNull(ResourceProvider.getServicePackageReader(componentFile));
		}

		@Test
		@DisplayName("readers without a resource file are never matched")
		void readerWithoutResource()
		{
			IPackageReader component = reader("comp", IPackageReader.WEB_COMPONENT, null);

			ResourceProvider.setPackages(List.of(List.of(component)));

			assertNull(ResourceProvider.getComponentPackageReader(new File(tempDir, "comp.zip")));
		}

		@Test
		@DisplayName("a second call replaces the first set and hands the old readers to the spec providers for removal")
		void secondCallReplacesFirst()
		{
			File oldFile = new File(tempDir, "old.zip");
			File newFile = new File(tempDir, "new.zip");
			IPackageReader oldComponent = reader("old", IPackageReader.WEB_COMPONENT, oldFile);
			IPackageReader newComponent = reader("new", IPackageReader.WEB_COMPONENT, newFile);
			ResourceProvider.setPackages(List.of(List.of(oldComponent)));

			ResourceProvider.setPackages(List.of(List.of(newComponent)));

			assertNull(ResourceProvider.getComponentPackageReader(oldFile));
			assertSame(newComponent, ResourceProvider.getComponentPackageReader(newFile));
			assertEquals(List.of(oldComponent), new ArrayList<>(captureRemoved(componentProvider, 2)));
		}

		@Test
		@DisplayName("the spec providers receive the new component and service readers to add")
		void providersGetNewReaders()
		{
			IPackageReader component = reader("comp", IPackageReader.WEB_COMPONENT, null);
			IPackageReader service = reader("svc", IPackageReader.WEB_SERVICE, null);

			ResourceProvider.setPackages(List.of(List.of(component, service)));

			verify(componentProvider).updatePackages(eq(List.of()), eq(List.of(component)));
			verify(serviceProvider).updatePackages(eq(List.of()), eq(List.of(service)));
		}

		@Test
		@DisplayName("readers with the same package name are kept side by side and removed one at a time")
		void packagesWithSameNameShareAKey()
		{
			File first = new File(tempDir, "a.zip");
			File second = new File(tempDir, "b.zip");
			IPackageReader r1 = reader("same", IPackageReader.WEB_COMPONENT, first);
			IPackageReader r2 = reader("same", IPackageReader.WEB_COMPONENT, second);

			ResourceProvider.setPackages(List.of(List.of(r1, r2)));
			ResourceProvider.updatePackageResources(List.of(r1), List.of(), List.of(), List.of());

			assertNull(ResourceProvider.getComponentPackageReader(first));
			assertSame(r2, ResourceProvider.getComponentPackageReader(second));
		}
	}

	@Nested
	class UpdatePackageResources
	{
		@Test
		@DisplayName("removed readers disappear, added readers appear, for components and services")
		void removeAndAdd()
		{
			File oldComponentFile = new File(tempDir, "oldc.zip");
			File oldServiceFile = new File(tempDir, "olds.zip");
			File newComponentFile = new File(tempDir, "newc.zip");
			File newServiceFile = new File(tempDir, "news.zip");
			IPackageReader oldComponent = reader("oldc", IPackageReader.WEB_COMPONENT, oldComponentFile);
			IPackageReader oldService = reader("olds", IPackageReader.WEB_SERVICE, oldServiceFile);
			IPackageReader newComponent = reader("newc", IPackageReader.WEB_COMPONENT, newComponentFile);
			IPackageReader newService = reader("news", IPackageReader.WEB_SERVICE, newServiceFile);
			ResourceProvider.setPackages(List.of(List.of(oldComponent, oldService)));

			ResourceProvider.updatePackageResources(List.of(oldComponent), List.of(newComponent), List.of(oldService), List.of(newService));

			assertNull(ResourceProvider.getComponentPackageReader(oldComponentFile));
			assertNull(ResourceProvider.getServicePackageReader(oldServiceFile));
			assertSame(newComponent, ResourceProvider.getComponentPackageReader(newComponentFile));
			assertSame(newService, ResourceProvider.getServicePackageReader(newServiceFile));
		}

		@Test
		@DisplayName("the existing spec providers are told about exactly the removed and added readers")
		void providersAreUpdated()
		{
			IPackageReader oldComponent = reader("oldc", IPackageReader.WEB_COMPONENT, null);
			IPackageReader oldService = reader("olds", IPackageReader.WEB_SERVICE, null);
			IPackageReader newComponent = reader("newc", IPackageReader.WEB_COMPONENT, null);
			IPackageReader newService = reader("news", IPackageReader.WEB_SERVICE, null);
			ResourceProvider.setPackages(List.of(List.of(oldComponent, oldService)));

			List<IPackageReader> componentsToRemove = List.of(oldComponent);
			List<IPackageReader> componentsToAdd = List.of(newComponent);
			List<IPackageReader> servicesToRemove = List.of(oldService);
			List<IPackageReader> servicesToAdd = List.of(newService);
			ResourceProvider.updatePackageResources(componentsToRemove, componentsToAdd, servicesToRemove, servicesToAdd);

			verify(componentProvider).updatePackages(componentsToRemove, componentsToAdd);
			verify(serviceProvider).updatePackages(servicesToRemove, servicesToAdd);
		}
	}

	@Nested
	class ComputeURL
	{
		@Test
		@DisplayName("a path with a leading slash is looked up below /war in the bundle")
		void leadingSlash() throws Exception
		{
			Bundle bundle = mock(Bundle.class);
			URL url = new File(tempDir, "x.js").toURI().toURL();
			when(bundle.getEntry("/war/js/x.js")).thenReturn(url);

			assertSame(url, ResourceProvider.computeURL("/js/x.js", bundle));
		}

		@Test
		@DisplayName("a path without a leading slash gets the /war/ prefix")
		void noLeadingSlash() throws Exception
		{
			Bundle bundle = mock(Bundle.class);
			URL url = new File(tempDir, "x.js").toURI().toURL();
			when(bundle.getEntry("/war/js/x.js")).thenReturn(url);

			assertSame(url, ResourceProvider.computeURL("js/x.js", bundle));
		}
	}

	@Nested
	class DoFilter
	{
		private final HttpServletRequest request = mock(HttpServletRequest.class);
		private final HttpServletResponse response = mock(HttpServletResponse.class);
		private final FilterChain chain = mock(FilterChain.class);
		private final ByteArrayOutputStream body = new ByteArrayOutputStream();

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
		}

		private File template(String name, String content) throws IOException
		{
			File dir = new File(tempDir, "server/webapps/ROOT/templates");
			Files.createDirectories(dir.toPath());
			File file = new File(dir, name);
			Files.writeString(file.toPath(), content);
			return file;
		}

		@Test
		@DisplayName("the root request is passed on to the chain untouched")
		void rootPassesThrough() throws Exception
		{
			when(request.getRequestURI()).thenReturn("/");

			new ResourceProvider().doFilter(request, response, chain);

			verify(chain).doFilter(request, response);
			verify(response, never()).setContentType(anyString());
		}

		@Test
		@DisplayName("a request without a URI is passed on to the chain")
		void nullUriPassesThrough() throws Exception
		{
			when(request.getRequestURI()).thenReturn(null);

			new ResourceProvider().doFilter(request, response, chain);

			verify(chain).doFilter(request, response);
		}

		@Test
		@DisplayName("an existing template of the application server is served with its content type, length and bytes")
		void servesTemplate() throws Exception
		{
			File file = template("hello.html", "<p>hello</p>");
			when(request.getRequestURI()).thenReturn("/templates/hello.html");

			new ResourceProvider().doFilter(request, response, chain);

			verify(response).setContentType("text/html");
			verify(response).setContentLength((int)file.length());
			assertArrayEquals("<p>hello</p>".getBytes(StandardCharsets.UTF_8), body.toByteArray());
			verify(chain, never()).doFilter(any(), any());
		}
	}
}
