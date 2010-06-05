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
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.labelproviders.DelegateLabelProvider;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.IKeywordChecker;
import com.servoy.eclipse.ui.views.IMaxDepthTreeContentProvider;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.Utils;

/**
 * A dialog for methods editor that manages a method field.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class MethodDialog extends TreeSelectDialog
{
	public static final MethodWithArguments METHOD_NONE = new MethodWithArguments(-1);
	public static final MethodWithArguments METHOD_DEFAULT = new MethodWithArguments(0);

	// used only in the dialog, are never selected
	public static final Object FORM_METHODS = new Object();
	public static final Object GLOBAL_METHODS = new Object();

	private static final Image solutionImage = Activator.getDefault().loadImageFromBundle("solution.gif");
	private static final Image formMethodsImage = Activator.getDefault().loadImageFromBundle("designer.gif");
	private static final Image globalMethodsImage = Activator.getDefault().loadImageFromBundle("global_method.gif");

	/**
	 * Creates a new method cell dialog parented under the given shell.
	 * 
	 * @param labelProvider
	 * 
	 * @param parent the parent control
	 */
	public MethodDialog(Shell shell, ILabelProvider labelProvider, ITreeContentProvider contentProvider, ISelection selection,
		Object /* MethodListOptions */input, int treeStyle, String title)
	{
		super(shell, true, true, TreePatternFilter.FILTER_LEAFS, IMaxDepthTreeContentProvider.DEPTH_DEFAULT,
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
			selection, TreeSelectDialog.METHOD_DIALOG);
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
		}
		return new StructuredSelection(lst.toArray(new MethodWithArguments[lst.size()]));
	}

	public static class MethodTreeContentProvider extends ArrayContentProvider implements ITreeContentProvider, IKeywordChecker
	{
		private final IPersist persist;
		private Map<Object, Object> parents;

		public MethodTreeContentProvider(IPersist persist)
		{
			this.persist = persist;
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
					lst.add(METHOD_NONE);
				}

				if (options.includeDefault)
				{
					lst.add(METHOD_DEFAULT);
				}

				if (options.includeFormMethods && options.includeGlobalMethods)
				{
					lst.add(FORM_METHODS);
					lst.add(GLOBAL_METHODS);
				}
				else
				{
					if (options.includeFormMethods)
					{
						lst.addAll(Arrays.asList(getChildren(FORM_METHODS)));
					}
					if (options.includeGlobalMethods)
					{
						lst.addAll(Arrays.asList(getChildren(GLOBAL_METHODS)));
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
			Iterator<ScriptMethod> scriptMethods = null;

			if (FORM_METHODS == parentElement)
			{
				Form form = (Form)persist.getAncestor(IRepository.FORMS);
				if (form != null)
				{
					try
					{
						scriptMethods = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(form).getFlattenedForm(form).getScriptMethods(
							true);
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
			}

			else if (GLOBAL_METHODS == parentElement)
			{
				Solution solution = (Solution)persist.getAncestor(IRepository.SOLUTIONS);
				Object[] children = getChildren(solution);
				Solution[] modules = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(solution).getModules();
				return Utils.arrayInsert(children, modules, children == null ? 0 : children.length, modules == null ? 0 : modules.length);
			}

			else if (parentElement instanceof Solution)
			{
				Solution solution = (Solution)parentElement;
				scriptMethods = solution.getScriptMethods(true);
			}

			if (scriptMethods != null)
			{
				List<MethodWithArguments> lst = new ArrayList<MethodWithArguments>();
				while (scriptMethods.hasNext())
				{
					MethodWithArguments mwa = new MethodWithArguments(scriptMethods.next().getID());
					lst.add(mwa);
				}
				return lst.toArray();
			}

			return null;
		}

		public Object getParent(Object element)
		{
			return parents.get(element);
		}

		public boolean hasChildren(Object element)
		{
			return FORM_METHODS == element || GLOBAL_METHODS == element || element instanceof Solution;
		}

		public boolean isKeyword(Object element)
		{
			return FORM_METHODS == element || GLOBAL_METHODS == element;
		}
	}

	public void expandFormNode()
	{
		getTreeViewer().getViewer().expandToLevel(FORM_METHODS, 1);
	}

	public void expandGlobalsNode()
	{
		getTreeViewer().getViewer().expandToLevel(GLOBAL_METHODS, 1);
	}

	public static class MethodListOptions
	{
		public final boolean includeNone;
		public final boolean includeDefault;
		public final boolean includeFormMethods;
		public final boolean includeGlobalMethods;

		public MethodListOptions(boolean includeNone, boolean includeDefault, boolean includeFormMethods, boolean includeGlobalMethods)
		{
			this.includeNone = includeNone;
			this.includeDefault = includeDefault;
			this.includeFormMethods = includeFormMethods;
			this.includeGlobalMethods = includeGlobalMethods;
		}
	}

	/**
	 * Label provider that adds text and images for the nodes defined by the dialog
	 * 
	 * @author rob
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
			if (GLOBAL_METHODS == value) return "global methods";
			if (value instanceof Solution) return ((Solution)value).getName();
			return null;
		}

		public Font getFont(Object value)
		{
			if (FORM_METHODS == value || GLOBAL_METHODS == value || value instanceof Solution)
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
			if (FORM_METHODS == value) return formMethodsImage;
			if (GLOBAL_METHODS == value) return globalMethodsImage;
			return null;
		}


		@Override
		public Object getDelegate()
		{
			// special implementation of getDelegate, add MethodDialogLabelProvider stuff to delegate label provider.
			Object delegate = super.getDelegate();
			while (delegate instanceof IDelegate)
			{
				delegate = ((IDelegate)delegate).getDelegate();
			}
			final ILabelProvider delegateLabelProvider = (ILabelProvider)delegate;
			return new LabelProvider()
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
