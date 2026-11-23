// Appartiene al pacchetto dao: gestisce l'accesso ai dati per 'utente' e 'corsoLaurea'
package it.unina.multicloud.dao;

// Importa la gestione della connessione al DB e il modello Utente
import it.unina.multicloud.db.DBConnection;
import it.unina.multicloud.model.Utente;

// Classi JDBC, crittografia e collezioni Java
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

// DAO per le query Q1-Q5 e le operazioni di supporto su utenti e corsi di laurea
public class UtenteDAO {

    // Q1 - Registra un nuovo utente e (se presente) il suo corso di laurea dentro un'unica transazione SQL:
    // o entrambe le operazioni vanno a buon fine (commit), o nessuna viene salvata (rollback)
    public void registraUtente(Utente u, String corsoLaurea) throws SQLException {
        Connection conn = DBConnection.getConnection();
        // Salva lo stato iniziale di autoCommit per ripristinarlo alla fine
        boolean prevAutoCommit = conn.getAutoCommit();
        // Disattiva l'auto-commit così le query restano in sospeso finché non chiamiamo commit()
        conn.setAutoCommit(false);
        try {
            // Prima query: inserisce i dati principali dell'utente (la password arriva già in forma di hash)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO utente (matricola, nome, cognome, email, password) VALUES (?,?,?,?,?)")) {
                ps.setString(1, u.getMatricola());
                ps.setString(2, u.getNome());
                ps.setString(3, u.getCognome());
                ps.setString(4, u.getEmail());
                ps.setString(5, u.getPasswordHash());
                ps.executeUpdate();
            }

            // Seconda query (facoltativa): se corsoLaurea è valorizzato e non vuoto, lo inserisce nella tabella collegata
            if (corsoLaurea != null && !corsoLaurea.isBlank()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO corsoLaurea (matricola, corsoLaurea) VALUES (?,?)")) {
                    ps.setString(1, u.getMatricola());
                    ps.setString(2, corsoLaurea);
                    ps.executeUpdate();
                }
            }

            // Conferma definitivamente entrambe le modifiche sul database
            conn.commit();
        } catch (SQLException e) {
            // Annulla tutte le modifiche fatte nella transazione corrente in caso di errore SQL
            conn.rollback();
            // Rilancia l'eccezione così il chiamante (es. la GUI) può gestirla e mostrare un messaggio
            throw e;
        } finally {
            // Ripristina sempre la modalità autoCommit originale, anche se è avvenuta un'eccezione
            conn.setAutoCommit(prevAutoCommit);
        }
    }

    // Q2 - Verifica le credenziali confrontando l'hash salvato nel DB con lo SHA-256 della password inserita.
    // Restituisce null sia se l'email non esiste sia se la password non corrisponde, per non dare indizi all'esterno
    public Utente login(String email, String passwordChiara) throws SQLException {
        String sql = "SELECT matricola, nome, cognome, password FROM utente WHERE email = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password");
                    // Confronta l'hash memorizzato con l'hash calcolato sulla password in chiaro
                    if (hash.equals(sha256(passwordChiara))) {
                        Utente u = new Utente(rs.getString("matricola"), rs.getString("nome"),
                                rs.getString("cognome"), email);
                        u.setPasswordHash(hash);
                        // Carica anche i corsi di laurea associati all'utente
                        u.setCorsiLaurea(getCorsiLaurea(u.getMatricola()));
                        return u;
                    }
                }
            }
        }
        return null;
    }

    // Q3 - Aggiorna la password dell'utente salvandone l'hash SHA-256
    public void aggiornaPassword(String matricola, String nuovaPasswordChiara) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "UPDATE utente SET password = ? WHERE matricola = ?")) {
            ps.setString(1, sha256(nuovaPasswordChiara));
            ps.setString(2, matricola);
            ps.executeUpdate();
        }
    }

    // Q4 - Recupera tutti i corsi di laurea di un utente (relazione 1-a-N modellata tramite tabella 'corsoLaurea')
    public List<String> getCorsiLaurea(String matricola) throws SQLException {
        List<String> corsi = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "SELECT corsoLaurea FROM corsoLaurea WHERE matricola = ?")) {
            ps.setString(1, matricola);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    corsi.add(rs.getString(1));
                }
            }
        }
        return corsi;
    }

    // Q5 - Elimina l'utente con la matricola indicata dalla tabella 'utente'
    public void eliminaUtente(String matricola) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "DELETE FROM utente WHERE matricola = ?")) {
            ps.setString(1, matricola);
            ps.executeUpdate();
        }
    }

    // Verifica se una matricola è già presente in 'utente' (utile per dare un feedback chiaro in registrazione)
    public boolean matricolaEsiste(String matricola) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "SELECT 1 FROM utente WHERE matricola = ?")) {
            ps.setString(1, matricola);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true se esiste almeno una riga con quella matricola
            }
        }
    }

    // Verifica se un indirizzo email è già registrato nella tabella 'utente'
    public boolean emailEsiste(String email) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "SELECT 1 FROM utente WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Recupera i dati completi di un utente (inclusa dataRegistrazione e corsiLaurea) per la vista profilo
    public Utente getByMatricola(String matricola) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "SELECT matricola, nome, cognome, email, dataRegistrazione FROM utente WHERE matricola = ?")) {
            ps.setString(1, matricola);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Utente u = new Utente(rs.getString("matricola"), rs.getString("nome"),
                            rs.getString("cognome"), rs.getString("email"));
                    u.setDataRegistrazione(rs.getTimestamp("dataRegistrazione"));
                    u.setCorsiLaurea(getCorsiLaurea(matricola));
                    return u;
                }
            }
        }
        return null;
    }

    // Calcola l'hash esadecimale SHA-256 (64 caratteri minuscoli) di una stringa in UTF-8
    public static String sha256(String testoChiaro) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(testoChiaro.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            // %02x converte ogni byte in 2 cifre esadecimali minuscole con eventuale zero iniziale
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            // Eccezioni considerate non recuperabili in un ambiente JVM standard che supporta SHA-256 e UTF-8
            throw new RuntimeException(e);
        }
    }
}