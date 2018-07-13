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
import java.util.List;
import java.util.Map;
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
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.util.HTTPUtils;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.NGClientEntryFilter;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormLayoutStructureGenerator;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

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
				((HttpServletResponse)response).setCharacterEncoding("UTF-8");
				PrintWriter w = response.getWriter();
				Set<String> formScripts = new HashSet<String>();
				formScripts.add("js/servoy-components.js?x=" + System.currentTimeMillis());
//				formScripts.add("solutions/" + solution + "/forms/" + form + ".js");
				Map<String, Object> variableSubstitution = new HashMap<String, Object>();
				variableSubstitution.put("orientation", Integer.valueOf(0)); // fs.getSolution().getTextOrientation()
				variableSubstitution.put("contextPort", Integer.valueOf(ApplicationServerRegistry.get().getWebServerPort()));
				List<String> css = new ArrayList<String>();
				css.add("css/servoy.css");
				SpecProviderState componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
				for (PackageSpecification<WebLayoutSpecification> entry : componentsSpecProviderState.getLayoutSpecifications().values())
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
				for (PackageSpecification<WebObjectSpecification> entry : componentsSpecProviderState.getWebObjectSpecifications().values())
				{
					if (entry.getCssDesignLibrary() != null)
					{
						css.addAll(entry.getCssDesignLibrary());
					}
					if (entry.getJsDesignLibrary() != null)
					{
						formScripts.addAll(entry.getJsDesignLibrary());
					}
				}
				IndexPageEnhancer.enhance(getClass().getResource("editor-content.html"), css, formScripts, null, variableSubstitution, w, null,
					NGClientEntryFilter.CONTRIBUTION_ENTRY_FILTER);
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

			String containerID = httpServletRequest.getParameter("cont");
			PrintWriter w = response.getWriter();
			((HttpServletResponse)response).setContentType("text/html");
			if (containerID != null)
			{
				int id = Utils.getAsInteger(containerID);
				if (id > 0)
				{
					try
					{
						IPersist container = flattenedForm.findChild(
							((EclipseRepository)ServoyModel.getDeveloperRepository()).getUUIDForElementId(id, id, -1, -1, null));
						if (container instanceof LayoutContainer)
						{
							FormLayoutGenerator.generateFormStartTag(w, flattenedForm, solutionAndFormName.getRight(), false, true);
							FormLayoutStructureGenerator.generateLayoutContainer((LayoutContainer)container, flattenedForm, fs, w, true,
								FormElementHelper.INSTANCE);
							FormLayoutGenerator.generateFormEndTag(w, true);
						}
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}

				}
			}
			else if (flattenedForm.isResponsiveLayout())
			{
				FormLayoutStructureGenerator.generateLayout(flattenedForm, solutionAndFormName.getRight(), fs, w, true);
			}
			else
			{
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
