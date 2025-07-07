/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentCommand.CreateComponentOptions;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Debug;

public class CreateComponentsHandler extends CreateComponentHandler
{

	public CreateComponentsHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		super(editorPart, selectionProvider);
	}

	@Override
	public Object executeMethod(String methodName, final JSONObject args)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				final IStructuredSelection[] newSelection = new IStructuredSelection[1];
				editorPart.getCommandStack().execute(new BaseRestorableCommand("createComponents")
				{
					private List<IPersist> newPersists;

					@Override
					public void execute()
					{
						try
						{
							List<IPersist> changedPersists = new ArrayList<IPersist>();
							if (args.has("components"))
							{
								JSONArray components = args.getJSONArray("components");
								newPersists = new ArrayList<IPersist>();
								for (int i = 0; i < components.length(); i++)
								{
									IPersist[] persist = CreateComponentCommand.createComponent(editorPart.getForm(),
										CreateComponentOptions.fromJson(components.getJSONObject(i)),
										changedPersists);
									for (IPersist iPersist : persist)
									{
										if (persist != null)
										{
											newPersists.add(iPersist);
											changedPersists.add(iPersist);
										}
										else
										{
											Debug.error("Could not create the component " + components.getJSONObject(i).toString());
										}
									}
								}
							}
							if (newPersists != null && newPersists.size() > 0)
							{
								ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changedPersists);
								newSelection[0] = new StructuredSelection(
									newPersists.stream().map(persist -> PersistContext.create(persist, editorPart.getForm())).collect(Collectors.toList()));
							}
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}

					@Override
					public void undo()
					{
						try
						{
							if (newPersists != null)
							{
								for (IPersist persist : newPersists)
								{
									((IDeveloperRepository)persist.getRootObject().getRepository()).deleteObject(persist);
								}
								ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, newPersists);
							}
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError("Could not undo create elements", e);
						}
					}

				});
				if (newSelection[0] != null) selectionProvider.setSelection(newSelection[0]);
			}
		});
		return null;
	}

}
