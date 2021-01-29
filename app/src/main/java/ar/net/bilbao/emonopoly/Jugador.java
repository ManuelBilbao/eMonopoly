package ar.net.bilbao.emonopoly;

public class Jugador {
    private static int lastIndex = 0;

    private int index;
    private String cardUID;
    private int color;
    private String nombre;
    private int plata;
    private boolean perdio;

    public Jugador(int color, String nombre, int plata) {
        this.index = lastIndex++;
        this.color = color;
        this.nombre = nombre;
        this.plata = plata;
        this.perdio = false;
    }

    public int getIndex() {
        return index;
    }

    public int getColor() {
        return color;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPlata() {
        return plata;
    }

    public void setPlata(int plata) {
        this.plata = plata;
    }

    public boolean perdio() {
        return perdio;
    }

    public void setPerdio(boolean perdio) {
        this.perdio = perdio;
    }

    public String getCardUID() {
        return cardUID;
    }

    public void setCardUID(String cardUID) {
        this.cardUID = cardUID;
    }
}
