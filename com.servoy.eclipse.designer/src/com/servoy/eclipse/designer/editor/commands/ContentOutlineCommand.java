/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.views.contentoutline.ContentOutline;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.outline.FormOutlinePage;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author gboros
 *
 */
public abstract class ContentOutlineCommand extends AbstractHandler implements IHandler
{
	/**
	 * @return
	 */
	protected List<IPersist> getSelection()
	{
		ArrayList<IPersist> result = new ArrayList<IPersist>();
		for (Object next : getSelectionList())
		{
			if (next instanceof WebFormComponentChildType) continue;
			if (next instanceof PersistContext)
			{
				PersistContext persistContext = (PersistContext)next;
				if (persistContext.getPersist() != null && !(persistContext.getPersist() instanceof WebFormComponentChildType))
					result.add(persistContext.getPersist());
			}
			else if (next instanceof IPersist)
			{
				result.add((IPersist)next);
			}
		}
		return result;
	}

	protected List<Object> getSelectionList()
	{
		ISelection viewSelection = getViewSelection();
		if (viewSelection != null)
		{
			return ((IStructuredSelection)viewSelection).toList();
		}
		return new ArrayList<Object>();
	}

	protected ISelection getViewSelection()
	{
		ISelection selection = null;
		ContentOutline contentOutline = DesignerUtil.getContentOutline();
		if (contentOutline != null)
		{
			selection = contentOutline.getSelection();
		}
		return selection;
	}

	protected FormOutlinePage getFormOutline()
	{
		ContentOutline contentOutline = DesignerUtil.getContentOutline();
		if (contentOutline != null)
		{
			IPage outline = contentOutline.getCurrentPage();
			if (outline instanceof FormOutlinePage formOutlinePage)
			{
				return formOutlinePage;
			}
		}
		return null;
	}

	protected BaseVisualFormEditor getEditorPart()
	{
		IWorkbenchWindow active = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (active == null) return null;
		IWorkbenchPage page = active.getActivePage();
		if (page == null) return null;
		IWorkbenchPart part = page.getActiveEditor();
		if (part instanceof BaseVisualFormEditor) return (BaseVisualFormEditor)part;
		return null;
	}
}
