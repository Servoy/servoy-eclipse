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
package com.servoy.eclipse.ui.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout.ParallelGroup;
import org.eclipse.swt.layout.grouplayout.GroupLayout.SequentialGroup;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.editors.DialogCellEditor;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.util.IStatusChangedListener;
import com.servoy.eclipse.ui.util.IStatusProvider;
import com.servoy.j2db.util.Utils;

/**
 * Viewer to edit a value in a tree select dialog. The value is displayed in a textfield, a button is shown to edit the text.
 *
 * <p>The text field can be made editable by calling setEditable(), the a content proposal will show options made available by the (tree) content provider
 * and formatted by the (text) label provider.
 * <br> When the foeld is editable, the user may enter an invalid value, in that case the foreground color is set to red and the isValid() method returns false (editors and
 * dialog should check this).
 *
 * <p>When a IValueEditor is passed in the constructor, an open-button is added to the viewer.
 *
 * @author rgansevles
 */
public class TreeSelectViewer extends StructuredViewer implements IStatusProvider
{
	public static final String DEFAULT_TITLE = "Select";
	public static final String DEFAULT_BUTTON_TEXT = "...";
	public static final String CONTENT_LOADING_JOB_FAMILY = "svyContentLoadingJobFamily";

	private IStructuredSelection selection;

	private final ListenerList stateChangeListeners = new ListenerList();

	private Composite composite;
	protected Text text;
	protected Button button;
	protected Button openButton;
	protected String title = DEFAULT_TITLE;
	protected String name;
	private ILabelProvider textLabelProvider; // different label provider for text and dialog
	private boolean showFilter = true;
	private boolean showFilterMode = false;
	private int defaultFilterMode = TreePatternFilter.FILTER_LEAFS;
	private IFilter selectionFilter;
	private TreeSelectContentProposalAdapter contentProposal;
	private boolean valid = true;
	private IValueEditor valueEditor;
	private ModifyListener textModifyListener;
	private Color textForeground;
	private int minTextWidth = 200;

	private boolean skipNextFocusGained = false;

	private boolean selectTextOnEnter = true;

	public boolean isSelectTextOnEnter()
	{
		return selectTextOnEnter;
	}

	public void setSelectTextOnEnter(boolean selectText)
	{
		this.selectTextOnEnter = selectText;
	}


	public TreeSelectViewer(Composite parent, int style)
	{
		init(parent, style, null);
	}

	public TreeSelectViewer(Composite parent, int style, IValueEditor valueEditor)
	{
		init(parent, style, valueEditor);
	}

	public TreeSelectViewer(Composite parent, int style, int minTextWidth)
	{
		this.minTextWidth = minTextWidth;
		init(parent, style, null);
	}

	protected void init(Composite parent, int style, IValueEditor valueEditor)
	{
		this.valueEditor = valueEditor;
		composite = new Composite(parent, style);

		text = createTextField(composite); // may return null if no text field is wanted

		setSelectTextOnEnter(true); // may have to override this setting in places we don't want autoselection of text


		button = createButton(composite);
		button.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				editValue(composite);
			}
		});

		// layout
		button.setText(DEFAULT_BUTTON_TEXT);


		if (valueEditor != null)
		{
			openButton = createOpenButton(composite);
			openButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(final SelectionEvent e)
				{
					openValue();
				}
			});
		}

		if (text == null)
		{
			// just a button
			composite.setLayout(new FillLayout());
		}
		else
		{
			text.addFocusListener(new FocusAdapter()
			{
				@Override
				public void focusLost(FocusEvent e)
				{
					skipNextFocusGained = contentProposal != null && contentProposal.isOpen();
				}

				@Override
				public void focusGained(FocusEvent e)
				{
					if (isSelectTextOnEnter())
					{
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								text.selectAll();
							}
						});
					}

					if (getEditable())
					{
						if (skipNextFocusGained)
						{
							skipNextFocusGained = false;
						}
						else if (contentProposal != null)
						{
							// automatically popup the proposals when field is empty
							String contents = text.getText();
							if (contents.trim().length() == 0)
							{
								contentProposal.openProposalPopup();
							}
						}
					}
					else if (button.isEnabled())
					{
						// redirect focus to the button
						button.setFocus();
					}
				}
			});

			GroupLayout groupLayout = new GroupLayout(composite);
			SequentialGroup sequentialGroup = groupLayout.createSequentialGroup();
			sequentialGroup.add(text, GroupLayout.PREFERRED_SIZE, minTextWidth, Integer.MAX_VALUE);
			sequentialGroup.addPreferredGap(LayoutStyle.RELATED).add(button);
			if (openButton != null)
			{
				sequentialGroup.addPreferredGap(LayoutStyle.RELATED).add(openButton);
			}
			groupLayout.setHorizontalGroup(sequentialGroup);

			ParallelGroup parallelGroup = groupLayout.createParallelGroup(GroupLayout.CENTER, false);
			parallelGroup.add(button, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE);
			if (openButton != null)
			{
				parallelGroup.add(openButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE);
			}
			parallelGroup.add(text, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
			groupLayout.setVerticalGroup(parallelGroup);

			composite.setLayout(groupLayout);
		}

		composite.pack();
		if (text != null)
		{
			// get fg color after css is applied
			textForeground = text.getForeground();
		}
		setEnabled((style & SWT.READ_ONLY) == 0);
	}

	protected Text createTextField(Composite parent)
	{
		Text txt = new Text(parent, SWT.BORDER);
		txt.setEditable(false);
		return txt;
	}

	protected Button createOpenButton(Composite parent)
	{
		Button b = new Button(parent, SWT.NONE);
		b.setImage(DialogCellEditor.OPEN_IMAGE);
		return b;
	}

	protected Button createButton(Composite parent)
	{
		return new Button(parent, SWT.NONE);
	}

	/**
	 * Causes the receiver to have the <em>keyboard focus</em>, such that all keyboard events will be delivered to it. Focus reassignment will respect
	 * applicable platform constraints.
	 *
	 * @return <code>true</code> if the control got focus, and <code>false</code> if it was unable to.
	 *
	 * @exception SWTException <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 *                </ul>
	 *
	 * @see #forceFocus
	 */
	public boolean setFocus()
	{
		if ((composite.getStyle() & SWT.NO_FOCUS) != 0) return false;
		return forceFocus();
	}

	/**
	 * Forces the receiver to have the <em>keyboard focus</em>, causing all keyboard events to be delivered to it.
	 *
	 * @return <code>true</code> if the control got focus, and <code>false</code> if it was unable to.
	 *
	 * @exception SWTException <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 *                </ul>
	 *
	 * @see #setFocus
	 */
	public boolean forceFocus()
	{
		return (getEditable() ? text : button).forceFocus();
	}

	public Object getValue()
	{
		return selection == null || selection.isEmpty() ? null : selection.getFirstElement();
	}

	/**
	 * Open the dialog to edit the value
	 */
	public void editValue(Control control)
	{
		IStructuredSelection newSelection = openDialogBox(control);
		if (newSelection != null)
		{
			selection = newSelection;
			internalRefresh(null);
			fireSelectionChanged(new SelectionChangedEvent(TreeSelectViewer.this, getSelection()));
			setValid(getValue() == null || getSelectionFilter().select(getValue()));
		}
	}

	public boolean canOpen()
	{
		Object value = getValue();
		return value != null && isValid() && valueEditor != null && valueEditor.canEdit(value);
	}

	public void openValue()
	{
		if (canOpen())
		{
			valueEditor.openEditor(getValue());
		}
	}

	public boolean isShowFilter()
	{
		return showFilter;
	}

	public void setShowFilter(boolean showFilter)
	{
		this.showFilter = showFilter;
	}

	public boolean isShowFilterMode()
	{
		return showFilterMode;
	}

	public void setShowFilterMode(boolean showFilterMode)
	{
		this.showFilterMode = showFilterMode;
	}

	public int getDefaultFilterMode()
	{
		return defaultFilterMode;
	}

	public void setDefaultFilterMode(int defaultFilterMode)
	{
		this.defaultFilterMode = defaultFilterMode;
	}

	public IFilter getSelectionFilter()
	{
		if (selectionFilter == null)
		{
			// by default filter out parent nodes
			selectionFilter = new LeafnodesSelectionFilter(getContentProvider());
		}
		return selectionFilter;
	}

	public ViewerComparator getViewerComparator()
	{
		return null;
	}

	protected TreeSelectDialog createDialog(Control control)
	{
		return new TreeSelectDialog(control.getShell(), isShowFilter(), isShowFilterMode(), getDefaultFilterMode(), getContentProvider(), getLabelProvider(),
			getViewerComparator(), getSelectionFilter(), SWT.SINGLE, title, getInput(), getSelection(), false, name, null, false);
	}

	protected IStructuredSelection openDialogBox(Control control)
	{
		// show tree select dialog
		TreeSelectDialog dialog = createDialog(control);
		dialog.open();

		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return null;
		}
		return (IStructuredSelection)dialog.getSelection();
	}

	@Override
	public void setContentProvider(IContentProvider provider)
	{
		Assert.isTrue(provider instanceof ITreeContentProvider);
		super.setContentProvider(provider);
		createContentProposalAdapter();
	}

	@Override
	public ITreeContentProvider getContentProvider()
	{
		return (ITreeContentProvider)super.getContentProvider();
	}

	protected void createContentProposalAdapter()
	{
		if (getEditable() && getContentProvider() != null)
		{
			if (contentProposal == null)
			{
				contentProposal = new TreeSelectContentProposalAdapter(text);
			}
			contentProposal.setContentProposalProvider(new ContentLabelProviderProposalProvider(getInput(), getContentProvider(), getTextLabelProvider()));
		}
	}

	/**
	 * User changed text (this method is not called when selection is set)
	 */
	protected void textChanged()
	{
		if (contentProposal != null)
		{
			String contents = text.getText();
			if (contents.trim().length() == 0)
			{
				// user made field empty, show proposals
				contentProposal.openProposalPopup();
			}
		}
	}

	protected Object determineValue(String contents)
	{
		ContentLabelProviderProposalProvider providerProposalProvider = (ContentLabelProviderProposalProvider)contentProposal.getContentProposalProvider();
		Object value = providerProposalProvider.determineValue(contents);
		return value;
	}

	public boolean isValid()
	{
		return valid;
	}

	public void setValid(boolean valid)
	{
		if (this.valid != valid)
		{
			this.valid = valid;
			if (text != null)
			{
				text.setForeground(valid ? textForeground : text.getDisplay().getSystemColor(SWT.COLOR_RED));
			}
			if (openButton != null)
			{
				openButton.setEnabled(valid && canOpen());
			}

			fireStateChange(valid);
		}
	}

	public void addStatusChangedListener(IStatusChangedListener listener)
	{
		stateChangeListeners.add(listener);
	}

	public void removeStatusChangedListener(IStatusChangedListener listener)
	{
		stateChangeListeners.remove(listener);
	}

	protected void fireStateChange(final boolean newValid)
	{
		getControl().getDisplay().asyncExec(new Runnable()
		{
			public void run()
			{
				Object[] listeners = stateChangeListeners.getListeners();
				for (Object listener : listeners)
				{
					final IStatusChangedListener l = (IStatusChangedListener)listener;
					SafeRunnable.run(new SafeRunnable()
					{
						public void run()
						{
							if (!text.isDisposed())
							{
								l.statusChanged(newValid);
							}
						}
					});
				}
			}
		});
	}

	@Override
	protected void inputChanged(Object input, Object oldInput)
	{
		super.inputChanged(input, oldInput);
		createContentProposalAdapter();
	}

	public void setSelectionFilter(IFilter filter)
	{
		this.selectionFilter = filter;
	}

	/**
	 * Set the label provider for the tree
	 */
	@Override
	public void setLabelProvider(IBaseLabelProvider labelProvider)
	{
		Assert.isTrue(labelProvider instanceof ILabelProvider);
		super.setLabelProvider(labelProvider);
		createContentProposalAdapter();
	}

	/**
	 * Set the label provider for the text field (defaults to getLabelProvider())
	 */
	public void setTextLabelProvider(ILabelProvider labelProvider)
	{
		this.textLabelProvider = labelProvider;
		createContentProposalAdapter();
	}

	public ILabelProvider getTextLabelProvider()
	{
		if (textLabelProvider == null)
		{
			return (ILabelProvider)getLabelProvider();
		}
		return textLabelProvider;
	}

	public void setEditable(boolean editable)
	{
		if (text != null)
		{
			text.setEditable(editable);
			if (editable && textModifyListener == null)
			{
				textModifyListener = new ModifyListener()
				{
					public void modifyText(ModifyEvent e)
					{
						textChanged();
						String contents = text.getText();
						if (!Utils.stringIsEmpty(contents))
						{
							Object value = determineValue(contents);
							setValid(value != null && getSelectionFilter().select(value));
							if (value != null)
							{
								setSelection(new StructuredSelection(value));
							}
						}
					}
				};
				text.addModifyListener(textModifyListener);
				createContentProposalAdapter();
			}
		}
	}

	public boolean getEditable()
	{
		return text != null && text.getEditable();
	}

	@Override
	public Control getControl()
	{
		return composite;
	}

	/**
	 * Set button label
	 * @param s
	 */

	public void setButtonText(String s)
	{
		if (s == null || s.length() == 0) button.setText("   ");
		else button.setText(s);
		int noLoops = 10; //at least 7 loops are necessary; for the ValueListEditor layout to be displayed correctly
		while (composite.getShell().isLayoutDeferred() && noLoops > 0)
		{
			composite.getShell().setLayoutDeferred(false);
			noLoops--;
		}
		composite.layout();
	}

	public void setTitleText(String s)
	{
		title = s;
	}

	public void setName(String s)
	{
		name = s;
	}

	@Override
	protected Widget doFindInputItem(Object element)
	{
		return null;
	}

	@Override
	protected Widget doFindItem(Object element)
	{
		return null;
	}

	@Override
	protected void doUpdateItem(Widget item, Object element, boolean fullMap)
	{
	}

	@Override
	protected List getSelectionFromWidget()
	{
		if (selection == null || selection.isEmpty()) return Collections.EMPTY_LIST;
		return Arrays.asList(selection.toArray());
	}

	@Override
	protected void internalRefresh(Object element)
	{
		Object value = getValue();
		if (text != null)
		{
			ILabelProvider lp = getTextLabelProvider();
			text.setText(lp.getText(value));
			if (lp instanceof IFontProvider)
			{
				text.setFont(((IFontProvider)lp).getFont(value));
			}
		}
	}

	@Override
	public void reveal(Object element)
	{
	}

	@Override
	public void setSelection(ISelection selection)
	{
		if (text != null && textModifyListener != null)
		{
			text.removeModifyListener(textModifyListener);
		}
		boolean cpEnabled = false;
		if (contentProposal != null)
		{
			cpEnabled = contentProposal.isEnabled();
			contentProposal.setEnabled(false);
		}

		super.setSelection(selection);

		if (cpEnabled && contentProposal != null)
		{
			contentProposal.setEnabled(true);
		}
		if (openButton != null)
		{
			openButton.setEnabled(canOpen());
		}
		if (text != null && textModifyListener != null)
		{
			text.addModifyListener(textModifyListener);
		}
		if (text != null) text.setSelection(text.getText().length(), text.getText().length());
		Object value = getValue();
		boolean isValid = !getEditable() || getSelectionFilter().select(value);
		// if valid and value is not null, check if the value can be resolved through the content provider.
		if (isValid && value != null)
		{
			ITreeContentProvider contentProvider = getContentProvider();
			if (contentProvider != null) // can be null for instances that have their own dialog
			{
				isValid = false;
				Object parent = contentProvider.getParent(value);
				List<Object> path = new ArrayList<Object>();
				path.add(value);
				while (parent != null)
				{
					path.add(parent);
					parent = contentProvider.getParent(parent);
				}
				waitForJobsToFinish();
				Object[] elements = contentProvider.getElements(getInput());
				for (int i = path.size(); --i >= 0;)
				{
					Object x = path.get(i);
					for (Object object : elements)
					{
						if (x.equals(object))
						{
							if (i == 0)
							{
								isValid = true;
							}
							else
							{
								elements = contentProvider.getChildren(x);
							}
							break;
						}
					}
				}
			}
		}
		setValid(isValid);
	}

	private void waitForJobsToFinish()
	{
		IJobManager jobManager = Job.getJobManager();
		try
		{
			//wait for content jobs to finish if it's the case
			jobManager.join(CONTENT_LOADING_JOB_FAMILY, null);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	protected void setSelectionToWidget(List l, boolean reveal)
	{
		if (l.size() == 0)
		{
			selection = StructuredSelection.EMPTY;
		}
		else
		{
			selection = new StructuredSelection(l.toArray());
		}
		internalRefresh(null);
	}

	public void setEnabled(boolean b)
	{
		if (text != null)
		{
			text.setEnabled(b);
		}
		if (openButton != null)
		{
			openButton.setEnabled(b && canOpen());
		}
		button.setEnabled(b);
	}


	@Override
	protected void handleDispose(DisposeEvent event)
	{
		super.handleDispose(event);
		stateChangeListeners.clear();
	}

	public class TreeSelectContentProposalAdapter extends ContentProposalAdapter implements IContentProposalListener2
	{
		private boolean open;

		private TreeSelectContentProposalAdapter(Control control)
		{
			super(control, new TextContentAdapter()
			{
				@Override
				public void setControlContents(Control cntrl, String text, int cursorPosition)
				{
					super.setControlContents(cntrl, text, cursorPosition);
					setValid(true);
				}
			}, null, null, null);
			setPropagateKeys(true);
			setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
			addContentProposalListener(this);
		}

		@Override
		public void openProposalPopup()
		{
			getControl().getDisplay().asyncExec(new Runnable()
			{
				public void run()
				{
					TreeSelectContentProposalAdapter.super.openProposalPopup();
				}
			});
		}

		public void proposalPopupOpened(ContentProposalAdapter adapter)
		{
			open = true;
		}

		public void proposalPopupClosed(ContentProposalAdapter adapter)
		{
			if (getEditable() && !text.isDisposed() && !text.isFocusControl() && text.getText().length() == 0)
			{
				// user made field empty
				setSelection(StructuredSelection.EMPTY);
			}
			// when the user clicks on the dialog, the text field gets focus, when the field is empty the popup should not keep reappearing
			skipNextFocusGained = true;
			open = false;
		}


		public boolean isOpen()
		{
			return open;
		}
	}
}
