/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.exporter.mobile.ui.wizard;

import java.util.Arrays;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

import com.servoy.eclipse.model.mobile.exporter.MobileExporter;

/**
 * @author jcompagner
 *
 */
public class MediaOrderPage extends WizardPage
{
	private final WizardPage nextPage;
	private final MobileExporter mobileExporter;
	private ListViewer listViewer;

	/**
	 * @param pageName
	 * @param mobileExporter 
	 * @param licensePage 
	 */
	protected MediaOrderPage(String pageName, WizardPage nextPage, MobileExporter mobileExporter)
	{
		super(pageName);
		this.nextPage = nextPage;
		this.mobileExporter = mobileExporter;
		setTitle("Order the media how they must be loaded in the browser");
	}

	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new GridLayout(2, false));

		ScrolledComposite scrolledComposite = new ScrolledComposite(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

		listViewer = new ListViewer(scrolledComposite, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		final List list = listViewer.getList();
		scrolledComposite.setContent(list);
		scrolledComposite.setMinSize(list.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		RowLayout rl_composite = new RowLayout(SWT.VERTICAL);
		rl_composite.fill = true;
		rl_composite.justify = true;
		composite.setLayout(rl_composite);

		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.setInput(mobileExporter.getMediaOrder());
		Button btnUp = new Button(composite, SWT.NONE);
		btnUp.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				int[] selectionIndices = list.getSelectionIndices();
				if (selectionIndices.length > 0)
				{
					Arrays.sort(selectionIndices);
					if (selectionIndices[0] > 0)
					{
						java.util.List<String> input = (java.util.List<String>)listViewer.getInput();
						for (int index : selectionIndices)
						{
							String obj = input.remove(index - 1);
							input.add(index, obj);
						}
						listViewer.refresh();
					}
				}
			}
		});
		btnUp.setText("Up");

		Button btnDown = new Button(composite, SWT.NONE);
		btnDown.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				int[] selectionIndices = list.getSelectionIndices();
				if (selectionIndices.length > 0)
				{
					Arrays.sort(selectionIndices);
					java.util.List<String> input = (java.util.List<String>)listViewer.getInput();
					if (selectionIndices[selectionIndices.length - 1] < (input.size() - 1))
					{
						for (int i = selectionIndices.length; --i >= 0;)
						{
							String obj = input.remove(selectionIndices[i] + 1);
							input.add(selectionIndices[i], obj);
						}
						listViewer.refresh();
					}
				}
			}
		});
		btnDown.setText("Down");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	@Override
	public IWizardPage getNextPage()
	{
		mobileExporter.setMediaOrder((java.util.List<String>)listViewer.getInput());
		return nextPage;
	}
}
