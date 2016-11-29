package com.servoy.eclipse.ui.views.solutionexplorer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ContextAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenScriptAction;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.util.PersistHelper;


public class FormHierarchyView extends ViewPart implements ISelectionChangedListener
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
				Iterator<ScriptMethod> it = s.getFlattenedForm(input, false).getScriptMethods(true);
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

	public class FormViewLabelProvider extends LabelProvider
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
			return null;
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
				Form f = (Form)parentElement;
				FlattenedSolution s = ModelUtils.getEditingFlattenedSolution(f);
				List<Form> formHierarchy = s.getFormHierarchy(input);
				if (!input.equals(f) && formHierarchy.contains(f))
				{
					return new Object[] { formHierarchy.get(formHierarchy.indexOf(f) - 1) };
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

	private Composite fParent;
	private Form selected = null;
	private SashForm fSplitter;
	private TableViewer list;
	private TreeViewer tree;
	private final com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();

	private ContextAction openAction;

	@Override
	public void createPartControl(Composite parent)
	{
		openAction = new ContextAction(this, Activator.loadImageDescriptorFromBundle("open.gif"), "Open");

		fParent = parent;
		fSplitter = new SashForm(fParent, SWT.NONE);
		fSplitter.setOrientation(SWT.VERTICAL);//TODO enhance orientation
		createTreeViewer(fSplitter);
		createListViewer(fSplitter);

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
				//if (openAction.isEnabled())
				//{
				openAction.run();
				//}
			}
		});

		IAction openScript = new OpenScriptAction();
		openAction.registerAction(Form.class, openScript);
		openAction.registerAction(ScriptMethod.class, openScript);
		((ISelectionChangedListener)openScript).selectionChanged(new SelectionChangedEvent(list, list.getSelection()));
		list.addSelectionChangedListener((ISelectionChangedListener)openScript);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		ISelectionProvider selectionProvider = event.getSelectionProvider();
		IStructuredSelection sel = (IStructuredSelection)selectionProvider.getSelection();
		if (sel.size() == 1)
		{
			selected = (Form)sel.iterator().next();
			list.setInput(selected);
			list.refresh();
		}
	}

	public void setSelection(Object object)
	{
		if (object instanceof Form)
		{
			selected = (Form)object;
			tree.setInput(new Form[] { selected });
//			List<Form> expanded = ModelUtils.getEditingFlattenedSolution(selected).getFormHierarchy(selected);
//			Collections.reverse(expanded);
//			tree.setSelection(new StructuredSelection(selected));
			//TODO tree.setExpandedTreePaths(new TreePath[] { new TreePath(expanded.toArray()) });
			tree.expandAll();
			tree.refresh();

			list.setInput(selected);
			list.refresh();
		}
	}

	@Override
	public void setFocus()
	{
		// TODO Auto-generated method stub

	}
}
