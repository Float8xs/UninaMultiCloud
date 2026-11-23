// Appartiene al pacchetto gui: finestra iniziale di autenticazione e registrazione
package it.unina.multicloud.gui;

// Importa il DAO per login, registrazione e controlli su matricola/email, oltre al modello Utente
import it.unina.multicloud.dao.UtenteDAO;
import it.unina.multicloud.model.Utente;

// Componenti Swing, bordi, layout e gestione eccezioni SQL
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

// Finestra principale (JFrame) con intestazione e due schede: ACCEDI e REGISTRATI
public class LoginRegisterFrame extends JFrame {

    // Istanza del DAO usata per verificare le credenziali e registrare nuovi utenti
    private final UtenteDAO utenteDAO = new UtenteDAO();

    // Costruttore: imposta finestra, intestazione e schede di login/registrazione
    public LoginRegisterFrame() {
        super("UninaMultiCloud - Accedi");
        // EXIT_ON_CLOSE termina l'intera applicazione se si chiude la finestra iniziale
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 620);
        setMinimumSize(new Dimension(420, 560));
        setLocationRelativeTo(null); // Centra la finestra sullo schermo
        getContentPane().setBackground(UITheme.NAVY);
        setLayout(new BorderLayout());

        // Intestazione superiore con logo e sottotitolo
        add(buildHeader(), BorderLayout.NORTH);

        // Pannello a schede per passare tra accesso e registrazione
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_BUTTON);
        tabs.addTab("ACCEDI", buildLoginPanel());
        tabs.addTab("REGISTRATI", buildRegisterPanel());
        tabs.setBorder(new EmptyBorder(10, 20, 20, 20));
        tabs.setOpaque(false);
        add(tabs, BorderLayout.CENTER);
    }

    // Costruisce l'header in alto con il nome dell'app e il sottotitolo descrittivo
    private JPanel buildHeader() {
        JPanel header = UITheme.navyPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(28, 20, 12, 20));

        JLabel logo = new JLabel("\u2601 UninaMultiCloud", SwingConstants.CENTER);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = new JLabel("Condividi e ascolta contenuti multimediali", SwingConstants.CENTER);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        // Applica lo stile grafico predefinito a titolo e sottotitolo
        UITheme.applyNiceHeader(logo, sub);

        header.add(logo);
        header.add(Box.createVerticalStrut(6));
        header.add(sub);
        return header;
    }

    // ---- login ----

    // Costruisce la scheda di accesso (email + password + pulsante ACCEDI)
    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.WHITE);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);

        JTextField emailField = UITheme.field();
        JPasswordField passField = UITheme.passwordField();
        // Uno spazio (" ") mantiene riservata l'altezza della riga anche quando non ci sono errori
        JLabel errore = new JLabel(" ");
        errore.setForeground(UITheme.ERROR);
        errore.setFont(UITheme.FONT_SUB);

        c.gridy = 0; panel.add(UITheme.label("EMAIL (@unina.it / @studenti.unina.it)"), c);
        c.gridy = 1; panel.add(emailField, c);
        c.gridy = 2; panel.add(Box.createVerticalStrut(10), c);
        c.gridy = 3; panel.add(UITheme.label("PASSWORD"), c);
        c.gridy = 4; panel.add(passField, c);
        c.gridy = 5; panel.add(errore, c);

        JButton loginBtn = UITheme.primaryButton("ACCEDI");
        c.gridy = 6;
        c.insets = new Insets(20, 0, 6, 0);
        panel.add(loginBtn, c);

        // Gestisce il click su ACCEDI: valida i campi, esegue il login in background e apre HomeFrame
        loginBtn.addActionListener(e -> {
            String email = emailField.getText().trim();
            // getPassword() restituisce char[]; viene convertito in String per passarlo a UtenteDAO.login
            String pass = new String(passField.getPassword());

            if (email.isEmpty() || pass.isEmpty()) {
                errore.setText("Inserisci email e password.");
                return;
            }

            // Disabilita il pulsante durante la richiesta per prevenire invii multipli accidentali
            loginBtn.setEnabled(false);
            errore.setText(" ");

            // Esegue la query di login su un thread separato tramite SwingWorker
            new SwingWorker<Utente, Void>() {
                Exception failure;

                @Override protected Utente doInBackground() {
                    try {
                        return utenteDAO.login(email, pass);
                    } catch (SQLException ex) {
                        failure = ex;
                        return null;
                    }
                }

                @Override protected void done() {
                    loginBtn.setEnabled(true);
                    if (failure != null) {
                        errore.setText("Errore di connessione al database: " + failure.getMessage());
                        return;
                    }
                    try {
                        Utente u = get();
                        if (u == null) {
                            errore.setText("Credenziali non valide.");
                        } else {
                            // Chiude LoginRegisterFrame e apre la schermata principale per l'utente autenticato
                            SwingUtilities.getWindowAncestor(panel).dispose();
                            new HomeFrame(u).setVisible(true);
                        }
                    } catch (Exception ex) {
                        errore.setText("Errore imprevisto: " + ex.getMessage());
                    }
                }
            }.execute();
        });

        return wrapScroll(panel);
    }

    // ---- registrazione ----

    // Costruisce la scheda di registrazione con validazioni client-side e inserimento tramite UtenteDAO
    private JPanel buildRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.WHITE);
        panel.setBorder(new EmptyBorder(20, 24, 24, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);

        JTextField matricolaField = UITheme.field();
        JTextField nomeField = UITheme.field();
        JTextField cognomeField = UITheme.field();
        JTextField emailField = UITheme.field();
        JTextField corsoField = UITheme.field();
        JPasswordField passField = UITheme.passwordField();
        JPasswordField confermaField = UITheme.passwordField();
        JLabel errore = new JLabel(" ");
        errore.setForeground(UITheme.ERROR);
        errore.setFont(UITheme.FONT_SUB);

        // Contatore incrementale per la riga (gridy) nel GridBagLayout
        int y = 0;
        c.gridy = y++; panel.add(UITheme.label("MATRICOLA"), c);
        c.gridy = y++; panel.add(matricolaField, c);
        c.gridy = y++; panel.add(UITheme.label("NOME"), c);
        c.gridy = y++; panel.add(nomeField, c);
        c.gridy = y++; panel.add(UITheme.label("COGNOME"), c);
        c.gridy = y++; panel.add(cognomeField, c);
        c.gridy = y++; panel.add(UITheme.label("E-MAIL (dominio unina.it)"), c);
        c.gridy = y++; panel.add(emailField, c);
        c.gridy = y++; panel.add(UITheme.label("CORSO DI LAUREA"), c);
        c.gridy = y++; panel.add(corsoField, c);
        c.gridy = y++; panel.add(UITheme.label("PASSWORD (min. 8 caratteri)"), c);
        c.gridy = y++; panel.add(passField, c);
        c.gridy = y++; panel.add(UITheme.label("CONFERMA PASSWORD"), c);
        c.gridy = y++; panel.add(confermaField, c);
        c.gridy = y++; panel.add(errore, c);

        JButton registerBtn = UITheme.primaryButton("REGISTRATI");
        c.gridy = y;
        c.insets = new Insets(16, 0, 6, 0);
        panel.add(registerBtn, c);

        // Gestisce il click su REGISTRATI: controlla i dati in input, verifica unicità su DB e salva l'utente
        registerBtn.addActionListener(e -> {
            String matricola = matricolaField.getText().trim();
            String nome = nomeField.getText().trim();
            String cognome = cognomeField.getText().trim();
            String email = emailField.getText().trim();
            String corso = corsoField.getText().trim();
            String pass = new String(passField.getPassword());
            String conferma = new String(confermaField.getPassword());

            // Validazioni preliminari lato client prima di interrogare il database
            if (matricola.isEmpty() || nome.isEmpty() || cognome.isEmpty() || email.isEmpty()) {
                errore.setText("Compila tutti i campi obbligatori.");
                return;
            }
            if (!email.endsWith("@unina.it") && !email.endsWith("@studenti.unina.it")) {
                errore.setText("L'email deve avere dominio @unina.it o @studenti.unina.it.");
                return;
            }
            if (pass.length() < 8) {
                errore.setText("La password deve avere almeno 8 caratteri.");
                return;
            }
            if (!pass.equals(conferma)) {
                errore.setText("Le due password non coincidono.");
                return;
            }

            registerBtn.setEnabled(false);
            errore.setText(" ");

            new SwingWorker<Void, Void>() {
                Exception failure;

                @Override protected Void doInBackground() {
                    try {
                        // Controlli espliciti per mostrare un errore leggibile prima dell'INSERT
                        if (utenteDAO.matricolaEsiste(matricola)) {
                            failure = new SQLException("Matricola già registrata.");
                            return null;
                        }
                        if (utenteDAO.emailEsiste(email)) {
                            failure = new SQLException("Email già registrata.");
                            return null;
                        }
                        Utente u = new Utente(matricola, nome, cognome, email);
                        // Calcola l'hash SHA-256 prima di affidare l'utente al DAO
                        u.setPasswordHash(UtenteDAO.sha256(pass));
                        utenteDAO.registraUtente(u, corso);
                    } catch (SQLException ex) {
                        failure = ex;
                    }
                    return null;
                }

                @Override protected void done() {
                    registerBtn.setEnabled(true);
                    if (failure != null) {
                        errore.setText(failure.getMessage());
                        return;
                    }
                    JOptionPane.showMessageDialog(panel,
                            "Registrazione completata! Ora puoi accedere.",
                            "MESSAGGIO CONFERMA", JOptionPane.INFORMATION_MESSAGE);

                    // Pulisce i campi del form dopo la registrazione andata a buon fine
                    matricolaField.setText("");
                    nomeField.setText("");
                    cognomeField.setText("");
                    emailField.setText("");
                    corsoField.setText("");
                    passField.setText("");
                    confermaField.setText("");
                }
            }.execute();
        });

        return wrapScroll(panel);
    }

    // Inserisce il pannello form dentro uno JScrollPane per permettere lo scorrimento su schermi piccoli
    private JPanel wrapScroll(JPanel inner) {
        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UITheme.WHITE);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }
}