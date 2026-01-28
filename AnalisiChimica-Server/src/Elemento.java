/*
    PROGRAMMA: Progetto Analisi chimiche dalla dispensa sui socket - Classe Elemento
    AUTORE: Lorenzo Porta
    DATA: 12/01/2026
*/

public class Elemento {
    private final int Id;
    private final String Simbolo;
    private final String Nome;
    private final double PesoAtomicoStandard;

    public Elemento(int id, String simbolo, String nome, double pesoAtomicoStandard) {
        this.Id = id;
        this.Simbolo = simbolo;
        this.Nome = nome;
        this.PesoAtomicoStandard = pesoAtomicoStandard;
    }

    public int getId() {
        return Id;
    }

    public String getSimbolo() {
        return Simbolo;
    }

    public String getNome() {
        return Nome;
    }

    public double getPesoAtomicoStandard() {
        return PesoAtomicoStandard;
    }

    @Override
    public String toString() {
        return String.format("%d;%s;%s;%f", Id, Simbolo, Nome, PesoAtomicoStandard);
    }
}
