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
import java.util.Map.Entry;
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
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.JSNode;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.SingleLineComment;
import org.eclipse.dltk.javascript.ast.StringLiteral;
import org.eclipse.dltk.javascript.ast.VariableDeclaration;
import org.eclipse.dltk.javascript.parser.JavaScriptParserUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
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
import org.eclipse.swt.events.SelectionAdapter;
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
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.builder.StringLiteralVisitor;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.util.FilterDelayJob;
import com.servoy.eclipse.ui.util.FilteredEntity;
import com.servoy.j2db.i18n.I18NMessagesModel;
import com.servoy.j2db.i18n.I18NMessagesModel.I18NMessagesModelEntry;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.I18NUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

public class I18NExternalizeDialog extends HelpDialog
{
	private static final String PREFERENCE_KEY_SHOW_DB_MESSAGES = "com.servoy.eclipse.ui.dialogs.I18NExternalizeDialog.show_db_messages";
	private static final String PREFERENCE_KEY_SHOW_EXTERNALIZED = "com.servoy.eclipse.ui.dialogs.I18NExternalizeDialog.show_externalized_messages";
	private static final String PREFERENCE_KEY_SHOW_IGNORED = "com.servoy.eclipse.ui.dialogs.I18NExternalizeDialog.show_ignored_messages";
	private static final String PREFERENCE_KEY_SHOW_EMPTY = "com.servoy.eclipse.ui.dialogs.I18NExternalizeDialog.show_empty_messages";
	private static final String PREFERENCE_KEY_SHOW_ONLY_EDITED = "com.servoy.eclipse.ui.dialogs.I18NExternalizeDialog.show_only_edited_messages";

	private static final int COLUMN_PROPERTY = 0;
	private static final int COLUMN_TEXT = 1;
	private static final int COLUMN_KEY = 2;
	private static final int COLUMN_STATUS = 3;

	private static enum STATE
	{
		EXTERNALIZE, INTERNALIZE, IGNORE
	}

	private static final String STATE_TXT_EXTERNALIZE = "Extern";
	private static final String STATE_TXT_INTERNALIZE = "Intern";
	private static final String STATE_TXT_IGNORE = "Ignore";

	private TreeViewer treeViewer;
	private TreeContentProvider treeContentProvider;
	private Text filterTextField;
	private Text commonPrefix;
	private Button showDatabaseMessagesButton;
	private Button showExternalizedMessagesButton;
	private Button showIgnoredMessagesButton;
	private Button showEmptyMessagesButton;
	private Button showOnlyEditedMessagesButton;

	private final ContentSpec contentSpec = StaticContentSpecLoader.getContentSpec();
	private final Image solutionImage = Activator.getDefault().loadImageFromBundle("solution.png");
	private final Image elementImage = Activator.getDefault().loadImageFromBundle("element.png");
	private final Image serverImage = Activator.getDefault().loadImageFromBundle("server.png");
	private final Image tableImage = Activator.getDefault().loadImageFromBundle("portal.png");
	private final Image jsImage = Activator.getDefault().loadImageFromBundle("js.png");
	private final ServoyProject project;
	private ArrayList<ServoyProject> i18nProjects;
	private HashMap<String, String> defaultMessages;
	private TreeNode content;
	private FilterDelayJob delayedFilterJob;
	private static final long FILTER_TYPE_DELAY = 300;
	private boolean isShowDatabaseMsg;
	private boolean isShowExternalizedMsg;
	private boolean isShowIgnoredMsg;
	private boolean isShowEmptyMsg;
	private boolean isShowOnlyEditedMsg;
	private String commonPrefixValue = "";

	private final IFileAccess workspaceFileAccess = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());

	public I18NExternalizeDialog(Shell parentShell, ServoyProject project)
	{
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		this.project = project;
		PlatformUI.getPreferenceStore().setDefault(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_DB_MESSAGES, true);
		PlatformUI.getPreferenceStore().setDefault(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_EXTERNALIZED, true);
		PlatformUI.getPreferenceStore().setDefault(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_IGNORED, true);
		PlatformUI.getPreferenceStore().setDefault(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_EMPTY, false);
		PlatformUI.getPreferenceStore().setDefault(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_ONLY_EDITED, false);

		isShowDatabaseMsg = PlatformUI.getPreferenceStore().getBoolean(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_DB_MESSAGES);
		isShowExternalizedMsg = PlatformUI.getPreferenceStore().getBoolean(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_EXTERNALIZED);
		isShowIgnoredMsg = PlatformUI.getPreferenceStore().getBoolean(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_IGNORED);
		isShowEmptyMsg = PlatformUI.getPreferenceStore().getBoolean(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_EMPTY);
		isShowOnlyEditedMsg = PlatformUI.getPreferenceStore().getBoolean(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_ONLY_EDITED);
	}

	@Override
	public String getHelpID()
	{
		return "com.servoy.eclipse.ui.i18nexternalize";
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

	private void updateDataMessagesNodes(ArrayList<ServoyProject> allProjects, String filter)
	{
		ArrayList<TreeNode> columnInfoNodes = new ArrayList<TreeNode>();

		final ArrayList<String> solutionServers = new ArrayList<String>();
		for (ServoyProject servoyProject : allProjects)
		{
			servoyProject.getSolution().acceptVisitor(new IPersistVisitor()
			{
				public Object visit(IPersist o)
				{
					if (o instanceof Form)
					{
						Form form = (Form)o;
						String dataSource = form.getDataSource();
						if (dataSource != null)
						{
							String serverName = DataSourceUtils.getDBServernameTablename(dataSource)[0];
							if (solutionServers.indexOf(serverName) == -1) solutionServers.add(serverName);
						}
						return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
					else if (o instanceof Relation)
					{
						Relation relation = (Relation)o;
						String serverLeft = relation.getPrimaryServerName();
						if (serverLeft != null && solutionServers.indexOf(serverLeft) == -1) solutionServers.add(serverLeft);
						String serverRight = relation.getForeignServerName();
						if (serverRight != null && solutionServers.indexOf(serverRight) == -1) solutionServers.add(serverRight);
						return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
					return CONTINUE_TRAVERSAL;
				}
			});
		}

		try
		{
			IServerManagerInternal sm = ApplicationServerRegistry.get().getServerManager();

			IServer server;
			for (String serverName : solutionServers)
			{
				server = sm.getServer(serverName, true, true);
				if (server == null) continue;
				List<String> tableAndViews = ((IServerInternal)server).getTableAndViewNames(true, true);
				for (String tableName : tableAndViews)
				{
					ITable table = server.getTable(tableName);
					if (table instanceof Table)
					{
						Table tableObj = (Table)table;
						String columnNames[] = tableObj.getColumnNames();
						TreeNode columnInfoNode;
						for (String columnName : columnNames)
						{
							Column column = tableObj.getColumn(columnName);
							columnInfoNode = addColumnInfoToTree(content, server, tableObj, column, filter);
							if (columnInfoNode != null) columnInfoNodes.add(columnInfoNode);
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

		ArrayList<TreeNode> serverNodes = new ArrayList<TreeNode>();
		for (TreeNode tn : content.getChildren())
		{
			if (tn.getData() instanceof IServer) serverNodes.add(tn);
		}

		if (treeViewer != null)
		{
			treeViewer.refresh(content);
			for (TreeNode cin : columnInfoNodes)
				treeViewer.expandToLevel(cin, AbstractTreeViewer.ALL_LEVELS);
		}
	}

	private boolean loadingContent;

	public void loadContent(String filterText)
	{
		loadingContent = true;
		if (defaultMessages == null)
		{
			Solution editingSolution = project.getEditingSolution();
			I18NMessagesModel i18nMessagesModel = new I18NMessagesModel(editingSolution != null ? editingSolution.getI18nDataSource() : null,
				ApplicationServerRegistry.get().getClientId(), Settings.getInstance(), ApplicationServerRegistry.get().getDataServer(),
				ApplicationServerRegistry.get().getDeveloperRepository());
			i18nMessagesModel.setLanguage(Locale.getDefault());

			defaultMessages = new HashMap<String, String>();
			Collection<I18NMessagesModelEntry> messages = i18nMessagesModel.getMessages(null, null, null, null,
				ServoyModelManager.getServoyModelManager().getServoyModel().isActiveSolutionMobile(), null);
			I18NMessagesModelEntry message;
			for (I18NMessagesModelEntry message2 : messages)
			{
				message = message2;
				defaultMessages.put(message.key, message.defaultvalue);
			}
		}
		String filter = filterText != null ? filterText.toLowerCase() : filterText;
		content = new TreeNode();

		ArrayList<ServoyProject> allProjects = getI18NProjects();
		for (ServoyProject s : allProjects)
			addSolutionContent(content, s.getEditingSolution(), filter);

		updateDataMessagesNodes(allProjects, filter);

		if (commonPrefix != null) commonPrefix.setText(commonPrefixValue);
		loadingContent = false;
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
					serverNodeFound = new TreeNode(root, server, null);
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
					tableNodeFound = new TreeNode(serverNodeFound, table, null);
					serverNodeFound.addChild(tableNodeFound);
				}
			}

			tableNodeFound.addChild(new TreeNode(tableNodeFound, jstxt, jstxt.getState()));
		}
	}

	private TreeNode addColumnInfoToTree(TreeNode root, IServer server, Table table, Column column, String filterText)
	{
		ColumnInfo columnInfo = column.getColumnInfo();
		if (columnInfo != null)
		{
			String columnInfoTitleText = columnInfo.getTitleText();
			if (columnInfoTitleText == null) columnInfoTitleText = "";
			//check filter
			if (filterText != null && server.toString().toLowerCase().indexOf(filterText) == -1 && table.toString().toLowerCase().indexOf(filterText) == -1 &&
				column.toString().toLowerCase().indexOf(filterText) == -1) return null;


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
				serverNodeFound = new TreeNode(root, server, null);
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
				tableNodeFound = new TreeNode(serverNodeFound, table, null);
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
				columnNodeFound = new TreeNode(tableNodeFound, column, null);
				tableNodeFound.addChild(columnNodeFound);
			}

			TreeNode columnInfoNode = new TreeNode(columnNodeFound, columnInfo,
				columnInfoTitleText.startsWith("i18n:") ? STATE.EXTERNALIZE : STATE.INTERNALIZE);
			columnNodeFound.addChild(columnInfoNode);

			return columnInfoNode;
		}

		return null;
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
			protected void createTreeItem(Widget parentWidget, Object element, int index)
			{
				Item item = newItem(parentWidget, SWT.NULL, index);

				boolean isChecked = true;
				if (element instanceof TreeNode) isChecked = ((TreeNode)element).isChecked();
				((TreeItem)item).setChecked(isChecked);

				updateItem(item, element);
				updatePlus(item, element);
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
			}

			private void checkTreeItemChildren(TreeItem treeItem, boolean flag)
			{
				Object data = treeItem.getData();
				if (data instanceof TreeNode) ((TreeNode)data).setChecked(flag);

				TreeItem[] children = treeItem.getItems();
				for (TreeItem child : children)
				{
					child.setChecked(flag);
					Object childData = child.getData();
					if (childData instanceof TreeNode) ((TreeNode)childData).setChecked(flag);
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
		new TreeViewerColumn(treeViewer, textColumn).setEditingSupport(new EditingSupport(treeViewer)
		{
			private final TextCellEditor editor = new TextCellEditor(treeViewer.getTree());

			@Override
			protected CellEditor getCellEditor(Object element)
			{
				return editor;
			}

			@Override
			protected boolean canEdit(Object element)
			{
				return ((TreeNode)element).getState() == STATE.INTERNALIZE || ((TreeNode)element).getState() == STATE.IGNORE;
			}

			@Override
			protected Object getValue(Object element)
			{
				return ((TreeNode)element).getText();
			}

			@Override
			protected void setValue(Object element, Object value)
			{
				((TreeNode)element).setText(value.toString());
				I18NExternalizeDialog.this.treeViewer.update(element, null);
			}

		});

		TreeColumn keyColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, I18NExternalizeDialog.COLUMN_KEY);
		keyColumn.setText("Key");
		new TreeViewerColumn(treeViewer, keyColumn).setEditingSupport(new EditingSupport(treeViewer)
		{
			private final TextCellEditor editor = new TextCellEditor(treeViewer.getTree())
			{
				/**
				 * State information for updating action enablement
				 */
				private boolean isSelection = false;

				private boolean isDeleteable = false;

				private boolean isSelectable = false;

				@Override
				protected void doSetFocus()
				{
					if (text != null)
					{
						if (commonPrefixValue.length() > 0 && text.getText().startsWith(commonPrefixValue))
						{
							text.setSelection(commonPrefixValue.length(), text.getText().length());
						}
						else
						{
							text.selectAll();
						}
						text.setFocus();
						checkSelection();
						checkDeleteable();
						checkSelectable();
					}
				}

				/**
				 * Checks to see if the "deletable" state (can delete/
				 * nothing to delete) has changed and if so fire an
				 * enablement changed notification.
				 */
				private void checkDeleteable()
				{
					boolean oldIsDeleteable = isDeleteable;
					isDeleteable = isDeleteEnabled();
					if (oldIsDeleteable != isDeleteable)
					{
						fireEnablementChanged(DELETE);
					}
				}

				/**
				 * Checks to see if the "selectable" state (can select)
				 * has changed and if so fire an enablement changed notification.
				 */
				private void checkSelectable()
				{
					boolean oldIsSelectable = isSelectable;
					isSelectable = isSelectAllEnabled();
					if (oldIsSelectable != isSelectable)
					{
						fireEnablementChanged(SELECT_ALL);
					}
				}

				/**
				 * Checks to see if the selection state (selection /
				 * no selection) has changed and if so fire an
				 * enablement changed notification.
				 */
				private void checkSelection()
				{
					boolean oldIsSelection = isSelection;
					isSelection = text.getSelectionCount() > 0;
					if (oldIsSelection != isSelection)
					{
						fireEnablementChanged(COPY);
						fireEnablementChanged(CUT);
					}
				}
			};

			@Override
			protected boolean canEdit(Object element)
			{
				return ((TreeNode)element).getState() == STATE.EXTERNALIZE;
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

		TreeColumn statusColumn = new TreeColumn(treeViewer.getTree(), SWT.CENTER, I18NExternalizeDialog.COLUMN_STATUS);
		statusColumn.setText("Operation");
		new TreeViewerColumn(treeViewer, statusColumn).setEditingSupport(new EditingSupport(treeViewer)
		{
			private final StateCellEditor defaultEditor = new StateCellEditor(treeViewer.getTree(), new STATE[] { STATE.EXTERNALIZE, STATE.INTERNALIZE });
			private final StateCellEditor jsEditor = new StateCellEditor(treeViewer.getTree(), STATE.values());

			@Override
			protected CellEditor getCellEditor(Object element)
			{
				TreeNode tn = (TreeNode)element;

				return tn.isJSText() ? jsEditor : defaultEditor;
			}

			@Override
			protected boolean canEdit(Object element)
			{
				TreeNode tn = (TreeNode)element;
				return (tn.isElement() || tn.isColumnInfo() || tn.isJSText());
			}

			@Override
			protected Object getValue(Object element)
			{
				return ((TreeNode)element).getState();
			}

			@Override
			protected void setValue(Object element, Object value)
			{
				((TreeNode)element).setState((STATE)value);
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
		layout.setColumnData(statusColumn, new ColumnWeightData(80, 80, true));

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

		Label commonPrefixLabel = new Label(composite, SWT.RIGHT);
		commonPrefixLabel.setText("Enter common prefix for generated keys");

		commonPrefix = new Text(composite, SWT.BORDER);
		commonPrefix.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent event)
			{
				if (event.keyCode == SWT.CR)
				{
					applyCommonPrefix();
				}
			}
		});

		Button commonPrefixApplyButton = new Button(composite, SWT.PUSH);
		commonPrefixApplyButton.setText("Apply");
		commonPrefixApplyButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				applyCommonPrefix();
			}
		});


		SelectionListener showSelectionListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				Object eventSource = e.getSource();
				if (eventSource == showDatabaseMessagesButton)
				{
					I18NExternalizeDialog.this.isShowDatabaseMsg = I18NExternalizeDialog.this.showDatabaseMessagesButton.getSelection();
					PlatformUI.getPreferenceStore().setValue(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_DB_MESSAGES,
						I18NExternalizeDialog.this.isShowDatabaseMsg);
				}
				else if (eventSource == showExternalizedMessagesButton)
				{
					I18NExternalizeDialog.this.isShowExternalizedMsg = I18NExternalizeDialog.this.showExternalizedMessagesButton.getSelection();
					PlatformUI.getPreferenceStore().setValue(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_EXTERNALIZED,
						I18NExternalizeDialog.this.isShowExternalizedMsg);
				}
				else if (eventSource == showIgnoredMessagesButton)
				{
					I18NExternalizeDialog.this.isShowIgnoredMsg = I18NExternalizeDialog.this.showIgnoredMessagesButton.getSelection();
					PlatformUI.getPreferenceStore().setValue(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_IGNORED, I18NExternalizeDialog.this.isShowIgnoredMsg);
				}
				else if (eventSource == showEmptyMessagesButton)
				{
					I18NExternalizeDialog.this.isShowEmptyMsg = I18NExternalizeDialog.this.showEmptyMessagesButton.getSelection();
					PlatformUI.getPreferenceStore().setValue(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_EMPTY, I18NExternalizeDialog.this.isShowEmptyMsg);
				}
				else if (eventSource == showOnlyEditedMessagesButton)
				{
					I18NExternalizeDialog.this.isShowOnlyEditedMsg = I18NExternalizeDialog.this.showOnlyEditedMessagesButton.getSelection();
					PlatformUI.getPreferenceStore().setValue(I18NExternalizeDialog.PREFERENCE_KEY_SHOW_ONLY_EDITED,
						I18NExternalizeDialog.this.isShowOnlyEditedMsg);
				}

				treeViewer.refresh();
				treeViewer.expandAll();
			}
		};

		showDatabaseMessagesButton = new Button(composite, SWT.CHECK);
		showDatabaseMessagesButton.setText("Show database messages");
		showDatabaseMessagesButton.setSelection(isShowDatabaseMsg);
		showDatabaseMessagesButton.addSelectionListener(showSelectionListener);

		showExternalizedMessagesButton = new Button(composite, SWT.CHECK);
		showExternalizedMessagesButton.setText("Show externalized messages");
		showExternalizedMessagesButton.setSelection(isShowExternalizedMsg);
		showExternalizedMessagesButton.addSelectionListener(showSelectionListener);

		showIgnoredMessagesButton = new Button(composite, SWT.CHECK);
		showIgnoredMessagesButton.setText("Show ignored messages");
		showIgnoredMessagesButton.setSelection(isShowIgnoredMsg);
		showIgnoredMessagesButton.addSelectionListener(showSelectionListener);

		showEmptyMessagesButton = new Button(composite, SWT.CHECK);
		showEmptyMessagesButton.setText("Show messages with empty text");
		showEmptyMessagesButton.setSelection(isShowEmptyMsg);
		showEmptyMessagesButton.addSelectionListener(showSelectionListener);

		showOnlyEditedMessagesButton = new Button(composite, SWT.CHECK);
		showOnlyEditedMessagesButton.setText("Show only edited messages");
		showOnlyEditedMessagesButton.setSelection(isShowOnlyEditedMsg);
		showOnlyEditedMessagesButton.addSelectionListener(showSelectionListener);

		GroupLayout i18NLayout = new GroupLayout(composite);
		i18NLayout.setHorizontalGroup(i18NLayout.createParallelGroup(GroupLayout.LEADING).add(i18NLayout.createSequentialGroup().addContainerGap().add(
			i18NLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING, treeViewerComposite, GroupLayout.PREFERRED_SIZE, 0,
				Short.MAX_VALUE).add(GroupLayout.LEADING, showDatabaseMessagesButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE)
				.add(GroupLayout.LEADING, showExternalizedMessagesButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE)
				.add(GroupLayout.LEADING, showIgnoredMessagesButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE)
				.add(GroupLayout.LEADING, showEmptyMessagesButton, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.add(GroupLayout.LEADING, showOnlyEditedMessagesButton,
					GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.add(
					i18NLayout.createSequentialGroup().add(filterLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED).add(filterTextField, GroupLayout.PREFERRED_SIZE, 0,
							Short.MAX_VALUE))
				.add(
					i18NLayout.createSequentialGroup().add(commonPrefixLabel, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED).add(
							commonPrefix, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
						.addPreferredGap(
							LayoutStyle.RELATED)
						.add(commonPrefixApplyButton, GroupLayout.PREFERRED_SIZE,
							GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)))
			.addContainerGap()));
		i18NLayout.setVerticalGroup(i18NLayout.createParallelGroup(GroupLayout.LEADING).add(i18NLayout.createSequentialGroup().addContainerGap().add(
			i18NLayout.createParallelGroup(GroupLayout.CENTER, false).add(filterLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
				GroupLayout.PREFERRED_SIZE).add(filterTextField, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
			.addPreferredGap(LayoutStyle.RELATED).add(
				treeViewerComposite, GroupLayout.PREFERRED_SIZE, 100, Short.MAX_VALUE)
			.addPreferredGap(LayoutStyle.RELATED).add(showDatabaseMessagesButton,
				GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addPreferredGap(LayoutStyle.RELATED).add(
				showExternalizedMessagesButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addPreferredGap(
				LayoutStyle.RELATED)
			.add(showIgnoredMessagesButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
				GroupLayout.PREFERRED_SIZE)
			.addPreferredGap(LayoutStyle.RELATED).add(showEmptyMessagesButton, GroupLayout.PREFERRED_SIZE,
				GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addPreferredGap(LayoutStyle.RELATED).add(
				showOnlyEditedMessagesButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
				GroupLayout.PREFERRED_SIZE)
			.addContainerGap().add(
				i18NLayout.createParallelGroup(GroupLayout.CENTER, false).add(commonPrefixLabel, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).add(commonPrefix, 0, GroupLayout.PREFERRED_SIZE,
						Short.MAX_VALUE)
					.add(commonPrefixApplyButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE))));

		composite.setLayout(i18NLayout);

		delayedFilterJob = new FilterDelayJob(new FilteredEntity()
		{

			public void filter(final String filterValue, IProgressMonitor monitor)
			{
				if (Display.getCurrent() != null)
				{
					treeViewer.refresh();
					treeViewer.expandAll();

				}
				else
				{
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							treeViewer.refresh();
							treeViewer.expandAll();
						}
					});
				}
			}

		}, FILTER_TYPE_DELAY, "Filtering");

		return composite;
	}

	private void applyCommonPrefix()
	{
		String newText = commonPrefix.getText();

		ArrayList<TreeNode> allNodes = new ArrayList<TreeNode>();
		fillNodes(treeViewer.getTree().getItems(), allNodes, false, false);

		for (TreeNode treeNode : allNodes)
		{
			String treeNodeKey = treeNode.getKey();
			if (treeNodeKey != null && treeNodeKey.length() > 0 && (newText.length() == 0 || !treeNodeKey.startsWith(newText)))
			{
				int subIdx = 0;
				if (commonPrefixValue != null && treeNodeKey.startsWith(commonPrefixValue))
				{
					subIdx = commonPrefixValue.length();
				}

				treeNode.setKey(newText + treeNodeKey.substring(subIdx));
			}
		}
		treeViewer.refresh();
		commonPrefixValue = newText;
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
						if (property == null) property = "";
						boolean visible = (filterText == null) || (property.toLowerCase().indexOf(filterText) != -1);
						ArrayList<IPersist> treePath = new ArrayList<IPersist>();

						IPersist persist = o;
						do
						{
							treePath.add(0, persist);
							if (filterText != null) visible = visible || (getPersistName(persist).toString().toLowerCase().indexOf(filterText) != -1);
						}
						while ((persist = persist.getParent()) != null);

						if (visible) content.addPersistElement(treePath, el, property.startsWith("i18n:") ? STATE.EXTERNALIZE : STATE.INTERNALIZE);
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
							if (visible) content.addPersistElement(treePath, jsText, jsText.getState());
						}
					}
				}

				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
	}

	private final HashMap<IPersist, Map<Integer, ArrayList<Pair<ASTNode, STATE>>>> persistToLineNumberForASTNodes = new HashMap<IPersist, Map<Integer, ArrayList<Pair<ASTNode, STATE>>>>();

	private ArrayList<JSText> getJSTexts(final IPersist persist)
	{
		final ArrayList<JSText> jsTexts = new ArrayList<JSText>();
		String jsPath = SolutionSerializer.getScriptPath(persist, false);
		if (jsPath != null && workspaceFileAccess.exists(jsPath))
		{
			try
			{
				String jsContent = workspaceFileAccess.getUTF8Contents(jsPath);
				final ArrayList<IProblem> problems = new ArrayList<IProblem>();
				IProblemReporter reporter = new IProblemReporter()
				{
					public void reportProblem(IProblem problem)
					{
						if (problem.isError())
						{
							problems.add(problem);
						}
					}
				};

				Script script = JavaScriptParserUtil.parse(jsContent, reporter);
				if (problems.size() == 0 && script != null)
				{
					final HashMap<Integer, Pair<ASTNode, STATE>> startIdxNodeMap = new HashMap<Integer, Pair<ASTNode, STATE>>();
					script.traverse(new ASTVisitor()
					{
						@Override
						public boolean visitGeneral(ASTNode node) throws Exception
						{
							if (node instanceof StringLiteral)
							{
								StringLiteral snode = (StringLiteral)node;
								String value = snode.getValue();
								if (value != null && value.length() != 0 && !value.startsWith("i18n:"))
								{
									ASTNode parent = ((StringLiteral)node).getParent();

									STATE state = (parent instanceof CallExpression &&
										((CallExpression)parent).getExpression().toString().equals(StringLiteralVisitor.I18N_EXTERNALIZE_CALLBACK))
											? STATE.EXTERNALIZE : STATE.INTERNALIZE;

									ASTNode i18nNode = (state == STATE.EXTERNALIZE) ? parent : node;
									startIdxNodeMap.put(Integer.valueOf(i18nNode.sourceStart()), new Pair<ASTNode, STATE>(i18nNode, state));
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
							startIdxNodeMap.put(Integer.valueOf(c.sourceStart()), new Pair<ASTNode, STATE>(c, null));
						}
					}

					Map<Integer, ArrayList<Pair<ASTNode, STATE>>> lineNumberForASTNodes = getLineNumbersForASTNodes(jsContent, startIdxNodeMap);
					persistToLineNumberForASTNodes.put(persist, lineNumberForASTNodes);
					ArrayList<Pair<ASTNode, STATE>> astNodes;
					ASTNode i18nNode;
					ArrayList<SingleLineComment> lineComments;
					int keyIdx = 0;
					for (ArrayList<Pair<ASTNode, STATE>> element : lineNumberForASTNodes.values())
					{
						astNodes = element;
						lineComments = new ArrayList<SingleLineComment>();
						if (astNodes.get(0).getLeft() instanceof SingleLineComment)
						{

							lineComments.add((SingleLineComment)astNodes.remove(0).getLeft());
						}

						for (int y = 0; y < astNodes.size(); y++)
						{
							boolean isIgnored = false;
							for (SingleLineComment lineComment : lineComments)
							{
								isIgnored = isIgnored || (lineComment.getText().indexOf("$NON-NLS-" + (y + 1) + "$") != -1);
								if (isIgnored) break;
							}

							i18nNode = astNodes.get(y).getLeft();
							String keyHint = getKeyHint(persist, null) + keyIdx++;
							STATE state = isIgnored ? STATE.IGNORE : astNodes.get(y).getRight();

							jsTexts.add(new JSText(persist, i18nNode instanceof StringLiteral ? ((StringLiteral)i18nNode).getText() : "",
								i18nNode.sourceStart(), i18nNode.sourceEnd(), keyHint, getNameHintForJSNodeLiteral((JSNode)i18nNode), state));
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

	private String removeNONNLSComments(String jsContent)
	{
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

		Script script = JavaScriptParserUtil.parse(jsContent, reporter);
		if (problems.size() == 0 && script != null)
		{
			String commentTxt;
			ArrayList<Comment> commentsToRemove = new ArrayList<Comment>();
			for (Comment comment : script.getComments())
			{
				if (comment instanceof SingleLineComment)
				{
					commentTxt = comment.getText();
					if (commentTxt.startsWith("//$NON-NLS-"))
					{
						commentsToRemove.add(comment);
					}
				}
			}
			if (commentsToRemove.size() > 0)
			{
				StringBuilder newJSContent = new StringBuilder(jsContent);

				for (Comment comment : commentsToRemove)
				{
					int offset = comment.getRange().getOffset();
					int len = comment.getRange().getLength();

					newJSContent.replace(offset, offset + len, "");
				}

				return newJSContent.toString();
			}
		}

		return jsContent;
	}

	private String getNameHintForJSNodeLiteral(JSNode jsNode)
	{
		String nameHint = "";
		ASTNode parentNode;
		JSNode currentNode = jsNode;
		while ((parentNode = currentNode.getParent()) instanceof JSNode)
		{
			if (parentNode instanceof VariableDeclaration) nameHint = ((VariableDeclaration)parentNode).getVariableName();
			else if (parentNode instanceof FunctionStatement)
			{
				nameHint = ((FunctionStatement)parentNode).getFunctionName();
				break;
			}
			currentNode = (JSNode)parentNode;
		}

		return nameHint;
	}

	private Map<Integer, ArrayList<Pair<ASTNode, STATE>>> getLineNumbersForASTNodes(String jsConent, Map<Integer, Pair<ASTNode, STATE>> astNodesMap)
	{
		Map<Integer, ArrayList<Pair<ASTNode, STATE>>> lineNumbersForASTNodesMap = new HashMap<Integer, ArrayList<Pair<ASTNode, STATE>>>();

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
					ArrayList<Pair<ASTNode, STATE>> lineNodes = lineNumbersForASTNodesMap.get(oLine);
					if (lineNodes == null)
					{
						lineNodes = new ArrayList<Pair<ASTNode, STATE>>();
						lineNumbersForASTNodesMap.put(oLine, lineNodes);
					}
					Pair<ASTNode, STATE> node = astNodesMap.get(oI);
					if (node.getLeft() instanceof SingleLineComment) lineNodes.add(0, node);
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
				Map.Entry<String, String> msgEntry;
				for (Entry<String, String> element2 : defaultMessages.entrySet())
				{
					msgEntry = element2;
					if (msgEntry.getValue().equals(defaultText)) return msgEntry.getKey();
				}
			}
		}

		StringBuffer keyHint = new StringBuffer();
		IPersist p = persist;
		Element pName;
		int idx = 0;
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
						name = "label_" + idx++;
						break;
					case IRepository.TABS :
						name = "tab_" + idx++;
						break;
					default :
						name = p.toString() + "_" + idx++;

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
		fillNodes(treeViewer.getTree().getItems(), selectedNodes, true, true);

		final HashMap<Solution, ArrayList<IPersist>> solutionChangedPersistsMap = new HashMap<Solution, ArrayList<IPersist>>();
		final ArrayList<Table> changedTables = new ArrayList<Table>();
		final HashMap<IPersist, TreeMap<Integer, JSText>> persistJSTextMap = new HashMap<IPersist, TreeMap<Integer, JSText>>();
		for (TreeNode selNode : selectedNodes)
		{
			if (selNode.isElement())
			{
				IPersist p = (IPersist)selNode.getParent().getData();
				Element element = (Element)selNode.getData();
				String propertyValue = null;
				if (selNode.getState() == STATE.EXTERNALIZE)
				{
					propertyValue = "i18n:" + selNode.getKey();
				}
				else if (selNode.getState() == STATE.INTERNALIZE)
				{
					propertyValue = selNode.getText();
				}
				if (propertyValue == null) continue;
				setProperty(p, element, propertyValue);
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
				String columnInfoValue = null;
				if (selNode.getState() == STATE.EXTERNALIZE)
				{
					columnInfoValue = "i18n:" + selNode.getKey();
				}
				else if (selNode.getState() == STATE.INTERNALIZE)
				{
					columnInfoValue = selNode.getText();
				}
				if (columnInfoValue == null) continue;
				ci.setTitleText(columnInfoValue);
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
				jstxt.text = selNode.getText();
				jstxt.state = selNode.getState();
				persistJSTexts.put(Integer.valueOf(jstxt.getStartPosition()), jstxt);
			}

			if (selNode.getState() == STATE.EXTERNALIZE)
			{
				if (!defaultMessages.containsKey(selNode.getKey()))
				{
					I18NUtil.MessageEntry messageEntry = new I18NUtil.MessageEntry(null, selNode.getKey(), selNode.text);
					projectMessages.put(messageEntry.getLanguageKey(), messageEntry);
				}
			}
			else if (selNode.getState() == STATE.INTERNALIZE)
			{
				defaultMessages.remove(selNode.getKey());
			}
		}

		WorkspaceJob externalizeJob = new WorkspaceJob("Externalizing texts ...")
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				try
				{
					EclipseMessages.writeMessages(editingSolution.getI18nServerName(), editingSolution.getI18nTableName(), projectMessages,
						workspaceFileAccess);

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
								String persistJSContent = I18NExternalizeDialog.this.removeNONNLSComments(workspaceFileAccess.getUTF8Contents(jsPath));
								StringBuffer replacedPersistJSContent = new StringBuffer();
								ArrayList<Integer> lineIdxOfIgnoredString = new ArrayList<Integer>();
								int line = 1;
								StringBuffer ignoreLineComment;
								for (int i = 0; i < persistJSContent.length();)
								{
									if (persistJSTextStartPos.size() > 0 && i == persistJSTextStartPos.get(0).intValue())
									{
										String replaceText = null;
										JSText jstxt = persistJSTexts.get(persistJSTextStartPos.get(0));
										if (jstxt.getState() == STATE.IGNORE)
										{
											replaceText = "'" + jstxt.getText() + "'";
											Map<Integer, ArrayList<Pair<ASTNode, STATE>>> linesNodes = persistToLineNumberForASTNodes.get(
												persistJSTextMapEntry.getKey());
											if (linesNodes != null)
											{
												ArrayList<Pair<ASTNode, STATE>> lineNodes = linesNodes.get(Integer.valueOf(line));
												if (lineNodes != null)
												{
													for (int j = 0; j < lineNodes.size(); j++)
													{
														if (lineNodes.get(j).getLeft() instanceof StringLiteral &&
															((StringLiteral)lineNodes.get(j).getLeft()).getText().equals(replaceText))
														{
															lineIdxOfIgnoredString.add(Integer.valueOf(j + 1));
															break;
														}
													}
												}
											}
										}
										else if (jstxt.getState() == STATE.EXTERNALIZE)
										{
											replaceText = new StringBuilder(StringLiteralVisitor.I18N_EXTERNALIZE_CALLBACK + "('").append(
												jstxt.getKeyHint()).append("')").toString();
										}
										else
										// STATE.INTERNALIZE
										{
											replaceText = "'" + jstxt.getText() + "'";
										}

										replacedPersistJSContent.append(replaceText);
										i = jstxt.getEndPosition();


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
							if (sp.getSolution().getUUID().equals(solutionChangedPersistsMapEntry.getKey().getUUID()))
							{
								ArrayList<IPersist> changedPersists = solutionChangedPersistsMapEntry.getValue();
								sp.saveEditingSolutionNodes(changedPersists.toArray(new IPersist[changedPersists.size()]), false);
								break;
							}
						}
					}


					IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
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

	private void fillNodes(TreeItem[] items, ArrayList<TreeNode> selectedNodes, boolean onlySelected, boolean onlyChanged)
	{
		if (items != null && items.length > 0)
		{
			TreeNode node;
			for (TreeItem treeItem : items)
			{
				node = (TreeNode)treeItem.getData();
				if (node != null && !node.isVisible()) continue;
				if (node == null || (!node.isElement() && !node.isColumnInfo() && !node.isJSText()))
					fillNodes(treeItem.getItems(), selectedNodes, onlySelected, onlyChanged);
				else if ((!onlySelected || treeItem.getChecked()) && (!onlyChanged || node.isChanged())) selectedNodes.add(node);
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
		String nameHint;
		STATE state;

		JSText(IPersist parent, String text, int startPosition, int endPosition, String keyHint, String nameHint, STATE state)
		{
			this.parent = parent;
			this.text = text;
			this.startPosition = startPosition;
			this.endPosition = endPosition;
			this.keyHint = keyHint;
			this.nameHint = nameHint;
			this.state = state;
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

		String getNameHint()
		{
			return nameHint;
		}

		STATE getState()
		{
			return state;
		}

		@Override
		public boolean equals(Object jsText)
		{
			if (jsText instanceof JSText)
			{
				JSText compareJSText = (JSText)jsText;
				return parent.equals(compareJSText.getParent()) && (startPosition == compareJSText.getStartPosition());
			}

			return false;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + parent.hashCode();
			result = prime * result + startPosition;
			return result;
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
					return treeNode.getState() == STATE.INTERNALIZE ? ""
						: (treeNode.isJSText() && (treeNode.getState() == STATE.IGNORE) ? "$NON-NLS$" : treeNode.getKey());
				case I18NExternalizeDialog.COLUMN_STATUS :
					return treeNode.getStateAsString();
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
								return ElementUtil.getImageForFormEncapsulation((Form)data);
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
					else if (data instanceof JSText)
					{
						return I18NExternalizeDialog.this.jsImage;
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

		private STATE state;

		private boolean isChecked = true;
		private boolean isChanged;

		private String initialText;
		private String initialKey;
		private STATE initialState;

		TreeNode()
		{
		}

		TreeNode(TreeNode parent, Object itemData, STATE state)
		{
			this.parent = parent;
			this.itemData = itemData;
			if (isElement() || isColumnInfo() || isJSText())
			{
				this.state = state;
				if (state == STATE.EXTERNALIZE)
				{
					text = "";
				}

				initialKey = getKey();
				initialText = getText();
				initialState = getState();

				// by default change intern nodes to externalize
				if (initialState == STATE.INTERNALIZE) setState(STATE.EXTERNALIZE);
			}
		}


		void addPersistElement(List<IPersist> treePath, Object el, STATE state)
		{
			if (treePath.size() == 0)
			{
				addChild(new TreeNode(this, el, state));
			}
			else
			{
				IPersist treePathHead = treePath.get(0);
				for (TreeNode child : children)
				{
					if (child.getData().equals(treePathHead))
					{
						child.addPersistElement(treePath.subList(1, treePath.size()), el, state);
						return;
					}
				}

				TreeNode child = new TreeNode(this, treePathHead, state);
				addChild(child);
				child.addPersistElement(treePath.subList(1, treePath.size()), el, state);
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
			ArrayList<TreeNode> visibleChildren = new ArrayList<TreeNode>();
			for (TreeNode treeNode : children)
			{
				if (treeNode.isVisible()) visibleChildren.add(treeNode);
			}
			return visibleChildren;
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
					text = ((ColumnInfo)getData()).getTitleText();
					if (text == null)
					{
						text = ((Column)parent.getData()).getName();
						text = Utils.stringInitCap(Utils.stringReplace(text, "_", " "));
					}

				}
				else if (isJSText())
				{
					text = ((JSText)itemData).getText();
					if (text.length() > 0) text = text.substring(1, text.length() - 1);
				}
				else text = "";
			}

			return text;
		}

		void setText(String text)
		{
			this.text = text;
			checkChange();
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
			checkChange();
		}

		void setChecked(boolean checked)
		{
			this.isChecked = checked;
		}

		boolean isChecked()
		{
			return isChecked;
		}

		STATE getState()
		{
			return state;
		}

		void setState(STATE state)
		{
			this.state = state;
			checkChange();
		}

		String getStateAsString()
		{
			if (state != null)
			{
				switch (state)
				{
					case EXTERNALIZE :
						return STATE_TXT_EXTERNALIZE;
					case INTERNALIZE :
						return STATE_TXT_INTERNALIZE;
					case IGNORE :
						return STATE_TXT_IGNORE;
				}
			}

			return "";
		}

		boolean isChanged()
		{
			return isChanged || (getState() == STATE.IGNORE);
		}

		private void checkChange()
		{
			isChanged = (initialState != getState()) || !getKey().equals(initialKey) || !getText().equals(initialText);
		}

		boolean isVisible()
		{
			if (!loadingContent)
			{
				boolean isI18NNode = isElement() || isColumnInfo() || isJSText();
				if (!isI18NNode && getChildren().size() == 0)
				{
					return false;
				}
				else if (isI18NNode)
				{
					String filterText = filterTextField != null ? filterTextField.getText() : null;
					if (filterText != null && filterText.length() > 0 && getText().indexOf(filterText) == -1) return false;

					if (!isShowDatabaseMsg && isColumnInfo()) return false;
					if (!isShowExternalizedMsg && getState() == STATE.EXTERNALIZE) return false;
					if ((!isShowIgnoredMsg && getState() == STATE.IGNORE)) return false;
					if (!isShowEmptyMsg && getText().length() == 0 && getState() != STATE.EXTERNALIZE) return false;
					if (isShowOnlyEditedMsg && !isChanged) return false;
				}
			}

			return true;
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
				return ((JSText)itemData).getNameHint();
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
		return new Point(1024, 768);
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


	private class StateCellEditor extends CellEditor
	{
		private final STATE[] states;
		private int valueIdx;

		StateCellEditor(Composite parent, STATE[] states)
		{
			super(parent);
			this.states = states;
		}

		/*
		 * @see org.eclipse.jface.viewers.CellEditor#createControl(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		protected Control createControl(Composite parent)
		{
			return null;
		}

		/*
		 * @see org.eclipse.jface.viewers.CellEditor#doGetValue()
		 */
		@Override
		protected Object doGetValue()
		{
			return states[valueIdx];
		}

		/*
		 * @see org.eclipse.jface.viewers.CellEditor#doSetFocus()
		 */
		@Override
		protected void doSetFocus()
		{
			// ignore
		}

		/*
		 * @see org.eclipse.jface.viewers.CellEditor#doSetValue(java.lang.Object)
		 */
		@Override
		protected void doSetValue(Object v)
		{
			for (int i = 0; i < states.length; i++)
			{
				if (states[i].equals(v))
				{
					valueIdx = i;
					break;
				}
			}
		}

		@Override
		public void activate()
		{
			valueIdx = (valueIdx + 1) % states.length;
			fireApplyEditorValue();
		}

		@Override
		public void activate(ColumnViewerEditorActivationEvent activationEvent)
		{
			if (activationEvent.eventType != ColumnViewerEditorActivationEvent.TRAVERSAL)
			{
				super.activate(activationEvent);
			}
		}
	}
}