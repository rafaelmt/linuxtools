/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Jeff Briggs, Henry Hughes, Ryan Morse
 *******************************************************************************/

package org.eclipse.linuxtools.systemtap.ui.editor.actions.file;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.linuxtools.systemtap.ui.editor.PathEditorInput;
import org.eclipse.linuxtools.systemtap.ui.editor.SimpleEditor;
import org.eclipse.linuxtools.systemtap.ui.editor.actions.EditorAction;
import org.eclipse.linuxtools.systemtap.ui.editor.internal.Localization;
import org.eclipse.linuxtools.systemtap.ui.editor.internal.RecentFileLog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;



public class OpenFileAction extends EditorAction {
	
	private boolean successful;
	
	public OpenFileAction() {
		super();
		setEnabled(true);
		successful = false;
	}
	
	public void run(IAction act) {
		run();
	}
	
	/**
	 * Opens the editor input.
	 */
	public void run() {
		successful = false;
		if (window == null)
					window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		File file = queryFile();
		if (file != null) {
			IEditorInput input= createEditorInput(file);
			String editorId= getEditorId(file);
			IWorkbenchPage page= window.getActivePage();
			try {
				page.openEditor(input, editorId);
				RecentFileLog.updateRecentFiles(file);
				successful = true;
			} catch (PartInitException e) {}
		} else if (file != null) {
			String msg = MessageFormat.format(Localization.getString("OpenFileAction.FileIsNull"), (Object [])(new String[] {file.getName()}));
			MessageDialog.openWarning(window.getShell(), Localization.getString("OpenFileAction.Problem"), msg);
		}
	}
	
	/**
	 * Request the name and location of the file to the user.
	 * @return the File object associated to the selected file.
	 */
	protected File queryFile() {
		FileDialog dialog= new FileDialog(window.getShell(), SWT.OPEN);
		dialog.setText(Localization.getString("OpenFileAction.OpenFile"));
		String path= dialog.open();
		if (path != null && path.length() > 0)
			return new File(path);
		return null;
	}

	/**
	 * Returns the editor ID associated with the specified file.
	 * @param file the file to examine
	 * @return the editor ID
	 */
	protected String getEditorId(File file) {
		IWorkbench workbench= window.getWorkbench();
		IEditorRegistry editorRegistry= workbench.getEditorRegistry();
		IEditorDescriptor[] descriptors= editorRegistry.getEditors(file.getName());
		for (IEditorDescriptor d : descriptors)
			if (d.getId().startsWith("org.eclipse.linuxtools.systemtap.ui.ide.editors")) {
				return d.getId();
			}
		return SimpleEditor.ID;
	}

	/**
	 * Creates an editor input.
	 * @param file the file you wish the editor to point at
	 * @return the input created
	 */
	protected IEditorInput createEditorInput(File file) {
		IPath location= new Path(file.getAbsolutePath());
		PathEditorInput input= new PathEditorInput(location);
		return input;
	}
	
	public boolean isSuccessful() {
		return successful;
	}
}
