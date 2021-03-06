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
package org.eclipse.linuxtools.internal.valgrind.massif.tests;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.linuxtools.internal.valgrind.core.LaunchConfigurationConstants;
import org.eclipse.linuxtools.internal.valgrind.massif.MassifOutput;
import org.eclipse.linuxtools.internal.valgrind.massif.MassifPidMenuAction;
import org.eclipse.linuxtools.internal.valgrind.massif.MassifSnapshot;
import org.eclipse.linuxtools.internal.valgrind.massif.MassifViewPart;
import org.eclipse.linuxtools.internal.valgrind.ui.ValgrindUIPlugin;
import org.eclipse.linuxtools.internal.valgrind.ui.ValgrindViewPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

public class MultiProcessTest extends AbstractMassifTest {
	ICProject refProj;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		refProj = createProjectAndBuild("alloctest"); //$NON-NLS-1$
		proj = createProjectAndBuild("multiProcTest"); //$NON-NLS-1$
	}
	
	@Override
	protected void tearDown() throws Exception {
		deleteProject(proj);
		deleteProject(refProj);
		super.tearDown();
	}
	
	public void testNoExec() throws Exception {
		ILaunchConfiguration config = createConfiguration(proj.getProject());
		doLaunch(config, "testNoExec"); //$NON-NLS-1$
		
		MassifViewPart view = (MassifViewPart) ValgrindUIPlugin.getDefault().getView().getDynamicView();
		MassifOutput output = view.getOutput();
		assertEquals(1, output.getPids().length);
		assertEquals(8, view.getSnapshots().length);
	}
	
	public void testExec() throws Exception {
		ILaunchConfigurationWorkingCopy config = createConfiguration(proj.getProject()).getWorkingCopy();
		config.setAttribute(LaunchConfigurationConstants.ATTR_GENERAL_TRACECHILD, true);
		config.doSave();
		doLaunch(config, "testExec"); //$NON-NLS-1$
		
		MassifViewPart view = (MassifViewPart) ValgrindUIPlugin.getDefault().getView().getDynamicView();
		MassifOutput output = view.getOutput();
		
		Integer[] pids = output.getPids();
		assertEquals(2, pids.length);
		
		// child not necessarily higher PID than parent
		MassifSnapshot[] snapshots1 = output.getSnapshots(pids[0]);
		assertTrue(snapshots1.length == 8 || snapshots1.length == 14);
		MassifSnapshot[] snapshots2 = output.getSnapshots(pids[1]);
		assertTrue(snapshots2.length == 8 || snapshots2.length == 14);
		assertTrue(snapshots1.length != snapshots2.length);
	}
	
	public void testExecPidMenu() throws Exception {
		ILaunchConfigurationWorkingCopy config = createConfiguration(proj.getProject()).getWorkingCopy();
		config.setAttribute(LaunchConfigurationConstants.ATTR_GENERAL_TRACECHILD, true);
		config.doSave();
		doLaunch(config, "testExec"); //$NON-NLS-1$
		
		ValgrindViewPart view = ValgrindUIPlugin.getDefault().getView();
		MassifViewPart dynamicView = (MassifViewPart) view.getDynamicView();
		MassifOutput output = dynamicView.getOutput();
		
		MassifPidMenuAction menuAction = null;
		IToolBarManager manager = view.getViewSite().getActionBars().getToolBarManager();
		for (IContributionItem item : manager.getItems()) {
			if (item instanceof ActionContributionItem
					&& ((ActionContributionItem) item).getAction() instanceof MassifPidMenuAction) {
				menuAction = (MassifPidMenuAction) ((ActionContributionItem) item).getAction();
			}
		}
		
		assertNotNull(menuAction);
		
		Integer[] pids = dynamicView.getOutput().getPids();
		Shell shell = new Shell(Display.getCurrent());
		Menu pidMenu = menuAction.getMenu(shell);
			
		assertEquals(2, pidMenu.getItemCount());
		ActionContributionItem firstPid = (ActionContributionItem) pidMenu.getItem(0).getData();
		ActionContributionItem secondPid = (ActionContributionItem) pidMenu.getItem(1).getData();
		
		// check initial state
		assertTrue(firstPid.getAction().isChecked());
		assertFalse(secondPid.getAction().isChecked());
		assertEquals(output.getSnapshots(pids[0]), dynamicView.getSnapshots());
		
		// select second pid
		selectItem(pidMenu, 1);
		
		assertFalse(firstPid.getAction().isChecked());
		assertTrue(secondPid.getAction().isChecked());
		assertEquals(output.getSnapshots(pids[1]), dynamicView.getSnapshots());
		
		// select second pid again
		selectItem(pidMenu, 1);
		
		assertFalse(firstPid.getAction().isChecked());
		assertTrue(secondPid.getAction().isChecked());
		assertEquals(output.getSnapshots(pids[1]), dynamicView.getSnapshots());
		
		// select first pid
		selectItem(pidMenu, 0);
		
		assertTrue(firstPid.getAction().isChecked());
		assertFalse(secondPid.getAction().isChecked());
		assertEquals(output.getSnapshots(pids[0]), dynamicView.getSnapshots());
	}

	protected void selectItem(Menu pidMenu, int index) {
		pidMenu.getItem(index).notifyListeners(SWT.Selection, null);
	}
}
