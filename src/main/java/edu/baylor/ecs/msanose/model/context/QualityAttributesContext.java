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
        boolean hasApiGateway = context.getAPIGatewayContext().hasApiGateway();

        this.understandability = 0.203*rightCuts + 0.177*acyclicCalls + 0.165*versionedAPIs + 0.152*nonESBMicroservices + 0.127*manageableConnections +
                0.101*manageableStandards + 0.076*coarseGrainedMicroservices;
        this.modularity = 0.239*rightCuts + 0.209*acyclicCalls + 0.194*separatePersistency + 0.149*appropriateServiceRelationship +
                0.119*separateLibraries + 0.090*coarseGrainedMicroservices;
        this.modifiability =  0.127*rightCuts + 0.127*resolvedEndpoints + 0.111*acyclicCalls + 0.103*separatePersistency +0.103*versionedAPIs +
                0.095*nonESBMicroservices + 0.079*manageableConnections + 0.079*appropriateServiceRelationship + 0.063*separateLibraries +
                0.063*manageableStandards+ 0.048*coarseGrainedMicroservices;
        this.reusability = 0.333*rightCuts + 0.292*acyclicCalls + 0.250*nonESBMicroservices + 0.125*coarseGrainedMicroservices;
        this.scalability = 0.250*rightCuts + 0.250*resolvedEndpoints + 0.219*acyclicCalls + 0.188*nonESBMicroservices + 0.094*coarseGrainedMicroservices;
        this.confidentiality = 0.565*separatePersistency + 0.435*appropriateServiceRelationship;
        if (hasApiGateway) {
            this.faultTolerance =  0.298*acyclicCalls + 0.277*separatePersistency + 0.255*nonESBMicroservices + 0.213*appropriateServiceRelationship +
                    0.170*separateLibraries - 0.213*manageableConnections;
        }else {
            this.faultTolerance =  0.246*acyclicCalls + 0.228*separatePersistency + 0.211*nonESBMicroservices + 0.175*appropriateServiceRelationship +
                    0.140*separateLibraries;
        }

        return context;
    }
}
