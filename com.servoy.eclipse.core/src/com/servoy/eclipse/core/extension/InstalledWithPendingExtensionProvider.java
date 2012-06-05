/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.core.extension;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.FileBasedExtensionProvider;
import com.servoy.extension.IExtensionProvider;
import com.servoy.extension.IFileBasedExtensionProvider;
import com.servoy.extension.IProgress;
import com.servoy.extension.Message;
import com.servoy.extension.MessageKeeper;
import com.servoy.extension.VersionStringUtils;
import com.servoy.extension.parser.IEXPParserPool;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SortedList;

/**
 * This is a composed extension provider based on folder-based providers. It will be able to provide
 * only one version for each extension id (the one from the last provider in the list that has that extension id (any version of it)).<br><br>
 * 
 * It is meant to combine installed & pending folders in order to simulate an "already installed" set of extensions.
 * @author acostescu
 */
public class InstalledWithPendingExtensionProvider implements IFileBasedExtensionProvider
{

	public final static String PENDING_FOLDER = ".pending"; //$NON-NLS-1$

	protected final static String DOT = "."; //$NON-NLS-1$

	protected FileBasedExtensionProvider[] folderProviders;
	protected MessageKeeper messages = new MessageKeeper();

	protected File extDir;
	protected IEXPParserPool parserSource;

	public InstalledWithPendingExtensionProvider(File extDir, IEXPParserPool parserSource)
	{
		this.extDir = extDir;
		this.parserSource = parserSource;

		createFolderProviderList();
	}

	protected void createFolderProviderList()
	{
		File[] pendingDirs = getPendingDirsAscending(extDir);
		folderProviders = new FileBasedExtensionProvider[pendingDirs.length + 1];
		folderProviders[0] = new FileBasedExtensionProvider(extDir, true, parserSource);
		for (int i = 0; i < pendingDirs.length; i++)
		{
			folderProviders[i + 1] = new FileBasedExtensionProvider(pendingDirs[i], true, parserSource);
		}
	}

	/**
	 * Returns all relative .pending/.# where # is an integer.
	 * @param extDir the base .extensions directory.
	 * @return all relative .pending/.# where # is an integer. If none is found returns an empty array.
	 */
	public static File[] getPendingDirsAscending(File extDir)
	{
		File pendingParent = new File(extDir, PENDING_FOLDER);
		SortedList<File> result = new SortedList<File>(new Comparator<File>()
		{
			public int compare(File f1, File f2)
			{
				// this method assumes that all elements in the sorted list are non-null and named like .1, .2, .3, ..., .#
				// otherwise exceptions will happen 
				String int1 = f1.getName().substring(1); // remove '.'
				String int2 = f2.getName().substring(1); // remove '.'
				return Integer.parseInt(int1) - Integer.parseInt(int2);
			}
		});

		if (pendingParent.exists())
		{
			File[] allChildren = pendingParent.listFiles();
			for (File f : allChildren)
			{
				if (f.isDirectory() && f.getName().startsWith(DOT))
				{
					try
					{
						Integer.parseInt(f.getName().substring(1));
						result.add(f);
					}
					catch (NumberFormatException e)
					{
						Debug.log("Found an invalid file name in .pending folder (it will probably be ignored and deleted)!", new RuntimeException()); //$NON-NLS-1$
					}
				}
				else
				{
					Debug.log("Found an invalid file/file name in .pending folder (it will probably be ignored and deleted)!", new RuntimeException()); //$NON-NLS-1$
				}
			}
		}
		return result.toArray(new File[result.size()]);
	}

	/**
	 * Returns a new relative pending dir (that will be installed after all others already existing).
	 * @param extDir the base .extensions directory.
	 * @return a new relative pending dir (that will be installed after all others already existing).
	 */
	public static File getNextFreePendingDir(File extDir)
	{
		File pendingParent = new File(extDir, PENDING_FOLDER);
		int highest = 1;

		if (pendingParent.exists())
		{
			File[] allChildren = pendingParent.listFiles();
			for (File f : allChildren)
			{
				if (f.isDirectory() && f.getName().startsWith(DOT))
				{
					try
					{
						int nr = Integer.parseInt(f.getName().substring(1));
						if (nr >= highest) highest = nr + 1;
					}
					catch (NumberFormatException e)
					{
						Debug.log("Found an invalid file name in .pending folder (it will probably be ignored and deleted)!", new RuntimeException()); //$NON-NLS-1$
					}
				}
				else
				{
					Debug.log("Found an invalid file/file name in .pending folder (it will probably be ignored and deleted)!", new RuntimeException()); //$NON-NLS-1$
				}
			}
		}
		return new File(pendingParent, DOT + String.valueOf(highest));
	}

	public int getFolderCount()
	{
		return folderProviders.length;
	}

	public Message[] getMessages()
	{
		List<Message> allMsgs = new ArrayList<Message>();
		for (IExtensionProvider exp : folderProviders)
		{
			allMsgs.addAll(Arrays.asList(exp.getMessages()));
		}
		allMsgs.addAll(Arrays.asList(messages.getMessages()));
		return allMsgs.toArray(new Message[allMsgs.size()]);
	}

	public void clearMessages()
	{
		for (IExtensionProvider exp : folderProviders)
		{
			exp.clearMessages();
		}
		messages.clearMessages();
	}

	public DependencyMetadata[] getDependencyMetadata(ExtensionDependencyDeclaration extensionDependency)
	{
		ExtensionDependencyDeclaration toSearchFor = new ExtensionDependencyDeclaration(extensionDependency.id, VersionStringUtils.UNBOUNDED,
			VersionStringUtils.UNBOUNDED);
		DependencyMetadata[] dmds = null;
		int folderProviderIdx = folderProviders.length - 1;

		while (folderProviderIdx >= 0 && dmds == null)
		{
			DependencyMetadata[] tmps = folderProviders[folderProviderIdx].getDependencyMetadata(toSearchFor);
			if (tmps != null && tmps.length > 0)
			{
				if (VersionStringUtils.belongsToInterval(tmps[0].version, extensionDependency.minVersion, extensionDependency.maxVersion))
				{
					dmds = tmps;
					if (dmds.length > 1)
					{
						messages.addWarning("More then one extension with id='" + toSearchFor.id + "' marked as installed. This is not supported."); //$NON-NLS-1$//$NON-NLS-2$
					}
				}
				else
				{
					folderProviderIdx = -1; // found it, but another version; this provider can only supply 1 version for each extension id (simulates installed)
				}
			}
			folderProviderIdx--;
		}
		return dmds;
	}

	public File getEXPFile(String extensionId, String version, IProgress progressMonitor)
	{
		File f = null;
		int folderProviderIdx = folderProviders.length - 1;

		while (folderProviderIdx >= 0 && f == null)
		{
			f = folderProviders[folderProviderIdx].getEXPFile(extensionId, version, progressMonitor);
			folderProviderIdx--;
		}
		return f;
	}

	public void dispose()
	{
		for (IExtensionProvider exp : folderProviders)
		{
			exp.dispose();
		}
	}

	public void flushCache()
	{
		// recreate whole list (maybe pending dirs changed)
		createFolderProviderList();
	}

	public DependencyMetadata[] getAllAvailableExtensions()
	{
		List<DependencyMetadata> dmds = new ArrayList<DependencyMetadata>();
		Set<String> foundIds = new HashSet<String>();
		int folderProviderIdx = folderProviders.length - 1;

		while (folderProviderIdx >= 0)
		{
			DependencyMetadata[] tmp = folderProviders[folderProviderIdx].getAllAvailableExtensions();
			if (tmp != null)
			{
				for (DependencyMetadata dmd : tmp)
				{
					if (!foundIds.contains(dmd.id))
					{
						foundIds.add(dmd.id);
						dmds.add(dmd);
					}
				}
			}
			folderProviderIdx--;
		}

		return dmds.toArray(new DependencyMetadata[dmds.size()]);
	}

}
