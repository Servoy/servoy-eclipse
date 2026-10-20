package com.servoy.eclipse.ngclient.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WebPackagesListener}'s two pure, deterministic string rules (SVY-21502):
 * {@code rewriteCssLibUrl(String, boolean, String[])} and {@code publicTarballName(String)}.
 * <p>
 * Both are package-private statics on {@link WebPackagesListener}; this test lives in the same package and calls them
 * directly (no reflection). They were extracted out of the OSGi job so they can be tested without an Activator /
 * workspace context. The copy-into-{@code .tarballs/}, the {@code checkPackage} alias-vs-install branch, the
 * seeding/commit-back flow and the actual Angular build all need a live install and are integration-level - out of
 * scope for this unit test.
 * </p>
 * <p>
 * The internal default component package set that the CSS rule treats as source packages (rewritten to the live
 * {@code ./projects/<name>/} path) is the SORTED {@code @servoy/*} set below - a local copy of
 * {@code WebPackagesListener.defaultPackages} (which is private); {@code rewriteCssLibUrl} binary-searches it, so it
 * must stay sorted.
 * </p>
 */
class WebPackagesListenerCssLibTest
{
	/** Sorted copy of the production {@code WebPackagesListener.defaultPackages} (private there). */
	private static final String[] INTERNAL = new String[] { "@servoy/dialogs", "@servoy/ngclientutils", "@servoy/servoydefault", "@servoy/window" };

	@Nested
	@DisplayName("rewriteCssLibUrl")
	class RewriteCssLibUrl
	{
		@Test
		@DisplayName("internal component CSS in pnpm mode is rewritten to the live ./projects/<name>/ source")
		void internalRewrittenToProjects()
		{
			assertAll(
				() -> assertEquals("./projects/window/servoy-menu.css",
					WebPackagesListener.rewriteCssLibUrl("~@servoy/window/servoy-menu.css", true, INTERNAL)),
				() -> assertEquals("./projects/servoydefault/defaultcomponents.css",
					WebPackagesListener.rewriteCssLibUrl("~@servoy/servoydefault/defaultcomponents.css", true, INTERNAL)));
		}

		@Test
		@DisplayName("a nested CSS path under an internal component keeps the rest of the path")
		void internalNestedPathPreserved()
		{
			assertEquals("./projects/servoydefault/assets/sub/dir/x.css",
				WebPackagesListener.rewriteCssLibUrl("~@servoy/servoydefault/assets/sub/dir/x.css", true, INTERNAL));
		}

		@Test
		@DisplayName("an external @servoy component keeps the ./node_modules/ mapping even in pnpm mode")
		void externalServoyStaysNodeModules()
		{
			assertAll(
				() -> assertEquals("./node_modules/@servoy/bootstrapcomponents/svy_bootstrapcomponents.css",
					WebPackagesListener.rewriteCssLibUrl("~@servoy/bootstrapcomponents/svy_bootstrapcomponents.css", true, INTERNAL)),
				() -> assertEquals("./node_modules/@servoy/12grid/12grid.css",
					WebPackagesListener.rewriteCssLibUrl("~@servoy/12grid/12grid.css", true, INTERNAL)));
		}

		@Test
		@DisplayName("a non-@servoy package keeps the ./node_modules/ mapping")
		void thirdPartyStaysNodeModules()
		{
			assertAll(
				() -> assertEquals("./node_modules/bootstrap/dist/css/bootstrap.min.css",
					WebPackagesListener.rewriteCssLibUrl("~bootstrap/dist/css/bootstrap.min.css", true, INTERNAL)),
				() -> assertEquals("./node_modules/@eonasdan/tempus-dominus/dist/css/tempus-dominus.css",
					WebPackagesListener.rewriteCssLibUrl("~@eonasdan/tempus-dominus/dist/css/tempus-dominus.css", true, INTERNAL)));
		}

		@Test
		@DisplayName("in npm mode (pnpmMode=false) even an internal component keeps the ./node_modules/ mapping")
		void npmModeStaysNodeModules()
		{
			assertEquals("./node_modules/@servoy/window/servoy-menu.css",
				WebPackagesListener.rewriteCssLibUrl("~@servoy/window/servoy-menu.css", false, INTERNAL));
		}

		@Test
		@DisplayName("a url without the ~ prefix is returned unchanged (no node_modules injection)")
		void noTildePassThrough()
		{
			assertAll(
				() -> assertEquals("@fortawesome/fontawesome-free/css/all.css",
					WebPackagesListener.rewriteCssLibUrl("@fortawesome/fontawesome-free/css/all.css", true, INTERNAL)),
				() -> assertEquals("src/styles.css",
					WebPackagesListener.rewriteCssLibUrl("src/styles.css", true, INTERNAL)));
		}

		@Test
		@DisplayName("a bare ~@servoy/<name> with no file part is not mis-rewritten (keeps node_modules)")
		void internalWithoutFilePartNotRewritten()
		{
			// only one slash after the scope -> no <rest>, so the projects/ rewrite does not apply
			assertEquals("./node_modules/@servoy/window",
				WebPackagesListener.rewriteCssLibUrl("~@servoy/window", true, INTERNAL));
		}

		@Test
		@DisplayName("null url returns null; null internal set falls back to node_modules")
		void nullHandling()
		{
			assertAll(
				() -> assertNull(WebPackagesListener.rewriteCssLibUrl(null, true, INTERNAL)),
				() -> assertEquals("./node_modules/@servoy/window/servoy-menu.css",
					WebPackagesListener.rewriteCssLibUrl("~@servoy/window/servoy-menu.css", true, null)));
		}
	}

	@Nested
	@DisplayName("publicTarballName")
	class PublicTarballName
	{
		@Test
		@DisplayName("a version yields servoy-public-<version>.tgz (matches the Maven pack_public name)")
		void versionedName()
		{
			assertAll(
				() -> assertEquals("servoy-public-2026.12.0.tgz", WebPackagesListener.publicTarballName("2026.12.0")),
				() -> assertEquals("servoy-public-2026.9.5.tgz", WebPackagesListener.publicTarballName("2026.9.5")));
		}

		@Test
		@DisplayName("a null version falls back to the unversioned servoy-public.tgz")
		void nullVersionFallback()
		{
			assertEquals("servoy-public.tgz", WebPackagesListener.publicTarballName(null));
		}
	}
}
