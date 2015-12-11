/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.ui.property;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

/**
 * Value editor for style classes defined at solution level (ngclient), opens the style and selects the style class.
 *
 * @author rgansevles
 *
 */
public class SolutionStyleClassValueEditor implements IValueEditor<String>
{
	private final Solution solution;

	public SolutionStyleClassValueEditor(Solution solution)
	{
		this.solution = solution;
	}

	public void openEditor(String value)
	{
		if (solution.getStyleSheetID() > 0)
		{
			Media media = ModelUtils.getEditingFlattenedSolution(solution).getMedia(solution.getStyleSheetID());
			if (media != null)
			{
				Pair<String, String> pathPair = SolutionSerializer.getFilePath(media, true);
				IEditorPart editor = EditorUtil.openFileEditor(ResourcesPlugin.getWorkspace().getRoot().getFile(
					new Path(pathPair.getLeft() + pathPair.getRight())));
				if (editor instanceof StructuredTextEditor)
				{
					EditorUtil.selectAndReveal((StructuredTextEditor)editor, value);
				}
			}
		}
	}

	public boolean canEdit(String value)
	{
		return solution.getStyleSheetID() > 0 && ModelUtils.getEditingFlattenedSolution(solution).getMedia(solution.getStyleSheetID()) != null;
	}
}
