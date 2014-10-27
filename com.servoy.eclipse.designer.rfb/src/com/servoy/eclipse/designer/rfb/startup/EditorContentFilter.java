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

package com.servoy.eclipse.designer.rfb.startup;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sablo.IndexPageEnhancer;

/**
 * @author jcompagner
 *
 */
@WebFilter(urlPatterns = { "/*" })
@SuppressWarnings("nls")
public class EditorContentFilter implements Filter
{
	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		HttpServletRequest httpServletRequest = (HttpServletRequest)request;
		if (httpServletRequest.getRequestURI().endsWith("editor-content.html"))
		{
			String solution = httpServletRequest.getParameter("s");
			String form = httpServletRequest.getParameter("f");
			if (solution != null && form != null)
			{
				((HttpServletResponse)response).setContentType("text/html");
				PrintWriter w = response.getWriter();
				Set<String> formScripts = new HashSet<String>();
				formScripts.add("solutions/" + solution + "/forms/" + form + ".js");
				HashMap<String, String> variableSubstitution = new HashMap<String, String>();
				variableSubstitution.put("orientation", String.valueOf(0)); // fs.getSolution().getTextOrientation()
				ArrayList<String> css = new ArrayList<String>();
				css.add("css/servoy.css");
				IndexPageEnhancer.enhance(getClass().getResource("editor-content.html"), httpServletRequest.getContextPath(), css, formScripts,
					variableSubstitution, w);
				w.flush();
				return;
			}
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy()
	{
	}

}
