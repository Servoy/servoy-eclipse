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

package com.servoy.eclipse.designer.editor.mobile.editparts;

import java.util.Collection;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.Activator;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.SetBoundsToSupportBoundsFigureListener;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;

/**
 * Edit part for items inside the inset list in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileListElementEditpart extends AbstractGraphicalEditPart implements IPersistChangeListener
{
	public static final Image COUNTBUBBLE_IMAGE = Activator.loadImageDescriptorFromBundle("list_countbubble.png").createImage(); //$NON-NLS-1$
	public static final Image IMAGE_IMAGE = Activator.loadImageDescriptorFromBundle("image.gif").createImage(); //$NON-NLS-1$

	public static enum MobileListElementType
	{
		Header, Button, Subtext, CountBubble, Image
	}

	protected IApplication application;
	private final BaseVisualFormEditor editorPart;
	private final MobileListElementType type;

	public MobileListElementEditpart(IApplication application, BaseVisualFormEditor editorPart, Object model, MobileListElementType type)
	{
		this.application = application;
		this.editorPart = editorPart;
		this.type = type;
		setModel(model);
	}

	/**
	 * @return the type
	 */
	public MobileListElementType getType()
	{
		return type;
	}

	/**
	 * @return the editorPart
	 */
	public BaseVisualFormEditor getEditorPart()
	{
		return editorPart;
	}

	@Override
	protected void createEditPolicies()
	{
	}

	@Override
	protected IFigure createFigure()
	{
		IFigure fig;
		switch (type)
		{
			case Image :
				fig = new ImageFigure(IMAGE_IMAGE);
				((ImageFigure)fig).setAlignment(PositionConstants.WEST);
				break;

			case Header :
				fig = new Label();
				((Label)fig).setLabelAlignment(PositionConstants.CENTER);
				fig.setBackgroundColor(MobilePartFigure.HEADER_COLOR);// TODO: use scheme
				break;

			case Button :
				fig = new Label();
				((Label)fig).setLabelAlignment(PositionConstants.LEFT);
				break;

			case Subtext :
				fig = new Label();
				((Label)fig).setLabelAlignment(PositionConstants.LEFT);
				break;

			case CountBubble :
				fig = new ImageFigure(COUNTBUBBLE_IMAGE);
				((ImageFigure)fig).setAlignment(PositionConstants.EAST);
				break;

			default :
				return null;
		}

		fig.setOpaque(type == MobileListElementType.Header);

//		LineBorder border = new LineBorder(1);
//		border.setColor(ColorConstants.white);
//		border.setStyle(Graphics.LINE_DASH);
//		fig.setBorder(border);

		if (getModel() instanceof ISupportBounds)
		{
			fig.addFigureListener(new SetBoundsToSupportBoundsFigureListener((ISupportBounds)getModel()));
		}
		return fig;
	}

	/*
	 * Apply dynamic changes to the figure, like text from a property.
	 */
	public void updateFigure(IFigure fig)
	{
		switch (type)
		{
			case Header :
				updateFigureForGC(fig, (GraphicalComponent)getModel(), "<header>");
				break;

			case Button :
				updateFigureForGC(fig, (GraphicalComponent)getModel(), "<button>");
				break;

			case Subtext :
				updateFigureForGC(fig, (GraphicalComponent)getModel(), "<subtext>");
				break;

			default :
		}
	}

	/**
	 * @param fig
	 * @param button
	 */
	private void updateFigureForGC(IFigure fig, GraphicalComponent button, String defaultText)
	{
		if (button.getDataProviderID() != null)
		{
			((Label)fig).setText(button.getDataProviderID());
			((Label)fig).setFont(FontResource.getDefaultFont(SWT.BOLD, -2));
		}
		else
		{
			String text = button.getText();
			((Label)fig).setText(text == null ? defaultText : text);
			((Label)fig).setFont(FontResource.getDefaultFont(text == null ? SWT.ITALIC : SWT.NORMAL, 0));
		}
	}

	@Override
	public void refresh()
	{
		super.refresh();
		if (figure != null)
		{
			updateFigure(figure);
		}
	}

	@Override
	public boolean isSelectable()
	{
		// select parent
		return false;
	}

	@Override
	public void activate()
	{
		// listen to changes to the elements
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.addPersistChangeListener(false, this);

		super.activate();
	}

	@Override
	public void deactivate()
	{
		// stop listening to changes to the elements
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.removePersistChangeListener(false, this);

		super.deactivate();
	}

	// If the form changed width we have to refresh
	public void persistChanges(Collection<IPersist> changes)
	{
		for (IPersist persist : changes)
		{
			// TODO: check for containing form in inset list //  if (getEditorPart().getForm().equals(persist.getAncestor(IRepository.FORMS)))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						refresh();
					}
				});
				return;
			}
		}
	}

}
