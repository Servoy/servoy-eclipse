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

package com.servoy.eclipse.ui.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.DataSourceWrapperFactory;
import com.servoy.eclipse.model.util.IDataSourceWrapper;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.RenameInMemTableAction;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Pair;

/**
 * @author emera
 */
public class RenameMemTableQuickFix implements IMarkerResolution
{
	private final TableNode tableNode;
	private final ServoyProject project;

	public RenameMemTableQuickFix(IPersist persist, ServoyProject servoyProject)
	{
		this.tableNode = (TableNode)persist;
		this.project = servoyProject;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	@Override
	public String getLabel()
	{
		return "Rename duplicate mem table " + tableNode.getTableName();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	@Override
	public void run(IMarker marker)
	{
		RenameInMemTableAction action = new RenameInMemTableAction(UIUtils.getActiveShell(),
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
			tableNode.getDataSource() == DataSourceUtils.INMEM_DATASOURCE ? UserNodeType.INMEMORY_DATASOURCE : UserNodeType.VIEW_FOUNDSET);
		action.setSelection(new StructuredSelection(
			new Pair<IDataSourceWrapper, IServer>(DataSourceWrapperFactory.getWrapper(tableNode.getDataSource()), project.getMemServer())));
		action.run();
	}

}
