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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;


import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Sets the orientation (splitting) for the solution explorer view.
 */
public class OrientationAction extends Action
{

    private SolutionExplorerView fView;    
    private int fActionOrientation;
    
    public OrientationAction(SolutionExplorerView v, int orientation)
    {
        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
        if (orientation == SolutionExplorerView.VIEW_ORIENTATION_HORIZONTAL) {
            setText("Horizontal View Orientation"); 
            setImageDescriptor(Activator.loadImageDescriptorFromBundle("th_horizontal.gif")); //$NON-NLS-1$
        } else if (orientation == SolutionExplorerView.VIEW_ORIENTATION_VERTICAL) {
            setText("Vertical View Orientation"); 
            setImageDescriptor(Activator.loadImageDescriptorFromBundle("th_vertical.gif")); //$NON-NLS-1$
		} else if (orientation == SolutionExplorerView.VIEW_ORIENTATION_AUTOMATIC) {
            setText("Automatic View Orientation"); 
			setImageDescriptor(Activator.loadImageDescriptorFromBundle("th_automatic.gif")); //$NON-NLS-1$
        } else {
            Assert.isTrue(false);
        }
        fView = v;
        fActionOrientation = orientation;
//        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_TOGGLE_ORIENTATION_ACTION);
    }
    
    public int getOrientation()
    {
        return fActionOrientation;
    }   
    
    public void run()
    {
        fView.setOrientation(fActionOrientation);
    }
    
}
