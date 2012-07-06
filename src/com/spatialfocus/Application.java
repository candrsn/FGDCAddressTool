package com.spatialfocus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.browser.Browser;

public class Application {

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Application window = new Application();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		Shell shlFgdcAddressTool = new Shell();
		shlFgdcAddressTool.setText("FGDC Address Tool");
		shlFgdcAddressTool.setSize(499, 303);
		shlFgdcAddressTool.setLayout(new FormLayout());
		
		Composite composite = new Composite(shlFgdcAddressTool, SWT.NONE);
		FormData fd_composite = new FormData();
		fd_composite.top = new FormAttachment(0, 5);
		fd_composite.left = new FormAttachment(0, 219);
		composite.setLayoutData(fd_composite);
//		composite.setLayout(new BoxLayout(BoxLayout.X_AXIS));
		
		Menu menu_1 = new Menu(shlFgdcAddressTool);
		shlFgdcAddressTool.setMenu(menu_1);
		
		Menu menu = new Menu(shlFgdcAddressTool, SWT.BAR);
		shlFgdcAddressTool.setMenuBar(menu);
		
		MenuItem mntmFile = new MenuItem(menu, SWT.CASCADE);
		mntmFile.setText("File");
		
		Menu menu_2 = new Menu(mntmFile);
		mntmFile.setMenu(menu_2);
		
		MenuItem mntmNewProject = new MenuItem(menu_2, SWT.NONE);
		mntmNewProject.setText("New Project");
		
		MenuItem mntmOpenProject = new MenuItem(menu_2, SWT.NONE);
		mntmOpenProject.setText("Open Project");
		
		new MenuItem(menu_2, SWT.SEPARATOR);
		
		MenuItem mntmClose = new MenuItem(menu_2, SWT.NONE);
		mntmClose.setText("Close");
		
		MenuItem mntmSave = new MenuItem(menu_2, SWT.NONE);
		mntmSave.setText("Save");
		
		MenuItem mntmExit = new MenuItem(menu_2, SWT.NONE);
		mntmExit.setText("Exit");

	}
}
