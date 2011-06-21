/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
package com.servoy.eclipse.core.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.ui.editor.saveparticipant.IPostSaveListener;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.MultiTextEdit;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Solution;

public class JavaScriptFilePostSaveListener implements IPostSaveListener
{

	public JavaScriptFilePostSaveListener()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.ui.editor.saveparticipant.IPostSaveListener#getName()
	 */
	public String getName()
	{
		return "Updates SolutionModel";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.ui.editor.saveparticipant.IPostSaveListener#getId()
	 */
	public String getId()
	{
		return "SolutionModelUpdater";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.ui.editor.saveparticipant.IPostSaveListener#isEnabled(org.eclipse.dltk.core.ISourceModule)
	 */
	public boolean isEnabled(ISourceModule compilationUnit)
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.ui.editor.saveparticipant.IPostSaveListener#needsChangedRegions(org.eclipse.dltk.core.ISourceModule)
	 */
	public boolean needsChangedRegions(ISourceModule compilationUnit) throws CoreException
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.ui.editor.saveparticipant.IPostSaveListener#saved(org.eclipse.dltk.core.ISourceModule, org.eclipse.jface.text.IRegion[],
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void saved(ISourceModule compilationUnit, IRegion[] changedRegions, IProgressMonitor monitor) throws CoreException
	{
		try
		{
			final File file = compilationUnit.getResource().getLocation().toFile();
			final IProject project = compilationUnit.getScriptProject().getProject();
			final ServoyModel servoyModel = (ServoyModel)ServoyModelFinder.getServoyModel();
			final Solution solution = (Solution)ServoyModel.getDeveloperRepository().getActiveRootObject(project.getName(), IRepository.SOLUTIONS);
			final ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solution.getName());
			final SolutionDeserializer sd = new SolutionDeserializer(ServoyModel.getDeveloperRepository(), servoyProject, file, compilationUnit.getSource());
			final IContainer workspace = project.getParent();
			final List<File> changedFiles = new ArrayList<File>();
			changedFiles.add(file);
			Set<ISupportChilds> changedScriptParents = servoyModel.handleChangedFiles(project, solution, changedFiles, servoyProject, workspace, sd);
			// add this file to the ignore once list, so that the real save will not parse it again.
			servoyModel.addIgnoreFile(file);
			for (ISupportChilds parent : changedScriptParents)
			{
				final IFile scriptFile = workspace.getFile(new Path(SolutionSerializer.getScriptPath(parent, false)));
				MultiTextEdit textEdit = servoyModel.getScriptFileChanges(parent, scriptFile);
				if (textEdit != null && textEdit.getChildrenSize() > 0)
				{
					ITextFileBufferManager textFileBufferManager = FileBuffers.getTextFileBufferManager();
					try
					{
						textFileBufferManager.connect(scriptFile.getFullPath(), LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
						continue;
					}

					try
					{
						ITextFileBuffer textFileBuffer = textFileBufferManager.getTextFileBuffer(scriptFile.getFullPath(), LocationKind.IFILE);
						IDocument document = textFileBuffer.getDocument();
						textEdit.apply(document);
					}
					finally
					{
						try
						{
							textFileBufferManager.disconnect(scriptFile.getFullPath(), LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}

		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}
}
