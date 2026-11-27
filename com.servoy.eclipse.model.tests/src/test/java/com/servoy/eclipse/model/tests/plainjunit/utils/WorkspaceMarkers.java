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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Stands in for the Eclipse workspace while a plain unit test runs builder code that creates problem markers: every
 * {@code ResourcesPlugin.getWorkspace().getRoot().getFile(path)} is an existing file whose {@code createMarker(type)} records a
 * {@link RecordedMarker}, and the extension registry has no marker attribute contributors. Use in try-with-resources: the static mocks are
 * thread-local and must be closed.
 */
@SuppressWarnings("restriction")
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

	private final List<RecordedMarker> markers = Collections.synchronizedList(new ArrayList<>());
	private final Map<String, String> fileContents = new java.util.concurrent.ConcurrentHashMap<>();
	private final MockedStatic<ResourcesPlugin> resourcesPlugin;
	private final MockedStatic<Platform> platform;

	public WorkspaceMarkers()
	{
		// the internal Workspace class, so that WorkspaceJobs (UpdateMarkersJob, ...) can be created and run
		IWorkspace workspace = mock(Workspace.class);
		IWorkspaceRoot root = mock(IWorkspaceRoot.class);
		when(workspace.getRoot()).thenReturn(root);
		when(root.getFile(any(IPath.class))).thenAnswer(invocation -> file(invocation.getArgument(0).toString()));
		org.eclipse.core.resources.IFolder rootFolder = mock(org.eclipse.core.resources.IFolder.class);
		when(rootFolder.exists()).thenReturn(Boolean.FALSE);
		when(root.getFolder(any(IPath.class))).thenReturn(rootFolder);

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
		return file(path, false);
	}

	/**
	 * A project whose files and folders are in this workspace: {@code getFile(path)} gives a file at {@code /<name>/<path>} that exists
	 * only when it got content through {@link #withFile(String, String)}; markers created on the project itself are recorded at
	 * {@code /<name>}. It is also a scheduling rule that contains itself, so jobs can use it.
	 */
	public IProject project(String name)
	{
		IProject project = mock(IProject.class);
		String root = "/" + name;
		when(project.getName()).thenReturn(name);
		when(project.exists()).thenReturn(Boolean.TRUE);
		when(project.isOpen()).thenReturn(Boolean.TRUE);
		when(project.getFullPath()).thenReturn(new org.eclipse.core.runtime.Path(root));
		when(project.contains(project)).thenReturn(Boolean.TRUE);
		when(project.isConflicting(project)).thenReturn(Boolean.TRUE);
		when(project.getFile(any(IPath.class))).thenAnswer(invocation -> file(root + "/" + invocation.getArgument(0), true));
		when(project.getFile(anyString())).thenAnswer(invocation -> file(root + "/" + invocation.getArgument(0), true));
		when(project.getFolder(any(IPath.class))).thenAnswer(invocation -> {
			org.eclipse.core.resources.IFolder folder = mock(org.eclipse.core.resources.IFolder.class);
			when(folder.getFullPath()).thenReturn(new org.eclipse.core.runtime.Path(root + "/" + invocation.getArgument(0)));
			return folder;
		});
		try
		{
			when(project.createMarker(anyString())).thenAnswer(invocation -> marker(root, invocation.getArgument(0)));
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
		return project;
	}

	/**
	 * A folder of the given project: {@code project.findMember(name)} returns it, {@code findMember(fileName)} gives one of the children and
	 * accepting a visitor on it visits the children - the way the resources API does it.
	 */
	public IFolder folder(IProject project, String name, IFile... children)
	{
		IFolder folder = mock(IFolder.class);
		String path = "/" + project.getName() + "/" + name;
		when(folder.getName()).thenReturn(name);
		when(folder.getFullPath()).thenReturn(new org.eclipse.core.runtime.Path(path));
		when(folder.exists()).thenReturn(Boolean.TRUE);
		when(folder.getType()).thenReturn(Integer.valueOf(IResource.FOLDER));
		when(folder.findMember(anyString())).thenAnswer(invocation -> {
			for (IFile child : children)
			{
				if (child.getName().equals(invocation.getArgument(0))) return child;
			}
			return null;
		});
		try
		{
			doAnswer(invocation -> {
				IResourceVisitor visitor = invocation.getArgument(0);
				for (IFile child : children)
				{
					visitor.visit(child);
				}
				return null;
			}).when(folder).accept(any(IResourceVisitor.class), anyInt(), anyBoolean());
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
		when(project.findMember(name)).thenReturn(folder);
		when(project.getFolder(name)).thenReturn(folder);
		when(project.getFolder(new org.eclipse.core.runtime.Path(path))).thenReturn(folder);
		return folder;
	}

	/** The content the code under test last wrote to the workspace file at the given path, or null. */
	public String content(String path)
	{
		return fileContents.get(path);
	}

	private IFile file(String path, boolean existsOnlyWithContent)
	{
		IFile file = mock(IFile.class);
		if (existsOnlyWithContent) when(file.exists()).thenAnswer(invocation -> Boolean.valueOf(fileContents.containsKey(path)));
		else when(file.exists()).thenReturn(Boolean.TRUE);
		when(file.toString()).thenReturn(path);
		when(file.getName()).thenReturn(path.substring(path.lastIndexOf('/') + 1));
		when(file.getFileExtension()).thenAnswer(invocation -> {
			String n = path.substring(path.lastIndexOf('/') + 1);
			int i = n.lastIndexOf('.');
			return (i >= 0 && i < n.length() - 1) ? n.substring(i + 1) : null;
		});
		when(file.getType()).thenReturn(Integer.valueOf(IResource.FILE));
		when(file.getFullPath()).thenReturn(new org.eclipse.core.runtime.Path(path));
		IProject project = mock(IProject.class);
		when(project.getName()).thenReturn(PROJECT_NAME);
		when(file.getProject()).thenReturn(project);
		try
		{
			when(file.createMarker(anyString())).thenAnswer(invocation -> marker(path, invocation.getArgument(0)));
			when(file.getContents(anyBoolean())).thenAnswer(invocation -> contents(path));
			when(file.getContents()).thenAnswer(invocation -> contents(path));
			when(file.findMarkers(Mockito.any(), anyBoolean(), anyInt())).thenReturn(new IMarker[0]);
			doAnswer(invocation -> {
				store(path, invocation.getArgument(0));
				return null;
			}).when(file).setContents(any(java.io.InputStream.class), anyBoolean(), anyBoolean(), Mockito.<IProgressMonitor> any());
			doAnswer(invocation -> {
				store(path, invocation.getArgument(0));
				return null;
			}).when(file).create(any(java.io.InputStream.class), anyBoolean(), Mockito.<IProgressMonitor> any());
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
		return file;
	}

	/**
	 * Gives the workspace file at the given path (as Path.toString() shows it, so with forward slashes and a leading '/') this
	 * UTF-8 content.
	 */
	public WorkspaceMarkers withFile(String path, String content)
	{
		fileContents.put(path, content);
		return this;
	}

	private void store(String path, java.io.InputStream stream) throws java.io.IOException
	{
		fileContents.put(path, new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
	}

	private java.io.InputStream contents(String path) throws org.eclipse.core.runtime.CoreException
	{
		String content = fileContents.get(path);
		if (content == null)
		{
			throw new org.eclipse.core.runtime.CoreException(org.eclipse.core.runtime.Status.error("no content for " + path));
		}
		return new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
		synchronized (markers)
		{
			return List.copyOf(markers);
		}
	}

	/**
	 * Waits until the scheduled jobs are done: code under test often creates markers in a (workspace) job. Note that the static mocks
	 * are per thread, so code running in a job must not call ResourcesPlugin/Platform itself.
	 */
	public void waitForJobs()
	{
		try
		{
			Job.getJobManager().join(null, null);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	/** The messages of the markers created so far, in creation order. */
	public List<String> messages()
	{
		return markers().stream().map(RecordedMarker::message).toList();
	}

	@Override
	public void close()
	{
		platform.close();
		resourcesPlugin.close();
	}
}
