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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.InMemServerWrapper;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.TableNode;

/**
 * Used to select persists of the same type.
 * @author jcompagner
 */
public abstract class AbstractPersistSelectionAction extends Action implements ISelectionChangedListener
{

	private IStructuredSelection selection;
	protected final Shell shell;
	protected UserNodeType nodeType;
	protected String persistString = null;
	protected IPersist selectedPersist;

	/**
	 *
	 */
	public AbstractPersistSelectionAction(Shell shell)
	{
		super();
		this.shell = shell;
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		nodeType = null;
		selection = null;
		ServoyProject project = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() > 0);
		if (state)
		{
			Iterator<SimpleUserNode> it = sel.iterator();
			while (it.hasNext())
			{

				SimpleUserNode node = it.next();
				UserNodeType type = node.getType();
				SimpleUserNode projectNode = node.getAncestorOfType(ServoyProject.class);
				if (projectNode != null)
				{
					ServoyProject sp = (ServoyProject)projectNode.getRealObject();
					if (project == null) project = sp;
					else
					{
						if (!project.equals(sp))
						{
							state = false;
						}
					}
					if (nodeType == null)
					{
						nodeType = type;
					}
					if (!isEnabledForNode(type))
					{
						state = false;
						break;
					}
				}
				else
				{
					state = false;
				}
			}
			selection = sel;
		}
		if (nodeType == UserNodeType.RELATION) persistString = "relation";
		if (nodeType == UserNodeType.INMEMORY_DATASOURCE) persistString = "in memory datasource";
		if (nodeType == UserNodeType.VALUELIST_ITEM) persistString = "valuelist";
		if (nodeType == UserNodeType.MEDIA_IMAGE) persistString = "media";
		if (nodeType == UserNodeType.FORM) persistString = "form";

		setEnabled(state);
	}

	protected abstract boolean isEnabledForNode(UserNodeType type);

	protected int getPersistType()
	{
		if (nodeType == UserNodeType.FORM) return IRepository.FORMS;
		if (nodeType == UserNodeType.RELATION) return IRepository.RELATIONS;
		if (nodeType == UserNodeType.VALUELISTS) return IRepository.VALUELISTS;
		if (nodeType == UserNodeType.MEDIA_IMAGE) return IRepository.MEDIA;
		if (nodeType == UserNodeType.INMEMORY_DATASOURCE) return IRepository.TABLENODES;
		if (selectedPersist instanceof ScriptCalculation) return IRepository.SCRIPTCALCULATIONS;
		if (selectedPersist instanceof AggregateVariable) return IRepository.AGGREGATEVARIABLES;
		return -1;
	}

	public void setPersist(IPersist persist)
	{
		this.selectedPersist = persist;
		if (persist instanceof ScriptCalculation) persistString = "calculation";
		if (persist instanceof AggregateVariable) persistString = "aggregation";
	}


	@Override
	public void run()
	{
		IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
		if (selection != null)
		{
			List<IPersist> persistList = new ArrayList<IPersist>();
			Iterator<SimpleUserNode> it = selection.iterator();
			while (it.hasNext())
			{
				SimpleUserNode node = it.next();
				SimpleUserNode projectNode = node.getAncestorOfType(ServoyProject.class);
				if (projectNode != null)
				{
					Object persist = node.getRealObject();
					if (persist instanceof IPersist && persist instanceof ISupportName)
					{
						persistList.add((IPersist)persist);
					}
					else if (persist instanceof InMemServerWrapper && projectNode.getRealObject() instanceof ServoyProject)
					{
						Iterator<TableNode> nodes = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getTableNodes(
							((InMemServerWrapper)persist).getDataSource());
						while (nodes.hasNext())
						{
							TableNode tableNode = nodes.next();
							if (tableNode.getColumns() != null)
							{
								persistList.add(tableNode);
								break;
							}
						}

					}
				}
			}

			doWork(persistList.toArray(new IPersist[persistList.size()]), nameValidator);
		}
		if (selectedPersist instanceof ISupportName)
		{
			doWork(new IPersist[] { selectedPersist }, nameValidator);
		}
	}

	/**
	 * @param editingForms
	 * @param nameValidator
	 */
	protected abstract void doWork(IPersist[] editingPersists, IValidateName nameValidator);

	protected String getName(IPersist persist)
	{
		return (persist instanceof ISupportName ? ((ISupportName)persist).getName() : ((TableNode)persist).getTableName());
	}
}