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
package com.servoy.eclipse.ui.labelproviders;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.core.repository.TableWrapper;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.TableContentProvider;

public class DatasourceLabelProvider extends LabelProvider
{
//	public static final DatasourceLabelProvider INSTANCE_IMAGE_FULLY_QUALIFIED = new DatasourceLabelProvider(Messages.LabelNone, true, true);
	public static final DatasourceLabelProvider INSTANCE_NO_IMAGE_FULLY_QUALIFIED = new DatasourceLabelProvider(Messages.LabelNone, false, true);
	public static final DatasourceLabelProvider INSTANCE_IMAGE_NAMEONLY = new DatasourceLabelProvider(Messages.LabelNone, true, false);
//	public static final DatasourceLabelProvider INSTANCE_NO_IMAGE_NAMEONLY = new DatasourceLabelProvider(Messages.LabelNone, false, false);

	private static final Image SERVER_IMAGE = Activator.getDefault().loadImageFromBundle("server.gif");
	private static final Image TABLE_IMAGE = Activator.getDefault().loadImageFromBundle("portal.gif");

	private final String defaultText;
	private final boolean showImage;
	private final boolean fullyQualifiedName;

	public DatasourceLabelProvider(String defaultText, boolean showImage, boolean fullyQualifiedName)
	{
		this.defaultText = defaultText;
		this.showImage = showImage;
		this.fullyQualifiedName = fullyQualifiedName;
	}

	@Override
	public String getText(Object value)
	{
		if (value == null || TableContentProvider.TABLE_NONE.equals(value))
		{
			return defaultText;
		}

		if (value instanceof TableWrapper)
		{
			TableWrapper tw = ((TableWrapper)value);
			if (tw.getTableName() == null)
			{
				// server
				return tw.getServerName();
			}
			if (fullyQualifiedName)
			{
				return tw.getServerName() + '.' + tw.getTableName();
			}
			return tw.getTableName();
		}

		return Messages.LabelUnresolved;
	}

	@Override
	public Image getImage(Object element)
	{
		if (showImage && !TableContentProvider.TABLE_NONE.equals(element) && element instanceof TableWrapper)
		{
			TableWrapper tw = ((TableWrapper)element);
			if (tw.getTableName() == null)
			{
				// server
				return SERVER_IMAGE;
			}
			return TABLE_IMAGE;
		}
		return super.getImage(element);
	}
}
