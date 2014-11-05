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

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.actions.ZOrderAction;
import com.servoy.eclipse.designer.actions.ZOrderAction.OrderableElement;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * @author user
 *
 */
public class ZOrderHandler implements IServerService
{

	private final ISelectionProvider selectionProvider;
	private final BaseVisualFormEditor editorPart;

	public ZOrderHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	/**
	 * @param methodName
	 * @param args
	 */
	public Object executeMethod(final String methodName, JSONObject args)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				List selection = ((IStructuredSelection)selectionProvider.getSelection()).toList();
				if (selection.size() > 0)
				{
					List<OrderableElement> elements = ZOrderAction.calculateNewZOrder(editorPart.getForm(), selection, methodName);
					CompoundCommand cc = new CompoundCommand();
					for (OrderableElement element : elements)
					{
						cc.add(new SetPropertyCommand("zindex", PersistPropertySource.createPersistPropertySource(element.getFormElement(), false),
							StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName(), element.zIndex));
					}
					editorPart.getCommandStack().execute(cc);
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, new ArrayList<IPersist>(selection));
				}
			}
		});
		return null;
	}

}
