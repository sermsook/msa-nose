package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.msanose.model.context.TooManyStandardsContext;
import edu.baylor.ecs.msanose.model.persistency.DatabaseInstance;
import edu.baylor.ecs.msanose.model.persistency.DatabaseType;
import edu.baylor.ecs.msanose.model.standards.BusinessType;
import edu.baylor.ecs.msanose.model.standards.PresentationType;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.service.ResourceService;
import lombok.AllArgsConstructor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

@Service
@AllArgsConstructor
public class TooManyStandardsService {

    private PersistencyService persistencyService;
    private ResourceService resourceService;

    public TooManyStandardsContext getStandards(RequestContext request) {
        return new TooManyStandardsContext(
                getPresentationStandards(request),  //check ว่ามี dependencies lib อะไรบ้างใน package.json  {react, angular, static}
                getBusinessStandards(request), //วนเช็คแต่ละ dependency ในแต่ละ pom.xml ว่ามีใช้ standard lib อะไรบ้าง
                getDataStandards(request) //get all database type ที่อยู่ในทุก .yml file
        );
    }

    public Set<PresentationType> getPresentationStandards(RequestContext request){
        Set<PresentationType> types = new HashSet<>();

        List<String> packageJsonFilePaths = resourceService.getPackageJsons(request.getPathToCompiledMicroservices());  //get all package.json file path

        for(String packageJsonFilePath : packageJsonFilePaths){
            try {
                String content = new Scanner(new File(packageJsonFilePath)).useDelimiter("\\Z").next();  //scan จน ถึง The end of the input but for the final terminator
                JSONObject packageJson = new JSONObject(content);
                JSONObject dependencies = packageJson.getJSONObject("dependencies");

                // Check for React
                String react = dependencies.getString("react");
                if(!react.isEmpty()){
                    types.add(PresentationType.REACT);
                }

                // Check for Angular
                String angular = dependencies.getString("@angular/common");
                if(!angular.isEmpty()){
                    types.add(PresentationType.ANGULAR);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }

        if(types.size() == 0){
            types.add(PresentationType.STATIC);
        }

        return types;
    }

    public Set<BusinessType> getBusinessStandards(RequestContext request){
        Set<BusinessType> types = new HashSet<>();


        List<String> fileNames = resourceService.getPomXML(request.getPathToCompiledMicroservices());  //get all pom.xml
        MavenXpp3Reader reader = new MavenXpp3Reader();
        for(String filePath : fileNames){

            try {
                Model model = reader.read(new FileReader(filePath));

                for (Dependency dependency : model.getDependencies()) {   //วนเช็คแต่ละ dependency ในแต่ละ pom.xml

                    if (dependency.getGroupId().equals("org.springframework.boot")) {
                        types.add(BusinessType.SPRING);
                    }

                    if (dependency.getGroupId().equals("javax")) {
                        types.add(BusinessType.EE);
                    }
                }

                if(model.getBuild() != null){
                    for(Plugin plugin : model.getBuild().getPlugins()){
                        if (plugin.getGroupId().equals("org.springframework.boot")) {
                            types.add(BusinessType.SPRING);
                        }

                        if (plugin.getGroupId().equals("javax")) {
                            types.add(BusinessType.EE);
                        }
                    }
                }

            } catch (Exception e){
                e.printStackTrace();
            }

        }

        return types;
    }

    public Set<DatabaseType> getDataStandards(RequestContext request){
        Map<String, DatabaseInstance> databases = persistencyService.getModulePersistencies(request);  //get all db config ใน yml file

        Set<DatabaseType> types = new HashSet<>();
        for(Map.Entry<String, DatabaseInstance> entry : databases.entrySet()){
            types.add(entry.getValue().getType());
        }

        return types;
    }


}
