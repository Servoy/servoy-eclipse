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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.border.Border;
import javax.swing.text.html.CSS;

import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.smart.dataui.StyledEnablePanel;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.Pair;

/**
 * Handles painting of form borders using awt printing.
 * 
 * @author rgansevles
 */

public class FormImageNotifier extends AbstractImageNotifier
{
	private final Form flattenedForm;

	public FormImageNotifier(IApplication application, Form flattenedForm)
	{
		super(application);
		this.flattenedForm = flattenedForm;
	}

	@Override
	protected Component createComponent()
	{
		Border border = ElementFactory.getFormBorder(application, flattenedForm);
		if (border == null)
		{
			return null;
		}
		Pair<IStyleSheet, IStyleRule> formStyle = ComponentFactory.getCSSPairStyleForForm(application, flattenedForm);

		StyledEnablePanel comp = new StyledEnablePanel(application);
		comp.setBorder(border);
		comp.setOpaque(false);
		Dimension size = flattenedForm.getSize();

		// add border insets
		Insets insets = border.getBorderInsets(comp);
		comp.setSize(new Dimension(size.width + insets.left + insets.right, size.height + insets.top + insets.bottom));

		if (formStyle != null && formStyle.getRight() != null && formStyle.getRight().hasAttribute(CSS.Attribute.BACKGROUND_IMAGE.toString()))
		{
			comp.setCssRule(formStyle.getRight());
		}
		return comp;
	}
}
