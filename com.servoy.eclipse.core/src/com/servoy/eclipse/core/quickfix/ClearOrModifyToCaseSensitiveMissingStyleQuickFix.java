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
package com.servoy.eclipse.core.quickfix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

public class ClearOrModifyToCaseSensitiveMissingStyleQuickFix extends WorkbenchMarkerResolution
{
	private final boolean clearStyle;
	private final String styleClass;
	private final String uuid;

	public ClearOrModifyToCaseSensitiveMissingStyleQuickFix(String uuid, boolean clearStyle, String styleClass)
	{
		this.clearStyle = clearStyle;
		this.styleClass = styleClass;
		this.uuid = uuid;
	}

	public String getLabel()
	{
		if (styleClass != null)
		{
			return "Modify styleClass to case sensitive value.";
		}
		else if (clearStyle)
		{
			return "Clear invalid style in form.";
		}
		else
		{
			return "Clear invalid style class.";
		}
	}

	public String getDescription()
	{
		return getLabel();
	}

	public Image getImage()
	{
		return null;
	}

	public String getStyleClass()
	{
		return styleClass;
	}

	public void run(IMarker marker)
	{
		boolean clearStyle = marker.getAttribute("clearStyle", false);
		String solutionName = marker.getAttribute("SolutionName", null);
		String uuid = marker.getAttribute("Uuid", null);
		String styleClass = marker.getAttribute("styleClass", null);
		if (uuid != null)
		{
			UUID id = UUID.fromString(uuid);
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (servoyProject != null)
			{
				try
				{
					IPersist persist = servoyProject.getEditingPersist(id);
					if (persist != null)
					{
						if (clearStyle)
						{
							if (persist instanceof Form)
							{
								((Form)persist).setStyleName(null);
							}
						}
						else
						{
							if (persist instanceof Form)
							{
								((Form)persist).setStyleClass(styleClass);
							}
							else if (persist instanceof BaseComponent)
							{
								((BaseComponent)persist).setStyleClass(styleClass);
							}
						}
						if (styleClass == null)
						{
							servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, true);
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}

	}

	@Override
	public void run(IMarker[] markers, IProgressMonitor monitor)
	{
		super.run(markers, monitor);
		if (styleClass != null)
		{
			List<String> solutions = new ArrayList<String>();
			for (IMarker marker : markers)
			{
				try
				{
					String solName = marker.getAttribute("SolutionName", null);
					if (solName != null && !solutions.contains(solName)) solutions.add(solName);
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
			for (String solName : solutions)
			{
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
				if (servoyProject != null)
				{
					try
					{
						servoyProject.saveEditingSolutionNodes(new IPersist[] { servoyProject.getEditingSolution() }, true);
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}
			}
		}
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers)
	{
		List<IMarker> similar = new ArrayList<IMarker>();
		if (getStyleClass() != null)
		{
			if (markers != null)
			{
				for (IMarker marker : markers)
				{
					try
					{
						if (marker.exists() &&
							!Utils.equalObjects(marker.getAttribute("Uuid", null), this.uuid) && marker.getType().equals(ServoyBuilder.MISSING_STYLE) &&
							marker.getAttribute("styleClass", null) != null) similar.add(marker);
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
		return similar.toArray(new IMarker[0]);
	}
}