package my_version;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BankManager implements Runnable {

    private final int MAX_PEOPLE = 500;

    private Person[] people = new Person[MAX_PEOPLE];
    int[] runningTasks = new int[MAX_PEOPLE];//list of user id's that are running at any given time


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
     *
     * @param id     - user id
     * @param amount - amount to deposit
     */
    public void deposit(int id, int amount) {//read/write

    }

    /**
     * withdraws amount from bank
     * if amount is not enough returns false without withdrawing
     *
     * @param id     - user id
     * @param amount - amount to withdraw
     * @return - true if withdrawn successfully, false otherwise
     */
    public boolean withdraw(int id, int amount) {//read only or read/write

        return false;
    }


    /**
     * initialize n users into people array
     */
    private void initializeUsers() {
        for (int i = 0; i < MAX_PEOPLE; i++) {
            Person temp = new Person(i);
            people[i] = temp;
        }
    }

    /**
     * initialize database with account numbers and initial money
     */
    private void initializeDatabase() {
        for (Person p: people) {
            //make database accounts for each person, initialize with random money from $1.00-$19,999.99
        }
    }

    private void execute() {

    }

    /**
     * decide a task for a thread to simulate
     * 1: withdraw for purchase(read+write)
     * 2: deposit some amount of money to be decided in thread
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

    public Client(BankManager bm, Person p, int task) {
        this.bm = bm;
        this.p = p;
        this.task = task;
    }

    @Override
    public void run() {
        //do stuff
        if(task == 1){
            //make random withdrawal $1.00-$9,999.99
            int withdrawal = ThreadLocalRandom.current().nextInt(1, 1000000);
            boolean success = bm.withdraw(p.getAccountNumber(), withdrawal);
            if(success){
                //if successful withdrawal add to current money
                p.setMoney(p.getMoney().get()+withdrawal);
            }

        } else if(task == 2){
            //make random deposit of $1 to current money amount-1
            int deposit = ThreadLocalRandom.current().nextInt(1, p.getMoney().get());
            bm.deposit(p.getAccountNumber(), deposit);
            //modify on-person money
            p.setMoney(p.getMoney().get()-deposit);
        } else if(task == 3){//increase money by an amount
            p.setMoney(p.getMoney().get()+ThreadLocalRandom.current().nextInt(50000, 2000000));
        }
        //doneListener.add(p);
    }

    public Person getPerson(){
        return p;
    }

}

class Person {
    private int accountNumber;
    private AtomicInteger money;

    public Person(int id) {
        this.accountNumber = id;

        //initial money in USD, last two digits are change
        this.money = new AtomicInteger(ThreadLocalRandom.current().nextInt(0, 100000));
    }

    public void printMoney() {
        System.out.println("User #" + accountNumber + " has $"+ (double)money.get() / 100);
    }

    public void setMoney(int newMoney) {
        money.set(newMoney);
    }

    public AtomicInteger getMoney() {
        return money;
    }

    public int getAccountNumber() {
        return accountNumber;
    }
}
