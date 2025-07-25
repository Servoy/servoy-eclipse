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

import static com.servoy.eclipse.core.util.UIUtils.runInUI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackEvent;
import org.eclipse.gef.commands.CommandStackEventListener;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IShowEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.internal.e4.compatibility.SelectionService;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.eclipse.ui.views.properties.PropertySheet;
import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.cheatsheets.actions.ISupportCheatSheetActions;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.DesignPagetype;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.DeveloperMenuCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.PersistFinder;
import com.servoy.eclipse.designer.property.UndoablePersistPropertySourceProvider;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.eclipse.ui.editors.IFlagChangeEditor;
import com.servoy.eclipse.ui.editors.ISupportDeveloperMenu;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.FlattenedPortal;
import com.servoy.j2db.persistence.FlattenedTabPanel;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.MenuPropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;


/**
 * Multi-page form editor.
 *
 * @author rgansevles
 */

public abstract class BaseVisualFormEditor extends MultiPageEditorPart
	implements IActiveProjectListener, IPersistChangeListener, IShowEditorInput, ISupportCheatSheetActions, IFlagChangeEditor, ISupportDeveloperMenu
{
	private static final String COM_SERVOY_ECLIPSE_DESIGNER_CONTEXT = "com.servoy.eclipse.designer.context";
	private static final String COM_SERVOY_ECLIPSE_RFB_DESIGNER_CONTEXT = "com.servoy.eclipse.designer.rfb.context";

	// edit request types
	public static final String REQ_SET_PROPERTY = "VFE_SET_PROPERTY";
	public static final String REQ_COPY = "VFE_COPY";
	public static final String REQ_CUT = "VFE_CUT";
	public static final String REQ_PASTE = "VFE_PASTE";
	public static final RequestType REQ_DROP_COPY = new RequestType();
	public static final String REQ_DROP_LINK = "VFE_DROP_LINK";
	private Form form; // The working model.
	private ServoyProject servoyProject; // the solution wrapper

	private IContextActivation activateContext;

	protected BaseVisualFormEditorDesignPage graphicaleditor = null;
	private boolean closing = false;

	private final CommandStack commandStack = new CommandStack();

	private final VfeCommandStackEventListener commandStackEventListener = new VfeCommandStackEventListener();
	private final VfeCommandStackListener commandStackListener = new VfeCommandStackListener();


	@Override
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException
	{
		IEditorInput input = editorInput;
		if (input instanceof FileEditorInput)
		{
			input = PersistEditorInput.createFormEditorInput(((FileEditorInput)input).getFile());
		}

		// Check input.
		if (!(input instanceof PersistEditorInput))
		{
			throw new PartInitException(getClass().getName() + " does not support input " + (input != null ? input.getClass() : null) + " of " + input);
		}

		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.addActiveProjectListener(this);
		super.init(site, input);

		if (!servoyModel.isProjectActive(servoyProject))
		{
			// editor is being restored but project is not active
			ServoyLog.logWarning("Closing form editor for " + input.getName() + " because solution " + servoyProject + " is not part of the active solution",
				null);
			close(false);
			return;
		}

		commandStack.addCommandStackEventListener(commandStackEventListener);
		commandStack.addCommandStackListener(commandStackListener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	protected void setInput(IEditorInput input)
	{
		PersistEditorInput formInput = (PersistEditorInput)input;
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
		{

			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			servoyProject = servoyModel.getServoyProject(formInput.getSolutionName());
			if (servoyProject == null)
			{
				ServoyLog.logWarning("Trying to open editor for an unexisting Servoy project: " + formInput.getSolutionName() + ". The editor will be closed.",
					null);
				close(false);
				return;
			}
			// force loading the flattend solution so this and the modules are loaded..
			servoyProject.getEditingFlattenedSolution();

			form = (Form)servoyProject.getEditingPersist(formInput.getUuid());
			if (form == null)
			{
				throw new RuntimeException("Could not find form " + formInput.getName() + " in solution " + formInput.getSolutionName());
			}
			setPartName(form.getName());
			servoyModel.addPersistChangeListener(false, this);

			// make sure we have a file resource
			if (formInput.getFile() == null)
			{
				formInput = PersistEditorInput.createFormEditorInput(form);
			}
		}


		super.setInput(formInput);
	}

	@Override
	public void dispose()
	{
		// stop listening
		commandStack.removeCommandStackEventListener(commandStackEventListener);
		commandStack.removeCommandStackListener(commandStackListener);
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.removeActiveProjectListener(this);
		servoyModel.removePersistChangeListener(false, this);
		revert(false, false);

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
	public void revert(boolean force, boolean refresh)
	{
		if (force || isDirty())
		{
			try
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().revertEditingPersist(servoyProject, form);
				getCommandStack().flush();
				if (refresh)
				{
					this.refresh(new ArrayList<IPersist>(), true);
				}
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

	/**
	 * @return the graphicaleditor
	 */
	public BaseVisualFormEditorDesignPage getGraphicaleditor()
	{
		return graphicaleditor;
	}

	@Override
	protected void pageChange(int newPageIndex)
	{
		super.pageChange(newPageIndex);
		if (graphicaleditor != null) graphicaleditor.commandStackChanged(new EventObject(this));

		activateEditorContext();
	}


	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter)
	{
		IEditorPart activeEditor = getActiveEditor();
		if (activeEditor != null)
		{
			// If the active inner part can provide the adapter, return it
			T returnValue = activeEditor.getAdapter(adapter);
			if (returnValue != null)
			{
				return returnValue;
			}
		}
		if (adapter.equals(IPersist.class))
		{
			return (T)form;
		}
		if (adapter.equals(IGotoMarker.class))
		{
			return (T)new IGotoMarker()
			{
				public void gotoMarker(IMarker marker)
				{
					String elementUuid = marker.getAttribute("elementUuid", null);
					int start = marker.getAttribute(IMarker.CHAR_START, -1);
					if (start != -1)
					{
						elementUuid = SolutionDeserializer.getUUID(marker.getResource().getLocation().toFile(), start);
					}
					String name = marker.getAttribute("Name", null);
					if (name != null && name.indexOf('$') >= 0)
					{
						elementUuid = name;
					}
					if (elementUuid != null)
					{
						try
						{
							showPersist(PersistFinder.INSTANCE.searchForPersist(BaseVisualFormEditor.this.getForm(), elementUuid));
						}
						catch (IllegalArgumentException e)
						{
							ServoyLog.logError(e);
						}
					}
				}

			};
		}
		if (adapter == IPropertySourceProvider.class)
		{
			return (T)new UndoablePersistPropertySourceProvider(this);
		}
		Object result = super.getAdapter(adapter);
		if (result == null && graphicaleditor != null && (graphicaleditor == getActiveEditor() || getActiveEditor() == null))
		{
			result = graphicaleditor.getAdapter(adapter);
		}
		if (result == null && adapter.equals(ActionRegistry.class))
		{
			// dummy return, this prevents a NPE when form editor is opened for form that is not part of the active solution
			return (T)getDummyActionRegistry();
		}
		return (T)result;
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
		return commandStack;
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		try
		{
			List<IPersist> removes = new ArrayList<IPersist>();
			for (IPersist ip : form.getAllObjectsAsList())
			{
				if (ip instanceof ISupportExtendsID && PersistHelper.isOverrideOrphanElement((ISupportExtendsID)ip))
				{
					removes.add(ip);
				}
				else if (ip instanceof ISupportChilds)
				{
					List<IPersist> removes2 = new ArrayList<IPersist>();
					for (IPersist child : ((AbstractBase)ip).getAllObjectsAsList())
					{
						if (child instanceof ISupportExtendsID && PersistHelper.isOverrideOrphanElement((ISupportExtendsID)child))
						{
							removes2.add(child);
						}
					}
					for (IPersist child : removes2)
					{
						((AbstractBase)ip).removeChild(child);
					}
				}
			}
			for (IPersist ip : removes)
			{
				form.removeChild(ip);
			}
			servoyProject.saveEditingSolutionNodes(new IPersist[] { form }, true);
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
		throw new RuntimeException("'Save as' is not allowed");
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
				if (getSite() != null && getSite().getPage() != null)
				{
					boolean wasClosed = getSite().getPage().closeEditor(BaseVisualFormEditor.this, save);
					if (save && !wasClosed) // the user clicked cancel
						getSite().getPage().closeEditor(BaseVisualFormEditor.this, false);
				}
			}
		});
	}

	public boolean isClosing()
	{
		return closing;
	}

	public void refreshGraphicalEditor()
	{
		if (!closing && graphicaleditor != null)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					graphicaleditor.refreshAllParts();
				}
			});
		}
	}

	/**
	 * Refresh all pages for the persist.
	 * <p>
	 * When null, refresh the entire interface.
	 *
	 * @param persist
	 */
	public void refresh(final List<IPersist> persists, boolean fullRefresh)
	{
		if (!closing)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					if (!isClosing())
					{
						doRefresh(persists, fullRefresh);
					}
				}
			});
		}
	}

	/**
	 *
	 */
	protected void doRefresh(List<IPersist> persists, boolean fullRefresh)
	{
		if (graphicaleditor != null)
		{
			graphicaleditor.refreshPersists(persists, fullRefresh);
		}
	}

	private Collection<IPersist> replaceValuelistWithFields(Collection<IPersist> changes)
	{
		ArrayList<IPersist> replaceValuelistWithFields = new ArrayList<IPersist>();
		FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(form);
		List<Form> formHierarchy = flattenedSolution.getFormHierarchy(form);
		for (IPersist changed : changes)
		{
			if (changed.getTypeID() == IRepository.VALUELISTS)
			{
				ValueList valueList = (ValueList)changed;
				Activator.getDefault().getDebugClientHandler().flushValueList(valueList);
				for (Form f : formHierarchy)
				{
					Iterator<Field> formFieldsIte = f.getFields();
					Field formField;
					while (formFieldsIte.hasNext())
					{
						formField = formFieldsIte.next();
						if (formField.getValuelistID() == valueList.getID())
						{
							replaceValuelistWithFields.add(formField);
						}
					}
				}
			}
			else replaceValuelistWithFields.add(changed);
		}

		return replaceValuelistWithFields;
	}

	public void persistChanges(Collection<IPersist> changedPersists)
	{
		if (isClosing()) return;
		if (!ServoyModelManager.getServoyModelManager().getServoyModel().isProjectActive(servoyProject))
		{
			ServoyLog.logWarning("Closing form editor for " + form.getName() + " because solution " + servoyProject + " is not part of the active solution",
				null);
			close(false);
			return;
		}

		boolean full_refresh = false;
		boolean form_refresh = false;
		List<IPersist> changedChildren = new ArrayList<IPersist>();

		// get all the uuids of the forms in the current hierarchy.
		FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(form);
		if (flattenedSolution == null)
		{
			ServoyLog.logWarning("Cannot read solution for form: " + form.getName(), null);
			return;
		}
		Set<UUID> formUuids = new HashSet<UUID>();
		for (Form f : flattenedSolution.getFormHierarchy(form))
		{
			formUuids.add(f.getUUID());
		}

		Collection<IPersist> changes = replaceValuelistWithFields(changedPersists);
		for (final IPersist changed : changes)
		{
			// is it a child of the current form hierarchy?
			Form formParent = (Form)changed.getAncestor(IRepository.FORMS);
			if (formParent != null && formUuids.contains(formParent.getUUID()))
			{
				try
				{
					IPersist child = flattenedSolution.searchPersist(changed);

					if (changed instanceof FlattenedTabPanel || changed instanceof FlattenedPortal) child = changed;
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
						form_refresh = true;
					}
					else
					{

						// is it a removed child?
						if (child == null)
						{
							// if is form component element, will not be found in hierarchy, just ignore for now
							if (!(changed instanceof WebFormComponentChildType)) form_refresh = true;
							// add it so it gets cleared (refreshed) as child of the form
							changedChildren.add(changed);
						}
						else
						{
							changedChildren.add(child);
						}

						// is it a part of this form?
						if (changed instanceof Part)
						{
							form_refresh = true;
						}
					}

					if (formParent != form && changed instanceof ISupportExtendsID)
					{
						IPersist override = (IPersist)form.acceptVisitor(new IPersistVisitor()
						{
							public Object visit(IPersist o)
							{
								if (o instanceof ISupportExtendsID && (changed.getID() == ((ISupportExtendsID)o).getExtendsID() ||
									(((ISupportExtendsID)changed).getExtendsID() > 0 &&
										((ISupportExtendsID)changed).getExtendsID() == ((ISupportExtendsID)o).getExtendsID())))
								{
									return o;
								}
								return CONTINUE_TRAVERSAL;
							}
						});
						if (override != null)
						{
							changedChildren.add(override);
						}

						// is it a part of this form?
						if (override instanceof Part)
						{
							form_refresh = true;
						}
					}

					IStructuredSelection currentSelection = (IStructuredSelection)graphicaleditor.getSite().getSelectionProvider().getSelection();
					boolean selectionWasOverriden = false;
					if (currentSelection.size() > 0)
					{
						Object[] arrSelections = currentSelection.toArray();
						for (int i = 0; i < arrSelections.length; i++)
						{
							if (changed instanceof ISupportExtendsID && arrSelections[i] instanceof PersistContext &&
								PersistHelper.getSuperPersist((ISupportExtendsID)changed) == ((PersistContext)arrSelections[i]).getPersist())
							{
								selectionWasOverriden = true;
								arrSelections[i] = PersistContext.create(changed, ((PersistContext)arrSelections[i]).getContext());
							}
						}

						if (selectionWasOverriden)
						{
							// set the correct selection on form editor and outline page when override the persist for the first time
							runInUI(() -> {
								graphicaleditor.getSite().getSelectionProvider().setSelection(new StructuredSelection(arrSelections));
								((SelectionService)getSite().getWorkbenchWindow().getSelectionService()).notifyListeners(BaseVisualFormEditor.this);
							}, false);
						}
					}
				}
				catch (RuntimeException e)
				{
					ServoyLog.logError(e);
					form_refresh = true;
				}
			}
			else if (changed instanceof Media && ((Media)changed).getName().toLowerCase().endsWith(".css"))
			{
				changedChildren.add(changed);
			}
			else if (changed.getTypeID() == IRepository.SOLUTIONS &&
				!ServoyModelManager.getServoyModelManager().getServoyModel().isProjectActive(servoyProject))
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
			else if (!full_refresh && formParent != null && formParent.isFormComponent().booleanValue())
			{
				if (hasFormReference(flattenedSolution, getForm(), formParent, new HashSet<String>()))
				{
					full_refresh = true;
				}
			}
			else if (changed instanceof MenuItem menuItem)
			{
				Menu menu = (Menu)menuItem.getAncestor(IRepository.MENUS);
				if (menu != null)
				{
					form.acceptVisitor(new IPersistVisitor()
					{
						@Override
						public Object visit(IPersist o)
						{
							if (o instanceof WebComponent webComponent)
							{
								WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState()
									.getWebObjectSpecification(((WebComponent)o).getTypeName());
								if (spec != null)
								{

									for (PropertyDescription pd : spec.getProperties(MenuPropertyType.INSTANCE))
									{
										if (Utils.equalObjects(webComponent.getFlattenedJson().opt(pd.getName()), menu.getUUID()))
										{
											changedChildren.add(o);
											break;
										}
									}
								}
							}
							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}
					});
				}
			}

		}

		if (form_refresh || full_refresh)
		{
			// refresh all
			changedChildren.add(form);
		}
		if (changedChildren.size() > 0)
		{
			refresh(changedChildren, full_refresh);
		}
	}

	private static boolean hasFormReference(final FlattenedSolution fs, Form form, final Form formRef, Set<String> recursionCheck)
	{
		final boolean hasFormReference[] = { false };
		Form flattenedForm = fs.getFlattenedForm(form);
		flattenedForm.acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof IFormElement)
				{
					IFormElement formElement = (IFormElement)o;
					FormElement fe = FormElementHelper.INSTANCE.getFormElement(formElement, fs, null, true);
					if (hasFormReference(fs, fe, formRef, recursionCheck))
					{
						hasFormReference[0] = true;
						return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});

		return hasFormReference[0];
	}

	private static boolean hasFormReference(FlattenedSolution fs, FormElement formElement, Form formRef, Set<String> recursionCheck)
	{
		WebObjectSpecification spec = formElement.getWebComponentSpec();
		if (spec != null)
		{
			Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
			if (properties.size() > 0)
			{
				for (PropertyDescription pd : properties)
				{
					Object propertyValue = formElement.getPropertyValue(pd.getName());
					Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
					if (frm == null) continue;
					if (!recursionCheck.add(frm.getName()))
					{
						Debug.error("recursive reference found between (List)FormComponents: " + recursionCheck); //$NON-NLS-1$
						continue;
					}
					if ((frm == formRef || FlattenedForm.hasFormInHierarchy(frm, formRef) || hasFormReference(fs, frm, formRef, recursionCheck)))
					{
						// only test for nested recursion, when going up the tree again this one can be removed
						// because a sibling with the same FC will result in a warning.
						recursionCheck.remove(frm.getName());
						return true;
					}
					recursionCheck.remove(frm.getName());
				}
			}
		}
		return false;
	}

	public void activeProjectChanged(ServoyProject activeProject)
	{
		if (ServoyModelManager.getServoyModelManager().getServoyModel().isProjectActive(servoyProject))
		{
			refresh(null, false);
		}
		else if (servoyProject == null && getPageCount() == 1)
		{
			// place hoder, replace it and create the normal pages
			Display.getDefault().asyncExec(() -> {
				setInput(getEditorInput());
				removePage(0);
				createPages();
				setActivePage(0);
				if (getEditorSite() != null && getEditorSite().getPage().getActivePart() == this)
				{
					//if an editor was previously open, then after restart we have to make sure that our modified properties sheet page is used
					//we do partClosed and partActivated to make it call getAdapter again for IPropertySheetPage.class and return our own
					PropertySheet propertySheet = (PropertySheet)getEditorSite().getPage().findView("org.eclipse.ui.views.PropertySheet");
					if (propertySheet != null)
					{
						propertySheet.partClosed(this);
						propertySheet.partActivated(this);
					}

					ContentOutline contentOutline = (ContentOutline)getEditorSite().getPage().findView("org.eclipse.ui.views.ContentOutline");
					if (contentOutline != null)
					{
						contentOutline.partClosed(this);
						contentOutline.partActivated(this);
					}
					// set up the editor actions, this is normally done in part activation listener
					getEditorSite().getActionBarContributor().setActiveEditor(this);
				}
			});
		}
		else
		{
			close(true);
		}
	}

	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
		// this form might be part of a previous module of the active solution - so check to see if that module is still active
		if (updateInfo == IActiveProjectListener.MODULES_UPDATED && !ServoyModelManager.getServoyModelManager().getServoyModel().isProjectActive(servoyProject))
		{
			close(true);
		}
		else if (updateInfo != IActiveProjectListener.RESOURCES_UPDATED_BECAUSE_ACTIVE_PROJECT_CHANGED)
		{
			// other stuff related to the active project has changed, so refresh the editor
			refresh(null, false);
		}
	}

	/**
	 * @see com.servoy.eclipse.model.nature.IActiveProjectListener#activeProjectWillChange(ServoyProject, com.servoy.eclipse.model.nature.ServoyProject)
	 */
	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
	{
		if (activeProject != null)
		{
			if (servoyProject != null)
			{
				form = (Form)servoyProject.getEditingPersist(form.getUUID());
			}
			else
			{
				form = null;
			}
		}
		if (form == null && activeProject != null)
		{
			close(false);
		}
		return true;
	}

	@Override
	protected void createPages()
	{
		if (!isClosing())
		{
			try
			{
				createDesignPage(null);
			}
			catch (PartInitException e)
			{
				ServoyLog.logError("Could not create design page", e);
			}
		}
	}

	public void setDesignPageType(DesignPagetype designPagetype) throws PartInitException
	{
		if (designPagetype != null && !designPagetype.equals(graphicaleditor.getDesignPagetype()))
		{
			if (isDirty())
			{
				if (!MessageDialog.openQuestion(UIUtils.getActiveShell(), "Save form", getTitle() + " has been modified. Save changes?"))
				{
					return;
				}
				doSave(null);
			}

			recreateDesignPage(designPagetype);
		}
	}

	protected void recreateDesignPage(DesignPagetype designPagetype) throws PartInitException
	{
		int activePage = getActivePage();

		BaseVisualFormEditorDesignPage oldGraphicaleditor = graphicaleditor;
		graphicaleditor = null;

		createDesignPage(designPagetype);

		// remove old graphicaleditor
		CTabItem[] items = ((CTabFolder)getContainer()).getItems();
		for (int i = 0; i < items.length; i++)
		{
			if (oldGraphicaleditor == items[i].getData())
			{
				removePage(i);
				oldGraphicaleditor.dispose();
				break;
			}
		}

		if (activePage >= 0)
		{
			setActivePage(activePage);
		}

		if (designPagetype != null && getEditorInput() instanceof PersistEditorInput)
		{
			// save last selected as default for next time
			((PersistEditorInput)getEditorInput()).setDesignPagetype(designPagetype);
		}

		IViewPart contentOutline = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView("org.eclipse.ui.views.ContentOutline");
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().hideView(contentOutline);
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.eclipse.ui.views.ContentOutline");

		if (this != PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart())
		{
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(this);
		}
	}

	protected void createDesignPage(DesignPagetype designPagetype) throws PartInitException
	{
		graphicaleditor = createGraphicaleditor(designPagetype);
		addPage(0, graphicaleditor, getEditorInput());
		setPageText(0, "Design");

		Display.getCurrent().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				// part needs to be activated first
				if (!PlatformUI.getWorkbench().isClosing()) activateEditorContext();
			}
		});
	}

	protected abstract BaseVisualFormEditorDesignPage createGraphicaleditor(DesignPagetype designPagetype);

	private boolean isModified;
	private boolean renderGhosts = false;

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
		IContextService service = getSite().getService(IContextService.class);
		if (service != null)
		{
			if (activateContext != null)
			{
				service.deactivateContext(activateContext);
				activateContext = null;
			}

			if (getActiveEditor() == graphicaleditor && graphicaleditor != null)
			{
				if (getGraphicaleditor().getDesignPagetype() == DesignPagetype.Rfb)
				{
					activateContext = service.activateContext(COM_SERVOY_ECLIPSE_RFB_DESIGNER_CONTEXT);
				}
				else
				{
					activateContext = service.activateContext(COM_SERVOY_ECLIPSE_DESIGNER_CONTEXT);
				}
			}
		}
	}

	public void deactivateEditorContext()
	{
		if (activateContext != null)
		{
			IContextService service = getSite().getService(IContextService.class);
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
			showPersist(SolutionDeserializer.findPersistFromFile(((FileEditorInput)editorInput).getFile()));
		}
	}

	/**
	 *  Request type for actions in Visual Form Editor.]
	 *
	 * @author rgansevles
	 *
	 */
	public static class RequestType
	{
		public static final int TYPE_TEMPLATE = 1;
		public static final int TYPE_SHAPE = 2;
		public static final int TYPE_LABEL = 3;
		public static final int TYPE_FIELD = 4;
		public static final int TYPE_BEAN = 5;
		public static final int TYPE_MEDIA = 6;
		public static final int TYPE_PORTAL = 7;
		public static final int TYPE_SPLIT_PANE = 8;
		public static final int TYPE_BUTTON = 9;
		public static final int TYPE_TAB = 10;
		public static final int TYPE_PART = 11;
		public static final int TYPE_COMPONENT = 12;

		private static final int UNSPECIFIED = -1;

		public final int type;

		public RequestType()
		{
			type = UNSPECIFIED;
		}

		public RequestType(int type)
		{
			this.type = type;
		}

		@Override
		public String toString()
		{
			return "RequestType(" + type + ")";
		}
	}

	/**
	 * @return the commandStackEventListener
	 */
	public VfeCommandStackEventListener getCommandStackEventListener()
	{
		return commandStackEventListener;
	}

	public class VfeCommandStackListener implements CommandStackListener
	{
		public void commandStackChanged(EventObject event)
		{
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}
	public static class VfeCommandStackEventListener implements CommandStackEventListener
	{
		private int lastState = CommandStack.POST_EXECUTE;

		public void stackChanged(CommandStackEvent event)
		{
			// make sure all changes are coming in as 1 set
			if (event.isPreChangeEvent())
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().startCollectingPersistChanges(false);
			}
			else if (event.isPostChangeEvent())
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().stopCollectingPersistChanges(false);
			}
			lastState = event.getDetail();
		}

		public boolean isRunningCommand()
		{
			return (lastState & CommandStack.POST_MASK) == 0;
		}
	}

	public void setRenderGhosts(boolean b)
	{
		renderGhosts = b;
	}

	public boolean isRenderGhosts()
	{
		return renderGhosts;
	}

	// cheat sheet helper methods

	public void createNewComponent(JSONObject componentDefinition)
	{
		if (graphicaleditor instanceof ISupportCheatSheetActions) ((ISupportCheatSheetActions)graphicaleditor).createNewComponent(componentDefinition);
	}

	@Override
	public void setContentDescription(String description)
	{
		super.setContentDescription(description);
	}

	public void executeDeveloperMenuCommand(Function callback, Form[] forms, BaseComponent[] components)
	{
		getCommandStack().execute(new DeveloperMenuCommand(callback, forms, components));
	}
}
