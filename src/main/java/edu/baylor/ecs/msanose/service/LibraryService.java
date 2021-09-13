package edu.baylor.ecs.msanose.service;

import edu.baylor.ecs.msanose.model.SharedLibrary;
import edu.baylor.ecs.msanose.model.context.SharedLibraryContext;
import edu.baylor.ecs.rad.context.RequestContext;
import edu.baylor.ecs.rad.service.ResourceService;
import lombok.AllArgsConstructor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Service
@AllArgsConstructor
public class LibraryService {

    private ResourceService resourceService;

    public SharedLibraryContext getSharedLibraries(RequestContext request) throws IOException, XmlPullParserException {

        //1 ระบบมี service หลายอันโดยแต่ละservice มี pom.xml เป็นของตัวเอง
        List<String> fileNames = resourceService.getPomXML(request.getPathToCompiledMicroservices());  // ได้ pom.xml ทั้งหมดของ1ระบบมา
        SharedLibraryContext sharedLibraryContext = new SharedLibraryContext();
        MavenXpp3Reader reader = new MavenXpp3Reader();
        for(int i = 0; i < fileNames.size() - 1; i++){
            for (int j = i + 1; j < fileNames.size(); j++) {

                Model modelA = reader.read(new FileReader(fileNames.get(i)));  //get pom ตัวที่ 1,2 มาเทียบกัน (ขยับคู่ตัวที่เทียบไปเรื่อยๆ)
                Model modelB = reader.read(new FileReader(fileNames.get(j)));

                for(Dependency dependencyA : modelA.getDependencies()){   //เทียบ dependency ทั้หมด ของ pomA กับ pomB
                    boolean matched = false;
                    for (Dependency dependencyB : modelB.getDependencies()) {
                        if(dependencyA.getArtifactId().equals(dependencyB.getArtifactId()) && dependencyA.getGroupId().equals(dependencyB.getGroupId())){  //get  dependency แต่ละตัวในแต่ละ pom มาเทียบกัน
                            matched = true;
                            String msaA = modelA.getGroupId() + ":" + modelA.getArtifactId();
                            String msaB = modelB.getGroupId() + ":" + modelB.getArtifactId();

                            String library = dependencyA.getGroupId() + ":" + dependencyA.getArtifactId();   //ได้ชื่อ lib ที่ dependency ใน pomA ใช้ เช่น org.projectlombok:lombok
                            SharedLibrary sharedLibrary = sharedLibraryContext.getOrDefault(library);
                            sharedLibrary.add(msaA, msaB);
                            sharedLibraryContext.addSharedLibrary(sharedLibrary);
                            break;
                        }
                    }

                    if(matched){
                        break;
                    }
                }
            }
        }

        return sharedLibraryContext;
    }
}
