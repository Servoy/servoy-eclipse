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

package com.servoy.eclipse.marketplace;

import java.io.File;
import java.io.FileInputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.ResourceProjectChoiceDialog;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.util.Utils;

/**
 * Class representing installables styles from the Servoy Marketplace
 * @author gboros
 *
 */
public class StylesInstall implements InstallItem
{
	private final File[] stylesFile;

	public StylesInstall(File[] stylesFile)
	{
		this.stylesFile = stylesFile;
	}

	public void install(IProgressMonitor monitor) throws Exception
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				// show resource project choice dialog
				final ResourceProjectChoiceDialog dialog = new ResourceProjectChoiceDialog(UIUtils.getActiveShell(), "Select resource project for the style",
					null);

				if (dialog.open() == Window.OK)
				{
					IProject newResourcesProject;
					if (dialog.getResourceProjectData().getNewResourceProjectName() != null)
					{
						newResourcesProject = ServoyModel.getWorkspace().getRoot().getProject(dialog.getResourceProjectData().getNewResourceProjectName());
					}
					else
					{
						newResourcesProject = dialog.getResourceProjectData().getExistingResourceProject().getProject();
					}

					WorkspaceFileAccess wfa = new WorkspaceFileAccess(newResourcesProject.getWorkspace());

					for (File styleFile : stylesFile)
					{
						FileInputStream fis = null;
						try
						{
							fis = new FileInputStream(styleFile);
							wfa.setContents(newResourcesProject.getName() + "/styles/" + styleFile.getName(), fis); //$NON-NLS-1$
						}
						catch (Exception ex)
						{
							MessageDialog.openError(UIUtils.getActiveShell(), "Servoy Marketplace",
								"Error installing " + styleFile.getName() + ".\n\n" + ex.getMessage());
						}
						finally
						{
							Utils.closeInputStream(fis);
						}
					}
				}
			}
		});
	}


	public String getName()
	{
		StringBuilder name = new StringBuilder();

		for (File styleFile : stylesFile)
			name.append(styleFile.getName()).append(' ');

		return name.toString();
	}
}
