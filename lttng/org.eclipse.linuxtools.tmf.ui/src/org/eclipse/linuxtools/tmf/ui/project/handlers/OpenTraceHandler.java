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

import java.io.FileNotFoundException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.linuxtools.tmf.event.TmfEvent;
import org.eclipse.linuxtools.tmf.experiment.TmfExperiment;
import org.eclipse.linuxtools.tmf.signal.TmfExperimentSelectedSignal;
import org.eclipse.linuxtools.tmf.signal.TmfSignalManager;
import org.eclipse.linuxtools.tmf.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.editors.TmfEditorInput;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * <b><u>OpenTraceHandler</u></b>
 * <p>
 * TODO: Add support for multiple trace selection
 */
public class OpenTraceHandler extends AbstractHandler {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private TmfTraceElement fTrace = null;

    // ------------------------------------------------------------------------
    // Validation
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

        // Make sure there is only one selection and that it is a trace
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
        return (fTrace != null);
    }

    // ------------------------------------------------------------------------
    // Execution
    // ------------------------------------------------------------------------

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object execute(ExecutionEvent event) throws ExecutionException {

        // Check if we are closing down
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null)
            return null;

        // Check that the trace is valid
        if (fTrace == null) {
            return null;
        }

        TmfEvent traceEvent = fTrace.instantiateEvent();
        ITmfTrace trace = fTrace.instantiateTrace();
        if (trace == null) {
            displayErrorMsg(Messages.OpenTraceHandler_NoTraceType);
            return null;
        }

        // Get the editor_id from the extension point
        String editorId = fTrace.getEditorId();
        boolean usesEditor = editorId != null && editorId.length() > 0;

        try {
            trace.initTrace(fTrace.getLocation().getPath(), traceEvent.getClass(), usesEditor);
        } catch (FileNotFoundException e) {
            displayErrorMsg(Messages.OpenTraceHandler_NoTrace);
            return null;
        }

        if (usesEditor) {
            try {
                IResource resource = fTrace.getResource();
                IEditorInput editorInput = new TmfEditorInput(resource, trace);
                IWorkbench wb = PlatformUI.getWorkbench();
                IWorkbenchPage activePage = wb.getActiveWorkbenchWindow().getActivePage();

                IEditorPart editor = activePage.findEditor(editorInput);
                if (editor != null && editor instanceof IReusableEditor) {
                    activePage.reuseEditor((IReusableEditor) editor, editorInput);
                    activePage.activate(editor);
                } else {
                    editor = activePage.openEditor(editorInput, editorId);
                }
            } catch (PartInitException e) {
                e.printStackTrace();
            }

        } else {
            ITmfTrace[] traces = new ITmfTrace[] { trace };
            TmfExperiment experiment = new TmfExperiment(traceEvent.getClass(), fTrace.getName(), traces, trace.getCacheSize());
            TmfExperiment.setCurrentExperiment(experiment);
            TmfSignalManager.dispatchSignal(new TmfExperimentSelectedSignal(this, experiment));
        }
        return null;
    }

    private void displayErrorMsg(String errorMsg) {
        MessageBox mb = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
        mb.setText(Messages.OpenTraceHandler_Title);
        mb.setMessage(errorMsg);
        mb.open();
    }

}
