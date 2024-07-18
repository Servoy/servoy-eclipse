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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Multipage editor that allows editing the values of the Servoy Theme Properties.
 * @author emera
 */
public class PropertiesLessEditor extends MultiPageEditorPart
{
	private PropertiesLessEditorInput editorInput;
	private LessPropertiesComposite propertiesComposite;
	private TextEditor textEditor;
	private boolean isPageModified = false;

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		if (isDirty())
		{
			if (getActivePage() == 0)
			{
				updateTextEditorFromProperties();
			}
			else
			{
				updatePropertiesFromTextEditor();
			}
			textEditor.doSave(monitor);
			isPageModified = false;
			editorInput.clearChanges();
			firePropertyChange(PROP_DIRTY);
		}
	}

	private void updateTextEditorFromProperties()
	{
		textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput()).set(editorInput.getText());
	}

	@Override
	public void doSaveAs()
	{
		// not supported, never called
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		if (input != null)
		{
			setInput(convertInput(input));
		}
	}

	private IEditorInput convertInput(IEditorInput input)
	{
		if (input instanceof FileEditorInput)
		{
			editorInput = PropertiesLessEditorInput.createFromFileEditorInput((FileEditorInput)input);
			if (editorInput != null)
			{
				return editorInput;
			}
		}
		return input;
	}

	@Override
	public boolean isDirty()
	{
		return isPageModified || super.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	@Override
	public void setFocus()
	{
		switch (getActivePage())
		{
			case 0 :
				propertiesComposite.setFocus();
				break;
			case 1 :
				textEditor.setFocus();
				break;
		}
	}


	public void propertyModified(LessPropertyEntry property)
	{
		if (!isDirty())
		{
			isPageModified = true;
			firePropertyChange(PROP_DIRTY);
		}
		editorInput.propertyModified(property);
	}

	@Override
	protected void createPages()
	{
		propertiesComposite = new LessPropertiesComposite(getContainer(), SWT.NONE, this);
		int index = addPage(propertiesComposite.getControl());
		setPageText(index, "Properties");
		propertiesComposite.getControl().getDisplay().asyncExec(() -> updatePropertiesFromTextEditor());

		try
		{
			textEditor = new TextEditor();
			index = addPage(textEditor, getEditorInput());
			setPageText(index, "Source");
		}
		catch (PartInitException e)
		{
			ServoyLog.logError("Error creating source editor for the Servoy theme properties less file.", e);
		}
		setTitleToolTip(getEditorInput().getToolTipText());
	}


	@Override
	protected void pageChange(int newPageIndex)
	{
		switch (newPageIndex)
		{
			case 0 :
				if (isDirty()) updatePropertiesFromTextEditor();
				break;
			case 1 :
				if (isPageModified) updateTextEditorFromProperties();
				break;
		}
		super.pageChange(newPageIndex);
	}


	private void updatePropertiesFromTextEditor()
	{
		editorInput.reloadProperties(textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput()).get(), false);
	}
}
