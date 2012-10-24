/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.designer.editor.mobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.jface.resource.ImageDescriptor;

import com.servoy.eclipse.designer.Activator;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor.RequestType;
import com.servoy.eclipse.designer.editor.mobile.palette.MobileElementCreationToolEntry;
import com.servoy.eclipse.designer.editor.palette.BaseVisualFormEditorPaletteFactory;
import com.servoy.eclipse.designer.editor.palette.ElementCreationToolEntry;
import com.servoy.eclipse.designer.editor.palette.ElementPaletteDrawer;
import com.servoy.eclipse.designer.editor.palette.RequestTypeCreationFactory;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.preferences.DesignerPreferences.PaletteCustomization;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Palette items for mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileVisualFormEditorPaletteFactory extends BaseVisualFormEditorPaletteFactory
{
	private static final String BUTTONS_ID = "buttons";
	private static final String BUTTON_BUTTON_ID = "button";
	private static final String BUTTONS_HEADER_TITLE_ID = "headertitle";

	private static final String[] BUTTONS_IDS = new String[] {
	/* */BUTTON_BUTTON_ID
	/* */, BUTTONS_HEADER_TITLE_ID
	/* */};

	private static final String PARTS_ID = "toolbars";
	private static final String PARTS_HEADER_ID = "header";
	private static final String PARTS_FOOTER_ID = "footer";

	private static final String[] PARTS_IDS = new String[] {
	/* */PARTS_HEADER_ID
	/* */, PARTS_FOOTER_ID
	/* */};

	private static final String ELEMENTS_ID = "elements";
	private static final String ELEMENTS_TEXT_FIELD_ID = "text field";
	private static final String ELEMENTS_TEXTAREA_ID = "text area";
	private static final String ELEMENTS_COMBOBOX_ID = "combobox";
	private static final String ELEMENTS_CHECKBOXES_ID = "checkboxes";
	private static final String ELEMENTS_RADIOBUTTONS_ID = "radio buttons";
	private static final String ELEMENTS_LABEL_ID = "label";

	private static final String[] ELEMENTS_IDS = new String[] {
	/* */ELEMENTS_TEXT_FIELD_ID
	/* */, ELEMENTS_TEXTAREA_ID
	/* */, ELEMENTS_COMBOBOX_ID
	/* */, ELEMENTS_CHECKBOXES_ID
	/* */, ELEMENTS_RADIOBUTTONS_ID
	/* */, ELEMENTS_LABEL_ID
	/* */};

	private static final String LISTS_ID = "lists";
	private static final String LISTS_FORM_ID = "list form";
	private static final String LISTS_INSET_ID = "inset list";
	private static final String[] LISTS_IDS = new String[] {
	/* */LISTS_FORM_ID
	/* */, LISTS_INSET_ID
	/* */};

	@Override
	protected void fillPalette(PaletteRoot palette)
	{
		PaletteCustomization defaultPaletteCustomization = getDefaultPaletteCustomization();
		PaletteCustomization savedPaletteCustomization = null; // RAGTEST new DesignerPreferences().getPaletteCustomization();
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
						entry.setUserModificationPermission(PaletteEntry.PERMISSION_LIMITED_MODIFICATION);
						applyPaletteCustomization(paletteCustomization.entryProperties, drawerId + '.' + itemId, entry,
							defaultPaletteCustomization.entryProperties);


						if (drawer == null)
						{
							drawer = new ElementPaletteDrawer("");
							drawer.setId(drawerId);
							drawer.setUserModificationPermission(PaletteEntry.PERMISSION_LIMITED_MODIFICATION);
							applyPaletteCustomization(paletteCustomization.entryProperties, drawerId, drawer, defaultPaletteCustomization.entryProperties);

							palette.add(drawer);
						}

						drawer.add(entry);
					}
				}
			}
		}
	}

	private PaletteEntry createPaletteEntry(String drawerId, String id)
	{
		if (BUTTONS_ID.equals(drawerId))
		{
			return createButtonsEntry(id);
		}

		if (PARTS_ID.equals(drawerId))
		{
			return createPartsEntry(id);
		}

		if (ELEMENTS_ID.equals(drawerId))
		{
			return createElementsEntry(id);
		}

		if (LISTS_ID.equals(drawerId))
		{
			return createListsEntry(id);
		}

		ServoyLog.logError("Unknown palette drawer: '" + drawerId + "'", null);
		return null;
	}

	private PaletteEntry createButtonsEntry(String id)
	{
		ImageDescriptor icon;
		RequestType requestType;
//		int displayType = -1;

		if (BUTTON_BUTTON_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("headerbutton.png");
			requestType = MobileVisualFormEditor.REQ_PLACE_BUTTON;
		}

		else if (BUTTONS_HEADER_TITLE_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("text.gif");
			requestType = MobileVisualFormEditor.REQ_PLACE_HEADER_TITLE;
		}

		else
		{
			ServoyLog.logError("Unknown palette elements entry: '" + id + "'", null);
			return null;
		}

		Map<String, Object> extendedData = new HashMap<String, Object>();
//		if (displayType != -1)
//		{
//			setProperty(
//				extendedData,
//				StaticContentSpecLoader.PROPERTY_DISPLAYTYPE,
//				PersistPropertySource.DISPLAY_TYPE_CONTOLLER.getConverter().convertProperty(StaticContentSpecLoader.PROPERTY_DISPLAYTYPE.getPropertyName(),
//					Integer.valueOf(displayType)));
//		}
		RequestTypeCreationFactory factory = new RequestTypeCreationFactory(requestType);
		factory.setExtendedData(extendedData);
		return new MobileElementCreationToolEntry("", "", factory, icon, icon);
	}

	private PaletteEntry createPartsEntry(String id)
	{
		RequestType requestType;
		ImageDescriptor icon;

		if (PARTS_HEADER_ID.equals(id))
		{
			requestType = MobileVisualFormEditor.REQ_PLACE_HEADER;
			icon = Activator.loadImageDescriptorFromBundle("button.gif"); // RAGTEST header.gif
		}

		else if (PARTS_FOOTER_ID.equals(id))
		{
			requestType = MobileVisualFormEditor.REQ_PLACE_FOOTER;
			icon = Activator.loadImageDescriptorFromBundle("text.gif"); // RAGTEST footer.gif
		}

		else
		{
			ServoyLog.logError("Unknown palette elements entry: '" + id + "'", null);
			return null;
		}

		return new MobileElementCreationToolEntry("", "", new RequestTypeCreationFactory(requestType), icon, icon);
	}

	private PaletteEntry createElementsEntry(String id)
	{
		ImageDescriptor icon;
		RequestType requestType = MobileVisualFormEditor.REQ_PLACE_FIELD;
		int displayType = -1;

		if (ELEMENTS_TEXT_FIELD_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("textinput.png"); // RAGTEST icon
		}

		else if (ELEMENTS_TEXTAREA_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("TEXTAREA16.png"); // RAGTEST icon
			displayType = Field.TEXT_AREA;
		}

		else if (ELEMENTS_COMBOBOX_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("SELECT16.png");// RAGTEST icon
			displayType = Field.COMBOBOX;
		}

		else if (ELEMENTS_CHECKBOXES_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("text.gif");// RAGTEST icon
			displayType = Field.CHECKS;
		}

		else if (ELEMENTS_RADIOBUTTONS_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("text.gif");// RAGTEST icon
			displayType = Field.RADIOS;
		}

		else if (ELEMENTS_LABEL_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("text.gif");// RAGTEST icon
			requestType = MobileVisualFormEditor.REQ_PLACE_LABEL;
		}

		else
		{
			ServoyLog.logError("Unknown palette elements entry: '" + id + "'", null);
			return null;
		}

		Map<String, Object> extendedData = new HashMap<String, Object>();
		if (displayType != -1)
		{
			setProperty(
				extendedData,
				StaticContentSpecLoader.PROPERTY_DISPLAYTYPE,
				PersistPropertySource.DISPLAY_TYPE_CONTOLLER.getConverter().convertProperty(StaticContentSpecLoader.PROPERTY_DISPLAYTYPE.getPropertyName(),
					Integer.valueOf(displayType)));
		}

		RequestTypeCreationFactory factory = new RequestTypeCreationFactory(requestType);
		factory.setExtendedData(extendedData);
		return new ElementCreationToolEntry("", "", factory, icon, icon);
	}

	private PaletteEntry createListsEntry(String id)
	{
		ImageDescriptor icon;
		RequestType requestType;

		if (LISTS_INSET_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("textinput.png"); // RAGTEST icon
			requestType = MobileVisualFormEditor.REQ_PLACE_INSET_LIST;
		}

		else if (LISTS_FORM_ID.equals(id))
		{
			icon = Activator.loadImageDescriptorFromBundle("text.gif");// RAGTEST icon
			requestType = MobileVisualFormEditor.REQ_PLACE_FORM_LIST;
		}

		else
		{
			ServoyLog.logError("Unknown palette lists entry: '" + id + "'", null);
			return null;
		}

		return new ElementCreationToolEntry("", "", new RequestTypeCreationFactory(requestType), icon, icon);
	}

	@Override
	protected PaletteCustomization getDefaultPaletteCustomization()
	{
		List<String> drawers = new ArrayList<String>();
		Map<String, List<String>> drawerEntries = new HashMap<String, List<String>>();
		Map<String, Object> entryProperties = new HashMap<String, Object>();

		// add buttons
		addFixedEntries(BUTTONS_ID, Messages.LabelButtonsPalette, BUTTONS_IDS, drawers, drawerEntries, entryProperties);

		// add elements
		addFixedEntries(ELEMENTS_ID, Messages.LabelElementsPalette, ELEMENTS_IDS, drawers, drawerEntries, entryProperties);

		// add lists
		addFixedEntries(LISTS_ID, Messages.LabelListsPalette, LISTS_IDS, drawers, drawerEntries, entryProperties);

		// add parts
		addFixedEntries(PARTS_ID, Messages.LabelPartsPalette, PARTS_IDS, drawers, drawerEntries, entryProperties);

		return new PaletteCustomization(drawers, drawerEntries, entryProperties);
	}
}
