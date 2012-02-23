/**********************************************************************
 * Copyright (c) 2012 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.linuxtools.lttng.ui.views.control.dialogs;

import org.eclipse.linuxtools.lttng.ui.views.control.model.impl.TraceChannelComponent;
import org.eclipse.linuxtools.lttng.ui.views.control.model.impl.TraceSessionComponent;

/**
 * <b><u>IEnableEventsDialog</u></b>
 * <p>
 * Interface for a dialog box for collecting information about the events to enable.
 * </p>
 */
public interface IGetEventInfoDialog {
    
    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------
    /**
     * @return the session the events shall be enabled.
     */
    public TraceSessionComponent getSession();

    /**
     * @return the channel the events shall be enabled. Null for default channel.
     */
    public TraceChannelComponent getChannel();

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------
    /**
     * @return returns the open return value
     */
    int open();
}
