// Appartiene al pacchetto dao: gestisce l'accesso ai dati per la tabella elementoMultimediale
package it.unina.multicloud.dao;

// Importa la gestione della connessione PostgreSQL e il modello ElementoMultimediale[cite: 1]
import it.unina.multicloud.db.DBConnection;
import it.unina.multicloud.model.ElementoMultimediale;

// Classi JDBC per PreparedStatement, Statement, ResultSet, tipi SQL ed eccezioni
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// DAO dedicato alle operazioni Q6-Q11, Q26, Q28 e al recupero del feed
public class ElementoMultimedialeDAO {

    // Q6 - Inserisce un nuovo elemento multimediale e restituisce l'ID auto-generato dal DB (o -1 se non ottenuto)
    public int carica(ElementoMultimediale e) throws SQLException {
        String sql = "INSERT INTO elementoMultimediale " +
                "(titolo, descrizione, formato, durata, dataCreazione, urlFile, urlCopertina, matricolaAutore) " +
                "VALUES (?,?,?,?,?,?,?,?)";

        // Statement.RETURN_GENERATED_KEYS richiede al driver JDBC di restituire le chiavi generate (es. SERIAL/IDENTITY)
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getTitolo());
            ps.setString(2, e.getDescrizione());
            ps.setString(3, e.getFormato());

            // Se la durata è null, imposta esplicitamente SQL NULL; altrimenti imposta il valore intero
            if (e.getDurata() != null) ps.setInt(4, e.getDurata());
            else ps.setNull(4, Types.INTEGER);

            ps.setDate(5, e.getDataCreazione());
            ps.setString(6, e.getUrlFile());
            ps.setString(7, e.getUrlCopertina());
            ps.setString(8, e.getMatricolaAutore());

            ps.executeUpdate(); // Esegue l'INSERT

            // Recupera il ResultSet contenente la chiave primaria generata per la nuova riga
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1; // Valore di fallback se nessuna chiave è stata restituita
    }

    // Q7 - Aggiorna titolo e descrizione di un elemento SOLO se l'utente corrisponde a matricolaAutore
    public boolean modifica(int idElemento, String matricolaAutore, String nuovoTitolo, String nuovaDescrizione) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "UPDATE elementoMultimediale SET titolo = ?, descrizione = ? WHERE idElementoMultimediale = ? AND matricolaAutore = ?")) {
            ps.setString(1, nuovoTitolo);
            ps.setString(2, nuovaDescrizione);
            ps.setInt(3, idElemento);
            ps.setString(4, matricolaAutore);
            // executeUpdate() restituisce il numero di righe modificate: > 0 significa che l'operazione è riuscita
            return ps.executeUpdate() > 0;
        }
    }

    // Q8 - Elimina un elemento multimediale SOLO se appartiene all'autore indicato
    public boolean elimina(int idElemento, String matricolaAutore) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "DELETE FROM elementoMultimediale WHERE idElementoMultimediale = ? AND matricolaAutore = ?")) {
            ps.setInt(1, idElemento);
            ps.setString(2, matricolaAutore);
            // Restituisce true se è stata eliminata almeno una riga, false se l'elemento non esiste o non è dell'autore
            return ps.executeUpdate() > 0;
        }
    }

    // Q9 - Restituisce tutti gli elementi caricati da un determinato autore, ordinati per dataCreazione decrescente
    public List<ElementoMultimediale> getByAutore(String matricola) throws SQLException {
        String sql = "SELECT idElementoMultimediale, titolo, formato, dataCreazione, descrizione, durata, " +
                "urlFile, urlCopertina, matricolaAutore, dataCaricamento " +
                "FROM elementoMultimediale WHERE matricolaAutore = ? ORDER BY dataCreazione DESC";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, matricola);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs); // Converte tutte le righe del ResultSet in una List<ElementoMultimediale>
            }
        }
    }

    // Q10 - Ricerca parziale case-insensitive sul titolo tramite ILIKE (specifico di PostgreSQL)
    public List<ElementoMultimediale> cercaPerTitolo(String parolaChiave) throws SQLException {
        String sql = "SELECT idElementoMultimediale, titolo, formato, matricolaAutore, descrizione, durata, " +
                "dataCreazione, urlFile, urlCopertina, dataCaricamento " +
                "FROM elementoMultimediale WHERE titolo ILIKE ? ORDER BY dataCreazione DESC";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            // Aggiunge i caratteri jolly "%" prima e dopo per cercare la sottostringa in qualsiasi posizione
            ps.setString(1, "%" + parolaChiave + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        }
    }

    // Recupera tutti gli elementi multimediali ordinati per dataCaricamento (dal più recente) per il feed della home
    public List<ElementoMultimediale> getAll() throws SQLException {
        String sql = "SELECT idElementoMultimediale, titolo, formato, matricolaAutore, descrizione, durata, " +
                "dataCreazione, urlFile, urlCopertina, dataCaricamento " +
                "FROM elementoMultimediale ORDER BY dataCaricamento DESC";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return mapAll(rs);
        }
    }

    // Q11 - Recupera i dettagli completi di un elemento, includendo nome/cognome dell'autore e totale riproduzioni
    public ElementoMultimediale getDettaglio(int idElemento) throws SQLException {
        // INNER JOIN su utente per ottenere i dati dell'autore;
        // LEFT JOIN su ascolti per includere l'elemento anche se ha 0 ascolti;
        // COALESCE(..., 0) trasforma l'eventuale NULL della SUM in 0
        String sql = "SELECT e.idElementoMultimediale, e.titolo, e.formato, e.durata, e.dataCreazione, " +
                "e.descrizione, e.urlFile, e.urlCopertina, e.matricolaAutore, e.dataCaricamento, " +
                "u.nome, u.cognome, COALESCE(SUM(a.numeroRiproduzioni), 0) AS totaleRiproduzioni " +
                "FROM elementoMultimediale e " +
                "JOIN utente u ON u.matricola = e.matricolaAutore " +
                "LEFT JOIN ascolti a ON a.idElementoMultimediale = e.idElementoMultimediale " +
                "WHERE e.idElementoMultimediale = ? " +
                "GROUP BY e.idElementoMultimediale, e.titolo, e.formato, e.durata, e.dataCreazione, " +
                "e.descrizione, e.urlFile, e.urlCopertina, e.matricolaAutore, e.dataCaricamento, u.nome, u.cognome";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idElemento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ElementoMultimediale e = map(rs);
                    e.setNomeAutore(rs.getString("nome"));
                    e.setCognomeAutore(rs.getString("cognome"));
                    e.setTotaleRiproduzioni(rs.getLong("totaleRiproduzioni"));
                    return e;
                }
            }
        }
        return null; // Nessun elemento trovato con quell'ID
    }

    // Q26 - Conta quanti elementi esistono per ciascun formato (es. Audio, Video) mantenendo l'ordine con LinkedHashMap
    public java.util.Map<String, Integer> distribuzionePerFormato() throws SQLException {
        java.util.Map<String, Integer> m = new java.util.LinkedHashMap<>();
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT formato, COUNT(*) AS numeroElementi FROM elementoMultimediale GROUP BY formato")) {
            while (rs.next()) {
                m.put(rs.getString("formato"), rs.getInt("numeroElementi"));
            }
        }
        return m;
    }

    // Q28 - Trova elementi creati fino a 'giorniInattivita' giorni fa che non risultano mai riprodotti nella tabella ascolti
    public List<ElementoMultimediale> maiRiprodottiDaGiorni(int giorniInattivita) throws SQLException {
        // LEFT JOIN + filtro "a.idElementoMultimediale IS NULL" isola le righe senza corrispondenza in 'ascolti' (anti-join);
        // CURRENT_DATE - ?::int sottrae un numero intero di giorni in PostgreSQL
        String sql = "SELECT e.idElementoMultimediale, e.titolo, e.matricolaAutore, e.dataCreazione, e.formato, " +
                "e.descrizione, e.durata, e.urlFile, e.urlCopertina, e.dataCaricamento " +
                "FROM elementoMultimediale e " +
                "LEFT JOIN ascolti a ON a.idElementoMultimediale = e.idElementoMultimediale " +
                "WHERE a.idElementoMultimediale IS NULL AND e.dataCreazione <= CURRENT_DATE - ?::int";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, giorniInattivita);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        }
    }

    // Scorre tutte le righe di un ResultSet e le raccoglie in una lista di oggetti ElementoMultimediale
    private List<ElementoMultimediale> mapAll(ResultSet rs) throws SQLException {
        List<ElementoMultimediale> list = new ArrayList<>();
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    // Mappa una singola riga del ResultSet su un oggetto ElementoMultimediale
    private ElementoMultimediale map(ResultSet rs) throws SQLException {
        ElementoMultimediale e = new ElementoMultimediale();
        e.setId(rs.getInt("idElementoMultimediale"));
        e.setTitolo(rs.getString("titolo"));
        e.setFormato(rs.getString("formato"));
        e.setMatricolaAutore(rs.getString("matricolaAutore"));

        // Blocchi try/catch per colonna: consentono di riutilizzare map(rs) anche quando una query
        // seleziona solo un sottoinsieme delle colonne opzionali
        try { e.setDescrizione(rs.getString("descrizione")); } catch (SQLException ignored) { }
        try {
            int d = rs.getInt("durata");
            // rs.wasNull() verifica se l'ultima colonna letta (durata) era SQL NULL (perché getInt torna 0)
            e.setDurata(rs.wasNull() ? null : d);
        } catch (SQLException ignored) { }
        try { e.setDataCreazione(rs.getDate("dataCreazione")); } catch (SQLException ignored) { }
        try { e.setUrlFile(rs.getString("urlFile")); } catch (SQLException ignored) { }
        try { e.setUrlCopertina(rs.getString("urlCopertina")); } catch (SQLException ignored) { }
        try { e.setDataCaricamento(rs.getTimestamp("dataCaricamento")); } catch (SQLException ignored) { }
        return e;
    }
}