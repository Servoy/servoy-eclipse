/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.designer.editor.mobile.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.CompoundCommand;
import org.sablo.specification.ValuesConfig;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.eclipse.designer.editor.commands.FormElementDeleteCommand;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.PersistPropertyHandler;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Command to modify the current form as a list form.
 * 
 * @author rgansevles
 *
 */
public class ConvertToRecordFormCommand extends CompoundCommand
{
	public ConvertToRecordFormCommand(Form form)
	{
		if (form.getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED)
		{
			add(SetValueCommand.createSetvalueCommand(
				"",
				PersistPropertySource.createPersistPropertySource(form, false),
				StaticContentSpecLoader.PROPERTY_VIEW.getPropertyName(),
				Integer.valueOf(((ValuesConfig)PersistPropertyHandler.VIEW_TYPE_VALUES.getConfig()).getRealIndexOf(Integer.valueOf(IFormConstants.VIEW_TYPE_RECORD)))));

			// delete all form list elements
			MobileListModel model = MobileListModel.create(form, form);
			List<IPersist> toDelete = new ArrayList<IPersist>();
			if (model.button != null) toDelete.add(model.button);
			if (model.countBubble != null) toDelete.add(model.countBubble);
			if (model.image != null) toDelete.add(model.image);
			if (model.subtext != null) toDelete.add(model.subtext);
			if (toDelete.size() > 0)
			{
				add(new FormElementDeleteCommand(toDelete.toArray(new IPersist[toDelete.size()])));
			}
		}
	}
}