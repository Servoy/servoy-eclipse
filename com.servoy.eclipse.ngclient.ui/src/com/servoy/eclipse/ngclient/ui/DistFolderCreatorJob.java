/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.ngclient.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * @author jcomp
 *
 */
public class DistFolderCreatorJob extends Job
{
	private final File nodeFolder;
	private final boolean includeSource;

	public DistFolderCreatorJob(File nodeFolder, boolean includeSource)
	{
		super("Copy distribution folder");
		this.nodeFolder = nodeFolder;
		this.includeSource = includeSource;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor)
	{
		long time = System.currentTimeMillis();
		nodeFolder.mkdirs();
		try
		{
			File dist = new File(nodeFolder, "dist");
			if (dist.exists()) FileUtils.deleteDirectory(dist);
			if (includeSource)
			{
				File src = new File(nodeFolder, "src");
				if (src.exists()) FileUtils.deleteDirectory(src);
			}
		}
		catch (IOException e1)
		{
			Activator.getInstance().getLog().error("Error creating the node filer 'dist' dir " + nodeFolder, e1);
		}

		String sourceFolder = includeSource ? "/node/" : "/node/dist/";
		Enumeration<URL> entries = Activator.getInstance().getBundle().findEntries(sourceFolder, "*", true);
		while (entries.hasMoreElements())
		{
			URL entry = entries.nextElement();
			String filename = entry.getFile();
			if (filename.startsWith("/node/")) filename = filename.substring("/node".length());
			else filename = filename.substring("node".length());
			if (filename.startsWith("/node_modules/") || filename.startsWith("/e2e/")) continue;
			try
			{
				if (filename.endsWith("/"))
				{
					File folder = new File(nodeFolder, filename);
					NodeFolderCreatorJob.createFolder(folder);
				}
				else
				{
					try (InputStream is = entry.openStream())
					{
						NodeFolderCreatorJob.copyOrCreateFile(filename, nodeFolder, is);
					}
				}
			}
			catch (Exception e)
			{
				Activator.getInstance().getLog().error("Error copy " + filename + " to the node folder " + nodeFolder, e);
			}
		}
		System.err.println("copied " + (System.currentTimeMillis() - time));
		NodeFolderCreatorJob.createFileWatcher(nodeFolder, "/dist");
		Activator.getInstance().stateLocationDone();
		return Status.OK_STATUS;
	}

}
