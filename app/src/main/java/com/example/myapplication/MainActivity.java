package com.example.myapplication;

import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends Activity {

    // guarda url do serviço de conversao de temperatura - rede local
    private static final String SERVICE_URL = "http://192.168.1.100:8080/MeuWebServer2_war_exploded/rest/controller";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // método do onclick do botão "converter para fahrenheit"
    public void convert (View vw) {
        // faz um post contendo o número que o usuário colocou no input
        postData(vw);
        // faz um get do número convertido pelo servidor
        getData(vw);
    }

    public void getData(View vw) {
        // define uma thread separada para rodar a operação, impedindo que o app trave enquanto a realiza
        WebServiceTask wstask = new WebServiceTask(WebServiceTask.GET, this);
        // define o que vai ser executado pela thread
        wstask.execute(new String[] {SERVICE_URL + "/convert_temperature"});
    }

    public void postData(View vw) {
        // captura o numero que foi colocado no input da interface
        EditText editTemperature = (EditText) findViewById(R.id.edTemperature);
        // salva número em string
        String temperature = editTemperature.getText().toString();

        // instancia uma thread para fazer o post
        WebServiceTask wstask = new WebServiceTask(WebServiceTask.POST, this);
        // cria um par key-value do JSON
        wstask.addPair("value", temperature);
        // passa a URL
        wstask.execute(new String[] {SERVICE_URL});
    }

    // lida com a response devolvida pelo serviço, um JSON em formato de string
    public void manageResponse(String response) {
        EditText editTemperature = (EditText) findViewById(R.id.edTemperature);

        editTemperature.setText("");

        // tenta usar o JSON recebido do serviço - transformar string em json apra extrair o valor convertido
        try {
            // cria um JSONObject
            JSONObject jo = new JSONObject(response);
            // armazena a temperatura convertida a ser extraida do JSON, na key "value"
            String temperature = jo.getString("value");
            // coloca na interface do app
            editTemperature.setText(temperature);
        } catch (Exception e) {
            Log.e("when managing response", response);
        }

    }

    // thread destinada para a conversa aplicativo-servidor
    // AsyncTask recebe tres parametros: o tipo de dado recebido, o que vai ser processado e o resultado do processo
    private class WebServiceTask extends AsyncTask<String, Integer, String> {

        // criando variáveis que serão utilizadas
        public static final int POST = 1;
        public static final int GET = 2;
        private int taskType;

        private Context context = null;

        private ArrayList<NameValuePair> pair = new ArrayList<NameValuePair>();

        // elemento de interface pop-up que aparece no app enquanto a conversa com o servidor está sendo feita
        private ProgressDialog popWait = null;

        // construtor
        public WebServiceTask(int taskType, Context context) {
            this.taskType = taskType;
            this.context = context;
        }

        // método adiciona key-value no arrayList
        public void addPair (String name, String value) {

            pair.add(new BasicNameValuePair(name, value));
        }

        // sobrescreve métodos de asynctask
        // o que vai acontecer antes de executar a computação - pop-up
        @Override
        protected void onPreExecute() {
            //instancia o pop-up
            popWait = new ProgressDialog(context);
            popWait.setMessage("Aguarde...");
            //define estilo do pop-up
            popWait.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            //define q não da para cancelar o pop-up
            popWait.setCancelable(false);
            popWait.show();

        }

        // o que vai acontecer depois de executar a computação - chama o manage response e fecha pop-up
        @Override
        protected void onPostExecute(String result) {
            manageResponse(result);
            popWait.dismiss();
        }

        // o que vai ser computado na thread
        @Override
        // tres pontinhos indicam que um ou mais
        // parametros desse tipo podem ser recbidos pelo método
        protected String doInBackground (String... urls) {

            // define strings que serão necessárias
            String url = urls[0];
            String result = "";

            // cria http response e http client
            HttpResponse response = null;
            HttpClient httpClient = new DefaultHttpClient(getHttpParameters());

            try {
                switch (taskType) {
                    case POST:
                        // cria elemento httpPost
                        HttpPost httpPost = new HttpPost(url);

                        httpPost.setEntity(new UrlEncodedFormEntity(pair));
                        response = httpClient.execute(httpPost);
                        break;
                    case GET:
                        // cria elemento httpGet
                        HttpGet httpGet = new HttpGet(url);

                        response = httpClient.execute(httpGet);
                        break;
                }
            } catch (Exception e) {
                Log.e("WebServiceTask", e.getLocalizedMessage());
            }

            if (response != null) {
                try {
                    result = convertInputToString(response.getEntity().getContent());
                } catch (IOException e) {
                    Log.e("WebServiceTask", e.getLocalizedMessage());
                }
            }

            return result;
        }

        // Configurar parâmetros
        private HttpParams getHttpParameters() {
            // recebe conjunto de parâmetros
            HttpParams parameters = new BasicHttpParams();

            HttpConnectionParams.setConnectionTimeout(parameters, 5000);
            HttpConnectionParams.setSoTimeout(parameters, 5000);

            return parameters;
        }

        // transforma inputStream recebido em string manipulável
        private String convertInputToString(InputStream input) {
            String line = "";
            StringBuilder fullString = new StringBuilder();

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            // tenta ler a linha
            try {
                while ((line = reader.readLine()) != null) {

                    fullString.append(line);

                }
            } catch (IOException e) {
                Log.e("WebServiceTask", e.getLocalizedMessage());
            }
            return fullString.toString();
        }
    }

}