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

import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.resource.DesignPagetype;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.designer.editor.mobile.MobileVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.mobile.MobileVisualFormEditorHtmlDesignPage;
import com.servoy.eclipse.designer.editor.rfb.RfbVisualFormEditorDesignPage;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.ViewPartHelpContextProvider;
import com.servoy.eclipse.ui.editors.ITabbedEditor;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.preferences.DesignerPreferences.FormEditorDesignerPreference;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.util.Utils;


/**
 * Multi-page form editor.
 *
 * @author rgansevles
 *
 */
public class VisualFormEditor extends BaseVisualFormEditor implements ITabbedEditor
{
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


	@Override
	protected void createPages()
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
			case MobileClassic :
				return new MobileVisualFormEditorDesignPage(this);
			case Mobile :
				return new MobileVisualFormEditorHtmlDesignPage(this);
			case Rfb :
				return new RfbVisualFormEditorDesignPage(this);
			case Classic :
				return new VisualFormEditorDesignPage(this);
		}

		throw new IllegalStateException("No design page for " + editorType);
	}

	private static DesignPagetype determineEditorType(FlattenedSolution fs, Form form)
	{
		FormEditorDesignerPreference formEditorDesignerPreference = new DesignerPreferences().getFormEditorDesignerPreference();
		if (isMobile(form))
		{
			if (formEditorDesignerPreference == FormEditorDesignerPreference.Classic
			// TODO for now we also map automatic to classic, because mobile doesn't really work correctly currently
				|| formEditorDesignerPreference == FormEditorDesignerPreference.Automatic)
			{
				return DesignPagetype.MobileClassic;
			}
			return DesignPagetype.Mobile;
		}

		if (formEditorDesignerPreference == FormEditorDesignerPreference.New)
		{
			return DesignPagetype.Rfb;
		}

		if (formEditorDesignerPreference == FormEditorDesignerPreference.Automatic && fs != null && fs.getSolution() != null &&
			fs.getSolution().getSolutionType() == SolutionMetaData.NG_CLIENT_ONLY)
		{
			return DesignPagetype.Rfb;
		}

		if (form != null && (form.getUseCssPosition() || form.isResponsiveLayout() || (fs != null && hasWebComponents(fs.getFlattenedForm(form)))))
		{
			return DesignPagetype.Rfb;
		}

		return DesignPagetype.Classic;
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
	public void revert(boolean force)
	{
		boolean revert = force || isDirty();
		super.revert(force);
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
		if (!isMobile() && seceditor != null) seceditor.saveSecurityElements();
		super.doSave(monitor);

		if (getForm().isFormComponent().booleanValue())
		{
			for (IWorkbenchPage page : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages())
			{
				for (IEditorReference editorReference : page.getEditorReferences())
				{
					IEditorPart editor = editorReference.getEditor(false);
					if (editor instanceof VisualFormEditor && editor != this)
					{
						VisualFormEditor visualFormEditor = (VisualFormEditor)editor;
						BaseVisualFormEditorDesignPage baseVisualFormEditorDesignPage = visualFormEditor.getGraphicaleditor();
						if (baseVisualFormEditorDesignPage instanceof RfbVisualFormEditorDesignPage)
						{
							RfbVisualFormEditorDesignPage rfbVisualFormEditorDesignPage = (RfbVisualFormEditorDesignPage)baseVisualFormEditorDesignPage;
							Form f = visualFormEditor.getForm();
							final FlattenedSolution fs = Activator.getDefault().getDesignClient().getFlattenedSolution();

							if (hasFormReference(fs, f, getForm()))
							{
								rfbVisualFormEditorDesignPage.refreshBrowserUrl(true);
							}
						}
					}
				}
			}
		}

		try
		{
			EclipseMessages.saveFormI18NTexts(getForm());
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError("Cannot save I18N keys", ex);
		}

	}

	private static boolean hasFormReference(final FlattenedSolution fs, Form form, final Form formRef)
	{
		final boolean hasFormReference[] = { false };
		Form flattenedForm = fs.getFlattenedForm(form);
		flattenedForm.acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof IFormElement)
				{
					IFormElement formElement = (IFormElement)o;
					FormElement fe = FormElementHelper.INSTANCE.getFormElement(formElement, fs, null, true);
					if (hasFormReference(fs, fe, formRef))
					{
						hasFormReference[0] = true;
						return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});

		return hasFormReference[0];
	}

	private static boolean hasFormReference(FlattenedSolution fs, FormElement formElement, Form formRef)
	{
		WebObjectSpecification spec = formElement.getWebComponentSpec();
		if (spec != null)
		{
			Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
			if (properties.size() > 0)
			{
				for (PropertyDescription pd : properties)
				{
					Object propertyValue = formElement.getPropertyValue(pd.getName());
					Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
					if (frm != null && (frm == formRef || FlattenedForm.hasFormInHierarchy(frm, formRef) || hasFormReference(fs, frm, formRef)))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 *
	 */
	@Override
	protected void doRefresh(List<IPersist> persists)
	{
		super.doRefresh(persists);
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

	@Override
	public Object getAdapter(Class adapter)
	{
		if (!isMobile() && adapter.equals(CommandStack.class) && getActivePage() >= 0 && getControl(getActivePage()).equals(seceditor))
		{
			return dummyCommandStack;
		}
		if (adapter.equals(IContextProvider.class))
		{
			return new ViewPartHelpContextProvider("com.servoy.eclipse.ui.form_editor");
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

	private boolean isMobile()
	{
		return isMobile(getForm());
	}

	private static boolean isMobile(Form form)
	{
		return form != null && form.getCustomMobileProperty(IMobileProperties.MOBILE_FORM.propertyName) != null;
	}
}
