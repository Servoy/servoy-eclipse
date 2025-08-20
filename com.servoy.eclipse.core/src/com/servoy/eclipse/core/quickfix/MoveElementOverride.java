/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.core.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Debug;

/**
 * @author lvostinar
 *
 */
public class MoveElementOverride implements IMarkerResolution
{
	private final ServoyProject project;
	private final IPersist persist;
	private final Form form;

	public MoveElementOverride(IPersist persist, ServoyProject servoyProject)
	{
		this.project = servoyProject;
		this.persist = persist;
		this.form = (Form)persist.getAncestor(IRepository.FORMS);
	}

	@Override
	public String getLabel()
	{
		return "Move overriden element: '" + ((IFormElement)persist).getName() + "' to correct parent.";
	}

	@Override
	public void run(IMarker marker)
	{
		try
		{
			if (!(persist.getParent() instanceof Form) && ((IFormElement)persist).getExtendsID() != null)
			{
				persist.getParent().removeChild(persist);
				form.addChild(persist);
				project.saveEditingSolutionNodes(new IPersist[] { form }, true);
			}
		}
		catch (RepositoryException e)
		{
			Debug.error("Could not move override for " + ((IFormElement)persist).getName(), e);
		}
	}
}
