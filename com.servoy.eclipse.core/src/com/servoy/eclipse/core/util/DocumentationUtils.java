/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.core.util;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.ASTVisitor;
import org.eclipse.dltk.javascript.ast.Comment;
import org.eclipse.dltk.javascript.ast.JSNode;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.Statement;
import org.sablo.util.ValueReference;

/**
 * Class for sharing utility methods between eclipse (ui) plugins and j2db_documentation that also contains the gitbook generators.
 *
 * @author acostescu
 */
public class DocumentationUtils
{

	public static void extractWebObjectLevelDoc(Script script, Consumer<String> webObjectDescriptionConsumer) throws Exception
	{
		// see if it starts with an overall description of the component/service
		List<Statement> scriptChildren = script.getStatements();
		List<Comment> comments = script.getComments();
		ASTNode firstScriptChild = (scriptChildren.size() > 0 ? scriptChildren.getFirst() : null);
		Comment firstComment = (comments.size() > 0 ? comments.getFirst() : null);
		if (firstComment != null)
		{
			// Note about webObjectLevelComment: if it exists it will be the first comment in the script file,
			// at the beginning of the script file
			// It can be a "//" (not recommended) or "/* ... */" single or multi-line comment (which is preferred),
			// or even a doc comment /** ... */; it has to be a stand-alone doc comment though, not linked to anything,
			// not a comment of some method / variable that follows

			// see that comment is before the first script child
			ValueReference<Boolean> isWebObjectComment = new ValueReference<>(
				Boolean.valueOf(firstScriptChild == null || firstScriptChild.start() > firstComment.end()));

			// see that the comment is not documentation for the first script child or it's children (for example
			// a "var a;" would be a VoidExpression that contains a VariableDeclaration that can have getDocumentation()
			// on it if it's after a /** */ comment)
			if (isWebObjectComment.value.booleanValue() && firstComment.isDocumentation())
			{
				firstScriptChild.traverse(new ASTVisitor()
				{
					@Override
					public boolean visitGeneral(ASTNode node) throws Exception
					{
						if (node instanceof JSNode scriptChildJSNode && firstComment.equals(scriptChildJSNode.getDocumentation()))
							isWebObjectComment.value = Boolean.FALSE;
						return isWebObjectComment.value.booleanValue() && super.visitGeneral(node);
					}
				});
			}

			if (isWebObjectComment.value.booleanValue())
				webObjectDescriptionConsumer.accept(firstComment.getText());
		}
	}

}
