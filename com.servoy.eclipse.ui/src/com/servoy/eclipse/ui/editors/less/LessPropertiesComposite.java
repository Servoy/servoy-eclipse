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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.CachingChildrenComposite;
import com.servoy.eclipse.ui.editors.less.LessPropertyEntry.LessPropertyType;
import com.servoy.j2db.server.ngclient.less.resources.ThemeResourceLoader;

/**
 * Editor for Servoy Theme Properties less.
 * @author emera
 */
public class LessPropertiesComposite extends Composite
{

	public class ExpandableLessPropertiesComposite extends Composite
	{
		private final String categoryName;

		public ExpandableLessPropertiesComposite(ExpandBar parent, PropertiesLessEditorInput propertiesLessEditorInput, Font font, String categoryName)
		{
			super(parent, SWT.NONE);
			this.categoryName = categoryName;
			GridLayout layout = new GridLayout(3, false);
			layout.marginRight = 5;
			setLayout(layout);
			setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			for (LessPropertyEntry property : propertiesLessEditorInput.getProperties(categoryName))
			{
				addPropertyEntry(this, font, propertiesLessEditorInput, property);
			}
			this.pack();
		}

		public void refresh()
		{
			int i = 0;
			LessPropertyEntry[] properties = ((PropertiesLessEditorInput)editor.getEditorInput()).getProperties(categoryName);
			for (Control c : getChildren())
			{
				if (c instanceof Text)
				{
					Text text = (Text)c;
					text.setText(properties[i++].getValue());
				}
			}
		}
	}

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
	private final static com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();

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

	private CachingChildrenComposite area;
	private final ScrolledComposite sc;
	private final PropertiesLessEditor editor;
	private final Color backgroundColor;
	private final char[] autoActivationCharacters = new char[] { '@' };
	private KeyStroke keyStroke;
	private ArrayList<ExpandableLessPropertiesComposite> categoryComposites;
	private Text firstText;
	private Button tiCheck;
	private CCombo combo;

	public LessPropertiesComposite(Composite parent, int style, final PropertiesLessEditor editor)
	{
		super(parent, style);
		try
		{
			keyStroke = KeyStroke.getInstance("Ctrl+Space");
		}
		catch (ParseException e)
		{
			ServoyLog.logError("Problem on theme editor initialization", e);
		}

		this.editor = editor;
		this.setLayout(new FillLayout());
		sc = new ScrolledComposite(parent, SWT.TRANSPARENT | SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		backgroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		sc.setBackground(backgroundColor);
		createArea();
	}

	/**
	 * @param parent
	 * @param editor
	 */
	private void createArea()
	{
		area = new CachingChildrenComposite(sc, SWT.TRANSPARENT);
		sc.setContent(area);
		GridLayout layout = new GridLayout(1, false);
		layout.marginRight = 5;
		area.setLayout(layout);
//		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		FontDescriptor descriptor = FontDescriptor.createFrom(sc.getFont());
		descriptor = descriptor.setStyle(SWT.BOLD);
		Font font = descriptor.createFont(getShell().getDisplay());

		try
		{
			Composite comp = new Composite(area, SWT.NONE);
			RowLayout rowLayout = new RowLayout();
			rowLayout.spacing = 8;
			comp.setLayout(rowLayout);
			comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

			PropertiesLessEditorInput propertiesLessEditorInput = (PropertiesLessEditorInput)editor.getEditorInput();

			Label l = new Label(comp, SWT.NONE);
			l.setText("Servoy Theme Version");
			l.setBackground(backgroundColor);
			l.setFont(font);
			combo = new CCombo(comp, SWT.READ_ONLY | SWT.BORDER);
			combo.setBackground(backgroundColor);
			combo.setItems(ThemeResourceLoader.VERSIONS);
			String version = propertiesLessEditorInput.getVersion();
			boolean tiVersion = false;
			if (version.endsWith("_ng2"))
			{
				tiVersion = true;
				version = version.substring(0, version.length() - 4);
			}
			combo.select(Arrays.asList(ThemeResourceLoader.VERSIONS).indexOf(version));
			combo.addListener(SWT.Selection, e -> {
				setThemeVersion();
			});
			tiCheck = new Button(comp, SWT.CHECK);
			tiCheck.setText("Titanum NG Theme");
			tiCheck.setSelection(tiVersion);
			tiCheck.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					setThemeVersion();
				}
			});
			categoryComposites = new ArrayList<ExpandableLessPropertiesComposite>();
			for (String categoryName : propertiesLessEditorInput.getCategories())
			{
				ExpandBar expandBar = new ExpandBar(area, SWT.NONE);
				expandBar.setBackground(backgroundColor);
				expandBar.setFont(font);
				expandBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
				ExpandableLessPropertiesComposite expandComposite = new ExpandableLessPropertiesComposite(expandBar, propertiesLessEditorInput, font,
					categoryName);
				categoryComposites.add(expandComposite);
				expandComposite.setBackground(backgroundColor);
				ExpandItem collapsableItem = new ExpandItem(expandBar, SWT.NONE, 0);
				collapsableItem.setHeight(expandComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
				collapsableItem.setControl(expandComposite);
				collapsableItem.setText(categoryName + " Properties");
				collapsableItem.setExpanded(categoryComposites.size() == 1);//expand the first one by default
				expandBar.addExpandListener(new ExpandListener()
				{
					public void itemExpanded(ExpandEvent e)
					{
						collapsableItem.setImage(uiActivator.loadImageFromBundle("collapse_tree.png"));
						Display.getCurrent().asyncExec(() -> {
							collapsableItem.setHeight(expandComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
							sc.setMinSize(area.computeSize(SWT.DEFAULT, SWT.DEFAULT));
							sc.layout(true, true);
						});
					}

					public void itemCollapsed(ExpandEvent e)
					{
						collapsableItem.setImage(uiActivator.loadImageFromBundle("expandall.png"));
						Display.getCurrent().asyncExec(() -> {
							collapsableItem.setHeight(0);
							sc.setMinSize(area.computeSize(SWT.DEFAULT, SWT.DEFAULT));
							sc.layout(true, true);
						});
					}
				});
				collapsableItem.setImage(uiActivator.loadImageFromBundle(collapsableItem.getExpanded() ? "collapse_tree.png" : "expandall.png"));
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		area.cacheChildren(true);
		sc.setMinSize(area.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		Display.getCurrent().asyncExec(() -> {
			if (firstText != null) firstText.setFocus();
		});
	}

	/**
	 * @param propertiesLessEditorInput
	 * @param combo
	 */
	protected void setThemeVersion()
	{
		PropertiesLessEditorInput propertiesLessEditorInput = (PropertiesLessEditorInput)editor.getEditorInput();
		int selectionIndex = combo.getSelectionIndex();
		if (selectionIndex > -1)
		{
			String selectedVersion = ThemeResourceLoader.VERSIONS[selectionIndex];
			if (tiCheck.getSelection()) selectedVersion = selectedVersion + "_ng2";
			String text = PropertiesLessEditorInput.getFileContent(propertiesLessEditorInput);
			int versionIndex = text.indexOf(ThemeResourceLoader.THEME_LESS);
			if (versionIndex != -1)
			{
				int endIndex = text.indexOf("';", versionIndex);
				if (selectedVersion == "latest") // special case
				{
					text = text.substring(0, versionIndex + ThemeResourceLoader.THEME_LESS.length()) + text.substring(endIndex);
				}
				else
				{
					text = text.substring(0, versionIndex + ThemeResourceLoader.THEME_LESS.length()) + "?version=" + selectedVersion +
						text.substring(endIndex);
				}
				try
				{
					propertiesLessEditorInput.getFile().setContents(new ByteArrayInputStream(text.getBytes(Charset.forName("UTF-8"))), IResource.FORCE,
						null);
				}
				catch (CoreException e1)
				{
					ServoyLog.logError(e1);
				}
				propertiesLessEditorInput.reloadProperties(text, true);
				area.dispose();
				createArea();
				sc.layout(true, true);
			}
		}
	}

	protected void addPropertyEntry(Composite container, Font font, PropertiesLessEditorInput propertiesLessEditorInput, LessPropertyEntry property)
	{
		Label label = new Label(container, SWT.NONE);
		label.setBackground(backgroundColor);
		label.setFont(font);
		label.setText(property.getLabel());
		final Text txtName = new Text(container, SWT.BORDER);
		if (firstText == null)
		{
			firstText = txtName;
		}

		Color normalBg = label.getForeground();
		label.addListener(SWT.MouseUp, e -> {
			if (e.button == 3 && property.getDefaultValue() != null)
			{
				if (property.getStoredDefault() != null && !property.getStoredDefault().equals(property.getDefaultValue()))
				{
					property.setStoredDefault(property.getDefaultValue());
					this.editor.propertyModified(property);
				}
				else
				{
					txtName.setText(property.getDefaultValue());
				}
				setChanged(property, label, normalBg);
			}
		});

		setChanged(property, label, normalBg);

		txtName.setText(property.getValue());
		txtName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtName.addListener(SWT.Modify, e -> {
			String newVal = txtName.getText().trim();
			if (!property.getValue().equals(newVal))
			{
				property.setValue(newVal);
				property.setStoredDefault(property.getDefaultValue());
				editor.propertyModified(property);
				setChanged(property, label, normalBg);
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
			Button editButton = new Button(container, SWT.FLAT);
			editButton.setBackground(backgroundColor);
			editButton.setText("Select Color");
			editButton.addListener(SWT.Selection, e -> {
				final Display display = editButton.getDisplay();
				final Shell shell = new Shell(display);
				Rectangle bounds = editButton.getBounds();
				shell.setLocation(bounds.x + bounds.width,
					bounds.y - (sc.getVerticalBar() != null ? sc.getVerticalBar().getSelection() : 0) + bounds.height);
				ColorDialog dialog = new ColorDialog(shell);
				//TODO dialog.setRGB(rgb); ?
				if (dialog.open() != null)
				{
					RGB rgb = dialog.getRGB();
					//TODO insert ?
					txtName.setText(String.format("#%02x%02x%02x", rgb.red, rgb.green, rgb.blue).toUpperCase());
				}
				shell.dispose();
			});
		}
		//TODO add other pickers
		else
		{
			new Label(container, SWT.NONE).setText("");
		}
	}

	/**
	 * @param property
	 * @param label
	 * @param normalBg
	 */
	private void setChanged(LessPropertyEntry property, Label label, Color normalBg)
	{
		if (property.getStoredDefault() != null && !property.getStoredDefault().equals(property.getDefaultValue()))
		{
			label.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			label.setToolTipText("Default value changed from: " + property.getStoredDefault() + " to " + property.getDefaultValue() +
				", right click to to update the default value or change the value itself");
		}
		else if (property.getDefaultValue() != null && !property.getDefaultValue().equals(property.getValue()))
		{
			label.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_LINK_FOREGROUND));
			label.setToolTipText("Value overwrites default value: " + property.getDefaultValue() + ", right click to reset");
		}
		else
		{
			label.setForeground(normalBg);
			label.setToolTipText(null);
		}
	}


	public Control getControl()
	{
		return sc;
	}

	public void refresh()
	{
		categoryComposites.forEach(c -> c.refresh());
	}
}
