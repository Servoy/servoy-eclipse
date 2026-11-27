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

package com.servoy.eclipse.model.tests.plainjunit.utils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.internal.preferences.PreferencesService;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Stands in for the Eclipse instance preferences while a plain unit test runs, so that ServoyBuilder.getSeverity(...) works without an
 * OSGi runtime (InstanceScope.getNode() otherwise fails in PreferencesService's static init). The DLTK JavaScript parser reads its
 * options the same way, so tests that parse scripts need it too.
 * <p>
 * Only the instance scope is covered: use persists whose root object is not a Solution (or a null project), so ProjectScope is not used.
 * Keys that were not {@link #set(String, String) set} answer with the default the builder passes in. Use in try-with-resources: the
 * static mock is thread-local and must be closed.
 * </p>
 */
@SuppressWarnings("restriction")
public final class SeverityPreferences implements AutoCloseable
{
	private final Map<String, String> values = new HashMap<>();
	private final MockedStatic<PreferencesService> preferencesService;

	public SeverityPreferences()
	{
		PreferencesService service = mock(PreferencesService.class);
		IEclipsePreferences root = mock(IEclipsePreferences.class);
		IEclipsePreferences node = mock(IEclipsePreferences.class);
		when(service.getRootNode()).thenReturn(root);
		when(root.node(anyString())).thenReturn(node);
		when(node.node(anyString())).thenReturn(node);
		when(node.get(anyString(), Mockito.any())).thenAnswer(invocation -> {
			String key = invocation.getArgument(0);
			return values.containsKey(key) ? values.get(key) : invocation.getArgument(1);
		});
		preferencesService = Mockito.mockStatic(PreferencesService.class);
		preferencesService.when(PreferencesService::getDefault).thenReturn(service);
	}

	/** Sets the severity preference (ERROR, WARNING, INFO, IGNORE or anything else) for a builder problem key. */
	public SeverityPreferences set(String key, String value)
	{
		values.put(key, value);
		return this;
	}

	@Override
	public void close()
	{
		preferencesService.close();
	}
}
