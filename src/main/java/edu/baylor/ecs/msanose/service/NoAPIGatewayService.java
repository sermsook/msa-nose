package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.jparser.component.Component;
import edu.baylor.ecs.jparser.component.context.AnalysisContext;
import edu.baylor.ecs.jparser.component.impl.AnnotationComponent;
import edu.baylor.ecs.jparser.component.impl.ClassComponent;
import edu.baylor.ecs.rad.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NoAPIGatewayService {
    public boolean findApiGateway(RequestContext request){
        List<String> apiGatewayClassPaths = new ArrayList<>();
        AtomicBoolean isFoundApiGatewayAnnotation = new AtomicBoolean(false);

        AnalysisContext analysisContext = JParserService.createContextFromPath(request.getPathToCompiledMicroservices());
        List<ClassComponent> classes = analysisContext.getClasses();
        classes.forEach(clazz -> {
            List<AnnotationComponent> annotationComponents = clazz.getAnnotations().stream().map(Component::asAnnotationComponent).collect(Collectors.toList());
            for(AnnotationComponent annotationComponent : annotationComponents) {
                String annotation = annotationComponent.getAsString();
                if (annotation.matches("@EnableZuulProxy")) {
                    log.info("package name: " + clazz.getPackageName() + ", class name: " + clazz.getClassName()+ " is found @EnableZuulProxy");
                    apiGatewayClassPaths.add(clazz.getClassName());
                    isFoundApiGatewayAnnotation.set(true);
                    break;
                }
            }
        });

        return isFoundApiGatewayAnnotation.get();
    }
}
