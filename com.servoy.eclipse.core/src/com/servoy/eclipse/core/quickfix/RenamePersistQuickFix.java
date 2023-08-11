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
package com.servoy.eclipse.core.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

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
		IPersist persist = servoyProject.getEditingPersist(id);
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
					final IPersist persist = servoyProject.getEditingPersist(id);
					if (persist instanceof ISupportUpdateableName || persist instanceof Media)
					{
						InputDialog dialog = new InputDialog(UIUtils.getActiveShell(), "Rename element", "Input a new name for element '" +
							((ISupportName)persist).getName() + "' from solution '" + servoyProject.getSolution().getName() + "'.",
							((ISupportName)persist).getName(), new IInputValidator()
							{
								public String isValid(String newText)
								{
									if (!(persist instanceof Media))
									{
										boolean valid = IdentDocumentValidator.isJavaIdentifier(newText);
										return valid ? ((newText.equalsIgnoreCase(((ISupportName)persist).getName())) ? "" : null) : (newText.length() == 0
											? "" : "Invalid name");
									}
									else
									{
										if (newText.length() == 0)
										{
											return "";
										}
										if (newText.indexOf('\\') >= 0 || newText.indexOf('/') >= 0 || newText.indexOf(' ') >= 0)
										{
											return "Invalid new media name";
										}
										// ok
										return null;
									}
								}
							});

						if (dialog.open() != Window.OK) return;

						String name = dialog.getValue();

						IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
						try
						{
							if (persist instanceof ISupportUpdateableName)
							{
								((ISupportUpdateableName)persist).updateName(validator, name);
							}
							else if (persist instanceof Media)
							{
								((Media)persist).setName(name);
							}
						}
						catch (RepositoryException e)
						{
							MessageDialog.openError(UIUtils.getActiveShell(), "Wrong name", e.getMessage());
							return;
						}
						servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, (persist instanceof Media) ? true : false);
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
