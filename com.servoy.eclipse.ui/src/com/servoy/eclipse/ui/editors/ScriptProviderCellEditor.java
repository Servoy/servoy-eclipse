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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.editors.ScriptProviderCellEditor.ScriptDialog.ScriptDialogLabelProvider;
import com.servoy.eclipse.ui.editors.ScriptProviderCellEditor.ScriptDialog.ScriptProviderValueEditor;
import com.servoy.eclipse.ui.labelproviders.IPersistLabelProvider;
import com.servoy.eclipse.ui.labelproviders.MethodLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.ScriptProviderPropertyController;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.IControlFactory;
import com.servoy.eclipse.ui.util.IKeywordChecker;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;

/**
 * A cell editor that manages a script field (calculation & global methods only)
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class ScriptProviderCellEditor extends DialogCellEditor
{
	private final ITable table;
	private final PersistContext persistContext;

	private final String methodKey;

	public ScriptProviderCellEditor(Composite parent, ITable table, PersistContext persistContext, String methodKey, boolean readOnly)
	{
		super(parent, new SolutionContextDelegateLabelProvider(new ScriptDialogLabelProvider(persistContext, table, true), persistContext.getContext()),
			new ScriptProviderValueEditor(persistContext.getPersist()), readOnly, SWT.NONE);
		this.table = table;
		this.persistContext = persistContext;
		this.methodKey = methodKey;
	}

	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{
		final ScriptDialog dialog = new ScriptDialog(cellEditorWindow.getShell(), persistContext, table, getSelection(), SWT.NONE, "Select script");
		dialog.setOptionsAreaFactory(new IControlFactory()
		{
			public Control createControl(Composite composite)
			{
				AddScriptProviderButtonsComposite buttons = new AddScriptProviderButtonsComposite(composite, methodKey, SWT.NONE);
				buttons.setTable(table);
				buttons.setPersist(persistContext.getPersist());
				buttons.setDialog(dialog);
				return buttons;
			}
		});

		dialog.open();

		if (dialog.getReturnCode() != Window.CANCEL)
		{
			return ((IStructuredSelection)dialog.getSelection()).getFirstElement(); // single select
		}

		return null;
	}

	public static class ScriptDialog extends TreeSelectDialog
	{
		private static final String CALCULATIONS = "calculations";
		private static final String GLOBAL_METHODS = "global methods";

		ScriptDialog(Shell shell, PersistContext persistContext, ITable table, ISelection selection, int treeStyle, String title)
		{
			super(shell, true, false, TreePatternFilter.FILTER_LEAFS,
				// content provider
				new ScriptTreeContentProvider(table, persistContext),
				// label provider
				new SolutionContextDelegateLabelProvider(new ScriptDialogLabelProvider(persistContext, table, false), persistContext.getContext()),
				// ViewerComparator
				null,
				// selection filter
				new LeafnodesSelectionFilter(new ScriptTreeContentProvider(table, persistContext)),
				// tree style
				treeStyle,
				// title
				title,
				// input
				new Object(),
				// selection
				selection, false, TreeSelectDialog.SCRIPT_DIALOG, null, false);
		}

		public void expandCalculationNode()
		{
			getTreeViewer().getViewer().expandToLevel(CALCULATIONS, 1);
		}

		public void expandGlobalsNode()
		{
			getTreeViewer().getViewer().expandToLevel(GLOBAL_METHODS, 1);
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

		public static class ScriptTreeContentProvider extends ArrayContentProvider implements ITreeContentProvider, IKeywordChecker
		{
			private final ITable table;
			private final PersistContext persistContext;

			public ScriptTreeContentProvider(ITable table, PersistContext persistContext)
			{
				this.table = table;
				this.persistContext = persistContext;
			}

			@Override
			public Object[] getElements(Object inputElement)
			{
				return new Object[] { ScriptProviderPropertyController.NONE, ScriptDialog.CALCULATIONS, ScriptDialog.GLOBAL_METHODS };
			}

			public Object[] getChildren(Object parentElement)
			{
				List<Object> children = new ArrayList<Object>();
				if (ScriptDialog.CALCULATIONS == parentElement && table != null)
				{
					Iterator<ScriptCalculation> calcs = ModelUtils.getEditingFlattenedSolution(persistContext.getContext()).getScriptCalculations(table, true);
					while (calcs.hasNext())
					{
						children.add(MethodWithArguments.create(calcs.next(), null));
					}
				}

				if (ScriptDialog.GLOBAL_METHODS == parentElement)
				{
					Iterator<ScriptMethod> scriptMethodsIte = ModelUtils.getEditingFlattenedSolution(persistContext.getContext()).getScriptMethods(true);
					while (scriptMethodsIte.hasNext())
					{
						children.add(MethodWithArguments.create(scriptMethodsIte.next(), null));
					}
				}

				return children.toArray();
			}

			public Object getParent(Object value)
			{
				if (value instanceof MethodWithArguments)
				{
					IScriptProvider scriptProvider = ModelUtils.getScriptMethod(persistContext.getPersist(), persistContext.getContext(),
						((MethodWithArguments)value).table, ((MethodWithArguments)value).methodUUID);
					if (scriptProvider instanceof ScriptCalculation)
					{
						return ScriptDialog.CALCULATIONS;
					}
					if (scriptProvider instanceof ScriptMethod)
					{
						return ScriptDialog.GLOBAL_METHODS;
					}
				}
				return null;
			}

			public boolean hasChildren(Object element)
			{
				return ScriptDialog.CALCULATIONS == element || ScriptDialog.GLOBAL_METHODS == element;
			}

			public boolean isKeyword(Object element)
			{
				return ScriptDialog.CALCULATIONS == element || ScriptDialog.GLOBAL_METHODS == element;
			}
		}

		public static class ScriptProviderValueEditor implements IValueEditor<Object>
		{
			private final IPersist persist;

			public ScriptProviderValueEditor(IPersist persist)
			{
				this.persist = persist;
			}

			public boolean canEdit(Object value)
			{
				return value instanceof MethodWithArguments &&
					ModelUtils.getScriptMethod(persist, null, ((MethodWithArguments)value).table, ((MethodWithArguments)value).methodUUID) != null;
			}

			public void openEditor(Object value)
			{
				IScriptProvider scriptprovider = ModelUtils.getScriptMethod(persist, null, ((MethodWithArguments)value).table,
					((MethodWithArguments)value).methodUUID);
				if (scriptprovider instanceof IDataProvider) // it is a calculation
				{
					EditorUtil.openDataProviderEditor((IDataProvider)scriptprovider);
				}
				EditorUtil.openScriptEditor(scriptprovider, null, true);
			}
		}


		public static class ScriptDialogLabelProvider implements IPersistLabelProvider, IFontProvider
		{
			private static final Image globalMethodsImage = Activator.getDefault().loadImageFromBundle("global_method.png");
			private final boolean showPrefix;
			private final PersistContext persistContext;
			private final ITable table;

			public ScriptDialogLabelProvider(PersistContext persistContext, ITable table, boolean showPrefix)
			{
				this.persistContext = persistContext;
				this.table = table;
				this.showPrefix = showPrefix;
			}

			public String getText(Object value)
			{
				if (value == null || ScriptProviderPropertyController.NONE == value) return Messages.LabelNone;
				if (value instanceof IScriptProvider)
				{
					if (showPrefix && value instanceof ScriptMethod)
					{
						return ((ScriptMethod)value).getPrefixedName();
					}

					return ((IScriptProvider)value).getDisplayName();
				}
				if (value instanceof MethodWithArguments)
				{
					return MethodLabelProvider.getMethodText((MethodWithArguments)value, persistContext, showPrefix, false, true);
				}

				return value.toString();
			}

			public Font getFont(Object value)
			{
				if (value == null || ScriptProviderPropertyController.NONE == value)
				{
					return FontResource.getDefaultFont(SWT.BOLD, -1);
				}

				if (ScriptDialog.CALCULATIONS == value || ScriptDialog.GLOBAL_METHODS == value)
				{
					return FontResource.getDefaultFont(SWT.ITALIC, 1);
				}

				return FontResource.getDefaultFont(SWT.NONE, 0);
			}

			/**
			 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
			 */
			public Image getImage(Object element)
			{
				if (ScriptDialog.GLOBAL_METHODS == element) return globalMethodsImage;
				return null;
			}

			public void addListener(ILabelProviderListener listener)
			{
			}

			public void dispose()
			{
			}

			public boolean isLabelProperty(Object element, String property)
			{
				return false;
			}

			public void removeListener(ILabelProviderListener listener)
			{
			}

			public IPersist getPersist(Object value)
			{
				if (value instanceof MethodWithArguments)
				{
					return ModelUtils.getScriptMethod(persistContext.getPersist(), persistContext.getContext(), ((MethodWithArguments)value).table,
						((MethodWithArguments)value).methodUUID);
				}
				return null;
			}
		}
	}
}
