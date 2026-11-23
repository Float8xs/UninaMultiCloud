// Appartiene al pacchetto gui: finestra di dialogo per la riproduzione di un contenuto multimediale[cite: 1]
package it.unina.multicloud.gui;

// Importa il modello ElementoMultimediale
import it.unina.multicloud.model.ElementoMultimediale;

// API standard Java per la riproduzione audio (Clip, AudioSystem, AudioInputStream)
import javax.sound.sampled.*;
// Componenti Swing, bordi e layout
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

// Finestra di dialogo (JDialog) modale dedicata al player audio/video
public class PlayerDialog extends JDialog {

    // Riferimento alla Clip audio attiva (per i .wav) così da poterla fermare/chiudere su riavvio o chiusura finestra
    private Clip clip;

    // Costruttore: imposta finestra modale, metadati dell'elemento, pulsante Riproduci e listener di chiusura
    public PlayerDialog(Frame owner, ElementoMultimediale elemento) {
        super(owner, "Player - " + elemento.getTitolo(), true); // true = modale
        setSize(460, 320);
        setLocationRelativeTo(owner); // Centra il dialogo sulla finestra genitore
        getContentPane().setBackground(UITheme.NAVY);
        setLayout(new BorderLayout());

        // Contenitore verticale centrale con sfondo trasparente (lascia vedere il NAVY del contentPane)
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(30, 30, 20, 30));

        // Mostra la copertina scalata se disponibile, oppure un'emoji in base al formato
        JLabel icona = creaCopertinaOEmoji(elemento, 140);
        icona.setAlignmentX(Component.CENTER_ALIGNMENT);
        icona.setForeground(UITheme.GOLD);

        // Titolo del contenuto multimediale
        JLabel titolo = new JLabel(elemento.getTitolo(), SwingConstants.CENTER);
        titolo.setFont(new Font("SansSerif", Font.BOLD, 18));
        titolo.setForeground(Color.WHITE);
        titolo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Se nomeAutore è valorizzato (es. da query con JOIN), mostra nome e cognome; altrimenti la matricola
        String autore = elemento.getNomeAutore() != null
                ? elemento.getNomeAutore() + " " + elemento.getCognomeAutore()
                : elemento.getMatricolaAutore();

        // Riga informativa con autore, durata formattata (mm:ss) e numero totale di riproduzioni
        JLabel sub = new JLabel(autore + "   \u00b7   " + elemento.getDurataFormattata()
                + "   \u00b7   " + elemento.getTotaleRiproduzioni() + " riproduzioni", SwingConstants.CENTER);
        sub.setForeground(UITheme.GOLD);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(icona);
        content.add(Box.createVerticalStrut(10));
        content.add(titolo);
        content.add(Box.createVerticalStrut(4));
        content.add(sub);
        content.add(Box.createVerticalStrut(24));

        // Pannello dei controlli con pulsante principale di riproduzione
        JPanel controlli = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        controlli.setOpaque(false);
        JButton playBtn = UITheme.primaryButton("\u25B6 Riproduci");
        controlli.add(playBtn);
        content.add(controlli);

        // Etichetta di stato per messaggi di riproduzione, fallback o errore
        JLabel stato = new JLabel(" ", SwingConstants.CENTER);
        stato.setForeground(Color.WHITE);
        stato.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(Box.createVerticalStrut(8));
        content.add(stato);

        // Costruisce il File a partire dal percorso salvato nell'elemento (se presente)
        File file = elemento.getUrlFile() != null ? new File(elemento.getUrlFile()) : null;

        // Gestisce il click su Riproduci: verifica l'esistenza del file, prova il player interno per i .wav,
        // e per gli altri formati (o se il player interno fallisce) apre l'applicazione predefinita del sistema
        playBtn.addActionListener(e -> {
            if (file == null || !file.exists()) {
                stato.setText("File non trovato sul disco locale: " + (file == null ? "percorso mancante" : file.getAbsolutePath()));
                return;
            }

            // javax.sound.sampled supporta nativamente formati audio non compressi come WAV;
            // MP3/MP4 richiederebbero codec/librerie esterne
            if (file.getName().toLowerCase().endsWith(".wav")) {
                try {
                    stopClip(); // Ferma e chiude eventuali riproduzioni WAV già avviate
                    AudioInputStream ais = AudioSystem.getAudioInputStream(file);
                    clip = AudioSystem.getClip();
                    clip.open(ais);
                    clip.start();
                    stato.setText("In riproduzione nel player integrato...");
                    return;
                } catch (Exception ex) {
                    stato.setText("Player integrato non disponibile (" + ex.getMessage() + "), apro il player di sistema...");
                    // Senza 'return', prosegue verso Desktop.getDesktop().open(file)
                }
            }

            try {
                // Apre il file con il programma associato dal sistema operativo (es. lettore audio/video)
                Desktop.getDesktop().open(file);
                stato.setText("Aperto nel player predefinito del sistema.");
            } catch (Exception ex) {
                stato.setText("Impossibile aprire il file: " + ex.getMessage());
            }
        });

        add(content, BorderLayout.CENTER);

        // Alla chiusura della finestra rilascia la Clip audio per non lasciarla attiva in background
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                stopClip();
            }
        });
    }

    // Carica e ridimensiona l'immagine di copertina se valida, altrimenti mostra un'emoji di ripiego
    private JLabel creaCopertinaOEmoji(ElementoMultimediale elemento, int dimensione) {
        String percorso = elemento.getUrlCopertina();
        if (percorso != null && !percorso.isBlank()) {
            try {
                java.io.File file = new java.io.File(percorso);
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(file);
                if (img != null) {
                    Image scalata = img.getScaledInstance(dimensione, dimensione, Image.SCALE_SMOOTH);
                    JLabel l = new JLabel(new ImageIcon(scalata), SwingConstants.CENTER);
                    l.setPreferredSize(new Dimension(dimensione, dimensione));
                    return l;
                }
            } catch (Exception ignored) {
                // In caso di percorso inesistente, file non leggibile o non immagine, usa l'emoji sotto
            }
        }
        JLabel icona = new JLabel("Video".equals(elemento.getFormato()) ? "\uD83C\uDFA5" : "\uD83C\uDFB5", SwingConstants.CENTER);
        icona.setFont(new Font("SansSerif", Font.PLAIN, 60));
        return icona;
    }

    // Ferma la Clip audio se in esecuzione, libera le risorse audio e azzera il riferimento
    private void stopClip() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }
}