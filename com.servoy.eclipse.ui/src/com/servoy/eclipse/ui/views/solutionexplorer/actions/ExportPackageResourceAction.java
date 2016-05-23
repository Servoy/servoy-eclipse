/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Exports the selected components or services packages
 *
 * @author jcompagner
 */
public class ExportPackageResourceAction extends Action implements ISelectionChangedListener
{

	private IStructuredSelection selection;
	private final Shell shell;
	private final SolutionExplorerView viewer;


	public ExportPackageResourceAction(SolutionExplorerView viewer, Shell shell)
	{
		this.viewer = viewer;
		this.shell = shell;
		setText("Export package");
		setToolTipText("Export the package to a zip file that can be imported in another installation");
	}

	@Override
	public void run()
	{
		if (selection != null)
		{
			Iterator<SimpleUserNode> it = selection.iterator();
			outer : while (it.hasNext())
			{
				SimpleUserNode next = it.next();
				Object realObject = next.getRealObject();
				IResource resource = null;
				if (realObject instanceof IContainer || realObject instanceof IFile)
				{
					resource = (IResource)realObject;
					FileDialog dialog = new FileDialog(shell, SWT.SAVE);
					dialog.setFileName(resource.getName() + (resource instanceof IContainer ? ".zip" : ""));
					String selectedFile = dialog.open();
					if (selectedFile != null)
					{
						if (!selectedFile.toLowerCase().endsWith(".zip")) selectedFile += ".zip";
						File file = new File(selectedFile);
						while (file.exists() && !MessageDialog.openConfirm(shell, getText(), "Are you sure you want to override?"))
						{
							selectedFile = dialog.open();
							if (selectedFile != null) file = new File(selectedFile);
							else continue outer;
						}
						if (resource instanceof IContainer)
						{
							ZipOutputStream zos = null;
							try
							{
								zos = new ZipOutputStream(new FileOutputStream(file));

								zipDir((IContainer)resource, zos, "");
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
							}
							finally
							{
								if (zos != null) try
								{
									zos.close();
								}
								catch (IOException e)
								{
									ServoyLog.logError(e);
								}
							}
						}
						else if (resource instanceof IFile)
						{
							InputStream contents = null;
							try
							{
								resource.refreshLocal(1, null);
								contents = ((IFile)resource).getContents();
								FileUtils.copyInputStreamToFile(contents, file);
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
							}
							finally
							{
								if (contents != null) try
								{
									contents.close();
								}
								catch (IOException e)
								{
									ServoyLog.logError(e);
								}
							}
						}

					}
				}
			}
		}
	}

	private void zipDir(IContainer folder, ZipOutputStream zos, String prefix) throws CoreException, FileNotFoundException, IOException
	{
		for (IResource member : folder.members())
		{
			if (member instanceof IContainer)
			{
				zipDir((IContainer)member, zos, prefix + member.getName() + '/');
			}
			else if (member instanceof IFile)
			{
				addToZipFile((IFile)member, prefix + member.getName(), zos);
			}
			else
			{
				ServoyLog.logInfo("unsupported file when exporting package: " + member);
			}
		}

	}

	private void addToZipFile(IFile file, String fileName, ZipOutputStream zos) throws FileNotFoundException, IOException, CoreException
	{
		file.refreshLocal(1, null);
		InputStream contents = null;
		try
		{
			contents = file.getContents(true);
			ZipEntry zipEntry = new ZipEntry(fileName);
			zos.putNextEntry(zipEntry);

			byte[] bytes = new byte[4096];
			int length;
			while ((length = contents.read(bytes)) >= 0)
			{
				zos.write(bytes, 0, length);
			}
			zos.closeEntry();
		}
		finally
		{
			contents.close();
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		// allow multiple selection
		selection = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = true;
		Iterator<SimpleUserNode> it = sel.iterator();
		while (it.hasNext() && state)
		{
			SimpleUserNode node = it.next();
			state = node.getType() == UserNodeType.COMPONENTS_NONPROJECT_PACKAGE || node.getType() == UserNodeType.SERVICES_NONPROJECT_PACKAGE ||
				node.getType() == UserNodeType.COMPONENTS_PROJECT_PACKAGE || node.getType() == UserNodeType.SERVICES_PROJECT_PACKAGE ||
				node.getType() == UserNodeType.LAYOUT_PROJECT_PACKAGE;
		}
		if (state)
		{
			selection = sel;
		}
		setEnabled(state);
	}

}
