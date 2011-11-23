/*******************************************************************************
 * (C) Copyright IBM Corp. 2010
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thavidu Ranatunga (IBM) - Initial implementation
 *******************************************************************************/
package org.eclipse.linuxtools.perf.ui;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.linuxtools.perf.PerfPlugin;
import org.eclipse.linuxtools.perf.model.TreeParent;
import org.eclipse.ui.IViewSite;

public class PerfViewContentProvider implements IStructuredContentProvider, 
ITreeContentProvider {
	private TreeParent invisibleRoot;
	private IViewSite viewSite;
	public PerfViewContentProvider(IViewSite vs) {
		viewSite = vs; 
	}
	
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}
	public void dispose() {
	}
	public Object[] getElements(Object parent) {
		if (parent.equals(viewSite)) {
			if (invisibleRoot==null) initialize();
			return getChildren(invisibleRoot);
		}
		return getChildren(parent);
	}
	public Object getParent(Object child) {
		if (child instanceof TreeParent) {
			return ((TreeParent)child).getParent();
		}
		return null;
	}
	public Object [] getChildren(Object parent) {
		if (parent instanceof TreeParent) {
			return ((TreeParent)parent).getChildren();
		}
		return new Object[0];
	}
	public boolean hasChildren(Object parent) {
		if (parent instanceof TreeParent)
			return ((TreeParent)parent).hasChildren();
		return false;
	}

	private void initialize() {
		invisibleRoot = PerfPlugin.getDefault().getModelRoot();			
	}
}
