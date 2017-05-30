package com.servoy.eclipse.ui.views.solutionexplorer;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.builder.ISourceLineTracker;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.JSDeclaration;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.VariableDeclaration;
import org.eclipse.dltk.javascript.parser.JavaScriptParserUtil;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.dltk.utils.TextUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
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
	private final ArrayList<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>();

	@Override
	public void addListener(ILabelProviderListener listener)
	{
		listeners.add(listener);
	}

	//TODO
	public void fireChanged(IResource[] resource)
	{
		synchronized (listeners)
		{
			for (ILabelProviderListener listener : listeners)
				listener.labelProviderChanged(new LabelProviderChangedEvent(this, resource));
		}
	}

	@Override
	public void dispose()
	{
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
		if (element instanceof ISupportExtendsID && ((ISupportExtendsID)element).getExtendsID() > 0)
		{
			decoration.addOverlay(DLTKPluginImages.DESC_OVR_OVERRIDES, IDecoration.BOTTOM_RIGHT);
		}

		//TODO if (showAllAction.isChecked() &&
		if (element instanceof AbstractBase)
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
							return getProblemLevel(jsMarkers, sourceModule, getFunctionStatementForName(script, ((ScriptMethod)element).getName()));
						}

						if (element instanceof ScriptVariable)
						{
							return getProblemLevel(jsMarkers, sourceModule, getVariableDeclarationForName(script, ((ScriptVariable)element).getName()));
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
		}
		return -1;
	}

	//TODO refactor, copied from solex
	public int getProblemLevel(IMarker[] jsMarkers, ISourceModule sourceModule, ASTNode node) throws ModelException
	{
		int problemLevel = -1;
		if (jsMarkers == null || node == null) return problemLevel;
		ISourceLineTracker sourceLineTracker = null;
		for (IMarker marker : jsMarkers)
		{
			if (marker.getAttribute(IMarker.SEVERITY, -1) > problemLevel)
			{
				int start = marker.getAttribute(IMarker.CHAR_START, -1);
				if (start != -1)
				{
					if (node.sourceStart() <= start && start <= node.sourceEnd())
					{
						problemLevel = marker.getAttribute(IMarker.SEVERITY, -1);
					}
				}
				else
				{
					int line = marker.getAttribute(IMarker.LINE_NUMBER, -1); // 1 based
					if (line != -1)
					{
						if (sourceLineTracker == null) sourceLineTracker = TextUtils.createLineTracker(sourceModule.getSource());
						// getLineNumberOfOffset == 0 based so +1 to match the markers line
						if (sourceLineTracker.getLineNumberOfOffset(node.sourceStart()) + 1 <= line &&
							line <= sourceLineTracker.getLineNumberOfOffset(node.sourceEnd()) + 1)
						{
							problemLevel = marker.getAttribute(IMarker.SEVERITY, -1);
						}
					}
				}

			}
		}
		return problemLevel;
	}

	//TODO refactor, copied from solex
	private FunctionStatement getFunctionStatementForName(Script script, String metName)
	{
		for (JSDeclaration dec : script.getDeclarations())
		{
			if (dec instanceof FunctionStatement)
			{
				FunctionStatement fstmt = (FunctionStatement)dec;
				if (fstmt.getFunctionName().equals(metName))
				{
					return fstmt;
				}
			}
		}
		return null;
	}

	//TODO refactor, copied from solex
	private VariableDeclaration getVariableDeclarationForName(Script script, String varName)
	{
		for (JSDeclaration dec : script.getDeclarations())
		{
			if (dec instanceof VariableDeclaration)
			{
				VariableDeclaration varDec = (VariableDeclaration)dec;
				if (varDec.getVariableName().equals(varName))
				{
					return varDec;
				}
			}
		}
		return null;
	}

}
