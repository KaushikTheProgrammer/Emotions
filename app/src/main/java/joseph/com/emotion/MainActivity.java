package joseph.com.emotion;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.FaceRectangle;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.contract.Scores;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private Button selectImageButton;

    private TextView mTextView;

    private EmotionServiceClient client;

    private static final int REQUEST_SELECT_IMAGE = 0;
    private Uri photoTaken;
    private Bitmap mBitmap;

    private Uri imageUri;


    private String mCurrentPhotoPath;

    private ArrayList<Feeling> feelingList = new ArrayList<>();

    private ArrayList<String>emotionNames = new ArrayList<>();
    public Boolean gotOneFace = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (client == null) {
            client = new EmotionServiceRestClient(getString(R.string.subscription_key));

        }

        emotionNames.add("anger");
        emotionNames.add("contempt");
        emotionNames.add("disgust");
        emotionNames.add("fear");
        emotionNames.add("happiness");
        emotionNames.add("sadness");
        emotionNames.add("surprise");



        mTextView = (TextView)findViewById(R.id.textView);

        selectImageButton = (Button)findViewById(R.id.pickImageButton);

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mTextView.setText("");

                Intent intent = new Intent(MainActivity.this, SelectImageActivity.class);
                startActivityForResult(intent, REQUEST_SELECT_IMAGE);
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("RecognizeActivity", "onActivityResult");
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    // If image is selected successfully, set the image URI and bitmap.
                    imageUri = data.getData();
                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            imageUri, getContentResolver());
                    if (mBitmap != null) {
                        // Add detection log.
                        Log.d("RecognizeActivity", "Image: " + imageUri + " resized to " + mBitmap.getWidth()
                                + "x" + mBitmap.getHeight());
                        doRecognize();
                    }
                }
                break;
            default:
                break;
        }
    }

    public void doRecognize() {

        try {
            new doRequest(false).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String faceSubscriptionKey = getString(R.string.faceSubscription_key);
        try {
            new doRequest(true).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

        gotOneFace = false;

    }



    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");

        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long startTime = System.currentTimeMillis();
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE STARTS HERE
        // -----------------------------------------------------------------------

        List<RecognizeResult> result = null;
        //
        // Detect emotion by auto-detecting faces in the image.
        //
        result = this.client.recognizeImage(inputStream);

        String json = gson.toJson(result);
        Log.d("result", json);

        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE ENDS HERE
        // -----------------------------------------------------------------------
        return result;
    }

    private List<RecognizeResult> processWithFaceRectangles() throws EmotionServiceException, com.microsoft.projectoxford.face.rest.ClientException, IOException {
        Log.d("emotion", "Do emotion detection with known face rectangles");
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long timeMark = System.currentTimeMillis();
        Log.d("emotion", "Start face detection using Face API");
        FaceRectangle[] faceRectangles = null;
        String faceSubscriptionKey = getString(R.string.faceSubscription_key);
        FaceServiceRestClient faceClient = new FaceServiceRestClient(faceSubscriptionKey);
        Face faces[] = faceClient.detect(inputStream, false, false, null);
        Log.d("emotion", String.format("Face detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));

        if (faces != null) {
            faceRectangles = new FaceRectangle[faces.length];

            for (int i = 0; i < faceRectangles.length; i++) {
                // Face API and Emotion API have different FaceRectangle definition. Do the conversion.
                com.microsoft.projectoxford.face.contract.FaceRectangle rect = faces[i].faceRectangle;
                faceRectangles[i] = new com.microsoft.projectoxford.emotion.contract.FaceRectangle(rect.left, rect.top, rect.width, rect.height);
            }
        }

        List<RecognizeResult> result = null;
        if (faceRectangles != null) {
            inputStream.reset();

            timeMark = System.currentTimeMillis();
            Log.d("emotion", "Start emotion detection using Emotion API");
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE STARTS HERE
            // -----------------------------------------------------------------------
            result = this.client.recognizeImage(inputStream, faceRectangles);

            String json = gson.toJson(result);
            Log.d("result", json);
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE ENDS HERE
            // -----------------------------------------------------------------------
            Log.d("emotion", String.format("Emotion detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));
        }
        return result;
    }


    private class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {
        // Store error message
        private Exception e = null;
        private boolean useFaceRectangles = false;

        public doRequest(boolean useFaceRectangles) {
            this.useFaceRectangles = useFaceRectangles;
        }

        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            if (this.useFaceRectangles == false) {
                try {
                    return processWithAutoFaceDetection();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            } else {
                try {
                    return processWithFaceRectangles();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            // Display based on error existence
            if (!gotOneFace) {

            if (this.useFaceRectangles == false) {
                mTextView.append("\n\nRecognizing emotions with auto-detected face rectangles...\n");
            } else {
                mTextView.append("\n\nRecognizing emotions with existing face rectangles from Face API...\n");
            }
            if (e != null) {
                mTextView.setText("Error: " + e.getMessage());
                this.e = null;
            } else {
                if (result.size() == 0) {
                    mTextView.append("No emotion detected :(");
                } else {
                    RecognizeResult r  = result.get(0);

                        ArrayList<Double> probabilities = new ArrayList<>();
                        probabilities.add(r.scores.anger);
                        probabilities.add(r.scores.contempt);
                        probabilities.add(r.scores.disgust);
                        probabilities.add(r.scores.fear);
                        probabilities.add(r.scores.happiness);
                        probabilities.add(r.scores.sadness);
                        probabilities.add(r.scores.surprise);


                        Feeling feeling = new Feeling(emotionNames, probabilities);

                        System.out.println("feeling: " + feeling.toString());
                        mTextView.setText("Wow, you sure are feeling " + feeling.maxEmotion + " today. Tell me why.");

                        gotOneFace = true;


                       /* mTextView.append(String.format("\nFace #%1$d \n", count));
                        mTextView.append(String.format("\t anger: %1$.5f\n", r.scores.anger));
                        mTextView.append(String.format("\t contempt: %1$.5f\n", r.scores.contempt));
                        mTextView.append(String.format("\t disgust: %1$.5f\n", r.scores.disgust));
                        mTextView.append(String.format("\t fear: %1$.5f\n", r.scores.fear));
                        mTextView.append(String.format("\t happiness: %1$.5f\n", r.scores.happiness));
                        mTextView.append(String.format("\t neutral: %1$.5f\n", r.scores.neutral));
                        mTextView.append(String.format("\t sadness: %1$.5f\n", r.scores.sadness));
                        mTextView.append(String.format("\t surprise: %1$.5f\n", r.scores.surprise));
                        mTextView.append(String.format("\t face rectangle: %d, %d, %d, %d", r.faceRectangle.left, r.faceRectangle.top, r.faceRectangle.width, r.faceRectangle.height));
                       */
               }
            }
            }

        }
    }

    class Feeling {
        ArrayList<String>emotionNames = new ArrayList<>();
        ArrayList<Double>probabilities = new ArrayList<>();

        String maxEmotion;

        public Feeling(ArrayList<String> emotionNames, ArrayList<Double>probabilities){
            this.emotionNames = emotionNames;
            this.probabilities = probabilities;

            maxEmotion = emotionNames.get(probabilities.indexOf(Collections.max(probabilities)));
        }

        public String toString(){
            String result = "";
            for (int i = 0; i < emotionNames.size(); i++){
                result += emotionNames.get(i) + ": " + probabilities.get(i);
                result += "\n";
            }
            return result;
        }



    }


}
