/*******************************************************************************
 * Copyright (c) 2010 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.ui.dialogs;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.linuxtools.tmf.ui.TmfUiPlugin;
import org.eclipse.linuxtools.tmf.ui.TmfUiPreferenceInitializer;
import org.eclipse.linuxtools.tmf.ui.editors.TmfEventsEditor;
import org.eclipse.linuxtools.tmf.ui.internal.Messages;
import org.eclipse.linuxtools.tmf.ui.parsers.ParserProviderManager;
import org.eclipse.linuxtools.tmf.ui.parsers.custom.CustomTraceDefinition;
import org.eclipse.linuxtools.tmf.ui.parsers.custom.CustomTxtTrace;
import org.eclipse.linuxtools.tmf.ui.parsers.custom.CustomTxtTraceDefinition;
import org.eclipse.linuxtools.tmf.ui.parsers.custom.CustomXmlTrace;
import org.eclipse.linuxtools.tmf.ui.parsers.custom.CustomXmlTraceDefinition;
import org.eclipse.linuxtools.tmf.ui.parsers.wizards.CustomTxtParserWizard;
import org.eclipse.linuxtools.tmf.ui.parsers.wizards.CustomXmlParserWizard;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

public class ManageCustomParsersDialog extends Dialog {

    private static final Image image = TmfUiPlugin.getDefault().getImageFromPath("/icons/etool16/customparser_wizard.gif"); //$NON-NLS-1$

    Button txtButton;
    Button xmlButton;
    List parserList;
    Button newButton;
    Button editButton;
    Button deleteButton;
    Button importButton;
    Button exportButton;
    Button parseButton;
    
    public ManageCustomParsersDialog(Shell parent) {
        super(parent);
        setShellStyle(SWT.RESIZE | SWT.MAX | getShellStyle());
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        getShell().setText(Messages.ManageCustomParsersDialog_DialogHeader);
        getShell().setImage(image);
        
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout(2, false));

        Composite listContainer = new Composite(composite, SWT.NONE);
        listContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout lcgl = new GridLayout();
        lcgl.marginHeight = 0;
        lcgl.marginWidth = 0;
        listContainer.setLayout(lcgl);
        
        Composite radioContainer = new Composite(listContainer, SWT.NONE);
        GridLayout rcgl = new GridLayout(2, true);
        rcgl.marginHeight = 0;
        rcgl.marginWidth = 0;
        radioContainer.setLayout(rcgl);
        
        txtButton = new Button(radioContainer, SWT.RADIO);
        txtButton.setText(Messages.ManageCustomParsersDialog_TextButtonLabel);
        txtButton.setSelection(true);
        txtButton.addSelectionListener(new SelectionListener(){
            @Override
			public void widgetDefaultSelected(SelectionEvent e) {}
            @Override
			public void widgetSelected(SelectionEvent e) {
                fillParserList();
            }});
        
        xmlButton = new Button(radioContainer, SWT.RADIO);
        xmlButton.setText("XML"); //$NON-NLS-1$
        xmlButton.addSelectionListener(new SelectionListener(){
            @Override
			public void widgetDefaultSelected(SelectionEvent e) {}
            @Override
			public void widgetSelected(SelectionEvent e) {
                fillParserList();
            }});
        
        parserList = new List(listContainer, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        parserList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        parserList.addSelectionListener(new SelectionListener(){
            @Override
			public void widgetDefaultSelected(SelectionEvent e) {}
            @Override
			public void widgetSelected(SelectionEvent e) {
                if (parserList.getSelectionCount() == 0) {
                    editButton.setEnabled(false);
                    deleteButton.setEnabled(false);
                    exportButton.setEnabled(false);
                    parseButton.setEnabled(false);
                } else {
                    editButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    exportButton.setEnabled(true);
                    parseButton.setEnabled(true);
                }
            }});
        
        Composite buttonContainer = new Composite(composite, SWT.NULL);
        buttonContainer.setLayout(new GridLayout());
        buttonContainer.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));

        newButton = new Button(buttonContainer, SWT.PUSH);
        newButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        newButton.setText(Messages.ManageCustomParsersDialog_NewButtonLabel);
        newButton.addSelectionListener(new SelectionListener(){
            @Override
			public void widgetDefaultSelected(SelectionEvent e) {}
            @Override
			public void widgetSelected(SelectionEvent e) {
                WizardDialog dialog = null;
                if (txtButton.getSelection()) {
                    dialog = new WizardDialog(getShell(), new CustomTxtParserWizard());
                } else if (xmlButton.getSelection()) {
                    dialog = new WizardDialog(getShell(), new CustomXmlParserWizard());
                }
                dialog.open();
                if (dialog.getReturnCode() == Dialog.OK) {
                    fillParserList();
                }
            }});
        
        editButton = new Button(buttonContainer, SWT.PUSH);
        editButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        editButton.setText(Messages.ManageCustomParsersDialog_EditButtonLabel);
        editButton.setEnabled(false);
        editButton.addSelectionListener(new SelectionListener(){
            @Override
			public void widgetDefaultSelected(SelectionEvent e) {}
            @Override
			public void widgetSelected(SelectionEvent e) {
                WizardDialog dialog = null;
                if (txtButton.getSelection()) {
                    dialog = new WizardDialog(getShell(),
                            new CustomTxtParserWizard(CustomTxtTraceDefinition.load(parserList.getSelection()[0])));
                } else if (xmlButton.getSelection()) {
                    dialog = new WizardDialog(getShell(),
                            new CustomXmlParserWizard(CustomXmlTraceDefinition.load(parserList.getSelection()[0])));
                }
                dialog.open();
                if (dialog.getReturnCode() == Dialog.OK) {
                    fillParserList();
                }
            }});

        deleteButton = new Button(buttonContainer, SWT.PUSH);
        deleteButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        deleteButton.setText(Messages.ManageCustomParsersDialog_DeleteButtonLabel);
        deleteButton.setEnabled(false);
        deleteButton.addSelectionListener(new SelectionListener(){
            @Override
			public void widgetDefaultSelected(SelectionEvent e) {}
            @Override
			public void widgetSelected(SelectionEvent e) {
                boolean confirm = MessageDialog.openQuestion(
                        getShell(),
                        Messages.ManageCustomParsersDialog_DeleteParserDialogHeader,
                        Messages.ManageCustomParsersDialog_DeleteConfirmation + parserList.getSelection()[0] + "?"); //$NON-NLS-1$
                if (confirm) {
                    if (txtButton.getSelection()) {
                        CustomTxtTraceDefinition.delete(parserList.getSelection()[0]);
                    } else if (xmlButton.getSelection()) {
                        CustomXmlTraceDefinition.delete(parserList.getSelection()[0]);
                    }
                    fillParserList();
                }
            }});

        new Label(buttonContainer, SWT.NONE); // filler
        
        importButton = new Button(buttonContainer, SWT.PUSH);
        importButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        importButton.setText(Messages.ManageCustomParsersDialog_ImportButtonLabel);
        importButton.addSelectionListener(new SelectionListener(){
            @Override
			public void widgetDefaultSelected(SelectionEvent e) {}
            @Override
			public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
                dialog.setText(Messages.ManageCustomParsersDialog_ImportParserSelection);
                dialog.setFilterExtensions(new String[] {"*.xml", "*"}); //$NON-NLS-1$ //$NON-NLS-2$
                String path = dialog.open();
                if (path != null) {
                    CustomTraceDefinition[] defs = null;
                    if (txtButton.getSelection()) {
                        defs = CustomTxtTraceDefinition.loadAll(path);
                    } else if (xmlButton.getSelection()) {
                        defs = CustomXmlTraceDefinition.loadAll(path);
                    }
                    if (defs != null && defs.length > 0) {
                        for (CustomTraceDefinition def : defs) {
                            def.save();
                        }
                        fillParserList();
                    }
                }
            }});

        exportButton = new Button(buttonContainer, SWT.PUSH);
        exportButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        exportButton.setText(Messages.ManageCustomParsersDialog_ExportButtonLabel);
        exportButton.setEnabled(false);
        exportButton.addSelectionListener(new SelectionListener(){
            @Override
			public void widgetDefaultSelected(SelectionEvent e) {}
            @Override
			public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
                dialog.setText(Messages.ManageCustomParsersDialog_ExportParserSelection + parserList.getSelection()[0]);
                dialog.setFilterExtensions(new String[] {"*.xml", "*"}); //$NON-NLS-1$ //$NON-NLS-2$
                String path = dialog.open();
                if (path != null) {
                    CustomTraceDefinition def = null;
                    if (txtButton.getSelection()) {
                        def = CustomTxtTraceDefinition.load(parserList.getSelection()[0]);
                    } else if (xmlButton.getSelection()) {
                        def = CustomXmlTraceDefinition.load(parserList.getSelection()[0]);
                    }
                    if (def != null) {
                        def.save(path);
                    }
                }
            }});

        parseButton = new Button(buttonContainer, SWT.PUSH);
        parseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        parseButton.setText(Messages.ManageCustomParsersDialog_ParseButtonLabel);
        parseButton.setEnabled(false);
        parseButton.addSelectionListener(new SelectionListener(){
            @Override
			public void widgetDefaultSelected(SelectionEvent e) {}
            @Override
			public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
                dialog.setText(Messages.ManageCustomParsersDialog_TraceSelection + parserList.getSelection()[0]);
                if (xmlButton.getSelection()) {
                    dialog.setFilterExtensions(new String[] {"*.xml", "*"}); //$NON-NLS-1$ //$NON-NLS-2$
                }
                String path = dialog.open();
                String parser = null;
                if (path != null) {
                    CustomTraceDefinition def = null;
                    if (txtButton.getSelection()) {
                        def = CustomTxtTraceDefinition.load(parserList.getSelection()[0]);
                        parser = CustomTxtTrace.class.getCanonicalName() + "." + def.definitionName; //$NON-NLS-1$
                    } else if (xmlButton.getSelection()) {
                        def = CustomXmlTraceDefinition.load(parserList.getSelection()[0]);
                        parser = CustomXmlTrace.class.getCanonicalName() + "." + def.definitionName; //$NON-NLS-1$
                    }
                    if (def != null) {
                        try {
                            IWorkspace workspace = ResourcesPlugin.getWorkspace();
                            IPath location = Path.fromOSString(path);
                            IFile file = workspace.getRoot().getFileForLocation(location);
                            if (file == null) {
                                file = createLink(new File(location.toPortableString()).toURI());
                            }
                            file.setPersistentProperty(ParserProviderManager.PARSER_PROPERTY, parser);
                            IEditorInput editorInput = new FileEditorInput(file);
                            IWorkbench wb = PlatformUI.getWorkbench();
                            IWorkbenchPage activePage = wb.getActiveWorkbenchWindow().getActivePage();
          
                            String editorId = TmfEventsEditor.ID;
                            IEditorPart editor = activePage.findEditor(editorInput);
                            if (editor != null && editor instanceof IReusableEditor) {
                                activePage.reuseEditor((IReusableEditor)editor, editorInput);
                                activePage.activate(editor);
                            } else {
                                editor = activePage.openEditor(editorInput, editorId);
                            }
                        } catch (CoreException e1) {
                            MessageDialog.openError(getShell(), "Parse Error", e1.getMessage()); //$NON-NLS-1$
                        }
                    }
                }
            }});

        fillParserList();

        getShell().setMinimumSize(300, 275);
        return composite;
    }

	// /////////////////////////////////////////////////////////////////////////////
	// FIXME: Duplicated in TmfEventsEditor
	// From the legacy ProjectView
	// /////////////////////////////////////////////////////////////////////////////

	// ------------------------------------------------------------------------
	// Static methods
	// ------------------------------------------------------------------------

	static public IFolder getActiveProjectTracesFolder() {
		IEclipsePreferences node = new InstanceScope()
				.getNode(TmfUiPlugin.PLUGIN_ID);
		String activeProjectName = node.get(
				TmfUiPreferenceInitializer.ACTIVE_PROJECT_PREFERENCE,
				TmfUiPreferenceInitializer.ACTIVE_PROJECT_DEFAULT);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		for (IProject project : projects) {
			if (project.isAccessible()
					&& project.getName().equals(activeProjectName)) {
				return project.getFolder(TmfTraceFolder.TRACE_FOLDER_NAME);
			}
		}
		return null;
	}

	static public IFile createLink(URI uri) throws CoreException {
		IFolder folder = getActiveProjectTracesFolder();
		if (folder == null || !folder.exists()) {
			throw new CoreException(new Status(Status.ERROR,
					TmfUiPlugin.PLUGIN_ID, "No active project set")); //$NON-NLS-1$
		}
		String path = uri.getPath();
		// TODO: support duplicate file names
		IFile file = folder.getFile(path.substring(path
				.lastIndexOf(Path.SEPARATOR)));
		if (!file.exists()) {
			file.createLink(uri, IResource.NONE, null);
		}
		return file;
	}

	// /////////////////////////////////////////////////////////////////////////////

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, false);
    }

    private void fillParserList() {
        parserList.removeAll();
        if (txtButton.getSelection()) {
            for (CustomTxtTraceDefinition def : CustomTxtTraceDefinition.loadAll()) {
                parserList.add(def.definitionName);
            }
        } else if (xmlButton.getSelection()) {
            for (CustomXmlTraceDefinition def : CustomXmlTraceDefinition.loadAll()) {
                parserList.add(def.definitionName);
            }
        }
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        exportButton.setEnabled(false);
        parseButton.setEnabled(false);
    }

}
