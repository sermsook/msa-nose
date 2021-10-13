package edu.baylor.ecs.msanose.model.context;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data public class MicroserviceDesignMetricsContext {
    double ratioOfVersionedAPIs;
    double ratioOfManageableStandards;
    double ratioOfAcyclicCalls;
    double ratioOfSeparateLibraries;
    double ratioOfRightCuts;
    double ratioOfCoarseGrainedMicroservices;
    double ratioOfResolvedEndpoints;
    double ratioOfSeparateDatabases;
    double ratioOfAppropriateDatabaseAccess;
    double ratioOfNonESBMicroservices;
    double ratioOfManageableConnections;

    public MicroserviceDesignMetricsContext() {
        this.ratioOfVersionedAPIs = 0;
        this.ratioOfManageableStandards = 0;
        this.ratioOfAcyclicCalls = 0;
        this.ratioOfSeparateLibraries = 0;
        this.ratioOfRightCuts = 0;
        this.ratioOfCoarseGrainedMicroservices = 0;
        this.ratioOfResolvedEndpoints = 0;
        this.ratioOfSeparateDatabases = 0;
        this.ratioOfAppropriateDatabaseAccess = 0;
        this.ratioOfNonESBMicroservices = 0;
        this.ratioOfManageableConnections = 0;
    }

    public ApplicationSmellsContext derivedMetric(ApplicationSmellsContext context) {
        this.ratioOfVersionedAPIs = 1 - context.getUnversionedAPIContext().getRatioOfNonVersionedAPIs();
        this.ratioOfManageableStandards = 1 -context.getTooManyStandardsContext().getRatioOfExcessiveStandards();
        this.ratioOfAcyclicCalls = 1 - context.getCyclicDependencyContext().getRatioOfCyclicDependency();
        this.ratioOfSeparateLibraries = 1 - context.getSharedLibraryContext().getRatioOfSharedLibraries();
        this.ratioOfRightCuts = 1 - context.getWrongCutsContext().getRatioOfWrongCuts();
        this.ratioOfCoarseGrainedMicroservices = 1 - context.getMicroservicesGreedyContext().getRatioOfNanoMicroservices();
        this.ratioOfResolvedEndpoints = 1 - context.getHardCodedEndpointsContext().getRatioOfHardCodedEndpoints();
        this.ratioOfSeparateDatabases = 1 - context.getSharedPersistencyContext().getRatioOfSharedDatabases();
        this.ratioOfAppropriateDatabaseAccess = 1 - context.getInappropriateServiceIntimacyContext().getRatioOfInappropriateDatabaseAccess();
        this.ratioOfNonESBMicroservices = 1 - context.getEsbContext().getRatioOfESBMicroservices();
        this.ratioOfManageableConnections = 1 - context.getAPIGatewayContext().getRatioOfUnmanageableConnections();
        return context;
    }
}
