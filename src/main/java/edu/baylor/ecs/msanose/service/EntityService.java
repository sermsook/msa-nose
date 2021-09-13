package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.jparser.component.Component;
import edu.baylor.ecs.jparser.component.context.AnalysisContext;
import edu.baylor.ecs.jparser.component.impl.AnnotationComponent;
import edu.baylor.ecs.jparser.component.impl.ClassComponent;
import edu.baylor.ecs.jparser.component.impl.MethodInfoComponent;
import edu.baylor.ecs.jparser.model.AnnotationValuePair;
import edu.baylor.ecs.msanose.model.context.APIContext;
import edu.baylor.ecs.rad.context.RequestContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EntityService {

    public List<String> getAllEntities(RequestContext request){

        List<String> entities = new ArrayList<>();

        AnalysisContext analysisContext = JParserService.createContextFromPath(request.getPathToCompiledMicroservices());
        List<ClassComponent> classes = analysisContext.getClasses();
        classes.forEach(clazz -> {
            List<AnnotationComponent> annotationComponents = clazz.getAnnotations().stream().map(Component::asAnnotationComponent).collect(Collectors.toList());
            for(AnnotationComponent annotationComponent : annotationComponents) {
                String annotation = annotationComponent.getAsString();
                if (annotation.matches("@Entity")) {
                    // Entity
                    entities.add(clazz.getClassName());
                }
            }
        });

        return entities;
    }

    public List<String> getEntitiesPerJar(RequestContext request, String path){

        Set<String> entities = new HashSet<>();

        String basePath = request.getPathToCompiledMicroservices();   //ex.  /User/sermsook.pul/acm-core-service-2

        // convert windows path to linux style
        basePath = basePath.replace("\\\\", "/");

        if(!basePath.endsWith("/")){
            basePath = basePath.concat("/");
        }

        String remainder = path.substring(basePath.length());      //ex.     core-service-api/target/core-service-api-4.0.27.jar
        String folder = remainder.substring(0, remainder.indexOf('/'));    //ex.  core-service-api

        String newPath = basePath.concat(folder).concat("/");      //ex.   /User/sermsook.pul/acm-core-service-2/core-service-api

        AnalysisContext analysisContext = JParserService.createContextFromPath(newPath);

        List<ClassComponent> classes = analysisContext.getClasses();   //get all class in this jar
        /*
        classes ex.
        ------------------- index 0 ------------------------
        ClassComponent(compilationUnit=package com.tmn.core.util;

        import java.text.ParseException;
        import java.text.SimpleDateFormat;
        import java.util.Date;

        public class DateRFC3339JsonSerializer {

            public static Date serializeeRFC3339Date(String dateString) throws ParseException {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                return sdf.parse(dateString);
            }
        }
        , constructors=[], fieldComponents=[])

        ------------------- index 1 ------------------------
        ClassComponent(compilationUnit=package com.tmn.core.api.message;

        public class CancelDeductRequest extends StandardAdminRequest {

            private String targetTransactionId;

            public String getTargetTransactionId() {
                return targetTransactionId;
            }

            public void setTargetTransactionId(String targetTransactionId) {
                this.targetTransactionId = targetTransactionId;
            }
        }
        , constructors=[], fieldComponents=[FieldComponent(annotations=[], variables=null, fieldName=targetTransactionId, accessor=PRIVATE, staticField=false, finalField=false, stringifiedDefaultValue=null, type=String)])
         */



        classes.forEach(clazz -> {

            if(clazz.getPackageName().contains("entity")){
                entities.add(clazz.getClassName());
            }

            List<AnnotationComponent> annotationComponents = clazz.getAnnotations().stream().map(Component::asAnnotationComponent).collect(Collectors.toList());
            for(AnnotationComponent annotationComponent : annotationComponents) {
                String annotation = annotationComponent.getAsString();
                if (annotation.matches("@Entity") || annotation.matches("@Document")) {
                    // Entity
                    entities.add(clazz.getClassName());
                }
            }
        });

        return new ArrayList<>(entities);
    }

    public List<String> getEntitiesPerFolder(String path){

        Set<String> entities = new HashSet<>();
        AnalysisContext analysisContext = JParserService.createContextFromPath(path);
        List<ClassComponent> classes = analysisContext.getClasses();
        classes.forEach(clazz -> {

            if(clazz.getPackageName().contains("entity")){
                entities.add(clazz.getClassName());
            }

            List<AnnotationComponent> annotationComponents = clazz.getAnnotations().stream().map(Component::asAnnotationComponent).collect(Collectors.toList());
            for(AnnotationComponent annotationComponent : annotationComponents) {
                String annotation = annotationComponent.getAsString();
                if (annotation.matches("@Entity") || annotation.matches("@Document")) {
                    // Entity
                    entities.add(clazz.getClassName());
                }
            }
        });

        return new ArrayList<>(entities);
    }
}
