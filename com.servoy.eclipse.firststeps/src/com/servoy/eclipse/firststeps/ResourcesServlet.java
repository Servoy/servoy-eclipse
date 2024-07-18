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

package com.servoy.eclipse.firststeps;

import static java.lang.Class.forName;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.servoy.eclipse.firststeps.ui.actions.IAction;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Utils;

@WebServlet("/firststeps/*")
public class ResourcesServlet extends HttpServlet
{
	private static final Map<String,String> allowedClasses = Map.of("com.servoy.eclipse.firststeps.ui.actions.CloseDialog", "com.servoy.eclipse.firststeps.ui.actions.CloseDialog",
																	"com.servoy.eclipse.firststeps.ui.actions.NewForm", "com.servoy.eclipse.firststeps.ui.actions.NewForm",
																	"com.servoy.eclipse.firststeps.ui.actions.OpenURL", "com.servoy.eclipse.firststeps.ui.actions.OpenURL");
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String path = Paths.get(req.getPathInfo()).normalize().toString().replace('\\', '/');
		
		if(path.startsWith("/action/"))
		{
			int idxClassNameEnd = path.indexOf('/', 8);
			if(idxClassNameEnd == -1)
			{
				idxClassNameEnd = path.length();
			}

			String identifier = path.substring(8, idxClassNameEnd);
			try
			{
				String className = allowedClasses.get(identifier);
				if (className != null) {
					Class<?> clazz = forName(className);
					Object actionInstance = clazz.getDeclaredConstructor().newInstance();
					if(actionInstance instanceof IAction)
					{
						String argument = path.substring(idxClassNameEnd);
						((IAction)actionInstance).run(argument);
					}
				}
			}
			catch(Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		else
		{
			
			path = Paths.get(req.getServletPath()).normalize().toString().replace('\\', '/') + path;
			if (path.startsWith("/firststeps/")) {
				URL res = getClass().getResource(path);
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
}
