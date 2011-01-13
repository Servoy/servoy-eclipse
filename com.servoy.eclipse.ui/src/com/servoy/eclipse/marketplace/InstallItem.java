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

package com.servoy.eclipse.marketplace;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

/**
 * Class representing an installable item from the Servoy Marketplace 
 * @author gabi
 *
 */
public abstract class InstallItem
{
	public static final String DOWNLOAD_TYPE_SERVOY = "servoy"; //$NON-NLS-1$
	public static final String DOWNLOAD_TYPE_ZIP = "zip"; //$NON-NLS-1$
	public static final String DOWNLOAD_TYPE_JAR = "jar"; //$NON-NLS-1$
	public static final String DOWNLOAD_TYPE_PSF = "psf"; //$NON-NLS-1$

	private String name, description, url;
	private final String downloadType;
	private boolean isRestartRequired;

	public InstallItem(Node entryNode)
	{
		downloadType = entryNode.getAttributes().getNamedItem("downloadtype").getNodeValue(); //$NON-NLS-1$
		NodeList entryChildren = entryNode.getChildNodes();
		Node entryChild;
		String entryChildName;
		for (int i = 0; i < entryChildren.getLength(); i++)
		{
			entryChild = entryChildren.item(i);
			entryChildName = entryChild.getNodeName();
			if ("name".equals(entryChildName)) //$NON-NLS-1$
			{
				name = entryChild.getTextContent();
			}
			else if ("description".equals(entryChildName)) //$NON-NLS-1$
			{
				description = entryChild.getTextContent();
			}
			else if ("url".equals(entryChildName)) //$NON-NLS-1$
			{
				url = entryChild.getTextContent();
			}
			else if ("restart".equals(entryChildName)) //$NON-NLS-1$
			{
				isRestartRequired = true;
			}
		}
	}

	public String getName()
	{
		return name;
	}

	public String getDescription()
	{
		return description;
	}

	public String getURL()
	{
		return url;
	}

	public String getDownloadType()
	{
		return downloadType;
	}

	public boolean isRestartRequired()
	{
		return isRestartRequired;
	}

	private void writeStream(InputStream is, File destination, IProgressMonitor monitor)
	{
		byte[] buffer = new byte[4092];
		int readDataLen;
		BufferedInputStream bis = null;
		FileOutputStream destinationFile = null;
		try
		{
			bis = new BufferedInputStream(is);
			destinationFile = new FileOutputStream(destination);
			while ((readDataLen = bis.read(buffer)) != -1)
			{
				destinationFile.write(buffer, 0, readDataLen);
				if (monitor != null) monitor.worked(readDataLen);
			}
			destinationFile.flush();

		}
		catch (IOException ex)
		{
			ServoyLog.logError(ex);
		}
		finally
		{
			if (destination != null)
			{
				try
				{
					destinationFile.close();
				}
				catch (IOException ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}
	}

	public File downloadURL(String destinationDir, IProgressMonitor monitor, String monitorMessage) throws IOException
	{
		File destination = null;
		InputStream sourceInputStream = null;
		try
		{
			URL sourceURL = new URL(getURL());
			URLConnection sourceURLConnection = sourceURL.openConnection();

			sourceInputStream = sourceURLConnection.getInputStream();
			int sourceURLContentLen = sourceURLConnection.getContentLength();

			monitor.beginTask(monitorMessage, sourceURLContentLen != -1 ? sourceURLContentLen : 1);
			String dType = getDownloadType();
			if (DOWNLOAD_TYPE_JAR.equals(dType) || DOWNLOAD_TYPE_SERVOY.equals(dType) || DOWNLOAD_TYPE_PSF.equals(dType))
			{
				File destinationDirFile = new File(ApplicationServerSingleton.get().getServoyApplicationServerDirectory(), destinationDir);
				destinationDirFile.mkdirs();
				destination = new File(destinationDirFile, getName() + "." + dType); //$NON-NLS-1$ 
				writeStream(sourceInputStream, destination, sourceURLContentLen != -1 ? monitor : null);
			}
			else if (DOWNLOAD_TYPE_ZIP.equals(getDownloadType()))
			{
				ZipInputStream zipIS = new ZipInputStream(sourceInputStream);
				ZipEntry zipEntry;
				String zipEntryName;
				File entryDestination;
				while ((zipEntry = zipIS.getNextEntry()) != null)
				{
					zipEntryName = zipEntry.getName();
					entryDestination = new File(new File(ApplicationServerSingleton.get().getServoyApplicationServerDirectory(), destinationDir), zipEntryName);

					// if we have *.servoy file in the solutions/marketplace folder, consider that as the return
					if (zipEntryName.startsWith(SolutionInstall.destinationDir) && zipEntryName.endsWith(DOWNLOAD_TYPE_SERVOY))
					{
						destination = entryDestination;
					}
					if (zipEntry.isDirectory()) entryDestination.mkdirs();
					else writeStream(zipIS, entryDestination, sourceURLContentLen != -1 ? monitor : null);
				}
			}
			else
			{
				ServoyLog.logWarning("Maketplace unknown download type " + getDownloadType(), null);
			}
		}
		finally
		{
			if (sourceInputStream != null)
			{
				try
				{
					sourceInputStream.close();
				}
				catch (IOException ex)
				{
					ServoyLog.logError(ex);
				}
			}
			monitor.done();
		}

		return destination;
	}

	public abstract void install(IProgressMonitor monitor) throws Exception;
}
