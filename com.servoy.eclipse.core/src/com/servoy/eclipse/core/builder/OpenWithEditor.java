package com.servoy.eclipse.core.builder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.model.extensions.IMarkerAttributeContributor;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptVariable;

/**
 * Implementation for a com.servoy.eclipse.model extension point.
 * @author acostescu
 */
public class OpenWithEditor implements IMarkerAttributeContributor
{

	public void contributeToMarker(IMarker marker, IPersist persist)
	{
		String contentTypeIdentifier = null;
		if (persist.getAncestor(IRepository.FORMS) != null && !(persist instanceof ScriptVariable))
		{
			contentTypeIdentifier = PersistEditorInput.FORM_RESOURCE_ID;
		}
		else if (persist.getAncestor(IRepository.RELATIONS) != null)
		{
			contentTypeIdentifier = PersistEditorInput.RELATION_RESOURCE_ID;
		}
		else if (persist.getAncestor(IRepository.VALUELISTS) != null)
		{
			contentTypeIdentifier = PersistEditorInput.VALUELIST_RESOURCE_ID;
		}
		if (contentTypeIdentifier != null)
		{
			try
			{
				if (ModelUtils.isUIRunning())
				{
					marker.setAttribute(
						IDE.EDITOR_ID_ATTR,
						PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
							Platform.getContentTypeManager().getContentType(contentTypeIdentifier)).getId());
				}
				marker.setAttribute("elementUuid", persist.getUUID().toString()); //$NON-NLS-1$
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

}