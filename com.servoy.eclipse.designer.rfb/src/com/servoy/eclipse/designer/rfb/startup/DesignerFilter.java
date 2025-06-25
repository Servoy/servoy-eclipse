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
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Manifest;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.osgi.service.prefs.BackingStoreException;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.util.HTTPUtils;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.rfb.palette.PaletteCommonsHandler;
import com.servoy.eclipse.designer.rfb.palette.PaletteFavoritesHandler;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyDeveloperProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.MenuPropertyType;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

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
 * Filter for designer editor
 * @author gboros
 */
@WebFilter(urlPatterns = { "/designer/*" })
@SuppressWarnings("nls")
public class DesignerFilter implements Filter
{
	private static final List<String> IGNORE_PACKAGE_LIST = Arrays.asList(new String[] { "servoycore" }); // IMPORTANT! all servoycore components (of course except ones in IGNORE_COMPONENT_LIST) WILL be added to servoydefault package in palette (there is custom code for that below)
	private static final List<String> IGNORE_COMPONENT_LIST = Arrays.asList(
		new String[] { "servoydefault-checkgroup", FormElement.ERROR_BEAN, "servoycore-navigator", "servoydefault-radiogroup", "servoydefault-htmlview", "servoycore-defaultLoadingIndicator" });

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
			String layoutType = StringEscapeUtils.escapeHtml4(request.getParameter("layout"));
			String formName = StringEscapeUtils.escapeHtml4(request.getParameter("formName"));
			if (uri != null && uri.endsWith("palette"))
			{
				SpecProviderState specProvider = WebComponentSpecProvider.getSpecProviderState();

				servletResponse.setContentType("application/json");

				if (servletResponse instanceof HttpServletResponse) HTTPUtils.setNoCacheHeaders((HttpServletResponse)servletResponse);

				try
				{
					ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();

					FlattenedSolution fl = activeProject.getEditingFlattenedSolution();
					Form form = fl.getForm(formName);

					if (form == null)
					{
						// try to find it in the developer project(s) from the active project
						for (ServoyDeveloperProject developerProjectsList : activeProject.getDeveloperProjects())
						{
							FlattenedSolution devSol = developerProjectsList.getEditingFlattenedSolution();
							form = devSol.getForm(formName);
							if (form != null) break;
						}
					}

					if (form == null)
					{
						ServoyLog.logInfo(
							"form " + formName + " not found for the editor in solutuion: " + fl + " maybe a switch of the solution for an old editor?");
						return;
					}

					boolean skipDefault = EditorUtil.hideDefaultComponents(form);
					boolean hasMenuProperty = false;

					TreeMap<String, Pair<PackageSpecification<WebObjectSpecification>, List<WebObjectSpecification>>> componentCategories = new TreeMap<>();
					for (Entry<String, PackageSpecification<WebObjectSpecification>> entry : specProvider.getWebObjectSpecifications().entrySet())
					{
						PackageSpecification<WebObjectSpecification> value = entry.getValue();
						for (WebObjectSpecification spec : value.getSpecifications().values())
						{
							String categoryName = spec.getCategoryName();
							if (Utils.stringIsEmpty(categoryName))
							{
								if (IGNORE_PACKAGE_LIST.contains(value.getPackageName())) continue;
								// filter deprecated servoydefault
								if (skipDefault && value.getPackageName().equals("servoydefault")) continue;
								categoryName = value.getPackageDisplayname();
							}
							Pair<PackageSpecification<WebObjectSpecification>, List<WebObjectSpecification>> pair = componentCategories.get(categoryName);
							if (pair == null)
							{
								List<WebObjectSpecification> list = new ArrayList<>();
								pair = new Pair<>(value, list);
								componentCategories.put(categoryName, pair);
							}
							pair.getRight().add(spec);
							if (spec.getProperties(MenuPropertyType.INSTANCE).size() > 0)
							{
								hasMenuProperty = true;
							}
						}
					}

					StringWriter sw = new StringWriter();
					JSONWriter jsonWriter = new JSONWriter(sw); //servletResponse.getWriter());
					jsonWriter.array();

					// Step 1: create a list with all keys containing first the layout and then the component packages
					List<String> orderedKeys = new ArrayList<String>();
					orderedKeys.add("Templates");
					if (hasMenuProperty && fl.getMenus(false).hasNext())
					{
						orderedKeys.add("Servoy Menu");
					}
					orderedKeys.addAll(specProvider.getLayoutSpecifications().keySet());

					for (String category : componentCategories.keySet())
					{
						if (!orderedKeys.contains(category)) orderedKeys.add(category);
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

					List<JSONObject> formComponents = null;
					for (String key : orderedKeys)
					{
						Pair<PackageSpecification<WebObjectSpecification>, List<WebObjectSpecification>> componentCategory = componentCategories.get(key);
						boolean startedArray = false;
						JSONObject categories = new JSONObject(); // categorised layout items, categories for components are already in the orderedKeys..
						if (specProvider.getLayoutSpecifications().containsKey(key))
						{
							// TODO check why getWebComponentSpecifications call below also returns the layout specifications.
							// hard coded that in absolute layout we get the servoycore (responsive container) layout
							if ((!"servoycore".equals(key) && !"Absolute-Layout".equals(layoutType)) ||
								("servoycore".equals(key) && "Absolute-Layout".equals(layoutType)))
							{
								PackageSpecification<WebLayoutSpecification> pkg = specProvider.getLayoutSpecifications().get(key);
								jsonWriter.object();
								jsonWriter.key("packageName").value(pkg.getPackageName());
								jsonWriter.key("packageDisplayname").value(pkg.getPackageDisplayname());
								jsonWriter.key("components");
								jsonWriter.array();
								startedArray = true;
							}
						}
						else if (componentCategory != null && isAccesibleInLayoutType(componentCategory.getLeft(), layoutType))
						{
							jsonWriter.object();
							jsonWriter.key("packageName").value(key);
							jsonWriter.key("packageDisplayname").value(key);
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
								if (form == null || form.getUseCssPosition() == null)
								{
									System.err.println("null");
								}
								String layout = form.getUseCssPosition().booleanValue() ? Template.LAYOUT_TYPE_CSS_POSITION : layoutType;

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
										try
										{
											JSONObject templateJSON = new ServoyJSONObject(content, false);
											if (templateJSON.optString(Template.PROP_LAYOUT, Template.LAYOUT_TYPE_ABSOLUTE).equals(layout))
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
												jsonWriter.key("icon").value("rfb/angular/js/modules/toolbaractions/icons/template_save.png");
												jsonWriter.endObject();
											}
										}
										catch (Exception e)
										{
											Debug.error("error parsing template '" + stringResource + "' with content " + content, e);
										}
									}
								}
								jsonWriter.endArray();
								jsonWriter.endObject();
							}
						}
						else if (key.equals("Servoy Menu"))
						{
							jsonWriter.object();
							jsonWriter.key("packageName").value("Servoy Menu");
							jsonWriter.key("packageDisplayname").value("Servoy Menu");
							jsonWriter.key("components");
							jsonWriter.array();
							fl.getMenus(true).forEachRemaining(menu -> {
								jsonWriter.object();
								jsonWriter.key("name").value("servoymenu-" + menu.getName());
								jsonWriter.key("componentType").value("jsmenu");
								jsonWriter.key("displayName").value(menu.getName());
								jsonWriter.key("icon").value("rfb/angular/images/column.png");
								Map<String, Object> model = new HashMap<String, Object>();
								HashMap<String, Number> size = new HashMap<String, Number>();
								size.put("height", Integer.valueOf(30));
								size.put("width", Integer.valueOf(30));
								model.put("size", size);
								jsonWriter.key("model").value(new JSONObject(model));
								jsonWriter.endObject();
							});
							jsonWriter.endArray();
							jsonWriter.endObject();
						}
						if (startedArray && specProvider.getLayoutSpecifications().containsKey(key))
						{
							PackageSpecification<WebLayoutSpecification> entry = specProvider.getLayoutSpecifications().get(key);
							for (WebLayoutSpecification spec : entry.getSpecifications().values())
							{
								if (spec.isDeprecated()) continue;
								JSONObject layoutJson = new JSONObject();
								layoutJson.put("name", spec.getName());
								if (spec.getConfig() != null)
								{
									String layoutName = new JSONObject((String)spec.getConfig()).optString("layoutName", null);
									if (layoutName != null)
									{
										layoutJson.put("layoutName", layoutName);
									}
									else layoutJson.put("layoutName", spec.getName());
								}
								else layoutJson.put("layoutName", spec.getName());

								layoutJson.put("packageName", spec.getPackageName());

								layoutJson.put("componentType", "layout");
								layoutJson.put("displayName", spec.getDisplayName());

								if (spec.getCategoryName() != null) layoutJson.put("category", spec.getCategoryName());

								JSONObject config = spec.getConfig() instanceof String ? new JSONObject((String)spec.getConfig()) : null;
								if (config == null || config.length() < 1)
								{
									layoutJson.put("tagName", "<div style='border-style: dotted;'></div>"); //TODO is tagname configurable by the spec
								}
								else
								{
									layoutJson.put("tagName", createLayoutDiv(config, new StringBuilder(), spec, false).toString());
								}
								layoutJson.put("attributes", getLayoutAttributes(config, spec, false));
								if (config != null && config.has("children"))
								{
									layoutJson.put("children", getChildren(config, spec));
								}

								Map<String, Object> model = new HashMap<String, Object>();
								if ("servoycore-responsivecontainer".equals(spec.getName()))
								{
									HashMap<String, Number> size = new HashMap<String, Number>();
									size.put("width", Integer.valueOf(200));
									size.put("height", Integer.valueOf(200));
									model.put("size", size);
									model.put("classes", new String[] { "highlight_element", "svy-responsivecontainer" });
								}
								else
								{
									PropertyDescription pd = spec.getProperty("size");
									if (pd != null && pd.getDefaultValue() != null)
									{
										model.put("size", pd.getDefaultValue());
									}
									else
									{
										HashMap<String, Number> size = new HashMap<String, Number>();
										size.put("width", Integer.valueOf(300));
										model.put("size", size);
									}
								}
								layoutJson.put("model", new JSONObject(model));
								if (spec.getIcon() != null)
								{
									layoutJson.put("icon", spec.getIcon());
								}
								if (spec.getPreview() != null)
								{
									layoutJson.put("preview", spec.getPreview());
								}
								layoutJson.put("topContainer", spec.isTopContainer());

								if (layoutJson.has("category"))
								{
									categories.append(layoutJson.getString("category"), layoutJson);
								}
								else jsonWriter.value(layoutJson);
							}
						}
						if (startedArray && componentCategory != null)
						{
							Collection<WebObjectSpecification> webComponentSpecsCollection = componentCategory.getRight();
							for (WebObjectSpecification spec : webComponentSpecsCollection)
							{
								if (!IGNORE_COMPONENT_LIST.contains(spec.getName()) && !spec.isDeprecated())
								{
									JSONObject componentJson = new JSONObject();
									componentJson.put("name", spec.getName());
									componentJson.put("packageName", spec.getPackageName());
									componentJson.put("componentType", "component");
									componentJson.put("displayName", spec.getDisplayName());
									componentJson.put("keywords", spec.getKeywords());
									if (spec.getStyleVariantCategory() != null)
									{
										FlattenedSolution efs = ServoyModelFinder.getServoyModel().getServoyProject(form.getSolution().getName())
											.getEditingFlattenedSolution();
										JSONArray variantsForCategory = efs.getVariantsHandler().getVariantsForCategory(spec.getStyleVariantCategory());
										if (variantsForCategory.length() > 0)
										{
											componentJson.put("styleVariantCategory", spec.getStyleVariantCategory());
										}
									}

									Map<String, Object> model = new HashMap<String, Object>();
									if (form.isResponsiveLayout())
									{
										WebComponent obj = (WebComponent)fl.getSolution().getChangeHandler().createNewObject(form, IRepository.WEBCOMPONENTS);
										obj.setName(spec.getName());
										obj.setTypeName(spec.getName());
										FormElement formElement = FormElementHelper.INSTANCE.getFormElement(obj, fl, null, true);
										StringWriter stringWriter = new StringWriter();
										PrintWriter printWriter = new PrintWriter(stringWriter);
										FormLayoutGenerator.generateFormElement(printWriter, formElement, form);
										componentJson.put("tagName", stringWriter.toString());
										componentJson.put("componentTagName", FormTemplateGenerator.getTagName(spec.getName()));
										model.put("componentName", formElement.getDesignId());
									}
									else componentJson.put("tagName", FormTemplateGenerator.getTagName(spec.getName()));

									Set<String> allPropertiesNames = spec.getAllPropertiesNames();
									for (String string : allPropertiesNames)
									{
										PropertyDescription property = spec.getProperty(string);
										if (property != null && property.getDefaultValue() != null)
										{
											Object defaultValue = property.getDefaultValue();
											if (defaultValue != null) model.put(string, defaultValue);
										}
										if (property != null && property.getInitialValue() != null)
										{
											Object initialValue = property.getInitialValue();
											if (initialValue != null) model.put(string, initialValue);
										}
									}
									PropertyDescription pd = spec.getProperty("size");
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
									componentJson.put("model", new JSONObject(model));
									if (spec.getIcon() != null)
									{
										componentJson.put("icon", spec.getIcon());
									}
									componentJson.put("types", new JSONArray(getPalleteTypeNames(spec)));
									if (!spec.getProperties(FormComponentPropertyType.INSTANCE).isEmpty())
									{
										if (formComponents == null) formComponents = getFormComponents(form);
										if (!formComponents.isEmpty())
										{
											componentJson.put("properties", getFormComponentPropertyNames(spec));
										}
									}
									jsonWriter.value(componentJson);
								}
							}
						}
						if (startedArray)
						{
							jsonWriter.endArray();
							if (categories.length() > 0) jsonWriter.key("categories").value(categories);
							jsonWriter.endObject();
						}
					}
					if (formComponents != null)
					{
						jsonWriter.object();
						jsonWriter.key("propertyValues").value(formComponents);
						jsonWriter.endObject();
					}
					jsonWriter.endArray();
					JSONArray jsonArray = new JSONArray(sw.toString());
					jsonArray = PaletteCommonsHandler.getInstance().insertcommonsCategory(jsonArray);
					jsonArray = PaletteFavoritesHandler.getInstance().insertFavoritesCategory(jsonArray);
					servletResponse.getWriter().write(jsonArray.toString());
				}
				catch (JSONException ex)
				{
					Debug.error("Exception during designer palette generation", ex);
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
			else if (uri != null && uri.endsWith("formplaceholder.html"))
			{
				if (servletResponse instanceof HttpServletResponse)
				{
					HTTPUtils.setNoCacheHeaders((HttpServletResponse)servletResponse);
					servletResponse.setContentType("text/html");
				}
				String width = "350px";
				String height = "200px";
				if (formName != null)
				{
					FlattenedSolution fl = ServoyModelFinder.getServoyModel().getActiveProject().getEditingFlattenedSolution();
					Form editingForm = fl.getForm(request.getParameter("editingForm"));
					if (editingForm == null || editingForm.isResponsiveLayout())
					{
						Form form = fl.getForm(formName);
						if (form != null)
						{
							if (!form.isResponsiveLayout())
							{
								width = form.getSize().width + "px";
								height = form.getSize().height + "px";
							}
							else
							{
								width = "100%";
							}
						}
					}
					else
					{
						height = "100%";
						width = "100%";
					}
				}
				if (formName == null) formName = "";
				PrintWriter w = servletResponse.getWriter();
				w.write("<div style=\"height:" + height + ";width:" + width + ";background-color:#e6e6e6;\">" + formName + "</div>");
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

	private JSONArray getChildren(JSONObject config, WebLayoutSpecification spec)
	{
		JSONArray children = config.optJSONArray("children");
		JSONArray res = new JSONArray();
		if (children != null)
		{
			for (int i = 0; i < children.length(); i++)
			{
				JSONObject jsonObject = children.getJSONObject(i);
				JSONObject childModel = jsonObject.optJSONObject("model");
				if (childModel != null)
				{
					JSONObject c = new JSONObject();
					c.put("model", childModel);
					c.put("attributes", getLayoutAttributes(childModel, spec, true));
					res.put(c);
				}
			}
		}
		return res;
	}


	private boolean isAccesibleInLayoutType(PackageSpecification< ? > pkg, String layoutType)
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
	 * @param spec
	 * @param specifications
	 * @throws JSONException
	 */
	protected StringBuilder createLayoutDiv(JSONObject config, StringBuilder sb, WebLayoutSpecification spec, boolean isChild) throws JSONException
	{
		String tagName = (String)(spec.getProperty(StaticContentSpecLoader.PROPERTY_TAGTYPE.getPropertyName()) != null
			? spec.getProperty(StaticContentSpecLoader.PROPERTY_TAGTYPE.getPropertyName()).getDefaultValue() : "div");
		sb.append("<" + tagName + " ");
		Iterator keys = config.keys();
		while (keys.hasNext())
		{
			String key = (String)keys.next();
			if (key.equals("children") || key.equals("layoutName")) continue;
			String value = config.getString(key);
			sb.append(key);
			sb.append("='");

			sb.append(value);
			if (key.equals("class"))
			{
				sb.append(" " + spec.getDesignStyleClass());
				sb.append("' ");
				sb.append("svy-title='" + (value.startsWith("col-") && !isChild ? "md-*" : value));
			}

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
					createLayoutDiv(childModel, sb, spec, true);
				}
			}
		}
		return sb.append("</" + tagName + ">");
	}

	private JSONObject getLayoutAttributes(JSONObject config, WebLayoutSpecification spec, boolean isChild)
	{
		JSONObject result = new JSONObject();
		String value = config != null ? config.optString("class") : null;
		if (value == null) result.put("svy-title", "<null>");
		else result.put("svy-title", (value.startsWith("col-") && !isChild ? "md-*" : value));
		result.put("designclass", spec.getDesignStyleClass());
		return result;
	}

	private List<String> getFormComponentPropertyNames(WebObjectSpecification spec)
	{
		List<String> result = new ArrayList<>();
		for (PropertyDescription propertyDescription : spec.getProperties(FormComponentPropertyType.INSTANCE))
		{
			result.add(propertyDescription.getName());
		}
		return result;
	}

	private List<JSONObject> getFormComponents(Form form)
	{
		List<JSONObject> result = new ArrayList<JSONObject>();
		Iterator<Form> it = form.getSolution().getAllComponentForms(true);
		while (it.hasNext())
		{
			Form formComponent = it.next();
			if (form.equals(formComponent)) continue;
			if (form.getDataSource() == null || formComponent.getDataSource() == null || formComponent.getDataSource().equals(form.getDataSource()))
			{
				JSONObject json = new JSONObject();
				json.put("displayName", formComponent.getName());
				JSONObject propertyvalue = new JSONObject();
				propertyvalue.put(FormComponentPropertyType.SVY_FORM, formComponent.getUUID());
				json.put("propertyValue", propertyvalue);
				json.put("isAbsoluteCSSPositionMix",
					!form.isResponsiveLayout() && !formComponent.isResponsiveLayout() && (form.getUseCssPosition() != formComponent.getUseCssPosition()));
				result.add(json);
			}
		}
		return result;
	}

	private List<JSONObject> getPalleteTypeNames(WebObjectSpecification spec)
	{
		List<JSONObject> result = new ArrayList<JSONObject>();
		Map<String, PropertyDescription> properties = spec.getProperties();
		Map<String, List<String>> droppableTypesToPropertyNames = new TreeMap<>();
		for (PropertyDescription propertyDescription : properties.values())
		{
			if (propertyDescription.isDeprecated() ||
				propertyDescription.getConfig() instanceof JSONObject && Boolean.TRUE.equals(((JSONObject)propertyDescription.getConfig()).opt("deprecated")))
			{
				continue;
			}
			Object configObject = propertyDescription.getConfig();
			if (RFBDesignerUtils.isDroppable(propertyDescription, configObject, true))
			{
				String type = PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(propertyDescription.getType());
				List<String> colunsWithThatType = droppableTypesToPropertyNames.get(type);
				if (colunsWithThatType == null)
				{
					colunsWithThatType = new SortedList<>();
					droppableTypesToPropertyNames.put(type, colunsWithThatType);
				}
				colunsWithThatType.add(propertyDescription.getName());
			}
		}
		for (Entry<String, List<String>> e : droppableTypesToPropertyNames.entrySet())
		{
			for (String propertyName : e.getValue())
			{
				JSONObject json = new JSONObject();
				if (e.getValue().size() > 1) json.put("multiple", true);
				json.put("type", e.getKey());
				json.put("propertyName", propertyName);
				result.add(json);
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
