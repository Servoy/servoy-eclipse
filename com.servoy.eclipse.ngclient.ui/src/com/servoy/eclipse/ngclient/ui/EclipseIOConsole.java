/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.eclipse.ngclient.ui;

import java.io.IOException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * @author jcompagner
 * @since 2021.09
 *
 */
public class EclipseIOConsole extends IOConsole implements IConsole
{

	/**
	 * @param name
	 * @param consoleType
	 * @param imageDescriptor
	 */
	public EclipseIOConsole(String name, String consoleType, ImageDescriptor imageDescriptor)
	{
		super(name, consoleType, imageDescriptor);
	}

	@Override
	public StringOutputStream outputStream()
	{
		IOConsoleOutputStream outputStream = super.newOutputStream();
		return new StringOutputStream()
		{

			@Override
			public void write(CharSequence chars) throws IOException
			{
				outputStream.write(chars);
			}

			@Override
			public void close() throws IOException
			{
				outputStream.close();
			}
		};
	}


}
