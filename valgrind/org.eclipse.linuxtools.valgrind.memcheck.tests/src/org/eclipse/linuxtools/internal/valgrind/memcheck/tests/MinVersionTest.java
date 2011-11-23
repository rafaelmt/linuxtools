/*******************************************************************************
 * Copyright (c) 2009 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Elliott Baron <ebaron@redhat.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.internal.valgrind.memcheck.tests;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.linuxtools.internal.valgrind.launch.ValgrindLaunchPlugin;
import org.eclipse.linuxtools.internal.valgrind.launch.ValgrindOptionsTab;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Version;

public class MinVersionTest extends AbstractMemcheckTest {
	private static final Version VER_3_2_1 = new Version(3, 2, 1);
	private Version verSave;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		proj = createProjectAndBuild("basicTest"); //$NON-NLS-1$
		
		saveVersion();
	}

	private void saveVersion() throws CoreException {
	//	verSave = ValgrindLaunchPlugin.getDefault().getValgrindVersion();
	//	ValgrindLaunchPlugin.getDefault().setValgrindVersion(VER_3_2_1);
	//	TODO: Fix test
	}
	
	@Override
	protected void tearDown() throws Exception {
		restoreVersion();
		
		deleteProject(proj);
		super.tearDown();
	}

	private void restoreVersion() {
	//	ValgrindLaunchPlugin.getDefault().setValgrindVersion(verSave);
	//	TODO: Fix test
	}
		
	public void testLaunchBadVersion() throws Exception {
		// Put this back so we can make a valid config
		restoreVersion();
		ILaunchConfiguration config = createConfiguration(proj.getProject());
		// For some reason we downgraded
		saveVersion();
		
		CoreException ce = null;		
		try {
			doLaunch(config, "testDefaults"); //$NON-NLS-1$
		} catch (CoreException e) {
			ce = e;
		}
		
		assertNotNull(ce);
	}
	
	public void testTabsBadVersion() throws Exception {
		Shell testShell = new Shell(Display.getDefault());
		testShell.setLayout(new GridLayout());
		ValgrindOptionsTab tab = new ValgrindOptionsTab();
		
		ILaunchConfiguration config = getLaunchConfigType().newInstance(null, getLaunchManager()
				.generateLaunchConfigurationName(
						proj.getProject().getName()));
		ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
		tab.setDefaults(wc);
		tab.createControl(testShell);
		tab.initializeFrom(config);
		tab.performApply(wc);
		
		assertFalse(tab.isValid(config));
		assertNotNull(tab.getErrorMessage());
		
		testShell.dispose();
	}

}
