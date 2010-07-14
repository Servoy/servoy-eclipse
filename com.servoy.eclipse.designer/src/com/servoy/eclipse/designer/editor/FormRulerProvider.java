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
package com.servoy.eclipse.designer.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.rulers.RulerProvider;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.commands.MovePartCommand;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Ruler provider for form designer, vertical rule is form widt, horizontal rules are part boundaries.
 * 
 * @author rgansevles
 */

public class FormRulerProvider extends RulerProvider
{
	private final boolean horizontal;
	private final EditPart formEditPart;

	public FormRulerProvider(EditPart formEditPart, boolean horizontal)
	{
		this.formEditPart = formEditPart;
		this.horizontal = horizontal;
	}

	@Override
	public int getGuidePosition(Object guide)
	{
		if (guide instanceof GraphicalEditPart)
		{
			Object model = ((EditPart)guide).getModel();
			if (horizontal)
			{
				return ((Form)model).getWidth();
			}
			else
			{
				return ((Part)model).getHeight();
			}
		}

		return super.getGuidePosition(guide);
	}

	@Override
	public List<EditPart> getGuides()
	{
		List<EditPart> guides = new ArrayList<EditPart>();
		if (horizontal)
		{
			guides.add(formEditPart);
		}
		else
		{
			List<EditPart> children = formEditPart.getChildren();
			for (EditPart child : children)
			{
				if (child instanceof FormPartGraphicalEditPart)
				{
					guides.add(child);
				}
			}
		}
		return guides;
	}

	@Override
	public Command getMoveGuideCommand(Object guide, int positionDelta)
	{
		int delta;
		if (guide instanceof FormPartGraphicalEditPart) // vertical
		{
			FormPartGraphicalEditPart editPart = (FormPartGraphicalEditPart)guide;
			if (!editPart.canBeMoved())
			{
				return null;
			}

			delta = editPart.limitPartMove(new Point(0, positionDelta)).y;

			return new MovePartCommand((Part)editPart.getModel(), new ChangeBoundsRequest(RequestConstants.REQ_MOVE), ((Part)editPart.getModel()).getHeight() +
				delta);
		}

		if (guide == formEditPart) // horizontal
		{
			// sub-form width is editable, but not smaller then superform
			int minVal = 0;
			Form form = (Form)formEditPart.getModel();
			if (form.getExtendsFormID() > 0)
			{
				FlattenedSolution editingFlattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(form);
				if (editingFlattenedSolution == null)
				{
					ServoyLog.logError("Could not get project for form " + form, null);
					return null;
				}
				Form superForm = editingFlattenedSolution.getForm(form.getExtendsFormID());
				if (superForm != null)
				{
					Form flattenedSuperForm = superForm;
					try
					{
						flattenedSuperForm = editingFlattenedSolution.getFlattenedForm(superForm);
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
					minVal = flattenedSuperForm.getWidth();
				}
			}

			String property = "width";
			PersistPropertySource persistProperties = new PersistPropertySource(form, form, false);
			int oldVal = ((Integer)persistProperties.getPropertyValue(property)).intValue();
			int newVal = (oldVal + positionDelta) > minVal ? (oldVal + positionDelta) : minVal;
			if (oldVal == newVal)
			{
				return null;
			}
			SetValueCommand setCommand = new SetValueCommand();
			setCommand.setTarget(persistProperties);
			setCommand.setPropertyId(property);
			setCommand.setPropertyValue(new Integer(newVal));
			return setCommand;
		}

		return super.getMoveGuideCommand(guide, positionDelta);
	}

	@Override
	public Object getRuler()
	{
		return this;
	}

	@Override
	public int getUnit()
	{
		DesignerPreferences designerPreferences = new DesignerPreferences(ServoyModel.getSettings());
		switch (designerPreferences.getMetrics())
		{
			case DesignerPreferences.CM :
				return RulerProvider.UNIT_CENTIMETERS;
			case DesignerPreferences.IN :
				return RulerProvider.UNIT_INCHES;
		}
		return RulerProvider.UNIT_PIXELS;
	}
}
