import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/*
    PROGRAMMA: Progetto Analisi chimiche dalla dispensa sui socket - Modulo Client
    AUTORE: Lorenzo Porta
    DATA: 22/01/2026
*/

public class AnalisiChimicaClient {
    private static final String NOME_SERVER = "localhost";
    private static final int PORTA_SERVER = 5120;

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.println("Sistema di analisi di formule chimiche.");
        System.out.println("Data una formula chimica, il sistema calcola la percentuale di ogni elemento di cui è composta.");

        String formula;
        do{
            System.out.println("Digitare la formula chimica da analizzare (es. \"H2O\", \"H2SO4\", \"CaCl2\", ecc...).");
            System.out.println("! Se si desidera terminare il programma digitare una riga vuota !");

            System.out.print("Formula: ");
            formula = input.nextLine();

            System.out.println();
            if(!formula.isEmpty()){
                if(sintassiFormulaValida(formula)){
                    analizza(formula);
                }
                else{
                    System.err.println("La formula \"" + formula + "\" presenta una sintassi non valida.");
                }
            }
        } while(!formula.isEmpty());

        System.out.println("È stata digitata una riga vuota.");
        System.out.println("Programma terminato.");
    }

    private static boolean sintassiFormulaValida(String formula) {
        // La formula chimica deve iniziare con una lettera maiuscola
        if(!Character.isUpperCase(formula.charAt(0))){
            return false;
        }
        else {
            for (char c : formula.toCharArray()) {
                // La formula chimica può contenere solo lettere e numeri
                if (!Character.isLetter(c) && !Character.isDigit(c)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static void analizza(String formula) {
        List<String> risposta = new LinkedList<>();

        try(Socket socket = new Socket(NOME_SERVER, PORTA_SERVER)){
            System.out.println("Connesso al server " + socket.getRemoteSocketAddress());
            risposta = comunica(socket, formula);
        }
        catch (IOException ex){
            System.err.println("Errore durante la comunicazione con il server: " + ex.getMessage());
        }

        System.out.println("Risposta del server: ");
        for(String line : risposta){
            System.out.println(line);
        }
        System.out.println();
    }

    private static List<String> comunica(Socket socket, String formula) throws IOException {
        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println(formula);

        String line;
        List<String> risposta = new LinkedList<>();
        while((line = in.readLine()) != null && !line.trim().isEmpty()){
            risposta.add(line);
        }

        return risposta;
    }
}
