package com.spinn3r.activemq;

import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.broker.jmx.QueueViewMBean;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class QueueMetaReader {

    private String hostname;

    private int port;

    public QueueMetaReader(String hostname) {
        this( hostname, 1099 );
    }

    public QueueMetaReader(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    /**
     * Read information about the queues on this host.
     */
    public Map<String,QueueMeta> read() throws IOException {

        Map<String,QueueMeta> result = new HashMap<>();

        String address = String.format( "service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi", hostname, port );

        JMXServiceURL url = new JMXServiceURL( address );

        try( JMXConnector jmxc = JMXConnectorFactory.connect( url ); ) {

            MBeanServerConnection conn = jmxc.getMBeanServerConnection();

            // NOTE: by convention we're going to use the machine name.
            ObjectName activeMQ = new ObjectName( "org.apache.activemq:type=Broker,brokerName=" + hostname );

            BrokerViewMBean mbean = MBeanServerInvocationHandler.newProxyInstance( conn, activeMQ, BrokerViewMBean.class, true );

            for ( ObjectName name : mbean.getQueues() ) {

                if ( name == null )
                    continue;

                try {

                    QueueViewMBean queueMbean = MBeanServerInvocationHandler.newProxyInstance( conn, name, QueueViewMBean.class, true );

                    String queueName = queueMbean.getName();
                    QueueMeta queueMeta = new QueueMeta( queueName,
                                                         queueMbean.getQueueSize(),
                                                         queueMbean.getConsumerCount() );

                    result.put( queueName, queueMeta );

                } catch ( UndeclaredThrowableException e ) {
                    // only thrown during a race in JMX where we read the queues
                    // but one gets GCd.
                } catch ( RuntimeErrorException e ) {
                    e.printStackTrace();
                }

            }

            return result;

        } catch ( JMException e ) {
            throw new IOException( "Caught JMX exception: ", e );
        }

    }

    public String readFormatted() throws IOException {
        return read().toString();
    }

}
