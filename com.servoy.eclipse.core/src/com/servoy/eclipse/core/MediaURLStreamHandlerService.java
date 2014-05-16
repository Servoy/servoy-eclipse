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
package com.servoy.eclipse.core;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.service.url.AbstractURLStreamHandlerService;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.Media;

/**
 * @author jcompagner
 * 
 */
public class MediaURLStreamHandlerService extends AbstractURLStreamHandlerService
{

	public static final String PROTOCOL = "media";
	private MediaURLStreamHandler mediaHandler;

	/**
	 * @see org.osgi.service.url.AbstractURLStreamHandlerService#openConnection(java.net.URL)
	 */
	@Override
	public URLConnection openConnection(URL u) throws IOException
	{
		if (mediaHandler == null)
		{
			mediaHandler = new MediaURLStreamHandler()
			{
				/**
				 * @see com.servoy.j2db.MediaURLStreamHandler#getMedia(java.lang.String, com.servoy.j2db.IServiceProvider)
				 */
				@Override
				public Media getMedia(String name, IServiceProvider application)
				{
					Media m = super.getMedia(name, application);
					if (m == null)
					{
						m = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getMedia(name);
					}
					return m;
				}
			};
		}
		return mediaHandler.openConnection(u);
	}
}
