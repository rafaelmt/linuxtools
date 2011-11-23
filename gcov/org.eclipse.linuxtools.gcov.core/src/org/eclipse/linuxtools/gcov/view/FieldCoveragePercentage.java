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
package org.eclipse.linuxtools.gcov.view;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.eclipse.linuxtools.dataviewers.abstractviewers.AbstractPercentageDrawerField;
import org.eclipse.linuxtools.dataviewers.charts.provider.IChartField;
import org.eclipse.linuxtools.gcov.model.CovRootTreeElement;
import org.eclipse.linuxtools.gcov.model.TreeElement;



public class FieldCoveragePercentage extends AbstractPercentageDrawerField implements IChartField {

	public final static NumberFormat nf = new DecimalFormat("##0.0#");


	public String getColumnHeaderText() {
		return "Coverage %";
	}

	@Override
	public String getValue(Object obj) {
		float f = getPercentage(obj);
		if (f < 0)
			f = 0.0f;
		return nf.format(f);
	}


	/**
	 * Gets the percentage value to display
	 * @param obj
	 * @return the percentage value to display, as a float
	 */
	public float getPercentage(Object obj) {
		TreeElement e = (TreeElement) obj;
		return e.getCoveragePercentage();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.linuxtools.dataviewers.abstractviewers.AbstractPercentageDrawerField#getNumberFormat()
	 */
	@Override
	public NumberFormat getNumberFormat() {
		return nf;
	}

	@Override
	public boolean isSettedNumberFormat() {
		return true;
	}

	public String getToolTipText(Object element) {
		TreeElement e = (TreeElement) element;
		String s =" Coverage % = "+Integer.toString((int)e.getCoveragePercentage());
		return s ;			
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.linuxtools.dataviewers.abstractviewers.ISTDataViewersField#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object obj1, Object obj2) {
		TreeElement e1 = (TreeElement) obj1;
		TreeElement e2 = (TreeElement) obj2;
		float f1 = e1.getCoveragePercentage();
		float f2 = e2.getCoveragePercentage();
		return Float.compare(f1, f2);
	}


	/*
	 * (non-Javadoc)
	 * @see org.eclipse.linuxtools.dataviewers.charts.provider.IChartField#getNumber(java.lang.Object)
	 */
	public Number getNumber(Object obj) {
		TreeElement e = (TreeElement) obj;
		float f = getPercentage(obj);
		if (e.getClass() == CovRootTreeElement.class)
			return 0;
		else {
			if (f < 0)
				f = 0.0f;
			return f;
		}
	}
}
