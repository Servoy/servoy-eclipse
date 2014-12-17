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
package com.servoy.eclipse.designer.editor;

import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.swt.graphics.Point;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.designer.actions.SetPropertyRequest;
import com.servoy.eclipse.designer.editor.commands.DataFieldRequest;
import com.servoy.eclipse.designer.editor.commands.DataRequest;
import com.servoy.eclipse.designer.editor.commands.FormElementDeleteCommand;
import com.servoy.eclipse.designer.editor.commands.FormPlaceElementCommand;
import com.servoy.eclipse.designer.editor.commands.FormPlaceFieldCommand;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.dnd.FormElementDragData.DataProviderDragData;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.dnd.IDragData;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportMedia;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.ValueList;

/**
 * This edit policy enables the removal and copy of a Form elements
 *
 * @author rgansevles
 */
public class PersistEditPolicy extends ComponentEditPolicy
{

	private final IFieldPositioner fieldPositioner;
	private final IApplication application;

	public PersistEditPolicy(IApplication application, IFieldPositioner fieldPositioner)
	{
		this.application = application;
		this.fieldPositioner = fieldPositioner;
	}

	@Override
	public EditPart getTargetEditPart(Request request)
	{
		if (understandsRequest(request))
		{
			return getHost();
		}
		return super.getTargetEditPart(request);
	}

	@Override
	public Command getCommand(Request request)
	{
		IPersist persist = (IPersist)getHost().getModel();
		EditPart formEditPart = getHost().getParent();
		while (formEditPart != null && !(formEditPart.getModel() instanceof Form))
		{
			formEditPart = formEditPart.getParent();
		}

		if (BaseVisualFormEditor.REQ_DROP_LINK.equals(request.getType()) && request instanceof DataRequest)
		{
			if (((DataRequest)request).getData() instanceof IDragData[] && ((IDragData[])((DataRequest)request).getData()).length > 0)
			{
				IDragData dragData = ((IDragData[])((DataRequest)request).getData())[0];
				if (dragData instanceof PersistDragData &&
					((persist instanceof Field || persist instanceof GraphicalComponent || persist instanceof ISupportMedia) || (((PersistDragData)dragData).type == IRepository.FORMS && persist.getAncestor(IRepository.TABPANELS) != null)))
				{
					return createDropPersistCommand((DataRequest)request);
				}
				if (dragData instanceof DataProviderDragData && persist instanceof ISupportDataProviderID)
				{
					return createDropColumnCommand((DataRequest)request);
				}
			}
			else if (((DataRequest)request).getData() instanceof TemplateElementHolder)
			{
				return createDropTemplateCommand((DataRequest)request);
			}
		}

		Command command = null;
		if (persist instanceof Portal &&
			(BaseVisualFormEditor.REQ_DROP_COPY.equals(request.getType()) || VisualFormEditor.REQ_PLACE_FIELD.equals(request.getType()) ||
				VisualFormEditor.REQ_PLACE_MEDIA.equals(request.getType()) || VisualFormEditor.REQ_PLACE_BUTTON.equals(request.getType()) ||
				VisualFormEditor.REQ_PLACE_LABEL.equals(request.getType()) || VisualFormEditor.REQ_PLACE_RECT_SHAPE.equals(request.getType())))
		{
			Portal portal = (Portal)persist;
			java.awt.Point portalLocation = portal.getLocation();
			Point fieldsLocation;
			if (request instanceof DataRequest && ((DataRequest)request).getlocation() != null)
			{
				fieldsLocation = ((DataRequest)request).getlocation().getSWTPoint();
			}
			else
			{
				fieldsLocation = new Point((portalLocation == null ? 0 : portalLocation.x) + 10, (portalLocation == null ? 0 : portalLocation.y) + 10);
			}

			Object data = request instanceof DataRequest ? ((DataRequest)request).getData() : null;
			if (VisualFormEditor.REQ_PLACE_FIELD.equals(request.getType()))
			{
				DataFieldRequest dataFieldRequest = ((DataFieldRequest)request);
				command = new FormPlaceFieldCommand(application, portal, (IPersist)formEditPart.getModel(), dataFieldRequest.getData(),
					dataFieldRequest.getType(), dataFieldRequest.getExtendedData(), fieldPositioner, fieldsLocation, null, dataFieldRequest.placeAsLabels,
					dataFieldRequest.placeWithLabels, dataFieldRequest.placeHorizontal, dataFieldRequest.fillText, dataFieldRequest.fillName,
					(IPersist)(formEditPart == null ? null : formEditPart.getModel()));
			}
			else
			{
				// other element
				command = new FormPlaceElementCommand(application, portal, data, request.getType(), request.getExtendedData(), fieldPositioner, fieldsLocation,
					null, (IPersist)(formEditPart == null ? null : formEditPart.getModel()));
			}
		}

		else if (persist instanceof TabPanel && VisualFormEditor.REQ_PLACE_TAB.equals(request.getType()) && request instanceof DataRequest)
		{
			// add tab to existing tab panel
			command = new FormPlaceElementCommand(application, (TabPanel)persist, ((DataRequest)request).getData(), request.getType(),
				request.getExtendedData(), null, null, null, (IPersist)(formEditPart == null ? null : formEditPart.getModel()));
		}
		else if ((BaseVisualFormEditor.REQ_SET_PROPERTY.equals(request.getType()) && request instanceof SetPropertyRequest))
		{
			SetPropertyRequest setPropertyRequest = (SetPropertyRequest)request;
			command = SetValueCommand.createSetvalueCommand(setPropertyRequest.getName(),
				PersistPropertySource.createPersistPropertySource(persist, formEditPart != null ? (Form)formEditPart.getModel() : null, false),
				setPropertyRequest.getPropertyId(), setPropertyRequest.getValue());
		}

		if (command != null)
		{
			return command;
		}

		return super.getCommand(request);
	}

	@Override
	public boolean understandsRequest(Request request)
	{
		Object model = getHost().getModel();
		if (BaseVisualFormEditor.REQ_DROP_LINK.equals(request.getType()) && request instanceof DataRequest)
		{
			if (((DataRequest)request).getData() instanceof IDragData[] && ((IDragData[])((DataRequest)request).getData()).length > 0)
			{
				IDragData dragData = ((IDragData[])((DataRequest)request).getData())[0];
				if (dragData instanceof PersistDragData)
				{
					if (((PersistDragData)dragData).type == IRepository.METHODS && (model instanceof Field || model instanceof GraphicalComponent))
					{
						return true;
					}
					if (((PersistDragData)dragData).type == IRepository.MEDIA && model instanceof ISupportMedia && model instanceof IPersist)
					{
						return true;
					}
					if (((PersistDragData)dragData).type == IRepository.VALUELISTS && model instanceof Field)
					{
						return true;
					}
					if (((PersistDragData)dragData).type == IRepository.FORMS && model instanceof IPersist &&
						((IPersist)model).getAncestor(IRepository.TABPANELS) != null)
					{
						// drop form on tabpanel
						IPersist form = ((IPersist)model).getAncestor(IRepository.FORMS);
						if (form != null)
						{
							return DesignerUtil.isDropFormAllowed(form, (PersistDragData)dragData);
						}

					}
				}

				else if (dragData instanceof DataProviderDragData && model instanceof ISupportDataProviderID)
				{
					return createDropColumnCommand((DataRequest)request) != null;
				}
			}

			else if (((DataRequest)request).getData() instanceof TemplateElementHolder)
			{
				TemplateElementHolder templateHolder = (TemplateElementHolder)((DataRequest)request).getData();
				List<JSONObject> templateElements = templateHolder == null ? null : ElementFactory.getTemplateElements(templateHolder.template,
					templateHolder.element);
				return templateElements != null && templateElements.size() == 1;
			}
		}
		if (model instanceof Portal &&
			(BaseVisualFormEditor.REQ_DROP_COPY.equals(request.getType()) || VisualFormEditor.REQ_PLACE_FIELD.equals(request.getType()) ||
				VisualFormEditor.REQ_PLACE_MEDIA.equals(request.getType()) || VisualFormEditor.REQ_PLACE_BUTTON.equals(request.getType()) ||
				VisualFormEditor.REQ_PLACE_LABEL.equals(request.getType()) || VisualFormEditor.REQ_PLACE_RECT_SHAPE.equals(request.getType())))
		{
			return true;
		}
		if (VisualFormEditor.REQ_PLACE_TAB.equals(request.getType()) && model instanceof TabPanel)
		{
			return true;
		}
		if (BaseVisualFormEditor.REQ_SET_PROPERTY.equals(request.getType()) && request instanceof SetPropertyRequest)
		{
			return true;
		}

		return super.understandsRequest(request);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.gef.editpolicies.ComponentEditPolicy#createDeleteCommand(org.eclipse.gef.requests.GroupRequest)
	 */
	@Override
	protected Command createDeleteCommand(GroupRequest deleteRequest)
	{
		if (getHost() instanceof BasePersistGraphicalEditPart && ((BasePersistGraphicalEditPart)getHost()).isDesignerContextActive())
		{
			Object child = getHost().getModel();
			if (child instanceof IPersist)
			{
				return new FormElementDeleteCommand((IPersist)child);
			}
		}
		return super.createDeleteCommand(deleteRequest);
	}

	protected Command createDropPersistCommand(DataRequest dropRequest)
	{
		Object child = getHost().getModel();
		if (dropRequest.getData() instanceof IDragData[] && ((IDragData[])dropRequest.getData()).length > 0 &&
			((IDragData[])dropRequest.getData())[0] instanceof PersistDragData)
		{
			// determine the editing context, the form we are on
			EditPart formEditPart = getHost().getParent();
			while (formEditPart != null && !(formEditPart.getModel() instanceof Form))
			{
				formEditPart = formEditPart.getParent();
			}

			PersistDragData dragData = (PersistDragData)((IDragData[])dropRequest.getData())[0];
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(dragData.solutionName);
			if (servoyProject == null) return null;
			IPersist persist = servoyProject.getEditingPersist(dragData.uuid);
			if (persist instanceof ScriptMethod && (child instanceof Field || child instanceof GraphicalComponent))
			{
				return SetValueCommand.createSetvalueCommand(
					"Drag-n-drop script method",
					PersistPropertySource.createPersistPropertySource((IPersist)child, (IPersist)(formEditPart == null ? null : formEditPart.getModel()), false),
					StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID.getPropertyName(), MethodWithArguments.create(persist, null));
			}
			if (persist instanceof Media && child instanceof ISupportMedia && child instanceof IPersist)
			{
				Media mediaPersist = (Media)persist;
				String mediaPersistName = mediaPersist.getName();
				return SetValueCommand.createSetvalueCommand("Drag-n-drop image", PersistPropertySource.createPersistPropertySource((IPersist)child,
					(IPersist)(formEditPart == null ? null : formEditPart.getModel()), false), StaticContentSpecLoader.PROPERTY_IMAGEMEDIAID.getPropertyName(),
					new MediaNode(mediaPersistName, mediaPersistName, MediaNode.TYPE.IMAGE, servoyProject.getEditingFlattenedSolution().getSolution(), null,
						mediaPersist));
			}
			if (persist instanceof ValueList && child instanceof Field)
			{
				return SetValueCommand.createSetvalueCommand(
					"Drag-n-drop value list",
					PersistPropertySource.createPersistPropertySource((IPersist)child, (IPersist)(formEditPart == null ? null : formEditPart.getModel()), false),
					StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName(), new Integer(persist.getID()));
			}
			if (persist instanceof Form && child instanceof IPersist && ((IPersist)child).getAncestor(IRepository.TABPANELS) != null)
			{
				return new FormPlaceElementCommand(application, (TabPanel)((IPersist)child).getAncestor(IRepository.TABPANELS),
					new Object[] { new ElementFactory.RelatedForm(null, (Form)persist) }, VisualFormEditor.REQ_PLACE_TAB, null, null,
					dropRequest.getlocation().getSWTPoint(), null, (IPersist)(formEditPart == null ? null : formEditPart.getModel()));
			}
		}
		return null;
	}

	protected Command createDropColumnCommand(DataRequest dropRequest)
	{
		Object child = getHost().getModel();
		if (dropRequest.getData() instanceof IDragData[] && child instanceof ISupportDataProviderID && child instanceof IPersist)
		{
			IDragData[] dragData = (IDragData[])dropRequest.getData();
			if (dragData.length == 0 || !(dragData[0] instanceof DataProviderDragData)) return null;
			DataProviderDragData dataProviderDragData = (DataProviderDragData)dragData[0];
			Form form = (Form)((IPersist)child).getAncestor(IRepository.FORMS);
			if (dataProviderDragData.serverName != null && dataProviderDragData.baseTableName != null)
			{
				if (!dataProviderDragData.serverName.equals(form.getServerName()) || !dataProviderDragData.baseTableName.equals(form.getTableName())) return null;
			}
			// else drop a form or global variable
			return SetValueCommand.createSetvalueCommand("Drag-n-drop data provider", PersistPropertySource.createPersistPropertySource((IPersist)child, form,
				false), StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(), dataProviderDragData.relationName == null
				? dataProviderDragData.dataProviderId : (dataProviderDragData.relationName + '.' + dataProviderDragData.dataProviderId));
		}
		return null;
	}

	protected Command createDropTemplateCommand(DataRequest dropRequest)
	{
		Object child = getHost().getModel();
		if (child instanceof IPersist)
		{
			return new ApplyTemplatePropertiesCommand((TemplateElementHolder)dropRequest.getData(), PersistContext.create((IPersist)child,
				(IPersist)getHost().getParent().getModel()));
		}

		return null;
	}
}
