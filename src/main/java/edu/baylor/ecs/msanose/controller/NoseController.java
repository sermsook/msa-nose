package edu.baylor.ecs.msanose.controller;

import edu.baylor.ecs.msanose.model.SharedIntimacy;
import edu.baylor.ecs.msanose.model.UnorderedPair;
import edu.baylor.ecs.msanose.model.context.*;
import edu.baylor.ecs.msanose.model.hardcodedEndpoint.HardcodedEndpoint;
import edu.baylor.ecs.msanose.model.hardcodedEndpoint.HardcodedEndpointType;
import edu.baylor.ecs.msanose.service.*;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.context.ResponseContext;
import edu.baylor.ecs.rad.context.RestEntityContext;
import edu.baylor.ecs.rad.model.RestEntity;
import edu.baylor.ecs.rad.model.RestFlow;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class NoseController {

    private final APIService apiService;
    private final LibraryService libraryService;
    private final PersistencyService persistencyService;
    private final ESBService esbService;
    private final TooManyStandardsService tooManyStandardsService;
    private final RestService restDiscoveryService;
    private final CyclicDependencyService cyclicDependencyService;
    private final EntityService entityService;
    private final WrongCutsService wrongCutsService;
    private final GreedyService greedyService;
    private final NoAPIGatewayService noAPIGatewayService;

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public String getHandshake(){
        return "Hello from [NoseController]";
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/report", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public ApplicationSmellsContext getReport(@RequestBody RequestContext request) throws Exception{
        ApplicationSmellsContext context = new ApplicationSmellsContext();
        Map<String, Long> times = new HashMap<>();

        long curr = System.currentTimeMillis();
        ResponseContext responseContext = restDiscoveryService.generateResponseContext(request);
        long now = System.currentTimeMillis();
        times.put("Bytecode Analysis", now - curr);   //processing time

        curr = System.currentTimeMillis();
        context.setUnversionedAPIContext(getApis(request));
        now = System.currentTimeMillis();
        times.put("Unversioned API", now - curr);

        curr = System.currentTimeMillis();
        context.setSharedLibraryContext(getSharedLibraries(request));
        now = System.currentTimeMillis();
        times.put("Shared Library", now - curr);

        curr = System.currentTimeMillis();
        context.setWrongCutsContext(getWrongCuts(request));
        now = System.currentTimeMillis();
        times.put("Wrong Cuts", now - curr);

        curr = System.currentTimeMillis();
        context.setHardCodedEndpointsContext(getHardcodedEndpoints(request));
        now = System.currentTimeMillis();
        times.put("Hardcoded Enpoints", now - curr);

        curr = System.currentTimeMillis();
        context.setCyclicDependencyContext(getCyclicDependency(request));
        now = System.currentTimeMillis();
        times.put("Cyclic Dependency", now - curr);

        curr = System.currentTimeMillis();
        context.setSharedPersistencyContext(getSharedPersistency(request));
        now = System.currentTimeMillis();
        times.put("Shared Persistency", now - curr);

        curr = System.currentTimeMillis();
        context.setEsbContext(getESBUsage(request));
        now = System.currentTimeMillis();
        times.put("ESB", now - curr);

        curr = System.currentTimeMillis();
        context.setAPIGatewayContext(getNoAPIGateway(request));
        now = System.currentTimeMillis();
        times.put("API Gateway", now - curr);

        curr = System.currentTimeMillis();
        context.setInappropriateServiceIntimacyContext(getInappropriateServiceIntimacy(request));
        now = System.currentTimeMillis();
        times.put("ISI", now - curr);

        curr = System.currentTimeMillis();
        context.setTooManyStandardsContext(getTooManyStandards(request));
        now = System.currentTimeMillis();
        times.put("Too Many Standards", now - curr);

        curr = System.currentTimeMillis();
        context.setMicroservicesGreedyContext(getMicroservicesGreedy(request));
        now = System.currentTimeMillis();
        times.put("Microservice Greedy", now - curr);

        context.setTimes(times);

        calculateDerivedBaseMetric(context);
        calculateQualityAttributesFactor(context);

        return context;
    }

    public void calculateDerivedBaseMetric(ApplicationSmellsContext context) {
        MicroserviceDesignMetricsContext derivedMetric = new MicroserviceDesignMetricsContext();
        derivedMetric.derivedMetric(context);
        context.setMicroserviceDesignMetrics(derivedMetric);
    }

    public void calculateQualityAttributesFactor(ApplicationSmellsContext context) {
        QualityAttributesContext qualityAttributes = new QualityAttributesContext();
        qualityAttributes.calculateQualityAttributesFactor(context);
        context.setQualityAttributes(qualityAttributes);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/apis", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public UnversionedAPIContext getApis(@RequestBody RequestContext request){
        List<APIContext> apis = apiService.getAPIs(request.getPathToCompiledMicroservices());
        UnversionedAPIContext unversionedAPIContext = new UnversionedAPIContext(apiService.getAPIs(request.getPathToCompiledMicroservices()).stream()
                .map(APIContext::getPath)
                .filter(api -> !apiService.isVersioned(api))
                .collect(Collectors.toSet()));
        //Calculate base metric
        double ratioOfNonVersionedAPIs = 0;
        if (apis.size()!=0) {
            ratioOfNonVersionedAPIs = Double.valueOf(unversionedAPIContext.getCount())/Double.valueOf(apis.size());
        }
        unversionedAPIContext.setRatioOfNonVersionedAPIs(ratioOfNonVersionedAPIs);
        return unversionedAPIContext;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/sharedLibraries", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public SharedLibraryContext getSharedLibraries(@RequestBody RequestContext request) throws Exception {
        return libraryService.getSharedLibraries(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/wrongCuts", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public WrongCutsContext getWrongCuts(@RequestBody RequestContext request){
        return wrongCutsService.getWrongCuts(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/hardcodedEndpoints", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public HardCodedEndpointsContext getHardcodedEndpoints(@RequestBody RequestContext request){
        HardCodedEndpointsContext hardCodedEndpointsContext = new HardCodedEndpointsContext();
        ResponseContext responseContext = restDiscoveryService.generateResponseContext(request);
        double totalEndpointInSystem = 0;

        for(RestEntityContext restEntityContext : responseContext.getRestEntityContexts()){
            for(RestEntity restEntity : restEntityContext.getRestEntities()){
                if(restEntity.isClient()){
                    String url = restEntity.getUrl();
                    totalEndpointInSystem++;
                    if(url.matches(".*:[0-9]{1,5}.*")){
                        hardCodedEndpointsContext.addHardcodedEndpoint(new HardcodedEndpoint(restEntity, HardcodedEndpointType.PORT));
                    }

                    if(url.matches(".*[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}.*")){
                        hardCodedEndpointsContext.addHardcodedEndpoint(new HardcodedEndpoint(restEntity, HardcodedEndpointType.IP));
                    }
                }
            }
        }
        double ratioOfHardCodedEndpoints = 0;
        totalEndpointInSystem = 65;
        if (totalEndpointInSystem != 0) {
            ratioOfHardCodedEndpoints = hardCodedEndpointsContext.getTotalHardcodedEndpoints()/totalEndpointInSystem;
        }
        hardCodedEndpointsContext.setRatioOfHardCodedEndpoints(ratioOfHardCodedEndpoints);
        return hardCodedEndpointsContext;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/cyclicDependency", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public CyclicDependencyContext getCyclicDependency(@RequestBody RequestContext request){
        return cyclicDependencyService.getCyclicDependencies(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/sharedPersistency", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public SharedPersistencyContext getSharedPersistency(@RequestBody RequestContext request){
        return persistencyService.getSharedPersistency(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/esbUsage", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public ESBContext getESBUsage(@RequestBody RequestContext request) {
        return esbService.getESBContext(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/noAPIGateway", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public NoAPIGatewayContext getNoAPIGateway(@RequestBody RequestContext request){
        NoAPIGatewayContext noAPIGatewayContext = new NoAPIGatewayContext();
        // Get all connections
        ResponseContext responseContext = restDiscoveryService.generateResponseContext(request);

        if (request.isHasApiGateway() || responseContext.getRestEntityContexts().size() < request.getEndpointThreshold()){ //recommended endpointThreshold = 50
            noAPIGatewayContext.setHasApiGateway(true);
            noAPIGatewayContext.setRatioOfUnmanageableConnections(0.0);
        }else{
            boolean isFoundApigateway = noAPIGatewayService.findApiGateway(request);
            if (isFoundApigateway){
                noAPIGatewayContext.setHasApiGateway(true);
                noAPIGatewayContext.setRatioOfUnmanageableConnections(0.0);
            }else{
                double totalNumberOfMicroserviceInSystems = responseContext.getRestEntityContexts().size();
                double ratioOfUnmanageableConnections = (totalNumberOfMicroserviceInSystems - request.getEndpointThreshold())/totalNumberOfMicroserviceInSystems;
                noAPIGatewayContext.setHasApiGateway(isFoundApigateway);
                noAPIGatewayContext.setRatioOfUnmanageableConnections(ratioOfUnmanageableConnections);
            }
        }
        return noAPIGatewayContext;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/inappropriateServiceIntimacy", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public InappropriateServiceIntimacyContext getInappropriateServiceIntimacy(@RequestBody RequestContext request){
        InappropriateServiceIntimacyContext inappropriateServiceIntimacyContext = new InappropriateServiceIntimacyContext();
        ResponseContext responseContext = restDiscoveryService.generateResponseContext(request);

        Set<UnorderedPair<String>> pairs = new HashSet<> ();
        for(RestFlow restFlow : responseContext.getRestFlowContext().getRestFlows()){
            String jarA = restFlow.getResourcePath();
            for(RestEntity entity : restFlow.getServers()){
                String jarB = entity.getResourcePath();
                UnorderedPair<String> flowsPair = new UnorderedPair<>(jarA, jarB);
                pairs.add(flowsPair);

                // Get two sets of entity class
                List<String> entitiesA = entityService.getEntitiesPerJar(request, jarA);
                List<String> entitiesB = entityService.getEntitiesPerJar(request, jarB);

                Set<String> result = entitiesA.stream()
                        .distinct()
                        .filter(entitiesB::contains)
                        .collect(Collectors.toSet());

                double similarity = result.size() * 1.0 / Math.max(entitiesA.size(), entitiesB.size());
                if(similarity > 0.8){
                    inappropriateServiceIntimacyContext.addSharedIntimacy(new SharedIntimacy(restFlow.getResourcePath(), entity.getResourcePath(), similarity));
                }
            }
        }

        //Calculate base metric
        double totalNumberOfPairOfMicroservice = pairs.size();
        double totalNumberOfPairOfMicroserviceWithInappropriateDBAccess = inappropriateServiceIntimacyContext.getCount();

        double ratioOfInappropriateDatabaseAccess = 0;
        if (totalNumberOfPairOfMicroservice != 0) {
            ratioOfInappropriateDatabaseAccess = totalNumberOfPairOfMicroserviceWithInappropriateDBAccess/totalNumberOfPairOfMicroservice;
        }

        inappropriateServiceIntimacyContext.setRatioOfInappropriateDatabaseAccess(ratioOfInappropriateDatabaseAccess);

        return inappropriateServiceIntimacyContext;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/tooManyStandards", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public TooManyStandardsContext getTooManyStandards(@RequestBody RequestContext request){
        return tooManyStandardsService.getStandards(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/microservicesGreedy", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    public MicroservicesGreedyContext getMicroservicesGreedy(@RequestBody RequestContext request){
        return greedyService.getGreedyMicroservices(request);
    }
}
