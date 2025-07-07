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

import java.sql.Types;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.Utils;

public class ColumnLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider
{
	public static final String UUID_TEXT_36 = "UUID (Text(36))";
	public static final String UUID_MEDIA_16 = "UUID (Media(16))";
	public static final String UUID_NATIVE = "UUID (DB native)";
	public static final Image TRUE_IMAGE = Activator.getDefault().loadImageFromBundle("check_on.png");
	public static final Image FALSE_IMAGE = Activator.getDefault().loadImageFromBundle("check_off.png");
	public static final Image TRUE_RADIO = Activator.getDefault().loadImageFromBundle("radio_on.png");
	public static final Image FALSE_RADIO = Activator.getDefault().loadImageFromBundle("radio_off.png");

	private static final RGB GRAY = new RGB(127, 127, 127);
	private static final RGB GRAY2 = new RGB(160, 160, 160);

	private final Color color;
	private final ColumnComposite columnComposite;

	public ColumnLabelProvider(Color color, ColumnComposite columnComposite)
	{
		this.color = color;
		this.columnComposite = columnComposite;
	}

	public ColumnLabelProvider()
	{
		this(null, null);
	}

	public Image getColumnImage(Object element, int columnIndex)
	{
		int delta = columnComposite != null && columnComposite.isDataProviderIdDisplayed() ? 1 : 0;
		int deltaForDelete = columnComposite != null && columnComposite.isDataProviderIdDisplayed() ? 5 : 0;
		if (columnIndex == ColumnComposite.CI_ALLOW_NULL + delta)
		{
			return ((Column)element).getAllowNull() ? TRUE_IMAGE : FALSE_IMAGE;
		}
		if (columnIndex == ColumnComposite.CI_DELETE + deltaForDelete)
		{
			return Activator.getDefault().loadImageFromBundle("delete.png");
		}
		if (columnIndex == ColumnComposite.CI_EX_VALIDATION)
		{
			ColumnInfo columnInfo = ((Column)element).getColumnInfo();
			return columnInfo != null && columnInfo.getValidatorName() != null ? TRUE_IMAGE : FALSE_IMAGE;
		}
		if (columnIndex == ColumnComposite.CI_EX_CONVERSION)
		{
			ColumnInfo columnInfo = ((Column)element).getColumnInfo();
			return columnInfo != null && columnInfo.getConverterName() != null ? TRUE_IMAGE : FALSE_IMAGE;
		}
		return null;
	}

	public String getColumnText(Object element, int columnIndex)
	{
		Column info = (Column)element;
		ColumnType columnType = info.getConfiguredColumnType();
		if (columnIndex == ColumnComposite.CI_NAME)
		{
			return columnComposite != null && columnComposite.isDataProviderIdDisplayed() ? info.getSQLName() : info.getName();
		}
		int delta = columnComposite != null && columnComposite.isDataProviderIdDisplayed() ? 1 : 0;
		int deltaForDelete = columnComposite != null && columnComposite.isDataProviderIdDisplayed() ? 5 : 0;
		if ((columnIndex == ColumnComposite.CI_DELETE + deltaForDelete) || (columnIndex == ColumnComposite.CI_ALLOW_NULL + delta) ||
			columnIndex == ColumnComposite.CI_EX_VALIDATION || columnIndex == ColumnComposite.CI_EX_CONVERSION)
		{
			return "";
		}
		if (columnIndex == ColumnComposite.CI_TYPE + delta)
		{
			if (info.hasFlag(IBaseColumn.UUID_COLUMN))
			{
				if (info.hasFlag(IBaseColumn.NATIVE_COLUMN))
				{
					return UUID_NATIVE;
				}
				int type = Column.mapToDefaultType(info.getConfiguredColumnType().getSqlType());
				if (type == IColumnTypes.MEDIA)
				{
					return UUID_MEDIA_16;
				}
				return UUID_TEXT_36;
			}
			if ((columnType.getSqlType() == Types.OTHER || columnType.getSqlType() == Types.JAVA_OBJECT) &&
				!Utils.stringIsEmpty(info.getNativeTypename()))
			{
				return info.getNativeTypename() + " (native type)";
			}
			return Column.getDisplayTypeString(columnType.getSqlType());
		}
		if (columnIndex == ColumnComposite.CI_LENGTH + delta)
		{
			return columnType.getScale() > 0 ? columnType.getLength() + "," + columnType.getScale() : Integer.toString(columnType.getLength());
		}
		if (columnIndex == ColumnComposite.CI_ROW_IDENT + delta)
		{
			return Column.getFlagsString(info.getRowIdentType());
		}
		if (columnIndex == ColumnComposite.CI_SEQUENCE_TYPE + delta)
		{
			return ColumnInfo.getSeqDisplayTypeString(info.getSequenceType());
		}
		if (columnIndex == ColumnComposite.CI_EX_FORMAT)
		{
			ColumnInfo columnInfo = info.getColumnInfo();
			return columnInfo != null ? columnInfo.getDefaultFormat() : "";
		}
		if (columnIndex == ColumnComposite.CI_EX_AUTOENTER)
		{
			ColumnInfo columnInfo = info.getColumnInfo();
			return columnInfo != null ? columnInfo.getAutoEnterSubTypeString(columnInfo.getAutoEnterType(), columnInfo.getAutoEnterSubType()) : "";
		}
		if (columnIndex == ColumnComposite.CI_DATAPROVIDER_ID)
		{
			return info.getDataProviderID();
		}
		return columnIndex + ": " + element;
	}

	public Color getBackground(Object element, int columnIndex)
	{
		boolean isMobile = false;

		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		FlattenedSolution fs = servoyModel.getFlattenedSolution();
		if (fs != null && fs.getSolution() != null)
		{
			isMobile = fs.getSolution().getSolutionType() == SolutionMetaData.MOBILE;
		}

		if (element instanceof Column && ((Column)element).hasBadNaming(servoyModel.getNameValidator(), isMobile))
		{
			return color;
		}
		return null;
	}

	public Color getForeground(Object element, int columnIndex)
	{
		if (element instanceof Column)
		{
			Column info = (Column)element;
			if (columnIndex == ColumnComposite.CI_DATAPROVIDER_ID && columnComposite != null && columnComposite.isDataProviderIdDisplayed() &&
				Utils.equalObjects(info.getName(), info.getDataProviderID()))
			{
				return ColorResource.INSTANCE.getColor(GRAY);
			}
			if (info.getColumnInfo() != null && info.getColumnInfo().isExcluded())
			{
				return ColorResource.INSTANCE.getColor(GRAY2);
			}
		}
		return null;
	}
}