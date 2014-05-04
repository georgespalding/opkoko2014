package se.op.opkoko2014.kafka;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.bridge.SLF4JBridgeHandler;
import ch.qos.logback.classic.LoggerContext;

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

    public static void main(String[] args) throws InterruptedException, NoSuchAlgorithmException {
        // OR -Djava.util.logging.config.file=log.properties
        //# register SLF4JBridgeHandler as handler for the j.u.l. root logger
        //#handlers = org.slf4j.bridge.SLF4JBridgeHandler

        // Optionally remove existing handlers attached to j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of your application
        SLF4JBridgeHandler.install();

        final SecureRandom sr=SecureRandom.getInstance("SHA1PRNG");
        //static final Logger log = Logger.getLogger("test");

        ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
        List<Future<Void>> list = new LinkedList<>();
        for (int i = 0; i < 200; i++) {
            final int ci=i;
            Callable<Void> worker = new Callable<Void>(){

                @Override
                public Void call() throws Exception {
                    final String userid = ""+sr.nextInt(17);
                    try{
                        MDC.getMDCAdapter().put("userid", userid);
                        MDC.getMDCAdapter().put("clientip", "192.168.47."+sr.nextInt(255));
                        MDC.getMDCAdapter().put("msgid", ""+ci);
                        Double d = sr.nextDouble();
                        String msg= String.format("%s %s %s %f",
                                nouns[sr.nextInt(nouns.length)],
                                verbs[sr.nextInt(verbs.length)],
                                adverbs[sr.nextInt(adverbs.length)],d);

                        slf4jLogger.info(msg);
                        return null;
                    }finally{
                        MDC.getMDCAdapter().remove("userid");
                        MDC.getMDCAdapter().remove("clientip");
                        MDC.getMDCAdapter().remove("msgid");
                        //slf4jLogger.debug("leaving");
                    }
                }
            };
            Future<Void> submit = executor.submit(worker);
            list.add(submit);
        }
        long sum = 0;
        System.out.println(list.size());
        // now retrieve the result (Just ensure they all have completed)
        for (Future<Void> future : list) {
            try {
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        System.err.println("Executor shutdown");
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
        System.err.println("Stopped executor");
    }
}
