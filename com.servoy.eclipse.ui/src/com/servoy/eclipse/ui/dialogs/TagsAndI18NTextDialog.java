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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderNodeWrapper;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.dialogs.TagsAndI18NTextDialog.StandardTagsContentProvider.StandardTagsLeafNode;
import com.servoy.eclipse.ui.dialogs.TagsAndI18NTextDialog.StandardTagsContentProvider.StandardTagsRelationNode;
import com.servoy.eclipse.ui.labelproviders.CombinedLabelProvider;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationList;
import com.servoy.j2db.persistence.Table;

public class TagsAndI18NTextDialog extends Dialog
{
	private Text text;
	private I18nComposite i18nComposite;
	private final Table table;
	private DataProviderTreeViewer dpTree;
	private final String title;
	private final IApplication application;
	private String value;
	private Button addButton;

	private static final String STANDARD_TAGS_LABEL = "Standard tags";
	public static final String[] STANDARD_TAGS = new String[] { "selectedIndex", "maxRecordIndex", "lazyMaxRecordIndex", "serverURL", "currentRecordIndex", "pageNumber", "totalNumberOfPages", "i18n:<messagekey>" };
	public static final String[] STANDARD_TAGS_ON_RELATION = new String[] { "maxRecordIndex", "lazyMaxRecordIndex" };

	private final FlattenedSolution flattenedSolution;
	private final IPersist persist;

	public TagsAndI18NTextDialog(Shell shell, IPersist persist, FlattenedSolution flattenedSolution, Table table, Object value, String title,
		IApplication application)
	{
		super(shell);
		this.persist = persist;
		this.flattenedSolution = flattenedSolution;
		this.table = table;
		this.application = application;
		this.value = value == null ? "" : value.toString();
		this.title = title;
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText(title);

		Composite composite = (Composite)super.createDialogArea(parent);
		composite.setLayout(new FillLayout());

		final SashForm sashForm = new SashForm(composite, SWT.BORDER);

		final Composite composite_1 = new Composite(sashForm, SWT.NONE);
		dpTree = new DataProviderTreeViewer(composite_1, new CombinedLabelProvider(StandardTagsLabelProvider.INSTANCE_HIDEPREFIX,
			new SolutionContextDelegateLabelProvider(new FormContextDelegateLabelProvider(DataProviderLabelProvider.INSTANCE_HIDEPREFIX, persist), persist)),
			new CombinedTreeContentProvider(new DataProviderContentProvider(persist, flattenedSolution, table), StandardTagsContentProvider.INSTANCE),
			new DataProviderTreeViewer.DataProviderOptions(false, true, true, true, true, true, true, true, INCLUDE_RELATIONS.NESTED, true, true, null), true,
			true, TreePatternFilter.getSavedFilterMode(getDialogBoundsSettings(), TreePatternFilter.FILTER_PARENTS), SWT.MULTI);

		addButton = new Button(composite_1, SWT.NONE);
		addButton.setText(">>");
		final GroupLayout groupLayout = new GroupLayout(composite_1);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
			groupLayout.createSequentialGroup().add(10, 10, 10).add(dpTree, GroupLayout.PREFERRED_SIZE, 232, Short.MAX_VALUE).addPreferredGap(
				LayoutStyle.RELATED).add(addButton, GroupLayout.PREFERRED_SIZE, 56, GroupLayout.PREFERRED_SIZE).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().add(130, 130, 130).add(addButton)).add(
					groupLayout.createSequentialGroup().add(10, 10, 10).add(dpTree, GroupLayout.PREFERRED_SIZE, 426, Short.MAX_VALUE))).add(24, 24, 24)));
		composite_1.setLayout(groupLayout);

		final Composite composite_2 = new Composite(sashForm, SWT.NONE);

		TabFolder tabFolder;
		tabFolder = new TabFolder(composite_2, SWT.NONE);

		final TabItem textTabItem = new TabItem(tabFolder, SWT.NONE);
		textTabItem.setText("Text");

		final TabItem i18nTabItem = new TabItem(tabFolder, SWT.NONE);
		i18nTabItem.setText("I18n");

		final Composite textComposite = new Composite(tabFolder, SWT.NONE);
		textTabItem.setControl(textComposite);

		text = new Text(textComposite, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		final GroupLayout textLayout = new GroupLayout(textComposite);
		textLayout.setHorizontalGroup(textLayout.createParallelGroup(GroupLayout.LEADING).add(
			textLayout.createSequentialGroup().add(10, 10, 10).add(text, GroupLayout.PREFERRED_SIZE, 418, Short.MAX_VALUE).add(10, 10, 10)));
		textLayout.setVerticalGroup(textLayout.createParallelGroup(GroupLayout.LEADING).add(
			textLayout.createSequentialGroup().add(10, 10, 10).add(text, GroupLayout.PREFERRED_SIZE, 379, Short.MAX_VALUE).add(10, 10, 10)));
		textComposite.setLayout(textLayout);

		i18nComposite = new I18nComposite(tabFolder, SWT.NONE, application, false);
		i18nTabItem.setControl(i18nComposite);

		i18nComposite.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				handleI18nSelectionChanged(event);
			}
		});

		final GroupLayout groupLayout_1 = new GroupLayout(composite_2);
		groupLayout_1.setHorizontalGroup(groupLayout_1.createParallelGroup(GroupLayout.TRAILING).add(
			groupLayout_1.createSequentialGroup().add(tabFolder, GroupLayout.PREFERRED_SIZE, 389, Short.MAX_VALUE).addContainerGap()));
		groupLayout_1.setVerticalGroup(groupLayout_1.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout_1.createSequentialGroup().addContainerGap().add(tabFolder, GroupLayout.PREFERRED_SIZE, 422, Short.MAX_VALUE).addContainerGap()));
		composite_2.setLayout(groupLayout_1);
		sashForm.setWeights(new int[] { 298, 379 });

		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		// create OK and Cancel buttons, no default button
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Control createContents(Composite parent)
	{
		Control control = super.createContents(parent);

		text.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				textModified();
			}
		});
		text.setText(value);
		text.setFocus();

		dpTree.addOpenListener(new IOpenListener()
		{
			public void open(OpenEvent event)
			{
				handleAdd();
			}

		});

		addButton.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent e)
			{
				handleAdd();
			}

			public void widgetDefaultSelected(SelectionEvent e)
			{
			}
		});
		return control;
	}

	protected void textModified()
	{
		value = text.getText();
		i18nComposite.selectKey(value);
	}

	protected void handleAdd()
	{
		IStructuredSelection selection = (IStructuredSelection)dpTree.getSelection();
		StringBuffer sb = new StringBuffer();
		for (Object sel : selection.toArray())
		{
			String txt = null;
			if (sel instanceof IDataProvider)
			{
				txt = DataProviderLabelProvider.INSTANCE_SHOWPREFIX.getText(sel);
			}
			else if (sel instanceof StandardTagsLeafNode)
			{
				txt = StandardTagsLabelProvider.INSTANCE_SHOWPREFIX.getText(sel);
			}
			if (txt != null && txt.length() > 0)
			{
				sb.append("%%");
				sb.append(txt);
				sb.append("%%");
			}
		}
		Point selPoint = text.getSelection();
		String txt = text.getText();
		String newValue = txt.substring(0, selPoint.x) + (sb.toString()) + txt.substring(selPoint.y);
		text.setText(newValue);
		text.setSelection(selPoint.x + sb.length());
	}

	protected void handleI18nSelectionChanged(SelectionChangedEvent event)
	{
		String key = i18nComposite.getSelectedKey();
		if (key != null && !text.getText().equals(key))
		{
			text.setText(key);
		}
	}

	public Object getValue()
	{
		return "".equals(value) ? null : value;
	}

	@Override
	public boolean close()
	{
		((TreePatternFilter)dpTree.getPatternFilter()).saveSettings(getDialogBoundsSettings());
		i18nComposite.saveTableColumnWidths();
		return super.close();
	}


	public static class StandardTagsContentProvider extends ArrayContentProvider implements ITreeContentProvider, com.servoy.eclipse.ui.util.IKeywordChecker
	{
		public static final StandardTagsContentProvider INSTANCE = new StandardTagsContentProvider();

		public Object[] getChildren(Object parentElement)
		{
			if (parentElement instanceof StandardTagsRelationNode)
			{
				Relation relation = ((StandardTagsRelationNode)parentElement).relation;
				String[] standardTags = relation != null ? STANDARD_TAGS_ON_RELATION : STANDARD_TAGS;
				Object[] tags = new Object[standardTags.length];
				for (int i = 0; i < standardTags.length; i++)
				{
					tags[i] = new StandardTagsLeafNode(relation, standardTags[i]);
				}
				return tags;
			}
			if (parentElement instanceof Relation)
			{
				return new Object[] { new StandardTagsRelationNode((Relation)parentElement) };
			}
			if (parentElement instanceof DataProviderNodeWrapper)
			{
				RelationList relations = ((DataProviderNodeWrapper)parentElement).relations;
				if (relations != null)
				{
					return new Object[] { new StandardTagsRelationNode(relations.getRelation(0)) };
				}
			}
			return new Object[0];
		}

		public Object getParent(Object element)
		{
			if (element instanceof StandardTagsRelationNode)
			{
				return ((StandardTagsRelationNode)element).relation;
			}
			if (element instanceof StandardTagsLeafNode)
			{
				return new StandardTagsRelationNode(((StandardTagsLeafNode)element).relation);
			}
			return null;
		}

		public boolean hasChildren(Object element)
		{
			return element instanceof StandardTagsRelationNode;
		}

		@Override
		public Object[] getElements(Object inputElement)
		{
			return new Object[] { new StandardTagsRelationNode(null) };
		}

		public boolean isKeyword(Object element)
		{
			return element instanceof StandardTagsRelationNode;
		}

		public static class StandardTagsRelationNode
		{
			public final Relation relation;

			public StandardTagsRelationNode(Relation relation)
			{
				this.relation = relation;
			}

			@Override
			public int hashCode()
			{
				final int prime = 31;
				int result = 1;
				result = prime * result + ((relation == null) ? 0 : relation.hashCode());
				return result;
			}

			@Override
			public boolean equals(Object obj)
			{
				if (this == obj) return true;
				if (obj == null) return false;
				if (getClass() != obj.getClass()) return false;
				final StandardTagsRelationNode other = (StandardTagsRelationNode)obj;
				if (relation == null)
				{
					if (other.relation != null) return false;
				}
				else if (!relation.equals(other.relation)) return false;
				return true;
			}

		}

		public static class StandardTagsLeafNode
		{
			public final String tag;
			public final Relation relation;

			public StandardTagsLeafNode(Relation relation, String tag)
			{
				this.relation = relation;
				this.tag = tag;
			}
		}
	}

	public static class StandardTagsLabelProvider extends LabelProvider implements IFontProvider
	{
		public static final StandardTagsLabelProvider INSTANCE_HIDEPREFIX = new StandardTagsLabelProvider(true);
		public static final StandardTagsLabelProvider INSTANCE_SHOWPREFIX = new StandardTagsLabelProvider(false);
		private final boolean hidePrefix;

		public StandardTagsLabelProvider(boolean hidePrefix)
		{
			this.hidePrefix = hidePrefix;
		}

		@Override
		public String getText(Object element)
		{
			if (element instanceof StandardTagsLeafNode)
			{
				StandardTagsLeafNode node = (StandardTagsLeafNode)element;
				if (hidePrefix || node.relation == null)
				{
					return node.tag;
				}
				return node.relation.getName() + '.' + node.tag;
			}

			if (element instanceof StandardTagsRelationNode)
			{
				return STANDARD_TAGS_LABEL;
			}

			return "";
		}

		public Font getFont(Object value)
		{
			if (value instanceof StandardTagsRelationNode)
			{
				return FontResource.getDefaultFont(SWT.ITALIC, 0);
			}
			return null;
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings()
	{
		return EditorUtil.getDialogSettings("tagsAndI18NTextDialog");
	}
}
