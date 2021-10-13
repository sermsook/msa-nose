package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.msanose.model.Pair;
import edu.baylor.ecs.msanose.model.context.CyclicDependencyContext;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.context.ResponseContext;
import edu.baylor.ecs.rad.context.RestEntityContext;
import edu.baylor.ecs.rad.context.RestFlowContext;
import edu.baylor.ecs.rad.model.RestEntity;
import edu.baylor.ecs.rad.model.RestFlow;
import edu.baylor.ecs.rad.service.RestDiscoveryService;
import lombok.AllArgsConstructor;
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
//            //Dao's logic for store existing service call
//            if (visited[i]) {
//                Pair visitedFlow = new Pair<>(i, c);
//                if (!context.getPairs().contains(visitedFlow)) {
//                    context.addPair(visitedFlow);
//                }else {
//                    continue;
//                }
//            }

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
                log.info("There are cyclic dependency.");
                continue;
            }
        }

        return context;
    }

    public CyclicDependencyContext getCyclicDependencies(RequestContext request){
        ResponseContext responseContext = restDiscoveryService.generateResponseContext(request); //หา RestEntity ทั้งหมดของทุก rest client ของทุก jar file โดยดูจาก annotaion, พยายามปั้น full url ของ rest client ขึ้นมา

        // Map all entities to indexes
        int ndx = 0;  //Dao's note: ndx is number of microservices in system
        for(RestEntityContext entity : responseContext.getRestEntityContexts()){
            //Dao's note: vertexMap is <path of microservice, index>
            vertexMap.put(entity.getResourcePath(), ndx);   //<node, index>
            ndx++;
        }
        log.info("All microservice in system: " +vertexMap);
        // Construct edges in adjacency list
        V = responseContext.getRestEntityContexts().size();
        adjList = new LinkedList<>();

        for(int i = 0; i < V; i++){  //สร้าง adjacency list ขนาดเท่ากับ #RestEntity ทั้งหมด ทุก jar file
            adjList.add(new LinkedList<>());
        }

        /**
         * RestFlow = {
         *  resourcePath=/Users/sermsook.pul/Documents/Me/Master-Project/MSANose/ex-microservice/train-ticket/ts-admin-user-service/target/ts-admin-user-service-1.0.jar,
         *  className=adminuser.service.AdminUserServiceImpl,
         *  methodName=getAllUsers,
         *  servers=[
         *      RestEntity= {
         *          isClient=false,
         *          url=/api/v1/userservice/users,
         *          applicationName=null,
         *          ribbonServerName=null,
         *          resourcePath=/Users/sermsook.pul/Documents/Me/Master-Project/MSANose/ex-microservice/train-ticket/ts-user-service/target/ts-user-service-1.0.jar,
         *          className=user.controller.UserController,
         *          methodName=getAllUser, returnType=org.springframework.http.ResponseEntity,
         *          path=/api/v1/userservice/users, httpMethod=GET, pathParams=null, queryParams=null, consumeType=null,
         *          produceType=null
         *      }
         *   ]
         * }
         */
        for(RestFlow flow : responseContext.getRestFlowContext().getRestFlows()){  //RestFlows= [A->B->C, C->D, E->F] , flow = each call chain eg. A->B->C
            int keyA = vertexMap.get(flow.getResourcePath()); //eg. keyA = index ของ /train-ticket/ts-admin-user-service/target/ts-admin-user-service-1.0.jar             //flow.getResourcePath() = client.ResourcePath()

            for(RestEntity server : flow.getServers()){
                int keyB = vertexMap.get(server.getResourcePath());
                adjList.get(keyA).add(keyB);   //สร้าง graph ว่า a call ไปหา b  (index ของ b)
                log.info("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                log.info("index: " + keyA + ", start node: " + flow.getResourcePath());
                log.info("index: " + keyB + ", end node: "+server.getResourcePath());
            }
        }

        CyclicDependencyContext context = isCyclic();  //เอา adjList ไป  check ว่ามี Cyclic ไหม
        double totalNumberOfServiceCall = context.getTotalServiceCall();
        double ratioOfCyclicDependency = 0.0;
        if(totalNumberOfServiceCall != 0) {
            ratioOfCyclicDependency = context.getTotalCyclicCall()/totalNumberOfServiceCall;
        }

        context.setRatioOfCyclicDependency(ratioOfCyclicDependency);
        log.info("****** Cyclic Dependency ******");
        log.info("totalNumberOfServiceCall: " +totalNumberOfServiceCall);
        log.info("totalNumberOfCyclicServiceCall: " +context.getTotalCyclicCall());
        log.info("ratioOfCyclicDependency: " +ratioOfCyclicDependency);
        log.info("====================================================");
        return context;
    }
}
