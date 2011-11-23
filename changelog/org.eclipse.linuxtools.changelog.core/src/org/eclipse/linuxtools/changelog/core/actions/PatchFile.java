/*******************************************************************************
 * Copyright (c) 2006, 2007 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Kyu Lee <klee@redhat.com> - initial API and implementation
 *    Jeff Johnston <jjohnstn@redhat.com> - add removed files support
 *******************************************************************************/
package org.eclipse.linuxtools.changelog.core.actions;


import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;

/**
 * 
 * @author klee
 *
 */
public class PatchFile {

	private IStorage storage;
	private ArrayList<PatchRangeElement> pranges = new ArrayList<PatchRangeElement>();
	
	private boolean newfile = false;
	private boolean removedfile = false;
	private IResource resource; // required only if dealing with change
	
	
	public boolean isNewfile() {
		return newfile;
	}

	public void setNewfile(boolean newfile) {
		this.newfile = newfile;
	}

	public boolean isRemovedFile() {
		return removedfile;
	}
	
	public void setRemovedFile(boolean removedfile) {
		this.removedfile = removedfile;
	}
	
	public PatchFile(IResource resource) {
		this.resource = resource;
	}
	
	public void addLineRange(int from, int to, boolean localChange) {
	
		pranges.add(new PatchRangeElement(from, to, localChange));
	}
	
	public PatchRangeElement[] getRanges() {
		Object[] tmpEle = pranges.toArray();
		PatchRangeElement[] ret = new PatchRangeElement[tmpEle.length];
		
		for (int i = 0; i < tmpEle.length; i++) {
			ret[i] = (PatchRangeElement) tmpEle[i];
		}
		return ret;
	}


	public IPath getPath() {
		return resource.getFullPath();
	}
	
	public IStorage getStorage() {
		return storage;
	}

	public void setStorage(IStorage storage) {
		this.storage = storage;
	}
	
	public IResource getResource() {
		return resource;
	}
	
	public int countRanges() {
		return pranges.size();
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (!(o instanceof PatchFile))
			return false;
		
		PatchFile that = (PatchFile) o;
		// check  fpath  +  count
		if (!this.resource.equals(that.resource) ||
				this.countRanges() != that.countRanges())
			return false;
		
		// check range elements
		PatchRangeElement[] thatsrange = that.getRanges();
		
		for(int i=0; i<this.countRanges();i++)
			if (!thatsrange[i].equals(pranges.get(i)))
				return false;
		return true;
	}
}