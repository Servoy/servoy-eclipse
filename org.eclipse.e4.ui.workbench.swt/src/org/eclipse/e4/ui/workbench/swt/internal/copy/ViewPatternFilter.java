/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 548578
 *******************************************************************************/
package org.eclipse.e4.ui.workbench.swt.internal.copy;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.dialogs.filteredtree.PatternFilter;
import org.eclipse.e4.ui.model.LocalizationHelper;
import org.eclipse.e4.ui.model.application.descriptor.basic.MPartDescriptor;
import org.eclipse.jface.viewers.Viewer;

/**
 * Based on of org.eclipse.ui.internal.dialogs.ViewPatternFilter.
 */
public class ViewPatternFilter extends PatternFilter {

	private IEclipseContext context;

	/**
	 * Create a new instance of a ViewPatternFilter
	 */
	public ViewPatternFilter(IEclipseContext context) {
		super();
		this.context = context;
	}

	@Override
	public boolean isElementSelectable(Object element) {
		return element instanceof MPartDescriptor;
	}

	@Override
	protected boolean isLeafMatch(Viewer viewer, Object element) {
		if (element instanceof String) {
			return false;
		}

		String text = null;
		if (element instanceof MPartDescriptor) {
			MPartDescriptor desc = (MPartDescriptor) element;
			text = LocalizationHelper.getLocalized(desc.getLabel(), desc,
					context);
			if (wordMatches(text)) {
				return true;
			}
		}

		return false;
	}
}
