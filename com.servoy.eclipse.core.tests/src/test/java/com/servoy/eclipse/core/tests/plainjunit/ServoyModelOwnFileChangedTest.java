/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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
package com.servoy.eclipse.core.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModel.OwnFileChange;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.util.UUID;

/**
 * SVY-21576: unit tests for the own-file classification helper
 * {@link ServoyModel#classifyOwnCompositeFileChange(com.servoy.j2db.persistence.IPersist, File, Set)}.
 *
 * <p>
 * The helper classifies, for a composite-with-items persist flagged changed,
 * <em>which</em> of its serialized files changed on disk, which in turn decides
 * how {@code ServoyModel.handleChangedFiles} refreshes the editing (in-memory)
 * copy:
 * </p>
 * <ul>
 * <li>{@link OwnFileChange#OWN_FILE} - the composite's own {@code .frm} changed:
 * the whole editing persist is refreshed from disk.</li>
 * <li>{@link OwnFileChange#FORM_CSS_ONLY} - only the form's {@code .less}
 * stylesheet changed: ONLY the editing form's {@code formCss} is refreshed, so
 * the designer preview repaints <em>without</em> overwriting unsaved editor
 * state on the form (SVY-21576, keeping the SVY-21249/SVY-21697 no-data-loss
 * guarantee). This is the key regression guard: a {@code .less}-only change must
 * classify as {@code FORM_CSS_ONLY}, never {@code OWN_FILE}.</li>
 * <li>{@link OwnFileChange#NONE} - only a child script {@code .js} changed: the
 * editing composite must not be overwritten at all (SVY-21249).</li>
 * </ul>
 *
 * <p>
 * These are pure path-logic tests: the helper is fed fabricated
 * {@code changedFilePaths} sets and a reflectively-built
 * {@code Solution}/{@code Form} - no OSGi runtime or workspace is needed. The
 * paths are composed with exactly the same base-dir convention
 * ({@code solutionParentDir} + {@link SolutionSerializer#getRelativeFilePath})
 * that {@code ServoyModel.handleChangedFiles} uses, so a match here mirrors a
 * real on-disk change.
 * </p>
 *
 * @author test-gen
 */
class ServoyModelOwnFileChangedTest
{
	/**
	 * The parent folder of the solution project, used as the resolution base for
	 * relative serialized paths.
	 */
	private static final File SOLUTION_PARENT_DIR = new File(System.getProperty("java.io.tmpdir"), "svy21576-ws");

	private static Solution newSolution(String name) throws Exception
	{
		SolutionMetaData metaData = new SolutionMetaData(UUID.randomUUID(), name, 0, 0, 0);
		// a current (>= VERSION_38) file version so Form/Relation count as
		// composites-with-items
		metaData.setFileVersion(SolutionSerializer.VERSION_38);
		Constructor<Solution> ctor = Solution.class.getDeclaredConstructor(IRepository.class, SolutionMetaData.class);
		ctor.setAccessible(true);
		return ctor.newInstance(null, metaData);
	}

	private static Form newForm(Solution solution, String name) throws Exception
	{
		Constructor<Form> ctor = Form.class.getDeclaredConstructor(ISupportChilds.class, UUID.class);
		ctor.setAccessible(true);
		Form form = ctor.newInstance(solution, UUID.randomUUID());
		form.setName(name);
		solution.internalAddChild(form);
		return form;
	}

	private static Relation newRelation(Solution solution, String name) throws Exception
	{
		Constructor<Relation> ctor = Relation.class.getDeclaredConstructor(ISupportChilds.class, UUID.class);
		ctor.setAccessible(true);
		Relation relation = ctor.newInstance(solution, UUID.randomUUID());
		relation.setName(name);
		solution.internalAddChild(relation);
		return relation;
	}

	/**
	 * The {@code .frm} path the helper builds for this persist, matching the
	 * production composition.
	 */
	private static String frmPath(com.servoy.j2db.persistence.IPersist persist)
	{
		return new File(SOLUTION_PARENT_DIR, SolutionSerializer.getRelativeFilePath(persist, false)).getAbsolutePath();
	}

	/**
	 * The form {@code .less} sibling path the helper builds, matching the
	 * production composition.
	 */
	private static String lessPath(Form form)
	{
		return new File(SOLUTION_PARENT_DIR, SolutionSerializer.getRelativePath(form, false) + form.getName() + SolutionSerializer.FORM_LESS_FILE_EXTENSION)
			.getAbsolutePath();
	}

	/**
	 * A fabricated child script {@code .js} path that is neither the form
	 * {@code .frm} nor its {@code .less}.
	 */
	private static String childJsPath(Form form)
	{
		return new File(SOLUTION_PARENT_DIR, SolutionSerializer.getRelativePath(form, false) + form.getName() + ".js")
			.getAbsolutePath();
	}

	@Nested
	class FormLessChange
	{
		/**
		 * SVY-21576 key regression guard: a form whose own {@code .less} stylesheet
		 * changed (and whose {@code .frm} did NOT) must classify as
		 * {@link OwnFileChange#FORM_CSS_ONLY} - NOT {@link OwnFileChange#OWN_FILE}. The
		 * {@code FORM_CSS_ONLY} branch refreshes only the editing form's {@code formCss}
		 * (so the preview repaints) without overwriting the rest of the editing form
		 * from disk, which would discard unsaved editor changes (component
		 * moves/deletes/adds). If this ever returned {@code OWN_FILE} the full
		 * {@code updateEditingPersist} would run and reintroduce the data-loss bug the
		 * review flagged.
		 */
		@Test
		@DisplayName("form .less changed (no .frm) classifies as FORM_CSS_ONLY, not OWN_FILE (no data loss)")
		void formLessChangeIsFormCssOnly() throws Exception
		{
			Solution solution = newSolution("mysolution");
			Form form = newForm(solution, "myform");

			Set<String> changedFilePaths = new HashSet<>();
			changedFilePaths.add(lessPath(form));

			assertAll(
				() -> assertEquals(OwnFileChange.FORM_CSS_ONLY,
					ServoyModel.classifyOwnCompositeFileChange(form, SOLUTION_PARENT_DIR, changedFilePaths),
					"a changed form .less must classify as FORM_CSS_ONLY so only formCss is refreshed (SVY-21576)"),
				() -> assertFalse(changedFilePaths.contains(frmPath(form)),
					"guard: the .frm must NOT be in the changed set for this scenario"));
		}

		/**
		 * The {@code .less} path the helper matches must be the real on-disk one - i.e.
		 * {@code <parent>/<solution>/forms/<form>.less}. Pinning the exact string
		 * proves the helper and the test build the comparison path identically (and
		 * that a mismatch would make the regression guard pass vacuously).
		 */
		@Test
		@DisplayName("the matched .less path is <parent>/<solution>/forms/<form>.less")
		void lessPathHasExpectedShape() throws Exception
		{
			Solution solution = newSolution("mysolution");
			Form form = newForm(solution, "myform");

			String expected = new File(SOLUTION_PARENT_DIR, "mysolution/forms/myform.less").getAbsolutePath();
			assertEquals(OwnFileChange.FORM_CSS_ONLY,
				ServoyModel.classifyOwnCompositeFileChange(form, SOLUTION_PARENT_DIR, Set.of(expected)),
				"the helper must match the expected forms/<form>.less path: " + expected);
		}

		/**
		 * When BOTH the {@code .frm} and the {@code .less} changed (e.g. the whole form
		 * was saved), the {@code .frm} wins: the classification is
		 * {@link OwnFileChange#OWN_FILE} so the full editing persist is refreshed. The
		 * form itself was just written to disk, so there is no newer unsaved editor
		 * state to protect.
		 */
		@Test
		@DisplayName("both .frm and .less changed classifies as OWN_FILE (full refresh)")
		void frmAndLessChangeIsOwnFile() throws Exception
		{
			Solution solution = newSolution("mysolution");
			Form form = newForm(solution, "myform");

			Set<String> changedFilePaths = new HashSet<>();
			changedFilePaths.add(frmPath(form));
			changedFilePaths.add(lessPath(form));

			assertEquals(OwnFileChange.OWN_FILE,
				ServoyModel.classifyOwnCompositeFileChange(form, SOLUTION_PARENT_DIR, changedFilePaths),
				"when the .frm itself changed, the full refresh branch applies even if the .less also changed");
		}
	}

	@Nested
	class FormFrmChange
	{
		/**
		 * A plain form {@code .frm} change is - and always was - an own-file change:
		 * the full refresh branch applies. This pins that the helper preserves the
		 * original {@code .frm} behaviour.
		 */
		@Test
		@DisplayName("form .frm changed classifies as OWN_FILE")
		void formFrmChangeIsOwnFile() throws Exception
		{
			Solution solution = newSolution("mysolution");
			Form form = newForm(solution, "myform");

			Set<String> changedFilePaths = new HashSet<>();
			changedFilePaths.add(frmPath(form));

			assertEquals(OwnFileChange.OWN_FILE,
				ServoyModel.classifyOwnCompositeFileChange(form, SOLUTION_PARENT_DIR, changedFilePaths),
				"a changed form .frm must classify as OWN_FILE");
		}
	}

	@Nested
	class ChildOnlyChange
	{
		/**
		 * SVY-21249 skip branch preserved: when ONLY a child script {@code .js} changed
		 * (neither the {@code .frm} nor the {@code .less} is in the changed set) the
		 * helper must classify as {@link OwnFileChange#NONE}, so the visitor takes the
		 * skip branch and does not overwrite the editing composite (which would discard
		 * a just-assigned, unsaved event handler).
		 */
		@Test
		@DisplayName("only a child .js changed (no .frm, no .less) classifies as NONE (SVY-21249 skip preserved)")
		void childJsOnlyChangeIsNone() throws Exception
		{
			Solution solution = newSolution("mysolution");
			Form form = newForm(solution, "myform");

			Set<String> changedFilePaths = new HashSet<>();
			changedFilePaths.add(childJsPath(form));

			assertAll(
				() -> assertEquals(OwnFileChange.NONE,
					ServoyModel.classifyOwnCompositeFileChange(form, SOLUTION_PARENT_DIR, changedFilePaths),
					"a child-.js-only change must classify as NONE (SVY-21249)"),
				() -> assertFalse(changedFilePaths.contains(frmPath(form)),
					"guard: the .frm must NOT be in the changed set"),
				() -> assertFalse(changedFilePaths.contains(lessPath(form)),
					"guard: the .less must NOT be in the changed set"));
		}

		/**
		 * An empty changed set can never be an own-file change.
		 */
		@Test
		@DisplayName("empty changedFilePaths classifies as NONE")
		void emptyChangedSetIsNone() throws Exception
		{
			Solution solution = newSolution("mysolution");
			Form form = newForm(solution, "myform");

			assertEquals(OwnFileChange.NONE,
				ServoyModel.classifyOwnCompositeFileChange(form, SOLUTION_PARENT_DIR, new HashSet<>()),
				"an empty changed set must classify as NONE");
		}
	}

	@Nested
	class NonFormComposite
	{
		/**
		 * The {@code .less} handling is form-only. A non-{@code Form}
		 * composite-with-items (here a {@link Relation}) whose own {@code .obj} did not
		 * change must still classify as {@link OwnFileChange#NONE} when only an
		 * unrelated child file changed - the helper must not invent a {@code .less}
		 * sibling for it.
		 */
		@Test
		@DisplayName("non-Form composite with only a child file changed classifies as NONE (no .less handling)")
		void nonFormCompositeChildChangeIsNone() throws Exception
		{
			Solution solution = newSolution("mysolution");
			Relation relation = newRelation(solution, "myrelation");

			// a sibling .less built with the Relation's name must NOT be treated as its own
			// file
			String relationLessLike = new File(SOLUTION_PARENT_DIR,
				SolutionSerializer.getRelativePath(relation, false) + relation.getName() + SolutionSerializer.FORM_LESS_FILE_EXTENSION).getAbsolutePath();
			Set<String> changedFilePaths = new HashSet<>();
			changedFilePaths.add(relationLessLike);

			assertAll(
				() -> assertEquals(OwnFileChange.NONE,
					ServoyModel.classifyOwnCompositeFileChange(relation, SOLUTION_PARENT_DIR, changedFilePaths),
					"a non-Form composite must not get the .less (FORM_CSS_ONLY) handling"),
				() -> assertFalse(changedFilePaths.contains(frmPath(relation)),
					"guard: the relation's own file must NOT be in the changed set"));
		}

		/**
		 * The same non-{@code Form} composite DOES take the own-file branch when its
		 * real own file changed - proving the {@code NONE} above is the no-{@code .less}
		 * path, not a blanket non-form rejection.
		 */
		@Test
		@DisplayName("non-Form composite whose own file changed classifies as OWN_FILE")
		void nonFormCompositeOwnFileChangeIsOwnFile() throws Exception
		{
			Solution solution = newSolution("mysolution");
			Relation relation = newRelation(solution, "myrelation");

			assertEquals(OwnFileChange.OWN_FILE,
				ServoyModel.classifyOwnCompositeFileChange(relation, SOLUTION_PARENT_DIR, Set.of(frmPath(relation))),
				"a non-Form composite whose own serialized file changed must classify as OWN_FILE");
		}
	}
}
