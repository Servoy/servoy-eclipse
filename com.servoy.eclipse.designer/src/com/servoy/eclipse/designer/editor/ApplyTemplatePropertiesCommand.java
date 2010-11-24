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

import java.util.Map;

import org.eclipse.gef.commands.Command;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.repository.SolutionDeserializer;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * Command to copy element properties from a template to an element.
 * 
 * @author rgansevles
 *
 */
public class ApplyTemplatePropertiesCommand extends Command
{
	private final Template template;
	private final IPersist persist;
	private Map<String, Object> undoPropertiesMap;

	public ApplyTemplatePropertiesCommand(Template template, IPersist persist)
	{
		this.template = template;
		this.persist = persist;
	}

	@Override
	public boolean canExecute()
	{
		if (template != null)
		{
			try
			{
				JSONObject json = new ServoyJSONObject(template.getContent(), false);
				// elements
				JSONArray elements = (JSONArray)json.opt(Template.PROP_ELEMENTS);
				return elements != null && elements.length() > 0;
			}
			catch (JSONException e)
			{
				ServoyLog.logError("Error processing template " + template.getName(), e);
			}
		}
		return false;
	}

	@Override
	public void execute()
	{
		try
		{
			JSONObject json = new ServoyJSONObject(template.getContent(), false);

			// elements
			JSONArray elements = (JSONArray)json.opt(Template.PROP_ELEMENTS);
			if (elements == null || elements.length() == 0)
			{
				return;
			}

			JSONObject object = elements.getJSONObject(0);

			IDeveloperRepository repository = (IDeveloperRepository)persist.getRootObject().getRepository();
			Map<String, Object> propertyValues = SolutionDeserializer.getPropertyValuesForJsonObject(repository, persist,
				ElementFactory.resolveCleanedProperties((Form)persist.getAncestor(IRepository.FORMS), object));
			propertyValues.remove(Template.PROP_LOCATION);
			if (persist instanceof AbstractBase)
			{
				undoPropertiesMap = ((AbstractBase)persist).getPropertiesMap();
			}

			repository.updatePersistWithValueMap(persist, propertyValues);
		}
		catch (JSONException e)
		{
			ServoyLog.logError("Error processing template " + template.getName(), e);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Error processing template " + template.getName(), e);
		}

		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, persist, false);
	}

	@Override
	public boolean canUndo()
	{
		return undoPropertiesMap != null;
	}

	@Override
	public void undo()
	{
		((AbstractBase)persist).copyPropertiesMap(null); // clears propertyMap
		((AbstractBase)persist).copyPropertiesMap(undoPropertiesMap);
		undoPropertiesMap = null;
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, persist, false);
	}
}
