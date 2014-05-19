/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tftp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.management.Query.or;

/**
 *
 * @author mathieu
 */
public class ClientSTF {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
           //sendFile("C:/Utilisateurs/mathieu/Bureau/test.txt","127.0.0.1");
           recieveFile("blabla.txt","test.txt","127.0.0.1");
    }

    public static int sendFile(String nomFichier, String addrDestStr) {
        DatagramSocket socketSend;
        DatagramPacket dpSend, dpReceive;
        InetAddress addrDestInet;
        byte buffer[] = new byte[512];
        Scanner sc;
        boolean ok = true;
        int count, block = 0;
        int TID;
        String opCode, mode, msg, data;

        //Création du socket
        try {
            socketSend = new DatagramSocket();
        } catch (SocketException ex) {
            Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("connection socket impossible");
            return 5; // unknown transfer ID
        }

        //Récupération de l'inet address 
        try {
            addrDestInet = InetAddress.getByName(addrDestStr);
        } catch (UnknownHostException ex) {
            Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("no such user");
            return 7;
        }

        //Initialisation de la connection
        opCode = "\0\2";
        mode = "NETASCII";
        msg = opCode + nomFichier + "\0" + mode + "\0";
        buffer = msg.getBytes();
        dpSend = new DatagramPacket(buffer, buffer.length, addrDestInet, 69);
        dpReceive = new DatagramPacket(buffer, buffer.length);
        try {
            socketSend.send(dpSend);
            socketSend.receive(dpReceive);
        } catch (IOException ex) {
            Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        TID = dpReceive.getPort();

        //Récupération du fichier à envoyer
        try {
            sc = new Scanner(new File(nomFichier));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("file not found");
            return 1;
        }
        while (ok) {
            count = 0;
            block++;
            data = null;
            while (sc.hasNextByte() && (data.length() < 513)) {
                data = sc.nextLine();
                count++;
            }
            ok = sc.hasNextByte();
            opCode = "\0\3";
            msg = opCode + data;
            buffer = msg.getBytes();
            dpSend = new DatagramPacket(buffer, buffer.length, addrDestInet, TID);

            //Reception de l'ACK
            try {
                socketSend.receive(dpReceive);
            } catch (IOException ex) {
                Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
            }
            buffer = dpReceive.getData();
            data = buffer.toString();
            if (!data.startsWith("04")) {
                return 3;
            }
        }
        socketSend.close();
        return 0;
    }

    public static int recieveFile(String nomFichierLocal, String nomFichierDistant, String adresseDistanteStr) {
        DatagramSocket socketRecieve;
        DatagramPacket dpSend, dpReceive;
        InetAddress addrDestInet;
        byte buffer[] = new byte[512];
        Scanner sc;
        boolean ok = true;
        int count, block = 0;
        int TID;
        String opCode, mode, msg, data, blockStr;

        //Création du socket
        try {
            socketRecieve = new DatagramSocket();
        } catch (SocketException ex) {
            Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("connection socket impossible");
            return 5; // unknown transfer ID
        }
        try {
            addrDestInet = InetAddress.getByName(adresseDistanteStr);
        } catch (UnknownHostException ex) {
            Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
            return 5;
        }

        //Initialisation de la connection
        opCode = "\0\1";
        mode = "NETASCII";
        msg = opCode + nomFichierDistant + "\0" + mode + "\0";
        buffer = msg.getBytes();
        dpSend = new DatagramPacket(buffer, buffer.length, addrDestInet, 69);
        dpReceive = new DatagramPacket(buffer, buffer.length);
        try {
            socketRecieve.send(dpSend);
        } catch (IOException ex) {
            Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            socketRecieve.receive(dpReceive);
        } catch (IOException ex) {
            Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
        }
        TID = dpReceive.getPort();

        //ouverture du fichier de reception
        File fichierLocal = new File(nomFichierLocal);
        /*if (!fichierLocal.exists()) {
            try {
                fichierLocal.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("erreur");
                return 1;
            }
        }
        else{return 7;}*/
        
        System.out.println("pas erreur");
        
        FileWriter fw = null;
        try {
            fw = new FileWriter(fichierLocal.getAbsoluteFile());
        } catch (IOException ex) {
            Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
        }
        BufferedWriter bw = new BufferedWriter(fw);

        //reception des blocks envoyés par le server
        do {
            try {
                socketRecieve.receive(dpReceive);
            } catch (IOException ex) {
                Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
            }

            //récupération des données
            data = dpReceive.getData().toString();
            blockStr = data.subSequence(2, 3).toString();
            msg = data.subSequence(4, data.length()).toString();

            //écriture dans le fichier
            try {
                bw.write(msg);
            } catch (IOException ex) {
                Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
            }

            //envoi de l'ACK
            buffer = ("\0\4" + blockStr).getBytes();
            dpSend = new DatagramPacket(buffer, buffer.length, addrDestInet, TID);
            try {
                socketRecieve.send(dpSend);
            } catch (IOException ex) {
                Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
            }

        } while (data.length() == 516);
        
        try {
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientSTF.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        socketRecieve.close();

        return 0;
    }
}
