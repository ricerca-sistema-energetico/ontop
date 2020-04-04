package it.unibz.inf.ontop.utils;

import com.google.inject.Injector;
import it.unibz.inf.ontop.dbschema.*;
import it.unibz.inf.ontop.protege.core.DuplicateMappingException;
import it.unibz.inf.ontop.exception.MetadataExtractionException;
import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.model.term.functionsymbol.db.BnodeStringTemplateFunctionSymbol;
import it.unibz.inf.ontop.model.type.TypeFactory;
import it.unibz.inf.ontop.spec.mapping.bootstrap.impl.DirectMappingEngine;
import it.unibz.inf.ontop.spec.mapping.pp.SQLPPMapping;
import it.unibz.inf.ontop.spec.mapping.pp.SQLPPTriplesMap;
import it.unibz.inf.ontop.protege.core.OBDAModel;
import it.unibz.inf.ontop.protege.core.OBDAModelManager;
import it.unibz.inf.ontop.protege.utils.JDBCConnectionManager;
import it.unibz.inf.ontop.spec.mapping.util.MappingOntologyUtils;
import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class BootstrapGenerator {


    private final JDBCConnectionManager connManager;
    private final OntopSQLOWLAPIConfiguration configuration;
    private final OBDAModel activeOBDAModel;
    private final OWLModelManager owlManager;
    private final TypeFactory typeFactory;
    private final DirectMappingEngine directMappingEngine;

    public BootstrapGenerator(OBDAModelManager obdaModelManager, String baseUri,
                              OWLModelManager owlManager)
            throws DuplicateMappingException, SQLException, MetadataExtractionException {
        connManager = JDBCConnectionManager.getJDBCConnectionManager();
        this.owlManager =  owlManager;
        configuration = obdaModelManager.getConfigurationManager().buildOntopSQLOWLAPIConfiguration(owlManager.getActiveOntology());
        activeOBDAModel = obdaModelManager.getActiveOBDAModel();
        typeFactory = obdaModelManager.getTypeFactory();
        Injector injector = configuration.getInjector();
        directMappingEngine = injector.getInstance(DirectMappingEngine.class);

        bootstrapMappingAndOntologyProtege(baseUri);
    }

    private void bootstrapMappingAndOntologyProtege(String baseUri) throws DuplicateMappingException, SQLException, MetadataExtractionException {

        List<SQLPPTriplesMap> sqlppTriplesMaps = bootstrapMapping(activeOBDAModel.generatePPMapping(), baseUri);

        // update protege ontology
        OWLOntologyManager manager = owlManager.getActiveOntology().getOWLOntologyManager();
        Set<OWLDeclarationAxiom> declarationAxioms = MappingOntologyUtils.extractDeclarationAxioms(
                manager,
                sqlppTriplesMaps.stream()
                        .flatMap(ax -> ax.getTargetAtoms().stream()),
                typeFactory,
                true
        );
        List<AddAxiom> addAxioms = declarationAxioms.stream()
                .map(ax -> new AddAxiom(owlManager.getActiveOntology(), ax))
                .collect(Collectors.toList());

        owlManager.applyChanges(addAxioms);
    }

    private List<SQLPPTriplesMap> bootstrapMapping(SQLPPMapping ppMapping, String baseURI)
            throws DuplicateMappingException, MetadataExtractionException, SQLException {

        List<SQLPPTriplesMap> newTriplesMap = new ArrayList<>();

        final Connection conn;
        try {
            conn = connManager.getConnection(configuration.getSettings());
        }
        catch (SQLException e) {
            throw new RuntimeException("JDBC connection is missing, have you setup Ontop Mapping properties?" +
                    " Message: " + e.getMessage());
        }

        if (baseURI == null || baseURI.isEmpty()) {
            baseURI = ppMapping.getPrefixManager().getDefaultPrefix();
        }
        else {
            baseURI = DirectMappingEngine.fixBaseURI(baseURI);
        }

        // this operation is EXPENSIVE
        Collection<DatabaseRelationDefinition> tables = RDBMetadataExtractionTools.loadAllRelations(conn, typeFactory.getDBTypeFactory());

        Map<DatabaseRelationDefinition, BnodeStringTemplateFunctionSymbol> bnodeTemplateMap = new HashMap<>();
        AtomicInteger currentMappingIndex = new AtomicInteger(ppMapping.getTripleMaps().size() + 1);
        for (DatabaseRelationDefinition td : tables) {
            newTriplesMap.addAll(directMappingEngine.getMapping(td, baseURI, bnodeTemplateMap, currentMappingIndex));
        }

        //add to the current model the boostrapped triples map
        for (SQLPPTriplesMap triplesMap: newTriplesMap) {
            activeOBDAModel.addTriplesMap(triplesMap, true);
        }
        return newTriplesMap;
    }


}
