/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.ui.editors.table;

import java.util.Arrays;
import java.util.TreeSet;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.inmemory.AbstractMemTable;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Utils;

/**
 * Base class for calculations, aggregations and method table editors.
 * @author emera
 */
public class AbstractTableEditorComposite extends Composite
{
	protected final Composite treeContainer;
	protected final TreeViewer treeViewer;
	protected TreeSet<Solution> rows;
	protected ScrolledComposite myScrolledComposite;
	protected Composite container;

	protected final FlattenedSolution flattenedSolution;

	public AbstractTableEditorComposite(Composite parent, int style, FlattenedSolution flattenedSolution)
	{
		super(parent, style);
		this.flattenedSolution = flattenedSolution;

		this.setLayout(new FillLayout());
		myScrolledComposite = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		container = new Composite(myScrolledComposite, SWT.NONE);
		myScrolledComposite.setContent(container);
		treeContainer = new Composite(container, SWT.NONE);
		treeViewer = new TreeViewer(treeContainer, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
	}

	protected void setRows(ITable t)
	{
		rows = new TreeSet<Solution>();
		try
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
				flattenedSolution.getSolution().getName());
			if (servoyProject != null)
			{
				if (t instanceof AbstractMemTable)
				{
					ServoyProject owner = ((AbstractMemTable)t).getServoyProject();
					ServoyProject[] projects = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
					for (ServoyProject project : projects)
					{
						if ((Utils.equalObjects(owner.getProject().getName(), project.getProject().getName()) ||
							Arrays.asList(ServoyBuilder.getSolutionModules(project)).contains(owner)) && !rows.contains(project.getEditingSolution()))
						{
							rows.add(project.getEditingSolution());
						}
					}
				}
				else
				{
					Solution solution = servoyProject.getEditingSolution();
					rows.add(solution);
					Solution[] modules = flattenedSolution.getModules();
					if (modules != null && modules.length > 0)
					{
						for (Solution module : modules)
						{
							ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(module.getName());
							if (project != null)
							{
								solution = (Solution)project.getEditingPersist(module.getUUID());
								if (solution != null)
								{
									rows.add(solution);
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		treeViewer.setInput(rows);
		treeViewer.expandAll();
	}
}
