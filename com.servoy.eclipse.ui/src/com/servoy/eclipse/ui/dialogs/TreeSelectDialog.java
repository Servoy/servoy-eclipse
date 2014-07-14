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

import java.util.Iterator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.IControlFactory;

/**
 * The Tree Selection dialog.
 */
public class TreeSelectDialog extends Dialog implements ISelectionChangedListener, IOpenListener
{
	private final String title;
	private final String name;

	public static final String BEAN_DIALOG = "beanDialog";
	public static final String SCRIPT_DIALOG = "scriptDialog";
	public static final String METHOD_DIALOG = "methodDialog";
	public static final String DATAPROVIDER_DIALOG = "dataProviderDialog";
	public static final String MEDIA_DIALOG = "mediaDialog";
	public static final String TAB_DIALOG = "tabDialog";
	public static final String TEMPLATE_DIALOG = "templateDialog";

	private static final int LIST_HEIGHT = 300;

	private static final int LIST_WIDTH = 250;


	private String optionalMessage = null;

	private Button okButton;
	private Button openButton;


	private final Object input;

	private final IBaseLabelProvider labelProvider;

	private final ITreeContentProvider contentProvider;

	private final ViewerComparator comparator;

	private final IFilter selectionFilter;

	private final int treeStyle;

	private FilteredTreeViewer treeViewer;

	private final boolean showFilter;

	private final boolean allowEmptySelection;

	private final ISelection selection;

	private IControlFactory optionsAreaFactory;
	private final boolean showFilterMenu;
	private final int defaultFilterMode;

	private final IValueEditor valueEditor;

	/**
	 * Constructs a new TreeSelectDialog.
	 * 
	 * @param shell
	 * @param contentProvider
	 * @param labelProvider
	 * @param comparator
	 * @param treeStyle
	 * @param title
	 * @param input
	 */
	public TreeSelectDialog(Shell shell, boolean showFilter, boolean showFilterMenu, int defaultFilterMode, ITreeContentProvider contentProvider,
		IBaseLabelProvider labelProvider, ViewerComparator comparator, IFilter selectionFilter, int treeStyle, String title, Object input,
		ISelection selection, boolean allowEmptySelection, String name, IValueEditor valueEditor)
	{
		super(shell);
		this.showFilter = showFilter;
		this.showFilterMenu = showFilterMenu;
		this.defaultFilterMode = defaultFilterMode;
		this.selectionFilter = selectionFilter;
		this.treeStyle = treeStyle;
		this.title = title;
		this.contentProvider = contentProvider;
		this.labelProvider = labelProvider;
		this.comparator = comparator;
		this.input = input;
		this.name = name;
		this.valueEditor = valueEditor;
		this.allowEmptySelection = allowEmptySelection;
		this.selection = selection;
		this.optionalMessage = "";
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	/**
	 * Notifies that the cancel button of this dialog has been pressed.
	 */
	@Override
	protected void cancelPressed()
	{
		treeViewer.clearOrderedSelection();
		super.cancelPressed();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell shell)
	{
		super.configureShell(shell);
		shell.setText(title);
	}

	/**
	 * Adds buttons to this dialog's button bar.
	 * <p>
	 * The default implementation of this framework method adds standard ok and cancel buttons using the <code>createButton</code> framework method. Subclasses
	 * may override.
	 * </p>
	 * 
	 * @param parent the button bar composite
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		if (valueEditor != null) openButton = createButton(parent, IDialogConstants.OPEN_ID, "OK && Show", false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		updateButtons();
	}

	/**
	 * Creates and returns the contents of the upper part of this dialog (above the button bar).
	 * 
	 * @param parent the parent composite to contain the dialog area
	 * @return the dialog area control
	 */
	@Override
	protected Control createDialogArea(Composite parent)
	{
		// Run super.
		Composite composite = (Composite)super.createDialogArea(parent);
		composite.setFont(parent.getFont());

		treeViewer = createFilteredTreeViewer(composite);
		treeViewer.addSelectionChangedListener(this);
		treeViewer.addOpenListener(this);
		if (selection != null)
		{
			treeViewer.setSelection(selection);
		}

		applyDialogFont(treeViewer);

		createDialogMessage(composite);

		// options area
		Control optionsArea = null;
		if (optionsAreaFactory != null)
		{
			optionsArea = optionsAreaFactory.createControl(composite);
			optionsArea.pack();
		}

		if (optionsArea == null)
		{
			layoutTopControl(treeViewer);
		}
		else
		{
			applyDialogFont(optionsArea);

			final GroupLayout groupLayout = new GroupLayout(composite);
			groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
				GroupLayout.LEADING,
				groupLayout.createSequentialGroup().addContainerGap().add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING, treeViewer, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).add(GroupLayout.TRAILING, optionsArea, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)).addContainerGap()));
			groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
				GroupLayout.LEADING,
				groupLayout.createSequentialGroup().add(15, 15, 15).add(treeViewer, GroupLayout.PREFERRED_SIZE, 216, Short.MAX_VALUE).addPreferredGap(
					LayoutStyle.RELATED).add(optionsArea, GroupLayout.PREFERRED_SIZE, optionsArea.getBounds().height, GroupLayout.PREFERRED_SIZE)));
			composite.setLayout(groupLayout);
		}

		updateButtons();

		// Return results.
		return composite;
	}

	/**
	 * @param composite
	 */
	private void createDialogMessage(Composite composite)
	{
		if (optionalMessage != null)
		{
			org.eclipse.swt.widgets.Label label = new org.eclipse.swt.widgets.Label(composite, SWT.WRAP);
			label.setText(optionalMessage);
			GridData spec = new GridData(GridData.FILL_HORIZONTAL);
			spec.widthHint = LIST_WIDTH;
			label.setLayoutData(spec);
		}
	}

	/**
	 * Create a new filtered tree viewer in the parent.
	 * 
	 * @param parent the parent <code>Composite</code>.
	 */
	protected FilteredTreeViewer createFilteredTreeViewer(Composite parent)
	{
		FilteredTreeViewer ftv = new FilteredTreeViewer(parent, showFilter, showFilterMenu, contentProvider, labelProvider, comparator, treeStyle,
			new TreePatternFilter(TreePatternFilter.getSavedFilterMode(getDialogBoundsSettings(), defaultFilterMode)), selectionFilter);
		ftv.setInput(input);
		return ftv;
	}

	public void refreshTree()
	{
		treeViewer.setInput(input);
	}

	/**
	 * @return the treeViewer
	 */
	public FilteredTreeViewer getTreeViewer()
	{
		return treeViewer;
	}


	/**
	 * Add an additional area to the composite, it will be laid out below the tree viewer.
	 */
	public void setOptionsAreaFactory(IControlFactory optionsAreaFactory)
	{
		this.optionsAreaFactory = optionsAreaFactory;
	}

	/**
	 * Returns the descriptors for the selected views.
	 * 
	 * @return the descriptors for the selected views
	 */
	public ISelection getSelection()
	{
		return treeViewer.getOrderedSelection() == null ? StructuredSelection.EMPTY : new StructuredSelection(treeViewer.getOrderedSelection());
	}

	/**
	 * Layout the top control.
	 * 
	 * @param control the control.
	 */
	protected void layoutTopControl(Control control)
	{
		GridData spec = new GridData(GridData.FILL_BOTH);
		spec.widthHint = LIST_WIDTH;
		spec.heightHint = LIST_HEIGHT;
		control.setLayoutData(spec);
	}

	/**
	 * Notifies that the selection has changed.
	 * 
	 * @param event event object describing the change
	 */
	public void selectionChanged(SelectionChangedEvent event)
	{
		updateButtons();
	}

	/**
	 * Update the button enablement state.
	 */
	protected void updateButtons()
	{
		boolean enabled = false;

		// control the way we handle empty selections when we have multiselect enabled
		if ((treeStyle & SWT.MULTI) != 0) enabled = (isAllowEmptySelection() || !getSelection().isEmpty());
		else enabled = !getSelection().isEmpty();

		if (okButton != null)
		{
			okButton.setEnabled(enabled);
		}

		if (openButton != null)
		{
			boolean enabledState = false;
			if (!getSelection().isEmpty() && getSelection() instanceof IStructuredSelection)
			{
				IStructuredSelection selection = (IStructuredSelection)getSelection();
				Iterator it = selection.iterator();
				while (it.hasNext())
				{
					Object o = it.next();
					if (valueEditor.canEdit(o))
					{
						enabledState = true;
					}
				}
			}
			openButton.setEnabled(enabledState);
		}
	}


	public void open(OpenEvent event)
	{
		setReturnCode(OK);
		close();
	}

	@Override
	public boolean close()
	{
		((TreePatternFilter)getTreeViewer().getPatternFilter()).saveSettings(getDialogBoundsSettings());
		return super.close();
	}

	/**
	 * @param selection
	 */
	public void setSelection(Object selection)
	{
		treeViewer.setSelection(new StructuredSelection(selection));
	}

	@Override
	public IDialogSettings getDialogBoundsSettings()
	{
		return EditorUtil.getDialogSettings(name);
	}

	@Override
	protected void buttonPressed(int buttonId)
	{
		super.buttonPressed(buttonId);
		if (buttonId == IDialogConstants.OPEN_ID) openPressed();
	}

	protected void openPressed()
	{
		if (!getSelection().isEmpty() && getSelection() instanceof IStructuredSelection)
		{
			IStructuredSelection selection = (IStructuredSelection)getSelection();
			Iterator it = selection.iterator();
			while (it.hasNext())
			{
				final Object value = it.next();
				// open button is always present when we have a valueEditor so don't have to check for null
				if (value != null && valueEditor.canEdit(value))
				{
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							valueEditor.openEditor(value);
						}
					});
				}
			}
		}
		okPressed();
	}

	/**
	 * Returns whether an empty selection is allowed for this dialog.
	 * 
	 * @return the allowEmptySelection
	 */
	public boolean isAllowEmptySelection()
	{
		return allowEmptySelection;
	}

	public void setOptionalMessage(String message)
	{
		this.optionalMessage = message;
	}

}
