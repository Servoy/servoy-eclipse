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

package com.servoy.eclipse.core.builder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.parser.IModuleDeclaration;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.SourceParserUtil;
import org.eclipse.dltk.core.builder.IBuildContext;
import org.eclipse.dltk.core.builder.IBuildParticipant;
import org.eclipse.dltk.core.builder.IBuildParticipantFactory;
import org.eclipse.dltk.javascript.ast.CallExpression;
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
		private static final String I18N_EXTERNALIZE_CALLBACK = "i18n.getI18NMessage"; //$NON-NLS-1$
		private static final String WARNING_MESSAGE = "Non-externalized string literal; it should be followed by //$NON-NLS-<n>$"; //$NON-NLS-1$

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

		private void reportWarning(Reporter reporter, int sourceStart, int sourceStop)
		{
			if (reporter != null)
			{
				reporter.setMessage(JSFileExternalizeProblem.NON_EXTERNALIZED_STRING, WARNING_MESSAGE);
				reporter.setSeverity(ProblemSeverity.WARNING);
				reporter.setRange(sourceStart, sourceStop);
				reporter.report();
			}
		}

		private void checkForNonExternalizedString(Script script, final Reporter reporter, final char[] content)
		{
			try
			{
				script.traverse(new org.eclipse.dltk.ast.ASTVisitor()
				{
					private boolean isEvaluatingI18N;
					private int parsingLineStartIdx, parsingLineEndIdx = -1;
					private int stringLiteralIdxInLine;

					@Override
					public boolean visitGeneral(ASTNode node) throws Exception
					{
						if (node instanceof StringLiteral)
						{
							if (!isEvaluatingI18N)
							{
								int nodeLineStartIdx = getNodeLineStartIdx((StringLiteral)node);
								if (parsingLineEndIdx < 0)
								{
									parsingLineEndIdx = getNodeLineEndIdx((StringLiteral)node);
								}
								if (parsingLineStartIdx != nodeLineStartIdx)
								{
									parsingLineStartIdx = nodeLineStartIdx;
									parsingLineEndIdx = getNodeLineEndIdx((StringLiteral)node);
									stringLiteralIdxInLine = 0;
								}

								stringLiteralIdxInLine++;

								if (!isMarkedForSkip(new String(content, parsingLineStartIdx, parsingLineEndIdx - parsingLineStartIdx + 1),
									stringLiteralIdxInLine)) reportWarning(reporter, node.sourceStart(), node.sourceEnd());
							}
						}
						else if (node instanceof CallExpression)
						{
							CallExpression mc = (CallExpression)node;
							if (I18N_EXTERNALIZE_CALLBACK.equals(mc.getExpression().toString()))
							{
								isEvaluatingI18N = true;
							}
						}
						return true;
					}

					@Override
					public void endvisitGeneral(ASTNode node) throws Exception
					{
						if (node instanceof CallExpression)
						{
							CallExpression mc = (CallExpression)node;
							if (I18N_EXTERNALIZE_CALLBACK.equals(mc.getExpression().toString()))
							{
								isEvaluatingI18N = false;
							}
						}
					}

					private int getNodeLineStartIdx(StringLiteral node)
					{
						int startIdx = node.sourceStart();
						while (startIdx > -1 && content[startIdx] != '\n')
							startIdx--;

						return startIdx + 1;
					}

					private int getNodeLineEndIdx(StringLiteral node)
					{
						int endIdx = node.sourceEnd();
						while (endIdx < content.length && content[endIdx] != '\n')
							endIdx++;

						return endIdx - 1;
					}

					private boolean isMarkedForSkip(String nodeLine, int nodeLineIdx)
					{
						return nodeLine.indexOf(new StringBuilder("//$NON-NLS-").append(nodeLineIdx).append("$").toString()) != -1; //$NON-NLS-1$ //$NON-NLS-2$
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