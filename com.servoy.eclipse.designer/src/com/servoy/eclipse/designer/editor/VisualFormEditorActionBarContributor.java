/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.designer.editor;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @author jcompagner
 *
 */
public class VisualFormEditorActionBarContributor extends MultiPageEditorActionBarContributor
{

	protected IEditorActionBarContributor designViewerActionBarContributor = null;
	protected IEditorActionBarContributor sourceViewerActionContributor = null;
	protected MultiPageEditorPart multiPageEditor = null;

	public VisualFormEditorActionBarContributor()
	{
		super();

		sourceViewerActionContributor = new TextEditorActionContributor();

		designViewerActionBarContributor = new ActionBarContributor();

	}

	@Override
	public void init(IActionBars actionBars)
	{
		super.init(actionBars);

		if (actionBars != null)
		{
			initDesignViewerActionBarContributor(actionBars);
			initSourceViewerActionContributor(actionBars);
		}
	}

	protected void initDesignViewerActionBarContributor(IActionBars actionBars)
	{
		designViewerActionBarContributor.init(actionBars, getPage());
	}

	protected void initSourceViewerActionContributor(IActionBars actionBars)
	{
		sourceViewerActionContributor.init(actionBars, getPage());
	}

	@Override
	public void dispose()
	{
		super.dispose();

		designViewerActionBarContributor.dispose();
		sourceViewerActionContributor.dispose();
		multiPageEditor = null;
	}

	/**
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	@Override
	public final void contributeToMenu(IMenuManager menu)
	{
		super.contributeToMenu(menu);
	}

	protected void addToMenu(IMenuManager menu)
	{
	}

	/**
	 * @see IExtendedContributor#contributeToPopupMenu(IMenuManager)
	 */
	public final void contributeToPopupMenu(IMenuManager menu)
	{
		addToPopupMenu(menu);
	}

	protected void addToPopupMenu(IMenuManager menu)
	{
	}

	/**
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToToolBar(IToolBarManager)
	 */
	@Override
	public final void contributeToToolBar(IToolBarManager toolBarManager)
	{
		super.contributeToToolBar(toolBarManager);

		addToToolBar(toolBarManager);
	}

	protected void addToToolBar(IToolBarManager toolBarManager)
	{
	}

	/**
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToStatusLine(IStatusLineManager)
	 */
	@Override
	public final void contributeToStatusLine(IStatusLineManager manager)
	{
		super.contributeToStatusLine(manager);

		addToStatusLine(manager);
	}

	protected void addToStatusLine(IStatusLineManager manager)
	{
	}

	/**
	 * @see IExtendedContributor#updateToolbarActions()
	 */
	public void updateToolbarActions()
	{
	}

	@Override
	public void setActiveEditor(IEditorPart targetEditor)
	{
		if (targetEditor instanceof MultiPageEditorPart)
		{
			multiPageEditor = (MultiPageEditorPart)targetEditor;
		}

		super.setActiveEditor(targetEditor);

		updateToolbarActions();
	}

	@Override
	public void setActivePage(IEditorPart activeEditor)
	{
		if (activeEditor == null) return;
		// This contributor is designed for StructuredTextMultiPageEditorPart.
		// To safe-guard this from problems caused by unexpected usage by
		// other editors, the following
		// check is added.
		if (multiPageEditor != null)
		{
			if (activeEditor instanceof ITextEditor)
			{
				activateSourcePage(activeEditor);
			}
			else
			{
				activateDesignPage(activeEditor);
			}
		}

		updateToolbarActions();

		IActionBars actionBars = getActionBars();
		if (actionBars != null)
		{
			// update menu bar and tool bar
			actionBars.updateActionBars();
		}
	}

	protected void activateDesignPage(IEditorPart activeEditor)
	{
		designViewerActionBarContributor.setActiveEditor(activeEditor);
	}

	protected void activateSourcePage(IEditorPart activeEditor)
	{
		sourceViewerActionContributor.setActiveEditor(activeEditor);
	}

}
