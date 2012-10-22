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

package com.servoy.eclipse.designer.editor.mobile.commands;

import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.designer.editor.commands.ISupportModels;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Command to add title to header in mobile form editor.
 *
 * @author rgansevles
 *
 */
public class MobileAddHeaderTitleCommand extends Command implements ISupportModels
{
	private final Form form;

	protected Object[] models;

	public MobileAddHeaderTitleCommand(Form form)
	{
		this.form = form;
	}

	public Object[] getModels()
	{
		return models;
	}

	@Override
	public void execute()
	{
		models = null;
		try
		{
			setLabel("place header text");
			GraphicalComponent label = ElementFactory.createLabel(form, "Title", null);
			label.putCustomMobileProperty("headeritem", Boolean.TRUE);
			label.putCustomMobileProperty("headerText", Boolean.TRUE);

			models = new IPersist[] { label };
			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, label, true);
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError(ex);
		}
	}
}
