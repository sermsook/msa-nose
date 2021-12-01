package edu.baylor.ecs.msanose.model.context;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NoAPIGatewayContext {

    private boolean hasApiGateway;
    private double ratioOfUnmanageableConnections;

    public NoAPIGatewayContext(){
        this.hasApiGateway = false;
        this.ratioOfUnmanageableConnections = 0.0;
    }

    public boolean hasApiGateway() {
        return hasApiGateway;
    }

    public void setHasApiGateway(boolean hasApiGateway) {
        this.hasApiGateway = hasApiGateway;
    }

    public double getRatioOfUnmanageableConnections() {
        return ratioOfUnmanageableConnections;
    }

    public void setRatioOfUnmanageableConnections(double ratioOfUnmanageableConnections) {
        this.ratioOfUnmanageableConnections = ratioOfUnmanageableConnections;
    }
}
