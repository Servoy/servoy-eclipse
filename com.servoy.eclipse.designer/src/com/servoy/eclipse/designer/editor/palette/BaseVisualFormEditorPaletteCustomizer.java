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

package com.servoy.eclipse.designer.editor.palette;

import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.palette.PaletteCustomizer;

import com.servoy.eclipse.designer.editor.IPaletteFactory;
import com.servoy.eclipse.ui.preferences.DesignerPreferences.PaletteCustomization;

/**
 * Base customizer for palette with VisualFormEditor.
 * 
 * @author rgansevles
 *
 */
public abstract class BaseVisualFormEditorPaletteCustomizer extends PaletteCustomizer
{
	private final IPaletteFactory paletteFactory;
	private final PaletteRoot paletteRoot;
	private PaletteCustomization savedPaletteCustomization;

	/**
	 * @param paletteRoot
	 */
	public BaseVisualFormEditorPaletteCustomizer(IPaletteFactory paletteFactory, PaletteRoot paletteRoot)
	{
		this.paletteFactory = paletteFactory;
		this.paletteRoot = paletteRoot;
	}

	public void initialize()
	{
		savedPaletteCustomization = createSavedPaletteCustomization();
	}

	protected abstract PaletteCustomization createSavedPaletteCustomization();

	/**
	 * @return the paletteFactory
	 */
	public IPaletteFactory getPaletteFactory()
	{
		return paletteFactory;
	}

	@Override
	public void revertToSaved()
	{
		savePaletteCustomization(savedPaletteCustomization);
	}

	@Override
	public void save()
	{
		savePaletteCustomization(savedPaletteCustomization = paletteFactory.createPaletteCustomization(paletteRoot));
	}

	public void revertToDefaults()
	{
		savePaletteCustomization(null);
	}

	protected abstract void savePaletteCustomization(PaletteCustomization paletteCustomization);
}
