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
package com.servoy.eclipse.core.quickfix.dbi;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ServoyLog;

public abstract class TableDifferenceQuickFix extends WorkbenchMarkerResolution implements IMarkerResolutionExceptionProvider
{

	private IMarker current = null;
	private Exception storedException = null;

	public static TableDifference getTableDifference(IMarker marker)
	{
		if (!marker.exists()) return null;

		TableDifference difference = null;
		try
		{
			String serverName = (String)marker.getAttribute(TableDifference.ATTRIBUTE_SERVERNAME);
			if (serverName != null)
			{
				DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
				if (dmm != null)
				{
					String tableName = null;
					String columnName = null;
					tableName = (String)marker.getAttribute(TableDifference.ATTRIBUTE_TABLENAME);
					columnName = (String)marker.getAttribute(TableDifference.ATTRIBUTE_COLUMNNAME);

					difference = dmm.getColumnDifference(serverName, tableName, columnName);
				}
				else
				{
					ServoyLog.logError("dmm is null but table difference marker exists", null);
				}
			}
			else
			{
				ServoyLog.logError("serverName is null for table difference marker", null);
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		return difference;
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers)
	{
		List<IMarker> solvableMarkers = new ArrayList<IMarker>();
		for (IMarker marker : markers)
		{
			if (marker == current) continue;
			try
			{
				if (marker.exists() && marker.getType().equals(ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE))
				{
					TableDifference difference = getTableDifference(marker);

					if (canHandleDifference(difference))
					{
						solvableMarkers.add(marker);
					}
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		return solvableMarkers.toArray(new IMarker[solvableMarkers.size()]);
	}

	@Override
	public void run(IMarker[] markers, IProgressMonitor monitor)
	{
		final TableDifference differences[] = new TableDifference[markers.length];
		for (int i = 0; i < markers.length; i++)
		{
			differences[i] = getTableDifference(markers[i]);
		}
		try
		{
			ServoyModel.getWorkspace().run(new IWorkspaceRunnable()
			{
				public void run(IProgressMonitor m) throws CoreException
				{
					for (TableDifference element : differences)
					{
						if (element != null)
						{
							m.subTask(element.getUserFriendlyMessage());
							TableDifferenceQuickFix.this.run(element);
							Exception e = TableDifferenceQuickFix.this.retrieveException(true);
							if (e != null)
							{
								MessageDialog.openError(UIUtils.getActiveShell(), "Error", "Quick fix failed: " + e.getMessage());
								break;
							}
						}
					}
				}
			}, ServoyModel.getWorkspace().getRoot(), IWorkspace.AVOID_UPDATE, monitor);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	public abstract boolean canHandleDifference(TableDifference difference);

	public abstract void run(TableDifference difference);

	public void run(IMarker marker)
	{
		if (!marker.exists()) return;
		TableDifference difference = getTableDifference(marker);
		if (difference == null) return;
		run(difference);
	}

	public void setCurrentMarker(IMarker marker)
	{
		current = marker;
	}

	public String getDescription()
	{
		return getLabel();
	}

	public String getShortLabel()
	{
		return getLabel();
	}

	public Image getImage()
	{
		return null;
	}

	protected final void storeException(Exception e)
	{
		this.storedException = e;
	}

	public Exception retrieveException(boolean clearOnExit)
	{
		//need this since new Exception(null) is not null
		Exception retException = (storedException != null) ? new Exception(storedException) : null;
		if ((storedException != null) && clearOnExit) storedException = null;
		return retException;
	}

}