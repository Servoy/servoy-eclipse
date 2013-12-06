/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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
package com.servoy.eclipse.designer.editor.mobile.commands;

import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.designer.editor.commands.DesignerSelectionActionDelegateHandler;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;

/**
 * Base class for actions that just work on the form, not on selected elements.
 * 
 * @author rgansevles
 */
public abstract class DesignerSelectionFormActionActionDelegateHandler extends DesignerSelectionActionDelegateHandler
{
	public DesignerSelectionFormActionActionDelegateHandler()
	{
		super(null);
	}

	@Override
	protected Command createCommand()
	{
		Object first = getSelection().getFirstElement();
		if (first == null) return null;

		if (first instanceof EditPart)
		{
			first = ((EditPart)first).getModel();
		}

		Form form = null;
		if (first instanceof FormElementGroup)
		{
			form = ((FormElementGroup)first).getParent();
		}
		else if (first instanceof MobileListModel)
		{
			form = ((MobileListModel)first).form;
		}
		else
		{
			IPersist persist = (IPersist)Platform.getAdapterManager().getAdapter(first, IPersist.class);
			if (persist != null)
			{
				form = (Form)persist.getAncestor(IRepository.FORMS);
			}
		}

		if (form == null) return null;

		return createCommand(form);
	}

	protected abstract Command createCommand(Form form);
}
