package se.omegapoint.util.log.kafka;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import ch.qos.logback.classic.spi.ILoggingEvent;
import kafka.serializer.Encoder;
import kafka.utils.VerifiableProperties;
import scala.Function1;

/**
 * Created with IntelliJ IDEA.
 * User: geospa
 * Date: 29/04/14
 * Time: 18:51
 *
 * @TODO Make indentation configurable
 * @TODO Make type selection configurable (types to exclude available in current classpath)
 * @TODO Make field selection configurable (which fields in ILoggoingEvent to exclude)
 *
 * @TODO Add Message Mac:ing functionality:
 * each message includes reference to previous events mac,
 * The serialized mac and event is hashed and that mac is added to the next message.
 * When started the previous mac is read from disk.
 * When stopped, the last mac is written to disk.
 * @TODO Webapp to view/process logs?!
 */
public class LoggingEventToJacksonEncoder implements Encoder<ILoggingEvent>{
    private final ObjectMapper mapper;

    public LoggingEventToJacksonEncoder(VerifiableProperties verifiableProperties){
        mapper=new ObjectMapper();

        SimpleModule testModule = new SimpleModule("DemoModule", VersionUtil.versionFor(this.getClass()));
        testModule.addSerializer(StackTraceElement.class,new JsonSerializer<StackTraceElement>() {
            @Override
            public void serialize(StackTraceElement value, JsonGenerator jgen, SerializerProvider provider){}
        });
        mapper.registerModule(testModule);

        verifiableProperties.getMap("hej",null);

        mapper.enable(SerializationFeature.INDENT_OUTPUT);

    }
    @Override
    public byte[] toBytes(ILoggingEvent iLoggingEvent) {

        try {
            return mapper.writeValueAsBytes(iLoggingEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize iLoggingEvent:"+iLoggingEvent+" to string.", e);
        }
    }
}
