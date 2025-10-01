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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import static java.util.Arrays.asList;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebComponent;

/**
 * @author lvostinar
 *
 */
public class ConvertComponentHandler implements IServerService
{
	private final BaseVisualFormEditor editorPart;
	private final ISelectionProvider selectionProvider;

	/**
	 * @param editorPart
	 * @param selectionProvider
	 */
	public ConvertComponentHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		if (selectionProvider != null && selectionProvider.getSelection() instanceof StructuredSelection selection && selection.size() == 1 &&
			selection.getFirstElement() instanceof PersistContext persistContext && persistContext.getPersist() instanceof BaseComponent originalComponent)
		{
			Display.getDefault().asyncExec(() -> {
				try
				{
					ConversionComponentDialog dialog = new ConversionComponentDialog(editorPart.getSite().getShell(), persistContext);
					dialog.open();

					if (dialog.getReturnCode() != Window.CANCEL && dialog.getValue() != null && dialog.getSpecName() != null)
					{
						final WebComponent[] finalPersist = new WebComponent[1];
						editorPart.getCommandStack().execute(new BaseRestorableCommand("convertComponent")
						{
							@Override
							public void execute()
							{
								try
								{
									((IDeveloperRepository)persistContext.getPersist().getRootObject().getRepository())
										.deleteObject(originalComponent);
									finalPersist[0] = ((AbstractContainer)originalComponent.getParent())
										.createNewWebComponent(originalComponent.getName(), dialog.getSpecName());
									dialog.getValue().forEach(pair -> {
										if (pair.getRight() != null && originalComponent.hasProperty(pair.getLeft()))
										{
											finalPersist[0].setProperty(pair.getRight(), originalComponent.getProperty(pair.getLeft()));
										}
									});
									ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false,
										asList(new IPersist[] { originalComponent, finalPersist[0] }));
								}
								catch (Exception e)
								{
									ServoyLog.logError("Cannot convert component", e);
								}
							}

							@Override
							public void undo()
							{
								try
								{
									if (finalPersist[0] != null)
									{
										ISupportChilds parent = finalPersist[0].getParent();
										((IDeveloperRepository)finalPersist[0].getRootObject().getRepository()).deleteObject(finalPersist[0]);
										IPersist revertedComponent = originalComponent.cloneObj(parent, true, null, false, false, false);
										ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false,
											asList(new IPersist[] { finalPersist[0], revertedComponent }));
									}
								}
								catch (RepositoryException e)
								{
									ServoyLog.logError("Could not undo create layout container", e);
								}
							}
						});

					}
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			});
		}
		return null;
	}

}
