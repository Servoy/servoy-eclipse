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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.DataProviderDialog;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.IControlFactory;
import com.servoy.eclipse.ui.views.PlaceFieldOptionGroup;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Action to add a portal in form designer, show selection dialog
 *
 * @author rgansevles
 *
 */
public class AddPortalAction extends DesignerToolbarAction
{
	protected PlaceFieldOptionGroup optionsGroup;

	public AddPortalAction(IWorkbenchPart part)
	{
		super(part, VisualFormEditor.REQ_PLACE_PORTAL);
	}

	@Override
	public Request createRequest(EditPart editPart)
	{
		Form form = (Form)getContext(editPart, IRepository.FORMS);
		if (form == null)
		{
			return null;
		}

		FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(form);
		ITable table = null;
		try
		{
			table = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(editingFlattenedSolution.getFlattenedForm(form).getDataSource());
			if (!editingFlattenedSolution.getRelations(table, true, false).hasNext())
			{
				org.eclipse.jface.dialogs.MessageDialog.openConfirm(getShell(), "Add Portal", "No relations are defined on form table " + table);
				return null;
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
			return null;
		}

		DataProviderDialog dialog = new DataProviderDialog(getShell(), DataProviderLabelProvider.INSTANCE_HIDEPREFIX, PersistContext.create(form),
			editingFlattenedSolution, table,
			new DataProviderTreeViewer.DataProviderOptions(false, false, false, true, false, false, false, false, INCLUDE_RELATIONS.NESTED, true, false, null),
			null, SWT.MULTI, "Select Data Providers");
		dialog.setOptionsAreaFactory(new IControlFactory()
		{
			public Control createControl(Composite composite)
			{
				optionsGroup = new PlaceFieldOptionGroup(composite, SWT.NONE);
				optionsGroup.setFillText(true);
				optionsGroup.setText("Options");
				return optionsGroup;
			}
		});
		dialog.open();

		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return null;
		}

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
		setText(DesignerActionFactory.ADD_PORTAL_TEXT);
		setToolTipText(DesignerActionFactory.ADD_PORTAL_TOOLTIP);
		setId(DesignerActionFactory.ADD_PORTAL.getId());
		setImageDescriptor(DesignerActionFactory.ADD_PORTAL_IMAGE);
	}

}
