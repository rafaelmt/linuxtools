/*******************************************************************************
 * Copyright (c) 2009, 2010, 2011 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   William Bourque - Initial API and implementation
 *   Yuriy Vashchuk - GUI reorganisation, simplification and some related code improvements.
 *   Yuriy Vashchuk - Histograms optimisation.   
 *   Yuriy Vashchuk - Histogram Canvas Heritage correction
 *   Francois Chouinard - Cleanup and refactoring
 *******************************************************************************/

package org.eclipse.linuxtools.lttng.ui.views.histogram;

import org.eclipse.linuxtools.lttng.event.LttngEvent;
import org.eclipse.linuxtools.tmf.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.event.TmfTimestamp;
import org.eclipse.linuxtools.tmf.experiment.TmfExperiment;
import org.eclipse.linuxtools.tmf.request.ITmfDataRequest.ExecutionType;
import org.eclipse.linuxtools.tmf.signal.TmfExperimentRangeUpdatedSignal;
import org.eclipse.linuxtools.tmf.signal.TmfExperimentSelectedSignal;
import org.eclipse.linuxtools.tmf.signal.TmfRangeSynchSignal;
import org.eclipse.linuxtools.tmf.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.signal.TmfSignalManager;
import org.eclipse.linuxtools.tmf.signal.TmfTimeSynchSignal;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * <b><u>HistogramView</u></b>
 * <p>
 * The purpose of this view is to provide graphical time distribution statistics about the experiment/trace events.
 * <p>
 * The view is composed of two histograms and two controls:
 * <ul>
 * <li>an event distribution histogram for the whole experiment;
 * <li>an event distribution histogram for current time window (window span);
 * <li>the timestamp of the currently selected event;
 * <li>the window span (size of the time window of the smaller histogram).
 * </ul>
 * The histograms x-axis show their respective time range.
 */
public class HistogramView extends TmfView {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    // The view ID as defined in plugin.xml
    public static final String ID = "org.eclipse.linuxtools.lttng.ui.views.histogram"; //$NON-NLS-1$

    // The initial window span (in nanoseconds)
    public static long INITIAL_WINDOW_SPAN = (1L * 100 * 1000 * 1000); // .1sec

    // Time scale
    private final byte TIME_SCALE = Histogram.TIME_SCALE;

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    // Parent widget
    private Composite fParent;

    // The current experiment
    private TmfExperiment<LttngEvent> fCurrentExperiment;

    // Current timestamp/time window
    private long fExperimentStartTime;
    private long fExperimentEndTime;
    private long fWindowStartTime;
    private long fWindowEndTime;
    private long fWindowSpan = INITIAL_WINDOW_SPAN;
    private long fCurrentTimestamp;

    // Time controls
    private HistogramTextControl fCurrentEventTimeControl;
    private HistogramTextControl fTimeSpanControl;

    // Histogram/request for the full trace range
    private static FullTraceHistogram fFullTraceHistogram;
    private HistogramRequest fFullTraceRequest;

    // Histogram/request for the selected time range
    private static TimeRangeHistogram fTimeRangeHistogram;
    private HistogramRequest fTimeRangeRequest;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    public HistogramView() {
        super(ID);
    }

    @Override
    public void dispose() {
        if (fTimeRangeRequest != null && !fTimeRangeRequest.isCompleted()) {
            fTimeRangeRequest.cancel();
        }
        if (fFullTraceRequest != null && !fFullTraceRequest.isCompleted()) {
            fFullTraceRequest.cancel();
        }
        fFullTraceHistogram.dispose();
        fTimeRangeHistogram.dispose();
        super.dispose();
    }

    // ------------------------------------------------------------------------
    // TmfView
    // ------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public void createPartControl(Composite parent) {

        fParent = parent;

        // Control labels
        final String currentEventLabel = Messages.HistogramView_currentEventLabel;
        final String windowSpanLabel = Messages.HistogramView_windowSpanLabel;

        // --------------------------------------------------------------------
        // Set the HistogramView layout
        // --------------------------------------------------------------------

        Composite viewComposite = new Composite(fParent, SWT.FILL);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridLayout.horizontalSpacing = 5;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        viewComposite.setLayout(gridLayout);

        // Use all available space
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        viewComposite.setLayoutData(gridData);

        // --------------------------------------------------------------------
        // Time controls
        // --------------------------------------------------------------------

        Composite controlsComposite = new Composite(viewComposite, SWT.FILL);
        gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 5;
        gridLayout.verticalSpacing = 0;
        gridLayout.makeColumnsEqualWidth = true;
        gridLayout.marginLeft = 5;
        gridLayout.marginRight = 5;
        controlsComposite.setLayout(gridLayout);

        // Current event time control
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.CENTER;
        gridData.verticalAlignment = SWT.CENTER;
        fCurrentEventTimeControl = new HistogramCurrentTimeControl(this, controlsComposite, SWT.BORDER, SWT.BORDER,
                currentEventLabel, HistogramUtils.nanosecondsToString(0L));
        fCurrentEventTimeControl.setLayoutData(gridData);

        // Window span time control
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.CENTER;
        gridData.verticalAlignment = SWT.CENTER;
        fTimeSpanControl = new HistogramTimeRangeControl(this, controlsComposite, SWT.BORDER, SWT.BORDER,
                windowSpanLabel, HistogramUtils.nanosecondsToString(0L));
        fTimeSpanControl.setLayoutData(gridData);

        // --------------------------------------------------------------------
        // Time range histogram
        // --------------------------------------------------------------------

        Composite timeRangeComposite = new Composite(viewComposite, SWT.FILL);
        gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginTop = 5;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginLeft = 5;
        gridLayout.marginRight = 5;
        timeRangeComposite.setLayout(gridLayout);

        // Use remaining horizontal space
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        timeRangeComposite.setLayoutData(gridData);

        // Histogram
        fTimeRangeHistogram = new TimeRangeHistogram(this, timeRangeComposite);

        // --------------------------------------------------------------------
        // Full range histogram
        // --------------------------------------------------------------------

        Composite fullRangeComposite = new Composite(viewComposite, SWT.FILL);
        gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginTop = 5;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginLeft = 5;
        gridLayout.marginRight = 5;
        fullRangeComposite.setLayout(gridLayout);

        // Use remaining horizontal space
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = SWT.FILL;
        gridData.horizontalSpan = 2;
        gridData.grabExcessHorizontalSpace = true;
        fullRangeComposite.setLayoutData(gridData);

        // Histogram
        fFullTraceHistogram = new FullTraceHistogram(this, fullRangeComposite);

        // Load the experiment if present
        fCurrentExperiment = (TmfExperiment<LttngEvent>) TmfExperiment.getCurrentExperiment();
        if (fCurrentExperiment != null)
            loadExperiment();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setFocus() {
        TmfExperiment<LttngEvent> experiment = (TmfExperiment<LttngEvent>) TmfExperiment.getCurrentExperiment();
        if ((experiment != null) && (experiment != fCurrentExperiment)) {
            fCurrentExperiment = experiment;
            initializeHistograms();
        }
        fParent.redraw();
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    public TmfTimeRange getTimeRange() {
        return new TmfTimeRange(new TmfTimestamp(fWindowStartTime, TIME_SCALE), new TmfTimestamp(fWindowEndTime,
                TIME_SCALE));
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    public void updateCurrentEventTime(long newTime) {
        if (fCurrentExperiment != null) {
            TmfTimeRange timeRange = new TmfTimeRange(new TmfTimestamp(newTime, TIME_SCALE), TmfTimestamp.BigCrunch);
            HistogramRequest request = new HistogramRequest(fTimeRangeHistogram, timeRange, 0, 1,
                    ExecutionType.FOREGROUND) {
                @Override
                public void handleData(LttngEvent event) {
                    if (event != null) {
                        TmfTimeSynchSignal signal = new TmfTimeSynchSignal(this, event.getTimestamp());
                        TmfSignalManager.dispatchSignal(signal);
                    }
                }
            };
            fCurrentExperiment.sendRequest(request);
        }
    }

    public void updateTimeRange(long startTime, long endTime) {
        if (fCurrentExperiment != null) {
            // Build the new time range; keep the current time
            TmfTimeRange timeRange = new TmfTimeRange(new TmfTimestamp(startTime, TIME_SCALE), new TmfTimestamp(
                    endTime, TIME_SCALE));
            TmfTimestamp currentTime = new TmfTimestamp(fCurrentTimestamp, TIME_SCALE);

            fTimeSpanControl.setValue(endTime - startTime);

            // Send the FW signal
            TmfRangeSynchSignal signal = new TmfRangeSynchSignal(this, timeRange, currentTime);
            TmfSignalManager.dispatchSignal(signal);
        }
    }

    public synchronized void updateTimeRange(long newDuration) {
        if (fCurrentExperiment != null) {
            long delta = newDuration - fWindowSpan;
            long newStartTime = fWindowStartTime + delta / 2;
            setNewRange(newStartTime, newDuration);
        }
    }

    private void setNewRange(long startTime, long duration) {
        if (startTime < fExperimentStartTime)
            startTime = fExperimentStartTime;

        long endTime = startTime + duration;
        if (endTime > fExperimentEndTime) {
            endTime = fExperimentEndTime;
            if (endTime - duration > fExperimentStartTime)
                startTime = endTime - duration;
            else {
                startTime = fExperimentStartTime;
            }
        }
        updateTimeRange(startTime, endTime);
    }

    // ------------------------------------------------------------------------
    // Signal handlers
    // ------------------------------------------------------------------------

    @TmfSignalHandler
    @SuppressWarnings("unchecked")
    public void experimentSelected(TmfExperimentSelectedSignal<LttngEvent> signal) {
        assert (signal != null);
        fCurrentExperiment = (TmfExperiment<LttngEvent>) signal.getExperiment();
        loadExperiment();
    }

    private void loadExperiment() {
        initializeHistograms();
        fParent.redraw();
    }

    @TmfSignalHandler
    @SuppressWarnings("unchecked")
    public void experimentRangeUpdated(TmfExperimentRangeUpdatedSignal signal) {

        fCurrentExperiment = (TmfExperiment<LttngEvent>) signal.getExperiment();
        boolean drawTimeRangeHistogram = fExperimentStartTime == 0;
        TmfTimeRange fullRange = signal.getRange();

        fExperimentStartTime = fullRange.getStartTime().getValue();
        fExperimentEndTime = fullRange.getEndTime().getValue();

        fFullTraceHistogram.setFullRange(fExperimentStartTime, fExperimentEndTime);
        fTimeRangeHistogram.setFullRange(fExperimentStartTime, fExperimentEndTime);

        if (drawTimeRangeHistogram) {
            fCurrentTimestamp = fExperimentStartTime;
            fCurrentEventTimeControl.setValue(fCurrentTimestamp);
            fFullTraceHistogram.setTimeRange(fExperimentStartTime, INITIAL_WINDOW_SPAN);
            fTimeRangeHistogram.setTimeRange(fExperimentStartTime, INITIAL_WINDOW_SPAN);
            sendTimeRangeRequest(fExperimentStartTime, fExperimentStartTime + INITIAL_WINDOW_SPAN);
        }

        sendFullRangeRequest(fullRange);
    }

    @TmfSignalHandler
    public void currentTimeUpdated(TmfTimeSynchSignal signal) {
        // Because this can't happen :-)
        assert (signal != null);

        // Update the selected event time
        TmfTimestamp currentTime = signal.getCurrentTime();
        fCurrentTimestamp = currentTime.getValue();

        // Notify the relevant widgets
        fFullTraceHistogram.setCurrentEvent(fCurrentTimestamp);
        fTimeRangeHistogram.setCurrentEvent(fCurrentTimestamp);
        fCurrentEventTimeControl.setValue(fCurrentTimestamp);
    }

    @TmfSignalHandler
    public void timeRangeUpdated(TmfRangeSynchSignal signal) {
        // Because this can't happen :-)
        assert (signal != null);

        if (fCurrentExperiment != null) {
            // Update the time range
            fWindowStartTime = signal.getCurrentRange().getStartTime().getValue();
            fWindowEndTime = signal.getCurrentRange().getEndTime().getValue();
            fWindowSpan = fWindowEndTime - fWindowStartTime;

            // Notify the relevant widgets
            sendTimeRangeRequest(fWindowStartTime, fWindowEndTime);
            fFullTraceHistogram.setTimeRange(fWindowStartTime, fWindowSpan);
            fTimeSpanControl.setValue(fWindowSpan);
        }
    }

    // ------------------------------------------------------------------------
    // Helper functions
    // ------------------------------------------------------------------------

    private void initializeHistograms() {
        TmfTimeRange fullRange = updateExperimentTimeRange(fCurrentExperiment);

        fTimeRangeHistogram.clear();
        fTimeRangeHistogram.setFullRange(fExperimentStartTime, fExperimentEndTime);
        fTimeRangeHistogram.setTimeRange(fExperimentStartTime, INITIAL_WINDOW_SPAN);
        fTimeRangeHistogram.setCurrentEvent(fExperimentStartTime);

        fFullTraceHistogram.clear();
        fFullTraceHistogram.setFullRange(fExperimentStartTime, fExperimentEndTime);
        fFullTraceHistogram.setTimeRange(fExperimentStartTime, INITIAL_WINDOW_SPAN);
        fFullTraceHistogram.setCurrentEvent(fExperimentStartTime);

        fWindowStartTime = fExperimentStartTime;
        fWindowSpan = INITIAL_WINDOW_SPAN;
        fWindowEndTime = fWindowStartTime + fWindowSpan;

        fCurrentEventTimeControl.setValue(fExperimentStartTime);
        fTimeSpanControl.setValue(fWindowSpan);

        sendTimeRangeRequest(fExperimentStartTime, fExperimentStartTime + fWindowSpan);
        sendFullRangeRequest(fullRange);
    }

    private TmfTimeRange updateExperimentTimeRange(TmfExperiment<LttngEvent> experiment) {
        fExperimentStartTime = 0;
        fExperimentEndTime = 0;
        fCurrentTimestamp = 0;

        TmfTimeRange timeRange = fCurrentExperiment.getTimeRange();
        if (timeRange != TmfTimeRange.Null) {
            fExperimentStartTime = timeRange.getStartTime().getValue();
            fExperimentEndTime = timeRange.getEndTime().getValue();
            fCurrentTimestamp = fExperimentStartTime;
        }
        return timeRange;
    }

    private void sendTimeRangeRequest(long startTime, long endTime) {
        if (fTimeRangeRequest != null && !fTimeRangeRequest.isCompleted()) {
            fTimeRangeRequest.cancel();
        }
        TmfTimestamp startTS = new TmfTimestamp(startTime, TIME_SCALE);
        TmfTimestamp endTS = new TmfTimestamp(endTime, TIME_SCALE);
        TmfTimeRange timeRange = new TmfTimeRange(startTS, endTS);

        fTimeRangeHistogram.clear();
        fTimeRangeHistogram.setTimeRange(startTime, endTime - startTime);
        fTimeRangeRequest = new HistogramRequest(fTimeRangeHistogram, timeRange, ExecutionType.FOREGROUND);
        fCurrentExperiment.sendRequest(fTimeRangeRequest);
    }

    private void sendFullRangeRequest(TmfTimeRange fullRange) {
        if (fFullTraceRequest != null && !fFullTraceRequest.isCompleted()) {
            fFullTraceRequest.cancel();
        }
        fFullTraceRequest = new HistogramRequest(fFullTraceHistogram, fullRange,
                (int) fFullTraceHistogram.fDataModel.getNbEvents(), ExecutionType.BACKGROUND);
        fCurrentExperiment.sendRequest(fFullTraceRequest);
    }

}
