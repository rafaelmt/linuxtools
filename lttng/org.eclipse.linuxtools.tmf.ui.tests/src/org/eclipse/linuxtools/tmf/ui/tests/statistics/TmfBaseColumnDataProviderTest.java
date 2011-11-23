/*******************************************************************************
 * Copyright (c) 2011 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Mathieu Denis (mathieu.denis@polymtl.ca) - Initial API and Implementation
 *   Bernd Hufmann - Fixed header and warnings
 *******************************************************************************/
package org.eclipse.linuxtools.tmf.ui.tests.statistics;

import java.util.Vector;

import junit.framework.TestCase;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.linuxtools.tmf.event.TmfEvent;
import org.eclipse.linuxtools.tmf.event.TmfEventContent;
import org.eclipse.linuxtools.tmf.event.TmfEventReference;
import org.eclipse.linuxtools.tmf.event.TmfEventSource;
import org.eclipse.linuxtools.tmf.event.TmfEventType;
import org.eclipse.linuxtools.tmf.event.TmfTimestamp;
import org.eclipse.linuxtools.tmf.ui.views.statistics.ITmfExtraEventInfo;
import org.eclipse.linuxtools.tmf.ui.views.statistics.model.Messages;
import org.eclipse.linuxtools.tmf.ui.views.statistics.model.TmfBaseColumnData;
import org.eclipse.linuxtools.tmf.ui.views.statistics.model.TmfBaseColumnData.ITmfColumnPercentageProvider;
import org.eclipse.linuxtools.tmf.ui.views.statistics.model.TmfBaseColumnDataProvider;
import org.eclipse.linuxtools.tmf.ui.views.statistics.model.TmfBaseStatisticsTree;
import org.eclipse.linuxtools.tmf.ui.views.statistics.model.TmfStatisticsTreeNode;
import org.eclipse.linuxtools.tmf.util.TmfFixedArray;
@SuppressWarnings("nls")
public class TmfBaseColumnDataProviderTest extends TestCase {

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------
    private final static String LEVEL_COLUMN = org.eclipse.linuxtools.tmf.ui.views.statistics.Messages.TmfStatisticsView_LevelColumn;
    private final static String EVENTS_COUNT_COLUMN = org.eclipse.linuxtools.tmf.ui.views.statistics.Messages.TmfStatisticsView_NbEventsColumn;
    
    private TmfBaseColumnDataProvider provider;

    private String fTestName;

    private final String fTypeId1 = "Some type1";
    private final String fTypeId2 = "Some type2";

    private final String fLabel0 = "label1";
    private final String fLabel1 = "label2";
    private final String fLabel2 = "label3";
    private final String[] fLabels = new String[] { fLabel0, fLabel1, fLabel2 };

    private final TmfTimestamp fTimestamp1 = new TmfTimestamp(12345, (byte) 2, 5);
    private final TmfTimestamp fTimestamp2 = new TmfTimestamp(12350, (byte) 2, 5);
    private final TmfTimestamp fTimestamp3 = new TmfTimestamp(12355, (byte) 2, 5);

    private final TmfEventSource fSource = new TmfEventSource("Source");

    private final TmfEventType fType1 = new TmfEventType(fTypeId1, fLabels);
    private final TmfEventType fType2 = new TmfEventType(fTypeId1, fLabels);
    private final TmfEventType fType3 = new TmfEventType(fTypeId2, fLabels);

    private final TmfEventReference fReference = new TmfEventReference("Some reference");

    private final TmfEvent fEvent1;
    private final TmfEvent fEvent2;
    private final TmfEvent fEvent3;

    private final TmfEventContent fContent1;
    private final TmfEventContent fContent2;
    private final TmfEventContent fContent3;

    private final TmfBaseStatisticsTree fStatsData;

    private final ITmfExtraEventInfo fExtraInfo;

    // ------------------------------------------------------------------------
    // Housekeeping
    // ------------------------------------------------------------------------

    public TmfBaseColumnDataProviderTest(final String name) {
        super(name);

        fTestName = name;

        fEvent1 = new TmfEvent(fTimestamp1, fSource, fType1, fReference);
        fContent1 = new TmfEventContent(fEvent1, "Some content");
        fEvent1.setContent(fContent1);

        fEvent2 = new TmfEvent(fTimestamp1, fTimestamp2, fSource, fType2, fReference);
        fContent2 = new TmfEventContent(fEvent2, "Some other content");
        fEvent2.setContent(fContent2);

        fEvent3 = new TmfEvent(fTimestamp2, fTimestamp3, fSource, fType3, fReference);
        fContent3 = new TmfEventContent(fEvent3, "Some other different content");
        fEvent3.setContent(fContent3);

        fStatsData = new TmfBaseStatisticsTree();
        fExtraInfo = new ITmfExtraEventInfo() {
            @Override
            public String getTraceName() {
                return name;
            }
        };
        fStatsData.getOrCreate(new TmfFixedArray<String>(fTestName, Messages.TmfStatisticsData_EventTypes));
        fStatsData.registerEvent(fEvent1, fExtraInfo);
        fStatsData.registerEvent(fEvent2, fExtraInfo);
        fStatsData.registerEvent(fEvent3, fExtraInfo);

        provider = new TmfBaseColumnDataProvider();
    }

    // ------------------------------------------------------------------------
    // Get Column Data
    // ------------------------------------------------------------------------

    public void testGetColumnData() {
        Vector<TmfBaseColumnData> columnsData = provider.getColumnData();
        assertNotNull("getColumnData", columnsData);
        assertEquals("getColumnData", 2, columnsData.size());

        TmfStatisticsTreeNode parentNode = fStatsData.get(new TmfFixedArray<String>(fTestName));
        TmfStatisticsTreeNode treeNode1  = fStatsData.get(new TmfFixedArray<String>(fTestName, Messages.TmfStatisticsData_EventTypes, fEvent1.getType().toString()));
        TmfStatisticsTreeNode treeNode2  = fStatsData.get(new TmfFixedArray<String>(fTestName, Messages.TmfStatisticsData_EventTypes, fEvent3.getType().toString()));
        ViewerComparator vComp = null;
        for (TmfBaseColumnData columnData : columnsData) {
            assertNotNull("getColumnData", columnData);
            assertNotNull("getColumnData", columnData.getHeader());
            assertNotNull("getColumnData", columnData.getTooltip());
            
            // Testing labelProvider
            ColumnLabelProvider labelProvider = columnData.getLabelProvider();
            if (columnData.getHeader().compareTo(LEVEL_COLUMN) == 0) {
                assertEquals("getColumnData", 0, labelProvider.getText(treeNode1).compareTo(treeNode1.getKey()));
            } else if (columnData.getHeader().compareTo(EVENTS_COUNT_COLUMN) == 0) {
                assertEquals("getColumnData", 0, labelProvider.getText(treeNode1).compareTo(Long.toString(2)));
            }

            // Testing comparator
            vComp = columnData.getComparator();
            if (columnData.getHeader().compareTo(LEVEL_COLUMN) == 0) {
                assertTrue("getColumnData", vComp.compare(null, treeNode1, treeNode2) < 0);
                assertTrue("getColumnData", vComp.compare(null, treeNode2, treeNode1) > 0);
                assertTrue("getColumnData", vComp.compare(null, treeNode1, treeNode1) == 0);
            } else if (columnData.getHeader().compareTo(EVENTS_COUNT_COLUMN) == 0) {
                assertTrue("getColumnData", vComp.compare(null, treeNode1, treeNode2) > 0);
                assertTrue("getColumnData", vComp.compare(null, treeNode2, treeNode1) < 0);
                assertTrue("getColumnData", vComp.compare(null, treeNode1, treeNode1) == 0);
            }

            // Testing percentage provider
            ITmfColumnPercentageProvider percentProvider = columnData.getPercentageProvider();
            if (columnData.getHeader().compareTo(LEVEL_COLUMN) == 0) {
                assertNull("getColumnData", percentProvider);
            } else if (columnData.getHeader().compareTo(EVENTS_COUNT_COLUMN) == 0) {
                double percentage = (double) treeNode1.getValue().nbEvents / parentNode.getValue().nbEvents;
                assertEquals("getColumnData", (double) percentage, (double) percentProvider.getPercentage(treeNode1));
            }
        }
    }
}