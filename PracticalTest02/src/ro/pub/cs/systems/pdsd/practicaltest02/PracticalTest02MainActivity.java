package ro.pub.cs.systems.pdsd.practicaltest02;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import ro.pub.cs.systems.pdsd.practicaltest02.R;

public class PracticalTest02MainActivity extends Activity {

 public final static String TEMPERATURE = "temperature";
 public final static String WIND_SPEED = "wind_speed";
 public final static String CONDITION = "condition";
 public final static String PRESSURE = "pressure";
 public final static String HUMIDITY = "humidity";
 public final static String SEARCH_KEY = "";
 public final static String WEB_SERVICE_ADDRESS = "http://services.aonaware.com/DictService/DictService.asmx/Define";
 public final static String QUERY_ATTRIBUTE = "query";
 public final static String ALL = "all";
     
	public EditText adresaClient,port,city;
    public Spinner info;
    public EditText portServer;
    public TextView ServerReply;
    public Button buttonClient,buttonServer;
    public ServerThread serverThread;
    public TextView cityEditText;
    public TextView wordResult;
    public TextView wordEditText;
    public TextView wordGet;
    private ConnectButtonClickListener connectButton = new ConnectButtonClickListener();
    private GetWordListener getWord = new GetWordListener();
    
	 public static BufferedReader getReader(Socket socket) throws IOException {
	        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    }

    public static PrintWriter getWriter(Socket socket) throws IOException {
        return new PrintWriter(socket.getOutputStream(), true);
    }
	    
    private class CommunicationThread extends Thread {

        private Socket socket;
        private Random random = new Random();
        private Context ctx;
        private String expectedWordPrefix = new String();

        public CommunicationThread(Context ctx,Socket socket) {
            if (socket != null) {
                this.socket = socket;
                this.ctx = ctx;
                Log.d("Debug", "[SERVER] Created communication thread with: "+socket.getInetAddress());
            }
        }

        @Override
        public void run() {
            if (socket != null) {
                try {
                    BufferedReader bufferedReader = getReader(socket);
                    PrintWriter    printWriter    = getWriter(socket);
                    if (bufferedReader != null && printWriter != null) {
                        Log.d("Debug", "[COMMUNICATION THREAD] Waiting for parameters from client (city / information type)!");
                        String word = bufferedReader.readLine();

                        Log.d("Debug", "[COMMUNICATION THREAD] GetData ");
                        String data = serverThread.getData();
                        Log.d("Debug", "[COMMUNICATION THREAD] I got = "+data);
                        if (word != null && !word.isEmpty() ) {
                           
                            Log.d("Debug", "[COMMUNICATION THREAD] Getting the information from the webservice...");
                            HttpClient httpClient = new DefaultHttpClient();
                            HttpPost httpPost = new HttpPost(WEB_SERVICE_ADDRESS);
                            List<NameValuePair> params = new ArrayList<NameValuePair>();
                            params.add(new BasicNameValuePair("word", word));
                            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
                            httpPost.setEntity(urlEncodedFormEntity);
                            ResponseHandler<String> responseHandler = new BasicResponseHandler();
                            String pageSourceCode = httpClient.execute(httpPost, responseHandler);
                            if (pageSourceCode != null) {
                            			serverThread.setData(pageSourceCode);

                                        printWriter.println(pageSourceCode);
                                        printWriter.flush();
                                   
                            } else {
                                Log.e("Error", "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                            }
                        }
                    } else {
                        Log.e("Error", "[COMMUNICATION THREAD] BufferedReader / PrintWriter are null!");
                    }
                    socket.close();
                } catch (IOException ioException) {
                    Log.e("Error", "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                        ioException.printStackTrace();
                } 
            } else {
                Log.e("Error", "[COMMUNICATION THREAD] Socket is null!");
            }
        }
    }

    public class ServerThread extends Thread {

        String word = new String();
        
        boolean isRunning = true;
        public ServerSocket serverSocket;
        
        public ServerThread (){
        	 Log.e("test","Empty constructor");
        }
        
        public ServerThread(int port) {
            try {

                Log.e("test","Incerc pe "+ port);
                this.serverSocket = new ServerSocket(port);
                Log.e("test","Am deschis socket pe "+ port);
                isRunning = true;
            } catch (IOException ioException) {
                Log.e("error", "An exception has occurred:" + ioException.getMessage());
                    ioException.printStackTrace();
            }
        }
        
        public ServerSocket getServerSocket(){
            return this.serverSocket;
        }
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Log.d("Debug", "[SERVER] Waiting for a connection...");
                    Socket socket = serverSocket.accept();
                    Log.d("Debug", "[SERVER] A connection request was received from " + socket.getInetAddress() + ":" + socket.getLocalPort());
                    CommunicationThread communicationThread = new CommunicationThread(getApplicationContext(), socket);
                    communicationThread.start();
                }
            }  catch (IOException ioException) {
                Log.e("error", "An exception has occurred: " + ioException.getMessage());
                    ioException.printStackTrace();
            }
        }

        public synchronized void setData(String wordToSearch) {
            //this.data.put(city, weatherForecastInformation);
        	this.word = wordToSearch;
        }

        public synchronized String getData() {
            return this.word;
        }

        public void stopThread() {
            if (serverSocket != null) {
                interrupt();
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (IOException ioException) {
                    Log.e("Error", "An exception has occurred: " + ioException.getMessage());
                        ioException.printStackTrace();
                }
            }
        }
    }

    private class ClientThread extends Thread {

        private Socket socket;
        private String address,city,info;
        private int port;
        private String word;
        private String result;
        
        public ClientThread(String clientAddress,int clientPort, String word){
            this.address = clientAddress;
            this.port = clientPort;
            this.word = word;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(address, port);
                if (socket == null) {
                    Log.e("Error", "[CLIENT THREAD] Could not create socket!");
                    return;
                }
                BufferedReader bufferedReader = getReader(socket);
                PrintWriter    printWriter    = getWriter(socket);
                if (bufferedReader != null && printWriter != null) {
                    printWriter.println(word);
                    printWriter.flush();
                    String wordInfo;
                    while ((wordInfo = bufferedReader.readLine()) != null) {
                    	Log.e("Result client", "Result " + wordInfo);
                        final String finalizedWeatherInformation = wordInfo;
                        wordEditText.post(new Runnable() {
                            @Override
                            public void run() {
                            	wordEditText.append(finalizedWeatherInformation + "\n");
                            }
                        });
                    }
                } else {
                    Log.e("Error", "[CLIENT THREAD] BufferedReader / PrintWriter are null!");
                }
                socket.close();
            } catch (IOException ioException) {
                Log.e("Error", "[CLIENT THREAD] An exception has occurred: " + ioException.getMessage());
                    ioException.printStackTrace();

            }
        }
    }

   
    
    private class ConnectButtonClickListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            String serverPort = portServer.getText().toString();
            if (serverPort == null || serverPort.isEmpty()) {
                Toast.makeText(
                        getApplicationContext(),
                        "Server port should be filled!",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            serverThread = new ServerThread(Integer.parseInt(serverPort));
            Log.e("test", "portul "+Integer.parseInt(serverPort));
            if (serverThread.getServerSocket() != null) {
                serverThread.start();
            } else {
                Log.e("error", "[MAIN ACTIVITY] Could not creat server thread!");
            }
        }
    }


    private class GetWordListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            String clientAddress = adresaClient.getText().toString();
            String clientPort    = port.getText().toString();
            if (clientAddress == null || clientAddress.isEmpty() ||
                    clientPort == null || clientPort.isEmpty()) {
                Toast.makeText(
                        getApplicationContext(),
                        "Client connection parameters should be filled!",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            if (serverThread == null || !serverThread.isAlive()) {
                Log.e("Error", "[MAIN ACTIVITY] There is no server to connect to!");
                return;
            }
            String word = wordGet.getText().toString();
            if (word == null || word.isEmpty() ) {
                Toast.makeText(
                        getApplicationContext(),
                        "Parameters from client --word-- should be filled!",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            wordEditText.setText("");
            ClientThread clientThread = new ClientThread(
                    clientAddress,
                    Integer.parseInt(clientPort),
                    word);
            clientThread.start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practical_test02_main);

        portServer = (EditText)findViewById(R.id.server_port_edit_text);
        adresaClient = (EditText) findViewById(R.id.client_address_edit_text);
        port = (EditText) findViewById(R.id.client_port_edit_text);
        wordGet = (EditText) findViewById(R.id.word_edit_text);
        wordEditText = (TextView) findViewById(R.id.word_edit_text);

        buttonServer = (Button) findViewById(R.id.connect_button);
        buttonServer.setOnClickListener(connectButton);

        buttonClient = (Button) findViewById(R.id.get_word_def_button);
        buttonClient.setOnClickListener(getWord);
    }
    @Override
    protected void onDestroy() {
        if (serverThread != null) {
            serverThread.stopThread();
        }
        super.onDestroy();
    }
}
