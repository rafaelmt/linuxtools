/*******************************************************************************
 * Copyright (c) 2011 MontaVista Software
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Yufen Kuo (ykuo@mvista.com) - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.lttng.ui.views.project.dialogs;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.linuxtools.lttng.ui.TraceHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

public class TraceLibraryPathPropertyPage extends PropertyPage {

    private static final String LTTVTRACEREAD_LOADER_LIBNAME = "lttvtraceread_loader";
    private Button browsePathButton;
    private Text traceLibraryPath;

    @Override
    protected Control createContents(Composite parent) {
        Composite client = new Composite(parent, SWT.NONE);
        client.setLayoutData(new GridData(GridData.FILL_BOTH));

        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        client.setLayout(layout);

        Label label = new Label(client, SWT.NONE);
        label.setText(Messages.TraceLibraryPath_label);
        traceLibraryPath = new Text(client, SWT.BORDER);
        traceLibraryPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                false));
        traceLibraryPath.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                setValid(validateInputs());
            }

        });
        browsePathButton = new Button(client, SWT.PUSH);
        browsePathButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false,
                false));
        browsePathButton.setText(Messages.TraceLibraryPath_browseBtn);
        browsePathButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                String dir = new DirectoryDialog(Display.getDefault()
                        .getActiveShell()).open();
                if (dir != null) {
                    traceLibraryPath.setText(dir);
                }

            }

        });
        performDefaults();
        return client;
    }

    public boolean validateInputs() {
        String path = traceLibraryPath.getText();
        if (path != null && !path.trim().isEmpty()) {
            File file = new File(path);
            if (file.exists() && file.isDirectory()) {
                File loaderLib = new File(path,
                        System.mapLibraryName(LTTVTRACEREAD_LOADER_LIBNAME));
                if (!loaderLib.exists()) {
                    setErrorMessage(Messages.TraceLibraryPathWizardPage_TraceLoaderLibrary_notExists);
                    return false;
                }
            } else {
                setErrorMessage(Messages.TraceLibraryPathWizardPage_SpecifiedTraceLibraryLocation_notExists);
                return false;
            }
        }
        setErrorMessage(null);
        return true;
    }

    @Override
    protected void performDefaults() {
        IResource resource = (IResource) getElement().getAdapter(
                IResource.class);
        IProject project = resource.getProject();
        if (project != null) {
            String traceLibDir = TraceHelper.getTraceLibDirFromProject(project);
            if (traceLibDir != null) {
                traceLibraryPath.setText(traceLibDir);
            }
        }
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        IResource resource = (IResource) getElement().getAdapter(
                IResource.class);
        IProject project = resource.getProject();
        boolean ok = false;
        if (project != null) {
            String libPath = traceLibraryPath.getText();
            if (libPath == null || libPath.trim().isEmpty())
                ok = TraceHelper.removeProjectPreference(project,
                        "traceLibraryPath");
            else
                ok = TraceHelper.setProjectPreference(project,
                        "traceLibraryPath", traceLibraryPath.getText());
        }
        return ok && super.performOk();
    }

}
