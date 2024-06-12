/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.eclipse.ui.views;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.equo.chromium.swt.Browser;
import com.servoy.eclipse.core.XMLScriptObjectAdapterLoader;
import com.servoy.eclipse.model.inmemory.MemServer;
import com.servoy.eclipse.model.view.ViewFoundsetsServer;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.documentation.IObjectDocumentation;
import com.servoy.j2db.persistence.CSSPositionLayoutContainer;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;

/**
 * @author jcompagner
 * @since 2024.09
 *
 */
public class DocumentationView extends ViewPart implements ISelectionListener, IPartListener2
{
	private boolean linkWithSelection = true;
	private Browser browser;

	@Override
	public void createPartControl(Composite parent)
	{
		parent.setLayout(new FillLayout());
		browser = new Browser(parent, SWT.NONE);
		// default just show the developer api
		browser.setUrl("https://docs.servoy.com/reference/servoycore/dev-api");

		IActionBars bars = getViewSite().getActionBars();
		bars.getMenuManager().add(new Action("Open url in browser")
		{
			@Override
			public void run()
			{
				String url = browser.getUrl();
				try
				{
					getSite().getWorkbenchWindow().getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URI(url).toURL());
				}
				catch (PartInitException | MalformedURLException | URISyntaxException e)
				{
					e.printStackTrace();
				}
			}
		});

		Action linkAction = new Action("Link with selection", IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				linkWithSelection = this.isChecked();
				if (linkWithSelection)
				{
					registerListenerAndSetSelection();
				}
				else
				{
					getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(DocumentationView.this);
				}
			}
		};
		linkAction.setChecked(linkWithSelection);
		linkAction.setImageDescriptor(Activator.loadImageDescriptorFromBundle("link_to_editor.png"));
		bars.getToolBarManager().add(linkAction);


		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
		getSite().getPage().addPartListener(this);
	}

	@Override
	public void setFocus()
	{
		browser.setFocus();

	}

	@Override
	public void dispose()
	{
		super.dispose();
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
		getSite().getPage().removePartListener(this);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{
		if (selection instanceof IStructuredSelection sl)
		{
			Object selected = sl.getFirstElement();
			Object element = selected;
			if (element instanceof PersistContext pc)
			{
				element = pc.getPersist();
			}
			else if (element instanceof SimpleUserNode node && node.getRealObject() != null)
			{
				element = node.getRealObject();
			}

			// for now just hard code the basic servoy stuff
			if (element instanceof Form)
			{
				browser.setUrl("https://docs.servoy.com/reference/servoycore/object-model/solution/form");
			}
			else if (element instanceof Solution)
			{
				browser.setUrl("https://docs.servoy.com/reference/servoycore/object-model/solution");
			}
			else if (element instanceof Relation)
			{
				browser.setUrl("https://docs.servoy.com/reference/servoycore/object-model/solution/relation");
			}
			else if (element instanceof ValueList)
			{
				browser.setUrl("https://docs.servoy.com/reference/servoycore/object-model/solution/valuelist");
			}
			else if (element instanceof CSSPositionLayoutContainer)
			{
				browser.setUrl("https://docs.servoy.com/reference/servoyextensions/ui-components/form-containers/responsivecontainer");
			}
			else if (element instanceof ViewFoundsetsServer)
			{
				browser.setUrl("https://docs.servoy.com/reference/servoycore/object-model/solution/view-foundset-data-source");
			}
			else if (element instanceof MemServer)
			{
				browser.setUrl("https://docs.servoy.com/reference/servoycore/object-model/solution/in-memory-data-source");
			}
			else if (element instanceof WebComponent wc)
			{
				WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(wc.getTypeName());
				String category = spec.getCategoryName();
				if (category != null)
				{
					category = category.replace(' ', '-').replace("&", "and");
					browser.setUrl("https://docs.servoy.com/reference/servoyextensions/ui-components/" + category + "/" +
						spec.getDisplayName().replace(' ', '-').toLowerCase());
				}
				else browser.setUrl("https://docs.servoy.com/reference/servoyextensions/ui-components");

			}
			else if (element instanceof WebObjectSpecification spec)
			{
				browser.setUrl("https://docs.servoy.com/reference/servoyextensions/browser-plugins/" + spec.getDisplayName().replace(' ', '-').toLowerCase());
			}
			else if (element instanceof Class cls)
			{
				IObjectDocumentation objectDocumentation = XMLScriptObjectAdapterLoader.getObjectDocumentation(cls);
				if (objectDocumentation != null && selected instanceof SimpleUserNode node)
				{
					String nodeUrl = getNodeUrl(node.parent);
					if (nodeUrl != null)
					{
						browser.setUrl(nodeUrl + "/" + objectDocumentation.getPublicName().toLowerCase());
					}
				}
			}
			else if (element instanceof SimpleUserNode node)
			{
				String nodeUrl = getNodeUrl(node);
				if (nodeUrl != null)
				{
					browser.setUrl(nodeUrl);
				}
				else
				{
					nodeUrl = getNodeUrl(node.parent);
					if (nodeUrl != null)
					{
						browser.setUrl(nodeUrl + "/" + node.getName().toLowerCase().replace(' ', '-'));
					}
				}
			}
			else if (selected instanceof SimpleUserNode node)
			{
				String nodeUrl = getNodeUrl(node);
				if (nodeUrl != null)
				{
					browser.setUrl(nodeUrl);
				}
				else
				{
					nodeUrl = getNodeUrl(node.parent);
					if (nodeUrl != null)
					{
						browser.setUrl(nodeUrl + "/" + node.getName().toLowerCase().replace(' ', '-'));
					}
				}
			}
			else
			{
//				System.err.println(element);
			}

		}
	}

	/**
	 * @param node
	 */
	private String getNodeUrl(SimpleUserNode node)
	{
		switch (node.getType())
		{
			case ALL_RELATIONS :
				break;
			case ALL_SOLUTIONS :
				break;
			case ALL_WEB_PACKAGE_PROJECTS :
				break;
			case APIEXPLORER :
				break;
			case APPLICATION :
			case APPLICATION_ITEM :
				return "https://docs.servoy.com/reference/servoycore/dev-api/application";
			case ARRAY :
				break;
			case BEAN :
				break;
			case BEANS :
				break;
			case BEAN_METHOD :
				break;
			case CALCULATIONS :
				break;
			case CALCULATIONS_ITEM :
				break;
			case CALC_RELATION :
				break;
			case CLIENT_UTILS :
			case CLIENT_UTIL_ITEM :
				return "https://docs.servoy.com/reference/servoycore/dev-api/client-utils";
			case COMPONENT :
				break;
			case COMPONENTS_FROM_RESOURCES :
				break;
			case COMPONENTS_NONPROJECT_PACKAGE :
				break;
			case COMPONENTS_PROJECT_PACKAGE :
				break;
			case COMPONENT_FORMS :
				break;
			case COMPONENT_RESOURCE :
				break;
			case CURRENT_FORM :
				break;
			case CURRENT_FORM_ITEM :
				break;
			case CUSTOM_TYPE :
				break;
			case DATASOURCES :
				break;
			case DATE :
				break;
			case EXCEPTIONS :
				break;
			case EXCEPTIONS_ITEM :
				break;
			case FORM :
				break;
			case FORMS :
				break;
			case FORMS_GRAYED_OUT :
				break;
			case FORM_CONTAINERS :
				break;
			case FORM_CONTAINERS_ITEM :
				break;
			case FORM_CONTROLLER :
				break;
			case FORM_CONTROLLER_FUNCTION_ITEM :
				break;
			case FORM_ELEMENTS :
				break;
			case FORM_ELEMENTS_GROUP :
				break;
			case FORM_ELEMENTS_INHERITED :
				break;
			case FORM_ELEMENTS_ITEM :
				break;
			case FORM_ELEMENTS_ITEM_METHOD :
				break;
			case FORM_FOUNDSET :
				break;
			case FORM_METHOD :
				break;
			case FORM_VARIABLES :
				break;
			case FORM_VARIABLE_ITEM :
				break;
			case FOUNDSET :
				break;
			case FOUNDSET_ITEM :
				break;
			case FOUNDSET_MANAGER :
			case FOUNDSET_MANAGER_ITEM :
				return "https://docs.servoy.com/reference/servoycore/dev-api/database-manager";
			case FUNCTIONS :
				break;
			case FUNCTIONS_ITEM :
				break;
			case GLOBALRELATIONS :
				break;
			case GLOBALSCRIPT :
				break;
			case GLOBALS_ITEM :
				break;
			case GLOBAL_METHOD_ITEM :
				break;
			case GLOBAL_VARIABLES :
				break;
			case GLOBAL_VARIABLE_ITEM :
				break;
			case GRAYED_OUT :
				break;
			case HISTORY :
			case HISTORY_ITEM :
				return "https://docs.servoy.com/reference/servoycore/dev-api/history";
			case I18N_FILES :
				break;
			case I18N_FILE_ITEM :
				break;
			case I18N :
			case I18N_ITEM :
				return "https://docs.servoy.com/reference/servoycore/dev-api/i18n";
			case INMEMORY_DATASOURCE :
				break;
			case INMEMORY_DATASOURCES :
				break;
			case ITERABELVALUE :
				break;
			case ITERATOR :
				break;
			case JSLIB :
				return "https://docs.servoy.com/reference/servoycore/dev-api/js-lib";
			case JSON :
				break;
			case JSUNIT :
			case JSUNIT_ITEM :
				return "https://docs.servoy.com/reference/servoycore/dev-api/jsunit";
			case LAYOUT :
				break;
			case LAYOUT_NONPROJECT_PACKAGE :
				break;
			case LAYOUT_PROJECT_PACKAGE :
				break;
			case LOADING :
				break;
			case MAP :
				break;
			case MEDIA :
				break;
			case MEDIA_FOLDER :
				break;
			case MEDIA_IMAGE :
				break;
			case MIGRATION :
				break;
			case MODULE :
				break;
			case MODULES :
				break;
			case NUMBER :
				break;
			case OBJECT :
				break;
			case PLUGIN :
				break;
			case PLUGINS :
			case PLUGINS_ITEM :
				return "https://docs.servoy.com/reference/servoyextensions/server-plugins"; // these are also browser plugins i guess.
			case PROCEDURE :
				break;
			case PROCEDURES :
				break;
			case REGEXP :
				break;
			case RELATION :
				break;
			case RELATIONS :
				break;
			case RELATION_COLUMN :
				break;
			case RELATION_METHODS :
				break;
			case RELEASE :
				break;
			case RESOURCES :
				break;
			case RETURNTYPE :
				break;
			case RETURNTYPEPLACEHOLDER :
				break;
			case RETURNTYPE_CONSTANT :
				return getNodeUrl(node.parent);
			case RETURNTYPE_ELEMENT :
				break;
			case SCOPES_ITEM :
				break;
			case SCOPES_ITEM_CALCULATION_MODE :
				break;
			case SECURITY :
			case SECURITY_ITEM :
				return "https://docs.servoy.com/reference/servoycore/dev-api/security";
			case SERVER :
				break;
			case SERVERS :
				break;
			case SERVICE :
				break;
			case SERVICES_FROM_RESOURCES :
				break;
			case SERVICES_NONPROJECT_PACKAGE :
				break;
			case SERVICES_PROJECT_PACKAGE :
				break;
			case SET :
				break;
			case SOLUTION :
				break;
			case SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES :
				break;
			case SOLUTION_DATASOURCES :
				break;
			case SOLUTION_ITEM :
				break;
			case SOLUTION_ITEM_NOT_ACTIVE_MODULE :
				break;
			case SOLUTION_MODEL :
			case SOLUTION_MODEL_ITEM :
				return "https://docs.servoy.com/reference/servoycore/dev-api/solutionmodel";
			case SPECIAL_OPERATORS :
				break;
			case STATEMENTS :
				break;
			case STATEMENTS_ITEM :
				break;
			case STRING :
				break;
			case STYLES :
				break;
			case STYLE_ITEM :
				break;
			case TABLE :
				break;
			case TABLES :
				break;
			case TABLE_COLUMNS :
				break;
			case TABLE_COLUMNS_ITEM :
				break;
			case TEMPLATES :
				break;
			case TEMPLATE_ITEM :
				break;
			case USER_GROUP_SECURITY :
				break;
			case UTILS :
			case UTIL_ITEM :
				return "https://docs.servoy.com/reference/servoycore/dev-api/utils";
			case VALUELISTS :
				break;
			case VALUELIST_ITEM :
				break;
			case VIEW :
				break;
			case VIEWS :
				break;
			case VIEW_FOUNDSET :
				break;
			case VIEW_FOUNDSETS :
				break;
			case WEB_OBJECT_FOLDER :
				break;
			case WEB_PACKAGE_PROJECT_IN_WORKSPACE :
				break;
			case WORKING_SET :
				break;
			case XML_LIST_METHODS :
				break;
			case XML_METHODS :
				break;
			case ZIP_RESOURCE :
				break;
			default :
				break;

		}
		return null;
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef)
	{
		if (partRef.getPart(false) == this)
		{
			// its hidden remove the listener
			getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
		}
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef)
	{
		if (partRef.getPart(false) == this)
		{
			registerListenerAndSetSelection();
		}
	}

	/**
	 *
	 */
	private void registerListenerAndSetSelection()
	{
		// its made visible again start listening and set the current selection
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
		selectionChanged(this, getSite().getWorkbenchWindow().getSelectionService().getSelection());
	}
}
