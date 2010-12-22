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
package com.servoy.eclipse.ui.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Style;

/**
 * IComboboxPropertyModel model for style classes.
 * <p>
 * Style classes are not cached, when a form style changes the new classes should be updated automatically.
 * 
 * @author rgansevles
 * 
 */
public class StyleClassesComboboxModel implements IComboboxPropertyModel<String>
{
	private final String lookupName;
	private final Form form;

	public StyleClassesComboboxModel(Form form, String lookupName)
	{
		this.form = form;
		this.lookupName = lookupName;
	}

	public String[] getDisplayValues()
	{
		String[] displayValues = getRealValues().clone();
		for (int i = 0; i < displayValues.length; i++)
		{
			if (displayValues[i] == null)
			{
				displayValues[i] = Messages.LabelDefault;
			}
		}
		return displayValues;
	}

	public String[] getRealValues()
	{
		return getStyleClasses();
	}

	/**
	 * Get the style classes usable for the persist.
	 * 
	 * @param persist
	 * @return
	 */
	protected String[] getStyleClasses()
	{
		List<String> styleClasses = new ArrayList<String>();

		FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(form);
		Style style = flattenedSolution.getStyleForForm(form, null);
		String[] classes = ModelUtils.getStyleClasses(style, lookupName);
		if (classes != null && classes.length > 0)
		{
			styleClasses.addAll(Arrays.asList(classes));
		}
		styleClasses.add(0, null);
		return styleClasses.toArray(new String[styleClasses.size()]);
	}

	public static String getStyleLookupname(IPersist persist)
	{
		if (persist instanceof BaseComponent)
		{
			return ComponentFactory.getLookupName((BaseComponent)persist);
		}
		if (persist instanceof Form)
		{
			return "form"; //$NON-NLS-1$
		}
		return null;
	}
}
