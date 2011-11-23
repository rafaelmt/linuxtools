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

package org.eclipse.linuxtools.tmf.ui.project.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.linuxtools.tmf.ui.project.model.ITmfProjectModelElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfExperimentFolder;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.linuxtools.tmf.ui.project.wizards.RenameTraceDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * <b><u>RenameTraceHandler</u></b>
 * <p>
 * TODO: Implement me. Please.
 */
public class RenameTraceHandler extends AbstractHandler {

	private TmfTraceElement fTrace = null;
	
	// ------------------------------------------------------------------------
    // isEnabled
    // ------------------------------------------------------------------------

    @Override
    public boolean isEnabled() {

        // Check if we are closing down
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null)
            return false;

        // Get the selection
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IWorkbenchPart part = page.getActivePart();
        ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
        if (selectionProvider == null)
            return false;
        ISelection selection = selectionProvider.getSelection();

        // Make sure there is only selection and that it is an experiment
        fTrace = null;
        if (selection instanceof TreeSelection) {
            TreeSelection sel = (TreeSelection) selection;
    		// There should be only one item selected as per the plugin.xml
            Object element = sel.getFirstElement();
            if (element instanceof TmfTraceElement) {
            	fTrace = (TmfTraceElement) element;
            }
        }

        // We only enable opening from the Traces folder for now
        return (fTrace != null && fTrace.getParent() instanceof TmfTraceFolder);
    }

    // ------------------------------------------------------------------------
    // Execution
    // ------------------------------------------------------------------------

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        // Check if we are closing down
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null)
            return null;

        // Fire the Rename Trace dialog
        Shell shell = window.getShell();
        TmfTraceFolder traceFolder = (TmfTraceFolder) fTrace.getParent();
        TmfTraceElement oldTrace = fTrace;
        RenameTraceDialog dialog = new RenameTraceDialog(shell, fTrace);
        if (dialog.open() != Window.OK)
            return null;

        // Locate the new trace object
        TmfTraceElement newTrace = null;
        String newTraceName = dialog.getNewTraceName();
        for (ITmfProjectModelElement element : traceFolder.getChildren()) {
            if (element instanceof TmfTraceElement) {
                TmfTraceElement trace = (TmfTraceElement) element;
                if (trace.getName().equals(newTraceName)) {
                    newTrace = trace;
                    break;
                }
            }
        }
        if (newTrace == null)
            return null;

        List<WorkspaceModifyOperation> removeOps = new ArrayList<WorkspaceModifyOperation>();
        TmfExperimentFolder experimentFolder = newTrace.getProject().getExperimentsFolder();
        for (final ITmfProjectModelElement experiment : experimentFolder.getChildren()) {
            for (final ITmfProjectModelElement trace : experiment.getChildren()) {
                if (trace.equals(oldTrace)) {
                    // Create a link to the renamed trace
                    createTraceLink(newTrace, experiment);
                    
                    // Queue the removal of the old trace link
                    removeOps.add(new WorkspaceModifyOperation() {
                        @Override
                        protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
                            experiment.removeChild(trace);
                            trace.getResource().delete(true, null);
                            experiment.refresh();
                        }
                    });
                }
            }
        }

        for (WorkspaceModifyOperation operation : removeOps) {
            try {
                PlatformUI.getWorkbench().getProgressService().busyCursorWhile(operation);
            } catch (InterruptedException exception) {
            } catch (InvocationTargetException exception) {
            } catch (RuntimeException exception) {
            }
        }
        
        return null;
    }

    private void createTraceLink(TmfTraceElement trace, final ITmfProjectModelElement experiment) {
        try {
            IResource resource = trace.getResource();
            IPath location = resource.getLocation();
            if (resource instanceof IFolder) {
                IFolder folder = ((IFolder) experiment.getResource()).getFolder(trace.getName());
                if (ResourcesPlugin.getWorkspace().validateLinkLocation(folder, location).isOK()) {
                    folder.createLink(location, IResource.REPLACE, null);
                }
                else {
                    System.out.println("Invalid Trace Location"); //$NON-NLS-1$
                }
            }
            else {
                IFile file = ((IFolder) experiment.getResource()).getFile(trace.getName());
                if (ResourcesPlugin.getWorkspace().validateLinkLocation(file, location).isOK()) {
                    file.createLink(location, IResource.REPLACE, null);
                }
                else {
                    System.out.println("Invalid Trace Location"); //$NON-NLS-1$
                }
            }
            experiment.refresh();
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

}
