/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.designer.webpackage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Utils;

/**
 * @author gganea
 *
 */
@WebServlet(urlPatterns = { "/wpm/*" })
public class WebPackageManagerResourcesServlet extends HttpServlet
{
	private static final long serialVersionUID = 2361198278530167217L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String path = req.getPathInfo();
		if ("/".equals(path)) path = "/index.html";
		Path normalizedPath = Paths.get(req.getServletPath() + "/browser" + path).normalize();
		if (normalizedPath.startsWith("/wpm/"))
		{
			URL res = getClass().getResource(normalizedPath.toString().replace('\\', '/'));
			if (res != null)
			{
				URLConnection uc = res.openConnection();
				resp.setContentLength(uc.getContentLength());
				resp.setContentType(MimeTypes.guessContentTypeFromName(path));
				InputStream in = uc.getInputStream();
				ServletOutputStream outputStream = resp.getOutputStream();
				Utils.streamCopy(in, outputStream);
				outputStream.flush();
				Utils.close(in);
			}
			else
			{
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}
		else
		{
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

}
