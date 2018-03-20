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
package com.servoy.eclipse.designer.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.EditPart;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.outline.FormOutlineContentProvider;
import com.servoy.eclipse.designer.property.IPersistEditPart;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportEncapsulation;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Utility methods for form designer.
 *
 * @author rgansevles
 */

public class DesignerUtil
{
	private static final String ADD_COMPONENT_SUBMENU_ITEM_TEXT = "component";

	public static List<EditPart> removeChildEditParts(List<EditPart> editParts)
	{
		List<EditPart> newEditParts = new ArrayList<EditPart>(editParts.size());

		for (EditPart editPart : editParts)
		{
			boolean foundParent = false;
			if (editPart instanceof IPersistEditPart)
			{
				IPersistEditPart persistEditpart = (IPersistEditPart)editPart;
				IPersist persist = persistEditpart.getPersist();
				for (EditPart editPart2 : editParts)
				{
					if (editPart2 instanceof IPersistEditPart && ((IPersistEditPart)editPart2).getPersist() == persist.getParent())
					{
						foundParent = true;
						break;
					}
				}
			}
			if (!foundParent) newEditParts.add(editPart);
		}

		return newEditParts;
	}


	/**
	 * @param awtDimension
	 * @return draw2d Dimension
	 */
	public static Dimension convertDimension(java.awt.Dimension awtDimension)
	{
		if (awtDimension == null)
		{
			return null;
		}
		return new Dimension(awtDimension.width, awtDimension.height);
	}

	public static boolean containsInheritedElement(List selectedEditParts)
	{
		if (selectedEditParts != null && !selectedEditParts.isEmpty())
		{
			if (selectedEditParts.get(0) instanceof EditPart)
			{
				for (int i = 0; i < selectedEditParts.size(); i++)
				{
					EditPart object = (EditPart)selectedEditParts.get(i);
					EditPart parent = object.getParent();
					if (parent != null && parent.getModel() instanceof IPersist && Utils.isInheritedFormElement(object.getModel(), (IPersist)parent.getModel()))
						return true;
				}
			}
			else
			{
				for (int i = 0; i < selectedEditParts.size(); i++)
				{
					Object formElement = null;
					IPersist context = null;
					if (selectedEditParts.get(i) instanceof PersistContext)
					{
						PersistContext persistContext = (PersistContext)selectedEditParts.get(i);
						context = persistContext.getContext();
						formElement = persistContext.getPersist();

					}
					else if (selectedEditParts.get(i) instanceof FormElementGroup)
					{
						context = ((FormElementGroup)selectedEditParts.get(i)).getParent();
						formElement = selectedEditParts.get(i);
					}
					if (Utils.isInheritedFormElement(formElement, context)) return true;
				}
			}
		}
		return false;
	}

	public static Part getPreviousPart(Part part)
	{
		Part previousPart = null;
		Form flattenedForm = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(part).getFlattenedForm(part);
		Iterator<Part> parts = flattenedForm.getObjects(IRepository.PARTS);
		while (parts.hasNext())
		{
			Part nextPart = parts.next();
			int nextHeight = nextPart.getHeight();
			if (nextHeight < part.getHeight() && (previousPart == null || nextHeight > previousPart.getHeight()))
			{
				previousPart = nextPart;
			}
		}
		return previousPart;
	}

	public static Set<EditPart> getFormEditparts(Iterable<EditPart> editparts)
	{
		if (editparts == null)
		{
			return null;
		}
		Set<EditPart> parents = new HashSet<EditPart>();

		for (EditPart editPart : editparts)
		{
			for (EditPart parent = editPart; parent != null; parent = parent.getParent())
			{
				if (parent.getModel() instanceof Form)
				{
					parents.add(parent);
					break;
				}
			}
		}

		return parents;
	}

	private static boolean isDropAllowed(IPersist dropTargetPersist, IPersist draggedPersist)
	{
		if (dropTargetPersist.getParent() != null && draggedPersist instanceof ISupportEncapsulation)
		{
			if (PersistEncapsulation.isModuleScope((ISupportEncapsulation)draggedPersist, (Solution)dropTargetPersist.getRootObject()))
			{
				return false;
			}
		}
		return true;
	}

	public static boolean isDropFormAllowed(IPersist dropTargetForm, PersistDragData dragData)
	{
		// cannot drop form onto itself
		if (dropTargetForm.getUUID().equals(dragData.uuid))
		{
			return false;
		}

		// cannot drop a (module) private form on a non-accessible form or inside on of its container elements (tabpanel,tablesspanel,etc)
		FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(dropTargetForm);
		IPersist realPersist = fs.searchPersist(dragData.uuid);
		if (realPersist == null) return false;
		else return DesignerUtil.isDropAllowed(dropTargetForm, realPersist);
	}

	/** Returns the ContentOutline view instance in the active page.
	 * @return Returns the ContentOutline view instance in the active page. Returns null if the view can not be found in the active page.
	 */
	public static ContentOutline getContentOutline()
	{
		IWorkbenchWindow active = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (active == null) return null;
		IWorkbenchPage page = active.getActivePage();
		if (page == null) return null;
		IViewReference[] viewReferences = page.getViewReferences();
		for (IViewReference iViewReference : viewReferences)
		{
			if (iViewReference.getId().equals("org.eclipse.ui.views.ContentOutline"))
			{
				IViewPart view = iViewReference.getView(false);
				if (view != null)
				{
					ContentOutline outline = (ContentOutline)view;
					return outline;
				}
			}
		}
		return null;
	}

	/** Returns the ContentOutline view instance in the active page.
	 * @return Returns the ContentOutline view instance in the active page. Returns null if the view can not be found in the active page.
	 */
	public static BaseVisualFormEditor getActiveEditor()
	{
		IWorkbenchWindow active = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (active == null) return null;
		IWorkbenchPage page = active.getActivePage();
		if (page == null) return null;
		IEditorPart activeEditor = page.getActiveEditor();
		if (activeEditor instanceof BaseVisualFormEditor)
		{
			return (BaseVisualFormEditor)activeEditor;
		}
		return null;
	}


	public static PersistContext getContentOutlineSelection()
	{
		if (getContentOutline() != null)
		{
			Object firstElement = ((IStructuredSelection)getContentOutline().getSelection()).getFirstElement();
			if (firstElement instanceof PersistContext)
			{
				return (PersistContext)firstElement;
			}
			if (firstElement == FormOutlineContentProvider.ELEMENTS)
			{
				IWorkbenchWindow active = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				if (active == null) return null;
				IWorkbenchPage page = active.getActivePage();
				if (page == null) return null;
				IEditorInput editorInput = page.getActiveEditor().getEditorInput();
				if (editorInput instanceof PersistEditorInput)
				{
					PersistEditorInput persistEditorInput = (PersistEditorInput)editorInput;
					ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
						persistEditorInput.getSolutionName());
					UUID uuid = persistEditorInput.getUuid();
					IPersist persist = servoyProject.getEditingPersist(uuid);
					return PersistContext.create(persist);
				}
			}
		}
		return null;
	}


	public static Map<String, Set<String>> getAllowedChildren()
	{
		final Map<String, Set<String>> map = new HashMap<String, Set<String>>();
		fillAllowedChildrenInternal(new AllowChildrenMapFiller()
		{
			@Override
			public void add(String key, Set<String> values)
			{
				map.put(key, values);
			}
		});
		return map;
	}

	public static JSONObject getAllowedChildrenAsJSON()
	{
		final JSONObject obj = new JSONObject();
		fillAllowedChildrenInternal(new AllowChildrenMapFiller()
		{
			@Override
			public void add(String key, Set<String> values)
			{
				JSONArray ar = new JSONArray();
				for (String child : values)
				{
					ar.put(child);
				}
				obj.put(key, ar);
			}
		});
		return obj;
	}

	private static void fillAllowedChildrenInternal(AllowChildrenMapFiller mapFiller)
	{
		// put "component" first if present and sort the others;
		Comparator<String> comparator = new Comparator<String>()
		{
			@Override
			public int compare(String o1, String o2)
			{
				String n1 = getDisplayName(o1);
				String n2 = getDisplayName(o2);
				boolean co1 = ADD_COMPONENT_SUBMENU_ITEM_TEXT.equals(n1);
				boolean co2 = ADD_COMPONENT_SUBMENU_ITEM_TEXT.equals(n2);
				if (co1 && co2) return 0;
				else if (co1) return -1;
				else if (co2) return 1;
				else return n1.compareTo(n2);
			}

			private String getDisplayName(String name)
			{
				if (name.contains("*") || ADD_COMPONENT_SUBMENU_ITEM_TEXT.equals(name)) return ADD_COMPONENT_SUBMENU_ITEM_TEXT;
				if (name.contains("."))
				{
					String[] parts = name.split("\\.");
					PackageSpecification<WebLayoutSpecification> pkg = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().get(parts[0]);
					if (pkg != null && parts.length > 0 && pkg.getSpecification(parts[1]) != null)
					{
						return pkg.getSpecification(parts[1]).getDisplayName();
					}
				}
				return name;
			}
		};
		Collection<PackageSpecification<WebLayoutSpecification>> packs = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().values();
		for (PackageSpecification<WebLayoutSpecification> pack : packs)
		{
			for (WebLayoutSpecification spec : pack.getSpecifications().values())
			{
				List<String> excludedChildren = spec.getExcludedChildren();
				List<String> specAllowedChildren = spec.getAllowedChildren();
				Set<String> allowedChildren = new TreeSet<String>(comparator);
				String packageName = pack.getPackageName();
				if (excludedChildren == null || excludedChildren.isEmpty())
				{
					if (!specAllowedChildren.isEmpty())
					{
						if (specAllowedChildren.contains("*"))//we allow all components and all layouts
						{
							for (WebLayoutSpecification layoutSpec : pack.getSpecifications().values())
							{
								allowedChildren.add(packageName + "." + layoutSpec.getName());
							}
							allowedChildren.add("component");
						}
						else
						{
							//we iterate through all the layouts that we have and check if the layoutName matches the current allowedChildName
							for (String allowedChild : specAllowedChildren)
							{
								if (allowedChild.equalsIgnoreCase("component") || allowedChild.endsWith(".*"))
								{
									//not sure what is the difference between "component" and something which ended in ".*", but in the old code we added "component" to the menu;
									//so will keep it like that to avoid breaking existing packages that we are not aware of and use stuff ending in ".*"
									allowedChildren.add("component");
								}
								else
								{
									for (WebLayoutSpecification layoutSpec : pack.getSpecifications().values())
									{
										try
										{
											String layoutName = new JSONObject((String)layoutSpec.getConfig()).optString("layoutName", null);
											if (layoutName == null)
											{
												layoutName = layoutSpec.getName();
											}
											if (allowedChild.equals(layoutName) || allowedChild.equals(packageName + "." + layoutName))
											{
												allowedChildren.add(packageName + "." + layoutSpec.getName());
											}
										}
										catch (JSONException e)
										{
											Debug.log(e);
										}
									}
								}
							}
						}
					}
				}
				if (excludedChildren != null)
				{
					for (WebLayoutSpecification layoutSpec : pack.getSpecifications().values())
					{
						// this can't be a filter. because also row can be a top level container and needs to be able to get into a column.
//						if (layoutSpec.isTopContainer()) continue;
						try
						{
							String layoutName = new JSONObject((String)layoutSpec.getConfig()).optString("layoutName", null);
							if (layoutName == null)
							{
								layoutName = layoutSpec.getName();
							}
							if (!excludedChildren.contains(layoutName) && !excludedChildren.contains(packageName + "." + layoutName))
							{
								allowedChildren.add(packageName + "." + layoutSpec.getName());
							}
						}
						catch (JSONException e)
						{
							Debug.log(e);
						}
					}
					if (!excludedChildren.contains("component")) allowedChildren.add("component");
				}

				if (allowedChildren.isEmpty() && excludedChildren == null && specAllowedChildren == null)
				{
					//add component if both excluded and allowed children are missing
					allowedChildren.add("component");
				}

				mapFiller.add(spec.getPackageName() + "." + spec.getName(), allowedChildren);
			}
		}
	}

	private interface AllowChildrenMapFiller
	{
		void add(String key, Set<String> values);
	}
}
