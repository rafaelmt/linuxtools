package org.eclipse.linuxtools.internal.oprofile.remote.core.linux;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.linuxtools.internal.oprofile.core.OpcontrolException;
import org.eclipse.linuxtools.internal.oprofile.core.OprofileCorePlugin;
import org.eclipse.linuxtools.internal.oprofile.core.linux.LinuxOpcontrolProvider;
import org.eclipse.linuxtools.tools.launch.core.factory.RuntimeProcessFactory;

/**
 * A class which encapsulates running opcontrol.
 */
public class RemoteLinuxOpcontrolProvider extends LinuxOpcontrolProvider {

	private static final String OPCONTROL_EXECUTABLE = "opcontrol";

	public RemoteLinuxOpcontrolProvider() throws OpcontrolException {
	}
	
	
	protected Process createOpcontrolProcess(String[] cmdArray, IProject project) throws OpcontrolException {
		Process p = null;
		try {
			p = RuntimeProcessFactory.getFactory().sudoExec(cmdArray, project);
		} catch (IOException ioe) {			
			throw new OpcontrolException(OprofileCorePlugin.createErrorStatus("opcontrolRun", ioe)); //$NON-NLS-1$
		}
		
		return p;
	}
	
	protected static String findOpcontrol(){
		return OPCONTROL_EXECUTABLE;
	}
}
