/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.designer.editor.mobile.commands;

import java.awt.Dimension;

import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.graphics.Point;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.designer.editor.commands.BaseFormPlaceElementCommand;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.IAnchorConstants;

/**
 * Command to add a inset list to the form.
 * The inset list consists of a tabpanel with 3 elements
 * 
 * @author rgansevles
 *
 */
public class AddInsetListCommand extends BaseFormPlaceElementCommand
{
	private Form tabForm;

	public AddInsetListCommand(IApplication application, Form form, CreateRequest request)
	{
		super(application, form, null, request.getType(), null, null, request.getLocation().getSWTPoint(), null, form);
	}

	@Override
	protected Object[] placeElements(Point location) throws RepositoryException
	{
		if (parent instanceof Form)
		{
			Form form = (Form)parent;

			// create a tabpanel
			IPersist[] createdTabPanel = ElementFactory.createTabs(application, form, null, location, TabPanel.DEFAULT, "list"); //$NON-NLS-1$
			if (createdTabPanel == null || createdTabPanel.length != 1 || !(createdTabPanel[0] instanceof TabPanel))
			{
				ServoyLog.logError("Could not create tabpanel for inset list", null);
				return null;
			}

			TabPanel tabPanel = (TabPanel)createdTabPanel[0];
			tabPanel.putCustomMobileProperty("list", Boolean.TRUE);
			// for debug in developer
			tabPanel.setSize(new Dimension(((Form)parent).getWidth(), 300));
			tabPanel.setAnchors(IAnchorConstants.ALL);

			// create target form
			Solution solution = (Solution)form.getAncestor(IRepository.SOLUTIONS);
			if (solution == null)
			{
				ServoyLog.logError("Could not find solution for inset list form", null);
				return null;
			}

			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			IValidateName nameValidator = servoyModel.getNameValidator();
			String tabFormName = null;
			for (int i = 0; i < 100; i++)
			{
				tabFormName = form.getName() + '_' + tabPanel.getName() + (i > 0 ? String.valueOf(i) : "");
				try
				{
					nameValidator.checkName(tabFormName, 0, new ValidatorSearchContext(IRepository.FORMS), false);
					break;
				}
				catch (RepositoryException e)
				{
				}
			}

			ServoyProject servoyProject = servoyModel.getServoyProject(solution.getName());
			tabForm = servoyProject.getEditingSolution().createNewForm(nameValidator, null, tabFormName, null, true, null);
			// add parts so it looks nice while developing in webclient
			tabForm.createNewPart(Part.HEADER, 40);
			tabForm.createNewPart(Part.BODY, 600);
			tabForm.setView(FormController.LOCKED_TABLE_VIEW);

			// mark target form as contained in this form
			tabForm.putCustomMobileProperty("mobileform", Boolean.TRUE);
			tabForm.putCustomMobileProperty("listitemFormContainer", form.getUUID());
			tabForm.putCustomMobileProperty("listitemFormTab", tabPanel.getUUID());
			tabForm.setStyleName("_servoy_mobile"); // set internal style name

			// add items for properties
			AddFormListCommand.addlistItems(tabForm);

			// add header
			GraphicalComponent header = ElementFactory.createLabel(tabForm, null, new Point(0, 0));
			header.setDisplaysTags(true);
			header.putCustomMobileProperty("listitemHeader", Boolean.TRUE);
			// for debug in developer
			header.setAnchors(IAnchorConstants.EAST | IAnchorConstants.WEST | IAnchorConstants.NORTH);
			header.setHorizontalAlignment(ISupportTextSetup.CENTER);
			header.setStyleClass("b"); // default for headers

			// add tab
			ElementFactory.createTabs(application, tabPanel, new Object[] { new ElementFactory.RelatedForm(null, tabForm) }, null, TabPanel.DEFAULT, null);

			// save the tabForm, it cannot be saved from the form editor
			servoyProject.saveEditingSolutionNodes(new IPersist[] { tabForm }, true);

			// models is tabpanel and containing form
			return new IPersist[] { tabPanel, tabForm };
		}

		return null;
	}

	@Override
	protected void deleteForUndo(IPersist persist) throws RepositoryException
	{
		if (tabForm == persist)
		{
			// delete the saved form
			((IDeveloperRepository)persist.getRootObject().getRepository()).deleteObject(persist);
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(persist.getRootObject().getName());
			servoyProject.saveEditingSolutionNodes(new IPersist[] { tabForm }, true);
			tabForm = null;
		}
		else
		{
			super.deleteForUndo(persist);
		}
	}
}