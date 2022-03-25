/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IViewPart;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Media;

/**
 * @author vidmarian
 *
 */
public class EditComponentVariantsAction extends Action implements ISelectionChangedListener
{

	private Media componentVariant = null;
	private final IViewPart viewPart;
	private boolean enabled = false;

	/**
	 * Creates a new edit variable action that will use the given shell to show the edit variable dialog.
	 *
	 * @param shell used to show a dialog.
	 */
	public EditComponentVariantsAction(IViewPart viewPart)
	{
		this.viewPart = viewPart;

		setText(Messages.EditComponentVariantsAction_editVariants);
		setToolTipText(Messages.EditComponentVariantsAction_editVariants);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		componentVariant = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean ok = (sel.size() == 1);
		if (ok)
		{
			SimpleUserNode un = (SimpleUserNode)sel.getFirstElement();
			if (un.getType() == UserNodeType.MEDIA_IMAGE && un.getName().startsWith("styles_wizard"))
			{

				ok = true;
				enabled = true;
				componentVariant = (Media)un.getRealObject();
			}
			else
			{
				ok = false;
				enabled = false;
				componentVariant = null;
			}
		}
		else
		{
			enabled = false;
		}

		setEnabled(ok);

	}

	@Override
	public boolean isEnabled()
	{
		return enabled;
	}

	@Override
	public void run()
	{
		EditorUtil.openComponentVariantsEditor("themeAndVariantsEditor=true");
	}
}
