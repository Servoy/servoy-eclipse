/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

package com.servoy.eclipse.opencode;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Perspective factory for the "Servoy AI" perspective.
 * <p>
 * The perspective contains a single {@link OpenCodeView} that fills the entire
 * window. All URL initialisation logic lives in
 * {@link OpenCodeView#createPartControl}
 * so that close/reopen of the view works correctly without restarting the
 * server.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
public class OpencodePerspective implements IPerspectiveFactory {
	public static final String PERSPECTIVE_ID = "com.servoy.eclipse.opencode.OpencodePerspective";

	/**
	 * System property that overrides the server URL (dev / external-server mode).
	 * Referenced by {@link OpenCodeView}.
	 */
	static final String URL_PROPERTY = "opencode.url";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		// Hide the editor area - the browser view fills everything.
		layout.setEditorAreaVisible(false);

		// Place the browser view to the left of the (hidden) editor area,
		// taking the full width (ratio = 1.0).
		layout.addStandaloneView(OpenCodeView.VIEW_ID, false, IPageLayout.LEFT, 1.0f, IPageLayout.ID_EDITOR_AREA);

		layout.setFixed(true);
	}
}
