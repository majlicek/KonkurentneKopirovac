import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.Socket;

/**
 *
 * Created by Milan Chrastina on 27.02.2016.
 */
public class KlientThread implements Runnable {

    private int server_port = 5501;
    private int vlakno;
    private int pocetVlakien;
    private final RandomAccessFile zapisovac;
    private int velkostSuboru;
    private GuiKopirovac.SaveWorker saveWorker;
    private GuiKopirovac okno;
    private boolean cancel;
    private boolean paused;
    private boolean isPreskocit;
    private int preskocit;
    private Socket klient;

    public int getPriebezneNacitanie() {
        return priebezneNacitanie;
    }

    public void setPriebezneNacitanie(int priebezneNacitanie) {
        this.priebezneNacitanie = priebezneNacitanie;
        isPreskocit = true;
        preskocit = priebezneNacitanie;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    private int priebezneNacitanie;

    public KlientThread(int vlakno, int pocetVlakien, RandomAccessFile zapisovac, long length, GuiKopirovac.SaveWorker saveWorker, GuiKopirovac okno) {
        this.vlakno = vlakno;
        this.pocetVlakien = pocetVlakien;
        this.zapisovac = zapisovac;
        this.velkostSuboru = (int) length;
        this.okno = okno;
        priebezneNacitanie = 0;
    }

    @Override
    public void run() {
        try {
            int cast = velkostSuboru / pocetVlakien;
            byte[] data = new byte[cast];
            if (pocetVlakien - 1 == vlakno) {
                data = new byte[(cast) + (velkostSuboru - (cast * pocetVlakien))];
            }
            int port = server_port + vlakno;
            klient = new Socket("localhost", port);
            InputStream is = klient.getInputStream();

            if (isPreskocit){
                is.skip(preskocit);
            }
            int uzNacitane = priebezneNacitanie;
            int usek = 1024;
            while (priebezneNacitanie < data.length) {
                if (cancel || paused) {
                    klient.close();
                    is.close();
                }
                if ((priebezneNacitanie + usek) > data.length) {
                    uzNacitane = is.read(data, priebezneNacitanie, data.length - priebezneNacitanie);
                    synchronized (zapisovac) {
                        zapisovac.seek(cast * vlakno + priebezneNacitanie);
                        zapisovac.write(data, priebezneNacitanie, data.length - priebezneNacitanie);
                    }
                } else {
                    uzNacitane = is.read(data, priebezneNacitanie, usek);
                    synchronized (zapisovac) {
                        zapisovac.seek(cast * vlakno + priebezneNacitanie);
                        zapisovac.write(data, priebezneNacitanie, usek);
                    }
                }
                priebezneNacitanie += uzNacitane;
                okno.setPercenta(1);
            }

//            synchronized (zapisovac) {
//                zapisovac.seek(cast * vlakno);
//                zapisovac.write(data);
//            }

            okno.setPriebeh(okno.getPriebeh() + 1);
            klient.close();
            is.close();

        } catch (IOException e) {
            try {
                klient.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
