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

package com.servoy.eclipse.ui.dialogs.autowizard;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ITable;

/**
 * @author jcompagner
 * @since 2022.06
 *
 */
public class PropertyWizardDialog extends Dialog
{
	private final PersistContext persistContext;
	private final FlattenedSolution flattenedSolution;
	private final ITable table;
	private final IDialogSettings settings;
	private final PropertyDescription property;
	private DataproviderPropertiesSelector dataprovidersSelector;
	private AutoWizardPropertiesComposite tableComposite;
	private StylePropertiesSelector stylePropertiesSelector;
	private final PropertyWizardDialogConfigurator configurator;
	private FormPropertiesSelector formPropertiesSelector;

	PropertyWizardDialog(Shell parentShell, PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table, final IDialogSettings settings,
		PropertyDescription property, PropertyWizardDialogConfigurator configurator)
	{
		super(parentShell);
		this.persistContext = persistContext;
		this.flattenedSolution = flattenedSolution;
		this.table = table;
		this.settings = settings;
		this.property = property;
		this.configurator = configurator;
	}

	@Override
	protected void okPressed()
	{
		tableComposite.commitAndCloseActiveCellEditor();
		super.okPressed();
	}

	@Override
	protected boolean isResizable()
	{
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Property configurator for " + property.getName());

		Composite area = (Composite)super.createDialogArea(parent);
		// first check what the main thing must be (dataproviders, forms, relations?)

		Composite c = new Composite(area, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.marginRight = 5;
		c.setLayout(layout);
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		gridData.verticalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		c.setLayoutData(gridData);

		SashForm hSash = new SashForm(c, SWT.HORIZONTAL);
		hSash.setLayout(layout);
		hSash.setLayoutData(gridData);
		SashForm vSash = new SashForm(hSash, SWT.VERTICAL);

		//sc is the direct parent on the nattable in AutowizardPropertiesComposite
		//make sure to add the border here (if any)
		//wrapping it inside another composite with a border is causing a wrong (very first) rendering of the nattable
		// basically the nat table is sitting "behind" the border widget's client area until resize
		ScrolledComposite sc = new ScrolledComposite(hSash, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);

		int initialW = settings.get("weight") != null ? settings.getInt("weight") : 400;
		hSash.setWeights(initialW, 1000 - initialW);
		vSash.addListener(SWT.Resize, e -> {
			settings.put("weight", hSash.getWeights()[0]);
		});

		if (configurator.getDataproviderProperties().size() > 0)
		{
			dataprovidersSelector = new DataproviderPropertiesSelector(this, vSash, persistContext, configurator, flattenedSolution,
				table, settings, getShell());
		}

		if (configurator.getStyleProperties().size() > 0) // should really be always 1 size..
		{
			stylePropertiesSelector = new StylePropertiesSelector(this, vSash, configurator.getStyleProperties());
		}

		if (dataprovidersSelector != null && stylePropertiesSelector.stylePropertiesViewer != null)
			vSash.setWeights(70, 30);

		if (configurator.getFormProperties().size() > 0)
		{
			formPropertiesSelector = new FormPropertiesSelector(this, vSash, configurator.getFormProperties(), configurator.getRelationProperties(),
				persistContext, settings, flattenedSolution);
		}

		tableComposite = new AutoWizardPropertiesComposite(
			sc,
			persistContext,
			flattenedSolution,
			configurator);

		return area;
	}

	public List<Map<String, Object>> getResult()
	{
		if (tableComposite != null)
		{
			return tableComposite.getResult();
		}
		return Collections.emptyList();
	}

	void setTreeInput(List<Map<String, Object>> list)
	{
		tableComposite.setInput(list);

	}

	void addNewRow(Map<String, Object> row)
	{
		tableComposite.addNewRow(row);
	}

	public List<Map<String, Object>> getInput()
	{
		return tableComposite.getInput();
	}

	@Override
	protected int getDialogBoundsStrategy()
	{
		return Dialog.DIALOG_PERSISTLOCATION | Dialog.DIALOG_PERSISTSIZE;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings()
	{
		return settings;
	}
}
