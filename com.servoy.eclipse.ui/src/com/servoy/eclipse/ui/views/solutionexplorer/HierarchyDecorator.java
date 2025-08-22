package com.servoy.eclipse.ui.views.solutionexplorer;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.parser.JavaScriptParserUtil;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportDeprecatedAnnotation;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.PersistHelper;

public class HierarchyDecorator implements ILightweightLabelDecorator
{
	public static final String ID = "com.servoy.eclipse.ui.views.solutionexplorer.FormHierarchyView.PersistDecorator";
	private final ArrayList<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>();
	private static IDialogSettings fDialogSettings = Activator.getDefault().getDialogSettings();

	@Override
	public void addListener(ILabelProviderListener listener)
	{
		listeners.add(listener);
	}

	public void fireChanged(IPersist[] persists)
	{
		synchronized (listeners)
		{
			for (ILabelProviderListener listener : listeners)
				listener.labelProviderChanged(new LabelProviderChangedEvent(this, persists));
		}
	}

	@Override
	public void dispose()
	{
		synchronized (listeners)
		{
			for (ILabelProviderListener listener : listeners)
				removeListener(listener);
		}
	}

	@Override
	public boolean isLabelProperty(Object element, String property)
	{
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener)
	{
		synchronized (listeners)
		{
			listeners.remove(listener);
		}
	}

	@Override
	public void decorate(Object element, IDecoration decoration)
	{
		if (!(element instanceof IPersist)) return;

		ImageDescriptor imageDescriptor = null;
		//problem (warning/error) decoration
		int severity = getProblemType((IPersist)element);
		if (severity == IMarker.SEVERITY_ERROR) imageDescriptor = DLTKPluginImages.DESC_OVR_ERROR;
		else if (severity == IMarker.SEVERITY_WARNING) imageDescriptor = DLTKPluginImages.DESC_OVR_WARNING;

		decoration.addOverlay(imageDescriptor, IDecoration.BOTTOM_LEFT);

		if (element instanceof ISupportDeprecatedAnnotation)
		{
			ISupportDeprecatedAnnotation isda = (ISupportDeprecatedAnnotation)element;
			if (isda.isDeprecated())
			{
				decoration.addOverlay(DLTKPluginImages.DESC_OVR_DEPRECATED, IDecoration.UNDERLAY);
			}
		}
		if (element instanceof ScriptMethod || element instanceof ScriptVariable)
		{
			//constructor decoration for functions
			if (element instanceof ScriptMethod && ((ScriptMethod)element).isConstructor())
			{
				decoration.addOverlay(DLTKPluginImages.DESC_OVR_CONSTRUCTOR, IDecoration.TOP_RIGHT);
			}

			//override decoration for functions
			if (element instanceof ScriptMethod && PersistHelper.getOverridenMethod((ScriptMethod)element) != null)
			{
				decoration.addOverlay(DLTKPluginImages.DESC_OVR_OVERRIDES, IDecoration.BOTTOM_RIGHT);
			}
		}

		if (element instanceof Form) return;
		if (element instanceof ISupportExtendsID && ((ISupportExtendsID)element).getExtendsID() != null)
		{
			decoration.addOverlay(DLTKPluginImages.DESC_OVR_OVERRIDES, IDecoration.BOTTOM_RIGHT);
		}
		if (fDialogSettings.getBoolean(FormHierarchyView.DIALOGSTORE_SHOW_ALL_MEMBERS) && element instanceof AbstractBase)
		{
			decoration.addSuffix(" [" + ((Form)((AbstractBase)element).getAncestor(IRepository.FORMS)).getName() + "]");
		}
	}

	private int getProblemType(IPersist element)
	{
		if (element instanceof ScriptVariable || element instanceof ScriptMethod)
		{
			IFile jsResource = ServoyModel.getWorkspace().getRoot().getFile(new Path(SolutionSerializer.getScriptPath(element, false)));
			if (jsResource.exists())
			{
				try
				{
					IMarker[] jsMarkers = jsResource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
					if (jsMarkers != null && jsMarkers.length > 0)
					{
						ISourceModule sourceModule = DLTKCore.createSourceModuleFrom(jsResource);
						Script script = JavaScriptParserUtil.parse(sourceModule);

						if (element instanceof ScriptMethod)
						{
							return DecoratorHelper.getProblemLevel(jsMarkers, sourceModule,
								DecoratorHelper.getFunctionStatementForName(script, ((ScriptMethod)element).getName()));
						}

						if (element instanceof ScriptVariable)
						{
							return DecoratorHelper.getProblemLevel(jsMarkers, sourceModule,
								DecoratorHelper.getVariableDeclarationForName(script, ((ScriptVariable)element).getName()));
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		else if (element instanceof Form)
		{
			IFile resource = ServoyModel.getWorkspace().getRoot().getFile(new Path(SolutionSerializer.getRelativeFilePath(element, false)));
			{
				if (resource.exists())
				{
					try
					{
						IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
						if (markers != null)
						{
							int severity = -1;
							for (IMarker marker : markers)
							{
								severity = marker.getAttribute(IMarker.SEVERITY, -1);
								if (severity == IMarker.SEVERITY_ERROR) break;
							}
							return severity;
						}
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
				else
				{
					ServoyLog.logInfo("Could not find frm file of form " + ((Form)element).getName());
				}
			}
		}
		return -1;
	}
}
