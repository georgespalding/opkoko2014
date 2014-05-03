package se.op.opkoko2014.kafka.log;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

/**
 * Created with IntelliJ IDEA.
 * User: geospa
 * Date: 22/04/14
 * Time: 23:10
 * To change this template use File | Settings | File Templates.
 */
public class KafkaAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private Producer<String, ILoggingEvent> producer;

    private Layout<ILoggingEvent> keyLayout;
    private Layout<ILoggingEvent> topicLayout;
    private String kafkaProperties;

    public Layout<ILoggingEvent> getKeyLayout() {
        return keyLayout;
    }

    public void setKeyLayout(Layout<ILoggingEvent> keyLayout) {
        this.keyLayout = keyLayout;
    }

    public Layout<ILoggingEvent> getTopicLayout() {
        return topicLayout;
    }

    public void setTopicLayout(Layout<ILoggingEvent> topicLayout) {
        this.topicLayout = topicLayout;
    }

    public String getKafkaProperties() {
        return kafkaProperties;
    }

    public void setKafkaProperties(String kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public void start() {
        Properties props=new Properties();
        props.setProperty("key.serializer.class","kafka.serializer.StringEncoder");
        props.setProperty("serializer.class","se.op.opkoko2014.kafka.log.LoggingEventToJacksonEncoder");
        if(kafkaProperties.startsWith("classpath:")){
            String kafkaResource = kafkaProperties.substring("classpath:".length());
            try(InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(kafkaResource)){
                props.load(is);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to load kafkaProperties from:"+kafkaProperties, e);
            } catch (NullPointerException e) {
                throw new IllegalArgumentException("Failed to fins kafkaProperties "+kafkaResource+" in classpath.", e);
            }
        }else{
            try{
                URL kafkaUrl = new URL(kafkaProperties);
                props.load(kafkaUrl.openStream());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Failed to construct kafkaProperties url from:"+kafkaProperties, e);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to load kafkaProperties url from:"+kafkaProperties, e);
            }
        }

        ProducerConfig config = new ProducerConfig(props);
        producer = new Producer<String,ILoggingEvent>(config);
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        producer.close();
        System.err.println("Stopped Kafka Log Appender");
    }

    protected String getTopic(ILoggingEvent e){
        return topicLayout.doLayout(e);
    }

    protected String getKey(ILoggingEvent e){
        return keyLayout.doLayout(e);
    }

    @Override
    protected void append(ILoggingEvent e) {
        producer.send(new KeyedMessage<>(getTopic(e),getKey(e),e));
        System.err.println("Kafkade ett meddelande till topic:"+getTopic(e)+" key:"+getKey(e)+" event:"+e);
    }
}
