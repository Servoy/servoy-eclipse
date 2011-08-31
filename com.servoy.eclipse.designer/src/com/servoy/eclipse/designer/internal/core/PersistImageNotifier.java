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

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import com.servoy.eclipse.core.DesignComponentFactory;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;


/**
 * Handles painting of form editor elements using awt printing.
 * 
 * @author rgansevles
 */

public class PersistImageNotifier extends AbstractImageNotifier
{
	private final IPersist persist;
	private final Form form;

	public PersistImageNotifier(IApplication application, IPersist persist, Form form)
	{
		super(application);
		this.persist = persist;
		this.form = form;
	}

	@Override
	protected Component createComponent()
	{
		FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(persist);
		Component comp = null;
		if (persist instanceof Bean)
		{
			Object beanInstance = DesignComponentFactory.getBeanDesignInstance(application, editingFlattenedSolution, (Bean)persist, form);
			if (beanInstance instanceof Component)
			{
				comp = (Component)beanInstance;
			}
		}
		if (comp == null)
		{
			comp = DesignComponentFactory.createDesignComponent(application, editingFlattenedSolution, persist, form);
		}

		if (persist instanceof AbstractBase)
		{
			Color background = null;
			if (comp instanceof JComponent && ((JComponent)comp).isOpaque() && comp.isBackgroundSet())
			{
				background = comp.getBackground();
				if (comp instanceof JScrollPane && ((JScrollPane)comp).getViewport().getView() instanceof JList)
				{
					ListCellRenderer renderer = ((JList)((JScrollPane)comp).getViewport().getView()).getCellRenderer();
					if (renderer != null)
					{
						Component rendererComponent = renderer.getListCellRendererComponent((JList)((JScrollPane)comp).getViewport().getView(), null, 0, false,
							false);
						if (rendererComponent != null)
						{
							background = rendererComponent.getBackground();
						}
					}
				}
			}
			((AbstractBase)persist).setRuntimeProperty(PersistPropertySource.LastPaintedBackgroundProperty, background);
			((AbstractBase)persist).setRuntimeProperty(PersistPropertySource.LastPaintedFontProperty, comp.getFont());
		}

		return comp;
	}

	@Override
	protected float handleAlpha(Component component)
	{
		if (component.isVisible())
		{
			return super.handleAlpha(component);
		}

		// paint 'invisible' elements with some translucency.
		component.setVisible(true);
		return 0.3f;
	}
}
