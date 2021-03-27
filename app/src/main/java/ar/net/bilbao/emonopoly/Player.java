package ar.net.bilbao.emonopoly;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;

public class Player implements Serializable {
    private static int lastIndex = 0;

    private final int index;
    private String cardUID;
    private int color;
    private final String name;
    private int money;
    private boolean hasLost;
    private final boolean isBanker;

    public Player(int color, String name, int money, boolean isBanker) {
        this.index = lastIndex++;
        this.color = color;
        this.name = name;
        this.money = money;
        this.isBanker = isBanker;
        this.hasLost = false;
        this.cardUID = "";
    }

    public Player(int color, String name, int money) {
        this(color, name, money, false);
    }

    public Player(int color, String name, boolean isBanker) {
        this(color, name, Integer.MAX_VALUE, isBanker);
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

    /**
     * @return A string with the money of the player, formatted with user's locales
     */
    public String getLocalizedMoney() {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(this.money);
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public void giveMoney(int amount) {
        if (this.isBanker) return;

        this.money += amount;
    }

    public boolean takeMoney(int amount, boolean allowNegative) {
        if (this.isBanker) return true;
        if (!allowNegative && this.money - amount < 0) return false;

        this.money -= amount;
        return true;
    }

    public boolean hasLost() {
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

    public boolean isBanker() {
        return isBanker;
    }
}
