package it.unina.multicloud;

import it.unina.multicloud.gui.LoginRegisterFrame;
//Importa la schermata iniziale di login/registrazione e la classe del tema grafico dal pacchetto gui
import it.unina.multicloud.gui.UITheme;

import javax.swing.*;// Importa il pacchetto Swing per l'interfaccia grafica desktop

//Classe contenente il metodo main, da cui Java fa partire l'esecuzione del programma
public class Main {
    public static void main(String[] args) {
    	// Applica colori, font e stile generale dell'interfaccia prima di creare/mostrare le finestre
        UITheme.apply(); 
      //() -> ... è una lambda expression di Java che definisce il Runnable da eseguire
        SwingUtilities.invokeLater(() -> new LoginRegisterFrame().setVisible(true));
        // Esegue la creazione e l'apertura di LoginRegisterFrame sull'Event Dispatch Thread (EDT) di Swing:
        // è la pratica standard in Swing per evitare problemi di concorrenza sulla grafica
    }
    //in Swing l'interfaccia grafica non è thread-safe, quindi la creazione 
    //e la manipolazione delle finestre andrebbero sempre lanciate sull'EDT
}
