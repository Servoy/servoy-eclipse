/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ServerSettings;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;

/**
 * Action to convert solution to new format.
 *
 * @author gboros
 */
public class ConvertToNewFormatAction extends Action implements ISelectionChangedListener
{
	private final Shell shell;
	private ServoyProject selectedSolutionProject;


	public ConvertToNewFormatAction(Shell shell)
	{
		this.shell = shell;
		setText(Messages.ConvertSolutionToNewFormatAction_convertToNewFormat);
		setToolTipText(Messages.ConvertSolutionToNewFormatAction_convertToNewFormat);
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		boolean enabled = true;
		ISelection sel = event.getSelection();
		if (sel instanceof IStructuredSelection)
		{
			IStructuredSelection s = (IStructuredSelection)sel;
			enabled = (s.size() == 1);
			if (enabled)
			{
				SimpleUserNode node = (SimpleUserNode)s.getFirstElement();
				UserNodeType type = node.getType();
				if (((type == UserNodeType.SOLUTION) || (type == UserNodeType.SOLUTION_ITEM) || (type == UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE)) &&
					(node.getRealObject() instanceof ServoyProject))
				{
					selectedSolutionProject = (ServoyProject)node.getRealObject();

//					// check if it is already converted
//					final IFileAccess wfa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
//					File frmd = new File(wfa.getProjectFile(selectedSolutionProject.getProject().getName()), SolutionSerializer.SOLUTION_SETTINGS);
//					String solutionmetadata = Utils.getTXTFileContent(frmd);
//					ServoyJSONObject solutionMetaDataJSONObject = new ServoyJSONObject(solutionmetadata, false);
//					String solutionmetadataConverter = solutionMetaDataJSONObject.toString();
//
//					if (solutionmetadataConverter.equals(solutionmetadata))
//					{
//						selectedSolutionProject = null;
//						enabled = false;
//					}
				}
				else
				{
					enabled = false;
				}
			}
		}
		else
		{
			enabled = false;
		}
		if (!enabled)
		{
			selectedSolutionProject = null;
		}
		setEnabled(enabled);
	}

	@Override
	public void run()
	{
		if (selectedSolutionProject == null) return;
		// Create a modal progress dialog
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);

		try
		{
			dialog.run(true, false, monitor -> {
				try
				{
					ResourcesPlugin.getWorkspace().run(workspaceMonitor -> {
						Solution[] modules = selectedSolutionProject.getModules();
						monitor.beginTask("Converting " + selectedSolutionProject.getSolution().getName() + " and its modules...", modules.length + 2);
						for (Solution solution : modules)
						{
							monitor.subTask("Converting " + solution.getName());
							convertSolutionToNewFormat(solution);
							monitor.worked(1);
							if (monitor.isCanceled()) break;
						}

						final boolean[] resourceProjectConvert = new boolean[] { true };
						Display.getDefault().syncExec(() -> {
							resourceProjectConvert[0] = MessageDialog.openQuestion(UIUtils.getActiveShell(),
								Messages.ConvertSolutionToNewFormatAction_convertToNewFormat,
								"Convert the database and security information files from the resource project?");
						});

						if (resourceProjectConvert[0])
						{
							monitor.subTask("Converting the resource project database information");
							IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
							for (String serverName : serverManager.getServerNames(false, true, false, true))
							{
								ServerSettings serverSettings = serverManager.getServerSettings(serverName);
								try
								{
									IFile serverDbiFile = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager()
										.getServerDBIFile(serverName);
									if (serverDbiFile != null && serverDbiFile.exists())
									{
										ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager().updateServerSettings(serverName,
											serverSettings);
									}
									IServer server = serverManager.getServer(serverName);
									for (String tableName : server.getTableNames(true))
									{
										IFile tableDbiFile = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager().getDBIFile(
											serverName,
											tableName);
										if (tableDbiFile != null && tableDbiFile.exists())
										{
											ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager()
												.updateAllColumnInfo(server.getTable(tableName));
										}
									}
								}
								catch (Exception e)
								{
									Debug.error(e);
								}
							}
							monitor.worked(1);

							monitor.subTask("Converting the resource project security information");
							try
							{
								ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().writeAllSecurityInformation(true);
							}
							catch (Exception e)
							{
								Debug.error(e);
							}
							monitor.worked(1);
						}
						else
						{
							monitor.worked(2);
						}

						monitor.done();
					}, null, IWorkspace.AVOID_UPDATE, monitor);
				}
				catch (CoreException e)
				{
					Debug.error(e);
				}
			});
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	private void convertSolutionToNewFormat(Solution solution)
	{
		final IFileAccess wfa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
		EclipseRepository eclipseRepository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
		List<IPersist> objectsToSave = new ArrayList<IPersist>(solution.getAllObjectsAsList());
		try
		{
			solution.getRootObjectMetaData().flagChanged();
			SolutionSerializer.writePersist(solution, wfa, eclipseRepository, true, false, false);
			boolean mediaAlreadyConverted = false;
			for (IPersist persist : objectsToSave)
			{
				if (persist instanceof Form ||
					(persist instanceof Media && !mediaAlreadyConverted) ||
					persist instanceof Menu ||
					persist instanceof Relation ||
					persist instanceof ValueList ||
					persist instanceof TableNode)
				{
					SolutionSerializer.writePersist(persist, wfa, eclipseRepository, true, false, false);
					if (persist instanceof Media && !mediaAlreadyConverted)
					{
						// only convert media once
						mediaAlreadyConverted = true;
					}
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}
}