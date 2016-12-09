package com.servoy.eclipse.ui.views.solutionexplorer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IAction;
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
import org.eclipse.ui.part.ViewPart;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ContextAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenPersistEditorAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenScriptAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OrientationAction;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;


public class FormHierarchyView extends ViewPart implements ISelectionChangedListener, IOrientedView
{
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
				FlattenedSolution s = ModelUtils.getEditingFlattenedSolution(input);
				List<Object> lst = new ArrayList<>();
				//TODO foundset?
				Form flattenedForm = s.getFlattenedForm(input, false);
				for (IPersist p : flattenedForm.getAllObjectsAsList())
				{
					if (p instanceof BaseComponent) lst.add(p);
				}
				Iterator<ScriptMethod> it = flattenedForm.getScriptMethods(true);
				while (it.hasNext())
				{
					ScriptMethod sm = it.next();
					if (!sm.isPrivate()) lst.add(sm);
				}
				return lst.toArray();
			}
			return new Object[0];
		}
	}

	public class FormViewLabelProvider extends ColumnLabelProvider
	{
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
			else if (element instanceof BaseComponent)
			{
				Pair<String, Image> pair = ElementUtil.getPersistNameAndImage((BaseComponent)element);
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
				ScriptMethod sm = (ScriptMethod)element;
				if (sm.getParent().equals(selected))
				{
					return getScriptMethodSignature(sm, null, false, true, true, true);
				}
				else
				{
					return getScriptMethodSignature(sm, null, false, true, true, true) + " [" + ((Form)sm.getParent()).getName() + "]";
				}
			}
			else if (element instanceof BaseComponent)
			{
				if (element instanceof ISupportName)
				{
					String name = ((ISupportName)element).getName();
					if (name != null && !"".equals(name)) return name;
				}
				Pair<String, Image> pair = ElementUtil.getPersistNameAndImage((BaseComponent)element);
				if (pair != null) return "<" + pair.getLeft() + ">";
			}
			return null;
		}

		@Override
		public Color getForeground(Object element)
		{
			if (element instanceof ScriptMethod || element instanceof BaseComponent)
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
		private IPersist input;

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
			if (inputElement instanceof Form[])
			{
				input = ((Form[])inputElement)[0];
				Form root = (Form)PersistHelper.getBasePersist((Form)input);
				return new Form[] { root };
			}
			else if (inputElement instanceof ScriptMethod[])
			{
				input = ((ScriptMethod[])inputElement)[0];
				Form root = (Form)PersistHelper.getBasePersist((Form)input.getParent());
				return new Form[] { root };
			}
			return new Form[0];
		}

		@Override
		public Object[] getChildren(Object parentElement)
		{
			//TODO check if form has a method with the same signature as input (if it overrides)
			if (input instanceof ScriptMethod && input.getParent().equals(parentElement))
			{
				FlattenedSolution s = ModelUtils.getEditingFlattenedSolution(input);
				ArrayList<IPersist> result = new ArrayList<IPersist>(s.getDirectlyInheritingForms((Form)parentElement));
				result.add(0, input);
				return result.toArray();
			}
			if (parentElement instanceof Form)
			{
				Form f = (Form)parentElement;
				FlattenedSolution s = ModelUtils.getEditingFlattenedSolution(f);
				if (input instanceof Form)
				{
					List<Form> formHierarchy = s.getFormHierarchy((Form)input);
					if (!input.equals(f) && formHierarchy.contains(f))
					{
						return new Object[] { formHierarchy.get(formHierarchy.indexOf(f) - 1) };
					}
				}
				return s.getDirectlyInheritingForms(f).toArray();
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
			if (element instanceof ScriptMethod)
			{
				return ((ScriptMethod)element).getParent();
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
				return !s.getDirectlyInheritingForms(f).isEmpty();
			}
			return false;
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
	private final com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();

	private ContextAction openAction;

	private int fCurrentOrientation;
	private OrientationAction[] fToggleOrientationActions;
	private SelectionProviderMediator fSelectionProviderMediator;

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
	}

	private void createTreeViewer(Composite parent)
	{
		tree = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		tree.setContentProvider(new FormTreeContentProvider());
		tree.setLabelProvider(new FormViewLabelProvider());
	}

	private void createListViewer(Composite parent)
	{
		ViewForm viewForm = new ViewForm(parent, SWT.NONE);
		list = new TableViewer(viewForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		ColumnViewerToolTipSupport.enableFor(list);
		list.setContentProvider(new FormListContentProvider(this));
		list.setLabelProvider(new FormViewLabelProvider());
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

		IAction openScript = new OpenScriptAction();
		openAction.registerAction(ScriptMethod.class, openScript);
		IAction openPersistEditor = new OpenPersistEditorAction();
		openAction.registerAction(BaseComponent.class, openPersistEditor);
		openAction.selectionChanged(new SelectionChangedEvent(list, list.getSelection()));
		list.addSelectionChangedListener(openAction);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
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
		Form inputForm = (Form)(object instanceof Form ? object : ((IPersist)object).getParent());
		tree.setAutoExpandLevel(ModelUtils.getEditingFlattenedSolution(inputForm).getFormHierarchy(inputForm).size() + 1);
		if (object instanceof Form)
		{
			selected = (Form)object;
			tree.setInput(new Form[] { selected });

			list.setInput(selected);
			list.refresh();
		}
		else if (object instanceof ScriptMethod)
		{
			ScriptMethod sm = (ScriptMethod)object;
			selected = (Form)sm.getParent();
			tree.setInput(new ScriptMethod[] { sm });

			list.setInput(sm);
			list.refresh();
		}
		tree.setSelection(new StructuredSelection(selected));
		tree.refresh();
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


	@Override
	public void setFocus()
	{
		// TODO Auto-generated method stub

	}
}
