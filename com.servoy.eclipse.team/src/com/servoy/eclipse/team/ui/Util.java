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
package com.servoy.eclipse.team.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class Util
{

	/*
	 * Create a checkbox field specific for this application
	 * 
	 * @param parent the parent of the new text field @return the new text field
	 */
	public static Button createCheckBox(Composite parent, String text)
	{
		Button checkBox = new Button(parent, SWT.CHECK);
	
		checkBox.setText(text);
		return checkBox;
	}

	/*
	 * Create a combo specific for this application
	 * 
	 * @param parent the parent of the new combo @return the new combo
	 */
	public static Combo createComboBox(Composite parent)
	{
		Combo combo = new Combo(parent, SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = GridData.CENTER;
		data.grabExcessVerticalSpace = false;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		combo.setLayoutData(data);
		return combo;
	}

	/*
	 * Creates composite control and sets the default layout data.
	 * 
	 * @param parent the parent of the new composite @param numColumns the number of columns for the new composite @return the newly-created coposite
	 */
	public static Composite createComposite(Composite parent, int numColumns)
	{
		Composite composite = new Composite(parent, SWT.NULL);
	
		// GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		composite.setLayout(layout);
	
		// GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		return composite;
	}

	/*
	 * Utility method that creates a label instance and sets the default layout data.
	 * 
	 * @param parent the parent for the new label @param text the text for the new label @return the new label
	 */
	public static Label createLabel(Composite parent, String text)
	{
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}

	/*
	 * Create a text field specific for this application
	 * 
	 * @param parent the parent of the new text field @return the new text field
	 */
	public static Text createPasswordField(Composite parent)
	{
		Text text = new Text(parent, SWT.PASSWORD | SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = GridData.CENTER;
		data.grabExcessVerticalSpace = false;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		text.setLayoutData(data);
		return text;
	}

	/*
	 * Create a text field specific for this application
	 * 
	 * @param parent the parent of the new text field @return the new text field
	 */
	public static Text createTextField(Composite parent)
	{
		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = GridData.CENTER;
		data.grabExcessVerticalSpace = false;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		text.setLayoutData(data);
		return text;
	}

}
