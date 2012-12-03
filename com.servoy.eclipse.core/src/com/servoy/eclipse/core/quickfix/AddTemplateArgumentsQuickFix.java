/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.core.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.preferences.JSDocScriptTemplates;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.MethodTemplate;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.util.UUID;

public class AddTemplateArgumentsQuickFix implements IMarkerResolution
{
	private final String uuid;
	private final String solutionName;
	private final int lineNumber;
	private final String eventName;

	public AddTemplateArgumentsQuickFix(String uuid, String solName, int lineNumber, String eventName)
	{
		this.uuid = uuid;
		this.solutionName = solName;
		this.lineNumber = lineNumber;
		this.eventName = eventName;
	}

	public String getLabel()
	{
		return "Apply event template to javascript method.";
	}

	public void run(IMarker marker)
	{
		if (uuid != null && lineNumber > 0)
		{
			UUID id = UUID.fromString(uuid);
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (servoyProject != null)
			{
				try
				{
					IPersist persist = servoyProject.getEditingPersist(id);
					if (persist instanceof ScriptMethod)
					{
						ScriptMethod method = (ScriptMethod)persist;
						String declaration = method.getDeclaration();
						int lineCount = lineNumber - method.getLineNumberOffset();
						if (lineCount > 0)
						{
							int position;
							for (position = declaration.indexOf("*/\n") + 3; position < declaration.length(); position++)
							{
								if (declaration.charAt(position) == '\n') lineCount--;
								if (lineCount == 0) break;
							}
							if (lineCount == 0)
							{
								declaration = declaration.substring(0, position) +
									"\n\t//TODO WARNING: do rewrite your code to not depend on 'arguments', append them to the parameter list.\n" +
									declaration.substring(position + 1);
							}
							method.setDeclaration(declaration);
							String source = method.getSource();
							if (source.startsWith("\t")) source = source.substring(1);
							MethodTemplate template = MethodTemplate.getTemplate(method.getClass(), eventName);
							MethodTemplate mixedTemplate = new MethodTemplate(template.getDescription(), new MethodArgument(method.getName(),
								template.getSignature().getType(), template.getSignature().getDescription()), template.getArguments(), source, false);
							JSDocScriptTemplates prefs = JSDocScriptTemplates.getTemplates();
							String userTemplate = prefs.getMethodTemplate();
							String comment = SolutionSerializer.getComment(method, null, ServoyModel.getDeveloperRepository());
							String methodDeclaration = mixedTemplate.getMethodDeclaration(null, null, userTemplate);
							if (comment != null && comment.indexOf(SolutionSerializer.PROPERTIESKEY) != -1)
							{
								int methodDeclarionCommentEnd = methodDeclaration.indexOf(SolutionSerializer.SV_COMMENT_END);
								String methodDoc = methodDeclaration.substring(0, methodDeclarionCommentEnd);
								StringBuilder sb = new StringBuilder();
								int startIndex = methodDoc.indexOf("@param");
								while (startIndex != -1)
								{
									int endIndex = methodDoc.indexOf("\n", startIndex);
									sb.append(methodDoc.subSequence(startIndex, endIndex));
									sb.append("\n * ");
									startIndex = methodDoc.indexOf("@param", startIndex + 5);
								}
								sb.append("\n * ");
								int propertiesIndex = comment.indexOf(SolutionSerializer.PROPERTIESKEY);
								comment = comment.substring(0, propertiesIndex) + sb.toString() + comment.substring(propertiesIndex);
								methodDeclaration = comment +
									methodDeclaration.substring(methodDeclarionCommentEnd + SolutionSerializer.SV_COMMENT_END.length());
							}
							method.setDeclaration(methodDeclaration);
							method.flagChanged();
						}

						servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, true);
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}

	}
}