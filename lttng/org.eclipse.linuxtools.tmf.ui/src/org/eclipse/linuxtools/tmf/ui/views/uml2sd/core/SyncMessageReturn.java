/**********************************************************************
 * Copyright (c) 2005, 2006, 2011 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * $Id: SyncMessageReturn.java,v 1.2 2006/09/20 20:56:25 ewchan Exp $
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 * Bernd Hufmann - Updated for TMF
 **********************************************************************/
package org.eclipse.linuxtools.tmf.ui.views.uml2sd.core;

import org.eclipse.linuxtools.tmf.ui.views.uml2sd.drawings.IGC;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.drawings.ISDPreferences;

/**
 * The message return graph node implementation.<br>
 * This class differs on the SynMessage class only on the drawing line style (dashed instead of plain line).<br>
 * Message return are generally associated to a message. This means, they are connected to the same lifelines than the
 * associated message but in the opposite direction and for a different event occurrence.<br>
 * <br>
 * WARNING: The association validity is not checked, it is not necessary to provide a valid association, not even needed
 * to set an association to drawn a message with a message return style.<br>
 * 
 * 
 * @see org.eclipse.linuxtools.tmf.ui.views.uml2sd.core.SyncMessage SyncMessage for usage example
 * @author sveyrier
 * 
 */
public class SyncMessageReturn extends SyncMessage {

    /**
     * The associated message(the message it is the return).
     */
    protected SyncMessage message = null;

    public static final String SYNC_MESS_RET_TAG = "SyncMessageRet"; //$NON-NLS-1$

    public SyncMessageReturn() {
        prefId = ISDPreferences.PREF_SYNC_MESS_RET;
    }

    /**
     * Set the associated message (the message it is the return).<br>
     * Setting the association will activate the navigation in the default sequence diagram implementation to the
     * message when the user right click on this message return.<br>
     * 
     * @param parentMessage the message to associate
     */
    public void setMessage(SyncMessage parentMessage) {
        message = parentMessage;
        message.setMessageReturn(this);
    }

    /**
     * Returns the syncMessage associated to this SyncMessageReturn
     * 
     * @return the associated message
     */
    public SyncMessage getMessage() {
        return message;
    }

    @Override
    public void draw(IGC context) {
        if (!isVisible())
            return;
        int oldStyle = context.getLineStyle();
        // Message return are dashed
        context.setLineStyle(context.getLineDotStyle());
        // Draw it selected?*/
        if (!isSelected()) {
            context.setBackground(Frame.getUserPref().getBackGroundColor(prefId));
            context.setForeground(Frame.getUserPref().getForeGroundColor(prefId));
        }
        super.draw(context);
        // restore the context
        context.setLineStyle(oldStyle);
    }

    @Override
    public String getArrayId() {
        return SYNC_MESS_RET_TAG;
    }
}
