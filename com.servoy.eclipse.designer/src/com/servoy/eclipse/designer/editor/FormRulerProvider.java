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
import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.rulers.RulerProvider;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;

/**
 * Ruler provider for form designer, vertical rule is form widt, horizontal rules are part boundaries.
 * 
 * @author rgansevles
 */

public class FormRulerProvider extends RulerProvider
{
	private final boolean horizontal;
	private final Form form;

	public FormRulerProvider(Form form, boolean horizontal)
	{
		this.form = form;
		this.horizontal = horizontal;
	}

	@Override
	public int getGuidePosition(Object guide)
	{
		if (guide instanceof Form)
		{
			return ((Form)guide).getWidth();
		}
		if (guide instanceof Part)
		{
			return ((Part)guide).getHeight();
		}

		return super.getGuidePosition(guide);
	}

	@Override
	public List<IPersist> getGuides()
	{
		List<IPersist> guides = new ArrayList<IPersist>();
		if (horizontal)
		{
			guides.add(form);
		}
		else
		{
			Iterator<Part> partsIt = form.getParts();
			while (partsIt.hasNext())
			{
				guides.add(partsIt.next());
			}
		}
		return guides;
	}

	@Override
	public Command getMoveGuideCommand(final Object guide, int positionDelta)
	{
		if (guide instanceof IPersist)
		{
			String property = horizontal ? "width" : "height";
			PersistPropertySource persistProperties = new PersistPropertySource((IPersist)guide, (IPersist)guide, false);
			int oldVal = ((Integer)persistProperties.getPropertyValue(property)).intValue();
			if (oldVal + positionDelta > 0)
			{
				SetValueCommand setCommand = new SetValueCommand();
				setCommand.setTarget(persistProperties);
				setCommand.setPropertyId(property);
				setCommand.setPropertyValue(new Integer(oldVal + positionDelta));
				return setCommand;
			}
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
		ServoyModelManager.getServoyModelManager().getServoyModel();
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
