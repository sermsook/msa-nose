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

        List<String> jars = resourceService.getResourcePaths(request.getPathToCompiledMicroservices()); //ได้list path ของ JAR or WAR file in the project directory
        /**
         * Old Code
         */
//        File directory = new File(request.getPathToCompiledMicroservices());     //ex.   /Users/sermsook.pul/acm-core-service-2
//
//        // Get all sub-directories from a directory.
//        File[] fList = directory.listFiles();
//        List<String> subdirectories = new ArrayList<>();
//        if(fList != null) {
//            for (File file : fList) {
//                if (file.isDirectory()) {
//                    subdirectories.add(file.getAbsolutePath());
//                }
//            }
//        }
//
//        // For each, get entity counts and static file counts
//        for(String dir : subdirectories){   //DaoNotes: dir = each module    ex. /Users/sermsook.pul/acm-core-service-2/core-service-web
//            List<String> entities = entityService.getEntitiesPerFolder(dir);      //all entity class name ex. [TmnProfileData, TmnProfile, TmnAddressPK, TmnAddress, LogableHeaderMataInfo, TmnProfileDataPK, TmnDocumentPK, TmnDocument]
//            List<File> staticFiles = new ArrayList<>();
//            getStaticFiles(dir, staticFiles);  //DaoNotes: get all front-end file
//            microservicesGreedyContext.addMetric(new MicroserviceMetric(dir, staticFiles.size(), entities.size()));
//            // System.out.println(dir + " - " + entities.size() + " " + staticFiles.size());
//
//            counts.put(dir, entities.size());
//        }

        /**
         * DaoNotes: Add logic to find outliers (2sd)
         */
        for(String jar : jars){
            List<String> entities = entityService.getEntitiesPerJar(request, jar); //หา entity class name ทั้งหมดใน jar โดยดูจาก annotation @Entity หรือ @Document
            counts.put(extractName(jar), entities.size());  //extract ชื่อ jar file ; counts = list(jarName, #entity class)
        }

        List<EntityPair> sorted = counts.entrySet()
                .stream()
                .map(x -> new EntityPair(x.getKey(), x.getValue()))
                .sorted(Comparator.comparing(EntityPair::getEntityCount))
                .collect(Collectors.toList());

        List<EntityPair> trimmed = sorted
                .stream()
                .filter(x -> x.getEntityCount() > 1)  //filter เอาอันที่ entity > 1
                .sorted(Comparator.comparing(EntityPair::getEntityCount))
                .collect(Collectors.toList());

        for (EntityPair eachService: sorted) {
            log.info("service name: " + eachService.getPath() + ", entity class count: " + eachService.getEntityCount());
        }
        log.info("---------------------------------------------------------------------------------------------------------");
        for (EntityPair eachService: trimmed) {
            log.info("service name: " + eachService.getPath() + ", entity class count: " + eachService.getEntityCount());
        }

        double avgEntityCount = 0;
        for (EntityPair pair : trimmed) {
            avgEntityCount += pair.getEntityCount();
        }

        avgEntityCount = avgEntityCount / trimmed.size();  //#entity ทั้งหมดทุกservice/#microservice            [#microservice =   #jar ที่มี entity > 1]
        log.info("avgEntityCount: "+avgEntityCount);

        double numerator = 0.0;
        for (EntityPair pair : trimmed) {
            numerator += Math.pow(pair.getEntityCount() - avgEntityCount, 2);  //numerator = sum(#entityต่อ1jar - avgEntityCount)^2
        }

        double std = Math.sqrt(numerator / (trimmed.size() - 1));   //std = sqrt(numerator/(#jar ที่มี entity > 1 - 1))
        log.info("std is " +std+ "---> 2sd is " + 2*std);

        List<EntityPair> possibleNanoMicroservices = new ArrayList<>();
        for(EntityPair pair : sorted){
            MicroserviceMetric microserviceMetric = new MicroserviceMetric();
            microserviceMetric.setPath(pair.getPath());
            microserviceMetric.setEntityFileCount(pair.getEntityCount());
//            double d = Math.max(avgEntityCount, pair.getEntityCount()) - Math.min(avgEntityCount, pair.getEntityCount());  //d = #entity มากสุด - #entity น้อยสุด
            double d = pair.getEntityCount() - avgEntityCount;
//            log.info(pair.getPath());
//            log.info("d = " + d);
            if(d < -(2 * std)){   //ถ้า d < -2sd ก็มีความเป็นไปได้ที่จะเป็น Nano Microservice
                microservicesGreedyContext.addGreedyMicroservice(microserviceMetric);
                possibleNanoMicroservices.add(pair);
//                log.info("small d = " + d);
            }
//            log.info("++++++++++++++++++++++++++++++++++++++");
        }

        //Calculate base metrics
        double totalNumberOfMicroserviceInSystems = jars.size();
        double totalNumberOfNanoMicroservices = possibleNanoMicroservices.size();
        double ratioOfNanoMicroservices = 0;
        if (totalNumberOfMicroserviceInSystems !=0) {
            ratioOfNanoMicroservices = totalNumberOfNanoMicroservices/totalNumberOfMicroserviceInSystems;
        }

        microservicesGreedyContext.setRatioOfNanoMicroservices(ratioOfNanoMicroservices);
        log.info("****** Nano Microservices ******");
        log.info("totalNumberOfMicroserviceInSystems: "+totalNumberOfMicroserviceInSystems);
        log.info("totalNumberOfNanoMicroservices: "+totalNumberOfNanoMicroservices);
        log.info("ratioOfNanoMicroservices: "+ratioOfNanoMicroservices);
        log.info("=======================================================");

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
