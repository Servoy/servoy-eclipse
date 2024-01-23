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
package com.servoy.eclipse.ui;

import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;

import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.wizards.NewFormWizard;
import com.servoy.eclipse.ui.wizards.NewMethodWizard;
import com.servoy.eclipse.ui.wizards.NewRelationWizard;
import com.servoy.eclipse.ui.wizards.NewServerWizard;
import com.servoy.eclipse.ui.wizards.NewSolutionWizard;
import com.servoy.eclipse.ui.wizards.NewStyleWizard;
import com.servoy.eclipse.ui.wizards.NewValueListWizard;

public class DesignPerspective implements IPerspectiveFactory
{

	public static final String TestRunnerViewPart_NAME = "org.eclipse.jdt.junit.ResultView"; //this field is copied from TestRunnerViewPart.NAME which is an eclipse internal class and cannot be referenced.
	public static final String CallHierarchyViewPart_ID = "org.eclipse.dltk.callhierarchy.view"; //this field is copied from CallHierarchyViewPart.ID_CALL_HIERARCHY

	@SuppressWarnings("restriction")
	public void createInitialLayout(IPageLayout layout)
	{
		String editorArea = layout.getEditorArea();
		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.29f, editorArea);//0.21f
		left.addView(SolutionExplorerView.PART_ID);
//		left.addView(IPageLayout.ID_RES_NAV);//move to synchronize perspective only

		IFolderLayout rightmost = layout.createFolder("rightmost", IPageLayout.RIGHT, 0.65f, editorArea);
		rightmost.addPlaceholder("org.eclipse.help.ui.HelpView");

		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.65f, editorArea);//0.8f
		right.addView(IPageLayout.ID_OUTLINE);
		right.addView(IPageLayout.ID_PROP_SHEET);
//		right.addPlaceholder("org.eclipse.help.ui.HelpView");

		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.8f, editorArea);
		bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
		bottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);//move to debug perspective only
		bottom.addView(IPageLayout.ID_TASK_LIST);
		bottom.addView(IPageLayout.ID_BOOKMARKS);
		bottom.addView("com.servoy.eclipse.debug.scriptingconsole");
		bottom.addView(NewSearchUI.SEARCH_VIEW_ID);
		bottom.addPlaceholder(CallHierarchyViewPart_ID);

		//maximize window on first launch, it start too small first time
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page == null || page.getPerspective() == null || !page.getPerspective().getId().toString().equals("com.servoy.eclipse.ui.DesignPerspective"))
		{
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setMaximized(true);
		}

		setContentsOfShowViewMenu(layout);
		addNewWizards(layout);

		layout.addPerspectiveShortcut("org.eclipse.debug.ui.DebugPerspective");
	}

	protected void setContentsOfShowViewMenu(IPageLayout layout)
	{
		layout.addShowViewShortcut(SolutionExplorerView.PART_ID);
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
		layout.addShowViewShortcut(TestRunnerViewPart_NAME);
	}

	protected void addNewWizards(IPageLayout layout)
	{
		layout.addNewWizardShortcut(NewServerWizard.ID);
		layout.addNewWizardShortcut(NewFormWizard.ID);
		layout.addNewWizardShortcut(NewMethodWizard.ID);
		layout.addNewWizardShortcut(NewRelationWizard.ID);
		layout.addNewWizardShortcut(NewSolutionWizard.ID);
		layout.addNewWizardShortcut(NewStyleWizard.ID);
		layout.addNewWizardShortcut(NewValueListWizard.ID);
	}
}
