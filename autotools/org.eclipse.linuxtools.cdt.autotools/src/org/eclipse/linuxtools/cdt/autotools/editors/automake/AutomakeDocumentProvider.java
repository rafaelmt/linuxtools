/*******************************************************************************
 * Copyright (c) 2006, 2007 Red Hat, Inc.
 * 
 * Largely copied from MakefileDocumentProvider which has the following
 * copyright notice:
 * 
 * Copyright (c) 2000, 2006 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.cdt.autotools.editors.automake;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.cdt.make.core.makefile.IMakefile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.linuxtools.cdt.autotools.internal.editors.automake.IMakefileDocumentProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;


public class AutomakeDocumentProvider extends TextFileDocumentProvider implements IMakefileDocumentProvider  {
	
	/**
	 * Remembers a IMakefile for each element.
	 */
	protected class AutomakefileFileInfo extends FileInfo {		
		public IMakefile fCopy;
	}
	
    /*
	 * @see org.eclipse.ui.editors.text.TextFileDocumentProvider#createEmptyFileInfo()
	 */
	protected FileInfo createEmptyFileInfo() {
		return new AutomakefileFileInfo();
	}
	
    /*
	 * @see org.eclipse.ui.editors.text.TextFileDocumentProvider#createFileInfo(java.lang.Object)
	 */
	protected FileInfo createFileInfo(Object element) throws CoreException {
		IMakefile original = null;
		if (element instanceof IFileEditorInput) {	
			IFileEditorInput input= (IFileEditorInput) element;
			if (input.getFile().exists())
				original= createMakefile(input.getFile().getLocation().toOSString());
		} else if (element instanceof IURIEditorInput) {
			IURIEditorInput input = (IURIEditorInput)element;
			original = createMakefile(input.getURI().getPath().toString());
		}
		if (original == null)
			return null;

		FileInfo info= super.createFileInfo(element);
		if (!(info instanceof AutomakefileFileInfo)) {
			return null;
		}

		AutomakefileFileInfo makefileInfo= (AutomakefileFileInfo) info;
		setUpSynchronization(makefileInfo);

		makefileInfo.fCopy = original;

		return makefileInfo;
	}
	
	/**
	 */
	private IMakefile createMakefile(String fileName) {
		IMakefile makefile = null;
		Automakefile automakefile = new Automakefile();
		try {
			automakefile.parse(fileName);
		} catch (IOException e) {
		}
		makefile = automakefile;
		return makefile;
	}
	
	/*
	 * @see org.eclipse.linuxtools.cdt.autotools.internal.editors.automake.IMakefileDocumentProvider#getWorkingCopy(java.lang.Object)
	 */
	public IMakefile getWorkingCopy(Object element) {
		FileInfo fileInfo= getFileInfo(element);		
		if (fileInfo instanceof AutomakefileFileInfo) {
			AutomakefileFileInfo info= (AutomakefileFileInfo) fileInfo;
			return info.fCopy;
		}
		return null;
	}
	
	/*
	 * @see org.eclipse.linuxtools.cdt.autotools.internal.editors.automake.IMakefileDocumentProvider#shutdown()
	 */
	public void shutdown() {
		Iterator e= getConnectedElementsIterator();
		while (e.hasNext())
			disconnect(e.next());
	}
	
	public void connect(Object element) throws CoreException {
		super.connect(element);
		IEditorInput input= (IEditorInput) element;
		IMakefile makefile = getWorkingCopy(element);
		IDocument document = getDocument(element);
		AutomakeErrorHandler errorHandler = new AutomakeErrorHandler(document);
		errorHandler.update(makefile);
	}
	
	public IDocument getDocument(Object element) {
		FileInfo info= (FileInfo) getFileInfo(element);
		if (info != null)
			return info.fTextFileBuffer.getDocument();
		return getParentProvider().getDocument(element);
	}
	
}