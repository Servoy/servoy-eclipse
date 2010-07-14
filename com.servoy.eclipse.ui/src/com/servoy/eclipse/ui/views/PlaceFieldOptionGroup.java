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
package com.servoy.eclipse.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * Group with buttons for place-fields options.
 * 
 * @author rgansevles
 * 
 */
public class PlaceFieldOptionGroup extends Group implements SelectionListener
{
	private Button placeAsLabelsButton;
	private Button placeWithLabelsButton;
	private Button placeHorizontalButton;
	private Button fillTextButton;
	private Button fillNameButton;

	private boolean placeAsLabels;
	private boolean placeWithLabels;
	private boolean placeHorizontal;
	private boolean fillText;
	private boolean fillName;

	public PlaceFieldOptionGroup(Composite parent, int style)
	{
		super(parent, style);
		init();
	}

	protected void init()
	{
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		setLayout(gridLayout);

		placeWithLabelsButton = new Button(this, SWT.CHECK);
		placeWithLabelsButton.setText("Place with labels");
		placeWithLabelsButton.addSelectionListener(this);
		placeWithLabelsButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		placeHorizontalButton = new Button(this, SWT.CHECK);
		placeHorizontalButton.setText("Place horizontal");
		placeHorizontalButton.addSelectionListener(this);
		placeHorizontalButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fillTextButton = new Button(this, SWT.CHECK);
		fillTextButton.setText("Fill text property");
		fillTextButton.addSelectionListener(this);
		fillTextButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fillNameButton = new Button(this, SWT.CHECK);
		fillNameButton.setText("Fill name property");
		fillNameButton.addSelectionListener(this);
		fillNameButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		placeAsLabelsButton = new Button(this, SWT.CHECK);
		placeAsLabelsButton.setText("Place as labels");
		placeAsLabelsButton.addSelectionListener(this);
		placeAsLabelsButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	public void widgetSelected(SelectionEvent e)
	{
		placeAsLabels = placeAsLabelsButton.getSelection();
		placeWithLabels = placeWithLabelsButton.getSelection();
		placeHorizontal = placeHorizontalButton.getSelection();
		fillText = fillTextButton.getSelection();
		fillName = fillNameButton.getSelection();
	}

	public void widgetDefaultSelected(SelectionEvent e)
	{
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	public boolean isPlaceAsLabels()
	{
		return placeAsLabels;
	}

	public void setPlaceAsLabels(boolean placeAsLabels)
	{
		this.placeAsLabels = placeAsLabels;
		placeAsLabelsButton.setSelection(placeAsLabels);
	}

	public boolean isPlaceWithLabels()
	{
		return placeWithLabels;
	}

	public void setPlaceWithLabels(boolean placeWithLabels)
	{
		this.placeWithLabels = placeWithLabels;
		placeWithLabelsButton.setSelection(placeWithLabels);
	}

	public boolean isPlaceHorizontal()
	{
		return placeHorizontal;
	}

	public void setPlaceHorizontal(boolean placeHorizontal)
	{
		this.placeHorizontal = placeHorizontal;
		placeHorizontalButton.setSelection(placeHorizontal);
	}

	public boolean isFillText()
	{
		return fillText;
	}

	public void setFillText(boolean fillText)
	{
		this.fillText = fillText;
		fillTextButton.setSelection(fillText);
	}

	public boolean isFillName()
	{
		return fillName;
	}

	public void setFillName(boolean fillName)
	{
		this.fillName = fillName;
		fillNameButton.setSelection(fillName);
	}

}
