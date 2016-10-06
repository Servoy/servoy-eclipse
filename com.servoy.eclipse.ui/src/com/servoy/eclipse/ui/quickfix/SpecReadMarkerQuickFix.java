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

package com.servoy.eclipse.ui.quickfix;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * @author emera
 */
public class SpecReadMarkerQuickFix implements IMarkerResolution
{
	private final String location;

	public SpecReadMarkerQuickFix(String location)
	{
		this.location = location;
	}

	@Override
	public String getLabel()
	{
		return "Search the manifest file in subfolders and remove extra folder";
	}

	@Override
	public void run(IMarker marker)
	{
		final File loc = new File(location);
		try
		{
			Collection<File> files = FileUtils.listFiles(loc, null, true);
			for (File file : files)
			{
				if (file.getName().equals("MANIFEST.MF"))
				{
					File toMove = file.getParentFile().getParentFile();
					for (File child : toMove.listFiles())
					{
						if (child.isDirectory())
						{
							final File dest = new File(loc, child.getName() + "/");
							final boolean[] move = new boolean[] { true };
							if (dest.exists())
							{
								move[0] = false;
								Display.getDefault().asyncExec(new Runnable()
								{
									public void run()
									{
										move[0] = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Folder already exists",
											"The folder " + dest.getName() + " already exists at the location" + loc.getAbsolutePath() + ". Overwrite?");
									}
								});
							}
							if (move[0]) FileUtils.moveDirectoryToDirectory(child, loc, false);

						}
						else
						{
							final File dest = new File(loc, child.getName());
							final boolean[] move = new boolean[] { true };
							if (dest.exists())
							{
								move[0] = false;
								Display.getDefault().asyncExec(new Runnable()
								{
									public void run()
									{
										move[0] = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "File already exists",
											"The file " + dest.getName() + " already exists at the location " + loc.getAbsolutePath() + ". Overwrite?");
									}
								});
							}
							if (move[0]) FileUtils.moveFileToDirectory(child, loc, false);
						}
					}
					return;
				}
			}
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Cannot fix package", "Manifest file not found");
				}
			});

		}
		catch (final Exception e)
		{
			ServoyLog.logError(e);
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Cannot fix package", e.getMessage());
				}
			});
		}
	}
}
