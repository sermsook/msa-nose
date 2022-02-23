package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.msanose.model.context.WrongCutsContext;
import edu.baylor.ecs.msanose.model.wrongCuts.EntityPair;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.service.ResourceService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class WrongCutsService {

    private final ResourceService resourceService;
    private final EntityService entityService;

    public WrongCutsContext getWrongCuts(RequestContext request){
        WrongCutsContext wrongCutsContext = new WrongCutsContext();
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

        List<EntityPair> possibleWrongCuts = new ArrayList<>();
        for(EntityPair pair : sorted){
            double d = pair.getEntityCount() - avgEntityCount;
            if(d > (2 * std) || d < -(2 * std)){
                possibleWrongCuts.add(pair);
            }
        }

        wrongCutsContext.setEntityCounts(sorted);
        wrongCutsContext.setPossibleWrongCuts(possibleWrongCuts);

        //Calculate base metrics
        double totalNumberOfMicroserviceInSystems = jars.size();
        double totalNumberOfPossibleWrongCuts = wrongCutsContext.getPossibleWrongCuts().size();
        double ratioOfWrongCuts = 0;
        if (totalNumberOfMicroserviceInSystems !=0) {
            ratioOfWrongCuts = totalNumberOfPossibleWrongCuts/totalNumberOfMicroserviceInSystems;
        }
        wrongCutsContext.setRatioOfWrongCuts(ratioOfWrongCuts);

        return wrongCutsContext;
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
