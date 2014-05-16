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
import java.util.List;

import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.FileBasedExtensionProvider;
import com.servoy.extension.IFileBasedExtensionProvider;
import com.servoy.extension.IProgress;
import com.servoy.extension.Message;
import com.servoy.extension.MessageKeeper;
import com.servoy.extension.parser.IEXPParserPool;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SortedList;

/**
 * This is a composed extension provider based on folder-based providers. It will be able to provide
 * only one version for each extension id (the one from the last provider in the list that has that extension id (any version of it)).<br><br>
 * 
 * It is meant to combine installed & pending folders in order to simulate an "already installed" set of extensions. It also takes into account pending
 * uninstall operations, hiding those uninstalled extensions at the right level.
 * @author acostescu
 */
public class InstalledWithPendingExtensionProvider implements IFileBasedExtensionProvider
{

	public final static String PENDING_FOLDER = ".pending";

	protected final static String DOT = ".";

	protected IFileBasedExtensionProvider topMostProvider;
	protected MessageKeeper messages = new MessageKeeper();

	protected File extDir;
	protected IEXPParserPool parserSource;

	private int folderCount;

	public InstalledWithPendingExtensionProvider(File extDir, IEXPParserPool parserSource)
	{
		this.extDir = extDir;
		this.parserSource = parserSource;

		createFolderProviderList();
	}

	protected void createFolderProviderList()
	{
		File[] pendingDirs = getPendingDirsAscending(extDir);
		IFileBasedExtensionProvider current = new FileBasedExtensionProvider(extDir, true, parserSource);
		folderCount = 1 + pendingDirs.length;
		for (File pendingDir : pendingDirs)
		{
			RestartState rs = new RestartState();
			String e = rs.recreateFromPending(pendingDir, false);
			if (e != null) messages.addError(e);

			if (e == null && rs.chosenPath.uninstall)
			{
				current = new UninstallChainedFileBasedExtensionProvider(rs, current);
			}
			else
			{
				current = new ChainedFileBasedExtensionProvider(pendingDir, parserSource, current);
			}
		}
		topMostProvider = current;
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
						Debug.log("Found an invalid file name in .pending folder (it will probably be ignored and deleted)!", new RuntimeException());
					}
				}
				else
				{
					Debug.log("Found an invalid file/file name in .pending folder (it will probably be ignored and deleted)!", new RuntimeException());
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
						Debug.log("Found an invalid file name in .pending folder (it will probably be ignored and deleted)!", new RuntimeException());
					}
				}
				else
				{
					Debug.log("Found an invalid file/file name in .pending folder (it will probably be ignored and deleted)!", new RuntimeException());
				}
			}
		}
		return new File(pendingParent, DOT + String.valueOf(highest));
	}

	public int getFolderCount()
	{
		return folderCount;
	}

	public Message[] getMessages()
	{
		List<Message> allMsgs = new ArrayList<Message>();
		allMsgs.addAll(Arrays.asList(topMostProvider.getMessages()));
		allMsgs.addAll(Arrays.asList(messages.getMessages()));
		return allMsgs.toArray(new Message[allMsgs.size()]);
	}

	public void clearMessages()
	{
		topMostProvider.clearMessages();
		messages.clearMessages();
	}

	public DependencyMetadata[] getDependencyMetadata(ExtensionDependencyDeclaration extensionDependency)
	{
		return topMostProvider.getDependencyMetadata(extensionDependency);
	}

	public File getEXPFile(String extensionId, String version, IProgress progressMonitor)
	{
		return topMostProvider.getEXPFile(extensionId, version, null);
	}

	public void dispose()
	{
		topMostProvider.dispose();
	}

	public void flushCache()
	{
		// recreate whole list (maybe pending dirs changed)
		createFolderProviderList();
	}

	public DependencyMetadata[] getAllAvailableExtensions()
	{
		return topMostProvider.getAllAvailableExtensions();
	}

}
