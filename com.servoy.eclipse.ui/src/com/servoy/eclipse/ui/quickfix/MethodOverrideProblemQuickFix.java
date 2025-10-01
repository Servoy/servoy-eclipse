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

package com.servoy.eclipse.ui.quickfix;

import java.io.OutputStream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;


/**
 * Removes the @override annotation when there is no super method.
 * @author emera
 */
class MethodOverrideProblemQuickFix implements IMarkerResolution
{
	@Override
	public void run(IMarker mk)
	{
		try
		{
			String solName = mk.getAttribute("SolutionName", null);
			String uuid = mk.getAttribute("Uuid", null);
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solName);
			if (uuid != null && servoyProject != null)
			{
				UUID id = UUID.fromString(uuid);
				IPersist persist = AbstractRepository.searchPersist(servoyProject.getSolution(), id);

				if (persist instanceof ScriptMethod)
				{
					ScriptMethod method = (ScriptMethod)persist;
					method.setDeclaration(method.getDeclaration().replace(" * " + SolutionSerializer.OVERRIDEKEY + "\n", ""));
				}

				IFileAccess fileAccess = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
				Pair<String, String> filepathname = SolutionSerializer.getFilePath(persist, false);
				String fileRelativePath = filepathname.getLeft() + filepathname.getRight();
				Object content = SolutionSerializer.generateScriptFile(persist.getParent(), SolutionSerializer.getScriptPath(persist, false),
					ApplicationServerRegistry.get().getDeveloperRepository(), null);

				OutputStream fos = fileAccess.getOutputStream(fileRelativePath);
				if (content instanceof byte[])
				{
					fos.write((byte[])content);
				}
				else if (content instanceof CharSequence)
				{
					fos.write(content.toString().getBytes("UTF8"));
				}
				Utils.closeOutputStream(fos);
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Can't quickfix override method marker.", e);
		}
	}

	@Override
	public String getLabel()
	{
		return "Remove the " + SolutionSerializer.OVERRIDEKEY + " annotation.";
	}
}