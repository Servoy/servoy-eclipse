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
package com.servoy.eclipse.ui.wizards;

import org.eclipse.core.runtime.IProgressMonitor;

import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.xmlxport.IXMLExportUserChannel;

public class EclipseExportUserChannel implements IXMLExportUserChannel
{
	private final ExportSolutionModel exportModel;
	private final IProgressMonitor monitor;

	public EclipseExportUserChannel(ExportSolutionModel exportModel, IProgressMonitor monitor)
	{
		this.exportModel = exportModel;
		this.monitor = monitor;
	}

	public SortedList getModuleIncludeList(SortedList allModules)
	{
		SortedList sl = new SortedList();
		String[] modules = exportModel.getModulesToExport();
		if (modules != null)
		{
			for (String element : modules)
				sl.add(element);
		}
		return sl;
	}

	public String getProtectionPassword(Solution solution)
	{
		return exportModel.getPassword();
	}

	public boolean unlock(Solution solution)
	{
		return true;
	}

	public void info(String message, int priority)
	{
		// Seems that for each referenced module we get one message with priority 1. 
		// We send these to the progress monitor.
		if (priority == 1)
		{
			monitor.setTaskName(message);
			monitor.worked(1);
		}
	}

	public boolean getExportAllTablesFromReferencedServers()
	{
		return exportModel.getExportAllTablesFromReferencedServers();
	}
}
