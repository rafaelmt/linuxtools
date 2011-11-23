/*******************************************************************************
 * Copyright (c) 2009 STMicroelectronics.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Xavier Raynaud <xavier.raynaud@st.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.gcov.view;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.linuxtools.dataviewers.abstractviewers.AbstractSTTreeViewer;
import org.eclipse.linuxtools.dataviewers.abstractviewers.ISTDataViewersField;
import org.eclipse.linuxtools.gcov.model.CovFileTreeElement;
import org.eclipse.linuxtools.gcov.model.CovFunctionTreeElement;
import org.eclipse.linuxtools.gcov.model.TreeElement;
import org.eclipse.linuxtools.gcov.parser.CovManager;
import org.eclipse.linuxtools.gcov.parser.SourceFile;
import org.eclipse.linuxtools.gcov.view.annotatedsource.OpenSourceFileAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;



public class CovViewer extends AbstractSTTreeViewer {

	private ISTDataViewersField[] fields;

	/**
	 * Constructor
	 * @param parent
	 */
	public CovViewer(Composite parent) {
		super(parent, SWT.BORDER | SWT.H_SCROLL| SWT.V_SCROLL | SWT.MULTI | 
				SWT.FULL_SELECTION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.linuxtools.dataviewers.abstractviewers.AbstractSTViewer#createContentProvider()
	 */
	@Override
	protected IContentProvider createContentProvider() {
		return CovFileContentProvider.sharedInstance;

	}

	/* (non-Javadoc)
	 * @see org.eclipse.linuxtools.dataviewers.abstractviewers.AbstractSTViewer#getAllFields()
	 */
	@Override
	public ISTDataViewersField[] getAllFields() {
		if (fields == null) {
			fields = new ISTDataViewersField[] { 
					new FieldName(),
					new FieldTotalLines(), 
					new FieldInstrumentedLines(),
					new FieldExecutedLines(), 
					new FieldCoveragePercentage() };
		}
		return fields;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.linuxtools.dataviewers.abstractviewers.AbstractSTViewer#getDialogSettings()
	 */
	public IDialogSettings getDialogSettings() {
		return org.eclipse.linuxtools.gcov.Activator.getDefault().getDialogSettings();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.linuxtools.dataviewers.abstractviewers.AbstractSTViewer#handleOpenEvent(org.eclipse.jface.viewers.OpenEvent)
	 */
	protected void handleOpenEvent(OpenEvent event) {

		IStructuredSelection selection = (IStructuredSelection) event
		.getSelection();
		TreeElement element = (TreeElement) selection.getFirstElement();

		if (element != null) {
			if (element.getParent() != null) {
				String sourceLoc = "";
				long lineNumber = 0;

				if (element.getClass() == CovFileTreeElement.class)
					sourceLoc = element.getName();

				else if (element.getClass() == CovFunctionTreeElement.class) {
					sourceLoc = ((CovFunctionTreeElement) element).getSourceFilePath();
					lineNumber  =((CovFunctionTreeElement)element).getFirstLnNmbr();
				}
				CovManager cvm = (CovManager) this.getInput();
				SourceFile sourceFile = cvm.getSourceFile(sourceLoc);
				if (sourceFile != null) {
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					String binaryLoc = cvm.getBinaryPath();
					IPath binaryPath = new Path(binaryLoc);
					IFile binary = root.getFileForLocation(binaryPath);
					IProject project = null;
					if (binary != null) project = binary.getProject();

					OpenSourceFileAction.sharedInstance.openAnnotatedSourceFile(project, 
							binary, sourceFile, (int)lineNumber);
				}
			}
		}
	}
}
