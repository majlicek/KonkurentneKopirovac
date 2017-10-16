import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 *
 * Created by Milan Chrastina on 21.02.2016.
 */
public class Server {
    private final static int server_port = 5500;
    private static ServerSocket serverSocket;
    private static ServerSocket soketServerKlient[] = new ServerSocket[10];

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(server_port);
            for (int i = 0; i < 10; i++) {
                soketServerKlient[i] = new ServerSocket(server_port + 1 + i);
            }
            while (true) {

                Socket connectionSocket = serverSocket.accept();
                BufferedReader input = new BufferedReader(new InputStreamReader(
                        connectionSocket.getInputStream()));
                String sprava = input.readLine();
                if ("pripojeny".equals(sprava)) {
                    System.out.println("Som pripojeny!!!");
                    connectionSocket.close();
                }
                if ("odpojeny".equals(sprava)) {
                    System.out.println("Som odhlaseny :(");
                    connectionSocket.close();
                }
                if (sprava.startsWith("subor")) {
                    Scanner skener = new Scanner(sprava);
                    skener.useDelimiter("#");
                    System.out.println(skener.next());
                    String subor = skener.next();
                    System.out.println(subor);
                    int pocetVlakien = Integer.parseInt(skener.next());
                    System.out.println(pocetVlakien);
                    connectionSocket.close();

                    //inicializovanie soketov
                    Socket[] klienti = new Socket[pocetVlakien];
                    for (int i = 0; i < pocetVlakien; i++) {
                        klienti[i] = soketServerKlient[i].accept();
                    }

                    // Rozdelenie suboru na posiielanie po viacerych vlaknach
                    RandomAccessFile raf = new RandomAccessFile(subor, "r");
                    File file = new File(subor);
                    int velkostSuboru = (int) file.length();
                    int cast = velkostSuboru / pocetVlakien;

                    for (int i = 0; i < pocetVlakien; i++) {
                        raf.seek(cast * i);
                        byte[] data = new byte[cast];
                        if (i == pocetVlakien - 1) {
                            data = new byte[(cast) + (velkostSuboru - (cast * pocetVlakien))];
                        }
                        raf.read(data);

                        Thread klient = new Thread(new ServerThread(klienti[i], data));
                        klient.start();
                    }

                }

            }


        } catch (IOException e1) {

            e1.printStackTrace();

        }

    }
}
