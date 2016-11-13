package joseph.com.emotion;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.FaceRectangle;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.api.ClarifaiResponse;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.input.image.ClarifaiImage;
import clarifai2.dto.model.ConceptModel;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{


    public static String journalFileName = "journal.txt";
    public static String statsFileName = "stats.txt";


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

    public String toSpeak = "";


    public Bitmap imageBitmap;
    public byte[] imageBytes;

    Menu mainMenu = null;

    private ClarifaiClient clarclient;

    TextToSpeech t1;
    int REQUEST_CODE = 9;

    private EditText reply;

    private Button send;
    private TextView processing;

    private Feeling feeling;

    public static Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;


        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });

        reply = (EditText) findViewById(R.id.input);

        processing = (TextView) findViewById(R.id.processing);

        send = (Button) findViewById(R.id.send);
        send.setOnClickListener(this);


        Intent intent = new Intent(MainActivity.this, Receiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, REQUEST_CODE, intent, 0);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setRepeating(am.RTC_WAKEUP, System.currentTimeMillis(), am.INTERVAL_FIFTEEN_MINUTES / 1, pendingIntent);






        /*
        *
        *
        *CLARIFAI STUFF
        *
        *
        * */

        clarclient = new ClarifaiBuilder(getString(R.string.clarifai_id), getString(R.string.clarifai_secret))
                // Optionally customize HTTP client via a custom OkHttp instance
                .client(new OkHttpClient.Builder()
                                .readTimeout(30, TimeUnit.SECONDS) // Increase timeout for poor mobile networks

                                // Log all incoming and outgoing data
                                // NOTE: You will not want to use the BODY log-level in production, as it will leak your API request details
                                // to the (publicly-viewable) Android log
            /*.addInterceptor(new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
              @Override public void log(String logString) {
                Timber.e(logString);
              }
            }).setLevel(HttpLoggingInterceptor.Level.BODY))*/
                                .build()
                )
                .buildSync();




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

                requestCameraPermission();  //will launch camera intent after

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

                        processing.setText("Processing....");



                        //Clarifai stuff here?
                        imageBitmap = mBitmap;

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        imageBytes = stream.toByteArray();

                        System.out.println("About to send to clarifai");
                        sendImageBytesToClarifai(imageBytes);

                    }
                }
                break;
            default:
                break;
        }
    }

    public void sendToEmotion() {

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
                //mTextView.append("\n\nRecognizing emotions with auto-detected face rectangles...\n");
            } else {
                //mTextView.append("\n\nRecognizing emotions with existing face rectangles from Face API...\n");
            }
            if (e != null) {
                mTextView.setText("Error: " + e.getMessage());
                this.e = null;
            } else {
                if (result.size() == 0) {   //no face detected, give general response
                    processing.setText("");
                    toSpeak = "What's up?";

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


                         processing.setText("");

                    feeling = new Feeling(emotionNames, probabilities);



                    System.out.println("feeling: " + feeling.toString());
                    toSpeak = "Computer: Wow, you sure are feeling " + feeling.maxEmotion + " today. Tell me why.";
                    mTextView.setText(toSpeak);
                    t1.speak(toSpeak.replaceFirst("Computer: ", "").replaceFirst("You", ""), TextToSpeech.QUEUE_FLUSH, null);

                    writeToFile(statsFileName, feeling.maxEmotion + "\n");



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

            mTextView.setText(toSpeak);
            //    t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
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

    public void launchCamera() {
        Intent intent = new Intent(MainActivity.this, SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }


    public void requestCameraPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode) {
            case 1: {
                //if request is cancelled, the results arrays are empty
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    launchCamera();
                } else {
                    //permission denied
                    Toast.makeText(MainActivity.this, "Permission denied to access camera", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }


    private void sendImageBytesToClarifai(@NonNull final byte[] imageBytes) {
        System.out.println("Sending image to Clarifai");
        // Now we will upload our image to the Clarifai API
      ///  setBusy(true);

        // Make sure we don't show a list of old concepts while the image is being uploaded
    //    adapter.setData(Collections.<Concept>emptyList());

        new AsyncTask<Void, Void, ClarifaiResponse<List<ClarifaiOutput<Concept>>>>() {
            @Override protected ClarifaiResponse<List<ClarifaiOutput<Concept>>> doInBackground(Void... params) {

                // The default Clarifai model that identifies concepts in images
                 final ConceptModel generalModel = clarifaiClient().getDefaultModels().generalModel();
                System.out.println("step1");

                // Use this model to predict, with the image that the user just selected as the input
                return generalModel.predict()
                        .withInputs(ClarifaiInput.forImage(ClarifaiImage.of(imageBytes)))
                        .executeSync();
            }

            @Override protected void onPostExecute(ClarifaiResponse<List<ClarifaiOutput<Concept>>> response) {
        //        setBusy(false);
                System.out.println("step 1.1");
                if (!response.isSuccessful()) {
                    return;
                }
                final List<ClarifaiOutput<Concept>> predictions = response.get();
                if (predictions.isEmpty()) {
                    return;
                }
                System.out.println("step 2");
                System.out.println("predictions.get(0).data() = " + predictions.get(0).data());
                List<Concept> dataList = predictions.get(0).data();
                System.out.println("First data point:" + dataList.get(0).name());
                String firstDataPoint = dataList.get(0).name();

                Boolean isPerson = true;
                if (dataList.toString().contains("no person")){
                    isPerson = false;
                }
                /*for (int i = 0; i < dataList.size(); i++) {
                    if (dataList.get(i).name().equals("no person")) {
                        System.out.println("NO PERSON detected in image");
                        isPerson = false;
                    }
                }*/
                if (isPerson) {
                    System.out.println("Person is detected!");
                    //do microsoft stuff.
                    sendToEmotion();
                    System.out.println("Sending to emotion");
                }
        //        int i = 0;
              //  while (firstDataPoint.equals("no person") || firstDataPoint.equals("indoors")){

                if (firstDataPoint.equals("no person")) {
                    firstDataPoint = dataList.get(1).name();
//                    i++;
                    System.out.println("corrected firstdatapoint = " + firstDataPoint);
                }
             //   }
                if  (!isPerson) {
                    String message = "Oh boy, you should tell me about that " + firstDataPoint;
                    processing.setText("");
                    updateResponse(message);
                }




             //   imageView.setImageBitmap(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
            }


        }.execute();
    }


    @NonNull
    public ClarifaiClient clarifaiClient() {
        final ClarifaiClient clarclient = this.clarclient;
        if (clarclient == null) {
            throw new IllegalStateException("Cannot use Clarifai client before initialized");
        }
        return clarclient;
    }

    @Override
    public void onClick(View view) {
        String input = reply.getText().toString();
        mTextView.append("\n\n" + "You: " + input + "\n");
        reply.getText().clear();
        if (feeling != null) {
            if ((input.contains("sad") || input.contains("dead") || input.contains("died") || input.contains("dying") || input.contains("divorce ")) && feeling.maxEmotion.equals("sadness")) {
                String words = "Computer: Don't worry things will always get better";
                updateResponse(words);
            } else if ((input.contains("angry") || input.contains("divorce") || input.contains("bully") || input.contains("bullying")) && feeling.maxEmotion.equals("anger")) {
                String words = "Computer: Anger is a natural emotion, it will go away eventually. The main thing is to make sure you don't act rashly";
                updateResponse(words);
            } else if ((input.contains("I won ") || input.contains("I'm alive")) && feeling.maxEmotion.equals("happiness")) {
                String words = "Computer: Great, that's really great, I hope you stay happy and continue your successful endeavors";
                updateResponse(words);
            }
        }

        writeToFile(journalFileName, "\n\n" + "You: " + input + "\n");

    }

    public void updateResponse(String message) {
        mTextView.append("\n" + message);
        t1.speak(message.replaceFirst("Computer: ", ""), TextToSpeech.QUEUE_FLUSH, null);
        writeToFile(journalFileName, "\n" + message + "\n");
    }

    public void writeToFile(String filename, String text){
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

    }

    public boolean onCreateOptionsMenu(Menu menu) {
// Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        mainMenu = menu;
        return true;
    }


    //Menu press should open 3 dot menu
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mainMenu.performIdentifierAction(R.id.options, 0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.journal:
                Intent intent = new Intent(this, Journal.class);
                this.startActivity(intent);
                break;
            case R.id.stat:
                // another startActivity, this is for item with id "menu_item2"
                Intent intent2 = new Intent(this, Statistics.class);
                this.startActivity(intent2);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }


}
