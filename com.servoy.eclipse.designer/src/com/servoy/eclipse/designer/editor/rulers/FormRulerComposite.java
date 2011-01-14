package com.servoy.eclipse.designer.editor.rulers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.swt.widgets.Composite;

/**
 * Composite to show rulers.
 * Extends default RulerComposite to set own key handlers.
 * 
 * @author rgansevles
 *
 */
public class FormRulerComposite extends RulerComposite
{
	/**
	 * @param parent
	 * @param style
	 */
	public FormRulerComposite(Composite parent, int style)
	{
		super(parent, style);
	}

	@Override
	public void setGraphicalViewer(final ScrollingGraphicalViewer primaryViewer)
	{
		super.setGraphicalViewer(primaryViewer);
		primaryViewer.addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (RulerProvider.PROPERTY_RULER_VISIBILITY.equals(evt.getPropertyName()) &&
					Boolean.TRUE.equals(primaryViewer.getProperty(RulerProvider.PROPERTY_RULER_VISIBILITY)))
				{
					setkeyHandlers();
				}
			}

		});
		setkeyHandlers();
	}

	protected void setkeyHandlers()
	{
		// replace the key handlers
		getTop().setKeyHandler(new FormRulerKeyHandler(getTop()));
		getLeft().setKeyHandler(new FormRulerKeyHandler(getLeft()));
	}
}