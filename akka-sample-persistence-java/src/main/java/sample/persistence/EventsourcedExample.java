package sample.persistence;

//#eventsourced-example

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import akka.actor.*;
import akka.japi.Procedure;
import akka.persistence.*;

// Bookeeping
class Record implements Serializable {
    public static final long serialVersionUID = 1L;
    public final BigDecimal amount;
    public final String note;
    public final Date received;
    public final boolean batch;

    Record(BigDecimal amount) {
        this(amount,false);
    }
    Record(BigDecimal amount,boolean batch) {
        this.batch=batch;
        this.amount = amount;
        final int diff=amount.compareTo(BigDecimal.ZERO);
        this.note = diff>0?"Credit":diff<0?"Debit":"Zero!";
        this.received = new Date();
    }

    @Override
    public String toString() {
        return "Record{" +
                "amount=" + amount +
                ", note='" + note + '\'' +
                ", received=" + received +
                ", batch=" + batch +
                '}';
    }
}

class Account implements Serializable {
    public static final long serialVersionUID = 1L;

    // Persistent state
    private BigDecimal balance = BigDecimal.ZERO;
    // Persistent state
    private List<Record> transactions = new LinkedList<>();

    // sortof transient state, not saved in eventqueue (but in snapshots)
    public boolean showtransactions = true;

    public Account() {}

    public Account(BigDecimal balance, List<Record> transactions) {
        this.balance = balance;
        this.transactions = transactions;
    }

    public void updateBalance(Record change){
        balance = balance.add(change.amount);
        transactions.add(change);
    }

    public Account copy(){
        return new Account(balance, transactions);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    @Override
    public String toString() {
        return "Account{" +
                "balance=" + balance +
                ", transactions=" + (showtransactions ?transactions:"[...]") +
                '}';
    }
}

class AccountProcessor extends UntypedEventsourcedProcessor {
    private Account account = new Account();

    public void onReceiveRecover(Object msg) {
        System.out.println("recover:"+msg);
        if (msg instanceof Record) {
            account.updateBalance((Record) msg);
        } else if (msg instanceof SnapshotOffer) {
            account = (Account)((SnapshotOffer)msg).snapshot();
        } else {
            System.err.println("onReceiveRecover unknown type! Type:"+msg.getClass());
        }
    }

    public void onReceiveCommand(Object msg) {
        System.out.println(">>>>"+msg);
        if (msg instanceof BigDecimal || msg instanceof BigDecimal[]) {
            // Create a batch of events (possibly only one) to be executed together.
            final LinkedList<Record> records=new LinkedList<>();
            BigDecimal totalChange = BigDecimal.ZERO;
            if (msg instanceof BigDecimal){
                records.add(new Record((BigDecimal)msg,false));
                totalChange = (BigDecimal)msg;
            }else {
                for(BigDecimal each:(BigDecimal[])msg){
                    records.add(new Record(each,true));
                    totalChange=totalChange.add(each);
                }
            }

            // Validate
            if(account.getBalance().add(totalChange).compareTo(BigDecimal.ZERO)<0){
                // Negativt saldo!!!
                sender().tell("Insufficient funds, (Out of Money)! REJECTED Command: "+records, getSelf());
                return;
            }

            // Persist the Records
            persist(records, new Procedure<Record>() {
                public void apply(Record change) throws Exception {
                    account.updateBalance(change);
                    // Commit after update of each event in this batch.
                    getContext().system().eventStream().publish(change);
                }
            });
        } else if (msg instanceof SaveSnapshotSuccess) {
            System.err.println("<<<<snapshot saved");
        } else if (msg.equals("snap")) {
            // IMPORTANT: create a copy of snapshot
            // because the Map is mutable !!!
            saveSnapshot(account.copy());
        } else if (msg.equals("print")) {
            System.out.println(account);
        } else if (msg.equals("showtran")) {
            account.showtransactions = true;
        } else if (msg.equals("hidetran")) {
            account.showtransactions = false;
        } else if (msg.equals("nuke")) {
            account = new Account();
        } else {
            System.err.println("What is this? "+msg);
        }
    }
}

//#eventsourced-example
public class EventsourcedExample {
    public static void main(String... args) throws Exception {

        final ActorSystem system = ActorSystem.create("example");
        final ActorRef processor = system.actorOf(Props.create(AccountProcessor.class), "processor-4-java");
        final ActorRef sender = system.actorOf(Props.create(ExampleDestination.class), "sender-4-java");

        processor.tell("print", null);

        // Sätt in
        //processor.tell(BigDecimal.valueOf(100), sender);

        // Ta ut
//        processor.tell(BigDecimal.valueOf(-400), sender);

        // Sätt in och ta ut i en batch
//        processor.tell(new BigDecimal[]{
//                BigDecimal.valueOf(-21),
//                BigDecimal.valueOf(11),
//                BigDecimal.valueOf(1)}, sender);

        // Sätt in och ta ut i en batch
//        processor.tell(new BigDecimal[]{
//                BigDecimal.valueOf(-21),
//                BigDecimal.valueOf(111),
//                BigDecimal.valueOf(1)}, sender);

//        processor.tell("hidetran", null);
//        processor.tell("print", null);
//        processor.tell("showtran", null);

        // Spara en snapshot
//        processor.tell("snap", null);
    processor.tell("print", null);

        Thread.sleep(1000);
        system.shutdown();
    }
}

class ExampleDestination extends UntypedActor {
    @Override
    public void onReceive(Object message) throws Exception {
        System.out.println("sender received " + message+ "from sender:"+sender());
    }
}


// Not used right now
class RandomNumberGenerator{
    private final SecureRandom sr;
    private final int seedIter;
    private final int width;
    private volatile int count=0;

    public RandomNumberGenerator(int width) throws NoSuchAlgorithmException {
        assert width > 0 : "Width "+width+" must be greater than zero";
        sr = SecureRandom.getInstance("SHA1PRNG");
        seedIter = sr.nextInt(width/2+1)+7;
        this.width=width;
    }

    public int nextInt(){
        try{
            return sr.nextInt(width);
        }finally{
            if(++count>seedIter){
                sr.setSeed(sr.generateSeed(7));
                count=0;
            }
        }
    }
}
