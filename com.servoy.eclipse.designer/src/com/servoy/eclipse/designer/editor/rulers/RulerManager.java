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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.rulers.RulerProvider;


/**
 * Manages guides added in a viewer.
 * 
 * @author rgansevles
 *
 */
public class RulerManager
{
	private final GraphicalViewer viewer;
	private final List<RulerGuide>[] addedGuides = new List[2];

	public RulerManager(GraphicalViewer viewer)
	{
		this.viewer = viewer;
	}

	public void refreshRulers(boolean showRulers)
	{
		viewer.setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY, Boolean.valueOf(showRulers));
		refreshRuler(true);
		refreshRuler(false);
	}

	private void refreshRuler(boolean horizontal)
	{
		if (Boolean.TRUE.equals(viewer.getProperty(RulerProvider.PROPERTY_RULER_VISIBILITY)))
		{
			viewer.setProperty(horizontal ? RulerProvider.PROPERTY_HORIZONTAL_RULER : RulerProvider.PROPERTY_VERTICAL_RULER, new FormRulerProvider(this,
				horizontal));
		}
	}

	public List<RulerGuide> getGuides(boolean horizontal)
	{
		if (!Boolean.TRUE.equals(viewer.getProperty(RulerProvider.PROPERTY_RULER_VISIBILITY)))
		{
			return Collections.<RulerGuide> emptyList();
		}
		List<RulerGuide> guides = addedGuides[horizontal ? 0 : 1];
		return guides == null ? Collections.<RulerGuide> emptyList() : guides;
	}

	/**
	 * @param guide
	 * @param horizontal
	 */
	public void addGuide(RulerGuide guide, boolean horizontal)
	{
		if (addedGuides[horizontal ? 0 : 1] == null)
		{
			addedGuides[horizontal ? 0 : 1] = new ArrayList<RulerGuide>();
		}
		addedGuides[horizontal ? 0 : 1].add(guide);

		// refresh
		refreshRuler(horizontal);
	}

	/**
	 * @param guide
	 * @param horizontal
	 */
	public void removeGuide(RulerGuide guide, boolean horizontal)
	{
		if (addedGuides[horizontal ? 0 : 1] != null && addedGuides[horizontal ? 0 : 1].remove(guide))
		{
			// refresh
			refreshRuler(horizontal);
		}
	}

	/**
	 * @param guide
	 * @param horizontal
	 */
	public void refreshGuide(RulerGuide guide, boolean horizontal)
	{
		if (addedGuides[horizontal ? 0 : 1] != null && addedGuides[horizontal ? 0 : 1].contains(guide))
		{
			// refresh
			refreshRuler(horizontal);
		}
	}
}
