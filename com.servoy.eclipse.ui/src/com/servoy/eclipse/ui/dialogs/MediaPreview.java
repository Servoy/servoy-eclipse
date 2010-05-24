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
package com.servoy.eclipse.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.servoy.eclipse.ui.editors.MediaComposite;
import com.servoy.j2db.FlattenedSolution;

/**
 * Group composite for previewing images.
 * 
 * @author rob
 * 
 */
public class MediaPreview extends Group implements ISelectionChangedListener
{
	private static final Point MEDIA_PREVIEW_SIZE = new Point(300, 200);

	private Button previewButton;
	private final FlattenedSolution flattenedSolution;
	private final ISelectionProvider selectionProvider;
	private MediaComposite mediaComposite;
	private IDialogSettings settings = null;

	public MediaPreview(Composite parent, int style, FlattenedSolution flattenedSolution, ISelectionProvider selectionProvider)
	{
		super(parent, style);
		this.flattenedSolution = flattenedSolution;
		this.selectionProvider = selectionProvider;

		initPreview();
	}

	public MediaPreview(Composite parent, int style, FlattenedSolution flattenedSolution, ISelectionProvider selectionProvider, IDialogSettings settings)
	{
		super(parent, style);
		this.flattenedSolution = flattenedSolution;
		this.selectionProvider = selectionProvider;
		this.settings = settings;

		initPreview();
	}

	protected void initPreview()
	{
		setText("Preview");

		selectionProvider.addSelectionChangedListener(this);

		previewButton = new Button(this, SWT.CHECK);
		previewButton.setSize(10, 10);

		boolean isSelected = false;
		if (settings != null)
		{
			isSelected = settings.getBoolean("previewSelected");
		}

		previewButton.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent e)
			{
				if (settings != null) settings.put("previewSelected", previewButton.getSelection());
				previewImage(selectionProvider.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e)
			{
				previewImage(selectionProvider.getSelection());
			}
		});

		previewButton.setSelection(isSelected);

		mediaComposite = new MediaComposite(this, SWT.NONE);
		mediaComposite.setFixedSize(MEDIA_PREVIEW_SIZE);
		mediaComposite.setNoImageText("");

		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(previewButton, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE).addPreferredGap(
				LayoutStyle.RELATED).add(mediaComposite, GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(12, 12, 12).add(previewButton, GroupLayout.PREFERRED_SIZE, 39, GroupLayout.PREFERRED_SIZE)).add(
					mediaComposite, GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)).add(26, 26, 26)));
		setLayout(groupLayout);
	}

	@Override
	protected void checkSubclass()
	{
	}

	@Override
	public void dispose()
	{
		selectionProvider.removeSelectionChangedListener(this);
		super.dispose();
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		previewImage((event.getSelection()));
	}

	protected void previewImage(ISelection selection)
	{
		if (previewButton.getSelection())
		{
			if (!selection.isEmpty() && selection instanceof IStructuredSelection)
			{
				Object mediaId = ((IStructuredSelection)selection).getFirstElement();
				if (mediaId instanceof Integer)
				{
					mediaComposite.setMedia(flattenedSolution.getMedia(((Integer)mediaId).intValue()));
					return;
				}
			}
		}
		mediaComposite.setMedia(null);
	}
}