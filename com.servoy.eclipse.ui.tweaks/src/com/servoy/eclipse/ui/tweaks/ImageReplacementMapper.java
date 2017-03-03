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

package com.servoy.eclipse.ui.tweaks;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.tweaks.bytecode.weave.ImageDescriptorWeaver;
import com.servoy.j2db.util.Pair;

/**
 * This class contains the mappings between old images locations and new image locations that are used to replace icons in non Servoy plug-ins.
 * It is done via byte-code weaving on ImageDescriptor. See {@link ImageDescriptorWeaver}.
 *
 * @author acostescu
 */
public class ImageReplacementMapper
{

	public static final String IMAGE_REPLACEMENT_EXTENSION_ID = "com.servoy.eclipse.ui.tweaks.imageReplacement";

	public static final String URL_BASED_IMAGE_REPLACEMENT_KEY = "urlBasedImageReplacement";
	public static final String CLASS_AND_FILE_IMAGE_REPLACEMENT_KEY = "classAndFileBasedImageReplacement";
	public static final String ORIGINAL_IMAGE_URL_KEY = "originalImageURL";
	public static final String ORIGINAL_FULL_CLASS_NAME_KEY = "originalRelativeToFullClassName";
	public static final String ORIGINAL_FILE_NAME_KEY = "originalImageFileName";

	public static final String ALTERNATE_CLASS_AND_FILE_EL_KEY = "alternateClassAndFileNameImageLocation";
	public static final String ALTERNATE_URL_EL_KEY = "alternateURLImageLocation";
	public static final String ALTERNATE_CLASS_KEY = "relativeToClass";
	public static final String ALTERNATE_FILE_NAME_KEY = "imageFileName";
	public static final String ALTERNATE_URL_KEY = "imageURL";

	public static final String LIST_ALL_INTERCEPTABLE_IMG_MAPPINGS_KEY = "listInterceptableImageDescriptorsInConsole";

	// just logging interceptable images stuff
	private static boolean LIST_ALL_INTERCEPTABLE_IMG_MAPPINGS = Boolean.valueOf(
		Activator.getDefault().getInitProperties().getProperty(LIST_ALL_INTERCEPTABLE_IMG_MAPPINGS_KEY, "false")).booleanValue();
	private static Set<String> interceptableUrls = null;
	private static Set<Pair<Class< ? >, String>> interceptableFiles = null;

	// replacements loaded from extension points
	private static Map<URL, AlternateImageLocation> urlReplacements = new HashMap<>();
	private static Map<Pair<String, String>, AlternateImageLocation> classAndFileNameReplacements = new HashMap<>();

	private static interface AlternateImageLocation
	{
		ImageDescriptor createAlternateImage(Method urlCreatorMethod, Method classAndFileNameCreatorMethod)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;
	}

	private static class AlternateURLImageLocation implements AlternateImageLocation
	{

		private final URL url;

		public AlternateURLImageLocation(URL url)
		{
			this.url = url;
		}

		@Override
		public ImageDescriptor createAlternateImage(Method urlCreatorMethod, Method classAndFileNameCreatorMethod)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
		{
			return (ImageDescriptor)urlCreatorMethod.invoke(null, url);
		}

	}

	private static class AlternateClassAndFileNameImageLocation implements AlternateImageLocation
	{

		private final Class< ? > relativeToClass;
		private final String fileName;

		public AlternateClassAndFileNameImageLocation(Class< ? > relativeToClass, String fileName)
		{
			this.relativeToClass = relativeToClass;
			this.fileName = fileName;
		}

		@Override
		public ImageDescriptor createAlternateImage(Method urlCreatorMethod, Method classAndFileNameCreatorMethod)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
		{
			return (ImageDescriptor)classAndFileNameCreatorMethod.invoke(null, relativeToClass, fileName);
		}

	}

	static
	{
		if (LIST_ALL_INTERCEPTABLE_IMG_MAPPINGS)
		{
			interceptableUrls = new HashSet<>(128);
			interceptableFiles = new HashSet<>(32);
		}

		// load intercepted images from extension points
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint(IMAGE_REPLACEMENT_EXTENSION_ID);
		IExtension[] extensions = ep.getExtensions();

		for (IExtension extension : extensions)
		{
			IConfigurationElement[] ces = extension.getConfigurationElements();
			for (IConfigurationElement ce : ces)
			{
				try
				{
					String replacementType = ce.getName();
					IConfigurationElement alternateElement = ce.getChildren()[0];
					if (URL_BASED_IMAGE_REPLACEMENT_KEY.equals(replacementType))
					{
						String originalURL = ce.getAttribute(ORIGINAL_IMAGE_URL_KEY);
						urlReplacements.put(new URL(originalURL), getAlternateImageCreator(alternateElement));
					}
					else if (CLASS_AND_FILE_IMAGE_REPLACEMENT_KEY.equals(replacementType))
					{
						String originalFullClassName = ce.getAttribute(ORIGINAL_FULL_CLASS_NAME_KEY);
						String originalFileName = ce.getAttribute(ORIGINAL_FILE_NAME_KEY);
						classAndFileNameReplacements.put(new Pair<>(originalFullClassName, originalFileName), getAlternateImageCreator(alternateElement));
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError("Could not load extension (extension point " + IMAGE_REPLACEMENT_EXTENSION_ID + ", " + ce.getName() + ", " +
						ce.getAttribute(ORIGINAL_IMAGE_URL_KEY) + ", " + ce.getAttribute(ORIGINAL_FULL_CLASS_NAME_KEY) + ", " +
						ce.getAttribute(ORIGINAL_FILE_NAME_KEY) + ")", e);
				}
			}
		}
	}

	private static AlternateImageLocation getAlternateImageCreator(IConfigurationElement alternateElement) throws ClassNotFoundException, MalformedURLException
	{
		String alternateType = alternateElement.getName();
		if (ALTERNATE_URL_EL_KEY.equals(alternateType))
		{
			String alternateURL = alternateElement.getAttribute(ALTERNATE_URL_KEY);
			return new AlternateURLImageLocation(new URL(alternateURL));
		}
		else if (ALTERNATE_CLASS_AND_FILE_EL_KEY.equals(alternateType))
		{
			String alternateFullClassName = alternateElement.getAttribute(ALTERNATE_CLASS_KEY);
			String alternateFileName = alternateElement.getAttribute(ALTERNATE_FILE_NAME_KEY);
			return new AlternateClassAndFileNameImageLocation(Class.forName(alternateFullClassName), alternateFileName);
		}
		throw new IllegalArgumentException("Invalid alternate image location configuration");
	}

	/**
	 * Creates and returns alternate image descriptors (if needed) from files.
	 * If there is no alternate image configured for this location, it just returns the original one.
	 *
	 * @param originalCreateFromFile the original implementation of {@link ImageDescriptor#createFromFile(Class, String)}.
	 *
	 * @throws InvocationTargetException if an attempt to call the original {@link ImageDescriptor#createFromFile(Class, String)} failed.
	 * @throws IllegalArgumentException if an attempt to call the original {@link ImageDescriptor#createFromFile(Class, String)} failed.
	 * @throws IllegalAccessException if an attempt to call the original {@link ImageDescriptor#createFromFile(Class, String)} failed.
	 *
	 * @return see description above.
	 */
	public static ImageDescriptor getFileBasedImageReplacement(Class< ? > classLocation, String fileName, final Method originalCreateFromURL,
		final Method originalCreateFromFile) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		AlternateImageLocation replacement = classAndFileNameReplacements.get(new Pair<>(classLocation.getName(), fileName));

		if (LIST_ALL_INTERCEPTABLE_IMG_MAPPINGS)
		{
			if (interceptableFiles.add(new Pair<Class< ? >, String>(classLocation, fileName)))
			{
				System.out.println((replacement == null ? "(ORIGINAL)" : "(REPLACED)") + " FileBasedImageReplacer: (" + classLocation + ", " + fileName + ")");
			}
		}

		if (replacement != null) return replacement.createAlternateImage(originalCreateFromURL, originalCreateFromFile);
		else return (ImageDescriptor)originalCreateFromFile.invoke(null, new Object[] { classLocation, fileName });
	}

	/**
	 * Creates and returns alternate image descriptors (if needed) from URLs.
	 * If there is no alternate image configured for this URL, it just returns the original one.
	 *
	 * @param originalCreateFromURL the original implementation of {@link ImageDescriptor#createFromURL(URL)}.
	 *
	 * @throws InvocationTargetException if an attempt to call the original {@link ImageDescriptor#createFromURL(URL)} failed.
	 * @throws IllegalArgumentException if an attempt to call the original {@link ImageDescriptor#createFromURL(URL)} failed.
	 * @throws IllegalAccessException if an attempt to call the original {@link ImageDescriptor#createFromURL(URL)} failed.
	 *
	 * @return see description above.
	 */
	public static ImageDescriptor getUrlBasedImageReplacement(URL url, final Method originalCreateFromURL, final Method originalCreateFromFile)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		AlternateImageLocation replacement = urlReplacements.get(url);

		if (LIST_ALL_INTERCEPTABLE_IMG_MAPPINGS)
		{
			if (interceptableUrls.add(String.valueOf(url)))
			{
				System.out.println((replacement == null ? "(ORIGINAL)" : "(REPLACED)") + " URLBasedImageReplacer: (" + url + ")");
			}
		}

		if (replacement != null) return replacement.createAlternateImage(originalCreateFromURL, originalCreateFromFile);
		else return (ImageDescriptor)originalCreateFromURL.invoke(null, new Object[] { url });
	}

}
