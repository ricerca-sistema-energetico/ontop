package it.unibz.krdb.obda.owlapi3.directmapping;

/*
 * #%L
 * ontop-obdalib-owlapi3
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

import it.unibz.krdb.obda.model.*;
import it.unibz.krdb.obda.model.Predicate.COL_TYPE;
import it.unibz.krdb.obda.utils.JdbcTypeMapper;
import it.unibz.krdb.sql.Attribute;
import it.unibz.krdb.sql.DBMetadata;
import it.unibz.krdb.sql.QuotedID;
import it.unibz.krdb.sql.ForeignKeyConstraint;
import it.unibz.krdb.sql.RelationID;
import it.unibz.krdb.sql.DatabaseRelationDefinition;
import it.unibz.krdb.sql.UniqueConstraint;
import it.unibz.krdb.sql.ForeignKeyConstraint.Component;

import java.util.*;

public class DirectMappingAxiom {
	private final DBMetadata metadata;
	private final DatabaseRelationDefinition table;
	private String SQLString;
	private final String baseuri;
	private final OBDADataFactory df;

	public DirectMappingAxiom(String baseuri, DatabaseRelationDefinition dd,
			DBMetadata obda_md, OBDADataFactory dfac) {
		this.table = dd;
		this.SQLString = "";
		this.metadata = obda_md;
		this.df = dfac;
		if (baseuri == null)
			throw new IllegalArgumentException("Base uri must be specified!");

		this.baseuri = baseuri;
	}


	public String getSQL() {
		String SQLStringTemple = "SELECT * FROM %s";

		SQLString = String.format(SQLStringTemple, table.getID().getSQLRendering());
		return SQLString;
	}

	public Map<String, List<Function>> getRefAxioms() {
		HashMap<String, List<Function>> refAxioms = new HashMap<>();
		List<ForeignKeyConstraint> fks = table.getForeignKeys();
		for (ForeignKeyConstraint fk : fks) {
			refAxioms.put(getRefSQL(fk), getRefCQ(fk));
		}
		return refAxioms;
	}

	private String getRefSQL(ForeignKeyConstraint fk) {

		String SQLStringTempl = "SELECT %s FROM %s WHERE %s";
		String Column = "";

		{
			UniqueConstraint pk = table.getPrimaryKey();
			List<Attribute> attributes;
			if (pk != null)
				attributes = pk.getAttributes();
			else
				attributes = table.getAttributes();

			for (Attribute att : attributes) {
				QuotedID attrName = att.getID();
				Column += getFullyQualifiedColumnName(table.getID(), attrName) + " AS " +
						getAlias(table.getID(), attrName) + ", ";
			}
		}

		String Table = table.getID().getSQLRendering();
		String Condition = "";
		// ROMAN: I'm not sure one tableRef is enough here in case of many foreign keys
		RelationID tableRef0 = null;
		
		// referring object
		int count = 0;
		for (ForeignKeyConstraint.Component comp : fk.getComponents()) {
			Condition += getFullyQualifiedColumnName(table.getID(), comp.getAttribute().getID()) + " = ";

			// get referenced object
			tableRef0 = comp.getReference().getRelation().getID();
			if (count == 0)
				Table += ", " + tableRef0.getSQLRendering();
			QuotedID columnRef0 = comp.getReference().getID();
			Column += getFullyQualifiedColumnName(tableRef0, columnRef0) + " AS "
					+ getAlias(tableRef0, columnRef0);

			Condition += tableRef0.getSQLRendering() + "." + columnRef0.getSQLRendering();

			if (count < fk.getComponents().size() - 1) {
				Column += ", ";
				Condition += " AND ";
			}
			count++;
		}
		for (DatabaseRelationDefinition tdef : metadata.getDatabaseRelations()) {
			if (tdef.getID().equals(tableRef0)) {
				UniqueConstraint pk = tdef.getPrimaryKey();
				if (pk != null) {
					for (Attribute att : pk.getAttributes()) {
						QuotedID attrName = att.getID();
						String refPki = getFullyQualifiedColumnName(tableRef0, attrName);
						if (!Column.contains(refPki))
							Column += ", " + refPki + " AS " + getAlias(tableRef0, attrName);
					}
				}
				else {
					for (Attribute att : tdef.getAttributes()) {
						QuotedID attrName = att.getID();
						Column += ", " + getFullyQualifiedColumnName(tableRef0, attrName) +
								" AS " + getAlias(tableRef0, attrName);
					}
				}
			}
		}

		return String.format(SQLStringTempl, Column, Table, Condition);
	}

	private static String getFullyQualifiedColumnName(RelationID tableId, QuotedID attrId) {
		 return tableId.getSQLRendering() + "." + attrId.getSQLRendering();
	}
	
	private static String getAlias(RelationID tableId, QuotedID attrId) {
		return tableId.getTableName() + "_" + attrId.getName();
	}
	
	public List<Function> getCQ() {
		Term sub = generateSubject(table, false);
		List<Function> atoms = new LinkedList<>();

		//Class Atom
		atoms.add(df.getFunction(df.getClassPredicate(generateClassURI(table.getID())), sub));


		//DataType Atoms
		JdbcTypeMapper typeMapper = df.getJdbcTypeMapper();
		for (Attribute att : table.getAttributes()) {
			Predicate.COL_TYPE type = typeMapper.getPredicate(att.getType());
			if (type == COL_TYPE.LITERAL) {
				Variable objV = df.getVariable(att.getID().getName());
				atoms.add(df.getFunction(
						df.getDataPropertyPredicate(generateDPURI(table.getID(), att.getID())), sub, objV));
			}
			else {
				Function obj = df.getTypedTerm(df.getVariable(att.getID().getName()), type);
				atoms.add(df.getFunction(
						df.getDataPropertyPredicate(generateDPURI(table.getID(), att.getID())), sub, obj));
			}
		}

		return atoms;
	}

	private List<Function> getRefCQ(ForeignKeyConstraint fk) {

        DatabaseRelationDefinition relation = fk.getRelation();
        Term sub = generateSubject(relation, true);

        DatabaseRelationDefinition referencedRelation = fk.getReferencedRelation();
		Term obj = generateSubject(referencedRelation, true);

		String opURI = generateOPURI(relation.getID(), fk);
		Function atom = df.getFunction(df.getObjectPropertyPredicate(opURI), sub, obj);

		return Collections.singletonList(atom);
	}

	// Generate an URI for class predicate from a string(name of table)
	private String generateClassURI(RelationID tableId) {
		return baseuri + tableId.getTableName();
	}

	/*
	 * Generate an URI for datatype property from a string(name of column) The
	 * style should be "baseuri/tablename#columnname" as required in Direct
	 * Mapping Definition
	 */
	private String generateDPURI(RelationID tableId, QuotedID columnId) {
		return baseuri + percentEncode(tableId.getTableName()) + "#" + percentEncode(columnId.getName());
	}

    /*
     * Generate an URI for object property from a string(name of column)
     *
     * <http://www.w3.org/TR/rdb-direct-mapping/>
     *
     * Definition reference property IRI: the concatenation of:
     *   - the percent-encoded form of the table name,
     *   - the string '#ref-',
     *   - for each column in the foreign key, in order:
     *     - the percent-encoded form of the column name,
     *     - if it is not the last column in the foreign key, a SEMICOLON character ';'
     */
    private String generateOPURI(RelationID tableId, ForeignKeyConstraint fk) {
        StringBuilder columnsInFK = new StringBuilder();

 		for (Component component : fk.getComponents())
            columnsInFK.append(component.getAttribute().getID().getName()).append(";");
        columnsInFK.setLength(columnsInFK.length() - 1); // remove the trailing ;
        
        return baseuri + percentEncode(tableId.getTableName()) + "#ref-" + columnsInFK;
    }

	/*
	 * Generate the subject term of the table
	 *
	 *
	 * TODO replace URI predicate to BNode predicate for tables without PKs in
	 * the following method after 'else'
	 */
	private Term generateSubject(DatabaseRelationDefinition td, boolean ref) {
		
		String tableName = "";
		if (ref)
			tableName = percentEncode(td.getID().getTableName()) + "_";

		UniqueConstraint pk = td.getPrimaryKey();
		if (pk != null) {
			List<Term> terms = new ArrayList<>(pk.getAttributes().size() + 1);
			terms.add(df.getConstantLiteral(subjectTemple(td)));
			for (Attribute att : pk.getAttributes())
				terms.add(df.getVariable(tableName + att.getID().getName()));

			return df.getUriTemplate(terms);
		}
		else {
			List<Term> vars = new ArrayList<>(td.getAttributes().size());
			for (Attribute att : td.getAttributes())
				vars.add(df.getVariable(tableName + att.getID().getName()));

			return df.getBNodeTemplate(vars);
		}
	}

    /**
     *
     *
     * - If the table has a primary key, the row node is a relative IRI obtained by concatenating:
     *   - the percent-encoded form of the table name,
     *   - the SOLIDUS character '/',
     *   - for each column in the primary key, in order:
     *     - the percent-encoded form of the column name,
     *     - a EQUALS SIGN character '=',
     *     - the percent-encoded lexical form of the canonical RDF literal representation of the column value as defined in R2RML section 10.2 Natural Mapping of SQL Values [R2RML],
     &     - if it is not the last column in the primary key, a SEMICOLON character ';'
     * - If the table has no primary key, the row node is a fresh blank node that is unique to this row.

     * @param td
     * @return
     */
	private String subjectTemple(DatabaseRelationDefinition td) {
		/*
		 * It is hard to generate a uniform temple since the number of PK
		 * differs For example, the subject uri temple with one pk should be
		 * like: baseuri+tablename/PKcolumnname={}('col={}...) For table with
		 * more than one pk columns, there will be a ";" between column names
		 */

		String temp = baseuri + percentEncode(td.getID().getTableName()) + "/";
		for (Attribute att : td.getPrimaryKey().getAttributes()) {
			//temp += percentEncode("{" + td.getPrimaryKeys().get(i).getName()) + "};";
			temp += percentEncode(att.getID().getName())+"={};";
		}
		// remove the last "." which is not neccesary
		temp = temp.substring(0, temp.length() - 1);
		// temp="\""+temp+"\"";
		return temp;
	}

	/*
	 * percent encoding for a String
	 */
	private static String percentEncode(String pe) {
		pe = pe.replace("#", "%23");
		pe = pe.replace(".", "%2E");
		pe = pe.replace("-", "%2D");
		pe = pe.replace("/", "%2F");

		pe = pe.replace(" ", "%20");
		pe = pe.replace("!", "%21");
		pe = pe.replace("$", "%24");
		pe = pe.replace("&", "%26");
		pe = pe.replace("'", "%27");
		pe = pe.replace("(", "%28");
		pe = pe.replace(")", "%29");
		pe = pe.replace("*", "%2A");
		pe = pe.replace("+", "%2B");
		pe = pe.replace(",", "%2C");
		pe = pe.replace(":", "%3A");
		pe = pe.replace(";", "%3B");
		pe = pe.replace("=", "%3D");
		pe = pe.replace("?", "%3F");
		pe = pe.replace("@", "%40");
		pe = pe.replace("[", "%5B");
		pe = pe.replace("]", "%5D");
		return pe;
	}

}
