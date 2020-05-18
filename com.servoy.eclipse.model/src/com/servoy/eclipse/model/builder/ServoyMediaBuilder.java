/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.model.builder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.MarkerMessages.ServoyMarker;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;

/**
 * @author lvostinar
 *
 */
public class ServoyMediaBuilder
{
	public static boolean addMediaMarkers(IProject project, IFile file)
	{
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		FlattenedSolution fs = servoyModel.getFlattenedSolution();

		String mediaName = file.getName();
		Media media = fs.getMedia(mediaName);
		if (media == null)
		{
			return false;
		}
		ServoyProject servoyProject = servoyModel.getServoyProject(project.getName());

		ServoyBuilder.checkPersistDuplicateName();
		ServoyBuilder.checkPersistDuplicateUUID();


		deleteMarkers(media);
		checkMedia(media);

		List<Form> forms = BuilderDependencies.getInstance().getMediaDependencies(media);
		if (forms != null)
		{
			Set<UUID> methodsParsed = new HashSet<UUID>();
			Map<Form, Boolean> formsAbstractChecked = new HashMap<Form, Boolean>();
			for (Form form : forms)
			{
				ServoyFormBuilder.deleteMarkers(form);
				ServoyFormBuilder.addFormMarkers(servoyProject, form, methodsParsed, formsAbstractChecked);
			}
		}

		return true;
	}

	public static void checkMedia(Media media)
	{
		if (media.getName().toLowerCase().endsWith(".tiff") || media.getName().toLowerCase().endsWith(".tif"))
		{
			Pair<String, String> path = SolutionSerializer.getFilePath(media, false);
			ServoyMarker mk = MarkerMessages.MediaTIFF.fill(path.getLeft() + path.getRight());
			ServoyBuilder.addMarker(ServoyBuilderUtils.getPersistResource(media), mk.getType(), mk.getText(), -1, ServoyBuilder.MEDIA_TIFF,
				IMarker.PRIORITY_NORMAL, null, media);
		}
	}


	public static void deleteMarkers(Media media)
	{
		IResource markerResource = ServoyBuilderUtils.getPersistResource(media);
		try
		{
			markerResource.deleteMarkers(ServoyBuilder.MEDIA_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}

	}
}
