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

package com.servoy.eclipse.ui.dialogs.autowizard.nattable;

import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.ui.dialogs.TagsAndI18NTextDialog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.ITable;

/**
 * @author emera
 */
public class I18NTextDialogCellEditor extends NatTextDialogCellEditor
{
	private final PersistContext persistContext;
	private final ITable table;
	private final boolean hideTags;
	private final IApplication application;
	private final FlattenedSolution flattenedSolution;


	public I18NTextDialogCellEditor(boolean hideTags, PersistContext persistContext, IApplication application, ITable table,
		FlattenedSolution flattenedSolution, String title, Image icon)
	{
		super(title, icon);
		this.persistContext = persistContext;
		this.hideTags = hideTags;
		this.application = application;
		this.table = table;
		this.flattenedSolution = flattenedSolution;
	}


	@Override
	public void openDialog(Shell shell, String value)
	{
		TagsAndI18NTextDialog dialog = new TagsAndI18NTextDialog(shell, persistContext, flattenedSolution, table, value, title,
			application, hideTags);
		dialog.open();

		if (dialog.getReturnCode() != Window.CANCEL)
		{
			setEditorValue(dialog.getValue().toString());
		}
	}

	@Override
	public DisplayConverter getDisplayConverter()
	{
		return null;
	}

	@Override
	public Object getCanonicalValue(Object value)
	{
		return value;
	}
}
