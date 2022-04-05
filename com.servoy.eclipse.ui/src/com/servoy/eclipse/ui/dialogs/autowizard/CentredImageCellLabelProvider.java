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

package com.servoy.eclipse.ui.dialogs.autowizard;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;

public abstract class CentredImageCellLabelProvider extends OwnerDrawLabelProvider
{
	public CentredImageCellLabelProvider()
	{
		super();
	}

	@Override
	protected void measure(Event event, Object element)
	{
		// No action
		event.height = 40;
	}

	@Override
	protected void erase(Event event, Object element)
	{
		// Don't call super.erase() to suppress non-standard selection draw
	}

	@Override
	protected void paint(Event event, Object element)
	{
		TableItem item = (TableItem)event.item;

		Rectangle itemBounds = item.getBounds(event.index);

		GC gc = event.gc;

		Image image = getImage(element);

		Rectangle imageBounds = image.getBounds();

		int x = event.x + Math.max(0, (itemBounds.width - imageBounds.width) / 2);
		int y = event.y + Math.max(0, (itemBounds.height - imageBounds.height) / 2);

		gc.drawImage(image, x, y);
	}

	protected abstract Image getImage(Object element);
}