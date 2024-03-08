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
package com.servoy.eclipse.designer.property;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Path;
import org.eclipse.gef.EditPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.mobile.property.MobileComponentWithTitlePropertySource;
import com.servoy.eclipse.designer.mobile.property.MobileListPropertySource;
import com.servoy.eclipse.designer.mobile.property.MobilePersistPropertySource;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.eclipse.ui.actions.Openable;
import com.servoy.eclipse.ui.editors.PersistEditor;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.DimensionPropertySource;
import com.servoy.eclipse.ui.property.IModelSavePropertySource;
import com.servoy.eclipse.ui.property.IPropertyHandler;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.eclipse.ui.property.PDPropertySource;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.PointPropertySource;
import com.servoy.eclipse.ui.property.PropertyCategory;
import com.servoy.eclipse.ui.property.RetargetToEditorPersistProperties;
import com.servoy.eclipse.ui.property.SavingPersistPropertySource;
import com.servoy.eclipse.ui.property.WebComponentPropertyHandler;
import com.servoy.eclipse.ui.property.WebComponentPropertySource;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Pair;

/**
 * Factory for adapters for the Properties view in the designer.
 */
public class DesignerPropertyAdapterFactory implements IAdapterFactory
{
	private static Class[] ADAPTERS = new Class[] { IPropertySource.class, IPersist.class, IResource.class, FormElementGroup.class, MobileListModel.class, Openable.class };

	public Class[] getAdapterList()
	{
		return ADAPTERS;
	}

	public Object getAdapter(Object obj, Class key)
	{
		if (obj instanceof MobileListModel && key == IPropertySource.class)
		{
			MobileListModel model = getEditingMobileListModel((MobileListModel)obj);
			return MobileListPropertySource.getMobileListPropertySource(model, model.form);
		}
		if (obj instanceof Dimension && key == IPropertySource.class)
		{
			return new DimensionPropertySource(new ComplexProperty<Dimension>((Dimension)obj), null);
		}
		if (obj instanceof Point && key == IPropertySource.class)
		{
			return new PointPropertySource(new ComplexProperty<Point>((Point)obj));
		}

		if (obj instanceof FormElementGroup && key == IPropertySource.class)
		{
			return createFormElementGroupPropertySource((FormElementGroup)obj, ((FormElementGroup)obj).getParent());
		}

		IPersist persist = null;
		IPersist context = null;
		boolean autoSave = false;
		boolean retargetToEditor = true;
		if (obj instanceof BaseVisualFormEditor)
		{
			persist = ((BaseVisualFormEditor)obj).getForm();
			retargetToEditor = false;
		}
		else if (obj instanceof SimpleUserNode)
		{
			SimpleUserNode userNode = (SimpleUserNode)obj;
			if (userNode.isEnabled())
			{
				Object realObject = userNode.getRealObject();
				if (realObject instanceof ServoyProject)
				{
					persist = ((ServoyProject)realObject).getSolution();
					autoSave = true; // there is no editor, changes must be saved straight through
				}
				// some nodes are stored in an object array
				if (realObject instanceof Object[] && ((Object[])realObject).length > 0)
				{
					realObject = ((Object[])realObject)[0];
				}

				if (key == MobileListModel.class)
				{
					if (realObject instanceof MobileListModel)
					{
						return getEditingMobileListModel((MobileListModel)realObject);
					}
					return null;
				}
				if (realObject instanceof MobileListModel && key == IPropertySource.class)
				{
					MobileListModel model = getEditingMobileListModel((MobileListModel)realObject);
					MobileListPropertySource mobileListPropertySource = MobileListPropertySource.getMobileListPropertySource(model, model.form);
					return new RetargetToEditorPersistProperties(mobileListPropertySource);
				}
				if (realObject instanceof FormElementGroup && key == FormElementGroup.class)
				{
					return getEditingFormElementGroup((FormElementGroup)realObject);
				}
				if (realObject instanceof FormElementGroup && key == IPropertySource.class)
				{
					SimpleUserNode formNode = userNode.getAncestorOfType(Form.class);
					return new RetargetToEditorPersistProperties(createFormElementGroupPropertySource((FormElementGroup)realObject,
						formNode == null ? ((FormElementGroup)obj).getParent() : (Form)formNode.getRealObject()));
				}

				if (realObject instanceof IPersist && !(realObject instanceof Solution) && !(realObject instanceof Style) &&
					!(realObject instanceof StringResource)) // solution is shown under ServoyProject nodes
				{
					persist = (IPersist)realObject;
					context = userNode.getForm();
				}
				if (persist instanceof IScriptElement || persist instanceof Media)
				{
					autoSave = true;
				}
			}
		}
		else if (obj instanceof EditPart)
		{
			EditPart editPart = (EditPart)obj;
			Form contextForm = getEditpartFormContext(editPart);
			context = contextForm;

			if (key == IResource.class)
			{
				return getResource(contextForm);
			}

			if (key == Openable.class)
			{
				Openable tmp = Openable.getOpenable(editPart.getModel() != null ? editPart.getModel() : contextForm);
				tmp.setAttribute("FormDesigner", "true");
				return tmp;
			}

			Object model = editPart.getModel();

			if (model instanceof FormElementGroup)
			{
				if (key == IPropertySource.class)
				{
					return createFormElementGroupPropertySource((FormElementGroup)model, contextForm);
				}
				if (key == FormElementGroup.class)
				{
					return getEditingFormElementGroup((FormElementGroup)model);
				}
				return null;
			}

			if (model instanceof MobileListModel)
			{
				if (key == IPropertySource.class)
				{
					return MobileListPropertySource.getMobileListPropertySource(getEditingMobileListModel((MobileListModel)model), contextForm);
				}
				return null;
			}

			if (model instanceof IPersist)
			{
				persist = (IPersist)model;
				retargetToEditor = false;
			}
		}
		else if (obj instanceof PersistEditor)
		{
			PersistEditor editPart = (PersistEditor)obj;
			persist = editPart.getPersist();
			retargetToEditor = false;
		}
		else if (obj instanceof PersistContext)
		{
			persist = ((PersistContext)obj).getPersist();
			context = ((PersistContext)obj).getContext();
			retargetToEditor = false;
		}
		else if (obj instanceof IPersist)
		{
			persist = (IPersist)obj;
			autoSave = persist instanceof Solution || persist instanceof IScriptElement;
			// retargetToEditor must be true so that persist saves from anywhere (like quickfix)
			// retargetToEditor = false;
		}

		if (persist != null)
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(persist.getRootObject().getName());
			if (servoyProject == null)
			{
				ServoyLog.logError("Cannot find Servoy project for persist " + persist, null);
				return null;
			}
			// make sure we have the in-memory editing version, the real solution is read-only
			persist = getEditingPersist(persist);

			if (persist == null)
			{
				// not present yet in editing solution
				return null;
			}
			if (context == null)
			{
				IPersist form = persist.getAncestor(IRepository.FORMS);
				context = form == null ? persist : form;
			}
			else
			{
				ServoyProject parentProject = servoyProject;
				if (!servoyProject.getSolution().getName().equals(context.getRootObject().getName()))
				{
					parentProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(context.getRootObject().getName());
				}
				if (parentProject == null)
				{
					ServoyLog.logError("Cannot find Servoy project for persist " + context, null);
					return null;
				}
				context = AbstractRepository.searchPersist(parentProject.getEditingSolution(), context);
			}

			if (key == IPersist.class)
			{
				return persist;
			}
			if (key == IPropertySource.class)
			{
				// for properties view
				PersistContext persistContext = PersistContext.create(persist, context);
				PersistPropertySource persistProperties = null;

				if (FormTemplateGenerator.isWebcomponentBean(persist))
				{
					PropertyDescription propertyDescription = null;
					if (persist.getParent() != null)
					{
						propertyDescription = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
							FormTemplateGenerator.getComponentTypeName((IBasicWebComponent)persist));
					}

					if (propertyDescription != null)
					{
						persistProperties = new WebComponentPropertySource(persistContext, false, propertyDescription);
					}
				}
				else if (persist instanceof WebCustomType)
				{

					WebCustomType ghostBean = (WebCustomType)persist;
					PropertyDescription propertyDescription = ghostBean.getPropertyDescription();


					if (propertyDescription != null)
					{
						persistProperties = new PDPropertySource(persistContext, false, propertyDescription);
					}
				}
				else if (persist instanceof WebFormComponentChildType)
				{
					WebFormComponentChildType formComponentChild = (WebFormComponentChildType)persist;
					PropertyDescription propertyDescription = formComponentChild.getPropertyDescription();

					if (propertyDescription != null)
					{
						persistProperties = new PDPropertySource(persistContext, false, propertyDescription)
						{
							@Override
							protected IPropertyHandler[] createPropertyHandlers(Object valueObject)
							{
								ArrayList<IPropertyHandler> handlers = new ArrayList<>();
								handlers.addAll(Arrays.asList(super.createPropertyHandlers(valueObject)));
								if (getPropertyDescription() instanceof WebObjectSpecification)
								{
									for (WebObjectFunctionDefinition desc : ((WebObjectSpecification)getPropertyDescription()).getHandlers().values())
									{
										handlers.add(new WebComponentPropertyHandler(desc.getAsPropertyDescription()));
									}
								}
								return handlers.toArray(new IPropertyHandler[handlers.size()]);
							}

							@Override
							protected PropertyCategory createPropertyCategory(PropertyDescriptorWrapper propertyDescriptor)
							{
								if (getPropertyDescription() instanceof WebObjectSpecification &&
									((WebObjectSpecification)getPropertyDescription()).getHandlers().containsKey(
										propertyDescriptor.propertyDescriptor.getName()))
									return PropertyCategory.Events;
								if (getPropertyDescription().getProperties().containsKey(propertyDescriptor.propertyDescriptor.getName()) ||
									"designTimeProperties".equals(propertyDescriptor.propertyDescriptor.getName()) ||
									IContentSpecConstants.PROPERTY_ATTRIBUTES.equals(propertyDescriptor.propertyDescriptor.getName()))
									return PropertyCategory.Component;
								return super.createPropertyCategory(propertyDescriptor);
							}

							@Override
							public String toString()
							{
								final WebFormComponentChildType child = (WebFormComponentChildType)getPersist();
								return child.getParentComponent().getName() + '.' + child.getKey() + " (" + getPropertyDescription().getName() + ')';
							}
						};
					}
				}
				else if (persist instanceof LayoutContainer)
				{
					LayoutContainer layoutContainer = (LayoutContainer)persist;
					PropertyDescription propertyDescription = null;
					if (persist.getParent() != null)
					{
						PackageSpecification<WebLayoutSpecification> pkg = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().get(
							layoutContainer.getPackageName());
						if (pkg != null)
						{
							propertyDescription = pkg.getSpecification(layoutContainer.getSpecName());
						}
					}

					if (propertyDescription != null)
					{
						persistProperties = new WebComponentPropertySource(persistContext, false, propertyDescription);
					}
				}

				if (persistProperties == null)
				{
					Form form = (Form)persistContext.getContext().getAncestor(IRepository.FORMS);
					boolean mobile = form != null && form.getCustomMobileProperty(IMobileProperties.MOBILE_FORM.propertyName) != null;
					if (!mobile)
					{
						Solution solution = (Solution)persistContext.getContext().getAncestor(IRepository.SOLUTIONS);
						mobile = solution != null && solution.getSolutionType() == SolutionMetaData.MOBILE;
					}
					if (mobile)
					{
						// mobile form or mobile solution
						persistProperties = new MobilePersistPropertySource(persistContext, false);
					}
					else
					{
						// regular
						persistProperties = new PersistPropertySource(persistContext, false);
					}
				}

				if (autoSave)
				{
					// all changes are saved immediately, on the persist node only (not recursive)
					return new SavingPersistPropertySource(persistProperties, servoyProject);
				}

				if (retargetToEditor)
				{
					// save actions are retargeted to the editor that handles the persist type
					return new RetargetToEditorPersistProperties(persistProperties);
				}
				return persistProperties;
			}
			if (key == IResource.class)
			{
				return getResource(persist);
			}
			if (key == Openable.class)
			{
				return Openable.getOpenable(persist);
			}
		}

		return null;
	}

	private static FormElementGroup getEditingFormElementGroup(FormElementGroup group)
	{
		if (group == null)
		{
			return null;
		}

		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(group.getParent().getRootObject().getName());
		if (servoyProject == null)
		{
			ServoyLog.logError("Cannot find Servoy project for persist " + group.getParent(), null);
			return null;
		}
		// make sure we have the in-memory editing version, the real solution is read-only
		return new FormElementGroup(group.getGroupID(), servoyProject.getEditingFlattenedSolution(),
			AbstractRepository.searchPersist(servoyProject.getEditingSolution(), group.getParent()));
	}

	private static MobileListModel getEditingMobileListModel(MobileListModel model)
	{
		if (model == null)
		{
			return null;
		}

		return new MobileListModel(getEditingPersist(model.form), getEditingPersist(model.component), getEditingPersist(model.header),
			getEditingPersist(model.button), getEditingPersist(model.subtext), getEditingPersist(model.countBubble), getEditingPersist(model.image));
	}

	private static <T extends IPersist> T getEditingPersist(T persist)
	{
		if (persist == null)
		{
			return null;
		}

		if (persist instanceof WebCustomType) return persist;
		if (persist instanceof WebFormComponentChildType) return persist;
		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(persist.getRootObject().getName());
		if (servoyProject == null)
		{
			ServoyLog.logError("Cannot find Servoy project for persist " + persist, null);
			return null;
		}
		// make sure we have the in-memory editing version, the real solution is read-only
		return AbstractRepository.searchPersist(servoyProject.getEditingSolution(), persist);
	}

	/**
	 * @param persist
	 * @return
	 */
	private IResource getResource(IPersist persist)
	{
		Pair<String, String> filePath = SolutionSerializer.getFilePath(persist, true);
		return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(filePath.getLeft() + filePath.getRight()));
	}

	private IModelSavePropertySource createFormElementGroupPropertySource(FormElementGroup group, Form context)
	{
		Form form = group.getParent();
		FormElementGroup editingGroup = getEditingFormElementGroup(group);
		boolean mobile = form != null && form.getCustomMobileProperty(IMobileProperties.MOBILE_FORM.propertyName) != null;
		return mobile ? new MobileComponentWithTitlePropertySource(editingGroup, context) : new FormElementGroupPropertySource(editingGroup, context);
	}

	/**
	 * @param editPart
	 * @return
	 */
	private Form getEditpartFormContext(EditPart editPart)
	{
		EditPart formEditPart = editPart;
		while (formEditPart != null)
		{
			if (formEditPart instanceof IPersistEditPart)
			{
				IPersist persist = ((IPersistEditPart)formEditPart).getPersist();
				if (persist instanceof Form)
				{
					return (Form)persist;
				}
			}
			formEditPart = formEditPart.getParent();
		}
		return null;
	}
}
