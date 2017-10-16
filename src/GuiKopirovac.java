import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

/**
 *
 * Created by Milan Chrastina on 20.02.2016.
 */
public class GuiKopirovac extends JFrame {
    private boolean pokracujem = false;
    private JPanel panel1;
    private JTextField vyberteSuborKtorySaTextField;
    private JButton btnZdroj;
    private JTextField vyberteCielKopirovaniaTextField;
    private JButton cancelButton;
    private JButton btnCiel;
    private JSpinner spinner1;
    private JProgressBar progressBar1;
    private JLabel PocetSocketov;
    private JButton pauzaButton;
    private JButton startButton;
    private JSlider slider1;
    private final static int server_port = 5500;
    private SaveWorker sw = null;
    private double percenta;
    private volatile int priebeh;
    private final GuiKopirovac okno = this;
    boolean kopirujem = false;
    private RandomAccessFile zapisovac;
    private File novySubor;
    private KlientThread[] vlakna;
    private File zaloha = new File("zaloha.txt");
    int[] zalohaCasti;
    int progresBarPoZalohe = 0;


    public void setPercenta(double percenta) {
        this.percenta += percenta;
    }

    public int getPriebeh() {
        return priebeh;
    }

    public void setPriebeh(int priebeh) {
        this.priebeh = priebeh;
    }

    protected final class SaveWorker extends SwingWorker<Void, Integer> {


        @Override
        protected Void doInBackground() throws Exception {
            percenta = 0;
            try {
                Socket soket = new Socket("localhost", server_port);
                PrintWriter out = new PrintWriter(soket.getOutputStream());

                out.println("subor" + "#" + vyberteSuborKtorySaTextField.getText() + "#" + spinner1.getValue());
                out.flush();
                soket.close();

                File s = new File(vyberteSuborKtorySaTextField.getText());
                String novaCesta = vyberteCielKopirovaniaTextField.getText() + "\\" + s.getName();
                novySubor = new File(novaCesta);

                zapisovac = new RandomAccessFile(novySubor, "rw");
                zapisovac.setLength(s.length());


                int pocetVlakien = (int) spinner1.getValue();
                vlakna = new KlientThread[pocetVlakien];
                if (!zaloha.exists()) {
                    setPercenta(0);
                    setPriebeh(0);
                    for (int i = 0; i < pocetVlakien; i++) {
                        vlakna[i] = new KlientThread(i, pocetVlakien, zapisovac, s.length(), this, okno);
                        Thread vlakno = new Thread(vlakna[i]);
                        vlakno.start();
                    }
                } else {
                    okno.setPercenta(progresBarPoZalohe);
                    for (int i = 0; i < pocetVlakien; i++) {
                        vlakna[i] = new KlientThread(i, pocetVlakien, zapisovac, s.length(), this, okno);
                        vlakna[i].setPriebezneNacitanie(zalohaCasti[i]);
                        Thread vlakno = new Thread(vlakna[i]);
                        vlakno.start();
                    }
                }

                while (okno.getPriebeh() < pocetVlakien) {
                    int percento = (int) (percenta * 1024 / s.length() * 100) + progresBarPoZalohe;
                    publish(percento);
                }

                zapisovac.close();
                if (zaloha.exists()) zaloha.delete();


            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return null;
        }


        @Override
        protected void process(List<Integer> chunks) {
            // v liste máme viacero percent, zaujíma nás len posledné

            int velkost = chunks.get(chunks.size() - 1);
            progressBar1.setValue(velkost);
            progressBar1.setString(progressBar1.getValue() + "%");

        }

        @Override
        protected void done() {
            progressBar1.setValue(100);
            progressBar1.setString(100+"%");
            spinner1.setEnabled(true);
            slider1.setEnabled(true);
            btnZdroj.setEnabled(true);
            btnCiel.setEnabled(true);
            startButton.setEnabled(true);
            startButton.setText("Start");
            pauzaButton.setEnabled(false);
            kopirujem = false;

        }

    }

    public GuiKopirovac() {
        setContentPane(panel1);
        pack();

        SpinnerNumberModel model1 = new SpinnerNumberModel(4, 4, 10, 1);
        spinner1.setModel(model1);
        spinner1.addChangeListener(e2 -> slider1.setValue((int) spinner1.getValue()));
        slider1.setMinimum(4);
        slider1.setValue((int) spinner1.getValue());
        slider1.setMaximum(10);
        slider1.addChangeListener(e1 -> spinner1.setValue(slider1.getValue()));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        vyberteSuborKtorySaTextField.setText("C:\\Users\\chras\\Documents\\Arrow.S04E13.HDTV.x264-LOL.mp4");
        vyberteCielKopirovaniaTextField.setText("C:\\Users\\chras\\Desktop");


        try {
            Socket soket = new Socket("localhost", server_port);
            PrintWriter out = new PrintWriter(soket.getOutputStream());
            out.println("pripojeny");
            out.flush();
            soket.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Nepodarilo sa pripojit na server", "Chyba", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Obnovenie kopirovania zo zalohy
        if (zaloha.exists()) {
            try {
                pokracujem = true;
                Scanner sc = new Scanner(zaloha);
                vyberteSuborKtorySaTextField.setText(sc.next());
                btnCiel.setEnabled(false);
                vyberteCielKopirovaniaTextField.setText(sc.next());
                btnZdroj.setEnabled(false);
                spinner1.setValue(sc.nextInt());
                spinner1.setEnabled(false);
                slider1.setEnabled(false);
                progresBarPoZalohe = sc.nextInt();
                progressBar1.setValue(progresBarPoZalohe);
                progressBar1.setString(progressBar1.getValue() + "%");
                priebeh = sc.nextInt();
                zalohaCasti = new int[slider1.getValue()];
                for (int i=0; i<zalohaCasti.length; i++)
                    zalohaCasti[i] = sc.nextInt();
                sc.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        btnZdroj.addActionListener(e -> {
            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.setDialogTitle("Vyberte subor na kopirovanie");
            int returnVal = jFileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File subor = jFileChooser.getSelectedFile();
                vyberteSuborKtorySaTextField.setText(subor.getAbsolutePath());
            }
        });

        btnCiel.addActionListener(e -> {
            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.setDialogTitle("Vyberte zlozku kde sa subor ulozi");
            jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = jFileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File subor = jFileChooser.getSelectedFile();
                vyberteCielKopirovaniaTextField.setText(subor.getAbsolutePath());
            }
        });

        setVisible(true);

        pauzaButton.setEnabled(false);

        startButton.addActionListener(e -> {
            if (vyberteSuborKtorySaTextField.getText().equals("") || vyberteCielKopirovaniaTextField.getText().equals(""))
                return;

            if (zaloha.exists()) {
                progresBarPoZalohe = progressBar1.getValue();
                progressBar1.setString(progresBarPoZalohe+ "%");
                progressBar1.setValue(progresBarPoZalohe);
            } else {
                progressBar1.setString("0");
                progressBar1.setValue(0);
                progresBarPoZalohe = 0;
            }
            spinner1.setEnabled(false);
            slider1.setEnabled(false);
            btnZdroj.setEnabled(false);
            btnCiel.setEnabled(false);
            startButton.setEnabled(false);
            pauzaButton.setEnabled(true);

            sw = new SaveWorker();
            sw.execute();
            kopirujem = true;


        });

        cancelButton.addActionListener(e -> {
            if (kopirujem) {
                sw.cancel(true);
                for (int i = 0; i < (int) spinner1.getValue(); i++) {
                    vlakna[i].setCancel(true);
                }
                try {
                    zapisovac.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                novySubor.delete();
                zaloha.delete();
                System.out.println("subor sa zmazal");
            }
            if (pokracujem) {
                File s = new File(vyberteSuborKtorySaTextField.getText());
                String novaCesta = vyberteCielKopirovaniaTextField.getText() + "\\" + s.getName();
                novySubor = new File(novaCesta);
                novySubor.delete();
                zaloha.delete();
            }
            this.dispose();
        });

        pauzaButton.addActionListener(e -> {
            zaloha = new File("zaloha.txt");
            try {

                PrintWriter pw = new PrintWriter(zaloha);
                pw.println(vyberteSuborKtorySaTextField.getText());
                pw.println(vyberteCielKopirovaniaTextField.getText());
                pw.println(slider1.getValue());
                pw.println(progressBar1.getValue());

                pw.println(priebeh);
                zalohaCasti = new int[slider1.getValue()];
                for (int i = 0; i < slider1.getValue(); i++) {
                    vlakna[i].setPaused(true);
                    zalohaCasti[i] = vlakna[i].getPriebezneNacitanie();
                    pw.println(vlakna[i].getPriebezneNacitanie());
                }
                pw.close();
                pokracujem = true;
                kopirujem = true;
                startButton.setEnabled(true);
                startButton.setText("Pokracuj");
                pauzaButton.setEnabled(false);



            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(GuiKopirovac::new);
    }
}