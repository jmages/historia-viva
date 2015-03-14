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
import android.widget.Toast;

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

public class TourListOffline extends ListActivity {

    String osmpath = "";
    String path    = "";

    private ProgressDialog pDialog;

    public static final int progress_bar_type = 0;

    ListAdapter adapter;

    @Override
    public void onCreate (Bundle bundle) {

        super.onCreate (bundle);

        Bundle b = getIntent().getExtras();

        osmpath = b.getString("osmpath");
        path    = b.getString("path");

        setContentView(R.layout.tour_view);

        adapter = createAdapter();

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

        if (valueList.size() == 0) {

            Toast.makeText(this,
                    getString(R.string.toast_no_offline_tours),
                    Toast.LENGTH_LONG).show();

            this.finish();

        }

        ListAdapter adapter = new ArrayAdapter <String> (
                this,
                android.R.layout.simple_list_item_1,
                valueList);

        return adapter;
    }

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id) {

        String selection = l.getItemAtPosition(position).toString();

        Intent i = new Intent();

        i.putExtra ("newPath", selection);

        if (selection.contains(".zip")) {

            if (selection.contains("mapnik")) {

                Log.d("Installing map", ">" + path + "/" + selection + "<");

                try {
                    copy (path + "/" + selection, osmpath + "/Mapnik.zip");

                } catch (IOException e) {

                    Log.e("IO Error", e.toString());
                }

                setResult(RESULT_CANCELED, i);

            } else {

                String destination = "";

                destination = path + "/" + selection.substring(0, selection.length()-4);

                Log.d("Unzipping source     ", ">" + path + "/" + selection + "<");
                Log.d("Unzipping destination", ">" + destination + "<");

                Toast.makeText(this, getString(R.string.toast_unzipping_1) + selection + getString(R.string.toast_unzipping_2),
                        Toast.LENGTH_LONG).show();

                unzip (path + "/" + selection, destination);

                setResult(RESULT_CANCELED, i);

                adapter = createAdapter();

                setListAdapter(adapter);
            }

        } else {

            setResult(RESULT_OK, i);

            this.finish();
        }
    }

    public void unzip (String zipFile, String location) {

        int size;

        int BUFFER_SIZE = 8192;

        byte[] buffer = new byte[BUFFER_SIZE];

        try {

            if ( ! location.endsWith("/") ) {

                location += "/";
            }

            File f = new File(location);

            if (! f.isDirectory()) {

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

                File file = new File(zipFile);

                file.delete();

                Log.d ("Unzipping", "finished");

                Toast.makeText(this, getString(R.string.toast_unzipping_finished), Toast.LENGTH_LONG).show();
            }
        }
        catch (Exception e) {

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

        Toast.makeText(this, getString(R.string.toast_copying_finished), Toast.LENGTH_LONG).show();
    }
}