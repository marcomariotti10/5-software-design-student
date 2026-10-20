package be.uantwerpen.sd.labs.lab3.singleton;

public class Main {

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        TicketService svc = new TicketService();
        System.out.println(svc.create("Printer broken"));
        System.out.println(svc.create("Monitor flicker"));
    }
}
