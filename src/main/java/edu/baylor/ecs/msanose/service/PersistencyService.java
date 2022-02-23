package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.msanose.model.SharedPersistency;
import edu.baylor.ecs.msanose.model.context.SharedPersistencyContext;
import edu.baylor.ecs.msanose.model.persistency.DatabaseInstance;
import edu.baylor.ecs.msanose.model.persistency.DatabaseType;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.service.ResourceService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
@Service
@AllArgsConstructor
public class PersistencyService {

    private final ResourceService resourceService;

    public SharedPersistencyContext getSharedPersistency(RequestContext request){
        SharedPersistencyContext context = new SharedPersistencyContext();
        List<String> resourcePaths = resourceService.getResourcePaths(request.getPathToCompiledMicroservices());
        Map<String, DatabaseInstance> databases = new HashMap<>();
        for (String path : resourcePaths) {
            Map<String, Map<String, Object>> yamls = resourceService.getYamls(path, request.getOrganizationPath());
            if (yamls.size() > 0) {
                for(Map.Entry<String, Map<String, Object>> entry : yamls.entrySet()){
                    Map<String, Object> yamlProperties = new HashMap<>();
                    forEachValue(entry.getValue(), "", yamlProperties::put);
                    if(yamlProperties.get("spring.data.mongodb.host") != null){
                        DatabaseInstance databaseInstance = new DatabaseInstance(
                                yamlProperties.get("spring.data.mongodb.database"),
                                yamlProperties.get("spring.data.mongodb.port"),
                                yamlProperties.get("spring.data.mongodb.host"),
                                DatabaseType.MONGO
                        );
                        databases.put(entry.getKey(), databaseInstance);
                    }

                    if(yamlProperties.get("spring.datasource.url") != null){
                        DatabaseInstance databaseInstance = new DatabaseInstance(
                                yamlProperties.get("spring.datasource.database"),
                                yamlProperties.get("spring.datasource.port"),
                                yamlProperties.get("spring.datasource.url"),
                                ((String) yamlProperties.get("spring.datasource.url")).contains("mysql") ? DatabaseType.MYSQL : DatabaseType.GENERIC
                        );
                        databases.put(entry.getKey(), databaseInstance);
                    }
                }
            }
        }

        List<SharedPersistency> sharedPersistencies = new ArrayList<>();
        for(Map.Entry<String, DatabaseInstance> entriesA : databases.entrySet()){
            for(Map.Entry<String, DatabaseInstance> entriesb : databases.entrySet()){
                if(!entriesA.getKey().equals(entriesb.getKey())){
                    if(entriesA.getValue().equals(entriesb.getValue())){

                        // First check if B-A is in the list
                        boolean exists = false;
                        for(SharedPersistency sharedPersistency : sharedPersistencies){
                            if(sharedPersistency.getMsaA().equals(entriesb.getKey()) && sharedPersistency.getMsaB().equals(entriesA.getKey())){
                                exists = true;
                            }
                        }
                        if(!exists){
                            SharedPersistency sharedPersistency = new SharedPersistency(entriesA.getKey(), entriesb.getKey(), entriesA.getValue());
                            sharedPersistencies.add(sharedPersistency);
                        }
                    }
                }
            }
        }

        context.setSharedPersistencies(sharedPersistencies);
        //Calculate base metrics
        int n = databases.size();
        double totalNumberOfPairOfMicroservice = ((n * (n+1))/2) - n; //Not count duplicate (A,B) (B,A) and compare with themselves (A,A)
        double totalNumberOfMicroserviceWithSharedDB = context.getSharedPersistencies().size();  //Eg. [microA, microB, microC] --> PairOfShared = 3
        double totalNumberOfSharedDB = ((totalNumberOfMicroserviceWithSharedDB * (totalNumberOfMicroserviceWithSharedDB + 1))/2) - totalNumberOfMicroserviceWithSharedDB;
        double ratioOfSharedDatabases = 0;
        if (totalNumberOfPairOfMicroservice !=0) {
            ratioOfSharedDatabases = totalNumberOfSharedDB/totalNumberOfPairOfMicroservice;
        }
        context.setRatioOfSharedDatabases(ratioOfSharedDatabases);

        return context;
    }

    public Map<String, DatabaseInstance> getModulePersistencies(RequestContext request){
        List<String> resourcePaths = resourceService.getResourcePaths(request.getPathToCompiledMicroservices());
        Map<String, DatabaseInstance> databases = new HashMap<>();
        for (String path : resourcePaths) {
            Map<String, Map<String, Object>> yamls = resourceService.getYamls(path, request.getOrganizationPath());
            if (yamls.size() > 0) {
                for(Map.Entry<String, Map<String, Object>> entry : yamls.entrySet()){
                    Map<String, Object> yamlProperties = new HashMap<>();
                    forEachValue(entry.getValue(), "", yamlProperties::put);

                    if(yamlProperties.get("spring.data.mongodb.host") != null){
                        DatabaseInstance databaseInstance = new DatabaseInstance(
                                yamlProperties.get("spring.data.mongodb.database"),
                                yamlProperties.get("spring.data.mongodb.port"),
                                yamlProperties.get("spring.data.mongodb.host"),
                                DatabaseType.MONGO
                        );
                        databases.put(entry.getKey(), databaseInstance);
                    }

                    if(yamlProperties.get("spring.datasource.url") != null){
                        DatabaseInstance databaseInstance = new DatabaseInstance(
                                yamlProperties.get("spring.datasource.database"),
                                yamlProperties.get("spring.datasource.port"),
                                yamlProperties.get("spring.datasource.url"),
                                ((String) yamlProperties.get("spring.datasource.url")).contains("mysql") ? DatabaseType.MYSQL : DatabaseType.GENERIC
                        );
                        databases.put(entry.getKey(), databaseInstance);
                    }

                }
            }
        }
        return databases;
    }

    private static void forEachValue(Map<String, Object> source, String base, BiConsumer<? super String, ? super Object> action) {
        if(source != null) {
            for (final Map.Entry<String, Object> entry : source.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    forEachValue((Map<String, Object>) entry.getValue(), base.concat(".").concat(entry.getKey()), action);
                } else {
                    action.accept(base.concat(".").concat(entry.getKey()).substring(1), entry.getValue());
                }
            }
        }

    }
}
