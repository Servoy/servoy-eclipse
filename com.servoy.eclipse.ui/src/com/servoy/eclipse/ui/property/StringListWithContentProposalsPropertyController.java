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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout.ParallelGroup;
import org.eclipse.swt.layout.grouplayout.GroupLayout.SequentialGroup;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.servoy.eclipse.ui.editors.DialogCellEditor;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.editors.TextDialogCellEditor;
import com.servoy.eclipse.ui.labelproviders.DefaultValueDelegateLabelProvider;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;

/**
 * Property controller for properties that are a space-separated string with content proposal for entries in the string.
 *
 * @author rgansevles
 */

public class StringListWithContentProposalsPropertyController extends PropertyController<String, Object>
{
	private static final StringTokenizerListConverter STRING_TO_LIST_CONVERTER = new StringTokenizerListConverter(" ", true);

	private final String[] proposals;
	private final String tooltip;
	private final String defaultValue;
	private final IValueEditor<String> valueEditor;

	private ILabelProvider labelProvider;

	public StringListWithContentProposalsPropertyController(Object id, String displayName, String[] proposals, String defaultValue, String tooltip,
		IValueEditor<String> valueEditor)
	{
		super(id, displayName);
		this.proposals = proposals;
		this.defaultValue = defaultValue;
		this.tooltip = tooltip;
		this.valueEditor = valueEditor;
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
						ListPropertySource propertySource = new ListPropertySource(this, valueEditor);
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
		if (labelProvider == null)
		{
			labelProvider = new DefaultValueDelegateLabelProvider(new LabelProvider()
			{
				@Override
				public String getText(Object list)
				{
					return STRING_TO_LIST_CONVERTER.convertValue(null, (List<String>)list);
				}
			}, defaultValue);
		}

		return labelProvider;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return new WordsWithContentProposalCellEditor(parent, proposals, tooltip, valueEditor);
	}

	private static String getWordAt(String txt, int pos)
	{
		int beginIndex = Math.max(txt.lastIndexOf(' ', Math.max(0, pos - 1)) + 1, 0);
		int endIndex = txt.indexOf(' ', pos);
		if (endIndex < 0)
		{
			endIndex = txt.length();
		}

		return beginIndex >= endIndex ? null : txt.substring(beginIndex, endIndex);
	}

	protected static class ListPropertySource extends ComplexPropertySourceWithStandardReset<List<String>>
	{
		private static final String REMOVE_VALUE = "<removed>&*^&^%&$#@^%$&%%^#$*$($l";

		private final IValueEditor<String> valueEditor;

		public ListPropertySource(ComplexProperty<List<String>> complexProperty, IValueEditor<String> valueEditor)
		{
			super(complexProperty);
			this.valueEditor = valueEditor;
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
							private Button openButton;

							@Override
							protected Control createContents(Composite parent, boolean createSingleLine)
							{
								Control contents = super.createContents(parent, createSingleLine);

								if (valueEditor != null && contents instanceof Text)
								{
									final Text textContents = (Text)contents;
									contents.addListener(SWT.Modify, new Listener()
									{
										@Override
										public void handleEvent(Event arg0)
										{
											openButton.setEnabled(valueEditor != null && valueEditor.canEdit(textContents.getText()));
										}
									});
								}


								return contents;
							}

							@Override
							protected Button createButton(Composite buttonParent)
							{
								Button button = super.createButton(buttonParent);
								button.setText("x");
								return button;
							}

							@Override
							protected Button createButton2(Composite buttonParent)
							{
								if (valueEditor == null)
								{
									return null;
								}

								openButton = new Button(buttonParent, SWT.FLAT);
								openButton.setImage(DialogCellEditor.OPEN_IMAGE);
								return openButton;

							}

							@Override
							public Object openDialogBox(Control cellEditorWindow)
							{
								// button is hit
								return REMOVE_VALUE;
							}

							@Override
							protected void editValue2(Control control)
							{
								valueEditor.openEditor(text.getText());
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
				// find proposals for last inserted style class prefix
				String word = getWordAt(contents, position);
				IContentProposal[] contentProposals;
				if (word != null && word.length() > 0)
				{
					int beginIndex = Math.max(contents.lastIndexOf(' ', Math.max(0, position - 1)) + 1, 0);
					setFiltering(true);
					contentProposals = super.getProposals(word.substring(0, position - beginIndex), position);
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
					modifiedProposels[i] = new ContentProposal(contentProposals[i].getContent(), contentProposals[i].getLabel(),
						tooltip.replaceFirst("\\{\\}", contentProposals[i].getContent()));
				}
				return modifiedProposels;
			}
		};

		ModifiedContentProposalAdapter contentProposalAdapter = new ModifiedContentProposalAdapter(text, new ReplaceWordsTextContentAdapter(), provider, null,
			null);
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

		@Override
		public void closeProposalPopup()
		{
			super.closeProposalPopup();
		}
	}

	/**
	 *
	 * @author rgansevles
	 *
	 */
	public static class ReplaceWordsTextContentAdapter extends TextContentAdapter
	{
		@Override
		public void setControlContents(Control control, String text, int cursorPosition)
		{
			String replaced = replaceCurrentWord(((Text)control).getText(), ((Text)control).getCaretPosition(), text);
			// set the cursor to the end of the current word
			int pos = ((Text)control).getCaretPosition();
			while (pos < replaced.length() && replaced.charAt(pos) != ' ')
			{
				pos++;
			}
			((Text)control).setText(replaced);
			((Text)control).setSelection(pos, pos);
		}

		private String replaceCurrentWord(String text, int pos, String replacement)
		{
			String remainder = text.substring(pos);
			while (remainder.length() > 0 && !remainder.startsWith(" "))
			{
				remainder = remainder.substring(1);
			}
			String begin = text.substring(0, pos);

			for (int i = replacement.length(); i > 0; i--)
			{
				if (endsWithIgnoreCase(begin, replacement.substring(0, i)))
				{
					begin = begin.substring(0, begin.length() - i);
					break;
				}
			}

			return begin + replacement + remainder;
		}

		private boolean endsWithIgnoreCase(String str, String suffix)
		{
			if (str == null || suffix == null) return false;
			if (suffix.length() > str.length()) return false;
			return str.regionMatches(true, str.length() - suffix.length(), suffix, 0, suffix.length());
		}
	}

	public static class ModifiedText extends Text
	{
		private final AbstractWordsWithContentProposalCellEditor cellEditor;

		public ModifiedText(Composite parent, int style, AbstractWordsWithContentProposalCellEditor cellEditor)
		{
			super(parent, style);
			this.cellEditor = cellEditor;
		}

		@Override
		public void paste()
		{
			super.paste();
			if (cellEditor.getContentProposalAdapter() != null) cellEditor.getContentProposalAdapter().closeProposalPopup();
		}

		@Override
		protected void checkSubclass()
		{
		}
	}

	static abstract class AbstractWordsWithContentProposalCellEditor extends CellEditor
	{

		private final String[] proposals;
		private final String tooltip;

		protected Button button;
		protected Text text;
		private ModifiedContentProposalAdapter contentProposalAdapter;

		public AbstractWordsWithContentProposalCellEditor(Composite parent, String[] proposals, String tooltip)
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
			text.selectAll();
		}

		@Override
		protected Control createControl(Composite parent)
		{
			Composite composite = new Composite(parent, SWT.NONE);

			text = new ModifiedText(composite, SWT.BORDER, this);
			addListeners();
			text.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetDefaultSelected(SelectionEvent e)
				{
					fireApplyEditorValue();
					deactivate();
				}
			});

			addButton(composite);

			// layout
			GroupLayout groupLayout = new GroupLayout(composite);
			SequentialGroup sequentialGroup = groupLayout.createSequentialGroup();
			sequentialGroup.add(text, GroupLayout.PREFERRED_SIZE, 135, Integer.MAX_VALUE);
			sequentialGroup.addPreferredGap(LayoutStyle.RELATED).add(button);
			groupLayout.setHorizontalGroup(sequentialGroup);

			ParallelGroup parallelGroup = groupLayout.createParallelGroup(GroupLayout.CENTER, false);
			parallelGroup.add(button, 0, 0, Integer.MAX_VALUE);
			parallelGroup.add(text, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
			groupLayout.setVerticalGroup(parallelGroup);

			composite.setLayout(groupLayout);

			return composite;
		}

		protected String getCurrentWord()
		{
			return getWordAt(text.getText(), text.getCaretPosition());
		}

		@Override
		protected void doSetFocus()
		{
			text.forceFocus();
		}

		protected abstract void addButton(Composite composite);

		protected abstract void addListeners();
	}

	class WordsWithContentProposalCellEditor extends AbstractWordsWithContentProposalCellEditor
	{
		private final IValueEditor<String> valueEditor;

		public WordsWithContentProposalCellEditor(Composite parent, String[] proposals, String tooltip, IValueEditor<String> valueEditor)
		{
			super(parent, proposals, tooltip);
			this.valueEditor = valueEditor;
		}


		@Override
		protected void addListeners()
		{
			text.addListener(SWT.Modify, new Listener()
			{
				@Override
				public void handleEvent(Event event)
				{
					markDirty();
					button.setEnabled(valueEditor != null && valueEditor.canEdit(getCurrentWord()));
				}

			});
			Listener listener = new Listener()
			{
				@Override
				public void handleEvent(Event arg0)
				{
					button.setEnabled(valueEditor != null && valueEditor.canEdit(getCurrentWord()));
				}
			};
			text.addListener(SWT.KeyUp, listener);
			text.addListener(SWT.MouseUp, listener);
		}

		@Override
		protected void addButton(Composite composite)
		{
			button = new Button(composite, SWT.FLAT);
			button.setImage(DialogCellEditor.OPEN_IMAGE);
			button.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					if (valueEditor != null)
					{
						valueEditor.openEditor(getCurrentWord());
					}
				}
			});
			button.setEnabled(false);
		}

		@Override
		protected List<String> doGetValue()
		{
			return STRING_TO_LIST_CONVERTER.convertProperty(null, text.getText());
		}

		@Override
		protected void doSetValue(Object val)
		{
			text.setText(PersistPropertySource.NULL_STRING_CONVERTER.convertProperty(null, STRING_TO_LIST_CONVERTER.convertValue(null, (List<String>)val)));
		}
	}
}
