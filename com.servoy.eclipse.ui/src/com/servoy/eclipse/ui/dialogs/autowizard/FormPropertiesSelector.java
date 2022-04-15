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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.ui.dialogs.FilteredTreeViewer;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.labelproviders.CombinedTreeLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormLabelProvider;
import com.servoy.eclipse.ui.labelproviders.RelationLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;

/**
 * @author emera
 */
public class FormPropertiesSelector
{
	private final PropertyWizardDialog propertyWizardDialog;
	private final List<PropertyDescription> formProperties;
	private final FilteredTreeViewer formPicker;

	public FormPropertiesSelector(PropertyWizardDialog propertyWizardDialog, SashForm form, List<PropertyDescription> formProperties,
		FlattenedSolution flattenedSolution, PersistContext persistContext, IDialogSettings settings)
	{
		this.propertyWizardDialog = propertyWizardDialog;
		this.formProperties = formProperties;

		Composite parent = new Composite(form, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.marginRight = 5;
		parent.setLayout(layout);

		ILabelProvider labelProvider = new CombinedTreeLabelProvider(1,
			new ILabelProvider[] { new RelationLabelProvider("", true, true), new FormLabelProvider(flattenedSolution, true) });
		ITreeContentProvider contentProvider = new RelatedFormsContentProvider(flattenedSolution, persistContext.getContext());
		formPicker = new FilteredTreeViewer(parent, true, true,
			// contentProvider
			contentProvider,
			//labelProvider
			labelProvider,
			// comparator
			null,
			// treeStyle
			SWT.MULTI,
			// filter
			new TreePatternFilter(TreePatternFilter.getSavedFilterMode(settings, TreePatternFilter.FILTER_LEAFS)),
			// selectionFilter
			new LeafnodesSelectionFilter(contentProvider));
		ITable formTable = ((Form)persistContext.getContext()).getDataSource() != null
			? ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(((Form)persistContext.getContext()).getDataSource()) : null;
		formPicker.setInput(new RelationContentProvider.RelationListOptions(formTable, null, false,
			true));

		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		gridData.verticalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.minimumWidth = 400;
		gridData.heightHint = 450;

		formPicker.setLayoutData(gridData);
		formPicker.addSelectionChangedListener(event -> moveFormSelection());
		formPicker.getViewer().getTree().setToolTipText("Select the forms to show in the component");
	}

	private void moveFormSelection()
	{
		IStructuredSelection selection = (IStructuredSelection)formPicker.getSelection();
		if (selection.getFirstElement() instanceof Form)
		{
			Form form = (Form)selection.getFirstElement();
			Map<String, Object> map = new HashMap<>();
			PropertyDescription propertyDescription = formProperties.get(0);
			map.put(propertyDescription.getName(), form.getUUID());
			//TODO this is kind of hardcoded, can we improve it?
			Object parent = formPicker.getViewer().getTree().getSelection()[0].getParentItem().getData();
			if (parent instanceof RelationsWrapper)
			{
				RelationsWrapper relationsWrapper = (RelationsWrapper)parent;
				Relation relation = relationsWrapper.relations[relationsWrapper.relations.length - 1];
				map.put(propertyDescription.getTag("wizardRelated").toString(), relation.getName());
			}
			propertyWizardDialog.addNewRow(form.getName(), map);
		}
	}
}
