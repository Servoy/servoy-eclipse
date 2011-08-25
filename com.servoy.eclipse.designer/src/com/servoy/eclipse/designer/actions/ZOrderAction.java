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

package com.servoy.eclipse.designer.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.editor.BasePersistGraphicalEditPart;
import com.servoy.eclipse.designer.editor.GroupGraphicalEditPart;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;
import com.servoy.eclipse.designer.editor.commands.DesignerSelectionAction;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.UUID;

/**
 * @author alorincz
 *
 */
public class ZOrderAction extends DesignerSelectionAction
{

	public static final String ID_Z_ORDER_BRING_TO_FRONT = "z_order_bring_to_front"; //$NON-NLS-1$
	public static final String ID_Z_ORDER_SEND_TO_BACK = "z_order_send_to_back"; //$NON-NLS-1$
	public static final String ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP = "z_order_bring_to_front_one_step"; //$NON-NLS-1$
	public static final String ID_Z_ORDER_SEND_TO_BACK_ONE_STEP = "z_order_send_to_back_one_step"; //$NON-NLS-1$

	private static class OrderableElement
	{
		public Object element;
		public int zIndex;
		public boolean needsAdjustement;
		public UUID id;
		public int nrOfSubElements = 0;

		public OrderableElement(Object element, boolean needsAdjustement)
		{
			if (element instanceof IFlattenedPersistWrapper)
			{
				element = ((IFlattenedPersistWrapper)element).getWrappedPersist();
			}
			this.element = element;
			if (element instanceof IFormElement)
			{
				this.zIndex = ((IFormElement)element).getFormIndex();
				id = ((IFormElement)element).getUUID();
				nrOfSubElements = 1;
			}
			else if (element instanceof FormElementGroup)
			{
				Iterator<IFormElement> groupElementIterator = ((FormElementGroup)element).getElements();
				this.zIndex = -10000000;
				while (groupElementIterator.hasNext())
				{
					IFormElement fe = groupElementIterator.next();
					if (fe.getFormIndex() > zIndex)
					{
						this.zIndex = fe.getFormIndex();
						id = (fe).getUUID();
					}
					nrOfSubElements++;
				}
			}
			this.needsAdjustement = needsAdjustement;
		}

		@Override
		public boolean equals(Object o)
		{
			if (o instanceof IFormElement && element instanceof IFormElement) return ((IFormElement)element).getUUID().equals(((IFormElement)o).getUUID());
			else if (o instanceof FormElementGroup && element instanceof FormElementGroup) return ((FormElementGroup)element).getGroupID().equals(
				((FormElementGroup)o).getGroupID());

			return false;
		}

		public HashMap<UUID, IFormElement> getElements()
		{
			HashMap<UUID, IFormElement> returnList = null;
			if (element instanceof IFormElement)
			{
				returnList = new HashMap<UUID, IFormElement>(1);
				returnList.put(((IFormElement)element).getUUID(), (IFormElement)element);
			}
			else if (element instanceof FormElementGroup)
			{
				returnList = new HashMap<UUID, IFormElement>();
				Iterator<IFormElement> it = ((FormElementGroup)element).getElements();
				while (it.hasNext())
				{
					IFormElement fe = it.next();
					returnList.put(fe.getUUID(), fe);
				}
			}

			return returnList;
		}
	}

	private static class OrdarableElementZOrderComparator implements Comparator<OrderableElement>
	{
		public static OrdarableElementZOrderComparator INSTANCE = new OrdarableElementZOrderComparator();

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(OrderableElement o1, OrderableElement o2)
		{
			return o1.zIndex - o2.zIndex;
		}
	}

	private static List<OrderableElement> orderForBringToFront(List<OrderableElement> unorderedList)
	{
		if (unorderedList == null || unorderedList.isEmpty() || unorderedList.size() == 1) return unorderedList;

		ArrayList<OrderableElement> orderedElements = new ArrayList<OrderableElement>();

		for (int index = 0; index <= unorderedList.size() - 1; index++)
		{
			OrderableElement current = unorderedList.get(index);
			if (!current.needsAdjustement)
			{
				orderedElements.add(current);
			}
		}

		for (int index = 0; index <= orderedElements.size() - 1; index++)
		{
			OrderableElement current = unorderedList.get(index);
			if (current.needsAdjustement)
			{
				orderedElements.add(current);
			}
		}

		return orderedElements;
	}

	private static List<OrderableElement> orderForSendToBack(List<OrderableElement> unorderedList)
	{
		if (unorderedList == null || unorderedList.isEmpty() || unorderedList.size() == 1) return unorderedList;

		ArrayList<OrderableElement> orderedElements = new ArrayList<OrderableElement>();

		for (int index = 0; index <= unorderedList.size() - 1; index++)
		{
			OrderableElement current = unorderedList.get(index);
			if (current.needsAdjustement)
			{
				orderedElements.add(current);
			}
		}

		for (int index = 0; index <= orderedElements.size() - 1; index++)
		{
			OrderableElement current = unorderedList.get(index);
			if (!current.needsAdjustement)
			{
				orderedElements.add(current);
			}
		}

		return orderedElements;
	}

	private static List<OrderableElement> orderForBringForward(List<OrderableElement> unorderedList)
	{
		if (unorderedList == null || unorderedList.isEmpty() || unorderedList.size() == 1) return unorderedList;

		ArrayList<OrderableElement> orderedList = new ArrayList<OrderableElement>();

		OrderableElement[] orderedArray = new OrderableElement[unorderedList.size() + 1];

		for (int index = 0; index < unorderedList.size(); index++)
		{
			OrderableElement current = unorderedList.get(index);
			if (current.needsAdjustement)
			{
				orderedArray[index + 1] = current;
			}
		}

		for (int index = unorderedList.size() - 1; index >= 0; index--)
		{
			OrderableElement current = unorderedList.get(index);
			if (!current.needsAdjustement)
			{
				int freeSlot = index;
				for (; orderedArray[freeSlot] != null && freeSlot >= 0;)
				{
					freeSlot--;
				}
				orderedArray[freeSlot] = current;
			}
		}

		for (OrderableElement oe : orderedArray)
		{
			if (oe != null) orderedList.add(oe);
		}

		return orderedList;
	}

	private static List<OrderableElement> orderForSendBackward(List<OrderableElement> unorderedList)
	{
		if (unorderedList == null || unorderedList.isEmpty() || unorderedList.size() == 1) return unorderedList;

		ArrayList<OrderableElement> orderedList = new ArrayList<OrderableElement>();

		OrderableElement[] orderedArray = new OrderableElement[unorderedList.size() + 1];

		for (int index = 0; index < unorderedList.size(); index++)
		{
			OrderableElement current = unorderedList.get(index);
			if (current.needsAdjustement)
			{
				orderedArray[index] = current;
			}
		}

		for (int index = 0; index < unorderedList.size(); index++)
		{
			OrderableElement current = unorderedList.get(index);
			if (!current.needsAdjustement)
			{
				int freeSlot = index + 1;
				for (; orderedArray[freeSlot] != null && freeSlot < unorderedList.size();)
				{
					freeSlot++;
				}
				orderedArray[freeSlot] = current;
			}
		}

		for (OrderableElement oe : orderedArray)
		{
			if (oe != null) orderedList.add(oe);
		}

		return orderedList;
	}

	protected static List<OrderableElement> calculateNewZOrder(Form form, List<Object> models, String zOrderId)
	{
		Form flattenedForm = ModelUtils.getEditingFlattenedSolution(form).getFlattenedForm(form);

		HashMap<UUID, OrderableElement> elementsToAdjust = new HashMap<UUID, OrderableElement>();

		List<OrderableElement> adjustedElements = new ArrayList<OrderableElement>();

		for (Object o : models)
		{
			if (o instanceof IFormElement || o instanceof FormElementGroup)
			{
				OrderableElement oe = new OrderableElement(o, true);
				elementsToAdjust.put(oe.id, oe);
			}
		}

		for (OrderableElement oe : elementsToAdjust.values())
		{
			if (!oe.needsAdjustement) continue;

			HashMap<UUID, IFormElement> neighbours = ElementUtil.getNeighbours(flattenedForm, oe.getElements(), oe.getElements());

			ArrayList<OrderableElement> unorderedNeighbours = groupElements(neighbours, form);
			Collections.sort(unorderedNeighbours, OrdarableElementZOrderComparator.INSTANCE);

			for (OrderableElement element : unorderedNeighbours)
			{
				if (elementsToAdjust.containsKey(element.id)) element.needsAdjustement = true;
			}

			List<OrderableElement> tmpAdjustedElements = null;

			if (ID_Z_ORDER_SEND_TO_BACK.equals(zOrderId))
			{
				tmpAdjustedElements = orderForSendToBack(unorderedNeighbours);
			}
			else if (ID_Z_ORDER_BRING_TO_FRONT.equals(zOrderId))
			{
				tmpAdjustedElements = orderForBringToFront(unorderedNeighbours);
			}
			else if (ID_Z_ORDER_SEND_TO_BACK_ONE_STEP.equals(zOrderId))
			{
				tmpAdjustedElements = orderForSendBackward(unorderedNeighbours);
			}
			else if (ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP.equals(zOrderId))
			{
				tmpAdjustedElements = orderForBringForward(unorderedNeighbours);
			}

			int index = 0;
			for (OrderableElement ae : tmpAdjustedElements)
			{
				if (elementsToAdjust.containsKey(ae.id)) ae.needsAdjustement = false;
				if (ae.element instanceof IFormElement) ae.zIndex = index++;
				else
				{
					ae.zIndex = index + ae.nrOfSubElements - 1;
					index = index + ae.nrOfSubElements;
				}
				adjustedElements.add(ae);
			}
		}

		return adjustedElements;
	}

	private static ArrayList<OrderableElement> groupElements(HashMap<UUID, IFormElement> elementList, Form form)
	{
		if (elementList == null) return null;

		ArrayList<OrderableElement> returnList = new ArrayList<OrderableElement>();

		List<IFormElement> simpleElements = new ArrayList<IFormElement>();
		Set<String> groupsSet = new LinkedHashSet<String>();

		for (IFormElement bc : elementList.values())
		{
			String groupID = bc.getGroupID();
			if (groupID == null) simpleElements.add(bc);
			else
			{
				groupsSet.add(groupID);
			}
		}

		for (IFormElement bc : simpleElements)
		{
			OrderableElement oe = new OrderableElement(bc, false);
			returnList.add(oe);
		}

		for (String s : groupsSet)
		{
			OrderableElement oe = new OrderableElement(new FormElementGroup(s, ModelUtils.getEditingFlattenedSolution(form), form), false);
			returnList.add(oe);
		}

		return returnList;
	}

	public static Map<EditPart, Request> createZOrderRequests(String zOrderId, List<EditPart> selected)
	{
		if (selected == null || selected.isEmpty()) return null;


		Map<EditPart, Request> requests = null;

		List<Object> models = new ArrayList<Object>();

		EditPart firstEditPart = selected.get(0);

		EditPart formEditPart = firstEditPart.getParent();
		while (formEditPart != null && !(formEditPart.getModel() instanceof Form))
		{
			formEditPart = formEditPart.getParent();
		}

		if (formEditPart == null) return null;

		for (EditPart editPart : selected)
		{
			if (editPart instanceof GraphicalEditPart)
			{
				models.add(editPart.getModel());
			}
		}

		if (models.isEmpty()) return null;

		Map<Object, EditPart> editPartMap = firstEditPart.getViewer().getEditPartRegistry();

		List<OrderableElement> orderedList = calculateNewZOrder((Form)formEditPart.getModel(), models, zOrderId);
		if (orderedList != null)
		{
			for (OrderableElement oe : orderedList)
			{
				if (requests == null)
				{
					requests = new HashMap<EditPart, Request>();
				}
				if (oe.element instanceof IFormElement) requests.put(editPartMap.get(oe.element), new SetPropertyRequest(VisualFormEditor.REQ_SET_PROPERTY,
					StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName(), Integer.valueOf(oe.zIndex), "")); //$NON-NLS-1$
				else
				{
					ArrayList<IFormElement> groupElements = new ArrayList<IFormElement>(oe.getElements().values());
					Collections.sort(groupElements, new Comparator<IFormElement>()
					{
						/*
						 * (non-Javadoc)
						 * 
						 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
						 */
						public int compare(IFormElement o1, IFormElement o2)
						{
							return o1.getFormIndex() - o2.getFormIndex();
						}
					});
					int index = oe.zIndex - oe.nrOfSubElements + 1;
					for (IFormElement bc : groupElements)
					{
						requests.put(
							editPartMap.get(bc),
							new SetPropertyRequest(VisualFormEditor.REQ_SET_PROPERTY, StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName(),
								Integer.valueOf(index++), "")); //$NON-NLS-1$
					}
				}
			}
		}

		return requests;
	}

	private final String zOrderType;

	public ZOrderAction(IWorkbenchPart part, Object requestType)
	{
		super(part, requestType);
		initAction((String)requestType);
		zOrderType = (String)requestType;
	}

	public void initAction(String zOrderType)
	{
		if (zOrderType.equals(ID_Z_ORDER_BRING_TO_FRONT))
		{
			setText(DesignerActionFactory.BRING_TO_FRONT_TEXT);
			setToolTipText(DesignerActionFactory.BRING_TO_FRONT_TOOLTIP);
			setImageDescriptor(DesignerActionFactory.BRING_TO_FRONT_IMAGE);
			setId(ID_Z_ORDER_BRING_TO_FRONT);
		}
		else if (zOrderType.equals(ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP))
		{
			setText(DesignerActionFactory.BRING_TO_FRONT_ONE_STEP_TEXT);
			setToolTipText(DesignerActionFactory.BRING_TO_FRONT_ONE_STEP_TOOLTIP);
			setImageDescriptor(DesignerActionFactory.BRING_TO_FRONT_ONE_STEP_IMAGE);
			setId(ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP);
		}
		else if (zOrderType.equals(ID_Z_ORDER_SEND_TO_BACK))
		{
			setText(DesignerActionFactory.SEND_TO_BACK_TEXT);
			setToolTipText(DesignerActionFactory.SEND_TO_BACK_TOOLTIP);
			setImageDescriptor(DesignerActionFactory.SEND_TO_BACK_IMAGE);
			setId(ID_Z_ORDER_SEND_TO_BACK);
		}
		else if (zOrderType.equals(ID_Z_ORDER_SEND_TO_BACK_ONE_STEP))
		{
			setText(DesignerActionFactory.SEND_TO_BACK_ONE_STEP_TEXT);
			setToolTipText(DesignerActionFactory.SEND_TO_BACK_ONE_STEP_TOOLTIP);
			setImageDescriptor(DesignerActionFactory.SEND_TO_BACK_ONE_STEP_IMAGE);
			setId(ID_Z_ORDER_SEND_TO_BACK_ONE_STEP);
		}
	}

	@Override
	protected Iterable<EditPart> getToRefresh(Iterable<EditPart> affected)
	{
		return DesignerUtil.getFormEditparts(affected);
	}

	@Override
	protected Map<EditPart, Request> createRequests(List<EditPart> selected)
	{
		return createZOrderRequests(getId(), selected);
	}

	@Override
	protected boolean calculateEnabled()
	{
		List selectedObjects = getSelectedObjects();
		for (Object o : selectedObjects)
		{
			if (o instanceof BasePersistGraphicalEditPart || o instanceof GroupGraphicalEditPart) return true;
		}

		return false;
	}
}
