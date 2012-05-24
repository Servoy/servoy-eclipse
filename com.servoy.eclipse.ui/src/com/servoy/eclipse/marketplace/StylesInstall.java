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
import java.nio.charset.Charset;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.ResourceProjectChoiceDialog;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.ResourcesProjectSetupJob;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.StringResourceDeserializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.util.UUID;
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
				final ResourceProjectChoiceDialog dialog = new ResourceProjectChoiceDialog(UIUtils.getActiveShell(), "Import style" + //$NON-NLS-1$
					(stylesFile.length > 1 ? "s" : "") + ": select resources project", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject(), true);

				if (dialog.open() == Window.OK)
				{
					final IProject resourcesProject;
					if (dialog.getResourceProjectData().getNewResourceProjectName() != null)
					{
						resourcesProject = ServoyModel.getWorkspace().getRoot().getProject(dialog.getResourceProjectData().getNewResourceProjectName());
					}
					else
					{
						resourcesProject = dialog.getResourceProjectData().getExistingResourceProject().getProject();
					}

					IDeveloperRepository repository = ServoyModel.getDeveloperRepository();
					if (repository instanceof EclipseRepository)
					{
						final EclipseRepository rep = (EclipseRepository)repository;
						final WorkspaceFileAccess wfa = new WorkspaceFileAccess(resourcesProject.getWorkspace());
						for (File styleFile : stylesFile)
						{
							final String txtContent = Utils.getTXTFileContent(styleFile, Charset.forName("UTF-8")); //$NON-NLS-1$
							if (txtContent != null)
							{
								final String styleName;
								if (styleFile.getName().indexOf('.') >= 0) styleName = styleFile.getName().substring(0, styleFile.getName().lastIndexOf('.'));
								else styleName = styleFile.getName();
								WorkspaceJob writeContentsJob = new WorkspaceJob("Persisting installed style: " + styleName)
								{
									@Override
									public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
									{
										try
										{
											// if it's a new resources project, create it
											if (!resourcesProject.exists()) ResourcesProjectSetupJob.createResourcesProject(resourcesProject);

											// create a style object without caching it or adding it to the rep., because it might be for another resources project
											// not for the active one
											UUID uuid = UUID.randomUUID();
											// the new element ID will not be serialized anyway, so we could even use -1 there...
											Style style = (Style)rep.createRootObject(rep.createRootObjectMetaData(rep.getNewElementID(uuid), uuid, styleName,
												IRepository.STYLES, 1, 1));
											style.setContent(txtContent);
											StringResourceDeserializer.writeStringResource(style, wfa, resourcesProject.getName());
										}
										catch (final Exception ex)
										{
											ServoyLog.logError(ex);
											UIUtils.runInUI(new Runnable()
											{

												public void run()
												{
													MessageDialog.openError(UIUtils.getActiveShell(), "Extension install task", "Error installing style: " +
														styleName + ".\n\n" + ex.getMessage());
												}
											}, false);
										}
										return Status.OK_STATUS;
									}
								};
								writeContentsJob.setSystem(true);
								writeContentsJob.setUser(false);
								writeContentsJob.schedule();
							}
						}
					}
					else
					{
						ServoyLog.logError("Cannot install styles", new RuntimeException("Internal error: unexpected repo. type")); //$NON-NLS-1$ //$NON-NLS-2$
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
