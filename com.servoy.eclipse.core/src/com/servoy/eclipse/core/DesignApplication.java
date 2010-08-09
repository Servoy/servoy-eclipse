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
package com.servoy.eclipse.core;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.print.PageFormat;
import java.net.URL;
import java.net.URLStreamHandler;
import java.rmi.Remote;
import java.text.MessageFormat;
import java.util.HashMap;
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

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.ILAFManager;
import com.servoy.j2db.IMessagesCallback;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.Messages;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.DataServerProxy;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IClientHost;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.dataprocessing.IGlobalValueEntry;
import com.servoy.j2db.dataprocessing.SwingFoundSetFactory;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.plugins.IPluginAccess;
import com.servoy.j2db.plugins.IPluginManager;
import com.servoy.j2db.plugins.PluginManager;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.smart.dataui.SwingItemFactory;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.ITaskExecuter;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.toolbar.IToolbarPanel;

/**
 * @author jcompagner
 * 
 */
@SuppressWarnings("nls")
public class DesignApplication implements IApplication, IMessagesCallback
{

	private IApplication client;
	private SwingItemFactory itemFactory;
	private volatile IFoundSetManagerInternal foundSetManager;

	private ResourceBundle localeJarMessages;
	private final HashMap<Locale, Properties> messages = new HashMap<Locale, Properties>();
	private PluginManager pluginManager;
	private PageFormat pageFormat;
	private IBeanManager beanManager;

	DesignApplication()
	{
	}

	public void addURLStreamHandler(String protocolName, URLStreamHandler handler)
	{
		getClient().addURLStreamHandler(protocolName, handler);
	}

	/**
	 * @return
	 */
	IApplication getClient()
	{
		if (client == null)
		{
			client = Activator.getDefault().getDebugJ2DBClient();
		}
		return client;
	}

	public void blockGUI(String reason)
	{
		getClient().blockGUI(reason);
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

	public IBeanManager getBeanManager()
	{
		if (beanManager == null)
		{
			beanManager = ApplicationServerSingleton.get().createBeanManager(getPluginManager().getClassLoader());
		}
		return beanManager;
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

	public DataServerProxy proxyDataServer()
	{
		return getClient().proxyDataServer();
	}

	public IClientHost getClientHost()
	{
		return getClient().getClientHost();
	}

	public JMenu getExportMenu()
	{
		return getClient().getExportMenu();
	}

	public FlattenedSolution getFlattenedSolution()
	{
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		return activeProject == null ? null : activeProject.getEditingFlattenedSolution();
	}

	public IFormManager getFormManager()
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
					foundSetManager = new FoundSetManager(this, null, new SwingFoundSetFactory())
					{
						/**
						 * @see com.servoy.j2db.dataprocessing.FoundSetManager#getGlobalScopeProvider()
						 */
						@Override
						public IGlobalValueEntry getGlobalScopeProvider()
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
		if (key == null || key.length() == 0) return key;

		Properties properties = getMessages(getLocale());
		return getI18NMessage(key, args, properties, localeJarMessages, getLocale());
	}

	public synchronized String getI18NMessage(String key)
	{
		if (key == null || key.length() == 0) return key;

		Properties properties = getMessages(getLocale());
		return getI18NMessage(key, null, properties, localeJarMessages, getLocale());
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
		if (key.startsWith("i18n:")) //$NON-NLS-1$
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
			message = Utils.stringReplace(message, "'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
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
			return '!' + key + "!,txt:" + message + ", error:" + e.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
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
				Messages.loadMessagesFromDatabase(null, getClientInfo().getClientId(), getSettings(), getClient().getDataServer(), getRepository(), properties,
					locale);
				if (getSolution() != null) //must be sure that solution is loaded, app might retrieve system messages, before solution loaded!
				{
					Messages.loadMessagesFromDatabase(getSolution(), getClientInfo().getClientId(), getSettings(), getClient().getDataServer(),
						getRepository(), properties, locale);
					messages.put(locale, properties);
				}
			}
		}
		// also test here for the local jar message
		if (localeJarMessages == null && locale.equals(getLocale()))
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

	public JMenu getImportMenu()
	{
		return getClient().getImportMenu();
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

	public ILAFManager getLAFManager()
	{
		return getClient().getLAFManager();
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
		return getClient().getMainApplicationFrame();
	}

	public IModeManager getModeManager()
	{
		return getClient().getModeManager();
	}

	public PageFormat getPageFormat()
	{
		if (pageFormat == null)
		{
			pageFormat = PersistHelper.createPageFormat(getSettings().getProperty("pageformat")); //$NON-NLS-1$
			if (pageFormat == null)
			{
				pageFormat = new PageFormat();
			}
		}
		return pageFormat;
	}

	public IPluginAccess getPluginAccess()
	{
		return getClient().getPluginAccess();
	}

	public IPluginManager getPluginManager()
	{
		if (pluginManager == null)
		{
			//getClient(); // do not create the client here, it needs to be created from within a job, otherwise the main thread 
			// may be blocked on the awt thread which causes problems on the mac (debug SC does not paint)

			// make sure appserver is started here, plugin manager depends on Settings being initialized
			ServoyModel.startAppServer();
			synchronized (this)
			{
				if (pluginManager == null)
				{
					pluginManager = new PluginManager(this)
					{
						/**
						 * @see com.servoy.j2db.plugins.PluginManager#checkIfInitialized()
						 */
						@Override
						protected void checkIfInitialized()
						{
						}
					};
					pluginManager.loadClientPlugins(this);
					((FoundSetManager)getFoundSetManager()).setColumnManangers(pluginManager.getColumnValidatorManager(),
						pluginManager.getColumnConverterManager());
				}
			}
		}
		return pluginManager;
	}

	public Container getPrintingRendererParent()
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
		return getClient().getRuntimeProperties();
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
		ServoyProject ap = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		return ap != null ? ap.getEditingSolution() : null;
	}

	public ITaskExecuter getThreadPool()
	{
		return getClient().getThreadPool();
	}

	public ScheduledExecutorService getScheduledExecutor()
	{
		return getClient().getScheduledExecutor();
	}

	public TimeZone getTimeZone()
	{
		return TimeZone.getDefault();
	}

	public IToolbarPanel getToolbarPanel()
	{
		return getClient().getToolbarPanel();
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

	public Window getWindow(String name)
	{
		return getClient().getWindow(name);
	}

	public String getCurrentWindowName()
	{
		return getClient().getCurrentWindowName();
	}

	public void setCurrentWindowName(String name)
	{
		getClient().setCurrentWindowName(name);
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

	public void registerWindow(String name, Window d)
	{
		getClient().registerWindow(name, d);
	}

	public void releaseGUI()
	{
		getClient().releaseGUI();
	}

	public void reportError(Component parentComponent, String msg, Object detail)
	{
		getClient().reportError(parentComponent, msg, detail);
	}

	public void reportError(String msg, Object detail)
	{
		getClient().reportError(msg, detail);
	}

	public void reportInfo(Component parentComponent, String msg, String title)
	{
		getClient().reportInfo(parentComponent, msg, title);
	}

	public void reportInfo(String msg)
	{
		getClient().reportInfo(msg);
	}

	public void reportJSError(String msg, Object detail)
	{
		getClient().reportJSError(msg, detail);
	}

	public void reportWarning(String msg)
	{
		getClient().reportWarning(msg);
	}

	public void reportWarningInStatus(String s)
	{
		getClient().reportWarningInStatus(s);
	}

	public void setI18NMessagesFilter(String columnname, String value)
	{
		getClient().setI18NMessagesFilter(columnname, value);
	}

	public void setLocale(Locale locale)
	{
		getClient().setLocale(locale);
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

	public void setTitle(String title)
	{
		getClient().setTitle(title);
	}

	public boolean setUIProperty(Object name, Object val)
	{
		return getClient().setUIProperty(name, val);
	}

	public Object getUIProperty(Object name)
	{
		return getClient().getUIProperty(name);
	}

	public void setUserProperty(String name, String value)
	{
		getClient().setUserProperty(name, value);
	}

	public void updateInsertMode(IDisplay display)
	{
		getClient().updateInsertMode(display);
	}

	public JComponent getEditLabel()
	{
		return ((J2DBClient)getClient()).getEditLabel();
	}


	public Dimension getScreenSize()
	{

		return getClient().getScreenSize();
	}


	public boolean showURL(String url, String target, String target_options, int timeout_ms)
	{
		return getClient().showURL(url, target, target_options, timeout_ms);
	}

	/**
	 * 
	 */
	public void refreshI18NMessages()
	{
		messages.clear();
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
	public String getI18NColumnValueFilter()
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

	public Rectangle getWindowBounds(String windowName)
	{
		return getClient().getWindowBounds(windowName);
	}
}
