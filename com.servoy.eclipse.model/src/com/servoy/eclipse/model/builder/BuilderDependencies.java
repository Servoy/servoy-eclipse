/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.model.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ValueList;

/**
 * @author lvostinar
 *
 */
public class BuilderDependencies
{
	// the cache contains real persists, not editing ones
	private final static BuilderDependencies me = new BuilderDependencies();
	private Map<Media, List<Form>> mediaToForms;
	private Map<ValueList, List<Form>> valuelistToForms;
	private Map<Relation, List<IPersist>> relationToPersists;
	// formDependencies is a helper cache for fast lookup, both persists of the dependency are needed as key
	private Map<Form, List<IPersist>> formDependencies;
	private Map<Form, List<Form>> formToFormsDependencies;
	private Map<String, List<IPersist>> scopeToPersists;
	private Map<String, List<IPersist>> datasourceToPersists;
	private List<Form> variantlistToForms;

	private BuilderDependencies()
	{
	}

	public static BuilderDependencies getInstance()
	{
		return me;
	}

	// same as settings, load/save from disk
	public void save()
	{
		//TODO
	}

	public void load()
	{
		//TODO
	}

	public void clear()
	{
		mediaToForms = null;
		formDependencies = null;
		valuelistToForms = null;
		relationToPersists = null;
		scopeToPersists = null;
		formToFormsDependencies = null;
		datasourceToPersists = null;
		variantlistToForms = null;
	}

	public boolean isInitialized()
	{
		return mediaToForms != null;
	}

	public void initialize()
	{
		mediaToForms = new HashMap<Media, List<Form>>();
	}

	public void removeForm(Form form)
	{
		if (formDependencies != null)
		{
			List<IPersist> dependencies = formDependencies.remove(form);
			if (dependencies != null)
			{
				for (IPersist persist : dependencies)
				{
					if (persist instanceof Media)
					{
						List<Form> dependencyForms = mediaToForms.get(persist);
						if (dependencyForms != null) dependencyForms.remove(form);
					}
					else if (persist instanceof ValueList)
					{
						List<Form> dependencyForms = valuelistToForms.get(persist);
						if (dependencyForms != null) dependencyForms.remove(form);
					}
					else if (persist instanceof Relation)
					{
						List<IPersist> dependencyPersists = relationToPersists.get(persist);
						if (dependencyPersists != null) dependencyPersists.remove(form);
					}
					else if (persist instanceof Form)
					{
						List<Form> dependencyForms = formToFormsDependencies.get(persist);
						if (dependencyForms != null) dependencyForms.remove(form);
					}
				}
			}
		}
		if (scopeToPersists != null)
		{
			Iterator<String> it = scopeToPersists.keySet().iterator();
			while (it.hasNext())
			{
				String scopeName = it.next();
				List<IPersist> persists = scopeToPersists.get(scopeName);
				if (persists != null && persists.contains(form))
				{
					persists.remove(form);
				}
				if (persists == null || persists.size() == 0)
				{
					it.remove();
				}
			}
		}

		if (datasourceToPersists != null)
		{
			Iterator<String> it = datasourceToPersists.keySet().iterator();
			while (it.hasNext())
			{
				String datasource = it.next();
				List<IPersist> persists = datasourceToPersists.get(datasource);
				if (persists != null && persists.contains(form))
				{
					persists.remove(form);
				}
				if (persists == null || persists.size() == 0)
				{
					it.remove();
				}
			}
		}
		if (variantlistToForms != null)
		{
			variantlistToForms.remove(form);
		}
	}

	public void addVariantDependency(Form form)
	{
		if (variantlistToForms == null)
		{
			variantlistToForms = new ArrayList<>();
		}
		if (!variantlistToForms.contains(form))
		{
			variantlistToForms.add(form);
		}
	}

	public void addDependency(Form form, ValueList valuelist)
	{
		if (valuelistToForms == null)
		{
			valuelistToForms = new HashMap<>();
		}
		List<Form> dependecyForms = valuelistToForms.get(valuelist);
		if (dependecyForms == null)
		{
			dependecyForms = new ArrayList<Form>();
			valuelistToForms.put(valuelist, dependecyForms);
		}
		if (!dependecyForms.contains(form)) dependecyForms.add(form);

		if (formDependencies == null)
		{
			formDependencies = new HashMap<>();
		}
		List<IPersist> dependecyPersists = formDependencies.get(form);
		if (dependecyPersists == null)
		{
			dependecyPersists = new ArrayList<IPersist>();
			formDependencies.put(form, dependecyPersists);
		}
		if (!dependecyPersists.contains(valuelist)) dependecyPersists.add(valuelist);
	}

	public void addDependency(IPersist persist, Relation relation)
	{
		if (relationToPersists == null)
		{
			relationToPersists = new HashMap<>();
		}
		List<IPersist> dependecyForms = relationToPersists.get(relation);
		if (dependecyForms == null)
		{
			dependecyForms = new ArrayList<IPersist>();
			relationToPersists.put(relation, dependecyForms);
		}
		if (!dependecyForms.contains(persist)) dependecyForms.add(persist);

		if (persist instanceof Form)
		{
			if (formDependencies == null)
			{
				formDependencies = new HashMap<>();
			}
			List<IPersist> dependecyPersists = formDependencies.get(persist);
			if (dependecyPersists == null)
			{
				dependecyPersists = new ArrayList<IPersist>();
				formDependencies.put((Form)persist, dependecyPersists);
			}
			if (!dependecyPersists.contains(relation)) dependecyPersists.add(relation);
		}
	}

	public void addDependency(Form form, Media media)
	{
		if (mediaToForms == null)
		{
			mediaToForms = new HashMap<>();
		}
		List<Form> dependecyForms = mediaToForms.get(media);
		if (dependecyForms == null)
		{
			dependecyForms = new ArrayList<Form>();
			mediaToForms.put(media, dependecyForms);
		}
		if (!dependecyForms.contains(form)) dependecyForms.add(form);

		if (formDependencies == null)
		{
			formDependencies = new HashMap<>();
		}
		List<IPersist> dependecyPersists = formDependencies.get(form);
		if (dependecyPersists == null)
		{
			dependecyPersists = new ArrayList<IPersist>();
			formDependencies.put(form, dependecyPersists);
		}
		if (!dependecyPersists.contains(media)) dependecyPersists.add(media);
	}

	public void addDependency(String scopeName, IPersist persist)
	{
		if (scopeName != null)
		{
			if (scopeToPersists == null)
			{
				scopeToPersists = new HashMap<String, List<IPersist>>();
			}
			List<IPersist> persists = scopeToPersists.get(scopeName);
			if (persists == null)
			{
				persists = new ArrayList<IPersist>();
				scopeToPersists.put(scopeName, persists);
			}
			if (!persists.contains(persist)) persists.add(persist);
		}
	}

	public void addDatasourceDependency(String datasource, IPersist persist)
	{
		if (datasource != null)
		{
			if (datasourceToPersists == null)
			{
				datasourceToPersists = new HashMap<String, List<IPersist>>();
			}
			List<IPersist> persists = datasourceToPersists.get(datasource);
			if (persists == null)
			{
				persists = new ArrayList<IPersist>();
				datasourceToPersists.put(datasource, persists);
			}
			if (!persists.contains(persist)) persists.add(persist);
		}
	}

	public void addDependency(Form source, Form destination)
	{
		if (formToFormsDependencies == null)
		{
			formToFormsDependencies = new HashMap<>();
		}
		List<Form> dependecyForms = formToFormsDependencies.get(destination);
		if (dependecyForms == null)
		{
			dependecyForms = new ArrayList<Form>();
			formToFormsDependencies.put(destination, dependecyForms);
		}
		if (!dependecyForms.contains(source)) dependecyForms.add(source);

		if (formDependencies == null)
		{
			formDependencies = new HashMap<>();
		}
		List<IPersist> dependecyPersists = formDependencies.get(source);
		if (dependecyPersists == null)
		{
			dependecyPersists = new ArrayList<IPersist>();
			formDependencies.put(source, dependecyPersists);
		}
		if (!dependecyPersists.contains(destination)) dependecyPersists.add(destination);
	}

	public void removeScopeDependencies(String scopeName)
	{
		if (scopeToPersists != null) scopeToPersists.remove(scopeName);
	}

	public void removeDatasourceDependencies(String datasource)
	{
		if (datasourceToPersists != null) datasourceToPersists.remove(datasource);
	}

	public List<Form> getFormDependencies(Form form)
	{
		if (formToFormsDependencies != null)
		{
			return formToFormsDependencies.get(form);
		}
		return null;
	}

	public List<IPersist> getScopeDependency(String scopeName)
	{
		if (scopeToPersists != null)
		{
			return scopeToPersists.get(scopeName);
		}
		return null;
	}

	public List<IPersist> getDatasourceDependency(String datasource)
	{
		if (datasourceToPersists != null)
		{
			return datasourceToPersists.get(datasource);
		}
		return null;
	}

	public List<Form> getMediaDependencies(Media media)
	{
		if (mediaToForms != null) return mediaToForms.get(media);
		return null;
	}

	public List<Form> getValuelistDependencies(ValueList valuelist)
	{
		if (valuelistToForms != null) return valuelistToForms.get(valuelist);
		return null;
	}

	public List<IPersist> getRelationDependencies(Relation relation)
	{
		if (relationToPersists != null) return relationToPersists.get(relation);
		return null;
	}

	public List<Form> getVariantDependencies()
	{
		return variantlistToForms;
	}
}
