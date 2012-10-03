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
import java.util.List;

import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.IFileBasedExtensionProvider;
import com.servoy.extension.IProgress;
import com.servoy.extension.Message;
import com.servoy.extension.dependency.InstallStep;

/**
 * A file based extension provider that is able to hide uninstalled extensionid/version from what the 'child' has to offer.
 * @author acostescu
 */
public class UninstallChainedFileBasedExtensionProvider implements IFileBasedExtensionProvider
{

	private final IFileBasedExtensionProvider child;
	private final InstallStep[] uninstalled;

	/**
	 * It will hide the extensions marked as uninstalled from rs that would normally be provided by child.
	 * @param rs the pending restart state deserialized from the uninstalled pending folder.
	 * @param child the child extension provider in the linked list. Cannot be null. (probably for another pending folder, or for the main installed provider)
	 */
	public UninstallChainedFileBasedExtensionProvider(RestartState rs, IFileBasedExtensionProvider child)
	{
		if (!rs.chosenPath.uninstall) throw new IllegalArgumentException("Pending chained provider expected to be an uninstall"); //$NON-NLS-1$
		this.child = child;
		this.uninstalled = rs.chosenPath.installSequence;
	}

	public DependencyMetadata[] getDependencyMetadata(ExtensionDependencyDeclaration extensionDependency)
	{
		return isUninstalled(extensionDependency.id) ? null : child.getDependencyMetadata(extensionDependency);
	}

	private boolean isUninstalled(String extensionId)
	{
		boolean isUninstalled = false;
		for (InstallStep is : uninstalled)
		{
			if (is.extension.id.equals(extensionId)) // we don't check for versions, cause if pending dirs are not corrupted, there is only one possible version after any pending install - that could be now uninstalled
			{
				isUninstalled = true;
				break;
			}
		}
		return isUninstalled;
	}

	public File getEXPFile(String extensionId, String version, IProgress progressMonitor)
	{
		return isUninstalled(extensionId) ? null : child.getEXPFile(extensionId, version, progressMonitor);
	}

	public void dispose()
	{
		child.dispose();
	}

	public Message[] getMessages()
	{
		return child.getMessages();
	}

	public void clearMessages()
	{
		child.clearMessages();
	}

	public void flushCache()
	{
		child.flushCache();
	}

	public DependencyMetadata[] getAllAvailableExtensions()
	{
		DependencyMetadata[] allFromChild = child.getAllAvailableExtensions();
		if (allFromChild != null)
		{
			List<DependencyMetadata> result = new ArrayList<DependencyMetadata>(allFromChild.length);
			for (DependencyMetadata dmd : allFromChild)
			{
				if (!isUninstalled(dmd.id)) result.add(dmd);
			}
			return result.toArray(new DependencyMetadata[result.size()]);
		}
		return null;
	}

}
