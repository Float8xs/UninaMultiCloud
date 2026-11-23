// Appartiene al pacchetto gui: finestra di dialogo per visualizzare il profilo, i propri caricamenti e cambiare password
package it.unina.multicloud.gui;

// Importa i DAO per leggere/aggiornare utenti ed elementi multimediali, più i relativi modelli
import it.unina.multicloud.dao.ElementoMultimedialeDAO;
import it.unina.multicloud.dao.UtenteDAO;
import it.unina.multicloud.model.ElementoMultimediale;
import it.unina.multicloud.model.Utente;

// Componenti Swing, bordi, layout, gestione eccezioni SQL e liste
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

// Finestra di dialogo (JDialog) modale per la gestione del profilo utente ("Dati e Privacy")
public class ProfileDialog extends JDialog {

    // Utente autenticato di cui vengono mostrati i dati e aggiornata la password
    private final Utente utente;
    // DAO per eseguire l'aggiornamento password (Q3)
    private final UtenteDAO utenteDAO = new UtenteDAO();
    // DAO per recuperare gli elementi caricati dall'utente (Q9)
    private final ElementoMultimedialeDAO elementoDAO = new ElementoMultimedialeDAO();

    // Costruttore: imposta finestra modale, dimensioni, centraggio e pannello principale
    public ProfileDialog(Frame owner, Utente utente) {
        super(owner, "Dati e Privacy", true); // true = modale rispetto al Frame genitore
        this.utente = utente;
        setSize(480, 520);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UITheme.WHITE);
        setLayout(new BorderLayout());
        add(buildContent(), BorderLayout.CENTER);
    }

    // Costruisce il contenuto della finestra: dati utente, lista elementi caricati e cambio password
    private JComponent buildContent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Dispone i blocchi dall'alto in basso
        panel.setBackground(UITheme.WHITE);
        panel.setBorder(new EmptyBorder(24, 26, 24, 26));

        // Intestazione con nome e cognome dell'utente
        JLabel titolo = new JLabel(utente.getNomeCompleto());
        titolo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titolo.setForeground(UITheme.NAVY);
        titolo.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(titolo);
        panel.add(Box.createVerticalStrut(4));
        // Mostra matricola, email e corsi di laurea (uniti da virgola se l'utente ne ha più di uno)
        panel.add(rigaDato("MATRICOLA", utente.getMatricola()));
        panel.add(rigaDato("E-MAIL", utente.getEmail()));
        panel.add(rigaDato("CORSO DI LAUREA", String.join(", ", utente.getCorsiLaurea())));
        panel.add(Box.createVerticalStrut(16));

        // Sezione dedicata agli elementi multimediali caricati dall'utente
        JLabel elementiTitolo = UITheme.label("ELEMENTI CARICATI");
        elementiTitolo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(elementiTitolo);
        panel.add(Box.createVerticalStrut(4));

        // Modello dati e JList per visualizzare gli elementi caricati dall'utente
        DefaultListModel<ElementoMultimediale> model = new DefaultListModel<>();
        JList<ElementoMultimediale> list = new JList<>(model);
        // Renderer personalizzato: definisce come viene disegnata ogni riga della JList
        list.setCellRenderer((l, value, index, isSelected, cellHasFocus) -> {
            JLabel lbl = new JLabel(value.getTitolo() + " [" + value.getFormato() + "] - " + value.getDataCreazione());
            lbl.setBorder(new EmptyBorder(4, 4, 4, 4));
            return lbl;
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setPreferredSize(new Dimension(400, 150));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        panel.add(scroll);
        panel.add(Box.createVerticalStrut(16));

        // Sezione per il cambio password
        JLabel cambioPasswordLabel = UITheme.label("CAMBIA PASSWORD");
        cambioPasswordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPasswordField nuovaPass = UITheme.passwordField();
        nuovaPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton aggiornaBtn = UITheme.secondaryButton("Aggiorna password");
        aggiornaBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel stato = new JLabel(" ");
        stato.setAlignmentX(Component.LEFT_ALIGNMENT);
        stato.setForeground(UITheme.GREY_TEXT);

        // Valida la lunghezza della nuova password e avvia l'aggiornamento su DB in background
        aggiornaBtn.addActionListener(e -> {
            String nuova = new String(nuovaPass.getPassword());
            if (nuova.length() < 8) {
                stato.setText("La password deve avere almeno 8 caratteri.");
                stato.setForeground(UITheme.ERROR);
                return;
            }
            new SwingWorker<Void, Void>() {
                Exception failure;

                @Override protected Void doInBackground() {
                    try {
                        utenteDAO.aggiornaPassword(utente.getMatricola(), nuova);
                    } catch (SQLException ex) {
                        failure = ex;
                    }
                    return null;
                }

                @Override protected void done() {
                    if (failure != null) {
                        stato.setForeground(UITheme.ERROR);
                        stato.setText("Errore: " + failure.getMessage());
                    } else {
                        // Colore verde per confermare il salvataggio e svuotamento del campo password
                        stato.setForeground(new Color(60, 140, 80));
                        stato.setText("Password aggiornata.");
                        nuovaPass.setText("");
                    }
                }
            }.execute();
        });

        panel.add(cambioPasswordLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(nuovaPass);
        panel.add(Box.createVerticalStrut(8));
        panel.add(aggiornaBtn);
        panel.add(Box.createVerticalStrut(6));
        panel.add(stato);

        // Carica in background gli elementi multimediali creati dall'utente (Q9) e aggiorna lista e contatore
        new SwingWorker<List<ElementoMultimediale>, Void>() {
            Exception failure;

            @Override protected List<ElementoMultimediale> doInBackground() {
                try {
                    return elementoDAO.getByAutore(utente.getMatricola());
                } catch (SQLException ex) {
                    failure = ex;
                    return List.of();
                }
            }

            @Override protected void done() {
                if (failure == null) {
                    try {
                        List<ElementoMultimediale> el = get();
                        for (ElementoMultimediale e : el) {
                            model.addElement(e);
                        }
                        elementiTitolo.setText("ELEMENTI CARICATI: " + el.size());
                    } catch (Exception ignored) { }
                }
            }
        }.execute();

        return new JScrollPane(panel);
    }

    // Crea un blocco compatto con etichetta in alto e relativo valore in basso (usa "-" se vuoto/null)
    private JPanel rigaDato(String label, String valore) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel l = UITheme.label(label);
        JLabel v = new JLabel(valore == null || valore.isBlank() ? "-" : valore);
        v.setFont(UITheme.FONT_FIELD);
        v.setForeground(UITheme.NAVY);
        JPanel inner = new JPanel(new GridLayout(2, 1));
        inner.setOpaque(false);
        inner.add(l);
        inner.add(v);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }
}