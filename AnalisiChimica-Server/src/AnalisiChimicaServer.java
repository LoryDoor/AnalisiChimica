import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    PROGRAMMA: Progetto Analisi chimiche dalla dispensa sui socket - Modulo Server
    AUTORE: Lorenzo Porta
    DATA: 12/01/2026
*/

public class AnalisiChimicaServer {
    private static final String FILE_ELEMENTI = "elementiChimici.csv";
    private static HashMap<String, Elemento> TavolaPeriodica = null;
    private static final String Regex = "([A-Z][a-z]?)([0-9]*)";

    private static final int PORTA_SERVER = 5120;

    private static final int MAX_THREAD = 4;
    private static final ExecutorService esecutore = Executors.newFixedThreadPool(MAX_THREAD);
    private static final Object LockConsole = new Object();

    public static void main(String[] args){
        System.out.println("Sistema di analisi di formule chimiche - Modulo Server");
        System.out.println("È in corso l'inizializzazione dell'applicazione.");
        try{
            System.out.println("Caricamento degli elementi della tavola periodica.");
            TavolaPeriodica = caricaElementi();
        }
        catch(IOException ex){
            System.err.println("Errore durante la lettura del file: " + ex.getMessage());
        }

        if(TavolaPeriodica != null){
            try(ServerSocket server = new ServerSocket(PORTA_SERVER)){
                System.out.println("Server in ascolto su: " + server.getLocalSocketAddress());
                while(true){
                    try{
                        Socket tempSocket = server.accept();
                        esecutore.execute(() -> {
                            try(Socket client = tempSocket){
                                synchronized (LockConsole) {
                                    System.out.format("Thread %d: Connesso al client %s%n", Thread.currentThread().getId(), client.getRemoteSocketAddress());
                                }
                                comunica(client);
                            }
                            catch(IOException ex){
                                System.err.println("Thread " + Thread.currentThread().getId() + ": Errore di comunicazione con il client: " + ex.getMessage());
                            }
                        });
                    }
                    catch(IOException ex){
                        System.err.println("Errore durante la gestione di nuove connessioni: " + ex.getMessage());
                    }
                }
            }
            catch(IOException ex){
                System.err.println("Errore durante la creazione del server: " + ex.getMessage());
            }
        }
    }

    private static HashMap<String, Elemento> caricaElementi() throws FileNotFoundException {
        Scanner file = new Scanner(new FileReader(FILE_ELEMENTI));
        HashMap<String, Elemento> elementi = new HashMap<>();

        file.nextLine(); // Consumo la prima riga con le intestazioni

        while(file.hasNextLine()){
            String line = file.nextLine();
            String[] data = line.split(";");
            Elemento elemento = new Elemento(Integer.parseInt(data[0]), data[1], data[2], Double.parseDouble(data[3]));
            elementi.put(data[1], elemento);
        }

        return elementi;
    }

    private static void comunica(Socket client) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()), true);

        String formula = in.readLine().trim();
        synchronized (LockConsole) {
            System.out.println("Thread " + Thread.currentThread().getId() + ": Ricevuta dal client la formuala: " + formula);
        }

        List<String> risultato = elabora(formula);

        System.out.println("Thread " + Thread.currentThread().getId() + ": Invio al client il risultato dell'analisi e chiudo la connessione.");
        for(String s : risultato){
            out.println(s);
        }
        out.println(); // Invio dato tappo
    }

    private static List<String> elabora(String formula) {
        List<String> risposta = new LinkedList<>();

        String nomeHost = "SERVER";
        try {
            nomeHost = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            System.err.println("Errore: " + ex.getMessage());
        }
        String dataOraFormattata = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss z"));

        risposta.add(String.format("Analisi chimica eseguita dal computer %s in data %s", nomeHost , dataOraFormattata));
        risposta.add(String.format("Composto analizzato: %s", formula));

        HashMap<String, Integer> elementi = splitFormula(formula);
        if(elementi.isEmpty()){
            risposta.add(String.format("Sono stati forniti dei parametri errati: %s non è un composto chimico valido.", formula));
        }
        else{
            // Verifico che nella formula siano presenti solo elementi chimici esistenti
            if(validaElementi(elementi)){
                // Calcolo il peso molecolare
                double pesoMolecolare = 0.0;
                for(String key : elementi.keySet()){
                    double pesoAtomicoStandard = TavolaPeriodica.get(key).getPesoAtomicoStandard();
                    int quantita = elementi.get(key);

                    pesoMolecolare += (pesoAtomicoStandard * quantita);
                }

                // Calcolo le percentuali dei singoli elementi
                List<String> percentuali = new LinkedList<>();
                for(String key : elementi.keySet()){
                    String nomeElemento = TavolaPeriodica.get(key).getNome();
                    double pesoAtomicoStandard = TavolaPeriodica.get(key).getPesoAtomicoStandard();

                    // (numero occorrenze * peso atomico standard) : X% = peso molecolare : 100%
                    // percentuale elemento = ((numero occorrenze * peso atomico standard) / peso molecolare) * 100
                    double percentuale = ((elementi.get(key) * pesoAtomicoStandard) / pesoMolecolare) * 100;

                    percentuali.add(String.format("\t%.2f%% %s (%s)", percentuale,  nomeElemento, key));
                }

                percentuali.sort(Collections.reverseOrder());
                risposta.addAll(percentuali);
            }
            else{
                risposta.add(String.format("Sono stati forniti dei parametri errati: \"%s\" non è un composto chimico valido.", formula));
            }
        }

        return risposta;
    }

    private static HashMap<String, Integer> splitFormula(String formula) {
        HashMap<String, Integer> elementi = new HashMap<>();

        if(sintassiFormulaValida(formula)) {
            Pattern pattern = Pattern.compile(Regex); // La regex viene compilata come un oggetto di classe Pattern
            Matcher matcher = pattern.matcher(formula); // Matcher è l'oggetto che si occupa di cercare il pattern all'interno della stringa data

            int lastEnd = 0; // Tiene traccia della fine dell'ultimo match

            // Estrazione di simbolo e quantità
            while (matcher.find()) { // Vero -> C'è un match; Falso -> Non c'è match
                // I gruppi di cattura da noi definiti sono 1-based (lo 0 è riservato)
                String simbolo = matcher.group(1);
                String strQta = matcher.group(2);
                // Se non c'è nulla nel secondo gruppo allora la quantità è sottintesa e vale 1
                int quantita = strQta.isEmpty() ? 1 : Integer.parseInt(strQta);

                elementi.put(simbolo, elementi.getOrDefault(simbolo, 0) + quantita);
                lastEnd = matcher.end(); // Aggiorna la posizione finale
            }

            // Controlla se ci sono caratteri non consumati dopo l'ultimo match
            if(lastEnd < formula.length()) {
                elementi.clear();
//                System.out.println("Dopo la fine della formula c'è altro.");
            }
        }

        return elementi;
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

    private static boolean validaElementi(Map<String, Integer> elementi){
        for(String key : elementi.keySet()){
            if(!TavolaPeriodica.containsKey(key)){
                // L'elemento estratto dalla formula deve esistere nella tavola periodica
                return false;
            }
            if(elementi.get(key) == 0){
                // L'elemento estratto dalla formula non può essere presente in molteplicità 0
                return false;
            }
        }

        return true;
    }
}
