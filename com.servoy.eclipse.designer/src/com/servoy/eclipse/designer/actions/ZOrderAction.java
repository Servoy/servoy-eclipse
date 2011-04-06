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
import java.util.LinkedList;
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
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.UUID;

/**
 * Action responsible for handling the layering adjustements:
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
	public enum ZOrderType
	{
		Z_ORDER_BRING_TO_FRONT, Z_ORDER_SEND_TO_BACK, Z_ORDER_BRING_TO_FRONT_ONE_STEP, Z_ORDER_SEND_TO_BACK_ONE_STEP
	}

	private final ZOrderType zOrderType;

	public static final String ID_Z_ORDER_BRING_TO_FRONT = "z_order_bring_to_front";
	public static final String ID_Z_ORDER_SEND_TO_BACK = "z_order_send_to_back";
	public static final String ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP = "z_order_bring_to_front_one_step";
	public static final String ID_Z_ORDER_SEND_TO_BACK_ONE_STEP = "z_order_send_to_back_one_step";

	private static final class FormElementComparator implements Comparator<IFormElement>
	{
		public final static FormElementComparator INSTANCE = new FormElementComparator();

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(IFormElement o1, IFormElement o2)
		{
			return o1.getFormIndex() - o2.getFormIndex();
		}
	}

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
		public boolean equals(Object afe)
		{
			if (afe instanceof AdjustedFormElement) return (this.formElement.getUUID()).equals(((AdjustedFormElement)afe).formElement.getUUID());
			else return false;
		}
	}


	public ZOrderAction(IWorkbenchPart part, Object requestType)
	{
		super(part, requestType);
		initAction((ZOrderType)requestType);
		zOrderType = (ZOrderType)requestType;
	}

	public void initAction(ZOrderType requestType)
	{
		switch (requestType)
		{
			case Z_ORDER_BRING_TO_FRONT :
				setText(DesignerActionFactory.BRING_TO_FRONT_TEXT);
				setToolTipText(DesignerActionFactory.BRING_TO_FRONT_TOOLTIP);
				setImageDescriptor(DesignerActionFactory.BRING_TO_FRONT_IMAGE);
				setId(ID_Z_ORDER_BRING_TO_FRONT);
				break;
			case Z_ORDER_BRING_TO_FRONT_ONE_STEP :
				setText(DesignerActionFactory.BRING_TO_FRONT_ONE_STEP_TEXT);
				setToolTipText(DesignerActionFactory.BRING_TO_FRONT_ONE_STEP_TOOLTIP);
				setImageDescriptor(DesignerActionFactory.BRING_TO_FRONT_ONE_STEP_IMAGE);
				setId(ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP);
				break;
			case Z_ORDER_SEND_TO_BACK :
				setText(DesignerActionFactory.SEND_TO_BACK_TEXT);
				setToolTipText(DesignerActionFactory.SEND_TO_BACK_TOOLTIP);
				setImageDescriptor(DesignerActionFactory.SEND_TO_BACK_IMAGE);
				setId(ID_Z_ORDER_SEND_TO_BACK);
				break;
			case Z_ORDER_SEND_TO_BACK_ONE_STEP :
				setText(DesignerActionFactory.SEND_TO_BACK_ONE_STEP_TEXT);
				setToolTipText(DesignerActionFactory.SEND_TO_BACK_ONE_STEP_TOOLTIP);
				setImageDescriptor(DesignerActionFactory.SEND_TO_BACK_ONE_STEP_IMAGE);
				setId(ID_Z_ORDER_SEND_TO_BACK_ONE_STEP);
				break;
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
		return createZOrderRequests(zOrderType, selected);
	}

	protected static LinkedList<AdjustedFormElement> calculateNewZOrder(Form form, Object[] models, ZOrderType requestType)
	{
		Form flattenedForm = ModelUtils.getEditingFlattenedSolution(form).getFlattenedForm(form);
		/*
		 * This is useful when multiple elements are selected. When one object from models is adjusted others from that array may be modified, and this map is
		 * used to avoid modifying it several times
		 */
		HashMap<UUID, AdjustedFormElement> uuidToFormElementMap = new HashMap<UUID, AdjustedFormElement>();

		List<Object> newModelsList = new LinkedList<Object>();
		for (Object model : models)
		{
			if (model instanceof IFormElement)
			{
				uuidToFormElementMap.put(((IFormElement)model).getUUID(), new AdjustedFormElement((IFormElement)model, true));
				newModelsList.add(model);
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

		Object[] modelsArray = newModelsList.toArray();

		LinkedList<AdjustedFormElement> returnList = new LinkedList<AdjustedFormElement>();

		for (Object model : modelsArray)
		{
			if (model instanceof IFormElement)
			{
				IFormElement formElement = (IFormElement)model;

				/*
				 * Check to see if the element isn't already adjusted
				 */
				if (!uuidToFormElementMap.get(((IFormElement)model).getUUID()).needsAdjustement) continue;

				/*
				 * Get the overlapping elements, if no overlapping elements exist then continue with the next model object
				 */
				List<IFormElement> tmpOverlappingElements = ElementUtil.getAllOverlappingFormElements(flattenedForm, formElement);
				if (tmpOverlappingElements == null || tmpOverlappingElements.isEmpty())
				{
					uuidToFormElementMap.get(((IFormElement)model).getUUID()).needsAdjustement = false;
					continue;
				}

				/*
				 * Create a new list using the list above transforming the elements to AdjustedFormElement
				 */
				LinkedList<AdjustedFormElement> overlappingElements = new LinkedList<AdjustedFormElement>();
				for (IFormElement bc : tmpOverlappingElements)
				{
					if (uuidToFormElementMap.get(bc.getUUID()) != null) overlappingElements.add(new AdjustedFormElement(bc,
						uuidToFormElementMap.get(bc.getUUID()).needsAdjustement));
					else overlappingElements.add(new AdjustedFormElement(bc, false));
				}

				/*
				 * Add the current formElement to the list
				 */
				overlappingElements.add(new AdjustedFormElement(formElement, true));

				/*
				 * Add the other group members (if any) to the list and their overlapping elements
				 */
				String modelGroupID = (String)((BaseComponent)formElement).getProperty(StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName());
				if (modelGroupID != null)
				{
					for (Object groupMember : modelsArray)
					{
						if (groupMember instanceof BaseComponent)
						{
							String groupMemberGroupID = (String)((BaseComponent)groupMember).getProperty(StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName());
							if (groupMemberGroupID != null && groupMemberGroupID.equals(modelGroupID))
							{
								AdjustedFormElement afe = new AdjustedFormElement((BaseComponent)groupMember, true);
								if (!overlappingElements.contains(afe)) overlappingElements.add(afe);
							}
						}
					}
				}

				Collections.sort(overlappingElements, AdjustedFormElementComparator.INSTANCE);
				/*
				 * Order the elements from overlappingElements
				 */
				LinkedList<AdjustedFormElement> orderedElements = overlappingElements;

				for (int index = 0; index < orderedElements.size(); index++)
				{
					orderedElements.get(index).layerNumber = index;
				}

				/*
				 * Adjust the form indexes to the new values
				 */
				LinkedList<AdjustedFormElement> newOrderedElements = new LinkedList<AdjustedFormElement>();
				if (ZOrderType.Z_ORDER_SEND_TO_BACK.equals(requestType))
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
				else if (ZOrderType.Z_ORDER_BRING_TO_FRONT.equals(requestType))
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
				else if (ZOrderType.Z_ORDER_SEND_TO_BACK_ONE_STEP.equals(requestType))
				{
					ArrayList<Integer> layerNumberList = new ArrayList<Integer>();

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
							else layerNumberList.add(new Integer(abc.layerNumber));
						}
					}

					AdjustedFormElement[] tmpNewOrderedElements = new AdjustedFormElement[orderedElements.size() + 1];
					for (int index = 0; index < orderedElements.size(); index++)
					{
						AdjustedFormElement abc = orderedElements.get(index);
						tmpNewOrderedElements[abc.layerNumber + 1] = abc;
					}

					for (int index = tmpNewOrderedElements.length - 1; index >= 0; index--)
					{
						if (tmpNewOrderedElements[index] != null)
						{
							String currentGroupID = (String)((BaseComponent)tmpNewOrderedElements[index].formElement).getProperty(StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName());
							newOrderedElements.add(0, tmpNewOrderedElements[index]);
							if (currentGroupID == null) continue;
							else
							{
								for (int backwardIndex = index - 1; backwardIndex >= 0; backwardIndex--)
								{
									if (tmpNewOrderedElements[backwardIndex] != null)
									{
										String backwardGroupID = (String)((BaseComponent)tmpNewOrderedElements[backwardIndex].formElement).getProperty(StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName());
										if (backwardGroupID != null && backwardGroupID.equals(currentGroupID))
										{
											newOrderedElements.add(0, tmpNewOrderedElements[backwardIndex]);
											tmpNewOrderedElements[backwardIndex] = null;
										}
									}
								}
							}
						}
					}
				}
				else if (ZOrderType.Z_ORDER_BRING_TO_FRONT_ONE_STEP.equals(requestType))
				{
					ArrayList<Integer> layerNumberList = new ArrayList<Integer>();

					for (AdjustedFormElement abc : orderedElements)
					{
						if (abc.needsAdjustement)
						{
							abc.layerNumber++;
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
							else layerNumberList.add(new Integer(abc.layerNumber));
						}
					}

					AdjustedFormElement[] tmpNewOrderedElements = new AdjustedFormElement[orderedElements.size() + 1];
					for (int index = 0; index < orderedElements.size(); index++)
					{
						AdjustedFormElement abc = orderedElements.get(index);
						tmpNewOrderedElements[abc.layerNumber] = abc;
					}

					for (int index = 0; index <= tmpNewOrderedElements.length - 1; index++)
					{
						if (tmpNewOrderedElements[index] != null)
						{
							String currentGroupID = (String)((BaseComponent)tmpNewOrderedElements[index].formElement).getProperty(StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName());
							newOrderedElements.add(tmpNewOrderedElements[index]);
							if (currentGroupID == null) continue;
							else
							{
								for (int forwardIndex = index + 1; forwardIndex <= tmpNewOrderedElements.length - 1; forwardIndex++)
								{
									if (tmpNewOrderedElements[forwardIndex] != null)
									{
										String forwardGroupID = (String)((BaseComponent)tmpNewOrderedElements[forwardIndex].formElement).getProperty(StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName());
										if (forwardGroupID != null && forwardGroupID.equals(currentGroupID))
										{
											newOrderedElements.add(tmpNewOrderedElements[forwardIndex]);
											tmpNewOrderedElements[forwardIndex] = null;
										}
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
		}

		return returnList;
	}

	public static Map<EditPart, Request> createZOrderRequests(ZOrderType requestType, List<EditPart> selected)
	{
		if (selected == null || selected.isEmpty()) return null;


		Map<EditPart, Request> requests = new HashMap<EditPart, Request>();

		List<Object> models = new LinkedList<Object>();

		EditPart firstEditPart = selected.get(0);
		Map editPartMap = firstEditPart.getViewer().getEditPartRegistry();

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

		LinkedList<AdjustedFormElement> orderedList = calculateNewZOrder((Form)formEditPart.getModel(), models.toArray(), requestType);
		if (orderedList != null && !orderedList.isEmpty())
		{
			for (AdjustedFormElement abc : orderedList)
			{
				requests.put((EditPart)editPartMap.get(abc.formElement), new SetPropertyRequest(VisualFormEditor.REQ_SET_PROPERTY,
					StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName(), abc.layerNumber, "")); //$NON-NLS-1$
			}
		}

		if (requests.isEmpty()) return null;
		else return requests;
	}
}
