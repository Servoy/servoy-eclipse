/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.actions.DistributeRequest.Distribution;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.FormXYLayoutPolicy;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.Debug;

/**
 * @author user
 *
 */
public class SpacingCentersPack implements IServerService
{

	private final ISelectionProvider selectionProvider;
	private final BaseVisualFormEditor editorPart;

	public SpacingCentersPack(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	/**
	 * @param methodName
	 * @param args
	 */
	public Object executeMethod(final String methodName, JSONObject args)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				PersistContext[] selection = (PersistContext[])((IStructuredSelection)selectionProvider.getSelection()).toList().toArray(new PersistContext[0]);
				if (selection.length > 0)
				{
					List<ISupportBounds> elements = new ArrayList<ISupportBounds>();
					for (PersistContext persist : selection)
					{
						if (persist.getPersist() instanceof ISupportBounds)
						{
							elements.add((ISupportBounds)persist.getPersist());
						}
						else
						{
							Debug.error("Unexpected selection element for distribution:" + persist);
							return;
						}
					}
					switch (methodName)
					{
						case "vertical_centers" :
						case "vertical_pack" :
						case "vertical_spacing" :
							Collections.sort(elements, PositionComparator.YX_BOUNDS_COMPARATOR);
							break;
						case "horizontal_centers" :
						case "horizontal_pack" :
						case "horizontal_spacing" :
							Collections.sort(elements, PositionComparator.XY_BOUNDS_COMPARATOR);
					}
					List<Point> deltas = FormXYLayoutPolicy.getDistributeChildrenDeltas(elements, Distribution.valueOf(methodName.toUpperCase()));
					CompoundCommand cc = new CompoundCommand();
					for (int i = 0; i < elements.size(); i++)
					{
						Point newLocation = new Point(CSSPosition.getLocation(elements.get(i)));
						newLocation.x += deltas.get(i).x;
						newLocation.y += deltas.get(i).y;
						cc.add(new SetPropertyCommand("move", PersistPropertySource.createPersistPropertySource((IPersist)elements.get(i), false),
							StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), newLocation));
					}
					editorPart.getCommandStack().execute(cc);
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, new ArrayList<IPersist>((List)elements));
				}
			}
		});
		return null;
	}

}
