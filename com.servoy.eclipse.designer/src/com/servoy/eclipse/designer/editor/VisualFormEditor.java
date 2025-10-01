/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.designer.editor;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.internal.genericeditor.ExtensionBasedTextEditor;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.DesignPagetype;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.designer.editor.mobile.MobileVisualFormEditorHtmlDesignPage;
import com.servoy.eclipse.designer.editor.rfb.ChromiumVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.rfb.RfbVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.rfb.SystemVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IEditorRefresh;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.ViewPartHelpContextProvider;
import com.servoy.eclipse.ui.editors.ITabbedEditor;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.resource.FileEditorInputFactory;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.AngularFormGenerator;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.util.Utils;


/**
 * Multi-page form editor.
 *
 * @author rgansevles
 *
 */
public class VisualFormEditor extends BaseVisualFormEditor implements ITabbedEditor, IEditorRefresh
{
	/**
	 * A viewer property indicating whether inherited elements are hidden. The value must  be a Boolean.
	 */
	public static final String PROPERTY_HIDE_INHERITED = "Hide.inherited";


	public static final RequestType REQ_PLACE_TAB = new RequestType(RequestType.TYPE_TAB);
	public static final RequestType REQ_PLACE_PORTAL = new RequestType(RequestType.TYPE_PORTAL);
	public static final RequestType REQ_PLACE_MEDIA = new RequestType(RequestType.TYPE_MEDIA);
	public static final RequestType REQ_PLACE_BEAN = new RequestType(RequestType.TYPE_BEAN);
	public static final RequestType REQ_PLACE_COMPONENT = new RequestType(RequestType.TYPE_COMPONENT);
	public static final RequestType REQ_PLACE_BUTTON = new RequestType(RequestType.TYPE_BUTTON);
	public static final RequestType REQ_PLACE_FIELD = new RequestType(RequestType.TYPE_FIELD);
	public static final RequestType REQ_PLACE_LABEL = new RequestType(RequestType.TYPE_LABEL);
	public static final RequestType REQ_PLACE_RECT_SHAPE = new RequestType(RequestType.TYPE_SHAPE);
	public static final RequestType REQ_PLACE_TEMPLATE = new RequestType(RequestType.TYPE_TEMPLATE);
	public static final String REQ_DISTRIBUTE = "VFE_DISTRIBUTE";
	public static final String VFE_PAGE_ID = "PageID";

	// constants for the mobile editor
	public static final RequestType REQ_PLACE_HEADER_TITLE = new RequestType(RequestType.TYPE_LABEL);
	public static final RequestType REQ_PLACE_HEADER = new RequestType(RequestType.TYPE_PART);
	public static final RequestType REQ_PLACE_FOOTER = new RequestType(RequestType.TYPE_PART);
	public static final RequestType REQ_PLACE_INSET_LIST = new RequestType(RequestType.TYPE_TAB);
	public static final RequestType REQ_PLACE_FORM_LIST = new RequestType(RequestType.TYPE_TAB);
	public static final RequestType REQ_PLACE_TOGGLE = new RequestType(RequestType.TYPE_FIELD);


	private VisualFormEditorPartsPage partseditor = null;
	private VisualFormEditorTabSequencePage tabseditor = null;
	private VisualFormEditorSecurityPage seceditor = null;
	private TextEditor cssEditor;

	private final CommandStack dummyCommandStack = new CommandStack()
	{
		@Override
		public boolean canRedo()
		{
			return false;
		}

		@Override
		public boolean canUndo()
		{
			return false;
		}
	};

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.designer.editor.BaseVisualFormEditor#setInput(org.eclipse.ui.IEditorInput)
	 */
	@Override
	protected void setInput(IEditorInput input)
	{
		super.setInput(input);
	}


	@Override
	protected void createPages()
	{
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
		{
			super.createPages();
			if (!isMobile() && !Utils.getAsBoolean(getForm().isFormComponent()))
			{
				if (!isClosing())
				{
					if (!getForm().isResponsiveLayout()) // is absolute layout
					{
						createPartsPage();
					}
					createTabsPage();
				}
				createSecPage(); // MultiPageEditorPart wants at least 1 page
				createCSSPage();
				if (DesignerUtil.getContentOutline() != null)
				{
					Display.getDefault().asyncExec(() -> {
						if (getSite() != null && getSite().getPage() != null)
						{
							getSite().getPage().activate(this);
						}
					});
				}
			}
		}
		else
		{
			setPageText(addPage(new Composite(getContainer(), SWT.NONE)), "Loading Solution");
		}
	}

	private void createPartsPage()
	{
		partseditor = new VisualFormEditorPartsPage(this, getContainer(), SWT.NONE);
		setPageText(addPage(partseditor), "Parts");
	}

	private void createTabsPage()
	{
		tabseditor = new VisualFormEditorTabSequencePage(this, getContainer(), SWT.NONE);
		tabseditor.setData(VFE_PAGE_ID, ITabbedEditor.VFE_TABSEQ_PAGE);
		setPageText(addPage(tabseditor), "Tab sequence");
	}

	private void createSecPage()
	{
		seceditor = new VisualFormEditorSecurityPage(this, getContainer(), SWT.NONE);
		setPageText(addPage(seceditor), "Security");
	}

	private void createCSSPage()
	{
		cssEditor = new ExtensionBasedTextEditor();
		try
		{
			IFile file = SolutionSerializer.getFormLESSFile(getForm());
			if (file.exists())
			{
				setPageText(addPage(cssEditor, FileEditorInputFactory.createFileEditorInput(file)), "Less");
			}
			else
			{
				Composite control = new Composite(getContainer(), SWT.NONE);
				control.setLayout(new RowLayout());
				Button button = new Button(control, SWT.PUSH);
				button.setText("Create Form Less file");
				final int page = addPage(control);
				setPageText(page, "Less");
				button.addListener(SWT.Selection, selection -> {
					try
					{
						removePage(page);

						byte[] bytes = new byte[0];
						try
						{
							bytes = IOUtils.toByteArray(VisualFormEditor.class.getResourceAsStream("less_form_template.less"));
						}
						catch (IOException e)
						{
							ServoyLog.logError(e);
						}
						file.create(bytes, true, false, null);
						int page2 = addPage(cssEditor, FileEditorInputFactory.createFileEditorInput(file));
						setPageText(page2, "Less");
						setActivePage(page2);
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}

				});
			}
		}
		catch (PartInitException e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	protected BaseVisualFormEditorDesignPage createGraphicaleditor(DesignPagetype designPagetype)
	{
		FlattenedSolution fs = Activator.getDefault().getDesignClient().getFlattenedSolution(); // enforce loading of internal style

		DesignPagetype editorType = designPagetype;
		if (editorType == null && getEditorInput() instanceof PersistEditorInput)
		{
			// saved last design page type that was selected by the user
			editorType = ((PersistEditorInput)getEditorInput()).getDesignPagetype();
		}

		if (editorType == null)
		{
			editorType = determineEditorType(fs, getForm());
		}

		switch (editorType)
		{
			case Mobile :
				return new MobileVisualFormEditorHtmlDesignPage(this);
			case Rfb :
				if (new DesignerPreferences().useChromiumBrowser()) return new ChromiumVisualFormEditorDesignPage(this);
				return new SystemVisualFormEditorDesignPage(this);
		}

		throw new IllegalStateException("No design page for " + editorType);
	}

	private static DesignPagetype determineEditorType(FlattenedSolution fs, Form form)
	{
		if (isMobile(form))
		{
			return DesignPagetype.Mobile;
		}

		return DesignPagetype.Rfb;
	}

	private static boolean hasWebComponents(Form flattenedForm)
	{
		return flattenedForm.acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof WebComponent)
				{
					return Boolean.TRUE;
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		}) == Boolean.TRUE;
	}

	@Override
	public void persistChanges(Collection<IPersist> changedPersists)
	{
		super.persistChanges(changedPersists);
		if (graphicaleditor instanceof RfbVisualFormEditorDesignPage)
		{
			((RfbVisualFormEditorDesignPage)graphicaleditor).refreshBrowserUrl(false);
		}
	}

	@Override
	public void refresh()
	{
		if (graphicaleditor instanceof RfbVisualFormEditorDesignPage)
		{
			((RfbVisualFormEditorDesignPage)graphicaleditor).refreshBrowserUrl(true);
		}

	}

	@Override
	public void revert(boolean force, boolean refresh)
	{
		boolean revert = force || isDirty();
		super.revert(force, refresh);
		if (revert)
		{
			if (graphicaleditor instanceof RfbVisualFormEditorDesignPage)
			{
				((RfbVisualFormEditorDesignPage)graphicaleditor).revert();
			}
		}
	}

	@Override
	public void dispose()
	{
		if (!isMobile())
		{
			if (partseditor != null)
			{
				partseditor.dispose();
			}
			if (tabseditor != null)
			{
				tabseditor.dispose();
			}
			if (seceditor != null)
			{
				seceditor.dispose();
			}
		}
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		if (cssEditor != null && cssEditor.isDirty())
		{
			IDocument document = cssEditor.getDocumentProvider().getDocument(cssEditor.getEditorInput());
			try
			{
				AngularFormGenerator.parseLess(document.get(), getForm().getName(), ServoyModelFinder.getServoyModel().getFlattenedSolution());
			}
			catch (Exception e)
			{
				MessageDialog.openError(getSite().getShell(), "Error saving Less", "There is an error saving the less content:" + e.getMessage());
			}
			cssEditor.doSave(monitor);
		}
		if (!isMobile() && seceditor != null) seceditor.saveSecurityElements();
		super.doSave(monitor);


		try
		{
			EclipseMessages.saveFormI18NTexts(getForm());
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError("Cannot save I18N keys", ex);
		}

	}

	/**
	 *
	 */
	@Override
	protected void doRefresh(List<IPersist> persists, boolean fullRefresh)
	{
		super.doRefresh(persists, fullRefresh);
		if (!isMobile())
		{
			if (partseditor != null)
			{
				partseditor.refresh();
			}
			if (tabseditor != null)
			{
				tabseditor.refresh();
			}
			if (seceditor != null)
			{
				seceditor.refresh();
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter)
	{
		if (adapter.equals(CommandStack.class))
		{
			if (getActiveEditor() == graphicaleditor)
				return (T)getCommandStack();
			else return (T)dummyCommandStack;
		}
		if (adapter.equals(IContextProvider.class))
		{
			return (T)new ViewPartHelpContextProvider("com.servoy.eclipse.ui.form_editor");
		}
		return super.getAdapter(adapter);
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.editors.TabbedEditor#changeActiveTab(java.lang.String)
	 */
	public void changeActiveTab(String tabName)
	{
		CTabItem[] items = ((CTabFolder)getContainer()).getItems();
		for (int i = 0; i < items.length; i++)
		{
			if (tabName.equals(items[i].getControl().getData(VFE_PAGE_ID)))
			{
				setActivePage(i);
				setFocus();
				return;
			}
		}
	}

	private Boolean isMobile = null;

	private boolean isMobile()
	{
		if (isMobile == null) isMobile = Boolean.valueOf(isMobile(getForm()));
		return isMobile.booleanValue();
	}

	private static boolean isMobile(Form form)
	{
		return form != null && form.getCustomMobileProperty(IMobileProperties.MOBILE_FORM.propertyName) != null;
	}
}
