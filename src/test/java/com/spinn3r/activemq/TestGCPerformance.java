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
public class TestGCPerformance {

    protected static String JMX_HOST = "localhost";

    protected static int JMX_PORT = 1099;

    private static final String TMPDIR = "/tmp/artemis-embedded-broker";

    public static final String BROKER_HOST = "127.0.0.1";

    public static final int BROKER_PORT = 16161;

    protected static String brokerURL = String.format( "tcp://%s:%s", BROKER_HOST, BROKER_PORT );

    protected BrokerService broker;

    protected QueueMetaReader queueMetaReader = new QueueMetaReader( JMX_HOST, JMX_PORT );

    @Before
    public void setUp() throws Exception {

        // ****  Turn on stdout logging in log4j
        DOMConfigurator.configure( getClass().getResource( "/log4j-stdout.xml" ) );

        System.setProperty( "activemq.data", TMPDIR );

        File tmpdir = new File( TMPDIR );

        if (tmpdir.exists()) {
            System.out.printf( "Deleting tmpdir: %s\n", tmpdir.getPath() );
            FileUtils.deleteDirectory( tmpdir );
        }

        broker = BrokerFactory.createBroker( new URI( "xbean:apache-activemq.xml" ) );

        broker.start();
    }

    @After
    public void tearDown() throws Exception {

        if (broker != null)
            broker.stop();

    }

    @Test
    public void test1() throws Exception {

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory( brokerURL );
        Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession( false, Session.AUTO_ACKNOWLEDGE );

        int nrQueues = 7500;

        List<MessageConsumer> messageConsumers = Lists.newArrayList();

        for (int i = 0; i < nrQueues; i++) {

            String queueName = "test-" + i;

            System.out.printf( "Creating queue %s\n", queueName );

            Queue dest = session.createQueue( queueName );

            MessageProducer producer = session.createProducer( dest );
            producer.setDeliveryMode( DeliveryMode.NON_PERSISTENT );

            Message message = session.createTextMessage( "xx" );
            producer.send( message );

            MessageConsumer consumer = session.createConsumer( dest );
            consumer.receive();

            producer.close();

            messageConsumers.add( consumer );

        }

        System.out.printf( "done producing messages.\n" );

        // **** create a producer and consumer we can hold open to test message
        // production and consumption with existing consumers and producers.

        ProducerConsumer existingProducerConsumer = new ProducerConsumer( session );

        // **** now close all the consumers which will allow activemq to GC
        // the destinations.  This will then cause problems connecting
        // with producers and consumers.

        System.out.printf( "closing all consumers...\n" );

        for (MessageConsumer messageConsumer : messageConsumers) {
            messageConsumer.close();
        }

        System.out.printf( "closing all consumers...done\n" );

        List<Long> createProducerLatencies = Lists.newArrayList();

        List<Long> createMessageOnExistingProducerAndConsumerLatencies = Lists.newArrayList();

        System.out.printf( "Waiting for queues to GC and measuring latencies..." );

        while (queueMetaReader.read().size() > 1) {

            createProducerLatencies.add( createProducer( session ) );
            createMessageOnExistingProducerAndConsumerLatencies.add( existingProducerConsumer.produceAndConsume() );

            Thread.sleep( 500 );

            System.out.printf( "." );
        }

        System.out.printf( "done!\n" );
        System.out.printf( "====\n" );

        System.out.printf( "max create producer latency: %,d ms\n", max( createProducerLatencies ) );
        System.out.printf( "max create message on existing producer and consumer: %,d ms\n", max( createMessageOnExistingProducerAndConsumerLatencies ) );

    }

    /**
     * Create a producer, close it, then return the latency required to create
     * the producer.
     *
     * @param session
     * @return
     * @throws Exception
     */
    private long createProducer(Session session) throws Exception {

        long before = System.currentTimeMillis();

        String queueName = "foo";

        Queue dest = session.createQueue( queueName );

        MessageProducer producer = session.createProducer( dest );
        producer.setDeliveryMode( DeliveryMode.NON_PERSISTENT );
        producer.close();

        long after = System.currentTimeMillis();

        return after - before;

    }

    /**
     * Create a producer and consumer, which we cna hold open, to produce and
     * then consume a message and record the latency.
     */
    static class ProducerConsumer {

        private Session session;

        private MessageProducer messageProducer;

        private MessageConsumer messageConsumer;

        public ProducerConsumer(Session session) throws Exception {
            this.session = session;

            String queueName = "foo";

            Queue dest = session.createQueue( queueName );

            messageProducer = session.createProducer( dest );
            messageProducer.setDeliveryMode( DeliveryMode.NON_PERSISTENT );

            messageConsumer = session.createConsumer( dest );

        }

        public long produceAndConsume() throws Exception {

            long before = System.currentTimeMillis();

            Message message = session.createTextMessage( "xx" );
            messageProducer.send( message );

            messageConsumer.receive();

            long after = System.currentTimeMillis();

            return after - before;

        }

        public void close() throws Exception {

            messageProducer.close();
            messageConsumer.close();

        }

    }

}
