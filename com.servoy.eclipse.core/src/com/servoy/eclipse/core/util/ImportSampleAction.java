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

package com.servoy.eclipse.core.util;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.PrintWriter;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.XMLEclipseWorkspaceImportHandlerVersions11AndHigher;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.servlets.ConfigServletImportUserChannel;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.xmlxport.IXMLImportEngine;
import com.servoy.j2db.util.xmlxport.IXMLImportHandlerVersions11AndHigher;
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;

/**
 * @author lvostinar
 *
 */
public class ImportSampleAction implements IWorkbenchWindowActionDelegate
{

	@Override
	public void run(IAction action)
	{
		final File sampleSolution = new File(
			ApplicationServerRegistry.get().getServoyApplicationServerDirectory() + "/solutions/examples/servoy_sample_crm.servoy");
		if (sampleSolution.exists())
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					try
					{
						WorkspaceJob job = new WorkspaceJob("Importing Servoy Solution")
						{
							@Override
							public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException
							{
								ConfigServletImportUserChannel userChannel = new ConfigServletImportUserChannel(new PrintWriter(new CharArrayWriter()), true,
									true, true, false, true, true, true, true, true, true, true, true, false,
									IXMLImportUserChannel.IMPORT_USER_POLICY_CREATE_U_UPDATE_G, true, true, false, false, false);
								IApplicationServerSingleton as = ApplicationServerRegistry.get();

								try
								{
									IXMLImportEngine importEngine = as.createXMLImportEngine(sampleSolution,
										(EclipseRepository)ServoyModel.getDeveloperRepository(), as.getDataServer(), as.getClientId(), userChannel);

									IXMLImportHandlerVersions11AndHigher x11handler = as.createXMLInMemoryImportHandler(importEngine.getVersionInfo(),
										as.getDataServer(), as.getClientId(), userChannel, (EclipseRepository)ServoyModel.getDeveloperRepository());

									IRootObject[] rootObjects = XMLEclipseWorkspaceImportHandlerVersions11AndHigher.importFromJarFile(importEngine, x11handler,
										userChannel, (EclipseRepository)ServoyModel.getDeveloperRepository(), "resources",
										ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject(), monitor, true, false);
									if (rootObjects != null && rootObjects.length > 0)
									{
										Display.getDefault().asyncExec(new Runnable()
										{
											public void run()
											{
												MessageDialog.openInformation(new Shell(), "Sample imported",
													"Servoy sample was imported. Press 'Get Started' in order to start using the sample.");
											}
										});
									}
								}
								catch (final RepositoryException ex)
								{
									Debug.error(ex);
									Display.getDefault().asyncExec(new Runnable()
									{
										public void run()
										{
											MessageDialog.openError(new Shell(), "Cannot import sample",
												"An error occured while importing sample: " + ex.getMessage());
										}
									});
								}
								return Status.OK_STATUS;
							}
						};
						job.setUser(true);
						job.schedule();
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}

			});
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection)
	{

	}

	@Override
	public void dispose()
	{

	}

	@Override
	public void init(IWorkbenchWindow window)
	{
	}

}
