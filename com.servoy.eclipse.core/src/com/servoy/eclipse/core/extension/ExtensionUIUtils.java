/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.core.extension;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import com.servoy.eclipse.core.Activator;
import com.servoy.extension.Message;

/**
 * Static utility methods for using extensions inside Eclipse UI.
 * @author acostescu
 */
public class ExtensionUIUtils
{

	/**
	 * Translates a set of Messages into a IStatus. Uses the message and severity in order to do that.
	 * @param the description of the created aggregate MultiStatus. (will be main text in an ErrorDialog for example)
	 */
	public static MultiStatus translateMessagesToStatus(String mainDescription, Message msgs[])
	{
		MultiStatus details = new MultiStatus(Activator.PLUGIN_ID, 1, mainDescription, null);
		for (Message msg : msgs)
		{
			details.add(translateMessageToStatus(msg));
		}
		return details;
	}

	/**
	 * Translates a Message into a IStatus. Uses the message and severity in order to do that.
	 */
	public static IStatus translateMessageToStatus(Message msg)
	{
		return new Status(translateMessageToStatusSeverity(msg.severity), Activator.PLUGIN_ID, 1, msg.message, null);
	}

	/**
	 * Translates severity levels between Message and IStatus.
	 * @param msgSeverity a severity value in context of a Message.
	 * @return matching IStatus severity.
	 */
	public static int translateMessageToStatusSeverity(int msgSeverity)
	{
		int translated;
		switch (msgSeverity)
		{
			case Message.ERROR :
				translated = IStatus.ERROR;
				break;
			case Message.WARNING :
				translated = IStatus.WARNING;
				break;
			case Message.INFO :
			default :
				translated = IStatus.INFO;
				break;
		}
		return translated;
	}

}
