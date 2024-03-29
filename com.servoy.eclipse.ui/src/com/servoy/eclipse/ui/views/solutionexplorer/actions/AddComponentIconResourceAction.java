/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.Package.DirPackageReader;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.util.Debug;

/**
 * Allows importing a image file in a folder component and changes the .spec file to the latest imported image.
 * @author gganea
 *
 */
public class AddComponentIconResourceAction extends Action implements ISelectionChangedListener
{

	private SimpleUserNode selection;
	private final SolutionExplorerView solutionExplorerView;

	/**
	 * @param solutionExplorerView
	 * @param shell
	 * @param string
	 * @param component
	 */
	public AddComponentIconResourceAction(SolutionExplorerView solutionExplorerView)
	{
		this.solutionExplorerView = solutionExplorerView;
		setText("Add Component Icon");
	}

	/*
	 *
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run()
	{
		if (selection != null)
		{
			FileDialog fd = new FileDialog(solutionExplorerView.getSite().getShell(), SWT.OPEN | SWT.SINGLE);
			String[] filterExt = { "*.png", "*.gif", "*.bmp", "*.jpg" };
			fd.setFilterExtensions(filterExt);
			String fileName = fd.open();
			if (fileName == null) return;

			BufferedImage bimg;
			try
			{
				bimg = ImageIO.read(new File(fileName));
				if (bimg != null)
				{
					int width = bimg.getWidth();
					int height = bimg.getHeight();
					if ((width != 16) || (height != 16))
					{
						MessageDialog dialog = new MessageDialog(solutionExplorerView.getSite().getShell(), "Incorrect image size.", null,
							"The size of selected image file is " + width + " x " + height + ". Please select an image of size 16 x 16.", 0,
							new String[] { "Ok" }, 0);
						dialog.open();
						return;
					}
				}
				else
				{
					Debug.error("ImageIO could not find an Image Reader found for file " + fileName);
					return;
				}
			}
			catch (IOException e1)
			{
				Debug.log(e1);
				//java does not see it as an image, we simply return
				return;
			}

			WebObjectSpecification spec = (WebObjectSpecification)selection.getRealObject();
			InputStream contents = null;
			BufferedInputStream bufferedInputStream = null;
			ByteArrayInputStream source = null;
			FileInputStream fis = null;
			try
			{
				IFile specfile = ResourcesUtils.findFileWithShortestPathForLocationURI(spec.getSpecURL().toURI());
				if (specfile != null && specfile.exists())
				{
					IFolder componentFolder = (IFolder)specfile.getParent();
					Path toCopy = new Path(fileName);
					String imageFileName = toCopy.lastSegment();

					IFile imageIFile = componentFolder.getFile(imageFileName);
					while (imageIFile.exists())
					{
						// we already have this file
						imageFileName = getNewFileName(imageFileName);
						if (imageFileName == null) return;// the user hit cancel, do not do anything
						imageIFile = componentFolder.getFile(imageFileName);

					}
					//copy image file contents
					fis = new FileInputStream(new File(fileName));
					if (imageIFile.exists()) imageIFile.setContents(fis, IResource.FORCE, null);
					else imageIFile.create(fis, IResource.FORCE, null);

					//change .spec file so that "icon" points to the new icon
					contents = specfile.getContents();
					bufferedInputStream = new BufferedInputStream(contents);
					String specFileString = IOUtils.toString(bufferedInputStream);
					JSONObject json = new JSONObject(specFileString);
					if (imageIFile.getParent() != null && imageIFile.getParent().getParent() != null)
						json.put("icon", imageIFile.getParent().getParent().getName() + "/" + imageIFile.getParent().getName() + "/" + imageFileName);
					source = new ByteArrayInputStream(json.toString(4).getBytes(StandardCharsets.UTF_8));
					specfile.setContents(source, true, false, null);
				}
			}
			catch (URISyntaxException e)
			{
				Debug.log(e);
			}
			catch (FileNotFoundException e)
			{
				Debug.log(e);
			}
			catch (CoreException e)
			{
				Debug.log(e);
			}
			catch (IOException e)
			{
				Debug.log(e);
			}
			catch (JSONException e)
			{
				Debug.log(e);
			}
			finally
			{
				if (contents != null) try
				{
					contents.close();
				}
				catch (IOException e)
				{
					Debug.log(e);
				}

				if (bufferedInputStream != null) try
				{
					bufferedInputStream.close();
				}
				catch (IOException e)
				{
					Debug.log(e);
				}
				if (source != null) try
				{
					source.close();
				}
				catch (IOException e)
				{
					Debug.log(e);
				}
				if (fis != null) try
				{
					fis.close();
				}
				catch (IOException e)
				{
					Debug.log(e);
				}
			}
		}
	}

	/**
	 * @return
	 */
	private String getNewFileName(String imageFileName)
	{
		InputDialog dlg = new InputDialog(solutionExplorerView.getSite().getShell(), "", imageFileName + " file already exists, please type in a new name",
			imageFileName, null);
		int open = dlg.open();
		if (open == org.eclipse.jface.window.Window.OK)
		{
			return dlg.getValue();

		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		selection = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		if (sel.size() == 1)
		{
			SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
			if ((node.getType() == UserNodeType.COMPONENT) &&
				(node.parent.getRealObject() instanceof IFolder || node.parent.getRealObject() instanceof IProject ||
					node.parent.getRealObject() instanceof DirPackageReader))
			{

				selection = node;
				setEnabled(true);
				return;
			}
		}
		setEnabled(false);
	}


}
