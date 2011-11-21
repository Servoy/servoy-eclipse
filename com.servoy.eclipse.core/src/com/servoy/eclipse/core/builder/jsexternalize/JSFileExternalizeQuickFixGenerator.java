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

package com.servoy.eclipse.core.builder.jsexternalize;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.compiler.problem.DefaultProblemIdentifier;
import org.eclipse.dltk.compiler.problem.IProblemIdentifier;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.javascript.ast.Comment;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.StringLiteral;
import org.eclipse.dltk.javascript.parser.JavaScriptParserUtil;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * JS file externalize marker resolution generator
 * @author gboros
 *
 */
public class JSFileExternalizeQuickFixGenerator implements IMarkerResolutionGenerator
{
	private static IMarkerResolution[] NONE = new IMarkerResolution[0];

	/*
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker)
	{
		IProblemIdentifier problemId = DefaultProblemIdentifier.getProblemId(marker);

		if (problemId == JSFileExternalizeProblem.NON_EXTERNALIZED_STRING)
		{
			return new IMarkerResolution[] { new AddMissingNLS(), new GenerateSuppressWarningsResolution("nls") }; //$NON-NLS-1$
		}

		return NONE;
	}

	class AddMissingNLS extends TextFileEditMarkerResolution
	{

		/*
		 * @see org.eclipse.ui.IMarkerResolution#getLabel()
		 */
		public String getLabel()
		{
			return "Insert missing $NON-NLS-<n>$";
		}

		/*
		 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
		 */
		public void run(final IMarker marker)
		{
			final IFile scriptFile = (IFile)marker.getResource();
			ISourceModule scriptFileSourceModule = DLTKCore.createSourceModuleFrom(scriptFile);
			Script script = JavaScriptParserUtil.parse(scriptFileSourceModule);

			try
			{
				script.traverse(new StringLiteralVisitor(scriptFileSourceModule.getSourceAsCharArray())
				{

					@Override
					public boolean visitStringLiteral(StringLiteral node, int lineStartIdx, int lineEndIdx, int stringLiteralIdx)
					{
						if (node.getRange().getOffset() == marker.getAttribute(IMarker.CHAR_START, -1))
						{
							InsertEdit nonNLSTextEdit = new InsertEdit(lineEndIdx + 1, " //$NON-NLS-" + stringLiteralIdx + "$"); //$NON-NLS-1$ //$NON-NLS-2$
							applyTextEdit(scriptFile, nonNLSTextEdit);
							return false;
						}

						return true;
					}

				});
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}

	class GenerateSuppressWarningsResolution extends TextFileEditMarkerResolution
	{

		private final String type;

		public GenerateSuppressWarningsResolution(String type)
		{
			this.type = type;
		}

		public String getLabel()
		{
			return "Generate SuppressWarnings(" + type + ")";
		}

		private String getAnnotation()
		{
			return "@SuppressWarnings(" + type + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		private FunctionStatement getFunction(final IFile scriptFile, final int position)
		{
			Script script = JavaScriptParserUtil.parse(DLTKCore.createSourceModuleFrom(scriptFile));
			final FunctionStatement[] functionStatement = new FunctionStatement[] { null };

			try
			{
				script.traverse(new org.eclipse.dltk.ast.ASTVisitor()
				{
					@Override
					public boolean visitGeneral(ASTNode node) throws Exception
					{
						if (node instanceof FunctionStatement)
						{
							FunctionStatement fs = (FunctionStatement)node;
							if (fs.sourceStart() < position && position < fs.sourceEnd())
							{
								functionStatement[0] = (FunctionStatement)node;
								return false;
							}
						}
						return true;
					}
				});
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}

			return functionStatement[0];
		}


		public void run(IMarker marker)
		{
			IFile scriptFile = (IFile)marker.getResource();

			FunctionStatement fs = getFunction(scriptFile, marker.getAttribute(IMarker.CHAR_START, -1));
			if (fs != null)
			{
				int insertOffset = -1;
				Comment documentation = fs.getDocumentation();
				if (documentation.isDocumentation()) // it must have doc
				{
					insertOffset = documentation.sourceEnd() - 2;
					InsertEdit suppressTextEdit = new InsertEdit(insertOffset, "* " + getAnnotation() + "\n "); //$NON-NLS-1$ //$NON-NLS-2$
					applyTextEdit(scriptFile, suppressTextEdit);
				}
			}
		}
	}

	abstract class TextFileEditMarkerResolution implements IMarkerResolution
	{
		protected void applyTextEdit(IFile scriptFile, TextEdit textEdit)
		{
			ITextFileBufferManager textFileBufferManager = FileBuffers.getTextFileBufferManager();
			try
			{
				textFileBufferManager.connect(scriptFile.getFullPath(), LocationKind.IFILE, null);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
				return;
			}

			try
			{
				ITextFileBuffer textFileBuffer = textFileBufferManager.getTextFileBuffer(scriptFile.getFullPath(), LocationKind.IFILE);
				IDocument document = textFileBuffer.getDocument();

				FileEditorInput editorInput = new FileEditorInput(scriptFile);
				final IEditorPart openEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ? null
					: PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findEditor(editorInput);

				boolean dirty = openEditor != null ? openEditor.isDirty() : textFileBuffer.isDirty();

				try
				{
					textEdit.apply(document);
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}

				if (!dirty)
				{
					if (openEditor != null)
					{
						openEditor.doSave(null);
					}
					else
					{
						try
						{
							textFileBuffer.commit(null, true);
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}
			finally
			{
				try
				{
					textFileBufferManager.disconnect(scriptFile.getFullPath(), LocationKind.IFILE, null);
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}
}
