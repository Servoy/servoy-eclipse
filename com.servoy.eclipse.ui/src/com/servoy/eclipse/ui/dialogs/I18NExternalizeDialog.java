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
package com.servoy.eclipse.ui.dialogs;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.ASTVisitor;
import org.eclipse.dltk.compiler.problem.IProblem;
import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.javascript.ast.CallExpression;
import org.eclipse.dltk.javascript.ast.Comment;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.SingleLineComment;
import org.eclipse.dltk.javascript.ast.StringLiteral;
import org.eclipse.dltk.javascript.parser.JavaScriptParser;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.FilterDelayJob;
import com.servoy.eclipse.ui.util.FilteredEntity;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.I18NUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.property.I18NMessagesModel;
import com.servoy.j2db.property.I18NMessagesModel.I18NMessagesModelEntry;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

public class I18NExternalizeDialog extends Dialog
{
	private static final int COLUMN_PROPERTY = 0;
	private static final int COLUMN_TEXT = 1;
	private static final int COLUMN_KEY = 2;

	private TreeViewer treeViewer;
	private TreeContentProvider treeContentProvider;
	private Text filterTextField;
	private Button databaseMessagesButton;
	private Button ignoreMessageButton;

	private final ContentSpec contentSpec = StaticContentSpecLoader.getContentSpec();
	private final Image solutionImage = Activator.getDefault().loadImageFromBundle("solution.gif");
	private final Image formImage = Activator.getDefault().loadImageFromBundle("designer.gif");
	private final Image elementImage = Activator.getDefault().loadImageFromBundle("element.gif");
	private final Image serverImage = Activator.getDefault().loadImageFromBundle("server.gif");
	private final Image tableImage = Activator.getDefault().loadImageFromBundle("portal.gif");
	private final ServoyProject project;
	private ArrayList<ServoyProject> i18nProjects;
	private HashMap<String, String> defaultMessages;
	private TreeNode content;
	private FilterDelayJob delayedFilterJob;
	private static final long FILTER_TYPE_DELAY = 300;
	private boolean externalizeDatabaseMessages = true;

	private final IFileAccess workspaceFileAccess = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());

	public I18NExternalizeDialog(Shell parentShell, ServoyProject project)
	{
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		this.project = project;
	}

	private ArrayList<ServoyProject> getI18NProjects()
	{
		if (i18nProjects == null)
		{
			i18nProjects = new ArrayList<ServoyProject>();
			ServoyProject[] allProjects = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
			String projectI18NDatasource = project.getSolution().getI18nDataSource();
			if (projectI18NDatasource != null)
			{
				for (ServoyProject sp : allProjects)
				{
					if (projectI18NDatasource.equals(sp.getSolution().getI18nDataSource())) i18nProjects.add(sp);
				}
			}
		}
		return i18nProjects;
	}

	private void handleFilterChanged()
	{
		// only apply filter when user stops typing for 300 ms
		delayedFilterJob.setFilterText(filterTextField.getText());
	}

	public void loadContent(String filterText)
	{
		if (defaultMessages == null)
		{
			Solution editingSolution = project.getEditingSolution();
			I18NMessagesModel i18nMessagesModel = new I18NMessagesModel(editingSolution != null ? editingSolution.getI18nDataSource() : null,
				ApplicationServerSingleton.get().getClientId(), ServoyModel.getSettings(), ServoyModel.getDataServer(), ServoyModel.getDeveloperRepository());
			i18nMessagesModel.setLanguage(Locale.getDefault());

			defaultMessages = new HashMap<String, String>();
			Collection<I18NMessagesModelEntry> messages = i18nMessagesModel.getMessages(null, null, null);
			Iterator<I18NMessagesModelEntry> messagesIte = messages.iterator();
			I18NMessagesModelEntry message;
			while (messagesIte.hasNext())
			{
				message = messagesIte.next();
				defaultMessages.put(message.key, message.defaultvalue);
			}
		}
		String filter = filterText != null ? filterText.toLowerCase() : filterText;
		content = new TreeNode();

		ArrayList<ServoyProject> allProjects = getI18NProjects();
		for (ServoyProject s : allProjects)
			addSolutionContent(content, s.getEditingSolution(), filter);

		if (externalizeDatabaseMessages) // add column infos
		{
			try
			{
				IServerManagerInternal sm = ServoyModel.getServerManager();
				String[] serverNames = sm.getServerNames(true, true, true, true);
				IServer server;
				for (String serverName : serverNames)
				{
					server = sm.getServer(serverName);
					List<String> tableAndViews = ((IServerInternal)server).getTableAndViewNames(true, true);
					for (String tableName : tableAndViews)
					{
						ITable table = server.getTable(tableName);
						if (table instanceof Table)
						{
							Table tableObj = (Table)table;
							String columnNames[] = tableObj.getColumnNames();
							for (String columnName : columnNames)
							{
								Column column = tableObj.getColumn(columnName);
								addColumnInfoToTree(content, server, tableObj, column, filter);
							}

							for (ServoyProject s : allProjects)
							{
								Iterator<TableNode> tableNodesIte = s.getSolution().getTableNodes(tableObj);
								while (tableNodesIte.hasNext())
								{
									addTableNodeToTree(content, server, tableObj, tableNodesIte.next(), filter);
								}
							}
						}
					}
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}

		if (ignoreMessageButton != null)
		{
			ignoreMessageButton.setSelection(false);
			ignoreMessageButton.setEnabled(false);
		}
	}

	private void addTableNodeToTree(TreeNode root, IServer server, Table table, TableNode tableNode, String filterText)
	{
		ArrayList<JSText> jsTexts = getJSTexts(tableNode);
		TreeNode tableNodeFound = null;
		for (JSText jstxt : jsTexts)
		{
			//check filter
			if (filterText != null && server.toString().toLowerCase().indexOf(filterText) == -1 && table.toString().toLowerCase().indexOf(filterText) == -1 &&
				jstxt.getText().toLowerCase().indexOf(filterText) == -1) continue;

			if (tableNodeFound == null)
			{
				ArrayList<TreeNode> serverNodes = root.getChildren();
				TreeNode serverNodeFound = null;
				for (TreeNode serverNode : serverNodes)
				{
					if (serverNode.getData().equals(server))
					{
						serverNodeFound = serverNode;
					}
				}
				if (serverNodeFound == null)
				{
					serverNodeFound = new TreeNode(root, server);
					root.addChild(serverNodeFound);
				}

				ArrayList<TreeNode> tableNodes = serverNodeFound.getChildren();
				for (TreeNode tblNode : tableNodes)
				{
					if (tblNode.getData().equals(table))
					{
						tableNodeFound = tblNode;
					}
				}
				if (tableNodeFound == null)
				{
					tableNodeFound = new TreeNode(serverNodeFound, table);
					serverNodeFound.addChild(tableNodeFound);
				}
			}

			tableNodeFound.addChild(new TreeNode(tableNodeFound, jstxt));
		}
	}

	private void addColumnInfoToTree(TreeNode root, IServer server, Table table, Column column, String filterText)
	{
		ColumnInfo columnInfo = column.getColumnInfo();
		if (columnInfo != null && (columnInfo.getTitleText() == null || !columnInfo.getTitleText().startsWith("i18n:")))
		{
			//check filter
			if (filterText != null && server.toString().toLowerCase().indexOf(filterText) == -1 && table.toString().toLowerCase().indexOf(filterText) == -1 &&
				column.toString().toLowerCase().indexOf(filterText) == -1) return;


			ArrayList<TreeNode> serverNodes = root.getChildren();
			TreeNode serverNodeFound = null;
			for (TreeNode serverNode : serverNodes)
			{
				if (serverNode.getData().equals(server))
				{
					serverNodeFound = serverNode;
				}
			}
			if (serverNodeFound == null)
			{
				serverNodeFound = new TreeNode(root, server);
				root.addChild(serverNodeFound);
			}

			ArrayList<TreeNode> tableNodes = serverNodeFound.getChildren();
			TreeNode tableNodeFound = null;
			for (TreeNode tableNode : tableNodes)
			{
				if (tableNode.getData().equals(table))
				{
					tableNodeFound = tableNode;
				}
			}
			if (tableNodeFound == null)
			{
				tableNodeFound = new TreeNode(serverNodeFound, table);
				serverNodeFound.addChild(tableNodeFound);
			}

			ArrayList<TreeNode> columnNodes = tableNodeFound.getChildren();
			TreeNode columnNodeFound = null;
			for (TreeNode columnNode : columnNodes)
			{
				if (columnNode.getData().equals(column))
				{
					columnNodeFound = columnNode;
				}
			}
			if (columnNodeFound == null)
			{
				columnNodeFound = new TreeNode(tableNodeFound, column);
				tableNodeFound.addChild(columnNodeFound);
			}

			TreeNode columnInfoNode = new TreeNode(columnNodeFound, columnInfo);
			columnNodeFound.addChild(columnInfoNode);
		}
	}

	public boolean hasContent()
	{
		return content != null && content.getChildren().size() > 0;
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Externalize Strings");
		Composite composite = (Composite)super.createDialogArea(parent);

		Composite treeViewerComposite = new Composite(composite, SWT.NONE);
		treeViewer = new TreeViewer(treeViewerComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK)
		{
			@Override
			protected Item newItem(Widget parent, int flags, int ix)
			{
				Item item = super.newItem(parent, flags, ix);
				((TreeItem)item).setChecked(true);
				return item;
			}
		};
		Tree tree = treeViewer.getTree();
		tree.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event event)
			{
				if (event.detail == SWT.CHECK)
				{
					TreeItem treeItem = (TreeItem)event.item;
					checkTreeItemChildren(treeItem, treeItem.getChecked());
				}
				TreeNode node = (TreeNode)((TreeItem)event.item).getData();
				if (node.isJSText())
				{
					I18NExternalizeDialog.this.ignoreMessageButton.setEnabled(true);
					I18NExternalizeDialog.this.ignoreMessageButton.setSelection(((JSText)node.getData()).isIgnored());
				}
				else
				{
					I18NExternalizeDialog.this.ignoreMessageButton.setSelection(false);
					I18NExternalizeDialog.this.ignoreMessageButton.setEnabled(false);
				}
			}

			private void checkTreeItemChildren(TreeItem treeItem, boolean flag)
			{
				TreeItem[] children = treeItem.getItems();
				for (TreeItem child : children)
				{
					child.setChecked(flag);
					checkTreeItemChildren(child, flag);
				}
			}
		});

		final Menu contextMenu = new Menu(getShell(), SWT.POP_UP);
		MenuItem mExpandAll = new MenuItem(contextMenu, SWT.PUSH);
		MenuItem mCollapseAll = new MenuItem(contextMenu, SWT.PUSH);

		mExpandAll.setText("Expand all");
		mExpandAll.addSelectionListener(new SelectionListener()
		{
			public void widgetDefaultSelected(SelectionEvent e)
			{
			}

			public void widgetSelected(SelectionEvent e)
			{
				TreeSelection treeSelection = (TreeSelection)treeViewer.getSelection();
				treeViewer.expandToLevel(treeSelection.getPaths()[0], AbstractTreeViewer.ALL_LEVELS);
			}

		});
		mCollapseAll.setText("Collapse all");
		mCollapseAll.addSelectionListener(new SelectionListener()
		{
			public void widgetDefaultSelected(SelectionEvent e)
			{
			}

			public void widgetSelected(SelectionEvent e)
			{
				TreeSelection treeSelection = (TreeSelection)treeViewer.getSelection();
				treeViewer.collapseToLevel(treeSelection.getPaths()[0], AbstractTreeViewer.ALL_LEVELS);
			}

		});

		tree.addMenuDetectListener(new MenuDetectListener()
		{
			public void menuDetected(MenuDetectEvent e)
			{
				contextMenu.setVisible(true);
			}
		});

		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		treeContentProvider = new TreeContentProvider();
		treeViewer.setContentProvider(treeContentProvider);
		if (content == null) loadContent(null);
		treeViewer.setInput(content);

		TreeColumn propertyColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, I18NExternalizeDialog.COLUMN_PROPERTY);
		propertyColumn.setText("Property");
		new TreeViewerColumn(treeViewer, propertyColumn);

		TreeColumn textColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, I18NExternalizeDialog.COLUMN_TEXT);
		textColumn.setText("Text");
		new TreeViewerColumn(treeViewer, textColumn);

		TreeColumn keyColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, I18NExternalizeDialog.COLUMN_KEY);
		keyColumn.setText("Key");
		new TreeViewerColumn(treeViewer, keyColumn).setEditingSupport(new EditingSupport(treeViewer)
		{
			private final TextCellEditor editor = new TextCellEditor(treeViewer.getTree());

			@Override
			protected boolean canEdit(Object element)
			{
				return ((TreeNode)element).isElement() || ((TreeNode)element).isColumnInfo() ||
					(((TreeNode)element).isJSText() && !((JSText)((TreeNode)element).getData()).isIgnored());
			}

			@Override
			protected CellEditor getCellEditor(Object element)
			{
				return editor;
			}

			@Override
			protected Object getValue(Object element)
			{
				return ((TreeNode)element).getKey();
			}

			@Override
			protected void setValue(Object element, Object value)
			{
				((TreeNode)element).setKey(value.toString());
				I18NExternalizeDialog.this.treeViewer.update(element, null);
			}
		});

		treeViewer.setLabelProvider(new TreeLabelProvider());

		// layout components
		TreeColumnLayout layout = new TreeColumnLayout();
		treeViewerComposite.setLayout(layout);
		layout.setColumnData(propertyColumn, new ColumnWeightData(200, 200, true));
		layout.setColumnData(textColumn, new ColumnWeightData(200, 200, true));
		layout.setColumnData(keyColumn, new ColumnWeightData(200, 200, true));

		treeViewer.expandAll();

		Label filterLabel = new Label(composite, SWT.RIGHT);
		filterLabel.setText("Filter");

		filterTextField = new Text(composite, SWT.BORDER);
		filterTextField.setText("");
		filterTextField.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				I18NExternalizeDialog.this.handleFilterChanged();
			}
		});
		filterTextField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent event)
			{
				if (event.keyCode == SWT.CR)
				{
					I18NExternalizeDialog.this.handleFilterChanged();
				}
			}
		});
		filterTextField.setFocus();

		databaseMessagesButton = new Button(composite, SWT.CHECK);
		databaseMessagesButton.setText("Show database messages");
		databaseMessagesButton.setSelection(externalizeDatabaseMessages);
		databaseMessagesButton.addSelectionListener(new SelectionListener()
		{

			public void widgetDefaultSelected(SelectionEvent e)
			{
				// ignore
			}

			public void widgetSelected(SelectionEvent e)
			{
				I18NExternalizeDialog.this.externalizeDatabaseMessages = I18NExternalizeDialog.this.databaseMessagesButton.getSelection();
				String filterText = I18NExternalizeDialog.this.filterTextField.getText();
				I18NExternalizeDialog.this.loadContent("".equals(filterText) ? null : filterText);
				I18NExternalizeDialog.this.treeViewer.setInput(I18NExternalizeDialog.this.content);
				I18NExternalizeDialog.this.treeViewer.expandAll();
			}

		});

		ignoreMessageButton = new Button(composite, SWT.CHECK);
		ignoreMessageButton.setText("Ignore selected message");
		ignoreMessageButton.addSelectionListener(new SelectionListener()
		{

			public void widgetDefaultSelected(SelectionEvent e)
			{
				// ignore
			}

			public void widgetSelected(SelectionEvent e)
			{
				TreeItem[] selection = treeViewer.getTree().getSelection();
				if (selection != null && selection.length > 0)
				{
					TreeNode node = (TreeNode)selection[selection.length - 1].getData();
					if (node.isJSText())
					{
						((JSText)node.getData()).setIgnored(I18NExternalizeDialog.this.ignoreMessageButton.getSelection());
						treeViewer.refresh(node);
					}
				}
			}

		});
		ignoreMessageButton.setSelection(false);
		ignoreMessageButton.setEnabled(false);

		GroupLayout i18NLayout = new GroupLayout(composite);
		i18NLayout.setHorizontalGroup(i18NLayout.createParallelGroup(GroupLayout.LEADING).add(
			i18NLayout.createSequentialGroup().addContainerGap().add(
				i18NLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING, treeViewerComposite, GroupLayout.PREFERRED_SIZE, 0,
					Short.MAX_VALUE).add(GroupLayout.LEADING, ignoreMessageButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE).add(GroupLayout.LEADING, databaseMessagesButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE).add(
					i18NLayout.createSequentialGroup().add(filterLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(
						LayoutStyle.RELATED).add(filterTextField, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))).addContainerGap()));
		i18NLayout.setVerticalGroup(i18NLayout.createParallelGroup(GroupLayout.LEADING).add(
			i18NLayout.createSequentialGroup().addContainerGap().add(
				i18NLayout.createParallelGroup(GroupLayout.CENTER, false).add(filterLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE).add(filterTextField, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)).addPreferredGap(LayoutStyle.RELATED).add(
				treeViewerComposite, GroupLayout.PREFERRED_SIZE, 100, Short.MAX_VALUE).addPreferredGap(LayoutStyle.RELATED).add(ignoreMessageButton,
				GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED).add(
				databaseMessagesButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addContainerGap()));

		composite.setLayout(i18NLayout);

		delayedFilterJob = new FilterDelayJob(new FilteredEntity()
		{

			public void filter(final String filterValue)
			{
				if (Display.getCurrent() != null)
				{
					I18NExternalizeDialog.this.loadContent("".equals(filterValue) ? null : filterValue);
					I18NExternalizeDialog.this.treeViewer.setInput(I18NExternalizeDialog.this.content);
					I18NExternalizeDialog.this.treeViewer.expandAll();

				}
				else
				{
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							I18NExternalizeDialog.this.loadContent("".equals(filterValue) ? null : filterValue);
							I18NExternalizeDialog.this.treeViewer.setInput(I18NExternalizeDialog.this.content);
							I18NExternalizeDialog.this.treeViewer.expandAll();
						}
					});
				}
			}

		}, FILTER_TYPE_DELAY, "Filtering");

		return composite;
	}

	private void addSolutionContent(final TreeNode content, Solution solution, final String filterText)
	{
		solution.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				Iterator<Element> elIte = contentSpec.getPropertiesForObjectType(o.getTypeID());
				while (elIte.hasNext())
				{
					Element el = elIte.next();
					String elName = el.getName();

					if ("text".equals(elName) || "titleText".equals(elName) || "toolTipText".equals(elName))
					{
						String property = I18NExternalizeDialog.this.getProperty(o, el);
						if ((property == null || !property.startsWith("i18n:")))
						{
							boolean visible = (filterText == null) || (property != null && property.toLowerCase().indexOf(filterText) != -1);
							ArrayList<IPersist> treePath = new ArrayList<IPersist>();

							IPersist persist = o;
							do
							{
								treePath.add(0, persist);
								if (filterText != null) visible = visible || (getPersistName(persist).toString().toLowerCase().indexOf(filterText) != -1);
							}
							while ((persist = persist.getParent()) != null);

							if (visible) content.addPersistElement(treePath, el);
						}
					}
				}

				if (o.getTypeID() == IRepository.FORMS || o.getTypeID() == IRepository.SOLUTIONS)
				{
					ArrayList<JSText> jsTexts = getJSTexts(o);
					if (jsTexts.size() > 0)
					{
						boolean visible = filterText == null;
						ArrayList<IPersist> treePath = new ArrayList<IPersist>();

						IPersist persist = o;
						do
						{
							treePath.add(0, persist);
							if (filterText != null) visible = visible || (getPersistName(persist).toString().toLowerCase().indexOf(filterText) != -1);
						}
						while ((persist = persist.getParent()) != null);

						for (JSText jsText : jsTexts)
						{
							visible = (filterText == null) || (jsText.getText().toLowerCase().indexOf(filterText) != -1);
							if (visible) content.addPersistElement(treePath, jsText);
						}
					}
				}

				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
	}

	private final HashMap<IPersist, Map<Integer, ArrayList<ASTNode>>> persistToLineNumberForASTNodes = new HashMap<IPersist, Map<Integer, ArrayList<ASTNode>>>();

	private ArrayList<JSText> getJSTexts(final IPersist persist)
	{
		final ArrayList<JSText> jsTexts = new ArrayList<JSText>();
		String jsPath = SolutionSerializer.getScriptPath(persist, false);
		if (jsPath != null && workspaceFileAccess.exists(jsPath))
		{
			try
			{
				String jsContent = workspaceFileAccess.getUTF8Contents(jsPath);
				JavaScriptParser parser = new JavaScriptParser();
				final ArrayList<IProblem> problems = new ArrayList<IProblem>();
				IProblemReporter reporter = new IProblemReporter()
				{

					public Object getAdapter(Class adapter)
					{
						return null;
					}

					public void reportProblem(IProblem problem)
					{
						if (problem.isError())
						{
							problems.add(problem);
						}
					}
				};

				Script script = parser.parse(jsContent, reporter);
				if (problems.size() == 0 && script != null)
				{
					final HashMap<Integer, ASTNode> startIdxNodeMap = new HashMap<Integer, ASTNode>();
					script.traverse(new ASTVisitor()
					{
						@Override
						public boolean visitGeneral(ASTNode node) throws Exception
						{
							if (node instanceof StringLiteral)
							{
								ASTNode parent = ((StringLiteral)node).getParent();
								if (!(parent instanceof CallExpression && ((CallExpression)parent).getExpression().toString().equalsIgnoreCase(
									"i18n.getI18NMessage")))
								{
									startIdxNodeMap.put(Integer.valueOf(node.sourceStart()), node);
								}
							}
							return true;
						}
					});

					List<Comment> comments = script.getComments();
					for (Comment c : comments)
					{
						if (c instanceof SingleLineComment)
						{
							startIdxNodeMap.put(Integer.valueOf(c.sourceStart()), c);
						}
					}

					Map<Integer, ArrayList<ASTNode>> lineNumberForASTNodes = getLineNumbersForASTNodes(jsContent, startIdxNodeMap);
					persistToLineNumberForASTNodes.put(persist, lineNumberForASTNodes);
					Iterator<ArrayList<ASTNode>> astNodesIte = lineNumberForASTNodes.values().iterator();
					ArrayList<ASTNode> astNodes;
					StringLiteral stringLiteral;
					ArrayList<SingleLineComment> lineComments;
					int keyIdx = 0;
					while (astNodesIte.hasNext())
					{
						astNodes = astNodesIte.next();
						lineComments = new ArrayList<SingleLineComment>();
						if (astNodes.get(0) instanceof SingleLineComment)
						{

							lineComments.add((SingleLineComment)astNodes.remove(0));
						}

						for (int y = 0; y < astNodes.size(); y++)
						{
							boolean isIgnored = false;
							for (SingleLineComment lineComment : lineComments)
							{
								isIgnored = isIgnored || (lineComment.getText().indexOf("$NON-NLS-" + (y + 1) + "$") != -1);
								if (isIgnored) break;
							}
							if (isIgnored) continue;
							stringLiteral = (StringLiteral)astNodes.get(y);
							String keyHint = getKeyHint(persist, null) + keyIdx++;
							jsTexts.add(new JSText(persist, stringLiteral.getText(), stringLiteral.sourceStart(), stringLiteral.sourceEnd(), keyHint));
						}

					}
				}

			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}

		return jsTexts;
	}

	private Map<Integer, ArrayList<ASTNode>> getLineNumbersForASTNodes(String jsConent, Map<Integer, ASTNode> astNodesMap)
	{
		Map<Integer, ArrayList<ASTNode>> lineNumbersForASTNodesMap = new HashMap<Integer, ArrayList<ASTNode>>();

		int line = 1;
		for (int i = 0; i < jsConent.length(); i++)
		{
			if (jsConent.charAt(i) == '\r' || (jsConent.charAt(i) == '\n' && (i == 0 || jsConent.charAt(i - 1) != '\r')))
			{
				line++;
			}
			else
			{
				Integer oI = Integer.valueOf(i);
				if (astNodesMap.containsKey(oI))
				{
					Integer oLine = Integer.valueOf(line);
					ArrayList<ASTNode> lineNodes = lineNumbersForASTNodesMap.get(oLine);
					if (lineNodes == null)
					{
						lineNodes = new ArrayList<ASTNode>();
						lineNumbersForASTNodesMap.put(oLine, lineNodes);
					}
					ASTNode node = astNodesMap.get(oI);
					if (node instanceof SingleLineComment) lineNodes.add(0, node);
					else lineNodes.add(node);
				}
			}
		}

		return lineNumbersForASTNodesMap;
	}

	private String getKeyHint(IPersist persist, Element element)
	{

		if (persist instanceof TableNode)
		{
			return ((TableNode)persist).getServerName() + "." + ((TableNode)persist).getTableName() + ".";
		}

		if (element != null)
		{
			String defaultText = getProperty(persist, element);
			if (defaultMessages.containsValue(defaultText))
			{
				Iterator<Map.Entry<String, String>> defMsgIte = defaultMessages.entrySet().iterator();
				Map.Entry<String, String> msgEntry;
				while (defMsgIte.hasNext())
				{
					msgEntry = defMsgIte.next();
					if (msgEntry.getValue().equals(defaultText)) return msgEntry.getKey();
				}
			}
		}

		StringBuffer keyHint = new StringBuffer();
		IPersist p = persist;
		Element pName;
		do
		{
			pName = contentSpec.getPropertyForObjectTypeByName(p.getTypeID(), "name");
			String name = null;
			if (pName != null)
			{
				name = getProperty(p, pName);
			}
			if (name == null || name.length() == 0)
			{
				switch (p.getTypeID())
				{
					case IRepository.SOLUTIONS :
						name = ((Solution)p).getName();
						break;
					case IRepository.GRAPHICALCOMPONENTS :
						name = "label_" + p.getID();
						break;
					case IRepository.TABS :
						name = "tab_" + p.getID();
						break;
					default :
						name = p.toString() + "_" + p.getID();

				}
			}
			keyHint.insert(0, '.');
			keyHint.insert(0, name);
		}
		while ((p = p.getParent()) != null);
		if (element != null) keyHint.append(element.getName());

		return keyHint.toString();
	}

	private String getProperty(IPersist persist, Element element)
	{
		try
		{
			BeanInfo beanInfo = Introspector.getBeanInfo(persist.getClass());
			PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor pd : propertyDescriptors)
			{
				String name = pd.getName();
				if (name.equals(element.getName()))
				{
					Object value = pd.getReadMethod().invoke(persist, (Object[])null);
					if (value != null) return value.toString();
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}

		return null;
	}

	private void setProperty(IPersist persist, Element element, String value)
	{
		try
		{
			BeanInfo beanInfo = Introspector.getBeanInfo(persist.getClass());
			PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor pd : propertyDescriptors)
			{
				String name = pd.getName();
				if (name.equals(element.getName()))
				{
					pd.getWriteMethod().invoke(persist, new Object[] { value });
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	private void writeMessages() throws RepositoryException
	{
		final Solution editingSolution = project.getEditingSolution();
		final TreeMap<String, I18NUtil.MessageEntry> projectMessages = EclipseMessages.readMessages(editingSolution.getI18nServerName(),
			editingSolution.getI18nTableName(), workspaceFileAccess);

		ArrayList<TreeNode> selectedNodes = new ArrayList<TreeNode>();
		fillSelectedNodes(treeViewer.getTree().getItems(), selectedNodes);

		final HashMap<Solution, ArrayList<IPersist>> solutionChangedPersistsMap = new HashMap<Solution, ArrayList<IPersist>>();
		final ArrayList<Table> changedTables = new ArrayList<Table>();
		final HashMap<IPersist, TreeMap<Integer, JSText>> persistJSTextMap = new HashMap<IPersist, TreeMap<Integer, JSText>>();
		for (TreeNode selNode : selectedNodes)
		{
			if (selNode.isElement())
			{
				IPersist p = (IPersist)selNode.getParent().getData();
				Element element = (Element)selNode.getData();
				setProperty(p, element, "i18n:" + selNode.getKey());
				Solution parentSolution = (Solution)p.getAncestor(IRepository.SOLUTIONS);
				if (parentSolution != null)
				{
					ArrayList<IPersist> solutionChangedPersists = solutionChangedPersistsMap.get(parentSolution);
					if (solutionChangedPersists == null)
					{
						solutionChangedPersists = new ArrayList<IPersist>();
						solutionChangedPersistsMap.put(parentSolution, solutionChangedPersists);
					}
					solutionChangedPersists.add(p);
				}
			}
			else if (selNode.isColumnInfo())
			{
				ColumnInfo ci = (ColumnInfo)selNode.getData();
				ci.setTitleText("i18n:" + selNode.getKey());
				ci.flagChanged();
				changedTables.add((Table)selNode.getParent().getParent().getData());
			}
			else if (selNode.isJSText())
			{
				JSText jstxt = (JSText)selNode.getData();
				TreeMap<Integer, JSText> persistJSTexts = persistJSTextMap.get(jstxt.getParent());
				if (persistJSTexts == null)
				{
					persistJSTexts = new TreeMap<Integer, JSText>();
					persistJSTextMap.put(jstxt.getParent(), persistJSTexts);
				}
				jstxt.keyHint = selNode.getKey();
				persistJSTexts.put(Integer.valueOf(jstxt.getStartPosition()), jstxt);
			}

			if (!defaultMessages.containsKey(selNode.getKey()))
			{
				I18NUtil.MessageEntry messageEntry = new I18NUtil.MessageEntry(null, selNode.getKey(), selNode.text);
				projectMessages.put(messageEntry.getLanguageKey(), messageEntry);
			}
		}

		WorkspaceJob externalizeJob = new WorkspaceJob("Externalizing texts ...")
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				try
				{
					EclipseMessages.writeMessages(editingSolution.getI18nServerName(), editingSolution.getI18nTableName(), projectMessages, workspaceFileAccess);

					// write js i18n keys 
					Iterator<Map.Entry<IPersist, TreeMap<Integer, JSText>>> persistJSTextMapIte = persistJSTextMap.entrySet().iterator();
					Map.Entry<IPersist, TreeMap<Integer, JSText>> persistJSTextMapEntry;
					while (persistJSTextMapIte.hasNext())
					{
						persistJSTextMapEntry = persistJSTextMapIte.next();
						String jsPath = SolutionSerializer.getScriptPath(persistJSTextMapEntry.getKey(), false);
						if (jsPath != null && workspaceFileAccess.exists(jsPath))
						{
							TreeMap<Integer, JSText> persistJSTexts = persistJSTextMapEntry.getValue();
							ArrayList<Integer> persistJSTextStartPos = new ArrayList<Integer>(persistJSTexts.keySet());
							try
							{
								String persistJSContent = workspaceFileAccess.getUTF8Contents(jsPath);
								StringBuffer replacedPersistJSContent = new StringBuffer();
								ArrayList<Integer> lineIdxOfIgnoredString = new ArrayList<Integer>();
								int line = 1;
								StringBuffer ignoreLineComment;
								for (int i = 0; i < persistJSContent.length();)
								{
									if (persistJSTextStartPos.size() > 0 && i == persistJSTextStartPos.get(0).intValue())
									{
										JSText jstxt = persistJSTexts.get(persistJSTextStartPos.get(0));
										if (jstxt.isIgnored())
										{
											Map<Integer, ArrayList<ASTNode>> linesNodes = persistToLineNumberForASTNodes.get(persistJSTextMapEntry.getKey());
											if (linesNodes != null)
											{
												ArrayList<ASTNode> lineNodes = linesNodes.get(Integer.valueOf(line));
												if (lineNodes != null)
												{
													for (int j = 0; j < lineNodes.size(); j++)
													{
														if (lineNodes.get(j) instanceof StringLiteral &&
															((StringLiteral)lineNodes.get(j)).getText().equals(jstxt.getText()))
														{
															lineIdxOfIgnoredString.add(Integer.valueOf(j + 1));
															break;
														}
													}
												}
											}
										}
										else
										{
											replacedPersistJSContent.append("i18n.getI18NMessage('").append(jstxt.getKeyHint()).append("')");
											i = jstxt.getEndPosition();
										}
										persistJSTextStartPos.remove(0);
									}
									else
									{
										char nextChar = persistJSContent.charAt(i);
										if (nextChar == '\r' || (nextChar == '\n' && (i == 0 || persistJSContent.charAt(i - 1) != '\r')))
										{
											ignoreLineComment = new StringBuffer();
											for (Integer ignoredIdx : lineIdxOfIgnoredString)
											{
												ignoreLineComment.append(" //$NON-NLS-").append(ignoredIdx).append("$");
											}
											if (ignoreLineComment.length() > 0)
											{
												replacedPersistJSContent.append(ignoreLineComment);
											}
											lineIdxOfIgnoredString.clear();
											line++;
										}
										replacedPersistJSContent.append(nextChar);
										i++;
									}
								}

								workspaceFileAccess.setUTF8Contents(jsPath, replacedPersistJSContent.toString());
							}
							catch (IOException ex)
							{
								ServoyLog.logError(ex);
							}
						}
					}

					// write elements i18n keys
					Iterator<Map.Entry<Solution, ArrayList<IPersist>>> changedSolutions = solutionChangedPersistsMap.entrySet().iterator();
					Map.Entry<Solution, ArrayList<IPersist>> solutionChangedPersistsMapEntry;
					while (changedSolutions.hasNext())
					{
						solutionChangedPersistsMapEntry = changedSolutions.next();
						for (ServoyProject sp : I18NExternalizeDialog.this.getI18NProjects())
						{
							if (sp.getSolution().getID() == solutionChangedPersistsMapEntry.getKey().getID())
							{
								ArrayList<IPersist> changedPersists = solutionChangedPersistsMapEntry.getValue();
								sp.saveEditingSolutionNodes(changedPersists.toArray(new IPersist[changedPersists.size()]), false);
								break;
							}
						}
					}


					IServerManagerInternal serverManager = ServoyModel.getServerManager();
					for (Table table : changedTables)
					{
						((IServerInternal)serverManager.getServer(table.getServerName())).updateAllColumnInfo(table);
					}
				}
				catch (RepositoryException ex)
				{
					ServoyLog.logError(ex);
				}
				return Status.OK_STATUS;
			}
		};
		externalizeJob.setUser(true);
		ISchedulingRule rule = ServoyModel.getWorkspace().getRoot();
		externalizeJob.setRule(rule);
		externalizeJob.schedule();
	}

	private void fillSelectedNodes(TreeItem[] items, ArrayList<TreeNode> selectedNodes)
	{
		if (items != null && items.length > 0)
		{
			TreeNode node;
			for (TreeItem treeItem : items)
			{
				node = (TreeNode)treeItem.getData();
				if (!node.isElement() && !node.isColumnInfo() && !node.isJSText()) fillSelectedNodes(treeItem.getItems(), selectedNodes);
				else if (treeItem.getChecked()) selectedNodes.add(node);
			}
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		createButton(parent, IDialogConstants.OK_ID, "Externalize", false);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}

	@Override
	protected void okPressed()
	{
		try
		{
			writeMessages();
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		super.okPressed();
	}

	private class JSText
	{
		IPersist parent;
		String text;
		int startPosition;
		int endPosition;
		String keyHint;
		boolean ignored;

		JSText(IPersist parent, String text, int startPosition, int endPosition, String keyHint)
		{
			this.parent = parent;
			this.text = text;
			this.startPosition = startPosition;
			this.endPosition = endPosition;
			this.keyHint = keyHint;
		}

		IPersist getParent()
		{
			return parent;
		}

		String getText()
		{
			return text;
		}

		int getStartPosition()
		{
			return startPosition;
		}

		int getEndPosition()
		{
			return endPosition;
		}

		String getKeyHint()
		{
			return keyHint;
		}

		void setIgnored(boolean ignored)
		{
			this.ignored = ignored;
		}

		boolean isIgnored()
		{
			return ignored;
		}
	}

	private class TreeContentProvider implements ITreeContentProvider
	{

		public Object[] getChildren(Object parentElement)
		{
			return ((TreeNode)parentElement).getChildren().toArray();
		}

		public Object getParent(Object element)
		{
			return ((TreeNode)element).getParent();
		}

		public boolean hasChildren(Object element)
		{
			return ((TreeNode)element).getChildren().size() > 0;
		}

		public Object[] getElements(Object inputElement)
		{
			return ((TreeNode)inputElement).getChildren().toArray();
		}

		public void dispose()
		{
			// TODO Auto-generated method stub

		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
		{
			// TODO Auto-generated method stub
		}
	}

	private class TreeLabelProvider extends LabelProvider implements ITableLabelProvider
	{
		public String getColumnText(Object element, int columnIndex)
		{
			TreeNode treeNode = (TreeNode)element;
			switch (columnIndex)
			{
				case I18NExternalizeDialog.COLUMN_PROPERTY :
					return treeNode.toString();
				case I18NExternalizeDialog.COLUMN_TEXT :
					return treeNode.getText();
				case I18NExternalizeDialog.COLUMN_KEY :
					return treeNode.isJSText() && ((JSText)treeNode.getData()).isIgnored() ? "<<IGNORED>>" : treeNode.getKey();
			}

			return "";
		}

		public Image getColumnImage(Object element, int columnIndex)
		{
			if (columnIndex == I18NExternalizeDialog.COLUMN_PROPERTY)
			{
				TreeNode treeNode = (TreeNode)element;
				if (!treeNode.isElement())
				{
					Object data = treeNode.getData();
					if (data instanceof IPersist)
					{
						switch (((IPersist)data).getTypeID())
						{
							case IRepository.SOLUTIONS :
								return I18NExternalizeDialog.this.solutionImage;
							case IRepository.FORMS :
								return I18NExternalizeDialog.this.formImage;
							default :
								return I18NExternalizeDialog.this.elementImage;
						}
					}
					else if (data instanceof IServer)
					{
						return I18NExternalizeDialog.this.serverImage;
					}
					else if (data instanceof ITable)
					{
						return I18NExternalizeDialog.this.tableImage;
					}
				}
			}
			return null;
		}
	}

	private class TreeNode
	{
		private final ArrayList<TreeNode> children = new ArrayList<TreeNode>();
		private TreeNode parent;
		private Object itemData;

		private String text;
		private String key;

		TreeNode()
		{
		}

		TreeNode(TreeNode parent, Object itemData)
		{
			this.parent = parent;
			this.itemData = itemData;
		}


		void addPersistElement(List<IPersist> treePath, Object el)
		{
			if (treePath.size() == 0)
			{
				addChild(new TreeNode(this, el));
			}
			else
			{
				IPersist treePathHead = treePath.get(0);
				for (TreeNode child : children)
				{
					if (child.getData().equals(treePathHead))
					{
						child.addPersistElement(treePath.subList(1, treePath.size()), el);
						return;
					}
				}

				TreeNode child = new TreeNode(this, treePathHead);
				addChild(child);
				child.addPersistElement(treePath.subList(1, treePath.size()), el);
			}
		}

		void addChild(TreeNode treeNode)
		{
			children.add(treeNode);
		}

		Object getData()
		{
			return itemData;
		}

		TreeNode getParent()
		{
			return parent;
		}

		ArrayList<TreeNode> getChildren()
		{
			return children;
		}


		boolean isElement()
		{
			return itemData instanceof Element;
		}

		boolean isColumnInfo()
		{
			return itemData instanceof ColumnInfo;
		}

		boolean isPersist()
		{
			return itemData instanceof IPersist;
		}

		boolean isJSText()
		{
			return itemData instanceof JSText;
		}

		String getText()
		{
			if (text == null)
			{
				if (isElement())
				{
					IPersist persistParent = (IPersist)parent.getData();
					text = I18NExternalizeDialog.this.getProperty(persistParent, (Element)getData());
					if (text == null) text = "";

				}
				else if (isColumnInfo())
				{
					text = ((Column)parent.getData()).getName();
					text = Utils.stringInitCap(Utils.stringReplace(text, "_", " "));

				}
				else if (isJSText())
				{
					text = ((JSText)itemData).getText();
					text = text.substring(1, text.length() - 1);
				}
				else text = "";
			}

			return text;
		}

		String getKey()
		{
			if (key == null)
			{
				if (isElement())
				{
					IPersist persistParent = (IPersist)parent.getData();
					key = I18NExternalizeDialog.this.getKeyHint(persistParent, (Element)getData());

				}
				else if (isColumnInfo())
				{
					String server = parent.getParent().getParent().toString();
					String table = parent.getParent().toString();
					String column = parent.toString();
					key = new StringBuffer(server).append(".").append(table).append(".").append(column).toString();
				}
				else if (isJSText())
				{
					key = ((JSText)itemData).getKeyHint();
				}
				else key = "";
			}

			return key;
		}

		void setKey(String key)
		{
			this.key = key;
		}

		@Override
		public String toString()
		{
			if (isElement())
			{
				return ((Element)itemData).getName();
			}
			else if (isColumnInfo())
			{
				return "title";
			}
			else if (isPersist())
			{
				return getPersistName((IPersist)itemData);
			}
			else if (isJSText())
			{
				return "";
			}

			return itemData.toString();
		}
	}

	private String getPersistName(IPersist persist)
	{
		Element pName = contentSpec.getPropertyForObjectTypeByName(persist.getTypeID(), "name");
		if (pName != null)
		{
			String name = I18NExternalizeDialog.this.getProperty(persist, pName);
			if (name == null || name.length() == 0)
			{
				switch (persist.getTypeID())
				{
					case IRepository.SOLUTIONS :
						return ((Solution)persist).getName();
					case IRepository.GRAPHICALCOMPONENTS :
						return "label";
					case IRepository.TABS :
						return "tab";
				}
			}
		}

		return persist.toString();
	}

	@Override
	protected Point getInitialSize()
	{
		return new Point(800, 600);
	}

	@Override
	public boolean close()
	{
		if (delayedFilterJob != null)
		{
			delayedFilterJob.cancel();
			delayedFilterJob = null;
		}
		return super.close();
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings()
	{
		return EditorUtil.getDialogSettings("i18n_externalize_dialog");
	}
}
