package com.servoy.eclipse.model.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.persistence.ICommonWebComponent;

class WebFormComponentChildTypeTest {
	@Nested
	class ImplementsICommonWebComponent {
		@Test
		@DisplayName("WebFormComponentChildType is assignable to ICommonWebComponent")
		void isAssignableToICommonWebComponent() {
			assertTrue(ICommonWebComponent.class.isAssignableFrom(WebFormComponentChildType.class));
		}

		@Test
		@DisplayName("casting WebFormComponentChildType to ICommonWebComponent does not throw ClassCastException")
		void castDoesNotThrowClassCastException() {
			WebFormComponentChildType mock = Mockito.mock(WebFormComponentChildType.class);
			assertDoesNotThrow(() -> {
				ICommonWebComponent casted = (ICommonWebComponent) mock;
				assertNotNull(casted);
			});
		}

		@Test
		@DisplayName("mock instance passes instanceof check for ICommonWebComponent")
		void instanceOfCheckPasses() {
			WebFormComponentChildType mock = Mockito.mock(WebFormComponentChildType.class);
			assertInstanceOf(ICommonWebComponent.class, mock);
		}
	}

	@Nested
	class GetPropertyDescriptionContract {
		@Test
		@DisplayName("getPropertyDescription method exists and matches ICommonWebComponent contract")
		void methodMatchesInterfaceContract() throws NoSuchMethodException {
			Method interfaceMethod = ICommonWebComponent.class.getMethod("getPropertyDescription");
			Method classMethod = WebFormComponentChildType.class.getMethod("getPropertyDescription");

			assertAll(
					() -> assertNotNull(classMethod, "getPropertyDescription must exist on WebFormComponentChildType"),
					() -> assertTrue(interfaceMethod.getReturnType().isAssignableFrom(classMethod.getReturnType()),
							"return type must be compatible with ICommonWebComponent.getPropertyDescription()"));
		}

		@Test
		@DisplayName("mocked getPropertyDescription returns value accessible via ICommonWebComponent reference")
		void getPropertyDescriptionAccessibleViaInterface() {
			WebFormComponentChildType mock = Mockito.mock(WebFormComponentChildType.class);
			PropertyDescription pd = Mockito.mock(PropertyDescription.class);
			Mockito.when(mock.getPropertyDescription()).thenReturn(pd);

			ICommonWebComponent asInterface = mock;
			assertNotNull(asInterface.getPropertyDescription());
		}
	}
}
