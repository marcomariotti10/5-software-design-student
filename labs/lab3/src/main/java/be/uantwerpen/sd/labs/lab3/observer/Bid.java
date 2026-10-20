package be.uantwerpen.sd.labs.lab3.observer;

public class Bid {
    private String bidder;
    private int amount;

    public Bid(String bidder, int amount) {
        this.bidder = bidder;
        this.amount = amount;
    }

    public String getBidder() {
        return bidder;
    }

    public void setBidder(String bidder) {
        this.bidder = bidder;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
