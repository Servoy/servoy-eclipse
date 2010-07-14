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
package com.servoy.eclipse.core.util;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * A message dialog that allows the user to choose entries from a combo-box.
 * 
 * @author acostescu
 */
public class OptionDialog extends MessageDialog
{

	private final String[] options;
	private Combo combo;
	private final int defaultOptionsIndex;
	private int selectedOption;

	/**
	 * Create an option dialog. Note that the dialog will have no visual representation (no widgets) until it is told to open.
	 * <p>
	 * The <code>open</code> method will return the index of the label in this array corresponding to the button that was pressed to close the dialog. If the
	 * dialog was dismissed without pressing a button (ESC, etc.) then -1 is returned. Note that the <code>open</code> method blocks.
	 * </p>
	 * 
	 * @param parentShell the parent shell
	 * @param dialogTitle the dialog title, or <code>null</code> if none
	 * @param dialogTitleImage the dialog title image, or <code>null</code> if none
	 * @param dialogMessage the dialog message
	 * @param dialogImageType one of the following values:
	 *            <ul>
	 *            <li><code>MessageDialog.NONE</code> for a dialog with no image</li>
	 *            <li><code>MessageDialog.ERROR</code> for a dialog with an error image</li>
	 *            <li><code>MessageDialog.INFORMATION</code> for a dialog with an information image</li>
	 *            <li><code>MessageDialog.QUESTION </code> for a dialog with a question image</li>
	 *            <li><code>MessageDialog.WARNING</code> for a dialog with a warning image</li>
	 *            </ul>
	 * @param dialogButtonLabels an array of labels for the buttons in the button bar
	 * @param defaultIndex the index in the button label array of the default button
	 * @param options the options that will be displayed to the user in a combo.
	 * @param defaultOptionsIndex the index in the options array of the default entry
	 */
	public OptionDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels,
		int defaultIndex, String options[], int defaultOptionsIndex)
	{
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
		this.options = options;
		this.defaultOptionsIndex = defaultOptionsIndex;
		setBlockOnOpen(true);
	}

	@Override
	protected Control createCustomArea(Composite parent)
	{
		selectedOption = -1;

		combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		UIUtils.setDefaultVisibleItemCount(combo);
		combo.setItems(options);
		combo.select(defaultOptionsIndex);

		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		combo.setLayoutData(gridData);

		return super.createCustomArea(parent);
	}

	@Override
	public boolean close()
	{
		selectedOption = (combo != null ? combo.getSelectionIndex() : -1);
		return super.close();
	}

	/**
	 * Returns the selected index in the options array. -1 if there is no selected index.
	 * 
	 * @return the selected index in the options array. -1 if there is no selected index.
	 */
	public int getSelectedOption()
	{
		return selectedOption;
	}

}