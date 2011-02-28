/*******************************************************************************
 * Copyright (c) 2007, 2009 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat - initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.rpm.ui.editor.parser;

public class SpecfileDefine extends SpecfileTag {

	public SpecfileDefine(String name, int value, Specfile specfile) {
		super(name, value, specfile);
	}

	public SpecfileDefine(String name, String value, Specfile specfile) {
		super(name, value, specfile);
	}

	public SpecfileDefine(SpecfileTag tag) {
		setName(tag.getName().toLowerCase());
		setSpecfile(tag.getSpecfile());
		setLineNumber(tag.getLineNumber());
		if (tag.getTagType().equals(TagType.STRING)) {
			setStringValue(tag.getStringValue());
		}
		if (tag.getTagType().equals(TagType.INT)) {
			setIntValue(tag.getIntValue());
		}
	}

}
