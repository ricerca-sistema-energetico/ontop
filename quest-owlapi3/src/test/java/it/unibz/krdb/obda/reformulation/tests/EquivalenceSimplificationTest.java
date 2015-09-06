package it.unibz.krdb.obda.reformulation.tests;

/*
 * #%L
 * ontop-quest-owlapi3
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


import it.unibz.krdb.obda.ontology.ClassExpression;
import it.unibz.krdb.obda.ontology.ImmutableOntologyVocabulary;
import it.unibz.krdb.obda.ontology.ObjectPropertyExpression;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.owlapi3.OWLAPI3TranslatorUtility;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.EquivalencesDAG;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.EquivalencesDAGImpl;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasonerImpl;
import junit.framework.TestCase;

public class EquivalenceSimplificationTest extends TestCase {

	private final String testURI = "http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#";
	private final String path = "src/test/resources/test/equivalence/";

	public void test_equivalence_namedclasses() throws Exception {

		/*
		 * The ontology contains classes A1 = A2 = A3 >= B1 = B2 = B3 >= C1 = C2 = C3
		 */

		Ontology ontology = OWLAPI3TranslatorUtility.loadOntologyFromFile(path + "test_401.owl");

		TBoxReasoner reasoner = TBoxReasonerImpl.create(ontology);
		TBoxReasoner simple = TBoxReasonerImpl.getEquivalenceSimplifiedReasoner(reasoner);

		EquivalencesDAGImpl<ClassExpression> classDAG = (EquivalencesDAGImpl<ClassExpression>)simple.getClassDAG();
		EquivalencesDAGImpl<ObjectPropertyExpression> propDAG = (EquivalencesDAGImpl<ObjectPropertyExpression>)simple.getObjectPropertyDAG();
		
		assertEquals(3, classDAG.vertexSetSize()); // A1, B1, C1
		assertEquals(0, propDAG.vertexSetSize()); // no properties
		assertEquals(2, classDAG.edgeSetSize());  // A1 <- B1 <- C1
		assertEquals(0, propDAG.edgeSetSize());  // no properties

		ImmutableOntologyVocabulary voc = ontology.getVocabulary();
		EquivalencesDAG<ClassExpression> classes = simple.getClassDAG();

		assertFalse(classes.getCanonicalRepresentative(voc.getClass(testURI + "A1")) != null);
		assertFalse(classes.getCanonicalRepresentative(voc.getClass(testURI + "B1")) != null);
		assertFalse(classes.getCanonicalRepresentative(voc.getClass(testURI + "C1")) != null);
		assertTrue(classes.getCanonicalRepresentative(voc.getClass(testURI + "A2")) != null); 
		assertTrue(classes.getCanonicalRepresentative(voc.getClass(testURI + "A3")) != null);
		assertTrue(classes.getCanonicalRepresentative(voc.getClass(testURI + "B2")) != null);
		assertTrue(classes.getCanonicalRepresentative(voc.getClass(testURI + "B3")) != null); 
		assertTrue(classes.getCanonicalRepresentative(voc.getClass(testURI + "C2")) != null);
		assertTrue(classes.getCanonicalRepresentative(voc.getClass(testURI + "C3")) != null);
		
		assertEquals(voc.getClass(testURI + "A1"), classes.getCanonicalRepresentative(voc.getClass(testURI + "A2")));
		assertEquals(voc.getClass(testURI + "A1"), classes.getCanonicalRepresentative(voc.getClass(testURI + "A3")));
		assertEquals(voc.getClass(testURI + "B1"), classes.getCanonicalRepresentative(voc.getClass(testURI + "B2"))); 
		assertEquals(voc.getClass(testURI + "B1"), classes.getCanonicalRepresentative(voc.getClass(testURI + "B3"))); 
		assertEquals(voc.getClass(testURI + "C1"), classes.getCanonicalRepresentative(voc.getClass(testURI + "C2")));
		assertEquals(voc.getClass(testURI + "C1"), classes.getCanonicalRepresentative(voc.getClass(testURI + "C3")));
	}
	
	
	public void test_equivalence_namedproperties() throws Exception {

		/*
		 * The ontology contains object properties A1 = A2 = A3 >= B1 = B2 = B3 >= C1 = C2 = C3
		 */

		Ontology ontology = OWLAPI3TranslatorUtility.loadOntologyFromFile(path + "test_402.owl");
		
		TBoxReasoner reasoner = TBoxReasonerImpl.create(ontology);
		TBoxReasoner simple = TBoxReasonerImpl.getEquivalenceSimplifiedReasoner(reasoner);

		EquivalencesDAGImpl<ClassExpression> classDAG = (EquivalencesDAGImpl<ClassExpression>)simple.getClassDAG();
		EquivalencesDAGImpl<ObjectPropertyExpression> propDAG = (EquivalencesDAGImpl<ObjectPropertyExpression>)simple.getObjectPropertyDAG();
		
		// \exists A1, \exists A1^-,  \exists B1, \exists B1^-,  \exists C1, \exists C1^-
		assertEquals(6, classDAG.vertexSetSize()); 
		assertEquals(6, propDAG.vertexSetSize()); // A1, A1^-, B1, B1^-, C1, C1^- 
		// \exists A1 <- \exists B1 <- \exists C1, \exists A1^- <- \exists B1^- <- \exists C1^-
		assertEquals(4, classDAG.edgeSetSize()); 
		assertEquals(4, classDAG.edgeSetSize()); // A1 <- B1 <- C1, A1^- <- B1^- <- C1^-

		ImmutableOntologyVocabulary voc = ontology.getVocabulary();
		EquivalencesDAG<ObjectPropertyExpression> ops = simple.getObjectPropertyDAG();
		
		assertFalse(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "A1")) != null);
		assertFalse(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "B1")) != null);
		assertFalse(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "C1")) != null);
		assertTrue(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "A2")) != null);
		assertTrue(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "A3")) != null);
		assertTrue(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "B2")) != null);
		assertTrue(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "B3")) != null); 
		assertTrue(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "C2")) != null);
		assertTrue(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "C3")) != null);
		
		assertEquals(voc.getObjectProperty(testURI + "A1"), ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "A2")));
		assertEquals(voc.getObjectProperty(testURI + "A1"), ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "A3")));
		assertEquals(voc.getObjectProperty(testURI + "B1"), ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "B2"))); 
		assertEquals(voc.getObjectProperty(testURI + "B1"), ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "B3"))); 
		assertEquals(voc.getObjectProperty(testURI + "C1"), ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "C2")));
		assertEquals(voc.getObjectProperty(testURI + "C1"), ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "C3")));
	}
	
	
	public void test_equivalence_namedclassesandexists() throws Exception {

		/*
		 * The ontology contains object properties M, R, S
		 * and classes A1 = A3 = \exists R <= B1 = B3 = \exists S^- <= C1 = C3 = \exists M
		 */

		Ontology ontology = OWLAPI3TranslatorUtility.loadOntologyFromFile(path + "test_403.owl");

		TBoxReasoner reasoner = TBoxReasonerImpl.create(ontology);
		TBoxReasoner simple = TBoxReasonerImpl.getEquivalenceSimplifiedReasoner(reasoner);
		
		EquivalencesDAGImpl<ClassExpression> classDAG = (EquivalencesDAGImpl<ClassExpression>)simple.getClassDAG();
		EquivalencesDAGImpl<ObjectPropertyExpression> propDAG = (EquivalencesDAGImpl<ObjectPropertyExpression>)simple.getObjectPropertyDAG();
		
		assertEquals(6, propDAG.vertexSetSize()); // M, M^-, R, R^-, S, S^-
		assertEquals(6, classDAG.vertexSetSize()); // A1, B1, C1, \exists R^-, \exists S, \exists M^-
		assertEquals(0, propDAG.edgeSetSize()); // 
		assertEquals(2, classDAG.edgeSetSize()); // A1 <- B1 <- C1

		ImmutableOntologyVocabulary voc = ontology.getVocabulary();
		EquivalencesDAG<ClassExpression> classes = simple.getClassDAG();

		assertFalse(classes.getCanonicalRepresentative(voc.getClass(testURI + "A1")) != null);
		assertFalse(classes.getCanonicalRepresentative(voc.getClass(testURI + "B1")) != null);
		assertFalse(classes.getCanonicalRepresentative(voc.getClass(testURI + "C1")) != null);
		assertTrue(classes.getCanonicalRepresentative(voc.getClass(testURI + "A3")) != null);
		assertTrue(classes.getCanonicalRepresentative(voc.getClass(testURI + "B3")) != null); 
		assertTrue(classes.getCanonicalRepresentative(voc.getClass(testURI + "C3")) != null);
		
		assertEquals(voc.getClass(testURI + "A1"), classes.getCanonicalRepresentative(voc.getClass(testURI + "A3")));
		assertEquals(voc.getClass(testURI + "B1"), classes.getCanonicalRepresentative(voc.getClass(testURI + "B3"))); 
		assertEquals(voc.getClass(testURI + "C1"), classes.getCanonicalRepresentative(voc.getClass(testURI + "C3")));
	}
	
	public void test_equivalence_namedproperties_and_inverses() throws Exception {

		/*
		 * The ontology contains object properties A1 = A2^- = A3 >= B1 = B2^- = B3 >= C1 = C2^- = C3
		 */

		Ontology ontology = OWLAPI3TranslatorUtility.loadOntologyFromFile(path + "test_404.owl");

		TBoxReasoner reasoner = TBoxReasonerImpl.create(ontology);
		TBoxReasoner simple = TBoxReasonerImpl.getEquivalenceSimplifiedReasoner(reasoner);

		EquivalencesDAGImpl<ClassExpression> classDAG = (EquivalencesDAGImpl<ClassExpression>)simple.getClassDAG();
		EquivalencesDAGImpl<ObjectPropertyExpression> propDAG = (EquivalencesDAGImpl<ObjectPropertyExpression>)simple.getObjectPropertyDAG();
		
		assertEquals(6, classDAG.vertexSetSize()); // A1, A1^-, B1, B1^-, C1, C1^-
		assertEquals(6, propDAG.vertexSetSize()); // 
		assertEquals(4, classDAG.edgeSetSize()); // A1 >= B1 >= C1, A1^- >= B1^- >= C1^-
		assertEquals(4, propDAG.edgeSetSize()); //

		ImmutableOntologyVocabulary voc = ontology.getVocabulary();
		EquivalencesDAG<ObjectPropertyExpression> ops = simple.getObjectPropertyDAG();

		assertFalse(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "A1")) != null);
		assertFalse(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "B1")) != null);
		assertFalse(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "C1")) != null);
		assertTrue(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "A2")) != null);
		assertTrue(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "A3")) != null);
		assertTrue(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "B2")) != null);
		assertTrue(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "B3")) != null); 
		assertTrue(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "C2")) != null);
		assertTrue(ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "C3")) != null);
		
		assertEquals(voc.getObjectProperty(testURI + "A1").getInverse(), ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "A2")));
		assertEquals(voc.getObjectProperty(testURI + "A1"), ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "A3")));
		assertEquals(voc.getObjectProperty(testURI + "B1").getInverse(), ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "B2"))); 
		assertEquals(voc.getObjectProperty(testURI + "B1"), ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "B3")));  
		assertEquals(voc.getObjectProperty(testURI + "C1").getInverse(), ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "C2")));
		assertEquals(voc.getObjectProperty(testURI + "C1"), ops.getCanonicalRepresentative(voc.getObjectProperty(testURI + "C3")));
	}

}
