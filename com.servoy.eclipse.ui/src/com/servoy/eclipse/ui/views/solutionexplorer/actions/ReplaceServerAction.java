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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.wizards.ReplaceTableWizard;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.DataSourceCollectorVisitor;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.DataSourceUtils;

public class ReplaceServerAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;
	private String sourceServer;
	private String targetServer;
	private boolean replaceInCalculations;

	public ReplaceServerAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD_DISABLED));
		setText("Replace server");
		setToolTipText("Replace server");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			SimpleUserNode node = ((SimpleUserNode)sel.getFirstElement());
			state = (node.getType() == UserNodeType.SOLUTION && node.getRealObject() != null);
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getRealObject() instanceof ServoyProject)
		{
			EditorUtil.saveDirtyEditors(viewer.getSite().getShell(), true);
			final ServoyProject project = (ServoyProject)node.getRealObject();
			final Solution solution = project.getEditingSolution();
			targetServer = null;
			sourceServer = null;
			replaceInCalculations = false;
			Wizard replaceServer = new Wizard()
			{
				@Override
				public boolean performFinish()
				{
					IRunnableWithProgress solutionSaveRunnable = new IRunnableWithProgress()
					{
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
						{
							String jobName = "Performing the solution saving;";
							monitor.beginTask(jobName, 1);
							try
							{
								if (sourceServer != null && targetServer != null)
								{
									solution.acceptVisitor(new IPersistVisitor()
									{
										public Object visit(IPersist object)
										{
											if (object instanceof Form)
											{
												// The object is a form, and a form has a table.
												Form form = ((Form)object);
												if (sourceServer.equals(form.getServerName()))
												{
													form.setServerName(targetServer);
												}
											}
											else if (object instanceof Relation)
											{
												// The object is a relation, and a relation has a primary and foreign table.
												Relation relation = ((Relation)object);
												if (sourceServer.equals(relation.getPrimaryServerName()))
												{
													relation.setPrimaryServerName(targetServer);
												}
												if (sourceServer.equals(relation.getForeignServerName()))
												{
													relation.setForeignServerName(targetServer);
												}
											}
											else if (object instanceof ValueList)
											{
												// The object is valuelist
												ValueList vl = ((ValueList)object);
												if (sourceServer.equals(vl.getServerName()))
												{
													vl.setServerName(targetServer);
												}
											}
											else if (object instanceof TableNode && replaceInCalculations)
											{
												TableNode tableNode = ((TableNode)object);
												if (sourceServer.equals(tableNode.getServerName()))
												{
													tableNode.setRuntimeProperty(AbstractBase.NameChangeProperty, tableNode.getDataSource());
													tableNode.setServerName(targetServer);
												}
											}
											return IPersistVisitor.CONTINUE_TRAVERSAL;
										}
									});
									project.saveEditingSolutionNodes(new IPersist[] { solution }, true);
									ModelUtils.getEditingFlattenedSolution(solution).flushAllCachedData();
									ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().flushAllCachedData();
								}
							}
							catch (RepositoryException e)
							{
								ServoyLog.logError(e);
							}

							monitor.worked(1);
							monitor.done();
						}
					};

					try
					{
						PlatformUI.getWorkbench().getProgressService().run(true, false, solutionSaveRunnable);
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
					return true;
				}

			};
			replaceServer.setWindowTitle("Replace server");
			WizardPage page = new WizardPage("Replace server selection")
			{
				private Combo sourceServersCombo;
				private Combo targetServersCombo;

				public void createControl(Composite parent)
				{
					Composite topLevel = new Composite(parent, SWT.NONE);
					topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

					setControl(topLevel);

					Label sourceServerLabel = new Label(topLevel, SWT.NONE);
					sourceServerLabel.setText("Source server");

					sourceServersCombo = new Combo(topLevel, SWT.DROP_DOWN | SWT.READ_ONLY);
					UIUtils.setDefaultVisibleItemCount(sourceServersCombo);
					sourceServersCombo.addSelectionListener(new SelectionAdapter()
					{
						@Override
						public void widgetSelected(SelectionEvent event)
						{
							sourceServer = sourceServersCombo.getText();
							setPageComplete(validatePage());
						}
					});

					Label targetServerLabel = new Label(topLevel, SWT.NONE);
					targetServerLabel.setText("Target server");

					targetServersCombo = new Combo(topLevel, SWT.DROP_DOWN | SWT.READ_ONLY);
					UIUtils.setDefaultVisibleItemCount(targetServersCombo);
					targetServersCombo.addSelectionListener(new SelectionAdapter()
					{
						@Override
						public void widgetSelected(SelectionEvent event)
						{
							targetServer = targetServersCombo.getText();
							setPageComplete(validatePage());
						}
					});

					final Button replaceCalculationsButton = new Button(topLevel, SWT.CHECK);
					replaceCalculationsButton.setText("Replace server in calculations/aggregations"); //$NON-NLS-1$
					replaceCalculationsButton.addSelectionListener(new SelectionAdapter()
					{
						@Override
						public void widgetSelected(SelectionEvent e)
						{
							replaceInCalculations = replaceCalculationsButton.getSelection();
						}
					});

					FormLayout formLayout = new FormLayout();
					formLayout.spacing = 5;
					formLayout.marginWidth = formLayout.marginHeight = 20;
					topLevel.setLayout(formLayout);

					// SOURCES
					//label
					FormData formData = new FormData();
					formData.left = new FormAttachment(0, 0);
					formData.top = new FormAttachment(sourceServersCombo, 0, SWT.CENTER);
					sourceServerLabel.setLayoutData(formData);

					//combo
					formData = new FormData();
					formData.left = new FormAttachment(sourceServerLabel, 0);
					formData.top = new FormAttachment(0, 0);
					formData.right = new FormAttachment(100, 0);
					sourceServersCombo.setLayoutData(formData);

					formData = new FormData();
					formData.left = new FormAttachment(0, 0);
					formData.top = new FormAttachment(targetServersCombo, 0, SWT.CENTER);
					targetServerLabel.setLayoutData(formData);

					formData = new FormData();
					formData.left = new FormAttachment(sourceServersCombo, 0, SWT.LEFT);
					formData.top = new FormAttachment(sourceServersCombo, 0, SWT.BOTTOM);
					formData.right = new FormAttachment(100, 0);
					targetServersCombo.setLayoutData(formData);

					formData = new FormData();
					formData.left = new FormAttachment(0, 0);
					formData.top = new FormAttachment(targetServerLabel, 20, SWT.LEFT);
					replaceCalculationsButton.setLayoutData(formData);

					DataSourceCollectorVisitor datasourceCollector = new DataSourceCollectorVisitor();
					solution.acceptVisitor(datasourceCollector);
					sourceServersCombo.setItems(DataSourceUtils.getServerNames(datasourceCollector.getDataSources()).toArray(new String[] { }));
					sourceServersCombo.add(ReplaceTableWizard.ALL_SOLUTION_SERVERS, 0);
					sourceServersCombo.select(0);
					IServerManagerInternal serverManager = ServoyModel.getServerManager();
					targetServersCombo.setItems(serverManager.getServerNames(false, false, true, true));
					targetServersCombo.add(ReplaceTableWizard.ALL_SERVERS, 0);
					targetServersCombo.select(0);
					setPageComplete(false);

				}

				public boolean validatePage()
				{
					if (sourceServer == null || sourceServersCombo == null || ReplaceTableWizard.ALL_SOLUTION_SERVERS.equals(sourceServersCombo.getText())) return false;
					if (targetServer == null || targetServersCombo == null || ReplaceTableWizard.ALL_SERVERS.equals(targetServersCombo.getText())) return false;
					return true;
				}
			};
			page.setTitle("Select a server to be replaced");
			replaceServer.addPage(page);
			WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), replaceServer);
			dialog.create();
			dialog.open();
		}
	}
}
