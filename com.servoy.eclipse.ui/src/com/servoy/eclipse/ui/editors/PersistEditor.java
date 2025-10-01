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
package com.servoy.eclipse.ui.editors;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.ViewPartHelpContextProvider;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;

/**
 * @author jcompagner
 */
public abstract class PersistEditor extends EditorPart implements IPersistChangeListener
{
	private ServoyProject servoyProject;
	private IPersist persist;
	private boolean disposed = false;
	private boolean refreshing = false;
	private IActiveProjectListener activeProjectListener;

	/**
	 * @return the persist
	 */
	public IPersist getPersist()
	{
		return persist;
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (adapter.equals(IPersist.class))
		{
			return getPersist();
		}
		if (adapter.equals(IPropertySourceProvider.class) && getPersist() != null)
		{
			// enable setting stuff via the properties editor
			return new IPropertySourceProvider()
			{
				public IPropertySource getPropertySource(Object object)
				{
					return PersistPropertySource.createPersistPropertySource(getPersist(), getPersist(), false/* readOnly */);
				}
			};
		}
		if (getContextId() != null && adapter.equals(IContextProvider.class))
		{
			return new ViewPartHelpContextProvider(getContextId());
		}
		return super.getAdapter(adapter);
	}

	protected String getContextId()
	{
		return null;
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	@Override
	protected void setInput(IEditorInput input)
	{
		if (input == null) return;

		super.setInput(input);

		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		if (servoyModel.getActiveProject() != null)
		{
			if (input instanceof PersistEditorInput)
			{
				PersistEditorInput persistInput = (PersistEditorInput)input;

				servoyProject = servoyModel.getServoyProject(persistInput.getSolutionName());
				if (servoyProject == null)
				{
					throw new IllegalArgumentException("Cannot open solution " + persistInput.getSolutionName());
				}
				persist = servoyProject.getEditingPersist(persistInput.getUuid());
			}
			else
			{
				throw new IllegalArgumentException("This editor does not support input " + input.getClass());
			}

			if (persist == null)
			{
				throw new IllegalArgumentException("Could not find element for input " + input);
			}

			if (!validatePersist(persist))
			{
				throw new IllegalArgumentException("This editor does not support type " + persist.getClass());
			}
			updateTitle();
		}
		else if (activeProjectListener == null)
		{
			activeProjectListener = new IActiveProjectListener()
			{
				@Override
				public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
				{
					return true;
				}

				@Override
				public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
				{
				}

				@Override
				public void activeProjectChanged(ServoyProject activeProject)
				{
					servoyModel.removeActiveProjectListener(activeProjectListener);
					activeProjectListener = null;
					setInput(input);
					init();
					refresh();
				}
			};
			servoyModel.addActiveProjectListener(activeProjectListener);
		}
	}

	/**
	 * called when the active project is changed and the persist is initialized
	 */
	protected void init()
	{
	}

	protected final void refresh()
	{
		if (refreshing || disposed) return;

		getSite().getShell().getDisplay().asyncExec(new Runnable()
		{
			public void run()
			{
				if (refreshing || disposed) return;
				refreshing = true;
				try
				{
					doRefresh();
				}
				finally
				{
					refreshing = false;
				}
			}
		});
	}

	protected abstract void doRefresh();

	/**
	 * Validate if the persist is supported by this editor.
	 *
	 * @param p
	 */
	protected abstract boolean validatePersist(IPersist p);

	@Override
	public void dispose()
	{
		disposed = true;
		ServoyModelManager.getServoyModelManager().getServoyModel().removePersistChangeListener(false, this);
		revert(false);
		super.dispose();
	}

	/**
	 * Revert persist, remove changes.
	 *
	 * @param force
	 */
	public void revert(boolean force)
	{
		if (force || isDirty())
		{
			try
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().revertEditingPersist(servoyProject, persist);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not revert object", e);
			}
		}
	}

	protected void updateTitle()
	{
		if (persist instanceof ISupportName)
		{
			String name = ((ISupportName)persist).getName();
			setPartName(name);
			setTitleToolTip(persist.getRootObject().getName() + '.' + name);
		}
		else
		{
			IEditorInput input = getEditorInput();
			setPartName(input.getName());
			setTitleToolTip(input.getToolTipText());
		}
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs()
	{
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		if (input != null)
		{
			setInput(convertInput(input));
			ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(false, this);
		}
		site.setSelectionProvider(new IPostSelectionProvider()
		{
			private final ListenerList listeners = new ListenerList();

			private final ListenerList postListeners = new ListenerList();

			private ISelection selection;

			public void addSelectionChangedListener(ISelectionChangedListener listener)
			{
				listeners.add(listener);
			}

			public void addPostSelectionChangedListener(ISelectionChangedListener listener)
			{
				postListeners.add(listener);
			}

			public void fireSelectionChanged(final SelectionChangedEvent event)
			{
				Object[] listeners = this.listeners.getListeners();
				fireEventChange(event, listeners);
			}

			public void firePostSelectionChanged(final SelectionChangedEvent event)
			{
				Object[] listeners = postListeners.getListeners();
				fireEventChange(event, listeners);
			}

			private void fireEventChange(final SelectionChangedEvent event, Object[] listeners)
			{
				for (Object listener : listeners)
				{
					final ISelectionChangedListener l = (ISelectionChangedListener)listener;
					SafeRunner.run(new SafeRunnable()
					{
						public void run()
						{
							l.selectionChanged(event);
						}
					});
				}
			}

			public ISelection getSelection()
			{
				if (this.selection != null)
				{
					return this.selection;
				}
				IPersist p = getPersist();
				return p != null ? new StructuredSelection(p) : StructuredSelection.EMPTY;
			}

			public void removeSelectionChangedListener(ISelectionChangedListener listener)
			{
				listeners.remove(listener);
			}

			public void removePostSelectionChangedListener(ISelectionChangedListener listener)
			{
				postListeners.remove(listener);
			}

			public void setSelection(ISelection selection)
			{
				ISelection oldSelection = getSelection();
				this.selection = selection;
				ISelection newSelection = getSelection();
				if (!oldSelection.equals(newSelection))
				{
					fireSelectionChanged(new SelectionChangedEvent(this, newSelection));
					firePostSelectionChanged(new SelectionChangedEvent(this, newSelection));
				}
			}
		});
	}

	protected IEditorInput convertInput(IEditorInput input)
	{
		if (input instanceof FileEditorInput)
		{
			IPersist filePersist = SolutionDeserializer.findPersistFromFile(((FileEditorInput)input).getFile());
			if (filePersist != null)
			{
				Solution solution = (Solution)filePersist.getAncestor(IRepository.SOLUTIONS);
				if (solution != null && filePersist instanceof ISupportName)
				{
					return new PersistEditorInput(((ISupportName)filePersist).getName(), solution.getName(), filePersist.getUUID());
				}
			}
		}
		return input;
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	@Override
	public boolean isDirty()
	{
		return persist != null && Boolean.TRUE.equals(persist.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (o.isChanged())
				{
					return Boolean.TRUE;
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		}));
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		if (isDirty())
		{
			try
			{
				servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, true);
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
				MessageDialog.openError(getSite().getShell(), "Error", "Save failed: " + e.getMessage());
				if (monitor != null) monitor.setCanceled(true);
			}
		}
	}

	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	public void persistChanges(Collection<IPersist> changes)
	{
		for (IPersist changed : changes)
		{
			// is it a child of the current persist (or the current persist itself)?
			IPersist parent = changed.getAncestor(persist.getTypeID());
			if (persist.equals(parent))
			{
				getSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable()
				{
					public void run()
					{
						if (!disposed)
						{
							refresh();
							firePropertyChange(IEditorPart.PROP_DIRTY);
						}
					}
				});
				return;
			}
		}
	}
}