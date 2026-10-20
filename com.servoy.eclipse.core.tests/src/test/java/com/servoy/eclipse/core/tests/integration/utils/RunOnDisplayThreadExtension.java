package com.servoy.eclipse.core.tests.integration.utils;

import java.lang.reflect.Method;

import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Runs the {@code @BeforeEach}, {@code @Test} and {@code @AfterEach} methods of the annotated test class (and of its
 * {@code @Nested} classes) on the SWT display thread.
 * <p>
 * PDE integration tests are not guaranteed to run on the UI thread (the workbench owns the display thread, tests run on
 * {@code WorkbenchTestable}), while SWT widgets can only be created and used on it. Tests must also never create their own
 * {@code Display}: on GTK (Linux/Jenkins) a second Display fails with {@code SWTError: Not implemented [multiple displays]}.
 * Code under test that uses {@code Display.getCurrent()} works because the test body runs on the display thread.
 */
public class RunOnDisplayThreadExtension implements InvocationInterceptor
{
	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
		ExtensionContext extensionContext) throws Throwable
	{
		runOnDisplayThread(invocation);
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
		ExtensionContext extensionContext) throws Throwable
	{
		runOnDisplayThread(invocation);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
		ExtensionContext extensionContext) throws Throwable
	{
		runOnDisplayThread(invocation);
	}

	private static void runOnDisplayThread(Invocation<Void> invocation) throws Throwable
	{
		Display display = Display.getDefault();
		if (display.getThread() == Thread.currentThread())
		{
			invocation.proceed();
			return;
		}
		Throwable[] failure = new Throwable[1];
		display.syncExec(() -> {
			try
			{
				invocation.proceed();
			}
			catch (Throwable t)
			{
				failure[0] = t;
			}
		});
		if (failure[0] != null) throw failure[0];
	}
}
