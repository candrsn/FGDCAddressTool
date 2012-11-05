package com.spatialfocus.gui;

import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.SWT;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Combo;

public class FldMapBuilderdlg extends Dialog {

	protected Object result;
	protected Shell shell;
	private Table table;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public FldMapBuilderdlg(Shell parent, int style) {
		super(parent, style);
		setText("Field Map Builder");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.setSize(450, 300);
		shell.setText(getText());
		shell.setLayout(new FormLayout());
		
		CLabel lblTableName = new CLabel(shell, SWT.NONE);
		FormData fd_lblTableName = new FormData();
		fd_lblTableName.top = new FormAttachment(0, 10);
		fd_lblTableName.left = new FormAttachment(0, 10);
		lblTableName.setLayoutData(fd_lblTableName);
		lblTableName.setText("Table Name");
		
		table = new Table(shell, SWT.BORDER | SWT.FULL_SELECTION);
		FormData fd_table = new FormData();
		fd_table.bottom = new FormAttachment(lblTableName, 131, SWT.BOTTOM);
		fd_table.right = new FormAttachment(0, 351);
		fd_table.top = new FormAttachment(lblTableName, 24);
		fd_table.left = new FormAttachment(0, 25);
		table.setLayoutData(fd_table);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn tblclmnRawName = new TableColumn(table, SWT.NONE);
		tblclmnRawName.setWidth(100);
		tblclmnRawName.setText("Raw Name");
		
		TableColumn tblclmnStandardName = new TableColumn(table, SWT.NONE);
		tblclmnStandardName.setWidth(100);
		tblclmnStandardName.setText("Standard Name");
		
		TableItem tableItem = new TableItem(table, SWT.NONE);
		tableItem.setText("Field 1");
		
		TableItem tableItem_2 = new TableItem(table, SWT.NONE);
		tableItem_2.setText("Field 2");
		
		TableItem tableItem_1 = new TableItem(table, SWT.NONE);
		tableItem_1.setText("Field 3");

	}
}
