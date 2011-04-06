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
import java.util.List;
import java.util.Map;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;
import com.servoy.eclipse.designer.editor.commands.DesignerSelectionAction;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.UUID;

/**
 * Action responsible for handling the layering adjustments:
 *  - bring forward (one layer)
 *  - send backward (one layer)
 *  - bring to front
 *  - send to back
 * 
 * @author alorincz
 *
 */
public class ZOrderAction extends DesignerSelectionAction
{
	public static final String ID_Z_ORDER_BRING_TO_FRONT = "z_order_bring_to_front";
	public static final String ID_Z_ORDER_SEND_TO_BACK = "z_order_send_to_back";
	public static final String ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP = "z_order_bring_to_front_one_step";
	public static final String ID_Z_ORDER_SEND_TO_BACK_ONE_STEP = "z_order_send_to_back_one_step";

	private static final class AdjustedFormElementComparator implements Comparator<AdjustedFormElement>
	{
		public final static AdjustedFormElementComparator INSTANCE = new AdjustedFormElementComparator();

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(AdjustedFormElement o1, AdjustedFormElement o2)
		{
			return o1.formElement.getFormIndex() - o2.formElement.getFormIndex();
		}
	}

	private static final class AdjustedFormElement
	{
		public final IFormElement formElement;
		public boolean needsAdjustement;
		public int layerNumber;

		public AdjustedFormElement(IFormElement formElement, boolean needsAdjustement)
		{
			this.formElement = formElement;
			this.needsAdjustement = needsAdjustement;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((formElement == null) ? 0 : formElement.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			AdjustedFormElement other = (AdjustedFormElement)obj;
			if (formElement == null)
			{
				if (other.formElement != null) return false;
			}
			else if (!formElement.equals(other.formElement)) return false;
			return true;
		}
	}

	public ZOrderAction(IWorkbenchPart part, String zOrderId)
	{
		super(part, VisualFormEditor.REQ_SET_PROPERTY);
		setId(zOrderId);
		initAction();
	}

	public void initAction()
	{
		if (ID_Z_ORDER_BRING_TO_FRONT.equals(getId()))
		{
			setText(DesignerActionFactory.BRING_TO_FRONT_TEXT);
			setToolTipText(DesignerActionFactory.BRING_TO_FRONT_TOOLTIP);
			setImageDescriptor(DesignerActionFactory.BRING_TO_FRONT_IMAGE);
		}
		else if (ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP.equals(getId()))
		{
			setText(DesignerActionFactory.BRING_TO_FRONT_ONE_STEP_TEXT);
			setToolTipText(DesignerActionFactory.BRING_TO_FRONT_ONE_STEP_TOOLTIP);
			setImageDescriptor(DesignerActionFactory.BRING_TO_FRONT_ONE_STEP_IMAGE);
		}
		else if (ID_Z_ORDER_SEND_TO_BACK.equals(getId()))
		{
			setText(DesignerActionFactory.SEND_TO_BACK_TEXT);
			setToolTipText(DesignerActionFactory.SEND_TO_BACK_TOOLTIP);
			setImageDescriptor(DesignerActionFactory.SEND_TO_BACK_IMAGE);
		}
		else if (ID_Z_ORDER_SEND_TO_BACK_ONE_STEP.equals(getId()))
		{
			setText(DesignerActionFactory.SEND_TO_BACK_ONE_STEP_TEXT);
			setToolTipText(DesignerActionFactory.SEND_TO_BACK_ONE_STEP_TOOLTIP);
			setImageDescriptor(DesignerActionFactory.SEND_TO_BACK_ONE_STEP_IMAGE);
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

	protected static List<AdjustedFormElement> calculateNewZOrder(Form form, List<Object> models, String zOrderId)
	{
		Form flattenedForm = ModelUtils.getEditingFlattenedSolution(form).getFlattenedForm(form);
		/*
		 * This is useful when multiple elements are selected. When one object from models is adjusted others from that array may be modified, and this map is
		 * used to avoid modifying it several times
		 */
		Map<UUID, AdjustedFormElement> uuidToFormElementMap = new HashMap<UUID, AdjustedFormElement>();

		List<IFormElement> newModelsList = new ArrayList<IFormElement>();
		for (Object model : models)
		{
			if (model instanceof IFormElement)
			{
				uuidToFormElementMap.put(((IFormElement)model).getUUID(), new AdjustedFormElement((IFormElement)model, true));
				newModelsList.add((IFormElement)model);
			}
			else if (model instanceof FormElementGroup)
			{
				Iterator<IFormElement> formGroupElements = ((FormElementGroup)model).getElements();
				while (formGroupElements.hasNext())
				{
					IFormElement fe = formGroupElements.next();
					if (!uuidToFormElementMap.containsKey(fe.getUUID()))
					{
						newModelsList.add(fe);
						uuidToFormElementMap.put(fe.getUUID(), new AdjustedFormElement(fe, true));
					}
				}
			}
		}

		List<AdjustedFormElement> returnList = new ArrayList<AdjustedFormElement>();

		for (IFormElement formElement : newModelsList)
		{
			/*
			 * Check to see if the element isn't already adjusted
			 */
			if (!uuidToFormElementMap.get(formElement.getUUID()).needsAdjustement) continue;

			/*
			 * Get the overlapping elements, if no overlapping elements exist then continue with the next model object
			 */
			List<IFormElement> tmpOverlappingElements = ElementUtil.getAllOverlappingFormElements(flattenedForm, formElement);
			if (tmpOverlappingElements == null || tmpOverlappingElements.isEmpty())
			{
				uuidToFormElementMap.get(formElement.getUUID()).needsAdjustement = false;
				continue;
			}

			/*
			 * Create a new list using the list above transforming the elements to AdjustedFormElement
			 */
			List<AdjustedFormElement> overlappingElements = new ArrayList<AdjustedFormElement>();
			for (IFormElement bc : tmpOverlappingElements)
			{
				if (uuidToFormElementMap.get(bc.getUUID()) != null)
				{
					overlappingElements.add(new AdjustedFormElement(bc, uuidToFormElementMap.get(bc.getUUID()).needsAdjustement));
				}
				else
				{
					overlappingElements.add(new AdjustedFormElement(bc, false));
				}
			}

			/*
			 * Add the current formElement to the list
			 */
			overlappingElements.add(new AdjustedFormElement(formElement, true));

			/*
			 * Add the other group members (if any) to the list and their overlapping elements
			 */
			String modelGroupID = formElement.getGroupID();
			if (modelGroupID != null)
			{
				for (IFormElement groupMember : newModelsList)
				{
					if (modelGroupID.equals(groupMember.getGroupID()))
					{
						AdjustedFormElement afe = new AdjustedFormElement(groupMember, true);
						if (!overlappingElements.contains(afe)) overlappingElements.add(afe);
					}
				}
			}

			Collections.sort(overlappingElements, AdjustedFormElementComparator.INSTANCE);
			/*
			 * Order the elements from overlappingElements
			 */
			List<AdjustedFormElement> orderedElements = overlappingElements;

			for (int index = 0; index < orderedElements.size(); index++)
			{
				orderedElements.get(index).layerNumber = index;
			}

			/*
			 * Adjust the form indexes to the new values
			 */
			List<AdjustedFormElement> newOrderedElements = new ArrayList<AdjustedFormElement>();


			if (ID_Z_ORDER_SEND_TO_BACK.equals(zOrderId))
			{
				for (int index = 0; index <= orderedElements.size() - 1; index++)
				{
					AdjustedFormElement current = orderedElements.get(index);
					if (current.needsAdjustement)
					{
						newOrderedElements.add(current);
					}
				}

				for (int index = 0; index <= orderedElements.size() - 1; index++)
				{
					AdjustedFormElement current = orderedElements.get(index);
					if (!current.needsAdjustement)
					{
						newOrderedElements.add(current);
					}
				}
			}
			else if (ID_Z_ORDER_BRING_TO_FRONT.equals(zOrderId))
			{
				for (int index = 0; index <= orderedElements.size() - 1; index++)
				{
					AdjustedFormElement current = orderedElements.get(index);
					if (!current.needsAdjustement)
					{
						newOrderedElements.add(current);
					}
				}

				for (int index = 0; index <= orderedElements.size() - 1; index++)
				{
					AdjustedFormElement current = orderedElements.get(index);
					if (current.needsAdjustement)
					{
						newOrderedElements.add(current);
					}
				}
			}
			else if (ID_Z_ORDER_SEND_TO_BACK_ONE_STEP.equals(zOrderId))
			{
				List<Integer> layerNumberList = new ArrayList<Integer>();

				for (AdjustedFormElement abc : orderedElements)
				{
					if (abc.needsAdjustement)
					{
						abc.layerNumber--;
						layerNumberList.add(new Integer(abc.layerNumber));
					}
				}

				for (int index = 0; index < orderedElements.size(); index++)
				{
					AdjustedFormElement abc = orderedElements.get(index);
					if (!abc.needsAdjustement)
					{
						if (layerNumberList.contains(new Integer(abc.layerNumber)))
						{
							for (int increment = 1;; increment++)
							{
								if (!layerNumberList.contains(new Integer(abc.layerNumber + increment)))
								{
									abc.layerNumber += increment;
									layerNumberList.add(new Integer(abc.layerNumber));
									break;
								}
							}
						}
						else
						{
							layerNumberList.add(new Integer(abc.layerNumber));
						}
					}
				}

				AdjustedFormElement[] tmpNewOrderedElements = new AdjustedFormElement[orderedElements.size() + 1];
				for (AdjustedFormElement abc : orderedElements)
				{
					tmpNewOrderedElements[abc.layerNumber + 1] = abc;
				}

				for (int index = tmpNewOrderedElements.length - 1; index >= 0; index--)
				{
					if (tmpNewOrderedElements[index] != null)
					{
						String currentGroupID = tmpNewOrderedElements[index].formElement.getGroupID();
						newOrderedElements.add(0, tmpNewOrderedElements[index]);
						if (currentGroupID != null)
						{
							for (int backwardIndex = index - 1; backwardIndex >= 0; backwardIndex--)
							{
								if (tmpNewOrderedElements[backwardIndex] != null &&
									currentGroupID.equals(tmpNewOrderedElements[backwardIndex].formElement.getGroupID()))
								{
									newOrderedElements.add(0, tmpNewOrderedElements[backwardIndex]);
									tmpNewOrderedElements[backwardIndex] = null;
								}
							}
						}
					}
				}
			}
			else if (ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP.equals(zOrderId))
			{
				List<Integer> layerNumberList = new ArrayList<Integer>();

				for (AdjustedFormElement abc : orderedElements)
				{
					if (abc.needsAdjustement)
					{
						abc.layerNumber++;
						layerNumberList.add(new Integer(abc.layerNumber));
					}
				}

				for (AdjustedFormElement abc : orderedElements)
				{
					if (!abc.needsAdjustement)
					{
						if (layerNumberList.contains(new Integer(abc.layerNumber)))
						{
							for (int decrement = 1;; decrement++)
							{
								if (!layerNumberList.contains(new Integer(abc.layerNumber - decrement)))
								{
									abc.layerNumber -= decrement;
									layerNumberList.add(new Integer(abc.layerNumber));
									break;
								}
							}
						}
						else
						{
							layerNumberList.add(new Integer(abc.layerNumber));
						}
					}
				}

				AdjustedFormElement[] tmpNewOrderedElements = new AdjustedFormElement[orderedElements.size() + 1];
				for (AdjustedFormElement abc : orderedElements)
				{
					tmpNewOrderedElements[abc.layerNumber] = abc;
				}

				for (int index = 0; index <= tmpNewOrderedElements.length - 1; index++)
				{
					if (tmpNewOrderedElements[index] != null)
					{
						String currentGroupID = tmpNewOrderedElements[index].formElement.getGroupID();
						newOrderedElements.add(tmpNewOrderedElements[index]);
						if (currentGroupID != null)
						{
							for (int forwardIndex = index + 1; forwardIndex <= tmpNewOrderedElements.length - 1; forwardIndex++)
							{
								if (tmpNewOrderedElements[forwardIndex] != null &&
									currentGroupID.equals(tmpNewOrderedElements[forwardIndex].formElement.getGroupID()))
								{
									newOrderedElements.add(tmpNewOrderedElements[forwardIndex]);
									tmpNewOrderedElements[forwardIndex] = null;
								}
							}
						}
					}
				}
			}
			else
			{
				continue;
			}

			for (int index = 0; index < newOrderedElements.size(); index++)
			{
				newOrderedElements.get(index).layerNumber = index;
				newOrderedElements.get(index).needsAdjustement = false;
				uuidToFormElementMap.put(newOrderedElements.get(index).formElement.getUUID(), newOrderedElements.get(index));
				returnList.add(newOrderedElements.get(index));
			}
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

		List<AdjustedFormElement> orderedList = calculateNewZOrder((Form)formEditPart.getModel(), models, zOrderId);
		if (orderedList != null)
		{
			for (AdjustedFormElement abc : orderedList)
			{
				if (requests == null)
				{
					requests = new HashMap<EditPart, Request>();
				}
				requests.put(editPartMap.get(abc.formElement), new SetPropertyRequest(VisualFormEditor.REQ_SET_PROPERTY,
					StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName(), Integer.valueOf(abc.layerNumber), "")); //$NON-NLS-1$
			}
		}

		return requests;
	}
}
