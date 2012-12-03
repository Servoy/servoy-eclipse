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
package com.servoy.eclipse.designer.editor;


import java.util.Collection;
import java.util.Iterator;

import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;

/**
 * Base graphical edit part for element groups.
 * 
 * @author rgansevles
 * 
 */
public abstract class BaseGroupGraphicalEditPart extends AbstractGraphicalEditPart implements IPersistChangeListener
{
	private final IApplication application;

	private final BaseVisualFormEditor editorPart;
	private final Form form;

	public BaseGroupGraphicalEditPart(IApplication application, BaseVisualFormEditor editorPart, Form form, FormElementGroup group)
	{
		this.application = application;
		this.editorPart = editorPart;
		this.form = form;
		setModel(group);
	}

	/**
	 * @return the application
	 */
	public IApplication getApplication()
	{
		return application;
	}

	/**
	 * @return the form
	 */
	public Form getForm()
	{
		return form;
	}

	/**
	 * @return the editorPart
	 */
	public BaseVisualFormEditor getEditorPart()
	{
		return editorPart;
	}

	protected abstract void doRefresh();

	@Override
	public void activate()
	{
		// listen to changes to the elements
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.addPersistChangeListener(false, this);

		super.activate();
	}

	@Override
	public void deactivate()
	{
		// stop listening to changes to the elements
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.removePersistChangeListener(false, this);

		super.deactivate();
	}

	// If the form changed width we have to refresh
	public void persistChanges(Collection<IPersist> changes)
	{
		String groupId = getGroup().getGroupID();
		boolean found = false;
		Iterator<IPersist> iterator = changes.iterator();
		while (groupId != null && !found && iterator.hasNext())
		{
			IPersist persist = iterator.next();
			found = persist instanceof IFormElement && groupId.equals(((IFormElement)persist).getGroupID());
		}
		if (found)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					if (getParent() != null) doRefresh();
				}
			});
		}
	}

	public FormElementGroup getGroup()
	{
		return (FormElementGroup)getModel();
	}
}
