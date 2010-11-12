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

import java.awt.Rectangle;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.util.ServoyJSONObject;


/**
 * Utility class that can create a GEF Palette for the Visual Form Editor.
 * 
 * @author rgansevles
 * @since 6.0
 */
public class VisualFormEditorPaletteFactory
{

	private static PaletteContainer createElementsDrawer()
	{
		PaletteDrawer componentsDrawer = new PaletteDrawer("Elements");
		ToolEntry component;

		component = new ElementCreationToolEntry("Button", "Create a button", new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_BUTTON),
			Activator.loadImageDescriptorFromBundle("button.gif"), Activator.loadImageDescriptorFromBundle("button.gif"));
		componentsDrawer.add(component);

		component = new ElementCreationToolEntry("Label", "Create a label", new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_LABEL),
			Activator.loadImageDescriptorFromBundle("text.gif"), Activator.loadImageDescriptorFromBundle("text.gif"));
		componentsDrawer.add(component);

		// TODO: Add more

		return componentsDrawer;
	}

	private static PaletteContainer createTemplatesDrawer()
	{
		PaletteDrawer componentsDrawer = new PaletteDrawer("Templates");
		ToolEntry component;

		// TODO: listen for new templates

		List<IRootObject> templates = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.TEMPLATES);
		Collections.sort(templates, NameComparator.INSTANCE);

		Iterator<IRootObject> templatesIterator = templates.iterator();
		while (templatesIterator.hasNext())
		{
			Template template = (Template)templatesIterator.next();
			RequestTypeCreationFactory factory = new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_TEMPLATE);
			factory.setData(template);
			ImageDescriptor icon = getTemplateIcon(template);
			if (icon == null)
			{
				// default icon
				icon = Activator.loadImageDescriptorFromBundle("template.gif");
			}
			component = new ElementCreationToolEntry(template.getName(), "Create/apply template " + template.getName(), factory, icon, icon);
			componentsDrawer.add(component);
		}

		return componentsDrawer;
	}

	static ImageDescriptor getTemplateIcon(Template template)
	{
		Rectangle box = null;
		try
		{
			JSONObject json = new ServoyJSONObject(template.getContent(), false);

			// elements
			JSONArray elements = (JSONArray)json.opt(Template.PROP_ELEMENTS);
			if (elements == null || elements.length() == 0)
			{
				return null;
			}

			if (elements.length() > 1)
			{
				return Activator.loadImageDescriptorFromBundle("group.gif");
			}

			JSONObject object = elements.getJSONObject(0);
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

		}
		catch (JSONException e)
		{
			ServoyLog.logError("Error reading template " + template.getName(), e);
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
//		palette.add(createToolsGroup(palette));
		palette.add(createElementsDrawer());
		palette.add(createTemplatesDrawer());
		return palette;
	}

}
