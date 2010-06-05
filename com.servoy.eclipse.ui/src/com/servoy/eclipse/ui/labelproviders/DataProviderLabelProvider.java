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

import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.property.DataProviderConverter;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;

/**
 * Textual representation for {@link IDataProvider}.
 * <p>
 * dataProvider may be IDataProvider or String.
 * 
 * @author rob
 * 
 */
public class DataProviderLabelProvider extends LabelProvider implements IFontProvider, IPersistLabelProvider
{
	public static final DataProviderLabelProvider INSTANCE_HIDEPREFIX = new DataProviderLabelProvider(true);
	public static final DataProviderLabelProvider INSTANCE_SHOWPREFIX = new DataProviderLabelProvider(false);
	private final boolean hidePrefix;
	private DataProviderConverter converter;

	public DataProviderLabelProvider(boolean hidePrefix)
	{
		this.hidePrefix = hidePrefix;
	}


	@Override
	public String getText(Object dataProvider)
	{
		if (dataProvider == null || dataProvider == DataProviderContentProvider.NONE)
		{
			return Messages.LabelNone;
		}

		if (dataProvider instanceof String)
		{
			if (converter != null)
			{
				IDataProvider provider = converter.convertProperty(null, (String)dataProvider);
				if (provider == null) return UnresolvedValue.getUnresolvedMessage((String)dataProvider);
			}
			return hidePrefix((String)dataProvider);
		}

		if (hidePrefix && dataProvider instanceof ColumnWrapper)
		{
			return ((ColumnWrapper)dataProvider).getColumn().getDataProviderID();
		}

		if (dataProvider instanceof IDataProvider)
		{
			return hidePrefix(((IDataProvider)dataProvider).getDataProviderID());
		}

		return Messages.LabelUnresolved;
	}

	/**
	 * Hide the prefix (everything before the last dot) if requested.
	 * 
	 * @param text
	 * @return
	 */
	protected String hidePrefix(String text)
	{
		if (hidePrefix)
		{
			int dot = text.lastIndexOf('.');
			if (dot >= 0)
			{
				return text.substring(dot + 1);
			}
		}
		return text;
	}

	public Font getFont(Object value)
	{
		if (value == null || value == DataProviderContentProvider.NONE)
		{
			return FontResource.getDefaultFont(SWT.BOLD, -1);
		}
		return FontResource.getDefaultFont(SWT.NONE, 0);
	}

	public IPersist getPersist(Object value)
	{
		if (value instanceof IPersist)
		{
			return (IPersist)value;
		}
		return null;
	}

	public DataProviderConverter getConverter()
	{
		return converter;
	}


	public void setConverter(DataProviderConverter converter)
	{
		this.converter = converter;
	}
}
