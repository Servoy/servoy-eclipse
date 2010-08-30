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
package com.servoy.eclipse.ui.actions;

import java.rmi.RemoteException;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.repository.EclipseMessages;
import com.servoy.eclipse.core.repository.TableWrapper;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.dialogs.I18NExternalizeDialog;
import com.servoy.eclipse.ui.dialogs.I18NServerTableDialog;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TableContentProvider;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.dialogs.TableContentProvider.TableListOptions;
import com.servoy.eclipse.ui.labelproviders.DatasourceLabelProvider;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.IControlFactory;
import com.servoy.eclipse.ui.views.IMaxDepthTreeContentProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Settings;

public class ShowI18NDialogActionDelegate implements IWorkbenchWindowActionDelegate
{
	public static final String ACTION_EDIT = "com.servoy.eclipse.ui.actions.ShowI1n8DialogActionDelegate";
	public static final String ACTION_EXTERNALIZE = "com.servoy.eclipse.ui.actions.I18nExternalizeDialog";

	private static final String DEFAULT_MESSAGES_TABLE = "defaultMessagesTable"; //$NON-NLS-1$
	private static final String DEFAULT_MESSAGES_SERVER = "defaultMessagesServer"; //$NON-NLS-1$

	private IWorkbenchWindow workbenchWindow;

	public void dispose()
	{
	}

	public void init(IWorkbenchWindow window)
	{
		this.workbenchWindow = window;
	}

	public void run(IAction action)
	{
		run(action.getId());
	}

	public void run(String actionId)
	{
		final Shell shell = (workbenchWindow == null ? new Shell() : workbenchWindow.getShell());

		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		final ServoyProject activeProject = servoyModel.getActiveProject();

		if (activeProject == null)
		{
			new MessageDialog(shell, ACTION_EXTERNALIZE.equals(actionId) ? "Externalize" : "Edit I18N messages", null,
				"The dialog does not work when there is no active solution set.", MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0).open();
			return;
		}
		String i18nDetails[] = hasI18NTable();
		if (i18nDetails == null)
		{
			if (MessageDialog.openQuestion(shell, ACTION_EXTERNALIZE.equals(actionId) ? "Externalize" : "Edit I18N messages",
				"There is no i18n table defined for the active solution. Do you want to define one now ?"))
			{
				TableContentProvider tableContentProvider = new TableContentProvider();
				final TreeSelectDialog dialog = new TreeSelectDialog(UIUtils.getActiveShell(), true, false, TreePatternFilter.FILTER_LEAFS,
					IMaxDepthTreeContentProvider.DEPTH_DEFAULT, tableContentProvider, DatasourceLabelProvider.INSTANCE_IMAGE_NAMEONLY, null,
					new LeafnodesSelectionFilter(tableContentProvider), SWT.NONE, "Select I18N table", new TableContentProvider.TableListOptions(
						TableListOptions.TableListType.I18N, true), null, "serverTableDialog", null);
				dialog.setOptionsAreaFactory(new IControlFactory()
				{
					public Control createControl(Composite composite)
					{
						Button createButton = new Button(composite, SWT.PUSH);
						createButton.setText("Create new I18N table"); //$NON-NLS-1$
						createButton.addListener(SWT.Selection, new Listener()
						{
							public void handleEvent(Event event)
							{
								String serverName = null;
								TreeSelection ts = (TreeSelection)dialog.getTreeViewer().getViewer().getSelection();
								if (!ts.isEmpty())
								{
									Object selection = ts.getFirstElement();
									if (selection instanceof TableWrapper)
									{
										serverName = ((TableWrapper)selection).getServerName();
									}
								}
								I18NServerTableDialog dlg = new I18NServerTableDialog(shell, serverName, "");
								dlg.open();

								if (dlg.getReturnCode() == Window.OK)
								{
									serverName = dlg.getSelectedServerName();
									String selectedTableName = dlg.getSelectedTableName();
									Table newTable = I18NServerTableDialog.createDefaultMessagesTable(serverName, selectedTableName, shell);

									dialog.refreshTree();
									dialog.getTreeViewer().setSelection(new StructuredSelection(new TableWrapper(newTable.getServerName(), newTable.getName())));
								}
							}
						});
						return createButton;
					}

				});
				dialog.open();

				if (dialog.getReturnCode() == Window.OK)
				{
					TableWrapper tableWrapper = (TableWrapper)((StructuredSelection)dialog.getSelection()).getFirstElement();
					if (tableWrapper.getServerName() != null && tableWrapper.getTableName() != null)
					{
						activeProject.getEditingSolution().setI18nDataSource(
							DataSourceUtils.createDBTableDataSource(tableWrapper.getServerName(), tableWrapper.getTableName()));
						EclipseMessages.showDatasourceWarning();
						try
						{
							activeProject.saveEditingSolutionNodes(new IPersist[] { activeProject.getEditingSolution() }, false);
							EclipseMessages.writeProjectI18NFiles(activeProject, false);
							i18nDetails = new String[] { tableWrapper.getServerName(), tableWrapper.getTableName() };
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}
		}


		if (i18nDetails != null)
		{
			WorkspaceJob i18nDialogJob;
			if (ACTION_EXTERNALIZE.equals(actionId))
			{
				i18nDialogJob = new WorkspaceJob("Finding texts to externalize ...")
				{
					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
					{
						final I18NExternalizeDialog dlg = new I18NExternalizeDialog(shell, activeProject);
						dlg.loadContent(null);
						if (!monitor.isCanceled())
						{
							Display.getDefault().asyncExec(new Runnable()
							{
								public void run()
								{
									if (dlg.hasContent()) dlg.open();
									else MessageDialog.openInformation(shell, shell.getText(), "No texts to externalize");
								}
							});
						}

						return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
					}
				};
				i18nDialogJob.setUser(true);
				ISchedulingRule rule = ServoyModel.getWorkspace().getRoot();
				i18nDialogJob.setRule(rule);
				i18nDialogJob.schedule();
			}
			else EditorUtil.openI18NEditor(i18nDetails[0], i18nDetails[1]);
		}


	}

	public void selectionChanged(IAction action, ISelection selection)
	{
	}

	public static String[] hasI18NTable()
	{
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject activeProject = servoyModel.getActiveProject();
		if (activeProject == null) return null;
		Solution activeSolution = activeProject.getEditingSolution();

		String serverName = activeSolution.getI18nServerName();
		String tableName = activeSolution.getI18nTableName();
		// If the solution does not define I18N server/table, then pick defaults from
		// global preferences.
		if (serverName == null || serverName.trim().length() == 0 || tableName == null || tableName.trim().length() == 0)
		{
			Settings settings = Settings.getInstance();
			serverName = settings.getProperty(DEFAULT_MESSAGES_SERVER);
			tableName = settings.getProperty(DEFAULT_MESSAGES_TABLE);
		}
		if (serverName == null || serverName.trim().length() == 0 || tableName == null || tableName.trim().length() == 0) return null;
		else return new String[] { serverName, tableName };
	}

	public static ITable getI18nTable(String serverName, String tableName)
	{
		IServer server = ServoyModel.getServerManager().getServer(serverName);
		if (server == null)
		{
			return null;
		}
		try
		{
			return server.getTable(tableName);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		catch (RemoteException e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

}
