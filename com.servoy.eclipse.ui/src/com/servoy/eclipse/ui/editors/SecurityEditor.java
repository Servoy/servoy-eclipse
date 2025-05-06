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
package com.servoy.eclipse.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.ViewPartHelpContextProvider;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;
import com.servoy.j2db.util.docvalidator.LengthDocumentValidator;
import com.servoy.j2db.util.docvalidator.ValidatingDocument.IDocumentValidator;

public class SecurityEditor extends EditorPart implements IActiveProjectListener
{

	private Table groupTable;
	private Tree usersTree;
	private Text userNameText;
	private Text groupText;
	private Composite treeContainer;
	private Composite tableContainer;
	public static final String ID = "com.servoy.eclipse.ui.editors.SecurityEditor";

	private boolean disposed = false;

	private final SecurityModel model;

	public SecurityEditor()
	{
		super();
		model = new SecurityModel();
	}

	/**
	 * Create contents of the editor part
	 *
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent)
	{

		//parent.setLayout(new FillLayout());
		ScrolledComposite myScrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);
		myScrolledComposite.setData(CSSSWTConstants.CSS_ID_KEY, "svyeditor");

		Composite container = new Composite(myScrolledComposite, SWT.NONE);

		myScrolledComposite.setContent(container);

		groupText = new Text(container, SWT.BORDER);

		groupText.addVerifyListener(new DocumentValidatorVerifyListener(
			new IDocumentValidator[] { new IdentDocumentValidator(IdentDocumentValidator.TYPE_JSON), new LengthDocumentValidator(
				200) }));

		Button newGroupButton;
		newGroupButton = new Button(container, SWT.NONE);
		newGroupButton.setText("New Permission");

		Button newUserButton;
		newUserButton = new Button(container, SWT.NONE);
		newUserButton.setText("New User");

		userNameText = new Text(container, SWT.BORDER);

		userNameText.addVerifyListener(DocumentValidatorVerifyListener.IDENT_JSON_VERIFIER);

		treeContainer = new Composite(container, SWT.NONE);
		tableContainer = new Composite(container, SWT.NONE);

		usersTree = new Tree(treeContainer, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK | SWT.V_SCROLL);

		groupTable = new Table(tableContainer, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.CHECK);

		final Button removeUserButton;
		removeUserButton = new Button(container, SWT.NONE);
		removeUserButton.setText("Remove User");
		removeUserButton.setEnabled(false);
		removeUserButton.setToolTipText("Delete selected user");

		final Button removeGroupButton;
		removeGroupButton = new Button(container, SWT.NONE);
		removeGroupButton.setText("Remove Permission(s)");
		removeGroupButton.setEnabled(false);
		removeGroupButton.setToolTipText("Delete checked permission(s)");

		newGroupButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (groupText.getText() != null && groupText.getText().length() != 0 && model.isColumnValid(groupText.getText(), CI_GROUP))
				{
					model.addGroup(groupText.getText());
					TableItem item = new TableItem(groupTable, SWT.NONE);
					item.setText(groupText.getText());
					flagModified();
				}
				else
				{
					MessageDialog.openError(getSite().getShell(), "Cannot create permission", "Please enter a valid name in textbox");
				}
			}
		});
		newUserButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (userNameText.getText() != null && userNameText.getText().length() != 0 && model.isColumnValid(userNameText.getText(), CI_NAME))
				{
					model.addUser(userNameText.getText());
					TreeItem item = new TreeItem(usersTree, SWT.NONE);
					item.setText(new String[] { userNameText.getText(), "password" });
					item.setChecked(false);
					flagModified();
				}
				else
				{
					MessageDialog.openError(getSite().getShell(), "Cannot create user", "Please enter a valid name in textbox");
				}
			}
		});
		removeUserButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (usersTree.getSelectionCount() == 1)
				{
					model.removeUser(usersTree.getSelection()[0].getText(CI_NAME));
					usersTree.getSelection()[0].dispose();
					flagModified();

				}
			}
		});

		groupTable.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (groupTable.getSelectionCount() == 1)
				{
					if (e.detail == SWT.CHECK)
					{
						List<String> groups = getCheckedGroups();
						if (groups != null && groups.size() > 0 && !groups.contains(IRepository.ADMIN_GROUP))
						{
							removeGroupButton.setEnabled(true);
						}
						else
						{
							removeGroupButton.setEnabled(false);
						}
					}
					model.createTreeData(usersTree, groupTable.getSelection()[0].getText());
				}
			}
		});

		groupTable.setToolTipText("Permission name(s)");
		groupTable.setHeaderVisible(true);
		groupTable.setLinesVisible(true);

		usersTree.setHeaderVisible(true);
		usersTree.setLinesVisible(true);

		usersTree.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				removeUserButton.setEnabled(true);
			}
		});

		removeGroupButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				ArrayList<String> groups = getCheckedGroups();
				if (groups != null && groups.size() > 0)
				{
					String groupsToDelete = groups.toString();
					if (MessageDialog.openConfirm(getSite().getShell(), "Delete permission(s)",
						"Are you sure you want to delete the permission(s): " + groupsToDelete + " ?"))
					{
						model.removeGroups(groups);
						for (TableItem item : groupTable.getItems())
						{
							if (groups.contains(item.getText()))
							{
								groupTable.remove(groupTable.indexOf(item));
							}
						}
						removeGroupButton.setEnabled(false);
						flagModified();
					}
				}
			}
		});

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().add(groupText, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE).addPreferredGap(
					LayoutStyle.RELATED).add(newGroupButton))
				.add(removeGroupButton).add(tableContainer, GroupLayout.PREFERRED_SIZE, 288,
					GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.TRAILING).add(groupLayout.createSequentialGroup().add(userNameText,
						GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED).add(newUserButton)).add(
							removeUserButton)
						.add(treeContainer, GroupLayout.PREFERRED_SIZE, 183, Short.MAX_VALUE))
				.addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().addContainerGap().add(
			groupLayout.createParallelGroup(GroupLayout.BASELINE).add(newUserButton).add(userNameText).add(groupText).add(newGroupButton)).addPreferredGap(
				LayoutStyle.RELATED)
			.add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(tableContainer, GroupLayout.PREFERRED_SIZE, 249, Short.MAX_VALUE).add(
					treeContainer, GroupLayout.PREFERRED_SIZE, 249, Short.MAX_VALUE))
			.addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(removeUserButton).add(removeGroupButton))
			.addContainerGap()));
		container.setLayout(groupLayout);

		initDataBindings();

		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	public static final int CI_NAME = 0;
	public static final int CI_PASSWORD = 1;
	public static final int CI_UID = 2;
	public static final int CI_GROUP = -1;

	private void initDataBindings()
	{
		TreeColumn nameColumn = new TreeColumn(usersTree, SWT.LEFT, CI_NAME);
		nameColumn.setText("Username");
		nameColumn.setToolTipText("Doubleclick cell to edit/Use checkbox for user assignment to selected group");

		TreeColumn passwordColumn = new TreeColumn(usersTree, SWT.LEFT, CI_PASSWORD);
		passwordColumn.setText("Password");
		passwordColumn.setToolTipText("Doubleclick cell to edit");

		TreeColumn uidColumn = new TreeColumn(usersTree, SWT.LEFT, CI_UID);
		uidColumn.setText("Uid");
		uidColumn.setToolTipText("Doubleclick cell to edit");

		TreeColumnLayout layout = new TreeColumnLayout();
		treeContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(20, 50, true));
		layout.setColumnData(passwordColumn, new ColumnWeightData(20, 50, true));
		layout.setColumnData(uidColumn, new ColumnWeightData(40, 50, true));

		TableColumn groupColumn = new TableColumn(groupTable, SWT.NONE, 0);
		groupColumn.setText("Permission name");
		groupColumn.setToolTipText("Select a group to assign users to it (users checkbox)");
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableLayout.setColumnData(groupColumn, new ColumnWeightData(20, 50, true));
		tableContainer.setLayout(tableLayout);

		model.createTreeData(usersTree, null);
		final TreeEditor editor = new TreeEditor(usersTree);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		usersTree.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDoubleClick(MouseEvent event)
			{
				final TreeItem item = usersTree.getItem(new Point(event.x, event.y));
				if (item != null)
				{
					boolean editPassword = item.getBounds(CI_PASSWORD).contains(new Point(event.x, event.y));

					final Text text;
					final int columnCount;
					if (!editPassword)
					{
						text = new Text(usersTree, SWT.NONE);
						if (item.getBounds(CI_NAME).contains(new Point(event.x, event.y)))
						{
							columnCount = CI_NAME;
						}
						else
						{
							columnCount = CI_UID;
						}
						text.setText(item.getText(columnCount));
					}
					else
					{
						text = new Text(usersTree, SWT.PASSWORD);
						columnCount = CI_PASSWORD;
						text.setText("");
					}

					text.addFocusListener(new FocusAdapter()
					{
						@Override
						public void focusLost(FocusEvent event)
						{
							if (model.isColumnValid(text.getText(), columnCount))
							{
								model.editElement(item.getText(CI_NAME), text.getText(), columnCount);
								if (columnCount != CI_PASSWORD) item.setText(columnCount, text.getText());
								flagModified();
							}
							text.dispose();
							usersTree.setFocus();
						}
					});

					text.addKeyListener(new KeyAdapter()
					{
						@Override
						public void keyPressed(KeyEvent event)
						{
							switch (event.keyCode)
							{
								case SWT.CR :
									if (model.isColumnValid(text.getText(), columnCount))
									{
										model.editElement(item.getText(CI_NAME), text.getText(), columnCount);
										if (columnCount != CI_PASSWORD) item.setText(columnCount, text.getText());
										flagModified();
									}
								case SWT.ESC :
									text.dispose();
									usersTree.setFocus();
									break;
							}
						}
					});

					editor.setEditor(text, item, columnCount);
					text.selectAll();
					text.setFocus();
				}
			}
		});
		usersTree.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event event)
			{
				if (event.detail == SWT.CHECK && groupTable.getSelectionCount() == 1)
				{
					model.modifyUserGroup(((TreeItem)event.item).getText(CI_NAME), groupTable.getSelection()[0].getText(), ((TreeItem)event.item).getChecked());
					flagModified();
				}
			}
		});

		addDataToGroupList();

		groupTable.setSelection(0);
		groupTable.notifyListeners(SWT.Selection, new Event());

	}

	private void addDataToGroupList()
	{
		groupTable.removeAll();
		IDataSet groups = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getGroups(ApplicationServerRegistry.get().getClientId());
		int groupsNr = groups.getRowCount();
		String[] groupNames = new String[groupsNr];
		boolean mustCreateAdmin = true;
		for (int i = 0; i < groupsNr; i++)
		{
			groupNames[i] = groups.getRow(i)[1].toString();
			if (groupNames[i].equals(IRepository.ADMIN_GROUP))
			{
				mustCreateAdmin = false;
			}
		}
		for (String name : groupNames)
		{
			TableItem item = new TableItem(groupTable, SWT.NONE, groupTable.getItemCount());
			item.setText(name);
		}
		if (mustCreateAdmin)
		{
			try
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().createGroup(ApplicationServerRegistry.get().getClientId(),
					IRepository.ADMIN_GROUP);
				TableItem item = new TableItem(groupTable, SWT.NONE, 0);
				item.setText(IRepository.ADMIN_GROUP);
			}
			catch (ServoyException e)
			{
				ServoyLog.logError(e);
				MessageDialog.openError(getSite().getShell(), "Error", "Save failed: " + e.getMessage());
			}

		}
	}

	@Override
	public void setFocus()
	{
		groupText.forceFocus();
	}

	private boolean isModified;

	public void flagModified()
	{
		isModified = true;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public boolean isDirty()
	{
		return isModified;
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		ServoyProject[] servoyProjects = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		for (ServoyProject servoyProject : servoyProjects)
		{
			try
			{
				IMarker[] markers = servoyProject.getProject().findMarkers(ServoyBuilder.USER_SECURITY_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				if (markers != null && markers.length > 0)
				{
					MessageDialog.openError(UIUtils.getActiveShell(), "Cannot save security settings",
						"There are security errors in the solution, you must solve those first.");
					return;
				}
			}
			catch (CoreException ex)
			{
				ServoyLog.logError(ex);
			}
		}
		model.saveSecurity();
		isModified = false;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}


	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		setInput(input);
		ServoyModelManager.getServoyModelManager().getServoyModel().addActiveProjectListener(this);
	}

	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	@Override
	public void doSaveAs()
	{
	}

	public void activeProjectChanged(ServoyProject activeProject)
	{
	}

	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
		if (updateInfo == IActiveProjectListener.SECURITY_INFO_CHANGED)
		{
			Display.getDefault().asyncExec(new Runnable()
			{

				public void run()
				{
					if (!disposed && model != null && groupTable != null && usersTree != null && usersTree.isDisposed() != true &&
						groupTable.isDisposed() != true)
					{
						String selection = null;
						if (groupTable.getSelection() != null && groupTable.getSelection().length > 0) selection = groupTable.getSelection()[0].getText();
						addDataToGroupList();
						boolean selected = false;
						if (selection != null)
						{
							for (TableItem item : groupTable.getItems())
							{
								if (selection.equals(item.getText()))
								{
									groupTable.setSelection(item);
									selected = true;
									break;
								}
							}
						}
						if (!selected) groupTable.setSelection(0);
						groupTable.notifyListeners(SWT.Selection, new Event());
					}
				}
			});
		}
		else if (updateInfo == IActiveProjectListener.RESOURCES_UPDATED_BECAUSE_ACTIVE_PROJECT_CHANGED ||
			updateInfo == IActiveProjectListener.RESOURCES_UPDATED_ON_ACTIVE_PROJECT)
		{
			closeEditor(false); // resources project changed already - so do not save
		}
	}

	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
	{
		return true;
	}

	protected void closeEditor(final boolean save)
	{
		if (Display.getCurrent() != null)
		{
			getSite().getPage().closeEditor(SecurityEditor.this, save);
		}
		else
		{
			getSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable()
			{
				public void run()
				{
					getSite().getPage().closeEditor(SecurityEditor.this, false); // do not try to save later because resource project might already be changed
				}
			});
		}
	}

	@Override
	public void dispose()
	{
		disposed = true;
		ServoyModelManager.getServoyModelManager().getServoyModel().removeActiveProjectListener(this);
		super.dispose();
	}

	private ArrayList<String> getCheckedGroups()
	{
		ArrayList<String> groups = new ArrayList<String>();
		for (int i = 0; i < groupTable.getItemCount(); i++)
		{
			if (groupTable.getItem(i).getChecked())
			{
				groups.add(groupTable.getItem(i).getText());
			}
		}
		return groups;
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (adapter.equals(IContextProvider.class))
		{
			return new ViewPartHelpContextProvider("com.servoy.eclipse.ui.security_editor");
		}
		return super.getAdapter(adapter);
	}
}
