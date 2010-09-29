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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.designer.property.IPersistEditPart;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.util.SnapToGridFieldPositioner;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;

/**
 * The Contents Graphical Edit Part for a Servoy Form.
 */
public class FormGraphicalEditPart extends AbstractGraphicalEditPart implements IPersistEditPart
{
	private final IApplication application;
	private final LastClickMouseListener mouseListener = new LastClickMouseListener();
	private final VisualFormEditor editorPart;
	private SnapToGridFieldPositioner snapToGridIFieldPositioner;

	public FormGraphicalEditPart(IApplication application, VisualFormEditor editorPart)
	{
		this.application = application;
		this.editorPart = editorPart;
		setModel(editorPart.getForm());
	}

	@Override
	protected List<Object> getModelChildren()
	{
		List<Object> list = new ArrayList<Object>();
		List<Part> parts = new ArrayList<Part>();
		Form flattenedForm = editorPart.getFlattenedForm();
		Set<FormElementGroup> groups = new HashSet<FormElementGroup>();
		if (flattenedForm != null)
		{
			Iterator<IPersist> it = flattenedForm.getAllObjectsSortedByFormIndex();
			while (it.hasNext())
			{
				IPersist o = it.next();
				if (o instanceof Part)
				{
					parts.add((Part)o);
				}
				else if (o instanceof IFormElement)
				{
					if (((IFormElement)o).getGroupID() != null)
					{
						FormElementGroup group = new FormElementGroup(((IFormElement)o).getGroupID(), (ISupportChilds)getModel());
						if (groups.add(group))
						{
							list.add(group);
						}
					}
					else
					{
						list.add(o);
						Iterator<IPersist> subElements = null;
						if (o instanceof TabPanel)
						{
							subElements = ((TabPanel)o).getTabs();
						}
						else if (o instanceof Portal)
						{
							subElements = ((Portal)o).getAllObjects();
						}
						while (subElements != null && subElements.hasNext())
						{
							list.add(subElements.next());
						}
					}
				}
			}
		}

		// parts go on top
		list.addAll(parts);
		return list;
	}

	@Override
	protected IFigure createFigure()
	{
		FreeformLayer formLayer = new FreeformLayer();
		formLayer.setLayoutManager(new FormLayout());
		return formLayer;
	}


	@Override
	public void activate()
	{
		super.activate();
		getViewer().getControl().addMouseListener(mouseListener);
	}

	@Override
	public void deactivate()
	{
		getViewer().getControl().removeMouseListener(mouseListener);
		snapToGridIFieldPositioner = null;
		super.deactivate();
	}

	@Override
	protected void createEditPolicies()
	{
		installEditPolicy(PasteToSupportChildsEditPolicy.PASTE_ROLE, new PasteToSupportChildsEditPolicy(getFieldPositioner()));
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new FormXYLayoutPolicy(this));
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new FormEditPolicy(getFieldPositioner()));
	}

	/**
	 * Create the child edit part.
	 */
	@Override
	protected EditPart createChild(Object child)
	{
		return createChild(application, editorPart, (Form)getModel(), child);
	}

	/**
	 * Create the child edit part.
	 */
	protected static EditPart createChild(IApplication application, VisualFormEditor editorPart, Form form, Object child)
	{
		if (child instanceof Part)
		{
			return new FormPartGraphicalEditPart(application, editorPart, (Part)child, ElementUtil.isReadOnlyFormElement(form, child));
		}
		if (child instanceof Tab)
		{
			return new TabFormGraphicalEditPart(application, (Tab)child, ElementUtil.isReadOnlyFormElement(form, child));
		}
		if (child instanceof FormElementGroup)
		{
			return new GroupGraphicalEditPart(application, editorPart, form, (FormElementGroup)child);
		}
		return new PersistGraphicalEditPart(application, (IPersist)child, form, ElementUtil.isReadOnlyFormElement(form, child));
	}

	@Override
	public Object getAdapter(Class key)
	{
		if (key == SnapToHelper.class)
		{
			if (Boolean.TRUE.equals(getViewer().getProperty(SnapToGrid.PROPERTY_GRID_ENABLED)))
			{
				// The GEF implementation of SnapToGrid adds one extra pixel when the components are snapped to EAST or SOUTH.
				// We do a correction in order to remove that extra 1px.
				if (false)
				{
					return new SnapToGrid(this)
					{
						@Override
						public int snapRectangle(Request request, int snapLocations, org.eclipse.draw2d.geometry.PrecisionRectangle rect,
							org.eclipse.draw2d.geometry.PrecisionRectangle result)
						{
							int alteredSnapLocations = super.snapRectangle(request, snapLocations, rect, result);
							boolean changed = false;
							if ((gridX > 0) && ((snapLocations & EAST) != 0) && ((alteredSnapLocations & EAST) == 0))
							{
								result.preciseWidth -= Math.IEEEremainder(rect.preciseWidth + result.preciseWidth, gridX);
								changed = true;
							}
							if ((gridY > 0) && ((snapLocations & SOUTH) != 0) && ((alteredSnapLocations & SOUTH) == 0))
							{
								result.preciseHeight -= Math.IEEEremainder(rect.preciseHeight + result.preciseHeight, gridY);
								changed = true;
							}
							if (changed) result.updateInts();
							return alteredSnapLocations;
						}
					};
				}
				return new SnapToElementAlignment(this);
			}
		}
		return super.getAdapter(key);
	}

	public SnapToGridFieldPositioner getFieldPositioner()
	{
		if (snapToGridIFieldPositioner == null)
		{
			DesignerPreferences designerPreferences = new DesignerPreferences(ServoyModel.getSettings());
			snapToGridIFieldPositioner = new SnapToGridFieldPositioner(designerPreferences)
			{
				@Override
				protected boolean getGridSnapTo()
				{
					return Boolean.TRUE.equals(getViewer().getProperty(SnapToGrid.PROPERTY_GRID_ENABLED));
				}
			};
			snapToGridIFieldPositioner.setDefaultLocation(new Point(60, 70));
		}
		return snapToGridIFieldPositioner;
	}

	/**
	 * Keep the last click position
	 */

	public class LastClickMouseListener implements MouseListener
	{
		public void mouseDoubleClick(MouseEvent e)
		{
			mouseDown(e);
		}

		public void mouseDown(MouseEvent e)
		{
			Rectangle.SINGLETON.setLocation(e.x, e.y);
			// translate to take scrollbar position into account
			getFigure().translateToRelative(Rectangle.SINGLETON);
			getFieldPositioner().setDefaultLocation(new Point(Rectangle.SINGLETON.x, Rectangle.SINGLETON.y));
		}

		public void mouseUp(MouseEvent e)
		{
		}
	}

	@Override
	protected void refreshChildren()
	{
		editorPart.refreshFlattenedForm();
		super.refreshChildren();
	}

	@Override
	protected void refreshVisuals()
	{
		getFigure().repaint();
		super.refreshVisuals();
	}

	public Form getPersist()
	{
		return (Form)getModel();
	}

	public boolean isReadOnly()
	{
		return false;
	}
}
