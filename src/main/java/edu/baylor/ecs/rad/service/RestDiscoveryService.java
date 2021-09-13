package edu.baylor.ecs.rad.service;

import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.context.ResponseContext;
import edu.baylor.ecs.rad.context.RestEntityContext;
import edu.baylor.ecs.rad.context.RestFlowContext;
import edu.baylor.ecs.rad.graph.GVGenerator;
import javassist.CtClass;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * This class constructs a {@link ResponseContext}.
 * It takes a {@link RequestContext} as input.
 *
 * @author Dipta Das
 */

@AllArgsConstructor
@Service
public class RestDiscoveryService {

    @Autowired
    private final ResourceService resourceService;

    @Autowired
    private final RestEntityService restEntityService;

    @Autowired
    private final RestFlowService restFlowService;

    public ResponseContext generateResponseContext(RequestContext request) {
        ResponseContext responseContext = new ResponseContext();
        responseContext.setRequest(request);

        List<String> resourcePaths = resourceService.getResourcePaths(request.getPathToCompiledMicroservices());  //return list of jar/war file
        for (String path : resourcePaths) {  //path = each jar file
            List<CtClass> ctClasses = resourceService.getCtClasses(path, request.getOrganizationPath());  //return list of objects (all class) loaded from a particular JAR file

            Set<Properties> propertiesSet = resourceService.getProperties(path, request.getOrganizationPath()); //get properties ทั้งหมดจาก application.properties (ของแต่ละ jar file)
            Properties properties;
            if (propertiesSet.size() > 0) {
                properties = propertiesSet.iterator().next();
            } else properties = null;

            // print the properties for debug
            // Helper.dumpProperties(properties, path);

            RestEntityContext restEntityContext = restEntityService.getRestEntityContext(ctClasses, path, null, properties); //ได้ list ของ restEntityContext ของrest client ทั้งหมด (พยายามปั้น full url ของ rest client ขึ้นมา)
            responseContext.getRestEntityContexts().add(restEntityContext);
        }

        RestFlowContext restFlowContext = restFlowService.getRestFlowContext(responseContext.getRestEntityContexts());
        responseContext.setRestFlowContext(restFlowContext);

        if (!request.getOutputPath().isEmpty()) {
            GVGenerator.generate(responseContext);
        }

        return responseContext;
    }

    // rest entities for single jar
    public RestEntityContext generateRestEntityContext(RequestContext request, String serviceDNS) {
        List<CtClass> ctClasses = resourceService.getCtClasses(request.getPathToCompiledMicroservices(), request.getOrganizationPath());

        Set<Properties> propertiesSet = resourceService.getProperties(request.getPathToCompiledMicroservices(), request.getOrganizationPath());
        Properties properties;
        if (propertiesSet.size() > 0) {
            properties = propertiesSet.iterator().next();
        } else properties = null;

        return restEntityService.getRestEntityContext(ctClasses, request.getPathToCompiledMicroservices(), serviceDNS, properties);
    }
}
