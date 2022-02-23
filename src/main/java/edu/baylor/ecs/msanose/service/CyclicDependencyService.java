package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.msanose.model.context.CyclicDependencyContext;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.context.ResponseContext;
import edu.baylor.ecs.rad.context.RestEntityContext;
import edu.baylor.ecs.rad.model.RestEntity;
import edu.baylor.ecs.rad.model.RestFlow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class CyclicDependencyService {

    private final RestService restDiscoveryService;

    private Map<String, Integer> vertexMap = new HashMap<>();
    private List<List<Integer>> adjList;
    private int V;

    public CyclicDependencyService(RestService restDiscoveryService){
        this.restDiscoveryService = restDiscoveryService;
    }

    // This function is a variation of DFSUtil() in
    // https://www.geeksforgeeks.org/archives/18212
    private boolean isCyclicUtil(int i, boolean[] visited, boolean[] recStack, CyclicDependencyContext context ) {

        // Mark the current node as visited and
        // part of recursion stack
        if (recStack[i])
            return true;

        if (visited[i])
            return false;

        visited[i] = true;

        recStack[i] = true;
        List<Integer> children = adjList.get(i);
        double totalCyclicCalls = context.getTotalCyclicCall();
        double totalServiceCalls = context.getTotalServiceCall();
        totalServiceCalls += children.size();
        context.setTotalServiceCall(totalServiceCalls);

        for (Integer c: children) {
            if (isCyclicUtil(c, visited, recStack, context)) {
                totalCyclicCalls++;
                context.setTotalCyclicCall(totalCyclicCalls);
                continue;
            }
        }

        recStack[i] = false;

        return false;
    }

    // Returns true if the graph contains a
    // cycle, else false.
    // This function is a variation of DFS() in
    // https://www.geeksforgeeks.org/archives/18212
    private CyclicDependencyContext isCyclic() {
        CyclicDependencyContext context = new CyclicDependencyContext();
        context.setTotalCyclicCall(0.0);
        context.setTotalServiceCall(0.0);

        // Mark all the vertices as not visited and
        // not part of recursion stack
        boolean[] visited = new boolean[V];
        boolean[] recStack = new boolean[V];

        // Call the recursive helper function to
        // detect cycle in different DFS trees
        for (int i = 0; i < V; i++) {
            if (isCyclicUtil(i, visited, recStack, context)) {
                continue;
            }
        }

        return context;
    }

    public CyclicDependencyContext getCyclicDependencies(RequestContext request){
        ResponseContext responseContext = restDiscoveryService.generateResponseContext(request);

        // Map all entities to indexes
        int ndx = 0;
        for(RestEntityContext entity : responseContext.getRestEntityContexts()){
            vertexMap.put(entity.getResourcePath(), ndx);
            ndx++;
        }

        // Construct edges in adjacency list
        V = responseContext.getRestEntityContexts().size();
        adjList = new LinkedList<>();

        for(int i = 0; i < V; i++){
            adjList.add(new LinkedList<>());
        }

        for(RestFlow flow : responseContext.getRestFlowContext().getRestFlows()){
            int keyA = vertexMap.get(flow.getResourcePath());

            for(RestEntity server : flow.getServers()){
                int keyB = vertexMap.get(server.getResourcePath());
                adjList.get(keyA).add(keyB);
            }
        }

        CyclicDependencyContext context = isCyclic();
        double totalNumberOfServiceCall = context.getTotalServiceCall();
        double ratioOfCyclicDependency = 0.0;
        if(totalNumberOfServiceCall != 0) {
            ratioOfCyclicDependency = context.getTotalCyclicCall()/totalNumberOfServiceCall;
        }

        context.setRatioOfCyclicDependency(ratioOfCyclicDependency);
        return context;
    }
}
