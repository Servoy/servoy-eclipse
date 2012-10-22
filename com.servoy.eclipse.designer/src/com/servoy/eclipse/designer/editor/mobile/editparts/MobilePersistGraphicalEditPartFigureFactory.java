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

package com.servoy.eclipse.designer.editor.mobile.editparts;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JLabel;

import org.eclipse.gef.GraphicalEditPart;

import com.servoy.eclipse.designer.editor.IFigureFactory;
import com.servoy.eclipse.designer.editor.PersistImageFigure;
import com.servoy.eclipse.designer.editor.SetBoundsToSupportBoundsFigureListener;
import com.servoy.eclipse.designer.internal.core.IImageNotifier;
import com.servoy.eclipse.designer.internal.core.PersistImageNotifier;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.util.gui.RoundedBorder;

/**
 * Factory for creating figures for persists in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobilePersistGraphicalEditPartFigureFactory implements IFigureFactory<PersistImageFigure>
{
	public final static Color BUTTON_BG = new Color(59, 59, 59); // TODO: use theme
	public final static Color BUTTON_BORDER = new Color(135, 135, 135); // TODO: use theme
	public final static Color LABEL_FG = new Color(119, 119, 97); // TODO: use theme


	private final IApplication application;
	private final Form form;

	public MobilePersistGraphicalEditPartFigureFactory(IApplication application, Form form)
	{
		this.application = application;
		this.form = form;
	}

	public PersistImageFigure createFigure(final GraphicalEditPart editPart)
	{
		final IPersist persist = (IPersist)editPart.getModel();
		PersistImageFigure figure = new PersistImageFigure(application, persist, form)
		{
			@Override
			protected IImageNotifier createImageNotifier()
			{
				return new PersistImageNotifier(application, persist, form, this)
				{
					@Override
					protected Component createComponent()
					{
						Component component = super.createComponent();
						if (component instanceof JButton)
						{
							((JButton)component).setBackground(BUTTON_BG);
							((JButton)component).setForeground(Color.white);
							RoundedBorder rborder = new RoundedBorder(1, 1, 1, 1, BUTTON_BORDER, BUTTON_BORDER, BUTTON_BORDER, BUTTON_BORDER);
							((JButton)component).setBorder(rborder);
						}
						else if (component instanceof JLabel && persist instanceof AbstractBase &&
							((AbstractBase)persist).getCustomMobileProperty("headerText") != null)
						{
							((JLabel)component).setForeground(LABEL_FG);
						}
						component.setFont(component.getFont().deriveFont(12f)); // 12 pt font
						return component;
					}
				};
			}
		};

		figure.addFigureListener(new SetBoundsToSupportBoundsFigureListener((ISupportBounds)editPart.getModel()));
		return figure;
	}
}
