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

import javax.swing.border.Border;
import javax.swing.text.html.CSS;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.smart.dataui.StyledEnablePanel;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;

/**
 * Handles painting of part backgrounds using awt printing.
 *
 * @author rgansevles
 */

public class PartImageNotifier extends AbstractImageNotifier
{
	private final Part part;
	private final Form context;

	public PartImageNotifier(IApplication application, Part part, Form context)
	{
		super(application);
		this.part = part;
		this.context = context;
	}

	@Override
	protected Component createComponent()
	{
		Form form = application.getFlattenedSolution().getFlattenedForm(context);

		StyledEnablePanel comp = new StyledEnablePanel(application);
		comp.setSize(form.getWidth(), part.getHeight() - form.getPartStartYPos(part.getUUID().toString()));

		Pair<IStyleSheet, IStyleRule> pair = ComponentFactory.getStyleForBasicComponent(application, part, form);
		SolutionMetaData solution = null;
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
		{
			solution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolutionMetaData();
		}
		if (solution != null && (solution.getSolutionType() == SolutionMetaData.SOLUTION || solution.getSolutionType() == SolutionMetaData.WEB_CLIENT_ONLY))
		{
			Pair<IStyleSheet, IStyleRule> formStyle = ComponentFactory.getCSSPairStyleForForm(application, form);
			boolean formHasBgImage = formStyle != null && formStyle.getRight() != null &&
				formStyle.getRight().hasAttribute(CSS.Attribute.BACKGROUND_IMAGE.toString());

			boolean partHasBgColor = (part.getBackground() != null) ||
				(pair != null && pair.getRight() != null && pair.getRight().hasAttribute(CSS.Attribute.BACKGROUND_COLOR.toString()));
			Color bg = null;
			if (!form.getTransparent())
			{
				bg = part.getBackground();
				if (bg == null)
				{
					if (partHasBgColor)
					{
						bg = PersistHelper.createColorWithTransparencySupport(pair.getRight().getValue(CSS.Attribute.BACKGROUND_COLOR.toString()));
					}
					else if (formStyle != null && formStyle.getRight() != null && formStyle.getRight().hasAttribute(CSS.Attribute.BACKGROUND_COLOR.toString()))
					{
						bg = PersistHelper.createColorWithTransparencySupport(formStyle.getRight().getValue(CSS.Attribute.BACKGROUND_COLOR.toString()));
					}
				}
			}

			comp.setBackground(bg == null && !formHasBgImage ? Color.white : bg);

			boolean paintPartBackgroundOnTopOfImage = !form.getTransparent() && (formHasBgImage || (partHasBgColor && bg.getAlpha() < 255));

			if (paintPartBackgroundOnTopOfImage)
			{
				comp.setPaintBackgroundOnTopOfFormImage(true);
			}

			comp.setOpaque(!form.getTransparent() && !formHasBgImage && !(partHasBgColor && bg.getAlpha() < 255));
			comp.setBgColor(part.getBackground());
			if (pair != null)
			{
				comp.setCssRule(pair.getRight());
			}

		}
		else
		{
			Color bg = ComponentFactory.getPartBackground(application, part, form);
			comp.setBackground(bg == null ? Color.white : bg);
		}
		if (pair != null && pair.getRight() != null && pair.getLeft() != null)
		{
			Border border = pair.getLeft().getBorder(pair.getRight());
			if (border != null)
			{
				comp.setBorder(border);
			}
		}

		return comp;
	}
}
