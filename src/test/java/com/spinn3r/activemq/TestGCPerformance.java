package com.spinn3r.activemq;

import com.google.common.collect.Lists;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.commons.io.FileUtils;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jms.*;
import java.io.File;
import java.net.URI;
import java.util.List;

import static java.util.Collections.max;

/**
 *
 */
public class TestGCPerformance extends BaseTestGCPerformance {

    public TestGCPerformance() {
        super( "apache-activemq.xml" );
    }
}
