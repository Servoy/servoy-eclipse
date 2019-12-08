package org.eclipse.jface.resource;

import java.net.URL;

/**
 * @since 3.4
 *
 */
public interface ImageDescriptorRelay {

	/**
	 * @param location
	 * @param filename
	 * @return ImageDescriptor
	 */
	ImageDescriptor createFromFile(Class<?> location, String filename);

	/**
	 * @param url
	 * @return ImageDescriptor
	 */
	ImageDescriptor createFromURL(URL url);

}
