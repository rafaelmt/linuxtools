/*******************************************************************************
 * Copyright (c) 2009, 2010, 2011 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.ui.project.wizards;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.linuxtools.tmf.ui.project.model.ITmfProjectModelElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfExperimentElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.linuxtools.tmf.ui.project.model.TraceFolderContentProvider;
import org.eclipse.linuxtools.tmf.ui.project.model.TraceFolderLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * <b><u>SelectTracesWizardPage</u></b>
 * <p>
 */
public class SelectTracesWizardPage extends WizardPage {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final TmfProjectElement fProject;
    private final TmfExperimentElement fExperiment;
    private HashMap<String, TmfTraceElement> fPreviousTraces;
    private CheckboxTableViewer fCheckboxTableViewer;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    protected SelectTracesWizardPage(TmfProjectElement project, TmfExperimentElement experiment) {
        super(""); //$NON-NLS-1$
        setTitle(Messages.SelectTracesWizardPage_WindowTitle);
        setDescription(Messages.SelectTracesWizardPage_Description);
        fProject = project;
        fExperiment = experiment;
    }

    // ------------------------------------------------------------------------
    // Dialog
    // ------------------------------------------------------------------------

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new FormLayout());
        setControl(container);

        fCheckboxTableViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER);
        fCheckboxTableViewer.setContentProvider(new TraceFolderContentProvider());
        fCheckboxTableViewer.setLabelProvider(new TraceFolderLabelProvider());

        final Table table = fCheckboxTableViewer.getTable();
        final FormData formData = new FormData();
        formData.bottom = new FormAttachment(100, 0);
        formData.right = new FormAttachment(100, 0);
        formData.top = new FormAttachment(0, 0);
        formData.left = new FormAttachment(0, 0);
        table.setLayoutData(formData);
        table.setHeaderVisible(true);

        final TableColumn tableColumn = new TableColumn(table, SWT.NONE);
        tableColumn.setWidth(200);
        tableColumn.setText(Messages.SelectTracesWizardPage_TraceColumnHeader);

        // Get the list of traces already part of the experiment
        fPreviousTraces = new HashMap<String, TmfTraceElement>();
        for (ITmfProjectModelElement child : fExperiment.getChildren()) {
            if (child instanceof TmfTraceElement) {
                TmfTraceElement trace = (TmfTraceElement) child;
                String name = trace.getResource().getName();
                fPreviousTraces.put(name, trace);
            }
        }

        // Populate the list of traces to choose from
        Set<String> keys = fPreviousTraces.keySet();
        TmfTraceFolder traceFolder = fProject.getTracesFolder();
        fCheckboxTableViewer.setInput(traceFolder);

        // Set the checkbox for the traces already included
        int index = 0;
        Object element = fCheckboxTableViewer.getElementAt(index++);
        while (element != null) {
            if (element instanceof TmfTraceElement) {
                TmfTraceElement trace = (TmfTraceElement) element;
                if (keys.contains(trace.getResource().getName())) {
                    fCheckboxTableViewer.setChecked(element, true);
                }
            }
            element = fCheckboxTableViewer.getElementAt(index++);
        }
    }

    public boolean performFinish() {

        IFolder experiment = fExperiment.getResource();

        // Add the selected traces to the experiment
        Set<String> keys = fPreviousTraces.keySet();
        TmfTraceElement[] traces = getSelection();
        for (TmfTraceElement trace : traces) {
            String name = trace.getResource().getName();
            if (keys.contains(name)) {
                fPreviousTraces.remove(name);
            } else {
                IResource resource = trace.getResource();
                IPath location = resource.getLocation();
                createLink(experiment, trace, resource, location);
            }
        }

        // Remove traces that were unchecked (thus left in fPreviousTraces)
        keys = fPreviousTraces.keySet();
        for (String key : keys) {
            fExperiment.removeChild(fPreviousTraces.get(key));
            IResource resource = experiment.findMember(key);
            try {
                resource.delete(true, null);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        fProject.refresh();

        return true;
    }

    /**
     * Create a link to the actual trace and set the trace type
     */
    private void createLink(IFolder experiment, TmfTraceElement trace, IResource resource, IPath location) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        try {
            Map<QualifiedName, String> properties = trace.getResource().getPersistentProperties();
            String bundleName = properties.get(TmfTraceElement.TRACEBUNDLE);
            String traceType = properties.get(TmfTraceElement.TRACETYPE);
            String iconUrl = properties.get(TmfTraceElement.TRACEICON);
            
            if (resource instanceof IFolder) {
                IFolder folder = experiment.getFolder(trace.getName());
                if (workspace.validateLinkLocation(folder, location).isOK()) {
                    folder.createLink(location, IResource.REPLACE, null);
                    setProperties(folder, bundleName, traceType, iconUrl);

                } else {
                    System.out.println("Invalid Trace Location"); //$NON-NLS-1$
                }
            } else {
                IFile file = experiment.getFile(trace.getName());
                if (workspace.validateLinkLocation(file, location).isOK()) {
                    file.createLink(location, IResource.REPLACE, null);
                    setProperties(file, bundleName, traceType, iconUrl);
                } else {
                    System.out.println("Invalid Trace Location"); //$NON-NLS-1$
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    private void setProperties(IResource resource, String bundleName, String traceType, String iconUrl) throws CoreException {
        resource.setPersistentProperty(TmfTraceElement.TRACEBUNDLE, bundleName);
        resource.setPersistentProperty(TmfTraceElement.TRACETYPE, traceType);
        resource.setPersistentProperty(TmfTraceElement.TRACEICON, iconUrl);
    }

    /**
     * Get the list of selected traces
     */
    private TmfTraceElement[] getSelection() {
        Vector<TmfTraceElement> traces = new Vector<TmfTraceElement>();
        Object[] selection = fCheckboxTableViewer.getCheckedElements();
        for (Object sel : selection) {
            if (sel instanceof TmfTraceElement)
                traces.add((TmfTraceElement) sel);
        }
        TmfTraceElement[] result = new TmfTraceElement[traces.size()];
        traces.toArray(result);
        return result;
    }

}
