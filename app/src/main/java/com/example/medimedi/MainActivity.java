package com.example.medimedi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public  class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button captureImageBtn, detectTextBtn, serverBtn,pickImageBtn;
    public TextView textview, serverView, connection;
    private Bitmap imageBitmap;
    static final int REQUEST_IMAGE_CAPTURE = 672;
    static final int PICK_IMAGE = 1;
    static final String TAG = "MainActivity";

    //String currentPhotoPath;
    private Uri photoUri;
    private String imageFilePath,ocrtext;
    Medi medInfo;
    static  String strJson = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pickImageBtn= findViewById(R.id.gallery_image_btn);
        captureImageBtn = findViewById(R.id.capture_image_btn);
        detectTextBtn = findViewById(R.id.detect_text_image_btn);
        imageView = findViewById(R.id.image_view);
        textview = findViewById(R.id.text_display);
        serverBtn = findViewById(R.id.server_test_btn);
        serverView =  findViewById(R.id.server_test_display);
        connection = findViewById(R.id.IsConnected);

        // 와이파이 연결 확인
        if(isConnected()){
            connection.setBackgroundColor(0xFF00CC00);
            connection.setText("You are conncted");
        }
        else{
            connection.setText("You are NOT conncted");
        }

        //카메라에서 사진찍기
        captureImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
                //setPic();
            }
        });

        //갤러리에서 사진 가져오기
        pickImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickPictureInetent();
            }
        });

        //ocr로 텍스트 인식
        detectTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectTextFromImage();
                textview.setText("");
            }
        });
        //서버로 값 보내기
        serverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.server_test_btn:
                        if(!validate())
                            Toast.makeText(getBaseContext(), "Enter some data!", Toast.LENGTH_LONG).show();
                        else {
                            // call AsynTask to perform network operation on separate thread
                            HttpAsyncTask httpTask = new HttpAsyncTask(MainActivity.this);
                            httpTask.execute("http://13.209.4.83:3000/api", ocrtext.toString());
                        }
                        break;
                }

            }
        });
    }

    /*HttpURLConnection로 서버와 연동, 결과 텍스트 json POST로 보내기*/
    public static String POST(String url, Medi meditext){

        InputStream is = null;

        String result = "";

        try {

            URL urlCon = new URL(url);

            HttpURLConnection httpCon = (HttpURLConnection)urlCon.openConnection();

            String json = "";

            // build jsonObject

            JSONObject jsonObject = new JSONObject();
            jsonObject.accumulate("medInfo", Medi.getMedInfo());

            // convert JSONObject to JSON to String

            json = jsonObject.toString();

            httpCon.setRequestProperty("Accept", "application/json");
            httpCon.setRequestProperty("Content-type", "application/json");

            // OutputStream으로 POST 데이터를 넘겨주겠다는 옵션.
            httpCon.setDoOutput(true);
            // InputStream으로 서버로 부터 응답을 받겠다는 옵션.
            httpCon.setDoInput(true);

            OutputStream os = httpCon.getOutputStream();

            os.write(json.getBytes("utf-8"));

            os.flush();

            // receive response as inputStream

            try {

                is = httpCon.getInputStream();

                // convert inputstream to string

                if(is != null)

                    result = convertInputStreamToString(is);

                else

                    result = "Did not work!";

            }

            catch (IOException e) {
                e.printStackTrace();
            }

            finally {
                httpCon.disconnect();
            }
        }

        catch (IOException e) {

            e.printStackTrace();
        }

        catch (Exception e) {

            Log.d("InputStream", e.getLocalizedMessage());

        }

        return result;
    }

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {

        private   MainActivity mainAct;

        HttpAsyncTask(MainActivity mainActivity) {
            this.mainAct = mainActivity;
        }
        @Override
        protected String doInBackground(String... urls) {

            medInfo = new Medi();
            Medi.setMedInfo(urls[1]);

            return POST(urls[0], medInfo);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            strJson = result;
            mainAct.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mainAct, "Received!", Toast.LENGTH_LONG).show();
                    try {
                        JSONArray json = new JSONArray(strJson);
                        mainAct.serverView.setText(json.toString(1));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private boolean validate(){
        if(textview.toString().trim().equals(""))
            return false;

        else
            return true;
    }
    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    /*서버연동 끝*/

    //카메라에서 원본크기로 사진 찍기
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this,"사진찍기 실패", Toast.LENGTH_LONG).show();
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        "com.example.medimedi", photoFile);

                galleryAddPic();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

            }
        }
    }

    //갤러리에서 사진 가져오기
    private void pickPictureInetent() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*"); //이미지만 보이게
        //Intent 시작 - 갤러리앱을 열어서 원하는 이미지를 선택할 수 있다.
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ExifInterface exif = null;
        int exifOrientation;
        int exifDegree;

        switch (requestCode) {
            //사진찍기
            case REQUEST_IMAGE_CAPTURE:
            if (resultCode == RESULT_OK) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath);

                try {
                    exif = new ExifInterface(imageFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (exif != null) {
                    exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    exifDegree = exifOrientationToDegrees(exifOrientation);
                } else {
                    exifDegree = 0;
                }

                //그레이스케일 변환
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

                imageBitmap = rotate(bitmap, exifDegree);
                imageView.setImageBitmap(imageBitmap);

                imageView.setColorFilter(filter);
            }
            break;

            //앨범에서 사진골라라
           case PICK_IMAGE:
                if (resultCode == RESULT_OK && null != data){
                //data에서 절대경로로 이미지를 가져옴

                    Uri uri = data.getData();
                    String filepath =MediaStore.Images.Media.DATA;
                    Bitmap bitmap = BitmapFactory.decodeFile(filepath);

                    //Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
                        exif = new ExifInterface(filepath);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //이미지가 한계이상(?) 크면 불러 오지 못하므로 사이즈를 줄여 준다.
                  //  int nh = (int) (bitmap.getHeight() * (1024.0 / bitmap.getWidth()));
                    //Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 1024, nh, true);

                    if (exif != null) {
                        exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                        exifDegree = exifOrientationToDegrees(exifOrientation);
                    } else {
                        exifDegree = 0;
                    }

                    //그레이스케일 변환
                    ColorMatrix matrix = new ColorMatrix();
                    matrix.setSaturation(0);
                    ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

                    imageBitmap = rotate(bitmap, exifDegree);
                    imageView.setImageBitmap(bitmap);

                    imageView.setColorFilter(filter);

                } else {
                    Toast.makeText(this, "취소 되었습니다.", Toast.LENGTH_LONG).show();
                }

        }

    }


    //사진 회전하여 올바르게 보이도록
    private int exifOrientationToDegrees(int exifOrientation) {

        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }
    private Bitmap rotate(Bitmap Bitmap, float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(Bitmap, 0, 0, Bitmap.getWidth(), Bitmap.getHeight(), matrix, true);
    }

    //사진 파일이름 생성
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        imageFilePath = image.getAbsolutePath();
        return image;
    }

    //사진 갤러리에 추가하기
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imageFilePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        //Toast.makeText(this, "사진이 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    //사진크기 리사이징(선택)
    private void setPic() {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(100/10, 100/10);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath, bmOptions);
        imageView.setImageBitmap(bitmap);
    }

    //google cloud vision API로 텍스트 추출하기
    private void detectTextFromImage() {

        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionDocumentTextRecognizer firebaseVisionTextRecognizer = FirebaseVision.getInstance()
                .getCloudDocumentTextRecognizer();

        firebaseVisionTextRecognizer.processImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
                    @Override
                    public void onSuccess(FirebaseVisionDocumentText firebaseVisionText)
                    {
                        displayTextFromImage(firebaseVisionText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Error:" + e.getMessage(), Toast.LENGTH_SHORT);

                        Log.d("Error: ", e.getMessage());
                    }
                });

    }

    //텍스트 추출 후 결과 보여주기
    private void displayTextFromImage(FirebaseVisionDocumentText firebaseVisionText)
    {

        List<FirebaseVisionDocumentText.Block> blockList = firebaseVisionText.getBlocks();
        if (blockList.size() == 0)
        {
            Toast.makeText(this,"No text found", Toast.LENGTH_SHORT);

        }
        else
        {
            for (FirebaseVisionDocumentText.Block block : firebaseVisionText.getBlocks())
            {
                ocrtext = block.getText();
                textview.setText(ocrtext);
            }
        }
    }
}