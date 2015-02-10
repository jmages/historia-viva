package jujumap.juju;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TourView extends ListActivity {

    String path = "";
    String url  = "";

    private ProgressDialog pDialog;
    public static final int progress_bar_type = 0;

    @Override
    public void onCreate (Bundle bundle) {

        super.onCreate (bundle);

        Bundle b = getIntent().getExtras();

        path = b.getString("path");
        url  = b.getString("url");

        setContentView(R.layout.tour_view);

        ListAdapter adapter = createAdapter();

        setListAdapter(adapter);
    }

    protected ListAdapter createAdapter() {

        List valueList = new ArrayList <String> ();

        File [] files = new File(path).listFiles();

        for ( File aFile : files ) {

            if ( aFile.isDirectory() ) {

                valueList.add(aFile.getName());

            } else {

                if (aFile.getName().equals("index.html")) {

                    // just leave it there
                }

                if (aFile.getName().contains(".zip")) {

                    valueList.add(aFile.getName());
                }
            }
        }

        DownloadFileFromURL task = new DownloadFileFromURL("index.html");

        task.execute(url);

        File f = new File(path + "/" + "index.html");

        if (f.exists()) {

            StringBuilder text = new StringBuilder();

            try {

                BufferedReader br = new BufferedReader(new FileReader(f));

                String line;

                while ((line = br.readLine()) != null) {

                    if (line.contains("id=\"hv-tour\"")) {

                        // <li><a href="benjamin_fr_v01.zip" id="hv-tour">Chemin Walter Benjamin (français, version 01, 20 MB, aktuell nur ein Fake)</a></li>

                        Pattern p = Pattern.compile("\"(.*?.zip)\"");
                        Matcher m = p.matcher(line);

                        while (m.find()) {

                            String n = m.group().substring(1, m.group().length()-1);

                            valueList.add(n + " (Tour online)");
                        }
                    }

                    if (line.contains("id=\"hv-map\"")) {

                        Pattern p = Pattern.compile("\"(.*?.zip)\"");
                        Matcher m = p.matcher(line);

                        while (m.find()) {

                            String n = m.group().substring(1, m.group().length()-1);

                            valueList.add(n + " (Karte online)");
                        }
                    }

                    text.append(line);
                    text.append('\n');
                }

                br.close();

            } catch (IOException e) {

            }
        }

        ListAdapter adapter = new ArrayAdapter <String> (
            this,
            android.R.layout.simple_list_item_1,
            valueList);

        return adapter;
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {

            case progress_bar_type: // we set this to 0

                pDialog = new ProgressDialog(this);

                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();

                return pDialog;

            default:

                return null;
        }
    }

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id) {

        String selection = l.getItemAtPosition(position).toString();

        Intent i = new Intent();

        i.putExtra ("newPath", selection);

        if (selection.contains(" (Tour online)")) {

            String name = selection.substring(0,selection.length()-14);

            DownloadFileFromURL task_zip = new DownloadFileFromURL(name);

            task_zip.execute(url);

            setResult(RESULT_CANCELED, i);

        } else if (selection.contains(" (Karte online)")) {

                String name = selection.substring(0,selection.length()-15);

                Log.d ("xDownloading", ">"+name+"<");

                setResult(RESULT_CANCELED, i);

            } else {

            setResult(RESULT_OK, i);
        }
    }

    class DownloadFileFromURL extends AsyncTask <String, String, String> {

        String name = "";

        public DownloadFileFromURL (String name) {

            this.name = name;
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();

            showDialog(progress_bar_type);
        }

        @Override
        protected String doInBackground(String... f_url) {

            int count;

            try {

                Log.d ("Downloading", f_url[0] + name);

                URL url = new URL(f_url[0] + name);

                URLConnection conection = url.openConnection();

                conection.connect();

                int lenghtOfFile = conection.getContentLength();

                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                OutputStream output = new FileOutputStream(path + "/" + name);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {

                    total += count;

                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

            } catch (Exception e) {

                Log.e("Error - no connection: ", e.getMessage());
            }

            return null;
        }

        protected void onProgressUpdate(String... progress) {

            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String file_url) {

            dismissDialog(progress_bar_type);

        }

        @Override
        protected void onCancelled(){

            // Handle what you want to do if you cancel this task
        }

    }

    public static void unzip (String zipFile, String location) throws IOException {

        int size;

        int BUFFER_SIZE = 8192;

        byte[] buffer = new byte[BUFFER_SIZE];

        try {
            if ( !location.endsWith("/") ) {
                location += "/";
            }
            File f = new File(location);
            if(!f.isDirectory()) {
                f.mkdirs();
            }
            ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), BUFFER_SIZE));
            try {
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {
                    String path = location + ze.getName();
                    File unzipFile = new File(path);

                    if (ze.isDirectory()) {
                        if(!unzipFile.isDirectory()) {
                            unzipFile.mkdirs();
                        }
                    } else {
                        // check for and create parent directories if they don't exist
                        File parentDir = unzipFile.getParentFile();
                        if ( null != parentDir ) {
                            if ( !parentDir.isDirectory() ) {
                                parentDir.mkdirs();
                            }
                        }

                        // unzip the file
                        FileOutputStream out = new FileOutputStream(unzipFile, false);
                        BufferedOutputStream fout = new BufferedOutputStream(out, BUFFER_SIZE);
                        try {
                            while ( (size = zin.read(buffer, 0, BUFFER_SIZE)) != -1 ) {
                                fout.write(buffer, 0, size);
                            }

                            zin.closeEntry();
                        }
                        finally {
                            fout.flush();
                            fout.close();
                        }
                    }
                }
            }
            finally {
                zin.close();
            }
        }
        catch (Exception e) {
            Log.e ("Error", "Unzip exception", e);
        }
    }
}