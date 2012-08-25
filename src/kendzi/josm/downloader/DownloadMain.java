package kendzi.josm.downloader;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

public class DownloadMain {
    public static final String VERSION_URL = "http://josm.openstreetmap.de/version";

    private static final String CACHE_DIR = "./cache";

    JProgressBar progressBar;

    JFrame frame;

    public static URL parseUrl(String str) {
        try {
            return new URL(str);
        } catch (Exception e) {
            throw new RuntimeException("error parsing Url: " + str, e);
        }
    }

    enum JosmBranch {
        LATEST("josm-latest", parseUrl("http://josm.openstreetmap.de/josm-latest.jar")),
        TESTED("josm-tested", parseUrl("http://josm.openstreetmap.de/josm-tested.jar"));

        String branch;
        URL url;

        JosmBranch(String pBranch, URL url) {
            this.branch = pBranch;
            this.url = url;
        }

        public String getBranch() {
            return this.branch + ".jar:";
        }

        public String fileName(String version) {
            return this.branch + "_" + version + ".jar";
        }

        public URL getUrl() {
            return this.url;
        }
    }
    String getCurrentVersin(JosmBranch branch) {

        URL versionUrl = null;
        InputStream in = null;

        try {
            versionUrl = new URL(VERSION_URL);

            in = versionUrl.openStream();


            InputStreamReader is = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(is);
            String read = null;

            while((read = br.readLine()) != null) {
                System.out.println(read);

                if (read != null) {
                    if (read.startsWith(branch.getBranch())) {
                        return read.substring(branch.getBranch().length()).trim();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeStream(in);
        }
        return null;
    }

    void download() throws IOException {

        JosmBranch branch = JosmBranch.TESTED;

        String currentVersin = getCurrentVersin(branch);

        File f = new File(CACHE_DIR + "/" + branch.fileName(currentVersin));

        if (!f.exists()) {
            downloadJosm(f, branch.getUrl());
        }

        runJar(f);

    }

    private void runJar(File f) throws IOException {
        //Process proc = Runtime.getRuntime().exec("cmd /C java -jar \"" + f.getAbsoluteFile() + "\" ");
        Process proc = Runtime.getRuntime().exec("java -jar \"" + f.getAbsoluteFile() + "\" ");
    }

    private void downloadJosm(File f, URL url ) throws IOException {

        File tmp = new File(f.getAbsoluteFile() + ".tmp");

        if (tmp.exists()) {
            tmp.delete();
        }

        boolean error = false;

        tmp.getParentFile().mkdirs();

        FileOutputStream fos = new FileOutputStream(tmp);
        InputStream is = url.openStream();

        URLConnection conn = url.openConnection();
        int size = conn.getContentLength();

        showProgressUi(size);

        //	      conn = url.openConnection();
        //	      size = conn.getContentLength();
        //	      if(size < 0)
        //	         System.out.println("Could not determine file size.");
        //	      else
        //	        System.out.println(args[0] + "\nSize: " + size);


        try {
            copyUi(is, fos, 2048);

        } catch (Exception e) {
            e.printStackTrace();
            error = true;
        } finally {
            closeStream(fos);
            closeStream(is);
        }
        if (!error) {
            tmp.renameTo(f);
        } else {
            // remove part of file
            tmp.delete();
        }

        this.frame.dispose();

    }
    void closeStream(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void copyUi(InputStream in, OutputStream out, int bufferSize) throws IOException {
        // Read bytes and write to destination until eof
        long count = 0;
        byte[] buf = new byte[bufferSize];
        int len = 0;
        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
            count += len;
            System.out.println("downloaded: " + ((double) count / ((double)(1024 * 1024))) + " MB" );

            updateProgressBar((int) count);
        }
    }

    void showProgressUi(int size) {
        JFrame f = new JFrame();

        JProgressBar progressBar = new JProgressBar(0, size);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        f.add(progressBar);

        f.pack();
        f.setVisible(true);

        this.progressBar = progressBar;
        this.frame = f;
    }

    void updateProgressBar(int progres) {
        this.progressBar.setValue(progres);
    }

    public static void main(String[] args) throws IOException {

        DownloadMain dm = new DownloadMain();

        dm.download();
    }

}
