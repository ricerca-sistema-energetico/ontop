package it.unibz.inf.ontop.model.term.impl;

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

import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.model.term.functionsymbol.Predicate;
import it.unibz.inf.ontop.model.type.TermType;

import javax.annotation.Nonnull;

import static it.unibz.inf.ontop.model.OntopModelSingletons.TYPE_FACTORY;

public class PredicateImpl implements Predicate {

	private final ImmutableList<TermType> expectedBaseTypes;

	private int arity = -1;
	private String name = null;
	private int identifier = -1;

	protected PredicateImpl(@Nonnull String name, int arity, @Nonnull ImmutableList<TermType> expectedBaseTypes) {
		if (expectedBaseTypes.size() != arity)
			throw new IllegalArgumentException("expectedBaseTypes.size() must be equal to the arity");
		this.name = name;
		this.identifier = name.hashCode();
		this.arity = arity;
		this.expectedBaseTypes = expectedBaseTypes;
	}

	@Override
	public int getArity() {
		return arity;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PredicateImpl)) {
			return false;
		}
		PredicateImpl pred2 = (PredicateImpl) obj;
		return this.identifier == pred2.identifier;
	}

	@Override
	public int hashCode() {
		return identifier;
	}

	@Override
	public Predicate clone() {
		return this;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public TermType getExpectedBaseType(int index) {
		return expectedBaseTypes.get(index);
	}

	@Override
	public ImmutableList<TermType> getExpectedBaseArgumentTypes() {
		return expectedBaseTypes;
	}

	@Override
	public boolean isClass() {
		return arity == 1
				&& isRDFObject(getExpectedBaseType(0));
	}

	@Override
	public boolean couldBeAnObjectProperty() {
		// IRI/BNode constraint on the subject
		if ((arity != 2) || (!isRDFObject(getExpectedBaseType(0))))
			return false;

		TermType secondBaseType = getExpectedBaseType(1);

		// IRI/BNode constraint
		return isRDFObject(secondBaseType)
				// Or accepting any RDF term
				|| secondBaseType.equals(TYPE_FACTORY.getAbstractRDFTermType());
	}

	@Override
	public boolean couldBeADataProperty() {
		// IRI/BNode constraint on the subject
		if ((arity != 2) || (!isRDFObject(getExpectedBaseType(0))))
			return false;

		TermType secondBaseType = getExpectedBaseType(1);

		// Literal constraint
		return secondBaseType.isA(TYPE_FACTORY.getAbstractRDFSLiteral())
				// Or accepting any RDF term
				|| secondBaseType.equals(TYPE_FACTORY.getAbstractRDFTermType());
	}

	private static boolean isRDFObject(TermType termType) {
		return termType.isA(TYPE_FACTORY.getAbstractObjectRDFType());
	}


	@Override
	public boolean isTriplePredicate() {
		return (arity == 3 && name.equals("triple"));
	}
}