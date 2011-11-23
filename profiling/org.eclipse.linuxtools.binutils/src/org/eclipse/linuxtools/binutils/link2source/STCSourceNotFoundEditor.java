/*******************************************************************************
 * Copyright (c) 2009 STMicroelectronics.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Xavier Raynaud <xavier.raynaud@st.com> - initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.binutils.link2source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.sourcelookup.MappingSourceContainer;
import org.eclipse.cdt.debug.internal.core.sourcelookup.MapEntrySourceContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditor;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.linuxtools.binutils.Activator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorInput;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Editor that lets you select a replacement for the missing source file
 * and modifies the source locator accordingly.
 *
 */
public class STCSourceNotFoundEditor extends CommonSourceNotFoundEditor {

	private final static String foundMappingsContainerName = "Found Mappings"; //$NON-NLS-1$
	

	private Button locateFileButton;
	private Button editLookupButton;

	/** Constructor */
	public STCSourceNotFoundEditor() {
	}

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
	}

	private IPath getMissingFile() {
		IEditorInput i = this.getEditorInput();
		if (i instanceof STCSourceNotFoundEditorInput) {
			STCSourceNotFoundEditorInput input = (STCSourceNotFoundEditorInput) i;
			return input.getSourcePath();
		}
		return null;
	}
	
	private int getLineNumber() {
		IEditorInput i = this.getEditorInput();
		if (i instanceof STCSourceNotFoundEditorInput) {
			STCSourceNotFoundEditorInput input = (STCSourceNotFoundEditorInput) i;
			return input.getLineNumber();
		}
		return -1;
	}

	private IProject getProject() {
		IEditorInput i = this.getEditorInput();
		if (i instanceof STCSourceNotFoundEditorInput) {
			STCSourceNotFoundEditorInput input = (STCSourceNotFoundEditorInput) i;
			return input.getProject();
		}
		return null;
	}

	public void setInput(IEditorInput input) {
		super.setInput(input);
		syncButtons();
	}
	
	private boolean isValidMissingFile() {
		IPath p = getMissingFile();
		boolean v = (p != null && p.toString().length() > 0);
		return v;
	}
	

	private void syncButtons() {
		boolean v = isValidMissingFile();
		if (locateFileButton != null)
			locateFileButton.setVisible(v);
		if (editLookupButton != null)
			editLookupButton.setVisible(v);
	}

	protected String getText() {
		boolean v = isValidMissingFile();
		if (v) {
			return "Can't find a source file at \""
			+ getMissingFile()
			+ "\" \n";
		} else {
			return "No source available for \"\"";
		}
	}

	protected void createButtons(Composite parent) {
		{
			GridData data;
			locateFileButton = new Button(parent, SWT.PUSH);
			data = new GridData();
			data.grabExcessHorizontalSpace = false;
			data.grabExcessVerticalSpace = false;
			locateFileButton.setLayoutData(data);
			locateFileButton.setText("Locate File..."); //$NON-NLS-1$
			locateFileButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					locateFile();
				}
			});
		}

		{
			GridData data;
			editLookupButton = new Button(parent, SWT.PUSH);
			data = new GridData();
			data.grabExcessHorizontalSpace = false;
			data.grabExcessVerticalSpace = false;
			editLookupButton.setLayoutData(data);
			editLookupButton.setText("Edit Source Lookup Path..."); 
			editLookupButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					editSourceLookupPath();
				}
			});
		}
		syncButtons();
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditor#editSourceLookupPath()
	 */
	@Override
	protected void editSourceLookupPath() {
		PreferenceDialog d = org.eclipse.ui.dialogs.PreferencesUtil.createPreferenceDialogOn(null, "org.eclipse.cdt.debug.ui.SourcePreferencePage", null, null);
		if (d.open() == PreferenceDialog.OK) {
			closeEditor();
			openSourceFileAtLocation(getProject(), getMissingFile(), getLineNumber());
		}
	}

	private void addSourceMappingToDirector(IPath missingPath, IPath newSourcePath, AbstractSourceLookupDirector director) throws CoreException {

		ArrayList<ISourceContainer> containerList = new ArrayList<ISourceContainer>(Arrays.asList(
				director.getSourceContainers()
		));
		boolean hasFoundMappings = false;
		MappingSourceContainer foundMappings = null;
		for (Iterator<ISourceContainer> iter = containerList.iterator(); iter.hasNext() && !hasFoundMappings;) {
			ISourceContainer container = iter.next();
			if (container instanceof MappingSourceContainer)
			{
				hasFoundMappings = container.getName().equals(foundMappingsContainerName);
				if (hasFoundMappings)
					foundMappings = (MappingSourceContainer) container;
			}
		}

		if (!hasFoundMappings) {
			foundMappings = new MappingSourceContainer(foundMappingsContainerName);
			foundMappings.init(director);
			containerList.add(foundMappings);
		}

		foundMappings.addMapEntry(new MapEntrySourceContainer(missingPath, newSourcePath));
		director.setSourceContainers(containerList.toArray(new ISourceContainer[containerList.size()]));
	}

	private void addSourceMappingToCommon(IPath missingPath, IPath newSourcePath) throws CoreException {
		AbstractSourceLookupDirector director = CDebugCorePlugin.getDefault().getCommonSourceLookupDirector();
		addSourceMappingToDirector(missingPath, newSourcePath, director);
		try {
			InstanceScope.INSTANCE.getNode(CDebugCorePlugin.PLUGIN_ID).flush();
		} catch (BackingStoreException e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Failed saving settings for content type " + CDebugCorePlugin.PLUGIN_ID, e);
			throw new CoreException(status);
		}
	}
	
	protected void locateFile() {
		FileDialog dialog = new FileDialog(getEditorSite().getShell(), SWT.NONE);
		IPath missingPath = getMissingFile();
		dialog.setFilterNames(new String[] {"Missing Source File"});
		dialog.setFilterExtensions(new String[] {"*." + missingPath.getFileExtension()}); //$NON-NLS-1$
		String res = dialog.open();
		if (res != null) {
			Path newPath = new Path(res);
			
			if (newPath.lastSegment().equalsIgnoreCase(missingPath.lastSegment())) {
				
				if (missingPath.segmentCount() > 1) {
					int missingPathSegCount = missingPath.segmentCount() - 2;
					int newPathSegCount = newPath.segmentCount() - 2;
					while (missingPathSegCount >= 0 && newPathSegCount >= 0) {
						if (!newPath.segment(newPathSegCount).equalsIgnoreCase(missingPath.segment(missingPathSegCount)))
							break;
						newPathSegCount--;
						missingPathSegCount--;
					}
					IPath compPath = missingPath.removeLastSegments(missingPath.segmentCount() - missingPathSegCount - 1);
					IPath newSourcePath = newPath.removeLastSegments(newPath.segmentCount() - newPathSegCount - 1);
					try {
						addSourceMappingToCommon(compPath, newSourcePath);							
					} catch (CoreException e) {}					
					
				}
				openSourceFileAtLocation(getProject(), newPath, getLineNumber());
				closeEditor();
			}
		}
	}
	
	protected void openSourceFileAtLocation(IProject project, IPath sourceLoc, int lineNumber){
		STLink2SourceSupport.sharedInstance.openSourceFileAtLocation(project, sourceLoc, lineNumber);
	}
}
