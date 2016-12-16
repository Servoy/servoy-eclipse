package com.servoy.eclipse.ui.views.solutionexplorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ContextAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenFormEditorAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenPersistEditorAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenScriptAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OrientationAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ShowAllMembers;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ShowMembersInFormHierarchy;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.SortedList;


public class FormHierarchyView extends ViewPart implements ISelectionChangedListener, IOrientedView
{
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
					if (next instanceof ScriptMethod || next instanceof BaseComponent)
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
							if (input instanceof FileEditorInput && persist instanceof ScriptMethod)
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

	public class FormListContentProvider implements IStructuredContentProvider
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
		public Object[] getElements(Object inputElement)
		{
			if (inputElement instanceof Form)
			{
				input = (Form)inputElement;
				Comparator< ? > comparator = input.isResponsiveLayout() ? PositionComparator.XY_PERSIST_COMPARATOR : NameComparator.INSTANCE;
				Form form = input;
				if (showAllInheritedMembers)
				{
					FlattenedSolution s = ModelUtils.getEditingFlattenedSolution(input);
					form = s.getFlattenedForm(input, false);
				}
				Iterator<Part> it1 = form.getParts();
				List<IPersist> parts = new SortedList<IPersist>(comparator);
				while (it1.hasNext())
				{
					parts.add(it1.next());
				}
				List<IPersist> elements = new SortedList<IPersist>(comparator);
				for (IPersist p : form.getAllObjectsAsList())
				{
					if (p instanceof BaseComponent) elements.add(p);
				}
				List<IPersist> methods = new SortedList<IPersist>(comparator);
				Iterator<ScriptMethod> it2 = form.getScriptMethods(true);
				while (it2.hasNext())
				{
					methods.add(it2.next());
				}
				List<IPersist> lst = new ArrayList<>(parts);
				lst.addAll(elements);
				lst.addAll(methods);
				return lst.toArray();
			}
			return new Object[0];
		}
	}

	public class FormViewLabelProvider extends ColumnLabelProvider
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
					return uiActivator.loadImageFromBundle("private_method.gif");
				}
				if (sm.isProtected())
				{
					return uiActivator.loadImageFromBundle("protected_method.gif");
				}
				return uiActivator.loadImageFromBundle("public_method.gif");
			}
			else if (element instanceof BaseComponent || element instanceof Part)
			{
				Pair<String, Image> pair = ElementUtil.getPersistNameAndImage((IPersist)element);
				if (pair != null) return pair.getRight();
			}
			return null;
		}

		@Override
		public String getText(Object element)
		{
			if (element instanceof Form)
			{
				return ((Form)element).getName();
			}
			else if (element instanceof ScriptMethod)
			{
				return getScriptMethodSignature((ScriptMethod)element, null, false, true, true, true);
			}
			else if (element instanceof BaseComponent)
			{
				if (element instanceof ISupportName)
				{
					String name = ((ISupportName)element).getName();
					if (name != null && !"".equals(name)) return name;
				}
				return "<no name>";
			}
			if (element instanceof Part)
			{
				return ((Part)element).toString();
			}
			return null;
		}

		@Override
		public Color getForeground(Object element)
		{
			if (element instanceof Form) return super.getForeground(element);
			if (provider instanceof ITreeContentProvider) return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE);
			if (element instanceof ISupportExtendsID && ((ISupportExtendsID)element).getExtendsID() > 0)
			{
				return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE);
			}
			if (element instanceof ScriptMethod)
			{
				AbstractBase sm = (AbstractBase)element;
				if (!sm.getParent().equals(selected))
				{
					return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE);
				}
			}
			return super.getForeground(element);
		}

		//TODO refactor, copied from solex lst content provider
		private String getScriptMethodSignature(ScriptMethod sm, String methodName, boolean showParam, boolean showParamType, boolean showReturnType,
			boolean showReturnTypeAtEnd)
		{
			MethodArgument[] args = sm.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);

			StringBuilder methodSignatureBuilder = new StringBuilder();
			methodSignatureBuilder.append(methodName != null ? methodName : sm.getName()).append('(');
			for (int i = 0; i < args.length; i++)
			{
				if ((showParam || showParamType) && args[i].isOptional()) methodSignatureBuilder.append('[');
				if (showParam)
				{
					methodSignatureBuilder.append(args[i].getName());
					if (showParamType) methodSignatureBuilder.append(':');
				}
				if (showParamType) methodSignatureBuilder.append(args[i].getType());
				if ((showParam || showParamType) && args[i].isOptional()) methodSignatureBuilder.append(']');
				if (i < args.length - 1) methodSignatureBuilder.append(", ");
			}
			methodSignatureBuilder.append(')');

			if (showReturnType)
			{
				MethodArgument returnTypeArgument = sm.getRuntimeProperty(IScriptProvider.METHOD_RETURN_TYPE);
				String returnType = "void";
				if (returnTypeArgument != null)
				{
					if ("*".equals(returnTypeArgument.getType().getName())) returnType = "Any";
					else returnType = returnTypeArgument.getType().getName();
				}
				if (showReturnTypeAtEnd)
				{
					methodSignatureBuilder.append(" - ").append(returnType);
				}
				else
				{
					methodSignatureBuilder.insert(0, ' ').insert(0, returnType);
				}
			}

			return methodSignatureBuilder.toString();
		}
	}

	public class FormTreeContentProvider implements ITreeContentProvider
	{
		private Form input;
		private IPersist listSelection;
		private FlattenedSolution activeSolution;
		private ArrayList<IPersist> leavesToExpand = new ArrayList<>();

		@Override
		public void dispose()
		{
			input = null;
			listSelection = null;
			activeSolution = null;
			leavesToExpand = null;
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
		{
			activeSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getEditingFlattenedSolution();
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
				if (listSelection instanceof ScriptMethod)
				{
					ScriptMethod sm = f.getScriptMethod(((ScriptMethod)listSelection).getName());
					if (sm != null)
					{
						result.add(sm);
						leavesToExpand.add(sm);
					}
				}
				else if (listSelection instanceof BaseComponent)
				{
					for (IPersist p : f.getAllObjectsAsList())
					{
						if (p.equals(listSelection) || PersistHelper.getOverrideHierarchy((ISupportExtendsID)listSelection).contains(p) ||
							PersistHelper.getHierarchyChildren((AbstractBase)listSelection).contains(p))
						{
							result.add(p);
							leavesToExpand.add(p);
						}
					}
				}

				List<Form> formHierarchy = activeSolution.getFormHierarchy(input);
				if (!input.equals(f) && formHierarchy.contains(f))
				{
					result.add(formHierarchy.get(formHierarchy.indexOf(f) - 1));
				}
				else
				{
					result.addAll(activeSolution.getDirectlyInheritingForms(f));
				}
				return result.toArray();
			}
			return new Object[0];
		}

		public List<IPersist> getLeavesToExpand()
		{
			return leavesToExpand;
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
				FlattenedSolution s = ModelUtils.getEditingFlattenedSolution(f);
				boolean hasChildren = !s.getDirectlyInheritingForms(f).isEmpty();
				if (!hasChildren && listSelection != null)
				{
					if (listSelection instanceof ScriptMethod)
					{
						ScriptMethod sm = f.getScriptMethod(((ScriptMethod)listSelection).getName());
						if (sm != null) return true;
					}
					else if (listSelection instanceof BaseComponent)
					{
						for (IPersist p : f.getAllObjectsAsList())
						{
							if (p.equals(listSelection) || PersistHelper.getOverrideHierarchy((ISupportExtendsID)listSelection).contains(p) ||
								PersistHelper.getHierarchyChildren((AbstractBase)listSelection).contains(p))
							{
								return true;
							}
						}
					}
				}
				return hasChildren;
			}
			return false;
		}

		public void setSelection(IPersist object)
		{
			listSelection = object;
			leavesToExpand = new ArrayList<>();
		}
	}

	public static final String ID = "com.servoy.eclipse.ui.views.solutionexplorer.FormHierarchyView";

	public static final int VIEW_ORIENTATION_VERTICAL = 0;
	public static final int VIEW_ORIENTATION_HORIZONTAL = 1;
	public static final int VIEW_ORIENTATION_AUTOMATIC = 2;
	public static final String DIALOGSTORE_VIEWORIENTATION = "FormHierarchyView.orientation";
	private static final String DIALOGSTORE_RATIO = "FormHierarchyView.ratio";

	private IDialogSettings fDialogSettings;

	private Composite fParent;
	private Form selected = null;
	private SashForm fSplitter;
	private TableViewer list;
	private TreeViewer tree;
	private ToolBar listToolBar;
	private final com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();

	private ContextAction openAction;

	private int fCurrentOrientation;
	private OrientationAction[] fToggleOrientationActions;
	private SelectionProviderMediator fSelectionProviderMediator;

	private FormTreeContentProvider treeProvider;

	private boolean showAllInheritedMembers;
	private boolean noSelectionChange = false;

	private ShowMembersInFormHierarchy showMembersAction;

	@Override
	public void createPartControl(Composite parent)
	{
		fParent = parent;
		fDialogSettings = Activator.getDefault().getDialogSettings();
		fToggleOrientationActions = new OrientationAction[] { new OrientationAction(this, VIEW_ORIENTATION_VERTICAL), new OrientationAction(this,
			VIEW_ORIENTATION_HORIZONTAL), new OrientationAction(this, VIEW_ORIENTATION_AUTOMATIC) };

		fSplitter = new SashForm(fParent, SWT.NONE);
		initOrientation();//TODO add preference page & menu for the orientation settings

		openAction = new ContextAction(this, Activator.loadImageDescriptorFromBundle("open.gif"), "Open");
		createTreeViewer(fSplitter);
		createListViewer(fSplitter);
		fSelectionProviderMediator = new SelectionProviderMediator(new StructuredViewer[] { tree, list }, null);
		getSite().setSelectionProvider(fSelectionProviderMediator);

		this.selectionChanged(new SelectionChangedEvent(tree, tree.getSelection()));
		tree.addSelectionChangedListener(this);

		contributeToActionBars();
	}

	private void createTreeViewer(Composite parent)
	{
		tree = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		treeProvider = new FormTreeContentProvider();
		tree.setContentProvider(treeProvider);
		tree.setLabelProvider(new FormViewLabelProvider(treeProvider));

		tree.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(DoubleClickEvent event)
			{
				if (openAction.isEnabled())
				{
					openAction.run();
				}
			}
		});

		tree.addPostSelectionChangedListener(new FocusSelectedElementListener());

		IAction openScript = new OpenScriptAction();
		openAction.registerAction(ScriptMethod.class, openScript);
		IAction openPersistEditor = new OpenPersistEditorAction();
		openAction.registerAction(BaseComponent.class, openPersistEditor);
		IAction openFormEditor = new OpenFormEditorAction();
		openAction.registerAction(Form.class, openFormEditor);//TODO preference whether to open form or script
		openAction.selectionChanged(new SelectionChangedEvent(tree, tree.getSelection()));
		tree.addSelectionChangedListener(openAction);
	}

	private void createListViewer(Composite parent)
	{
		ViewForm viewForm = new ViewForm(parent, SWT.NONE);
		list = new TableViewer(viewForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		ColumnViewerToolTipSupport.enableFor(list);
		list.setContentProvider(new FormListContentProvider(this));
		list.setLabelProvider(new FormViewLabelProvider(null));
		viewForm.setContent(list.getControl());

		list.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(DoubleClickEvent event)
			{
				if (openAction.isEnabled())
				{
					openAction.run();
				}
			}
		});

		list.addPostSelectionChangedListener(new FocusSelectedElementListener());

		IAction openScript = new OpenScriptAction();
		openAction.registerAction(ScriptMethod.class, openScript);
		IAction openPersistEditor = new OpenPersistEditorAction();
		openAction.registerAction(BaseComponent.class, openPersistEditor);
		openAction.selectionChanged(new SelectionChangedEvent(list, list.getSelection()));
		list.addSelectionChangedListener(openAction);

		listToolBar = new ToolBar(viewForm, SWT.FLAT | SWT.WRAP);
		viewForm.setTopCenter(listToolBar);
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
			}
		}
	}

	public void setSelection(Object object)
	{
		if (object instanceof Form)
		{
			selected = (Form)object;
			tree.setAutoExpandLevel(ModelUtils.getEditingFlattenedSolution(selected).getFormHierarchy(selected).size() + 1);
			tree.setInput(new Form[] { selected });
			tree.setSelection(new StructuredSelection(selected));
			tree.refresh();

			list.setInput(selected);
		}
		else if (tree.getInput() == null)
		{
			showMembersAction.setChecked(true);//TODO check if eclipse does this or shows only when the button is checked already
			selected = (Form)(((IPersist)object).getParent());
			tree.setInput(new Form[] { selected });
			showMembersInFormHierarchy(object);

			list.setInput(selected);
		}
		else
		{
			showMembersInFormHierarchy(object);
		}
		list.refresh();
	}

	private void showMembersInFormHierarchy(Object object)
	{
		treeProvider.setSelection((IPersist)object);
		if (object == null)
		{
			tree.refresh();
			return;
		}

		tree.getTree().setRedraw(false);
		tree.refresh();
		tree.expandAll();
		List<IPersist> toExpand = treeProvider.getLeavesToExpand();
		Set<TreePath> paths = new HashSet<>();
		for (IPersist p : toExpand)
		{
			Form parent = (Form)p.getParent();
			List<IPersist> path = new ArrayList<IPersist>();
			path.addAll(ModelUtils.getEditingFlattenedSolution(parent).getFormHierarchy(parent));
			Collections.reverse(path);
			paths.add(new TreePath(path.toArray()));
		}
		tree.getTree().setRedraw(true);
		tree.refresh();
		noSelectionChange = true;
		tree.setExpandedTreePaths(paths.toArray(new TreePath[paths.size()]));
		tree.setSelection(new StructuredSelection(object));
		noSelectionChange = false;
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
	}

	private void fillListToolbar(ToolBarManager lowertbmanager)
	{
		showMembersAction = new ShowMembersInFormHierarchy(this, false);
		lowertbmanager.add(showMembersAction);

		ShowAllMembers showAllAction = new ShowAllMembers(this, false);
		lowertbmanager.add(showAllAction);

		showMembersAction.selectionChanged(new SelectionChangedEvent(list, list.getSelection()));
		list.addSelectionChangedListener(showMembersAction);
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

	public void showAllInheritedMembers(boolean on)
	{
		showAllInheritedMembers = on;
		list.refresh();
	}
}
