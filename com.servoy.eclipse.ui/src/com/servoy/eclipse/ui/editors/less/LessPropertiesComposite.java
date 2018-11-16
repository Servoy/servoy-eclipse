/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.eclipse.ui.editors.less;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.less.LessPropertyEntry.LessPropertyType;

/**
 * Editor for Servoy Theme Properties less.
 * @author emera
 */
public class LessPropertiesComposite extends Composite
{

	private final class LessPropertiesContentProposalListener implements IContentProposalListener
	{
		private final Text textField;

		private LessPropertiesContentProposalListener(Text txtName)
		{
			this.textField = txtName;
		}

		@Override
		public void proposalAccepted(IContentProposal proposal)
		{
			int beginIndex = getBeginIndex(textField.getText(), textField.getCaretPosition());
			String word = getWordAt(textField.getText(), beginIndex);
			textField.setText(new StringBuffer(textField.getText()).replace(beginIndex, beginIndex + word.length(), proposal.getContent()).toString());
			int end = proposal.getCursorPosition() + beginIndex;
			textField.setSelection(end, end);
		}
	}

	private static final Pattern pattern = Pattern.compile("[@\\w]([\\w-]*)");

	private String getWordAt(String txt, int pos)
	{
		Matcher matcher = pattern.matcher(txt.substring(pos));
		return matcher.find() ? matcher.group() : "";
	}

	private int getBeginIndex(String txt, int idx)
	{
		int fromIndex = idx - 1;
		int lastIndexOf = txt.lastIndexOf(' ', fromIndex);
		if (lastIndexOf < 0) lastIndexOf = txt.lastIndexOf('(', fromIndex);
		if (lastIndexOf < 0) lastIndexOf = txt.lastIndexOf(')', fromIndex);
		if (lastIndexOf < 0) lastIndexOf = txt.lastIndexOf(',', fromIndex);
		int beginIndex = Math.max(lastIndexOf + 1, 0);
		return beginIndex;
	}

	private final class LessPropertiesContentProposalProvider extends SimpleContentProposalProvider
	{
		private LessPropertiesContentProposalProvider(String[] proposals)
		{
			super(proposals);
		}

		@Override
		public IContentProposal[] getProposals(String contents, int position)
		{
			int beginIndex = getBeginIndex(contents, position);
			String word = getWordAt(contents, beginIndex);
			IContentProposal[] contentProposals;
			if (word != null && word.length() > 0)
			{
				setFiltering(true);
				contentProposals = super.getProposals(word.substring(0, position - beginIndex), position);
				setFiltering(false);
			}
			else
			{
				contentProposals = super.getProposals(contents, position);
			}
			return contentProposals;
		}
	}

	private final Composite area;
	private final ScrolledComposite sc;
	private final PropertiesLessEditor editor;

	public LessPropertiesComposite(Composite parent, int style, final PropertiesLessEditor editor)
	{
		super(parent, style);
		this.editor = editor;
		this.setLayout(new FillLayout());
		sc = new ScrolledComposite(parent, SWT.TRANSPARENT | SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);

		area = new Composite(sc, SWT.TRANSPARENT);
		sc.setContent(area);
		area.setLayout(new GridLayout(3, false));
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		try
		{
			PropertiesLessEditorInput propertiesLessEditorInput = (PropertiesLessEditorInput)editor.getEditorInput();
			char[] autoActivationCharacters = new char[] { '@' };
			KeyStroke keyStroke = KeyStroke.getInstance("Ctrl+Space");

			for (LessPropertyEntry property : propertiesLessEditorInput.getProperties())
			{
				new Label(area, SWT.NONE).setText(property.getLabel());
				Text txtName = new Text(area, SWT.BORDER);
				txtName.setText(property.getValue());
				txtName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				txtName.addListener(SWT.Modify, e -> {
					String newVal = txtName.getText().trim();
					if (!property.getValue().equals(newVal))
					{
						property.setValue(newVal);
						editor.propertyModified(property);
					}
				});
				txtName.addListener(SWT.FocusOut, e -> {
					String newVal = txtName.getText().trim();
					if (newVal.equals(""))
					{
						txtName.setText(property.getLastTxtValue());
					}
				});

				String[] contentProposals = propertiesLessEditorInput.getContentProposals(property.getType(), property.getName());
				if (contentProposals != null && contentProposals.length > 0)
				{
					LessPropertiesContentProposalProvider proposalProvider = new LessPropertiesContentProposalProvider(contentProposals);
					ContentProposalAdapter adapter = new ContentProposalAdapter(txtName, new TextContentAdapter(), proposalProvider, keyStroke,
						autoActivationCharacters);
					adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_IGNORE);
					adapter.addContentProposalListener(new LessPropertiesContentProposalListener(txtName));
				}

				if (property.getType() == LessPropertyType.COLOR)
				{
					Button editButton = new Button(area, SWT.BORDER);
					editButton.setText("Select Color");
					editButton.addListener(SWT.Selection, e -> {
						ColorDialog dialog = new ColorDialog(getShell());
						//TODO dialog.setRGB(rgb); ?
						if (dialog.open() != null)
						{
							RGB rgb = dialog.getRGB();
							//TODO insert ?
							txtName.setText(String.format("#%02x%02x%02x", rgb.green, rgb.red, rgb.blue).toUpperCase());
						}
					});
				}
				//TODO add other pickers
				else
				{
					new Label(area, SWT.NONE).setText("");
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		sc.setMinSize(area.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	public Control getControl()
	{
		return sc;
	}

	public void refresh()
	{
		int i = 0;
		LessPropertyEntry[] properties = ((PropertiesLessEditorInput)editor.getEditorInput()).getProperties();
		for (Control c : area.getChildren())
		{
			if (c instanceof Text)
			{
				Text text = (Text)c;
				text.setText(properties[i++].getValue());
			}
		}
	}

}
