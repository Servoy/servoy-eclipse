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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.util.EditorUtil;
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

	private final IPersist persist;

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
		FormatDialog dialog = new FormatDialog(cellEditorWindow.getShell(), (String)getValue());
		dialog.open();
		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return TextDialogCellEditor.CANCELVALUE;
		}
		return dialog.getFormat();
	}

	private class FormatDialog extends Dialog
	{

		private String format;

		private IFormatTextContainer container = null;

		/**
		 * @param parent
		 */
		public FormatDialog(Shell parent, String format)
		{
			super(parent);
			this.format = format;
		}

		/**
		 * @param value
		 */
		public String getFormat()
		{
			return format;
		}

		@Override
		protected void configureShell(Shell shell)
		{
			super.configureShell(shell);
			shell.setText("Edit format property"); //$NON-NLS-1$
		}

		@Override
		protected boolean isResizable()
		{
			return true;
		}

		/**
		 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
		 */
		@SuppressWarnings("nls")
		@Override
		protected Control createDialogArea(Composite parent)
		{

			Composite composite = (Composite)super.createDialogArea(parent);
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
					try
					{
						IDataProvider dataProvider = dataproviderLookup.getDataProvider(dataProviderID);
						if (dataProvider != null)
						{
							int type = dataProvider.getDataProviderType();
							switch (type)
							{
								case IColumnTypes.DATETIME :
								{
									container = new FormatDateContainer(composite, SWT.NONE);
									((Composite)container).setLayoutData(new GridData(GridData.FILL_BOTH));
									container.setFormat(format);
									getShell().setText("Edit date format property");
									break;
								}
								case IColumnTypes.INTEGER :
								case IColumnTypes.NUMBER :
								{
									container = new FormatIntegerContainer(composite, SWT.NONE);
									((Composite)container).setLayoutData(new GridData(GridData.FILL_BOTH));
									container.setFormat(format);
									getShell().setText("Edit integer/number format property");
									break;
								}
								case IColumnTypes.TEXT :
								{
									container = new FormatTextContainer(composite, SWT.NONE);
									((Composite)container).setLayoutData(new GridData(GridData.FILL_BOTH));
									container.setFormat(format);
									getShell().setText("Edit text format property");
									break;
								}
							}
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
			if (container == null)
			{
				container = new FormatTextContainer(composite, SWT.NONE);
				((Composite)container).setLayoutData(new GridData(GridData.FILL_BOTH));
				container.setFormat(format);
			}
			return composite;
		}

		/**
		 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
		 */
		@Override
		protected void okPressed()
		{
			try
			{
				this.format = container.getFormat();
				super.okPressed();
			}
			catch (Exception e)
			{
				MessageDialog.openError(getShell(), "Error in format", e.getLocalizedMessage()); //$NON-NLS-1$
			}
		}

		@Override
		protected IDialogSettings getDialogBoundsSettings()
		{
			return EditorUtil.getDialogSettings("formatDialog"); //$NON-NLS-1$
		}
	}

	public static interface IFormatTextContainer
	{
		String getFormat();

		void setFormat(String format);
	}
}
