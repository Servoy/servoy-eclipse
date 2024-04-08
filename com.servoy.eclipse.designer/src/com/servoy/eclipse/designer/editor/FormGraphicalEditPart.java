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
import java.util.Comparator;
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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;

import com.servoy.eclipse.designer.editor.FormBorderGraphicalEditPart.BorderModel;
import com.servoy.eclipse.designer.editor.FormPartpanelGraphicalEditPart.PartpanelModel;
import com.servoy.eclipse.designer.property.IFieldPositionerProvider;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.util.SnapToGridFieldPositioner;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * The Contents Graphical Edit Part for a Servoy Form.
 *
 * @author rgansevles
 */
public class FormGraphicalEditPart extends BaseFormGraphicalEditPart implements IFieldPositionerProvider
{
	private final LastClickMouseListener mouseListener = new LastClickMouseListener();
	private SnapToGridFieldPositioner snapToGridIFieldPositioner;

	public FormGraphicalEditPart(IApplication application, BaseVisualFormEditor editorPart)
	{
		super(application, editorPart);
	}

	@Override
	protected List<Object> getModelChildren()
	{
		Form flattenedForm = ModelUtils.getEditingFlattenedSolution(getPersist()).getFlattenedForm(getPersist());
		List<Object> list = new ArrayList<Object>();

		list.add(new BorderModel(getPersist())); // A separate editpart to show the form border and resize handles
		for (Part part : Utils.iterate(flattenedForm.getParts()))
		{
			// separate editparts for painting part backgrounds
			list.add(new PartpanelModel(part, getPersist()));
		}

		Set<FormElementGroup> groups = new HashSet<FormElementGroup>();
		if (flattenedForm != null)
		{
			for (IFormElement o : Utils.iterate(flattenedForm.getFormElementsSortedByFormIndex()))
			{
				if (Boolean.TRUE.equals(getViewer().getProperty(VisualFormEditor.PROPERTY_HIDE_INHERITED)) && !getPersist().equals(o.getParent()))
				{
					// Hide inherited elements
					continue;
				}

				if (o instanceof ISupportExtendsID && PersistHelper.isOverrideOrphanElement(o))
				{
					// skip orphaned overrides
					continue;
				}
				if (o.getGroupID() != null)
				{
					// use persist.getparent as form in the group, not the current form.
					// when the group elements are not overrides yet and a group property is changed, an different FormElementGroup is created with this form as parent.
					FormElementGroup group = new FormElementGroup((o).getGroupID(), ModelUtils.getEditingFlattenedSolution(getPersist()), (Form)o.getParent());
					if (groups.add(group))
					{
						list.add(group);
					}
				}
				else
				{
					// don't use FlattenedTabPanel or FlattenedPortal for model, use the wrapped tabpanel/portal
					list.add(o instanceof IFlattenedPersistWrapper ? ((IFlattenedPersistWrapper< ? >)o).getWrappedPersist() : o);
					Iterator<IPersist> subElements = null;
					if (o instanceof TabPanel)
					{
						subElements = ((TabPanel)o).getTabs();
					}
					else if (o instanceof Portal)
					{
						subElements = ((Portal)o).getAllObjects(new Comparator<IPersist>()
						{
							public int compare(IPersist persist1, IPersist persist2)
							{
								if (persist1 instanceof IFormElement && persist2 instanceof IFormElement)
								{
									return ((IFormElement)persist1).getFormIndex() - ((IFormElement)persist2).getFormIndex();
								}
								return 0;
							}
						});
					}
					while (subElements != null && subElements.hasNext())
					{
						list.add(subElements.next());
					}
				}
			}
		}

		// parts go on top
		for (Part part : Utils.iterate(flattenedForm.getParts()))
		{
			list.add(part);
		}

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
		installEditPolicy(PasteToSupportChildsEditPolicy.PASTE_ROLE, new PasteToSupportChildsEditPolicy(getApplication(), getFieldPositioner()));
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new FormXYLayoutPolicy(getApplication(), this));
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new FormEditPolicy(getApplication(), getFieldPositioner()));
	}

	/**
	 * Create the child edit part.
	 */
	@Override
	protected EditPart createChild(Object child)
	{
		return createChild(getApplication(), getEditorPart(), (Form)getModel(), child);
	}

	/**
	 * Create the child edit part.
	 */
	public static EditPart createChild(IApplication application, BaseVisualFormEditor editorPart, Form form, Object child)
	{
		if (child instanceof BorderModel)
		{
			return new FormBorderGraphicalEditPart(application, (BorderModel)child);
		}
		if (child instanceof PartpanelModel)
		{
			return new FormPartpanelGraphicalEditPart(application, (PartpanelModel)child);
		}
		if (child instanceof Part)
		{
			return new FormPartGraphicalEditPart(application, editorPart, (Part)child, Utils.isInheritedFormElement(child, form));
		}
		if (child instanceof Tab)
		{
			return new TabFormGraphicalEditPart(application, (Tab)child, Utils.isInheritedFormElement(child, form));
		}
		if (child instanceof FormElementGroup)
		{
			return new GroupGraphicalEditPart(application, editorPart, form, (FormElementGroup)child);
		}
		return new PersistGraphicalEditPart(application, (IPersist)child, form, Utils.isInheritedFormElement(child, form),
			new PersistGraphicalEditPartFigureFactory(application, form));
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
				return new SnapToGrid(this)
				{
					@Override
					public int snapRectangle(Request request, int snapLocations, org.eclipse.draw2d.geometry.PrecisionRectangle rect,
						org.eclipse.draw2d.geometry.PrecisionRectangle result)
					{
						int alteredSnapLocations = super.snapRectangle(request, snapLocations, rect, result);
						if ((gridX > 0) && ((snapLocations & EAST) != 0) && ((alteredSnapLocations & EAST) == 0))
						{
							result.setPreciseWidth(result.preciseWidth() - Math.IEEEremainder(rect.preciseWidth() + result.preciseWidth(), gridX));
						}
						if ((gridY > 0) && ((snapLocations & SOUTH) != 0) && ((alteredSnapLocations & SOUTH) == 0))
						{
							result.setPreciseHeight(result.preciseHeight() - Math.IEEEremainder(rect.preciseHeight() + result.preciseHeight(), gridY));
						}
						return alteredSnapLocations;
					}
				};
			}
			if (Boolean.TRUE.equals(getViewer().getProperty(SnapToElementAlignment.PROPERTY_ALIGNMENT_ENABLED)))
			{
				return new SnapToElementAlignment(this);
			}
		}
		return super.getAdapter(key);
	}

	public SnapToGridFieldPositioner getFieldPositioner()
	{
		if (snapToGridIFieldPositioner == null)
		{
			DesignerPreferences designerPreferences = new DesignerPreferences();
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
	protected void refreshVisuals()
	{
		getFigure().repaint();
		super.refreshVisuals();
	}

	/*
	 * Added because refreshing forms with many children (specially tabpanels) leads to performance degradation (refresh takes too long).
	 */
	@Override
	public void refreshWithoutChildren()
	{
		refreshVisuals();
		refreshFormEditParts();
		refreshSourceConnections();
		refreshTargetConnections();
	}

	/**
	 * Refresh edit parts that are based on the same form, not edit parts that are based on form children.
	 */
	protected void refreshFormEditParts()
	{

		List<EditPart> toRefresh = new ArrayList<EditPart>(2);

		for (EditPart ep : (List<EditPart>)getChildren())
		{
			if (ep.getModel() instanceof BorderModel && getModel().equals(((BorderModel)ep.getModel()).form))
			{
				toRefresh.add(ep);
			}
			else if (ep.getModel() instanceof PartpanelModel && getModel().equals(((PartpanelModel)ep.getModel()).context))
			{
				toRefresh.add(ep);
			}
		}
		for (EditPart ep : toRefresh)
		{
			ep.refresh();
		}
	}

	@Override
	public Form getPersist()
	{
		return (Form)getModel();
	}

	@Override
	public boolean isInherited()
	{
		return false;
	}
}
