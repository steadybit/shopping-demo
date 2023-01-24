package com.steadybit.demo.shopping.checkout;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.testcontainers.containers.GenericContainer;

public class ActiveMqContainer extends GenericContainer<ActiveMqContainer> {
    private static final int ACTIVEMQ_JMX_PORT = 61616;
    private static final int ACTIVEMQ_WEB_PORT = 8161;
    private ActiveMQConnectionFactory connectionFactory;

    public ActiveMqContainer(String imageName) {
        super(imageName);
    }

    @Override
    protected void configure() {
        this.withExposedPorts(ACTIVEMQ_JMX_PORT, ACTIVEMQ_WEB_PORT);
    }

    public String getBrokerUrl() {
        return "tcp://" + this.getHost() + ":" + this.getMappedPort(ACTIVEMQ_JMX_PORT);
    }

}