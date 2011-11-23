/*******************************************************************************
 * Copyright (c) 2011 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Mathieu Denis (mathieu.denis@polymtl.ca)  - Initial design and implementation
 *   Bernd Hufmann - Changed interface and class name
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.ui.views.statistics;

/**
 * This class provides an extension for updating data model and to pass along more information beside events
 */
public interface ITmfExtraEventInfo {
    public String getTraceName();
}
