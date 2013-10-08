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
package com.servoy.eclipse.ui.views.solutionexplorer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.util.IDelegate;

/**
 * A decorating column label provider is a label provider which combines a nested column label provider and an optional decorator. The decorator decorates the
 * label text, image, font and colors provided by the nested label provider.
 */
public class DecoratingColumnLabelProvider extends ColumnLabelProvider implements IDelegate<ColumnLabelProvider>
{

	private final ColumnLabelProvider provider;

	private ILabelDecorator decorator;

	// Need to keep our own list of listeners
	private final ListenerList listeners = new ListenerList();

	private IDecorationContext decorationContext = DecorationContext.DEFAULT_CONTEXT;

	/**
	 * Creates a decorating label provider which uses the given label decorator to decorate labels provided by the given label provider.
	 * 
	 * @param provider the nested label provider
	 * @param decorator the label decorator, or <code>null</code> if no decorator is to be used initially
	 */
	public DecoratingColumnLabelProvider(ColumnLabelProvider provider, ILabelDecorator decorator)
	{
		Assert.isNotNull(provider);
		this.provider = provider;
		this.decorator = decorator;
	}

	/**
	 * The <code>DecoratingLabelProvider</code> implementation of this <code>IBaseLabelProvider</code> method adds the listener to both the nested label
	 * provider and the label decorator.
	 * 
	 * @param listener a label provider listener
	 */
	@Override
	public void addListener(ILabelProviderListener listener)
	{
		super.addListener(listener);
		provider.addListener(listener);
		if (decorator != null)
		{
			decorator.addListener(listener);
		}
		listeners.add(listener);
	}

	/**
	 * The <code>DecoratingLabelProvider</code> implementation of this <code>IBaseLabelProvider</code> method disposes both the nested label provider and
	 * the label decorator.
	 */
	@Override
	public void dispose()
	{
		provider.dispose();
		if (decorator != null)
		{
			decorator.dispose();
		}
		super.dispose();
	}

	/**
	 * The <code>DecoratingLabelProvider</code> implementation of this <code>ILabelProvider</code> method returns the image provided by the nested label
	 * provider's <code>getImage</code> method, decorated with the decoration provided by the label decorator's <code>decorateImage</code> method.
	 */
	@Override
	public Image getImage(Object element)
	{
		Image image = provider.getImage(element);

		if (element instanceof PlatformSimpleUserNode)
		{
			if (((PlatformSimpleUserNode)element).getType() == UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE) return image;
		}

		if (decorator != null)
		{
			if (decorator instanceof LabelDecorator)
			{
				LabelDecorator ld2 = (LabelDecorator)decorator;
				Image decorated = ld2.decorateImage(image, element, getDecorationContext());
				if (decorated != null)
				{
					return decorated;
				}
			}
			else
			{
				Image decorated = decorator.decorateImage(image, element);
				if (decorated != null)
				{
					return decorated;
				}
			}
		}
		return image;
	}

	/**
	 * Returns the label decorator, or <code>null</code> if none has been set.
	 * 
	 * @return the label decorator, or <code>null</code> if none has been set.
	 */
	public ILabelDecorator getLabelDecorator()
	{
		return decorator;
	}

	/**
	 * Returns the nested label provider.
	 * 
	 * @return the nested label provider
	 */
	@Override
	public ColumnLabelProvider getDelegate()
	{
		return provider;
	}

	/**
	 * The <code>DecoratingLabelProvider</code> implementation of this <code>ILabelProvider</code> method returns the text label provided by the nested
	 * label provider's <code>getText</code> method, decorated with the decoration provided by the label decorator's <code>decorateText</code> method.
	 */
	@Override
	public String getText(Object element)
	{
		String text = provider.getText(element);
		if (decorator != null)
		{
			if (decorator instanceof LabelDecorator)
			{
				LabelDecorator ld2 = (LabelDecorator)decorator;
				String decorated = ld2.decorateText(text, element, getDecorationContext());
				if (decorated != null)
				{
					return decorated;
				}
			}
			else
			{
				String decorated = decorator.decorateText(text, element);
				if (decorated != null)
				{
					return decorated;
				}
			}
		}
		return text;
	}

	/**
	 * The <code>DecoratingLabelProvider</code> implementation of this <code>IBaseLabelProvider</code> method returns <code>true</code> if the
	 * corresponding method on the nested label provider returns <code>true</code> or if the corresponding method on the decorator returns <code>true</code>.
	 */
	@Override
	public boolean isLabelProperty(Object element, String property)
	{
		if (provider.isLabelProperty(element, property))
		{
			return true;
		}
		if (decorator != null && decorator.isLabelProperty(element, property))
		{
			return true;
		}
		return false;
	}

	/**
	 * The <code>DecoratingLabelProvider</code> implementation of this <code>IBaseLabelProvider</code> method removes the listener from both the nested
	 * label provider and the label decorator.
	 * 
	 * @param listener a label provider listener
	 */
	@Override
	public void removeListener(ILabelProviderListener listener)
	{
		super.removeListener(listener);
		provider.removeListener(listener);
		if (decorator != null)
		{
			decorator.removeListener(listener);
		}
		listeners.remove(listener);
	}

	/**
	 * Sets the label decorator. Removes all known listeners from the old decorator, and adds all known listeners to the new decorator. The old decorator is not
	 * disposed. Fires a label provider changed event indicating that all labels should be updated. Has no effect if the given decorator is identical to the
	 * current one.
	 * 
	 * @param decorator the label decorator, or <code>null</code> if no decorations are to be applied
	 */
	public void setLabelDecorator(ILabelDecorator decorator)
	{
		ILabelDecorator oldDecorator = this.decorator;
		if (oldDecorator != decorator)
		{
			Object[] listenerList = this.listeners.getListeners();
			if (oldDecorator != null)
			{
				for (Object element : listenerList)
				{
					oldDecorator.removeListener((ILabelProviderListener)element);
				}
			}
			this.decorator = decorator;
			if (decorator != null)
			{
				for (Object element : listenerList)
				{
					decorator.addListener((ILabelProviderListener)element);
				}
			}
			fireLabelProviderChanged(new LabelProviderChangedEvent(this));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
	@Override
	public Color getBackground(Object element)
	{
		return ((IColorProvider)provider).getBackground(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
	 */
	@Override
	public Font getFont(Object element)
	{
		return ((IFontProvider)provider).getFont(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	@Override
	public Color getForeground(Object element)
	{
		return ((IColorProvider)provider).getForeground(element);
	}

	/**
	 * Return the decoration context associated with this label provider. It will be passed to the decorator if the decorator is an instance of
	 * {@link LabelDecorator}.
	 * 
	 * @return the decoration context associated with this label provider
	 * 
	 * @since 3.2
	 */
	public IDecorationContext getDecorationContext()
	{
		return decorationContext;
	}

	/**
	 * Set the decoration context that will be based to the decorator for this label provider if that decorator implements {@link LabelDecorator}.
	 * 
	 * @param decorationContext the decoration context.
	 * 
	 * @since 3.2
	 */
	public void setDecorationContext(IDecorationContext decorationContext)
	{
		Assert.isNotNull(decorationContext);
		this.decorationContext = decorationContext;
	}

	@Override
	public void update(ViewerCell cell)
	{
		Object element = cell.getElement();
		String oldText = cell.getText();
		ILabelDecorator currentDecorator = getLabelDecorator();
		if (currentDecorator instanceof LabelDecorator)
		{
			LabelDecorator labelDecorator = (LabelDecorator)currentDecorator;
			labelDecorator.prepareDecoration(element, oldText, getDecorationContext());
		}

		cell.setBackground(getBackground(element));
		cell.setForeground(getForeground(element));
		cell.setFont(getFont(element));

		super.update(cell);
	}


	@Override
	public Image getToolTipImage(Object object)
	{
		return provider.getToolTipImage(object);
	}

	@Override
	public String getToolTipText(Object element)
	{
		return provider.getToolTipText(element);
	}

	@Override
	public Color getToolTipBackgroundColor(Object object)
	{
		return provider.getToolTipBackgroundColor(object);
	}

	@Override
	public Color getToolTipForegroundColor(Object object)
	{
		return provider.getToolTipForegroundColor(object);
	}

	@Override
	public Font getToolTipFont(Object object)
	{
		return provider.getToolTipFont(object);
	}

	@Override
	public Point getToolTipShift(Object object)
	{
		return provider.getToolTipShift(object);
	}

	@Override
	public boolean useNativeToolTip(Object object)
	{
		return provider.useNativeToolTip(object);
	}

	@Override
	public int getToolTipTimeDisplayed(Object object)
	{
		return provider.getToolTipTimeDisplayed(object);
	}

	@Override
	public int getToolTipDisplayDelayTime(Object object)
	{
		return provider.getToolTipDisplayDelayTime(object);
	}

	@Override
	public int getToolTipStyle(Object object)
	{
		return provider.getToolTipStyle(object);
	}
}
