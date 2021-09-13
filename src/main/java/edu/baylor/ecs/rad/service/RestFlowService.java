package edu.baylor.ecs.rad.service;

import edu.baylor.ecs.rad.analyzer.Helper;
import edu.baylor.ecs.rad.context.RestEntityContext;
import edu.baylor.ecs.rad.context.RestFlowContext;
import edu.baylor.ecs.rad.model.RestEntity;
import edu.baylor.ecs.rad.model.RestFlow;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * This class constructs a {@link RestFlowContext}.
 * It takes a list of {@link RestEntityContext} as input.
 * It matches client entities with server entities based on url and other properties.
 *
 * @author Dipta Das
 */

@Service
public class RestFlowService {   //match ว่า rest client ตัวไหน เรียกไปหา server ไหน
    public RestFlowContext getRestFlowContext(List<RestEntityContext> restEntityContexts) {
        List<RestEntity> serverEntities = new ArrayList<>();
        List<RestEntity> clientEntities = new ArrayList<>();

        for (RestEntityContext restEntityContext : restEntityContexts) {
            for (RestEntity restEntity : restEntityContext.getRestEntities()) {   //วนแต่ละ URL ใน serviceว่าเป็น client หรือ server //วน rest client แต่ละตัวในแต่ละ jar file
                if (restEntity.isClient()) clientEntities.add(restEntity);
                else serverEntities.add(restEntity);
            }
        }

        RestFlowContext restFlowContext = new RestFlowContext();
        restFlowContext.getRestFlows().addAll(getRestFlows(serverEntities, clientEntities));
        // restFlowContext.getRestFlows().addAll(getPossibleRestFlows(serverEntities, clientEntities));



        /* Dao note
        server entities example
        RestEntity(isClient=false, url=/confirmForgotPassword, applicationName=null, ribbonServerName=null, resourcePath=/Users/sermsook.pul/acm-core-service-2/core-service-web/target/core-service-web-4.0.27.jar, className=com.tmn.core.controller.TmnProfileAdministrationController, methodName=confirmForgotPassword, returnType=com.tmn.core.api.message.StandardBizResponse, path=/tmn-profile-administration/confirmForgotPassword, httpMethod=POST, pathParams=null, queryParams=null, consumeType=null, produceType=null)
        RestEntity(isClient=false, url=/getProfile, applicationName=null, ribbonServerName=null, resourcePath=/Users/sermsook.pul/acm-core-service-2/core-service-web/target/core-service-web-4.0.27.jar, className=com.tmn.core.controller.TmnProfileController, methodName=getProfile, returnType=com.tmn.core.api.message.GetProfileResponse, path=/tmn-profile/getProfile, httpMethod=POST, pathParams=null, queryParams=null, consumeType=null, produceType=null)
        -------------------------------------
        client entities example
        RestEntity(isClient=true, url={0}, applicationName=null, ribbonServerName=null, resourcePath=/Users/sermsook.pul/acm-core-service-2/core-service-web/target/core-service-web-4.0.27.jar, className=com.tmn.core.client.utiba.rest.UtibaAbstractRestRequestor, methodName=executeRequest, returnType=java.lang.Object, path=/, httpMethod=GET, pathParams=null, queryParams=null, consumeType=null, produceType=null)
        RestEntity(isClient=true, url={0}, applicationName=null, ribbonServerName=null, resourcePath=/Users/sermsook.pul/acm-core-service-2/core-service-web/target/core-service-web-4.0.27.jar, className=com.tmn.core.client.utiba.rest.UpdateAccountRestRequestor, methodName=executeRequest, returnType=java.lang.Object, path=/, httpMethod=GET, pathParams=null, queryParams=null, consumeType=null, produceType=null)
        RestEntity(isClient=true, url={0}, applicationName=null, ribbonServerName=null, resourcePath=/Users/sermsook.pul/acm-core-service-2/target/core-service-4.0.27.jar, className=com.tmn.core.client.risk.rest.VerifyBlacklistRestRequestor, methodName=executeRequest, returnType=java.lang.Object, path=/, httpMethod=GET, pathParams=null, queryParams=null, consumeType=null, produceType=null)
        */

        return restFlowContext;
    }

    private List<RestFlow> getRestFlows(List<RestEntity> serverEntities, List<RestEntity> clientEntities) {
        List<RestFlow> restFlows = new ArrayList<>();

        // populate RestFlow
        for (RestEntity restClientEntity : clientEntities) {    //วนเทียบแต่ละ client (ex. com.tmn.core.client.utiba.rest.SellRestRequestor client ใน core service)กับ server เป็นคู่ๆ (server คือพวกที่เรากำหนด url เอง ex. /updateProfile)
            for (RestEntity restServerEntity : serverEntities) {
                // match url and http method
                if (restClientEntity.getHttpMethod() == restServerEntity.getHttpMethod() &&
                        Helper.matchUrl(restClientEntity.getUrl(), restServerEntity.getUrl())) {  //ถ้ามี url กับ method (ex. GET POST) ตรงกันคือเรียกไปหากัน

                    createRestFlow(restFlows, restServerEntity, restClientEntity);
                }
            }
        }
        return restFlows;
    }

    private List<RestFlow> getPossibleRestFlows(List<RestEntity> serverEntities, List<RestEntity> clientEntities) {
        List<RestFlow> restFlows = new ArrayList<>();

        // populate RestFlow
        for (RestEntity restClientEntity : clientEntities) {
            for (RestEntity restServerEntity : serverEntities) {
                // match return type and http method
                if (restClientEntity.getHttpMethod() == restServerEntity.getHttpMethod() &&
                        restClientEntity.getReturnType() != null &&
                        restServerEntity.getReturnType() != null &&
                        restClientEntity.getReturnType().equals(restServerEntity.getReturnType())) {

                    // narrow down, match server name if specified
                    String serverName = restClientEntity.getRibbonServerName();
                    String applicationName = restServerEntity.getApplicationName();
                    if (serverName != null && !serverName.equals(applicationName)) {
                        continue;
                    }

                    createRestFlow(restFlows, restServerEntity, restClientEntity);
                }
            }
        }

        return restFlows;
    }

    private void createRestFlow(List<RestFlow> restFlows, RestEntity server, RestEntity client) {
        // search if there is already an entry for this client
        for (RestFlow restFlow : restFlows) {
            if (restFlow.getResourcePath().equals(client.getResourcePath()) &&
                    restFlow.getClassName().equals(client.getClassName()) &&
                    restFlow.getMethodName().equals(client.getMethodName())) {

                restFlow.getServers().add(server);
                return;
            }
        }

        RestFlow restFlow = new RestFlow();

        restFlow.setResourcePath(client.getResourcePath());
        restFlow.setClassName(client.getClassName());
        restFlow.setMethodName(client.getMethodName());

        restFlow.setServers(new ArrayList<>());
        restFlow.getServers().add(server);

        restFlows.add(restFlow);
    }
}
