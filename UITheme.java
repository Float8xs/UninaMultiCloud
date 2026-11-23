// Appartiene al pacchetto gui: raccoglie stile, colori, font e metodi helper per i componenti Swing
package it.unina.multicloud.gui;

// Componenti Swing, bordi e classi AWT per colori, font, cursori e LookAndFeel
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// "final": impedisce di ereditare da questa classe; contiene solo costanti e metodi statici di stile
public final class UITheme {

    // Palette cromatica dell'applicazione (toni blu navy, oro, neutri e rosso per gli errori)
    public static final Color NAVY        = new Color(5, 45, 96);
    public static final Color NAVY_DARK   = new Color(3, 28, 61);
    public static final Color NAVY_LIGHT  = new Color(16, 66, 130);
    public static final Color GOLD        = new Color(212, 176, 118);
    public static final Color GOLD_DARK   = new Color(178, 143, 85);
    public static final Color WHITE       = new Color(250, 250, 248);
    public static final Color GREY_TEXT   = new Color(90, 100, 115);
    public static final Color CARD_BG     = Color.WHITE;
    public static final Color ERROR       = new Color(196, 60, 60);

    // Font riutilizzabili per mantenere coerenza tipografica tra le varie finestre e dialoghi
    public static final Font FONT_TITLE   = new Font("SansSerif", Font.BOLD, 26);
    public static final Font FONT_SUB     = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_LABEL   = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONT_FIELD   = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_BUTTON  = new Font("SansSerif", Font.BOLD, 14);

    // Costruttore privato: evita di istanziare UITheme perché tutto è accessibile in modo statico
    private UITheme() { }

    // Da invocare una volta all'avvio (in Main.java) prima di costruire le finestre:
    // prova ad applicare il Look and Feel nativo del sistema operativo e personalizza i tooltip
    public static void apply() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Se il Look and Feel nativo non è disponibile, prosegue con quello predefinito di Swing
        }
        UIManager.put("ToolTip.background", NAVY);
        UIManager.put("ToolTip.foreground", Color.WHITE);
    }

    // Crea un pulsante principale (es. ACCEDI, Play) con sfondo oro e testo blu scuro
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, GOLD, NAVY_DARK, GOLD_DARK);
        return b;
    }

    // Crea un pulsante secondario (es. Cerca, + Playlist) con sfondo blu chiaro e testo bianco
    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, NAVY_LIGHT, Color.WHITE, NAVY);
        return b;
    }

    // Crea un pulsante per azioni distruttive o di uscita (es. Elimina, LOG OUT) con tonalità rossa
    public static JButton dangerButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, new Color(220, 90, 90), Color.WHITE, ERROR);
        return b;
    }

    // Applica le proprietà grafiche comuni a qualsiasi JButton creato dalle factory sopra
    private static void styleButton(JButton b, Color bg, Color fg, Color border) {
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(FONT_BUTTON);
        b.setFocusPainted(false); // Evita il riquadro tratteggiato di focus sul testo del bottone
        b.setBorder(new EmptyBorder(10, 18, 10, 18)); // Padding interno uniforme
        b.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Mostra la manina al passaggio del mouse
        b.setOpaque(true); // Necessario su alcuni Look and Feel per far vedere il colore di sfondo
        b.setBorderPainted(false);
    }

    // Crea un campo di testo singolo già formattato con lo stile dell'app
    public static JTextField field() {
        JTextField f = new JTextField();
        styleField(f);
        return f;
    }

    // Crea un campo password già formattato con lo stile dell'app
    public static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        styleField(f);
        return f;
    }

    // Applica font, bordo inferiore dorato e spaziatura interna ai campi di input testuali
    private static void styleField(JTextField f) {
        f.setFont(FONT_FIELD);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, GOLD_DARK),
                new EmptyBorder(6, 4, 6, 4)));
    }

    // Crea un'etichetta (JLabel) per i campi con font in grassetto e colore NAVY
    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(NAVY);
        return l;
    }

    // Crea un JPanel con sfondo blu NAVY preimpostato
    public static JPanel navyPanel() {
        JPanel p = new JPanel();
        p.setBackground(NAVY);
        return p;
    }

    // Formatta titolo e sottotitolo opzionale per le intestazioni delle finestre
    public static void applyNiceHeader(JLabel title, JLabel subtitle) {
        title.setFont(FONT_TITLE);
        title.setForeground(Color.WHITE);
        if (subtitle != null) {
            subtitle.setFont(FONT_SUB);
            subtitle.setForeground(GOLD);
        }
    }
}