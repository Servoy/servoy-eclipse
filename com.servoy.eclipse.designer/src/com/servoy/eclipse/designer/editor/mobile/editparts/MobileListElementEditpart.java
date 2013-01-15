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

import java.awt.Color;
import java.util.Collection;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.xhtmlrenderer.css.constants.CSSName;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.Activator;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.SetBoundsToSupportBoundsFigureListener;
import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.resource.ImageResource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.Pair;

/**
 * Edit part for items inside the inset list in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileListElementEditpart extends AbstractGraphicalEditPart implements IPersistChangeListener
{
	public static final Image COUNTBUBBLE_IMAGE = Activator.loadImageDescriptorFromBundle("mobile/list_countbubble.png").createImage(); //$NON-NLS-1$
	public static final Image IMAGE_IMAGE = Activator.loadImageDescriptorFromBundle("image.gif").createImage(); //$NON-NLS-1$

	public static enum MobileListElementType
	{
		Header, Button, Subtext, CountBubble, Image, Icon
	}

	protected IApplication application;
	private final BaseVisualFormEditor editorPart;

	public MobileListElementEditpart(IApplication application, BaseVisualFormEditor editorPart, Pair<BaseComponent, MobileListElementType> model)
	{
		this.application = application;
		this.editorPart = editorPart;
		setModel(model);
	}

	@Override
	public Pair<BaseComponent, MobileListElementType> getModel()
	{
		return (Pair<BaseComponent, MobileListElementType>)super.getModel();
	}

	/**
	 * @return the type
	 */
	public MobileListElementType getType()
	{
		return getModel().getRight();
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
		MobileListElementType type = getType();
		switch (type)
		{
			case Image :
			case Icon :
				fig = new ImageFigure(IMAGE_IMAGE);
				((ImageFigure)fig).setAlignment(PositionConstants.WEST);
				break;

			case Header :
			case Button :
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
		fig.addFigureListener(new SetBoundsToSupportBoundsFigureListener(editorPart.getForm(), getModel().getLeft(), false));

		return fig;
	}

	public static org.eclipse.swt.graphics.Color getGcColor(GraphicalComponent gc, IApplication application, Form form, Color def)
	{
		Color awtColor = null;
		if (gc.getBackground() != null)
		{
			awtColor = gc.getBackground();
		}
		else
		{
			Pair<IStyleSheet, IStyleRule> elementStyle = ComponentFactory.getStyleForBasicComponent(application, gc, form);
			if (elementStyle != null && elementStyle.getRight() != null && elementStyle.getRight().hasAttribute(CSSName.BACKGROUND_COLOR.toString()))
			{
				awtColor = elementStyle.getLeft().getBackground(elementStyle.getRight());
			}
		}

		return ColorResource.INSTANCE.getColor(ColorResource.ColorAwt2Rgb(awtColor == null ? def : awtColor));
	}

	/*
	 * Apply dynamic changes to the figure, like text from a property.
	 */
	public void updateFigure(IFigure fig)
	{
		switch (getType())
		{
			case Header :
				updateFigureForGC(fig, (GraphicalComponent)getModel().getLeft(), "<header>");
				fig.setBackgroundColor(getGcColor((GraphicalComponent)getModel().getLeft(), application, editorPart.getForm(), Color.white));
				break;

			case Button :
				updateFigureForGC(fig, (GraphicalComponent)getModel().getLeft(), "<button>");
				break;

			case Subtext :
				updateFigureForGC(fig, (GraphicalComponent)getModel().getLeft(), "<subtext>");
				break;

			case Icon :
				updateIcon((ImageFigure)fig, (GraphicalComponent)getModel().getLeft());
				break;

			default :
		}
	}

	/**
	 * @param fig
	 * @param gc
	 */
	private void updateFigureForGC(IFigure fig, GraphicalComponent gc, String defaultText)
	{
		if (gc.getDataProviderID() != null)
		{
			String text = gc.getDataProviderID();
			if (gc.getParent() instanceof Portal)
			{
				Portal p = (Portal)gc.getParent();
				if (p.getRelationName() != null && text.startsWith(p.getRelationName() + '.'))
				{
					text = text.substring(p.getRelationName().length() + 1);
				}
			}

			((Label)fig).setText(text);
			((Label)fig).setFont(FontResource.getDefaultFont(SWT.BOLD, -2));
		}
		else
		{
			String text = gc.getText();
			((Label)fig).setText(text == null ? defaultText : text);
			((Label)fig).setFont(FontResource.getDefaultFont(text == null ? SWT.ITALIC : SWT.NORMAL, 0));
		}
	}

	private void updateIcon(ImageFigure fig, GraphicalComponent button)
	{
		if (getType() == MobileListElementType.Icon)
		{
			Image image = null;
			String dataIcon = (String)button.getCustomMobileProperty(IMobileProperties.DATA_ICON.propertyName);
			if (dataIcon == null)
			{
				dataIcon = "arrow-r"; // default
			}
			image = ImageResource.INSTANCE.getImageWithRoundBackground(
				Activator.loadImageDescriptorFromBundle("mobile/icons-18-white-" + dataIcon + ".png"),
				new RGB(IconWithRoundBackground.DATA_ICON_BG.getRed(), IconWithRoundBackground.DATA_ICON_BG.getGreen(),
					IconWithRoundBackground.DATA_ICON_BG.getBlue()));
			fig.setImage(image);
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
		ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(false, this);

		super.activate();
	}

	@Override
	public void deactivate()
	{
		// stop listening to changes to the elements
		ServoyModelManager.getServoyModelManager().getServoyModel().removePersistChangeListener(false, this);

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
						if (getParent() != null) refresh();
					}
				});
				return;
			}
		}
	}

}
