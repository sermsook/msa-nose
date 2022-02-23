package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.msanose.model.context.MicroservicesGreedyContext;
import edu.baylor.ecs.msanose.model.greedy.MicroserviceMetric;
import edu.baylor.ecs.msanose.model.wrongCuts.EntityPair;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.service.ResourceService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class GreedyService {

    private final ResourceService resourceService;
    private final EntityService entityService;

    public MicroservicesGreedyContext getGreedyMicroservices(RequestContext request){
        MicroservicesGreedyContext microservicesGreedyContext = new MicroservicesGreedyContext();
        Map<String, Integer> counts = new HashMap<>();

        List<String> jars = resourceService.getResourcePaths(request.getPathToCompiledMicroservices());

        for(String jar : jars){
            List<String> entities = entityService.getEntitiesPerJar(request, jar);
            counts.put(extractName(jar), entities.size());
        }

        List<EntityPair> sorted = counts.entrySet()
                .stream()
                .map(x -> new EntityPair(x.getKey(), x.getValue()))
                .sorted(Comparator.comparing(EntityPair::getEntityCount))
                .collect(Collectors.toList());

        List<EntityPair> trimmed = sorted
                .stream()
                .filter(x -> x.getEntityCount() > 1)
                .sorted(Comparator.comparing(EntityPair::getEntityCount))
                .collect(Collectors.toList());

        double avgEntityCount = 0;
        for (EntityPair pair : trimmed) {
            avgEntityCount += pair.getEntityCount();
        }

        avgEntityCount = avgEntityCount / trimmed.size();

        double numerator = 0.0;
        for (EntityPair pair : trimmed) {
            numerator += Math.pow(pair.getEntityCount() - avgEntityCount, 2);
        }

        double std = Math.sqrt(numerator / (trimmed.size() - 1));

        List<EntityPair> possibleNanoMicroservices = new ArrayList<>();
        for(EntityPair pair : sorted){
            MicroserviceMetric microserviceMetric = new MicroserviceMetric();
            microserviceMetric.setPath(pair.getPath());
            microserviceMetric.setEntityFileCount(pair.getEntityCount());
            double d = pair.getEntityCount() - avgEntityCount;

            if(d < -(2 * std)){
                microservicesGreedyContext.addGreedyMicroservice(microserviceMetric);
                possibleNanoMicroservices.add(pair);
            }
        }

        //Calculate base metrics
        double totalNumberOfMicroserviceInSystems = jars.size();
        double totalNumberOfNanoMicroservices = possibleNanoMicroservices.size();
        double ratioOfNanoMicroservices = 0;
        if (totalNumberOfMicroserviceInSystems !=0) {
            ratioOfNanoMicroservices = totalNumberOfNanoMicroservices/totalNumberOfMicroserviceInSystems;
        }

        microservicesGreedyContext.setRatioOfNanoMicroservices(ratioOfNanoMicroservices);
        return microservicesGreedyContext;
    }

    public void getStaticFiles(String path, List<File> files){
        File directory = new File(path);

        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if(fList != null)
            for (File file : fList) {
                if (file.isFile()) {
                    if(file.getName().contains(".js") || file.getName().contains(".html") || file.getName().contains(".css") || file.getName().contains(".tsx"))
                        files.add(file);
                } else if (file.isDirectory()) {
                    getStaticFiles(file.getAbsolutePath(), files);
                }
            }
    }

    private String extractName(String path){
        int begin = path.lastIndexOf("/");
        int end = path.lastIndexOf('.');
        if(begin == -1 || end == -1){
            return path;
        }
        return path.substring(begin, end);
    }

}
