package com.servoy.eclipse.ui.views.solutionexplorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.PropertySheetPage;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.ViewPartHelpContextProvider;
import com.servoy.eclipse.ui.preferences.SolutionExplorerPreferences;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.search.SearchAction;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.util.IDeprecationProvider;
import com.servoy.eclipse.ui.views.ModifiedPropertySheetEntry;
import com.servoy.eclipse.ui.views.ModifiedPropertySheetPage;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ContextAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.FormHierarchyFilter;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenFormEditorAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenPersistEditorAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenScriptAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenWizardAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OrientationAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OverrideMethodAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ShowMembersInFormHierarchy;
import com.servoy.eclipse.ui.wizards.NewFormWizard;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportDeprecated;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UUID;


public class FormHierarchyView extends ViewPart implements ISelectionChangedListener, IOrientedView, ITreeListView
{
	private final class FormHierarchyDoubleClickListener implements IDoubleClickListener
	{
		public void doubleClick(DoubleClickEvent event)
		{
			if (openAction.isEnabled())
			{
				openAction.run();
				fSelectionProviderMediator.doSelectionChanged(new SelectionChangedEvent(event.getViewer(), event.getSelection()));
			}
		}
	}

	private final class FocusSelectedElementListener implements ISelectionChangedListener
	{
		@Override
		public void selectionChanged(SelectionChangedEvent event)
		{
			if (openAction.isEnabled())
			{
				IPersist persist = null;
				IStructuredSelection sel = (IStructuredSelection)event.getSelection();
				Iterator< ? > it = sel.iterator();
				if (it.hasNext())
				{
					Object next = it.next();
					if (next instanceof ScriptMethod || next instanceof ScriptVariable || next instanceof BaseComponent)
					{
						persist = (IPersist)next;
					}
				}
				//if the editor is open, show persist in the editor
				if (isEditorOpen(persist)) openAction.run();
			}
		}

		private boolean isEditorOpen(IPersist persist)
		{
			if (persist == null) return false;
			for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows())
			{
				for (IWorkbenchPage page : window.getPages())
				{
					for (IEditorReference editor : page.getEditorReferences())
					{
						try
						{
							IEditorInput input = editor.getEditorInput();
							if (input instanceof FileEditorInput && (persist instanceof ScriptMethod || persist instanceof ScriptVariable))
							{
								FileEditorInput fileEditorInput = (FileEditorInput)input;
								String path = SolutionSerializer.getScriptPath(persist.getParent(), false);
								if (path.equals(
									fileEditorInput.getFile().getProject().getName() + "/" + fileEditorInput.getFile().getProjectRelativePath().toString()))
									return true;
							}
							if (input instanceof PersistEditorInput && persist instanceof BaseComponent)
							{
								PersistEditorInput persistEditorInput = (PersistEditorInput)input;
								Form f = (Form)persist.getAncestor(IRepository.FORMS);
								if (f.getUUID().equals(persistEditorInput.getUuid())) return true;
							}
						}
						catch (PartInitException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}
			return false;
		}
	}

	private static Pair<String, Image> ELEMENTS;
	private static Pair<String, Image> PARTS;
	private static Pair<String, Image> METHODS;
	private static Pair<String, Image> VARIABLES;
	static
	{
		ELEMENTS = new Pair<String, Image>("elements", getImage("element.png"));
		PARTS = new Pair<String, Image>("parts", getImage("parts.png"));
		METHODS = new Pair<String, Image>("methods", getImage("function.png"));
		VARIABLES = new Pair<String, Image>("variables", getImage("form_variable.png"));
	}

	private static Image getImage(String name)
	{
		Image image = null;
		if (name != null)
		{
			image = Activator.getDefault().loadImageFromOldLocation(name);
			if (image == null)
			{
				image = Activator.getDefault().loadImageFromBundle(name);
			}
		}
		return image;
	}


	public class FormListContentProvider implements ITreeContentProvider
	{
		private Form input;
		private final FormHierarchyView view;

		public FormListContentProvider(FormHierarchyView formHierarchyView)
		{
			this.view = formHierarchyView;
		}


		@Override
		public void dispose()
		{
			input = null;
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
		{
		}

		@Override
		public Object[] getChildren(Object inputElement)
		{
			Comparator< ? > comparator = input.isResponsiveLayout() ? PositionComparator.XY_PERSIST_COMPARATOR : NameComparator.INSTANCE;
			Form form = showAllAction.isChecked() ? getActiveSolution().getFlattenedForm(input, false) : input;
			if (inputElement instanceof Pair)
			{
				if (inputElement == ELEMENTS)
				{
					Set<Pair<String, Image>> availableCategories = new TreeSet<Pair<String, Image>>(new Comparator<Pair<String, Image>>()
					{
						@Override
						public int compare(Pair<String, Image> o1, Pair<String, Image> o2)
						{
							if (o1 != null && o2 != null && o1.getLeft() != null && o2.getLeft() != null)
							{
								return o1.getLeft().compareTo(o2.getLeft());
							}
							return 0;
						}
					});
					for (IPersist p : form.getAllObjectsAsList())
					{
						if (p instanceof IFormElement) availableCategories.add(ElementUtil.getPersistNameAndImage(p));
					}
					return availableCategories.toArray();
				}

				List<IPersist> lst = new SortedList<IPersist>(comparator);
				for (IPersist persist : form.getAllObjectsAsList())
				{
					Pair<String, Image> nameAndImage = ElementUtil.getPersistNameAndImage(persist);
					if (nameAndImage.equals(inputElement)) lst.add(persist);
					if (inputElement == METHODS && persist instanceof ScriptMethod) lst.add(persist);
					if (inputElement == VARIABLES && persist instanceof ScriptVariable) lst.add(persist);
				}
				return lst.toArray();
			}
			if (inputElement instanceof Form)
			{
				Iterator<Part> it1 = form.getParts();
				List<IPersist> parts = new SortedList<IPersist>(comparator);
				if (!hidePartsAction.isChecked())
				{
					while (it1.hasNext())
					{
						parts.add(it1.next());
					}
				}
				List<IPersist> elements = new SortedList<IPersist>(comparator);
				List<IPersist> methods = new SortedList<IPersist>(comparator);
				List<IPersist> variables = new SortedList<IPersist>(comparator);
				for (IPersist p : form.getAllObjectsAsList())
				{
					if (p instanceof BaseComponent && !hideElementsAction.isChecked()) elements.add(p);
					if (p instanceof ScriptMethod && !hideMethodsAction.isChecked()) methods.add(p);
					if (p instanceof ScriptVariable && !hideVariablesAction.isChecked()) variables.add(p);
				}
				List<IPersist> lst = new ArrayList<>(parts);
				lst.addAll(elements);
				lst.addAll(methods);
				lst.addAll(variables);
				return lst.toArray();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element)
		{
			if (element instanceof IPersist)
			{
				return fDialogSettings.getBoolean(GROUP_ELEMENTS_BY_TYPE) ? ElementUtil.getPersistNameAndImage((IPersist)element)
					: ((IPersist)element).getParent();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element)
		{
			return getChildren(element).length > 0;
		}

		@Override
		public Object[] getElements(Object inputElement)
		{
			if (inputElement instanceof Form)
			{
				input = (Form)inputElement;
				if (fDialogSettings.getBoolean(GROUP_ELEMENTS_BY_TYPE))
				{
					Form form = showAllAction.isChecked() ? getActiveSolution().getFlattenedForm(input, true) : input;
					List<Pair<String, Image>> availableCategories = new ArrayList<>();
					if (!hideElementsAction.isChecked() && form.getFormElementsSortedByFormIndex().hasNext()) availableCategories.add(ELEMENTS);
					if (!hidePartsAction.isChecked() && form.getParts().hasNext()) availableCategories.add(PARTS);
					if (!hideMethodsAction.isChecked() && form.getScriptMethods(false).hasNext()) availableCategories.add(METHODS);
					if (!hideVariablesAction.isChecked() && form.getScriptVariables(false).hasNext()) availableCategories.add(VARIABLES);
					return availableCategories.toArray();
				}
				return getChildren(inputElement);
			}
			return new Form[0];
		}
	}

	private class FormViewLabelProvider extends ColumnLabelProvider implements IDeprecationProvider
	{
		private final IStructuredContentProvider provider;

		public FormViewLabelProvider(IStructuredContentProvider provider)
		{
			this.provider = provider;
		}

		@Override
		public Image getImage(Object element)
		{
			if (element instanceof Form)
			{
				return ElementUtil.getImageForFormEncapsulation((Form)element);
			}
			else if (element instanceof ScriptMethod)
			{
				ScriptMethod sm = (ScriptMethod)element;
				if (sm.isPrivate())
				{
					return uiActivator.loadImageFromBundle("method_private.png");
				}
				if (sm.isProtected())
				{
					return uiActivator.loadImageFromBundle("method_protected.png");
				}
				return sm.isPublic() ? uiActivator.loadImageFromBundle("method_public.png") : uiActivator.loadImageFromBundle("method_default.png");
			}
			else if (element instanceof Pair< ? , ? >)
			{
				return ((Pair<String, Image>)element).getRight();
			}
			else if (element instanceof PersistContext)
			{
				Pair<String, Image> pair = ElementUtil.getPersistNameAndImage(((PersistContext)element).getPersist());
				if (pair != null) return pair.getRight();
			}
			else
			{
				Pair<String, Image> pair = ElementUtil.getPersistNameAndImage((IPersist)element);
				if (pair != null) return pair.getRight();
			}
			return null;
		}

		@Override
		public String getText(Object element)
		{
			if (element instanceof ScriptMethod)
			{
				return ((ScriptMethod)element).getScriptMethodSignature(null, false, true, true, true);
			}
			if (element instanceof Part)
			{
				return ((Part)element).toString();
			}
			if (element instanceof Pair< ? , ? >)
			{
				return ((Pair<String, Image>)element).getLeft();
			}
			if (element instanceof ISupportName)
			{
				String name = ((ISupportName)element).getName();
				if (name != null && !"".equals(name)) return name;
			}
			return "<no name>";
		}

		@Override
		public Color getForeground(Object element)
		{
			if (element instanceof Form) return super.getForeground(element);
			if (provider instanceof FormTreeContentProvider) return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE);
			if (element instanceof AbstractBase)
			{
				AbstractBase sm = (AbstractBase)element;
				if (!sm.getParent().equals(selected))
				{
					return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE);
				}
			}
			return super.getForeground(element);
		}


		@Override
		public Boolean isDeprecated(Object element)
		{
			if (element instanceof ISupportDeprecated)
			{
				String deprecated = ((ISupportDeprecated)element).getDeprecated();
				return Boolean.valueOf(deprecated != null && !"".equals(deprecated.trim()));
			}
			return null;
		}
	}

	public class FormTreeContentProvider implements ITreeContentProvider
	{
		private Form input;
		private IPersist listSelection;
		private List<Form> hierarchy = new ArrayList<>();

		@Override
		public void dispose()
		{
			input = null;
			listSelection = null;
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
		{
		}

		@Override
		public Object[] getElements(Object inputElement)
		{
			if (inputElement instanceof Form[])
			{
				input = ((Form[])inputElement)[0];
				Form root = (Form)PersistHelper.getBasePersist(input);
				return new Form[] { root };
			}
			return new Form[0];
		}

		@Override
		public Object[] getChildren(Object parentElement)
		{
			if (parentElement instanceof Form)
			{
				ArrayList<IPersist> result = new ArrayList<IPersist>();
				Form f = (Form)parentElement;
				List<Form> directChildren = getActiveSolution().getDirectlyInheritingForms(f);
				if (showMembersAction.isChecked() && listSelection != null)
				{
					for (Form directChild : directChildren)
					{
						for (Form h : hierarchy)
						{
							if (getActiveSolution().getFormHierarchy(h).contains(directChild))
							{
								result.add(directChild);
								break;
							}
						}
					}
				}
				else
				{
					List<Form> formHierarchy = getActiveSolution().getFormHierarchy(input);
					if (!input.equals(f) && formHierarchy.contains(f))
					{
						result.add(formHierarchy.get(formHierarchy.indexOf(f) - 1));
					}
					else
					{
						result.addAll(directChildren);
					}
				}
				result.sort(NameComparator.INSTANCE);

				if (showMembersAction.isChecked())
				{
					IPersist child = findChild(f, listSelection);
					if (child != null)
					{
						result.add(0, child);
					}
				}

				return result.toArray();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element)
		{
			if (element instanceof Form)
			{
				Form f = (Form)element;
				return f.extendsForm;
			}
			if (element instanceof IPersist)
			{
				return ((IPersist)element).getParent();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element)
		{
			if (element instanceof Form)
			{
				Form f = (Form)element;
				boolean hasChildren = !getActiveSolution().getDirectlyInheritingForms(f).isEmpty();
				if (!hasChildren && listSelection != null && findChild(f, listSelection) != null)
				{
					return true;
				}
				return hasChildren;
			}
			return false;
		}

		public void setSelection(IPersist object)
		{
			listSelection = object;
			hierarchy = getPersistFormHierarchy(listSelection);
		}

		private IPersist findChild(Form f, IPersist persist)
		{
			IPersist child = null;
			if (persist instanceof ScriptMethod)
			{
				ScriptMethod sm = f.getScriptMethod(((ScriptMethod)persist).getName());
				if (sm != null) child = sm;
			}
			else if (persist instanceof BaseComponent)
			{
				for (IPersist p : f.getAllObjectsAsList())
				{
					if (p.equals(persist) || PersistHelper.getOverrideHierarchy((ISupportExtendsID)persist).contains(p) ||
						PersistHelper.getHierarchyChildren((AbstractBase)persist).contains(p))
					{
						child = p;
					}
				}
			}
			return child;
		}

		private List<Form> getPersistFormHierarchy(IPersist persist)
		{
			List<Form> result = new ArrayList<Form>();
			List<AbstractBase> forms = PersistHelper.getOverrideHierarchy(getInput());
			forms.addAll(getAllSubforms(getInput()));
			for (AbstractBase ab : forms)
			{
				if (ab instanceof Form)
				{
					Form form = (Form)ab;
					if (findChild(form, persist) != null)
					{
						result.add(form);
					}
				}
			}
			return result;
		}

		private List<Form> getAllSubforms(Form form)
		{
			List<Form> subforms = new ArrayList<>();
			List<Form> inheriting = getActiveSolution().getDirectlyInheritingForms(form);
			subforms.addAll(inheriting);
			for (Form child : inheriting)
			{
				subforms.addAll(getAllSubforms(child));
			}
			return subforms;
		}
	}

	public static final String ID = "com.servoy.eclipse.ui.views.solutionexplorer.FormHierarchyView";

	public static final int VIEW_ORIENTATION_VERTICAL = 0;
	public static final int VIEW_ORIENTATION_HORIZONTAL = 1;
	public static final int VIEW_ORIENTATION_AUTOMATIC = 2;
	public static final String DIALOGSTORE_VIEWORIENTATION = "FormHierarchyView.orientation";
	private static final String DIALOGSTORE_RATIO = "FormHierarchyView.ratio";
	private static final String DIALOGSTORE_SHOW_MEMBERS = "FormHierarchy.SHOW_MEMBERS";
	public static final String DIALOGSTORE_SHOW_ALL_MEMBERS = "FormHierarchy.SHOW_ALL_MEMBERS";
	public static final String DIALOGSTORE_HIDE_ELEMENTS = "FormHierarchy.HIDE_ELEMENTS";
	public static final String DIALOGSTORE_HIDE_METHODS = "FormHierarchy.HIDE_METHODS";
	public static final String DIALOGSTORE_HIDE_PARTS = "FormHierarchy.HIDE_PARTS";
	public static final String DIALOGSTORE_HIDE_VARIABLES = "FormHierarchy.HIDE_VARIABLES";

	private static final String GROUP_ELEMENTS_BY_TYPE = "GroupElements";
	private static final String OPEN_FORM_PREFERENCE = "FormHierarchy.OPEN_FORM";
	public static final int OPEN_IN_FORM_EDITOR = 0;
	public static final int OPEN_IN_SCRIPT_EDITOR = 1;

	private IDialogSettings fDialogSettings;

	private Composite fParent;
	private Form selected = null;
	private SashForm fSplitter;
	private TreeViewer list;
	private TreeViewer tree;
	private ToolBar listToolBar;
	private final com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();
	private ContextAction openAction;
	private ContextAction treeNewAction;
	private ContextAction searchReference;
	private OverrideMethodAction overrideAction;
	private int fCurrentOrientation;
	private OrientationAction[] fToggleOrientationActions;
	private SelectionProviderMediator fSelectionProviderMediator;
	private FormTreeContentProvider treeProvider;
	private boolean noSelectionChange = false;
	private ShowMembersInFormHierarchy showMembersAction;
	private Menu listDropDownMenu;
	private MenuItem groupElementsToggleButton;
	private FormHierarchyFilter hideElementsAction;
	private FormHierarchyFilter showAllAction;
	private FormHierarchyFilter hideMethodsAction;
	private FormHierarchyFilter hidePartsAction;
	private FormHierarchyFilter hideVariablesAction;

	private IMemento memento;
	private StatusBarUpdater statusBarUpdater;
	private IResourceChangeListener resourceChangeListener;
	private OpenFormAction openInFormEditor;
	private OpenFormAction openInScriptEditor;
	private static final String SELECTED_FORM = "FormHierarchy.SELECTION";

	@Override
	public void saveState(IMemento mem)
	{
		if (getInput() != null)
		{
			mem.putString(SELECTED_FORM, getInput().getUUID().toString());
		}
		else
		{
			//this shouldn't happen
			ServoyLog.logInfo("Save State failed." + (tree.getInput() == null ? "Form Hierarchy input is null." : (getInput().toString() + " uuid is null.")));
		}
		super.saveState(mem);
	}

	@Override
	public void init(IViewSite site, IMemento mem) throws PartInitException
	{
		this.memento = mem;
		super.init(site, mem);
	}

	@Override
	public void createPartControl(Composite parent)
	{
		fParent = parent;
		fDialogSettings = Activator.getDefault().getDialogSettings();
		fToggleOrientationActions = new OrientationAction[] { new OrientationAction(this, VIEW_ORIENTATION_VERTICAL), new OrientationAction(this,
			VIEW_ORIENTATION_HORIZONTAL), new OrientationAction(this, VIEW_ORIENTATION_AUTOMATIC) };
		fSplitter = new SashForm(fParent, SWT.NONE);
		initOrientation();//TODO add preference page & menu for the orientation settings

		openAction = new ContextAction(this, Activator.loadImageDescriptorFromBundle("open.png"), "Open");
		treeNewAction = new ContextAction(this, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD), "New");
		searchReference = new ContextAction(this, null, null);
		overrideAction = new OverrideMethodAction(this);
		createTreeViewer(fSplitter);
		createListViewer(fSplitter);
		createSelectionProvider();
		createStatusBarUpdater();
		contributeToActionBars();
		hookContextMenu();
		restoreView();
		addResourceListener();
	}


	private void restoreView()
	{
		showMembersAction.setChecked(fDialogSettings.getBoolean(DIALOGSTORE_SHOW_MEMBERS));
		if (memento == null) return;
		String formUuid = memento.getString(SELECTED_FORM);
		if (formUuid == null) return;
		IPersist persist = getActiveSolution().searchPersist(formUuid);
		if (persist instanceof Form)
		{
			Form form = (Form)persist;
			tree.setInput(new Form[] { form });
			setContentDescription(form.getName() + " [" + form.getRootObject().getName() + "]");
			setSelection(form);
			expandTreePaths(form);
		}
	}

	private void createStatusBarUpdater()
	{
		IStatusLineManager slManager = getViewSite().getActionBars().getStatusLineManager();
		statusBarUpdater = new StatusBarUpdater(slManager);
		statusBarUpdater.setShowModule(true);
		statusBarUpdater.selectionChanged(new SelectionChangedEvent(list, list.getSelection()));
		list.addSelectionChangedListener(statusBarUpdater);
		statusBarUpdater.selectionChanged(new SelectionChangedEvent(tree, tree.getSelection()));
		tree.addSelectionChangedListener(statusBarUpdater);
	}

	private void createSelectionProvider()
	{
		fSelectionProviderMediator = new SelectionProviderMediator(new StructuredViewer[] { tree, list }, null);
		getSite().setSelectionProvider(fSelectionProviderMediator);
		this.selectionChanged(new SelectionChangedEvent(tree, tree.getSelection()));
		tree.addSelectionChangedListener(this);
	}

	private void createTreeViewer(Composite parent)
	{
		tree = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		treeProvider = new FormTreeContentProvider();
		tree.setContentProvider(treeProvider);
		tree.setLabelProvider(new DecoratedLabelProvider(new FormViewLabelProvider(treeProvider)));

		tree.addDoubleClickListener(new FormHierarchyDoubleClickListener());
		tree.addPostSelectionChangedListener(new FocusSelectedElementListener());

		IAction openScript = new OpenScriptAction();
		openAction.registerAction(ScriptMethod.class, openScript);
		openAction.registerAction(ScriptVariable.class, openScript);
		IAction openPersistEditor = new OpenPersistEditorAction();
		openAction.registerAction(BaseComponent.class, openPersistEditor);
		final OpenFormEditorAction openFormEditor = new OpenFormEditorAction();
		openFormEditor.selectionChanged(new SelectionChangedEvent(tree, tree.getSelection()));
		tree.addSelectionChangedListener(openFormEditor);
		final OpenScriptAction openScriptEditor = new OpenScriptAction();
		openScriptEditor.selectionChanged(new SelectionChangedEvent(tree, tree.getSelection()));
		tree.addSelectionChangedListener(openScriptEditor);
		openAction.registerAction(Form.class, new Action()
		{
			@Override
			public void run()
			{
				fDialogSettings = Activator.getDefault().getDialogSettings();
				if (fDialogSettings.get(OPEN_FORM_PREFERENCE) == null)
				{
					IEclipsePreferences store = InstanceScope.INSTANCE.getNode(Activator.getDefault().getBundle().getSymbolicName());
					String formDblClickOption = store.get(SolutionExplorerPreferences.FORM_DOUBLE_CLICK_ACTION,
						SolutionExplorerPreferences.DOUBLE_CLICK_OPEN_FORM_EDITOR);
					if (SolutionExplorerPreferences.DOUBLE_CLICK_OPEN_FORM_EDITOR.equals(formDblClickOption))
					{
						openFormEditor.run();
					}
					else
					{
						openScriptEditor.run();
					}
				}
				else
				{
					if (fDialogSettings.getInt(OPEN_FORM_PREFERENCE) == OPEN_IN_FORM_EDITOR)
					{
						openFormEditor.run();
					}
					else
					{
						openScriptEditor.run();
					}
				}
			}
		});
		openAction.selectionChanged(new SelectionChangedEvent(tree, tree.getSelection()));
		tree.addSelectionChangedListener(openAction);

		IAction newSubform = new OpenWizardAction(NewFormWizard.class, Activator.loadImageDescriptorFromBundle("designer.png"), "Create new sub form");
		treeNewAction.registerAction(Form.class, newSubform);
		treeNewAction.selectionChanged(new SelectionChangedEvent(tree, tree.getSelection()));
		tree.addSelectionChangedListener(treeNewAction);
		IAction sa = new SearchAction();
		searchReference.registerAction(Form.class, sa);
		searchReference.registerAction(ScriptMethod.class, sa);
		searchReference.registerAction(BaseComponent.class, sa);
		searchReference.selectionChanged(new SelectionChangedEvent(tree, tree.getSelection()));
		tree.addSelectionChangedListener(searchReference);
	}

	private void createListViewer(Composite parent)
	{
		ViewForm viewForm = new ViewForm(parent, SWT.NONE);
		list = new TreeViewer(viewForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		ColumnViewerToolTipSupport.enableFor(list);
		FormListContentProvider listProvider = new FormListContentProvider(this);
		list.setContentProvider(listProvider);
		list.setLabelProvider(new DecoratedLabelProvider(new FormViewLabelProvider(listProvider)));
		viewForm.setContent(list.getControl());

		list.addDoubleClickListener(new FormHierarchyDoubleClickListener());
		list.addPostSelectionChangedListener(new FocusSelectedElementListener());

		IAction openScript = new OpenScriptAction();
		openAction.registerAction(ScriptMethod.class, openScript);
		IAction openPersistEditor = new OpenPersistEditorAction();
		openAction.registerAction(BaseComponent.class, openPersistEditor);
		openAction.selectionChanged(new SelectionChangedEvent(list, list.getSelection()));
		list.addSelectionChangedListener(openAction);
		overrideAction.selectionChanged(new SelectionChangedEvent(list, list.getSelection()));
		list.addSelectionChangedListener(overrideAction);

		searchReference.selectionChanged(new SelectionChangedEvent(list, list.getSelection()));
		list.addSelectionChangedListener(searchReference);
		listToolBar = new ToolBar(viewForm, SWT.FLAT | SWT.WRAP);
		viewForm.setTopCenter(listToolBar);

		final ToolBar listMenuToolbar = new ToolBar(viewForm, SWT.FLAT | SWT.WRAP);
		ToolBarManager listMenuToolbarManager = new ToolBarManager(listMenuToolbar);
		viewForm.setTopRight(listMenuToolbar);

		listDropDownMenu = new Menu(listMenuToolbar);
		Action pullDown = new Action("Menu", IAction.AS_PUSH_BUTTON)
		{
			@Override
			public void run()
			{
				Point pt = listMenuToolbar.toDisplay(0, listMenuToolbar.getSize().y);
				listDropDownMenu.setLocation(pt.x, pt.y);
				listDropDownMenu.setVisible(true);
			}
		};
		Image dropDownImage = UIUtils.paintViewMenuImage();
		if (dropDownImage != null)
		{
			ImageDescriptor dropDownImageDescriptor = ImageDescriptor.createFromImage(dropDownImage);
			pullDown.setImageDescriptor(dropDownImageDescriptor);
			pullDown.setDisabledImageDescriptor(dropDownImageDescriptor);
			pullDown.setHoverImageDescriptor(dropDownImageDescriptor);
		}
		pullDown.setToolTipText("More options");
		listMenuToolbarManager.add(pullDown);
		listMenuToolbarManager.update(true);

	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		if (noSelectionChange) return;
		ISelectionProvider selectionProvider = event.getSelectionProvider();
		IStructuredSelection sel = (IStructuredSelection)selectionProvider.getSelection();
		if (sel.size() == 1)
		{
			Object obj = sel.iterator().next();
			if (obj instanceof Form)
			{
				selected = (Form)obj;
				list.setInput(selected);
				list.refresh();
				list.expandAll();
			}
		}
	}

	public void setSelection(IPersist persist)
	{
		setSelection(persist, true);
	}

	public void setSelection(IPersist persist, boolean refreshList)
	{
		if (noSelectionChange) return;
		if (persist == null)
		{
			tree.refresh();
			return;
		}

		if (persist instanceof Form)
		{
			setSelectionInTree(persist);
			list.setInput(selected);
		}
		else if (tree.getInput() == null)
		{
			selected = (Form)((persist).getParent());
			showMembersInFormHierarchy(persist, true);
			list.setInput(selected);
		}
		else
		{
			showMembersInFormHierarchy(persist, refreshList);
		}
		list.refresh();
		list.expandAll();
	}

	private void setSelectionInTree(Object object)
	{
		selected = (Form)object;
		tree.setAutoExpandLevel(getActiveSolution().getFormHierarchy(selected).size() + 1);
		tree.setSelection(new StructuredSelection(selected));
		tree.refresh();
	}

	private void showMembersInFormHierarchy(IPersist persist, boolean refreshList)
	{
		treeProvider.setSelection(persist);

		tree.refresh();
		expandTreePaths(persist);
		try
		{
			noSelectionChange = true;
			if (refreshList)
			{
				list.setInput(persist.getParent());
				list.setSelection(new StructuredSelection(persist));
				list.reveal(persist);
			}
		}
		finally
		{
			noSelectionChange = false;
		}
	}


	private void expandTreePaths(IPersist object)
	{
		if (showMembersAction.isChecked())
		{
			//the tree is filtered, so every leaf is an override of the searched member or the member itself
			tree.expandAll();
			try
			{
				noSelectionChange = true;
				tree.setSelection(new StructuredSelection(object));
			}
			finally
			{
				noSelectionChange = false;
			}
		}
		else
		{
			List<IPersist> path = new ArrayList<IPersist>();
			path.addAll(getActiveSolution().getFormHierarchy(getInput()));
			Collections.reverse(path);
			TreePath treePath = new TreePath(path.toArray());
			tree.getTree().setRedraw(true);
			tree.refresh();
			try
			{
				noSelectionChange = true;
				tree.setExpandedTreePaths(new TreePath[] { treePath });
				tree.setSelection(new StructuredSelection(object));
			}
			finally
			{
				noSelectionChange = false;
			}
		}
	}

	public void setOrientation(int o)
	{
		int orientation = o == VIEW_ORIENTATION_VERTICAL || o == VIEW_ORIENTATION_HORIZONTAL ? o : VIEW_ORIENTATION_AUTOMATIC;

		if (fCurrentOrientation != orientation)
		{
			if (fCurrentOrientation >= 0)
			{
				saveSplitterRatio();
			}
			if (fSplitter != null && !fSplitter.isDisposed())
			{
				int swtOrientation;
				if (orientation == VIEW_ORIENTATION_HORIZONTAL)
				{
					swtOrientation = SWT.HORIZONTAL;
				}
				else if (orientation == VIEW_ORIENTATION_VERTICAL)
				{
					swtOrientation = SWT.VERTICAL;
				}
				else
				{
					swtOrientation = computeDesiredOrientation();
				}
				fSplitter.setOrientation(swtOrientation);
				fSplitter.layout();
			}

			fCurrentOrientation = orientation;
			fDialogSettings.put(DIALOGSTORE_VIEWORIENTATION, fCurrentOrientation);
			restoreSplitterRatio();
		}
	}

	private void saveSplitterRatio()
	{
		if (fSplitter != null && !fSplitter.isDisposed())
		{
			int[] weigths = fSplitter.getWeights();
			int ratio = (weigths[0] * 1000) / (weigths[0] + weigths[1]);
			String key = DIALOGSTORE_RATIO + fSplitter.getOrientation();
			fDialogSettings.put(key, ratio);
		}
	}

	private void restoreSplitterRatio()
	{
		String ratio = fDialogSettings.get(DIALOGSTORE_RATIO + fSplitter.getOrientation());
		if (ratio == null) return;
		int intRatio = Integer.parseInt(ratio);
		fSplitter.setWeights(new int[] { intRatio, 1000 - intRatio });
	}

	public int computeDesiredOrientation()
	{
		Point size = fParent.getSize();
		if (size.x != 0 && size.y != 0)
		{
			if (size.x > size.y) return SWT.HORIZONTAL;
			else return SWT.VERTICAL;
		}
		return SWT.VERTICAL;
	}

	private void initOrientation()
	{
		int savedOrientation;
		try
		{
			savedOrientation = fDialogSettings.getInt(DIALOGSTORE_VIEWORIENTATION);

			if ((savedOrientation < 0) || (savedOrientation > 3))
			{
				savedOrientation = VIEW_ORIENTATION_AUTOMATIC;
			}
		}
		catch (NumberFormatException e)
		{
			savedOrientation = VIEW_ORIENTATION_AUTOMATIC;
		}

		// force the update
		fCurrentOrientation = -1;
		setOrientation(savedOrientation);
		updateOrientationState();
	}

	private void updateOrientationState()
	{
		for (OrientationAction element : fToggleOrientationActions)
		{
			element.setChecked(fCurrentOrientation == element.getOrientation());
		}
	}

	private void contributeToActionBars()
	{
		ToolBarManager lowertbmanager = new ToolBarManager(listToolBar);
		fillListToolbar(lowertbmanager);
		lowertbmanager.update(true);
		fillListMenu();

		IActionBars bars = getViewSite().getActionBars();
		fillViewMenu(bars.getMenuManager());
	}

	private class OpenFormAction extends Action
	{
		private final int selectionValue;

		public OpenFormAction(String text, int selectionValue, String image)
		{
			super(text, AS_RADIO_BUTTON);
			this.selectionValue = selectionValue;
			setImageDescriptor(Activator.loadImageDescriptorFromBundle(image));
			if (fDialogSettings.get(OPEN_FORM_PREFERENCE) != null) setChecked(fDialogSettings.getInt(OPEN_FORM_PREFERENCE) == selectionValue);
		}

		@Override
		public void run()
		{
			updateOpenFormPreference(selectionValue);
		}
	}

	private void updateOpenFormPreference(int selectionValue)
	{
		fDialogSettings.put(OPEN_FORM_PREFERENCE, selectionValue);
		openInFormEditor.setChecked(fDialogSettings.getInt(OPEN_FORM_PREFERENCE) == OPEN_IN_FORM_EDITOR);
		openInScriptEditor.setChecked(fDialogSettings.getInt(OPEN_FORM_PREFERENCE) == OPEN_IN_SCRIPT_EDITOR);

	}

	private void fillViewMenu(IMenuManager menuManager)
	{
		MenuManager openSubMenu = new MenuManager("Select default open action");
		openInFormEditor = new OpenFormAction("Form Editor", OPEN_IN_FORM_EDITOR, "form.png");
		openSubMenu.add(openInFormEditor);
		openInScriptEditor = new OpenFormAction("Script Editor", OPEN_IN_SCRIPT_EDITOR, "js.png");
		openSubMenu.add(openInScriptEditor);
		menuManager.add(openSubMenu);
	}

	private void fillListMenu()
	{
		groupElementsToggleButton = new MenuItem(listDropDownMenu, SWT.CHECK);
		groupElementsToggleButton.setText("Group elements by type");
		groupElementsToggleButton.setSelection(fDialogSettings.getBoolean(GROUP_ELEMENTS_BY_TYPE));
		groupElementsOption(fDialogSettings.getBoolean(GROUP_ELEMENTS_BY_TYPE));
		groupElementsToggleButton.addSelectionListener(new SelectionListener()
		{

			public void widgetDefaultSelected(SelectionEvent e)
			{
			}

			public void widgetSelected(SelectionEvent e)
			{
				groupElementsOption(groupElementsToggleButton.getSelection());
			}
		});

		ActionContributionItem item = new ActionContributionItem(hideElementsAction);
		item.fill(listDropDownMenu, -1);
		item = new ActionContributionItem(hideMethodsAction);
		item.fill(listDropDownMenu, -1);
		item = new ActionContributionItem(hidePartsAction);
		item.fill(listDropDownMenu, -1);
		item = new ActionContributionItem(hideVariablesAction);
		item.fill(listDropDownMenu, -1);
	}

	private void hookContextMenu()
	{
		MenuManager menuMgr = new MenuManager("#TreePopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener()
		{
			public void menuAboutToShow(IMenuManager manager)
			{
				FormHierarchyView.this.fillTreeContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(tree.getControl());
		tree.getControl().setMenu(menu);

		getSite().registerContextMenu(ID + ".tree", menuMgr, tree);

		menuMgr = new MenuManager("#ListPopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener()
		{
			public void menuAboutToShow(IMenuManager manager)
			{
				FormHierarchyView.this.fillListContextMenu(manager);
			}
		});
		menu = menuMgr.createContextMenu(list.getControl());
		list.getControl().setMenu(menu);

		getSite().registerContextMenu(ID + ".list", menuMgr, list);

	}

	private void fillTreeContextMenu(IMenuManager manager)
	{
		manager.add(new Separator(IWorkbenchActionConstants.OPEN_EXT));
		manager.add(new Separator());
		if (treeNewAction.isEnabled()) manager.add(treeNewAction);
		manager.add(new Separator());
		if (searchReference.isEnabled()) manager.add(searchReference);
	}

	private void fillListContextMenu(IMenuManager manager)
	{
		manager.add(new Separator(IWorkbenchActionConstants.OPEN_EXT));
		manager.add(new Separator());
		if (overrideAction.isEnabled()) manager.add(overrideAction);
		manager.add(new Separator());
		if (searchReference.isEnabled()) manager.add(searchReference);
	}

	public void groupElementsOption(boolean group)
	{
		fDialogSettings.put(GROUP_ELEMENTS_BY_TYPE, group);
		list.refresh();
		list.expandAll();
	}

	private void fillListToolbar(ToolBarManager lowertbmanager)
	{
		showMembersAction = new ShowMembersInFormHierarchy(this, false);
		lowertbmanager.add(showMembersAction);
		showMembersAction.selectionChanged(new SelectionChangedEvent(list, list.getSelection()));
		list.addSelectionChangedListener(showMembersAction);

		showAllAction = new FormHierarchyFilter(list, "show_all_inherited.png", "Show All Inherited Members", DIALOGSTORE_SHOW_ALL_MEMBERS, fDialogSettings);
		lowertbmanager.add(showAllAction);

		hideElementsAction = new FormHierarchyFilter(list, "hide_elements.png", "Hide elements", DIALOGSTORE_HIDE_ELEMENTS, fDialogSettings);
		lowertbmanager.add(hideElementsAction);

		hideMethodsAction = new FormHierarchyFilter(list, "hide_method.png", "Hide methods", DIALOGSTORE_HIDE_METHODS, fDialogSettings);
		lowertbmanager.add(hideMethodsAction);

		hidePartsAction = new FormHierarchyFilter(list, "hide_parts.png", "Hide parts", DIALOGSTORE_HIDE_PARTS, fDialogSettings);
		lowertbmanager.add(hidePartsAction);

		hideVariablesAction = new FormHierarchyFilter(list, "hide_variables.png", "Hide variables", DIALOGSTORE_HIDE_VARIABLES, fDialogSettings);
		lowertbmanager.add(hideVariablesAction);
	}

	@Override
	public void setFocus()
	{
		StructuredViewer focusedView = fSelectionProviderMediator.getViewerInFocus();
		if (focusedView != null)
		{
			focusedView.getControl().setFocus();
		}
		else
		{
			tree.getControl().setFocus();
		}
	}

	private void addResourceListener()
	{
		resourceChangeListener = new IResourceChangeListener()
		{
			private WorkspaceJob refreshHierarchyJob;

			public void resourceChanged(IResourceChangeEvent event)
			{
				HierarchyDecorator decorator = (HierarchyDecorator)PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(HierarchyDecorator.ID);
				if ((event.getType() & IResourceChangeEvent.POST_CHANGE) != 0)
				{
					boolean mustRefresh = false;
					IResourceDelta[] affectedChildren = event.getDelta().getAffectedChildren();
					try
					{
						for (IResourceDelta element : affectedChildren)
						{
							IResource resource = element.getResource();
							if (resource instanceof IProject && ((IProject)resource).hasNature(ServoyProject.NATURE_ID))
							{
								mustRefresh = true;
								break;
							}
						}
					}
					catch (CoreException ex)
					{
						ServoyLog.logError(ex);
					}

					if (mustRefresh)
					{
						if (refreshHierarchyJob != null) refreshHierarchyJob.cancel();
						refreshHierarchyJob = new WorkspaceJob("Refreshing Form Hierarchy")
						{
							@Override
							public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
							{
								Display.getDefault().asyncExec(new Runnable()
								{
									@Override
									public void run()
									{
										setSelection(selected);//refresh
									}
								});
								return Status.OK_STATUS;
							}
						};
						refreshHierarchyJob.setRule(ServoyModel.getWorkspace().getRoot());
						refreshHierarchyJob.schedule();
					}
				}
				else if (decorator != null)
				{
					IMarkerDelta[] markersDelta = event.findMarkerDeltas(IMarker.PROBLEM, true);
					HashSet<IPersist> changedProblemPersists = new HashSet<IPersist>();
					for (IMarkerDelta md : markersDelta)
					{
						IResource r = md.getResource();
						if (r instanceof IFile)
						{
							IFile resource = (IFile)r;
							if (resource.getFileExtension() != null && SolutionSerializer.FORM_FILE_EXTENSION.equals("." + resource.getFileExtension()))//TODO how to refresh decorators for items in js resources?
							{
								ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
									resource.getProject().getName());
								if (servoyProject == null) continue;
								UUID uuid = SolutionDeserializer.getUUID(resource.getRawLocation().toFile());
								IPersist persist = getActiveSolution().searchPersist(uuid);
								if (persist != null) changedProblemPersists.add(persist);
							}
						}
					}

					decorator.fireChanged(changedProblemPersists.toArray(new IPersist[changedProblemPersists.size()]));
				}
			}
		};

		ServoyModelManager.getServoyModelManager().getServoyModel().addResourceChangeListener(resourceChangeListener,
			IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.POST_BUILD);
	}


	@Override
	public Object getAdapter(Class type)
	{
		if (type == org.eclipse.ui.views.properties.IPropertySheetPage.class)
		{
			PropertySheetPage page = new ModifiedPropertySheetPage(null);
			page.setRootEntry(new ModifiedPropertySheetEntry());
			return page;
		}
		if (type.equals(IContextProvider.class))
		{
			return new ViewPartHelpContextProvider("com.servoy.eclipse.ui.form_hierarchy_view");
		}

		return super.getAdapter(type);
	}

	@Override
	public void dispose()
	{
		fDialogSettings.put(DIALOGSTORE_SHOW_MEMBERS, showMembersAction.isChecked());

		if (statusBarUpdater != null)
		{
			statusBarUpdater.dispose();
			fSelectionProviderMediator.removeSelectionChangedListener(statusBarUpdater);
			statusBarUpdater = null;
		}

		if (resourceChangeListener != null)
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().removeResourceChangeListener(resourceChangeListener);
			resourceChangeListener = null;
		}

		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.ITreeListView#getSelectedTreeElement()
	 */
	@Override
	public Object getSelectedTreeElement()
	{
		return ((ITreeSelection)tree.getSelection()).getFirstElement();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.ITreeListView#getSelectedListElement()
	 */
	@Override
	public Object getSelectedListElement()
	{
		return ((IStructuredSelection)list.getSelection()).getFirstElement();
	}

	public void open(Object obj)
	{
		showMembersAction.clearSelection();
		showMembersAction.setChecked(obj instanceof ScriptMethod || obj instanceof IFormElement);
		if (obj instanceof IPersist)
		{
			Form form = (Form)(obj instanceof Form ? obj : (((IPersist)obj).getAncestor(IRepository.FORMS)));
			if (!form.equals(getInput()))
			{
				selected = form;
				tree.setInput(new Form[] { selected });
				setContentDescription(selected.getName() + " [" + selected.getRootObject().getName() + "]");
				setSelectionInTree(selected);
			}
			setSelection((IPersist)obj);
			expandTreePaths((IPersist)obj);
		}
	}

	private Form getInput()
	{
		if (tree.getInput() instanceof Form[])
		{
			Form[] forms = (Form[])tree.getInput();
			return forms.length != 0 ? forms[0] : null;
		}
		return null;
	}

	private FlattenedSolution getActiveSolution()
	{
		return ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
	}
}
