package com.servoy.eclipse.cypress.actions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CypressTestPropertyTester")
public class CypressTestPropertyTesterTest {
	private CypressTestPropertyTester tester;

	@BeforeEach
	void setUp() {
		tester = new CypressTestPropertyTester();
	}

	@Nested
	@DisplayName("with single form test target")
	class SingleFormTarget {
		private final CypressFormTestTarget singleFormTarget = new CypressFormTestTarget() {
			@Override
			public String getFormName() {
				return "myForm";
			}

			@Override
			public boolean isSolutionLevel() {
				return false;
			}

			@Override
			public List<String> getTestFormNames() {
				return Collections.singletonList("myForm");
			}
		};

		@Test
		@DisplayName("isSingleFormTest returns true")
		void isSingleFormTestReturnsTrue() {
			assertTrue(tester.test(singleFormTarget, "isSingleFormTest", new Object[0], null));
		}

		@Test
		@DisplayName("isSolutionLevelTest returns false")
		void isSolutionLevelTestReturnsFalse() {
			assertFalse(tester.test(singleFormTarget, "isSolutionLevelTest", new Object[0], null));
		}
	}

	@Nested
	@DisplayName("with solution level test target")
	class SolutionLevelTarget {
		private final CypressFormTestTarget solutionTarget = new CypressFormTestTarget() {
			@Override
			public String getFormName() {
				return null;
			}

			@Override
			public boolean isSolutionLevel() {
				return true;
			}

			@Override
			public List<String> getTestFormNames() {
				return List.of("formA", "formB");
			}
		};

		@Test
		@DisplayName("isSingleFormTest returns false")
		void isSingleFormTestReturnsFalse() {
			assertFalse(tester.test(solutionTarget, "isSingleFormTest", new Object[0], null));
		}

		@Test
		@DisplayName("isSolutionLevelTest returns true")
		void isSolutionLevelTestReturnsTrue() {
			assertTrue(tester.test(solutionTarget, "isSolutionLevelTest", new Object[0], null));
		}
	}

	@Nested
	@DisplayName("invalid inputs")
	class InvalidInputs {
		@Test
		@DisplayName("returns false for null receiver")
		void returnsFalseForNullReceiver() {
			assertFalse(tester.test(null, "isSingleFormTest", new Object[0], null));
		}

		@Test
		@DisplayName("returns false for non-CypressFormTestTarget receiver")
		void returnsFalseForWrongType() {
			assertAll(() -> assertFalse(tester.test("a string", "isSingleFormTest", new Object[0], null)),
					() -> assertFalse(tester.test(Integer.valueOf(42), "isSolutionLevelTest", new Object[0], null)),
					() -> assertFalse(tester.test(new Object(), "isSingleFormTest", new Object[0], null)));
		}

		@ParameterizedTest
		@ValueSource(strings = { "unknownProperty", "isFormTest", "", "isSingleForm" })
		@DisplayName("returns false for unknown property names")
		void returnsFalseForUnknownProperty(String property) {
			CypressFormTestTarget target = new CypressFormTestTarget() {
				@Override
				public String getFormName() {
					return "test";
				}

				@Override
				public boolean isSolutionLevel() {
					return false;
				}

				@Override
				public List<String> getTestFormNames() {
					return Collections.singletonList("test");
				}
			};
			assertFalse(tester.test(target, property, new Object[0], null));
		}

		@ParameterizedTest
		@NullSource
		@DisplayName("returns false for null property")
		void returnsFalseForNullProperty(String property) {
			CypressFormTestTarget target = new CypressFormTestTarget() {
				@Override
				public String getFormName() {
					return "test";
				}

				@Override
				public boolean isSolutionLevel() {
					return false;
				}

				@Override
				public List<String> getTestFormNames() {
					return Collections.singletonList("test");
				}
			};
			assertFalse(tester.test(target, property, new Object[0], null));
		}
	}
}

