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

package com.servoy.eclipse.ui.quickfix.jsexternalize;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.parser.IModuleDeclaration;
import org.eclipse.dltk.compiler.problem.IProblemIdentifier;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.SourceParserUtil;
import org.eclipse.dltk.core.builder.IBuildContext;
import org.eclipse.dltk.core.builder.IBuildParticipant;
import org.eclipse.dltk.core.builder.IBuildParticipantFactory;
import org.eclipse.dltk.javascript.ast.Comment;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.StringLiteral;
import org.eclipse.dltk.javascript.parser.JavaScriptParser;
import org.eclipse.dltk.javascript.parser.Reporter;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Class used for generating warnings for non-externalized strings in the js file
 * @author gboros
 */
public class JSFileExternalize implements IBuildParticipantFactory
{

	private StringExternalizeParser stringExternalizeParser;

	/*
	 * @see org.eclipse.dltk.core.builder.IBuildParticipantFactory#createBuildParticipant(org.eclipse.dltk.core.IScriptProject)
	 */
	public IBuildParticipant createBuildParticipant(IScriptProject project) throws CoreException
	{
		return getParser();
	}

	private StringExternalizeParser getParser()
	{
		if (stringExternalizeParser == null)
		{
			stringExternalizeParser = new StringExternalizeParser();
		}
		return stringExternalizeParser;
	}

	class StringExternalizeParser implements IBuildParticipant
	{
		private static final String WARNING_MESSAGE = "Non-externalized string literal; it should be followed by ";
		private static final String SUPPRESS_WARNING_NLS = "@SuppressWarnings(nls)";

		/*
		 * @see org.eclipse.dltk.core.builder.IBuildParticipant#build(org.eclipse.dltk.core.builder.IBuildContext)
		 */
		public void build(IBuildContext context) throws CoreException
		{
			final Script script = getScript(context);
			if (script != null)
			{
				checkForNonExternalizedString(script, new Reporter(context.getLineTracker(), context.getProblemReporter()), context.getContents());
			}
		}

		private Script getScript(IBuildContext context)
		{
			final IModuleDeclaration savedAST = (IModuleDeclaration)context.get(IBuildContext.ATTR_MODULE_DECLARATION);
			if (savedAST instanceof Script)
			{
				return (Script)savedAST;
			}
			final ISourceModule module = context.getSourceModule();
			if (module != null)
			{
				final IModuleDeclaration declaration = SourceParserUtil.parse(module, context.getProblemReporter());
				if (declaration instanceof Script)
				{
					context.set(IBuildContext.ATTR_MODULE_DECLARATION, declaration);
					return (Script)declaration;
				}
			}
			final JavaScriptParser parser = new JavaScriptParser();
			final Script script = parser.parse(context, context.getProblemReporter());
			context.set(IBuildContext.ATTR_MODULE_DECLARATION, script);
			return script;
		}

		private void reportWarning(Reporter reporter, int sourceStart, int sourceStop, int stringLiteralIdx)
		{
			if (reporter != null)
			{
				reporter.reportProblem(JSFileExternalizeProblem.NON_EXTERNALIZED_STRING,
					WARNING_MESSAGE + "//$NON-NLS-" + stringLiteralIdx + "$", sourceStart, sourceStop);
			}
		}

		private void checkForNonExternalizedString(Script script, final Reporter reporter, final char[] content)
		{
			try
			{
				script.traverse(new StringLiteralVisitor(content)
				{
					private final List<IProblemIdentifier> suppressProblems = Arrays.asList(new IProblemIdentifier[] { JSFileExternalizeProblem.NON_EXTERNALIZED_STRING });

					/*
					 * @see com.servoy.eclipse.core.builder.StringLiteralVisitor#visitStringLiteral(org.eclipse.dltk.javascript.ast.StringLiteral, int, int,
					 * int)
					 */
					@Override
					public boolean visitStringLiteral(StringLiteral node, int lineStartIdx, int lineEndIdx, int stringLiteralIdx)
					{
						if (!isMarkedForSkip(new String(content, lineStartIdx, lineEndIdx - lineStartIdx + 1), stringLiteralIdx)) reportWarning(reporter,
							node.sourceStart(), node.sourceEnd(), stringLiteralIdx);
						return true;
					}

					@Override
					public boolean visitGeneral(ASTNode node) throws Exception
					{
						if (node instanceof FunctionStatement)
						{
							Comment comment = ((FunctionStatement)node).getDocumentation();
							if (comment != null)
							{
								String commentStr = comment.getText();
								if (commentStr != null && commentStr.indexOf(SUPPRESS_WARNING_NLS) != -1)
								{
									reporter.pushSuppressWarnings(suppressProblems);
								}
							}
						}
						return super.visitGeneral(node);
					}

					@Override
					public void endvisitGeneral(ASTNode node) throws Exception
					{
						if (node instanceof FunctionStatement)
						{
							reporter.popSuppressWarnings();
						}
						super.endvisitGeneral(node);
					}

					private boolean isMarkedForSkip(String nodeLine, int nodeLineIdx)
					{
						return nodeLine.indexOf(new StringBuilder("//$NON-NLS-").append(nodeLineIdx).append("$").toString()) != -1;
					}
				});
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}
}