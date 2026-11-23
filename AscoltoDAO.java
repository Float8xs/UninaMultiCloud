package it.unina.multicloud.dao;
//Appartiene al pacchetto dao (Data Access Object): isola le query SQL dal resto dell'applicazione

import it.unina.multicloud.db.DBConnection;
import it.unina.multicloud.model.ElementoMultimediale;
import it.unina.multicloud.model.Utente;
//Importa la classe per ottenere la connessione al DB PostgreSQL e i modelli di dominio

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
//Importa le classi JDBC per query, statement, risultati ed eccezioni SQL

// Classe dedicata alle query relative agli ascolti e alle statistiche (codificate come Q22, Q24, Q25, Q27) statistiche
public class AscoltoDAO {
	
	// Q22 - Operazione di "UPSERT" (INSERT + UPDATE in un colpo solo, sintassi specifica di PostgreSQL):
    // se la coppia (matricola, idElementoMultimediale) non esiste ancora, inserisce la prima riproduzione (numeroRiproduzioni = 1);
    // se esiste già (ON CONFLICT), incrementa il contatore e aggiorna la data dell'ultimo ascolto.

    public void registraAscolto(String matricolaUtente, int idElemento) throws SQLException {
        String sql = "INSERT INTO ascolti (matricola, idElementoMultimediale, numeroRiproduzioni, dataUltimoAscolto) " +
                "VALUES (?, ?, 1, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (matricola, idElementoMultimediale) DO UPDATE SET " + // qui e' la parte "upsert"
                "numeroRiproduzioni = ascolti.numeroRiproduzioni + 1, dataUltimoAscolto = CURRENT_TIMESTAMP";
        
        // PreparedStatement previene SQL injection e gestisce in modo sicuro i parametri "?"
        // Il try chiude automaticamente il PreparedStatement a fine blocco
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, matricolaUtente);	// Imposta il primo "?" con la matricola dell'utente
            ps.setInt(2, idElemento);	// Imposta il secondo "?" con l'ID dell'elemento multimediale
            ps.executeUpdate();		// Esegue la query di scrittura (INSERT / UPDATE)
        }
    }

    // Q24 - Restituisce i contenuti più riprodotti in assoluto (usato ad esempio nella sezione Tendenze)    
    public List<ElementoMultimediale> topElementi(int limite) throws SQLException {
    	// Somma le riproduzioni di ciascun elemento su tutti gli ascolti, unendo ascolti e elementoMultimediale;
        // ordina dal più ascoltato al meno ascoltato e limita il numero di risultati con LIMIT ?
        String sql = "SELECT e.idElementoMultimediale, e.titolo, e.formato, e.matricolaAutore, e.urlCopertina, " +
                "SUM(a.numeroRiproduzioni) AS totaleRiproduzioni " +
                "FROM ascolti a JOIN elementoMultimediale e ON e.idElementoMultimediale = a.idElementoMultimediale " +
                "GROUP BY e.idElementoMultimediale, e.titolo, e.formato, e.matricolaAutore, e.urlCopertina " +
                "ORDER BY totaleRiproduzioni DESC LIMIT ?";
        List<ElementoMultimediale> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, limite);	// Imposta il limite massimo di elementi da restituire
            try (ResultSet rs = ps.executeQuery()) {	// Esegue la SELECT e ottiene il cursore dei risultati
                while (rs.next()) {		// Scorre una riga alla volta del ResultSet
                    ElementoMultimediale e = new ElementoMultimediale();
                    e.setId(rs.getInt("idElementoMultimediale"));
                    e.setTitolo(rs.getString("titolo"));
                    e.setFormato(rs.getString("formato"));
                    e.setMatricolaAutore(rs.getString("matricolaAutore"));
                    e.setUrlCopertina(rs.getString("urlCopertina"));
                    // Popola il campo derivato "totaleRiproduzioni" con l'alias calcolato dalla SUM(...)
                    e.setTotaleRiproduzioni(rs.getLong("totaleRiproduzioni"));
                    list.add(e);
                }
            }
        }
        return list;
    }

    // Q25 - Recupera la cronologia d'ascolto di un singolo utente, dalla riproduzione più recente alla più vecchia
    public List<ElementoMultimediale> cronologia(String matricola) throws SQLException {
    	
        String sql = "SELECT e.idElementoMultimediale, e.titolo, e.formato, a.numeroRiproduzioni, a.dataUltimoAscolto " +
                "FROM ascolti a JOIN elementoMultimediale e ON e.idElementoMultimediale = a.idElementoMultimediale " +
                "WHERE a.matricola = ? ORDER BY a.dataUltimoAscolto DESC";
        List<ElementoMultimediale> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, matricola);	// Filtra gli ascolti per la matricola richiesta
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ElementoMultimediale e = new ElementoMultimediale();
                    e.setId(rs.getInt("idElementoMultimediale"));
                    e.setTitolo(rs.getString("titolo"));
                    e.setFormato(rs.getString("formato"));
                 // Riutilizza il campo totaleRiproduzioni per memorizzare quante volte l'utente ha ascoltato quel brano
                    e.setTotaleRiproduzioni(rs.getLong("numeroRiproduzioni"));
                    list.add(e);
                }
            }
        }
        return list;
    }

    // Q27 - Statistica sugli utenti ordinati per numero di playlist possedute (decrescente)
    public List<Object[]> utentiConPiuPlaylist() throws SQLException {
        String sql = "SELECT u.matricola, u.nome, u.cognome, COUNT(p.idPlaylist) AS numeroPlaylist " +
                "FROM utente u JOIN playlist p ON p.matricolaProprietario = u.matricola " +
                "GROUP BY u.matricola, u.nome, u.cognome ORDER BY numeroPlaylist DESC";
        List<Object[]> list = new ArrayList<>();
        // Qui non ci sono parametri dinamici ("?"), quindi viene usato uno Statement semplice
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
            	// Costruisce l'oggetto Utente con i campi disponibili dalla query (email lasciata a null)
                Utente u = new Utente(rs.getString("matricola"), rs.getString("nome"), rs.getString("cognome"), null);
             // Inserisce nella lista un array di 2 oggetti: posizione [0] = Utente, posizione [1] = Integer (conteggio)
                list.add(new Object[]{u, rs.getInt("numeroPlaylist")});
            }
        }
        return list;
    }
}
