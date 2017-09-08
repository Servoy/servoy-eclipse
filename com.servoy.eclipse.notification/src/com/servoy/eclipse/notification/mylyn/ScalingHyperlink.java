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

/*******************************************************************************
 * Copyright (c) 2004, 2011 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package com.servoy.eclipse.notification.mylyn;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

/**
 * Enhanced {@link ImageHyperlink} that truncates the link text at the end rather than the middle if it is wider than
 * the available space. Also provides default color and underline on hover.
 * 
 * @author Leo Dos Santos
 * @author Mik Kersten
 * @since 3.7
 */
public class ScalingHyperlink extends ImageHyperlink {

	private boolean strikeThrough;

	protected final MouseTrackListener MOUSE_TRACK_LISTENER = new MouseTrackListener() {

		public void mouseEnter(MouseEvent e) {
			setUnderlined(true);
		}

		public void mouseExit(MouseEvent e) {
			setUnderlined(false);
		}

		public void mouseHover(MouseEvent e) {
		}
	};

	public ScalingHyperlink(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	public void dispose() {
		removeMouseTrackListener(MOUSE_TRACK_LISTENER);
		super.dispose();
	}

	public void registerMouseTrackListener() {
		addMouseTrackListener(MOUSE_TRACK_LISTENER);
	}

	public boolean isStrikeThrough() {
		return strikeThrough;
	}

	@Override
	protected void paintText(GC gc, Rectangle bounds) {
		super.paintText(gc, bounds);
		if (strikeThrough) {
			Point totalSize = computeTextSize(SWT.DEFAULT, SWT.DEFAULT);
			int textWidth = Math.min(bounds.width, totalSize.x);
			int textHeight = totalSize.y;

			//			int descent = gc.getFontMetrics().getDescent();
			int lineY = bounds.y + (textHeight / 2); // - descent + 1;
			gc.drawLine(bounds.x, lineY, bounds.x + textWidth, lineY);
		}
	}

	public void setStrikeThrough(boolean strikethrough) {
		this.strikeThrough = strikethrough;
	}

	@Override
	protected String shortenText(GC gc, String t, int width) {
		if (t == null) {
			return null;
		}

		if ((getStyle() & SWT.SHORT) != 0) {
			return t;
		}

		String returnText = t;
		if (gc.textExtent(t).x > width) {
			for (int i = t.length(); i > 0; i--) {
				String test = t.substring(0, i);
				test = test + "..."; //$NON-NLS-1$
				if (gc.textExtent(test).x < width) {
					returnText = test;
					break;
				}
			}
		}
		return returnText;
	}

}
