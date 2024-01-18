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

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.I18NEditorInput;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.ui.ViewPartHelpContextProvider;
import com.servoy.eclipse.ui.dialogs.I18nComposite;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.i18n.I18NMessagesModel.I18NMessagesModelEntry;
import com.servoy.j2db.persistence.I18NUtil.MessageEntry;
import com.servoy.j2db.util.DataSourceUtils;

public class I18NEditor extends EditorPart
{
	private final IApplication application;
	private final EclipseMessages messagesManager;
	private String i18nDatasource;
	private I18nComposite i18nComposite;
	private ISelectionChangedListener i18nCompositeSelectionChangedListener;

	private Text keyText;
	private Text referenceText;
	private Text localeText;


	private final ModifyListener messageChanged = new ModifyListener()
	{
		public void modifyText(ModifyEvent e)
		{
			I18NMessagesModelEntry row = I18NEditor.this.i18nComposite.getSelectedRow();
			if (row != null && row.key.equals(I18NEditor.this.keyText.getText()))
			{
				String currentRefText = I18NEditor.this.referenceText.getText();
				if (currentRefText == null) currentRefText = "";
				String currentLocaleText = I18NEditor.this.localeText.getText();
				if (currentLocaleText == null) currentLocaleText = "";
				i18nComposite.setSelectionChangedListener(null);
				onChange(row.key, currentRefText, currentLocaleText, true);
				i18nComposite.setSelectionChangedListener(i18nCompositeSelectionChangedListener);
			}
		}
	};

	public I18NEditor()
	{
		application = Activator.getDefault().getDesignClient();
		messagesManager = ServoyModelManager.getServoyModelManager().getServoyModel().getMessagesManager();
	}

	@Override
	public void createPartControl(Composite parent)
	{
		ScrolledComposite myScrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);
		myScrolledComposite.setData(CSSSWTConstants.CSS_ID_KEY, "svyeditor");

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
		keyComposite.setLayout(new GridLayout(5, false));
		keyText = new Text(keyComposite, SWT.BORDER);
		keyText.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));

		Button bAddUpdate = new Button(keyComposite, SWT.PUSH);
		bAddUpdate.setText("Add");
		bAddUpdate.addSelectionListener(new SelectionListener()
		{

			public void widgetDefaultSelected(SelectionEvent e)
			{
				// ignore
			}

			public void widgetSelected(SelectionEvent e)
			{
				onAdd();
			}

		});
		Button bClear = new Button(keyComposite, SWT.PUSH);
		bClear.setText("Clear");
		bClear.addSelectionListener(new SelectionListener()
		{

			public void widgetDefaultSelected(SelectionEvent e)
			{
				// ignore
			}

			public void widgetSelected(SelectionEvent e)
			{
				onClear();
			}

		});

		Button bCopy = new Button(keyComposite, SWT.PUSH);
		bCopy.setText("Copy");
		bCopy.addSelectionListener(new SelectionListener()
		{

			public void widgetDefaultSelected(SelectionEvent e)
			{
				// ignored
			}

			public void widgetSelected(SelectionEvent e)
			{
				onCopy();
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
		referenceText.addModifyListener(messageChanged);

		Group localeTextGroup = new Group(textComposite, SWT.SHADOW_ETCHED_IN);
		localeTextGroup.setText("Locale text");
		localeTextGroup.setLayout(new FillLayout());
		localeText = new Text(localeTextGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		localeText.addModifyListener(messageChanged);

		parentComposite.setLayout(new FillLayout());
		i18nComposite = new I18nComposite(parentComposite, SWT.NONE, application, true, i18nDatasource);
		i18nComposite.setSelectionChangedListener(i18nCompositeSelectionChangedListener = new ISelectionChangedListener()
		{

			public void selectionChanged(SelectionChangedEvent event)
			{
				I18NMessagesModelEntry row = I18NEditor.this.i18nComposite.getSelectedRow();
				I18NEditor.this.referenceText.removeModifyListener(messageChanged);
				I18NEditor.this.localeText.removeModifyListener(messageChanged);
				if (row != null)
				{
					if (!row.key.equals(I18NEditor.this.keyText.getText()))
					{
						I18NEditor.this.keyText.setText(row.key);
					}
					String refText = row.defaultvalue != null ? row.defaultvalue : "";
					if (!refText.equals(I18NEditor.this.referenceText.getText()))
					{
						I18NEditor.this.referenceText.setText(refText);
					}
					String locText = row.localeValue != null ? row.localeValue : "";
					if (!locText.equals(I18NEditor.this.localeText.getText()))
					{
						I18NEditor.this.localeText.setText(locText);
					}
				}
				else
				{
					I18NEditor.this.keyText.setText("");
					I18NEditor.this.referenceText.setText("");
					I18NEditor.this.localeText.setText("");
				}
				I18NEditor.this.referenceText.addModifyListener(messageChanged);
				I18NEditor.this.localeText.addModifyListener(messageChanged);
			}
		});

		i18nComposite.getTableViewer().getTable().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent event)
			{
				Point pt = new Point(event.x, event.y);
				TableItem item = i18nComposite.getTableViewer().getTable().getItem(pt);
				if (item != null && item.getBounds(I18nComposite.CI_DELETE).contains(pt))
				{
					onDelete(item.getText(I18nComposite.CI_KEY));
				}
				if (item != null && !item.isDisposed() && item.getBounds(I18nComposite.CI_COPY).contains(pt))
				{
					String txt = item.getText(I18nComposite.CI_KEY);
					txt = "i18n:" + txt;
					StringSelection stsel = new StringSelection(txt);
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
				}
			}
		});
		sash.setWeights(new int[] { 2, 1 });
		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	private void onAdd()
	{
		String keyTextValue = keyText.getText();
		if (keyTextValue != null)
		{
			if (keyTextValue.length() > 0)
			{
				onChange(keyTextValue, referenceText.getText(), localeText.getText(), false);
				i18nComposite.selectKey("i18n:" + keyTextValue);
			}
		}
	}

	private void onClear()
	{
		referenceText.setText("");
		localeText.setText("");
	}

	private void onDelete(String messageKey)
	{
		messagesManager.removeMessage(i18nDatasource, messageKey);
		i18nComposite.refresh(null);
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	private void onCopy()
	{
		String kt = keyText.getText();
		if (kt.length() > 0)
		{
			kt = "i18n:" + kt;
			Clipboard clipboard = new Clipboard(getSite().getShell().getDisplay());
			clipboard.setContents(new Object[] { kt }, new Transfer[] { TextTransfer.getInstance() });
			clipboard.dispose();
		}
	}

	private void onChange(String key, String refText, String locText, boolean keepSelection)
	{
		messagesManager.addMessage(i18nDatasource, new MessageEntry(null, key, refText));
		messagesManager.addMessage(i18nDatasource, new MessageEntry(i18nComposite.getSelectedLanguage().toString(), key, locText));
		i18nComposite.refresh(keepSelection ? new I18NMessagesModelEntry(key, refText, locText) : null);
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	private void save()
	{
		messagesManager.save(i18nDatasource);
		firePropertyChange(IEditorPart.PROP_DIRTY);
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
		return messagesManager.hasUnsavedMessages(i18nDatasource);
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
			String i18nServer = ((I18NEditorInput)setInput).getServer();
			String i18nTable = ((I18NEditorInput)setInput).getTable();
			i18nDatasource = DataSourceUtils.createDBTableDataSource(i18nServer, i18nTable);
			messagesManager.clearUnsavedMessages(i18nDatasource);
			setPartName(setInput.getName());
		}
	}

	public void refresh()
	{
		i18nComposite.refresh(null);
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (adapter.equals(IContextProvider.class))
		{
			return new ViewPartHelpContextProvider("com.servoy.eclipse.ui.i18n_editor");
		}
		return null;
	}
}
