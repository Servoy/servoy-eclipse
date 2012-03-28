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
package com.servoy.eclipse.designer.internal.core;

import java.awt.Color;
import java.awt.Component;

import javax.swing.text.html.CSS;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.smart.dataui.StyledEnablePanel;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.Pair;

/**
 * Handles painting of part backgrounds using awt printing.
 * 
 * @author rgansevles
 */

public class PartImageNotifier extends AbstractImageNotifier
{
	private final Part part;

	public PartImageNotifier(IApplication application, Part part)
	{
		super(application);
		this.part = part;
	}

	@Override
	protected Component createComponent()
	{
		Form form = application.getFlattenedSolution().getFlattenedForm(part);

		StyledEnablePanel comp = new StyledEnablePanel(application);
		comp.setSize(form.getWidth(), part.getHeight() - form.getPartStartYPos(part.getID()));

		Color bg = ComponentFactory.getPartBackground(application, part, form);
		comp.setBackground(bg == null ? Color.white : bg);

		Pair<IStyleSheet, IStyleRule> formStyle = ComponentFactory.getCSSPairStyleForForm(application, form);
		boolean formHasBgImage = formStyle != null && formStyle.getRight() != null &&
			formStyle.getRight().hasAttribute(CSS.Attribute.BACKGROUND_IMAGE.toString());

		Pair<IStyleSheet, IStyleRule> pair = ComponentFactory.getStyleForBasicComponent(application, part, form);
		boolean partHasBgColor = (part.getBackground() != null) ||
			(pair != null && pair.getRight() != null && pair.getRight().hasAttribute(CSS.Attribute.BACKGROUND_COLOR.toString()));

		boolean paintPartBackgroundOnTopOfImage = formHasBgImage && !form.getTransparent() && partHasBgColor;

		if (paintPartBackgroundOnTopOfImage)
		{
			comp.setPaintBackgroundOnTopOfFormImage(true);
		}

		comp.setOpaque(!form.getTransparent() && !formHasBgImage);

		if (pair != null && pair.getRight() != null && pair.getRight().hasAttribute(CSS.Attribute.BACKGROUND_IMAGE.toString()))
		{
			comp.setCssRule(pair.getRight());
		}

		return comp;
	}
}
