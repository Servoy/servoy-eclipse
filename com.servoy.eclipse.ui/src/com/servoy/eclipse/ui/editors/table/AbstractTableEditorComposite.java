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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.DataSourceUtils;

/**
 * Base class for calculations, aggregations and method table editors.
 * @author emera
 */
public class AbstractTableEditorComposite extends Composite
{
	protected final Composite treeContainer;
	protected final TreeViewer treeViewer;
	protected ArrayList<Solution> rows;
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
		rows = new ArrayList<Solution>();
		try
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
				flattenedSolution.getSolution().getName());
			if (servoyProject != null)
			{
				Solution solution = servoyProject.getEditingSolution();
				rows.add(solution);

				if (t.getServerName().equals(DataSourceUtils.INMEM_DATASOURCE))
				{
					Iterator<TableNode> nodes = flattenedSolution.getTableNodes(t);
					if (nodes.hasNext())
					{
						TableNode tn = nodes.next();
						Solution s = (Solution)tn.getAncestor(IRepository.SOLUTIONS);
						if (!rows.contains(s)) rows.add(s);
						ServoyProject[] projects = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProjects();
						for (ServoyProject project : projects)
						{
							if (project.getFlattenedSolution().hasModule(s.getName()) && flattenedSolution.hasModule(project.getSolution().getName()) &&
								!rows.contains(project.getSolution()))
							{
								rows.add(project.getSolution());
							}
						}
					}
				}
				else
				{

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
