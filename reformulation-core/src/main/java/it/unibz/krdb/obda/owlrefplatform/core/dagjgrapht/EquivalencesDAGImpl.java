package it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht;

/*
 * #%L
 * ontop-reformulation-core
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



import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import com.google.common.collect.ImmutableMap;


/**
 * DAG from an OntologyGraph
 * 
 * The vertices of the DAG are equivalence sets (Equivalences)
 * The edges form the minimal set whose transitive and reflexive closure
 * coincides with the transitive and reflexive closure of the ontology graph
 *  
 * The key component is the Gabow SCC algorithm for computing 
 * strongly connected components
 * 
*/

public class EquivalencesDAGImpl<T> implements EquivalencesDAG<T> {
	
	private final SimpleDirectedGraph <Equivalences<T>,DefaultEdge> dag;
	private final ImmutableMap<T, Equivalences<T>> vertexIndex;
	
	/**
	 * the EquivalenceIndex maps predicates to the representatives of their equivalence class (in TBox)
	 * (these maps are only used for the equivalence-reduced TBoxes)
	 * 
	 * it contains 
	 * 		- an entry for each property name other than the representative of an equivalence class 
	 * 				(or its inverse)
	 * 		- an entry for each class name other than the representative of its equivalence class
	 */
	

	// maps Ts to the vertices of the DAG that they would be in in the non-reduced DAG
	final ImmutableMap<T, Equivalences<T>> equivalenceIndex;   
	
	private final Map<Equivalences<T>, Set<Equivalences<T>>> cacheSub;
	private final Map<T, Set<T>> cacheSubRep;

	
	private DefaultDirectedGraph<T,DefaultEdge> graph; // used in tests and SIGMA reduction
	
	public EquivalencesDAGImpl(DefaultDirectedGraph<T,DefaultEdge> graph, SimpleDirectedGraph <Equivalences<T>,DefaultEdge> dag, ImmutableMap<T, Equivalences<T>> vertexIndex, ImmutableMap<T, Equivalences<T>> equivalenceIndex) {	
		this.graph = graph;
		this.dag = dag;
		this.vertexIndex = vertexIndex;
		this.equivalenceIndex = equivalenceIndex;
		
		this.cacheSub = new HashMap<>();
		this.cacheSubRep = new HashMap<>();
	}

	
	/** 
	 * 
	 */
	@Override
	public Equivalences<T> getVertex(T v) {
		return vertexIndex.get(v);
	}
	
	@Override
	public T getCanonicalRepresentative(T v) {
		Equivalences<T> vs = equivalenceIndex.get(v);
		if (vs == null)
			return null;
		
		return vs.getRepresentative();		
	}
	
	/**
	 * 
	 */
	@Override
	public Set<Equivalences<T>> getDirectSub(Equivalences<T> v) {
		Set<Equivalences<T>> result = new LinkedHashSet<>();

		for (DefaultEdge edge : dag.incomingEdgesOf(v)) {	
			Equivalences<T> source = dag.getEdgeSource(edge);
			result.add(source);
		}
		return Collections.unmodifiableSet(result);
	}

	/** 
	 * 
	 */
	@Override
	public Set<Equivalences<T>> getSub(Equivalences<T> v) {

		Set<Equivalences<T>> result = cacheSub.get(v);
		if (result == null) {
			result = new LinkedHashSet<>();

			BreadthFirstIterator<Equivalences<T>, DefaultEdge>  iterator = 
						new BreadthFirstIterator<Equivalences<T>, DefaultEdge>(
								new EdgeReversedGraph<>(dag), v);

			while (iterator.hasNext()) {
				Equivalences<T> child = iterator.next();
				result.add(child);
			}
			result = Collections.unmodifiableSet(result);
			cacheSub.put(v, result);
		}
		return result; 
	}

	/** 
	 * 
	 */
	@Override
	public Set<T> getSubRepresentatives(T v) {
		Equivalences<T> eq = vertexIndex.get(v);
		
		if (eq == null)
			return Collections.singleton(v);
		
		Set<T> result = cacheSubRep.get(eq.getRepresentative());
		if (result == null) {
			result = new LinkedHashSet<T>();

			BreadthFirstIterator<Equivalences<T>, DefaultEdge>  iterator = 
						new BreadthFirstIterator<Equivalences<T>, DefaultEdge>(
								new EdgeReversedGraph<>(dag), eq);

			while (iterator.hasNext()) {
				Equivalences<T> child = iterator.next();
				result.add(child.getRepresentative());
			}
			result = Collections.unmodifiableSet(result);
			cacheSubRep.put(eq.getRepresentative(), result);
		}
		return result; 
	}
	

	
	/** 
	 * 
	 */
	@Override
	public Set<Equivalences<T>> getDirectSuper(Equivalences<T> v) {
		Set<Equivalences<T>> result = new LinkedHashSet<>();

		for (DefaultEdge edge : dag.outgoingEdgesOf(v)) {	
			Equivalences<T> source = dag.getEdgeTarget(edge);
			result.add(source);
		}
		return Collections.unmodifiableSet(result);
	}
	
	/** 
	 * 
	 */
	@Override
	public Set<Equivalences<T>> getSuper(Equivalences<T> v) {

		Set<Equivalences<T>> result = new LinkedHashSet<>();

		BreadthFirstIterator<Equivalences<T>, DefaultEdge>  iterator = 
				new BreadthFirstIterator<Equivalences<T>, DefaultEdge>(dag, v);

		while (iterator.hasNext()) {
			Equivalences<T> parent = iterator.next();
			result.add(parent);
		}
		return Collections.unmodifiableSet(result);
	}

	@Override
	public String toString() {
		return dag.toString() + 
				"\n\nEquivalencesMap\n" + vertexIndex;
	}

	/** 
	 * 
	 */
	@Override
	public Iterator<Equivalences<T>> iterator() {
		return dag.vertexSet().iterator();
	}
	
	
	/*
	 *  test only methods
	 */
	
	@Deprecated
	public int edgeSetSize() {
		return dag.edgeSet().size();
	}

	@Deprecated 
	public int vertexSetSize() { 
		return dag.vertexSet().size();
	}
	
	
	public DefaultDirectedGraph<T,DefaultEdge> getGraph() {
		if (graph == null) {
			graph = new DefaultDirectedGraph<>(DefaultEdge.class);

			for (Equivalences<T> node : dag.vertexSet()) {
				for (T v : node) 
					graph.addVertex(v);
				for (T v : node)  {
					graph.addEdge(v, node.getRepresentative());
					graph.addEdge(node.getRepresentative(), v);
				}
			}
			
			for (DefaultEdge edge : dag.edgeSet()) 
				graph.addEdge(dag.getEdgeSource(edge).getRepresentative(), dag.getEdgeTarget(edge).getRepresentative());
		}
		return graph;		
	}
	
	
	
	/*
	 *  construction: main algorithms (static generic methods)
	 */
	
	public static <TT> EquivalencesDAGImpl<TT> getEquivalencesDAG(DefaultDirectedGraph<TT,DefaultEdge> graph) {
		
		
		// each set contains vertices which together form a strongly connected
		// component within the given graph
		GabowSCC<TT, DefaultEdge> inspector = new GabowSCC<>(graph);
		List<Equivalences<TT>> equivalenceSets = inspector.stronglyConnectedSets();

		SimpleDirectedGraph<Equivalences<TT>,DefaultEdge> dag0 = 
					new SimpleDirectedGraph<>(DefaultEdge.class);
		ImmutableMap.Builder<TT, Equivalences<TT>> equivalencesMapB = new ImmutableMap.Builder<>();

		for (Equivalences<TT> equivalenceSet : equivalenceSets)  {
			for (TT node : equivalenceSet) 
				equivalencesMapB.put(node, equivalenceSet);

			dag0.addVertex(equivalenceSet);
		}

		ImmutableMap<TT, Equivalences<TT>> equivalencesMap = equivalencesMapB.build();
		
		for (Equivalences<TT> equivalenceSet : equivalenceSets)  {
			for (TT e : equivalenceSet) {			
				for (DefaultEdge edge : graph.outgoingEdgesOf(e)) {
					TT t = graph.getEdgeTarget(edge);
					if (!equivalenceSet.contains(t))
						dag0.addEdge(equivalenceSet, equivalencesMap.get(t));
				}
				for (DefaultEdge edge : graph.incomingEdgesOf(e)) {
					TT s = graph.getEdgeSource(edge);
					if (!equivalenceSet.contains(s))
						dag0.addEdge(equivalencesMap.get(s), equivalenceSet);
				}
			}
		}
		

		// removed redundant edges
		
		SimpleDirectedGraph <Equivalences<TT>,DefaultEdge> dag = 
						new SimpleDirectedGraph<>(DefaultEdge.class);

		for (Equivalences<TT> v : dag0.vertexSet())
			dag.addVertex(v);

		for (DefaultEdge edge : dag0.edgeSet()) {
			Equivalences<TT> v1 = dag0.getEdgeSource(edge);
			Equivalences<TT> v2 = dag0.getEdgeTarget(edge);
			boolean redundant = false;

			if (dag0.outDegreeOf(v1) > 1) {
				// an edge is redundant if 
				//  its source has an edge going to a vertex 
				//         from which the target is reachable (in one step) 
				for (DefaultEdge e2 : dag0.outgoingEdgesOf(v1)) 
					if (dag0.containsEdge(dag0.getEdgeTarget(e2), v2)) {
						redundant = true;
						break;
					}
			}
			if (!redundant)
				dag.addEdge(v1, v2);
		}
		
		return new EquivalencesDAGImpl<TT>(graph, dag, equivalencesMap, ImmutableMap.<TT, Equivalences<TT>>of());
	}

}
