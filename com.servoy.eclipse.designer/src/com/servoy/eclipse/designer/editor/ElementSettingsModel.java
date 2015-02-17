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
package com.servoy.eclipse.designer.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.SecurityInfo;
import com.servoy.j2db.util.UUID;

/**
 * Model for setting security checkboxes in form editor security page.
 *
 * @author lvostinar
 */

public class ElementSettingsModel
{
	private String currentGroup;
	private final Map<String, Map<UUID, Integer>> securityInfo;
	private final Form form;

	public ElementSettingsModel(Form form)
	{
		this.form = form;
		securityInfo = new HashMap<String, Map<UUID, Integer>>();
	}

	public void setCurrentGroup(String group)
	{
		this.currentGroup = group;
	}

	public boolean hasRight(IPersist element, int mask)
	{
		int access = getAccess(element);
		return (access & mask) != 0;
	}

	private int getAccess(IPersist element)
	{
		UUID uuid = element.getUUID();
		int access = IRepository.IMPLICIT_FORM_ACCESS;// default value
		Map<UUID, Integer> currentGroupSecurityInfo = securityInfo.get(currentGroup);
		if (currentGroupSecurityInfo != null && currentGroupSecurityInfo.containsKey(uuid))
		{
			access = currentGroupSecurityInfo.get(uuid).intValue();
		}
		else
		{
			Form parent = (Form)element.getAncestor(IRepository.FORMS);
			List<SecurityInfo> infos = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getSecurityInfos(currentGroup, parent);
			if (infos != null)
			{
				Iterator<SecurityInfo> iterator = infos.iterator();
				while (iterator.hasNext())
				{
					SecurityInfo info = iterator.next();
					if (info.element_uid.equals(uuid.toString()))
					{
						access = info.access;
						break;
					}

				}
			}
		}
		return access;
	}

	public void setRight(boolean hasRight, IPersist element, int mask)
	{
		int access = getAccess(element);
		if (hasRight) access = access | mask;
		else access = access & (~mask);
		UUID uuid = element.getUUID();
		try
		{
			element = ElementUtil.getOverridePersist(PersistContext.create(element, form));
			if (!uuid.equals(element.getUUID()))
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, element.getAncestor(IRepository.FORMS), false);
			}
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError(ex);
		}
		Map<UUID, Integer> currentGroupSecurityInfo = securityInfo.get(currentGroup);
		if (currentGroupSecurityInfo == null)
		{
			currentGroupSecurityInfo = new HashMap<UUID, Integer>();
			securityInfo.put(currentGroup, currentGroupSecurityInfo);
		}
		currentGroupSecurityInfo.remove(uuid);
		currentGroupSecurityInfo.put(element.getUUID(), new Integer(access));
	}

	public void saveSecurityElements()
	{
		try
		{
			ArrayList<IPersist> formElements = new ArrayList<IPersist>();
			Iterator<IPersist> it = form.getAllObjects();
			while (it.hasNext())
			{
				IPersist elem = it.next();
				if (elem instanceof IFormElement && ((IFormElement)elem).getName() != null && ((IFormElement)elem).getName().length() != 0)
				{
					formElements.add(elem);
				}
			}
			String solutionName = form.getSolution().getName();
			formElements.add(0, form);
			Iterator<String> iterator = securityInfo.keySet().iterator();
			while (iterator.hasNext())
			{
				String group = iterator.next();
				Map<UUID, Integer> currentGroupSecurityInfo = securityInfo.get(group);
				if (currentGroupSecurityInfo != null)
				{
					Iterator<UUID> uuidIterator = currentGroupSecurityInfo.keySet().iterator();
					while (uuidIterator.hasNext())
					{
						UUID uuid = uuidIterator.next();
						if (elementIsInList(formElements, uuid))
						{
							ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().setFormSecurityAccess(
								ApplicationServerRegistry.get().getClientId(), group, currentGroupSecurityInfo.get(uuid), uuid, solutionName);
						}

					}
				}
			}
			securityInfo.clear();
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}

	private boolean elementIsInList(ArrayList<IPersist> formElements, UUID uuid)
	{
		Iterator<IPersist> iterator = formElements.iterator();
		while (iterator.hasNext())
		{
			if (iterator.next().getUUID().equals(uuid)) return true;
		}
		return false;
	}

	public Form getForm()
	{
		return form;
	}
}
