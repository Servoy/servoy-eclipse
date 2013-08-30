package com.servoy.eclipse.ui.property;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.property.MethodPropertyController.MethodPropertySource;

/**
 * 
 * the text is readonly 
 * @author obuligan
 */
public class TextAndButtonCellEditor extends CellEditor
{

	/**
	 * The text value.
	 */
	Text text;
	Button button;
	/**
	 * Default CheckboxCellEditor style
	 */
	private static final int defaultStyle = SWT.NONE;

	/**
	 * Creates a new checkbox cell editor with no control
	 */
	public TextAndButtonCellEditor()
	{
		setStyle(defaultStyle);
	}

	public TextAndButtonCellEditor(Composite parent)
	{
		this(parent, defaultStyle);
	}


	public TextAndButtonCellEditor(Composite parent, int style)
	{
		super(parent, style);
	}

	@Override
	protected Control createControl(final Composite parent)
	{
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FormLayout());


		button = new Button(composite, SWT.NONE);
		button.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
		FormData fd_button = new FormData();
		fd_button.top = new FormAttachment(text, 0, SWT.TOP);
		fd_button.right = new FormAttachment(100);
		fd_button.bottom = new FormAttachment(100);
		button.setLayoutData(fd_button);
		button.setText("");

		text = new Text(composite, SWT.READ_ONLY);
		FormData fd_text = new FormData();
		fd_text.top = new FormAttachment(0);
		fd_text.left = new FormAttachment(0);
		text.setLayoutData(fd_text);

		setValueValid(true);
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseUp(MouseEvent e)
			{
				clicked();
			}
		});

		composite.pack();
		return composite;

	}

	public void setButtonText(String text)
	{
		button.setText(text);
	}

	public String getButtonText()
	{
		return button.getText();
	}

	@Override
	public void activate()
	{
	}

	protected void clicked()
	{
		//value = !value;
		doSetValue(MethodPropertySource.DELETED_ARGUMENT);
		markDirty();
		fireApplyEditorValue();
	}

	@Override
	protected Object doGetValue()
	{
		return text.getText();
	}

	/*
	 * (non-Javadoc) Method declared on CellEditor.
	 */
	@Override
	protected void doSetFocus()
	{
		// Ignore
	}

	@Override
	protected void doSetValue(Object val)
	{
		Assert.isTrue(text != null);
		text.setText((String)val);
	}
}