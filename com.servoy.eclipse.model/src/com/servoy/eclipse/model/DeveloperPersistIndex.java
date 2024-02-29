/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.eclipse.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONObject;

import com.servoy.j2db.ISolutionModelPersistIndex;
import com.servoy.j2db.PersistIndex;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScope;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.persistence.WebObjectImpl;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 8.4
 *
 */
public class DeveloperPersistIndex extends PersistIndex implements ISolutionModelPersistIndex
{
	private final Map<String, Set<Form>> formCacheByDataSource = new HashMap<String, Set<Form>>();
	private final ConcurrentMap<String, List<Form>> formCacheByNamedFoundset = new ConcurrentHashMap<>();
	private final Map<UUID, List<IPersist>> duplicatesUUIDs = new HashMap<UUID, List<IPersist>>();
	private final Map<String, Map<String, List<IPersist>>> duplicateNames = new HashMap<String, Map<String, List<IPersist>>>();
	private final Map<Form, String> formToDataSource = new HashMap<>();
	private static final String ALL_FORMS = "";

	public DeveloperPersistIndex(List<Solution> solutions)
	{
		super(solutions);
		for (Solution solution : solutions)
		{
			if (solution.getChangeHandler() != null) solution.getChangeHandler().addIPersistListener(this);
		}
		createDatasources();
	}

	private void createDatasources()
	{
		final Set<Form> allforms = new TreeSet<Form>(NameComparator.INSTANCE);

		visit((persist) -> {
			if (persist instanceof Form)
			{
				Form f = (Form)persist;
				allforms.add(f);
				formToDataSource.put(f, f.getDataSource() != null ? f.getDataSource() : Form.DATASOURCE_NONE);

				String ds = f.getDataSource();
				if (ds == null) ds = Form.DATASOURCE_NONE;
				Set<Form> set = formCacheByDataSource.get(ds);
				if (set == null)
				{
					set = new TreeSet<>(NameComparator.INSTANCE);
					formCacheByDataSource.put(ds, set);
				}
				set.add(f);

				String namedFoundset = f.getNamedFoundSet();
				if (namedFoundset != null)
				{
					List<Form> list = formCacheByNamedFoundset.get(namedFoundset);
					if (list == null)
					{
						list = new ArrayList<Form>();
						formCacheByNamedFoundset.put(namedFoundset, list);
					}
					list.add(f);
				}
			}
			return IPersistVisitor.CONTINUE_TRAVERSAL;
		});

		formCacheByDataSource.put(ALL_FORMS, allforms);

	}

	@Override
	public void destroy()
	{
		super.destroy();
		formCacheByDataSource.clear();
		formCacheByNamedFoundset.clear();
		formToDataSource.clear();
		duplicatesUUIDs.clear();
		duplicateNames.clear();
	}

	@Override
	public void itemChanged(IPersist persist)
	{
		super.itemChanged(persist);
		if (persist instanceof Form)
		{
			updateDataSourceCache((Form)persist);
		}
	}

	private void updateDataSourceCache(Form form)
	{
		String oldDataSource = formToDataSource.get(form);
		String newDataSource = form.getDataSource() != null ? form.getDataSource() : Form.DATASOURCE_NONE;
		if (oldDataSource != null && !oldDataSource.equals(newDataSource) || oldDataSource == null && newDataSource != null)
		{
			getFormsByDatasource(oldDataSource, false).remove(form);
			getFormsByDatasource(newDataSource, false).add(form);
			formToDataSource.put(form, newDataSource);

			if (form.isFormComponent().booleanValue())
			{
				ServoyModelFinder.getServoyModel().fireFormComponentChanged();
			}
		}
		for (String namedFoundset : formCacheByNamedFoundset.keySet())
		{
			if (formCacheByNamedFoundset.get(namedFoundset).remove(form))
			{
				break;
			}
		}
		if (form.getNamedFoundSet() != null)
		{
			getFormsByNamedFoundsetImpl(form.getNamedFoundSet(), true).add(form);
		}
	}

	Set<Form> getFormsByDatasource(String datasource, boolean includeNone)
	{
		String ds = datasource == null ? ALL_FORMS : datasource;
		Set<Form> datasourceSet = formCacheByDataSource.get(ds);
		if (datasourceSet == null)
		{
			datasourceSet = fillSet(datasource);
			formCacheByDataSource.put(ds, datasourceSet);
		}
		if (includeNone)
		{
			Set<Form> datasourceNoneSet = formCacheByDataSource.get(Form.DATASOURCE_NONE);
			if (datasourceNoneSet == null)
			{
				datasourceNoneSet = fillSet(Form.DATASOURCE_NONE);
				formCacheByDataSource.put(Form.DATASOURCE_NONE, datasourceNoneSet);
			}
			Set<Form> result = new TreeSet<Form>(NameComparator.INSTANCE);
			result.addAll(datasourceSet);
			result.addAll(datasourceNoneSet);
			return result;
		}
		return datasourceSet;
	}

	public List<Form> getFormsByNamedFoundset(String namedFoundset)
	{
		List<Form> list = getFormsByNamedFoundsetImpl(namedFoundset, false);
		if (list == null) return Collections.emptyList();
		return Collections.unmodifiableList(list);
	}

	private List<Form> getFormsByNamedFoundsetImpl(String namedFoundset, boolean create)
	{
		if (namedFoundset != null)
		{
			List<Form> namedFoundsetSet = formCacheByNamedFoundset.get(namedFoundset);
			if (namedFoundsetSet == null && create)
			{
				namedFoundsetSet = new ArrayList<Form>(6);
				formCacheByNamedFoundset.put(namedFoundset, namedFoundsetSet);
			}
			return namedFoundsetSet;
		}
		return null;
	}

	/**
	 * @param datasource
	 * @return
	 */
	private Set<Form> fillSet(String datasource)
	{
		final Set<Form> result = new TreeSet<Form>(NameComparator.INSTANCE);
		visit((persist) -> {
			if (persist instanceof Form && Utils.equalObjects(datasource, ((Form)persist).getDataSource()))
			{
				Form f = (Form)persist;
				result.add(f);
				formToDataSource.put(f, f.getDataSource() != null ? f.getDataSource() : Form.DATASOURCE_NONE);
			}
			return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
		});
		return result;
	}

	@Override
	public void itemCreated(IPersist persist)
	{
		super.itemCreated(persist);
		if (persist instanceof Form)
		{
			Form form = (Form)persist;
			getFormsByDatasource(null, false).add(form);
			String ds = form.getDataSource() != null ? form.getDataSource() : Form.DATASOURCE_NONE;
			formToDataSource.put(form, ds);
			getFormsByDatasource(ds, false).add(form);
			if (form.getNamedFoundSet() != null)
			{
				getFormsByNamedFoundsetImpl(form.getNamedFoundSet(), true).add(form);
			}
		}
	}

	@Override
	public void itemRemoved(IPersist persist)
	{
		super.itemRemoved(persist);
		if (persist instanceof Form)
		{
			Form form = (Form)persist;
			getFormsByDatasource(null, false).remove(form);
			formToDataSource.remove(form);
			getFormsByDatasource(form.getDataSource() != null ? form.getDataSource() : Form.DATASOURCE_NONE, false).remove(form);
			if (form.getNamedFoundSet() != null)
			{
				List<Form> lst = getFormsByNamedFoundsetImpl(form.getNamedFoundSet(), false);
				if (lst != null) lst.remove(form);
			}
			if (form.isFormComponent().booleanValue())
			{
				ServoyModelFinder.getServoyModel().fireFormComponentChanged();
			}
		}
		if (duplicatesUUIDs.containsKey(persist.getUUID()))
		{
			List<IPersist> duplicates = duplicatesUUIDs.get(persist.getUUID());
			if (duplicates != null && duplicates.contains(persist))
			{
				duplicates.remove(persist);
			}
			if (duplicates == null || duplicates.size() <= 1)
			{
				duplicatesUUIDs.remove(persist.getUUID());
				if (duplicates.size() == 1 && !uuidToPersist.containsKey(persist.getUUID().toString()))
				{
					// fix the cache, put the other one in
					this.putInCache(duplicates.get(0));
				}
			}
		}
	}

	@Override
	protected void putInCache(IPersist persist)
	{
		if (persist instanceof AbstractBase && ((AbstractBase)persist).isAClone()) return;
		if (persist.getParent() instanceof AbstractBase && ((AbstractBase)persist.getParent()).isAClone()) return;
		if (uuidToPersist.containsKey(persist.getUUID().toString()))
		{
			IPersist existingPersist = uuidToPersist.get(persist.getUUID().toString());
			if (persist != existingPersist)
			{
				if (isDifferentPersist(existingPersist, persist))
				{
					List<IPersist> duplicates = duplicatesUUIDs.get(persist.getUUID());
					if (duplicates == null)
					{
						duplicates = new ArrayList<IPersist>();
						duplicatesUUIDs.put(persist.getUUID(), duplicates);
						duplicates.add(persist);
						duplicates.add(existingPersist);
					}
					else if (duplicates.stream().allMatch(duplicate -> this.isDifferentPersist(duplicate, persist)))
					{
						duplicates.add(persist);
					}
				}
			}
		}
		super.putInCache(persist);

		if (persist instanceof WebComponent)
		{
			List<IChildWebObject> children = ((WebComponent)persist).getImplementation().getAllPersistMappedProperties();
			children.forEach(child -> putInCache(child));
		}

	}

	private boolean isDifferentPersist(IPersist existingPersist, IPersist persist)
	{
		boolean isDifferent = false;
		if (persist != existingPersist)
		{
			// if this is a WebCustomType this can be created multiply times for the same thing
			// we should check if it is the same parent and the same index then assume this is the same thing
			isDifferent = !((persist instanceof WebCustomType && existingPersist instanceof WebCustomType) &&
				(existingPersist.getAncestor(IRepository.WEBCOMPONENTS).getUUID().equals(persist.getAncestor(IRepository.WEBCOMPONENTS).getUUID())) &&
				(((WebCustomType)existingPersist).getIndex() == ((WebCustomType)persist).getIndex()));
			if (isDifferent)
			{
				ISupportChilds parent = persist.getParent();
				while (isDifferent && parent instanceof WebComponent)
				{
					isDifferent = ((WebComponent)parent).getRuntimeProperty(FormElementHelper.FORM_COMPONENT_UUID) == null;
					parent = parent.getParent();
				}
				parent = existingPersist.getParent();
				while (isDifferent && parent instanceof WebComponent)
				{
					isDifferent = ((WebComponent)parent).getRuntimeProperty(FormElementHelper.FORM_COMPONENT_UUID) == null;
					parent = parent.getParent();
				}
			}
		}
		return isDifferent;
	}

	// below method are teh one of the ISoluitonModelPersistIndex. they are just empty in the developer fs.
	@Override
	public void setSolutionModelSolution(Solution solution)
	{
	}

	@Override
	public void addRemoved(IPersist persist)
	{
	}

	@Override
	public Set<IPersist> getRemoved()
	{
		return Collections.emptySet();
	}

	@Override
	public void removeRemoved(IPersist persist)
	{
	}

	@Override
	public boolean isRemoved(IPersist persist)
	{
		return false;
	}

	public Map<UUID, List<IPersist>> getDuplicateUUIDList()
	{
		// check if the uuids where not changed in the mean time..
		Iterator<Entry<UUID, List<IPersist>>> mapIterator = duplicatesUUIDs.entrySet().iterator();
		while (mapIterator.hasNext())
		{
			Entry<UUID, List<IPersist>> entry = mapIterator.next();
			Iterator<IPersist> listIterator = entry.getValue().iterator();
			while (listIterator.hasNext())
			{
				IPersist persist = listIterator.next();
				if (!persist.getUUID().equals(entry.getKey()))
				{
					listIterator.remove();
				}
				if (persist instanceof WebCustomType)
				{
					// many are generated, not sure if the one we have is up to date, check the model
					JSONObject fullJSONInFrmFile = WebObjectImpl.getFullJSONInFrmFile((WebCustomType)persist, false);
					if (fullJSONInFrmFile != null && fullJSONInFrmFile.has(IChildWebObject.UUID_KEY) &&
						!Utils.equalObjects(entry.getKey(), fullJSONInFrmFile.get(IChildWebObject.UUID_KEY)))
					{
						listIterator.remove();
					}
				}
			}
			if (entry.getValue().size() <= 1)
			{
				mapIterator.remove();
			}
		}
		return duplicatesUUIDs;
	}

	@Override
	protected void addInNameCache(ConcurrentMap<String, IPersist> cache, IPersist persist)
	{
		testDuplicateCache(cache, persist, persist.getClass().getName());
		super.addInNameCache(cache, persist);
	}

	/**
	 * @param cache
	 * @param persist
	 * @param cacheName
	 */
	protected void testDuplicateCache(Map<String, ? extends IPersist> cache, IPersist persist, String cacheName)
	{
		String name = ((ISupportName)persist).getName();
		if (name != null)
		{
			IPersist duplicatePersist = cache.get(name);
			if (duplicatePersist != null && !duplicatePersist.equals(persist))
			{
				Map<String, List<IPersist>> duplicates = duplicateNames.get(name);
				if (duplicates == null)
				{
					duplicates = new HashMap<String, List<IPersist>>();
					duplicateNames.put(name, duplicates);
				}
				List<IPersist> duplicatePersists = duplicates.get(cacheName);
				if (duplicatePersists == null)
				{
					duplicatePersists = new ArrayList<>();
					duplicates.put(cacheName, duplicatePersists);
				}
				if (!duplicatePersists.contains(persist)) duplicatePersists.add(persist);
				if (!duplicatePersists.contains(duplicatePersist)) duplicatePersists.add(duplicatePersist);
			}
		}
	}

	@Override
	protected void testNameCache(IPersist item, EventType type)
	{
		if (item instanceof ISupportName)
		{
			String name = ((ISupportName)item).getName();
			if (name != null)
			{
				switch (type)
				{
					case CREATED :
						ConcurrentMap<String, IPersist> classToList = nameToPersist.get(item.getClass());
						if (classToList != null && classToList.containsKey(name))
						{
							Map<String, List<IPersist>> duplicates = duplicateNames.get(name);
							if (duplicates == null)
							{
								duplicates = new HashMap<String, List<IPersist>>();
								duplicateNames.put(name, duplicates);
							}
							List<IPersist> duplicatePersists = duplicates.get(item.getClass().getName());
							if (duplicatePersists == null)
							{
								duplicatePersists = new ArrayList<>();
								duplicates.put(item.getClass().getName(), duplicatePersists);
							}
							duplicatePersists.add(item);
							IPersist duplicatePersist = classToList.get(name);
							if (!duplicatePersists.contains(duplicatePersist)) duplicatePersists.add(duplicatePersist);
						}
						break;
					case REMOVED :
						if (duplicateNames.containsKey(name))
						{
							Map<String, List<IPersist>> duplicates = duplicateNames.get(name);
							if (duplicates != null)
							{
								List<IPersist> duplicatePersists = duplicates.get(item.getClass().getName());
								if (duplicatePersists != null)
								{
									duplicatePersists.remove(item);
								}
								if (duplicatePersists != null && duplicatePersists.size() <= 1)
								{
									duplicates.remove(item.getClass().getName());
								}
								if (duplicates.size() == 0)
								{
									duplicateNames.remove(name);
								}
							}
						}
						break;
					case UPDATED :
						//maybe new name and duplicate is fixed?
						for (String oldName : duplicateNames.keySet())
						{
							Map<String, List<IPersist>> duplicates = duplicateNames.get(oldName);
							List<IPersist> duplicatePersists = duplicates.get(item.getClass().getName());
							if (duplicatePersists != null && duplicatePersists.contains(item) && !oldName.equals(name))
							{
								duplicatePersists.remove(item);
								if (duplicatePersists != null && duplicatePersists.size() <= 1)
								{
									duplicates.remove(item.getClass().getName());
								}
								break;
							}
						}
						// maybe new name and is duplicated ?
						ConcurrentMap<String, IPersist> classToList2 = nameToPersist.get(item.getClass());
						if (classToList2 != null && classToList2.containsKey(name))
						{
							Map<String, List<IPersist>> duplicates = duplicateNames.get(name);
							if (duplicates == null)
							{
								duplicates = new HashMap<String, List<IPersist>>();
								duplicateNames.put(name, duplicates);
							}
							List<IPersist> duplicatePersists = duplicates.get(item.getClass().getName());
							if (duplicatePersists == null)
							{
								duplicatePersists = new ArrayList<>();
								duplicates.put(item.getClass().getName(), duplicatePersists);
							}
							if (!duplicatePersists.contains(item)) duplicatePersists.add(item);
							IPersist duplicatePersist = classToList2.get(name);
							if (!duplicatePersists.contains(duplicatePersist)) duplicatePersists.add(duplicatePersist);
						}
				}
			}
		}
		super.testNameCache(item, type);
	}

	@Override
	protected void testDatasourceCache(IPersist item)
	{
		String ds = null;
		if (item instanceof TableNode || item.getParent() instanceof TableNode)
		{
			ds = item instanceof TableNode ? ((TableNode)item).getDataSource() : ((TableNode)item.getParent()).getDataSource();
			if (ds != null)
			{
				// remove all items of the datasource, it will be recreated
				for (String name : duplicateNames.keySet())
				{
					Map<String, List<IPersist>> duplicates = duplicateNames.get(name);
					Iterator<String> it = duplicates.keySet().iterator();
					while (it.hasNext())
					{
						String className = it.next();
						if (className.startsWith(ds))
						{
							it.remove();
						}
					}
				}
			}
		}
		super.testDatasourceCache(item);
		if (ds != null && (item instanceof TableNode || item.getParent() instanceof TableNode))
		{
			initDatasourceCache(ds);
		}
	}

	@Override
	protected void addInDatasourceCache(ConcurrentMap<String, IPersist> cache, IPersist persist, String datasource)
	{
		testDuplicateCache(cache, persist, datasource + '_' + persist.getClass().getName());
		super.addInDatasourceCache(cache, persist, datasource);
	}

	@Override
	protected void addInScopeCache(Map<String, IScriptElement> cache, IScriptElement persist)
	{
		testDuplicateCache(cache, persist, ISupportScope.class.getName() + "_" + persist.getScopeName());
		super.addInScopeCache(cache, persist);
	}

	@Override
	protected void cleanScopeCache()
	{
		super.cleanScopeCache();
		for (String name : duplicateNames.keySet())
		{
			Map<String, List<IPersist>> duplicates = duplicateNames.get(name);
			Iterator<String> it = duplicates.keySet().iterator();
			while (it.hasNext())
			{
				String className = it.next();
				if (className.startsWith(ISupportScope.class.getName()))
				{
					it.remove();
				}
			}
		}
	}

	public Map<String, Map<String, List<IPersist>>> getDuplicateNamesList()
	{
		initNameCache(Form.class);
		initNameCache(ValueList.class);
		initNameCache(Media.class);
		initNameCache(Relation.class);
		initDatasourceCache(null);
		getGlobalScopeCache();
		return duplicateNames;
	}
}
