/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.model.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.servoy.j2db.util.Utils;

/**
 * IFileAccess implementation for accessing files in an eclipse workspace.
 * 
 * @author rgansevles
 * 
 */
public class WorkspaceFileAccess implements IFileAccess
{
	protected final IWorkspaceRoot workspaceRoot;

	public WorkspaceFileAccess(IWorkspace workspace)
	{
		this.workspaceRoot = workspace.getRoot();
	}

	public void setUTF8Contents(String relativeFilePath, String contents) throws IOException
	{
		InputStream encodedStream = null;
		try
		{
			encodedStream = Utils.getUTF8EncodedStream(contents);
			setContents(relativeFilePath, encodedStream);
		}
		finally
		{
			Utils.closeInputStream(encodedStream);
		}
	}

	public void setContents(String relativeFilePath, byte[] contents) throws IOException
	{
		InputStream byteArrayStream = null;
		try
		{
			byteArrayStream = new ByteArrayInputStream(contents);
			setContents(relativeFilePath, byteArrayStream);
		}
		finally
		{
			Utils.closeInputStream(byteArrayStream);
		}
	}

	public String getUTF8Contents(String relativeFilePath) throws IOException
	{
		IFile file = workspaceRoot.getFile(new Path(relativeFilePath));

		InputStreamReader contents = null;
		try
		{
			contents = new InputStreamReader(file.getContents(true), "UTF-8");
			StringWriter stringWriter = new StringWriter();
			Utils.readerWriterCopy(contents, stringWriter);
			return stringWriter.toString();
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
			throw new IOException(e.getMessage());
		}
		finally
		{
			Utils.closeReader(contents);
		}
	}

	public byte[] getContents(String relativeFilePath) throws IOException
	{
		IFile file = workspaceRoot.getFile(new Path(relativeFilePath));

		BufferedInputStream contents = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int len;
		try
		{
			contents = new BufferedInputStream(file.getContents(true));
			while ((len = contents.read(buffer)) != -1)
				bos.write(buffer, 0, len);
			return bos.toByteArray();
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
			throw new IOException(e.getMessage());
		}
		finally
		{
			Utils.closeOutputStream(bos);
			Utils.closeInputStream(contents);
		}
	}

	/**
	 * Create the folder and its parents
	 * 
	 * @param container
	 * @throws CoreException
	 */
	public static void mkdirs(IContainer container) throws CoreException
	{
		if (container != null && !container.exists())
		{
			mkdirs(container.getParent());
			if (container instanceof IFolder)
			{
				// delete resources with the same name, maybe diff case
				// as we cannot have in windows such resources
				WorkspaceFileAccess.deleteResourceWithSameName(container);
				((IFolder)container).create(true, true, null);
			}
		}
	}

	public void setContents(String relativeFilePath, InputStream inputStream) throws IOException
	{
		try
		{
			IFile file = workspaceRoot.getFile(new Path(relativeFilePath));
			mkdirs(file.getParent());

			if (file.exists())
			{
				file.setContents(inputStream, true, true, null);
			}
			else
			{
				// delete resources with the same name, maybe diff case
				// as we cannot have in windows such resources
				WorkspaceFileAccess.deleteResourceWithSameName(file);
				file.create(inputStream, true, null);
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
			throw new IOException(e.getMessage());
		}
	}

	public void createFolder(String relativeFilePath) throws IOException
	{
		try
		{
			mkdirs(workspaceRoot.getFolder(new Path(relativeFilePath)));
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Create an output stream for the file. Store the data in a ByteArrayInputStream in memory and save it in the file when the input stream is closed.
	 */
	public OutputStream getOutputStream(final String relativeFilePath)
	{
		return new ByteArrayOutputStream()
		{
			@Override
			public void close() throws IOException
			{
				super.close();
				setContents(relativeFilePath, new ByteArrayInputStream(toByteArray()));
			}
		};
	}

	public void closeOutputStream(OutputStream os) throws IOException
	{
		if (os != null) os.close();
	}

	public boolean exists(String relativeFilePath)
	{
		Path path = new Path(relativeFilePath);
		return workspaceRoot.getFile(path).exists() || workspaceRoot.getFolder(path).exists();
	}

	public void delete(String relativeFilePath) throws IOException
	{
		try
		{
			Path path = new Path(relativeFilePath);
			IFile file = workspaceRoot.getFile(path);
			if (file.exists())
			{
				file.delete(true, null);
			}
			else
			{
				IFolder folder = workspaceRoot.getFolder(path);
				if (folder.exists())
				{
					folder.delete(true, null);
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
			throw new IOException(e.getMessage());
		}
	}

	public void deleteAll(String relativeFilePath) throws IOException
	{
		delete(relativeFilePath);
	}

	protected boolean moveFile(Path relativePathFrom, Path relativePathTo) throws CoreException
	{
		IFile fileFrom = workspaceRoot.getFile(relativePathFrom);
		if (fileFrom.exists())
		{
			IFile fileTo = workspaceRoot.getFile(relativePathTo);
			mkdirs(fileTo.getParent());
			fileFrom.move(fileTo.getFullPath(), true, null);
			return true;
		}
		return false;
	}

	protected boolean moveFolder(Path relativePathFrom, Path relativePathTo) throws CoreException
	{
		IFolder folderFrom = workspaceRoot.getFolder(relativePathFrom);
		if (folderFrom.exists())
		{
			IFolder folderTo = workspaceRoot.getFolder(relativePathTo);
			mkdirs(folderTo.getParent());
			folderFrom.move(folderTo.getFullPath(), true, null);
			return true;
		}
		return false;
	}

	public boolean move(String relativeFilePathFrom, String relativeFilePathTo) throws IOException
	{
		try
		{
			Path relativePathFrom = new Path(relativeFilePathFrom);
			Path relativePathTo = new Path(relativeFilePathTo);
			return moveFile(relativePathFrom, relativePathTo) || moveFolder(relativePathFrom, relativePathTo);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
			throw new IOException(e.getMessage());
		}
	}

	public long getFileLength(String relativeFilePath) throws IOException
	{
		return workspaceRoot.getFile(new Path(relativeFilePath)).getLocation().toFile().length();
	}

	public String getWorkspaceOSPath(String projectName)
	{
		if (projectName != null)
		{
			IProject project = workspaceRoot.getProject(projectName);
			if (project != null) return project.getLocation().removeLastSegments(1).toOSString();
		}
		return workspaceRoot.getLocation().toOSString();
	}

	public String[] list()
	{
		IResource[] members;
		try
		{
			members = workspaceRoot.members();
		}
		catch (CoreException e)
		{
			ServoyLog.logError("Could not list members for " + workspaceRoot.getLocation().toOSString(), e);
			return new String[0];
		}
		String[] names = new String[members.length];
		for (int i = 0; i < members.length; i++)
		{
			names[i] = members[i].getName();
		}
		return names;
	}

	public File getProjectFile(String projectName)
	{
		if (projectName != null)
		{
			IProject project = workspaceRoot.getProject(projectName);
			if (project != null) return project.getLocation().toFile();
		}
		return null;
	}

	@Override
	public String toString()
	{
		return "WorkspaceFileAccess(" + workspaceRoot + ')';
	}

	private static void deleteResourceWithSameName(IResource resource) throws CoreException
	{
		IContainer parent = resource.getParent();
		if (parent != null && parent.exists())
		{
			IResource[] childResources = parent.members();
			for (IResource childResource : childResources)
			{
				if (childResource.getName().equalsIgnoreCase(resource.getName())) childResource.delete(true, null);
			}
		}
	}
}
