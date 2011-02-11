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

import java.util.List;

import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.ui.palette.customize.PaletteContainerFactory;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONObject;

import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.model.repository.SolutionSerializer;

/**
 * Factory for creating drawers from templates with named elements in the PaletteCustomizer.
 * 
 * @author rgansevles
 *
 */
public class PaletteTemplateElementsFactory extends PaletteContainerFactory
{

	/**
	 * Constructor
	 */
	public PaletteTemplateElementsFactory()
	{
		setLabel("Form template drawer");
	}

	/*
	 * can create if it has at least 1 named entry.
	 */
	@Override
	public boolean canCreate(PaletteEntry selected)
	{
		TemplateElementHolder templateHolder = null;
		if (selected instanceof ElementCreationToolEntry && ((ElementCreationToolEntry)selected).getTemplate() instanceof RequestTypeCreationFactory &&
			((RequestTypeCreationFactory)((ElementCreationToolEntry)selected).getTemplate()).getData() instanceof TemplateElementHolder)
		{
			templateHolder = (TemplateElementHolder)((RequestTypeCreationFactory)((ElementCreationToolEntry)selected).getTemplate()).getData();
		}
		if (templateHolder == null)
		{
			return false;
		}

		List<JSONObject> templateElements = ElementFactory.getTemplateElements(templateHolder.template, templateHolder.element);
		return templateElements != null && templateElements.size() > 0; // has some objects available
	}

	@Override
	public PaletteEntry createNewEntry(Shell shell, PaletteEntry selected)
	{
		TemplateElementHolder templateHolder = null;
		if (selected instanceof ElementCreationToolEntry && ((ElementCreationToolEntry)selected).getTemplate() instanceof RequestTypeCreationFactory &&
			((RequestTypeCreationFactory)((ElementCreationToolEntry)selected).getTemplate()).getData() instanceof TemplateElementHolder)
		{
			templateHolder = (TemplateElementHolder)((RequestTypeCreationFactory)((ElementCreationToolEntry)selected).getTemplate()).getData();
		}
		if (templateHolder == null)
		{
			return null;
		}
		List<JSONObject> templateElements = ElementFactory.getTemplateElements(templateHolder.template, templateHolder.element);
		if (templateElements == null)
		{
			return null;
		}

		PaletteContainer parent = determineContainerForNewEntry(selected);
		int index = determineIndexForNewEntry(parent, selected);
		PaletteDrawer drawer = new PaletteDrawer(selected.getLabel());
		drawer.setId(VisualFormEditorPaletteFactory.TEMPLATE_ID_PREFIX + templateHolder.template.getName());
		drawer.setUserModificationPermission(PaletteEntry.PERMISSION_FULL_MODIFICATION);

		for (int i = 0; i < templateElements.size(); i++)
		{
			JSONObject jsonObject = templateElements.get(i);
			String name = jsonObject.optString(SolutionSerializer.PROP_NAME);
			String displayName = (name == null || name.length() == 0) ? "item " + (i + 1) : name;
			PaletteEntry toolEntry = VisualFormEditorPaletteFactory.createTemplateToolEntry(templateHolder.template, jsonObject, displayName, i);
			if (toolEntry != null)
			{
				toolEntry.setId(Integer.toString(i));
				drawer.add(toolEntry);
			}
		}

		parent.add(index, drawer);
		return drawer;
	}

	@Override
	protected PaletteEntry createNewEntry(Shell shell)
	{
		return null; // never called
	}

	/**
	 * @see org.eclipse.gef.ui.palette.customize.PaletteEntryFactory#determineTypeForNewEntry(org.eclipse.gef.palette.PaletteEntry)
	 */
	@Override
	protected Object determineTypeForNewEntry(PaletteEntry selected)
	{
		return PaletteDrawer.PALETTE_TYPE_DRAWER;
	}


}
