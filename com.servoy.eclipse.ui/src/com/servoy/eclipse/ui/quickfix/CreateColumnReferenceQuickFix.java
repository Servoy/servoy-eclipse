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

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.UUID;

/**
 * create a column for an invalid DataProviderID
 * @author lvostinar
 *
 */
public class CreateColumnReferenceQuickFix implements IMarkerResolution
{
	private final String uuid;
	private final String solutionName;

	public CreateColumnReferenceQuickFix(String uuid, String solName)
	{
		this.uuid = uuid;
		this.solutionName = solName;
	}

	public String getLabel()
	{
		return "Create column for invalid DataProviderID.";
	}

	public void run(IMarker marker)
	{
		if (uuid != null)
		{
			UUID id = UUID.fromString(uuid);
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject servoyProject = servoyModel.getServoyProject(solutionName);
			if (servoyProject != null)
			{
				try
				{
					IPersist persist = servoyProject.getEditingPersist(id);
					if (persist instanceof ISupportDataProviderID)
					{
						IPersist parent = persist.getAncestor(IRepository.FORMS);
						if (parent != null)
						{
							ITable table = null;
							String columName = ((ISupportDataProviderID)persist).getDataProviderID();
							int indx = columName.lastIndexOf('.');
							if (indx > 0)
							{
								String relName = columName.substring(0, indx);
								Relation rel = servoyProject.getSolution().getRelation(relName);
								if (rel != null)
								{
									columName = columName.substring(indx + 1);
									table = servoyModel.getDataSourceManager().getDataSource(rel.getForeignDataSource());
								}
							}
							else
							{
								table = servoyModel.getDataSourceManager().getDataSource(((Form)parent).getDataSource());
							}
							if (table != null)
							{
								if (table.getColumn(columName) == null)
								{
									IValidateName nameValidator = servoyModel.getNameValidator();
									Column column = table.createNewColumn(nameValidator, columName, ColumnType.getInstance(IColumnTypes.TEXT, 50, 0));
									IEditorPart editor = EditorUtil.openTableEditor(table);
									if (editor instanceof TableEditor)
									{
										((TableEditor)editor).refresh();
										((TableEditor)editor).selectColumn(column);
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
			}
		}

	}
}
