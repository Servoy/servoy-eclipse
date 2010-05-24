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
package com.servoy.eclipse.ui.editors.table;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;

public class ColumnLabelProvider extends LabelProvider implements ITableLabelProvider, ITableFontProvider, ITableColorProvider
{
	public static final Image TRUE_IMAGE = Activator.getDefault().loadImageFromBundle("chk_on_icon.gif"); //$NON-NLS-1$
	public static final Image FALSE_IMAGE = Activator.getDefault().loadImageFromBundle("chk_off_icon.gif"); //$NON-NLS-1$
	public static final Image TRUE_RADIO = Activator.getDefault().loadImageFromBundle("radio_on.gif"); //$NON-NLS-1$
	public static final Image FALSE_RADIO = Activator.getDefault().loadImageFromBundle("radio_off.gif"); //$NON-NLS-1$

	private Color color = null;

	public ColumnLabelProvider(Color color)
	{
		super();
		this.color = color;
	}

	public ColumnLabelProvider()
	{
		super();
		this.color = null;
	}

	public Image getColumnImage(Object element, int columnIndex)
	{
		if (columnIndex == ColumnComposite.CI_ALLOW_NULL)
		{
			if (((Column)element).getAllowNull())
			{
				return TRUE_IMAGE;
			}
			else
			{
				return FALSE_IMAGE;
			}
		}
		else
		{
			return null;
		}

	}

	public String getColumnText(Object element, int columnIndex)
	{
		Column info = (Column)element;
		switch (columnIndex)
		{
			case ColumnComposite.CI_NAME :
				return info.getName();
			case ColumnComposite.CI_TYPE :
				return info.getTypeAsString();
			case ColumnComposite.CI_LENGHT :
				return info.getScale() > 0 ? info.getLength() + "," + info.getScale() : Integer.toString(info.getLength());
			case ColumnComposite.CI_ROW_IDENT :
				return Column.getFlagsString(info.getRowIdentType());
			case ColumnComposite.CI_ALLOW_NULL :
				return "";
				//return (info.getAllowNull() ? "yes" : "no");
			case ColumnComposite.CI_SEQUENCE_TYPE :
				return ColumnInfo.getSeqDisplayTypeString(info.getSequenceType());
			default :
				return columnIndex + ": " + element;
		}
	}

	public Font getFont(Object element, int columnIndex)
	{
//		ParameterInfo info = (ParameterInfo)element;
//		if (info.isAdded()) return JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
//		else 
		return null;
	}

	public Color getBackground(Object element, int columnIndex)
	{
		if (element instanceof Column)
		{
			Column column = (Column)element;
			if (column.hasBadNaming())
			{
				return color;
			}
		}
		return null;
	}

	public Color getForeground(Object element, int columnIndex)
	{
		return null;
	}
}