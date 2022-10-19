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

package com.servoy.eclipse.ui.editors.table;

import org.eclipse.core.databinding.observable.AbstractObservable;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.ChangeSupport;
import org.eclipse.core.databinding.observable.IChangeListener;

public class ChangeSupportObservable extends AbstractObservable
{
	private final ChangeSupport changeSupport;

	public ChangeSupportObservable(ChangeSupport changeSupport)
	{
		super(changeSupport.getRealm());
		this.changeSupport = changeSupport;
	}

	public ChangeSupport getChangeSupport()
	{
		return changeSupport;
	}

	@Override
	public void addChangeListener(IChangeListener listener)
	{
		changeSupport.addChangeListener(listener);
	}

	@Override
	public void removeChangeListener(IChangeListener listener)
	{
		changeSupport.removeChangeListener(listener);
	}

	public boolean isStale()
	{
		return false;
	}

	public void fireChangeEvent()
	{
		changeSupport.fireEvent(new ChangeEvent(this));
	}
}