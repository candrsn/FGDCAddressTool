package com.spatialfocus;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Device;
import com.spatialfocus.*;

public class AddressToolapp {

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			AddressToolapp window = new AddressToolapp();
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
		final Shell shell = new Shell();
		shell.setSize(450, 300);
		shell.setText("FGDC Address Tool");
		shell.setLayout(null);

		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);

		MenuItem mntmFile = new MenuItem(menu, SWT.CASCADE);
		mntmFile.setText("File");

		Menu menu_2 = new Menu(mntmFile);
		mntmFile.setMenu(menu_2);

		MenuItem mntmNewProject = new MenuItem(menu_2, SWT.NONE);
		mntmNewProject.setText("New Project");

		MenuItem mntmOpenProject = new MenuItem(menu_2, SWT.NONE);
		mntmOpenProject.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
		        FileDialog fd = new FileDialog(shell, SWT.OPEN);
		        fd.setText("Open");
		        fd.setFilterPath("C:/");
		        String[] filterExt = { "*.txt", "*.doc", ".rtf", "*.*" };
		        fd.setFilterExtensions(filterExt);
		        String selected = fd.open();
		        System.out.println(selected);
			}
		});
		mntmOpenProject.setText("Open Project");

		new MenuItem(menu_2, SWT.SEPARATOR);

		MenuItem mntmRefresh = new MenuItem(menu_2, SWT.NONE);
		mntmRefresh.setText("Refresh");

		new MenuItem(menu_2, SWT.SEPARATOR);

		MenuItem mntmClose = new MenuItem(menu_2, SWT.NONE);
		mntmClose.setText("Close");

		MenuItem mntmSave = new MenuItem(menu_2, SWT.NONE);
		mntmSave.setText("Save");

		MenuItem mntmExit = new MenuItem(menu_2, SWT.NONE);
		mntmExit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
		        MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION
		                | SWT.YES | SWT.NO);
		            messageBox.setMessage("Do you really want to exit?");
		            messageBox.setText("Exiting Application");
		            int response = messageBox.open();
		            if (response == SWT.YES)
		              System.exit(0);
				
			}
		});
		mntmExit.setText("Exit");

		MenuItem mntmTools = new MenuItem(menu, SWT.CASCADE);
		mntmTools.setText("Tools");

		Menu menu_1 = new Menu(mntmTools);
		mntmTools.setMenu(menu_1);

		MenuItem mntmDefine = new MenuItem(menu_1, SWT.NONE);
		mntmDefine.setText("Define ...");

		MenuItem mntmReports = new MenuItem(menu_1, SWT.NONE);
		mntmReports.setText("Reports");

		MenuItem mntmPublish = new MenuItem(menu_1, SWT.NONE);
		mntmPublish.setText("Publish");

		MenuItem mntmExport = new MenuItem(menu_1, SWT.NONE);
		mntmExport.setText("Export");

		MenuItem mntmImport = new MenuItem(menu_1, SWT.NONE);
		mntmImport.setText("Import");

		MenuItem mntmHelp = new MenuItem(menu, SWT.CASCADE);
		mntmHelp.setText("Help");

		Menu menu_3 = new Menu(mntmHelp);
		mntmHelp.setMenu(menu_3);

		MenuItem mntmWelcome = new MenuItem(menu_3, SWT.NONE);
		mntmWelcome.setText("Welcome");

		MenuItem mntmHelpContents = new MenuItem(menu_3, SWT.NONE);
		mntmHelpContents.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				BareBonesBrowserLaunch.openURL("http://meadow.spatialfocus.com/address_standard");
			}
		});
		mntmHelpContents.setText("Help Contents");

		MenuItem mntmCheatSheets = new MenuItem(menu_3, SWT.NONE);
		mntmCheatSheets.setText("Cheat Sheets ...");

		new MenuItem(menu_3, SWT.SEPARATOR);

		MenuItem mntmAbout = new MenuItem(menu_3, SWT.NONE);
		mntmAbout.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				Aboutdlg About = new Aboutdlg(e.display.getActiveShell(),
						SWT.APPLICATION_MODAL);
				About.open();

			}
		});
		mntmAbout.setSelection(true);
		mntmAbout.setText("About");

		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
}
