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
package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.json.JSONException;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.ngpackages.ILoadedNGPackagesListener;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * An action to save some group of elements as template.
 */
public class SaveAsTemplateAction extends SelectionAction
{
	public SaveAsTemplateAction(IWorkbenchPart part)
	{
		super(part);
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText(DesignerActionFactory.SAVE_AS_TEMPLATE_TEXT);
		setToolTipText(DesignerActionFactory.SAVE_AS_TEMPLATE_TOOLTIP);
		setId(DesignerActionFactory.SAVE_AS_TEMPLATE.getId());
		setImageDescriptor(DesignerActionFactory.SAVE_AS_TEMPLATE_IMAGE);
	}

	@Override
	protected boolean calculateEnabled()
	{
		return getSelection() != null && !getSelection().isEmpty();
	}

	private boolean groupTemplateElements = false;
	private final BiPredicate<List<IPersist>, ISupportChilds> isNested = (List<IPersist> selected, ISupportChilds t) -> {
		if (selected.contains(t)) return true;
		ISupportChilds realParent = PersistHelper.getRealParent(t);
		return realParent instanceof LayoutContainer && this.isNested.test(selected, realParent);
	};

	private static Pair<String, Boolean> askForTemplateName(Shell shell, boolean grouping)
	{
		UIUtils.InputAndCheckDialog dialog = new UIUtils.InputAndCheckDialog(shell, "New template", "Specify a template name", null, new IInputValidator()
		{
			public String isValid(String newText)
			{
				if (newText.length() == 0) return "";
				return validateMethodName(newText);
			}
		}, "Group template elements", grouping);

		dialog.setBlockOnOpen(true);
		dialog.open();

		String name = (dialog.getReturnCode() == Window.CANCEL) ? null : dialog.getValue();

		return new Pair<String, Boolean>(name, dialog.getExtendedValue());
	}

	protected static String validateMethodName(String templateName)
	{
		// see if style name is OK
		if (!IdentDocumentValidator.isJavaIdentifier(templateName))
		{
			return "Template name has unsupported characters";
		}
		if (templateName.length() > IRepository.MAX_ROOT_OBJECT_NAME_LENGTH)
		{
			return "Name is too long";
		}
		else
		{
			IStatus validationResult = ServoyModel.getWorkspace().validateName(templateName, IResource.FILE);
			if (!validationResult.isOK())
			{
				return "The name of the template to be created is not valid: " + validationResult.getMessage();
			}

			List<IRootObject> allTemplatesList = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.TEMPLATES);
			for (IRootObject template : allTemplatesList)
			{
				if (templateName.toLowerCase().equals(template.getName().toLowerCase()))
				{
					return "A template with this name already exists. Please modify the template name.";
				}
			}
		}
		return null;
	}


	@Override
	public void run()
	{
		Pair<String, Boolean> result = askForTemplateName(UIUtils.getActiveShell(), groupTemplateElements);
		String templateName = result.getLeft();
		groupTemplateElements = result.getRight().booleanValue();

		if (templateName == null)
		{
			// cancelled
			return;
		}

		// check existing template
		StringResource existingTemplate = (Template)ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObject(templateName,
			IRepository.TEMPLATES);
		if (existingTemplate != null && !MessageDialog.openConfirm(getWorkbenchPart().getSite().getShell(), "Template exists",
			"A template with name '" + templateName + "' already exists, do you want to overwrite?"))
		{
			return;
		}

		Form form = null;
		List<IPersist> persists = new ArrayList<IPersist>();
		for (Object selected : getSelectedObjects())
		{
			if (selected instanceof EditPart)
			{
				Object model = ((EditPart)selected).getModel();
				if (model instanceof Form)
				{
					form = (Form)model;
				}
				else if (model instanceof FormElementGroup)
				{
					Iterator<ISupportFormElement> elements = ((FormElementGroup)model).getElements();
					while (elements.hasNext())
					{
						persists.add(elements.next());
					}
				}
				else if (model instanceof IPersist)
				{
					persists.add((IPersist)model);
				}
			}
			else if (selected instanceof PersistContext)
			{
				IPersist persist = ((PersistContext)selected).getPersist();

				if (persist instanceof Form) form = (Form)persist;
				else persists.add(persist);

			}
			else if (selected instanceof Form)
			{
				form = (Form)selected;
			}
		}

		int resourceType = StringResource.ELEMENTS_TEMPLATE;
		if (form == null && persists.size() > 0)
		{
			form = (Form)persists.get(0).getAncestor(IRepository.FORMS);
		}
		else if (form != null && persists.size() == 0)
		{
			resourceType = StringResource.FORM_TEMPLATE;
			// just the form, add all non-script children
			for (IPersist persist : Utils.iterate(form.getAllObjects()))
			{
				if (!(persist instanceof IScriptElement))
				{
					persists.add(persist);
				}
			}
		}

		if (form == null)
		{
			// should not happen
			ServoyLog.logError("Save template: no form (selection empty?)", null);
			return;
		}

		List<IPersist> selected = completeContainment(form, persists);
		List<IPersist> unflattenedSelection = new ArrayList<IPersist>();
		for (IPersist currentPersist : selected)
		{
			if (currentPersist instanceof IFlattenedPersistWrapper)
			{
				unflattenedSelection.add(((IFlattenedPersistWrapper)currentPersist).getWrappedPersist());
			}
			else
			{
				unflattenedSelection.add(currentPersist);
			}
		}
		List<IPersist> sel = selected.stream().filter(p -> !isNested.test(unflattenedSelection, PersistHelper.getRealParent(p))).collect(Collectors.toList());

		ServoyModelManager.getServoyModelManager().getServoyModel();
		EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
		try
		{
			StringResource template;
			if (existingTemplate == null)
			{
				// new template
				template = (StringResource)repository.createNewRootObject(templateName, IRepository.TEMPLATES);
			}
			else
			{
				// overwrite
				template = existingTemplate;
			}
			template.setResourceType(resourceType);
			template.setContent(ElementFactory.createTemplateContent(repository, form, sel, resourceType, groupTemplateElements));
			repository.updateRootObject(template);
		}
		catch (JSONException e)
		{
			ServoyLog.logError("Could not generate template content", e);
			MessageDialog.openError(getWorkbenchPart().getSite().getShell(), "Cannot create new template", "Reason: " + e.getMessage());
			return;
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Cannot save template", e);
			MessageDialog.openError(getWorkbenchPart().getSite().getShell(), "Cannot save template", "Reason: " + e.getMessage());
		}

		// TODO - is this ok? manually triggering the ng component package change broadcast just to refresh the palette?
		ServoyModelFinder.getServoyModel().getNGPackageManager().ngPackagesChanged(ILoadedNGPackagesListener.CHANGE_REASON.RELOAD, false);
	}

	/**
	 * Replace selected subelements with their parent nodes, for instance replace a tab with its tabpanel
	 *
	 * @param form
	 * @param persists
	 * @return
	 */
	private List<IPersist> completeContainment(Form form, List<IPersist> persists)
	{
		List<IPersist> retval = new ArrayList<IPersist>();

		//add all nodes between the form and the selected items (for instance a tabpanel container of a selected tab)
		for (IPersist persist : persists)
		{
			if (persist != form)
			{
				while (persist != null && !(persist instanceof Form) /* can be another form in case of inherited elements */)
				{
					if (!retval.contains(persist))
					{
						retval.add(persist);
					}
					persist = persist.getParent();
					if (persist instanceof LayoutContainer) break;
				}
			}
		}
		return retval;
	}

}
