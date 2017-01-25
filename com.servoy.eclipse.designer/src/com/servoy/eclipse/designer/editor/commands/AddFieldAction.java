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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.dialogs.PlaceDataProviderConfiguration;
import com.servoy.eclipse.ui.dialogs.PlaceDataprovidersComposite;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;

/**
 * Action to add a field in form designer, show field selection dialog
 *
 * @author rgansevles
 *
 */
public class AddFieldAction extends DesignerToolbarAction
{
	/**
	 * @author jcomp
	 *
	 */
	private final class PlaceDataProviderAndFields extends Dialog
	{
		private PlaceDataprovidersComposite comp;
		private final PersistContext persistContext;
		private final FlattenedSolution flattenedSolution;
		private final DataProviderOptions dataproviderOptions;
		private final ITable table;

		/**
		 * @param parentShell
		 * @param dialog
		 * @param input
		 * @param table
		 * @param flattenedSolution
		 * @param persistContext
		 */
		private PlaceDataProviderAndFields(Shell parentShell, PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table,
			DataProviderOptions dataproviderOptions)
		{
			super(parentShell);
			this.persistContext = persistContext;
			this.flattenedSolution = flattenedSolution;
			this.table = table;
			this.dataproviderOptions = dataproviderOptions;
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}

		@Override
		protected void configureShell(Shell newShell)
		{
			super.configureShell(newShell);
			newShell.setText("Select dataprovider and its component/template");
		}

		@Override
		public IDialogSettings getDialogBoundsSettings()
		{
			return EditorUtil.getDialogSettings("PlaceDataProviderAndFields");
		}

		@Override
		protected Control createContents(Composite parent)
		{
			Control contents = super.createContents(parent);
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			return contents;
		}

		@Override
		protected Control createDialogArea(Composite parent)
		{
			Composite area = (Composite)super.createDialogArea(parent);
			comp = new PlaceDataprovidersComposite(area, persistContext, flattenedSolution, table, dataproviderOptions, getDialogBoundsSettings());
			comp.addReadyListener(new PlaceDataprovidersComposite.IReadyListener()
			{
				@Override
				public void isReady(boolean ready)
				{
					getButton(IDialogConstants.OK_ID).setEnabled(ready);
				}
			});
			comp.setLayoutData(new GridData(GridData.FILL_BOTH));
			return area;
		}

		public PlaceDataProviderConfiguration getDataProviderConfiguration()
		{
			return comp.getDataProviderConfiguration();
		}
	}

	public AddFieldAction(IWorkbenchPart part)
	{
		super(part, VisualFormEditor.REQ_PLACE_FIELD);
	}

	@Override
	public Request createRequest(EditPart editPart)
	{
		FlattenedSolution flattenedSolution;
		ITable table = null;
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
			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			flattenedSolution = servoyModel.getEditingFlattenedSolution(form);
			table = servoyModel.getDataSourceManager().getDataSource(flattenedSolution.getFlattenedForm(form).getDataSource());
			input = new DataProviderTreeViewer.DataProviderOptions(true, table != null, table != null, true, true, true, table != null, true,
				INCLUDE_RELATIONS.NESTED, true, true, null);
		}

		PlaceDataProviderAndFields dialog = new PlaceDataProviderAndFields(getShell(), PersistContext.create(form), flattenedSolution, table, input);
		dialog.open();

		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return null;
		}

		PlaceDataProviderConfiguration dpConf = dialog.getDataProviderConfiguration();

		// multiple selection
		return new DataFieldRequest(getRequestType(), dpConf.getDataProvidersConfig(), false, dpConf.isPlaceWithLabels(), dpConf.isPlaceHorizontally(),
			dpConf.isFillText(), dpConf.isFillName(), dpConf.getFieldSpacing(), dpConf.getLabelSpacing(), dpConf.getLabelComponent(), dpConf.isPlaceOnTop(),
			dpConf.getFieldSize(), dpConf.getLabelSize());
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
