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

import java.util.Collection;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.actions.AbstractEditorActionDelegateHandler;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * @author user
 *
 */
public class SetTabSequenceCommand extends AbstractEditorActionDelegateHandler implements IServerService
{
	private final BaseVisualFormEditor editorPart;
	private final ISelectionProvider selectionProvider;


	public SetTabSequenceCommand()
	{
		this.editorPart = null;
		this.selectionProvider = null;
	}

	public SetTabSequenceCommand(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	public Object executeMethod(String methodName, JSONObject args)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				editorPart.getCommandStack().execute(createCommand());
			}
		});
		return null;
	}

	@Override
	protected Command createCommand()
	{
		PersistContext[] selection = null;
		if (selectionProvider != null)
		{
			selection = (PersistContext[])((IStructuredSelection)selectionProvider.getSelection()).toList().toArray(new PersistContext[0]);
		}
		else
		{
			selection = getSelectedObjects().toArray(new PersistContext[0]);
		}
		if (selection.length > 0)
		{
			int tabIndex = 1;
			CompoundCommand cc = new CompoundCommand();
			for (PersistContext persist : selection)
			{
				if (persist.getPersist() instanceof Bean)
				{
					WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(
						((Bean)persist.getPersist()).getBeanClassName());
					Collection<PropertyDescription> tabSeqProps = spec.getProperties(TypesRegistry.getType("tabseq"));
					for (PropertyDescription pd : tabSeqProps)
					{
						cc.add(new SetPropertyCommand("tabSeq", PersistPropertySource.createPersistPropertySource(persist, false), pd.getName(),
							Integer.valueOf(tabIndex)));
						tabIndex++;
					}
				}
				else
				{
					cc.add(new SetPropertyCommand("tabSeq", PersistPropertySource.createPersistPropertySource(persist, false),
						StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName(), Integer.valueOf(tabIndex)));
					tabIndex++;
				}
			}
			return cc;
		}
		return super.createCommand();
	}
}
