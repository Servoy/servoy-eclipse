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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.FileBasedExtensionProvider;
import com.servoy.extension.IFileBasedExtensionProvider;
import com.servoy.extension.IProgress;
import com.servoy.extension.Message;
import com.servoy.extension.MessageKeeper;
import com.servoy.extension.VersionStringUtils;
import com.servoy.extension.parser.IEXPParserPool;

/**
 * A file based extension provider that is able to add it's own .exp files from a folder on top of what the "child" IFileBasedExtensionProvider offers.
 * @author acostescu
 */
public class ChainedFileBasedExtensionProvider implements IFileBasedExtensionProvider
{

	private final FileBasedExtensionProvider provider;
	private final IFileBasedExtensionProvider child;
	private final MessageKeeper messages = new MessageKeeper();

	/**
	 * It will simulate the given pendingFolder on top of the child extension provider.
	 * @param pendingFolder the pending folder that this chained provider will service. Each such folder can have only one version of for a particular extension id.
	 * @param parserSource exp parser pool to use.
	 * @param child the child extension provider in the linked list. Cannot be null. (probably for another pending folder, or for the main installed provider)
	 */
	public ChainedFileBasedExtensionProvider(File pendingFolder, IEXPParserPool parserSource, IFileBasedExtensionProvider child)
	{
		provider = new FileBasedExtensionProvider(pendingFolder, true, parserSource);
		this.child = child;
		if (child == null) throw new NullPointerException("Child linked provider cannot be null");
	}

	public DependencyMetadata[] getDependencyMetadata(ExtensionDependencyDeclaration extensionDependency)
	{
		ExtensionDependencyDeclaration toSearchFor = new ExtensionDependencyDeclaration(extensionDependency.id, VersionStringUtils.UNBOUNDED,
			VersionStringUtils.UNBOUNDED);
		DependencyMetadata[] dmds = null;

		DependencyMetadata[] tmps = provider.getDependencyMetadata(toSearchFor);
		if (tmps != null && tmps.length > 0)
		{
			if (tmps.length == 1 && VersionStringUtils.belongsToInterval(tmps[0].version, extensionDependency.minVersion, extensionDependency.maxVersion))
			{
				dmds = tmps;
			}
			else
			{
				if (tmps.length > 1)
				{
					messages.addWarning("More then one extension with id='" + toSearchFor.id + "' marked as installed. This is not supported.");
				} // else found it, but another version; this provider can only supply 1 version for each extension id (simulates installed)
			}
		}
		else
		{
			dmds = child.getDependencyMetadata(extensionDependency);
		}
		return dmds;
	}

	public File getEXPFile(String extensionId, String version, IProgress progressMonitor)
	{
		DependencyMetadata[] x = provider.getDependencyMetadata(new ExtensionDependencyDeclaration(extensionId, VersionStringUtils.UNBOUNDED,
			VersionStringUtils.UNBOUNDED));
		if (x == null || x.length == 0)
		{
			return child.getEXPFile(extensionId, version, progressMonitor);
		}
		else if (x.length == 1 && VersionStringUtils.sameVersion(x[0].version, version))
		{
			return provider.getEXPFile(extensionId, version, progressMonitor);
		}
		else if (x.length > 1)
		{
			messages.addWarning("More then one extension with id='" + extensionId + "' marked as installed. This is not supported...");
		}

		return null;
	}

	public void dispose()
	{
		provider.dispose();
		child.dispose();
	}

	public Message[] getMessages()
	{
		List<Message> allMsgs = new ArrayList<Message>();
		allMsgs.addAll(Arrays.asList(provider.getMessages()));
		allMsgs.addAll(Arrays.asList(child.getMessages()));
		allMsgs.addAll(Arrays.asList(messages.getMessages()));
		return allMsgs.toArray(new Message[allMsgs.size()]);
	}

	public void clearMessages()
	{
		provider.clearMessages();
		child.clearMessages();
		messages.clearMessages();
	}

	public void flushCache()
	{
		// this is not really useful for chained stuff - the whole chain/list should be reconstructed
		provider.flushCache();
		child.flushCache();
	}

	public DependencyMetadata[] getAllAvailableExtensions()
	{
		DependencyMetadata[] result;
		DependencyMetadata[] childExts;
		DependencyMetadata[] exts = provider.getAllAvailableExtensions();
		childExts = child.getAllAvailableExtensions();

		if (childExts != null)
		{
			List<DependencyMetadata> allExts = new ArrayList<DependencyMetadata>();
			Set<String> foundIds = new HashSet<String>();
			for (DependencyMetadata dmd : exts)
			{
				if (!foundIds.contains(dmd.id))
				{
					foundIds.add(dmd.id);
					allExts.add(dmd);
				}
			}
			for (DependencyMetadata dmd : childExts)
			{
				if (!foundIds.contains(dmd.id))
				{
					foundIds.add(dmd.id);
					allExts.add(dmd);
				}
			}
			result = allExts.toArray(new DependencyMetadata[allExts.size()]);
		}
		else result = exts;
		return result;
	}

}
