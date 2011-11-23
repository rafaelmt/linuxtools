/*******************************************************************************
 * Copyright (c) 2009, 2010 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.signal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.linuxtools.tmf.Tracer;

/**
 * <b><u>TmfSignalHandler</u></b>
 * <p>
 * This class manages the set of signal listeners and the signals they are
 * interested in. When a signal is broadcasted, the appropriate listeners
 * signal handlers are invoked.
 * <p>
 */
public class TmfSignalManager {

	// The set of event listeners and their corresponding handler methods.
	// Note: listeners could be restricted to ITmfComponents but there is no
	// harm in letting anyone use this since it is not tied to anything but
	// the signal data type.
	static private Map<Object, Method[]> fListeners = new HashMap<Object, Method[]>();
    static private Map<Object, Method[]> fVIPListeners = new HashMap<Object, Method[]>();

	// If requested, add universal signal tracer
	// TODO: Temporary solution: should be enabled/disabled dynamically 
	private static boolean fTraceIsActive = false;
	private static TmfSignalTracer fSignalTracer;
	static {
		if (fTraceIsActive) {
			fSignalTracer = TmfSignalTracer.getInstance();
			register(fSignalTracer);
		}
	}

	public static synchronized void register(Object listener) {
		Method[] methods = getSignalHandlerMethods(listener);
		if (methods.length > 0)
			fListeners.put(listener, methods);
	}

    public static synchronized void registerVIP(Object listener) {
        Method[] methods = getSignalHandlerMethods(listener);
        if (methods.length > 0)
            fVIPListeners.put(listener, methods);
    }

	public static synchronized void deregister(Object listener) {
		fVIPListeners.remove(listener);
        fListeners.remove(listener);
	}

	/**
	 * Returns the list of signal handlers in the listener. Signal handler name
	 * is irrelevant; only the annotation (@TmfSignalHandler) is important.
	 * 
	 * @param listener
	 * @return
	 */
	static private Method[] getSignalHandlerMethods(Object listener) {
		List<Method> handlers = new ArrayList<Method>();
		Method[] methods = listener.getClass().getMethods();
		for (Method method : methods) {
			if (method.isAnnotationPresent(TmfSignalHandler.class)) {
				handlers.add(method);
			}
		}
		return handlers.toArray(new Method[handlers.size()]);
	}

	/**
	 * Invokes the handling methods that listens to signals of a given type.
	 * 
	 * The list of handlers is built on-the-fly to allow for the dynamic
	 * creation/deletion of signal handlers. Since the number of signal
	 * handlers shouldn't be too high, this is not a big performance issue
	 * to pay for the flexibility.
	 * 
	 * For synchronization purposes, the signal is bracketed by two synch signals.
	 * 
	 * @param signal the signal to dispatch
	 */
	static int fSignalId = 0;
	static public synchronized void dispatchSignal(TmfSignal signal) {
		fSignalId++;
		sendSignal(new TmfStartSynchSignal(fSignalId));
		signal.setReference(fSignalId);
		sendSignal(signal);
		sendSignal(new TmfEndSynchSignal(fSignalId));
	}

    static private void sendSignal(TmfSignal signal) {
        sendSignal(fVIPListeners, signal);
        sendSignal(fListeners, signal);
    }

    static private void sendSignal(Map<Object, Method[]> listeners, TmfSignal signal) {

        if (Tracer.isSignalTraced()) Tracer.traceSignal(signal, "(start)"); //$NON-NLS-1$

        // Build the list of listener methods that are registered for this signal
        Class<?> signalClass = signal.getClass();
        Map<Object, List<Method>> targets = new HashMap<Object, List<Method>>();
        targets.clear();
        for (Map.Entry<Object, Method[]> entry : listeners.entrySet()) {
            List<Method> matchingMethods = new ArrayList<Method>();
            for (Method method : entry.getValue()) {
                if (method.getParameterTypes()[0].isAssignableFrom(signalClass)) {
                    matchingMethods.add(method);
                }
            }
            if (!matchingMethods.isEmpty()) {
                targets.put(entry.getKey(), matchingMethods);
            }
        }

        // Call the signal handlers 
        for (Map.Entry<Object, List<Method>> entry : targets.entrySet()) {
            for (Method method : entry.getValue()) {
                try {
                    method.invoke(entry.getKey(), new Object[] { signal });
                    if (Tracer.isSignalTraced()) {
                        Object key = entry.getKey();
                        String hash = String.format("%1$08X", entry.getKey().hashCode()); //$NON-NLS-1$
                        String target = "[" + hash + "] " + key.getClass().getSimpleName() + ":" + method.getName();   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                        Tracer.traceSignal(signal, target);                     
                    }
                } catch (IllegalArgumentException e) {
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                }
            }
        }

        if (Tracer.isSignalTraced()) Tracer.traceSignal(signal, "(end)"); //$NON-NLS-1$
    }

}
