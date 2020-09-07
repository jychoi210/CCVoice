package com.example.gill.cc_voice;

/**
 * Created by gill on 18. 5. 31.
 */

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.apache.http.util.EntityUtils.getContentCharSet;

public class Join extends AppCompatActivity {
    EditText et_id;
    EditText et_pw;
    EditText et_pw_chk;
    EditText et_name;
    String sId, sPw, sPw_chk, sName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.join);
        //BaseActivity.setGlobalFont(this, getWindow().getDecorView());

        et_id = (EditText) findViewById(R.id.editText01);
        et_pw = (EditText) findViewById(R.id.editText02);
        et_pw_chk = (EditText) findViewById(R.id.editText05);
        et_name = (EditText) findViewById(R.id.editText03);

        sId = et_id.getText().toString().trim();
        sPw = et_pw.getText().toString().trim();
        sPw_chk = et_pw_chk.getText().toString().trim();
        sName = et_name.getText().toString().trim();


        //getSupportActionBar().setTitle(Html.fromHtml("<font color='#000000'>검색하기</font>"));
        getSupportActionBar().setTitle(Html.fromHtml("<font color='#000000'>회원가입</font>"));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFFFFFF));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Log.v("plz", "1");
    }



    public void bt_Join(View view) {
        Log.v("plz", "2");
        sId = et_id.getText().toString().trim();
        sPw = et_pw.getText().toString().trim();
        sPw_chk = et_pw_chk.getText().toString().trim();
        sName = et_name.getText().toString().trim();

        if (sId.getBytes().length <= 0 || sPw.getBytes().length <= 0 || sPw_chk.getBytes().length <= 0 || sName.getBytes().length <= 0 ) {
            Log.v("plz", "3");
            AlertDialog.Builder alert = new AlertDialog.Builder(Join.this);
            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alert.setMessage("정보를 다 입력해주세요");
            alert.show();

        }
        else if (!Pattern.matches("^[A-Za-z0-9]*.{8,16}$", sPw)) {
            Log.v("plz", "4");
            AlertDialog.Builder alert = new AlertDialog.Builder(Join.this);
            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alert.setMessage("비밀번호를 영문, 숫자, 8~16자로 설정해주세요");
            alert.show();
        }
        else if(!Pattern.matches("^[a-zA-Z0-9]*$", sId)){
            AlertDialog.Builder alert = new AlertDialog.Builder(Join.this);
            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alert.setMessage("아이디를 영문과 숫자를 사용하여 설정해주세요");
            alert.show();
        }
        else if (sPw.equals(sPw_chk) && Pattern.matches("^[A-Za-z0-9]*.{8,16}$", sPw)) {
            //패스워드 확인이 정상적으로 됨
            GetData rdb = new GetData();
            rdb.execute();
            Log.e("plz", "5");


            Button JoinButton = (Button) findViewById(R.id.button02);
            JoinButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v("plz", "6");
                    Intent intent1 = new Intent(Join.this, MainActivity.class);
                    startActivity(intent1);
                }
            });
        }
    }

    private class GetData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... params) {
            String content = executeClient();
            return content;
        }

        public String executeClient() {
            HttpClient client = new DefaultHttpClient();

            String postURL = "http://172.30.1.2/join.php";
            HttpPost httpPost = new HttpPost(postURL); // Post객체 생성

            List<NameValuePair> post = new ArrayList<NameValuePair>();
            post.add(new BasicNameValuePair("ID", sId));
            post.add(new BasicNameValuePair("PW",sPw));
            post.add(new BasicNameValuePair("NAME",sName));

            HttpParams params = client.getParams();
            HttpConnectionParams.setConnectionTimeout(params, 5000);
            HttpConnectionParams.setSoTimeout(params, 5000);

            try {
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(post, "UTF-8");
                httpPost.setEntity(entity);
                HttpResponse responsePOST = client.execute(httpPost);
                HttpEntity resEntity = responsePOST.getEntity();

                return getContentCharSet(entity);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}

