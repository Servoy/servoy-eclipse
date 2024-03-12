/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import static com.servoy.j2db.server.ngclient.AngularIndexPageWriter.addcontentSecurityPolicyHeader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.sablo.security.ContentSecurityPolicyConfig;

import com.servoy.j2db.server.ngclient.AngularIndexPageWriter;
import com.servoy.j2db.server.ngclient.NGLocalesFilter;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author jcomp
 *
 */
@WebFilter(urlPatterns = { "/*" }, dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.FORWARD })
public class IndexPageFilter implements Filter
{
	public static final String SOLUTIONS_PATH = "/solution/";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		StatelessLoginHandler.init();
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException
	{
		HttpServletRequest request = (HttpServletRequest)servletRequest;
		HttpServletResponse response = (HttpServletResponse)servletResponse;
		request.getSession();
		String requestURI = request.getRequestURI();
		if (requestURI.toLowerCase().endsWith("/index.html") &&
			(requestURI.toLowerCase().contains("rfb/angular2") || requestURI.toLowerCase().contains("/solution/")) && WebPackagesListener.isBuildRunning())
		{
			String indexHtml = Utils.getURLContent(Activator.getInstance().getBundle().getEntry("/resources/loadingclient.html"));
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html");
			response.setContentLengthLong(indexHtml.length());
			response.getWriter().write(indexHtml);

			return;
		}
		File distFolder = null;
		File indexFile = null;
		File projectFolder = Activator.getInstance().getSolutionProjectFolder();
		if (projectFolder != null)
		{
			distFolder = new File(projectFolder, "dist/app/browser");
			indexFile = new File(distFolder, "index.html");
		}
		String solutionName = getSolutionNameFromURI(Paths.get(requestURI).normalize());
		if (indexFile != null && indexFile.exists())
		{
			if (solutionName != null &&
				(requestURI.endsWith("/") || requestURI.endsWith("/" + solutionName) ||
					requestURI.toLowerCase().endsWith("/index.html")))
			{
				Pair<Boolean, String> showLogin = StatelessLoginHandler.mustAuthenticate(request, response, solutionName);
				if (showLogin.getLeft().booleanValue())
				{
					StatelessLoginHandler.writeLoginPage(request, response, solutionName);
					return;
				}
				if (showLogin.getRight() != null)
				{
					((HttpServletRequest)servletRequest).getSession().setAttribute(StatelessLoginHandler.ID_TOKEN, showLogin.getRight());
				}

				String indexHtml = FileUtils.readFileToString(indexFile, "UTF-8");

				ContentSecurityPolicyConfig contentSecurityPolicyConfig = addcontentSecurityPolicyHeader(request, response, false); // for NG2 remove the unsafe-eval
				AngularIndexPageWriter.writeIndexPage(indexHtml, request, response, solutionName,
					contentSecurityPolicyConfig == null ? null : contentSecurityPolicyConfig.getNonce());
				return;
			}
			else if (solutionName != null && StatelessLoginHandler.handlePossibleCloudRequest(request, response, solutionName))
			{
				return;
			}
			else if (solutionName != null && requestURI.toLowerCase().endsWith("/startup.js"))
			{
				AngularIndexPageWriter.writeStartupJs(request, (HttpServletResponse)servletResponse, solutionName);
				return;
			}
			else if (AngularIndexPageWriter.handleDeeplink(request, (HttpServletResponse)servletResponse))
			{
				return;
			}
			else
			{
				String normalize = Paths.get(requestURI).normalize().toString();
				File file = new File(distFolder, normalize);
				String localeId = request.getParameter("localeid");
				if (requestURI.startsWith("/locales/") && localeId != null && !file.exists())
				{
					//this code is a bit of copy of the NGLocalesFilter that bases it on servlet resources
					String[] locales = NGLocalesFilter.generateLocaleIds(localeId);
					for (String locale : locales)
					{
						file = new File(distFolder, normalize.replace(localeId, locale));
						if (file.exists()) break;
					}
				}
				if (file.exists() && !file.isDirectory() & !file.getName().equals("favicon.ico"))
				{
					String contentType = MimeTypes.guessContentTypeFromName(requestURI);
					if (contentType != null) servletResponse.setContentType(contentType);
					FileUtils.copyFile(file, servletResponse.getOutputStream());
					return;
				}

			}
		}
		else if (solutionName != null)
		{
			// the system is not ready because the TiNG resources are not build/ not build properly
			String indexHtml = Utils.getURLContent(Activator.getInstance().getBundle().getEntry("/resources/buildfailedclient.html"));
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html");
			response.setContentLengthLong(indexHtml.length());
			response.getWriter().write(indexHtml);
			return;
		}
		chain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy()
	{
	}

	private String getSolutionNameFromURI(Path path)
	{
		Path p = path;
		if (p.startsWith("/designer"))
		{
			p = Paths.get(path.toString().substring(9));
		}
		if (p.startsWith(SOLUTIONS_PATH))
		{
			return p.getName(1).toString();
		}
		return null;
	}
}
