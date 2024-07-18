/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.ui.dialogs.autowizard.nattable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractUiBindingConfiguration;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.ui.action.IKeyAction;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.CellLabelMouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.KeyEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;

/**
 * @author emera
 */
public class LinkClickConfiguration extends AbstractUiBindingConfiguration implements IMouseAction, IKeyAction
{
	class LinkCellLabelKeyMatcher extends KeyEventMatcher
	{
		public LinkCellLabelKeyMatcher(int keyCode)
		{
			super(keyCode);
		}

		@Override
		public boolean matches(KeyEvent event)
		{
			Collection<ILayerCell> selectedCells = selectionLayer.getSelectedCells();
			if (selectedCells.size() == 1)
			{
				ILayerCell cell = selectedCells.iterator().next();
				return super.matches(event) && cell.getConfigLabels().contains(LINK_CELL_LABEL);
			}
			return false;
		}
	}

	public static final String LINK_CELL_LABEL = "LINK_CELL_LABEL";
	private final List<IMouseAction> clickListeners = new ArrayList<>();
	private final SelectionLayer selectionLayer;
	private final List<IKeyAction> keyListeners = new ArrayList<>();

	public LinkClickConfiguration(SelectionLayer selectionLayer)
	{
		this.selectionLayer = selectionLayer;
	}

	/**
	 * Configure the UI bindings for the mouse click
	 */
	@Override
	public void configureUiBindings(UiBindingRegistry uiBindingRegistry)
	{
		// Match a mouse event on the body, when the left button is clicked
		// and the custom cell label is present
		CellLabelMouseEventMatcher mouseEventMatcher = new CellLabelMouseEventMatcher(
			GridRegion.BODY,
			MouseEventMatcher.LEFT_BUTTON,
			LINK_CELL_LABEL);

		CellLabelMouseEventMatcher mouseHoverMatcher = new CellLabelMouseEventMatcher(GridRegion.BODY, 0, LINK_CELL_LABEL);

		// Inform the button painter of the click.
		uiBindingRegistry.registerFirstMouseDownBinding(mouseEventMatcher, this);

		// show hand cursor, which is usually used for links
		uiBindingRegistry.registerFirstMouseMoveBinding(mouseHoverMatcher, (natTable, event) -> {
			natTable.setCursor(natTable.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		});

		uiBindingRegistry.registerFirstKeyBinding(new LinkCellLabelKeyMatcher(SWT.SPACE), this);
		uiBindingRegistry.registerFirstKeyBinding(new LinkCellLabelKeyMatcher(SWT.F2), this);
	}

	@Override
	public void run(NatTable natTable, KeyEvent event)
	{
		for (IKeyAction listener : this.keyListeners)
		{
			listener.run(natTable, event);
		}
	}

	@Override
	public void run(final NatTable natTable, MouseEvent event)
	{
		for (IMouseAction listener : this.clickListeners)
		{
			listener.run(natTable, event);
		}
	}

	public void addKeyListener(IKeyAction keyAction)
	{
		this.keyListeners.add(keyAction);
	}

	public void removeKeyListener(IKeyAction keyAction)
	{
		this.keyListeners.remove(keyAction);
	}

	public void addClickListener(IMouseAction mouseAction)
	{
		this.clickListeners.add(mouseAction);
	}

	public void removeClickListener(IMouseAction mouseAction)
	{
		this.clickListeners.remove(mouseAction);
	}

	public SelectionLayer getSelectionLayer()
	{
		return selectionLayer;
	}
}