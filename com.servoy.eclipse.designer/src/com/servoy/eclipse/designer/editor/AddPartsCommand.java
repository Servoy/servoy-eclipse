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

package com.servoy.eclipse.designer.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.commands.AbstractModelsCommand;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Command to add parts to a form
 * 
 * @author rgansevles
 *
 */
public class AddPartsCommand extends AbstractModelsCommand
{
	private final Form form;
	private final int[] partTypeIds;
	List<Part> createdParts = null;

	public AddPartsCommand(Form form, int[] partTypeIds)
	{
		this.form = form;
		this.partTypeIds = partTypeIds;
		setLabel("Add part(s)");
	}

	protected Part createPart(int partTypeId) throws RepositoryException
	{
		return form.createNewPart(partTypeId, Integer.MAX_VALUE); // is used in sorting in form.getParts()
	}

	@Override
	public Object[] getModels()
	{
		return createdParts == null ? null : createdParts.toArray();
	}

	@Override
	public void execute()
	{
		try
		{
			List<Part> list = new ArrayList<Part>();
			for (int partTypeId : partTypeIds)
			{
				Part part = createPart(partTypeId);
				list.add(part);
				int prevHeight = -1;
				int nextHeight = 0;

				// flattened form for this form does not contain new part yet
				Iterator<Part> it = ModelUtils.getEditingFlattenedSolution(form).getFlattenedForm(form, false).getParts();
				while (it.hasNext())
				{
					Part element = it.next();
					if (element == part)
					{
						if (it.hasNext())
						{
							nextHeight = it.next().getHeight();
							if (prevHeight == -1)
							{
								// first part
								prevHeight = 0;
							}
						}
						else
						{
							if (prevHeight == -1)
							{
								// only part
								prevHeight = 90;// new height will become 100
							}
							//else: last part
							nextHeight = prevHeight + 20;
						}
						break;
					}
					prevHeight = element.getHeight();
				}
				part.setHeight((prevHeight + nextHeight) / 2);
				ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, part, false);
			}
			createdParts = list;
		}
		catch (Exception e)
		{
			ServoyLog.logError("Cannot create part", e);
		}
	}

	@Override
	public boolean canUndo()
	{
		return createdParts != null;
	}

	@Override
	public void undo()
	{
		List<Part> list = createdParts;
		createdParts = null;
		for (Part part : list)
		{
			try
			{
				((IDeveloperRepository)part.getRootObject().getRepository()).deleteObject(part);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not undo create part " + part, e);
			}
			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, part, false);
		}
	}
}