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
package com.servoy.eclipse.designer;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.ImageIcon;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.util.BundleUtility;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.sablo.Container;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.websocket.IWebsocketSession;
import org.sablo.websocket.IWebsocketSessionFactory;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.eclipse.core.I18NChangeListener;
import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;
import com.servoy.j2db.server.ngclient.design.DesignNGClient;
import com.servoy.j2db.server.ngclient.design.DesignNGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.design.IDesignerSolutionProvider;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin
{

	// The plug-in ID
	public static final String PLUGIN_ID = "com.servoy.eclipse.designer";

	// The shared instance
	private static Activator plugin;

	private I18NChangeListener i18nChangeListener;

	private final Map<String, ImageIcon> imageIcons = new HashMap<String, ImageIcon>();

	private DesignNGClient client = null;

	/**
	 * The constructor
	 */
	public Activator()
	{
	}

	private final class DeveloperDesignClient extends DesignNGClient implements IPersistChangeListener
	{
		/**
		 * @author user
		 *
		 */
		private final class SendCSSFile implements Runnable
		{
			private final Solution solution;

			/**
			 * @param name
			 */
			public SendCSSFile(Solution solution)
			{
				this.solution = solution;
			}

			@Override
			public void run()
			{
				getWebsocketSession().solutionLoaded(solution);
			}
		}

		/**
		 * @author user
		 *
		 */
		private final class FormUpdater implements Runnable
		{
			/**
			 *
			 */
			private final Map<Form, List<IFormElement>> frms;
			/**
			 *
			 */
			private final Form changedForm;

			/**
			 * @param frms
			 * @param changedForm
			 * @param changedSolution
			 */
			private FormUpdater(Map<Form, List<IFormElement>> frms, Form changedForm)
			{
				this.frms = frms;
				this.changedForm = changedForm;
			}

			@Override
			public void run()
			{
				for (Entry<Form, List<IFormElement>> entry : frms.entrySet())
				{
					List<IFormController> cachedFormControllers = getFormManager().getCachedFormControllers(entry.getKey());
					ServoyDataConverterContext cntxt = new ServoyDataConverterContext(DeveloperDesignClient.this);
					for (IFormController fc : cachedFormControllers)
					{
						boolean bigChange = false;
						outer : for (IFormElement persist : entry.getValue())
						{
							if (persist.getParent().getChild(persist.getUUID()) == null)
							{
								// deleted persist
								bigChange = true;
								break;
							}
							FormElement newFe = new FormElement(persist, cntxt.getSolution(), new PropertyPath(), true);

							IWebFormUI formUI = (IWebFormUI)fc.getFormUI();
							WebFormComponent webComponent = findWebComponent(formUI.getComponents(), newFe.getName());
							if (webComponent != null)
							{
								FormElement existingFe = webComponent.getFormElement();

								WebComponentSpecification spec = webComponent.getSpecification();
								Set<String> allKeys = new HashSet<String>();
								allKeys.addAll(newFe.getRawPropertyValues().keySet());
								allKeys.addAll(existingFe.getRawPropertyValues().keySet());
								for (String property : allKeys)
								{
									Object currentPropValue = existingFe.getPropertyValue(property);
									Object newPropValue = newFe.getPropertyValue(property);
									if (!Utils.equalObjects(currentPropValue, newPropValue))
									{
										if (spec.getHandler(property) != null)
										{
											// this is a handler change so a big change (component could react to a handler differently)
											bigChange = true;
											break outer;
										}
										if (property.equals("formIndex"))
										{
											bigChange = true;
											break outer;
										}
										if (webComponent.getParent() != formUI && (property.equals("location") || property.equals("size")))
										{
											bigChange = true;
											break outer;
										}
										PropertyDescription prop = spec.getProperty(property);
										if (prop != null)
										{
											if (prop.getType() == DataproviderPropertyType.INSTANCE)
											{
												// if it is a portal based component then the dataprovider is only the last part for this webcomponent
												// so if the new value ends with the current value then it is still the same and it is not a big change (this also doesn't have to be set on the component)
												if (webComponent.getParent() == formUI ||
													!((newPropValue instanceof String) && (currentPropValue instanceof String) && ((String)newPropValue).endsWith((String)currentPropValue)))
												{
													// this is a design property change so a big change
													bigChange = true;
													break outer;
												}
												continue;
											}
											else if ("design".equals(prop.getScope()))
											{
												// this is a design property change so a big change
												bigChange = true;
												break outer;
											}
											if (property.equals("tabs"))
											{
												bigChange = true;
												break outer;
											}
											webComponent.setFormElement(newFe);
											webComponent.setProperty(property, newFe.getPropertyValueConvertedForWebComponent(property, webComponent,
												formUI.getDataAdapterList() instanceof DataAdapterList ? (DataAdapterList)formUI.getDataAdapterList() : null));

										}
									}
								}
							}
							else
							{
								// no webcomponent found, so new one or name change, recreate all
								bigChange = true;
								break;
							}
						}
						if (bigChange)
						{
							fc.recreateUI();
							getWebsocketSession().getService(DesignNGClientWebsocketSession.EDITOR_CONTENT_SERVICE).executeAsyncServiceCall("refreshGhosts",
								new Object[] { });
						}
					}
				}
				if (changedForm != null)
				{
					Form form = ModelUtils.getEditingFlattenedSolution(changedForm).getFlattenedForm(changedForm);
					List<IFormController> cachedFormControllers = getFormManager().getCachedFormControllers(changedForm);
					for (IFormController iFormController : cachedFormControllers)
					{
						iFormController.recreateUI();
					}
					getWebsocketSession().getService(DesignNGClientWebsocketSession.EDITOR_CONTENT_SERVICE).executeAsyncServiceCall("refreshGhosts",
						new Object[] { });
					if (!form.getLayoutContainers().hasNext())
					{
						getWebsocketSession().getService(DesignNGClientWebsocketSession.EDITOR_CONTENT_SERVICE).executeAsyncServiceCall(
							"updateForm",
							new Object[] { changedForm.getName(), changedForm.getUUID().toString(), Integer.valueOf((int)form.getSize().getWidth()), Integer.valueOf((int)form.getSize().getHeight()) });
					}
				}
				else
				{
					getWebsocketSession().getService(DesignNGClientWebsocketSession.EDITOR_CONTENT_SERVICE).executeAsyncServiceCall("refreshDecorators",
						new Object[] { });
				}
			}

			/**
			 * @param components
			 * @param name
			 * @return
			 */
			private WebFormComponent findWebComponent(Collection<WebComponent> components, String name)
			{
				if (components == null) return null;
				for (WebComponent webComponent : components)
				{
					if (webComponent.getName().equals(name) && webComponent instanceof WebFormComponent) return (WebFormComponent)webComponent;
					if (webComponent instanceof Container)
					{
						WebFormComponent comp = findWebComponent(((Container)webComponent).getComponents(), name);
						if (comp != null) return comp;
					}
				}
				return null;
			}
		}

		private final IDesignerSolutionProvider solutionProvider;


		/**
		 * @param wsSession
		 * @param solutionProvider
		 */
		private DeveloperDesignClient(INGClientWebsocketSession wsSession, IDesignerSolutionProvider solutionProvider) throws Exception
		{
			super(wsSession);
			this.solutionProvider = solutionProvider;
			solutionProvider.addPersistListener(this);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.NGClient#shutDown(boolean)
		 */
		@Override
		public synchronized void shutDown(boolean force)
		{
			super.shutDown(force);
			if (solutionProvider != null) solutionProvider.removePersistListener(this);
		}

		@Override
		protected IActiveSolutionHandler createActiveSolutionHandler()
		{
			return new AbstractActiveSolutionHandler(getApplicationServer())
			{

				@Override
				protected Solution loadSolution(RootObjectMetaData solutionDef) throws RemoteException, RepositoryException
				{
					return solutionProvider.getEditingSolution(solutionDef.getName());
				}

				@Override
				public IRepository getRepository()
				{
					try
					{
						return getApplicationServerAccess().getRepository();
					}
					catch (RemoteException e)
					{
						Debug.error(e);
					}
					return null;
				}
			};
		}

		@Override
		public void loadSolution(String solutionName) throws com.servoy.j2db.persistence.RepositoryException
		{
			SolutionMetaData metaData = (SolutionMetaData)solutionProvider.getActiveEditingSolution().getMetaData();
			if (getSolution() == null || !getSolution().getName().equals(metaData.getName()))
			{
				loadSolution(metaData);
			}
			// fake this so that it seems to be in solution model node.
			solutionRoot.getSolutionCopy(true);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.persistence.IPersistChangeListener#persistChanges(java.util.Collection)
		 */
		@Override
		public void persistChanges(Collection<IPersist> changes)
		{
			final Map<Form, List<IFormElement>> frms = new HashMap<Form, List<IFormElement>>();
			Form changedForm = null;
			Media cssFile = null;
			for (IPersist persist : changes)
			{

				if (persist instanceof IFormElement || persist instanceof Tab)
				{
					IPersist parent = persist;
					if (persist instanceof Tab)
					{
						parent = ((Tab)persist).getParent();
						persist = parent;
					}
					while (parent != null)
					{
						if (parent instanceof Form)
						{
							List<IFormElement> list = frms.get(parent);
							if (list == null)
							{
								list = new ArrayList<IFormElement>();
								frms.put((Form)parent, list);
							}
							list.add((IFormElement)persist);
							break;
						}
						parent = parent.getParent();
					}
				}
				else if (persist instanceof Form)
				{
					changedForm = (Form)persist;
				}
				else if (persist instanceof Part)
				{
					changedForm = (Form)persist.getParent();
				}
				else if (persist instanceof LayoutContainer)
				{
					changedForm = (Form)persist.getAncestor(IRepository.FORMS);
				}
				else if (persist instanceof Media)
				{
					if (((Media)persist).getName().endsWith(".css"))
					{
						cssFile = (Media)persist;
					}
				}
			}
			if (frms.size() > 0 || changedForm != null)
			{
				getWebsocketSession().getEventDispatcher().addEvent(new FormUpdater(frms, changedForm));
			}
			if (cssFile != null)
			{
				ISupportChilds parent = cssFile.getParent();
				if (parent instanceof Solution)
				{
					Solution theSolution = (Solution)parent;
					//TODO change to commented code once IPersistChangeListener is notified only the modified file, not all media files: case SVY-7581
					/*
					 * Object property = theSolution.getProperty(StaticContentSpecLoader.PROPERTY_STYLESHEET.getPropertyName()); if
					 * (property.equals(cssFile.getID()) || (Integer)property == 0) getWebsocketSession().getEventDispatcher().addEvent( new
					 * SendCSSFile(theSolution));
					 */
					getWebsocketSession().getEventDispatcher().addEvent(new SendCSSFile(theSolution));
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception
	{
		ModelUtils.assertUINotDisabled(PLUGIN_ID);

		super.start(context);
		plugin = this;
		ServoyModelManager.getServoyModelManager().getServoyModel().addI18NChangeListener(i18nChangeListener = new I18NChangeListener()
		{
			public void i18nChanged()
			{
				com.servoy.eclipse.core.Activator.getDefault().getDesignClient().refreshI18NMessages();

				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null &&
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null)
				{
					IEditorReference[] editorRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
					for (IEditorReference editorRef : editorRefs)
					{
						IEditorPart editor = editorRef.getEditor(false);
						if (editor instanceof BaseVisualFormEditor)
						{
							((BaseVisualFormEditor)editor).refreshGraphicalEditor();
						}
					}
				}
			}
		});

		ServoyModelManager.getServoyModelManager().getServoyModel().addActiveProjectListener(new IActiveProjectListener()
		{

			@Override
			public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
			{
				if (client != null)
				{
					if (toProject.getProject().getName().startsWith("import_placeholder"))
					{
						if (client.isSolutionLoaded())
						{
							client.closeSolution(true, null);
						}
					}
					client.shutDown(true);
				}
				return true;
			}

			@Override
			public void activeProjectChanged(ServoyProject activeProject)
			{

			}

			@Override
			public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
			{

			}
		});

		if (ApplicationServerRegistry.getServiceRegistry() != null)
		{
			final IDebugClientHandler service = ApplicationServerRegistry.getServiceRegistry().getService(IDebugClientHandler.class);
			if (service != null)
			{
				WebsocketSessionManager.setWebsocketSessionFactory(WebsocketSessionFactory.DESIGN_ENDPOINT, new IWebsocketSessionFactory()
				{
					@Override
					public IWebsocketSession createSession(String uuid) throws Exception
					{
						DesignNGClientWebsocketSession designerSession = new DesignNGClientWebsocketSession(uuid);
						final IDesignerSolutionProvider solutionProvider = ApplicationServerRegistry.getServiceRegistry().getService(
							IDesignerSolutionProvider.class);
						Activator.this.client = new DeveloperDesignClient(designerSession, solutionProvider);
						designerSession.setClient(Activator.this.client);
						return designerSession;
					}
				});
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		if (ServoyModelManager.getServoyModelManager().isServoyModelCreated())
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().removeI18NChangeListener(i18nChangeListener);
		}
		imageIcons.clear();
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault()
	{
		return plugin;
	}

	/**
	 * Get an image with the given name from this plugin's bundle.
	 *
	 * @param name the name of the image file.
	 * @return the image descriptor for the file.
	 */
	public static ImageDescriptor loadImageDescriptorFromBundle(String name)
	{
		return imageDescriptorFromPlugin(PLUGIN_ID, "$nl$/icons/" + name);
	}

	/**
	 * Get an swing image icon with the given name from this plugin's bundle.
	 *
	 * @param name the name of the image file.
	 * @return the image icon for the file.
	 */
	public ImageIcon loadImageIconFromBundle(String name)
	{
		if (name == null)
		{
			throw new IllegalArgumentException();
		}

		ImageIcon imageIcon = imageIcons.get(name);
		if (imageIcon == null)
		{
			// if the bundle is not ready then there is no image
			Bundle bundle = Platform.getBundle(PLUGIN_ID);
			if (!BundleUtility.isReady(bundle))
			{
				return null;
			}

			String imageFilePath = "$nl$/icons/" + name;
			// look for the image (this will check both the plugin and fragment folders
			URL fullPathString = BundleUtility.find(bundle, imageFilePath);

			if (fullPathString == null)
			{
				try
				{
					fullPathString = new URL(imageFilePath);
				}
				catch (MalformedURLException e)
				{
					return null;
				}
			}

			imageIcons.put(name, imageIcon = new ImageIcon(fullPathString, fullPathString.toExternalForm().intern()));
		}
		return imageIcon;
	}

}
