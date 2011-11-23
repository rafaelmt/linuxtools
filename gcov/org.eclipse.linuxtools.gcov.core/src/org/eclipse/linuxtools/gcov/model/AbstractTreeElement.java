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
package org.eclipse.linuxtools.gcov.model;

import java.util.LinkedList;

public abstract class AbstractTreeElement implements TreeElement{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4911602250295116203L;
	private final TreeElement parent;
	private final LinkedList<TreeElement> children = new LinkedList<TreeElement>();
	private final String name;
	private final int totalLines;
	private final int executedLines;
	private final int instrumentedLines;
	
	public AbstractTreeElement(TreeElement parent, String name, int totalLines,
			int executedLines, int instrumentedLines) {
		this.parent = parent;
		this.name = name;
		this.totalLines = totalLines;
		this.executedLines = executedLines;
		this.instrumentedLines = instrumentedLines;
	}
	
	public TreeElement getParent() {
		return parent;
	}
	
	public boolean hasChildren() {
		return (children.size()>0);
	}

	public LinkedList<? extends TreeElement> getChildren() {
		return children;
	}

	public String getName() {
		return name;
	}
	
	public TreeElement getRoot() {
		if (parent == null) return this;
		return parent.getRoot();
	}

	public int getExecutedLines() {
		return executedLines;
	}
	
	public int getInstrumentedLines() {
		return instrumentedLines;
	}

	public float getCoveragePercentage() {
		 if (instrumentedLines !=0 )
			return (100.f*executedLines)/(instrumentedLines);
		else return 0;
	}

	public void addChild(TreeElement child){
		children.add(child);
	}
	
	public int getTotalLines() {
		return totalLines;
	}
}