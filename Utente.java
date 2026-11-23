package it.unina.multicloud.model;
//Appartiene al pacchetto model, cioè alle classi che rappresentano i dati (le entità) dell'app
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//Classe del modello (model) che rappresenta un utente registrato nell'applicazione
public class Utente {
    private String matricola;   // Identificativo univoco / chiave primaria (PK) dell'utente
    private String nome;   // Nome dell'utente
    private String cognome;   // Cognome dell'utente
    private String email;	// Indirizzo email dell'utente
    private String passwordHash;   // Hash della password (per non conservare la password in chiaro)
    private Timestamp dataRegistrazione;	// Data e ora di registrazione (compatibile con i campi SQL)
    private List<String> corsiLaurea = new ArrayList<>(); // Elenco dei corsi di laurea associati all'utente, inizializzata subito per non beccare NullPointer dopo

    public Utente() { } // Costruttore vuoto: permette di creare un'istanza e impostare i campi in secondo momento con getter, setter e lettura al db  

    // Costruttore parametrico: inizializza subito i dati anagrafici principali dell'utente
    public Utente(String matricola, String nome, String cognome, String email) {
        this.matricola = matricola;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
    }

 // Getter e setter per accedere e modificare la matricola
    public String getMatricola() { return matricola; }
    public void setMatricola(String matricola) { this.matricola = matricola; }

 // Getter e setter per il nome
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

 // Getter e setter per il cognome
    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

 // Getter e setter per l'email
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

 // Getter e setter per l'hash della password
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

 // Getter e setter per il timestamp di registrazione
    public Timestamp getDataRegistrazione() { return dataRegistrazione; }
    public void setDataRegistrazione(Timestamp d) { this.dataRegistrazione = d; }

 // Getter e setter per la lista dei corsi di laurea
    public List<String> getCorsiLaurea() { return corsiLaurea; }
    public void setCorsiLaurea(List<String> corsiLaurea) { this.corsiLaurea = corsiLaurea; }

 // Metodo di utilità che restituisce nome e cognome concatenati
    public String getNomeCompleto() { return nome + " " + cognome; }

 // Rappresentazione testuale dell'oggetto (utile ad esempio per stampe o componenti UI)
    @Override //personalizza il metodo già creato, senza di esso Java creerebbe per errore un nuovo metodo invece di sostituire l'originale
    public String toString() { return getNomeCompleto() + " (" + matricola + ")"; }
}