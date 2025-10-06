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
package com.servoy.eclipse.model;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.print.PageFormat;
import java.net.URL;
import java.net.URLStreamHandler;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IEventsManager;
import com.servoy.j2db.IJSFormManager;
import com.servoy.j2db.IMenuManager;
import com.servoy.j2db.IMessagesCallback;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IPermissionManager;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.IValueListManager;
import com.servoy.j2db.Messages;
import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.DataServerProxy;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.FoundSetManagerConfig;
import com.servoy.j2db.dataprocessing.IClientHost;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IFoundSetListener;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.dataprocessing.IGlobalValueEntry;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.dataprocessing.SwingFoundSetFactory;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IPluginAccess;
import com.servoy.j2db.plugins.IPluginManager;
import com.servoy.j2db.plugins.PluginManager;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.JSBlobLoaderBuilder;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerAccess;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.smart.dataui.SwingItemFactory;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IFormUI;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.RendererParentWrapper;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.toolbar.IToolbarPanel;

/**
 * @author jcompagner
 */
public class DesignApplication implements ISmartClientApplication, IMessagesCallback
{
	private IApplication client;
	private SwingItemFactory itemFactory;
	private volatile IFoundSetManagerInternal foundSetManager;

	private ResourceBundle localeJarMessages;
	private final HashMap<Locale, Properties> messages = new HashMap<Locale, Properties>();
	private PluginManager pluginManager;
	private PageFormat pageFormat;
	private ClientPluginAccessProvider pluginAccess;

	public DesignApplication()
	{
	}

	IApplication getClient()
	{
		if (client == null)
		{
			client = ApplicationServerRegistry.get().getDebugClientHandler().getDebugSmartClient();
		}
		return client;
	}

	public void blockGUI(String reason)
	{
		getClient().blockGUI(reason);
	}

	@Override
	public void blockGUII18NMessage(String key, Object... args)
	{
		getClient().blockGUII18NMessage(key, args);
	}


	public void updateUI(int time)
	{
		getClient().updateUI(time);
	}

	public void clearLoginForm()
	{
		getClient().clearLoginForm();
	}

	public boolean closeSolution(boolean force, Object[] args)
	{
		return true;
	}

	public String getApplicationName()
	{
		return "Servoy Client";
	}

	public int getApplicationType()
	{
		return CLIENT;
	}

	@Override
	public Object generateBrowserFunction(String functionString)
	{
		return functionString;
	}

	@Override
	public JSBlobLoaderBuilder createUrlBlobloaderBuilder(String dataprovider)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getClientOSName()
	{
		return System.getProperty("os.name");
	}

	public boolean isInDeveloper()
	{
		return true;
	}

	public boolean isShutDown()
	{
		return getClient().isShutDown();
	}

	public int getClientPlatform()
	{
		return Utils.getPlatform();
	}

	public String getClientID()
	{
		return getClient().getClientID();
	}

	public ClientInfo getClientInfo()
	{
		return getClient().getClientInfo();
	}

	public ICmdManager getCmdManager()
	{
		return getClient().getCmdManager();
	}

	public IDataRendererFactory getDataRenderFactory()
	{
		return getClient().getDataRenderFactory();
	}

	public IDataServer getDataServer()
	{
		return getClient().getDataServer();
	}

	public DataServerProxy getDataServerProxy()
	{
		return getClient().getDataServerProxy();
	}

	public IClientHost getClientHost()
	{
		return getClient().getClientHost();
	}

	public JMenu getExportMenu()
	{
		return null;
	}

	public JMenu getImportMenu()
	{
		return null;
	}

	public FlattenedSolution getFlattenedSolution()
	{
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeProject == null)
		{
			return null;
		}
		FlattenedSolution flattenedSolution = activeProject.getEditingFlattenedSolution();
		installServoyMobileInternalStyle(flattenedSolution);
		return flattenedSolution;
	}

	public static void installServoyMobileInternalStyle(FlattenedSolution flattenedSolution)
	{
		if (flattenedSolution == null) return;

		SolutionMetaData mainSolutionMetaData = flattenedSolution.getMainSolutionMetaData();
		if (mainSolutionMetaData != null && mainSolutionMetaData.getSolutionType() == SolutionMetaData.MOBILE)
		{
			// Add a built-in style for mobile schemes
			if (flattenedSolution.getStyle("_servoy_mobile") == null)
			{
				String servoyMobileStyle = //

					"label, label.a, label.b, label.c, label.d, label.e {"//
						+ "color: #101010;" //
						+ "margin-left:5px;" //
						+ "background-color: #414141;" //
						+ "}"//

						+ "headertext, headertext.a {"//
						+ "color: #ffffff;" //
						+ "background-color: #414141;" //
						+ "font-weight:bold;" //
						+ "}"//

						+ "field {"//
						+ "color: #101010;" //
						+ "background-color: #F9F9F9;" //
						+ "}"//

						+ "button, button.a, combobox.a, check.a, radio.a, portal.a {"//
						+ "color: #FFFFFF;" //
						+ "background-color: #414141;" + "font-weight:bold;" //
						+ "}"//

						+ "header, header.a, footer.a,  title_header, title_header.a, title_footer.a {" //
						+ "background-color: #262626;" //
						+ "}" //

						+ "headertext.b {"//
						+ "color: #ffffff;" //
						+ "background-color: #4C83B1;" //
						+ "font-weight:bold;" //
						+ "}" //

						+ "button.b, combobox.b, check.b, radio.b, portal.b {"//
						+ "color: #FFFFFF;" //
						+ "background-color: #4C83B1;" //
						+ "font-weight:bold;" //
						+ "}" //

						+ "header.b, footer.b, title_header.b, title_footer.b {" //
						+ "background-color: #5A91BF;" //
						+ "}" //

						+ "headertext.c {"//
						+ "color: #101010;" //
						+ "background-color: #4C83B1;" //
						+ "font-weight:bold;" //
						+ "}" //

						+ "button.c, combobox.c, check.c, radio.c, portal.c {" //
						+ "color: #363636;" //
						+ "background-color: #F6F6F6;"//
						+ "font-weight:bold;" //
						+ "}"//

						+ "header.c, footer.c, title_header.c, title_footer.c {" //
						+ "background-color: #E4E4E4;" //
						+ "}" //

						+ "headertext.d {"//
						+ "color: #101010;" //
						+ "background-color: #4C83B1;" //
						+ "font-weight:bold;" //
						+ "}" //

						+ "button.d, combobox.d, check.d, radio.d, portal.d {" //
						+ "color: #363636;" //
						+ "background-color: #F8F8F8;" //
						+ "font-weight:bold;" //
						+ "}"//

						+ "header.d, footer.d, title_header.d, title_footer.d {" //
						+ "background-color: #C7C7C7;" //
						+ "}" //

						+ "headertext.e {"//
						+ "color: #101010;" //
						+ "background-color: #4C83B1;" //
						+ "font-weight:bold;" //
						+ "}" //

						+ "button.e, combobox.e, check.e, radio.e, portal.e {" //
						+ "color: #363636;" //
						+ "background-color: #FFE87C;" //
						+ "font-weight:bold;" //
						+ "}" //

						+ "header.e, footer.e, title_header.e, title_footer.e {" //
						+ "background-color: #FBEE90;" //
						+ "}" //
				;
				flattenedSolution.createStyle("_servoy_mobile", servoyMobileStyle);
			}
		}
		else
		{
			flattenedSolution.removeStyle("_servoy_mobile");
		}
	}


	public IBasicFormManager getFormManager()
	{
		return getClient().getFormManager();
	}

	public IFoundSetManagerInternal getFoundSetManager()
	{
		if (foundSetManager == null)
		{
			synchronized (this)
			{
				if (foundSetManager == null)
				{
					foundSetManager = new FoundSetManager(this, new FoundSetManagerConfig(getSettings()), new SwingFoundSetFactory())
					{
						@Override
						public ITable getTable(String dataSource) throws RepositoryException
						{
							return ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(dataSource);
						}

						@Override
						public IGlobalValueEntry getScopesScopeProvider()
						{
							return new IGlobalValueEntry()
							{

								public Object setDataProviderValue(String dataProviderID, Object value)
								{
									return null;
								}

								public Object getDataProviderValue(String dataProviderID)
								{
									return null;
								}

								public boolean containsDataProvider(String dataProviderID)
								{
									return false;
								}
							};
						}

						@Override
						public FoundSet getEmptyFoundSet(IFoundSetListener panel) throws ServoyException
						{
							// no foundsets in designer
							return null;
						}

						@Override
						public IFoundSetInternal getGlobalRelatedFoundSet(String name) throws RepositoryException, ServoyException
						{
							// no foundsets in designer
							return null;
						}

						@Override
						public IFoundSetInternal getNewFoundSet(ITable table, QuerySelect pkSelect, List<SortColumn> defaultSortColumns) throws ServoyException
						{
							// no foundsets in designer
							return null;
						}

						@Override
						public IFoundSet getFoundSet(IQueryBuilder query) throws ServoyException
						{
							// no foundsets in designer
							return null;
						}

						@Override
						public IFoundSetInternal getFoundSet(String dataSource) throws ServoyException
						{
							// no foundsets in designer
							return null;
						}

						@Override
						public IFoundSet getNewFoundSet(String dataSource) throws ServoyException
						{
							// no foundsets in designer
							return null;
						}

						@Override
						public IFoundSetInternal getNewFoundSet(String dataSource, QuerySelect pkSelect, List<SortColumn> defaultSortColumns)
							throws ServoyException
						{
							// no foundsets in designer
							return null;
						}

						@Override
						public IFoundSetInternal getSeparateFoundSet(IFoundSetListener l, List<SortColumn> defaultSortColumns) throws ServoyException
						{
							// no foundsets in designer
							return null;
						}

						@Override
						public IFoundSetInternal getSharedFoundSet(String dataSource) throws ServoyException
						{
							// no foundsets in designer
							return null;
						}

						@Override
						public IFoundSetInternal getSharedFoundSet(String dataSource, List<SortColumn> defaultSortColumns) throws ServoyException
						{
							// no foundsets in designer
							return null;
						}

						@Override
						public SortColumn getSortColumn(ITable table, String dataProviderID, boolean logIfCannotBeResolved) throws RepositoryException
						{
							return null;
						}

						@Override
						public List<SortColumn> getSortColumns(ITable t, String options)
						{
							return Collections.emptyList();
						}

						@Override
						public List<SortColumn> getSortColumns(String dataSource, String options) throws RepositoryException
						{
							return Collections.emptyList();
						}

						@Override
						public void addFoundSetListener(IFoundSetListener l) throws ServoyException
						{
						}

						@Override
						public void addTableListener(ITable table, com.servoy.j2db.dataprocessing.ITableChangeListener l)
						{
						};
					};

					//((FoundSetManager)foundSetManager).setInfoListener(this);
					foundSetManager.init();
					// get the plugin manager so that converters/validators are initialized
					getPluginManager();
				}
			}
		}
		return foundSetManager;
	}

	public synchronized String getI18NMessage(String key, Object[] args)
	{
		return getI18NMessage(key, args, getLocale());
	}

	public synchronized String getI18NMessage(String key)
	{
		return getI18NMessage(key, null, getLocale());
	}

	@Override
	public String getI18NMessage(String key, Object[] args, String language, String country)
	{
		if (language == null || country == null) return key;
		return getI18NMessage(key, args, new Locale(language, country));
	}

	@Override
	public String getI18NMessage(String key, String language, String country)
	{
		if (language == null || country == null) return key;
		return getI18NMessage(key, null, new Locale(language, country));
	}

	private String getI18NMessage(String key, Object[] args, Locale locale)
	{
		if (key == null || key.length() == 0) return key;
		Properties properties = getMessages(locale);
		return getI18NMessage(key, args, properties, localeJarMessages, locale);
	}

	public void setI18NMessage(String key, String value)
	{
		if (key != null)
		{
			Properties properties = getMessages(getLocale());
			if (value == null)
			{
				properties.remove(key);
				refreshI18NMessages();
			}
			else
			{
				properties.setProperty(key, value);
			}

		}
	}

	private String getI18NMessage(String key, Object[] args, Properties messages, ResourceBundle jar, Locale loc)
	{
		if (key.startsWith("i18n:"))
		{
			key = key.substring(5);
		}
		String message = null;
		try
		{
			if (jar != null)
			{
				try
				{
					message = jar.getString(key);
				}
				catch (Exception e)
				{
				}
			}
			if (message != null && messages.getProperty(key) == null)
			{
				return message;
			}
			message = messages.getProperty(key);
			if (message == null) return '!' + key + '!';
			message = Utils.stringReplace(message, "'", "''");
			MessageFormat mf = new MessageFormat(message);
			mf.setLocale(loc);
			return mf.format(args);
		}
		catch (MissingResourceException e)
		{
			return '!' + key + '!';
		}
		catch (Exception e)
		{
			return '!' + key + "!,txt:" + message + ", error:" + e.getMessage();
		}
	}


	private Properties getMessages(Locale locale)
	{
		Properties properties = null;
		synchronized (messages)
		{
			properties = messages.get(locale);
			if (properties == null)
			{
				properties = new Properties();
				Messages.loadMessagesFromDatabaseInternal(null, null, getSettings(), null, null, properties, locale, getFoundSetManager());
				if (getSolution() != null) //must be sure that solution is loaded, app might retrieve system messages, before solution loaded!
				{
					Messages.loadMessagesFromDatabaseInternal(DataSourceUtils.getI18NDataSource(getSolution(), getSettings()), null, getSettings(), null, null,
						properties, locale, getFoundSetManager());
					messages.put(locale, properties);
				}
			}
		}
		// also test here for the local jar message
		if (locale.equals(getLocale()))
		{
			localeJarMessages = ResourceBundle.getBundle(Messages.BUNDLE_NAME, locale);
		}

		return properties == null ? new Properties() : properties;
	}

	/*
	 * @see IServiceProvider#getI18NMessageIfPrefixed(String,Object[])
	 */
	public String getI18NMessageIfPrefixed(String key)
	{
		if (key != null && key.startsWith("i18n:"))
		{
			return getI18NMessage(key.substring(5), null);
		}
		return key;
	}

	public ItemFactory getItemFactory()
	{
		if (itemFactory == null)
		{
			synchronized (this)
			{
				if (itemFactory == null)
				{
					itemFactory = new SwingItemFactory(this);
				}
			}
		}
		return itemFactory;
	}

	public Locale getLocale()
	{
		String settingsLocale = getSettings().getProperty("locale.default");
		if (settingsLocale != null)
		{
			Locale loc = PersistHelper.createLocale(settingsLocale);
			if (loc != null) return loc;
		}
		return Locale.getDefault();
	}

	public JFrame getMainApplicationFrame()
	{
		return null;
	}

	public IModeManager getModeManager()
	{
		return getClient().getModeManager();
	}

	@Override
	public IMenuManager getMenuManager()
	{
		return getClient().getMenuManager();
	}

	public PageFormat getPageFormat()
	{
		if (pageFormat == null)
		{
			pageFormat = PersistHelper.createPageFormat(getSettings().getProperty("pageformat"));
			if (pageFormat == null)
			{
				pageFormat = new PageFormat();
			}
		}
		return pageFormat;
	}

	public IPluginAccess getPluginAccess()
	{
		getPluginManager();
		return pluginAccess;
	}

	public IPluginManager getPluginManager()
	{
		if (pluginManager == null)
		{
			pluginAccess = new ClientPluginAccessProvider(this);
			//getClient(); // do not create the client here, it needs to be created from within a job, otherwise the main thread
			// may be blocked on the awt thread which causes problems on the mac (debug SC does not paint)

			// make sure appserver is started here, plugin manager depends on Settings being initialized
			ServoyModelFinder.getServoyModelProvider().startAppServerIfNeeded();

			synchronized (this)
			{
				if (pluginManager == null)
				{
					pluginManager = new PluginManager(this, ApplicationServerRegistry.get().getBaseClassLoader())
					{
						/**
						 * @see com.servoy.j2db.smart.plugins.PluginManager#checkIfInitialized()
						 */
						@Override
						protected void checkIfInitialized()
						{
						}
					};
					pluginManager.loadClientPlugins(this);
					((FoundSetManager)getFoundSetManager()).setColumnManangers(pluginManager.getColumnValidatorManager(),
						pluginManager.getColumnConverterManager(), pluginManager.getUIConverterManager());
				}
			}
		}
		return pluginManager;
	}

	public RendererParentWrapper getPrintingRendererParent()
	{
		return getClient().getPrintingRendererParent();
	}

	public IApplicationServer getApplicationServer()
	{
		return getClient().getApplicationServer();
	}

	public IRepository getRepository()
	{
		return getClient().getRepository();
	}

	public IApplicationServerAccess getApplicationServerAccess()
	{
		return getClient().getApplicationServerAccess();
	}

	public boolean haveRepositoryAccess()
	{
		return true;
	}

	public ResourceBundle getResourceBundle(Locale locale)
	{
		return getClient().getResourceBundle(locale);
	}

	public Map getRuntimeProperties()
	{
		return new HashMap<Object, Object>();
	}

	public IExecutingEnviroment getScriptEngine()
	{
		if (getClient() != null) return getClient().getScriptEngine();
		return null;
	}

	public Remote getServerService(String name)
	{
		return getClient().getServerService(name);
	}

	public URL getServerURL()
	{
		return getClient().getServerURL();
	}

	public Properties getSettings()
	{
		return Settings.getInstance();
	}

	public Solution getSolution()
	{
		ServoyProject ap = ServoyModelFinder.getServoyModel().getActiveProject();
		return ap != null ? ap.getEditingSolution() : null;
	}

	public boolean isSolutionLoaded()
	{
		return getSolution() != null;
	}

	public ScheduledExecutorService getScheduledExecutor()
	{
		return getClient().getScheduledExecutor();
	}

	public TimeZone getTimeZone()
	{
		return TimeZone.getDefault();
	}

	public IUserManager getUserManager()
	{
		return getClient().getUserManager();
	}

	public String getUserName()
	{
		return getClient().getUserName();
	}

	public String getUserProperty(String name)
	{
		return getClient().getUserProperty(name);
	}

	public String[] getUserPropertyNames()
	{
		return getClient().getUserPropertyNames();
	}

	public String getUserUID()
	{
		return getClient().getUserUID();
	}

	public void handleException(String servoyMsg, Exception e)
	{
		getClient().handleException(servoyMsg, e);
	}

	public void handleClientUserUidChanged(String userUidBefore, String userUidAfter)
	{
		getClient().handleClientUserUidChanged(userUidBefore, userUidAfter);
	}

	public void invokeAndWait(Runnable r)
	{
		getClient().invokeAndWait(r);
	}

	public void invokeLater(Runnable r)
	{
		getClient().invokeLater(r);
	}

	public boolean isEventDispatchThread()
	{
		return getClient().isEventDispatchThread();
	}

	public boolean isRunningRemote()
	{
		// do not call getClient() here, this my be called from getPluginManager() within a sync block
		return false; // Design client is not running remote
	}

	public ImageIcon loadImage(String name)
	{
		return getClient().loadImage(name);
	}

	public void logout(final Object[] solution_to_open_args)
	{
		getClient().logout(solution_to_open_args);
	}

	public Object authenticate(String authenticator_solution, String method, Object[] credentials) throws RepositoryException
	{
		return getClient().authenticate(authenticator_solution, method, credentials);
	}

	public void output(Object msg)
	{
		getClient().output(msg, INFO);
	}

	public void output(Object msg, int level)
	{
		getClient().output(msg, level);
	}

	public void releaseGUI()
	{
		getClient().releaseGUI();
	}

	public void reportError(Component parentComponent, String msg, Object detail)
	{
		((ISmartClientApplication)getClient()).reportError(parentComponent, msg, detail);
	}

	public void reportError(String msg, Object detail)
	{
		getClient().reportError(msg, detail);
	}

	public void reportInfo(Component parentComponent, String msg, String title)
	{
		((ISmartClientApplication)getClient()).reportInfo(parentComponent, msg, title);
	}

	public void reportInfo(String msg)
	{
		getClient().reportInfo(msg);
	}

	public void reportJSError(String msg, Object detail)
	{
		getClient().reportJSError(msg, detail);
	}

	@Override
	public void reportJSWarning(String msg)
	{
		getClient().reportJSWarning(msg);
	}

	@Override
	public void reportJSWarning(String msg, Throwable t)
	{
		getClient().reportJSWarning(msg, t);
	}

	@Override
	public void reportJSInfo(String msg)
	{
		getClient().reportJSInfo(msg);
	}

	public void reportWarning(String msg)
	{
		getClient().reportWarning(msg);
	}

	public void reportWarningInStatus(String s)
	{
		getClient().reportWarningInStatus(s);
	}

	public void setI18NMessagesFilter(String columnname, String[] value)
	{
		getClient().setI18NMessagesFilter(columnname, value);
	}

	public void setLocale(Locale locale)
	{
		getClient().setLocale(locale);
	}

	public void setTimeZone(TimeZone zone)
	{
		getClient().setTimeZone(zone);
	}

	public void setPageFormat(PageFormat currentPageFormat)
	{
		getClient().setPageFormat(currentPageFormat);
	}

	public void setStatusProgress(int progress)
	{
		getClient().setStatusProgress(progress);
	}

	public void setStatusText(String text, String tooltip)
	{
		getClient().setStatusText(text, tooltip);
	}

	@Override
	public void showSolutionLoading(boolean loading)
	{
		getClient().showSolutionLoading(loading); // TODO I think this and many others should actually do nothing in design client
	}

	public void setTitle(String title)
	{
		getClient().setTitle(title);
	}

	public boolean putClientProperty(Object name, Object val)
	{
		return getClient().putClientProperty(name, val);
	}

	public Object getClientProperty(Object name)
	{
		return getClient().getClientProperty(name);
	}

	public void setUserProperty(String name, String value)
	{
		getClient().setUserProperty(name, value);
	}

	public void removeUserProperty(String name)
	{
		getClient().removeUserProperty(name);
	}

	public void removeAllUserProperties()
	{
		getClient().removeAllUserProperties();
	}

	public JComponent getEditLabel()
	{
		return ((J2DBClient)getClient()).getEditLabel();
	}


	public Dimension getScreenSize()
	{

		return getClient().getScreenSize();
	}


	public boolean showURL(String url, String target, String target_options, int timeout, boolean onRootFrame)
	{
		return getClient().showURL(url, target, target_options, timeout, onRootFrame);
	}

	public void refreshI18NMessages()
	{
		messages.clear();
	}

	public String getSolutionName()
	{
		return getClient().getSolutionName();
	}

	/**
	 * @see com.servoy.j2db.IMessagesCallback#getI18NColumnNameFilter()
	 */
	public String getI18NColumnNameFilter()
	{
		if (getClient() instanceof IMessagesCallback)
		{
			return ((IMessagesCallback)getClient()).getI18NColumnNameFilter();
		}
		return null;
	}

	/**
	 * @see com.servoy.j2db.IMessagesCallback#getI18NColumnValueFilter()
	 */
	public String[] getI18NColumnValueFilter()
	{
		if (getClient() instanceof IMessagesCallback)
		{
			return ((IMessagesCallback)getClient()).getI18NColumnValueFilter();
		}
		return null;
	}

	/**
	 * @see com.servoy.j2db.IMessagesCallback#messagesLoaded()
	 */
	public void messagesLoaded()
	{
		refreshI18NMessages();
	}

	public RuntimeWindowManager getRuntimeWindowManager()
	{
		return getClient().getRuntimeWindowManager();
	}

	public void looseFocus()
	{
		getClient().looseFocus();
	}

	/**
	 * @see com.servoy.j2db.ISmartClientApplication#addURLStreamHandler(java.lang.String, java.net.URLStreamHandler)
	 */
	public void addURLStreamHandler(String protocolName, URLStreamHandler handler)
	{

	}

	public void registerWindow(String name, Window d)
	{

	}

	public Window getWindow(String name)
	{
		return null;
	}

	public IToolbarPanel getToolbarPanel()
	{
		return null;
	}

	public String showI18NDialog(String preselect_key, String preselect_language)
	{
		return null;
	}

	public Date showCalendar(String pattern, Date date)
	{
		return null;
	}

	public String showColorChooser(String originalColor)
	{
		return null;
	}

	public String showFontChooser(String font)
	{
		return null;
	}

	public void beep()
	{

	}

	public void setClipboardContent(String string)
	{

	}

	public String getClipboardString()
	{
		return null;
	}

	public void setNumpadEnterAsFocusNextEnabled(boolean enabled)
	{

	}

	public int exportObject(Remote object) throws RemoteException
	{
		return 0;
	}

	public void setPaintTableImmediately(boolean b)
	{

	}

	public int getPaintTableImmediately()
	{
		return 0;
	}

	public void updateInsertModeIcon(IDisplay display)
	{

	}

	private boolean isFormElementsEditableInFindMode = true;

	public void setFormElementsEditableInFindMode(boolean editable)
	{
		isFormElementsEditableInFindMode = editable;
	}

	public boolean isFormElementsEditableInFindMode()
	{
		return isFormElementsEditableInFindMode;
	}

	public void setValueListItems(String name, Object[] displayValues, Object[] realValues, boolean autoconvert)
	{
		client.setValueListItems(name, displayValues, realValues, autoconvert);
	}

	@Override
	public String getFormNameFor(IComponent component)
	{
		if (component instanceof Component)
		{
			Container parent = ((Component)component).getParent();
			while (!(parent instanceof IFormUI))
			{
				parent = parent.getParent();
			}
			return ((IFormUI)parent).getController().getName();
		}
		return "";
	}

	@Override
	public IEventsManager getEventsManager()
	{
		return getClient().getEventsManager();
	}

	public IPermissionManager getPermissionManager()
	{
		return getClient().getPermissionManager();
	}

	@Override
	public IValueListManager getValueListManager()
	{
		return getClient().getValueListManager();
	}

	@Override
	public IJSFormManager getJSFormManager()
	{
		return getClient().getJSFormManager();
	}
}
