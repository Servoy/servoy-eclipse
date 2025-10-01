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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.UUID;

/**
 * @author emera
 */
public abstract class AbstractGroupCommand extends AbstractEditorAndOutlineActionDelegateHandler implements IServerService
{

	public static class GroupCommand extends AbstractGroupCommand implements IServerService
	{
		public GroupCommand()
		{
			super("group");
		}

		public GroupCommand(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
		{
			super("group", editorPart, selectionProvider);
		}
	}

	public static class UngroupCommand extends AbstractGroupCommand implements IServerService
	{
		public UngroupCommand()
		{
			super("ungroup");
		}

		public UngroupCommand(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
		{
			super("ungroup", editorPart, selectionProvider);
		}
	}

	private String methodName = null;
	private ISelectionProvider selectionProvider = null;
	private BaseVisualFormEditor editorPart = null;
	private List<FormElementGroup> selectedGroups = null;

	public AbstractGroupCommand(String method)
	{
		super();
		this.methodName = method;
	}

	public AbstractGroupCommand(String method, BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.methodName = method;
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	@Override
	public Object executeMethod(String methodName, JSONObject args)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				List< ? > contextSelection = ((IStructuredSelection)selectionProvider.getSelection()).toList();
				selectedGroups = new ArrayList<>();
				if (contextSelection.size() > 0)
				{
					List<IPersist> selection = new ArrayList<IPersist>();
					for (Object element : contextSelection)
					{
						if (element instanceof PersistContext)
						{
							selection.add(((PersistContext)element).getPersist());
						}
						else if (element instanceof FormElementGroup)
						{
							selectedGroups.add((FormElementGroup)element);
						}
					}

					editorPart.getCommandStack().execute(createCommand());
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, new ArrayList<IPersist>(selection));
				}
			}
		});
		return null;
	}

	@Override
	protected Command createCommand()
	{
		List contextSelection = null;
		List<IPersist> selection = new ArrayList<IPersist>();
		if (selectionProvider != null)
		{
			contextSelection = ((IStructuredSelection)selectionProvider.getSelection()).toList();
		}
		else
		{
			selectedGroups = new ArrayList<>();
			contextSelection = getSelectedObjects();
		}
		for (Object pc : contextSelection)
		{
			if (pc instanceof PersistContext)
			{
				selection.add(((PersistContext)pc).getPersist());
			}
			else if (pc instanceof FormElementGroup)
			{
				selectedGroups.add((FormElementGroup)pc);
			}
		}

		if (selectedGroups.size() > 0)
		{
			for (FormElementGroup group : selectedGroups)
			{
				Iterator<ISupportFormElement> elementsIt = group.getElements();
				while (elementsIt.hasNext())
				{
					selection.add(elementsIt.next());
				}
			}
		}

		if (selection.size() > 1 || selectedGroups.size() > 0)
		{
			CompoundCommand cc = new CompoundCommand();

			String groupID = null;
			if ("group".equals(methodName))
			{
				//add to selected group or generate new group id
				groupID = (selectedGroups.size() == 1) ? selectedGroups.get(0).getGroupID() : UUID.randomUUID().toString();
			}
			for (IPersist element : selection)
			{
				cc.add(new SetPropertyCommand("group", PersistPropertySource.createPersistPropertySource(element, false),
					StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName(), groupID));
			}
			return cc;
		}
		return super.createCommand();
	}

}
