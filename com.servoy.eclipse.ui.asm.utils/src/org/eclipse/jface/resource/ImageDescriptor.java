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

package org.eclipse.jface.resource;


import java.lang.reflect.Method;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.Policy;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.ui.asm.utils.RunAsmifier;
import com.servoy.eclipse.ui.tweaks.ImageReplacementMapper;


/**
 * This is the template in plain java for what we want the end result to look like for a runtime modified org.eclipse.jface.resource.ImageDescriptor bytecode.</br>
 * The idea is that we can write in here in plain java and then run the Asmifier (see {@link RunAsmifier}) on it - and that will generate runtime class generating code that we can
 * use/integrate in our actual class weaver (so we don't have to write everything by hand).<br>
 *
 * For example we want a particular method, just take that block from the generated asm code
 * and use it in the weaver.
 *
 * BASICALLY: we override what createFromFile(...) and createFromURL(...) do so that they will forward and map certain arguments to another set of images as defined in the Servoy extension points.
 *
 * @author acostescu
 */
public abstract class ImageDescriptor extends DeviceResourceDescriptor
{

	protected static final ImageData DEFAULT_IMAGE_DATA = new ImageData(6, 6, 1, new PaletteData(new RGB[] { new RGB(255, 0, 0) }));

	static Method originalFileBasedImageCreator;
	static Method originalUrlBasedImageCreator;

	static
	{
		try
		{
			originalFileBasedImageCreator = ImageDescriptor.class.getDeclaredMethod("originalCreateFromFile", Class.class, String.class);
			originalUrlBasedImageCreator = ImageDescriptor.class.getDeclaredMethod("originalCreateFromURL", URL.class);
		}
		catch (NoSuchMethodException e)
		{
			// should never happen
			originalFileBasedImageCreator = null;
			originalUrlBasedImageCreator = null;
			Policy.getLog().log(new Status(IStatus.ERROR, Policy.JFACE,
				"Exception trying to get original createFrom... method. Should never happen: " + e.getLocalizedMessage(), e));
		}
		catch (SecurityException e)
		{
			originalFileBasedImageCreator = null;
			originalUrlBasedImageCreator = null;
			Policy.getLog().log(
				new Status(IStatus.ERROR, Policy.JFACE, "Exception trying to get original createFrom... method: " + e.getLocalizedMessage(), e));
		}
	}

	protected ImageDescriptor()
	{
		// this is the default constructor for abstract classes
	}

	public static ImageDescriptor createFromFile(Class< ? > classLocation, String fileName)
	{
		try
		{
			return ImageReplacementMapper.getFileBasedImageReplacement(classLocation, fileName, originalUrlBasedImageCreator, originalFileBasedImageCreator);
		}
		catch (Exception e)
		{
			Policy.getLog().log(new Status(IStatus.ERROR, Policy.JFACE,
				"Exception trying to get replacement file image for " + classLocation.getCanonicalName() + ", " + fileName + ": " + e.getLocalizedMessage(),
				e));
			return originalCreateFromFile(classLocation, fileName);
		}
	}

	public static ImageDescriptor createFromURL(URL url)
	{
		try
		{
			return ImageReplacementMapper.getUrlBasedImageReplacement(url, originalUrlBasedImageCreator, originalFileBasedImageCreator);
		}
		catch (Exception e)
		{
			Policy.getLog().log(
				new Status(IStatus.ERROR, Policy.JFACE, "Exception trying to get replacement url image for " + url + ": " + e.getLocalizedMessage(), e));
			return originalCreateFromURL(url);
		}
	}

	static ImageDescriptor originalCreateFromFile(Class< ? > location, String filename)
	{
		return new FileImageDescriptor(location, filename);
	}

	static ImageDescriptor originalCreateFromURL(URL url)
	{
		if (url == null)
		{
			return getMissingImageDescriptor();
		}
		return new URLImageDescriptor(url);
	}

	public static ImageDescriptor createFromImageData(ImageData data)
	{
		return new ImageDataImageDescriptor(data);
	}

	public static ImageDescriptor createFromImage(Image img)
	{
		return new ImageDataImageDescriptor(img);
	}

	public static ImageDescriptor createWithFlags(ImageDescriptor originalImage, int swtFlags)
	{
		return new DerivedImageDescriptor(originalImage, swtFlags);
	}

	@Deprecated
	public static ImageDescriptor createFromImage(Image img, Device theDevice)
	{
		return new ImageDataImageDescriptor(img);
	}

	@Override
	public Object createResource(Device device) throws DeviceResourceException
	{
		Image result = createImage(false, device);
		if (result == null)
		{
			throw new DeviceResourceException(this);
		}
		return result;
	}

	@Override
	public void destroyResource(Object previouslyCreatedObject)
	{
		((Image)previouslyCreatedObject).dispose();
	}

	public Image createImage()
	{
		return createImage(true);
	}

	public Image createImage(boolean returnMissingImageOnError)
	{
		return createImage(returnMissingImageOnError, Display.getCurrent());
	}

	public Image createImage(Device device)
	{
		return createImage(true, device);
	}

	public Image createImage(boolean returnMissingImageOnError, Device device)
	{

		ImageData data = getImageData();
		if (data == null)
		{
			if (!returnMissingImageOnError)
			{
				return null;
			}
			data = DEFAULT_IMAGE_DATA;
		}

		try
		{
			if (data.transparentPixel >= 0)
			{
				ImageData maskData = data.getTransparencyMask();
				return new Image(device, data, maskData);
			}
			return new Image(device, data);
		}
		catch (SWTException exception)
		{
			if (returnMissingImageOnError)
			{
				try
				{
					return new Image(device, DEFAULT_IMAGE_DATA);
				}
				catch (SWTException nextException)
				{
					return null;
				}
			}
			return null;
		}
	}

	public abstract ImageData getImageData();

	public static ImageDescriptor getMissingImageDescriptor()
	{
		return MissingImageDescriptor.getInstance();
	}

}
