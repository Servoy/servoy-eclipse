/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * @author jcompagner
 *
 */
public class RenameComponentOrService extends AbstractRenameAction
{

	public RenameComponentOrService(SolutionExplorerView viewer, Shell shell, UserNodeType nodeType)
	{
		super(viewer, shell, nodeType);
	}

	@Override
	protected void renameFiles(IFolder pack, String currentName, String newName) throws CoreException, IOException
	{
		IFile defFile = pack.getFile(currentName + ".js");
		try (InputStream is = defFile.getContents())
		{
			String text = IOUtils.toString(is, "UTF-8");
			String moduleName = pack.getParent().getName() + newName.substring(0, 1).toUpperCase() + newName.substring(1);
			String oldName = pack.getParent().getName() + currentName.substring(0, 1).toUpperCase() + currentName.substring(1);
			text = text.replaceAll(oldName, moduleName);
			text = text.replaceAll(currentName + "/" + currentName + ".html", newName + "/" + newName + ".html");
			IFile newDefFile = pack.getFile(newName + ".js");
			newDefFile.create(new ByteArrayInputStream(text.getBytes()), IResource.NONE, new NullProgressMonitor());
		}

		IFile htmlFile = pack.getFile(currentName + ".html");
		if (htmlFile.exists())
		{
			IFile newHTML = pack.getFile(newName + ".html");
			try (InputStream is = htmlFile.getContents())
			{
				newHTML.create(is, IResource.NONE, new NullProgressMonitor());
			}
			htmlFile.delete(true, new NullProgressMonitor());
		}
		defFile.delete(true, new NullProgressMonitor());
	}

}
