package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.msanose.model.context.MicroservicesGreedyContext;
import edu.baylor.ecs.msanose.model.greedy.MicroserviceMetric;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.service.ResourceService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class GreedyService {

    private final ResourceService resourceService;
    private final EntityService entityService;

    public MicroservicesGreedyContext getGreedyMicroservices(RequestContext request){
        MicroservicesGreedyContext microservicesGreedyContext = new MicroservicesGreedyContext();

        List<String> jars = resourceService.getResourcePaths(request.getPathToCompiledMicroservices());
        File directory = new File(request.getPathToCompiledMicroservices());     //ex.   /Users/sermsook.pul/acm-core-service-2

        // Get all sub-directories from a directory.
        File[] fList = directory.listFiles();
        List<String> subdirectories = new ArrayList<>();
        if(fList != null)
            for (File file : fList) {
                if (file.isDirectory()) {
                    subdirectories.add(file.getAbsolutePath());
                }
            }

        // For each, get entity counts and static file counts
        for(String dir : subdirectories){   //DaoNotes: dir = each module    ex. /Users/sermsook.pul/acm-core-service-2/core-service-web
            List<String> entities = entityService.getEntitiesPerFolder(dir);      //all entity class name ex. [TmnProfileData, TmnProfile, TmnAddressPK, TmnAddress, LogableHeaderMataInfo, TmnProfileDataPK, TmnDocumentPK, TmnDocument]
            List<File> staticFiles = new ArrayList<>();
            getStaticFiles(dir, staticFiles);  //DaoNotes: get all front-end file
            microservicesGreedyContext.addMetric(new MicroserviceMetric(dir, staticFiles.size(), entities.size()));
            // System.out.println(dir + " - " + entities.size() + " " + staticFiles.size());
        }

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

}
