import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 *
 * Created by Milan Chrastina on 27.02.2016.
 */
public class ServerThread implements Runnable {

    private Socket klient;
    private byte[] data;

    public ServerThread(Socket klient, byte[] data) {
        this.klient = klient;
        this.data = data;
    }

    @Override
    public void run() {
        try {
            OutputStream os = klient.getOutputStream();
            try {
                os.write(data, 0, data.length);
                os.flush();
                klient.close();
            } catch (SocketException e) {
                klient.close();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}

