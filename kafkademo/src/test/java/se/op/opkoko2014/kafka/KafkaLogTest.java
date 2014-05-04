package se.op.opkoko2014.kafka;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.bridge.SLF4JBridgeHandler;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

/**
 * Created with IntelliJ IDEA.
 * User: geospa
 * Date: 29/04/14
 * Time: 21:07
 * To change this template use File | Settings | File Templates.
 */
public class KafkaLogTest {
    static final org.slf4j.Logger slf4jLogger=LoggerFactory.getLogger(KafkaLogTest.class);
    private static final String[] nouns = {"Apples","Bodrum","Clustering","DDD"};
    private static final String[] verbs = {"rules","tastes","rocks","sucks"};
    private static final String[] adverbs = {"tomorrow","everywhere","big time","today"};
    private static final int NTHREDS = 10;
    private static ExecutorService executor;
    private static List<Future<String>> work;

    @BeforeClass
    public static void setupLogging() throws InterruptedException, NoSuchAlgorithmException {
        // OR -Djava.util.logging.config.file=log.properties
        //# register SLF4JBridgeHandler as handler for the j.u.l. root logger
        //#handlers = org.slf4j.bridge.SLF4JBridgeHandler

        // Optionally remove existing handlers attached to j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of your application
        SLF4JBridgeHandler.install();

        //static final Logger log = Logger.getLogger("test");
    }
    @BeforeClass
    public static void setupExecutor() throws InterruptedException, NoSuchAlgorithmException {
        executor = Executors.newFixedThreadPool(NTHREDS);
        work = new LinkedList<>();
    }

    @Ignore
    @Test
    public void testLog() throws NoSuchAlgorithmException {
        final SecureRandom  finalSr = SecureRandom.getInstance("SHA1PRNG");
        for (int i = 0; i < 200; i++) {
            final int ci=i;
            Callable<String> worker = new Callable<String>(){

                @Override
                public String call() throws Exception {
                    try{
                        // TODO capture values to be asserted
                        final String userid = ""+finalSr.nextInt(17);
                        MDC.getMDCAdapter().put("userid", userid);
                        MDC.getMDCAdapter().put("clientip", "192.168.47."+finalSr.nextInt(255));
                        MDC.getMDCAdapter().put("msgid", ""+ci);
                        Double d = finalSr.nextDouble();
                        String msg= String.format("%s %s %s %f",
                                nouns[finalSr.nextInt(nouns.length)],
                                verbs[finalSr.nextInt(verbs.length)],
                                adverbs[finalSr.nextInt(adverbs.length)],d);

                        slf4jLogger.info(msg);
                        return msg;
                    }finally{
                        MDC.getMDCAdapter().remove("userid");
                        MDC.getMDCAdapter().remove("clientip");
                        MDC.getMDCAdapter().remove("msgid");
                        //slf4jLogger.debug("leaving");
                    }
                }
            };
            Future<String> submit = executor.submit(worker);
            work.add(submit);
        }

        // now retrieve the result (Just ensure they all have completed)
        for (Future<String> future : work) {
            try {
                String result = future.get();
                //TODO assert captured data and ensure that it is correct in the message that was logged.
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @AfterClass
    public static void afterClass(){
        executor.shutdown();
        System.err.println("Executor shutdown");
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
        System.err.println("Stopped executor");
    }
}
