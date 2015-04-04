package jujumap.jjmap;

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
import android.widget.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
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

        setContentView(R.layout.tour_view);

        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnected()) {

            ListView listView = this.getListView();

            listView.setFastScrollEnabled(true);

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

        String name = "Radreise-Wiki:RouteList.txt";

        int count;

        Log.d ("Downloading", url + name);

        try {

            URL url_i = new URL(url + name);

            URLConnection conection = url_i.openConnection();

            conection.connect();

            //int lenghtOfFile = conection.getContentLength();

            InputStream input = new BufferedInputStream(url_i.openStream(), 8192);

            OutputStream output = new FileOutputStream(path + "/" + "index.html");

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

            Log.e("Err - Download failed: ", e.getMessage());
        }

        File f = new File(path + "/" + "index.html");

        if (f.exists()) {

            StringBuilder text = new StringBuilder();

            try {

                BufferedReader br = new BufferedReader (new FileReader(f));

                String line;

                Pattern zipfilename = Pattern.compile(":: . :: .*$");

                while ((line = br.readLine()) != null) {

                    if (line.contains(" :: ")) {

                        Matcher m = zipfilename.matcher(line);

                        String fn = "";

                        if (m.find()) fn = m.group().substring(8, m.group().length());

                        valueList.add(fn);
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

        Collections.sort(valueList);

        ListAdapter adapter = new AlphabeticalAdapter (
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

        Log.d("onListItemClick", selectedFileName);

        Intent intent = new Intent();

        intent.putExtra("newPath", selectedFileName);

        DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL(selectedFileName + ".kmz");

        downloadFileFromURL.execute(url + "images/");

        setResult(RESULT_CANCELED, intent);
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

        @SuppressWarnings("deprecation")
        @Override
        protected String doInBackground (String... f_url) {

            int count = f_url.length;

            String result;

            String url_e;

            try {

                for (int i = 0; i < count; i++) {

                    url_e = URLEncoder.encode(fileName.replace(" ", "_"));

                    Log.d("Downloading", f_url[i] + url_e);

                    URL url = new URL(f_url[i] + url_e);

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
                }

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

            if (fileName.contains(".kmz")) {

                String destination = "";

                destination = fileName.substring(0, fileName.length()-4);

                Log.d("Unzipping source     ", ">" + path + "/" + fileName    + "<");
                Log.d("Unzipping destination", ">" + path + "/" + destination + "<");

                unzip (path + "/", fileName, destination);

            } else {

            }

            //dismissDialog(progress_bar_type);

            //finish();

            super.onPostExecute(result);
        }

        @Override
        protected void onCancelled(){

            Log.d("onCancelled: ", "download cancelled");
        }
    }

    class AlphabeticalAdapter extends ArrayAdapter <String> implements SectionIndexer
    {
        private HashMap<String, Integer> alphaIndexer;
        private String[] sections;

        public AlphabeticalAdapter(Context c, int resource, List <String> data)
        {
            super(c, resource, data);
            alphaIndexer = new HashMap<String, Integer>();
            for (int i = 0; i < data.size(); i++)
            {
                String s = data.get(i).substring(0, 1).toUpperCase();
                if (!alphaIndexer.containsKey(s))
                    alphaIndexer.put(s, i);
            }

            Set<String> sectionLetters = alphaIndexer.keySet();

            ArrayList<String> sectionList = new ArrayList<String>(sectionLetters);

            Collections.sort(sectionList);

            sections = new String[sectionList.size()];

            for (int i = 0; i < sectionList.size(); i++)

                sections[i] = sectionList.get(i);
        }

        public int getPositionForSection(int section)
        {
            return alphaIndexer.get(sections[section]);
        }

        public int getSectionForPosition(int position)
        {
            return 1;
        }

        public Object[] getSections()
        {
            return sections;
        }
    }

    public void unzip (String sourcePath, String zipFileName, String destination) {

        int size;
        int BUFFER_SIZE = 8192;
        byte[] buffer   = new byte[BUFFER_SIZE];

        String zipFile  = sourcePath + zipFileName;
        String destPath = sourcePath + destination;

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

                Log.d ("Unzipping", "finished");

                Intent i = new Intent();
                i.putExtra ("newPath", destination);

                File poitrackFile = new File(destPath + "/poitrack.kml");

                File kmlFile = new File(destPath + "/doc.kml");

                if (kmlFile.exists()) {

                    kmlFile.renameTo(poitrackFile);

                    setResult(RESULT_OK, i);

                    this.finish();
                }

                kmlFile = new File(destPath + "/" + destination + ".kml");

                if (kmlFile.exists()) {

                    kmlFile.renameTo(poitrackFile);

                    setResult(RESULT_OK, i);

                    this.finish();
                }

                setResult(RESULT_CANCELED, i);

                this.finish();
            }

        } catch (Exception e) {

            Log.e ("Error", "Unzip exception", e);
        }
    }
}