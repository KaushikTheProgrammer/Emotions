package joseph.com.emotion;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Statistics extends AppCompatActivity {

    double anger = 0;
    double contempt = 0;
    double sadness = 0;
    double disgust = 0;
    double fear = 0;
    double happiness = 0;
    double surprise = 0;

    TextView fearView;
    TextView sadnessView;
    TextView disgustView;
    TextView surpriseView;
    TextView happinessView;
    TextView contemptView;
    TextView angerView;


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        try {
            getPercent(MainActivity.statsFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        angerView.setText("Anger: " + Double.toString(anger));
        sadnessView.setText("Sadness: " + Double.toString(sadness));
        disgustView.setText("Disgust: " + Double.toString(disgust));
        surpriseView.setText("Surprise: " + Double.toString(surprise));
        happinessView.setText("Happiness: " + Double.toString(happiness));
        contemptView.setText("Contempt: " + Double.toString(contempt));
        fearView.setText("Fear: " + Double.toString(fear));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void getPercent(String filename) throws IOException {
        File file = new File(MainActivity.mContext.getFilesDir(), filename);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("line = " + line);
                if(line.contains("anger")) {
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
                    surprise += 1;
                }
            }

            double total = anger+contempt+sadness+disgust+fear+happiness+surprise;

            anger = (anger/total) * 100;
            contempt = (contempt/total) * 100;
            sadness = (sadness/total) * 100;
            disgust = (disgust/total) * 100;
            fear = (fear/total) * 100;
            happiness = (happiness/total) * 100;
            surprise = (surprise/total) * 100;

            System.out.println("Anger is " + anger);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}