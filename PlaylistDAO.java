// Appartiene al pacchetto dao: gestisce l'accesso ai dati per playlist, composizione e accessi condivisi
package it.unina.multicloud.dao;

// Importa la connessione al DB e le classi del modello di dominio[cite: 1]
import it.unina.multicloud.db.DBConnection;
import it.unina.multicloud.model.ElementoMultimediale;
import it.unina.multicloud.model.Playlist;
import it.unina.multicloud.model.Utente;

// Classi JDBC e collezioni per gestire query, risultati e liste
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// DAO per le query Q12-Q21 e Q29 relative alle playlist
public class PlaylistDAO {

    // Q12 - Crea una nuova playlist e restituisce l'ID auto-generato dal database (oppure -1)
    public int crea(String titolo, String visibilita, String matricolaProprietario) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "INSERT INTO playlist (titolo, visibilita, matricolaProprietario) VALUES (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, titolo);
            ps.setString(2, visibilita);
            ps.setString(3, matricolaProprietario);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    // Q13 - Inserisce un elemento in composizionePlaylist assegnandogli la prossima posizione libera (MAX + 1).
    // La sottoquery SELECT calcola la posizione direttamente nel DB; COALESCE(..., 0) restituisce 0 se la playlist è vuota.
    // Il campo numeroElementi in 'playlist' viene aggiornato automaticamente da un trigger sul database.
    public void aggiungiElemento(int idPlaylist, int idElemento) throws SQLException {
        String sql = "INSERT INTO composizionePlaylist (idPlaylist, idElementoMultimediale, posizione) " +
                "SELECT ?, ?, COALESCE(MAX(posizione), 0) + 1 FROM composizionePlaylist WHERE idPlaylist = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idPlaylist);
            ps.setInt(2, idElemento);
            ps.setInt(3, idPlaylist);
            ps.executeUpdate();
        }
    }

    // Q14 - Rimuove uno specifico elemento multimediale da una playlist
    public void rimuoviElemento(int idPlaylist, int idElemento) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "DELETE FROM composizionePlaylist WHERE idPlaylist = ? AND idElementoMultimediale = ?")) {
            ps.setInt(1, idPlaylist);
            ps.setInt(2, idElemento);
            ps.executeUpdate();
        }
    }

    // Q15 - Restituisce gli elementi multimediali di una playlist rispettando l'ordine impostato da cp.posizione
    public List<ElementoMultimediale> contenuto(int idPlaylist) throws SQLException {
        String sql = "SELECT cp.posizione, e.idElementoMultimediale, e.titolo, e.formato, e.durata, " +
                "e.matricolaAutore, e.descrizione, e.dataCreazione, e.urlFile, e.urlCopertina, e.dataCaricamento " +
                "FROM composizionePlaylist cp " +
                "JOIN elementoMultimediale e ON e.idElementoMultimediale = cp.idElementoMultimediale " +
                "WHERE cp.idPlaylist = ? ORDER BY cp.posizione";
        List<ElementoMultimediale> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idPlaylist);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ElementoMultimediale e = new ElementoMultimediale();
                    e.setId(rs.getInt("idElementoMultimediale"));
                    e.setTitolo(rs.getString("titolo"));
                    e.setFormato(rs.getString("formato"));
                    int d = rs.getInt("durata");
                    // Se 'durata' era SQL NULL, rs.wasNull() vale true e impostiamo null su Integer
                    e.setDurata(rs.wasNull() ? null : d);
                    e.setMatricolaAutore(rs.getString("matricolaAutore"));
                    e.setDescrizione(rs.getString("descrizione"));
                    e.setDataCreazione(rs.getDate("dataCreazione"));
                    e.setUrlFile(rs.getString("urlFile"));
                    e.setUrlCopertina(rs.getString("urlCopertina"));
                    e.setDataCaricamento(rs.getTimestamp("dataCaricamento"));
                    list.add(e);
                }
            }
        }
        return list;
    }

    // Q16 - Cambia la visibilità di una playlist solo se l'utente richiedente è il proprietario
    public boolean cambiaVisibilita(int idPlaylist, String nuovaVisibilita, String matricolaProprietario) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "UPDATE playlist SET visibilita = ? WHERE idPlaylist = ? AND matricolaProprietario = ?")) {
            ps.setString(1, nuovaVisibilita);
            ps.setInt(2, idPlaylist);
            ps.setString(3, matricolaProprietario);
            return ps.executeUpdate() > 0;
        }
    }

    // Q17 - Elimina una playlist solo se appartiene a matricolaProprietario
    public boolean elimina(int idPlaylist, String matricolaProprietario) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "DELETE FROM playlist WHERE idPlaylist = ? AND matricolaProprietario = ?")) {
            ps.setInt(1, idPlaylist);
            ps.setString(2, matricolaProprietario);
            return ps.executeUpdate() > 0;
        }
    }

    // Q18 - Concede l'accesso a una playlist a un altro utente (eventuali vincoli su playlist CONDIVISA sono gestiti da trigger sul DB)
    public void concediAccesso(int idPlaylist, String matricolaOspite) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "INSERT INTO accessoPlaylist (idPlaylist, matricola) VALUES (?,?)")) {
            ps.setInt(1, idPlaylist);
            ps.setString(2, matricolaOspite);
            ps.executeUpdate();
        }
    }

    // Q19 - Revoca l'accesso a un utente ospite su una playlist
    public void revocaAccesso(int idPlaylist, String matricolaOspite) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "DELETE FROM accessoPlaylist WHERE idPlaylist = ? AND matricola = ?")) {
            ps.setInt(1, idPlaylist);
            ps.setString(2, matricolaOspite);
            ps.executeUpdate();
        }
    }

    // Q20 - Restituisce la lista degli utenti autorizzati ad accedere a una playlist tramite accessoPlaylist
    public List<Utente> utentiAutorizzati(int idPlaylist) throws SQLException {
        String sql = "SELECT u.matricola, u.nome, u.cognome FROM accessoPlaylist ap " +
                "JOIN utente u ON u.matricola = ap.matricola WHERE ap.idPlaylist = ?";
        List<Utente> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idPlaylist);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Utente(rs.getString("matricola"), rs.getString("nome"), rs.getString("cognome"), null));
                }
            }
        }
        return list;
    }

    // Q21 - Restituisce tutte le playlist visibili a 'matricolaUtente':
    // 1) di cui è proprietario, 2) PUBBLICA, oppure 3) CONDIVISA e presente in accessoPlaylist per quell'utente.
    // DISTINCT evita duplicati; il parametro matricolaUtente va passato due volte (una nel LEFT JOIN, una nel WHERE).
    public List<Playlist> visibiliPerUtente(String matricolaUtente) throws SQLException {
        String sql = "SELECT DISTINCT p.idPlaylist, p.titolo, p.visibilita, p.matricolaProprietario, " +
                "p.dataCreazione, p.numeroElementi " +
                "FROM playlist p " +
                "LEFT JOIN accessoPlaylist ap ON ap.idPlaylist = p.idPlaylist AND ap.matricola = ? " +
                "WHERE p.matricolaProprietario = ? OR p.visibilita = 'PUBBLICA' " +
                "OR (p.visibilita = 'CONDIVISA' AND ap.matricola IS NOT NULL)";
        List<Playlist> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, matricolaUtente);
            ps.setString(2, matricolaUtente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // Q29 - Calcola la somma delle durate (in secondi) di tutti gli elementi contenuti nella playlist.
    // Restituisce Long (wrapper) così può valere null se la durata non è disponibile / non calcolabile.
    public Long durataTotaleSecondi(int idPlaylist) throws SQLException {
        String sql = "SELECT SUM(e.durata) AS durataTotaleSecondi FROM playlist p " +
                "JOIN composizionePlaylist cp ON cp.idPlaylist = p.idPlaylist " +
                "JOIN elementoMultimediale e ON e.idElementoMultimediale = cp.idElementoMultimediale " +
                "WHERE p.idPlaylist = ? GROUP BY p.idPlaylist";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idPlaylist);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long v = rs.getLong(1);
                    return rs.wasNull() ? null : v;
                }
            }
        }
        return null;
    }

    // Converte una riga del ResultSet in un oggetto Playlist
    private Playlist map(ResultSet rs) throws SQLException {
        Playlist p = new Playlist();
        p.setId(rs.getInt("idPlaylist"));
        p.setTitolo(rs.getString("titolo"));
        p.setVisibilita(rs.getString("visibilita"));
        p.setMatricolaProprietario(rs.getString("matricolaProprietario"));
        p.setDataCreazione(rs.getDate("dataCreazione"));
        p.setNumeroElementi(rs.getInt("numeroElementi"));
        return p;
    }
}