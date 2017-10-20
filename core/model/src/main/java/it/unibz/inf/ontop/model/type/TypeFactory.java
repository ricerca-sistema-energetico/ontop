package it.unibz.inf.ontop.model.type;

import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.model.term.functionsymbol.DatatypePredicate;
import it.unibz.inf.ontop.model.term.functionsymbol.Predicate;
import it.unibz.inf.ontop.model.vocabulary.XSD;
import org.apache.commons.rdf.api.IRI;

import java.util.Optional;

public interface TypeFactory {

	@Deprecated
	Optional<RDFDatatype> getOptionalDatatype(String uri);

	Optional<RDFDatatype> getOptionalDatatype(IRI iri);

	Optional<TermType> getInternalType(DatatypePredicate predicate);

	DatatypePredicate getRequiredTypePredicate(TermType type);

	DatatypePredicate getRequiredTypePredicate(IRI datatypeIri);

	Optional<DatatypePredicate> getOptionalTypePredicate(TermType type);
		
	boolean isBoolean(Predicate p);
	
	boolean isInteger(Predicate p);
	
	boolean isFloat(Predicate p);
	
//	boolean isLiteral(Predicate p);
	
	boolean isString(Predicate p);
	
	ImmutableList<Predicate> getDatatypePredicates();

	/**
	 * TODO: refactor it
	 */
	TermType getTermType(COL_TYPE type);
	RDFDatatype getLangTermType(String languageTag);

	/**
	 * Don't call it with langString!
	 */
	RDFDatatype getDatatype(IRI iri);

	ObjectRDFType getIRITermType();

	ObjectRDFType getBlankNodeType();

	UnboundRDFTermType getUnboundTermType();

	RDFDatatype getUnsupportedDatatype();

	RDFDatatype getAbstractOntopNumericDatatype();
	RDFDatatype getAbstractRDFSLiteral();

	TermType getAbstractAtomicTermType();

	RDFTermType getAbstractRDFTermType();

	ObjectRDFType getAbstractObjectRDFType();

	default RDFDatatype getXsdIntegerDatatype() {
		return getDatatype(XSD.INTEGER);
	}

	default RDFDatatype getXsdDecimalDatatype() {
		return getDatatype(XSD.DECIMAL);
	}

	default RDFDatatype getXsdStringDatatype() {
		return getDatatype(XSD.STRING);
	}

	default RDFDatatype getXsdBooleanDatatype() {
		return getDatatype(XSD.BOOLEAN);
	}

	default RDFDatatype getXsdDoubleDatatype() {
		return getDatatype(XSD.DOUBLE);
	}

	default RDFDatatype getXsdFloatDatatype() {
		return getDatatype(XSD.FLOAT);
	}

	default RDFDatatype getXsdDatetimeDatatype() {
		return getDatatype(XSD.DATETIME);
	}
}