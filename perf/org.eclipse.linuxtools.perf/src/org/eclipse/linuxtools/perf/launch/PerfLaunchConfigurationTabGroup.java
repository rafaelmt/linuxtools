/*******************************************************************************
 * Copyright (c) 2008 Red Hat, Inc.
 * (C) Copyright 2010 IBM Corp. 2010
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Kent Sebastian <ksebasti@redhat.com> - initial API and implementation
 *    Thavidu Ranatunga (IBM) - derived from
 *        org.eclipse.linuxtools.oprofile.launch.configuration.OprofileLaunchConfigurationTabGroup
 *******************************************************************************/ 
package org.eclipse.linuxtools.perf.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.linuxtools.profiling.launch.ProfileLaunchConfigurationTabGroup;

public class PerfLaunchConfigurationTabGroup extends ProfileLaunchConfigurationTabGroup {
	@Override
	public AbstractLaunchConfigurationTab[] getProfileTabs() {
		return new AbstractLaunchConfigurationTab[] { new PerfOptionsTab(), new PerfEventsTab() };
	}
}
