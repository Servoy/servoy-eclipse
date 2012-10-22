/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;

import com.servoy.eclipse.ui.editors.ITabbedEditor;
import com.servoy.j2db.persistence.IPersist;


/**
 * Multi-page form editor.
 * 
 * @author rgansevles
 *
 */
public class VisualFormEditor extends BaseVisualFormEditor implements ITabbedEditor
{
	public static final RequestType REQ_PLACE_TAB = new RequestType(RequestType.TYPE_TAB);
	public static final RequestType REQ_PLACE_PORTAL = new RequestType(RequestType.TYPE_PORTAL);
	public static final RequestType REQ_PLACE_MEDIA = new RequestType(RequestType.TYPE_MEDIA);
	public static final RequestType REQ_PLACE_BEAN = new RequestType(RequestType.TYPE_BEAN);
	public static final RequestType REQ_PLACE_BUTTON = new RequestType(RequestType.TYPE_BUTTON);
	public static final RequestType REQ_PLACE_FIELD = new RequestType(RequestType.TYPE_FIELD);
	public static final RequestType REQ_PLACE_LABEL = new RequestType(RequestType.TYPE_LABEL);
	public static final RequestType REQ_PLACE_RECT_SHAPE = new RequestType(RequestType.TYPE_SHAPE);
	public static final RequestType REQ_PLACE_TEMPLATE = new RequestType(RequestType.TYPE_TEMPLATE);
	public static final String REQ_DISTRIBUTE = "VFE_DISTRIBUTE"; //$NON-NLS-1$
	public static final String VFE_PAGE_ID = "PageID"; //$NON-NLS-1$

	private VisualFormEditorPartsPage partseditor = null;
	private VisualFormEditorTabSequencePage tabseditor = null;
	private VisualFormEditorSecurityPage seceditor = null;

	private final CommandStack dummyCommandStack = new CommandStack()
	{
		@Override
		public boolean canRedo()
		{
			return false;
		}

		@Override
		public boolean canUndo()
		{
			return false;
		}
	};


	@Override
	protected void createPages()
	{
		super.createPages();
		if (!isClosing())
		{
			createPartsPage();
			createTabsPage();
		}
		createSecPage(); // MultiPageEditorPart wants at least 1 page
	}

	private void createPartsPage()
	{
		partseditor = new VisualFormEditorPartsPage(this, getContainer(), SWT.NONE);
		setPageText(addPage(partseditor), "Parts");
	}

	private void createTabsPage()
	{
		tabseditor = new VisualFormEditorTabSequencePage(this, getContainer(), SWT.NONE);
		tabseditor.setData(VFE_PAGE_ID, ITabbedEditor.VFE_TABSEQ_PAGE);
		setPageText(addPage(tabseditor), "Tab sequence");
	}

	private void createSecPage()
	{
		seceditor = new VisualFormEditorSecurityPage(this, getContainer(), SWT.NONE);
		setPageText(addPage(seceditor), "Security");
	}

	@Override
	protected BaseVisualFormEditorDesignPage createGraphicaleditor()
	{
		return new VisualFormEditorDesignPage(this);
	}

	@Override
	public void dispose()
	{
		if (partseditor != null)
		{
			partseditor.dispose();
		}
		if (tabseditor != null)
		{
			tabseditor.dispose();
		}
		if (seceditor != null)
		{
			seceditor.dispose();
		}
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		seceditor.saveSecurityElements();
		super.doSave(monitor);
	}

	/**
	 * 
	 */
	@Override
	protected void doRefresh(List<IPersist> persists)
	{
		super.doRefresh(persists);
		if (partseditor != null)
		{
			partseditor.refresh();
		}
		if (tabseditor != null)
		{
			tabseditor.refresh();
		}
		if (seceditor != null)
		{
			seceditor.refresh();
		}
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (adapter.equals(CommandStack.class) && getActivePage() >= 0 && getControl(getActivePage()).equals(seceditor))
		{
			return dummyCommandStack;
		}
		return super.getAdapter(adapter);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.editors.TabbedEditor#changeActiveTab(java.lang.String)
	 */
	public void changeActiveTab(String tabName)
	{
		CTabItem[] items = ((CTabFolder)getContainer()).getItems();
		for (int i = 0; i < items.length; i++)
		{
			if (tabName.equals(items[i].getControl().getData(VFE_PAGE_ID)))
			{
				setActivePage(i);
				setFocus();
				return;
			}
		}
	}


}
