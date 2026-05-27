package com.servoy.eclipse.opencode.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class BrowserEditorInput implements IEditorInput
{
	private final String url;
	private final String name;
	private final String tooltip;

	public BrowserEditorInput(String url, String name)
	{
		this(url, name, url);
	}

	public BrowserEditorInput(String url, String name, String tooltip)
	{
		this.url = url;
		this.name = name;
		this.tooltip = tooltip;
	}

	public String getUrl()
	{
		return url;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter)
	{
		return null;
	}

	@Override
	public boolean exists()
	{
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public IPersistableElement getPersistable()
	{
		return null;
	}

	@Override
	public String getToolTipText()
	{
		return tooltip;
	}

	@Override
	public int hashCode()
	{
		return url.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		BrowserEditorInput other = (BrowserEditorInput)obj;
		return url.equals(other.url);
	}
}
