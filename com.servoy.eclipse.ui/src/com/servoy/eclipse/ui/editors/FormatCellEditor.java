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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * @author jcompagner
 *
 */
public class FormatCellEditor extends TextDialogCellEditor
{

	final IPersist persist;

	/**
	 * @param parent
	 * @param persist 
	 */
	public FormatCellEditor(Composite parent, IPersist persist)
	{
		super(parent, SWT.NONE, null);
		this.persist = persist;
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#openDialogBox(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{
		int type = IColumnTypes.TEXT;
		if (persist instanceof ISupportDataProviderID)
		{
			String dataProviderID = ((ISupportDataProviderID)persist).getDataProviderID();

			ISupportChilds supportChilds = persist.getParent();
			while (supportChilds != null && !(supportChilds instanceof Form))
			{
				supportChilds = supportChilds.getParent();
			}
			if (supportChilds instanceof Form)
			{
				Form form = (Form)supportChilds;
				IDataProviderLookup dataproviderLookup = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getDataproviderLookup(
					null, form);
				ComponentFormat componentFormat = null;
				if (persist instanceof Field)
				{
					componentFormat = ComponentFormat.getComponentFormat(((Field)persist).getFormat(), dataProviderID, dataproviderLookup,
						Activator.getDefault().getDesignClient());

				}
				if (componentFormat != null)
				{
					type = componentFormat.dpType;
				}
				else
				{
					try
					{
						IDataProvider dataProvider = dataproviderLookup.getDataProvider(dataProviderID);
						if (dataProvider != null)
						{
							type = dataProvider.getDataProviderType();
						}
					}
					catch (RepositoryException re)
					{
						ServoyLog.logError(re);
					}
				}
			}
		}
		FormatDialog dialog = new FormatDialog(cellEditorWindow.getShell(), (String)getValue(), type);
		dialog.open();
		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return TextDialogCellEditor.CANCELVALUE;
		}
		return dialog.getFormatString();
	}
}
