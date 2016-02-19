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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.osgi.service.prefs.BackingStoreException;
import org.sablo.specification.NGPackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HTTPUtils;

/**
 * Filter for designer editor
 * @author gboros
 */
@WebFilter(urlPatterns = { "/designer/*" })
@SuppressWarnings("nls")
public class DesignerFilter implements Filter
{
	private static final List<String> IGNORE_PACKAGE_LIST = Arrays.asList(new String[] { "servoycore" });
	private static final List<String> IGNORE_COMPONENT_LIST = Arrays.asList(
		new String[] { "servoydefault-checkgroup", FormElement.ERROR_BEAN, "servoycore-navigator", "servoydefault-radiogroup", "servoydefault-htmlview", "colorthefoundset" });

	public static final String PREFERENCE_KEY = "com.servoy.eclipse.designer.rfb.palette.order";
	@SuppressWarnings("nls")
	private final static String[] layoutTypeNames = { "Absolute-Layout", "Responsive-Layout" };


	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
	{
		try
		{
			HttpServletRequest request = (HttpServletRequest)servletRequest;
			String uri = request.getRequestURI();
			String layoutType = request.getParameter("layout");
			String formName = request.getParameter("formName");
			if (uri != null && uri.endsWith("palette"))
			{
				WebComponentSpecProvider provider = WebComponentSpecProvider.getInstance();

				((HttpServletResponse)servletResponse).setContentType("application/json");

				if (servletResponse instanceof HttpServletResponse) HTTPUtils.setNoCacheHeaders((HttpServletResponse)servletResponse);

				try
				{
					JSONWriter jsonWriter = new JSONWriter(servletResponse.getWriter());
					jsonWriter.array();


					//Step 1: create a list with all keys containing first the layout and then the component packages
					ArrayList<String> orderedKeys = new ArrayList<String>();
					orderedKeys.add("Templates");
					Set<String> keySet = provider.getLayoutSpecifications().keySet();
					for (String key : keySet)
					{
						orderedKeys.add(key);
					}

					//Create a list with a default sort where the default components are always first
					keySet = provider.getWebComponentSpecifications().keySet();

					ArrayList<String> componentPackages = new ArrayList<String>();
					for (String key : keySet)
					{
						if (IGNORE_PACKAGE_LIST.contains(key)) continue;
						componentPackages.add(key);
					}
					Collections.sort(componentPackages, new Comparator<String>()
					{
						@Override
						public int compare(String pkg1, String pkg2)
						{
							if (pkg1.equals("servoydefault")) return -1;
							if (pkg2.equals("servoydefault")) return 1;
							return pkg1.compareTo(pkg2);
						}
					});

					for (int i = 0; i < componentPackages.size(); i++)
					{
						if (!orderedKeys.contains(componentPackages.get(i))) orderedKeys.add(componentPackages.get(i));
					}

					IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode("com.servoy.eclipse.designer.rfb");
					prefs.sync();
					String json = prefs.get(PREFERENCE_KEY, "{}");
					JSONObject jsonObject = new JSONObject(json);
					JSONArray ordered = null;
					if (jsonObject.has(layoutType)) ordered = (JSONArray)jsonObject.get(layoutType);

					final ArrayList<String> orderPreference = new ArrayList<String>();
					if (ordered != null)
					{
						for (int i = 0; i < ordered.length(); i++)
						{
							if (ordered.get(i) instanceof String) orderPreference.add(ordered.getString(i));
						}
					}
					// orderPreference array has to contain all keys, otherwise sorting does not work correctly
					for (String key : orderedKeys)
					{
						if (!orderPreference.contains(key)) orderPreference.add(key);
					}

					if (orderPreference.size() > 0)
					{
						Collections.sort(orderedKeys, new Comparator<String>()
						{
							@Override
							public int compare(String pkg1, String pkg2)
							{
								if (orderPreference.indexOf(pkg1) > -1 && orderPreference.indexOf(pkg2) > -1)
									return orderPreference.indexOf(pkg1) - orderPreference.indexOf(pkg2);
								else
								{
									if (orderPreference.indexOf(pkg1) > 0) return -1;
									else if (orderPreference.indexOf(pkg2) > 0) return 1;
								}
								return pkg1.compareTo(pkg2);
							}
						});
					}
					for (String key : orderedKeys)
					{
						boolean startedArray = false;
						if ((provider.getLayoutSpecifications().containsKey(key) &&
							isAccesibleInLayoutType(provider.getLayoutSpecifications().get(key), layoutType)))
						{
							NGPackageSpecification<WebLayoutSpecification> pkg = provider.getLayoutSpecifications().get(key);
							jsonWriter.object();
							jsonWriter.key("packageName").value(pkg.getPackageName());
							jsonWriter.key("packageDisplayname").value(pkg.getPackageDisplayname());
							jsonWriter.key("components");
							jsonWriter.array();
							startedArray = true;
						}
						else if (provider.getWebComponentSpecifications().containsKey(key) &&
							isAccesibleInLayoutType(provider.getWebComponentSpecifications().get(key), layoutType))
						{
							NGPackageSpecification<WebObjectSpecification> pkg = provider.getWebComponentSpecifications().get(key);
							jsonWriter.object();
							jsonWriter.key("packageName").value(pkg.getPackageName());
							jsonWriter.key("packageDisplayname").value(pkg.getPackageDisplayname());
							jsonWriter.key("components");
							jsonWriter.array();
							startedArray = true;
						}
						else if (key.equals("Templates"))
						{
							List<IRootObject> templates = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(
								IRepository.TEMPLATES);
							if (templates.size() > 0)
							{
								jsonWriter.object();
								jsonWriter.key("packageName").value("Templates");
								jsonWriter.key("packageDisplayname").value("Templates");
								jsonWriter.key("components");
								jsonWriter.array();
								for (IRootObject iRootObject : templates)
								{
									if (iRootObject instanceof StringResource)
									{
										StringResource stringResource = (StringResource)iRootObject;
										String content = stringResource.getContent();
										JSONObject templateJSON = new JSONObject(content);
										if ((layoutType.equals(layoutTypeNames[0]) && (!templateJSON.has(Template.PROP_LAYOUT)) ||
											(templateJSON.has(Template.PROP_LAYOUT) && templateJSON.get(Template.PROP_LAYOUT).equals(layoutType))))
										{
											jsonWriter.object();
											jsonWriter.key("name").value(iRootObject.getName());
											jsonWriter.key("componentType").value("template");
											jsonWriter.key("displayName").value(iRootObject.getName());
											jsonWriter.key("tagName").value("<div  style=\"border-style: dotted;\"></div>");
											Map<String, Object> model = new HashMap<String, Object>();
											HashMap<String, Number> size = new HashMap<String, Number>();
											size.put("height", Integer.valueOf(80));
											size.put("width", Integer.valueOf(80));
											model.put("size", size);
											jsonWriter.key("model").value(new JSONObject(model));
											jsonWriter.key("icon").value("rfb/angular/js/modules/toolbaractions/icons/template.gif");
											jsonWriter.endObject();
										}
									}
								}
								jsonWriter.endArray();
								jsonWriter.endObject();
							}
						}
						if (startedArray && provider.getLayoutSpecifications().containsKey(key))
						{
							NGPackageSpecification<WebLayoutSpecification> entry = provider.getLayoutSpecifications().get(key);

							for (WebLayoutSpecification spec : entry.getSpecifications().values())
							{
								jsonWriter.object();
								jsonWriter.key("name").value(spec.getName());
								if (spec.getConfig() != null)
								{
									String layoutName = new JSONObject((String)spec.getConfig()).optString("layoutName", null);
									if (layoutName != null)
									{
										jsonWriter.key("layoutName").value(layoutName);
									}
									else jsonWriter.key("layoutName").value(spec.getName());
								}
								else jsonWriter.key("layoutName").value(spec.getName());
								jsonWriter.key("componentType").value("layout");
								jsonWriter.key("displayName").value(spec.getDisplayName());
								JSONObject config = spec.getConfig() instanceof String ? new JSONObject((String)spec.getConfig()) : null;
								if (config == null)
								{
									jsonWriter.key("tagName").value("<div style='border-style: dotted;'></div>"); //TODO is tagname configurable by the spec
								}
								else
								{
									jsonWriter.key("tagName").value(createLayoutDiv(config, new StringBuilder()).toString());
								}
								Map<String, Object> model = new HashMap<String, Object>();
								PropertyDescription pd = spec.getProperty("size");
								if (pd != null && pd.getDefaultValue() != null)
								{
									model.put("size", pd.getDefaultValue());
								}
								else
								{
									HashMap<String, Number> size = new HashMap<String, Number>();
									size.put("height", Integer.valueOf(20));
									size.put("width", Integer.valueOf(100));
									model.put("size", size);
								}
								jsonWriter.key("model").value(new JSONObject(model));
								if (spec.getIcon() != null)
								{
									jsonWriter.key("icon").value(spec.getIcon());
								}
								if (spec.getPreview() != null)
								{
									jsonWriter.key("preview").value(spec.getPreview());
								}
								jsonWriter.key("topContainer").value(spec.isTopContainer());


								jsonWriter.endObject();
							}
						}
						if (startedArray && provider.getWebComponentSpecifications().containsKey(key))
						{
							NGPackageSpecification<WebObjectSpecification> pkg = provider.getWebComponentSpecifications().get(key);
							Collection<WebObjectSpecification> webComponentSpecsCollection = pkg.getSpecifications().values();
							if ("servoydefault".equals(key))
							{
								ArrayList<WebObjectSpecification> webComponentSpecsCollectionEx = new ArrayList<WebObjectSpecification>(
									webComponentSpecsCollection);
								webComponentSpecsCollectionEx.addAll(provider.getWebComponentSpecifications().get("servoycore").getSpecifications().values());
								webComponentSpecsCollection = webComponentSpecsCollectionEx;
							}
							for (WebObjectSpecification spec : webComponentSpecsCollection)
							{
								if (!IGNORE_COMPONENT_LIST.contains(spec.getName()))
								{
									jsonWriter.object();
									jsonWriter.key("name").value(spec.getName());
									jsonWriter.key("componentType").value("component");
									jsonWriter.key("displayName").value(spec.getDisplayName());
									FlattenedSolution fl = ServoyModelFinder.getServoyModel().getActiveProject().getEditingFlattenedSolution();
									Form form = fl.getForm(formName);
									Map<String, Object> model = new HashMap<String, Object>();
									if (form.isResponsiveLayout())
									{
										WebComponent obj = (WebComponent)fl.getSolution().getChangeHandler().createNewObject(form, IRepository.WEBCOMPONENTS);
										obj.setName(spec.getName());
										obj.setTypeName(spec.getName());
										FormElement formElement = FormElementHelper.INSTANCE.getFormElement(obj, fl, null, false);
										StringWriter stringWriter = new StringWriter();
										PrintWriter printWriter = new PrintWriter(stringWriter);
										FormLayoutGenerator.generateFormElement(printWriter, formElement, form, true, false);
										jsonWriter.key("tagName").value(stringWriter.toString());
										model.put("componentName", formElement.getName());
									}
									else jsonWriter.key("tagName").value(FormTemplateGenerator.getTagName(spec.getName()));
									PropertyDescription pd = spec.getProperty("size");
									Set<String> allPropertiesNames = spec.getAllPropertiesNames();
									for (String string : allPropertiesNames)
									{
										if (spec.getProperty(string) != null && spec.getProperty(string).getDefaultValue() != null)
										{
											Object defaultValue = spec.getProperty(string).getDefaultValue();
											if (defaultValue != null) model.put(string, defaultValue);
										}
									}
									if (pd != null && pd.getDefaultValue() != null)
									{
										model.put("size", pd.getDefaultValue());
									}
									if (spec.getProperty("enabled") != null)
									{
										model.put("enabled", Boolean.TRUE);
									}
									if (spec.getProperty("editable") != null)
									{
										model.put("editable", Boolean.TRUE);
									}

									model.put("visible", Boolean.TRUE);

									jsonWriter.key("model").value(new JSONObject(model));
									if (spec.getIcon() != null)
									{
										jsonWriter.key("icon").value(spec.getIcon());
									}
									jsonWriter.key("types").value(new JSONArray(getPalleteTypeNames(spec)));
									jsonWriter.endObject();
								}
							}
						}
						if (startedArray)
						{
							jsonWriter.endArray();
							jsonWriter.endObject();
						}
					}

					jsonWriter.endArray();
				}
				catch (JSONException ex)
				{
					Debug.error("Exception during designe palette generation", ex);
				}
				catch (BackingStoreException e)
				{
					Debug.error(e);
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}

				return;
			}

			filterChain.doFilter(request, servletResponse);
		}
		catch (RuntimeException e)
		{
			Debug.error(e);
			throw e;
		}
	}


	private boolean isAccesibleInLayoutType(NGPackageSpecification< ? > pkg, String layoutType)
	{
		if (pkg.getManifest() != null && pkg.getManifest().getMainAttributes() != null &&
			Boolean.valueOf(pkg.getManifest().getMainAttributes().getValue(layoutType)).booleanValue()) return true;
		if (noLayoutTypeSpecified(pkg.getManifest())) return true;
		return false;
	}

	private boolean noLayoutTypeSpecified(Manifest mf)
	{
		for (String layoutTypeName : layoutTypeNames)
		{
			if (mf != null && mf.getMainAttributes() != null && mf.getMainAttributes().getValue(layoutTypeName) != null) return false;
		}
		return true;
	}


	/**
	 * @param config
	 * @param sb
	 * @param specifications
	 * @throws JSONException
	 */
	protected StringBuilder createLayoutDiv(JSONObject config, StringBuilder sb) throws JSONException
	{
		sb.append("<div style='border-style: dotted;' "); // TODO tagname from spec?
		Iterator keys = config.keys();
		while (keys.hasNext())
		{
			String key = (String)keys.next();
			if (key.equals("children") || key.equals("layoutName")) continue;
			String value = config.getString(key);
			sb.append(key);
			sb.append("='");
			sb.append(value);
			sb.append("' ");
		}
		sb.append(">");
		JSONArray children = config.optJSONArray("children");
		if (children != null)
		{
			for (int i = 0; i < children.length(); i++)
			{
				JSONObject jsonObject = children.getJSONObject(i);
				JSONObject childModel = jsonObject.optJSONObject("model");
				if (childModel != null)
				{
					createLayoutDiv(childModel, sb);
				}
			}
		}
		return sb.append("</div>");
	}

	private List<JSONObject> getPalleteTypeNames(WebObjectSpecification spec)
	{
		List<JSONObject> result = new ArrayList<JSONObject>();
		Map<String, PropertyDescription> properties = spec.getProperties();
		for (PropertyDescription propertyDescription : properties.values())
		{
			Object configObject = propertyDescription.getConfig();
			if (configObject instanceof JSONObject && Boolean.TRUE.equals(((JSONObject)configObject).opt(FormElement.DROPPABLE)))
			{
				if (PropertyUtils.isCustomJSONProperty(propertyDescription.getType()))
				{
					JSONObject json = new JSONObject();
					json.put("type", PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(propertyDescription.getType()));
					json.put("propertyName", propertyDescription.getName());
					result.add(json);
				}
			}
		}
		return result;
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}
}
