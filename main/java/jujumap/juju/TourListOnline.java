package jujumap.juju;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TourListOnline extends ListActivity {

    String osmpath = "";
    String path    = "";
    String url     = "";

    SharedPreferences        settings;
    SharedPreferences.Editor editor;

    private ProgressDialog pDialog;

    public static final int progress_bar_type = 0;

    @Override
    public void onCreate (Bundle bundle) {

        super.onCreate (bundle);

        Bundle b = getIntent().getExtras();

        osmpath = b.getString("osmpath");
        path    = b.getString("path");
        url     = b.getString("url");

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        editor   = settings.edit();

        setContentView(R.layout.tour_view);

        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnected()) {

            ListAdapter adapter = createAdapter();

            setListAdapter(adapter);

        } else {

            Toast.makeText(this,
                    getString(R.string.toast_please_connect),
                    Toast.LENGTH_LONG).show();

            this.finish();
        }
    }

    protected ListAdapter createAdapter() {

        List valueList = new ArrayList <String> ();

        //DownloadFileFromURL task = new DownloadFileFromURL("index.html");

        //task.execute(url);

        String name = "index.html";

        int count;

        Log.d ("Downloading", url + name);

        try {

            URL url_i = new URL(url + name);

            URLConnection conection = url_i.openConnection();

            conection.connect();

            //int lenghtOfFile = conection.getContentLength();

            InputStream input = new BufferedInputStream(url_i.openStream(), 8192);

            OutputStream output = new FileOutputStream(path + "/" + name);

            byte data[] = new byte[1024];

            long total = 0;

            while ((count = input.read(data)) != -1) {

                total += count;

                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

        } catch (Exception e) {

            Log.e("Error - no connection: ", e.getMessage());
        }

        File f = new File(path + "/" + "index.html");

        if (f.exists()) {

            StringBuilder text = new StringBuilder();

            try {

                BufferedReader br = new BufferedReader (new FileReader(f));

                String line;

                Pattern zipfilename = Pattern.compile("\".*?.zip\"");
                Pattern zipfiletext = Pattern.compile("\">.*?</a>");

                while ((line = br.readLine()) != null) {

                    if (line.contains("id=\"hv-")) {

                        // <li><a href="benjamin_fr_v01.zip" id="hv-tour">Chemin Walter Benjamin (français, version 01, 20 MB, aktuell nur ein Fake)</a></li>

                        Matcher m = zipfilename.matcher(line);

                        String fn = "";
                        String tn = "";

                        if (m.find()) fn = m.group().substring(1, m.group().length()-1);

                        m = zipfiletext.matcher(line);

                        if (m.find()) tn = m.group().substring(2, m.group().length()-4);

                        if ( (! fn.equals("")) && (! tn.equals("")) ) {

                            JujuMap.tour_file2text.put(fn, tn);
                            JujuMap.tour_text2file.put(tn, fn);

                            valueList.add(tn);

                        } else if ( (! fn.equals("")) && tn.equals("") ) {

                            valueList.add(fn);
                        }
                    }

                    text.append(line);
                    text.append('\n');
                }

                br.close();

            } catch (IOException e) {

            }
        }

        if (valueList.size() == 0) {

        }

        ListAdapter adapter = new ArrayAdapter <String> (
                this,
                android.R.layout.simple_list_item_1,
                valueList);

        return adapter;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {

            case progress_bar_type:

                pDialog = new ProgressDialog(this);

                pDialog.setMessage(getString(R.string.downloading_please_wait));
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(false);
                pDialog.show();

                return pDialog;

            default:

                return null;
        }
    }

    @Override
    protected void onListItemClick (ListView listView, View v, int position, long id) {

        String selectedFileName = listView.getItemAtPosition(position).toString();

        if (JujuMap.tour_text2file.containsKey(selectedFileName))

            selectedFileName = JujuMap.tour_text2file.get(selectedFileName);

        Intent intent = new Intent();

        intent.putExtra("newPath", selectedFileName);

        if (selectedFileName.contains(".zip")) {

            DownloadFileFromURL downloadFileFromURL =
                    new DownloadFileFromURL(selectedFileName);

            downloadFileFromURL.execute(url);

            setResult(RESULT_CANCELED, intent);

        } else {

            setResult(RESULT_OK, intent);
        }
    }

    class DownloadFileFromURL extends AsyncTask <String, String, String> {

        // AsyncTask <TypeOfVarArgParams , ProgressValue , ResultValue>

        String fileName = "";

        public DownloadFileFromURL (String fileName) {

            this.fileName = fileName;
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void onPreExecute() {

            super.onPreExecute();

            showDialog(progress_bar_type);
        }

        @Override
        protected String doInBackground (String... f_url) {

            int count;

            String result;

            try {

                Log.d ("Downloading", f_url[0] + fileName);

                URL url = new URL (f_url[0] + fileName);

                URLConnection conection = url.openConnection();

                conection.connect();

                int lenghtOfFile = conection.getContentLength();

                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                OutputStream output = new FileOutputStream(path + "/" + fileName);

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

                result = "finished";

            } catch (Exception e) {

                Log.e("Error - no connection: ", e.getMessage());

                result = "error";
            }

            return result;
        }

        protected void onProgressUpdate(String... progress) {

            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void onPostExecute(String result) {

            Log.d("onPostExecute: ", "download " + result);

            if (fileName.contains("mapnik")) {

                Log.d("Installing map", ">" + path + "/" + fileName + "<");

                try {

                    copy(path + "/" + fileName, osmpath + "/Mapnik.zip");

                } catch (IOException e) {

                    Log.e("IO Error", e.toString());
                }

                setResult(RESULT_CANCELED, new Intent());

            } else {

                String destination = fileName.substring(0, fileName.length() - 4);

                Log.d("Unzipping source     ", ">" + path + "/" + fileName + "<");
                Log.d("Unzipping destination", ">" + path + "/" + destination + "<");

                unzip(path + "/", fileName, destination);

                Intent i = new Intent();
                i.putExtra("newPath", destination);

                setResult(RESULT_OK, i);
            }

            dismissDialog(progress_bar_type);

            finish();

            super.onPostExecute(result);
        }

        @Override
        protected void onCancelled(){

            Log.d("onCancelled: ", "download cancelled");
        }
    }

    public void unzip (String sourcePath, String zipFileName, String destination) {

        int size;
        int BUFFER_SIZE = 8192;
        byte[] buffer   = new byte[BUFFER_SIZE];

        String zipFile  = sourcePath + zipFileName;
        String destPath = sourcePath + destination;

        Toast.makeText(this,
                getString(R.string.toast_unzipping_1) + zipFileName + getString(R.string.toast_unzipping_2),
                Toast.LENGTH_LONG).show();

        try {

            if ( ! destPath.endsWith("/") ) {

                destPath += "/";
            }

            File f = new File(destPath);

            if (! f.isDirectory()) {

                f.mkdirs();
            }

            ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), BUFFER_SIZE));

            try {

                ZipEntry ze = null;

                while ((ze = zin.getNextEntry()) != null) {

                    String path = destPath + ze.getName();
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

                File file = new File(zipFile);

                file.delete();

                Log.d("Unzipping", "finished");
            }

        } catch (Exception e) {

            Log.e ("Error", "Unzip exception", e);
        }
    }

    public void copy (String src, String dst) throws IOException {

        InputStream   in = new FileInputStream (new File (src));
        OutputStream out = new FileOutputStream(new File (dst));

        byte[] buf = new byte[1024];

        int len;

        while ((len = in.read(buf)) > 0) {

            out.write(buf, 0, len);
        }

        in.close();
        out.close();

        Log.d("Filecopy", "Copying finished.");

        Toast.makeText(this, getString(R.string.toast_offline_map_installed), Toast.LENGTH_LONG).show();
    }
}