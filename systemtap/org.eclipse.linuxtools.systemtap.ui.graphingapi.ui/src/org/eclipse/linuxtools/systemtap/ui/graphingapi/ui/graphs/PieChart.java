/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Jeff Briggs, Henry Hughes, Ryan Morse
 *******************************************************************************/

package org.eclipse.linuxtools.systemtap.ui.graphingapi.ui.graphs;

import org.eclipse.linuxtools.systemtap.ui.graphingapi.nonui.IGraphColorConstants;
import org.eclipse.linuxtools.systemtap.ui.graphingapi.nonui.adapters.BlockAdapter;
import org.eclipse.linuxtools.systemtap.ui.graphingapi.nonui.structures.NumberType;
import org.eclipse.linuxtools.systemtap.ui.graphingapi.ui.widgets.GraphComposite;
import org.eclipse.linuxtools.systemtap.ui.graphingapi.ui.widgets.GraphLegend;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;



public class PieChart extends AChart implements IBlockGraph {
	public PieChart(GraphComposite parent, int style, String title, BlockAdapter adapt) {
		super(parent, style, title, adapt);
		adapter = adapt;
		handleUpdateEvent();
	}

	private void updateLegend() {
		Object [][] l = adapter.getData();
		String [] labels = new String[adapter.getRecordCount()];
		Color[] colors = new Color[labels.length];
		
		for(int i=0; i<labels.length; i++) {
			labels[i] = l[i][0].toString();
			colors[i] = new Color(this.getDisplay(), IGraphColorConstants.COLORS[i]);
		}

		legend = new GraphLegend(this, labels, colors);
	}
	
	@SuppressWarnings("unchecked")
	public void paintElementList(GC gc) {
		updateLegend(); //Make sure legend has current keys
		Color temp = gc.getForeground();
		Color temp1 = gc.getBackground();
		Color c;

		int pw = Math.min(super.getSize().x - (super.getXPadding()<<1), super.getSize().y - (super.getYPadding()<<1));
		int px = (super.getSize().x - super.getXPadding() - pw)>>1;
		int py = (super.getSize().y - super.getYPadding() - pw)>>1;
		
		int angle1, angle0 = 0;
		Number[] points = new Number[0];
		points = (Number[])elementList[0].toArray(points);
		for(int i=0; i<points.length; i++) {
			c = new Color(getDisplay(), IGraphColorConstants.COLORS[i]);
			gc.setForeground(c);
			gc.setBackground(c);
			angle1 = (int)((MAX_ANGLE*(points[i].doubleValue()/sum))+0.51);
			gc.fillArc(px, py, pw, pw, angle0, angle1);
			angle0 += angle1;
		}
		
		//If there is no data to display draw a crossed out circle
		if(0 == points.length) {
			gc.drawArc(px, py, pw, pw, 0, 360);
			gc.drawLine(px, py, px+pw, py+pw);
			gc.drawLine(px, py+pw, px+pw, py);
		}
		
		gc.setForeground(temp);
		gc.setBackground(temp1);
	}
	
	public boolean isMultiGraph() {
		return false;
	}
	
	public void handleUpdateEvent() {
		if(null == adapter) return;

		this.getDisplay().syncExec(new Runnable() {
			@SuppressWarnings("unchecked")
			public void run() {
				Object[][] data;
				elementList[0].clear();
				data = adapter.getData();
	
				Number n;
				sum = 0;
				for(int i=0; i<data.length; i++) {
					n = NumberType.obj2num(data[i][1]);
					elementList[0].add(n);
					sum += n.doubleValue();
				}
			}
		});
		this.repaint();
	}
	
	public static final String ID = "org.eclipse.linuxtools.systemtap.ui.graphingapi.ui.graphs.piechart";
	private BlockAdapter adapter;
	private double sum;
	private static final int MAX_ANGLE = 360;
}
