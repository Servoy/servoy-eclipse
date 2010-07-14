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
package com.servoy.eclipse.designer.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IShowEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.repository.SolutionDeserializer;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Multi-page form editor.
 * 
 * @author rgansevles
 */

public class VisualFormEditor extends MultiPageEditorPart implements CommandStackListener, IActiveProjectListener, IPersistChangeListener, IShowEditorInput
{
	private static final String COM_SERVOY_ECLIPSE_DESIGNER_CONTEXT = "com.servoy.eclipse.designer.context";

	// edit request types
	public static final String REQ_COPY = "VFE_COPY"; //$NON-NLS-1$
	public static final String REQ_CUT = "VFE_CUT"; //$NON-NLS-1$
	public static final String REQ_BRING_TO_FRONT = "VFE_BRING_TO_FRONT"; //$NON-NLS-1$
	public static final String REQ_SEND_TO_BACK = "VFE_SEND_TO_BACK"; //$NON-NLS-1$
	public static final String REQ_PASTE = "VFE_PASTE"; //$NON-NLS-1$
	public static final String REQ_DROP_COPY = "VFE_DROP_COPY"; //$NON-NLS-1$
	public static final String REQ_DROP_LINK = "VFE_DROP_LINK"; //$NON-NLS-1$
	public static final String REQ_PLACE_TAB = "VFE_PLACE_TAB"; //$NON-NLS-1$
	public static final String REQ_PLACE_SPLIT_PANE = "VFE_PLACE_SPLIT_PANE"; //$NON-NLS-1$
	public static final String REQ_PLACE_PORTAL = "VFE_PLACE_PORTAL"; //$NON-NLS-1$
	public static final String REQ_PLACE_MEDIA = "VFE_PLACE_MEDIA"; //$NON-NLS-1$
	public static final String REQ_PLACE_BEAN = "VFE_PLACE_BEAN"; //$NON-NLS-1$
	public static final String REQ_PLACE_BUTTON = "VFE_PLACE_BUTTON"; //$NON-NLS-1$
	public static final String REQ_PLACE_FIELD = "VFE_PLACE_FIELD"; //$NON-NLS-1$
	public static final String REQ_PLACE_LABEL = "VFE_PLACE_LABEL"; //$NON-NLS-1$
	public static final String REQ_PLACE_RECT_SHAPE = "VFE_PLACE_RECT_SHAPE"; //$NON-NLS-1$
	public static final String REQ_PLACE_TEMPLATE = "VFE_PLACE_TEMPLATE"; //$NON-NLS-1$
	public static final String REQ_DISTRIBUTE = "VFE_DISTRIBUTE"; //$NON-NLS-1$
	public static final String REQ_SET_PROPERTY = "VFE_SET_PROPERTY"; //$NON-NLS-1$

	private Form form; // The working model.
	private Form flattenedForm;
	private ServoyProject servoyProject; // the solution wrapper

	private IContextActivation activateContext;

	private VisualFormEditorDesignPage graphicaleditor = null;
	private VisualFormEditorPartsPage partseditor = null;
	private VisualFormEditorTabSequencePage tabseditor = null;
	private VisualFormEditorSecurityPage seceditor = null;
	private boolean closing = false;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		final IPersist filePersist;
		if (input instanceof FileEditorInput)
		{
			filePersist = SolutionDeserializer.findPersistFromFile((FileEditorInput)input);
			if (filePersist != null)
			{
				Form f = (Form)filePersist.getAncestor(IRepository.FORMS);
				if (f != null)
				{
					input = new PersistEditorInput(f.getName(), f.getSolution().getName(), f.getUUID());
				}
			}
		}
		else
		{
			filePersist = null;
		}
		// Check input.
		if (!(input instanceof PersistEditorInput))
		{
			throw new PartInitException(getClass().getName() + " does not support input " + input.getClass());
		}

		ServoyModelManager.getServoyModelManager().getServoyModel().addActiveProjectListener(this);
		super.init(site, input);

		if (projectIsNoLongerActive())
		{
			// editor is being restored but project is not active
			ServoyLog.logWarning("Closing form editor for " + input.getName() + " because solution " + servoyProject + " is not part of the active solution",
				null);
			close(false);
			return;
		}

		activateEditorContext();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	protected void setInput(IEditorInput input)
	{
		super.setInput(input);
		PersistEditorInput formInput = (PersistEditorInput)input;

		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyProject = servoyModel.getServoyProject(formInput.getSolutionName());
		if (servoyProject == null)
		{
			ServoyLog.logWarning("Trying to open editor for an unexisting Servoy project: " + formInput.getSolutionName() + ". The editor will be closed.",
				null);
			close(false);
			return;
		}
		try
		{
			form = (Form)servoyProject.getEditingPersist(formInput.getUuid());
			if (form == null)
			{
				throw new RuntimeException("Could not find form " + formInput.getName() + " in solution " + formInput.getSolutionName());
			}
			setPartName(form.getName());
			servoyModel.addPersistChangeListener(false, this);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
			throw new RuntimeException("Could not initialize form editor: " + e.getMessage());
		}
	}

	@Override
	public void dispose()
	{
		// stop listening
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.removeActiveProjectListener(this);
		servoyModel.removePersistChangeListener(false, this);
		revert(false);

		if (partseditor != null)
		{
			partseditor.dispose();
		}
		if (tabseditor != null)
		{
			tabseditor.dispose();
		}
		if (seceditor != null)
		{
			seceditor.dispose();
		}
		if (dummyActionRegistry != null)
		{
			dummyActionRegistry.dispose();
		}
		super.dispose();
	}

	/**
	 * Revert form, remove changes.
	 * 
	 * @param force
	 */
	public void revert(boolean force)
	{
		if (force || isDirty())
		{
			try
			{
				servoyProject.revertEditingPersist(form);
				getCommandStack().flush();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not revert form", e);
			}
		}
	}

	public Form getForm()
	{
		return form;
	}

	public Form getFlattenedForm()
	{
		if (flattenedForm == null)
		{
			try
			{
				FlattenedSolution editingFlattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(form);
				if (editingFlattenedSolution == null)
				{
					ServoyLog.logError("Could not get project for form " + form, null);
					return null;
				}
				flattenedForm = editingFlattenedSolution.getFlattenedForm(form);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not create flattened form for form " + form, e);
			}
		}
		return flattenedForm;
	}

	public void refreshFlattenedForm()
	{
		flattenedForm = null;
	}

	private final CommandStack dummyCommandStack = new CommandStack()
	{
		@Override
		public boolean canRedo()
		{
			return false;
		}

		@Override
		public boolean canUndo()
		{
			return false;
		}
	};

	@Override
	protected void pageChange(int newPageIndex)
	{
		super.pageChange(newPageIndex);
		if (graphicaleditor != null) graphicaleditor.commandStackChanged(new EventObject(this));

	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (adapter.equals(IPersist.class))
		{
			return form;
		}
		if (adapter.equals(IGotoMarker.class))
		{
			return new IGotoMarker()
			{
				public void gotoMarker(IMarker marker)
				{
					String elementUuid = null;
					try
					{
						elementUuid = (String)marker.getAttribute("elementUuid");
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
					if (marker.getAttribute(IMarker.CHAR_START, -1) != -1)
					{
						elementUuid = SolutionDeserializer.getUUID(marker.getResource().getLocation().toFile(),
							Utils.getAsInteger(marker.getAttribute(IMarker.CHAR_START, -1)));
					}
					if (elementUuid != null)
					{
						try
						{
							showPersist(AbstractRepository.searchPersist(form, UUID.fromString(elementUuid)));
						}
						catch (IllegalArgumentException e)
						{
							ServoyLog.logError(e);
						}
					}
				}

			};
		}
		if (adapter.equals(CommandStack.class) && getActivePage() >= 0 && getControl(getActivePage()).equals(seceditor))
		{
			return dummyCommandStack;
		}
		Object result = super.getAdapter(adapter);
		if (result == null && graphicaleditor != null)
		{
			return graphicaleditor.getAdapter(adapter);
		}
		if (result == null && adapter.equals(ActionRegistry.class))
		{
			// dummy return, this prevents a NPE when form editor is opened for form that is not part of the active solution
			return getDummyActionRegistry();
		}
		return result;
	}

	private ActionRegistry dummyActionRegistry;

	protected ActionRegistry getDummyActionRegistry()
	{
		if (dummyActionRegistry == null)
		{
			dummyActionRegistry = new ActionRegistry();
		}
		return dummyActionRegistry;
	}

	public CommandStack getCommandStack()
	{
		return (CommandStack)graphicaleditor.getAdapter(CommandStack.class);
	}

	public void commandStackChanged(EventObject event)
	{
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		try
		{
			servoyProject.saveEditingSolutionNodes(new IPersist[] { form }, true);
			seceditor.saveSecurityElements();
			graphicaleditor.doSave(monitor); // for marking the command stack
			isModified = false;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
	}

	/**
	 * Does nothing be default. This method should be overridden if {@link #isSaveAsAllowed()} has been overridden to return <code>true</code>.
	 * 
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	@Override
	public void doSaveAs()
	{
		throw new RuntimeException("'Save as' is not allowed"); //$NON-NLS-1$
	}

	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	/**
	 * Close this editor.
	 * 
	 * @param save
	 */
	public void close(final boolean save)
	{
		closing = true;
		getSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable()
		{
			public void run()
			{
				boolean wasClosed = getSite().getPage().closeEditor(VisualFormEditor.this, save);
				if (save && !wasClosed) // the user clicked cancel
				getSite().getPage().closeEditor(VisualFormEditor.this, false);
			}
		});
	}

	/**
	 * Refresh all pages for the persist.
	 * <p>
	 * When null, refresh the entire interface.
	 * 
	 * @param persist
	 */
	public void refresh(final List<IPersist> persists)
	{
		if (!closing)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					if (graphicaleditor != null)
					{
						graphicaleditor.refreshPersists(persists);
					}
					if (partseditor != null)
					{
						partseditor.refresh();
					}
					if (tabseditor != null)
					{
						tabseditor.refresh();
					}
					if (seceditor != null)
					{
						seceditor.refresh();
					}
				}
			});
		}
	}


	public void persistChanges(Collection<IPersist> changes)
	{
		boolean full_refresh = false;
		List<IPersist> changedChildren = new ArrayList<IPersist>();

		// get all the uuids of the forms in the current hierarchy.
		FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(form);
		Set<UUID> formUuids = new HashSet<UUID>();
		for (Form f : flattenedSolution.getFormHierarchy(form))
		{
			formUuids.add(f.getUUID());
		}

		for (IPersist changed : changes)
		{
			// is it a child of the current form hierarchy?
			IPersist formParent = changed.getAncestor(IRepository.FORMS);
			if (formParent != null && formUuids.contains(formParent.getUUID()))
			{
				try
				{
					IPersist child = AbstractRepository.searchPersist(flattenedSolution.getSolution(), changed);

					// is it the form itself ?
					if (changed.getUUID().equals(form.getUUID()))
					{
						if (child == null)
						{
							// form self deleted
							close(false);
							return;
						}
						getSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable()
						{
							public void run()
							{
								setPartName(form.getName());
							}
						});
						full_refresh = true;
					}
					else
					{
						// is it a removed child?
						if (child == null)
						{
							full_refresh = true;
						}
						else if (changed.getParent() != null)
						{
							// is it a part of this form?
							if (changed instanceof Part && formUuids.contains(changed.getParent().getUUID()))
							{
								full_refresh = true;
							}
							// is it a tab in this form (tab figure is child of the form)
							else if (changed instanceof Tab && changed.getParent().getParent() != null &&
								formUuids.contains(changed.getParent().getParent().getUUID()))
							{
								full_refresh = true;
							}
							changedChildren.add(child);
						}
					}
				}
				catch (RuntimeException e)
				{
					ServoyLog.logError(e);
					full_refresh = true;
				}
			}
			else if (changed.getTypeID() == IRepository.SOLUTIONS && projectIsNoLongerActive())
			{
				close(false);
				return;
			}
			else if (changed.getTypeID() == IRepository.STYLES)
			{
				if (((Style)changed).getName().equals(form.getStyleName()))
				{
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, form, true);
					return;
				}
			}
		}

		refreshFlattenedForm();
		if (full_refresh)
		{
			// refresh all
			changedChildren.add(form);
		}
		refresh(changedChildren);

	}

	public void activeProjectChanged(ServoyProject activeProject)
	{
		if (projectIsNoLongerActive())
		{
			close(true);
		}
		// else refresh(null); is already performed from activeProjectWillChange(...) implementation
	}

	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
		// this form might be part of a previous module of the active solution - so check to see if that module is still active
		if (updateInfo == IActiveProjectListener.MODULES_UPDATED && projectIsNoLongerActive())
		{
			close(true);
		}
		else
		{
			// other stuff related to the active project has changed, so refresh the editor
			refresh(null);
		}
	}

	private boolean projectIsNoLongerActive()
	{
		ServoyProject[] modules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		boolean result = true;
		for (int i = modules.length - 1; i >= 0; i--)
		{
			if (modules[i] == servoyProject)
			{
				result = false;
				break;
			}
		}
		return result;
	}

	/**
	 * @see com.servoy.eclipse.core.IActiveProjectListener#activeProjectWillChange(ServoyProject, com.servoy.eclipse.core.ServoyProject)
	 */
	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
	{
		if (activeProject != null)
		{
			try
			{
				form = (Form)servoyProject.getEditingPersist(form.getUUID());
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not get form", e);
			}
		}
		if (form == null)
		{
			close(false);
		}
		else
		{
			refresh(null);
		}
		return true;
	}

	@Override
	protected void createPages()
	{
		if (!closing)
		{
			try
			{
				createDesignPage();
			}
			catch (PartInitException e)
			{
				ServoyLog.logError("Could not create design page", e);
			}
			createPartsPage();
			createTabsPage();
		}
		createSecPage(); // MultiPageEditorPart wants at least 1 page
	}

	protected void createDesignPage() throws PartInitException
	{
		graphicaleditor = new VisualFormEditorDesignPage(this);
		setPageText(addPage(graphicaleditor, getEditorInput()), "Design");
	}

	private void createPartsPage()
	{
		partseditor = new VisualFormEditorPartsPage(this, getContainer(), SWT.NONE);
		setPageText(addPage(partseditor), "Parts");
	}

	private void createTabsPage()
	{
		tabseditor = new VisualFormEditorTabSequencePage(this, getContainer(), SWT.NONE);
		setPageText(addPage(tabseditor), "Tab sequence");
	}

	private void createSecPage()
	{
		seceditor = new VisualFormEditorSecurityPage(this, getContainer(), SWT.NONE);
		setPageText(addPage(seceditor), "Security");
	}

	private boolean isModified;

	public void flagModified()
	{
		isModified = true;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public boolean isDirty()
	{
		return isModified || super.isDirty();
	}

	public void activateEditorContext()
	{
		if (activateContext == null)
		{
			IContextService service = (IContextService)getSite().getService(IContextService.class);
			activateContext = service.activateContext(COM_SERVOY_ECLIPSE_DESIGNER_CONTEXT);
		}
	}

	public void deactivateEditorContext()
	{
		if (activateContext != null)
		{
			IContextService service = (IContextService)getSite().getService(IContextService.class);
			service.deactivateContext(activateContext);
			activateContext = null;
		}
	}

	public boolean isDesignerContextActive()
	{
		return activateContext != null;
	}

	protected void showPersist(IPersist persist)
	{
		if (persist != null)
		{
			if (graphicaleditor.showPersist(persist))
			{
				setActiveEditor(graphicaleditor);
			}
			// else if partseditor.showPersist(persist) ...
		}
	}

	public void showEditorInput(IEditorInput editorInput)
	{
		if (editorInput instanceof FileEditorInput)
		{
			showPersist(SolutionDeserializer.findPersistFromFile((FileEditorInput)editorInput));
		}
	}
}
