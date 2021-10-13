package edu.baylor.ecs.msanose.model.context;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data public class NoAPIGatewayContext {

    private boolean isHasApiGateway;
    private double ratioOfUnmanageableConnections;

    public NoAPIGatewayContext(){
        this.isHasApiGateway = false;
        this.ratioOfUnmanageableConnections = 0.0;
    }
}
