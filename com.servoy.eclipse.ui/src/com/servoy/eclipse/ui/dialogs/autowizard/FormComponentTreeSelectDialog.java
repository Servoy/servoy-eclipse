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

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory.RelatedForm;
import com.servoy.eclipse.core.util.EclipseDatabaseUtils;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.labelproviders.RelatedFormsLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.RelatedFormsContentProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class FormComponentTreeSelectDialog extends TreeSelectDialog
{
	public FormComponentTreeSelectDialog(Form context, RelatedForm selection)
	{
		super(UIUtils.getActiveShell(), true, true, TreePatternFilter.FILTER_LEAFS,
			new RelatedFormsContentProvider(context, true),
			new SolutionContextDelegateLabelProvider(RelatedFormsLabelProvider.INSTANCE, context),
			null, new IFilter()
			{

				@Override
				public boolean select(Object toTest)
				{
					return toTest instanceof RelatedForm && ((RelatedForm)toTest).form != null;
				}

			}, SWT.NONE,
			"Select Form Component", context,
			selection != null ? new StructuredSelection(selection) : null, true, "FormComponentDialog", null, false);
	}

	public static void selectFormComponent(WebComponent component, Form context)
	{
		RelatedForm initialSelection = null;
		Object containedForm = component.getProperty("containedForm");
		if (containedForm instanceof JSONObject)
		{
			String formName = ((JSONObject)containedForm).optString(FormComponentPropertyType.SVY_FORM, null);
			if (formName != null)
			{
				Object foundsetDescription = component.getProperty("foundset");
				FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(context);
				Relation[] relations = null;
				if (foundsetDescription instanceof JSONObject)
				{
					String descriptor = ((JSONObject)foundsetDescription).optString(FoundsetPropertyType.FOUNDSET_SELECTOR, null);
					if (descriptor != null)
					{
						relations = fs.getRelationSequence(descriptor);
					}
				}
				initialSelection = new RelatedForm(relations, fs.getForm(formName));
			}
		}
		FormComponentTreeSelectDialog dialog = new FormComponentTreeSelectDialog(context, initialSelection);
		if (dialog.open() == Window.OK)
		{
			ISelection selection = dialog.getSelection();
			if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).getFirstElement() instanceof RelatedForm)
			{
				RelatedForm relatedForm = (RelatedForm)((IStructuredSelection)selection).getFirstElement();
				JSONObject json = new JSONObject();
				json.put(FormComponentPropertyType.SVY_FORM, relatedForm.form.getUUID().toString());
				component.setProperty("containedForm", json);
				json = new JSONObject();
				json.put(FoundsetPropertyType.FOUNDSET_SELECTOR,
					relatedForm.relations != null ? EclipseDatabaseUtils.getRelationsString(relatedForm.relations)
						: (Utils.equalObjects(relatedForm.form.getDataSource(), context.getDataSource()) ? "" : relatedForm.form.getDataSource()));
				component.setProperty("foundset", json);
				ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, component, true);
			}
		}
	}
}
