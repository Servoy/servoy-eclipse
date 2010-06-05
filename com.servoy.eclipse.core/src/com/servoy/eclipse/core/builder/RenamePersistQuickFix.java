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
package com.servoy.eclipse.core.builder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.IdentDocumentValidator;
import com.servoy.j2db.util.UUID;

public class RenamePersistQuickFix implements IMarkerResolution
{
	private final String uuid;
	private final String solutionName;


	public RenamePersistQuickFix(String uuid, String solName)
	{
		this.uuid = uuid;
		this.solutionName = solName;
	}

	public String getLabel()
	{
		UUID id = UUID.fromString(uuid);
		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
		IPersist persist = null;
		try
		{
			persist = servoyProject.getEditingPersist(id);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return "Rename element '" + ((persist instanceof ISupportName) ? ((ISupportName)persist).getName() : "") + "' from solution '" + solutionName + "'.";
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
					if (persist instanceof ISupportUpdateableName)
					{
						InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "Rename element", "Input a new name for element '" +
							((ISupportName)persist).getName() + "' from solution '" + servoyProject.getSolution().getName() + "'.", "", new IInputValidator()
						{
							public String isValid(String newText)
							{
								boolean valid = IdentDocumentValidator.isJavaIdentifier(newText);
								return valid ? null : (newText.length() == 0 ? "" : "Invalid name");
							}
						});

						if (dialog.open() != Window.OK) return;

						String name = dialog.getValue();

						IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
						try
						{
							((ISupportUpdateableName)persist).updateName(validator, name);
						}
						catch (RepositoryException e)
						{
							MessageDialog.openError(Display.getCurrent().getActiveShell(), "Wrong name", e.getMessage());
							return;
						}
						servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, false);
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
