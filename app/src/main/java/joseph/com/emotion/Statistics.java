package joseph.com.emotion;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Statistics extends AppCompatActivity {

    int anger = 0;
    int contempt = 0;
    int sadness = 0;
    int disgust = 0;
    int fear = 0;
    int happiness = 0;
    int surprised = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void getPercent(String filename) throws IOException {
        File file = new File(MainActivity.mContext.getFilesDir(), filename);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if(line.equals("anger")) {
                    anger += 1;
                }

                else if(line.equals("contempt")) {
                    contempt += 1;
                }

                else if(line.equals("sadness")) {
                    sadness += 1;
                }

                else if(line.equals("disgust")) {
                    disgust += 1;
                }

                else if(line.equals("fear")) {
                    fear += 1;
                }

                else if(line.equals("happiness")) {
                    happiness += 1;
                }

                else if(line.equals("surprised")) {
                    surprised += 1;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}



