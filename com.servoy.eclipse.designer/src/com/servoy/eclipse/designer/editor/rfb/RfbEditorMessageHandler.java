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

package com.servoy.eclipse.designer.editor.rfb;

import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.designer.rib.editor.MessageDispatcher;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.util.IMessageHandler;

/**
 * Handle communication with a remote rfb form editor.
 * 
 * @author rgansevles
 *
 */
public class RfbEditorMessageHandler implements IMessageHandler
{
	private final String id;
	private final Form form;

	public RfbEditorMessageHandler(Form form, String id)
	{
		this.form = form;
		this.id = id;
	}

	public String getId()
	{
		return id;
	}

	private void callSomeVoidMethod(boolean b)
	{
	}

	private String callGetFormLayoutGrid()
	{
		return form.getLayoutGrid();
	}

	private void sendMessage(String message)
	{
		MessageDispatcher.INSTANCE.sendMessage(id, message, this);
	}

	@Override
	public void messageReceived(final String message)
	{
		if (message != null)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				@Override
				public void run()
				{
					if (message.startsWith("somemethod:"))
					{
						// example void method
						callSomeVoidMethod(Boolean.parseBoolean(message.substring("somemethod:".length())));
					}
					else if (message.startsWith("<"))
					{
						// message tagged with id, a response is requested
						String[] split = message.split(":");
						String req = message.substring(split[0].length() + 1);
						String response = "unknown request";
						try
						{
							if (req.startsWith("getFormLayoutGrid"))
							{
								response = callGetFormLayoutGrid();
							}
						}
						finally
						{
							sendMessage('>' + split[0].substring(1) + ':' + (response == null ? "" : response));
						}
					}
				}
			});
		}
	}
}
