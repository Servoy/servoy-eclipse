package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.servoy.eclipse.ngclient.ui.WebPackagesListener;

/**
 * SVY-21502: only nightly (-pre) web packages may be re-resolved (non-frozen) on a WAR/CI export when they differ from
 * the committed pnpm-lock.yaml; releases and release candidates must match the lockfile.
 */
class WebPackagesListenerNightlyVersionTest
{
	@Test
	void preVersionsAreNightlies()
	{
		assertTrue(WebPackagesListener.isNightlyVersion("2026.9.1-pre"));
		assertTrue(WebPackagesListener.isNightlyVersion("2026.9.3-pre.2"));
		assertTrue(WebPackagesListener.isNightlyVersion("2026.9.3-pre2"));
		assertTrue(WebPackagesListener.isNightlyVersion("2026.9.3-pre+build.5"));
	}

	@Test
	void releasesAndReleaseCandidatesAreNotNightlies()
	{
		assertFalse(WebPackagesListener.isNightlyVersion("1.0.0"));
		assertFalse(WebPackagesListener.isNightlyVersion("2026.9.1"));
		assertFalse(WebPackagesListener.isNightlyVersion("2026.9.1-rc1"));
		assertFalse(WebPackagesListener.isNightlyVersion("2026.9.1-rc.1"));
		assertFalse(WebPackagesListener.isNightlyVersion("2026.9.1-preview"));
		assertFalse(WebPackagesListener.isNightlyVersion("2026.9.1+pre"));
		assertFalse(WebPackagesListener.isNightlyVersion(null));
	}
}
