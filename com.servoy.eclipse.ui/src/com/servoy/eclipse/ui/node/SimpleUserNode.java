/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.ui.node;

import org.eclipse.core.internal.runtime.AdapterManager;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Image;

import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ISupportDeprecated;
import com.servoy.j2db.persistence.ISupportHTMLToolTipText;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.annotations.AnnotationManagerReflection;

/**
 * Universal class to use in tree nodes and as list item.
 *
 * @author jblok, jcompagner
 */
public class SimpleUserNode implements IAdaptable
{
	private final UserNodeType type;
	private String name;
	private IDeveloperFeedback developerFeedback;
	private boolean enabled = true;

	private Image icon;

	private Object _realObject;
	private IPersist containingPersist; // Form or Solution if they are needed to determine more specific characteristics of the node

	public SimpleUserNode[] children;
	public SimpleUserNode parent;

	public final static int FLAG_HIDDEN = 1;
	public final static int FLAG_METADATA = 2;
	public final static int FLAG_NO_PK = 4;
	private int flags = 0;

	public final static int TEXT_ITALIC = 1;
	public final static int TEXT_GRAYED_OUT = 2;
	private int appearanceFlags = 0;

	protected boolean hidden = false;

	protected ClientSupport clientSupport = null;

	public SimpleUserNode(String displayName, UserNodeType type)
	{
		this.name = displayName;
		this.type = type;
	}

	public SimpleUserNode(String displayName, UserNodeType type, Object realObject, Image icon)
	{
		this(displayName, type);
		this._realObject = realObject;
		storeContainingPersistIfNeeded(_realObject);
		this.icon = icon;
	}

	public SimpleUserNode(String displayName, UserNodeType type, Object realObject, Image icon, Class< ? > realType, IDeveloperFeedback feedback)
	{
		this(displayName, type);
		this._realObject = realObject;
		storeContainingPersistIfNeeded(_realObject);
		this.icon = icon;
		this.clientSupport = AnnotationManagerReflection.getInstance().getClientSupport(realType, ClientSupport.Default);
		this.developerFeedback = feedback;
	}

	public SimpleUserNode(String displayName, UserNodeType type, Object realObject, IPersist containingPersist, Image icon)
	{
		this(displayName, type);
		this._realObject = realObject;
		this.icon = icon;
		this.containingPersist = containingPersist;
	}

	public SimpleUserNode(String displayName, UserNodeType type, IDeveloperFeedback developerFeedback, Object realObject, Image icon)
	{
		this(displayName, type);
		this._realObject = realObject;
		storeContainingPersistIfNeeded(_realObject);
		this.icon = icon;
		this.developerFeedback = developerFeedback;
	}

	public void setChildren(SimpleUserNode[] children)
	{
		this.children = children;
		if (hidden && this.children != null)
		{
			for (SimpleUserNode un : children)
			{
				un.hide();
			}
		}
	}

	private void storeContainingPersistIfNeeded(Object realObject)
	{
		if (realObject instanceof Form || realObject instanceof Solution)
		{
			containingPersist = (IPersist)realObject;
		}
		else
		{
			containingPersist = null;
		}
	}

	public IDeveloperFeedback getDeveloperFeedback()
	{
		return developerFeedback;
	}

	public void setDeveloperFeedback(IDeveloperFeedback developerFeedback)
	{
		this.developerFeedback = developerFeedback;
	}

	public void setToolTipText(final String txt)
	{
		final IDeveloperFeedback feedback = getDeveloperFeedback();
		if (feedback instanceof SimpleDeveloperFeedback)
		{
			((SimpleDeveloperFeedback)feedback).setToolTipText(txt);
		}
		else if (feedback != null)
		{
			setDeveloperFeedback(new IDeveloperFeedback()
			{

				public String getToolTipText()
				{
					return txt;
				}

				public String getSample()
				{
					return feedback.getSample();
				}

				public String getCode()
				{
					return feedback.getCode();
				}
			});
		}
		else
		{
			setDeveloperFeedback(new SimpleDeveloperFeedback(null, null, txt));
		}
	}

	public void setDisplayName(String s)
	{
		name = s;
	}

	public void setFlags(int flags)
	{
		this.flags = flags;
	}

	public int getFlags()
	{
		return flags;
	}

	public void setAppearenceFlags(int flags)
	{
		appearanceFlags = flags;
	}

	public int getAppearenceFlags()
	{
		return appearanceFlags;
	}

	public String getCode()
	{
		if (developerFeedback != null) return developerFeedback.getCode();
		return null;
	}

	public UserNodeType getType()
	{
		if (hidden)
		{
			return UserNodeType.GRAYED_OUT;
		}
		return type;
	}

	public UserNodeType getRealType()
	{
		return type;
	}

	public String getName()
	{
		return name;
	}

	public Object getRealObject()
	{
		return _realObject;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public boolean isHidden()
	{
		return hidden;
	}

	public void checkClientSupportInChildren()
	{
		for (int i = 0; clientSupport != ClientSupport.All && i < children.length; i++)
		{
			clientSupport = clientSupport == null ? children[i].getClientSupport() : clientSupport.union(children[i].getClientSupport());
		}
	}

	public boolean hide()
	{
		boolean needsRefresh = !hidden;
		hidden = true;
		if (children != null)
		{
			for (SimpleUserNode un : children)
			{
				needsRefresh = needsRefresh || un.hide();
			}
		}
		return needsRefresh;
	}

	public boolean unhide()
	{
		boolean needsRefresh = hidden;
		if (hidden)
		{
			if (children != null)
			{
				for (SimpleUserNode un : children)
				{
					needsRefresh = needsRefresh || un.unhide();
				}
			}
			hidden = false;
		}
		return needsRefresh;
	}

	/**
	 * @return the clientSupport
	 */
	public ClientSupport getClientSupport()
	{
		return clientSupport;
	}

	/**
	 * @param clientSupport the clientSupport to set
	 */
	public void setClientSupport(ClientSupport clientSupport)
	{
		this.clientSupport = clientSupport;
	}

	/**
	 * Method getToolTipText.
	 *
	 * @return Object
	 */
	public String getToolTipText()
	{
		if (developerFeedback == null) return null;
		String toolTip = developerFeedback.getToolTipText();
		if (_realObject instanceof IServerInternal && !((IServerInternal)_realObject).getName().equals(getName()) && toolTip != null)
		{
			// duplicate server tooltip
			return toolTip;
		}
		if (_realObject instanceof ISupportHTMLToolTipText)
		{
			return ((ISupportHTMLToolTipText)_realObject).toHTML();
		}
		if (toolTip != null)
		{
			if (toolTip.startsWith("<")) return toolTip;
			return "<pre>" + toolTip + "</pre>";
		}
		return developerFeedback.getCode();
	}

	/**
	 * Returns the icon.
	 *
	 * @return Object
	 */
	public Image getIcon()
	{
		return icon;
	}

	public Form getForm()
	{
		return containingPersist instanceof Form ? (Form)containingPersist : null;
	}

	public Solution getSolution()
	{
		if (containingPersist != null)
		{
			IRootObject ro = containingPersist.getRootObject();
			if (ro instanceof Solution)
			{
				return (Solution)ro;
			}
		}
		return null;
	}

	/**
	 * Sets the icon.
	 *
	 * @param icon The icon to set
	 */
	public void setIcon(Image icon)
	{
		this.icon = icon;
	}

	/**
	 * Returns the samlpeCode.
	 *
	 * @return String
	 */
	public String getSampleCode()
	{
		if (developerFeedback == null) return null;
		String sampleCode = developerFeedback.getSample();
		if (sampleCode != null) return sampleCode;
		return developerFeedback.getCode();
	}

	/**
	 * Method setRealObject.
	 *
	 * @param table
	 */
	public void setRealObject(Object o)
	{
		_realObject = o;
	}

	@Override
	public String toString()
	{
		return getName();
	}

	/**
	 * Searches up in the node hierarchy (including this node) for nodes who's real objects are of the given type.
	 *
	 * @param searchClass the class to search for in real objects.
	 * @return the first node found (searching from this node upwards) in the node hierarchy who's real object is of type searchClass, or null if not found.
	 */
	public SimpleUserNode getAncestorOfType(Class< ? > searchClass)
	{
		if (searchClass == null) return null;

		SimpleUserNode searchNode = this;
		while (searchNode != null && !searchClass.isInstance(searchNode._realObject))
		{
			searchNode = searchNode.parent;
		}
		return searchNode;
	}

	public Object getAdapter(Class adapter)
	{
		if (adapter == ISupportDeprecated.class)
		{
			switch (type)
			{
				case FORM_CONTROLLER :
					return null;
				default :
			}
		}
		return AdapterManager.getDefault().getAdapter(this, adapter);
	}


}