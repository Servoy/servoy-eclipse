/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.model.builder;

import org.eclipse.dltk.ast.parser.IModuleDeclaration;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.SourceParserUtil;
import org.eclipse.dltk.core.builder.IBuildContext;
import org.eclipse.dltk.core.builder.IBuildParticipant;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.parser.JavaScriptParser;

/**
 * @author gboros
 */
public abstract class JSFileBuildParticipant implements IBuildParticipant
{
	protected Script getScript(IBuildContext context)
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
}
