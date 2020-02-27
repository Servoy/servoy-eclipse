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

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * @author jcomp
 *
 */
public class DistFolderCreatorJob extends Job
{
	private final File nodeFolder;

	public DistFolderCreatorJob(File nodeFolder)
	{
		super("Copy distribution folder");
		this.nodeFolder = nodeFolder;
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
		}
		catch (IOException e1)
		{
			ServoyLog.logError(e1);
		}

		Enumeration<URL> entries = Activator.getInstance().getBundle().findEntries("/node/dist", "*", true);
		while (entries.hasMoreElements())
		{
			URL entry = entries.nextElement();
			String filename = entry.getFile();
			if (filename.startsWith("/node/")) filename = filename.substring("/node".length());
			else filename = filename.substring("node".length());
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
				ServoyLog.logError(e);
			}
		}
		System.err.println("copied " + (System.currentTimeMillis() - time));
		NodeFolderCreatorJob.createFileWatcher(nodeFolder, "/dist");
		return Status.OK_STATUS;
	}

}
