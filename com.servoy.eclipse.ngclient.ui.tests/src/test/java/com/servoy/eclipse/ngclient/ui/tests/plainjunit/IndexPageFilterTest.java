package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import com.servoy.eclipse.ngclient.ui.IndexPageFilter;
import com.servoy.j2db.server.ngclient.AngularIndexPageWriter;
import com.servoy.j2db.server.ngclient.auth.CloudStatelessAccessManager;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class IndexPageFilterTest {
	@TempDir
	Path root;

	private HttpServletRequest request;
	private HttpServletResponse response;
	private FilterChain chain;
	private StringWriter body;

	/**
	 * Overrides the two seams of the filter: the solution project folder and the
	 * bundle entry lookup.
	 */
	private class TestFilter extends IndexPageFilter {
		private final File projectFolder;

		TestFilter(File projectFolder) {
			this.projectFolder = projectFolder;
		}

		@Override
		protected File getSolutionProjectFolder() {
			return projectFolder;
		}

		@Override
		protected URL getBundleEntry(String path) {
			try {
				Path file = root.resolve("bundle" + path);
				Files.createDirectories(file.getParent());
				Files.writeString(file, "<html>" + path + "</html>");
				return file.toUri().toURL();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	@BeforeEach
	void setUp() throws Exception {
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		chain = mock(FilterChain.class);
		body = new StringWriter();
		when(request.getContextPath()).thenReturn("");
		when(response.getWriter()).thenReturn(new PrintWriter(body));
	}

	private void request(String uri) {
		when(request.getRequestURI()).thenReturn(uri);
	}

	@Nested
	class SolutionName {
		@ParameterizedTest
		@CsvSource({ "/solution/myapp/index.html,myapp", "/solution/myapp,myapp", "/designer/solution/myapp/x.js,myapp",
				"/solution/a_b/,a_b" })
		void solutionNameIsTheSegmentAfterTheSolutionPath(String uri, String expected) {
			assertEquals(expected, new TestFilter(null).getSolutionNameFromURI(Paths.get(uri)));
		}

		@ParameterizedTest
		@CsvSource({ "/other/myapp/index.html", "/rfb/angular2/index.html", "/" })
		void nonSolutionPathsHaveNoSolutionName(String uri) {
			assertNull(new TestFilter(null).getSolutionNameFromURI(Paths.get(uri)));
		}
	}

	@Nested
	class DoFilter {
		@Test
		void nonSolutionRequestWithoutBuiltIndexPassesThrough() throws Exception {
			request("/some/other/path.js");
			new TestFilter(null).doFilter(request, response, chain);
			verify(chain).doFilter(request, response);
		}

		@Test
		void requestForAFileThatIsNotInTheBuildPassesThrough() throws Exception {
			File project = Files.createDirectories(root.resolve("project")).toFile();
			Files.createDirectories(root.resolve("project/dist/app/browser"));
			Files.writeString(root.resolve("project/dist/app/browser/index.html"), "<html/>");
			request("/rfb/angular2/missing.js");
			new TestFilter(project).doFilter(request, response, chain);
			verify(chain).doFilter(request, response);
		}

		@Test
		void solutionIndexWithoutBuildOutputShowsTheBuildFailedPage() throws Exception {
			request("/solution/myapp/index.html");
			new TestFilter(null).doFilter(request, response, chain);
			assertAll(() -> assertTrue(body.toString().contains("/resources/buildfailedclient.html"), body.toString()),
					() -> verify(response).setContentType("text/html"),
					() -> verify(response).setCharacterEncoding("UTF-8"),
					() -> verify(chain, never()).doFilter(any(), any()));
		}

		@Test
		void buildFailedPageIsAlsoShownWhenTheProjectHasNoDistFolder() throws Exception {
			File project = Files.createDirectories(root.resolve("project")).toFile();
			request("/solution/myapp/index.html");
			new TestFilter(project).doFilter(request, response, chain);
			assertTrue(body.toString().contains("/resources/buildfailedclient.html"), body.toString());
		}

		@Test
		void builtFileOfTheSolutionIsServedWithItsContentType() throws Exception {
			Path dist = Files.createDirectories(root.resolve("project/dist/app/browser"));
			Files.writeString(dist.resolve("index.html"), "<html/>");
			// the filter resolves the whole request URI below the dist folder
			Files.createDirectories(dist.resolve("solution/myapp"));
			Files.writeString(dist.resolve("solution/myapp/main.js"), "console.log(1);");
			ByteArrayServletOutput out = new ByteArrayServletOutput();
			when(response.getOutputStream()).thenReturn(out);
			request("/solution/myapp/main.js");

			try (MockedStatic<CloudStatelessAccessManager> cloud = mockStatic(CloudStatelessAccessManager.class);
					MockedStatic<AngularIndexPageWriter> writer = mockStatic(AngularIndexPageWriter.class)) {
				cloud.when(() -> CloudStatelessAccessManager.handlePossibleCloudRequest(any(), any(), anyString()))
						.thenReturn(false);
				writer.when(() -> AngularIndexPageWriter.handleShortSolutionRequest(any(), any())).thenReturn(false);
				writer.when(() -> AngularIndexPageWriter.handleDeeplink(any(), any())).thenReturn(false);
				new TestFilter(root.resolve("project").toFile()).doFilter(request, response, chain);
			}

			assertAll(() -> assertEquals("console.log(1);", out.text()),
					() -> verify(chain, never()).doFilter(any(), any()));
		}

		@Test
		void pathTraversalOutsideTheDistFolderPassesThroughInsteadOfServingTheFile() throws Exception {
			Path dist = Files.createDirectories(root.resolve("project/dist/app/browser"));
			Files.writeString(dist.resolve("index.html"), "<html/>");
			Files.writeString(root.resolve("project/secret.txt"), "secret");
			ByteArrayServletOutput out = new ByteArrayServletOutput();
			when(response.getOutputStream()).thenReturn(out);
			request("/solution/myapp/../../../../../secret.txt");

			try (MockedStatic<AngularIndexPageWriter> writer = mockStatic(AngularIndexPageWriter.class)) {
				writer.when(() -> AngularIndexPageWriter.handleShortSolutionRequest(any(), any())).thenReturn(false);
				writer.when(() -> AngularIndexPageWriter.handleDeeplink(any(), any())).thenReturn(false);
				new TestFilter(root.resolve("project").toFile()).doFilter(request, response, chain);
			}

			assertAll(() -> assertEquals("", out.text(), "the file outside of the dist folder must not be served"),
					() -> verify(chain).doFilter(request, response));
		}
	}

	private static class ByteArrayServletOutput extends ServletOutputStream {
		private final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();

		@Override
		public void write(int b) {
			bytes.write(b);
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
		}

		String text() {
			return bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
		}
	}
}
