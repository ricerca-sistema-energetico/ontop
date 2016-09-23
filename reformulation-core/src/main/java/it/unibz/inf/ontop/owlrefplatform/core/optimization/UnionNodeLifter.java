package it.unibz.inf.ontop.owlrefplatform.core.optimization;

import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.model.Variable;
import it.unibz.inf.ontop.pivotalrepr.IntermediateQuery;
import it.unibz.inf.ontop.pivotalrepr.QueryNode;
import it.unibz.inf.ontop.pivotalrepr.UnionNode;

import java.util.Optional;

/**
 *  Class to help choosing the useful ancestors to lift the UnionNode.
 *
 */

public interface UnionNodeLifter {

    /**
     *
     * @param currentQuery
     * @param unionNode
     * @param unionVariables variables usually coming from conflicting bindings in the unionNode
     * @return querynode to which the union will be lifted, if possible
     */
    public Optional<QueryNode> chooseLevelLift(IntermediateQuery currentQuery, UnionNode unionNode, ImmutableSet<Variable> unionVariables);


}