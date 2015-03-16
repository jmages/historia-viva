package jujumap.juju;

import android.app.Activity;
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

public class TourListOnline extends ListActivity {

    String osmpath = "";
    String path    = "";
    String url     = "";

    SharedPreferences settings;
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

                BufferedReader br = new BufferedReader(new FileReader(f));

                String line;

                Pattern zipfilename = Pattern.compile("\".*?.zip\"");
                Pattern zipfiletext = Pattern.compile("\">.*?</a>");

                while ((line = br.readLine()) != null) {

                    if (line.contains("id=\"hv-tour\"")) {

                        // <li><a href="benjamin_fr_v01.zip" id="hv-tour">Chemin Walter Benjamin (français, version 01, 20 MB, aktuell nur ein Fake)</a></li>

                        Matcher m = zipfilename.matcher(line);

                        if (m.find()) {

                            String n = m.group().substring(1, m.group().length()-1);

                            valueList.add(n);
                        }

                        m = zipfiletext.matcher(line);

                        if (m.find()) {

                            String n = m.group().substring(2, m.group().length()-4);

                            //valueList.add(n);
                        }
                    }

                    if (line.contains("id=\"hv-map\"")) {

                        Matcher m = zipfilename.matcher(line);

                        while (m.find()) {

                            String n = m.group().substring(1, m.group().length()-1);

                            valueList.add(n);
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

            case progress_bar_type: // we set this to 0

                pDialog = new ProgressDialog(this);

                pDialog.setMessage(getString(R.string.downloading_please_wait));
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
    protected void onListItemClick (ListView listView, View v, int position, long id) {

        String selectedFileName = listView.getItemAtPosition(position).toString();

        Intent intent = new Intent();

        intent.putExtra("newPath", selectedFileName);

        if (selectedFileName.contains(".zip")) {

            DownloadFileFromURL downloadFileFromURL =
                    new DownloadFileFromURL(selectedFileName, this);

            downloadFileFromURL.execute(url);

            setResult(RESULT_CANCELED, intent);

        } else {

            setResult(RESULT_OK, intent);
        }
    }

    @Override
    protected void onDestroy() {

         super.onDestroy();
    }

    class DownloadFileFromURL extends AsyncTask <String, String, String> {

        private Activity caller;
        String fileName = "";

        public DownloadFileFromURL (String fileName, Activity caller) {

            this.fileName = fileName;
            this.caller   = caller;
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

            } catch (Exception e) {

                Log.e("Error - no connection: ", e.getMessage());
            }

            return null;
        }

        protected void onProgressUpdate(String... progress) {

            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void onPostExecute(String result) {

            Log.d("onPostExecute: ", "download finished ");

            dismissDialog(progress_bar_type);

            caller.finish();
        }

        @Override
        protected void onCancelled(){

            Log.d("onCancelled: ", "download cancelled");
        }

    }
}