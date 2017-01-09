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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import org.eclipse.jface.action.Action;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.FormHierarchyView;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author emera
 */
public abstract class AbstractFormHierarchyFilter extends Action
{

	protected IPersist selection;
	protected final FormHierarchyView view;
	boolean on;

	public AbstractFormHierarchyFilter(FormHierarchyView view, boolean initValue, String img, String text)
	{
		setImageDescriptor(Activator.loadImageDescriptorFromBundle(img));
		setText(text);
		setToolTipText(getText());
		this.view = view;
		setChecked(initValue);
		on = initValue;
	}

	@Override
	public void run()
	{
		if (on != isChecked())
		{
			on = isChecked();
			setFilter(on);
		}
		setChecked(on);
	}

	protected abstract void setFilter(boolean on);
}
