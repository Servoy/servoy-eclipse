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

package com.servoy.eclipse.core.util;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

/**
 * @author jcomp
 *
 */
public class ServoyMessageDialog extends MessageDialog
{

	static String[] getButtonLabels(int kind)
	{
		String[] dialogButtonLabels;
		switch (kind)
		{
			case ERROR :
			case INFORMATION :
			case WARNING :
			{
				dialogButtonLabels = new String[] { IDialogConstants.OK_LABEL };
				break;
			}
			case CONFIRM :
			{
				dialogButtonLabels = new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };
				break;
			}
			case QUESTION :
			{
				dialogButtonLabels = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };
				break;
			}
			case QUESTION_WITH_CANCEL :
			{
				dialogButtonLabels = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL };
				break;
			}
			default :
			{
				throw new IllegalArgumentException("Illegal value for kind in MessageDialog.open()"); //$NON-NLS-1$
			}
		}
		return dialogButtonLabels;
	}

	/**
	 * Convenience method to open a simple confirm (OK/Cancel) dialog.
	 *
	 * @param parent  the parent shell of the dialog, or <code>null</code> if none
	 * @param title   the dialog's title, or <code>null</code> if none
	 * @param message the message
	 * @return <code>true</code> if the user presses the OK button,
	 *         <code>false</code> otherwise
	 */
	public static boolean openConfirm(Shell parent, String title, String message)
	{
		return open(CONFIRM, parent, title, message, SWT.NONE);
	}

	/**
	 * Convenience method to open a standard error dialog.
	 *
	 * @param parent  the parent shell of the dialog, or <code>null</code> if none
	 * @param title   the dialog's title, or <code>null</code> if none
	 * @param message the message
	 */
	public static void openError(Shell parent, String title, String message)
	{
		open(ERROR, parent, title, message, SWT.NONE);
	}

	/**
	 * Convenience method to open a standard information dialog.
	 *
	 * @param parent  the parent shell of the dialog, or <code>null</code> if none
	 * @param title   the dialog's title, or <code>null</code> if none
	 * @param message the message
	 */
	public static void openInformation(Shell parent, String title, String message)
	{
		open(INFORMATION, parent, title, message, SWT.NONE);
	}

	/**
	 * Convenience method to open a simple Yes/No question dialog.
	 *
	 * @param parent  the parent shell of the dialog, or <code>null</code> if none
	 * @param title   the dialog's title, or <code>null</code> if none
	 * @param message the message
	 * @return <code>true</code> if the user presses the Yes button,
	 *         <code>false</code> otherwise
	 */
	public static boolean openQuestion(Shell parent, String title, String message)
	{
		return open(QUESTION, parent, title, message, SWT.NONE);
	}

	/**
	 * Convenience method to open a standard warning dialog.
	 *
	 * @param parent  the parent shell of the dialog, or <code>null</code> if none
	 * @param title   the dialog's title, or <code>null</code> if none
	 * @param message the message
	 */
	public static void openWarning(Shell parent, String title, String message)
	{
		open(WARNING, parent, title, message, SWT.NONE);
	}

	public static boolean open(int kind, Shell parent, String title, String message, int style)
	{
		ServoyMessageDialog dialog = new ServoyMessageDialog(parent, title, null, message, kind, 0, getButtonLabels(kind));
		style &= SWT.SHEET;
		dialog.setShellStyle(dialog.getShellStyle() | style);
		return dialog.open() == 0;
	}


	/**
	 * @param parent
	 * @param title
	 * @param object
	 * @param message
	 * @param kind
	 * @param i
	 * @param buttonLabels
	 */
	private ServoyMessageDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage,
		int dialogImageType, int defaultIndex, String... dialogButtonLabels)
	{
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, defaultIndex, dialogButtonLabels);
	}

	@Override
	protected void setShellStyle(int newShellStyle)
	{
		super.setShellStyle((newShellStyle & ~SWT.APPLICATION_MODAL) | SWT.PRIMARY_MODAL);
	}
}
