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

package com.servoy.eclipse.ui.quickfix;

import java.beans.PropertyDescriptor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewMethodAction;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.MethodTemplate;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.util.UUID;

/**
 * creates a missing method for an event/command
 * 
 * @author lvostinar
 *
 */
public class CreateMethodReferenceQuickFix implements IMarkerResolution
{
	private final String uuid;
	private final String solutionName;
	private final String eventName;
	private final boolean globalMethod;

	public CreateMethodReferenceQuickFix(String uuid, String solName, String eventName, boolean globalMethod)
	{
		this.uuid = uuid;
		this.solutionName = solName;
		this.eventName = eventName;
		this.globalMethod = globalMethod;
	}

	public String getLabel()
	{
		if (globalMethod)
		{
			return "Create default global method for missing " + eventName; //$NON-NLS-1$
		}
		else
		{
			return "Create default form method for missing " + eventName; //$NON-NLS-1$
		}
	}

	public void run(IMarker marker)
	{
		if (uuid != null)
		{
			UUID id = UUID.fromString(uuid);
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (servoyProject != null)
			{
				try
				{
					IPersist persist = servoyProject.getEditingPersist(id);
					IPersist parent = null;
					if (persist != null)
					{
						if (globalMethod)
						{
							parent = persist.getAncestor(IRepository.SOLUTIONS);
						}
						else
						{
							parent = persist.getAncestor(IRepository.FORMS);
						}
					}
					if (parent != null)
					{
						MethodTemplate template = MethodTemplate.getTemplate(ScriptMethod.class, eventName);
						if (template != null)
						{
							MethodArgument signature = template.getSignature();
							if (signature != null)
							{
								String name = signature.getName();
								ScriptMethod method = NewMethodAction.createNewMethod(UIUtils.getActiveShell(), parent, eventName, true, name);
								if (method != null)
								{
									PropertyDescriptor descriptor = new PropertyDescriptor(eventName, persist.getClass());
									descriptor.getWriteMethod().invoke(persist, method.getID());
									servoyProject.saveEditingSolutionNodes(new IPersist[] { parent }, true);
								}
							}
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}

	}
}
