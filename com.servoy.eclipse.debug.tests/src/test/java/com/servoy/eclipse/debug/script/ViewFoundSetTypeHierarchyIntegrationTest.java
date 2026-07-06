package com.servoy.eclipse.debug.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dltk.javascript.core.Types;
import org.eclipse.dltk.javascript.internal.core.TypeSystems;
import org.eclipse.dltk.javascript.typeinfo.IRTypeDeclaration;
import org.eclipse.dltk.javascript.typeinfo.TypeCompatibility;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.ViewFoundSet;

@DisplayName("SVY-21154: ViewFoundSet type hierarchy")
class ViewFoundSetTypeHierarchyIntegrationTest {
	private static TypeCreator typeCreator;
	private static Type viewFoundSetType;
	private static Type jsFoundSetType;

	@BeforeAll
	static void setUp() {
		typeCreator = TypeProviderFactory.getTypeProvider().getTypeCreator();
		assertNotNull(typeCreator, "TypeCreator should be available");

		viewFoundSetType = typeCreator.findType(null, ViewFoundSet.VIEW_FOUNDSET);
		jsFoundSetType = typeCreator.findType(null, FoundSet.JS_FOUNDSET);
	}

	@Nested
	@DisplayName("AC1: ViewFoundSet has JSFoundSet as super type")
	class SuperTypeRelationship {
		@Test
		@DisplayName("ViewFoundSet type is registered in the type system")
		void viewFoundSetTypeExists() {
			assertNotNull(viewFoundSetType, "ViewFoundSet type should be registered");
			assertEquals(ViewFoundSet.VIEW_FOUNDSET, viewFoundSetType.getName());
		}

		@Test
		@DisplayName("JSFoundSet type is registered in the type system")
		void jsFoundSetTypeExists() {
			assertNotNull(jsFoundSetType, "JSFoundSet type should be registered");
			assertEquals(FoundSet.JS_FOUNDSET, jsFoundSetType.getName());
		}

		@Test
		@DisplayName("ViewFoundSet super type is set to JSFoundSet")
		void viewFoundSetSuperTypeIsJSFoundSet() {
			Type superType = viewFoundSetType.getSuperType();
			assertNotNull(superType, "ViewFoundSet must have a super type");
			assertEquals(FoundSet.JS_FOUNDSET, superType.getName(),
					"ViewFoundSet's super type must be JSFoundSet to prevent assignment warnings");
		}
	}

	@Nested
	@DisplayName("AC1: isAssignableFrom behavioral verification")
	class AssignabilityBehavior {
		@Test
		@DisplayName("JSFoundSet.isAssignableFrom(ViewFoundSet) returns TRUE via super-type chain")
		void viewFoundSetIsAssignableToJSFoundSet() {
			IRTypeDeclaration jsFoundSetDecl = TypeSystems.GLOBAL.convert(jsFoundSetType);
			IRTypeDeclaration viewFoundSetDecl = TypeSystems.GLOBAL.convert(viewFoundSetType);

			assertEquals(TypeCompatibility.TRUE, jsFoundSetDecl.isAssignableFrom(viewFoundSetDecl),
					"ViewFoundSet must be assignable to JSFoundSet because ViewFoundSet's super type is JSFoundSet");
		}

		@Test
		@DisplayName("JSFoundSet.isAssignableFrom(JSFoundSet) returns TRUE (self-assignment)")
		void jsFoundSetIsAssignableToItself() {
			IRTypeDeclaration jsFoundSetDecl = TypeSystems.GLOBAL.convert(jsFoundSetType);

			assertEquals(TypeCompatibility.TRUE, jsFoundSetDecl.isAssignableFrom(jsFoundSetDecl),
					"JSFoundSet must be assignable to itself");
		}
	}

	@Nested
	@DisplayName("AC3: Incompatible types are still rejected")
	class IncompatibleTypeRejection {
		@Test
		@DisplayName("JSFoundSet.isAssignableFrom(String) returns FALSE")
		void stringIsNotAssignableToJSFoundSet() {
			IRTypeDeclaration jsFoundSetDecl = TypeSystems.GLOBAL.convert(jsFoundSetType);
			IRTypeDeclaration stringDecl = TypeSystems.GLOBAL.convert(Types.STRING);

			assertFalse(jsFoundSetDecl.isAssignableFrom(stringDecl).ok(),
					"String must NOT be assignable to JSFoundSet - the fix must not make the type system too permissive");
		}

		@Test
		@DisplayName("JSFoundSet.isAssignableFrom(Number) returns FALSE")
		void numberIsNotAssignableToJSFoundSet() {
			IRTypeDeclaration jsFoundSetDecl = TypeSystems.GLOBAL.convert(jsFoundSetType);
			IRTypeDeclaration numberDecl = TypeSystems.GLOBAL.convert(Types.NUMBER);

			assertFalse(jsFoundSetDecl.isAssignableFrom(numberDecl).ok(),
					"Number must NOT be assignable to JSFoundSet");
		}
	}

	@Nested
	@DisplayName("AC4: ViewFoundSet retains its own members for code completion")
	class CodeCompletionMembers {
		@Test
		@DisplayName("ViewFoundSet type has members defined")
		void viewFoundSetHasMembers() {
			assertNotNull(viewFoundSetType.getMembers(), "ViewFoundSet should have a members list");
			assertTrue(viewFoundSetType.getMembers().size() > 0,
					"ViewFoundSet should expose members for code completion");
		}

		@Test
		@DisplayName("ViewFoundSet type name is distinct from JSFoundSet")
		void viewFoundSetNameIsDistinct() {
			assertEquals(ViewFoundSet.VIEW_FOUNDSET, viewFoundSetType.getName());
			assertTrue(!viewFoundSetType.getName().equals(jsFoundSetType.getName()),
					"ViewFoundSet should remain a distinct type from JSFoundSet");
		}

		@Test
		@DisplayName("ViewFoundSet has at least one member not present on JSFoundSet")
		void viewFoundSetHasOwnMembers() {
			Set<String> jsFoundSetMemberNames = jsFoundSetType.getMembers().stream().map(Member::getName)
					.collect(Collectors.toSet());

			boolean hasOwnMember = viewFoundSetType.getMembers().stream()
					.anyMatch(member -> !jsFoundSetMemberNames.contains(member.getName()));

			assertTrue(hasOwnMember,
					"ViewFoundSet should have at least one member unique to itself (not inherited from JSFoundSet) to prove code completion won't lose ViewFoundSet-specific entries");
		}
	}

	@Nested
	@DisplayName("AC2: JSFoundSet self-assignment still works")
	class JSFoundSetSelfAssignment {
		@Test
		@DisplayName("JSFoundSet type has members")
		void jsFoundSetHasMembers() {
			assertNotNull(jsFoundSetType.getMembers());
			assertTrue(jsFoundSetType.getMembers().size() > 0, "JSFoundSet should have members");
		}

		@Test
		@DisplayName("JSFoundSet is assignable to itself via the type system")
		void jsFoundSetSelfAssignmentWorks() {
			IRTypeDeclaration jsFoundSetDecl = TypeSystems.GLOBAL.convert(jsFoundSetType);

			assertEquals(TypeCompatibility.TRUE, jsFoundSetDecl.isAssignableFrom(jsFoundSetDecl),
					"JSFoundSet must remain self-assignable after the ViewFoundSet super-type change");
		}
	}

	@Nested
	@DisplayName("Super type chain integrity")
	class SuperTypeChainIntegrity {
		@Test
		@DisplayName("JSFoundSet is reachable from ViewFoundSet via single super type hop")
		void singleHopFromViewFoundSetToJSFoundSet() {
			Type superType = viewFoundSetType.getSuperType();
			assertNotNull(superType);
			assertEquals(FoundSet.JS_FOUNDSET, superType.getName(),
					"ViewFoundSet -> JSFoundSet should be a direct super type relationship");
		}

		@Test
		@DisplayName("ViewFoundSet does not have itself as super type (no cycle)")
		void noCycleInSuperType() {
			Type superType = viewFoundSetType.getSuperType();
			assertTrue(!ViewFoundSet.VIEW_FOUNDSET.equals(superType.getName()),
					"ViewFoundSet must not be its own super type");
		}

		@Test
		@DisplayName("JSFoundSet members are inherited by ViewFoundSet via super type")
		void jsFoundSetMembersAccessibleViaSuperType() {
			Type superType = viewFoundSetType.getSuperType();
			assertNotNull(superType);
			assertTrue(superType.getMembers().size() > 0,
					"The super type (JSFoundSet) should have members that become accessible");

			// These members are core FoundSet API methods that must exist on JSFoundSet.
			// They are checked to confirm the super-type link exposes real members
			// (not an empty or incorrectly wired type).
			boolean hasFoundSetMember = false;
			for (Member member : superType.getMembers()) {
				if ("getSize".equals(member.getName()) || "getSelectedRecord".equals(member.getName())
						|| "getName".equals(member.getName()) || "getQuery".equals(member.getName())) {
					hasFoundSetMember = true;
					break;
				}
			}
			assertTrue(hasFoundSetMember,
					"JSFoundSet super type should contain typical foundset members like getSize/getSelectedRecord/getName/getQuery");
		}
	}
}
