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

import java.util.Arrays;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
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
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.builder.ServoyBuilder;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.server.ApplicationServerSingleton;
import com.servoy.j2db.util.ServoyException;

public class SecurityEditor extends EditorPart implements IActiveProjectListener
{

	private List list;
	private Tree tree;
	private Text userNameText;
	private Text groupText;
	private Composite treeContainer;
	public static final String ID = "com.servoy.eclipse.ui.editors.SecurityEditor"; //$NON-NLS-1$

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

		Composite container = new Composite(myScrolledComposite, SWT.NONE);

		myScrolledComposite.setContent(container);

		groupText = new Text(container, SWT.BORDER);

		Button newGroupButton;
		newGroupButton = new Button(container, SWT.NONE);
		newGroupButton.setText("New Group");

		Button newUserButton;
		newUserButton = new Button(container, SWT.NONE);
		newUserButton.setText("New User");

		userNameText = new Text(container, SWT.BORDER);

		treeContainer = new Composite(container, SWT.NONE);
		tree = new Tree(treeContainer, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK | SWT.V_SCROLL);

		list = new List(container, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);

		final Button removeUserButton;
		removeUserButton = new Button(container, SWT.NONE);
		removeUserButton.setText("Remove User");
		removeUserButton.setEnabled(false);

		final Button removeGroupButton;
		removeGroupButton = new Button(container, SWT.NONE);
		removeGroupButton.setText("Remove Group");
		removeGroupButton.setEnabled(false);


		newGroupButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (groupText.getText() != null && groupText.getText().length() != 0 && model.isColumnValid(groupText.getText(), CI_GROUP))
				{
					model.addGroup(groupText.getText());
					list.add(groupText.getText());
					flagModified();

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
					TreeItem item = new TreeItem(tree, SWT.NONE);
					item.setText(new String[] { userNameText.getText(), "password" });
					item.setChecked(false);
					flagModified();

				}
			}
		});
		removeUserButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (tree.getSelectionCount() == 1)
				{
					model.removeUser(tree.getSelection()[0].getText(CI_NAME));
					tree.getSelection()[0].dispose();
					flagModified();

				}
			}
		});

		list.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (list.getSelectionCount() == 1)
				{
					String group = list.getSelection()[0];
					if (group.equals(IRepository.ADMIN_GROUP))
					{
						removeGroupButton.setEnabled(false);
					}
					else
					{
						removeGroupButton.setEnabled(true);
					}
					model.createTreeData(tree, list.getSelection()[0]);
				}

			}
		});


		tree.addSelectionListener(new SelectionAdapter()
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
				if (list.getSelectionCount() == 1)
				{
					String group = list.getSelection()[0];
					if (MessageDialog.openConfirm(getSite().getShell(), "Delete group", "Are you sure you want to delete the group " + group + " ?"))
					{
						model.removeGroup(group);
						list.remove(group);
						flagModified();
					}

				}
			}
		});

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(groupText, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE).addPreferredGap(
						LayoutStyle.RELATED).add(newGroupButton)).add(removeGroupButton).add(list, GroupLayout.PREFERRED_SIZE, 288, GroupLayout.PREFERRED_SIZE)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
					groupLayout.createSequentialGroup().add(userNameText, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE).addPreferredGap(
						LayoutStyle.RELATED).add(newUserButton)).add(removeUserButton).add(treeContainer, GroupLayout.PREFERRED_SIZE, 183, Short.MAX_VALUE)).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(newUserButton).add(userNameText).add(groupText).add(newGroupButton)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(list, GroupLayout.PREFERRED_SIZE, 249, Short.MAX_VALUE).add(treeContainer,
					GroupLayout.PREFERRED_SIZE, 249, Short.MAX_VALUE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(removeUserButton).add(removeGroupButton)).addContainerGap()));
		container.setLayout(groupLayout);

		initDataBindings();

		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	public static final int CI_NAME = 0;
	public static final int CI_PASSWORD = 1;
	public static final int CI_GROUP = -1;

	private void initDataBindings()
	{
		TreeColumn nameColumn = new TreeColumn(tree, SWT.LEFT, CI_NAME);
		nameColumn.setText("Name");
		//nameColumn.setWidth(400);

		TreeColumn passwordColumn = new TreeColumn(tree, SWT.LEFT, CI_PASSWORD);
		nameColumn.setText("Password");

		TreeColumnLayout layout = new TreeColumnLayout();
		treeContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(20, 50, true));
		layout.setColumnData(passwordColumn, new ColumnWeightData(20, 50, true));

		model.createTreeData(tree, null);
		final TreeEditor editor = new TreeEditor(tree);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		tree.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDoubleClick(MouseEvent event)
			{
				final TreeItem item = tree.getItem(new Point(event.x, event.y));
				boolean editUserName = item.getBounds(CI_NAME).contains(new Point(event.x, event.y));

				final Text text;
				final int columnCount;
				if (editUserName)
				{
					text = new Text(tree, SWT.NONE);
					text.setText(item.getText());
					columnCount = CI_NAME;
				}
				else
				{
					text = new Text(tree, SWT.PASSWORD);
					columnCount = CI_PASSWORD;
					text.setText("");
				}

				text.selectAll();
				text.setFocus();

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
								break;
						}
					}
				});

				editor.setEditor(text, item, columnCount);
			}
		});
		tree.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event event)
			{
				if (event.detail == SWT.CHECK && list.getSelectionCount() == 1)
				{
					model.modifyUserGroup(((TreeItem)event.item).getText(CI_NAME), list.getSelection()[0], ((TreeItem)event.item).getChecked());
					flagModified();
				}
			}
		});

		addDataToGroupList();

		list.setSelection(0);
		list.notifyListeners(SWT.Selection, new Event());

	}

	private void addDataToGroupList()
	{
		IDataSet groups = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getGroups(ApplicationServerSingleton.get().getClientId());
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

		list.setItems(groupNames);
		if (mustCreateAdmin)
		{
			try
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().createGroup(ApplicationServerSingleton.get().getClientId(),
					IRepository.ADMIN_GROUP);
				list.add(IRepository.ADMIN_GROUP, 0);
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
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Cannot save security settings",
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
					if (!disposed && model != null && list != null && tree != null && tree.isDisposed() != true && list.isDisposed() != true)
					{
						String selection = null;
						if (list.getSelection() != null && list.getSelection().length > 0) selection = list.getSelection()[0];
						addDataToGroupList();
						java.util.List<String> items = Arrays.asList(list.getItems());
						if (selection != null && items.contains(selection))
						{
							list.setSelection(items.indexOf(selection));
						}
						else
						{
							list.setSelection(0);
						}
						list.notifyListeners(SWT.Selection, new Event());
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
}
