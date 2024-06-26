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
package com.servoy.eclipse.ui.dialogs;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.labelproviders.DelegateLabelProvider;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.IKeywordChecker;
import com.servoy.eclipse.ui.util.ScopeWithContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * A dialog for methods editor that manages a method field.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class MethodDialog extends TreeSelectDialog
{
	// used only in the dialog, are never selected
	public static final Object FORM_METHODS = new Object();
	public static final Object SCOPE_METHODS = new Object();

	private static final Image solutionImage = Activator.getDefault().loadImageFromBundle("solution.png");
	private static final Image formMethodsImage = Activator.getDefault().loadImageFromBundle("designer.png");
	private static final Image scopeMethodsImage = Activator.getDefault().loadImageFromBundle("scopes.png");
	private static final Image foundsetMethodsImage = Activator.getDefault().loadImageFromBundle("foundset_method.png");

	/**
	 * Creates a new method cell dialog parented under the given shell.
	 *
	 * @param labelProvider
	 *
	 * @param parent the parent control
	 */
	public MethodDialog(Shell shell, ILabelProvider labelProvider, ITreeContentProvider contentProvider, ISelection selection,
		Object /* MethodListOptions */ input, int treeStyle, String title, IValueEditor< ? > valueEditor)
	{
		super(shell, true, true, TreePatternFilter.FILTER_LEAFS,
			// content provider
			contentProvider,
			// label provider
			new MethodDialogLabelProvider(labelProvider),
			// ViewerComparator
			null,
			// selection filter
			new LeafnodesSelectionFilter(contentProvider),
			// tree style
			treeStyle,
			// title
			title,
			// input: MethodListOptions
			input,
			// selection
			selection, false, TreeSelectDialog.METHOD_DIALOG, valueEditor, false);

	}

	@Override
	public ISelection getSelection()
	{
		IStructuredSelection selection = (IStructuredSelection)super.getSelection();
		List<MethodWithArguments> lst = new ArrayList<MethodWithArguments>();
		for (Object o : selection.toArray())
		{
			if (o instanceof MethodWithArguments)
			{
				lst.add((MethodWithArguments)o);
			}
			else if (o instanceof ScriptMethod)
			{
				lst.add(MethodWithArguments.create((ScriptMethod)o, null));
			}
		}
		return new StructuredSelection(lst.toArray(new MethodWithArguments[lst.size()]));
	}

	public static class MethodTreeContentProvider extends ArrayContentProvider implements ITreeContentProvider, IKeywordChecker
	{
		private final PersistContext persistContext;
		private Map<Object, Object> parents;
		private Form currentFlattenedForm;

		public MethodTreeContentProvider(PersistContext persistContext)
		{
			this.persistContext = persistContext;
		}

		@Override
		public Object[] getElements(Object inputElement)
		{
			if (inputElement instanceof MethodListOptions)
			{
				// top node
				MethodListOptions options = (MethodListOptions)inputElement;
				List<Object> lst = new ArrayList<Object>();

				if (options.includeNone)
				{
					lst.add(MethodWithArguments.METHOD_NONE);
				}

				if (options.includeDefault)
				{
					lst.add(MethodWithArguments.METHOD_DEFAULT);
				}

				int n = 0;
				if (options.includeFormMethods) n++;
				if (options.includeFoundsetMethods && options.table != null) n++;
				if (options.includeScopeMethods) n++;

				if (n > 1)
				{
					if (options.includeFormMethods) lst.add(FORM_METHODS);
					if (options.includeFoundsetMethods && options.table != null) lst.add(options.table);
					if (options.includeScopeMethods) lst.add(SCOPE_METHODS);
				}
				else
				{
					// just 1 option, load the children at top level
					if (options.includeFormMethods)
					{
						lst.addAll(Arrays.asList(getChildren(FORM_METHODS)));
					}
					else if (options.includeFoundsetMethods && options.table != null)
					{
						lst.addAll(Arrays.asList(getChildren(options.table)));
					}
					else if (options.includeScopeMethods)
					{
						lst.addAll(Arrays.asList(getChildren(SCOPE_METHODS)));
					}
				}

				Object[] nodes = lst.toArray();
				parents = new HashMap<Object, Object>();
				fillParentMap(nodes);
				return nodes;
			}

			return super.getElements(inputElement);
		}

		protected void fillParentMap(Object[] nodes)
		{
			for (Object element : nodes)
			{
				Object[] children = getChildren(element);
				if (children != null && children.length > 0)
				{
					for (Object child : children)
					{
						parents.put(child, element);
					}
					fillParentMap(children);
				}
			}
		}

		public Object[] getChildren(Object parentElement)
		{
			IPersist context = persistContext.getContext();

			if (SCOPE_METHODS == parentElement)
			{
				Solution solution = (Solution)context.getAncestor(IRepository.SOLUTIONS);
				Collection<Pair<String, IRootObject>> scopes = ModelUtils.getEditingFlattenedSolution(solution).getScopes();
				Iterator<Pair<String, IRootObject>> it = scopes.iterator();
				SortedList<ScopeWithContext> scopesList = new SortedList<ScopeWithContext>(ScopeWithContext.SCOPE_COMPARATOR);
				while (it.hasNext())
				{
					Pair<String, IRootObject> sc = it.next();
					if (sc.getRight().getName().equals(solution.getName()))
					{
						scopesList.add(new ScopeWithContext(sc.getLeft(), sc.getRight()));
					}
				}

				Solution[] modules = ModelUtils.getEditingFlattenedSolution(solution).getModules();
				return Utils.arrayInsert(scopesList.toArray(), modules, scopesList == null ? 0 : scopesList.size(), modules == null ? 0 : modules.length);
			}

			IPersist parent;
			Iterator<ScriptMethod> scriptMethods = null;
			List<AbstractBase> nodes = null;
			if (FORM_METHODS == parentElement)
			{
				parent = context.getAncestor(IRepository.FORMS);
				if (parent != null)
				{
					//add superforms
					Form form = (Form)parent;
					scriptMethods = form.getScriptMethods(true);
					currentFlattenedForm = ModelUtils.getEditingFlattenedSolution(context).getFlattenedForm(parent);
					List<AbstractBase> forms = PersistHelper.getOverrideHierarchy(form);
					if (forms.size() >= 2)
					{
						forms.remove(0);
						nodes = forms;
					}
				}
			}
			else if (parentElement instanceof ScopeWithContext)
			{
				scriptMethods = ((Solution)((ScopeWithContext)parentElement).getRootObject()).getScriptMethods(((ScopeWithContext)parentElement).getName(),
					true);
			}
			else if (parentElement instanceof ITable)
			{
				// foundset methods
				parent = context.getAncestor(IRepository.TABLENODES);
				FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(context);
				try
				{
					scriptMethods = editingFlattenedSolution.getFoundsetMethods((ITable)parentElement, true).iterator();
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}
			}

			else if (parentElement instanceof Solution)
			{
				Solution solution = (Solution)parentElement;
				Collection<Pair<String, IRootObject>> scopes = ModelUtils.getEditingFlattenedSolution(solution).getScopes();
				Iterator<Pair<String, IRootObject>> it = scopes.iterator();
				ArrayList<Object> scopesArray = new ArrayList<Object>();
				while (it.hasNext())
				{
					Pair<String, IRootObject> sc = it.next();
					if (solution.getName().equals(sc.getRight().getName()))
					{
						scopesArray.add(new ScopeWithContext(sc.getLeft(), sc.getRight()));
					}
				}

				return scopesArray.toArray();
			}
			else if (parentElement instanceof Form)
			{
				scriptMethods = getFormScriptMethods(parentElement).iterator();
			}
			else
			{
				return null;
			}

			List<Object> lst = new ArrayList<Object>();
			if (scriptMethods != null)
			{
				while (scriptMethods.hasNext())
				{
					ScriptMethod sm = scriptMethods.next();
					if (context.equals(sm.getParent()) || !sm.isPrivate())
					{
						lst.add(MethodWithArguments.create(sm, null));
					}
				}
			}
			if (nodes != null)
			{
				for (AbstractBase form : nodes)
				{
					lst.add(form);
				}
			}
			return lst.toArray();
		}

		private List<ScriptMethod> getFormScriptMethods(Object parentElement)
		{
			List<ScriptMethod> methods = new ArrayList<>();
			Form form = (Form)parentElement;
			if (form.getScriptMethods(false).hasNext())
			{
				Iterator<ScriptMethod> it = currentFlattenedForm.getScriptMethods(true);
				while (it.hasNext())
				{
					ScriptMethod sm = it.next();
					if (sm.getParent().equals(form))
					{
						methods.add(form.getScriptMethod(sm.getName()));
					}
				}
			}
			return methods;
		}

		public Object getParent(Object element)
		{
			if (parents == null)
			{
				parents = new HashMap<Object, Object>();
				fillParentMap(new Object[] { element });
			}
			return parents.get(element);
		}

		public boolean hasChildren(Object element)
		{
			return FORM_METHODS == element || SCOPE_METHODS == element || element instanceof Solution || element instanceof ITable ||
				element instanceof ScopeWithContext || hasMethods(element);
		}

		private boolean hasMethods(Object element)
		{
			if (element instanceof Form)
			{
				Form form = (Form)element;
				if (form.getScriptMethods(false).hasNext())
				{
					Iterator<ScriptMethod> it = currentFlattenedForm.getScriptMethods(false);
					while (it.hasNext())
					{
						ScriptMethod sm = it.next();
						if (sm.getParent().equals(form))
						{
							return true;
						}
					}
				}
			}
			return false;
		}

		public boolean isKeyword(Object element)
		{
			return FORM_METHODS == element || SCOPE_METHODS == element;
		}
	}

	public void expandFormNode()
	{
		getTreeViewer().getViewer().expandToLevel(FORM_METHODS, 1);
	}

	public void expandScopesNode()
	{
		getTreeViewer().getViewer().expandToLevel(SCOPE_METHODS, 1);
	}

	public static class MethodListOptions
	{
		public final boolean includeNone;
		public final boolean includeDefault;
		public final boolean includeFormMethods;
		public final boolean includeScopeMethods;
		public final boolean includeFoundsetMethods;
		public final ITable table;

		public MethodListOptions(boolean includeNone, boolean includeDefault, boolean includeFormMethods, boolean includeScopeMethods,
			boolean includeFoundsetMethods, ITable table)
		{
			this.includeNone = includeNone;
			this.includeDefault = includeDefault;
			this.includeFormMethods = includeFormMethods;
			this.includeScopeMethods = includeScopeMethods;
			this.includeFoundsetMethods = includeFoundsetMethods;
			this.table = table;
		}
	}

	/**
	 * Label provider that adds text and images for the nodes defined by the dialog
	 *
	 * @author rgansevles
	 *
	 */
	public static class MethodDialogLabelProvider extends DelegateLabelProvider implements IFontProvider
	{
		public MethodDialogLabelProvider(ILabelProvider labelProvider)
		{
			super(labelProvider);
		}

		@Override
		public String getText(Object value)
		{
			String methodDialogText = getMethodDialogText(value);
			if (methodDialogText == null)
			{
				return super.getText(value);
			}
			return methodDialogText;
		}

		protected String getMethodDialogText(Object value)
		{
			if (FORM_METHODS == value) return "form methods";
			if (SCOPE_METHODS == value) return "scope methods";
			if (value instanceof ITable) return "entity methods";
			if (value instanceof ScopeWithContext) return ((ScopeWithContext)value).getName();
			if (value instanceof Solution) return ((Solution)value).getName();
			return null;
		}

		@Override
		public Font getFont(Object value)
		{
			if (FORM_METHODS == value || SCOPE_METHODS == value || value instanceof Solution || value instanceof ITable || value instanceof ScopeWithContext)
			{
				return FontResource.getDefaultFont(SWT.ITALIC, 1);
			}
			if (getLabelProvider() instanceof IFontProvider)
			{
				return ((IFontProvider)getLabelProvider()).getFont(value);
			}
			return null;
		}

		/**
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		@Override
		public Image getImage(Object element)
		{
			Image methodDialogImage = getMethodDialogImage(element);
			if (methodDialogImage == null)
			{
				return super.getImage(element);
			}
			return methodDialogImage;
		}

		protected Image getMethodDialogImage(Object value)
		{
			if (value instanceof Solution) return solutionImage;
			if (FORM_METHODS == value || value instanceof Form) return formMethodsImage;
			if (SCOPE_METHODS == value) return scopeMethodsImage;
			if (value instanceof ScopeWithContext) return scopeMethodsImage;
			if (value instanceof ITable) return foundsetMethodsImage;
			return null;
		}


		@Override
		public Object getDelegate()
		{
			// special implementation of getDelegate, add MethodDialogLabelProvider stuff to delegate label provider.
			Object delegate = super.getDelegate();
			while (delegate instanceof IDelegate)
			{
				delegate = ((IDelegate< ? >)delegate).getDelegate();
			}
			final ILabelProvider delegateLabelProvider = (ILabelProvider)delegate;
			return new DelegateLabelProvider(delegateLabelProvider)
			{
				@Override
				public String getText(Object element)
				{
					String methodDialogText = getMethodDialogText(element);
					if (methodDialogText == null)
					{
						return delegateLabelProvider.getText(element);
					}
					return methodDialogText;
				}

				@Override
				public Image getImage(Object element)
				{
					Image methodDialogImage = getMethodDialogImage(element);
					if (methodDialogImage == null)
					{
						return delegateLabelProvider.getImage(element);
					}
					return methodDialogImage;
				}
			};
		}
	}
}
