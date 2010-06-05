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

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.UUID;

public class RemoveInvalidSortColumnsQuickFix implements IMarkerResolution
{
	private final String uuid;
	private final String solutionName;

	public RemoveInvalidSortColumnsQuickFix(String uuid, String solName)
	{
		this.uuid = uuid;
		this.solutionName = solName;
	}

	public String getLabel()
	{
		return "Remove invalid sort columns.";
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
					String options = null;
					Table table = null;
					if (persist instanceof Relation)
					{
						options = ((Relation)persist).getInitialSort();
						table = ((Relation)persist).getForeignTable();
					}
					else if (persist instanceof Form)
					{
						options = ((Form)persist).getInitialSort();
						table = ((Form)persist).getTable();
					}
					else if (persist instanceof ValueList)
					{
						ValueList vl = ((ValueList)persist);
						options = vl.getSortOptions();
						if (vl.getRelationName() != null)
						{
							Relation[] relations = servoyProject.getEditingFlattenedSolution().getRelationSequence(vl.getRelationName());
							if (relations != null)
							{
								table = relations[relations.length - 1].getForeignTable();
							}
						}
						else
						{
							table = (Table)vl.getTable();
						}
					}
					if (table != null && options != null)
					{
						List<SortColumn> sortColumns = com.servoy.eclipse.core.Activator.getDefault().getDesignClient().getFoundSetManager().getSortColumns(
							table, options);
						String newOptions = FoundSetManager.getSortColumnsAsString(sortColumns);
						if (persist instanceof Relation)
						{
							((Relation)persist).setInitialSort(newOptions);
						}
						else if (persist instanceof Form)
						{
							((Form)persist).setInitialSort(newOptions);
						}
						else if (persist instanceof ValueList)
						{
							((ValueList)persist).setSortOptions(newOptions);
						}
					}
					servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, true);
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}
}
