package it.unibz.inf.ontop.spec.mapping.parser.exception;

/*
 * #%L
 * ontop-obdalib-core
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;

public class UnsupportedTagException extends IOException {

	private static final long serialVersionUID = 1L;

	private String tagName;
	
	public UnsupportedTagException(String tagName) {
		super();
		this.tagName = tagName;
	}
	
	@Override
    public String getMessage() {
		return "The tag " + tagName + " is no longer supported. You may safely remove the content from the file.";
	}
}