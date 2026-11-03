/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.eclipse.ui.quickfix;

import java.util.Arrays;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.builder.ServoyFormBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Quickfix for the cssPosition-mismatch form problem (a {@link ServoyBuilder#PROJECT_FORM_MARKER_TYPE} marker carrying the
 * {@link ServoyFormBuilder#CSS_POSITION_MISMATCH_PROBLEM} attribute): rewrites the fields that are present in the web
 * component's {@code json.cssPosition} object so they match the authoritative compact {@code cssPosition} string (the one
 * Servoy actually renders from). Reconciles json &larr; string. See SVY-21669.
 *
 * @author lvostinar
 */
public class ReconcileCssPositionQuickFix extends WorkbenchMarkerResolution
{
	private final IMarker marker;

	public ReconcileCssPositionQuickFix(IMarker marker)
	{
		this.marker = marker;
	}

	@Override
	public String getLabel()
	{
		return "Fix json cssPosition to match the stored cssPosition string";
	}

	@Override
	public void run(IMarker markerToFix)
	{
		String solutionName = markerToFix.getAttribute("SolutionName", null);
		String uuid = markerToFix.getAttribute("Uuid", null);
		if (solutionName == null || uuid == null) return;

		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
		if (servoyProject == null) return;

		IPersist persist = servoyProject.getEditingPersist(UUID.fromString(uuid));
		if (persist instanceof WebComponent)
		{
			WebComponent webComponent = (WebComponent)persist;
			if (reconcile(webComponent))
			{
				try
				{
					servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, true);
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}

	private boolean reconcile(WebComponent webComponent)
	{
		CSSPosition stringPos = webComponent.getCssPosition();
		if (stringPos == null) return false;

		JSONObject json = (JSONObject)webComponent.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		if (json == null) return false;
		Object jsonPos = json.opt(StaticContentSpecLoader.PROPERTY_CSS_POSITION.getPropertyName());
		if (!(jsonPos instanceof JSONObject)) return false;

		JSONObject cssPosition = (JSONObject)jsonPos;
		// set each of the six fields to its authoritative value so the json agrees with the string, only touching the object
		// when something actually differs so an already-correct json is not needlessly dirtied/re-serialized
		boolean changed = setField(cssPosition, "top", stringPos.top) |
			setField(cssPosition, "right", stringPos.right) |
			setField(cssPosition, "bottom", stringPos.bottom) |
			setField(cssPosition, "left", stringPos.left) |
			setField(cssPosition, "width", stringPos.width) |
			setField(cssPosition, "height", stringPos.height);
		if (!changed) return false;

		webComponent.flagChanged();
		webComponent.setJson(json);
		return true;
	}

	/**
	 * Sets the field to the authoritative value (a null value is stored as JSON null) and returns true only when that
	 * actually changed the json object, so an already-correct field is left untouched.
	 */
	private static boolean setField(JSONObject cssPosition, String name, String value)
	{
		Object newValue = value != null ? value : JSONObject.NULL;
		if (Utils.equalObjects(cssPosition.opt(name), newValue)) return false;
		cssPosition.put(name, newValue);
		return true;
	}

	@Override
	public String getDescription()
	{
		return null;
	}

	@Override
	public Image getImage()
	{
		return null;
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers)
	{
		if (markers != null)
		{
			return Arrays.stream(markers).filter(
				mk -> {
					try
					{
						return mk.getId() != marker.getId() && mk.exists() && mk.getType().equals(ServoyBuilder.PROJECT_FORM_MARKER_TYPE) &&
							mk.getAttribute(ServoyFormBuilder.CSS_POSITION_MISMATCH_PROBLEM, false);
					}
					catch (CoreException e)
					{
						ServoyLog.logError("Could not find all css position mismatch markers.", e);
					}
					return false;
				})
				.toArray(IMarker[]::new);
		}
		return markers;
	}
}
