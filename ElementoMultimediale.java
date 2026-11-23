package it.unina.multicloud.model;
//Appartiene al pacchetto model, cioè alle classi che rappresentano i dati (le entità) dell'app
import java.sql.Date;
import java.sql.Timestamp;
//Date (solo data, es. YYYY-MM-DD) e Timestamp (data + ora) dal pacchetto SQL per mappare i tipi del DB


//Classe modello che descrive un brano/video/contenuto multimediale nel sistema
public class ElementoMultimediale {
    private int id;	// Identificativo univoco del contenuto (probabilmente chiave primaria / serial / auto-increment sul DB)
    private String titolo;	// Titolo del contenuto multimediale
    private String descrizione;	 // Descrizione testuale (opzionale)
    private String formato;  // Tipologia del file ("Audio" oppure "Video"); il vincolo sui valori ammessi è demandato al DB (CHECK)
    private Integer durata;    // Durata in secondi; si usa Integer e non int primitivo perché può valere null
    private Date dataCreazione;	  // Data di creazione/origine del contenuto
    private String urlFile;	   // Percorso o URL del file multimediale da riprodurre
    private String urlCopertina;     // Percorso o URL dell'immagine di copertina (opzionale)
    private String matricolaAutore;    // Chiave esterna verso l'utente che ha caricato/creato l'elemento (collega a Utente.matricola)
    private Timestamp dataCaricamento;	// Data e ora esatta in cui il file è stato caricato sulla piattaforma

    // Campi "calcolati" o derivati: non sono colonne dirette della tabella principale dell'elemento,
    // ma vengono valorizzati dai DAO quando eseguono query con JOIN (su utente e/o tabella ascolti)
    private String nomeAutore;
    private String cognomeAutore;
    private long totaleRiproduzioni; // long è utile per conteggi che possono crescere molto

 // Getter e setter per l'ID
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

 // Getter e setter per il titolo
    public String getTitolo() { return titolo; }
    public void setTitolo(String titolo) { this.titolo = titolo; }

 // Getter e setter per la descrizione
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

 // Getter e setter per il formato
    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }

 // Getter e setter per la durata (in secondi, può essere null)
    public Integer getDurata() { return durata; }
    public void setDurata(Integer durata) { this.durata = durata; }

 // Getter e setter per la data di creazione
    public Date getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(Date dataCreazione) { this.dataCreazione = dataCreazione; }

 // Getter e setter per l'URL/percorso del file
    public String getUrlFile() { return urlFile; }
    public void setUrlFile(String urlFile) { this.urlFile = urlFile; }

 // Getter e setter per l'URL/percorso della copertina
    public String getUrlCopertina() { return urlCopertina; }
    public void setUrlCopertina(String urlCopertina) { this.urlCopertina = urlCopertina; }

 // Getter e setter per la matricola dell'autore
    public String getMatricolaAutore() { return matricolaAutore; }
    public void setMatricolaAutore(String matricolaAutore) { this.matricolaAutore = matricolaAutore; }

 // Getter e setter per il timestamp di caricamento
    public Timestamp getDataCaricamento() { return dataCaricamento; }
    public void setDataCaricamento(Timestamp d) { this.dataCaricamento = d; }

 // Getter e setter per il nome dell'autore (da JOIN con utente)
    public String getNomeAutore() { return nomeAutore; }
    public void setNomeAutore(String nomeAutore) { this.nomeAutore = nomeAutore; }

 // Getter e setter per il cognome dell'autore (da JOIN con utente)
    public String getCognomeAutore() { return cognomeAutore; }
    public void setCognomeAutore(String cognomeAutore) { this.cognomeAutore = cognomeAutore; }

 // Getter e setter per il numero totale di riproduzioni (da aggregazione/JOIN con ascolti)
    public long getTotaleRiproduzioni() { return totaleRiproduzioni; }
    public void setTotaleRiproduzioni(long t) { this.totaleRiproduzioni = t; }

 // Metodo di formattazione per la GUI: converte i secondi totali nel formato "mm:ss" 
    public String getDurataFormattata() {
        if (durata == null) return "--:--"; // Se la durata non è disponibile, evita NullPointerException
        int m = durata / 60;   // Divisione intera, i minuti
        int s = durata % 60;   // Resto della divisione, i secondi che avanzano
        return String.format("%02d:%02d", m, s); // %02d = sempre 2 cifre, tipo 03 invece di 3
    }

	//Ridefinisce Object.toString() per mostrare titolo e formato in modo leggibile
    @Override
    public String toString() { return titolo + " [" + formato + "]"; }
}
