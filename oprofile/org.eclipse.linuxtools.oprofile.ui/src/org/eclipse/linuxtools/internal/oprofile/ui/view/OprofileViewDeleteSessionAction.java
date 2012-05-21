package org.eclipse.linuxtools.internal.oprofile.ui.view;

import java.io.FileNotFoundException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.linuxtools.internal.oprofile.core.OpcontrolException;
import org.eclipse.linuxtools.internal.oprofile.core.OprofileCorePlugin;
import org.eclipse.linuxtools.internal.oprofile.ui.OprofileUiPlugin;
import org.eclipse.linuxtools.internal.oprofile.ui.model.UiModelSession;
import org.eclipse.linuxtools.internal.oprofile.core.Oprofile;
import org.eclipse.linuxtools.internal.oprofile.core.opxml.sessions.SessionManager;
import org.eclipse.linuxtools.profiling.launch.IRemoteFileProxy;
import org.eclipse.linuxtools.profiling.launch.RemoteProxyManager;

public class OprofileViewDeleteSessionAction extends Action {
	
	private TreeViewer treeViewer;

	private IRemoteFileProxy proxy;
	
	public OprofileViewDeleteSessionAction(TreeViewer tree) {
		super("Delete Session"); //$NON-NLS-1$
		treeViewer = tree;
	}

	@Override
	public void run() {
		TreeSelection tsl = (TreeSelection) treeViewer.getSelection();
		if (tsl.getFirstElement() instanceof UiModelSession) {
			UiModelSession sess = (UiModelSession) tsl.getFirstElement();
			deleteSession(sess);
		}

		OprofileUiPlugin.getDefault().getOprofileView().refreshView();
	}

	/**
	 * Delete the session with the specified name for the specified event
	 * @param sessionName The name of the session to delete
	 * @param eventName The name of the event containing the session
	 */
	private void deleteSession(UiModelSession sess) {
		String sessionName = sess.getLabelText();
		String eventName = sess.getParent().getLabelText();
		
		try {
			proxy = RemoteProxyManager.getInstance().getFileProxy(Oprofile.OprofileProject.getProject());
			IFileStore fileStore = proxy.getResource(SessionManager.OPXML_PREFIX + SessionManager.MODEL_DATA + eventName + sessionName);
			fileStore.delete(EFS.NONE, new NullProgressMonitor());
			
			SessionManager sessMan = new SessionManager(SessionManager.SESSION_LOCATION);
			sessMan.removeSession(sessionName, eventName);
			sessMan.write();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
