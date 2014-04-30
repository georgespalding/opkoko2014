package se.op.opkoko2014.kafka.log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.BasicBeanDescription;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.JsonValueSerializer;
import ch.qos.logback.classic.spi.ILoggingEvent;
import kafka.serializer.Encoder;
import kafka.utils.VerifiableProperties;

/**
 * Created with IntelliJ IDEA.
 * User: geospa
 * Date: 29/04/14
 * Time: 18:51
 * To change this template use File | Settings | File Templates.
 */
public class LoggingEventToJacksonEncoder implements Encoder<ILoggingEvent>{
    private final VerifiableProperties verifiableProperties;

    public LoggingEventToJacksonEncoder(VerifiableProperties verifiableProperties){
        this.verifiableProperties = verifiableProperties;
    }
    @Override
    public byte[] toBytes(ILoggingEvent iLoggingEvent) {
        ObjectMapper mapper=new ObjectMapper();

        Method method;
        try {
            method = ILoggingEvent.class.getMethod("getCallerData");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to get method getCallerData on class ILoggingEvent",e);
        }
        SimpleModule testModule = new SimpleModule("DemoModule", VersionUtil.versionFor(this.getClass()));
                 testModule.addSerializer(
                         new JsonValueSerializer(
                                 method,
                                 null));
        mapper.registerModule(testModule);

        mapper.enable(SerializationFeature.INDENT_OUTPUT);


        try {
            return mapper.writeValueAsBytes(iLoggingEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize iLoggingEvent:"+iLoggingEvent+" to string.", e);
        }
    }
}
