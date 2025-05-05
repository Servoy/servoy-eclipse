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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.core.elements.ElementFactory.RelatedForm;
import com.servoy.eclipse.ui.dialogs.FilteredTreeViewer;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.labelproviders.RelatedFormsLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.RelatedFormsContentProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Relation;

/**
 * @author emera
 */
public class FormPropertiesSelector
{
	private final PropertyWizardDialog propertyWizardDialog;
	private final List<PropertyDescription> formProperties;
	private final FilteredTreeViewer formPicker;
	private String relationPropertyName;

	public FormPropertiesSelector(PropertyWizardDialog propertyWizardDialog, SashForm form, List<PropertyDescription> formProperties,
		List<PropertyDescription> relationProperties, PersistContext persistContext, IDialogSettings settings, FlattenedSolution flattenedSolution)
	{
		this.propertyWizardDialog = propertyWizardDialog;
		this.formProperties = formProperties;

		JSONObject tag = (JSONObject)formProperties.get(0).getTag("wizard");
		Object relatedProperty = tag != null ? tag.get("wizardRelated") : null;
		boolean hasRelationProperty = relatedProperty != null &&
			relationProperties.stream().filter(pd -> relatedProperty.equals(pd.getName())).findAny().isPresent();
		if (hasRelationProperty)
		{
			relationPropertyName = relatedProperty.toString();
		}

		Composite parent = new Composite(form, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.marginRight = 5;
		parent.setLayout(layout);

		Form frm = persistContext.getContext() != null ? (Form)persistContext.getContext().getAncestor(IRepository.FORMS) : null;
		ITreeContentProvider contentProvider = new RelatedFormsContentProvider(frm, flattenedSolution);
		formPicker = new FilteredTreeViewer(parent, true, true,
			// contentProvider
			contentProvider,
			//labelProvider
			new SolutionContextDelegateLabelProvider(RelatedFormsLabelProvider.INSTANCE, persistContext.getContext()),
			// comparator
			null,
			// treeStyle
			SWT.MULTI,
			// filter
			new TreePatternFilter(TreePatternFilter.getSavedFilterMode(settings, TreePatternFilter.FILTER_LEAFS)),
			// selectionFilter
			new LeafnodesSelectionFilter(contentProvider));
		formPicker.setInput(frm);

		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		gridData.verticalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.minimumWidth = 10;
		gridData.heightHint = 450;

		formPicker.setLayoutData(gridData);
		formPicker.addSelectionChangedListener(event -> moveFormSelection());
		formPicker.getViewer().getTree().setToolTipText("Select the forms to show in the component");
	}

	private void moveFormSelection()
	{
		IStructuredSelection selection = (IStructuredSelection)formPicker.getSelection();
		if (selection.getFirstElement() instanceof RelatedForm)
		{
			RelatedForm relForm = (RelatedForm)selection.getFirstElement();
			Form form = relForm.form;
			Map<String, Object> map = new HashMap<>();
			PropertyDescription propertyDescription = formProperties.get(0);
			map.put(propertyDescription.getName(), form.getUUID().toString());

			if (relationPropertyName != null)
			{
				Relation relation = relForm.relations != null ? relForm.relations[relForm.relations.length - 1] : null;
				map.put(relationPropertyName, relation != null ? relation.getName() : null);
			}
			propertyWizardDialog.addNewRow(map);
		}
	}
}
