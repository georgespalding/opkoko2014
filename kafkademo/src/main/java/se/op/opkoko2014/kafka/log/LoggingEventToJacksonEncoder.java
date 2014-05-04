package se.op.opkoko2014.kafka.log;

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

/**
 * Created with IntelliJ IDEA.
 * User: geospa
 * Date: 29/04/14
 * Time: 18:51
 *
 * each message includes reference to previous events mac,
 * The serialized mac and event is hashed and that mac is added to the next message.
 * When started the previous mac is read from disk.
 * When stopped, the last mac is written to disk.
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

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public byte[] toBytes(ILoggingEvent iLoggingEvent) {
        try {
            return mapper.writeValueAsBytes(iLoggingEvent.getLoggerContextVO());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize iLoggingEvent:"+iLoggingEvent+" to string.", e);
        }
    }
}
