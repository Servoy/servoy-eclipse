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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Stands in for the Eclipse workspace while a plain unit test runs builder code that creates problem markers: every
 * {@code ResourcesPlugin.getWorkspace().getRoot().getFile(path)} is an existing file whose {@code createMarker(type)} records a
 * {@link RecordedMarker}, and the extension registry has no marker attribute contributors. Use in try-with-resources: the static mocks are
 * thread-local and must be closed.
 */
public final class WorkspaceMarkers implements AutoCloseable
{
	/** A marker the code under test created: its file, type and the attributes that were set on it. */
	public record RecordedMarker(String path, String type, Map<String, Object> attributes)
	{
		public String message()
		{
			return (String)attributes.get(IMarker.MESSAGE);
		}

		public Integer severity()
		{
			return (Integer)attributes.get(IMarker.SEVERITY);
		}

		public Integer priority()
		{
			return (Integer)attributes.get(IMarker.PRIORITY);
		}
	}

	/** The name of the project every file is in. */
	public static final String PROJECT_NAME = "test_solution";

	private final List<RecordedMarker> markers = new ArrayList<>();
	private final MockedStatic<ResourcesPlugin> resourcesPlugin;
	private final MockedStatic<Platform> platform;

	public WorkspaceMarkers()
	{
		IWorkspace workspace = mock(IWorkspace.class);
		IWorkspaceRoot root = mock(IWorkspaceRoot.class);
		when(workspace.getRoot()).thenReturn(root);
		when(root.getFile(any(IPath.class))).thenAnswer(invocation -> file(invocation.getArgument(0).toString()));

		IExtensionRegistry registry = mock(IExtensionRegistry.class);
		IExtensionPoint extensionPoint = mock(IExtensionPoint.class);
		when(registry.getExtensionPoint(anyString())).thenReturn(extensionPoint);
		when(extensionPoint.getExtensions()).thenReturn(new IExtension[0]);

		resourcesPlugin = Mockito.mockStatic(ResourcesPlugin.class);
		resourcesPlugin.when(ResourcesPlugin::getWorkspace).thenReturn(workspace);
		platform = Mockito.mockStatic(Platform.class);
		platform.when(Platform::getExtensionRegistry).thenReturn(registry);
	}

	/** An existing workspace file at the given path that records the markers created on it. */
	public IFile file(String path)
	{
		IFile file = mock(IFile.class);
		when(file.exists()).thenReturn(Boolean.TRUE);
		when(file.toString()).thenReturn(path);
		IProject project = mock(IProject.class);
		when(project.getName()).thenReturn(PROJECT_NAME);
		when(file.getProject()).thenReturn(project);
		try
		{
			when(file.createMarker(anyString())).thenAnswer(invocation -> marker(path, invocation.getArgument(0)));
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
		return file;
	}

	private IMarker marker(String path, String type) throws Exception
	{
		Map<String, Object> attributes = new HashMap<>();
		markers.add(new RecordedMarker(path, type, attributes));
		IMarker marker = mock(IMarker.class);
		doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1))).when(marker).setAttribute(anyString(),
			anyInt());
		doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1))).when(marker).setAttribute(anyString(),
			anyBoolean());
		doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1))).when(marker).setAttribute(anyString(),
			Mockito.<Object> any());
		when(marker.getType()).thenReturn(type);
		return marker;
	}

	/** The markers created so far, in creation order. */
	public List<RecordedMarker> markers()
	{
		return Collections.unmodifiableList(markers);
	}

	/** The messages of the markers created so far, in creation order. */
	public List<String> messages()
	{
		return markers.stream().map(RecordedMarker::message).toList();
	}

	@Override
	public void close()
	{
		platform.close();
		resourcesPlugin.close();
	}
}
