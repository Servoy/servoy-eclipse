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
package com.servoy.eclipse.ui.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Media;


/**
 * Read-only editor for media.
 * 
 * @author rob
 * 
 */
public class MediaViewer extends PersistEditor
{
	private MediaComposite mediaViewer;

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent)
	{
		mediaViewer = new MediaComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		refresh();
	}

	@Override
	protected boolean validatePersist(IPersist persist)
	{
		return persist instanceof Media;
	}

	@Override
	protected void doRefresh()
	{
		mediaViewer.setMedia((Media)getPersist());
	}

	/**
	 * Cannot edit media, view-only.
	 */
	@Override
	public boolean isDirty()
	{
		return false;
	}

	@Override
	public void setFocus()
	{
		mediaViewer.forceFocus();
	}

}
