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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IFileAccess;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.WorkspaceFileAccess;
import com.servoy.eclipse.core.repository.EclipseMessages;
import com.servoy.eclipse.core.resource.I18NEditorInput;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.dialogs.I18nComposite;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.persistence.I18NUtil;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.property.I18NMessagesModel.I18NMessagesModelEntry;

public class I18NEditor extends EditorPart
{
	private final IApplication application;
	private final IFileAccess workspace;
	private String i18nServer;
	private String i18nTable;
	private I18nComposite i18nComposite;

	private Text keyText;
	private Text referenceText;
	private Text localeText;

	private final HashMap<String, String[]> unsavedText = new HashMap<String, String[]>();
	private String oldRefText = "";
	private String oldLocaleText = "";

	public I18NEditor()
	{
		application = Activator.getDefault().getDesignClient();
		workspace = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
	}

	@Override
	public void createPartControl(Composite parent)
	{
		ScrolledComposite myScrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		Composite container = new Composite(myScrolledComposite, SWT.NONE);

		myScrolledComposite.setContent(container);
		container.setLayout(new FillLayout());

		SashForm sash = new SashForm(container, SWT.VERTICAL);
		Composite parentComposite = new Composite(sash, SWT.NONE);
		Composite upperComposite = new Composite(sash, SWT.NONE);
		upperComposite.setLayout(new GridLayout(1, false));

		Group keyGroup = new Group(upperComposite, SWT.SHADOW_ETCHED_IN);
		keyGroup.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		keyGroup.setText("Key");
		keyGroup.setLayout(new FillLayout());

		Composite keyComposite = new Composite(keyGroup, SWT.NONE);
		keyComposite.setLayout(new GridLayout(4, false));
		keyText = new Text(keyComposite, SWT.BORDER);
		keyText.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		Button bAddUpdate = new Button(keyComposite, SWT.PUSH);
		bAddUpdate.setText("Add/Update");
		bAddUpdate.addSelectionListener(new SelectionListener()
		{

			public void widgetDefaultSelected(SelectionEvent e)
			{
				// ignore
			}

			public void widgetSelected(SelectionEvent e)
			{
				onAddUpdate();
			}

		});
		Button bClear = new Button(keyComposite, SWT.PUSH);
		bClear.setText("Clear");
		bClear.addSelectionListener(new SelectionListener()
		{

			public void widgetDefaultSelected(SelectionEvent e)
			{
				// TODO Auto-generated method stub

			}

			public void widgetSelected(SelectionEvent e)
			{
				onClear();
			}

		});
		Button bDelete = new Button(keyComposite, SWT.PUSH);
		bDelete.setText("Delete");
		bDelete.addSelectionListener(new SelectionListener()
		{

			public void widgetDefaultSelected(SelectionEvent e)
			{
				// ignored
			}

			public void widgetSelected(SelectionEvent e)
			{
				onDelete();
			}

		});

		SashForm textComposite = new SashForm(upperComposite, SWT.HORIZONTAL);
		GridData textCompositeGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		textCompositeGridData.widthHint = 500;
		textComposite.setLayoutData(textCompositeGridData);

		Group refTextGroup = new Group(textComposite, SWT.SHADOW_ETCHED_IN);
		refTextGroup.setText("Reference text");
		refTextGroup.setLayout(new FillLayout());
		referenceText = new Text(refTextGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP);

		Group localeTextGroup = new Group(textComposite, SWT.SHADOW_ETCHED_IN);
		localeTextGroup.setText("Locale text");
		localeTextGroup.setLayout(new FillLayout());
		localeText = new Text(localeTextGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP);

		parentComposite.setLayout(new FillLayout());
		i18nComposite = new I18nComposite(parentComposite, SWT.NONE, application);
		i18nComposite.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener()
		{

			public void selectionChanged(SelectionChangedEvent event)
			{
				I18NMessagesModelEntry row = I18NEditor.this.i18nComposite.getSelectedRow();
				if (row != null)
				{
					String currentKey = I18NEditor.this.keyText.getText();
					if (currentKey != null && currentKey.length() > 0)
					{
						String currentRefText = I18NEditor.this.referenceText.getText();
						if (currentRefText == null) currentRefText = "";
						String currentLocaleText = I18NEditor.this.localeText.getText();
						if (currentLocaleText == null) currentLocaleText = "";


						if (!I18NEditor.this.oldRefText.equals(currentRefText) || !I18NEditor.this.oldLocaleText.equals(currentLocaleText))
						{
							I18NEditor.this.unsavedText.put(currentKey.trim(), new String[] { currentRefText.trim(), currentLocaleText.trim() });

							Collection<I18NMessagesModelEntry> currentView = (Collection<I18NMessagesModelEntry>)I18NEditor.this.i18nComposite.getTableViewer().getInput();
							Iterator<I18NMessagesModelEntry> currentViewIte = currentView.iterator();
							I18NMessagesModelEntry entry;
							while (currentViewIte.hasNext())
							{
								entry = currentViewIte.next();
								if (entry.key.equals(currentKey))
								{
									entry.defaultvalue = currentRefText;
									entry.localeValue = currentLocaleText;
									I18NEditor.this.i18nComposite.getTableViewer().refresh();
									break;
								}
							}
							firePropertyChange(IEditorPart.PROP_DIRTY);
						}
					}

					I18NEditor.this.oldRefText = row.defaultvalue != null ? row.defaultvalue : "";
					I18NEditor.this.oldLocaleText = row.localeValue != null ? row.localeValue : "";

					I18NEditor.this.keyText.setText(row.key != null ? row.key : "");
					I18NEditor.this.referenceText.setText(I18NEditor.this.oldRefText);
					I18NEditor.this.localeText.setText(I18NEditor.this.oldLocaleText);
				}
				else
				{
					I18NEditor.this.keyText.setText("");
					I18NEditor.this.referenceText.setText("");
					I18NEditor.this.localeText.setText("");
				}
			}
		});
		i18nComposite.addLanguageSelectionListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				if (I18NEditor.this.checkForUnsavedMessages() &&
					MessageDialog.openQuestion(UIUtils.getActiveShell(), "I18N editor", "There are unsaved messages, save now ?")) save();
			}

		});
		sash.setWeights(new int[] { 2, 1 });
		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	private void onAddUpdate()
	{
		try
		{
			TreeMap<String, I18NUtil.MessageEntry> messages = EclipseMessages.readMessages(i18nServer, i18nTable, workspace);
			String key = keyText.getText().trim();
			I18NUtil.MessageEntry defaultEntry = new I18NUtil.MessageEntry(null, key, referenceText.getText().trim());
			I18NUtil.MessageEntry localeEntry = new I18NUtil.MessageEntry(Messages.localeToString(i18nComposite.getSelectedLanguage()), key,
				localeText.getText().trim());

			messages.put(defaultEntry.getLanguageKey(), defaultEntry);
			messages.put(localeEntry.getLanguageKey(), localeEntry);

			EclipseMessages.writeMessages(i18nServer, i18nTable, messages, workspace);
			i18nComposite.refresh();
			i18nComposite.selectKey("i18n:" + key);
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError(ex);
		}
	}

	private void onClear()
	{
		referenceText.setText("");
		localeText.setText("");
	}

	private void onDelete()
	{
		if (MessageDialog.openQuestion(UIUtils.getActiveShell(), "I18N editor", "Are you sure you want to delete the key : '" + keyText.getText().trim() +
			"' ?"))
		{
			try
			{
				ArrayList<String> messageKeyToRemove = new ArrayList<String>();
				TreeMap<String, I18NUtil.MessageEntry> messages = EclipseMessages.readMessages(i18nServer, i18nTable, workspace);
				Iterator<Map.Entry<String, I18NUtil.MessageEntry>> messageIte = messages.entrySet().iterator();
				Map.Entry<String, I18NUtil.MessageEntry> messageEntry;
				String keyTextValue = keyText.getText();
				while (messageIte.hasNext())
				{
					messageEntry = messageIte.next();
					if (messageEntry.getValue().getKey().equals(keyTextValue)) messageKeyToRemove.add(messageEntry.getKey());
				}
				for (String mkey : messageKeyToRemove)
					messages.remove(mkey);

				EclipseMessages.writeMessages(i18nServer, i18nTable, messages, workspace);
				i18nComposite.refresh();
			}
			catch (RepositoryException ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}


	private void save()
	{
		try
		{
			TreeMap<String, I18NUtil.MessageEntry> messages = EclipseMessages.readMessages(i18nServer, i18nTable, workspace);
			String key = keyText.getText().trim();

			I18NUtil.MessageEntry defaultEntry = new I18NUtil.MessageEntry(null, key, referenceText.getText().trim());
			I18NUtil.MessageEntry localeEntry = new I18NUtil.MessageEntry(Messages.localeToString(i18nComposite.getSelectedLanguage()), key,
				localeText.getText().trim());

			messages.put(defaultEntry.getLanguageKey(), defaultEntry);
			messages.put(localeEntry.getLanguageKey(), localeEntry);

			Iterator<String> unsavedKeysIte = unsavedText.keySet().iterator();
			while (unsavedKeysIte.hasNext())
			{
				key = unsavedKeysIte.next();
				String[] texts = unsavedText.get(key);

				defaultEntry = new I18NUtil.MessageEntry(null, key, texts[0]);
				localeEntry = new I18NUtil.MessageEntry(Messages.localeToString(i18nComposite.getSelectedLanguage()), key, texts[1]);

				messages.put(defaultEntry.getLanguageKey(), defaultEntry);
				messages.put(localeEntry.getLanguageKey(), localeEntry);
			}


			EclipseMessages.writeMessages(i18nServer, i18nTable, messages, workspace);
			i18nComposite.refresh();

			unsavedText.clear();
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError(ex);
		}
	}

	private boolean checkForUnsavedMessages()
	{
		boolean isCurrentChanged = false;

		String currentKey = I18NEditor.this.keyText.getText();
		if (currentKey != null && currentKey.length() > 0)
		{
			String currentRefText = I18NEditor.this.referenceText.getText();
			if (currentRefText == null) currentRefText = "";
			String currentLocaleText = I18NEditor.this.localeText.getText();
			if (currentLocaleText == null) currentLocaleText = "";


			isCurrentChanged = !I18NEditor.this.oldRefText.equals(currentRefText) || !I18NEditor.this.oldLocaleText.equals(currentLocaleText);

		}

		return (isCurrentChanged || unsavedText.size() > 0);
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		save();
	}

	@Override
	public void doSaveAs()
	{
		// ignored
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty()
	{
		return checkForUnsavedMessages();
	}

	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	@Override
	public void setFocus()
	{
		// ignored
	}

	@Override
	protected void setInput(IEditorInput input)
	{
		IEditorInput setInput = (input instanceof FileEditorInput) ? setInput = I18NEditorInput.createFromFileEditorInput((FileEditorInput)input) : input;
		super.setInput(setInput);
		if (setInput instanceof I18NEditorInput)
		{
			i18nServer = ((I18NEditorInput)setInput).getServer();
			i18nTable = ((I18NEditorInput)setInput).getTable();
			setPartName(setInput.getName());
		}
	}
}
