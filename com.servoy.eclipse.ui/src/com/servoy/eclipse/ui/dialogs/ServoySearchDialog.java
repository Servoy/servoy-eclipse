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

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.FormHierarchyView;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenFormEditorAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenScriptAction;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScope;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 */
public class ServoySearchDialog extends FilteredItemsSelectionDialog
{
	private boolean contentLoaded = false;

	/**
	 * @author jcompagner
	 */
	private final class ServoyItemsFilter extends ItemsFilter
	{
		boolean showTables = showTablesAction.isChecked();
		boolean showTableColumns = showTableColumnsAction.isChecked();
		boolean showCalculations = showCalculationsAction.isChecked();
		boolean showElements = showElementsAction.isChecked();
		boolean showForms = showFormsAction.isChecked();
		boolean showMethods = showMethodsAction.isChecked();
		boolean showVariables = showVariablesAction.isChecked();
		boolean showRelations = showRelationsAction.isChecked();
		boolean showValuelists = showValuelistsAction.isChecked();
		boolean showMedia = showMediaAction.isChecked();
		boolean showScopes = showScopesAction.isChecked();

		/**
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#matchItem(java.lang.Object)
		 */
		@Override
		public boolean matchItem(Object item)
		{
			String name = ((ISupportName)item).getName();
			boolean b = name != null && matches(name);
			if (b)
			{
				if (item instanceof Table)
				{
					b = showTables;
				}
				else if (item instanceof Column)
				{
					b = showTableColumns;
				}
				else if (item instanceof ScriptCalculation)
				{
					b = showCalculations;
				}
				else if (item instanceof BaseComponent)
				{
					b = showElements;
				}
				else if (item instanceof Form)
				{
					b = showForms;
				}
				else if (item instanceof ScriptMethod)
				{
					b = showMethods;
				}
				else if (item instanceof ScriptVariable)
				{
					b = showVariables;
				}
				else if (item instanceof Relation)
				{
					b = showRelations;
				}
				else if (item instanceof ValueList)
				{
					b = showValuelists;
				}
				else if (item instanceof Media)
				{
					b = showMedia;
				}
				else if (item instanceof Scope)
				{
					b = showScopes;
				}
			}
			return b;
		}

		/**
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#isConsistentItem(java.lang.Object)
		 */
		@Override
		public boolean isConsistentItem(Object item)
		{
			return true;
		}

		/**
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#isSubFilter(org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter)
		 */
		@Override
		public boolean isSubFilter(ItemsFilter filter)
		{
			return false;
		}

		/**
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#equalsFilter(org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter)
		 */
		@Override
		public boolean equalsFilter(ItemsFilter filter)
		{
			if (super.equalsFilter(filter) && filter instanceof ServoyItemsFilter)
			{
				ServoyItemsFilter siFilter = (ServoyItemsFilter)filter;
				return siFilter.showCalculations == showCalculations && siFilter.showElements == showElements && siFilter.showForms == showForms &&
					siFilter.showMethods == showMethods && siFilter.showRelations == showRelations && siFilter.showTables == showTables &&
					siFilter.showTableColumns == showTableColumns &&
					siFilter.showValuelists == showValuelists && siFilter.showVariables == showVariables && siFilter.showMedia == showMedia &&
					siFilter.showScopes == showScopes;

			}
			return false;
		}
	}

	/**
	 * @author jcompagner
	 *
	 */
	private class ShowAction extends Action
	{
		public ShowAction(String name)
		{
			super(name, IAction.AS_CHECK_BOX);
			setChecked(true);
		}

		/**
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@Override
		public void run()
		{
			applyFilter();
		}
	}

	private class OpenInScriptEditorAction extends OpenScriptAction
	{
		/**
		 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenScriptAction#run()
		 */
		@Override
		public void run()
		{
			super.run();
			getShell().close();
		}
	}

	private class OpenInFormEditorAction extends OpenFormEditorAction
	{
		/**
		 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenFormEditorAction#run()
		 */
		@Override
		public void run()
		{
			super.run();
			getShell().close();
		}
	}

	private class OpenFormHierarchyAction extends OpenInFormEditorAction
	{
		public OpenFormHierarchyAction()
		{
			setImageDescriptor(Activator.loadImageDescriptorFromBundle("form_hierarchy.png"));
			setText("Open Form Hierarchy");
			setToolTipText(getText());
		}

		@Override
		public void run()
		{
			try
			{
				IStructuredSelection select = this.selection;
				FormHierarchyView view = (FormHierarchyView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(FormHierarchyView.ID);
				IPersist persist = select.getFirstElement() instanceof PersistContext ? ((PersistContext)select.getFirstElement()).getPersist()
					: (IPersist)select.getFirstElement();
				view.open(persist);
			}
			catch (PartInitException e)
			{
				ServoyLog.logError(e);
			}
			getShell().close();
		}
	}

	private class ToggleAction extends Action
	{
		public ToggleAction()
		{
			super("Toggle All", IAction.AS_PUSH_BUTTON);
		}

		/**
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@Override
		public void run()
		{
			showFormsAction.setChecked(!showFormsAction.isChecked());
			showMethodsAction.setChecked(!showMethodsAction.isChecked());
			showVariablesAction.setChecked(!showVariablesAction.isChecked());
			showTablesAction.setChecked(!showTablesAction.isChecked());
			showTableColumnsAction.setChecked(!showTableColumnsAction.isChecked());
			showCalculationsAction.setChecked(!showCalculationsAction.isChecked());
			showElementsAction.setChecked(!showElementsAction.isChecked());
			showRelationsAction.setChecked(!showRelationsAction.isChecked());
			showValuelistsAction.setChecked(!showValuelistsAction.isChecked());
			showMediaAction.setChecked(!showMediaAction.isChecked());
			showScopesAction.setChecked(!showScopesAction.isChecked());

			applyFilter();
		}
	}

	private class CheckUncheckAction extends Action
	{
		public CheckUncheckAction()
		{
			super("Check/Uncheck All", IAction.AS_PUSH_BUTTON);
		}

		/**
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@Override
		public void run()
		{
			boolean checked = !showFormsAction.isChecked() || !showMethodsAction.isChecked() || !showVariablesAction.isChecked() ||
				!showTableColumnsAction.isChecked() || !showTablesAction.isChecked() || !showCalculationsAction.isChecked() ||
				!showElementsAction.isChecked() || !showRelationsAction.isChecked() ||
				!showValuelistsAction.isChecked() || !showMediaAction.isChecked() || !showScopesAction.isChecked();

			showFormsAction.setChecked(checked);
			showMethodsAction.setChecked(checked);
			showVariablesAction.setChecked(checked);
			showTablesAction.setChecked(checked);
			showTableColumnsAction.setChecked(checked);
			showCalculationsAction.setChecked(checked);
			showElementsAction.setChecked(checked);
			showRelationsAction.setChecked(checked);
			showValuelistsAction.setChecked(checked);
			showMediaAction.setChecked(checked);
			showScopesAction.setChecked(checked);

			applyFilter();
		}
	}

	private static final IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

	private static final String SHOW_FORMS = "ShowForms";
	private static final String SHOW_METHODS = "ShowMethods";
	private static final String SHOW_VARIABLES = "ShowVariables";
	private static final String SHOW_TABLES = "ShowTables";
	private static final String SHOW_TABLE_COLUMNS = "ShowTableColumns";
	private static final String SHOW_CALCULATIONS = "ShowCalculations";
	private static final String SHOW_ELEMENTS = "ShowElements";
	private static final String SHOW_RELATIONS = "ShowRelations";
	private static final String SHOW_VALUELIST = "ShowValuelist";
	private static final String SHOW_MEDIA = "ShowValuelist";
	private static final String SHOW_SCOPES = "ShowScopes";

	private final ShowAction showFormsAction;
	private final ShowAction showMethodsAction;
	private final ShowAction showVariablesAction;
	private final ShowAction showTablesAction;
	private final ShowAction showTableColumnsAction;
	private final ShowAction showCalculationsAction;
	private final ShowAction showElementsAction;
	private final ShowAction showRelationsAction;
	private final ShowAction showValuelistsAction;
	private final ShowAction showMediaAction;
	private final ShowAction showScopesAction;

	private final ToggleAction toggleAllAction;
	private final CheckUncheckAction checkUncheckAllAction;
	private final OpenInScriptEditorAction showEditWithScriptEditor;
	private final OpenInFormEditorAction showEditWithFormEditor;
	private final OpenFormHierarchyAction showEditWithFormHierarchy;

	private boolean isAltKeyPressed;
	private SelectionListener okButtonSelectionListener;

	/**
	 * @param shell
	 */

	public ServoySearchDialog(Shell shell)
	{
		super(shell);
		setTitle("Locate Servoy Resource");
		showFormsAction = new ShowAction("Show Forms");
		showMethodsAction = new ShowAction("Show Methods");
		showVariablesAction = new ShowAction("Show Variables");
		showTablesAction = new ShowAction("Show Tables");
		showTableColumnsAction = new ShowAction("Show Table Columns");
		showCalculationsAction = new ShowAction("Show Calculations");
		showElementsAction = new ShowAction("Show Elements");
		showRelationsAction = new ShowAction("Show Relations");
		showValuelistsAction = new ShowAction("Show Valuelists");
		showMediaAction = new ShowAction("Show Media");
		showScopesAction = new ShowAction("Show Scopes");
		toggleAllAction = new ToggleAction();
		checkUncheckAllAction = new CheckUncheckAction();
		showEditWithScriptEditor = new OpenInScriptEditorAction();
		showEditWithFormEditor = new OpenInFormEditorAction();
		showEditWithFormHierarchy = new OpenFormHierarchyAction();

		setSelectionHistory(new SelectionHistory()
		{
			/**
			 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#restoreItemFromMemento(org.eclipse.ui.IMemento)
			 */
			@Override
			protected Object restoreItemFromMemento(IMemento memento)
			{
				String uuidString = memento.getString("UUID");
				if (uuidString == null)
				{
					String dataSource = memento.getString("datasource");
					if (dataSource != null)
					{
						String[] snt = DataSourceUtils.getDBServernameTablename(dataSource);
						if (snt != null)
						{
							IServer server = ApplicationServerRegistry.get().getServerManager().getServer(snt[0], true, true);
							try
							{
								if (server != null && server.getTableType(snt[1]) != ITable.UNKNOWN) // server.getTable() Initializes table (fetches columns)
								{
									String name = memento.getString("columnname");
									if (name != null)
									{
										return new Column(dataSource, name);

									}
									else
									{
										return new Table(dataSource);
									}
								}
							}
							catch (RepositoryException e)
							{
								ServoyLog.logError(e);
							}
							catch (RemoteException e)
							{
								ServoyLog.logError(e);
							}
						}
					}
					else
					{
						String scopeName = memento.getString("scopename");
						String solutionName = memento.getString("solutionname");
						if (scopeName != null && solutionName != null && servoyModel.getServoyProject(solutionName) != null)
						{
							Solution solution = servoyModel.getServoyProject(solutionName).getSolution();
							if (solution != null && servoyModel.isSolutionActive(solutionName) && solution.getScopeNames().contains(scopeName))
							{
								return new Scope(scopeName, solutionName);
							}
						}
					}
					return null;
				}
				UUID uuid = UUID.fromString(uuidString);
				IPersist searchPersist = AbstractRepository.searchPersist(servoyModel.getActiveProject().getSolution(), uuid);
				if (searchPersist != null) return searchPersist;
				Solution[] modules = servoyModel.getFlattenedSolution().getModules();
				if (modules == null) return null;
				for (Solution solution : modules)
				{
					searchPersist = AbstractRepository.searchPersist(solution, uuid);
					if (searchPersist != null) return searchPersist;
				}
				return null;
			}

			/**
			 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#storeItemToMemento(java.lang.Object, org.eclipse.ui.IMemento)
			 */
			@Override
			protected void storeItemToMemento(Object item, IMemento memento)
			{
				if (item instanceof IPersist)
				{
					memento.putString("UUID", ((IPersist)item).getUUID().toString());
				}
				else if (item instanceof Table)
				{
					Table table = (Table)item;
					memento.putString("datasource", table.getDataSource());
				}
				else if (item instanceof Column)
				{
					Column column = (Column)item;
					memento.putString("datasource", column.getDataSource());
					memento.putString("columnname", column.getColumnName());
				}
				else if (item instanceof Scope)
				{
					Scope scope = (Scope)item;
					memento.putString("scopename", scope.getScopeName());
					memento.putString("solutionname", scope.getSolutionName());
				}
			}
		});
		ILabelProvider provider = new LabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				if (element == null) return null;
				String name = ((ISupportName)element).getName();

				if (element instanceof IPersist)
				{
					String parent = "";
					IPersist persist = (IPersist)element;
					while (persist.getParent() != null)
					{
						persist = persist.getParent();

						if (persist instanceof ISupportName)
						{
							parent = '/' + ((ISupportName)persist).getName() + parent;
						}
						else if (persist instanceof Solution)
						{
							parent = '/' + ((Solution)persist).getName() + parent;
						}
						else if (persist instanceof TableNode)
						{
							parent = "  (" + ((TableNode)persist).getDataSource() + ')' + parent;
						}
					}
					if (element instanceof ISupportScope && ((ISupportScope)element).getScopeName() != null)
					{
						parent += "/" + ScriptVariable.SCOPES + "/" + ((ISupportScope)element).getScopeName();
					}
					name += " - " + parent.substring(1);
				}
				return name;
			}

			@Override
			public Image getImage(Object element)
			{
				String image = null;
				if (element instanceof IPersist)
				{
					image = ElementUtil.getPersistImageName((IPersist)element);
				}
				else if (element instanceof Table)
				{
					image = "portal.png";
				}
				else if (element instanceof Column)
				{
					image = "column.png";
				}
				else if (element instanceof Scope)
				{
					image = "scopes.png";
				}

				if (image == null)
				{
					return null;
				}
				Image img = Activator.getDefault().loadImageFromOldLocation(image);
				if (img == null)
				{
					img = Activator.getDefault().loadImageFromBundle(image);
				}
				return img;
			}
		};
		setDetailsLabelProvider(provider);
		setListLabelProvider(provider);

	}


	/**
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	protected void fillContextMenu(IMenuManager menuManager)
	{

		if (this.getSelectedItems().getFirstElement() instanceof Form)
		{
			menuManager.add(showEditWithFormHierarchy);
			menuManager.add(showEditWithScriptEditor);
			menuManager.add(showEditWithFormEditor);

			super.fillContextMenu(menuManager);
			showEditWithFormHierarchy.setSelection(getSelectedItems());
			showEditWithScriptEditor.setSelection(getSelectedItems());
			showEditWithFormEditor.setSelection(getSelectedItems());
		}
	}


	/**
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#fillViewMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	protected void fillViewMenu(IMenuManager menuManager)
	{
		menuManager.add(showTablesAction);
		menuManager.add(showTableColumnsAction);
		menuManager.add(showFormsAction);
		menuManager.add(showMethodsAction);
		menuManager.add(showVariablesAction);
		menuManager.add(showElementsAction);
		menuManager.add(showCalculationsAction);
		menuManager.add(showRelationsAction);
		menuManager.add(showValuelistsAction);
		menuManager.add(showMediaAction);
		menuManager.add(showScopesAction);
		menuManager.add(toggleAllAction);
		menuManager.add(checkUncheckAllAction);
		super.fillViewMenu(menuManager);
	}

	/**
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#storeDialog(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	@Override
	protected void storeDialog(IDialogSettings settings)
	{
		settings.put(SHOW_FORMS, !showFormsAction.isChecked());
		settings.put(SHOW_METHODS, !showMethodsAction.isChecked());
		settings.put(SHOW_VARIABLES, !showVariablesAction.isChecked());
		settings.put(SHOW_TABLES, !showTablesAction.isChecked());
		settings.put(SHOW_TABLE_COLUMNS, !showTableColumnsAction.isChecked());
		settings.put(SHOW_CALCULATIONS, !showCalculationsAction.isChecked());
		settings.put(SHOW_ELEMENTS, !showElementsAction.isChecked());
		settings.put(SHOW_RELATIONS, !showRelationsAction.isChecked());
		settings.put(SHOW_VALUELIST, !showValuelistsAction.isChecked());
		settings.put(SHOW_MEDIA, !showMediaAction.isChecked());
		settings.put(SHOW_SCOPES, !showScopesAction.isChecked());
		super.storeDialog(settings);
	}

	/**
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#restoreDialog(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	@Override
	protected void restoreDialog(IDialogSettings settings)
	{
		showFormsAction.setChecked(!settings.getBoolean(SHOW_FORMS));
		showMethodsAction.setChecked(!settings.getBoolean(SHOW_METHODS));
		showVariablesAction.setChecked(!settings.getBoolean(SHOW_VARIABLES));
		showTablesAction.setChecked(!settings.getBoolean(SHOW_TABLES));
		showTableColumnsAction.setChecked(!settings.getBoolean(SHOW_TABLE_COLUMNS));
		showCalculationsAction.setChecked(!settings.getBoolean(SHOW_CALCULATIONS));
		showElementsAction.setChecked(!settings.getBoolean(SHOW_ELEMENTS));
		showRelationsAction.setChecked(!settings.getBoolean(SHOW_RELATIONS));
		showValuelistsAction.setChecked(!settings.getBoolean(SHOW_VALUELIST));
		showMediaAction.setChecked(!settings.getBoolean(SHOW_MEDIA));
		showScopesAction.setChecked(!settings.getBoolean(SHOW_SCOPES));
		super.restoreDialog(settings);
	}

	/**
	 * Create contents of the dialog
	 *
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite container = (Composite)super.createDialogArea(parent);
		return container;
	}

	@Override
	public void create()
	{
		super.create();
		// add our on selection listener to OK so we can read the modifier keys
		okButtonSelectionListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				// on linux we really need to check for BUTTON_MASK (mouse click)
				// because on key press ENTER, SWT.ALT is always set
				if (Platform.getOS().equals(Platform.OS_LINUX))
				{
					isAltKeyPressed = (e.stateMask & SWT.BUTTON_MASK) != 0 && (e.stateMask & SWT.ALT) != 0;
				}
				else
				{
					isAltKeyPressed = (e.stateMask & SWT.ALT) != 0;
				}

				getOkButton().removeSelectionListener(okButtonSelectionListener);
				superOkPressed();
			}
		};
		getOkButton().addSelectionListener(okButtonSelectionListener);
	}

	@Override
	protected void okPressed()
	{
		// avoid calling super okPressed, let our own listener get the key modifiers first
	}

	@Override
	protected void handleDoubleClick()
	{
		superOkPressed();
	}

	private void superOkPressed()
	{
		super.okPressed();
	}

	public boolean isAltKeyPressed()
	{
		return isAltKeyPressed;
	}

	/**
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createExtendedContentArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createExtendedContentArea(Composite parent)
	{
		return null;
	}

	/**
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createFilter()
	 */
	@Override
	protected ItemsFilter createFilter()
	{
		return new ServoyItemsFilter();
	}

	@Override
	public void scheduleRefresh()
	{
		if (contentLoaded)
		{
			super.scheduleRefresh();
		}
	}

	/**
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#fillContentProvider(org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.AbstractContentProvider,
	 *      org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException
	{
		progressMonitor.subTask("Loading Servoy content...");
		FlattenedSolution flattenedSolution = servoyModel.getFlattenedSolution();

		//add persists and elements
		List<IPersist> allObjectsAsList = flattenedSolution.getAllObjectsAsList();
		for (IPersist persist : allObjectsAsList)
		{
			if (persist instanceof ISupportName && ((ISupportName)persist).getName() != null)
			{
				contentProvider.add(persist, itemsFilter);
			}
			if (persist instanceof Form)
			{
				Iterator<IPersist> allObjects = ((Form)persist).getAllObjects();
				while (allObjects.hasNext())
				{
					IPersist p2 = allObjects.next();
					if (p2 instanceof ISupportName && ((ISupportName)p2).getName() != null)
					{
						contentProvider.add(p2, itemsFilter);
					}
				}
			}
			else if (persist instanceof TableNode)
			{
				TableNode node = (TableNode)persist;
				Iterator<IPersist> allObjects = node.getAllObjects();
				while (allObjects.hasNext())
				{
					IPersist p = allObjects.next();
					if (p.getTypeID() == IRepository.SCRIPTCALCULATIONS || p.getTypeID() == IRepository.METHODS)
					{
						contentProvider.add(p, itemsFilter);
					}
				}
			}
		}

		//add tables
		String[] serverNames = ApplicationServerRegistry.get().getServerManager().getServerNames(true, true, false, false);
		for (String serverName : serverNames)
		{
			try
			{
				IServer server = ApplicationServerRegistry.get().getServerManager().getServer(serverName);
				List<String> tableNames = server.getTableAndViewNames(true);
				for (String tableName : tableNames)
				{
					contentProvider.add(new Table(DataSourceUtils.createDBTableDataSource(serverName, tableName)), itemsFilter);
					if (showTableColumnsAction.isChecked())
					{
						Job job = new Job("Loading columns for table " + tableName)
						{
							@Override
							protected IStatus run(IProgressMonitor monitor)
							{
								try
								{
									ITable table = server.getTable(tableName);
									table.getColumns().forEach(
										column -> contentProvider.add(
											new Column(DataSourceUtils.createDBTableDataSource(serverName, tableName), column.getName()),
											itemsFilter));
									reloadCache(false, new NullProgressMonitor());
									Display.getDefault().asyncExec(() -> {
										refresh();
									});
								}
								catch (RepositoryException | RemoteException e)
								{
									ServoyLog.logError(e);
								}
								return Status.OK_STATUS;
							}
						};
						job.schedule();
						job.setRule(new ServerScheduleRule(serverName));
					}
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}

		ServoyProject[] modulesOfActiveProject = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
		for (ServoyProject servoyProject : modulesOfActiveProject)
		{
			try
			{
				List<String> tables = servoyProject.getMemServer().getTableNames(false);
				if (tables != null)
				{
					for (String tableName : tables)
					{
						contentProvider.add(new Table(DataSourceUtils.createInmemDataSource(tableName)), itemsFilter);
						servoyProject.getMemServer().getTable(tableName).getColumns().forEach(
							column -> contentProvider.add(new Column(DataSourceUtils.createInmemDataSource(tableName), column.getName()), itemsFilter));
					}
				}
				tables = servoyProject.getViewFoundsetsServer().getTableNames(false);
				if (tables != null)
				{
					for (String tableName : tables)
					{
						contentProvider.add(new Table(DataSourceUtils.createViewDataSource(tableName)), itemsFilter);
						servoyProject.getViewFoundsetsServer().getTable(tableName).getColumns()
							.forEach(column -> contentProvider.add(new Column(DataSourceUtils.createViewDataSource(tableName), column.getName()), itemsFilter));
					}
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}

		//add scopes
		Collection<Pair<String, IRootObject>> scopes = flattenedSolution.getAllScopes();
		for (Pair<String, IRootObject> scope : scopes)
		{
			contentProvider.add(new Scope(scope.getLeft(), scope.getRight().getName()), itemsFilter);
		}
		contentLoaded = true;
	}

	/**
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getDialogSettings()
	 */
	@Override
	protected IDialogSettings getDialogSettings()
	{
		return Activator.getDefault().getDialogSettings();
	}

	/**
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getElementName(java.lang.Object)
	 */
	@Override
	public String getElementName(Object item)
	{
		return ((ISupportName)item).getName();
	}

	/**
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getItemsComparator()
	 */
	@Override
	protected Comparator< ? > getItemsComparator()
	{
		return new Comparator<ISupportName>()
		{

			public int compare(ISupportName o1, ISupportName o2)
			{
				return o1.getName().compareToIgnoreCase(o2.getName());
			}

		};
	}

	/**
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#validateItem(java.lang.Object)
	 */
	@Override
	protected IStatus validateItem(Object item)
	{
		return Status.OK_STATUS;
	}

	public static class Column implements ISupportName
	{
		private final String dataSource;
		private final String columnName;
		private final String name;

		public Column(String dataSource, String columnName)
		{
			this.dataSource = dataSource;
			this.columnName = columnName;
			this.name = columnName + " - " + dataSource;
		}

		/**
		 * @see com.servoy.j2db.persistence.ISupportName#getName()
		 */
		public String getName()
		{
			return name;
		}

		public String getColumnName()
		{
			return columnName;
		}

		/**
		 * @return
		 */
		public String getDataSource()
		{
			return dataSource;
		}
	}

	public static class Table implements ISupportName
	{
		private final String dataSource;
		private final String name;

		public Table(String dataSource)
		{
			this.dataSource = dataSource;
			String table = DataSourceUtils.getDataSourceTableName(dataSource);
			String server = DataSourceUtils.getDataSourceServerName(dataSource);
			if (table != null && server != null)
			{
				if (server.equals(IServer.INMEM_SERVER))
				{
					name = table + " - InMemory Server";
				}
				else if (server.equals(IServer.VIEW_SERVER))
				{
					name = table + " - VIEW Server";
				}
				else name = table + " - " + server;
			}
			else name = dataSource;
		}

		/**
		 * @see com.servoy.j2db.persistence.ISupportName#getName()
		 */
		public String getName()
		{
			return name;
		}

		/**
		 * @return
		 */
		public String getDataSource()
		{
			return dataSource;
		}
	}

	public static class Scope implements ISupportName
	{
		private final String scopeName;
		private final String solutionName;

		public Scope(String scopeName, String solutionName)
		{
			this.scopeName = scopeName;
			this.solutionName = solutionName;
		}

		/**
		 * @see com.servoy.j2db.persistence.ISupportName#getName()
		 */
		public String getName()
		{
			return scopeName + " - " + solutionName;
		}

		/**
		 * @return
		 */
		public String getSolutionName()
		{
			return solutionName;
		}

		/**
		 * @return
		 */
		public String getScopeName()
		{
			return scopeName;
		}
	}

	public static final class ServerScheduleRule implements ISchedulingRule
	{
		private final String serverName;

		/**
		 * @param serverName
		 */
		public ServerScheduleRule(String serverName)
		{
			this.serverName = serverName;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule)
		{
			return rule instanceof ServerScheduleRule ssr && ssr.serverName.equals(serverName);
		}

		@Override
		public boolean contains(ISchedulingRule rule)
		{
			return false;
		}
	}
}
