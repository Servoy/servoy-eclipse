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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.ast.parser.IModuleDeclaration;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.core.PreferencesLookupDelegate;
import org.eclipse.dltk.core.SourceParserUtil;
import org.eclipse.dltk.javascript.ast.Comment;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.StringLiteral;
import org.eclipse.dltk.ui.editor.saveparticipant.IPostSaveListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Javascript file post save listener used for removing unused $NON-NLS$ tags
 * @author gboros
 */
public class PostSaveNLSRemover implements IPostSaveListener
{
	public static final String ID = "NLSRemover"; //$NON-NLS-1$
	public static final String EDITOR_SAVE_PARTICIPANT_PREFIX = "editor_save_participant_"; //$NON-NLS-1$

	/* 
	 * @see org.eclipse.dltk.ui.editor.saveparticipant.IPostSaveListener#getName()
	 */
	public String getName()
	{
		return ID;
	}

	/* 
	 * @see org.eclipse.dltk.ui.editor.saveparticipant.IPostSaveListener#getId()
	 */
	public String getId()
	{
		return ID;
	}

	/* 
	 * @see org.eclipse.dltk.ui.editor.saveparticipant.IPostSaveListener#isEnabled(org.eclipse.dltk.core.ISourceModule)
	 */
	public boolean isEnabled(ISourceModule compilationUnit)
	{
		return new PreferencesLookupDelegate(compilationUnit.getScriptProject().getProject()).getBoolean(Activator.PLUGIN_ID, EDITOR_SAVE_PARTICIPANT_PREFIX +
			getId());
	}

	/* 
	 * @see org.eclipse.dltk.ui.editor.saveparticipant.IPostSaveListener#needsChangedRegions(org.eclipse.dltk.core.ISourceModule)
	 */
	public boolean needsChangedRegions(ISourceModule compilationUnit) throws CoreException
	{
		return false;
	}

	/* 
	 * @see org.eclipse.dltk.ui.editor.saveparticipant.IPostSaveListener#saved(org.eclipse.dltk.core.ISourceModule, org.eclipse.jface.text.IRegion[],
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void saved(ISourceModule compilationUnit, IRegion[] changedRegions, IProgressMonitor monitor) throws CoreException
	{
		IModuleDeclaration declaration = SourceParserUtil.parse(compilationUnit, null);
		if (declaration instanceof Script)
		{
			Script script = (Script)declaration;
			List<Comment> allComments = script.getComments();

			// remove non NLS comments
			final ArrayList<Comment> allNLSComments = new ArrayList<Comment>();
			Iterator<Comment> allCommentsIte = allComments.iterator();
			Comment c;
			while (allCommentsIte.hasNext())
			{
				c = allCommentsIte.next();
				if (!c.isMultiLine() && c.getText().startsWith("//$NON-NLS-")) //$NON-NLS-1$
				{
					allNLSComments.add(c);
				}
			}

			// get a map with all nls comments and associated list of valid ids
			final Map<Comment, ArrayList<Integer>> nlsUsedIdxMap = new HashMap<Comment, ArrayList<Integer>>();

			try
			{
				script.traverse(new StringLiteralVisitor(compilationUnit.getSourceAsCharArray())
				{

					@Override
					public boolean visitStringLiteral(StringLiteral node, int lineStartIdx, int lineEndIdx, int stringLiteralIdx)
					{
						int commentStartIdx;

						for (Comment nlsComment : allNLSComments)
						{
							commentStartIdx = nlsComment.getRange().getOffset();
							if (lineStartIdx < commentStartIdx && commentStartIdx < lineEndIdx)
							{
								if (!nlsUsedIdxMap.containsKey(nlsComment))
								{
									nlsUsedIdxMap.put(nlsComment, new ArrayList<Integer>());
								}

								if (nlsComment.getText().indexOf("//$NON-NLS-" + stringLiteralIdx + "$") != -1) //$NON-NLS-1$ //$NON-NLS-2$
								{
									nlsUsedIdxMap.get(nlsComment).add(Integer.valueOf(stringLiteralIdx));
								}
								break;
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

			// rewrite all nls comments using only the valid ones
			writeUsedNLS((IFile)compilationUnit.getResource(), nlsUsedIdxMap);
		}
	}

	private void writeUsedNLS(IFile file, Map<Comment, ArrayList<Integer>> nlsUsedIdxMap)
	{
		Iterator<Map.Entry<Comment, ArrayList<Integer>>> nlsUsedIdxMapEntrySetIte = nlsUsedIdxMap.entrySet().iterator();
		Map.Entry<Comment, ArrayList<Integer>> nlsUsedIdxMapEntry;
		MultiTextEdit multiTextEdit = new MultiTextEdit();
		while (nlsUsedIdxMapEntrySetIte.hasNext())
		{
			nlsUsedIdxMapEntry = nlsUsedIdxMapEntrySetIte.next();
			Comment nlsComment = nlsUsedIdxMapEntry.getKey();
			ISourceRange nlsCommentRange = nlsComment.getRange();
			ArrayList<Integer> usedIdx = nlsUsedIdxMapEntry.getValue();

			StringBuilder sb = new StringBuilder();
			for (Integer i : usedIdx)
				sb.append("//$NON-NLS-").append(i.intValue()).append("$ "); //$NON-NLS-1$ //$NON-NLS-2$
			String replaceNLS = sb.toString();
			if (!nlsComment.getText().equals(replaceNLS))
			{
				ReplaceEdit replaceEdit = new ReplaceEdit(nlsCommentRange.getOffset(), nlsCommentRange.getLength(), sb.toString());
				multiTextEdit.addChild(replaceEdit);
			}
		}

		if (multiTextEdit.getChildrenSize() > 0)
		{
			TextFileEditUtil.applyTextEdit(file, multiTextEdit);
		}
	}
}
