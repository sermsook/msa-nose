package edu.baylor.ecs.msanose.model.context;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data public class QualityAttributesContext {
    double understandability;
    double modularity;
    double modifiability;
    double reusability;
    double scalability;
    double confidentiality;
    double faultTolerance;

    public QualityAttributesContext() {
        this.understandability = 0;
        this.modularity = 0;
        this.modifiability = 0;
        this.reusability = 0;
        this.scalability = 0;
        this.confidentiality = 0;
        this.faultTolerance = 0;
    }

    public ApplicationSmellsContext calculateQualityAttributesFactor(ApplicationSmellsContext context) {
        MicroserviceDesignMetricsContext derivedMetric = context.getMicroserviceDesignMetrics();
        double versionedAPIs = derivedMetric.getRatioOfVersionedAPIs();
        double manageableStandards = derivedMetric.getRatioOfManageableStandards();
        double acyclicCalls = derivedMetric.getRatioOfAcyclicCalls();
        double nonESBMicroservices = derivedMetric.getRatioOfNonESBMicroservices();
        double separateLibraries = derivedMetric.getRatioOfSeparateLibraries();
        double rightCuts = derivedMetric.getRatioOfRightCuts();
        double coarseGrainedMicroservices = derivedMetric.getRatioOfCoarseGrainedMicroservices();
        double separatePersistency = derivedMetric.getRatioOfSeparateDatabases();
        double appropriateServiceRelationship = derivedMetric.getRatioOfAppropriateDatabaseAccess();
        double manageableConnections = derivedMetric.getRatioOfManageableConnections();
        double resolvedEndpoints = derivedMetric.getRatioOfResolvedEndpoints();

        this.understandability = 0.3*versionedAPIs + 0.15*manageableStandards + 0.35*acyclicCalls + 0.2*nonESBMicroservices;
        this.modularity = 0.25*acyclicCalls + 0.08*separateLibraries + 0.3*rightCuts + 0.04*coarseGrainedMicroservices + 0.2*separatePersistency + 0.13*appropriateServiceRelationship;
        this.modifiability = 0.25*versionedAPIs + 0.3*acyclicCalls + 0.08*separateLibraries+ 0.04*coarseGrainedMicroservices + 0.2*nonESBMicroservices + 0.13*manageableConnections;
        this.reusability = 0.5*acyclicCalls + 0.2*coarseGrainedMicroservices + 0.3*nonESBMicroservices;
        this.scalability = 0.5*rightCuts + 0.5*resolvedEndpoints;
        this.confidentiality = 0.6*separatePersistency + 0.4*appropriateServiceRelationship;
        this.faultTolerance = 0.45*separatePersistency + 0.2*appropriateServiceRelationship + 0.35*nonESBMicroservices;
        return context;
    }
}
