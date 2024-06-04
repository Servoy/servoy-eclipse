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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Assert;
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
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.PersistFinder;
import com.servoy.eclipse.designer.outline.FormOutlineContentProvider;
import com.servoy.eclipse.designer.property.IPersistEditPart;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionLayoutContainer;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ISupportCSSPosition;
import com.servoy.j2db.persistence.ISupportEncapsulation;
import com.servoy.j2db.persistence.ISupportSize;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;
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
				for (Object selectedEditPart : selectedEditParts)
				{
					EditPart object = (EditPart)selectedEditPart;
					EditPart parent = object.getParent();
					if (parent != null && parent.getModel() instanceof IPersist && Utils.isInheritedFormElement(object.getModel(), (IPersist)parent.getModel()))
						return true;
				}
			}
			else
			{
				for (Object selectedEditPart : selectedEditParts)
				{
					Object formElement = null;
					IPersist context = null;
					if (selectedEditPart instanceof PersistContext)
					{
						PersistContext persistContext = (PersistContext)selectedEditPart;
						context = persistContext.getContext();
						formElement = persistContext.getPersist();

					}
					else if (selectedEditPart instanceof FormElementGroup)
					{
						context = ((FormElementGroup)selectedEditPart).getParent();
						formElement = selectedEditPart;
					}
					if (Utils.isInheritedFormElement(formElement, context)) return true;
				}
			}
		}
		return false;
	}

	public static boolean containsFormComponentElement(List selectedEditParts)
	{
		if (selectedEditParts != null && !selectedEditParts.isEmpty())
		{
			for (Object selectedEditPart : selectedEditParts)
			{
				IPersist formElement = null;
				if (selectedEditPart instanceof PersistContext)
				{
					PersistContext persistContext = (PersistContext)selectedEditPart;
					formElement = persistContext.getPersist();

				}
				if (formElement != null && (formElement instanceof WebFormComponentChildType || formElement.getParent() instanceof WebFormComponentChildType))
					return true;
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
				setToJSONArray(obj, key, values);
			}
		});
		setToJSONArray(obj, "topContainer", findTopContainers(false));
		return obj;
	}

	public static void setToJSONArray(final JSONObject obj, String key, Set<String> values)
	{
		if (!values.isEmpty())
		{
			JSONArray ar = new JSONArray();
			for (String child : values)
			{
				ar.put(child);
			}
			obj.put(key, ar);
		}
	}

	// put "component" first if present and sort the others;
	private static Comparator<String> comparator = new Comparator<String>()
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
			else if ("template".equals(n1)) return -1;
			else if ("template".equals(n2)) return 1;
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

	private static void fillAllowedChildrenInternal(AllowChildrenMapFiller mapFiller)
	{
		Collection<PackageSpecification<WebLayoutSpecification>> packs = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().values();
		for (PackageSpecification<WebLayoutSpecification> pack : packs)
		{
			for (WebLayoutSpecification spec : pack.getSpecifications().values())
				mapFiller.add(spec.getPackageName() + "." + spec.getName(), findAllowedChildren(pack, spec, false));
		}
	}

	public static Set<String> findAllowedChildren(String packName, String specName, boolean skipTemplate)
	{
		if (packName != null && specName != null)
		{
			PackageSpecification<WebLayoutSpecification> packageSpecification = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().get(
				packName);
			return findAllowedChildren(packageSpecification, packageSpecification.getSpecification(specName), skipTemplate);
		}
		return findTopContainers(skipTemplate);
	}


	public static Set<String> findTopContainers(boolean skipTemplate)
	{
		Set<String> topContainers = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().values().stream().flatMap(
			pack -> pack.getSpecifications().values().stream().filter(WebLayoutSpecification::isTopContainer)
				.filter(layoutSpec -> !layoutSpec.isDeprecated()).map(layoutSpec -> pack.getPackageName() + "." + layoutSpec.getName()))
			.collect(
				Collectors.toCollection(() -> !skipTemplate ? new TreeSet<>(comparator) : new HashSet<String>()));
		if (!skipTemplate && hasResponsiveLayoutTemplates(null, null))
		{
			topContainers.add("template");
		}
		return topContainers;
	}


	public static Set<String> findAllowedChildren(PackageSpecification<WebLayoutSpecification> pack, WebLayoutSpecification spec, boolean skipTemplate)
	{
		Set<String> allowedChildren = new TreeSet<String>(comparator);
		List<String> excludedChildren = spec.getExcludedChildren();
		List<String> specAllowedChildren = spec.getAllowedChildren();
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
								if (layoutSpec.isDeprecated()) continue;
								try
								{
									String config = (String)layoutSpec.getConfig();
									String layoutName = config == null ? null : new JSONObject(config).optString("layoutName", null);
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
		else if (excludedChildren != null)
		{
			for (WebLayoutSpecification layoutSpec : pack.getSpecifications().values())
			{
				// this can't be a filter. because also row can be a top level container and needs to be able to get into a column.
//						if (layoutSpec.isTopContainer()) continue;
				if (layoutSpec.isDeprecated()) continue;
				try
				{
					String config = (String)layoutSpec.getConfig();
					String layoutName = config == null ? null : new JSONObject(config).optString("layoutName", null);
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
		if (!skipTemplate && hasResponsiveLayoutTemplates(packageName, spec.getName()))
		{
			allowedChildren.add("template");
		}
		return allowedChildren;
	}

	public static String getLayoutContainerAsString(LayoutContainer layout)
	{
		StringBuilder tag = new StringBuilder("<");
		tag.append(layout.getTagType());
		Map<String, String> attributes = layout.getMergedAttributes();
		for (Entry<String, String> entry : attributes.entrySet())
		{
			tag.append(" ");
			tag.append(entry.getKey());
			if (entry.getValue() != null && entry.getValue().length() > 0)
			{
				tag.append("=\"");
				tag.append(entry.getValue());
				tag.append("\"");
			}
		}
		if (layout instanceof CSSPositionLayoutContainer)
		{
			tag.append(" [ResponsiveContainer]");
		}
		tag.append(">");
		if (layout.getName() != null)
		{
			tag.append("[");
			tag.append(layout.getName());
			tag.append("]");
		}
		return tag.toString();
	}

	private interface AllowChildrenMapFiller
	{
		void add(String key, Set<String> values);
	}

	public static TemplateElementHolder[] getResponsiveLayoutTemplates(AbstractContainer container)
	{
		String packName = container instanceof LayoutContainer ? ((LayoutContainer)container).getPackageName() : null;
		String specName = container instanceof LayoutContainer ? ((LayoutContainer)container).getSpecName() : null;
		Set<String> allowedChildren = findAllowedChildren(packName, specName, true);
		List<IRootObject> templates = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.TEMPLATES);
		List<TemplateElementHolder> elements = new ArrayList<>();
		for (IRootObject template2 : templates)
		{
			Template template = (Template)template2;
			if (addTemplate(allowedChildren, template))
			{
				elements.add(new TemplateElementHolder(template));
			}
		}
		TemplateElementHolder[] templ = elements.toArray(new TemplateElementHolder[elements.size()]);
		return templ;
	}


	public static boolean hasResponsiveLayoutTemplates(AbstractContainer container)
	{
		return container instanceof LayoutContainer
			? hasResponsiveLayoutTemplates(((LayoutContainer)container).getPackageName(), ((LayoutContainer)container).getSpecName())
			: hasResponsiveLayoutTemplates(null, null);

	}

	private static boolean hasResponsiveLayoutTemplates(String packName, String specName)
	{
		Set<String> allowedChildren = findAllowedChildren(packName, specName, true);
		List<IRootObject> templates = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.TEMPLATES);
		return templates.stream().anyMatch(template -> addTemplate(allowedChildren, (Template)template));
	}


	public static boolean addTemplate(Set<String> allowedChildren, Template template)
	{
		try
		{
			JSONObject templateJSON = new ServoyJSONObject(template.getContent(), false);
			if (templateJSON.optString(Template.PROP_LAYOUT, Template.LAYOUT_TYPE_ABSOLUTE).equals(Template.LAYOUT_TYPE_RESPONSIVE))
			{
				JSONArray elements = (JSONArray)templateJSON.opt(Template.PROP_ELEMENTS);
				if (elements.length() >= 1)
				{
					JSONObject element = elements.getJSONObject(0);
					String spec = element.optString("typeName");
					String packAndSpec = "";
					if (!element.has("typeName"))
					{
						if (element.has("customProperties") && element.get("customProperties") instanceof String)
						{
							String customProperties = element.getString("customProperties");
							element = new ServoyJSONObject(customProperties, true, true, false);
							if (element.has("properties"))
							{
								JSONObject properties = element.getJSONObject("properties");
								spec = properties.optString("specname");
								packAndSpec = properties.optString("packagename") + "." + properties.optString("specname");
							}
						}
					}
					if (spec != null)
					{
						if (allowedChildren.contains(spec) || allowedChildren.contains(packAndSpec) ||
							allowedChildren.contains("component") && WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(spec) != null)
						{
							return true;
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Wrong template, parsing problem: " + template, e);
			return false;
		}
		return false;
	}

	public static boolean isDropAllowed(AbstractContainer parent, IPersist webObj, Form form)
	{
		if (parent instanceof LayoutContainer && !form.isResponsiveLayout()) return true;
		if (parent instanceof LayoutContainer)
		{
			Set<String> allowed = getAllowedChildren().get(
				((LayoutContainer)parent).getPackageName() + "." + ((LayoutContainer)parent).getSpecName());
			if (webObj instanceof LayoutContainer)
				return allowed.contains(((LayoutContainer)webObj).getPackageName() + "." + ((LayoutContainer)webObj).getSpecName());
			if (webObj instanceof WebComponent) return allowed.contains("component");
			return true;
		}
		if (parent instanceof Form)
		{
			Set<String> topContainers = findTopContainers(true);
			if (webObj instanceof LayoutContainer)
				return topContainers.contains(((LayoutContainer)webObj).getPackageName() + "." + ((LayoutContainer)webObj).getSpecName());
		}
		return false;
	}

	public static CSSPosition cssPositionFromJSON(Form form, IPersist persist, JSONObject properties)
	{
		return cssPositionFromJSON(form, persist, properties, false);
	}

	public static CSSPosition cssPositionFromJSON(Form form, IPersist persist, JSONObject properties, boolean isResize)
	{
		CSSPosition newPosition;
		JSONObject obj = properties.getJSONObject("cssPos");
		CSSPosition position = ((ISupportCSSPosition)persist).getCssPosition();
		if (position == null)
		{
			newPosition = new CSSPosition(properties.optString("y", "0"), "-1", "-1", properties.optString("x", "0"),
				properties.optString("w", "0"), properties.optString("h", "0"));
		}
		else
		{
			Point oldLocation = CSSPositionUtils.getLocation((ISupportCSSPosition)persist);
			java.awt.Dimension oldSize = CSSPositionUtils.getSize((ISupportCSSPosition)persist);
			newPosition = CSSPositionUtils.adjustCSSPosition((ISupportCSSPosition)persist,
				properties.optInt("x", oldLocation.x), properties.optInt("y", oldLocation.y),
				properties.optInt("width", oldSize.width),
				properties.optInt("height", oldSize.height), properties.optBoolean("move", false));
		}

		AbstractContainer componentParent = null;
		for (String property : obj.keySet())
		{
			if (componentParent == null)
			{
				componentParent = CSSPositionUtils.getParentContainer((ISupportSize)persist);
			}
			JSONObject jsonObject = obj.getJSONObject(property);
			ISupportCSSPosition target = jsonObject.optString("uuid") != null ? ((ISupportCSSPosition)PersistFinder.INSTANCE.searchForPersist(form,
				jsonObject.optString("uuid"))) : null;
			AbstractContainer targetParent = target != null ? CSSPositionUtils.getParentContainer(target) : null;

			switch (property)
			{
				case "middleH" :
					snapToMiddle(newPosition, position, componentParent.getSize(), target.getCssPosition(), targetParent.getSize(), "left", "right", "width");
					break;

				case "middleV" :
					snapToMiddle(newPosition, position, componentParent.getSize(), target.getCssPosition(), targetParent.getSize(), "top", "bottom", "height");
					break;

				case "endWidth" :
				case "endHeight" :
					break;

				case "distX" :
					snapToDist(jsonObject, newPosition, position, componentParent.getSize(), form, "left", "right", "width");
					break;

				case "distY" :
					snapToDist(jsonObject, newPosition, position, componentParent.getSize(), form, "top", "bottom", "height");
					break;

				case "sameWidth" :
					snapToSameSize(newPosition, componentParent.getSize(), target.getCssPosition(), targetParent.getSize(), "left",
						"right", "width");
					break;

				case "sameHeight" :
					snapToSameSize(newPosition, componentParent.getSize(), target.getCssPosition(), targetParent.getSize(), "top",
						"bottom", "height");
					break;

				default :
					setCssValue(jsonObject, property, newPosition, position, target.getCssPosition(), componentParent.getSize(), targetParent.getSize(),
						isResize);
			}
		}

		if (obj.has("endWidth") || obj.has("endHeight"))
		{
			if (componentParent == null)
			{
				componentParent = CSSPositionUtils.getParentContainer((ISupportSize)persist);
			}

			handleEndSize(newPosition, obj, componentParent, form, "endWidth", "left", "right", "width");
			handleEndSize(newPosition, obj, componentParent, form, "endHeight", "top", "bottom", "height");
		}

		return newPosition;
	}

	public static void snapToSameSize(CSSPosition newPosition, java.awt.Dimension parentSize,
		CSSPosition targetPosition, java.awt.Dimension targetParentSize, String lowerProperty, String higherProperty, String sizeProperty)
	{
		CSSValue sizeValue = getCssValue(targetPosition, sizeProperty, targetParentSize);
		CSSValue lowerPropertyValue = getCssValue(targetPosition, lowerProperty, targetParentSize);
		if (sizeValue.isSet())
		{
			setCssValue(newPosition, sizeProperty, sizeValue);
			if (getCssValue(newPosition, lowerProperty, parentSize).isSet() && getCssValue(newPosition, higherProperty, parentSize).isSet())
			{
				//source component was anchored left-right or top-bottom, need to clear one of the properties
				//and make the anchoring the same as for the target
				setCssValue(newPosition, lowerPropertyValue.isSet() ? higherProperty : lowerProperty, CSSValue.NOT_SET);
			}
		}
		else
		{
			//target is anchored left-right or top-bottom
			CSSValue sourceLowerPropertyValue = getOrComputeValue(newPosition, lowerProperty, parentSize);
			sizeValue = computeDimension(lowerProperty, lowerPropertyValue, getCssValue(targetPosition, higherProperty, targetParentSize));
			CSSValue sourceHigherPropertyValue = sourceLowerPropertyValue.plus(sizeValue).toHigherProperty();
			setCssValue(newPosition, lowerProperty, sourceLowerPropertyValue);
			setCssValue(newPosition, higherProperty, sourceHigherPropertyValue);
			setCssValue(newPosition, sizeProperty, CSSValue.NOT_SET);
		}
	}


	private static void snapToDist(JSONObject obj, CSSPosition newPosition, CSSPosition oldPosition, java.awt.Dimension parentSize, Form form,
		String lowerProperty, String higherProperty, String sizeProperty)
	{
		JSONArray uuids = obj.getJSONArray("targets");
		ISupportCSSPosition target1 = ((ISupportCSSPosition)PersistFinder.INSTANCE.searchForPersist(form, uuids.getString(0)));
		AbstractContainer parent1 = target1 != null ? CSSPositionUtils.getParentContainer(target1) : null;
		ISupportCSSPosition target2 = ((ISupportCSSPosition)PersistFinder.INSTANCE.searchForPersist(form, uuids.getString(1)));
		AbstractContainer parent2 = target1 != null ? CSSPositionUtils.getParentContainer(target2) : null;
		CSSPosition pos1 = target1.getCssPosition();
		CSSPosition pos2 = target2.getCssPosition();
		java.awt.Dimension parent1Size = parent1.getSize();
		java.awt.Dimension parent2Size = parent2.getSize();

		int pos = obj.optInt("pos", -1);
		snapToDist(newPosition, oldPosition, parentSize, lowerProperty, higherProperty, sizeProperty, pos1, pos2, parent1Size, parent2Size, pos);

	}

	public static void snapToDist(CSSPosition newPosition, CSSPosition oldPosition, java.awt.Dimension parentSize, String lowerProperty, String higherProperty,
		String sizeProperty, CSSPosition pos1, CSSPosition pos2, java.awt.Dimension parent1Size, java.awt.Dimension parent2Size,
		int pos)
	{
		switch (pos)
		{
			case -1 :
			{
				//above the targets
				CSSValue dist = computeDist(lowerProperty, higherProperty, pos1, pos2, parent1Size, parent2Size);
				CSSValue l1 = getOrComputeValue(pos1, lowerProperty, parent1Size);
				CSSValue higherPropertyValue = l1.minus(dist).toHigherProperty();

				//set the same anchoring as the closest target (first)
				if (getCssValue(pos1, lowerProperty, parent1Size).isSet())
				{
					CSSValue lowerPropertyValue = higherPropertyValue.minus(getCssValue(newPosition, sizeProperty, parentSize)); //TODO check if the new size set?
					setCssValue(newPosition, lowerProperty, lowerPropertyValue);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(oldPosition, sizeProperty, parentSize).isSet())
					{
						setCssValue(newPosition, higherProperty, CSSValue.NOT_SET);
					}
				}
				else
				{
					setCssValue(newPosition, higherProperty, higherPropertyValue);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(oldPosition, sizeProperty, parentSize).isSet())
					{
						setCssValue(newPosition, lowerProperty, CSSValue.NOT_SET);
					}
				}
				break;
			}
			case 1 :
			{
				//below the targets
				CSSValue dist = computeDist(lowerProperty, higherProperty, pos1, pos2, parent1Size, parent2Size);
				CSSValue h2 = getOrComputeValue(pos2, higherProperty, parent2Size);

				//set the same anchoring as the closest target (second)
				if (getCssValue(pos2, lowerProperty, parent2Size).isSet())
				{
					CSSValue lowerPropertyValue = h2.plus(dist);
					setCssValue(newPosition, lowerProperty, lowerPropertyValue);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(oldPosition, sizeProperty, parentSize).isSet())
					{
						setCssValue(newPosition, higherProperty, CSSValue.NOT_SET);
					}
				}
				else
				{
					CSSValue higherPropertyValue = h2.plus(dist).plus(getCssValue(newPosition, sizeProperty, parentSize)).toHigherProperty();//TODO check if the new size set?
					setCssValue(newPosition, higherProperty, higherPropertyValue);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(oldPosition, sizeProperty, parentSize).isSet())
					{
						setCssValue(newPosition, lowerProperty, CSSValue.NOT_SET);
					}
				}
				break;
			}
			case 0 :
			{
				//between the targets
				CSSValue h1 = getOrComputeValue(pos1, higherProperty, parent1Size);
				CSSValue l2 = getOrComputeValue(pos2, lowerProperty, parent2Size);
				CSSValue size = getCssValue(newPosition, sizeProperty, parentSize);
				if (!size.isSet())
				{
					size = computeDimension(sizeProperty, getCssValue(oldPosition, lowerProperty, parentSize),
						getCssValue(oldPosition, higherProperty, parentSize));
				}
				CSSValue dist = l2.minus(h1).div(2).minus(size.div(2));

				if (getCssValue(pos1, lowerProperty, parent1Size).isSet() && getCssValue(pos2, lowerProperty, parent2Size).isSet())
				{
					CSSValue lowerPropertyValue = l2.minus(dist).minus(getCssValue(newPosition, sizeProperty, parentSize)); //TODO check if the new size set?
					setCssValue(newPosition, lowerProperty, lowerPropertyValue);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(oldPosition, sizeProperty, parentSize).isSet())
					{
						setCssValue(newPosition, higherProperty, CSSValue.NOT_SET);
					}
				}
				else if (getCssValue(pos1, higherProperty, parent1Size).isSet() && getCssValue(pos2, higherProperty, parent2Size).isSet())
				{
					CSSValue higherPropertyValue = h1.plus(dist).minus(getCssValue(newPosition, sizeProperty, parentSize)); //TODO check if the new size set?
					setCssValue(newPosition, higherProperty, higherPropertyValue);
					//clear the opposite property value if the size property is set
					if (oldPosition == null || getCssValue(oldPosition, sizeProperty, parentSize).isSet())
					{
						setCssValue(newPosition, lowerProperty, CSSValue.NOT_SET);
					}
				}
				else
				{
					//what should happen if the targets don't have the same anchoring?
					setCssValue(newPosition, lowerProperty, h1.plus(dist));
					setCssValue(newPosition, higherProperty, l2.minus(dist));
					setCssValue(newPosition, sizeProperty, CSSValue.NOT_SET);
				}
			}
		}
	}


	private static CSSValue computeDist(String lowerProperty, String higherProperty, CSSPosition pos1, CSSPosition pos2, java.awt.Dimension parent1Size,
		java.awt.Dimension parent2Size)
	{
		CSSValue h1 = getOrComputeValue(pos1, higherProperty, parent1Size);
		CSSValue l2 = getOrComputeValue(pos2, lowerProperty, parent2Size);
		CSSValue dist = l2.minus(h1);
		return dist;
	}


	private static void handleEndSize(CSSPosition newPosition, JSONObject obj, AbstractContainer componentParent, Form form, String key, String start,
		String end, String dimension)
	{
		if (obj.has(key))
		{
			JSONObject jsonObject = obj.getJSONObject(key);
			ISupportCSSPosition target = (ISupportCSSPosition)PersistFinder.INSTANCE.searchForPersist(form, jsonObject.optString("uuid"));
			AbstractContainer parent = CSSPositionUtils.getParentContainer(target);
			snapToEndSize(newPosition, componentParent.getSize(), target.getCssPosition(), parent.getSize(), start, end, dimension);
		}
	}

	public static void snapToEndSize(CSSPosition newPosition, java.awt.Dimension parentSize, CSSPosition targetCssPosition,
		java.awt.Dimension targetParentSize, String lowerProperty, String higherProperty, String sizeProperty)
	{
		CSSValue higherPropertyValue = getCssValue(targetCssPosition, higherProperty, targetParentSize);
		CSSValue lowerPropertyValue = getCssValue(targetCssPosition, lowerProperty, targetParentSize);
		CSSValue sourceLowerPropertyValue = getCssValue(newPosition, lowerProperty, parentSize);
		CSSValue sourceSizePropertyValue = getCssValue(newPosition, sizeProperty, parentSize);
		if (higherPropertyValue.isSet())
		{
			setCssValue(newPosition, higherProperty, higherPropertyValue);
			if (sourceLowerPropertyValue.isSet())
			{
				CSSValue size = higherPropertyValue.minus(sourceLowerPropertyValue);
				setCssValue(newPosition, sizeProperty, size);
			}
			else
			{
				CSSValue size = getCssValue(targetCssPosition, sizeProperty, targetParentSize);
				if (size.isSet())
				{
					setCssValue(newPosition, sizeProperty, size);
				}
			}
			setCssValue(newPosition, lowerProperty, CSSValue.NOT_SET);
		}
		else
		{
			CSSValue computedHigherPropertyValue = lowerPropertyValue.plus(getCssValue(targetCssPosition, sizeProperty, targetParentSize)).toHigherProperty();
			if (!sourceSizePropertyValue.isSet())
			{
				setCssValue(newPosition, higherProperty, higherPropertyValue);
			}
			else
			{
				CSSValue size = computedHigherPropertyValue.minus(sourceLowerPropertyValue);
				setCssValue(newPosition, sizeProperty, size);
				setCssValue(newPosition, higherProperty, CSSValue.NOT_SET);
			}
		}
	}


	public static void snapToMiddle(CSSPosition newPosition, CSSPosition oldPosition, java.awt.Dimension parentSize, CSSPosition midCssPosition,
		java.awt.Dimension targetParentSize, String lowerProperty, String higherProperty, String sizeProperty)
	{
		CSSValue mid = CSSValue.NOT_SET;
		CSSValue lowerPropertyValue = getCssValue(midCssPosition, lowerProperty, targetParentSize);
		CSSValue higherPropertyValue = getCssValue(midCssPosition, higherProperty, targetParentSize);
		CSSValue targetSizeValue = getCssValue(midCssPosition, sizeProperty, targetParentSize);
		CSSValue sizeValue = getCssValue(oldPosition, sizeProperty, parentSize);
		if (sizeValue.isSet())
		{
			//maintain the size of the old css position
			setCssValue(newPosition, sizeProperty, sizeValue);
		}
		else if (oldPosition != null)
		{
			//the size is not set (component anchored left-right or top-bottom),
			//need to compute it based on the opposing properties and the parent container size
			sizeValue = getCssValue(oldPosition, higherProperty, parentSize).minus(getCssValue(oldPosition, lowerProperty, parentSize));
			setCssValue(newPosition, sizeProperty, sizeValue);
		}
		else
		{
			sizeValue = getCssValue(newPosition, sizeProperty, parentSize);
		}
		Assert.isTrue(sizeValue.isSet());

		if (targetSizeValue.isSet())
		{
			if (lowerPropertyValue.isSet())
			{
				mid = lowerPropertyValue.plus(targetSizeValue.div(2));
				setCssValue(newPosition, lowerProperty, mid.minus(sizeValue.div(2)));
				setCssValue(newPosition, higherProperty, CSSValue.NOT_SET);
			}
			else if (higherPropertyValue.isSet())
			{
				mid = higherPropertyValue.minus(targetSizeValue.div(2));
				setCssValue(newPosition, higherProperty, mid.plus(sizeValue.div(2)).toHigherProperty());
				setCssValue(newPosition, lowerProperty, CSSValue.NOT_SET);
			}
		}
		else
		{
			mid = lowerPropertyValue.plus(higherPropertyValue).div(2);
			setCssValue(newPosition, lowerProperty, mid.minus(sizeValue.div(2)));
			setCssValue(newPosition, higherProperty, mid.plus(sizeValue.div(2)).toHigherProperty());
			setCssValue(newPosition, sizeProperty, CSSValue.NOT_SET);
		}
	}

	private static CSSValue getOrComputeValue(CSSPosition pos, String property, java.awt.Dimension parentSize)
	{
		CSSValue value = getCssValue(pos, property, parentSize);
		if (!value.isSet())
		{
			value = computeValueBasedOnOppositeProperty(pos, property, parentSize, getCssValue(pos, getOppositeProperty(property), parentSize));
		}
		return value;
	}

	private static CSSValue getCssValue(CSSPosition position, String property, java.awt.Dimension containerSize)
	{
		if (position == null) return CSSValue.NOT_SET;
		switch (property)
		{
			case "left" :
				return new CSSValue(position.left, containerSize.width, false);
			case "right" :
				return new CSSValue(position.right, containerSize.width, true);
			case "top" :
				return new CSSValue(position.top, containerSize.height, false);
			case "bottom" :
				return new CSSValue(position.bottom, containerSize.height, true);
			case "width" :
				return new CSSValue(position.width, containerSize.width, false);
			case "height" :
				return new CSSValue(position.height, containerSize.height, false);
			default :
				return CSSValue.NOT_SET;
		}
	}

	private static void setCssValue(CSSPosition position, String property, CSSValue val)
	{
		if (position == null) return;
		String value = val.toString();
		switch (property)
		{
			case "left" :
				position.left = value;
				return;
			case "right" :
				position.right = value;
				return;
			case "top" :
				position.top = value;
				return;
			case "bottom" :
				position.bottom = value;
				return;
			case "width" :
				position.width = value;
				return;
			case "height" :
				position.height = value;
				return;
		}
	}

	private static String getOppositeProperty(String property)
	{
		switch (property)
		{
			case "left" :
				return "right";
			case "right" :
				return "left";
			case "top" :
				return "bottom";
			case "bottom" :
				return "top";
			case "width" :
				return "height";
			case "height" :
				return "width";
			default :
				return null;
		}
	}

	private static String getSizeProperty(String property)
	{
		if ("left".equals(property) || "right".equals(property))
		{
			return "width";
		}
		else if ("top".equals(property) || "bottom".equals(property))
		{
			return "height";
		}
		return null;
	}

	/**
	 * Copy the anchoring from the target component.
	 *
	 * @param jsonObject contains the property name to copy from the target; for instance align the right edge with the right of left property of the target
	 * @param property the property to be set (one of top, bottom, left, right)
	 * @param newPosition the css position object on which the values should be set
	 * @param oldPosition the old css position object of the source component; null for new components
	 * @param targetPosition the css position object of the snap target
	 * @param containerSize the size of the source component parent
	 * @param targetContainerSize the size of the target component parent
	 * @param isResize
	 */
	public static void setCssValue(JSONObject jsonObject, String property, CSSPosition newPosition, CSSPosition oldPosition, CSSPosition targetPosition,
		java.awt.Dimension containerSize, java.awt.Dimension targetContainerSize,
		boolean isResize)
	{
		String sizeProperty = getSizeProperty(property);
		String oppositeProperty = getOppositeProperty(property);
		String targetProperty = jsonObject.optString("prop", property);
		String targetOppositeProperty = getOppositeProperty(oppositeProperty);
		CSSValue val = getCssValue(targetPosition, targetProperty, targetContainerSize);
		if (val.isSet())
		{
			//if the property is set on the target, then we copy on the source component and clear the opposite property if the size property is set
			//when moving or resizing a component, if the property is set on the target, then we copy its value on the source component
			if (property.equals(targetProperty))
			{
				setCssValue(newPosition, property, val);
			}
			else
			{
				CSSValue computed = computeValueBasedOnOppositeTargetProperty(property, containerSize, val);
				setCssValue(newPosition, property, computed);
			}
			//clear the opposite property value if the size property is set
			if (oldPosition == null || getCssValue(oldPosition, sizeProperty, containerSize).isSet())
			{
				setCssValue(newPosition, oppositeProperty, CSSValue.NOT_SET);
			}
		}
		else if (getCssValue(targetPosition, oppositeProperty, targetContainerSize).isSet() &&
			getCssValue(targetPosition, sizeProperty, targetContainerSize).isSet())
		{
			//the property is not set on the target, need to compute it using the size and the value of the opposite property
			CSSValue oppositePropertyValue = getCssValue(targetPosition, oppositeProperty, targetContainerSize);
			CSSValue computedPropertyValue = computeValueBasedOnOppositeProperty(targetPosition, property, targetContainerSize, oppositePropertyValue);

			//clear the property because the target component does also not have it and we want the same anchoring
			setCssValue(newPosition, property, CSSValue.NOT_SET);
			CSSValue dimension = getCssValue(oldPosition, sizeProperty, containerSize);
			if (dimension.isSet())
			{
				//maintain the size of the old css position
				setCssValue(newPosition, sizeProperty, dimension);
			}
			else if (oldPosition != null)
			{
				//the size is not set (component anchored left-right or top-bottom),
				//need to compute it based on the opposing properties and the parent container size
				CSSValue computedDimension = computeDimension(property, getCssValue(oldPosition, property, containerSize),
					getCssValue(oldPosition, oppositeProperty, containerSize));
				setCssValue(newPosition, sizeProperty, computedDimension);
			}
			//compute the opposite property value for the source component using the size property, container size and the computed property value of the target
			CSSValue computedOppositePropertySourceComponent = computeValueBasedOnOppositeProperty(newPosition, oppositeProperty, containerSize,
				computedPropertyValue);
			setCssValue(newPosition, oppositeProperty, computedOppositePropertySourceComponent);
		}
		else if (getCssValue(targetPosition, targetOppositeProperty, targetContainerSize).isSet() &&
			getCssValue(targetPosition, sizeProperty, targetContainerSize).isSet())
		{
			CSSValue oppositePropertyValue = getCssValue(targetPosition, targetOppositeProperty, targetContainerSize);
			CSSValue computed = computeValueBasedOnOppositeProperty(targetPosition, targetProperty, targetContainerSize,
				oppositePropertyValue);

			if (property.equals(targetProperty))
			{
				//use the computed property value of the target
				setCssValue(newPosition, property, computed);
			}
			else
			{
				CSSValue computedPropertyValue = computeValueBasedOnOppositeTargetProperty(property, containerSize, computed);
				setCssValue(newPosition, property, computedPropertyValue);
			}
		}
	}

	private static CSSValue computeValueBasedOnOppositeProperty(CSSPosition position, String property, java.awt.Dimension containerSize,
		CSSValue oppositePropertyValue)
	{
		switch (property)
		{
			case "left" :
			{
				CSSValue width = getCssValue(position, "width", containerSize);
				return oppositePropertyValue.minus(width);
			}
			case "right" :
			{
				CSSValue width = getCssValue(position, "width", containerSize);
				return oppositePropertyValue.plus(width).toHigherProperty();
			}
			case "top" :
			{
				CSSValue height = getCssValue(position, "height", containerSize);
				return oppositePropertyValue.minus(height);
			}
			case "bottom" :
			{
				CSSValue height = getCssValue(position, "height", containerSize);
				return oppositePropertyValue.plus(height).toHigherProperty();
			}
		}
		return CSSValue.NOT_SET;
	}

	private static CSSValue computeValueBasedOnOppositeTargetProperty(String property, java.awt.Dimension containerSize,
		CSSValue oppositePropertyValue)
	{
		if (oppositePropertyValue.isPercentage())
		{
			return new CSSValue(100 - oppositePropertyValue.getPercentage(), 0);
		}
		switch (property)
		{
			case "left" :
				return new CSSValue(0, oppositePropertyValue.getAsPixels());
			case "right" :
				return new CSSValue(0, containerSize.width - oppositePropertyValue.getAsPixels());//TODO check

			case "top" :
				return new CSSValue(0, oppositePropertyValue.getAsPixels());
			case "bottom" :
				return new CSSValue(0, containerSize.height - oppositePropertyValue.getAsPixels()); //TODO check
		}
		return CSSValue.NOT_SET;
	}


	private static CSSValue computeDimension(String property, CSSValue value, CSSValue oppositePropertyValue)
	{
		switch (property)
		{
			case "left" :
			case "top" :
				return oppositePropertyValue.minus(value);
			case "right" :
			case "bottom" :
				return value.minus(oppositePropertyValue);
		}
		return CSSValue.NOT_SET;
	}
}