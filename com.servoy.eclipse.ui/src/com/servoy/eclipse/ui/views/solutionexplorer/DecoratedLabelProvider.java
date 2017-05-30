/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;

public class DecoratedLabelProvider extends DecoratingStyledCellLabelProvider implements IPropertyChangeListener, ILabelProvider, ITableLabelProvider
{

	private static class StyledLabelProviderAdapter implements IStyledLabelProvider, ITableLabelProvider, IColorProvider, IFontProvider
	{

		private final ILabelProvider provider;

		public StyledLabelProviderAdapter(ILabelProvider provider)
		{
			this.provider = provider;
		}

		@Override
		public Image getImage(Object element)
		{
			return provider.getImage(element);
		}

		@Override
		public StyledString getStyledText(Object element)
		{
			if (provider instanceof IStyledLabelProvider)
			{
				return ((IStyledLabelProvider)provider).getStyledText(element);
			}
			String text = provider.getText(element);
			if (text == null) text = ""; //$NON-NLS-1$
			return new StyledString(text);
		}

		@Override
		public void addListener(ILabelProviderListener listener)
		{
			provider.addListener(listener);
		}

		@Override
		public void dispose()
		{
			provider.dispose();
		}

		@Override
		public boolean isLabelProperty(Object element, String property)
		{
			return provider.isLabelProperty(element, property);
		}

		@Override
		public void removeListener(ILabelProviderListener listener)
		{
			provider.removeListener(listener);
		}

		@Override
		public Color getBackground(Object element)
		{
			if (provider instanceof IColorProvider)
			{
				return ((IColorProvider)provider).getBackground(element);
			}
			return null;
		}

		@Override
		public Color getForeground(Object element)
		{
			if (provider instanceof IColorProvider)
			{
				return ((IColorProvider)provider).getForeground(element);
			}
			return null;
		}

		@Override
		public Font getFont(Object element)
		{
			if (provider instanceof IFontProvider)
			{
				return ((IFontProvider)provider).getFont(element);
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex)
		{
			if (provider instanceof ITableLabelProvider)
			{
				return ((ITableLabelProvider)provider).getColumnImage(element, columnIndex);
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex)
		{
			if (provider instanceof ITableLabelProvider)
			{
				return ((ITableLabelProvider)provider).getColumnText(element, columnIndex);
			}
			return null;
		}
	}

	/**
	 * Creates a {@link DecoratedLabelProvider}
	 *
	 * @param commonLabelProvider the label provider to use
	 * @param formViewLabelDecorator
	 */
	public DecoratedLabelProvider(ILabelProvider commonLabelProvider)
	{
		super(new StyledLabelProviderAdapter(commonLabelProvider), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator(), null);
	}

	@Override
	public void initialize(ColumnViewer viewer, ViewerColumn column)
	{
		PlatformUI.getPreferenceStore().addPropertyChangeListener(this);
		JFaceResources.getColorRegistry().addListener(this);

		setOwnerDrawEnabled(showColoredLabels());

		super.initialize(viewer, column);
	}

	@Override
	public void dispose()
	{
		super.dispose();
		PlatformUI.getPreferenceStore().removePropertyChangeListener(this);
		JFaceResources.getColorRegistry().removeListener(this);
	}

	private void refresh()
	{
		ColumnViewer viewer = getViewer();
		if (viewer == null)
		{
			return;
		}
		boolean showColoredLabels = showColoredLabels();
		if (showColoredLabels != isOwnerDrawEnabled())
		{
			setOwnerDrawEnabled(showColoredLabels);
			viewer.refresh();
		}
		else if (showColoredLabels)
		{
			viewer.refresh();
		}
	}

	private static boolean showColoredLabels()
	{
		return true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event)
	{
		String property = event.getProperty();
		if (property.equals(JFacePreferences.QUALIFIER_COLOR) || property.equals(JFacePreferences.COUNTER_COLOR) ||
			property.equals(JFacePreferences.DECORATIONS_COLOR) || property.equals(IWorkbenchPreferenceConstants.USE_COLORED_LABELS))
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				@Override
				public void run()
				{
					refresh();
				}
			});
		}
	}

	@Override
	public String getText(Object element)
	{
		return getStyledText(element).getString();
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex)
	{
		return ((StyledLabelProviderAdapter)getStyledStringProvider()).getColumnImage(element, columnIndex);
	}

	@Override
	public String getColumnText(Object element, int columnIndex)
	{
		return ((StyledLabelProviderAdapter)getStyledStringProvider()).getColumnText(element, columnIndex);
	}
}