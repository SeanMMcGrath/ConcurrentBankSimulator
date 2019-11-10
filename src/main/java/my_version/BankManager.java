package my_version;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author SeanMcGrath
 */
public class BankManager implements Runnable {

    private final int MAX_PEOPLE = 500;

    //seq lock
    private AtomicInteger lock = new AtomicInteger();
    //hashmap to hold bank info
    private volatile HashMap bankDatabase = new HashMap();

    private Person[] people = new Person[MAX_PEOPLE];
    String[] runningTasks = new String[MAX_PEOPLE];//list of user id's that are running at any given time

    private BlockingQueue<Person> finishedTasks = new ArrayBlockingQueue<>(MAX_PEOPLE);


    /**
     * initialize
     * create tasks and threads to run said tasks
     * tasks will simulate stripped-down bank system + users
     */
    public void run() {
        initializeUsers();
        initializeDatabase();
        execute();
    }

    /**
     * adds deposit amount to current stored money
     * loops until a lock is successfully achieved
     *
     * @param id     - user id
     * @param amount - amount to deposit
     */
    public void deposit(String id, int amount) {//read/write
        for (; ; ) {
            int start = lock.get();
            if (start % 2 == 0) {//if not locked
                if (lock.compareAndSet(start, start + 1)) {//lock it, false if someone else has taken lock so loop again
                    bankDatabase.put(id, bankDatabase.get(id) + amount);//deposit
                    lock.incrementAndGet(); //unlock the lock
                    return;
                }
            }
        }
    }

    /**
     * withdraws amount from bank
     * if amount is not enough returns false without withdrawing
     *
     * @param id     - user id
     * @param amount - amount to withdraw
     * @return - true if withdrawn successfully, false otherwise
     */
    public boolean withdraw(String id, int amount) {//read only or read/write
        for (; ; ) {
            int start = lock.get();
            if (start % 2 == 0) {//if not locked
                if (lock.compareAndSet(start, start + 1)) {//lock it, false if someone else has taken lock so loop again
                    if (bankDatabase.get(id) > amount) {//if there is enough money to withdraw
                        //then withdraw, unlock, and return true for success
                        bankDatabase.put(id, bankDatabase.get(id) - amount);
                        lock.incrementAndGet();
                        return true;
                    }
                    //else unlock and return false for failure to withdraw
                    lock.incrementAndGet();
                    return false;
                }
            }
        }
    }


    /**
     * initialize n users into people array
     */
    private void initializeUsers() {
        for (int i = 0; i < MAX_PEOPLE; i++) {
            Person temp = new Person(Integer.toString(i));
            people[i] = temp;
        }

        //set runningtasks to default of -1
        for (int i = 0; i < runningTasks.length; i++) {
            runningTasks[i] = null;
        }
    }

    /**
     * initialize database with account numbers and initial money
     */
    private void initializeDatabase() {
        for (Person p : people) {
            //make database accounts for each person, initialize with random money from $1.00-$19,999.99
            bankDatabase.put(p.getAccountNumber(), ThreadLocalRandom.current().nextInt(1,2000000));
        }
    }

    private void execute() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicBoolean doRun = new AtomicBoolean(true);

        Runnable taskCreator = () -> {//creates and runs tasks
            ExecutorService taskExecutor = Executors.newCachedThreadPool();

            for (int i = 0; i < 1000; i++) {//try 1000 tasks, max 500 concurrently running
                int user = getOpenUser();
                Client temp = new Client(this, people[user], decideTask(), finishedTasks);
                for (int j = 0; j < runningTasks.length; j++) {
                    if (runningTasks[j] == null) {
                        runningTasks[j] = people[user].getAccountNumber();
                    }
                }
                taskExecutor.execute(temp);
            }
            doRun.set(false);
            taskExecutor.shutdown();
            while (!taskExecutor.isShutdown()) {

            }//wait for executor to shutdown
        };
        Runnable taskListener = () -> {//listens for task completions and removes them from list of running tasks, as well as updates people list with new info
            while (doRun.get()) {
                try {
                    Person done = finishedTasks.poll(1, TimeUnit.SECONDS);

                    if (done != null) {
                        //replace old memory of person
                        for (int i = 0; i < people.length; i++) {
                            if (done.getAccountNumber() == people[i].getAccountNumber()) {
                                people[i] = done;
                            }
                        }
                        //remove person from list of running tasks, so new ones can be made of them
                        for (int i = 0; i < runningTasks.length; i++) {
                            if (done.getAccountNumber() == runningTasks[i]) {
                                runningTasks[i] = null;
                            }
                        }
                    } else {
                        doRun.set(false);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("exe stack");
                }
            }
        };

        executor.execute(taskCreator);
        executor.execute(taskListener);
        while (doRun.get()) {
            //
        }
        executor.shutdown();
        while (!executor.isShutdown()) {
            //wait until shutdown to leave
        }
    }

    /**
     * keeps iterating until it randomly finds a user that is not currently being used in a task
     *
     * @return - int for location of open user inside person array
     */
    private int getOpenUser() {
        for (; ; ) {
            boolean use = true;
            int user = ThreadLocalRandom.current().nextInt(0, MAX_PEOPLE);
            for (int i = 0; i < runningTasks.length; i++) {
                if (people[user].getAccountNumber() == runningTasks[i]) {
                    use = false;
                    break;
                    //if already running dont use
                }
            }
            if (use) {
                return user;
            }
        }
    }

    /**
     * decide a task for a thread to simulate
     * 1: withdraw for purchase(read+write)
     * 2: deposit some amount of money to be decided in threadK
     * 3: receive pay(no 'database' access, pay is in local user not bank)
     *
     * @return a number from 1-3 of which task to do, that will be given to a thread
     */
    private int decideTask() {
        return ThreadLocalRandom.current().nextInt(1, 4);
    }

}


//simulates a client connection
class Client extends Thread {

    private final BankManager bm;
    private final Person p;
    private final int task;
    private BlockingQueue<Person> doneListener;

    public Client(BankManager bm, Person p, int task, BlockingQueue<Person> bq) {
        this.bm = bm;
        this.p = p;
        this.task = task;
        this.doneListener = bq;
    }

    @Override
    public void run() {
        //do stuff
        if (task == 1) {
            //make random withdrawal $1.00-$9,999.99
            int withdrawal = ThreadLocalRandom.current().nextInt(1, 1000000);
            boolean success = bm.withdraw(p.getAccountNumber(), withdrawal);
            if (success) {
                //if successful withdrawal add to current money
                p.setMoney(p.getMoney().get() + withdrawal);
            }

        } else if (task == 2) {
            //make random deposit of $1 to current money amount-1
            int deposit = ThreadLocalRandom.current().nextInt(1, p.getMoney().get());
            bm.deposit(p.getAccountNumber(), deposit);
            //modify on-person money
            p.setMoney(p.getMoney().get() - deposit);
        } else if (task == 3) {//increase money by an amount
            p.setMoney(p.getMoney().get() + ThreadLocalRandom.current().nextInt(50000, 2000000));
        }
        try {
            doneListener.put(p);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("client stack");
        }
    }

    public Person getPerson() {
        return p;
    }

}

class Person {
    private String accountNumber;
    private AtomicInteger money;

    public Person(String id) {
        this.accountNumber = id;

        //initial money in USD, last two digits are change
        this.money = new AtomicInteger(ThreadLocalRandom.current().nextInt(0, 100000));
    }

    public void printMoney() {
        System.out.println("User #" + accountNumber + " has $" + (double) money.get() / 100);
    }

    public void setMoney(int newMoney) {
        money.set(newMoney);
    }

    public AtomicInteger getMoney() {
        return money;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
