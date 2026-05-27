/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/

package com.servoy.eclipse.opencode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for the package-visible helper
 * {@link RunOpencodeCommand#buildServoyXdgEnv()}.
 * <p>
 * No OSGi runtime is required â?? the method is pure Java and reads only
 * {@code System} properties.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
public class RunOpencodeXdgEnvTest {
	private static final String ESCAPE_HATCH_PROPERTY = "opencode.use.default.xdg";

	/**
	 * Ensure the escape-hatch property is cleared after every test that sets it.
	 */
	@After
	public void clearEscapeHatch() {
		System.clearProperty(ESCAPE_HATCH_PROPERTY);
	}

	// --- default (no escape hatch) ---

	@Test
	public void buildServoyXdgEnv_returnsExactlyFourEntries() {
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		assertEquals("Expected exactly 4 XDG environment variables (config, data, state, cache)", 4, env.size());
	}

	@Test
	public void buildServoyXdgEnv_containsXdgConfigHome() {
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		assertTrue("Map must contain XDG_CONFIG_HOME", env.containsKey("XDG_CONFIG_HOME"));
	}

	@Test
	public void buildServoyXdgEnv_containsXdgDataHome() {
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		assertTrue("Map must contain XDG_DATA_HOME", env.containsKey("XDG_DATA_HOME"));
	}

	@Test
	public void buildServoyXdgEnv_containsXdgStateHome() {
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		assertTrue("Map must contain XDG_STATE_HOME", env.containsKey("XDG_STATE_HOME"));
	}

	@Test
	public void buildServoyXdgEnv_containsXdgCacheHome() {
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		assertTrue("Map must contain XDG_CACHE_HOME", env.containsKey("XDG_CACHE_HOME"));
	}

	@Test
	public void buildServoyXdgEnv_cacheHomeMatchesExpectedPath() {
		String expected = System.getProperty("user.home") + File.separator + ".servoy";
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		assertEquals("XDG_CACHE_HOME must equal {user.home}/.servoy", expected, env.get("XDG_CACHE_HOME"));
	}

	@Test
	public void buildServoyXdgEnv_allValuesAreEqual() {
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		String config = env.get("XDG_CONFIG_HOME");
		String data = env.get("XDG_DATA_HOME");
		String state = env.get("XDG_STATE_HOME");
		String cache = env.get("XDG_CACHE_HOME");
		assertEquals("XDG_CONFIG_HOME and XDG_DATA_HOME must point to the same directory", config, data);
		assertEquals("XDG_CONFIG_HOME and XDG_STATE_HOME must point to the same directory", config, state);
		assertEquals("XDG_CONFIG_HOME and XDG_CACHE_HOME must point to the same directory", config, cache);
	}

	@Test
	public void buildServoyXdgEnv_pathEndsWithDotServoy() {
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		String configHome = env.get("XDG_CONFIG_HOME");
		assertNotNull(configHome);
		assertTrue("XDG_CONFIG_HOME must end with '.servoy'", configHome.endsWith(".servoy"));
	}

	@Test
	public void buildServoyXdgEnv_configHomeMatchesExpectedPath() {
		String expected = System.getProperty("user.home") + File.separator + ".servoy";
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		assertEquals("XDG_CONFIG_HOME must equal {user.home}/.servoy", expected, env.get("XDG_CONFIG_HOME"));
	}

	@Test
	public void buildServoyXdgEnv_dataHomeMatchesExpectedPath() {
		String expected = System.getProperty("user.home") + File.separator + ".servoy";
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		assertEquals("XDG_DATA_HOME must equal {user.home}/.servoy", expected, env.get("XDG_DATA_HOME"));
	}

	@Test
	public void buildServoyXdgEnv_stateHomeMatchesExpectedPath() {
		String expected = System.getProperty("user.home") + File.separator + ".servoy";
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		assertEquals("XDG_STATE_HOME must equal {user.home}/.servoy", expected, env.get("XDG_STATE_HOME"));
	}

	// --- escape hatch ---

	@Test
	public void buildServoyXdgEnv_escapehatchTrue_returnsEmptyMap() {
		System.setProperty(ESCAPE_HATCH_PROPERTY, "true");
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		assertTrue("Escape hatch must cause buildServoyXdgEnv() to return an empty map", env.isEmpty());
	}

	@Test
	public void buildServoyXdgEnv_escapehatchFalse_returnsNonEmptyMap() {
		System.setProperty(ESCAPE_HATCH_PROPERTY, "false");
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		assertFalse("Escape hatch=false must NOT suppress XDG overrides", env.isEmpty());
	}

	@Test
	public void buildServoyXdgEnv_escapehatchNotSet_returnsNonEmptyMap() {
		// Ensure property is absent (cleared by @After if a previous test set it)
		System.clearProperty(ESCAPE_HATCH_PROPERTY);
		Map<String, String> env = RunOpencodeCommand.buildServoyXdgEnv();
		assertFalse("Absent escape-hatch property must not suppress XDG overrides", env.isEmpty());
	}
}
