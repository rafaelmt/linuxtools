/*******************************************************************************
 * Copyright (c) 2009 STMicroelectronics.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marzia Maugeri <marzia.maugeri@st.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.dataviewers.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.linuxtools.dataviewers.abstractviewers.AbstractSTViewer;
import org.eclipse.linuxtools.dataviewers.abstractviewers.STDataViewersCSVExporter;
import org.eclipse.linuxtools.dataviewers.abstractviewers.STDataViewersImages;
import org.eclipse.linuxtools.dataviewers.abstractviewers.STDataViewersMessages;
import org.eclipse.linuxtools.dataviewers.dialogs.STDataViewersExportToCSVDialog;
import org.eclipse.swt.graphics.Image;


/**
 * This action export the STViewers data to CSV format file
 *
 */
public class STExportToCSVAction extends Action {
	
	private final AbstractSTViewer stViewer;
	
	private STDataViewersCSVExporter exporter;
	
	/**
	 * Job family for Export to CSV background jobs.
	 */
	public static final String EXPORT_TO_CSV_JOB_FAMILY = "Export to CSV";
	
	/**
	 * Constructor
	 * @param stViewer the stViewer to export
	 */
	public STExportToCSVAction(AbstractSTViewer stViewer) {
		super(STDataViewersMessages.exportToCSVAction_title);
		Image img = STDataViewersImages.getImage(STDataViewersImages.IMG_EXPORT); 
		super.setImageDescriptor(ImageDescriptor.createFromImage(img));
		
		this.stViewer = stViewer;
		this.exporter = new STDataViewersCSVExporter(stViewer);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		STDataViewersExportToCSVDialog dialog =
			new STDataViewersExportToCSVDialog(stViewer.getViewer().getControl().getShell(), exporter);
		if (dialog.open() == Dialog.OK) {
			Job exportToCSVJob =
				new Job("Export to CSV") {
					public IStatus run(IProgressMonitor monitor) {
						exporter.export(monitor);
						return Status.OK_STATUS;
					}

					@Override
					public boolean belongsTo(Object family) {
						return EXPORT_TO_CSV_JOB_FAMILY.equals(family);
					}
					
					
					
				};
			exportToCSVJob.setUser(true);
			exportToCSVJob.schedule();
		}
	}
	
	/**
	 * 
	 * @return exporter
	 */
	public STDataViewersCSVExporter getExporter(){
		return exporter;
	}
}
