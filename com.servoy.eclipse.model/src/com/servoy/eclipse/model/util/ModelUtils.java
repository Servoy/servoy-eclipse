/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.model.util;

import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.extensions.IUnexpectedSituationHandler;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.DataSourceCollectorVisitor;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ServerProxy;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.less.LessCompiler;
import com.servoy.j2db.server.ngclient.property.types.NGStyleClassPropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.ui.ISupportRowStyling;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyStyleSheet;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.Utils;

public class ModelUtils
{
	private static final ConcurrentMap<List<String>, Pair<Long, String[]>> cachedCssClasses = new ConcurrentHashMap<>();

	public static final String ONLY_WHEN_UI_DISABLED_ATTRIBUTE_NAME = "whenUIDisabledStateIs";

	public static String getTokenValue(Object[] value, String delim)
	{
		if (value == null || value.length == 0)
		{
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (Object o : value)
		{
			if (sb.length() > 0)
			{
				sb.append(delim);
			}
			sb.append(o instanceof String ? (String)o : "");
		}
		return sb.toString();
	}

	public static String getStyleLookupname(IPersist persist)
	{
		if (persist instanceof BaseComponent)
		{
			return ComponentFactory.getLookupName((BaseComponent)persist);
		}
		if (persist instanceof Form)
		{
			return "form";
		}
		if (persist instanceof Part)
		{
			return Part.getCSSSelector(((Part)persist).getPartType());
		}
		return null;
	}

	/**
	 * Weak cache for style classes per lookupname.
	 */
	private final static WeakHashMap<IStyleSheet, Map<Pair<String, String>, String[]>> styleClassesCache = new WeakHashMap<IStyleSheet, Map<Pair<String, String>, String[]>>();

	/**
	 * Het styleclasses with default value.
	 *
	 * @param flattenedSolution
	 * @param form
	 * @param persist
	 * @param styleClassProperty
	 * @param lookupName
	 */
	public static Pair<String[], String> getStyleClasses(FlattenedSolution flattenedSolution, Form form, IPersist persist, String styleClassProperty,
		String lookupName)
	{
		if (flattenedSolution == null || form == null)
		{
			return new Pair<>(new String[0], null);
		}

		form = flattenedSolution.getFlattenedForm(form);
		Solution solution = (Solution)form.getRootObject();
		ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solution.getName());

		List<String> mediaStyleSheets = PersistHelper.getOrderedStyleSheets(servoyProject.getEditingFlattenedSolution());
		if (mediaStyleSheets != null && mediaStyleSheets.size() == 0 &&
			(persist instanceof IFormElement || persist instanceof Form || persist instanceof Part) && !(persist instanceof WebComponent))
		{
			// legacy component, no css at solution level
			return new Pair<>(getStyleClasses(flattenedSolution.getStyleForForm(form, null), lookupName, form.getStyleClass()), null);
		}

		// if we have solution level css, return spec file and classes from css, ignore form style
		String defaultValue = null;

		WebObjectSpecification spec = null;
		if (persist instanceof IFormElement)
		{
			spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
				FormTemplateGenerator.getComponentTypeName((IFormElement)persist));
		}
		else if (persist instanceof LayoutContainer)
		{
			PackageSpecification<WebLayoutSpecification> pkg = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().get(
				((LayoutContainer)persist).getPackageName());
			if (pkg != null)
			{
				spec = pkg.getSpecification(((LayoutContainer)persist).getSpecName());
			}
		}

		String[] css = new String[0];

		if (mediaStyleSheets != null)
		{
			long lastModifiedTime = mediaStyleSheets.stream().map(name -> flattenedSolution.getMedia(name)).mapToLong(Media::getLastModifiedTime).max()
				.orElse(0);
			Pair<Long, String[]> pair = cachedCssClasses.get(mediaStyleSheets);
			if (pair != null && pair.getLeft().longValue() == lastModifiedTime)
			{
				css = pair.getRight();
			}
			else
			{
				// clear the cache if its a cache miss, could be that the numer of media files did change.
				cachedCssClasses.clear();
				Set<String> cssClasses = new TreeSet<>(StringComparator.INSTANCE);
				for (String styleSheet : mediaStyleSheets)
				{
					Media media = flattenedSolution.getMedia(styleSheet);
					if (media != null)
					{
						String lessStyleSheet = media.getName();
						int index = lessStyleSheet.indexOf(".less");
						if (index > 0)
						{
							String ng2Filename = lessStyleSheet.substring(0, index) + "_ng2.less";
							Media media2 = flattenedSolution.getMedia(ng2Filename);
							if (media2 != null)
							{
								media = media2;
							}
						}

						try
						{
							List<String> styleNames = null;
							if (styleNames == null)
							{
								String cssContent = media.getName().endsWith(".less") ? LessCompiler.compileSolutionLessFile(media, flattenedSolution)
									: new String(media.getMediaData(), "UTF-8");
								// we only use the css3 styling (getStyleNames() so that we can give a boolean to ignore/don't create the rest
								IStyleSheet ss = new ServoyStyleSheet(cssContent, media.getName(), true);
								styleNames = ss.getStyleNames();
							}
							for (String cssSelector : styleNames)
							{
								if (cssSelector.contains("."))
								{
									String[] selectors = cssSelector.split("\\.");
									for (int i = 0; i < selectors.length; i++)
									{
										if (i == 0 && selectors.length > 1) continue;
										String selector = selectors[i];
										int cutIndex = selector.indexOf(" ");
										int pseudoClassIndex = selector.indexOf(":");
										cutIndex = Math.min(Math.max(cutIndex, 0), Math.max(pseudoClassIndex, 0));
										if (cutIndex > 0)
										{
											selector = selector.substring(0, cutIndex);
										}
										cssClasses.add(selector);
									}
								}
							}
							cssClasses.toArray(new String[0]);
						}
						catch (UnsupportedEncodingException ex)
						{
							ServoyLog.logError(ex);
						}
					}
				}
				css = cssClasses.toArray(css);
				cachedCssClasses.put(mediaStyleSheets, new Pair<Long, String[]>(Long.valueOf(lastModifiedTime), css));
			}
		}
		else
		{
			Set<String> cssClasses = new TreeSet<>(StringComparator.INSTANCE);
			if (spec != null)
			{
				PropertyDescription pd = spec.getProperty(styleClassProperty);
				if (pd != null)
				{
					List<Object> values = pd.getValues();
					for (Object val : Utils.iterate(values))
					{
						cssClasses.add(val.toString());
					}
					if (pd.hasDefault() && pd.getDefaultValue() != null)
					{
						defaultValue = NGStyleClassPropertyType.NG_INSTANCE.fromDesignValue(pd.getDefaultValue().toString(), pd, persist);
					}
				}
			}
			css = cssClasses.toArray(css);
		}
		return new Pair<>(css, defaultValue);
	}

	public static String[] getStyleClasses(Style style, String lookupName, String formStyleClass)
	{
		if (lookupName == null || lookupName.length() == 0 || style == null)
		{
			return new String[0];
		}

		IStyleSheet styleSheet = ComponentFactory.getCSSStyle(null, style);
		Map<Pair<String, String>, String[]> map = styleClassesCache.get(styleSheet);
		if (map == null)
		{
			map = new HashMap<Pair<String, String>, String[]>();
			styleClassesCache.put(styleSheet, map);
		}

		String[] styleClasses = map.get(new Pair<String, String>(lookupName, formStyleClass));
		if (styleClasses == null)
		{
			styleClasses = calculateStyleClasses(styleSheet, lookupName, formStyleClass);
			map.put(new Pair<String, String>(lookupName, formStyleClass), styleClasses);
		}
		return styleClasses;
	}

	private static String[] calculateStyleClasses(IStyleSheet styleSheet, String lookupName, String formStyleClass)
	{
		List<String> styleClasses = new ArrayList<String>();
		boolean matchedFormPrefix = false;
		String formPrefix = "form";
		if (formStyleClass != null)
		{
			formPrefix += "." + formStyleClass;
		}

		if (!lookupName.equals("form"))
		{
			boolean styleExist = false;
			boolean matchedFormPrefixField = false;
			List<String> selectors = styleSheet.getStyleNames();
			for (String selector : selectors)
			{
				String[] styleParts = selector.split("\\p{Space}+?");
				if (styleParts.length <= 2 && (styleParts.length == 1 || styleParts[0].equals(formPrefix)))
				{
					String styleName = styleParts[styleParts.length - 1];
					if (styleName.equals(lookupName))
					{
						matchedFormPrefix |= styleParts.length == 2; // found a match with form prefix, skip root matches
						styleExist = true;
					}
					else if (styleName.startsWith(lookupName + '.'))
					{
						styleExist = true;
					}
					else if (styleName.equals("field"))
					{
						matchedFormPrefixField |= styleParts.length == 2;
					}
				}
			}
			if (!styleExist && (lookupName.equals("check") || lookupName.equals("combobox") || lookupName.equals("radio")))
			{
				lookupName = "field";
				matchedFormPrefix = matchedFormPrefixField;
			}
		}

		List<String> selectors = styleSheet.getStyleNames();
		for (String selector : selectors)
		{
			String[] styleParts = selector.split("\\p{Space}+?");
			int stylePartsCount = styleParts.length;
			String styleName;

			if ("form".equals(lookupName))
			{
				styleName = styleParts[0];
			}
			else
			{
				styleName = styleParts[styleParts.length - 1];
				if (styleParts.length > 1 && (styleName.equals(ISupportRowStyling.CLASS_ODD) || styleName.equals(ISupportRowStyling.CLASS_EVEN) ||
					styleName.equals(ISupportRowStyling.CLASS_SELECTED)))
				{
					styleName = styleParts[styleParts.length - 2];
					stylePartsCount--;
				}

				if ((matchedFormPrefix && stylePartsCount == 1) // found a match with form prefix, skip root matches
					|| stylePartsCount > 2 || !styleName.startsWith(lookupName) || (stylePartsCount == 2 && !styleParts[0].equals(formPrefix)))
				{
					continue;
				}
			}

			int index = styleName.indexOf('.');
			if (index == lookupName.length() && styleName.startsWith(lookupName))
			{
				String styleToAdd = styleName.substring(index + 1);
				if (styleClasses.indexOf(styleToAdd) == -1) styleClasses.add(styleToAdd);
			}
		}

		Collections.sort(styleClasses);

		return styleClasses.toArray(new String[styleClasses.size()]);
	}

	public static FlattenedSolution getEditingFlattenedSolution(IPersist persist)
	{
		if (persist == null) return null;

		Solution solution = (Solution)persist.getAncestor(IRepository.SOLUTIONS);
		if (solution == null) return null;

		ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solution.getName());
		if (servoyProject == null) return null;

		return servoyProject.getEditingFlattenedSolution();
	}

	public static FlattenedSolution getEditingFlattenedSolution(IPersist persist, IPersist context)
	{
		return getEditingFlattenedSolution(Utils.isInheritedFormElement(persist, context) ? context : persist);
	}

	/**
	 * Get a script method by id.
	 *
	 * @param persist
	 * @param context
	 * @param table
	 * @param methodId
	 * @return
	 */
	public static IScriptProvider getScriptMethod(IPersist persist, IPersist context, ITable table, int methodId)
	{
		if (methodId <= 0)
		{
			return null;
		}

		// This will normally now return any method now..
		FlattenedSolution editingFlattenedSolution = getEditingFlattenedSolution(persist, context);
		ScriptMethod sm = editingFlattenedSolution.getScriptMethod(methodId);
		if (sm == null)
		{
			// this code shouldn't be hit anymore
			if (table != null)
			{
				Iterator<TableNode> tableNodes = editingFlattenedSolution.getTableNodes(table);
				while (tableNodes.hasNext())
				{
					TableNode tableNode = tableNodes.next();
					IPersist method = AbstractBase.selectById(tableNode.getAllObjects(), methodId);
					if (method instanceof IScriptProvider)
					{
						return (IScriptProvider)method;
					}
				}
			}
		}
		// find the form method
		Form formBase = (Form)(context == null ? persist : context).getAncestor(IRepository.FORMS); // search via context if provided
		if (formBase == null)
		{
			// not a form method
			return sm;
		}

		List<Form> formHierarchy = editingFlattenedSolution.getFormHierarchy(formBase);

		if (sm != null && sm.getParent() instanceof Form && formHierarchy.contains(sm.getParent()))
		{
			// found form method by id, now find the actual implementation based on name (respecting form hierarchy)
			for (Form f : formHierarchy)
			{
				ScriptMethod formSm = f.getScriptMethod(sm.getName());
				if (formSm != null)
				{
					return formSm;
				}
			}
		}

		return sm;
	}

	private static boolean uiDisabled = false;

	/**
	 * Servoy plugins that are not supposed to start when developer is started with UI disabled (for example by workspace exporter apps)
	 * should call this method first thing in their bundle activator's start() method.
	 * @throws RuntimeException if UI is not supposed to be used
	 */
	public static void assertUINotDisabled(String bundleName) throws RuntimeException
	{
		// probably Servoy developer was started via a workspace exporter app. - it must not initialize core/ui and other related projects
		// but some extension points these use (for example DLTK extension points) will cause them to get loaded; do not allow this!
		if (ModelUtils.isUIDisabled()) throw new RuntimeException(
			bundleName != null ? "'" + bundleName + "' bundle will not be started as Servoy is started without UI. Please ignore this log message."
				: "Assertion failed. UI is marked as not running.");
	}

	public static boolean isUIDisabled()
	{
		return uiDisabled;
	}

	public static void setUIDisabled(boolean disabled)
	{
		uiDisabled = disabled;
	}

	/**
	 * Updates the solution's server (proxy) cache.
	 */
	public static void updateSolutionServerProxies(final Solution solution, IDeveloperRepository repository)
	{
		DataSourceCollectorVisitor datasourceCollector = new DataSourceCollectorVisitor();
		solution.acceptVisitor(datasourceCollector);

		ConcurrentMap<String, IServer> serverProxies = new ConcurrentHashMap<String, IServer>();
		for (String serverName : DataSourceUtils.getServerNames(datasourceCollector.getDataSources()))
		{
			try
			{
				IServer s = repository.getServer(serverName);
				if (s != null)
				{
					serverProxies.put(serverName, new ServerProxy(s)
					{
						@Override
						public ITable getTable(String tableName) throws RepositoryException, RemoteException
						{
							// do not use the caching, in developer a table may have been deleted, proxies are needed for databasemanager.getServerNames() in developer
							return server.getTable(tableName);
						}
					});
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}

			solution.setServerProxies(serverProxies);
		}
	}

	/**
	 * Updates server proxies for all active solutions/modules.
	 */
	public static void updateActiveSolutionServerProxies(IDeveloperRepository repository)
	{
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		for (ServoyProject active : servoyModel.getModulesOfActiveProject())
		{
			Solution solution = active.getSolution();
			if (solution != null)
			{
				updateSolutionServerProxies(solution, repository);
			}
		}
	}

	public static <T> List<T> getExtensions(String extensionID)
	{
		List<T> ts = new ArrayList<T>();
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint(extensionID);
		IExtension[] extensions = ep.getExtensions();

		for (IExtension extension : extensions)
		{
			IConfigurationElement[] ces = extension.getConfigurationElements();
			for (IConfigurationElement ce : ces)
			{
				try
				{
					String expectedUIDisabledState = ce.getAttribute(ONLY_WHEN_UI_DISABLED_ATTRIBUTE_NAME);
					if (expectedUIDisabledState == null || expectedUIDisabledState.equals(Boolean.toString(ModelUtils.isUIDisabled())))
					{
						T t = (T)ce.createExecutableExtension("class");
						if (t != null)
						{
							ts.add(t);
						}
					}
				}
				catch (CoreException e)
				{
					ServoyLog.logError("Could not load extension (extension point " + extensionID + ", " + ce.getAttribute("class") + ")", e);
				}
				catch (ClassCastException e)
				{
					ServoyLog.logError("Extension class has wrong type (extension point " + extensionID + ", " + ce.getAttribute("class") + ")", e);
				}
			}
		}
		return ts;
	}

	public static IUnexpectedSituationHandler getUnexpectedSituationHandler()
	{
		List<IUnexpectedSituationHandler> handlers = getExtensions(IUnexpectedSituationHandler.EXTENSION_ID);
		if (handlers.size() == 1) return handlers.get(0);

		throw new RuntimeException("Expected to find exactly one compatible '" + IUnexpectedSituationHandler.EXTENSION_ID + "' extension. Found:\n" + handlers);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getAdapter(Object object, Class< ? extends T> adapter)
	{
		if (object == null)
		{
			return null;
		}

		if (adapter.isAssignableFrom(object.getClass()))
		{
			return (T)object;
		}

		Object adapted = null;
		if (object instanceof IAdaptable)
		{
			adapted = ((IAdaptable)object).getAdapter(adapter);
		}
		if (adapted == null)
		{
			adapted = Platform.getAdapterManager().getAdapter(object, adapter);
		}
		return (T)adapted;
	}

}