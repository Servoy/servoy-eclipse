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
package com.servoy.eclipse.ui.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenSqlEditorAction;
import com.servoy.j2db.util.Debug;

public class SolutionExplorerPreferences extends PreferencePage implements IWorkbenchPreferencePage
{
	private Button chAutomaticPerspectiveSwitch;
	private Button rdOpenFormEditor;
	private Button rdOpenFormScriptEditor;
	private Button rdExpandFormTree;
	private Button rdOpenGlobalScriptEditor;
	private Button rdExpandGlobalsTree;
	private Button rdVerticalAlignement;
	private Button rdHorizontalAlignement;
	private Button rdAutomaticAlignement;
	private Button navigationContextMenu;
	private Button treeHandlingContextMenu;
	private Button developerSolution;
	private Button chOpenAsDefaultAction;
	private Button chShowInheritedMethods;
	private Button chIncludeModules;

	public static final String FORM_DOUBLE_CLICK_ACTION = "formDblClickAction";
	public static final String GLOBALS_DOUBLE_CLICK_ACTION = "globalsDblClickAction";
	public static final String SOLEX_ALIGNEMENT = "solexAlignement";
	public static final String DOUBLE_CLICK_OPEN_FORM_EDITOR = "openFormEditor";
	public static final String DOUBLE_CLICK_OPEN_FORM_SCRIPT = "openFormScriptEditor";
	public static final String DOUBLE_CLICK_EXPAND_FORM_TREE = "expandFormTree";
	public static final String DOUBLE_CLICK_OPEN_GLOBAL_SCRIPT = "openGlobalScriptEditor";
	public static final String DOUBLE_CLICK_EXPAND_GLOBAL_TREE = "expandGlobalTree";

	private IDialogSettings solexDialogSettings;

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		solexDialogSettings = Activator.getDefault().getDialogSettings();

		Composite cp = new Composite(parent, SWT.NULL);

		// GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		cp.setLayout(layout);

		// GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		cp.setLayoutData(data);

		chAutomaticPerspectiveSwitch = new Button(cp, SWT.CHECK);
		chAutomaticPerspectiveSwitch.setText("Activate SQL Explorer perspective on 'Open SQL Editor'");

		Group solexFormGroup = new Group(cp, SWT.NONE);
		solexFormGroup.setText("Form node double click operation");
		solexFormGroup.setLayout(new GridLayout(1, true));

		rdOpenFormEditor = new Button(solexFormGroup, SWT.RADIO);
		rdOpenFormEditor.setText("Open Form Editor");

		rdOpenFormScriptEditor = new Button(solexFormGroup, SWT.RADIO);
		rdOpenFormScriptEditor.setText("Open Script Editor");

		rdExpandFormTree = new Button(solexFormGroup, SWT.RADIO);
		rdExpandFormTree.setText("Expand tree");

		Group solexGlobalsGroup = new Group(cp, SWT.NONE);
		solexGlobalsGroup.setText("Scopes node double click operation");
		solexGlobalsGroup.setLayout(new GridLayout(1, true));

		rdOpenGlobalScriptEditor = new Button(solexGlobalsGroup, SWT.RADIO);
		rdOpenGlobalScriptEditor.setText("Open Script Editor");

		rdExpandGlobalsTree = new Button(solexGlobalsGroup, SWT.RADIO);
		rdExpandGlobalsTree.setText("Expand tree");

		IEclipsePreferences store = InstanceScope.INSTANCE.getNode(Activator.getDefault().getBundle().getSymbolicName());

		String option = store.get(OpenSqlEditorAction.AUTOMATIC_SWITCH_PERSPECTIVE_PROPERTY, MessageDialogWithToggle.NEVER);
		chAutomaticPerspectiveSwitch.setSelection(MessageDialogWithToggle.ALWAYS.equals(option) ? true : false);

		option = store.get(FORM_DOUBLE_CLICK_ACTION, DOUBLE_CLICK_OPEN_FORM_EDITOR);
		if (DOUBLE_CLICK_OPEN_FORM_EDITOR.equals(option))
		{
			rdOpenFormEditor.setSelection(true);
		}
		else if (DOUBLE_CLICK_OPEN_FORM_SCRIPT.equals(option))
		{
			rdOpenFormEditor.setSelection(false);
		}
		else
		{
			rdExpandFormTree.setSelection(true);
		}

		if (DOUBLE_CLICK_OPEN_FORM_SCRIPT.equals(option))
		{
			rdOpenFormScriptEditor.setSelection(true);
		}
		else
		{
			rdOpenFormScriptEditor.setSelection(false);
		}

		option = store.get(GLOBALS_DOUBLE_CLICK_ACTION, DOUBLE_CLICK_OPEN_GLOBAL_SCRIPT);
		if (DOUBLE_CLICK_OPEN_GLOBAL_SCRIPT.equals(option))
		{
			rdOpenGlobalScriptEditor.setSelection(true);
		}
		else
		{
			rdExpandGlobalsTree.setSelection(true);
		}

		Group solexAlignementGroup = new Group(cp, SWT.NONE);
		solexAlignementGroup.setText("Solution Explorer Alignement");
		solexAlignementGroup.setLayout(new GridLayout(1, true));

		rdVerticalAlignement = new Button(solexAlignementGroup, SWT.RADIO);
		rdVerticalAlignement.setText("Align vertically the solution explorer view");

		rdHorizontalAlignement = new Button(solexAlignementGroup, SWT.RADIO);
		rdHorizontalAlignement.setText("Align horizontally the solution explorer view");

		rdAutomaticAlignement = new Button(solexAlignementGroup, SWT.RADIO);
		rdAutomaticAlignement.setText("Align automatically the solution explorer view");

		int solexOrientation = solexDialogSettings.getInt(SolutionExplorerView.DIALOGSTORE_VIEWORIENTATION);
		if (solexOrientation == SolutionExplorerView.VIEW_ORIENTATION_HORIZONTAL)
		{
			rdHorizontalAlignement.setSelection(true);
		}
		else if (solexOrientation == SolutionExplorerView.VIEW_ORIENTATION_VERTICAL)
		{
			rdVerticalAlignement.setSelection(true);
		}
		else
		{
			rdAutomaticAlignement.setSelection(true);
		}

		Group solexTreeViewGroup = new Group(cp, SWT.NONE);
		solexTreeViewGroup.setText("Solution Explorer Tree View");
		solexTreeViewGroup.setLayout(new GridLayout(1, true));

		navigationContextMenu = new Button(solexTreeViewGroup, SWT.CHECK);
		navigationContextMenu.setText("Show Navigation Group In Context Menu");

		treeHandlingContextMenu = new Button(solexTreeViewGroup, SWT.CHECK);
		treeHandlingContextMenu.setText("Show Tree Handling Group In Context Menu");

		developerSolution = new Button(solexTreeViewGroup, SWT.CHECK);
		developerSolution.setText("Show Developer Sotion node (experimental)");


		navigationContextMenu.setSelection(solexDialogSettings.getBoolean(SolutionExplorerView.DIALOGSTORE_CONTEXT_MENU_NAVIGATION));
		treeHandlingContextMenu.setSelection(solexDialogSettings.getBoolean(SolutionExplorerView.DIALOGSTORE_CONTEXT_MENU_TREE_HANDLING));
		developerSolution.setSelection(solexDialogSettings.getBoolean(SolutionExplorerView.INCLUDE_DEVELOPER_SOLUTION));

		Group solexListViewerGroup = new Group(cp, SWT.NONE);
		solexListViewerGroup.setText("Solution Explorer List Viewer Options");
		solexListViewerGroup.setLayout(new GridLayout(1, true));

		boolean listViewOption = solexDialogSettings.getBoolean(SolutionExplorerView.USE_OPEN_AS_DEFAULT);
		chOpenAsDefaultAction = new Button(solexListViewerGroup, SWT.CHECK);
		chOpenAsDefaultAction.setText("Use 'open' as default action");
		chOpenAsDefaultAction.setSelection(listViewOption);

		listViewOption = solexDialogSettings.getBoolean(SolutionExplorerView.SHOW_INHERITED_METHODS);
		chShowInheritedMethods = new Button(solexListViewerGroup, SWT.CHECK);
		chShowInheritedMethods.setText("Show inherited methods");
		chShowInheritedMethods.setSelection(listViewOption);

		listViewOption = solexDialogSettings.getBoolean(SolutionExplorerView.INCLUDE_ENTRIES_FROM_MODULES);
		chIncludeModules = new Button(solexListViewerGroup, SWT.CHECK);
		chIncludeModules.setText("Include modules");
		chIncludeModules.setSelection(listViewOption);

		return cp;
	}

	public void init(IWorkbench workbench)
	{

	}

	@Override
	public boolean performOk()
	{
		IEclipsePreferences store = InstanceScope.INSTANCE.getNode(Activator.getDefault().getBundle().getSymbolicName());
		if (chAutomaticPerspectiveSwitch.getSelection())
		{
			store.put(OpenSqlEditorAction.AUTOMATIC_SWITCH_PERSPECTIVE_PROPERTY, MessageDialogWithToggle.ALWAYS);
		}
		else
		{
			store.put(OpenSqlEditorAction.AUTOMATIC_SWITCH_PERSPECTIVE_PROPERTY, MessageDialogWithToggle.NEVER);
		}

		if (rdOpenFormEditor.getSelection())
		{
			store.put(FORM_DOUBLE_CLICK_ACTION, DOUBLE_CLICK_OPEN_FORM_EDITOR);
		}
		else if (rdOpenFormScriptEditor.getSelection())
		{
			store.put(FORM_DOUBLE_CLICK_ACTION, DOUBLE_CLICK_OPEN_FORM_SCRIPT);
		}
		else
		{
			store.put(FORM_DOUBLE_CLICK_ACTION, DOUBLE_CLICK_EXPAND_FORM_TREE);
		}

		solexDialogSettings.put(SolutionExplorerView.INCLUDE_DEVELOPER_SOLUTION, developerSolution.getSelection());

		if (rdOpenGlobalScriptEditor.getSelection())
		{
			store.put(GLOBALS_DOUBLE_CLICK_ACTION, DOUBLE_CLICK_OPEN_GLOBAL_SCRIPT);
		}
		else
		{
			store.put(GLOBALS_DOUBLE_CLICK_ACTION, DOUBLE_CLICK_EXPAND_GLOBAL_TREE);
		}

		IViewReference solexRef = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findViewReference(SolutionExplorerView.PART_ID);
		SolutionExplorerView solexView = null;
		if (solexRef != null)
		{
			solexView = (SolutionExplorerView)solexRef.getView(false);
		}

		if (rdHorizontalAlignement.getSelection())
		{
			if (solexView != null)
			{
				solexView.setOrientation(SolutionExplorerView.VIEW_ORIENTATION_HORIZONTAL);
			}
			else
			{
				solexDialogSettings.put(SolutionExplorerView.DIALOGSTORE_VIEWORIENTATION, SolutionExplorerView.VIEW_ORIENTATION_HORIZONTAL);
			}
		}
		else if (rdVerticalAlignement.getSelection())
		{
			if (solexView != null)
			{
				solexView.setOrientation(SolutionExplorerView.VIEW_ORIENTATION_VERTICAL);
			}
			else
			{
				solexDialogSettings.put(SolutionExplorerView.DIALOGSTORE_VIEWORIENTATION, SolutionExplorerView.VIEW_ORIENTATION_VERTICAL);
			}
		}
		else
		{
			if (solexView != null)
			{
				solexView.setOrientation(SolutionExplorerView.VIEW_ORIENTATION_AUTOMATIC);
			}
			else
			{
				solexDialogSettings.put(SolutionExplorerView.DIALOGSTORE_VIEWORIENTATION, SolutionExplorerView.VIEW_ORIENTATION_AUTOMATIC);
			}
		}

		if (solexView != null)
		{
			solexView.showContextMenuNavigationGroup(navigationContextMenu.getSelection());
		}
		else
		{
			solexDialogSettings.put(SolutionExplorerView.DIALOGSTORE_CONTEXT_MENU_NAVIGATION, navigationContextMenu.getSelection());
		}

		if (solexView != null)
		{
			solexView.showContextMenuTreeHandling(treeHandlingContextMenu.getSelection());
		}
		else
		{
			solexDialogSettings.put(SolutionExplorerView.DIALOGSTORE_CONTEXT_MENU_TREE_HANDLING, treeHandlingContextMenu.getSelection());
		}

		if (solexView != null)
		{
			solexView.setOpenAsDefaultOption(chOpenAsDefaultAction.getSelection());
		}
		else
		{
			solexDialogSettings.put(SolutionExplorerView.USE_OPEN_AS_DEFAULT, chOpenAsDefaultAction.getSelection());
		}

		if (solexView != null)
		{
			solexView.setShowInheritedMethods(chShowInheritedMethods.getSelection());
		}
		else
		{
			solexDialogSettings.put(SolutionExplorerView.SHOW_INHERITED_METHODS, chShowInheritedMethods.getSelection());
		}

		if (solexView != null)
		{
			solexView.setIncludeModulesOption(chIncludeModules.getSelection());
		}
		else
		{
			solexDialogSettings.put(SolutionExplorerView.INCLUDE_ENTRIES_FROM_MODULES, chIncludeModules.getSelection());
		}

		try
		{
			store.flush();
		}
		catch (BackingStoreException e)
		{
			Debug.log(e);
		}
		return super.performOk();
	}

	@Override
	protected void performDefaults()
	{
		super.performDefaults();
		chAutomaticPerspectiveSwitch.setSelection(false);
		rdOpenFormEditor.setSelection(true);
		rdOpenFormScriptEditor.setSelection(false);
		rdExpandFormTree.setSelection(false);
		rdOpenGlobalScriptEditor.setSelection(true);
		rdExpandGlobalsTree.setSelection(false);
		rdAutomaticAlignement.setSelection(true);
		rdVerticalAlignement.setSelection(false);
		rdHorizontalAlignement.setSelection(false);
		navigationContextMenu.setSelection(false);
		treeHandlingContextMenu.setSelection(false);
		chOpenAsDefaultAction.setSelection(true);
		chIncludeModules.setSelection(false);
		IViewReference solexRef = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findViewReference(SolutionExplorerView.PART_ID);
		SolutionExplorerView solexView = null;
		if (solexRef != null)
		{
			solexView = (SolutionExplorerView)solexRef.getView(false);
			solexView.setOrientation(SolutionExplorerView.VIEW_ORIENTATION_AUTOMATIC);
			solexView.setOpenAsDefaultOption(true);
			solexView.setShowInheritedMethods(true);
			solexView.setIncludeModulesOption(false);
			solexView.showContextMenuNavigationGroup(false);
			solexView.showContextMenuTreeHandling(false);
		}
		else
		{
			solexDialogSettings.put(SolutionExplorerView.DIALOGSTORE_VIEWORIENTATION, SolutionExplorerView.VIEW_ORIENTATION_AUTOMATIC);
			solexDialogSettings.put(SolutionExplorerView.USE_OPEN_AS_DEFAULT, true);
			solexDialogSettings.put(SolutionExplorerView.INCLUDE_ENTRIES_FROM_MODULES, false);
			solexDialogSettings.put(SolutionExplorerView.DIALOGSTORE_CONTEXT_MENU_NAVIGATION, false);
			solexDialogSettings.put(SolutionExplorerView.DIALOGSTORE_CONTEXT_MENU_TREE_HANDLING, false);
		}
	}

}
