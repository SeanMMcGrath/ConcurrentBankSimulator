package jdk_version;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BankManager implements Runnable{

    private final int MAX_PEOPLE = 500;
    private final int MAX_CONCURRENT_TASKS = 4;

    ConcurrentHashMap<String,AtomicReference<BigDecimal>> bankDatabase = new ConcurrentHashMap<>();
    Person[] people = new Person[MAX_PEOPLE];
    Client[] runningTasks = new Client[MAX_CONCURRENT_TASKS];

    /**
     * initialize
     * create tasks and threads to run said tasks
     * tasks will simulate
     */
    public void run() {
        initializeUsers();
        initializeDatabase();

        //executor?

    }



    /**
     * initialize n users into people array
     */
    private void initializeUsers(){

    }

    /**
     * initialize database with account numbers and
     */
    private void initializeDatabase(){

    }

    /**
     * decide a task for a thread to simulate
     * 1: withdraw for purchase(read+write)
     * 2: deposit some amount of money to be decided in thread
     * 3: receive pay(no 'database' access, pay is in local user not bank)
     * @return a number from 1-3 of which task to do, that will be given to a thread
     */
    private int decideTask(){
        return ThreadLocalRandom.current().nextInt(1, 4);
    }

}


//simulates a client connnection
class Client extends Thread{

    private final BankManager bm;
    private final Person p;
    private final ConcurrentHashMap database;
    private final int task;

    public Client(BankManager bm, ConcurrentHashMap database, Person p, int task){
        this.bm = bm;
        this.p = p;
        this.database = database;
        this.task = task;
    }

    @Override
    public void run() {
        //do stuff

        //AtomicReference<BigDecimal> money
    }


}

class Person {
    String accountNumber;
    private AtomicInteger money;

    public Person(){
        this.accountNumber = hash();

        //initial money in USD, last two digits are change
        this.money = new AtomicInteger(ThreadLocalRandom.current().nextInt(0, 100000));
    }

    private String hash(){
        return null;
    }

    public void printMoney(){
        System.out.println(money.get()/100);
    }


    public void setMoney(int newMoney){ money.set(newMoney);}
    public AtomicInteger getMoney(){return money;}
    public String getAccountNumber(){return accountNumber;}
}