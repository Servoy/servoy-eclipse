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
import com.servoy.eclipse.designer.editor.commands.MultipleSelectionAction;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.UUID;

/**
 * @author lorand
 *
 */
public class ZOrderAction extends MultipleSelectionAction
{

	private static final class BaseComponentComparator implements Comparator<BaseComponent>
	{
		public final static BaseComponentComparator INSTANCE = new BaseComponentComparator();

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(BaseComponent o1, BaseComponent o2)
		{
			// TODO Auto-generated method stub
			return o1.getFormIndex() - o2.getFormIndex();
		}

	}

	private static final class AdjustedBaseComponent
	{
		public final BaseComponent baseComponent;
		public boolean needsAdjustement;
		public int layerNumber;

		public AdjustedBaseComponent(BaseComponent baseComponent, boolean needsAdjustement)
		{
			this.baseComponent = baseComponent;
			this.needsAdjustement = needsAdjustement;
		}
	}

	public ZOrderAction(IWorkbenchPart part, Object requestType)
	{
		super(part, requestType);
	}

	public static LinkedList<AdjustedBaseComponent> execute(Form form, Object[] models, String requestType)
	{
		Form flattenedForm = ModelUtils.getEditingFlattenedSolution(form).getFlattenedForm(form);

		/*
		 * This is useful when multiple elements are selected. When one object from models is adjusted others from that array may be modified, and this map is
		 * used to avoid modifying it more times
		 */
		HashMap<UUID, AdjustedBaseComponent> uuidToFormElementMap = new HashMap<UUID, AdjustedBaseComponent>();

		for (Object model : models)
		{
			if (model instanceof BaseComponent)
			{
				uuidToFormElementMap.put(((BaseComponent)model).getUUID(), new AdjustedBaseComponent((BaseComponent)model, true));
			}
		}

		LinkedList<AdjustedBaseComponent> returnList = new LinkedList<AdjustedBaseComponent>();

		for (Object model : models)
		{
			if (model instanceof BaseComponent)
			{
				BaseComponent formElement = (BaseComponent)model;

				/*
				 * Check to see if the element isn't already adjusted
				 */
				if (!uuidToFormElementMap.get(((BaseComponent)model).getUUID()).needsAdjustement) continue;

				/*
				 * Get the overlapping elements
				 */
				List<BaseComponent> tmpOverlappingElements = flattenedForm.getOverlapingFormElements(formElement);
				if (tmpOverlappingElements == null || tmpOverlappingElements.isEmpty())
				{
					uuidToFormElementMap.get(((BaseComponent)model).getUUID()).needsAdjustement = false;
					continue;
				}
				/*
				 * Create a new list using the list above transforming the elements to AdjustedBaseComponent
				 */
				List<AdjustedBaseComponent> overlappingElements = new LinkedList<AdjustedBaseComponent>();
				for (BaseComponent bc : tmpOverlappingElements)
				{
					if (uuidToFormElementMap.get(bc.getUUID()) != null) overlappingElements.add(new AdjustedBaseComponent(bc,
						uuidToFormElementMap.get(bc.getUUID()).needsAdjustement));
					else overlappingElements.add(new AdjustedBaseComponent(bc, false));
				}
				/*
				 * Order the elements from overlappingElements
				 */
				LinkedList<AdjustedBaseComponent> orderedElements = new LinkedList<AdjustedBaseComponent>();
				/*
				 * Add the current formElement to the list
				 */
				orderedElements.add(new AdjustedBaseComponent(formElement, true));
				for (AdjustedBaseComponent abc : overlappingElements)
				{
					Iterator<AdjustedBaseComponent> it = orderedElements.iterator();

					AdjustedBaseComponent previous = orderedElements.getFirst();
					if (BaseComponentComparator.INSTANCE.compare(abc.baseComponent, previous.baseComponent) <= 0) orderedElements.add(0, abc);
					else
					{
						boolean itemAdded = false;
						it.next();
						for (int index = 1; it.hasNext(); index++)
						{
							AdjustedBaseComponent current = it.next();
							if (BaseComponentComparator.INSTANCE.compare(abc.baseComponent, previous.baseComponent) > 0 &&
								BaseComponentComparator.INSTANCE.compare(abc.baseComponent, current.baseComponent) <= 0)
							{
								orderedElements.add(index, abc);
								itemAdded = true;
								break;
							}
							previous = current;
						}
						if (!itemAdded) orderedElements.add(abc);
					}
				}

				for (int index = 0; index < orderedElements.size(); index++)
				{
					orderedElements.get(index).layerNumber = index;
				}

				/*
				 * Adjust the form indexes to the new values
				 */
				LinkedList<AdjustedBaseComponent> newOrderedElements = new LinkedList<AdjustedBaseComponent>();
				if (VisualFormEditor.REQ_SEND_TO_BACK.equals(requestType))
				{
					for (int index = 0; index <= orderedElements.size() - 1; index++)
					{
						AdjustedBaseComponent current = orderedElements.get(index);
						if (current.needsAdjustement)
						{
							newOrderedElements.add(current);
						}
					}

					for (int index = 0; index <= orderedElements.size() - 1; index++)
					{
						AdjustedBaseComponent current = orderedElements.get(index);
						if (!current.needsAdjustement)
						{
							newOrderedElements.add(current);
						}
					}
				}
				else if (VisualFormEditor.REQ_BRING_TO_FRONT.equals(requestType))
				{
					for (int index = 0; index <= orderedElements.size() - 1; index++)
					{
						AdjustedBaseComponent current = orderedElements.get(index);
						if (!current.needsAdjustement)
						{
							newOrderedElements.add(current);
						}
					}

					for (int index = 0; index <= orderedElements.size() - 1; index++)
					{
						AdjustedBaseComponent current = orderedElements.get(index);
						if (current.needsAdjustement)
						{
							newOrderedElements.add(current);
						}
					}
				}
				else if (VisualFormEditor.REQ_SEND_TO_BACK_ONE_STEP.equals(requestType))
				{
					ArrayList<Integer> layerNumberList = new ArrayList<Integer>();

					for (AdjustedBaseComponent abc : orderedElements)
					{
						if (abc.needsAdjustement)
						{
							abc.layerNumber--;
							layerNumberList.add(new Integer(abc.layerNumber));
						}
					}

					for (int index = 0; index < orderedElements.size(); index++)
					{
						AdjustedBaseComponent abc = orderedElements.get(index);
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

					AdjustedBaseComponent[] tmpNewOrderedElements = new AdjustedBaseComponent[orderedElements.size() + 1];
					for (int index = 0; index < orderedElements.size(); index++)
					{
						AdjustedBaseComponent abc = orderedElements.get(index);
						tmpNewOrderedElements[abc.layerNumber + 1] = abc;
					}

					for (AdjustedBaseComponent tmpNewOrderedElement : tmpNewOrderedElements)
					{
						if (tmpNewOrderedElement != null) newOrderedElements.add(tmpNewOrderedElement);
					}
				}
				else if (VisualFormEditor.REQ_BRING_TO_FRONT_ONE_STEP.equals(requestType))
				{
					ArrayList<Integer> layerNumberList = new ArrayList<Integer>();

					for (AdjustedBaseComponent abc : orderedElements)
					{
						if (abc.needsAdjustement)
						{
							abc.layerNumber++;
							layerNumberList.add(new Integer(abc.layerNumber));
						}
					}

					for (int index = 0; index < orderedElements.size(); index++)
					{
						AdjustedBaseComponent abc = orderedElements.get(index);
						if (!abc.needsAdjustement)
						{
							if (layerNumberList.contains(new Integer(abc.layerNumber)))
							{
								for (int increment = 1;; increment++)
								{
									if (!layerNumberList.contains(new Integer(abc.layerNumber - increment)))
									{
										abc.layerNumber -= increment;
										layerNumberList.add(new Integer(abc.layerNumber));
										break;
									}
								}
							}
							else layerNumberList.add(new Integer(abc.layerNumber));
						}
					}

					AdjustedBaseComponent[] tmpNewOrderedElements = new AdjustedBaseComponent[orderedElements.size() + 1];
					for (int index = 0; index < orderedElements.size(); index++)
					{
						AdjustedBaseComponent abc = orderedElements.get(index);
						tmpNewOrderedElements[abc.layerNumber] = abc;
					}

					for (AdjustedBaseComponent tmpNewOrderedElement : tmpNewOrderedElements)
					{
						if (tmpNewOrderedElement != null) newOrderedElements.add(tmpNewOrderedElement);
					}
				}
				else
				{
					continue;
				}

				for (int index = 0; index < newOrderedElements.size(); index++)
				{
					newOrderedElements.get(index).layerNumber = index;
					AdjustedBaseComponent abc = uuidToFormElementMap.get(newOrderedElements.get(index));
					if (abc != null) abc.needsAdjustement = false;
					returnList.add(newOrderedElements.get(index));
				}
			}
		}

		return returnList;
	}

	public static Map<EditPart, Request> createZOrderRequests(String requestType, List<EditPart> selected)
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

		LinkedList<AdjustedBaseComponent> orderedList = execute((Form)formEditPart.getModel(), models.toArray(), requestType);
		if (orderedList != null && !orderedList.isEmpty())
		{
			for (AdjustedBaseComponent abc : orderedList)
			{
				requests.put((EditPart)editPartMap.get(abc.baseComponent), new SetPropertyRequest(requestType,
					StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName(), abc.layerNumber, "")); //$NON-NLS-1$
			}
		}

		if (requests.isEmpty()) return null;
		else return requests;
	}
}
