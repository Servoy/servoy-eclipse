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

package com.servoy.eclipse.designer.editor.rfb;

import java.awt.Dimension;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.gef.ui.actions.SelectAllAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.json.JSONObject;
import org.sablo.eventthread.WebsocketSessionWindows;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.WebsocketSessionKey;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.eclipse.cheatsheets.actions.ISupportCheatSheetActions;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.core.resource.DesignPagetype;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.rfb.actions.CopyAction;
import com.servoy.eclipse.designer.editor.rfb.actions.CutAction;
import com.servoy.eclipse.designer.editor.rfb.actions.DeleteAction;
import com.servoy.eclipse.designer.editor.rfb.actions.FixedSelectAllAction;
import com.servoy.eclipse.designer.editor.rfb.actions.PasteAction;
import com.servoy.eclipse.designer.outline.FormOutlinePage;
import com.servoy.eclipse.designer.rfb.endpoint.EditorHttpSession;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.IFormComponentListener;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.ngpackages.ILoadedNGPackagesListener;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.util.DefaultFieldPositioner;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.SelectionProviderAdapter;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Design page for browser based rfb editor.
 *
 * @author rgansevles
 *
 */
public abstract class RfbVisualFormEditorDesignPage extends BaseVisualFormEditorDesignPage implements ISupportCheatSheetActions
{
	private static final AtomicInteger clientNrgenerator = new AtomicInteger();

	// for setting selection when clicked in editor
	private final ISelectionProvider selectionProvider = new SelectionProviderAdapter()
	{
		@Override
		public ISelection getSelection()
		{
			// select form if nothing else is selected
			return selection == null ? new StructuredSelection(editorPart.getForm()) : selection;
		}
	};

	private final IFieldPositioner fieldPositioner = new DefaultFieldPositioner(new Point(40, 50))
	{
		@Override
		public Point getNextLocation(Point location)
		{
			if (location == null)
			{
				// should we use designerPreferences.getCopyPasteOffset() here ?
				Point point = new Point(defaultLocation.x, defaultLocation.y);
				defaultLocation.x += 20;
				defaultLocation.y += 20;
				return point;
			}
			return location;
		}
	};

	// for updating selection in editor when selection changes in IDE
	private RfbSelectionListener selectionListener;

	private RfbDirtyListener dirtyListener;

	private EditorWebsocketSession editorWebsocketSession;
	private DesignerWebsocketSession designerWebsocketSession;

	private EditorServiceHandler editorServiceHandler;

	private String layout = null;
	private String formName = null;
	private int extendsId = -1;
	protected WebsocketSessionKey editorKey = null;
	protected WebsocketSessionKey clientKey = null;
	private AbstractContainer showedContainer;
	private final Stack<AbstractContainer> zoomed = new Stack<>();

	private final PartListener partListener = new PartListener();

	private final DesignerPreferences designerPreferences = new DesignerPreferences();

	public RfbVisualFormEditorDesignPage(BaseVisualFormEditor editorPart)
	{
		super(editorPart);
	}

	@Override
	public DesignPagetype getDesignPagetype()
	{
		return DesignPagetype.Rfb;
	}

	@Override
	public void createPartControl(Composite parent)
	{
		// why would we reload the specs if nothing changed? I this is no longer needed now
//		// always reload the current spec so that always the latest stuff is shown.
//		FormElementHelper.INSTANCE.reload(); // we can't reload just specs cause lately FormElement can add size/location/anchors to spec and we don't want to use old/cached/already initialized form elements while new specs were reloaded

		// Serve requests for rfb editor

		String httpSessionId = EditorHttpSession.getInstance().getId();
		editorKey = new WebsocketSessionKey(httpSessionId, clientNrgenerator.incrementAndGet());
		clientKey = new WebsocketSessionKey(httpSessionId, clientNrgenerator.incrementAndGet());

		WebsocketSessionManager.addSession(editorWebsocketSession = new EditorWebsocketSession(editorKey));
		WebsocketSessionManager.addSession(designerWebsocketSession = new DesignerWebsocketSession(clientKey, editorPart));
		selectionListener = new RfbSelectionListener(editorPart.getForm(), editorWebsocketSession, selectionProvider);
		getSite().setSelectionProvider(selectionProvider);
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);

		editorServiceHandler = new EditorServiceHandler(editorPart, selectionProvider, selectionListener, fieldPositioner);

		editorWebsocketSession.registerServerService("formeditor", editorServiceHandler);
		ServoyModelFinder.getServoyModel().getNGPackageManager().addLoadedNGPackagesListener(partListener);
		ServoyModelFinder.getServoyModel().addFormComponentListener(partListener);
		ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(true, partListener);
		designerPreferences.addPreferenceChangeListener(partListener);
		createBrowser(parent);

		refreshBrowserUrl(false);
		openViewers();

		dirtyListener = new RfbDirtyListener(editorPart, editorWebsocketSession);
		editorPart.addPropertyListener(dirtyListener);
	}

	protected abstract void createBrowser(Composite parent);

	@Override
	public Object getAdapter(Class type)
	{
		Object adapter = super.getAdapter(type);

		if (type.equals(IContentOutlinePage.class))
		{
			adapter = new FormOutlinePage(editorPart.getForm(), null, getActionRegistry());
		}

		return adapter;
	}

	/**
	 * @param form
	 * @return
	 */
	private String computeLayout(Form form, boolean isCSSPositionContainer)
	{
		if (isCSSPositionContainer) return "csspos";
		if (form.isResponsiveLayout()) return "flow";
		if (form.getUseCssPosition()) return "csspos";
		return "absolute";
	}

	@Override
	public void refreshPersists(List<IPersist> persists, boolean fullRefresh)
	{
		this.partListener.refreshPersists(persists, fullRefresh);
	}

	public void refreshPalette()
	{
		CurrentWindow.runForWindow(new WebsocketSessionWindows(editorWebsocketSession), new Runnable()
		{
			@Override
			public void run()
			{
				editorWebsocketSession.getClientService(EditorWebsocketSession.EDITOR_SERVICE).executeAsyncServiceCall("refreshPalette", new Object[] { });
				editorWebsocketSession.valueChanged();
			}
		});
	}

	public void refreshBrowserUrl(boolean force)
	{
		Form form = editorPart.getForm();
		Form flattenedForm = ModelUtils.getEditingFlattenedSolution(form).getFlattenedForm(form);
		if (flattenedForm == null) return; // form is already deleted?

		boolean isCSSPositionContainer = false;
		if (showedContainer instanceof LayoutContainer)
		{
			isCSSPositionContainer = CSSPositionUtils.isCSSPositionContainer((LayoutContainer)showedContainer);
		}

		String newLayout = computeLayout(flattenedForm, isCSSPositionContainer);
		if (!Utils.equalObjects(layout, newLayout) || !Utils.equalObjects(formName, flattenedForm.getName()) || extendsId != flattenedForm.getExtendsID() ||
			force)
		{
			layout = newLayout;
			formName = flattenedForm.getName();
			extendsId = flattenedForm.getExtendsID();
			boolean hideDefault = EditorUtil.hideDefaultComponents(form);
			boolean marqueeSelectOuter = designerPreferences.getMarqueeSelectOuter();
			Dimension formSize = flattenedForm.getSize();
			if (isCSSPositionContainer) formSize = showedContainer.getSize();
			final String path = "angular2";
			final String url = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/rfb/" + path + "/index.html?s=" +
				form.getSolution().getName() + "&l=" + layout + "&f=" + form.getName() + "&w=" + formSize.getWidth() + "&h=" + formSize.getHeight() +
				"&clientnr=" + editorKey.getClientnr() + "&c_clientnr=" + clientKey.getClientnr() + "&hd=" + hideDefault + "&mso=" + marqueeSelectOuter +
				(showedContainer != null ? ("&cont=" + showedContainer.getID()) : "") + (form.isFormComponent().booleanValue() ? "&fc=true" : "");
			final Runnable runnable = new Runnable()
			{
				public void run()
				{
					try
					{
						ServoyLog.logInfo("Browser url for editor: " + url);
						showUrl(url);
					}
					catch (Exception ex)
					{
						ServoyLog.logError("couldn't load the editor: " + url, ex);
					}
				}
			};
			if ("true".equals(System.getProperty("svy.tomcat.started")))
			{
				UIUtils.runInUI(runnable, false);
			}
			else
			{
				// tomcat not yet started, delay a bit set url
				Job urlSetter = new Job("Set browser url")
				{
					@Override
					protected IStatus run(IProgressMonitor monitor)
					{
						Display.getDefault().asyncExec(runnable);
						return Status.OK_STATUS;
					}
				};
				urlSetter.setPriority(Job.LONG);
				urlSetter.schedule();
			}
		}
	}

	public void requestSelection()
	{
		selectionListener.updateSelection(true);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		super.init(site, input);
		getSite().getPage().addPartListener(partListener);
	}

	@Override
	public void dispose()
	{
		super.dispose();
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
		getSite().setSelectionProvider(null);
		getSite().getPage().removePartListener(partListener);
		WebsocketSessionManager.removeSession(editorWebsocketSession.getSessionKey());
		WebsocketSessionManager.removeSession(designerWebsocketSession.getSessionKey());
		ServoyModelFinder.getServoyModel().getNGPackageManager().removeLoadedNGPackagesListener(partListener);
		ServoyModelFinder.getServoyModel().removeFormComponentListener(partListener);
		designerPreferences.removePreferenceChangeListener(partListener);
	}

//	protected IWebsocketSession getContentWebsocketSession()
//	{
//		return designerWebsocketSession;
//	}
//
//	protected IWindow getContentWindow()
//	{
//		IWebsocketSession editorContentWebsocketSession = getContentWebsocketSession();
//		if (editorContentWebsocketSession != null)
//		{
//			return editorContentWebsocketSession.getActiveWindow(getEditorPart().getForm().getName());
//		}
//
//		return null;
//	}

	@Override
	public void refreshAllParts()
	{
		// TODO new impl
//		IWindow window = getContentWindow();
//		if (window != null)
//		{
//			CurrentWindow.runForWindow(window, new Runnable()
//			{
//				@Override
//				public void run()
//				{
//					IWebsocketSession editorContentWebsocketSession = getContentWebsocketSession();
//					editorContentWebsocketSession.getEventDispatcher().addEvent(
//						new FormUpdater(editorContentWebsocketSession, null, Arrays.asList(new Form[] { getEditorPart().getForm() })));
//				}
//			});
//		}
	}

	public void revert()
	{
		// TODO new impl
//		if (getContentWebsocketSession() != null && CurrentWindow.exists())
//		{
//			List<IFormController> cachedFormControllers = getContentWebsocketSession().getClient().getFormManager().getCachedFormControllers(
//				editorPart.getForm());
//			for (IFormController iFormController : cachedFormControllers)
//			{
//				iFormController.recreateUI();
//			}
//		}
	}

	/**
	 * @param persists
	 * @param form
	 * @return
	 */
	private Set<IPersist> filterByParent(List<IPersist> persists, Form form)
	{
		// first add the stuff of the form itself to the map.
		Set<IPersist> filtered = new HashSet<>();
		if (persists != null)
		{
			for (IPersist persist : persists)
			{
				if (showedContainer != null)
				{
					IPersist parent = persist;
					while (parent != null)
					{
						if (parent == showedContainer)
						{
							filtered.add(persist);
							break;
						}
						parent = parent.getParent();
					}
				}
				else
				{
					IPersist ancestor = persist.getAncestor(IRepository.FORMS);
					if (ancestor != null && ancestor.getUUID().equals(form.getUUID()))
					{
						filtered.add(persist);
					}
				}
			}
			// if there are other persist left, check if they are in the hierarchy
			if (filtered.size() != persists.size())
			{
				Form flattenedForm = ModelUtils.getEditingFlattenedSolution(form).getFlattenedForm(form);
				if (flattenedForm instanceof FlattenedForm)
				{
					// if it is a flattend form then walk over the forms.
					List<Form> allForms = ((FlattenedForm)flattenedForm).getAllForms();
					outer : for (IPersist persist : persists)
					{
						// skip the one already there.
						if (filtered.contains(persist)) continue;
						IPersist ancestor = persist.getAncestor(IRepository.FORMS);
						if (ancestor == null) continue;
						for (Form superForm : allForms)
						{
							if (superForm.getUUID().equals(ancestor.getUUID()))
							{
								// the form uuid of the persist is the same as a superform
								// check if we should add it
								for (IPersist filteredPersist : filtered)
								{
									if (filteredPersist instanceof ISupportExtendsID)
									{
										IPersist superPersist = PersistHelper.getSuperPersist((ISupportExtendsID)filteredPersist);
										while (superPersist instanceof ISupportExtendsID)
										{
											// if there is already one
											if (superPersist.getID() == persist.getID())
											{
												continue outer;
											}
											superPersist = PersistHelper.getSuperPersist((ISupportExtendsID)superPersist);
										}
									}

								}
								filtered.add(persist);
								break;
							}
						}
					}
				}
			}
		}
		return filtered;
	}

	@Override
	public boolean showPersist(IPersist persist)
	{
		selectionProvider.setSelection(new StructuredSelection(persist));
		return true;
	}

	@Override
	public abstract void setFocus();

	@Override
	protected DeleteAction createDeleteAction()
	{
		return new DeleteAction(editorPart);
	}

	@Override
	protected IAction createCopyAction()
	{
		return new CopyAction(editorPart);
	}

	@Override
	protected IAction createCutAction()
	{
		return new CutAction(editorPart);
	}

	@Override
	protected IAction createPasteAction()
	{
		return new PasteAction(Activator.getDefault().getDesignClient(), selectionProvider, editorPart, fieldPositioner);
	}

	@Override
	protected SelectAllAction createSelectAllAction()
	{
		return new FixedSelectAllAction(editorPart, selectionProvider);
	}

	public void createNewComponent(JSONObject componentDefinition)
	{
		if (editorServiceHandler != null)
		{
			editorServiceHandler.executeMethod("createComponent", componentDefinition);
		}
	}

	public void refreshContent()
	{
		CurrentWindow.runForWindow(new WebsocketSessionWindows(designerWebsocketSession), new Runnable()
		{
			@Override
			public void run()
			{
				designerWebsocketSession.getClientService("$editorContentService").executeAsyncServiceCall("contentRefresh", new Object[] { });
				designerWebsocketSession.valueChanged();
			}
		});
	}

	public void showContainer(AbstractContainer container)
	{
		this.showedContainer = container;
		refreshBrowserUrl(true);
		if (DesignerUtil.getContentOutline() != null)
		{
			IPage outline = DesignerUtil.getContentOutline().getCurrentPage();
			if (outline instanceof FormOutlinePage)
			{
				((FormOutlinePage)outline).refresh();
			}
		}
		if (showedContainer instanceof LayoutContainer)
		{
			editorPart.setContentDescription("Showing container: " + DesignerUtil.getLayoutContainerAsString((LayoutContainer)showedContainer));
		}
		else
		{
			editorPart.setContentDescription("");
		}
	}

	public AbstractContainer getShowedContainer()
	{
		return showedContainer;
	}

	/**
	 * @param url
	 */
	protected abstract void showUrl(final String url);

	enum RefreshType
	{
		NO_REFRESH, FULL_REFRESH, PALETTE_REFRESH;
	}

	private final class PartListener
		implements IPartListener2, ILoadedNGPackagesListener, IFormComponentListener, IPreferenceChangeListener, IPersistChangeListener
	{
		private boolean hidden = false;
		private RefreshType refresh = RefreshType.NO_REFRESH;

		@Override
		public void ngPackagesChanged(CHANGE_REASON changeReason, boolean loadedPackagesAreTheSameAlthoughReferencingModulesChanged)
		{
			if (!loadedPackagesAreTheSameAlthoughReferencingModulesChanged)
			{
				if (!hidden)
				{
					refresh(true);
				}
				else refresh = RefreshType.FULL_REFRESH;
			}
		}

		public void formComponentChanged()
		{
			if (!hidden)
			{
				refresh(false);
			}
			else if (refresh != RefreshType.FULL_REFRESH)
			{
				refresh = RefreshType.PALETTE_REFRESH;
			}
		}

		private void refresh(boolean full)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				@Override
				public void run()
				{
					if (full)
					{
						((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).refreshBrowserUrl(true);
					}
					else
					{
						((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).refreshPalette();
					}
				}
			});
		}

		@Override
		public void partVisible(IWorkbenchPartReference partRef)
		{
			if ((partRef.getPart(false) == getEditorPart()))
			{
				if (refresh != RefreshType.NO_REFRESH)
				{
					refresh(refresh == RefreshType.FULL_REFRESH);
					refresh = RefreshType.NO_REFRESH;
				}
				hidden = false;
			}
		}

		@Override
		public void partHidden(IWorkbenchPartReference partRef)
		{
			if ((partRef.getPart(false) == getEditorPart()))
			{
				hidden = true;
			}
		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef)
		{
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef)
		{
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef)
		{
		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef)
		{
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef)
		{
		}

		@Override
		public void partActivated(IWorkbenchPartReference partRef)
		{
		}

		@Override
		public void preferenceChange(PreferenceChangeEvent event)
		{
			if ((DesignerPreferences.DESIGNER_SETTINGS_PREFIX + DesignerPreferences.MARQUEE_SELECT_OUTER_SETTING).equals(event.getKey()))
			{
				if (!hidden)
				{
					refresh(true);
				}
				else refresh = RefreshType.FULL_REFRESH;
			}
		}


		public void persistChanges(Collection<IPersist> changes)
		{
			Object persist = changes.iterator().next();

			if (persist instanceof Media mediaFile)
			{

				if (mediaFile.getName().equals("variants.json") || mediaFile.getName().endsWith(".less") || mediaFile.getName().endsWith(".css"))
				{
					refresh(true);
				}

			}
			else if (persist instanceof Menu)
			{
				refresh(false);
			}

		}


		public void refreshPersists(final List<IPersist> persists, boolean fullRefresh)
		{
			if (persists != null)
			{
				if (persists.size() == 1 && persists.get(0) == showedContainer && showedContainer instanceof LayoutContainer &&
					CSSPositionUtils.isCSSPositionContainer((LayoutContainer)showedContainer))
				{
					// probably size has changed we need a full refresh
					refreshBrowserUrl(true);
					return;
				}
				FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(editorPart.getForm());
				final Form form = fs.getFlattenedForm(editorPart.getForm(), false);
				final String componentsJSON = designerWebsocketSession.getComponentsJSON(fs, filterByParent(persists, form));
				List<String> styleSheets = PersistHelper.getOrderedStyleSheets(fs);
				String[] newStylesheets = null;
				for (IPersist persist : persists)
				{
					if (persist instanceof Media && styleSheets.contains(((Media)persist).getName()))
					{
						newStylesheets = designerWebsocketSession.getSolutionStyleSheets(fs);
						break;
					}
				}
				if (persists.size() == 1 && persists.get(0) instanceof Form)
				{
					// datasource changed, refresh palette for form components
					this.refreshPalette();
				}
				final String[] newStylesheetsFinal = newStylesheets;
				CurrentWindow.runForWindow(new WebsocketSessionWindows(designerWebsocketSession), new Runnable()
				{
					@Override
					public void run()
					{
						designerWebsocketSession.getClientService("$editorContentService").executeAsyncServiceCall("updateFormData",
							new Object[] { componentsJSON });
						if (persists.contains(form instanceof FlattenedForm ? ((FlattenedForm)form).getWrappedPersist() : form))
						{
							designerWebsocketSession.getClientService("$editorContentService").executeAsyncServiceCall("updateForm",
								new Object[] { form.getUUID(), form.extendsForm != null ? form.extendsForm.getUUID()
									: null, form.getSize().width, form.getSize().height });
						}
						if (fullRefresh)
						{
							designerWebsocketSession.getClientService("$editorContentService").executeAsyncServiceCall("contentRefresh",
								new Object[] { });
						}
						if (newStylesheetsFinal != null)
						{
							designerWebsocketSession.getClientService("$editorContentService").executeAsyncServiceCall("updateStyleSheets",
								new Object[] { newStylesheetsFinal });
						}
						designerWebsocketSession.valueChanged();
					}
				});
			}
		}

		public void refreshPalette()
		{
			CurrentWindow.runForWindow(new WebsocketSessionWindows(editorWebsocketSession), new Runnable()
			{
				@Override
				public void run()
				{
					editorWebsocketSession.getClientService(EditorWebsocketSession.EDITOR_SERVICE).executeAsyncServiceCall("refreshPalette", new Object[] { });
					editorWebsocketSession.valueChanged();
				}
			});
		}
	}

	public void zoomIn(LayoutContainer container)
	{
		zoomed.push(showedContainer);
		showContainer(container);
	}

	public void zoomOut()
	{
		showContainer(zoomed.isEmpty() ? null : zoomed.pop());
	}

}
