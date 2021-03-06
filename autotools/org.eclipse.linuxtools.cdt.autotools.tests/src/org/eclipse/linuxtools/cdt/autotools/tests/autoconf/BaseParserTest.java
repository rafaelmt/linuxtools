/*******************************************************************************
 * Copyright (c) 2008 Nokia Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ed Swartz (Nokia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.cdt.autotools.tests.autoconf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.linuxtools.cdt.autotools.ui.editors.parser.AutoconfElement;
import org.eclipse.linuxtools.cdt.autotools.ui.editors.parser.AutoconfMacroDetector;
import org.eclipse.linuxtools.cdt.autotools.ui.editors.parser.AutoconfMacroElement;
import org.eclipse.linuxtools.cdt.autotools.ui.editors.parser.AutoconfParser;
import org.eclipse.linuxtools.cdt.autotools.ui.editors.parser.IAutoconfErrorHandler;
import org.eclipse.linuxtools.cdt.autotools.ui.editors.parser.IAutoconfMacroValidator;
import org.eclipse.linuxtools.cdt.autotools.ui.editors.parser.ParseException;

public abstract class BaseParserTest extends TestCase {

	private IAutoconfErrorHandler errorHandler;
	protected List errors;
	private IAutoconfMacroValidator macroValidator;
	private Set macroNames;
	private AutoconfMacroDetector macroDetector;

	public BaseParserTest() {
		super();
	}

	public BaseParserTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		errors = new ArrayList();
		this.errorHandler = new IAutoconfErrorHandler() {
	
			public void handleError(ParseException exception) {
				assertNotNull(exception);
				errors.add(exception);
			}
			
		};
	
		this.macroDetector = new AutoconfMacroDetector();
		
		macroNames = new HashSet/*<String>*/();
		this.macroValidator = new IAutoconfMacroValidator() {
	
			public void validateMacroCall(AutoconfMacroElement element)
					throws ParseException {
				assertNotNull(element);
				assertNotNull(element.getName());
				assertNotNull(element.getChildren());
				macroNames.add(element.getName());
			}
			
		};
	}

	protected void tearDown() throws Exception {
	}

	protected IDocument createDocument(String text) {
		return new Document(text);
	}

	/**
	 * Parse the document with no error or macro handlers
	 * @param parser
	 * @param document
	 * @return root
	 */
	protected AutoconfElement parseNoHandlers(IDocument document) {
		AutoconfParser parser = new AutoconfParser(null, null, null);
		AutoconfElement root = parser.parse(document);
		assertNotNull(root);
		return root;
	}

	/**
	 * Parse the document with the standard error or macro handlers,
	 * cleared out before use
	 * @param parser
	 * @param document
	 * @return root 
	 */
	protected AutoconfElement parseWithHandlers(IDocument document) {
		AutoconfParser parser = new AutoconfParser(errorHandler, macroDetector, macroValidator);
		
		errors.clear();
		macroNames.clear();
		
		AutoconfElement root = parser.parse(document);
		assertNotNull(root);
		return root;
	}

	/**
	 * Parse the document in 'string' twice, once without any handlers and once with the standard error or macro handlers,
	 * cleared out before use
	 * @param string
	 * @return root 
	 */
	protected AutoconfElement parse(String string) {
		AutoconfElement tree = parse(string, false);
		return tree;
	}

	protected AutoconfElement parse(String string, boolean allowErrors) {
		IDocument document = createDocument(string);
		AutoconfElement root1 = parseNoHandlers(document);
		assertNotNull(root1);
		AutoconfElement root2 = parseWithHandlers(document);
		assertNotNull(root2);
	
		validateSourceTree(root1);
		validateSourceTree(root2);
	
		// TODO: check root1 == root2
		
		// ensure the doc wasn't changed (sanity)
		assertEquals(string, document.get());
	
		if (!allowErrors) {
			if (errors.size() > 0)
				fail("got errors" + errors.get(0));
		}
		else
			assertTrue(errors.size() > 0);
		
		return root2;
	}

	protected void checkError(String msgKey) {
		for (Iterator iter = errors.iterator(); iter.hasNext(); ) {
			ParseException exc = (ParseException) iter.next();
			if (exc.getMessage().contains(msgKey))
				return;
		}
		String any = "";
		if (errors.size() > 0)
			any = ", but saw " + errors.get(0).toString();
		fail("did not find error: " + msgKey + any );
	}

	protected void checkError(String msgKey, int line) {
		ParseException possible = null;
		int distance = 999;
		for (Iterator iter = errors.iterator(); iter.hasNext(); ) {
			ParseException exc = (ParseException) iter.next();
			if (exc.getMessage().contains(msgKey)) {
				int curDistance = Math.abs(exc.getLineNumber() - line);
				if (curDistance < distance) {
					possible = exc;
					distance = curDistance;
				}
				if (exc.getLineNumber() == line)
					return;
			}
		}
		if (possible == null)
			fail("did not find any error: " + msgKey);
		else
			fail("did not find error: " + msgKey + " on line: " + line +", but found one at " + possible.getLineNumber());
	}

	/**
	 * Make sure the source ranges for the tree are sane:
	 * <p>
	 * <li>document set
	 * <li>line numbers, columns valid
	 * <li>children encompassed in parent
	 * <li>siblings non-overlapping
	 * @param element
	 */
	protected void validateSourceTree(AutoconfElement element) {
		validateSourceElement(element);
		AutoconfElement[] kids = element.getChildren();
		for (int i = 0; i < kids.length; i++) {
			if (kids[i].getStartOffset() < element.getStartOffset() 
					|| kids[i].getEndOffset() > element.getEndOffset())
				fail(describeElement(kids[i]) + " not inside parent " + describeElement(element));
			validateSourceTree(kids[i]);
		}
		for (int i = 0; i < kids.length - 1; i++) {
			AutoconfElement kid1 = kids[i];
			AutoconfElement kid2 = kids[i + 1];
			if (kid1.getEndOffset() > kid2.getStartOffset())
				fail(describeElement(kid1) + " overlaps " + describeElement(kid2));
		}
	}

	/**
	 * Make sure the source ranges for the element are sane:
	 * <p>
	 * <li>document set
	 * <li>line numbers, columns valid
	 * @param element
	 */
	private void validateSourceElement(AutoconfElement element) {
		if (element.getDocument() == null)
			fail("no document for " + describeElement(element));
		if (element.getStartOffset() < 0)
			fail("no start offset for " + describeElement(element));
		if (element.getEndOffset() < 0)
			fail("no end offset for " + describeElement(element));
		if (element.getStartOffset() > element.getEndOffset())
			fail("invalid range (start > end) for " + describeElement(element));
	}

	private String describeElement(AutoconfElement element) {
		return element.getClass().getSimpleName() + " <<" + element.getSource() + ">>";
	}

	protected void assertEqualSource(String text, AutoconfElement element) {
		assertEquals(text, element.getSource());
	}

	/** Check that a tree has the given structure.  'elements' is a flattened
	 * representation of the tree, where each node represents one level deeper
	 * in the tree, except for 'null' which backs out one level.
	 * @param tree
	 * @param elements
	 */
	protected void assertTreeStructure(AutoconfElement tree, String[] elements) {
		// if fails, the elements[] array is missing final nulls
		assertEquals(elements.length, assertTreeStructure(tree, elements, 0));
	}

	private int assertTreeStructure(AutoconfElement tree, String[] elements,
			int elementIdx) {
		AutoconfElement[] kids = tree.getChildren();
		for (int j = 0; j < kids.length; j++) {
			if (elementIdx >= elements.length  || elements[elementIdx] == null) {
				fail("extra children in " + tree + " at " + kids[j]);
			}
			if (!kids[j].getName().equals(elements[elementIdx])) 
				fail("did not match " + elements[elementIdx] + ", instead got " + kids[j].getClass().getSimpleName() + "=" + kids[j].getName());
			
			elementIdx++;
			if (kids[j].getChildren().length > 0) {
				elementIdx = assertTreeStructure(kids[j], elements, elementIdx);
				if (elementIdx >= elements.length)
					fail("Missing null in elements list, or invalid tree hierarchy");
				if (elements[elementIdx] != null) {
					fail("not enough children in " + tree);
				}
				elementIdx++;
			}
		}
		return elementIdx;
	}
}
