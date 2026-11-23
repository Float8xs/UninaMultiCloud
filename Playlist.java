package it.unina.multicloud.model;
//Appartiene al pacchetto model: rappresenta l'entità Playlist del dominio applicativo

import java.sql.Date;
//Tipo Date di java.sql per mappare la colonna data del database (solo data, senza orario)

//Classe modello che rappresenta una playlist
public class Playlist {
 
	// Costanti pubbliche e condivise a livello di classe (public static final)
    // Servono per evitare di riscrivere stringhe a mano in giro per il codice ed evitare refusi
    public static final String PUBBLICA = "PUBBLICA";
    public static final String PRIVATA = "PRIVATA";
    public static final String CONDIVISA = "CONDIVISA";

    private int id; // Identificativo univoco della playlist (chiave primaria sul database)
    private String titolo;  //Nome della playlist
    private String visibilita; 	// Livello di visibilità della playlist
    private Date dataCreazione;	 // Data di creazione della playlist
    private int numeroElementi;  // Numero di elementi multimediali presenti nella playlist: tenuto aggiornato da un TRIGGER sul DB quando si aggiungono/rimuovono brani
    private String matricolaProprietario;  // Chiave esterna che indica l'utente proprietario della playlist (Utente.matricola)

 // Getter e setter per l'ID della playlist
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

 // Getter e setter per il titolo della playlist
    public String getTitolo() { return titolo; }
    public void setTitolo(String titolo) { this.titolo = titolo; }

 // Getter e setter per la visibilità
    public String getVisibilita() { return visibilita; }
    public void setVisibilita(String visibilita) { this.visibilita = visibilita; }

 // Getter e setter per la data di creazione
    public Date getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(Date dataCreazione) { this.dataCreazione = dataCreazione; }

 // Getter e setter per il numero di elementi (il setter serve soprattutto al DAO per leggere il valore dal DB)
    public int getNumeroElementi() { return numeroElementi; }
    public void setNumeroElementi(int numeroElementi) { this.numeroElementi = numeroElementi; }

 // Getter e setter per la matricola del proprietario
    public String getMatricolaProprietario() { return matricolaProprietario; }
    public void setMatricolaProprietario(String m) { this.matricolaProprietario = m; }

 // Ridefinisce Object.toString() per restituire una descrizione sintetica e leggibile della playlist
    @Override
    public String toString() {
        return titolo + " (" + visibilita + ", " + numeroElementi + " elementi)";
    }
}
