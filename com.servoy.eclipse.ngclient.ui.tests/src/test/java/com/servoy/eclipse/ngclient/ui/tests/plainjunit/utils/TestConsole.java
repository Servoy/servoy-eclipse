package com.servoy.eclipse.ngclient.ui.tests.plainjunit.utils;

import com.servoy.eclipse.ngclient.ui.StringOutputStream;

/**
 * Collects everything written to a {@link StringOutputStream}.
 */
public class TestConsole implements StringOutputStream {
	private final StringBuffer text = new StringBuffer();

	@Override
	public void write(CharSequence chars) {
		text.append(chars);
	}

	@Override
	public void close() {
	}

	public String text() {
		return text.toString();
	}
}
