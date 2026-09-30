/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.designer.rfb.endpoint;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.text.StringEscapeUtils;

import com.servoy.eclipse.ngclient.ui.Activator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HTTPUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Stateless HTTP filter for the form-template render route (SVY-21460).
 *
 * <p>Serves two resources, both statelessly (no clientnr, no solution in the URL, no websocket,
 * no session); the active editing flattened solution is always used and form names are unique:
 * <ul>
 * <li><b>{@code /formtemplate/<formname>.html}</b> - the compiled Angular {@code index.html} with the
 * form-state JSON and the component client-side specs JSON injected inline as
 * {@code <script type="application/json">} blobs, a {@code <link rel="stylesheet" href="stylesheet.css">}
 * and a {@code window.formtemplateName} marker so the Angular route knows which form to render.</li>
 * <li><b>{@code /formtemplate/stylesheet.css}</b> - the active solution's CSS as {@code text/css},
 * produced the design-time way (no running client).</li>
 * </ul>
 *
 * @author Servoy
 */
@WebFilter(urlPatterns = { "/formtemplate/*" })
@SuppressWarnings("nls")
public class FormTemplateHttpEndpoint implements Filter
{
	private static final String FORM_TEMPLATE_PATH = "/formtemplate/";

	private final HeadlessFormTemplateContent content = new HeadlessFormTemplateContent();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		HttpServletRequest httpServletRequest = (HttpServletRequest)request;
		HttpServletResponse httpServletResponse = (HttpServletResponse)response;
		String requestURI = httpServletRequest.getRequestURI();

		int idx = requestURI.indexOf(FORM_TEMPLATE_PATH);
		if (idx < 0)
		{
			chain.doFilter(request, response);
			return;
		}

		String resource = requestURI.substring(idx + FORM_TEMPLATE_PATH.length());
		HTTPUtils.setNoCacheHeaders(httpServletResponse);

		if ("stylesheet.css".equals(resource))
		{
			writeStyleSheet(httpServletResponse);
			return;
		}
		if (resource.endsWith(".html"))
		{
			String formName = resource.substring(0, resource.length() - ".html".length());
			writeFormTemplatePage(httpServletResponse, formName);
			return;
		}

		chain.doFilter(request, response);
	}

	private void writeStyleSheet(HttpServletResponse response) throws IOException
	{
		response.setContentType("text/css");
		response.setCharacterEncoding("UTF-8");
		PrintWriter w = response.getWriter();
		String css = content.getSolutionStyleSheet();
		if (css != null) w.write(css);
		w.flush();
	}

	private void writeFormTemplatePage(HttpServletResponse response, String formName) throws IOException
	{
		File indexFile = getAngularIndexFile();
		if (indexFile == null || !indexFile.exists())
		{
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Angular resources are not built yet");
			return;
		}

		String indexHtml;
		try
		{
			String formState = content.getFormDataJS(formName);
			if (formState == null)
			{
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Form not found: " + formName);
				return;
			}
			// spec assembly is best-effort: a component with a missing/unavailable spec must not turn into an HTTP 500
			String specs = content.getComponentSpecsJSON(formName);

			indexHtml = new String(Files.readAllBytes(indexFile.toPath()), StandardCharsets.UTF_8);
			String injected = buildInjection(formName, formState, specs);
			indexHtml = insertBeforeBodyEnd(indexHtml, injected);
		}
		catch (IOException | RuntimeException e)
		{
			Debug.error("Can't generate form template page for form '" + formName + "'", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Can't render form template: " + formName);
			return;
		}

		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		PrintWriter w = response.getWriter();
		w.write(indexHtml);
		w.flush();
	}

	private String buildInjection(String formName, String formState, String specs)
	{
		StringBuilder sb = new StringBuilder(formState.length() + (specs != null ? specs.length() : 0) + 512);
		sb.append("<link rel=\"stylesheet\" href=\"" + FORM_TEMPLATE_PATH + "stylesheet.css\">\n");
		sb.append("<script>window.formtemplateName = \"").append(StringEscapeUtils.escapeEcmaScript(formName)).append("\";</script>\n");
		sb.append("<script id=\"svy-formtemplate-formstate\" type=\"application/json\">").append(escapeForInlineJson(formState)).append("</script>\n");
		if (specs != null)
		{
			sb.append("<script id=\"svy-formtemplate-specs\" type=\"application/json\">").append(escapeForInlineJson(specs)).append("</script>\n");
		}
		return sb.toString();
	}

	/**
	 * Escapes a JSON string so it is safe to embed inside an inline
	 * {@code <script type="application/json">} block (only the {@code <} of a closing tag matters).
	 */
	private String escapeForInlineJson(String json)
	{
		return json.replace("<", "\\u003c");
	}

	private String insertBeforeBodyEnd(String html, String injected)
	{
		int bodyEnd = html.lastIndexOf("</body>");
		if (bodyEnd >= 0)
		{
			return html.substring(0, bodyEnd) + injected + html.substring(bodyEnd);
		}
		// no </body>: just append
		return html + injected;
	}

	private File getAngularIndexFile()
	{
		File projectFolder = Activator.getInstance().getSolutionProjectFolder();
		if (projectFolder == null) return null;
		return new File(new File(projectFolder, "dist/app/browser"), "index.html");
	}

	@Override
	public void destroy()
	{
	}
}
