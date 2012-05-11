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

package com.servoy.eclipse.ui.wizards.extension;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * This class holds the state that needs to be shared across multiple install extension wizard pages.
 * @author acostescu
 */
public class InstallExtensionState extends RestartState
{

	public String extensionID;
	public String version;

	public String expFile;
	public List<Image> allocatedImages = new ArrayList<Image>();
	public Display display;

	public boolean canFinish = false;
	public boolean mustRestart = false;

	public void dispose()
	{
		if (extensionProvider != null) extensionProvider.dispose();
		if (installedExtensionsProvider != null) installedExtensionsProvider.dispose();
		for (Image img : allocatedImages)
		{
			img.dispose();
		}
	}

}
