/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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
package com.servoy.eclipse.ui.property;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout.ParallelGroup;
import org.eclipse.swt.layout.grouplayout.GroupLayout.SequentialGroup;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.editors.TextDialogCellEditor;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;

/**
 * Property controller for properties that are a space-separated string with content proposal for entries in the string.
 *
 * @author rgansevles
 */

public class StringListWithContentProposalsPropertyController extends PropertyController<String, Object>
{
	private static final StringTokenizerListConverter STRING_TO_LIST_CONVERTER = new StringTokenizerListConverter(" ", true);

	private static final LabelProvider STRING_TO_LIST_LABEL_PPROVIDER = new LabelProvider()
	{
		@Override
		public String getText(Object list)
		{
			return list == null ? Messages.LabelDefault : STRING_TO_LIST_CONVERTER.convertValue(null, (List<String>)list);
		}
	};

	private final String[] proposals;
	private final String tooltip;

	public StringListWithContentProposalsPropertyController(String id, String displayName, String[] proposals, String tooltip)
	{
		super(id, displayName);
		this.proposals = proposals;
		this.tooltip = tooltip;
	}

	@Override
	protected IPropertyConverter<String, Object> createConverter()
	{
		IPropertyConverter<List<String>, Object> complexConverter = new ComplexPropertyConverter<List<String>>()
		{
			@Override
			public Object convertProperty(Object id, List<String> value)
			{
				return new ComplexProperty<List<String>>(value)
				{
					@Override
					public IPropertySource getPropertySource()
					{
						ListPropertySource propertySource = new ListPropertySource(this);
						propertySource.setReadonly(StringListWithContentProposalsPropertyController.this.isReadOnly());
						return propertySource;
					}
				};
			}
		};
		return new ChainedPropertyConverter<String, List<String>, Object>(STRING_TO_LIST_CONVERTER, complexConverter);
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		return STRING_TO_LIST_LABEL_PPROVIDER;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return new AddEntriesCellEditor(parent, proposals, tooltip);
	}

	protected static class ListPropertySource extends ComplexPropertySource<List<String>>
	{
		private static final String REMOVE_VALUE = "<removed>&*^&^%&$#@^%$&%%^#$*$($l";

		public ListPropertySource(ComplexProperty<List<String>> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			List<String> list = getEditableValue();
			if (list == null)
			{
				return IPropertyController.NO_DESCRIPTORS;
			}

			IPropertyDescriptor[] descs = new IPropertyDescriptor[list.size()];
			for (int i = 0; i < list.size(); i++)
			{
				String id = String.valueOf(i);
				descs[i] = new TextPropertyDescriptor(id, "")
				{
					@Override
					public CellEditor createPropertyEditor(Composite parent)
					{
						return new TextDialogCellEditor(parent, SWT.NONE, new LabelProvider())
						{
							@Override
							protected Button createButton(Composite buttonParent)
							{
								Button button = super.createButton(buttonParent);
								button.setText("x");
								return button;
							}

							@Override
							public Object openDialogBox(Control cellEditorWindow)
							{
								// button is hit
								return REMOVE_VALUE;
							}
						};
					}
				};
			}

			return descs;
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			List<String> list = getEditableValue();
			if (list == null)
			{
				return null;
			}
			return list.get(Integer.parseInt((String)id));
		}

		@Override
		protected List<String> setComplexPropertyValue(Object id, Object value)
		{
			List<String> list = getEditableValue();
			int index = Integer.parseInt((String)id);
			if (REMOVE_VALUE.equals(value))
			{
				if (list != null)
				{
					list.remove(index);
				}
			}
			else
			{
				if (list == null)
				{
					list = new ArrayList<String>();
				}

				if (index >= list.size())
				{
					list.add((String)value);
				}
				else
				{
					list.set(index, (String)value);
				}
			}
			return list.size() == 0 ? null : list;
		}
	}

	public static ModifiedContentProposalAdapter createContentProposalAdapter(Text text, String[] proposals, final String tooltip)
	{
		final AtomicBoolean popupOpen = new AtomicBoolean(false);

		SimpleContentProposalProvider provider = new SimpleContentProposalProvider(proposals)
		{
			@Override
			public IContentProposal[] getProposals(String contents, int position)
			{
				IContentProposal[] contentProposals = null;
				//find proposals for last inserted style class prefix
				int lastIndexOfSpace = contents.substring(0, position).lastIndexOf(" ");
				if (contents.length() > 0 && lastIndexOfSpace < contents.length() - 1)
				{
					setFiltering(true);
					contentProposals = super.getProposals(contents.substring(lastIndexOfSpace + 1), position);
					setFiltering(false);
				}
				else
				{
					contentProposals = super.getProposals(contents, position);
				}

				if (tooltip == null)
				{
					return contentProposals;
				}

				// set tooltip
				IContentProposal[] modifiedProposels = new IContentProposal[contentProposals.length];
				for (int i = 0; i < contentProposals.length; i++)
				{
					modifiedProposels[i] = new ContentProposal(contentProposals[i].getContent(), contentProposals[i].getLabel(), tooltip.replaceFirst("\\{\\}",
						contentProposals[i].getContent()));
				}
				return modifiedProposels;
			}
		};

		ModifiedContentProposalAdapter contentProposalAdapter = new ModifiedContentProposalAdapter(text, new TextContentAdapter(), provider, null, null);
		contentProposalAdapter.addContentProposalListener(new IContentProposalListener2()
		{
			@Override
			public void proposalPopupClosed(ContentProposalAdapter adapter)
			{
				popupOpen.set(false);
			}

			@Override
			public void proposalPopupOpened(ContentProposalAdapter adapter)
			{
				popupOpen.set(true);
			}
		});
		contentProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

		return contentProposalAdapter;
	}

	public static class ModifiedContentProposalAdapter extends ContentProposalAdapter
	{
		/**
		 * @param control
		 * @param controlContentAdapter
		 * @param proposalProvider
		 * @param keyStroke
		 * @param autoActivationCharacters
		 */
		public ModifiedContentProposalAdapter(Control control, IControlContentAdapter controlContentAdapter, IContentProposalProvider proposalProvider,
			KeyStroke keyStroke, char[] autoActivationCharacters)
		{
			super(control, controlContentAdapter, proposalProvider, keyStroke, autoActivationCharacters);
		}

		@Override
		public void openProposalPopup()
		{
			super.openProposalPopup();
		}
	}

	private static class AddEntriesCellEditor extends CellEditor
	{
		private final String[] proposals;
		private final String tooltip;

		private List<String> value;
		private Button button;
		private Text text;
		private ModifiedContentProposalAdapter contentProposalAdapter;

		public AddEntriesCellEditor(Composite parent, String[] proposals, String tooltip)
		{
			super(parent);
			this.proposals = proposals;
			this.tooltip = tooltip;
		}

		/**
		 * @return the contentProposalAdapter
		 */
		public ModifiedContentProposalAdapter getContentProposalAdapter()
		{
			if (contentProposalAdapter == null && proposals != null && proposals.length > 0)
			{
				contentProposalAdapter = createContentProposalAdapter(text, proposals, tooltip);
			}
			return contentProposalAdapter;
		}

		@Override
		public void activate()
		{
			if (getContentProposalAdapter() != null)
			{
				getControl().getDisplay().asyncExec(new Runnable()
				{
					public void run()
					{
						getContentProposalAdapter().openProposalPopup();
					}
				});
			}
		}

		protected boolean allowNewEntry(String txt)
		{
			return txt != null && txt.length() > 0 && (value == null || !value.contains(txt.trim()));
		}

		protected void addEntry(String txt)
		{
			// create a copy-list so that value is seen as modified
			if (value != null)
			{
				value = new ArrayList<String>(value);
			}
			if (txt != null && txt.trim().length() > 0)
			{
				for (String split : txt.split(" "))
				{
					String val = split.trim();
					if (val.length() > 0 && (value == null || !value.contains(val)))
					{
						if (value == null)
						{
							value = new ArrayList<>();
						}
						value.add(val);
					}
				}
			}

			text.setText("");
			markDirty();
			fireApplyEditorValue();
		}

		@Override
		protected Control createControl(Composite parent)
		{
			Composite composite = new Composite(parent, SWT.NONE);

			text = new Text(composite, SWT.BORDER);
			text.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					button.setEnabled(allowNewEntry(text.getText()));
				}
			});

			text.addTraverseListener(new TraverseListener()
			{
				public void keyTraversed(TraverseEvent e)
				{
					if (e.detail == SWT.TRAVERSE_RETURN &&
						((contentProposalAdapter == null || !contentProposalAdapter.isProposalPopupOpen()) && allowNewEntry(text.getText())))
					{
						e.doit = false;
						addEntry(text.getText());
					}
				}
			});

			button = new Button(composite, SWT.PUSH);
			button.setText("add");
			button.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					addEntry(text.getText());
				}
			});
			button.setEnabled(false);

			// layout
			GroupLayout groupLayout = new GroupLayout(composite);
			SequentialGroup sequentialGroup = groupLayout.createSequentialGroup();
			sequentialGroup.add(text, GroupLayout.PREFERRED_SIZE, 135, Integer.MAX_VALUE);
			sequentialGroup.addPreferredGap(LayoutStyle.RELATED).add(button);
			groupLayout.setHorizontalGroup(sequentialGroup);

			ParallelGroup parallelGroup = groupLayout.createParallelGroup(GroupLayout.CENTER, false);
			parallelGroup.add(button, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE);
			parallelGroup.add(text, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
			groupLayout.setVerticalGroup(parallelGroup);

			composite.setLayout(groupLayout);

			return composite;
		}

		@Override
		protected List<String> doGetValue()
		{
			return value;
		}

		@Override
		protected void doSetValue(Object val)
		{
			this.value = (List<String>)val;
		}

		@Override
		protected void doSetFocus()
		{
			text.forceFocus();
		}
	}
}
