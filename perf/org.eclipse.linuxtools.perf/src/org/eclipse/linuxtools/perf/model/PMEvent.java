/*******************************************************************************
 * (C) Copyright 2010 IBM Corp. 2010
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thavidu Ranatunga (IBM) - Initial implementation.
 *******************************************************************************/
package org.eclipse.linuxtools.perf.model;

public class PMEvent extends TreeParent {

	public PMEvent(String name) {
		super(name);
		// Don't think theres anything else to do in here.
	}

	public PMCommand getCommand(String name) {
		TreeParent tmp = getChild(name);
		if ((tmp != null) && (tmp instanceof PMCommand)) {
			return (PMCommand) tmp;
		}
		return null;
	}
	
}
