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
package com.servoy.eclipse.designer.editor.commands;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.DataProviderDialog;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.IControlFactory;
import com.servoy.eclipse.ui.views.PlaceFieldOptionGroup;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;

/**
 * Action to add a field in form designer, show field selection dialog
 * 
 * @author rgansevles
 *
 */
public class AddFieldAction extends DesignerToolbarAction
{
	protected PlaceFieldOptionGroup optionsGroup;

	public AddFieldAction(IWorkbenchPart part)
	{
		super(part, VisualFormEditor.REQ_PLACE_FIELD);
	}

	@Override
	public Request createRequest(EditPart editPart)
	{
		FlattenedSolution flattenedSolution;
		Table table = null;
		DataProviderOptions input;
		Form form = null;
		Portal portal = (Portal)getContext(editPart, IRepository.PORTALS);
		if (portal != null && portal.getRelationName() != null)
		{
			flattenedSolution = ModelUtils.getEditingFlattenedSolution(portal);
			Relation[] relations = flattenedSolution.getRelationSequence(portal.getRelationName());
			if (relations == null)
			{
				org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Relation not found", "Could not find relation for portal");
				return null;
			}
			input = new DataProviderTreeViewer.DataProviderOptions(true, false, false, true /* related calcs */, false, false, false, false,
				INCLUDE_RELATIONS.NESTED, false, true, relations);
		}
		else
		{
			form = (Form)getContext(editPart, IRepository.FORMS);
			flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(form);

			try
			{
				table = flattenedSolution.getFlattenedForm(form).getTable();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not get table for form " + form, e);
			}
			input = new DataProviderTreeViewer.DataProviderOptions(true, table != null, table != null, true, true, true, table != null, true,
				INCLUDE_RELATIONS.NESTED, true, true, null);
		}

		DataProviderDialog dialog = new DataProviderDialog(getShell(), new SolutionContextDelegateLabelProvider(new FormContextDelegateLabelProvider(
			DataProviderLabelProvider.INSTANCE_HIDEPREFIX, form), form), PersistContext.create(form, null), flattenedSolution, table, input, null, SWT.MULTI,
			"Select Data Providers");

		IDialogSettings settings = dialog.getDataProvideDialogSettings();
		final boolean isPlaceHorizontal = settings.getBoolean("placeHorizontal");
		final String isPlaceAsLabels = settings.get("placeAsLabels");
		final String isPlaceWithLabels = settings.get("placeLabels");
		final boolean isFillText = settings.getBoolean("fillText");
		final boolean isFillName = settings.getBoolean("fillName");

		dialog.setOptionsAreaFactory(new IControlFactory()
		{
			public Control createControl(Composite composite)
			{
				optionsGroup = new PlaceFieldOptionGroup(composite, SWT.NONE);
				optionsGroup.setText("Options");

				optionsGroup.setPlaceAsLabels(String.valueOf(true).equals(isPlaceAsLabels));
				// placeWithLabels defaults to true
				optionsGroup.setPlaceWithLabels(isPlaceWithLabels == null || String.valueOf(true).equals(isPlaceWithLabels));
				optionsGroup.setPlaceHorizontal(isPlaceHorizontal);
				optionsGroup.setFillText(isFillText);
				optionsGroup.setFillName(isFillName);

				return optionsGroup;
			}
		});

		dialog.open();

		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return null;
		}
		settings.put("placeHorizontal", optionsGroup.isPlaceHorizontal());
		settings.put("placeAsLabels", optionsGroup.isPlaceAsLabels());
		settings.put("placeLabels", optionsGroup.isPlaceWithLabels());
		settings.put("fillText", optionsGroup.isFillText());
		settings.put("fillName", optionsGroup.isFillName());

		// multiple selection
		return new DataFieldRequest(getRequestType(), ((IStructuredSelection)dialog.getSelection()).toArray(), optionsGroup.isPlaceAsLabels(),
			optionsGroup.isPlaceWithLabels(), optionsGroup.isPlaceHorizontal(), optionsGroup.isFillText(), optionsGroup.isFillName());
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText(DesignerActionFactory.ADD_FIELD_TEXT);
		setToolTipText(DesignerActionFactory.ADD_FIELD_TOOLTIP);
		setId(DesignerActionFactory.ADD_FIELD.getId());
		setImageDescriptor(DesignerActionFactory.ADD_FIELD_IMAGE);
	}

}
