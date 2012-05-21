package org.eclipse.linuxtools.internal.oprofile.remote.launch.launching;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.linuxtools.internal.oprofile.launch.launching.OprofileLaunchConfigurationDelegate;
import org.eclipse.ptp.core.IPTPLaunchConfigurationConstants;


public class OprofileRemoteLaunchConfigurationDelegate extends OprofileLaunchConfigurationDelegate {

	@Override
	protected IProject getProject() {
		String name = null;
		try {
			name = config.getAttribute(IPTPLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		} catch (CoreException e) {
			return null;
		}
		if (name == null) {
			return null;
		}

		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}
	
	@Override
	protected IPath getExePath(ILaunchConfiguration config)
			throws CoreException {
		String pathString = config.getAttribute(IPTPLaunchConfigurationConstants.ATTR_EXECUTABLE_PATH, (String) null);
		
		IPath path = new Path(pathString);
		
		return path;
	}

}
