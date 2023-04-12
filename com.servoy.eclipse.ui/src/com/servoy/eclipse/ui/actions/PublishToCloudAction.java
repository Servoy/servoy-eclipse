/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.eclipse.ui.actions;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.cloud.PublishHandler;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;

/**
 * @author vidmarian
 *
 */
public class PublishToCloudAction extends Action implements ISelectionChangedListener
{
	private PublishHandler publishHandler = null;

	public PublishToCloudAction(Shell shell)
	{
		publishHandler = new PublishHandler();
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("publish.png"));
		setText(Messages.Cloud_publishToCloud);
		setToolTipText(Messages.Cloud_publishToCloud);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		setEnabled(publishHandler.isEnabled());
	}

	@Override
	public void run()
	{
		try
		{
			publishHandler.execute(null);
			setEnabled(publishHandler.isEnabled());
		}
		catch (ExecutionException e)
		{
			ServoyLog.logError(e);
		}

	}
}
