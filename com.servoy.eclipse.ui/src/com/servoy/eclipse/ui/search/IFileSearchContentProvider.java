package com.servoy.eclipse.ui.search;

/**
 * Content provider interface for file search results.
 *  
 * @author jcompagner
 * @since 6.0
 */
public interface IFileSearchContentProvider
{

	public abstract void elementsChanged(Object[] updatedElements);

	public abstract void clear();

}