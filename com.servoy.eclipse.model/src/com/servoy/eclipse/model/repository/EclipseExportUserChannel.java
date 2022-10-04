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
package com.servoy.eclipse.model.repository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.model.export.IExportSolutionModel;
import com.servoy.eclipse.model.util.ScrollableDialog;
import com.servoy.j2db.util.ILogLevel;

public class EclipseExportUserChannel extends AbstractEclipseExportUserChannel
{
	public EclipseExportUserChannel(IExportSolutionModel exportModel, IProgressMonitor monitor)
	{
		super(exportModel, monitor);
	}

	public void info(String message, int priority)
	{
		// Seems that for each referenced module we get one message with priority INFO.
		// We send these to the progress monitor.
		if (priority == ILogLevel.INFO)
		{
			monitor.setTaskName(message);
			monitor.worked(1);
		}
	}


	@Override
	public void displayWarningMessage(String title, String message, boolean scrollableDialog)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				if (scrollableDialog)
				{
					ScrollableDialog dialog = new ScrollableDialog(Display.getDefault().getActiveShell(), IMessageProvider.WARNING, "War export", title,
						message);
					dialog.open();
				}
				else
				{
					MessageDialog.openWarning(Display.getDefault().getActiveShell(), title,
						message);
				}
			}
		});
	}
}
