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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.servoy.j2db.ISolutionModelPersistIndex;
import com.servoy.j2db.PersistIndex;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 8.4
 *
 */
public class DeveloperPersistIndex extends PersistIndex implements ISolutionModelPersistIndex
{
	private final Map<String, Set<Form>> formCacheByDataSource = new HashMap<String, Set<Form>>();
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
		formToDataSource.clear();
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

			if (form.isFormComponent().booleanValue())
			{
				ServoyModelFinder.getServoyModel().fireFormComponentChanged();
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

			if (form.isFormComponent().booleanValue())
			{
				ServoyModelFinder.getServoyModel().fireFormComponentChanged();
			}
		}
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
}
