package joseph.com.emotion;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Journal extends AppCompatActivity {

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);

        textView = (TextView)findViewById(R.id.textView2);

        String journalText = readFile(MainActivity.journalFileName);
        textView.setText(journalText);


    }

    public String readFile(String filename) {
        String text = "";

        File file = new File(MainActivity.mContext.getFilesDir(), filename);
        System.out.println("reading file location: " + file.getAbsolutePath());
        FileInputStream inputStream;
        StringBuilder sb = new StringBuilder();
        try {
            inputStream = openFileInput(filename);
            int c;
            while((c = inputStream.read()) != -1) {
                sb.append((char)c);
            }
            text = sb.toString();
            System.out.println("Text read from file: " + text);
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return text;

    }

    /*public void writeToFile(String filename, String text){
        File file = new File(MainActivity.this.getFilesDir(), filename);
        System.out.println("file location: " + file.getAbsolutePath());
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_APPEND);
            outputStream.write(text.getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }*/
}
