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
			if (property.startsWith("middle"))
			{
				snapToMiddle(form, newPosition, obj, property);
			}
			else
			{
				JSONObject jsonObject = obj.getJSONObject(property);
				ISupportCSSPosition target = ((ISupportCSSPosition)PersistFinder.INSTANCE.searchForPersist(form,
					jsonObject.optString("uuid")));
				if (componentParent == null)
				{
					componentParent = CSSPositionUtils.getParentContainer((ISupportSize)persist);
				}
				AbstractContainer parent = CSSPositionUtils.getParentContainer(target);
				java.awt.Dimension containerSize = parent.getSize();
				setCssValue(jsonObject, property, newPosition, position, target.getCssPosition(), componentParent.getSize(), containerSize, isResize);
			}
		}

		return newPosition;
	}


	private static void snapToMiddle(Form form, CSSPosition newPosition, JSONObject obj, String property)
	{
		if ("middleH".equals(property))
		{
			JSONObject jsonObject = obj.getJSONObject("middleH");
			ISupportCSSPosition middle = ((ISupportCSSPosition)PersistFinder.INSTANCE.searchForPersist(form,
				jsonObject.optString("uuid")));
			if (middle != null)
			{
				CSSPosition midCssPosition = middle.getCssPosition();
				int mid = -1;
				if (CSSPositionUtils.isSet(midCssPosition.left) && CSSPositionUtils.isSet(midCssPosition.width))
				{
					mid = CSSPositionUtils.getPixelsValue(midCssPosition.left) + CSSPositionUtils.getPixelsValue(midCssPosition.width) / 2;
				}
				else if (CSSPositionUtils.isSet(midCssPosition.right) && CSSPositionUtils.isSet(midCssPosition.width))
				{
					mid = CSSPositionUtils.getPixelsValue(midCssPosition.right) - CSSPositionUtils.getPixelsValue(midCssPosition.width) / 2;
				}
				else
				{
					mid = (CSSPositionUtils.getPixelsValue(midCssPosition.left) + CSSPositionUtils.getPixelsValue(midCssPosition.right)) / 2;
				}

				if (CSSPositionUtils.isSet(newPosition.width))
				{
					if (CSSPositionUtils.isSet(newPosition.left))
					{
						newPosition.left = (mid - CSSPositionUtils.getPixelsValue(newPosition.width) / 2) + "";
					}
					if (CSSPositionUtils.isSet(newPosition.right))
					{
						newPosition.right = (mid + CSSPositionUtils.getPixelsValue(newPosition.width) / 2) + "";
					}
				}
				else
				{
					newPosition.left = (mid - CSSPositionUtils.getPixelsValue(newPosition.width) / 2) + "";
					newPosition.right = (mid + CSSPositionUtils.getPixelsValue(newPosition.width) / 2) + "";
				}
			}
		}
		else if ("middleV".equals(property))
		{
			JSONObject jsonObject = obj.getJSONObject("middleV");
			ISupportCSSPosition middle = ((ISupportCSSPosition)PersistFinder.INSTANCE.searchForPersist(form,
				jsonObject.optString("uuid")));
			if (middle != null)
			{
				CSSPosition midCssPosition = middle.getCssPosition();
				int mid = -1;
				if (CSSPositionUtils.isSet(midCssPosition.top) && CSSPositionUtils.isSet(midCssPosition.height))
				{
					mid = CSSPositionUtils.getPixelsValue(midCssPosition.top) + CSSPositionUtils.getPixelsValue(midCssPosition.height) / 2;
				}
				else if (CSSPositionUtils.isSet(midCssPosition.bottom) && CSSPositionUtils.isSet(midCssPosition.height))
				{
					mid = CSSPositionUtils.getPixelsValue(midCssPosition.bottom) - CSSPositionUtils.getPixelsValue(midCssPosition.height) / 2;
				}
				else
				{
					mid = (CSSPositionUtils.getPixelsValue(midCssPosition.top) + CSSPositionUtils.getPixelsValue(midCssPosition.height)) / 2;
				}

				if (CSSPositionUtils.isSet(newPosition.height))
				{
					if (CSSPositionUtils.isSet(newPosition.top))
					{
						newPosition.top = (mid - CSSPositionUtils.getPixelsValue(newPosition.height) / 2) + "";
					}
					if (CSSPositionUtils.isSet(newPosition.bottom))
					{
						newPosition.bottom = (mid + CSSPositionUtils.getPixelsValue(newPosition.height) / 2) + "";
					}
				}
				else
				{
					newPosition.top = (mid - CSSPositionUtils.getPixelsValue(newPosition.height) / 2) + "";
					newPosition.bottom = (mid + CSSPositionUtils.getPixelsValue(newPosition.height) / 2) + "";
				}
			}
		}
	}

	private static String getCssValue(CSSPosition position, String property)
	{
		if (position == null) return "-1";
		switch (property)
		{
			case "left" :
				return position.left;
			case "right" :
				return position.right;
			case "top" :
				return position.top;
			case "bottom" :
				return position.bottom;
			case "width" :
				return position.width;
			case "height" :
				return position.height;
			default :
				return "-1";
		}
	}

	private static void setCssValue(CSSPosition position, String property, String value)
	{
		if (position == null) return;
		//TODO make sure the val is int or percentage
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
	 * @param oldPosition the old css position object of the source componen; null for new components
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
		String value = getCssValue(targetPosition, jsonObject.optString("prop", property));
		if (CSSPositionUtils.isSet(value))
		{
			//if the property is set on the target, then we copy on the source component and clear the opposite property if the size property is set
			if (isResize)
			{
				int w = CSSPositionUtils.getPixelsValue(value) - CSSPositionUtils.getPixelsValue(getCssValue(oldPosition, oppositeProperty)); //TODO prop!
				if (w > 0)
				{
					setCssValue(newPosition, sizeProperty, w + "");
				}
			}
			else
			{
				//when moving a component, if the property is set on the target, then we copy its value on the source component
				if (property.equals(jsonObject.optString("prop", property)))
				{
					setCssValue(newPosition, property, value);
				}
				else
				{
					int computed = computeValueBasedOnOppositeTargetProperty(property, containerSize, value);
					setCssValue(newPosition, property, computed + "");
				}
				//clear the opposite property value if the size property is set
				if (oldPosition == null || CSSPositionUtils.isSet(getCssValue(oldPosition, sizeProperty)))
				{
					setCssValue(newPosition, oppositeProperty, "-1");
				}
			}
		}
		else if (CSSPositionUtils.isSet(getCssValue(targetPosition, oppositeProperty)) && CSSPositionUtils.isSet(getCssValue(targetPosition, sizeProperty)))
		{
			//the property is not set on the target, need to compute it using the size and the value of the opposite property
			String oppositePropertyValue = getCssValue(targetPosition, oppositeProperty);
			int computedPropertyValue = computeValueBasedOnOppositeProperty(targetPosition, property, targetContainerSize, oppositePropertyValue);

			if (isResize)
			{
				int computedDimension = computeDimension(property, computedPropertyValue + "", containerSize, getCssValue(oldPosition, oppositeProperty));
				setCssValue(newPosition, sizeProperty, computedDimension + "");
			}
			else
			{
				//clear the property because the target component does also not have it and we want the same anchoring
				setCssValue(newPosition, property, "-1");
				String dimension = getCssValue(oldPosition, sizeProperty);
				if (CSSPositionUtils.isSet(dimension))
				{
					//maintain the size of the old css position
					setCssValue(newPosition, sizeProperty, dimension);
				}
				else if (oldPosition != null)
				{
					//the size is not set (component anchored left-right or top-bottom),
					//need to compute it based on the opposing properties and the parent container size
					int computedDimension = computeDimension(property, getCssValue(oldPosition, property), containerSize,
						getCssValue(oldPosition, oppositeProperty));
					setCssValue(newPosition, sizeProperty, computedDimension + "");
				}
				//compute the opposite property value for the source component using the size property, container size and the computed property value of the target
				int computedOppositePropertySourceComponent = computeValueBasedOnOppositeProperty(newPosition, oppositeProperty, containerSize,
					computedPropertyValue + "");
				setCssValue(newPosition, oppositeProperty, computedOppositePropertySourceComponent + "");
			}
		}
		//else error?
	}

	private static int computeValueBasedOnOppositeProperty(CSSPosition position, String property, java.awt.Dimension containerSize,
		String oppositePropertyValue)
	{
		int oppositeValuePX = CSSPositionUtils.getPixelsValue(oppositePropertyValue);
		switch (property)
		{
			case "left" :
			case "right" :
				return containerSize.width - oppositeValuePX - CSSPositionUtils.getPixelsValue(position.width);

			case "top" :
			case "bottom" :
				return containerSize.height - oppositeValuePX - CSSPositionUtils.getPixelsValue(position.height);
		}
		return -1;
	}

	private static int computeValueBasedOnOppositeTargetProperty(String property, java.awt.Dimension containerSize,
		String oppositePropertyValue)
	{
		int oppositeValuePX = CSSPositionUtils.getPixelsValue(oppositePropertyValue);
		switch (property)
		{
			case "left" :
			case "right" :
				return containerSize.width - oppositeValuePX;

			case "top" :
			case "bottom" :
				return containerSize.height - oppositeValuePX;
		}
		return -1;
	}

	private static int computeDimension(String property, String value, java.awt.Dimension containerSize, String oppositePropertyValue)
	{
		int propertyValue = CSSPositionUtils.getPixelsValue(value);
		int oppositeValuePX = CSSPositionUtils.getPixelsValue(oppositePropertyValue);
		switch (property)
		{
			case "left" :
				return containerSize.width - oppositeValuePX - propertyValue;
			case "top" :
				return containerSize.height - oppositeValuePX - propertyValue;

			case "right" :
				return containerSize.width - propertyValue - oppositeValuePX;
			case "bottom" :
				return containerSize.height - propertyValue - oppositeValuePX;
		}
		return -1;
	}
}