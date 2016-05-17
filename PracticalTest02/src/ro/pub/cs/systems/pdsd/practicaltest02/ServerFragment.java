package ro.pub.cs.systems.pdsd.practicaltest02;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import ro.pub.cs.systems.pdsd.practicaltest02.R;

public class ServerFragment extends Fragment {

    private EditText serverTextEditText;

    private ServerTextContentWatcher serverTextContentWatcher = new ServerTextContentWatcher();
    private class ServerTextContentWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }

    }

    private class CommunicationThread extends Thread {

        private Socket socket;

        public CommunicationThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
          
        }
    }

    private ServerThread serverThread;
    private class ServerThread extends Thread {

        private boolean isRunning;

        private ServerSocket serverSocket;

        public void startServer() {
            isRunning = true;
            start();
            
        }

        public void stopServer() {
            isRunning = false;
            try {
                serverSocket.close();
            } catch(IOException ioException) {
               
            }
            //Log.v(Constants.TAG, "stopServer() method invoked");
        }

        @Override
        public void run() {
            try {
                //serverSocket = new ServerSocket(Constants.SERVER_PORT);
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    if (socket != null) {
                        CommunicationThread communicationThread = new CommunicationThread(socket);
                        communicationThread.start();
                    }
                }
            } catch (IOException ioException) {
               
            }
        }
    }

   // @Override
   // public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
     //   return inflater.inflate(R.layout.fragment_server, parent, false);
    //}

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);

        serverTextEditText = (EditText)getActivity().findViewById(R.id.server_port_edit_text);
        serverTextEditText.addTextChangedListener(serverTextContentWatcher);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serverThread != null) {
            serverThread.stopServer();
        }
    }

}