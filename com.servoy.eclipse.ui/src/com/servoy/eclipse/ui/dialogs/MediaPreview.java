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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.servoy.eclipse.ui.editors.MediaComposite;
import com.servoy.eclipse.ui.util.MediaNode;

/**
 * Group composite for previewing images.
 *
 * @author rgansevles
 *
 */
public class MediaPreview extends Composite implements ISelectionChangedListener
{
	private static final Point MEDIA_PREVIEW_SIZE = new Point(300, 200);

	private final ISelectionProvider selectionProvider;
	private MediaComposite mediaComposite;
	private IDialogSettings settings = null;
	private ExpandableComposite excomposite;

	public MediaPreview(Composite parent, int style, ISelectionProvider selectionProvider)
	{
		super(parent, style);
		this.selectionProvider = selectionProvider;

		initPreview();
	}

	public MediaPreview(Composite parent, int style, ISelectionProvider selectionProvider, IDialogSettings settings)
	{
		super(parent, style);
		this.selectionProvider = selectionProvider;
		this.settings = settings;

		initPreview();
	}

	protected void initPreview()
	{
		setLayout(new FormLayout());
		selectionProvider.addSelectionChangedListener(this);

		final boolean isPreviewShown;
		if (settings != null)
		{
			isPreviewShown = settings.getBoolean("previewSelected");
		}
		else
		{
			isPreviewShown = true;
		}

		excomposite = new ExpandableComposite(this, SWT.NONE,
			ExpandableComposite.TWISTIE);
		excomposite.setText("Preview");
		excomposite.setExpanded(true);

		// First and only item
		mediaComposite = new MediaComposite(excomposite, SWT.V_SCROLL);
		mediaComposite.setFixedSize(MEDIA_PREVIEW_SIZE);
		mediaComposite.setNoImageText("");

		ImageHyperlink image = new ImageHyperlink(excomposite, SWT.None);
		image.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE));
		excomposite.setClient(mediaComposite);
		excomposite.setTextClient(image);

		// then update expanded state later
		getDisplay().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				excomposite.setExpanded(isPreviewShown);
				updateItemState(isPreviewShown);

				// make shell higher if it's not high enough...
				Shell shell = getShell();
				if (!shell.isDisposed() && shell.isVisible())
				{
					Point s = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					if (s.y > shell.getSize().y) shell.setSize(shell.getSize().x, s.y);
				}
				selectionChanged(selectionProvider.getSelection());
			}
		});
		excomposite.addExpansionListener(new IExpansionListener()
		{

			@Override
			public void expansionStateChanging(ExpansionEvent e)
			{

			}

			@Override
			public void expansionStateChanged(ExpansionEvent e)
			{
				if (settings != null) settings.put("previewSelected", e.getState());
				previewImage(selectionProvider.getSelection(), e.getState());

			}

		});

		FormData fd = new FormData();
		fd.left = new FormAttachment(0, 0);
		fd.top = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0);
		excomposite.setLayoutData(fd);
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
		selectionChanged(event.getSelection());
	}

	protected void selectionChanged(ISelection sel)
	{
		previewImage(sel, excomposite.isExpanded());
	}

	protected void previewImage(ISelection selection, boolean expanded)
	{
		if (expanded)
		{
			if (!selection.isEmpty() && selection instanceof IStructuredSelection)
			{
				Object media = ((IStructuredSelection)selection).getFirstElement();
				if (media instanceof MediaNode && ((MediaNode)media).getType() == MediaNode.TYPE.IMAGE)
				{
					mediaComposite.setMedia(((MediaNode)media).getMedia());
					updateItemState(expanded);
					return;
				}
			}
		}
		mediaComposite.setMedia(null);
		updateItemState(expanded);
	}

	protected void updateItemState(boolean expanded)
	{
		getDisplay().asyncExec(new Runnable()
		{

			@Override
			public void run()
			{
				layout();
			}
		});
	}
}