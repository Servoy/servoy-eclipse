/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.swt.widgets.Composite;

/**
 * Composite to show rulers.
 * Extends default RulerComposite to set own key handlers.
 * 
 * @author rgansevles
 *
 */
public class FormRulerComposite extends RulerComposite
{
	/**
	 * @param parent
	 * @param style
	 */
	public FormRulerComposite(Composite parent, int style)
	{
		super(parent, style);
	}

	@Override
	public void setGraphicalViewer(final ScrollingGraphicalViewer primaryViewer)
	{
		super.setGraphicalViewer(primaryViewer);
		primaryViewer.addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (RulerProvider.PROPERTY_RULER_VISIBILITY.equals(evt.getPropertyName()))
				{
					// set key handlers a bit later, top and left may not have been created yet
					getDisplay().asyncExec(new Runnable()
					{
						public void run()
						{
							setkeyHandlers(primaryViewer);
						}
					});
				}
			}
		});
		setkeyHandlers(primaryViewer);
	}

	protected void setkeyHandlers(ScrollingGraphicalViewer primaryViewer)
	{
		// replace the key handlers
		if (Boolean.TRUE.equals(primaryViewer.getProperty(RulerProvider.PROPERTY_RULER_VISIBILITY)))
		{
			getTop().setKeyHandler(new FormRulerKeyHandler(getTop()));
			getLeft().setKeyHandler(new FormRulerKeyHandler(getLeft()));
		}
	}
}