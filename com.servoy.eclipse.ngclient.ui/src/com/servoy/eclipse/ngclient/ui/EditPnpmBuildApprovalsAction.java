/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.ngclient.ui;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * SVY-21502: Solution Explorer context-menu action on the <code>ng_web_packages</code> node
 * (<code>UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES</code>) that opens
 * {@link PnpmBuildApprovalsDialog} to review and edit the solution's pnpm dependency build-script allow/deny lists,
 * saving to <code>ng_web_packages/pnpm-build-approvals.json</code> via {@link PnpmBuildApprovals}.
 *
 * @author jcompagner
 * @since 2026.12
 */
public class EditPnpmBuildApprovalsAction extends Action
{
	private final String solutionName;

	/**
	 * @param solutionName the name of the solution whose approvals are edited; must not be <code>null</code>
	 */
	public EditPnpmBuildApprovalsAction(String solutionName)
	{
		this.solutionName = solutionName;
		setText("Edit pnpm build-script approvals");
		setToolTipText("Review and edit which dependency build scripts pnpm is allowed to run for this solution");
	}

	@Override
	public void run()
	{
		if (solutionName == null) return;
		ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(solutionName);
		if (project == null)
		{
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "pnpm build-script approvals",
				"Could not resolve the solution project '" + solutionName + "'.");
			return;
		}
		PnpmBuildApprovals approvals = PnpmBuildApprovals.fromWebPackagesFolder(project.getProject());
		PnpmBuildApprovalsDialog dialog = new PnpmBuildApprovalsDialog(Display.getCurrent().getActiveShell(), approvals);
		if (dialog.open() == org.eclipse.jface.dialogs.IDialogConstants.OK_ID)
		{
			try
			{
				approvals.saveToWebPackagesFolder(project.getProject());
				project.getProject().getFolder(SolutionSerializer.NG_PACKAGES_DIR_NAME).refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
			}
			catch (IOException e)
			{
				ServoyLog.logError("Error saving pnpm build approvals for solution " + solutionName, e);
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "pnpm build-script approvals",
					"Could not save the approvals file: " + e.getMessage());
			}
			catch (CoreException e)
			{
				ServoyLog.logError("Error refreshing ng_web_packages after saving pnpm build approvals for solution " + solutionName, e);
			}
		}
	}
}
