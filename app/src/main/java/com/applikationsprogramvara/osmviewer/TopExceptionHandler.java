package com.applikationsprogramvara.osmviewer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class TopExceptionHandler implements Thread.UncaughtExceptionHandler {
    public static final String STACK_TRACE_FILENAME = "error_stack.trace";
    private Thread.UncaughtExceptionHandler defaultUEH;
    private Context context;

    public TopExceptionHandler(Context context) {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        this.context = context;
    }

    public static void retrieveLastErrorReport(Context context) {

        String trace = "";
        String stack_trace_filename = STACK_TRACE_FILENAME;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.openFileInput(stack_trace_filename)));
            String line;
            while((line = reader.readLine()) != null) {
                trace += line+"\n";
            }
        } catch(FileNotFoundException fnfe) {
            return;
        } catch(IOException ioe) {
            return;
        }

        if (trace.equals(""))
            return;

        // option 2 - Save to clipboardTopic

        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Error report", trace);
        clipboard.setPrimaryClip(clip);

        context.deleteFile(stack_trace_filename);

        Toast.makeText(context, "Error report is copied to the clipboard", Toast.LENGTH_LONG).show();
    }

    public void uncaughtException(Thread t, Throwable e) {
        StackTraceElement[] arr = e.getStackTrace();
        String report = e.toString()+"\n\n";
        report += "--------- Stack trace ---------\n\n";
        for (int i=0; i<Math.min(arr.length, 1000); i++) {
            report += "    "+arr[i].toString()+"\n";
        }
        report += "-------------------------------\n\n";

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause

        report += "--------- Cause ---------\n\n";
        Throwable cause = e.getCause();
        if(cause != null) {
            report += cause.toString() + "\n\n";
            arr = cause.getStackTrace();
            for (int i=0; i<arr.length; i++) {
                report += "    "+arr[i].toString()+"\n";
            }
        }
        report += "-------------------------------\n\n";

        try {
            FileOutputStream trace = context.openFileOutput(
                    //Environment.getExternalStorageDirectory() + File.separator +
                    STACK_TRACE_FILENAME,
                    Context.MODE_PRIVATE);
            trace.write(report.getBytes());
            trace.close();
        } catch(IOException ioe) {
        }

        defaultUEH.uncaughtException(t, e);
    }
}