package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.msanose.model.context.ESBContext;
import edu.baylor.ecs.msanose.model.context.MicroserviceContext;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.context.ResponseContext;
import edu.baylor.ecs.rad.model.RestEntity;
import edu.baylor.ecs.rad.model.RestFlow;
import edu.baylor.ecs.rad.service.ResourceService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Data
@AllArgsConstructor
public class ESBService {

    private RestService restDiscoveryService;
    private final ResourceService resourceService;

    public ESBContext getESBContext(RequestContext request){
        ESBContext esbContext = new ESBContext();

        // Get all connections
        ResponseContext responseContext = restDiscoveryService.generateResponseContext(request);

        // Count all incoming for each microservice
        Map<String, Integer> incoming = new HashMap<>();
        Map<String, Integer> outgoing = new HashMap<>();
        for(RestFlow flow : responseContext.getRestFlowContext().getRestFlows()){
            Integer out = outgoing.getOrDefault(flow.getResourcePath(), 0);
            outgoing.put(flow.getResourcePath(), ++out);

            for(RestEntity entity : flow.getServers()){
                Integer in = incoming.getOrDefault(entity.getResourcePath(), 0);
                incoming.put(entity.getResourcePath(), ++in);
            }
        }

        List<ServerPair> sortedIncoming = incoming.entrySet()
                .stream()
                .map(x -> new ServerPair(x.getKey(), x.getValue()))
                .sorted(Comparator.comparing(ServerPair::getEdges))
                .collect(Collectors.toList());

        List<ServerPair> sortedOutgoing = outgoing.entrySet()
                .stream()
                .map(x -> new ServerPair(x.getKey(), x.getValue()))
                .sorted(Comparator.comparing(ServerPair::getEdges))
                .collect(Collectors.toList());

        // If outlier, then ESB
        double avgStepOut = 0.0;
        for(int i = 0; i < sortedOutgoing.size() - 1; i++){
            int outA = sortedOutgoing.get(i).getEdges();
            int outB = sortedOutgoing.get(i + 1).getEdges();
            avgStepOut += (outB - outA);
        }
        avgStepOut = avgStepOut / (sortedOutgoing.size() - 1);
        log.info("sd is " + avgStepOut + "  ---> 2sd is " + (2*avgStepOut));

        List<ServerPair> possibleESBOut = new ArrayList<>();
        for(int i = 0; i < sortedOutgoing.size() - 1; i++){
            int outA = sortedOutgoing.get(i).getEdges();
            int outB = sortedOutgoing.get(i + 1).getEdges();

            if(( outB - outA) > (2 * avgStepOut)){
                possibleESBOut.add(sortedOutgoing.get(i + 1));
            }
        }

        double avgStepIn = 0.0;
        for(int i = 0; i < sortedIncoming.size() - 1; i++){
            int inA = sortedIncoming.get(i).getEdges();
            int inB = sortedIncoming.get(i + 1).getEdges();
            avgStepIn += (inB - inA);
        }
        avgStepIn = avgStepIn / (sortedIncoming.size() - 1);
        List<ServerPair> possibleESBIn = new ArrayList<>();
        for(int i = 0; i < sortedIncoming.size() - 1; i++){
            int inA = sortedIncoming.get(i).getEdges();
            int inB = sortedIncoming.get(i + 1).getEdges();
            if(( inB - inA) > (2 * avgStepIn)){
                possibleESBIn.add(sortedIncoming.get(i + 1));
            }
        }

        for(ServerPair pairOut : possibleESBOut){
            for(ServerPair pairIn : possibleESBIn){
                if(pairIn.getPath().equals(pairOut.getPath())){
                    esbContext.getCandidateESBs().add(new MicroserviceContext(pairIn.getPath()));
                }
            }
        }

        List<String> jars = resourceService.getResourcePaths(request.getPathToCompiledMicroservices());

        //Calculate base metrics
        double totalNumberOfMicroserviceInSystems = jars.size();
        double totalNumberOfCandidateESBs = esbContext.getCandidateESBs().size();
        double ratioOfESBMicroservices = 0;
        if (totalNumberOfMicroserviceInSystems !=0) {
            ratioOfESBMicroservices = totalNumberOfCandidateESBs/totalNumberOfMicroserviceInSystems;
        }

        esbContext.setRatioOfESBMicroservices(ratioOfESBMicroservices);

        return esbContext;
    }

    @Data
    @AllArgsConstructor
    private static class ServerPair {
        private String path;
        private int edges;
    }
}
