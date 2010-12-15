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

package com.servoy.eclipse.designer.editor.palette;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.jface.resource.ImageDescriptor;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.palette.RequestTypeCreationFactory.IGetSize;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.BeanClassContentProvider;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.preferences.DesignerPreferences.PaletteCustomization;
import com.servoy.j2db.IServoyBeanFactory;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.util.Utils;


/**
 * Utility class that can create a GEF Palette for the Visual Form Editor.
 * 
 * @author rgansevles
 * @since 6.0
 */
public class VisualFormEditorPaletteFactory
{
	public static final String TEMPLATES_ID = "templates";
	public static final String ELEMENTS_ID = "elements";

	public static final String BEANS_ID_PREFIX = "beans:";
	public static final String SERVOY_BEANS_ID = BEANS_ID_PREFIX + "servoy";
	public static final String JAVA_BEANS_ID = BEANS_ID_PREFIX + "java";

	public static final String TEMPLATE_ID_PREFIX = "template:";

	private static final String ELEMENTS_LABEL_ID = "label";
	private static final String ELEMENTS_BUTTON_ID = "button";

	private static PaletteCustomization getDefaultPaletteCustomization()
	{
		List<String> drawers = new ArrayList<String>();
		Map<String, List<String>> drawerEntries = new HashMap<String, List<String>>();
		Map<String, Object> entryProperties = new HashMap<String, Object>();

		// add elements
		addElements(drawers, drawerEntries, entryProperties);

		// add templates
		addTemplates(drawers, drawerEntries, entryProperties);

		// add servoy beans
		addBeans(true, drawers, drawerEntries, entryProperties);

		// add other beans
		addBeans(false, drawers, drawerEntries, entryProperties);

		return new PaletteCustomization(drawers, drawerEntries, entryProperties);
	}

	private static void addElements(List<String> drawers, Map<String, List<String>> drawerEntries, Map<String, Object> entryProperties)
	{
		String id = ELEMENTS_ID;
		drawers.add(id);
		entryProperties.put(id + '.' + PaletteCustomization.PROPERTY_LABEL, Messages.LabelElementsPalette);
		String[] elements = new String[] { ELEMENTS_BUTTON_ID, ELEMENTS_LABEL_ID };
		drawerEntries.put(id, Arrays.asList(elements));
		for (String itemId : elements)
		{
			entryProperties.put(id + '.' + itemId + '.' + PaletteCustomization.PROPERTY_LABEL, Utils.stringInitCap(itemId));
			entryProperties.put(id + '.' + itemId + '.' + PaletteCustomization.PROPERTY_DESCRIPTION, "Create a " + itemId);
		}
	}

	private static void addTemplates(List<String> drawers, Map<String, List<String>> drawerEntries, Map<String, Object> entryProperties)
	{
		String id = TEMPLATES_ID;

		List<IRootObject> templates = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.TEMPLATES);
		Collections.sort(templates, NameComparator.INSTANCE);

		List<String> templateNames = new ArrayList<String>();
		for (IRootObject template : templates)
		{
			String templateId = template.getName();
			templateNames.add(templateId);
			entryProperties.put(id + '.' + templateId + '.' + PaletteCustomization.PROPERTY_LABEL, Utils.stringInitCap(templateId));
			entryProperties.put(id + '.' + templateId + '.' + PaletteCustomization.PROPERTY_DESCRIPTION, "Create/apply template " + templateId);

		}
		if (templateNames.size() > 0)
		{
			drawers.add(id);
			entryProperties.put(id + '.' + PaletteCustomization.PROPERTY_LABEL, Messages.LabelTemplatesPalette);
			drawerEntries.put(id, templateNames);
		}
	}

	private static void addBeans(boolean servoyBeans, List<String> drawers, Map<String, List<String>> drawerEntries, Map<String, Object> entryProperties)
	{
		String id = servoyBeans ? SERVOY_BEANS_ID : JAVA_BEANS_ID;

		Object[] beans = BeanClassContentProvider.DEFAULT.getElements(BeanClassContentProvider.BEANS_DUMMY_INPUT);
		List<String> beanIds = new ArrayList<String>();
		for (Object bean : beans)
		{
			if (bean instanceof BeanInfo)
			{
				BeanDescriptor beanDescriptor = ((BeanInfo)bean).getBeanDescriptor();
				if ((IServoyBeanFactory.class.isAssignableFrom(beanDescriptor.getBeanClass()) || IServoyAwareBean.class.isAssignableFrom(beanDescriptor.getBeanClass())) == servoyBeans)
				{
					String beanId = beanDescriptor.getBeanClass().getName();
					beanIds.add(beanId);

					String name = beanDescriptor.getDisplayName();
					if (name == null || name.length() == 0)
					{
						name = beanDescriptor.getName();
					}
					entryProperties.put(id + '.' + beanId + '.' + PaletteCustomization.PROPERTY_LABEL, name);
					String desc = beanDescriptor.getShortDescription();
					if (desc == null || desc.length() == 0)
					{
						desc = "Place bean " + name;
					}
					entryProperties.put(id + '.' + beanId + '.' + PaletteCustomization.PROPERTY_DESCRIPTION, desc);
				}
			}
		}

		if (beanIds.size() > 0)
		{
			drawers.add(id);
			entryProperties.put(id + '.' + PaletteCustomization.PROPERTY_LABEL, servoyBeans ? Messages.LabelServoyBeansPalette : Messages.LabelJavaBeansPalette);
			drawerEntries.put(id, beanIds);
		}
	}

	private static PaletteEntry createPaletteEntry(String drawerId, String id)
	{
		if (ELEMENTS_ID.equals(drawerId))
		{
			return createElementsEntry(id);
		}

		if (TEMPLATES_ID.equals(drawerId))
		{
			return createTemplatesEntry(id);
		}

		if (drawerId.startsWith(BEANS_ID_PREFIX))
		{
			return createBeansEntry(id);
		}

		if (drawerId.startsWith(TEMPLATE_ID_PREFIX))
		{
			return createTemplateToolEntry(drawerId.substring(TEMPLATE_ID_PREFIX.length()), id);
		}

		ServoyLog.logError("Unknown palette drawer: '" + drawerId + "'", null);
		return null;
	}

	private static PaletteEntry createElementsEntry(String id)
	{
		if (ELEMENTS_BUTTON_ID.equals(id))
		{
			return new ElementCreationToolEntry("", " ", new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_BUTTON, new Dimension(80, 20)),
				Activator.loadImageDescriptorFromBundle("button.gif"), Activator.loadImageDescriptorFromBundle("button.gif"));
		}

		if (ELEMENTS_LABEL_ID.equals(id))
		{
			return new ElementCreationToolEntry("", "", new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_LABEL, new Dimension(80, 20)),
				Activator.loadImageDescriptorFromBundle("text.gif"), Activator.loadImageDescriptorFromBundle("text.gif"));
		}

		// TODO: Add more

		ServoyLog.logError("Unknown palette elements entry: '" + id + "'", null);
		return null;
	}

	private static PaletteEntry createBeansEntry(final String beanClassName)
	{
		// determine preferred size in a lazy way, only when object is really dragged
		RequestTypeCreationFactory factory = new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_BEAN, new IGetSize()
		{
			public Dimension getSize()
			{
				try
				{
					java.awt.Dimension prefferredSize = ElementFactory.getBeanPrefferredSize(ComponentFactory.getBeanInstanceFromXML(
						com.servoy.eclipse.core.Activator.getDefault().getDesignClient(), beanClassName, null));
					if (prefferredSize != null)
					{
						return new Dimension(prefferredSize.width, prefferredSize.height);
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError("Could not instantiate bean " + beanClassName, e);
				}
				return new Dimension(80, 80);
			}
		});
		factory.setData(beanClassName);
		ImageDescriptor icon = Activator.loadImageDescriptorFromBundle("bean.gif");
		return new ElementCreationToolEntry("", "", factory, icon, icon);
	}

	private static PaletteEntry createTemplatesEntry(String id)
	{
		Template template = (Template)ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObject(id, IRepository.TEMPLATES);
		if (template == null)
		{
			return null;
		}

		TemplateElementHolder data = new TemplateElementHolder(template);
		RequestTypeCreationFactory factory = new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_TEMPLATE,
			DesignerUtil.convertDimension(ElementFactory.getTemplateBoundsize(data)));
		factory.setData(data);
		ImageDescriptor icon = getTemplateIcon(data);
		if (icon == null)
		{
			// default icon
			icon = Activator.loadImageDescriptorFromBundle("template.gif");
		}
		return new ElementCreationToolEntry("", "", factory, icon, icon);
	}

	private static PaletteEntry createTemplateToolEntry(String templateName, String elementName)
	{
		Template template = (Template)ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObject(templateName, IRepository.TEMPLATES);
		if (template == null)
		{
			return null;
		}

		TemplateElementHolder templateHolder = new TemplateElementHolder(template);
		List<JSONObject> templateElements = ElementFactory.getTemplateElements(templateHolder);
		if (templateElements != null)
		{
			for (JSONObject jsonObject : templateElements)
			{
				if (elementName.equals(jsonObject.optString(SolutionSerializer.PROP_NAME)))
				{
					return VisualFormEditorPaletteFactory.createTemplateToolEntry(templateHolder.template, jsonObject, elementName);
				}
			}
		}

		return null;
	}

	/**
	 * @param json
	 * @return
	 */
	public static PaletteEntry createTemplateToolEntry(Template template, JSONObject json, String name)
	{
		TemplateElementHolder data = new TemplateElementHolder(template, name);
		RequestTypeCreationFactory factory = new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_TEMPLATE,
			DesignerUtil.convertDimension(ElementFactory.getTemplateBoundsize(data)));
		factory.setData(data);
		ImageDescriptor icon = getTemplateIcon(json);
		if (icon == null)
		{
			// default icon
			icon = Activator.loadImageDescriptorFromBundle("template.gif");
		}
		return new ElementCreationToolEntry(Utils.stringInitCap(name), "Create/apply template item " + name, factory, icon, icon);
	}

	static ImageDescriptor getTemplateIcon(TemplateElementHolder template)
	{
		// elements
		List<JSONObject> elements = ElementFactory.getTemplateElements(template);

		if (elements == null || elements.size() == 0)
		{
			return null;
		}

		if (elements.size() > 1)
		{
			return Activator.loadImageDescriptorFromBundle("group.gif");
		}

		return getTemplateIcon(elements.get(0));
	}

	static ImageDescriptor getTemplateIcon(JSONObject object)
	{
		switch (object.optInt(SolutionSerializer.PROP_TYPEID))
		{
			case IRepository.FIELDS :

				switch (object.optInt("displayType"))
				{
					case Field.CHECKS :
						return Activator.loadImageDescriptorFromBundle("chk_on_icon.gif");

					case Field.RADIOS :
						return Activator.loadImageDescriptorFromBundle("radio_on.gif");

					case Field.COMBOBOX :
						return Activator.loadImageDescriptorFromBundle("dropdown_icon.gif");

					case Field.CALENDAR :
						return Activator.loadImageDescriptorFromBundle("calendar_icon.gif");

				}

				return Activator.loadImageDescriptorFromBundle("field.gif");

			case IRepository.GRAPHICALCOMPONENTS :
				if (object.optInt("onActionMethodID") == 0 || !object.optBoolean("showClick", true))
				{
					if (object.has("imageMediaID"))
					{
						return Activator.loadImageDescriptorFromBundle("image.gif");
					}
					return Activator.loadImageDescriptorFromBundle("text.gif");
				}

				return Activator.loadImageDescriptorFromBundle("button.gif");

			case IRepository.RECTSHAPES :
			case IRepository.SHAPES :
				return Activator.loadImageDescriptorFromBundle("rectangle.gif");

			case IRepository.PORTALS :
				return Activator.loadImageDescriptorFromBundle("portal.gif");


			case IRepository.TABPANELS :
				int orient = object.optInt("tabOrientation");
				if (orient == TabPanel.SPLIT_HORIZONTAL || orient == TabPanel.SPLIT_VERTICAL)
				{
					return Activator.loadImageDescriptorFromBundle("split.gif");
				}
				return Activator.loadImageDescriptorFromBundle("tabs.gif");

			case IRepository.TABS :
				return Activator.loadImageDescriptorFromBundle("tabs.gif");

			case IRepository.BEANS :
				return Activator.loadImageDescriptorFromBundle("bean.gif");
		}

		return null;
	}


	/**
	 * Creates the PaletteRoot and adds all palette elements. Use this factory
	 * method to create a new palette for your graphical editor.
	 * 
	 * @return a new PaletteRoot
	 */
	static public PaletteRoot createPalette()
	{
		PaletteRoot palette = new PaletteRoot();
		fillPalette(palette);
		return palette;
	}

	private static void fillPalette(PaletteRoot palette)
	{
		PaletteCustomization defaultPaletteCustomization = getDefaultPaletteCustomization();
		PaletteCustomization savedPaletteCustomization = new DesignerPreferences().getPaletteCustomization();
		PaletteCustomization paletteCustomization = savedPaletteCustomization == null ? defaultPaletteCustomization : savedPaletteCustomization;

		for (String drawerId : paletteCustomization.drawers == null ? defaultPaletteCustomization.drawers : paletteCustomization.drawers)
		{
			List<String> itemIds = null;
			if (paletteCustomization.drawerEntries != null)
			{
				itemIds = paletteCustomization.drawerEntries.get(drawerId);
			}
			if (itemIds == null)
			{
				itemIds = defaultPaletteCustomization.drawerEntries.get(drawerId);
			}
			else
			{
				List<String> defaultItemIds = defaultPaletteCustomization.drawerEntries.get(drawerId);
				if (defaultItemIds != null)
				{
					List<String> combinedIds = new ArrayList<String>(itemIds);
					for (int i = 0; i < defaultItemIds.size(); i++)
					{
						String defaultItemId = defaultItemIds.get(i);
						if (combinedIds.indexOf(defaultItemId) < 0)
						{
							// a new default item, add after previous item or at the beginning
							int idx = (i > 0) ? combinedIds.indexOf(defaultItemIds.get(i - 1)) : 0;
							combinedIds.add(idx, defaultItemId);
						}
					}
					itemIds = combinedIds;
				}
			}

			if (itemIds != null && itemIds.size() > 0)
			{
				PaletteDrawer drawer = new PaletteDrawer("");
				drawer.setId(drawerId);
				drawer.setUserModificationPermission(drawerId.startsWith(TEMPLATE_ID_PREFIX) ? PaletteEntry.PERMISSION_FULL_MODIFICATION
					: PaletteEntry.PERMISSION_LIMITED_MODIFICATION);
				applyPaletteCustomization(paletteCustomization.entryProperties, drawerId, drawer, defaultPaletteCustomization.entryProperties);

				palette.add(drawer);

				for (String itemId : itemIds)
				{
					PaletteEntry entry = createPaletteEntry(drawerId, itemId);
					if (entry != null)
					{
						entry.setId(itemId);
						entry.setUserModificationPermission(drawerId.startsWith(TEMPLATE_ID_PREFIX) ? PaletteEntry.PERMISSION_FULL_MODIFICATION
							: PaletteEntry.PERMISSION_LIMITED_MODIFICATION);
						applyPaletteCustomization(paletteCustomization.entryProperties, drawerId + '.' + itemId, entry,
							defaultPaletteCustomization.entryProperties);
						drawer.add(entry);
					}
				}
			}
		}
	}

	private static void applyPaletteCustomization(Map<String, Object> map, String key, PaletteEntry entry, Map<String, Object> defaults)
	{
		entry.setVisible(!getValueWithDefaults(map, key + '.' + PaletteCustomization.PROPERTY_HIDDEN, defaults, Boolean.FALSE).booleanValue());
		entry.setLabel(getValueWithDefaults(map, key + '.' + PaletteCustomization.PROPERTY_LABEL, defaults, ""));
		entry.setDescription(getValueWithDefaults(map, key + '.' + PaletteCustomization.PROPERTY_DESCRIPTION, defaults, ""));

		if (entry instanceof PaletteDrawer)
		{
			((PaletteDrawer)entry).setInitialState(getValueWithDefaults(map, key + '.' + PaletteCustomization.PROPERTY_INITIAL_STATE, defaults,
				Integer.valueOf(PaletteDrawer.INITIAL_STATE_OPEN)).intValue());
		}
	}

	private static <T> T getValueWithDefaults(Map<String, Object> map, String key, Map<String, Object> defaults, T defval)
	{
		if (map != null && map.containsKey(key))
		{
			return (T)map.get(key);
		}
		if (defaults != null && defaults.containsKey(key))
		{
			return (T)defaults.get(key);
		}
		return defval;
	}

	public static void refreshPalette(PaletteRoot palette)
	{
		palette.setChildren(new ArrayList<PaletteEntry>());
		fillPalette(palette);
	}

	public static void savePaletteCustomization(PaletteRoot palette)
	{
		// get the current and the default drawers and entries, save where they differ
		PaletteCustomization defaultPaletteCustomization = getDefaultPaletteCustomization();

		Map<String, List<String>> drawerEntries = new HashMap<String, List<String>>();
		Map<String, Object> entryProperties = new HashMap<String, Object>();
		List<String> drawerIds = new ArrayList<String>();

		for (PaletteEntry drawer : ((List<PaletteEntry>)palette.getChildren()))
		{
			if (drawer instanceof PaletteDrawer)
			{
				String drawerId = drawer.getId();
				drawerIds.add(drawerId);
				addPaletteEntryProperties(entryProperties, drawerId, drawer, defaultPaletteCustomization.entryProperties);

				List<String> itemIds = new ArrayList<String>();
				for (PaletteEntry entry : (List<PaletteEntry>)((PaletteDrawer)drawer).getChildren())
				{
					itemIds.add(entry.getId());
					addPaletteEntryProperties(entryProperties, drawerId + '.' + entry.getId(), entry, defaultPaletteCustomization.entryProperties);
				}
				List<String> defaultItemIds = defaultPaletteCustomization.drawerEntries.get(drawerId);
				if (!itemIds.equals(defaultItemIds))
				{
					drawerEntries.put(drawerId, itemIds);
				}
				// else do not save
			}
		}

		if (defaultPaletteCustomization.drawers.equals(drawerIds))
		{
			drawerIds = null; // do not save
		}

		new DesignerPreferences().setPaletteCustomization(new PaletteCustomization(drawerIds, drawerEntries, entryProperties));
	}

	/**
	 * @param entryProperties
	 * @param id
	 * @param entry
	 */
	private static void addPaletteEntryProperties(Map<String, Object> map, String key, PaletteEntry entry, Map<String, Object> defaults)
	{
		putIfNotDefault(map, key + '.' + PaletteCustomization.PROPERTY_HIDDEN, Boolean.valueOf(!entry.isVisible()), defaults, Boolean.FALSE);
		putIfNotDefault(map, key + '.' + PaletteCustomization.PROPERTY_LABEL, entry.getLabel(), defaults, null);
		putIfNotDefault(map, key + '.' + PaletteCustomization.PROPERTY_DESCRIPTION, entry.getDescription(), defaults, "");
		if (entry instanceof PaletteDrawer)
		{
			putIfNotDefault(map, key + '.' + PaletteCustomization.PROPERTY_INITIAL_STATE, Integer.valueOf(((PaletteDrawer)entry).getInitialState()), defaults,
				Integer.valueOf(PaletteDrawer.INITIAL_STATE_OPEN));
		}
	}

	private static void putIfNotDefault(Map<String, Object> map, String key, Object value, Map<String, Object> defaults, Object defval)
	{
		Object def = defaults.containsKey(key) ? defaults.get(key) : defval;
		if (def != null ? !def.equals(value) : value != null)
		{
			map.put(key, value);
		}
	}
}
