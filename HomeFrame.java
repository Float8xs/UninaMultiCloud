// Appartiene al pacchetto gui: contiene le finestre e i componenti grafici dell'applicazione
package it.unina.multicloud.gui;

// Importa i DAO per leggere/modificare elementi multimediali e ascolti, più i relativi modelli
import it.unina.multicloud.dao.AscoltoDAO;
import it.unina.multicloud.dao.ElementoMultimedialeDAO;
import it.unina.multicloud.model.ElementoMultimediale;
import it.unina.multicloud.model.Utente;

// Componenti Swing, bordi, layout, gestione eccezioni SQL e liste
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

// Finestra principale (JFrame) con menu laterale (WEST), barra di ricerca/saluto (NORTH) e feed/tendenze (CENTER)
public class HomeFrame extends JFrame {

    // Utente autenticato passato dal login; rimane costante per tutta la sessione di questa finestra
    private final Utente utenteCorrente;
    // DAO per recuperare, cercare ed eliminare elementi multimediali
    private final ElementoMultimedialeDAO elementoDAO = new ElementoMultimedialeDAO();
    // DAO per registrare gli ascolti e leggere la classifica delle tendenze
    private final AscoltoDAO ascoltoDAO = new AscoltoDAO();

    // Contenitore verticale in cui vengono inserite dinamicamente le card degli elementi
    private JPanel listaPanel;
    // Etichetta che mostra il numero di elementi visualizzati o il titolo della classifica tendenze
    private JLabel contatoreLabel;
    // Campo di testo per la ricerca per titolo
    private JTextField ricercaField;

    // Costruttore: riceve l'utente loggato e imposta layout, sezioni e primo caricamento del feed
    public HomeFrame(Utente utente) {
        super("UninaMultiCloud - Home");
        this.utenteCorrente = utente;
        // DISPOSE_ON_CLOSE chiude solo questa finestra senza terminare l'intera JVM
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 680);
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(null); // Centra la finestra sullo schermo
        setLayout(new BorderLayout());

        // Assembla le tre macro-aree della finestra
        add(buildSidebar(), BorderLayout.WEST);
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildFeed(), BorderLayout.CENTER);

        // Carica all'avvio tutti gli elementi (nessuna parola chiave di ricerca)
        caricaFeed(null);
    }

    // ---- menu laterale ----

    // Costruisce la barra di navigazione verticale a sinistra con logo, voci di menu e pulsante di logout
    private JPanel buildSidebar() {
        JPanel side = UITheme.navyPanel();
        side.setPreferredSize(new Dimension(210, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(24, 16, 24, 16));

        // Logo testuale con icona nuvola Unicode
        JLabel logo = new JLabel("\u2601 UninaMultiCloud");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 16));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(logo);
        side.add(Box.createVerticalStrut(30));

        // Collega ciascun pulsante del menu alla relativa azione tramite lambda expression
        side.add(menuButton("FEED", e -> caricaFeed(ricercaField != null ? ricercaField.getText() : null)));
        side.add(menuButton("+ CARICA", e -> apriUpload()));
        side.add(menuButton("MY PLAYLISTS", e -> new PlaylistDialog(this, utenteCorrente).setVisible(true)));
        side.add(menuButton("TENDENZE", e -> mostraTendenze()));
        side.add(menuButton("DATI E PRIVACY", e -> new ProfileDialog(this, utenteCorrente).setVisible(true)));

        // Spinge il pulsante LOG OUT fino in fondo alla colonna occupando lo spazio verticale residuo
        side.add(Box.createVerticalGlue());

        JButton logout = UITheme.dangerButton("LOG OUT");
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        logout.addActionListener(e -> {
            dispose(); // Chiude la Home
            new LoginRegisterFrame().setVisible(true); // Riporta alla schermata di login/registrazione
        });
        side.add(logout);

        return side;
    }

    // Metodo helper per creare pulsanti del menu laterale con stile uniforme ed effetto hover
    private JButton menuButton(String text, java.awt.event.ActionListener listener) {
        JButton b = new JButton(text);
        b.setForeground(Color.WHITE);
        b.setBackground(UITheme.NAVY);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(UITheme.FONT_BUTTON);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(listener);
        side_hover(b);
        return b;
    }

    // Cambia il colore del testo in oro quando il cursore entra sul pulsante e lo riporta bianco all'uscita
    private void side_hover(JButton b) {
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setForeground(UITheme.GOLD); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { b.setForeground(Color.WHITE); }
        });
    }

    // ---- barra in alto ----

    // Costruisce la barra superiore con campo di ricerca, pulsante Cerca e saluto all'utente loggato
    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UITheme.WHITE);
        top.setBorder(new EmptyBorder(16, 20, 16, 20));

        ricercaField = new JTextField();
        ricercaField.setFont(UITheme.FONT_FIELD);
        ricercaField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.GOLD_DARK, 1, true),
                new EmptyBorder(8, 14, 8, 14)));
        ricercaField.putClientProperty("JTextField.placeholderText", "Ricerca...");
        // Premendo Invio nel campo di testo si avvia subito la ricerca
        ricercaField.addActionListener(e -> caricaFeed(ricercaField.getText()));

        JButton cerca = UITheme.secondaryButton("Cerca");
        cerca.addActionListener(e -> caricaFeed(ricercaField.getText()));

        JPanel searchWrap = new JPanel(new BorderLayout(8, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(ricercaField, BorderLayout.CENTER);
        searchWrap.add(cerca, BorderLayout.EAST);

        // Saluto personalizzato con emoji (codice surrogato UTF-16 per l'onda/mano che saluta)
        JLabel benvenuto = new JLabel("Ciao, " + utenteCorrente.getNome() + " \uD83D\uDC4B");
        benvenuto.setFont(new Font("SansSerif", Font.BOLD, 16));
        benvenuto.setForeground(UITheme.NAVY);
        benvenuto.setBorder(new EmptyBorder(0, 20, 0, 0));

        top.add(searchWrap, BorderLayout.CENTER);
        top.add(benvenuto, BorderLayout.EAST);
        return top;
    }

    // ---- feed ----

    // Costruisce l'area centrale scorrevole con il contatore in alto e l'elenco verticale delle card
    private JScrollPane buildFeed() {
        listaPanel = new JPanel();
        listaPanel.setLayout(new BoxLayout(listaPanel, BoxLayout.Y_AXIS));
        listaPanel.setBackground(UITheme.WHITE);
        listaPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.WHITE);

        contatoreLabel = new JLabel("Elementi caricati: 0");
        contatoreLabel.setForeground(UITheme.GREY_TEXT);
        contatoreLabel.setBorder(new EmptyBorder(0, 20, 0, 0));
        wrapper.add(contatoreLabel, BorderLayout.NORTH);
        wrapper.add(listaPanel, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        // Aumenta la velocità di scorrimento con la rotellina del mouse
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // Carica tutti gli elementi o filtra per parola chiave in background tramite SwingWorker,
    // così l'interfaccia Swing resta reattiva durante la query SQL
    private void caricaFeed(String parolaChiave) {
        listaPanel.removeAll();
        listaPanel.add(caricamentoLabel());
        listaPanel.revalidate();
        listaPanel.repaint();

        new SwingWorker<List<ElementoMultimediale>, Void>() {
            Exception failure;

            // Eseguito su un thread separato (non sull'EDT): svolge l'accesso al database
            @Override protected List<ElementoMultimediale> doInBackground() {
                try {
                    if (parolaChiave == null || parolaChiave.isBlank()) {
                        return elementoDAO.getAll();
                    }
                    return elementoDAO.cercaPerTitolo(parolaChiave.trim());
                } catch (SQLException ex) {
                    failure = ex;
                    return List.of();
                }
            }

            // Eseguito sull'EDT al termine di doInBackground(): aggiorna i componenti grafici
            @Override protected void done() {
                listaPanel.removeAll();
                if (failure != null) {
                    listaPanel.add(new JLabel("Errore nel caricamento: " + failure.getMessage()));
                } else {
                    try {
                        List<ElementoMultimediale> risultati = get();
                        contatoreLabel.setText("Elementi caricati: " + risultati.size());
                        if (risultati.isEmpty()) {
                            listaPanel.add(new JLabel("Nessun elemento trovato."));
                        }
                        for (ElementoMultimediale el : risultati) {
                            listaPanel.add(creaCard(el));
                            listaPanel.add(Box.createVerticalStrut(10));
                        }
                    } catch (Exception ex) {
                        listaPanel.add(new JLabel("Errore imprevisto: " + ex.getMessage()));
                    }
                }
                listaPanel.revalidate();
                listaPanel.repaint();
            }
        }.execute();
    }

    // Crea un'etichetta standard di attesa durante il caricamento dati
    private JLabel caricamentoLabel() {
        JLabel l = new JLabel("Caricamento...");
        l.setForeground(UITheme.GREY_TEXT);
        return l;
    }

    // Crea una card orizzontale con miniatura, titolo/metadati e pulsanti azione (Play, + Playlist, Elimina)
    private JPanel creaCard(ElementoMultimediale el) {
        JPanel card = new JPanel(new BorderLayout(14, 0));
        card.setBackground(UITheme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(12, 16, 12, 16)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icona = creaMiniatura(el, 48);

        // Colonna centrale con titolo e formato + matricola dell'autore
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel titolo = new JLabel(el.getTitolo());
        titolo.setFont(new Font("SansSerif", Font.BOLD, 15));
        titolo.setForeground(UITheme.NAVY);
        JLabel meta = new JLabel(el.getFormato() + " \u00b7 autore: " + el.getMatricolaAutore());
        meta.setFont(UITheme.FONT_SUB);
        meta.setForeground(UITheme.GREY_TEXT);
        info.add(titolo);
        info.add(meta);

        // Pannello a destra con i pulsanti disponibili per l'elemento
        JPanel azioni = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        azioni.setOpaque(false);
        JButton play = UITheme.primaryButton("\u25B6 Play");
        play.addActionListener(e -> apriPlayer(el.getId()));
        JButton aggiungi = UITheme.secondaryButton("+ Playlist");
        aggiungi.addActionListener(e -> aggiungiAPlaylist(el.getId()));
        azioni.add(play);
        azioni.add(aggiungi);

        // Mostra il pulsante "Elimina" solo se l'utente corrente è l'autore dell'elemento
        if (el.getMatricolaAutore().equals(utenteCorrente.getMatricola())) {
            JButton elimina = UITheme.dangerButton("Elimina");
            elimina.addActionListener(e -> eliminaElemento(el));
            azioni.add(elimina);
        }

        card.add(icona, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(azioni, BorderLayout.EAST);
        return card;
    }

    // Prova a leggere l'immagine di copertina dal percorso salvato e a ridimensionarla;
    // in caso di assenza o errore ripiega su un'emoji (telecamera per Video, nota musicale per Audio)
    private JLabel creaMiniatura(ElementoMultimediale el, int dimensione) {
        String percorso = el.getUrlCopertina();
        if (percorso != null && !percorso.isBlank()) {
            try {
                java.io.File file = new java.io.File(percorso);
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(file);
                if (img != null) {
                    Image scalata = img.getScaledInstance(dimensione, dimensione, Image.SCALE_SMOOTH);
                    JLabel miniatura = new JLabel(new ImageIcon(scalata));
                    miniatura.setPreferredSize(new Dimension(dimensione, dimensione));
                    return miniatura;
                }
            } catch (Exception ignored) {
                // Se l'immagine non esiste o non è valida, prosegue usando l'emoji di fallback
            }
        }
        JLabel icona = new JLabel("Video".equals(el.getFormato()) ? "\uD83C\uDFA5" : "\uD83C\uDFB5");
        icona.setFont(new Font("SansSerif", Font.PLAIN, (int) (dimensione * 0.55)));
        icona.setHorizontalAlignment(SwingConstants.CENTER);
        icona.setPreferredSize(new Dimension(dimensione, dimensione));
        return icona;
    }

    // Recupera i dettagli completi dell'elemento, registra l'ascolto per l'utente corrente e apre PlayerDialog
    private void apriPlayer(int idElemento) {
        try {
            ElementoMultimediale dettaglio = elementoDAO.getDettaglio(idElemento);
            if (dettaglio == null) return;
            ascoltoDAO.registraAscolto(utenteCorrente.getMatricola(), idElemento);
            new PlayerDialog(this, dettaglio).setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Errore: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Apre PlaylistDialog in modalità aggiunta passando l'ID dell'elemento selezionato
    private void aggiungiAPlaylist(int idElemento) {
        new PlaylistDialog(this, utenteCorrente, idElemento).setVisible(true);
    }

    // Q8 - Chiede conferma e, se confermato, elimina l'elemento tramite DAO aggiornando poi il feed
    private void eliminaElemento(ElementoMultimediale el) {
        int conferma = JOptionPane.showConfirmDialog(this,
                "Eliminare definitivamente \"" + el.getTitolo() + "\"?\nVerra' rimosso anche da eventuali playlist.",
                "Conferma eliminazione", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conferma != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = elementoDAO.elimina(el.getId(), utenteCorrente.getMatricola());
            if (ok) {
                caricaFeed(ricercaField != null ? ricercaField.getText() : null);
            } else {
                JOptionPane.showMessageDialog(this, "Impossibile eliminare: non risulti l'autore di questo elemento.",
                        "Operazione non riuscita", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Errore durante l'eliminazione: " + ex.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Apre la finestra modale di caricamento; se va a buon fine ricarica il feed principale
    private void apriUpload() {
        UploadDialog dialog = new UploadDialog(this, utenteCorrente);
        dialog.setVisible(true);
        if (dialog.isCaricamentoRiuscito()) {
            caricaFeed(null);
        }
    }

    // Carica i primi 20 elementi più ascoltati in background e li mostra nella stessa area centrale del feed
    private void mostraTendenze() {
        listaPanel.removeAll();
        listaPanel.add(caricamentoLabel());
        listaPanel.revalidate();
        new SwingWorker<List<ElementoMultimediale>, Void>() {
            Exception failure;

            @Override protected List<ElementoMultimediale> doInBackground() {
                try {
                    return ascoltoDAO.topElementi(20);
                } catch (SQLException ex) {
                    failure = ex;
                    return List.of();
                }
            }

            @Override protected void done() {
                listaPanel.removeAll();
                if (failure != null) {
                    listaPanel.add(new JLabel("Errore nel caricamento: " + failure.getMessage()));
                } else {
                    try {
                        List<ElementoMultimediale> top = get();
                        contatoreLabel.setText("Tendenze - top " + top.size());
                        if (top.isEmpty()) {
                            listaPanel.add(new JLabel("Nessun ascolto registrato ancora."));
                        }
                        for (ElementoMultimediale el : top) {
                            JPanel card = creaCard(el);
                            listaPanel.add(card);
                            listaPanel.add(Box.createVerticalStrut(10));
                        }
                    } catch (Exception ignored) { }
                }
                listaPanel.revalidate();
                listaPanel.repaint();
            }
        }.execute();
    }
}