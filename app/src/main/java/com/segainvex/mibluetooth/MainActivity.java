package com.segainvex.mibluetooth;

import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
{
    private static String miMAC = "98:D3:41:F5:AC:C0";
    EditText respuestaBase;
    EditText comandoBase;
    BluetoothAdapter miBluetoothAdapter =null;
    BluetoothSocket miSocket = null;
    BluetoothDevice miDevice;
    final int REQUEST_ENABLE_BT=1;
    Thread workerThread;
    private ConnectedThread miThread;
    Handler miHandler;//Para administrar los datos recibidos en un handler
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //Lista de constantes para identificar lo que retorna el thread secundario
    private interface MessageConstants
    {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
        // ... (Add other message types here as needed.)
    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        comandoBase = (EditText) findViewById(R.id.comando);
        respuestaBase = (EditText) findViewById(R.id.respuesta);
        //Send Button
        Button sendButton = (Button) findViewById(R.id.send);
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v){
            String msg = comandoBase.getText().toString();
            msg += "\r";
            miThread.write(msg.getBytes());
        }
        });
        //Obtenemos el Bluetooth adapter
        miBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(miBluetoothAdapter == null){finish();}//Si no hay Bluetooth salimos

        // Habilitación del Bluetooth
        if(!miBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth,REQUEST_ENABLE_BT);
            //El resultado del intent implicito se gestiona en onActivityResult
        }

        //Definición del handler para administrar los datos recibidos
        miHandler = new Handler()
        {
            public void handleMessage(android.os.Message msg)
            {
                if (msg.what == MessageConstants.MESSAGE_READ)
                {
                    String mensajeRecibido = (String) msg.obj;
                    respuestaBase.setText(mensajeRecibido);
                }
            }
        };
    }//onCreate
    /**********************************************************************
     * Métodos de ActivityMain
     * ********************************************************************/
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode,resultCode,data);
            //Si no se habilita el Bluetooth por el usuario salimos
            if (requestCode == REQUEST_ENABLE_BT) {
                if (resultCode != RESULT_OK) {finish();}
            }
        }//onActivityResult

    /**********************************************************************
     * onResume. Aquí conectamos el bluetooth y
     * arrancamos el thred que administra el tráfico de datos
    ************************************************************************/
    @Override
    public void onResume()
    {
        super.onResume();
        //Hacemos una lista con los dispositivos vinculados
        Set<BluetoothDevice> bondedDevices = miBluetoothAdapter.getBondedDevices();
        //Si hay algún dispositivo vinculado....
        if(bondedDevices.size() > 0)
        {
            boolean deviceFound=false;
            for(BluetoothDevice device : bondedDevices)//buscamos el nuestro por la MAC o nombre
            {
                //if(device.getName().equals("nombre dispositivo”))
                if(device.getAddress().equals(miMAC))// ejemplo de MAC
                {
                    deviceFound=true;
                    miDevice = device;
                    break;//Sale del bucle for
                }
            }
            if(!deviceFound) finish();//Si no está nuestro dispositivo entre los vinculados salimos
        }
        else finish();//Si no hay dispositivos vinculados con los que conectarse salimos
       // Cancela cualquier  discovery en proceso
        miBluetoothAdapter.cancelDiscovery();//Cancel cualquier busqueda en proceso
        //Ahora hay que conectar
        try {//Como sin bluetooth no podemos seguir no hace falta un tread secundario
            miSocket = miDevice.createRfcommSocketToServiceRecord(BTMODULEUUID);//Creamos el socket
        } catch (IOException e) {e.printStackTrace();}
        try {
            miSocket.connect();//Conectamos el socket. Esto puede llevar un tiempo
        } catch (IOException e) {e.printStackTrace();}
        // TODO si no conecta deberíamos salir ¿?
        miThread = new ConnectedThread(miSocket);//Tread para manejar el Bluetooth
        miThread.start();//Ejecuta el thread para adminostrar la conexión
    }
    /************************************************************************
     * onResume. Aquí cerramos la conexión
     ************************************************************************/
    public void onPause()
    {
        super.onPause();
       // Cuando se sale de la aplicación esta parte permite que no se deje abierto el socket
        miThread.cancel();//miSocket.close();
    }
    /************************************************************************
     * class que extends Thread para administrar la conexión en subproceso
     * En el bucle run se leen datos y se programan métodos para escribir
     *  y para cerrar la conexión
     ************************************************************************/
    private class ConnectedThread extends Thread
     {
             private static final String TAG = "MY_APP_DEBUG_TAG";
             private final BluetoothSocket mmSocket;//Para poder cerrarlo
             // streams de entrada y salida
             private final InputStream miInStream;
             private final OutputStream miOutStream;
            //Constructor
             public ConnectedThread(BluetoothSocket socket)
             {
                 mmSocket = socket;
                 InputStream tmpIn = null;
                 OutputStream tmpOut = null;
                 // Get the input and output streams; using temp objects because
                 // member streams are final.
                 try {
                     tmpIn = socket.getInputStream();
                 } catch (IOException e) {
                     Log.e(TAG, "Error occurred when creating input stream", e);
                 }
                 try {
                     tmpOut = socket.getOutputStream();
                 } catch (IOException e) {
                     Log.e(TAG, "Error occurred when creating output stream", e);
                 }
                  miInStream = tmpIn;
                 miOutStream = tmpOut;
             }//Fin Constructor
             public void run()
             {
                 final byte delimiter = 10;
                 int readBufferPosition = 0;
                 byte[] readBuffer = new byte[1024];
                 while (true)
                 {
                     try
                     {
                         int bytesAvailable = miInStream.available();
                         //Si hay datos..
                         if (bytesAvailable > 0)
                         {
                             byte[] packetBytes = new byte[bytesAvailable];
                             miInStream.read(packetBytes);//los lee en packetBytes
                             //Recorre el array de bytes leido
                             for(int i=0;i<bytesAvailable;i++)
                             {
                                 byte b = packetBytes[i];//Lee un byte
                                 if(b == delimiter) //Si es el delimitador lo añade a readBuffer y sale
                                 {
                                     byte[] encodedBytes = new byte[readBufferPosition];
                                     System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                     //Crea el string data con el comando
                                     final String data = new String(encodedBytes, "US-ASCII");
                                     int longData = readBufferPosition;
                                     readBufferPosition = 0;
                                     // Send the obtained bytes to the UI Activity via handler
                                     miHandler.obtainMessage( MessageConstants.MESSAGE_READ, longData, -1, data).sendToTarget();
                                 }//if(b == delimiter)
                                 else //Si no es el delimitador simplemente añade el byte al buffer de lectura
                                 {
                                     readBuffer[readBufferPosition++] = b;
                                 }
                             }//for(int i=0;i<bytesAvailable;i++)
                         }//if (bytesAvailable > 0)
                     }
                     catch (IOException ex){break;}
                 }//While(true)
             }//run
          // Call this from the main activity to send data to the remote device.
            public void write(byte[] bytes)
            {
                try {
                    miOutStream.write(bytes);

                    // Share the sent message with the UI activity.
                    Message writtenMsg = miHandler.obtainMessage(
                            MessageConstants.MESSAGE_WRITE, -1, -1, bytes);
                    writtenMsg.sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when sending data", e);

                    // Send a failure message back to the activity.
                    Message writeErrorMsg =
                            miHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                    Bundle bundle = new Bundle();
                    bundle.putString("toast","Couldn't send data to the other device");
                    writeErrorMsg.setData(bundle);
                    miHandler.sendMessage(writeErrorMsg);
                }
            }//Write
            // Call this method from the main activity to shut down the connection.
            public void cancel()
            {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close the connect socket", e);
                }
            }
        }
}