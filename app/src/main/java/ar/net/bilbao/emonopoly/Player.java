package ar.net.bilbao.emonopoly;

public class Player {
    private static int lastIndex = 0;

    private int index;
    private String cardUID;
    private int color;
    private String name;
    private int money;
    private boolean hasLost;

    public Player(int color, String name, int money) {
        this.index = lastIndex++;
        this.color = color;
        this.name = name;
        this.money = money;
        this.hasLost = false;
    }

    public int getIndex() {
        return index;
    }

    public int getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public boolean perdio() {
        return hasLost;
    }

    public void setHasLost(boolean hasLost) {
        this.hasLost = hasLost;
    }

    public String getCardUID() {
        return cardUID;
    }

    public void setCardUID(String cardUID) {
        this.cardUID = cardUID;
    }
}
