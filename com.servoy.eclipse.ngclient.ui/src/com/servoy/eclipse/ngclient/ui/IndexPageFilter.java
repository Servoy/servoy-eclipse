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

import java.io.File;
import java.io.IOException;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;

import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Settings;

/**
 * @author jcomp
 *
 */
@WebFilter(urlPatterns = { "/*" }, dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.FORWARD })
public class IndexPageFilter implements Filter
{
	public static final String SOLUTIONS_PATH = "solution/";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException
	{
		File projectFolder = Activator.getInstance().getProjectFolder();
		File distFolder = new File(projectFolder, "dist");
		if (distFolder.exists())
		{
			HttpServletRequest request = (HttpServletRequest)servletRequest;
			String requestURI = request.getRequestURI();
			String solutionName = getSolutionNameFromURI(requestURI);
			if (solutionName != null &&
				(requestURI.endsWith("/") || requestURI.endsWith("/" + solutionName) || requestURI.toLowerCase().endsWith("/index.html")))
			{
				File file = new File(distFolder, "index.html");
				String indexHtml = FileUtils.readFileToString(file, "UTF-8");
				final String path = Settings.getInstance().getProperty("servoy.context.path", request.getContextPath() + '/');
				indexHtml = indexHtml.replace("<base href=\"/\">", "<base href=\"" + path + "\">");
				servletResponse.setCharacterEncoding("UTF-8");
				servletResponse.setContentType("text/html");
				servletResponse.setContentLengthLong(indexHtml.length());
				servletResponse.getWriter().write(indexHtml);
				return;
			}
			else
			{
				File file = new File(distFolder, requestURI);
				if (file.exists())
				{
					String contentType = MimeTypes.guessContentTypeFromName(requestURI);
					if (contentType != null) servletResponse.setContentType(contentType);
					FileUtils.copyFile(file, servletResponse.getOutputStream());
					return;
				}
			}
		}
		chain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy()
	{
	}

	private String getSolutionNameFromURI(String uri)
	{
		int solutionIndex = uri.indexOf(SOLUTIONS_PATH);
		int solutionEndIndex = uri.indexOf("/", solutionIndex + SOLUTIONS_PATH.length() + 1);
		if (solutionEndIndex == -1) solutionEndIndex = uri.length();
		if (solutionIndex >= 0 && solutionEndIndex > solutionIndex)
		{
			String possibleSolutionName = uri.substring(solutionIndex + SOLUTIONS_PATH.length(), solutionEndIndex);
			// skip all names that have a . in them
			if (possibleSolutionName.contains(".")) return null;
			return possibleSolutionName;
		}
		return null;
	}

}
