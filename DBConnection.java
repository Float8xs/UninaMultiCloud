package it.unina.multicloud.db; //Appartiene al pacchetto db, dedicato alla gestione della connessione al database

//Classi standard di java.sql per rappresentare la connessione, ottenerla tramite DriverManager e gestire errori SQL
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//Gestisce la connessione al DB (postgres)
//"final": la classe non può essere estesa (non ha senso creare sottoclassi di una utility di connessione)
public final class DBConnection {
	// Valori predefiniti per collegarsi a PostgreSQL in locale
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "5432";
    private static final String DEFAULT_DB   = "postgres";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASS = "victus015";

    private static Connection connection; // Riferimento statico all'unica istanza di Connection gestita da questa classe
    //Unica connessione condivisa
    
    private DBConnection() { } // Costruttore privato: impedisce di fare "new DBConnection()" da fuori, perché metodi e campi sono statici

    // Metodo statico e "synchronized" (thread-safe): se due thread chiamano getConnection() nello stesso momento,
    // entrano uno alla volta evitando di creare connessioni duplicate o interferire sullo stato
    public static synchronized Connection getConnection() throws SQLException {
    	// Crea una nuova connessione solo se non è mai stata aperta o se risulta già chiusa
        if (connection == null || connection.isClosed()) { 
        	// Carica eventuali parametri personalizzati dal file db.properties
            java.util.Properties p = loadOverrides();
            String host = p.getProperty("host", DEFAULT_HOST);
            String port = p.getProperty("port", DEFAULT_PORT);
            String db   = p.getProperty("database", DEFAULT_DB);
            String user = p.getProperty("user", DEFAULT_USER);
            String pass = p.getProperty("password", DEFAULT_PASS);
            // Legge ciascun parametro dal file; se manca o non c'è il file, usa il valore di DEFAULT_*
            
            // Costruisce la stringa JDBC standard per PostgreSQL
            String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
            try {
            	// Carica esplicitamente la classe del driver JDBC di PostgreSQL nel classloader
                Class.forName("org.postgresql.Driver"); 
            } catch (ClassNotFoundException e) {
            	// Se il .jar del driver Postgres non è nel classpath, rilancia una SQLException incapsulando l'errore
                throw new SQLException("Driver JDBC PostgreSQL non trovato nel classpath.", e);
            }
            connection = DriverManager.getConnection(url, user, pass);
         // Apre effettivamente la connessione verso il DB usando URL, utente e password
        }
        return connection;	// Restituisce la connessione attiva
    }

 // Cerca un file "db.properties" nella directory di lavoro corrente e ne carica le coppie chiave=valore
    private static java.util.Properties loadOverrides() {
        java.util.Properties p = new java.util.Properties();
        java.io.File f = new java.io.File("db.properties");
        if (f.exists()) {
        	// try-with-resources: chiude automaticamente il FileInputStream a fine blocco, anche in caso di eccezione
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                p.load(in);
            } catch (java.io.IOException ignored) {
            	// In caso di errore di lettura, ignora e lascia Properties vuoto (verranno usati i default)
            }
        }
        return p;
    }

    // Metodo di utilità per chiudere la connessione quando non serve più
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            	// Ignora eventuali errori in fase di chiusura
            } finally {
                connection = null;
                // Azzera sempre il riferimento così al prossimo getConnection() verrà riaperta dinuovo
            }
        }
    }
}
