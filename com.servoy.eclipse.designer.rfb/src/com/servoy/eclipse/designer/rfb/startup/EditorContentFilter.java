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
import org.sablo.specification.NGPackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormLayoutStructureGenerator;
import com.servoy.j2db.util.HTTPUtils;
import com.servoy.j2db.util.Pair;

/**
 * @author jcompagner
 *
 */
@WebFilter(urlPatterns = { "/*" })
@SuppressWarnings("nls")
public class EditorContentFilter implements Filter
{
	private static final String DESIGNER_TEMPLATE_PATH = "/designertemplate/";


	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		HttpServletRequest httpServletRequest = (HttpServletRequest)request;
		String requestURI = httpServletRequest.getRequestURI();
		if (requestURI.endsWith("editor-content.html"))
		{
			String solution = httpServletRequest.getParameter("s");
			if (solution == null)
			{
				ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
				Solution activeSolution = activeProject.getSolution();
				solution = activeSolution.getName();
			}

			String form = httpServletRequest.getParameter("f");

			if (solution != null && form != null)
			{
				((HttpServletResponse)response).setContentType("text/html");
				PrintWriter w = response.getWriter();
				Set<String> formScripts = new HashSet<String>();
				formScripts.add("js/servoy-components.js?x=" + System.currentTimeMillis());
//				formScripts.add("solutions/" + solution + "/forms/" + form + ".js");
				HashMap<String, String> variableSubstitution = new HashMap<String, String>();
				variableSubstitution.put("orientation", String.valueOf(0)); // fs.getSolution().getTextOrientation()
				ArrayList<String> css = new ArrayList<String>();
				css.add("css/servoy.css");
				for (NGPackageSpecification<WebLayoutSpecification> entry : WebComponentSpecProvider.getInstance().getLayoutSpecifications().values())
				{
					if (entry.getCssDesignLibrary() != null)
					{
						css.addAll(entry.getCssDesignLibrary());
					}
					if (entry.getJsDesignLibrary() != null)
					{
						formScripts.addAll(entry.getJsDesignLibrary());
					}
					if (entry.getCssClientLibrary() != null)
					{
						css.addAll(entry.getCssClientLibrary());
					}
					if (entry.getJsClientLibrary() != null)
					{
						formScripts.addAll(entry.getJsClientLibrary());
					}
				}
				IndexPageEnhancer.enhance(getClass().getResource("editor-content.html"), httpServletRequest.getContextPath(), css, formScripts,
					variableSubstitution, w, null);
				w.flush();
				return;
			}
		}
		else if (requestURI.startsWith(DESIGNER_TEMPLATE_PATH))
		{
			Pair<String, String> solutionAndFormName = getSolutionAndFormNameFromURI(requestURI);
			ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solutionAndFormName.getLeft());
			FlattenedSolution fs = servoyProject.getEditingFlattenedSolution();
			Form flattenedForm = fs.getFlattenedForm(fs.getForm(solutionAndFormName.getRight()));
			HTTPUtils.setNoCacheHeaders((HttpServletResponse)response);

			PrintWriter w = response.getWriter();
			if (flattenedForm.isResponsiveLayout())
			{
				((HttpServletResponse)response).setContentType("text/html");
				FormLayoutStructureGenerator.generateLayout(flattenedForm, solutionAndFormName.getRight(), new ServoyDataConverterContext(fs), w, true);
			}
			else
			{
				((HttpServletResponse)response).setContentType("text/html");
				FormLayoutGenerator.generateRecordViewForm(w, flattenedForm, solutionAndFormName.getRight(), new ServoyDataConverterContext(fs), true);
			}
			w.flush();
			return;

		}
		chain.doFilter(request, response);
	}

	private Pair<String, String> getSolutionAndFormNameFromURI(String uri)
	{
		int solutionIndex = uri.indexOf(DESIGNER_TEMPLATE_PATH);
		if (solutionIndex >= 0)
		{
			int formIndex = uri.indexOf("/", solutionIndex + DESIGNER_TEMPLATE_PATH.length() + 1);
			String solutionName = uri.substring(solutionIndex + DESIGNER_TEMPLATE_PATH.length(), formIndex);
			String formName = uri.substring(formIndex + 1, uri.lastIndexOf(".html"));
			return new Pair<String, String>(solutionName, formName);
		}
		return null;
	}

	@Override
	public void destroy()
	{
	}

}
