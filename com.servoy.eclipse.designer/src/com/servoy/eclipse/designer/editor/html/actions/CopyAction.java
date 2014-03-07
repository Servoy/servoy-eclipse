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

package com.servoy.eclipse.designer.editor.html.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.PersistUtils;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.designer.editor.commands.FormElementCopyCommand;
import com.servoy.eclipse.designer.editor.mobile.MobileVisualFormEditorHtmlDesignPage;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileFooterGraphicalEditPart;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileHeaderGraphicalEditPart;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.util.Utils;

/** Command to copy selected editor models.
 * 
 * @author rgansevles
 *
 */
public class CopyAction extends SelectionAction
{
	/**
	 * Constructs a <code>CopyAction</code> using the specified part.
	 * 
	 * @param part
	 *            The part for this action
	 */
	public CopyAction(IWorkbenchPart part)
	{
		super(part);
	}

	@Override
	protected void init()
	{
		super.init();
		setText(GEFMessages.CopyAction_Label);
		setToolTipText(GEFMessages.CopyAction_Tooltip);
		setId(ActionFactory.COPY.getId());
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
	}

	/**
	 * Create a command to copy the selected objects.
	 */
	public Command createCopyCommand(List< ? > objects)
	{
		if (objects.isEmpty()) return null;

		final List<IPersist> toCopy = new ArrayList<IPersist>();
		for (Object modelObject : objects)
		{
			if (modelObject instanceof Form) continue; // do not copy entire form here

			if (modelObject instanceof FormElementGroup)
			{
				toCopy.addAll(Utils.asList(((FormElementGroup)modelObject).getElements()));
			}
			else if (modelObject instanceof IPersist)
			{
				Part part = null;
				if (modelObject instanceof Part)
				{
					part = (Part)modelObject;
				}
				else if (modelObject instanceof GraphicalComponent &&
					((GraphicalComponent)modelObject).getCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName) != null)
				{
					// copying header, copying part
					part = MobileVisualFormEditorHtmlDesignPage.getHeaderPart((GraphicalComponent)modelObject);
				}

				if (part != null)
				{
					if (PersistUtils.isHeaderPart(part.getPartType()))
					{
						for (IPersist item : MobileHeaderGraphicalEditPart.getHeaderModelChildren(Activator.getDefault().getDesignClient(),
							(Form)(part).getAncestor(IRepository.FORMS)))
						{
							if (item != modelObject)
							{
								toCopy.add(item);
							}
						}
						if (part != modelObject)
						{
							toCopy.add(part);
						}
					}
					else if (PersistUtils.isFooterPart(part.getPartType()))
					{
						for (IPersist item : MobileFooterGraphicalEditPart.getFooterModelChildren(Activator.getDefault().getDesignClient(),
							(Form)(part).getAncestor(IRepository.FORMS)))
						{
							toCopy.add(item);
						}
					}
				}

				toCopy.add((IPersist)modelObject);
			}
			else if (modelObject instanceof MobileListModel)
			{
				MobileListModel listModel = (MobileListModel)modelObject;
				if (listModel.component != null)
				{
					// inset list
					if (listModel.header != null) toCopy.add(listModel.header);
					if (listModel.button != null) toCopy.add(listModel.button);
					if (listModel.subtext != null) toCopy.add(listModel.subtext);
					if (listModel.countBubble != null) toCopy.add(listModel.countBubble);
					if (listModel.image != null) toCopy.add(listModel.image);
					toCopy.add(listModel.component);
				}
				// else: list form
			}
		}

		if (toCopy.size() == 0)
		{
			// nothing to copy
			return null;
		}
		return new FormElementCopyCommand(toCopy.toArray(new IPersist[toCopy.size()]));
	}

	@Override
	protected boolean calculateEnabled()
	{
		return createCopyCommand(getSelectedObjects()) != null;
	}

	@Override
	public void run()
	{
		// Run copy command immediately, not via command stack, editor should not get dirty from it
		Command command = createCopyCommand(getSelectedObjects());
		if (command != null && command.canExecute())
		{
			command.execute();
		}
	}
}