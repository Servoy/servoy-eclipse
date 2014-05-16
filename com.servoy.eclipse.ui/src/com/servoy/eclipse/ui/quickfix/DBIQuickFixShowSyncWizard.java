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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import com.servoy.eclipse.core.quickfix.dbi.TableDifferenceQuickFix;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.resource.ImageResource;
import com.servoy.eclipse.ui.wizards.SynchronizeDBIWithDBWizard;

public class DBIQuickFixShowSyncWizard extends WorkbenchMarkerResolution
{

	private IMarker current = null; // avoid duplicate listings in quick-fix dialog (for choosing multiple markers to fix)

	private static DBIQuickFixShowSyncWizard instance;

	private DBIQuickFixShowSyncWizard()
	{
	}

	public static DBIQuickFixShowSyncWizard getInstance()
	{
		if (instance == null)
		{
			instance = new DBIQuickFixShowSyncWizard();
		}
		return instance;
	}

	public void setCurrentMarker(IMarker marker)
	{
		current = marker;
	}

	public String getLabel()
	{
		return "Open 'Synchronize DB tables with DB information' wizard";
	}

	public Image getImage()
	{
		return ImageResource.INSTANCE.getImage(Activator.loadImageDescriptorFromBundle("correction_change.gif"));
	}

	public String getDescription()
	{
		return getLabel();
	}

	public void run(IMarker marker)
	{
		runInternal();
	}

	private void runInternal()
	{
		IWorkbenchWizard wizard = new SynchronizeDBIWithDBWizard();

		IStructuredSelection selection = StructuredSelection.EMPTY;
		wizard.init(PlatformUI.getWorkbench(), selection);

		// Instantiates the wizard container with the wizard and opens it
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
		dialog.create();
		dialog.open();
		wizard.dispose();
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
					TableDifference difference = TableDifferenceQuickFix.getTableDifference(marker);

					if (difference.getType() == TableDifference.MISSING_DBI_FILE || difference.getType() == TableDifference.MISSING_TABLE)
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
		monitor.beginTask("Running 'Synchronize DB tables with DB information' wizard", IProgressMonitor.UNKNOWN);
		try
		{
			runInternal();
		}
		finally
		{
			monitor.done();
		}
	}

}