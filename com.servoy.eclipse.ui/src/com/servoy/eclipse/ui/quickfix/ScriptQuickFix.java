/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.ui.quickfix;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.dltk.compiler.problem.DefaultProblemIdentifier;
import org.eclipse.dltk.compiler.problem.IProblemIdentifier;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.javascript.core.JavaScriptProblems;
import org.eclipse.dltk.ui.editor.IScriptAnnotation;
import org.eclipse.dltk.ui.text.IScriptCorrectionContext;
import org.eclipse.dltk.ui.text.IScriptCorrectionProcessor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.quickfix.jsexternalize.TextFileEditResolution;
import com.servoy.eclipse.ui.resource.ImageResource;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewMethodAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewScopeAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewVariableAction;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author gboros
 */
public class ScriptQuickFix implements IScriptCorrectionProcessor, IMarkerResolutionGenerator
{
	private static IMarkerResolution[] NONE = new IMarkerResolution[0];

	/*
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker)
	{
		IProblemIdentifier problemId = DefaultProblemIdentifier.getProblemId(marker);

		if (problemId == JavaScriptProblems.UNDEFINED_JAVA_PROPERTY ||
			problemId == JavaScriptProblems.UNDEFINED_JAVA_METHOD ||
			problemId == JavaScriptProblems.UNDEFINED_FUNCTION)
		{
			IResource scriptFile = marker.getResource();
			if (scriptFile instanceof IFile)
			{
				int problemStartIdx = marker.getAttribute(IMarker.CHAR_START, -1);

				return new IMarkerResolution[] { new ScriptFixResolution(scriptFile.getProject(), (IFile)scriptFile, problemStartIdx, problemId) };
			}
		}

		return NONE;
	}

	/*
	 * @see org.eclipse.dltk.ui.text.IScriptCorrectionProcessor#canFix(org.eclipse.dltk.ui.editor.IScriptAnnotation)
	 */
	public boolean canFix(IScriptAnnotation annotation)
	{
		IProblemIdentifier problemId = annotation.getId();
		return problemId == JavaScriptProblems.UNDEFINED_JAVA_PROPERTY ||
			problemId == JavaScriptProblems.UNDEFINED_JAVA_METHOD ||
			problemId == JavaScriptProblems.UNDEFINED_FUNCTION;
	}

	/*
	 * @see org.eclipse.dltk.ui.text.IScriptCorrectionProcessor#canFix(org.eclipse.core.resources.IMarker)
	 */
	public boolean canFix(IMarker marker)
	{
		IProblemIdentifier problemId = DefaultProblemIdentifier.getProblemId(marker);
		return problemId == JavaScriptProblems.UNDEFINED_JAVA_PROPERTY ||
			problemId == JavaScriptProblems.UNDEFINED_JAVA_METHOD ||
			problemId == JavaScriptProblems.UNDEFINED_FUNCTION;
	}

	/*
	 * @see org.eclipse.dltk.ui.text.IScriptCorrectionProcessor#computeQuickAssistProposals(org.eclipse.dltk.ui.editor.IScriptAnnotation,
	 * org.eclipse.dltk.ui.text.IScriptCorrectionContext)
	 */
	public void computeQuickAssistProposals(IScriptAnnotation annotation, IScriptCorrectionContext context)
	{
		IProblemIdentifier problemId = annotation.getId();

		if (problemId == JavaScriptProblems.UNDEFINED_JAVA_PROPERTY ||
			problemId == JavaScriptProblems.UNDEFINED_JAVA_METHOD ||
			problemId == JavaScriptProblems.UNDEFINED_FUNCTION)
		{
			IResource scriptFile = annotation.getSourceModule().getResource();
			if (scriptFile instanceof IFile)
			{
				int problemStartIdx = context.getInvocationContext().getOffset();
				context.addResolution(new ScriptFixResolution(scriptFile.getProject(), (IFile)scriptFile, problemStartIdx, problemId), annotation);
			}
		}
	}

	/*
	 * @see org.eclipse.dltk.ui.text.IScriptCorrectionProcessor#computeQuickAssistProposals(org.eclipse.core.resources.IMarker,
	 * org.eclipse.dltk.ui.text.IScriptCorrectionContext)
	 */
	public void computeQuickAssistProposals(IMarker marker, IScriptCorrectionContext context)
	{
		IProblemIdentifier problemId = DefaultProblemIdentifier.getProblemId(marker);
		if (problemId == JavaScriptProblems.UNDEFINED_JAVA_PROPERTY ||
			problemId == JavaScriptProblems.UNDEFINED_JAVA_METHOD ||
			problemId == JavaScriptProblems.UNDEFINED_FUNCTION)
		{
			IResource scriptFile = marker.getResource();
			if (scriptFile instanceof IFile)
			{
				int problemStartIdx = context.getInvocationContext().getOffset();
				context.addResolution(new ScriptFixResolution(scriptFile.getProject(), (IFile)scriptFile, problemStartIdx, problemId), marker);
			}
		}
	}

	class ScriptFixResolution extends TextFileEditResolution
	{
		private String scope;
		private String scriptItem;
		private final IProblemIdentifier problemId;
		private final IProject project;

		/**
		 * @param scriptFile
		 * @param problemStartIdx
		 */
		public ScriptFixResolution(IProject project, IFile scriptFile, int problemStartIdx, IProblemIdentifier problemId)
		{
			super(scriptFile, problemStartIdx);
			this.problemId = problemId;
			this.project = project;
			ISourceModule scriptFileSourceModule = DLTKCore.createSourceModuleFrom(scriptFile);

			try
			{
				String contents = scriptFileSourceModule.getBuffer().getContents();
				String scriptProperty = ScriptQuickFix.getTokenAt(contents, problemStartIdx);
				// Parse scriptProperty according to rules
				this.scope = null;
				this.scriptItem = null;
				if (scriptProperty != null && scriptProperty.startsWith("scopes."))
				{
					String[] parts = scriptProperty.split("\\.");
					this.scope = parts.length > 1 ? parts[1] : null;
					if (parts.length >= 3)
					{
						this.scriptItem = parts[2];
					}
				}
				else if (scriptProperty != null)
				{
					String[] parts = scriptProperty.split("\\.");
					if (parts.length >= 1)
					{
						this.scriptItem = parts[0];
					}
				}
			}
			catch (ModelException ex)
			{
				ServoyLog.logError(ex);
			}
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
		 */
		@Override
		public String getDescription()
		{
			return getLabel() + (this.scriptItem != null
				? (" '" + this.scriptItem + "' in the " + (this.scope != null ? "'" + this.scope + "'" + " scope" : "current file")) : " " + this.scope);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.ui.IMarkerResolution2#getImage()
		 */
		@Override
		public Image getImage()
		{
			return ImageResource.INSTANCE.getImage(Activator.loadImageDescriptorFromBundle("correction_change.gif"));
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.ui.IMarkerResolution#getLabel()
		 */
		@Override
		public String getLabel()
		{
			return "Create " + (this.scriptItem != null ? (problemId == JavaScriptProblems.UNDEFINED_JAVA_PROPERTY ? "variable" : "method") : " scope");
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.eclipse.ui.quickfix.jsexternalize.TextFileEditResolution#run()
		 */
		@Override
		public void run()
		{
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			final ServoyProject editingServoyProject = servoyModel.getServoyProject(this.project.getName());
			if (editingServoyProject != null)
			{
				if (this.scriptItem == null) // create scope
				{
					NewScopeAction.createScope(this.scope, UIUtils.getActiveShell(), editingServoyProject, false);
				}
				else
				{
					// Determine if this is a form or scope/global script file and get the name
					ScriptFileTypeAndName typeAndName = ScriptQuickFix.getScriptFileTypeAndName(this.scriptFile);
					final IPersist parent;
					String scopeType;
					String scopeName = null;
					final Object validationContext;
					if (typeAndName != null && "form".equals(typeAndName.type) && this.scope == null)
					{
						parent = editingServoyProject.getEditingFlattenedSolution().getForm(typeAndName.name);
						scopeType = "form";
						validationContext = parent;
					}
					else if ((typeAndName != null && "scope".equals(typeAndName.type)) || this.scope != null)
					{
						parent = editingServoyProject.getEditingFlattenedSolution().getSolution();
						scopeName = this.scope != null ? this.scope : typeAndName.name;
						validationContext = scopeName;
						scopeType = "global";
					}
					else
					{
						return;
					}


					if (this.problemId == JavaScriptProblems.UNDEFINED_JAVA_PROPERTY) // create variable
					{
						NewVariableAction.VariableEditDialog askUserDialog = NewVariableAction
							.showVariableEditDialog(UIUtils.getActiveShell(), validationContext, scopeType, this.scriptItem);

						String variableName = askUserDialog.getVariableName();
						int variableType = askUserDialog.getVariableType();
						String defaultValue = askUserDialog.getVariableDefaultValue();

						if (variableName != null)
						{
							NewVariableAction.createNewVariable(UIUtils.getActiveShell(), parent, scopeName, variableName, variableType,
								defaultValue, false);
						}
					}
					else // create method
					{
						NewMethodAction.createNewMethod(UIUtils.getActiveShell(), parent, null, false,
							this.scriptItem, scopeName, null, null, null, false);
					}

				}
			}
		}
	}

	/**
	 * Returns the type ("form" or "scope") and the name (form name or scope name) for the given script file.
	 * Returns null if the file is not a form or scope script.
	 */
	private static ScriptFileTypeAndName getScriptFileTypeAndName(IFile scriptFile)
	{
		if (scriptFile == null) return null;
		String path = scriptFile.getProjectRelativePath().toString();
		String fileName = scriptFile.getName();
		int dotIdx = fileName.lastIndexOf('.');
		String name = (dotIdx > 0) ? fileName.substring(0, dotIdx) : fileName;
		if (path.startsWith("forms/"))
		{
			return new ScriptFileTypeAndName("form", name);
		}
		else if (path.startsWith("datasources/"))
		{
			return null;
		}
		return new ScriptFileTypeAndName("scope", name);
	}

	/**
	 * Simple holder for script file type and name.
	 */
	private static class ScriptFileTypeAndName
	{
		public final String type; // "form" or "scope"
		public final String name; // form name or scope name

		public ScriptFileTypeAndName(String type, String name)
		{
			this.type = type;
			this.name = name;
		}
	}

	private static String getTokenAt(String contents, int idx)
	{
		if (contents == null || idx < 0 || idx >= contents.length()) return null;
		int start = idx;
		int end = idx;
		// Move start left to the first non-token character
		while (start > 0)
		{
			char ch = contents.charAt(start - 1);
			if (Character.isJavaIdentifierPart(ch) || ch == '.')
			{
				start--;
			}
			else
			{
				break;
			}
		}
		// Move end right to the first non-token character
		while (end < contents.length())
		{
			char ch = contents.charAt(end);
			if (Character.isJavaIdentifierPart(ch) || ch == '.')
			{
				end++;
			}
			else
			{
				break;
			}
		}
		return contents.substring(start, end);
	}
}
