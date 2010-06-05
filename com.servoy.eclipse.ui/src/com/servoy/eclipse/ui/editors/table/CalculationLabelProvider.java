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
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;

public class CalculationLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider
{
	private final Table table;
	private final Color color;

	public CalculationLabelProvider(Table table, Color color)
	{
		this.table = table;
		this.color = color;
	}

	public Image getColumnImage(Object element, int columnIndex)
	{
		if (element instanceof ScriptCalculation)
		{
			if (columnIndex == CalculationsComposite.CI_STORED)
			{
				Column c = table.getColumn(((ScriptCalculation)element).getName());
				if (c != null)
				{
					return ColumnLabelProvider.TRUE_IMAGE;
				}
				else
				{
					return ColumnLabelProvider.FALSE_IMAGE;
				}
			}
		}
		else if (element instanceof Solution)
		{
			if (columnIndex == CalculationsComposite.CI_NAME)
			{
				return PlatformUI.getWorkbench().getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);
			}
		}
		return null;
	}

	public String getColumnText(Object element, int columnIndex)
	{
		if (element instanceof ScriptCalculation)
		{
			ScriptCalculation calculation = (ScriptCalculation)element;
			switch (columnIndex)
			{
				case CalculationsComposite.CI_NAME :
					return calculation.getName();
				case CalculationsComposite.CI_TYPE :
					return Column.getDisplayTypeString(Column.mapToDefaultType(calculation.getType()));
				case CalculationsComposite.CI_CALCULATION :
					return calculation.getMethodCode();
				case CalculationsComposite.CI_STORED :
					return "";
				default :
					return columnIndex + ": " + element;
			}
		}
		if (element instanceof Solution)
		{
			switch (columnIndex)
			{
				case CalculationsComposite.CI_NAME :
					return ((Solution)element).getName();
				default :
					return "";
			}
		}
		return element.toString();
	}

	public Color getBackground(Object element, int columnIndex)
	{
		if (element instanceof ScriptCalculation)
		{
			ScriptCalculation calculation = (ScriptCalculation)element;
			if (!calculation.getName().toLowerCase().equals(calculation.getName()))
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
