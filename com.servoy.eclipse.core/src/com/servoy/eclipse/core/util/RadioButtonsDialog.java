/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Diana
 * Class that will create a dialog containing radio buttons, also including show/hide advanced settings.
 */
public class RadioButtonsDialog extends Dialog
{

	private final List<Button> radioButtons = new ArrayList<Button>();
	private List<String> radioButtonsTexts = new ArrayList<String>();
	private final String dialogTitle;
	private Button selectedButton;
	private ExpandItem collapsableItem;
	private Composite topLevel;
	private Composite expandComposite;
	private ExpandBar expandBar;
	SelectionListener selectionListener;

	/**
	 * @param parentShell
	 * @param radioButtonsTexts
	 * @param dialogTitle
	 */
	public RadioButtonsDialog(Shell parentShell, List<String> radioButtonsTexts, String dialogTitle)
	{
		super(parentShell);
		setShellStyle(SWT.TITLE);
		this.dialogTitle = dialogTitle;
		this.radioButtonsTexts = radioButtonsTexts;
	}

	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText(dialogTitle);
	}

	@Override
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton)
	{
		if (id == IDialogConstants.CANCEL_ID) return null;
		return super.createButton(parent, id, label, defaultButton);
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite area = (Composite)super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(1, false);
		container.setLayout(layout);
		createRadioButtonListToDisplay(container);
		return area;
	}

	@Override
	protected boolean canHandleShellCloseEvent()
	{
		return false;
	}

	private void createRadioButtonListToDisplay(Composite container)
	{
		selectionListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				Button radioButtonSelected = ((Button)event.widget);
				if (radioButtonSelected.getSelection())
				{
					selectedButton = radioButtonSelected;
					for (Button radioButton : radioButtons)
					{
						if (radioButton.getSelection() && !radioButton.getText().equals(selectedButton.getText()))
						{
							radioButton.setSelection(false);
						}
					}
				}
			}
		};

		// top level group
		topLevel = container;

		Button radioButton = new Button(topLevel, SWT.RADIO);
		selectedButton = radioButton;
		radioButton.setSelection(true);
		radioButton.setText(radioButtonsTexts.get(0));
		radioButton.addSelectionListener(selectionListener);
		radioButtons.add(radioButton);

		addAdvancedSettings();
	}

	public void addAdvancedSettings()
	{
		expandBar = new ExpandBar(topLevel, SWT.NONE);
		expandBar.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		expandBar.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));

		expandComposite = new Composite(expandBar, SWT.NONE);
		GridLayout layout = new GridLayout();
		expandComposite.setLayout(layout);


		for (int i = 1; i < radioButtonsTexts.size(); i++)
		{
			Button radioButton = new Button(expandComposite, SWT.RADIO);
			radioButton.setText(radioButtonsTexts.get(i));
			radioButton.addSelectionListener(selectionListener);
			radioButtons.add(radioButton);
		}

		collapsableItem = new ExpandItem(expandBar, SWT.NONE, 0);
		collapsableItem.setHeight(expandComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		collapsableItem.setControl(expandComposite);
		expandBar.addExpandListener(new ExpandListener()
		{
			public void itemExpanded(ExpandEvent e)
			{
				collapsableItem.setText("Hide advanced install database settings");
				resizeDialog();
			}

			public void itemCollapsed(ExpandEvent e)
			{
				collapsableItem.setText("Show advanced install database settings");
				resizeDialog();
			}
		});

		collapsableItem.setExpanded(false);
		if (collapsableItem.getExpanded())
		{
			Shell shell = getShell();
			shell.getDisplay().asyncExec(() -> {
				//center dialog vertically
				Rectangle parentSize = shell.getParent().getBounds();
				Rectangle bounds = shell.getBounds();
				bounds.y = (parentSize.height - bounds.height) / 2 + parentSize.y;
				shell.setBounds(bounds);
			});
		}
		collapsableItem.setText(collapsableItem.getExpanded() ? "Hide advanced install database settings" : "Show advanced install database settings");

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gridData.minimumWidth = 550; // needs to be wide
		expandBar.setLayoutData(gridData);

	}

	private void resizeDialog()
	{
		Shell shell = getShell();
		shell.getDisplay().asyncExec(() -> {

			Point preferredSize = shell.computeSize(shell.getSize().x, SWT.DEFAULT, true);
			if (!collapsableItem.getExpanded())
			{
				//if the help page is visible, we don't shrink
				return;
			}

			Rectangle bounds = shell.getBounds();
			bounds.height = preferredSize.y;
			if (collapsableItem.getExpanded())
			{
				//when it is expanded we center the dialog, because sometimes it shows right on top
				Rectangle parentSize = shell.getParent().getBounds();
				bounds.y = (parentSize.height - bounds.height) / 2 + parentSize.y;
			}
			shell.setBounds(bounds);
			shell.layout(true, true);
		});
	}

	/**
	 * The method returns the order of the radioButton
	 */
	@Override
	public int open()
	{
		int open = super.open();
		open = radioButtons.indexOf(selectedButton);
		return open;
	}
}
