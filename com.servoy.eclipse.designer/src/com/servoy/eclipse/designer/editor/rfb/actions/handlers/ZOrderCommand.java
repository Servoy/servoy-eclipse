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

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.actions.ZOrderAction;
import com.servoy.eclipse.designer.actions.ZOrderAction.OrderableElement;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * @author lvostinar
 *
 */
public class ZOrderCommand extends AbstractEditorAndOutlineActionDelegateHandler implements IServerService
{

	private final ISelectionProvider selectionProvider;
	private final BaseVisualFormEditor editorPart;
	private final String methodName;

	public ZOrderCommand(String methodName)
	{
		this.editorPart = null;
		this.selectionProvider = null;
		this.methodName = methodName;
	}

	public ZOrderCommand(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider, String methodName)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
		this.methodName = methodName;
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
				List<PersistContext> contextSelection = ((IStructuredSelection)selectionProvider.getSelection()).toList();
				if (contextSelection.size() > 0)
				{
					List<IPersist> selection = new ArrayList<IPersist>();
					for (PersistContext pc : contextSelection)
					{
						selection.add(pc.getPersist());
					}

					editorPart.getCommandStack().execute(createCommand());
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, new ArrayList<IPersist>(selection));
				}
			}
		});
		return null;
	}

	protected Command createCommand(String methodName)
	{
		List contextSelection = null;
		List selection = new ArrayList<IPersist>();
		if (selectionProvider != null)
		{
			contextSelection = ((IStructuredSelection)selectionProvider.getSelection()).toList();
		}
		else
		{
			contextSelection = getSelectedObjects();
		}
		for (Object pc : contextSelection)
		{
			selection.add(((PersistContext)pc).getPersist());
			if (((PersistContext)pc).getPersist().getAncestor(IRepository.CSSPOS_LAYOUTCONTAINERS) != null) return null;
		}
		Form form = null;
		if (editorPart != null)
		{
			form = editorPart.getForm();
		}
		else
		{
			form = ((BaseVisualFormEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor()).getForm();
		}
		if (selection.size() > 0)
		{
			List<OrderableElement> elements = ZOrderAction.calculateNewZOrder(form, selection, methodName);
			CompoundCommand cc = new CompoundCommand();
			for (OrderableElement element : elements)
			{
				cc.add(new SetPropertyCommand("zindex", PersistPropertySource.createPersistPropertySource(element.getFormElement(), form, false),
					StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName(), element.zIndex));
			}
			return cc;
		}
		return super.createCommand();
	}

	@Override
	protected Command createCommand()
	{
		return createCommand(methodName);
	}

	public static class ToFront extends ZOrderCommand
	{
		public ToFront()
		{
			super("z_order_bring_to_front");
		}
	}

	public static class ToBack extends ZOrderCommand
	{
		public ToBack()
		{
			super("z_order_send_to_back");
		}
	}

	public static class ToFrontOneStep extends ZOrderCommand
	{
		public ToFrontOneStep()
		{
			super("z_order_bring_to_front_one_step");
		}
	}

	public static class ToBackOneStep extends ZOrderCommand
	{
		public ToBackOneStep()
		{
			super("z_order_send_to_back_one_step");
		}
	}
}
