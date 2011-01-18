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
package com.servoy.eclipse.designer.editor.rulers;

import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.rulers.RulerProvider;

import com.servoy.eclipse.ui.preferences.DesignerPreferences;

/**
 * Ruler provider for form designer, vertical rule is form width, horizontal rules are part boundaries.
 * 
 * @author rgansevles
 */

public class FormRulerProvider extends RulerProvider
{
	private final boolean horizontal;
	private final RulerManager rulerManager;

	public FormRulerProvider(RulerManager rulerManager, boolean horizontal)
	{
		this.rulerManager = rulerManager;
		this.horizontal = horizontal;
	}

	public boolean isHorizontal()
	{
		return horizontal;
	}

	@Override
	public int getGuidePosition(Object guide)
	{
		if (guide instanceof RulerGuide)
		{
			return ((RulerGuide)guide).getPosition();
		}

		return super.getGuidePosition(guide);
	}

	@Override
	public List<RulerGuide> getGuides()
	{
		return rulerManager.getGuides(horizontal);
	}

	@Override
	public int[] getGuidePositions()
	{
		List<RulerGuide> guides = getGuides();
		int[] guidePositions = new int[guides.size()];
		for (int i = 0; i < guides.size(); i++)
		{
			guidePositions[i] = getGuidePosition(guides.get(i));
		}
		return guidePositions;
	}

	@Override
	public Command getMoveGuideCommand(Object guide, int positionDelta)
	{
		if (guide instanceof RulerGuide)
		{
			return new MoveGuideCommand(this, (RulerGuide)guide, positionDelta);
		}

		return super.getMoveGuideCommand(guide, positionDelta);
	}

	@Override
	public Command getCreateGuideCommand(int position)
	{
		return new CreateGuideCommand(this, position);
	}

	@Override
	public Command getDeleteGuideCommand(Object guide)
	{
		if (guide instanceof RulerGuide)
		{
			return new DeleteGuideCommand(this, (RulerGuide)guide);
		}
		return super.getDeleteGuideCommand(guide);
	}

	@Override
	public Object getRuler()
	{
		return this;
	}

	@Override
	public int getUnit()
	{
		DesignerPreferences designerPreferences = new DesignerPreferences();
		switch (designerPreferences.getMetrics())
		{
			case DesignerPreferences.CM :
				return RulerProvider.UNIT_CENTIMETERS;
			case DesignerPreferences.IN :
				return RulerProvider.UNIT_INCHES;
		}
		return RulerProvider.UNIT_PIXELS;
	}

	/**
	 * @param guide
	 */
	public void addGuide(RulerGuide guide)
	{
		rulerManager.addGuide(guide, isHorizontal());
	}

	/**
	 * @param guide
	 */
	public void removeGuide(RulerGuide guide)
	{
		rulerManager.removeGuide(guide, isHorizontal());
	}

	/**
	 * @param guide
	 */
	public void refreshGuide(RulerGuide guide)
	{
		rulerManager.refreshGuide(guide, isHorizontal());
	}

}
