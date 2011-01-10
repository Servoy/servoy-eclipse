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

import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Template;

/**
 * Command to copy element properties from a template to an element.
 * 
 * @author rgansevles
 *
 */
public class ApplyTemplatePropertiesCommand extends BaseRestorableCommand
{
	private final TemplateElementHolder templateHolder;
	private final IPersist persist;

	public ApplyTemplatePropertiesCommand(TemplateElementHolder template, IPersist persist)
	{
		super("Apply template");
		this.templateHolder = template;
		this.persist = persist;
	}

	@Override
	public boolean canExecute()
	{
		// elements
		List<JSONObject> elements = ElementFactory.getTemplateElements(templateHolder);
		return elements != null && elements.size() > 0;
	}

	@Override
	public void execute()
	{
		saveState(persist);

		// elements
		List<JSONObject> elements = ElementFactory.getTemplateElements(templateHolder);
		if (elements == null || elements.size() == 0)
		{
			return;
		}

		try
		{
			JSONObject json = elements.get(0);

			IDeveloperRepository repository = (IDeveloperRepository)persist.getRootObject().getRepository();
			Map<String, Object> propertyValues = SolutionDeserializer.getPropertyValuesForJsonObject(repository, persist,
				ElementFactory.resolveCleanedProperties((Form)persist.getAncestor(IRepository.FORMS), json));
			propertyValues.remove(Template.PROP_LOCATION);

			repository.updatePersistWithValueMap(persist, propertyValues, false);
		}
		catch (JSONException e)
		{
			ServoyLog.logError("Error processing template " + templateHolder.template.getName(), e);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Error processing template " + templateHolder.template.getName(), e);
		}

		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, persist, false);
	}
}
