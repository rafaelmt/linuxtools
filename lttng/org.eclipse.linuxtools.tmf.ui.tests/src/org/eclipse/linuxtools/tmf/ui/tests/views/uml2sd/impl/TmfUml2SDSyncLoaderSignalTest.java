/*******************************************************************************
 * Copyright (c) 2011 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.tmf.ui.tests.views.uml2sd.impl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.linuxtools.tmf.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.event.TmfTimestamp;
import org.eclipse.linuxtools.tmf.signal.TmfRangeSynchSignal;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.core.GraphNode;

public class TmfUml2SDSyncLoaderSignalTest extends TestCase {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    private Uml2SDTestFacility fFacility;
    private Uml2SDSignalValidator fTmfComponent;

    // ------------------------------------------------------------------------
    // Static methods
    // ------------------------------------------------------------------------ 

    /**
     * Returns test setup used when executing test case stand-alone.
     * @return Test setup class 
     */
    public static Test suite() {
        return new Uml2SDTestSetup(new TestSuite(TmfUml2SDSyncLoaderSignalTest.class));
    }
    
    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    public TmfUml2SDSyncLoaderSignalTest() {
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fFacility = Uml2SDTestFacility.getInstance();
        fFacility.selectExperiment();
    }

    @Override
    public void tearDown() throws Exception {
        fFacility.disposeExperiment();
        fFacility = null;
        super.tearDown();
    }
    
    @SuppressWarnings("nls")
    public void testSignalHandling() {
        
        TmfTimeRange range = new TmfTimeRange(new Uml2SDTestTimestamp(9789689220871L), new Uml2SDTestTimestamp(9789773881426L));
        // Get range window for tests below
        TmfTimestamp rangeWindow =  range.getEndTime().getDelta(range.getStartTime());
        TmfTimestamp currentTime = new Uml2SDTestTimestamp(9789773782043L);

        fFacility.getTrace().broadcast(new TmfRangeSynchSignal(this, range, currentTime));
        fFacility.delay(IUml2SDTestConstants.BROADCAST_DELAY);

        fTmfComponent = new Uml2SDSignalValidator();

        /*
         * Test Case: 001
         * Description: Verify that time range signal is send with correct values when going to first page  
         * Verified Methods: broadcast()
         * Expected result: Time range sync signal is sent with correct range and current time.
         */
        currentTime = new Uml2SDTestTimestamp(9788641608418L);
        range = new TmfTimeRange(currentTime, new Uml2SDTestTimestamp(currentTime.getValue() + rangeWindow.getValue()));
        
        fTmfComponent.setSignalError(false);
        fTmfComponent.setSignalReceived(false);
        fTmfComponent.setCurrentTimeError(false);
        fTmfComponent.setRangeError(false);
        fTmfComponent.setSourceError(false);

        // set expected values
        fTmfComponent.setSource(fFacility.getLoader());
        fTmfComponent.setCurrentTime(currentTime);
        fTmfComponent.setCurrentRange(range);

        fFacility.firstPage();
        assertTrue("TmfRangeSynchSignal",  fTmfComponent.isSignalReceived());
        assertFalse("TmfRangeSynchSignal", fTmfComponent.isSignalError());
        assertFalse("TmfRangeSynchSignal", fTmfComponent.isCurrentTimeError());
        assertFalse("TmfRangeSynchSignal", fTmfComponent.isSourceError());
        assertFalse("TmfRangeSynchSignal", fTmfComponent.isRangeError());
        
        /*
         * Test Case: 002
         * Description: Verify that time sync signal is sent correctly after selection   
         * Verified Methods: loader.broadcast(), testSelectionChanged
         * Expected result: Time sync signal is sent with correct current time.
         */
        fTmfComponent.setSignalReceived(false);

        int count = fFacility.getSdView().getFrame().syncMessageCount();
        assertEquals("Test Preparation", IUml2SDTestConstants.MAX_MESSEAGES_PER_PAGE, count);
        GraphNode node = fFacility.getSdView().getFrame().getSyncMessage(3);

        // set expected values
        fTmfComponent.setSource(fFacility.getLoader());
        fTmfComponent.setCurrentTime(new Uml2SDTestTimestamp(9788642113228L));
        fTmfComponent.setCurrentRange(null); // not used

        fFacility.getSdView().getSDWidget().moveTo(node); // selects the given node
        assertTrue("TmfTimeSynchSignal", fTmfComponent.isSignalReceived());
        assertFalse("TmfTimeSynchSignal", fTmfComponent.isSignalError());
        assertFalse("TmfTimeSynchSignal", fTmfComponent.isCurrentTimeError());
        assertFalse("TmfTimeSynchSignal", fTmfComponent.isSourceError());

        fTmfComponent.setSignalReceived(false);
        
        fTmfComponent.dispose();
    }
}
