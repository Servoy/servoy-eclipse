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

import java.awt.Image;
import java.awt.Insets;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.jface.resource.ImageDescriptor;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.ValuesConfig;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.designer.Activator;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor.RequestType;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.palette.RequestTypeCreationFactory.IGetSize;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.BeanClassContentProvider;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.preferences.DesignerPreferences.PaletteCustomization;
import com.servoy.eclipse.ui.property.BorderPropertyController;
import com.servoy.eclipse.ui.property.BorderPropertyController.BorderType;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.PersistPropertyHandler;
import com.servoy.j2db.IServoyBeanFactory;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;


/**
 * Utility class that can create a GEF Palette for the Visual Form Editor.
 *
 * @author rgansevles
 * @since 6.0
 */
public class VisualFormEditorPaletteFactory extends BaseVisualFormEditorPaletteFactory
{
	private static final String ELEMENTS_ID = "elements";
	private static final String ELEMENTS_LABEL_ID = "label";
	private static final String ELEMENTS_BUTTON_ID = "button";
	private static final String ELEMENTS_TEXT_FIELD_ID = "text field";
	private static final String ELEMENTS_TEXT_AREA_ID = "text area";
	private static final String ELEMENTS_COMBOBOX_ID = "combobox";
	private static final String ELEMENTS_RADIOS_ID = "radio button";
	private static final String ELEMENTS_CHECKS_ID = "checkbox";
	private static final String ELEMENTS_CALENDAR_ID = "calendar";
	private static final String ELEMENTS_PASSWORD_ID = "password field";
	private static final String ELEMENTS_RTF_AREA_ID = "rtf area";
	private static final String ELEMENTS_HTML_AREA_ID = "html area";
	private static final String ELEMENTS_IMAGE_MEDIA_ID = "media field";
	private static final String ELEMENTS_TYPE_AHEAD_ID = "type ahead";
	private static final String ELEMENTS_PORTAL_ID = "portal";
	private static final String ELEMENTS_LISTBOX_ID = "listbox";
	private static final String ELEMENTS_MULTISELECT_LISTBOX_ID = "multiselect listbox";
	private static final String ELEMENTS_SPINNER_ID = "spinner";
	private static final String[] ELEMENTS_IDS = new String[] {
	/* */ELEMENTS_BUTTON_ID,
	/* */ELEMENTS_RADIOS_ID,
	/* */ELEMENTS_CHECKS_ID,
	/* */ELEMENTS_LABEL_ID,
	/* */ELEMENTS_TEXT_FIELD_ID,
	/* */ELEMENTS_TEXT_AREA_ID,
	/* */ELEMENTS_HTML_AREA_ID,
	/* */ELEMENTS_RTF_AREA_ID,
	/* */ELEMENTS_PASSWORD_ID,
	/* */ELEMENTS_CALENDAR_ID,
	/* */ELEMENTS_IMAGE_MEDIA_ID,
	/* */ELEMENTS_COMBOBOX_ID,
	/* */ELEMENTS_LISTBOX_ID,
	/* */ELEMENTS_MULTISELECT_LISTBOX_ID,
	/* */ELEMENTS_TYPE_AHEAD_ID,
	/* */ELEMENTS_SPINNER_ID,
	/* */ELEMENTS_PORTAL_ID
	/* */};

	private static final String SHAPES_ID = "shapes";
	private static final String SHAPES_BORDER_PANEL_ID = "border panel";
	private static final String SHAPES_RECTANGLE_ID = "rectangle";
	private static final String SHAPES_ROUNDED_RECTANGLE_ID = "rounded rectangle";
	private static final String SHAPES_OVAL_ID = "circle";
	private static final String SHAPES_HORIZONTAL_LINE_ID = "horizontal line";
	private static final String SHAPES_VERTICAL_LINE_ID = "vertical line";
	private static final String[] SHAPES_IDS = new String[] {
	/* */SHAPES_BORDER_PANEL_ID,
	/* */SHAPES_RECTANGLE_ID,
	/* */SHAPES_ROUNDED_RECTANGLE_ID,
	/* */SHAPES_OVAL_ID,
	/* */SHAPES_HORIZONTAL_LINE_ID,
	/* */SHAPES_VERTICAL_LINE_ID
	/* */};

	private static final String BEANS_ID_PREFIX = "beans:";
	private static final String SERVOY_BEANS_ID = BEANS_ID_PREFIX + "servoy";
	private static final String JAVA_BEANS_ID = BEANS_ID_PREFIX + "java";
	private static final Map<String, BeanInfo> beanInfos = new HashMap<String, BeanInfo>();

	private static final String COMPONENTS_ID = "components";

	private static final String TEMPLATES_ID = "templates";
	protected static final String TEMPLATE_ID_PREFIX = "template:";

	private static final String CONTAINERS_ID = "containers";
	private static final String CONTAINERS_DEFAULT_PANEL_ID = "tabpanel";
	private static final String CONTAINERS_TABLESS_PANEL_ID = "tabless panel";
	private static final String CONTAINERS_SPLIT_PANE_HORIZONTAL_ID = "split pane";
	private static final String CONTAINERS_ACCORDION_PANEL_ID = "accordion panel";
	private static final String[] CONTAINERS_IDS = new String[] {
	/* */CONTAINERS_DEFAULT_PANEL_ID,
	/* */CONTAINERS_TABLESS_PANEL_ID,
	/* */CONTAINERS_SPLIT_PANE_HORIZONTAL_ID,
	/* */CONTAINERS_ACCORDION_PANEL_ID
	/* */};

	@Override
	protected PaletteCustomization getDefaultPaletteCustomization()
	{
		List<String> drawers = new ArrayList<String>();
		Map<String, List<String>> drawerEntries = new HashMap<String, List<String>>();
		Map<String, Object> entryProperties = new HashMap<String, Object>();

		// add elements
		drawers.add(ELEMENTS_ID);
		entryProperties.put(ELEMENTS_ID + '.' + PaletteCustomization.PROPERTY_LABEL, Messages.LabelElementsPalette);
		drawerEntries.put(ELEMENTS_ID, Arrays.asList(ELEMENTS_IDS));
		for (String itemId : ELEMENTS_IDS)
		{
			entryProperties.put(ELEMENTS_ID + '.' + itemId + '.' + PaletteCustomization.PROPERTY_LABEL, Utils.stringInitCap(itemId));
			entryProperties.put(ELEMENTS_ID + '.' + itemId + '.' + PaletteCustomization.PROPERTY_DESCRIPTION, "Create a " + itemId);
		}

		// add containers
		addFixedEntries(CONTAINERS_ID, Messages.LabelContainersPalette, CONTAINERS_IDS, drawers, drawerEntries, entryProperties);

		// add templates
		addTemplates(drawers, drawerEntries, entryProperties);

		// add servoy beans
		addBeans(true, drawers, drawerEntries, entryProperties);

		// add other beans
		addBeans(false, drawers, drawerEntries, entryProperties);

		// add components
		addComponents(drawers, drawerEntries, entryProperties);

		// add shapes
		addFixedEntries(SHAPES_ID, Messages.LabelShapesPalette, SHAPES_IDS, drawers, drawerEntries, entryProperties);

		return new PaletteCustomization(drawers, drawerEntries, entryProperties);
	}

	private static void addTemplates(List<String> drawers, Map<String, List<String>> drawerEntries, Map<String, Object> entryProperties)
	{
		List<IRootObject> templates = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.TEMPLATES);
		if (templates.size() > 0)
		{
			String id = TEMPLATES_ID;
			Collections.sort(templates, NameComparator.INSTANCE);

			List<String> templateNames = new ArrayList<String>();
			for (IRootObject template : templates)
			{
				String templateId = template.getName();
				templateNames.add(templateId);
				entryProperties.put(id + '.' + templateId + '.' + PaletteCustomization.PROPERTY_LABEL, Utils.stringInitCap(templateId));
				entryProperties.put(id + '.' + templateId + '.' + PaletteCustomization.PROPERTY_DESCRIPTION, "Create/apply template " + templateId);
			}

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
				if (beanDescriptor.getBeanClass() != null &&
					((IServoyBeanFactory.class.isAssignableFrom(beanDescriptor.getBeanClass()) || IServoyAwareBean.class.isAssignableFrom(beanDescriptor.getBeanClass())) == servoyBeans))
				{
					String beanId = beanDescriptor.getBeanClass().getName();
					beanIds.add(beanId);
					beanInfos.put(beanId, (BeanInfo)bean);

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
			// by default hide java beans
			entryProperties.put(id + '.' + PaletteCustomization.PROPERTY_HIDDEN, Boolean.valueOf(!servoyBeans));
			drawerEntries.put(id, beanIds);
		}
	}

	private static void addComponents(List<String> drawers, Map<String, List<String>> drawerEntries, Map<String, Object> entryProperties)
	{
		Map<String, List<String>> allComponents = new HashMap<String, List<String>>();
		Map<String, String> drawerNames = new HashMap<String, String>();

		for (WebComponentSpecification spec : WebComponentSpecProvider.getInstance().getWebComponentSpecifications())
		{
			String packageName = spec.getPackageName();
			String id = COMPONENTS_ID + "." + packageName;

			drawerNames.put(id, packageName);

			List<String> componentIds = new ArrayList<String>();
			if (allComponents.get(id) != null) componentIds = allComponents.get(id);
			else
			{
				allComponents.put(id, componentIds);
			}

			String componentId = spec.getName();
			componentIds.add(componentId);

			String name = spec.getDisplayName();
			if (name == null || name.length() == 0)
			{
				name = "<UNKNOWN>";
			}
			entryProperties.put(id + '.' + componentId + '.' + PaletteCustomization.PROPERTY_LABEL, name);
			entryProperties.put(id + '.' + componentId + '.' + PaletteCustomization.PROPERTY_DESCRIPTION, "Place component " + name);
		}

		for (String containerPackageID : allComponents.keySet())
		{
			List<String> componentIds = allComponents.get(containerPackageID);
			if (componentIds.size() > 0)
			{
				drawers.add(containerPackageID);
				entryProperties.put(containerPackageID + '.' + PaletteCustomization.PROPERTY_LABEL, drawerNames.get(containerPackageID));
				entryProperties.put(containerPackageID + '.' + PaletteCustomization.PROPERTY_HIDDEN, Boolean.FALSE); // TRUE if you want to hide components by default
				drawerEntries.put(containerPackageID, allComponents.get(containerPackageID));
			}
		}


	}

	private PaletteEntry createPaletteEntry(String drawerId, String id)
	{
		if (ELEMENTS_ID.equals(drawerId))
		{
			return createElementsEntry(id);
		}

		if (SHAPES_ID.equals(drawerId))
		{
			return createShapesEntry(id);
		}

		if (CONTAINERS_ID.equals(drawerId))
		{
			return createContainersEntry(id);
		}

		if (TEMPLATES_ID.equals(drawerId))
		{
			return createTemplatesEntry(id);
		}

		if (drawerId.startsWith(COMPONENTS_ID))
		{
			return createComponentsEntry(id);
		}

		if (drawerId.startsWith(BEANS_ID_PREFIX))
		{
			return createBeansEntry(id);
		}

		if (drawerId.startsWith(TEMPLATE_ID_PREFIX))
		{
			int element;
			try
			{
				element = Integer.parseInt(id);
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
				return null;
			}
			return createTemplateToolEntry(drawerId.substring(TEMPLATE_ID_PREFIX.length()), element);
		}

		ServoyLog.logError("Unknown palette drawer: '" + drawerId + "'", null);
		return null;
	}

	private PaletteEntry createElementsEntry(String id)
	{
		ImageDescriptor icon = null;
		Dimension size = new Dimension(140, 20);
		RequestType requestType = VisualFormEditor.REQ_PLACE_FIELD;
		int displayType = -1;
		Map<String, Object> extendedData = new HashMap<String, Object>();

		if (ELEMENTS_BUTTON_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("button.gif");
			requestType = VisualFormEditor.REQ_PLACE_BUTTON;
			size = new Dimension(80, 20);
		}

		else if (ELEMENTS_LABEL_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("text.gif");
			requestType = VisualFormEditor.REQ_PLACE_LABEL;
			size = new Dimension(80, 20);
		}

		else if (ELEMENTS_TEXT_FIELD_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("field.gif");
		}

		else if (ELEMENTS_TEXT_AREA_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("TEXTAREA16.png");
			displayType = Field.TEXT_AREA;
			size = new Dimension(140, 140);
		}

		else if (ELEMENTS_RTF_AREA_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("doc_rtf.png");
			displayType = Field.RTF_AREA;
			size = new Dimension(140, 140);
			setProperty(extendedData, StaticContentSpecLoader.PROPERTY_EDITABLE, Boolean.FALSE);
		}

		else if (ELEMENTS_HTML_AREA_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("H1_C16.png");
			displayType = Field.HTML_AREA;
			size = new Dimension(140, 140);
			setProperty(extendedData, StaticContentSpecLoader.PROPERTY_EDITABLE, Boolean.FALSE);
		}

		else if (ELEMENTS_COMBOBOX_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("SELECT16.png");
			displayType = Field.COMBOBOX;
		}

		else if (ELEMENTS_RADIOS_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("RADIO16.png");
			displayType = Field.RADIOS;
			setProperty(extendedData, StaticContentSpecLoader.PROPERTY_TRANSPARENT, Boolean.TRUE);
		}

		else if (ELEMENTS_CHECKS_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("CHECKBOX16.png");
			displayType = Field.CHECKS;
			setProperty(extendedData, StaticContentSpecLoader.PROPERTY_TRANSPARENT, Boolean.TRUE);
		}

		else if (ELEMENTS_CALENDAR_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("Calendar_C16.png");
			displayType = Field.CALENDAR;
		}

		else if (ELEMENTS_PASSWORD_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("password_field_16.png");
			displayType = Field.PASSWORD;
		}

		else if (ELEMENTS_IMAGE_MEDIA_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("IMG16.png");
			displayType = Field.IMAGE_MEDIA;
		}

		else if (ELEMENTS_TYPE_AHEAD_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("bhdropdownlisticon.gif");
			displayType = Field.TYPE_AHEAD;
		}
		else if (ELEMENTS_LISTBOX_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("listbox.png");
			displayType = Field.LIST_BOX;
			size = new Dimension(140, 140);
		}
		else if (ELEMENTS_MULTISELECT_LISTBOX_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("listbox.png");
			displayType = Field.MULTISELECT_LISTBOX;
			size = new Dimension(140, 140);
		}
		else if (ELEMENTS_SPINNER_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("spinner.png");
			displayType = Field.SPINNER;
		}

		else if (ELEMENTS_PORTAL_ID.equals(id))
		{
			requestType = VisualFormEditor.REQ_PLACE_PORTAL;
			size = new Dimension(200, 200);
			icon = com.servoy.eclipse.designer.Activator.loadImageDescriptorFromBundle("portal.gif");
		}

		if (icon != null)
		{
			if (displayType != -1)
			{
				setProperty(extendedData, StaticContentSpecLoader.PROPERTY_DISPLAYTYPE,
					Integer.valueOf(((ValuesConfig)PersistPropertyHandler.DISPLAY_TYPE_VALUES.getConfig()).getRealIndexOf(Integer.valueOf(displayType))));
			}
			RequestTypeCreationFactory factory = new RequestTypeCreationFactory(requestType, size);
			factory.setExtendedData(extendedData);
			return new ElementCreationToolEntry("", "", factory, icon, icon);
		}

		ServoyLog.logError("Unknown palette elements entry: '" + id + "'", null);
		return null;
	}

	private PaletteEntry createShapesEntry(String id)
	{
		ImageDescriptor icon = null;
		Dimension size = new Dimension(70, 70);
		RequestType requestType = VisualFormEditor.REQ_PLACE_RECT_SHAPE;
		int shapeType = -1;
		Map<String, Object> extendedData = new HashMap<String, Object>();

		if (SHAPES_BORDER_PANEL_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("titledBorder.gif");
			shapeType = RectShape.BORDER_PANEL;
			setProperty(extendedData, StaticContentSpecLoader.PROPERTY_BORDERTYPE, new ComplexProperty<Border>(
				BorderPropertyController.getDefaultBorderValuesMap().get(BorderType.Title)));
		}

		else if (SHAPES_RECTANGLE_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("rectangle.gif");
			shapeType = RectShape.RECTANGLE;
		}

		else if (SHAPES_ROUNDED_RECTANGLE_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("rrect.gif");
			shapeType = RectShape.ROUNDED_RECTANGLE;
			setProperty(extendedData, StaticContentSpecLoader.PROPERTY_ROUNDEDRADIUS, Integer.valueOf(35));
		}

		else if (SHAPES_OVAL_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("circle.gif");
			shapeType = RectShape.OVAL;
		}

		else if (SHAPES_HORIZONTAL_LINE_ID.equals(id) || SHAPES_VERTICAL_LINE_ID.equals(id))
		{
			float top, left;
			if (SHAPES_HORIZONTAL_LINE_ID.equals(id))
			{
				icon = Activator.loadImageDescriptorFromBundle("hline.gif");
				top = 1;
				left = 0;
				size = new Dimension(40, 1);
			}
			else
			{
				// SHAPES_VERTICAL_LINE_ID
				icon = Activator.loadImageDescriptorFromBundle("vline.gif");
				top = 0;
				left = 1;
				size = new Dimension(1, 40);
			}

			requestType = VisualFormEditor.REQ_PLACE_LABEL;
			setProperty(extendedData, StaticContentSpecLoader.PROPERTY_TEXT, "");
			setProperty(extendedData, StaticContentSpecLoader.PROPERTY_BORDERTYPE, new ComplexProperty<Border>(
				BorderPropertyController.getDefaultBorderValuesMap().get(BorderType.SpecialMatte)));
			setProperty(extendedData, StaticContentSpecLoader.PROPERTY_BORDERTYPE.getPropertyName() + '.' + "width", new ComplexProperty<Insets>(new Insets(
				(int)top, (int)left, 0, 0)));
			setProperty(
				extendedData,
				StaticContentSpecLoader.PROPERTY_HORIZONTALALIGNMENT,
				Integer.valueOf(((ValuesConfig)PersistPropertyHandler.HORIZONTAL_ALIGNMENT_VALUES.getConfig()).getRealIndexOf(Integer.valueOf(SwingConstants.RIGHT))));
			setProperty(extendedData, StaticContentSpecLoader.PROPERTY_TRANSPARENT, Boolean.TRUE);
		}

		if (icon != null)
		{
			if (shapeType != -1)
			{
				setProperty(extendedData, StaticContentSpecLoader.PROPERTY_SHAPETYPE,
					Integer.valueOf(((ValuesConfig)PersistPropertyHandler.SHAPE_TYPE_VALUES.getConfig()).getRealIndexOf(Integer.valueOf(shapeType))));
			}
			RequestTypeCreationFactory factory = new RequestTypeCreationFactory(requestType, size);
			factory.setExtendedData(extendedData);
			return new ElementCreationToolEntry("", "", factory, icon, icon);
		}

		ServoyLog.logError("Unknown palette elements entry: '" + id + "'", null);
		return null;
	}

	private PaletteEntry createContainersEntry(String id)
	{
		ImageDescriptor icon = null;
		RequestTypeCreationFactory factory = null;
		String nameHint = null;

		// tab panels
		int tabOrienation = TabPanel.DEFAULT_ORIENTATION;
		if (CONTAINERS_DEFAULT_PANEL_ID.equals(id))
		{
			icon = com.servoy.eclipse.designer.Activator.loadImageDescriptorFromBundle("tabs.gif");
		}
		else if (CONTAINERS_SPLIT_PANE_HORIZONTAL_ID.equals(id))
		{
			icon = com.servoy.eclipse.designer.Activator.loadImageDescriptorFromBundle("split.gif");
			tabOrienation = TabPanel.SPLIT_HORIZONTAL;
			nameHint = "split";
		}
		else if (CONTAINERS_TABLESS_PANEL_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("tabless.gif");
			tabOrienation = TabPanel.HIDE;
			nameHint = "tabless";
		}
		else if (CONTAINERS_ACCORDION_PANEL_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("accordion.jpg");
			tabOrienation = TabPanel.ACCORDION_PANEL;
			nameHint = "accordion";
		}

		if (icon != null)
		{
			// one of the tab panels above
			factory = new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_TAB, new Dimension(300, 300));
			setProperty(factory.getExtendedData(), StaticContentSpecLoader.PROPERTY_TABORIENTATION,
				Integer.valueOf(((ValuesConfig)PersistPropertyHandler.TAB_ORIENTATION_VALUES.getConfig()).getRealIndexOf(Integer.valueOf(tabOrienation))));
			if (nameHint != null)
			{
				factory.getExtendedData().put(ElementFactory.NAME_HINT_PROPERTY, nameHint);
			}
		}

		if (factory == null)
		{
			ServoyLog.logError("Unknown palette containers entry: '" + id + "'", null);
			return null;
		}

		return new ElementCreationToolEntry("", " ", factory, icon, icon);
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

		Image smallIcon = null, largeIcon = null;
		BeanInfo beanInfo = beanInfos.get(beanClassName);
		if (beanInfo != null)
		{
			smallIcon = beanInfo.getIcon(BeanInfo.ICON_COLOR_16x16);
			if (smallIcon == null) smallIcon = beanInfo.getIcon(BeanInfo.ICON_MONO_16x16);
			if (smallIcon == null) smallIcon = beanInfo.getIcon(BeanInfo.ICON_COLOR_32x32);
			if (smallIcon == null) smallIcon = beanInfo.getIcon(BeanInfo.ICON_MONO_32x32);
			if (smallIcon != null)
			{
				largeIcon = beanInfo.getIcon(BeanInfo.ICON_COLOR_32x32);
				if (largeIcon == null) largeIcon = beanInfo.getIcon(BeanInfo.ICON_MONO_32x32);
			}
		}
		ImageDescriptor iconSmall, iconLarge;
		if (smallIcon == null)
		{
			iconSmall = Activator.loadImageDescriptorFromBundle("bean.gif");
		}
		else
		{
			iconSmall = UIUtils.createImageDescriptorFromAwtImage(smallIcon, true);
		}
		if (largeIcon == smallIcon || largeIcon == null)
		{
			iconLarge = iconSmall;
		}
		else
		{
			iconLarge = UIUtils.createImageDescriptorFromAwtImage(largeIcon, true);
		}

		return new ElementCreationToolEntry("", "", factory, iconSmall, iconLarge);
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

	private static PaletteEntry createComponentsEntry(String beanClassName)
	{
		String webComponentClassName = FormTemplateGenerator.getComponentTypeName(beanClassName);
		WebComponentSpecification webComponentDescription = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponentClassName);
		Dimension dimension = getDimensionFromSpec(webComponentDescription);
		ImageDescriptor beanIcon = Activator.loadImageDescriptorFromBundle("bean.gif");
		RequestTypeCreationFactory factory = new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_BEAN, dimension);
		factory.setData(beanClassName);

		return new ElementCreationToolEntry("", "", factory, beanIcon, beanIcon);
	}

	/**
	 * @param webComponentDescription
	 * @param dimension
	 * @return
	 */
	private static Dimension getDimensionFromSpec(WebComponentSpecification webComponentDescription)
	{
		Dimension dimension = new Dimension(100, 100);
		if (webComponentDescription.getProperty("size") != null)
		{
			Object defaultValue = webComponentDescription.getProperty("size").getDefaultValue();
			if (defaultValue instanceof JSONObject)
			{
				try
				{
					Integer width = ((JSONObject)defaultValue).getInt("width");
					Integer height = ((JSONObject)defaultValue).getInt("height");
					dimension = new Dimension(width, height);
				}
				catch (JSONException e)
				{
					Debug.log(e);
				}
			}
		}
		return dimension;
	}

	private PaletteEntry createTemplateToolEntry(String templateName, int element)
	{
		Template template = (Template)ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObject(templateName, IRepository.TEMPLATES);
		if (template == null)
		{
			return null;
		}

		List<JSONObject> templateElements = ElementFactory.getTemplateElements(template, element);
		if (templateElements != null && templateElements.size() > 0)
		{
			return createTemplateToolEntry(template, templateElements.get(0), null, element);
		}

		return null;
	}

	/**
	 * @param json
	 * @return
	 */
	public PaletteEntry createTemplateToolEntry(Template template, JSONObject json, String displayName, int element)
	{
		TemplateElementHolder data = new TemplateElementHolder(template, element);
		RequestTypeCreationFactory factory = new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_TEMPLATE,
			DesignerUtil.convertDimension(ElementFactory.getTemplateBoundsize(data)));
		factory.setData(data);
		ImageDescriptor icon = getTemplateIcon(json);
		if (icon == null)
		{
			// default icon
			icon = Activator.loadImageDescriptorFromBundle("template.gif");
		}
		return new ElementCreationToolEntry(Utils.stringInitCap(displayName), "Create/apply template " + displayName, factory, icon, icon);
	}

	static ImageDescriptor getTemplateIcon(TemplateElementHolder templateHolder)
	{
		// elements
		List<JSONObject> elements = ElementFactory.getTemplateElements(templateHolder.template, templateHolder.element);
		if (elements == null || elements.size() == 0)
		{
			return null;
		}

		if (elements.size() > 1)
		{
			return com.servoy.eclipse.designer.Activator.loadImageDescriptorFromBundle("group.gif");
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


	@Override
	protected void fillPalette(PaletteRoot palette)
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
				PaletteDrawer drawer = null;

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


						if (drawer == null)
						{
							drawer = new ElementPaletteDrawer("");
							drawer.setId(drawerId);
							drawer.setUserModificationPermission(drawerId.startsWith(TEMPLATE_ID_PREFIX) ? PaletteEntry.PERMISSION_FULL_MODIFICATION
								: PaletteEntry.PERMISSION_LIMITED_MODIFICATION);
							applyPaletteCustomization(paletteCustomization.entryProperties, drawerId, drawer, defaultPaletteCustomization.entryProperties);

							palette.add(drawer);
						}

						drawer.add(entry);
					}
				}
			}
		}
	}
}
