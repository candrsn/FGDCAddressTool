package com.spatialfocus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
//import swing2swt.layout.BoxLayout;
//import swing2swt.layout.FlowLayout;
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

public class FGDCAddressTool {

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			FGDCAddressTool window = new FGDCAddressTool();
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
		
		MenuItem mntmTools = new MenuItem(menu, SWT.CASCADE);
		mntmTools.setText("Tools");
		
		Menu menu_3 = new Menu(mntmTools);
		mntmTools.setMenu(menu_3);
		
		MenuItem mntmDefine = new MenuItem(menu_3, SWT.NONE);
		mntmDefine.setText("Define...");
		
		MenuItem mntmReports = new MenuItem(menu_3, SWT.NONE);
		mntmReports.setText("Reports");
		
		MenuItem mntmExport = new MenuItem(menu_3, SWT.NONE);
		mntmExport.setText("Export");
		
		MenuItem mntmImport = new MenuItem(menu_3, SWT.NONE);
		mntmImport.setText("Import");
		
		MenuItem mntmHelp = new MenuItem(menu, SWT.CASCADE);
		mntmHelp.setText("Help");
		
		Menu menu_4 = new Menu(mntmHelp);
		mntmHelp.setMenu(menu_4);
		
		MenuItem mntmWelcome = new MenuItem(menu_4, SWT.NONE);
		mntmWelcome.setText("Welcome");
		
		MenuItem mntmHelpContents = new MenuItem(menu_4, SWT.NONE);
		mntmHelpContents.setText("Help Contents");
		
		MenuItem mntmCheatSheets = new MenuItem(menu_4, SWT.NONE);
		mntmCheatSheets.setText("Cheat Sheets...");
		
		new MenuItem(menu_4, SWT.SEPARATOR);
		
		MenuItem mntmAbout = new MenuItem(menu_4, SWT.NONE);
		mntmAbout.setText("About");
		
		Browser browser = new Browser(shlFgdcAddressTool, SWT.NONE);
		FormData fd_browser = new FormData();
		fd_browser.top = new FormAttachment(0, 5);
		fd_browser.left = new FormAttachment(0, 10);
		fd_browser.bottom = new FormAttachment(100, -10);
		fd_browser.right = new FormAttachment(100, -10);
		browser.setLayoutData(fd_browser);
		while (!shlFgdcAddressTool.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
}
