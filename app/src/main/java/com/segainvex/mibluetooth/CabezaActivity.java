package com.segainvex.mibluetooth;

import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class CabezaActivity extends AppCompatActivity {

    //private static String miMAC = "98:D3:41:F5:AC:C0";
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

    /********************************************************
     *          onCreate
     * *******************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cabeza);
        //Componentes de la pantalla
        comandoBase = (EditText) findViewById(R.id.comando);
        respuestaBase = (EditText) findViewById(R.id.respuesta);
        //Send Button
        Button sendButton = (Button) findViewById(R.id.subir);
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
        /*****************************************************************
         * Gestión del mensaje recibido
         * Handler para recibir mensajes del thread del bluetooth
         *****************************************************************/
        miHandler = new Handler()
        {
            public void handleMessage(android.os.Message msg)
            {
                switch (msg.what)
                {
                    case Global.TipoRespuesta.FOTODIODO:
                        respuestaBase.setText((String) msg.obj);
                        //fotodiodo(msg.obj); //Gestiona los datos del fotodiodo
                    break;
                    case Global.TipoRespuesta.LO_ENVIADO:
                        //comandoEnviado.setText((String) msg.obj);
                    break;
                    case Global.TipoRespuesta.TEMPERATURA_HUMEDAD:
                        respuestaBase.setText((String) msg.obj);
                        //temperaturaHumedad();
                    break;
                    case Global.TipoRespuesta.PASOS:
                        respuestaBase.setText((String) msg.obj);
                        //pasosPorDar();
                    break;
                    case Global.TipoRespuesta.SIN_FIRMA:
                        respuestaBase.setText((String) msg.obj);
                        //respuestaBase();
                    break;
                    case Global.TipoRespuesta.VARIABLES:
                        respuestaBase.setText((String) msg.obj);
                        //variablesEstado();
                    break;
                    case Global.TipoRespuesta.CONTADOR:
                        respuestaBase.setText((String) msg.obj);
                        //contadorBase();
                    break;
                    case Global.TipoRespuesta.STOP:
                        respuestaBase.setText((String) msg.obj);
                        //stopRecibido();
                    break;
                }
            }
        };
    }//onCreate
    /**********************************************************************
     * Métodos del ciclo de vida de la activity
     * ********************************************************************/
     /*********************************************************************
     * onResume.
      * Aquí conectamos el bluetooth y  arrancamos el thred que
      * administra el tráfico de datos
     *********************************************************************/
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
                if(device.getAddress().equals(Global.miMAC))// ejemplo de MAC
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
        miThread = new ConnectedThread(miSocket,this,miHandler);//Tread para manejar el Bluetooth
        miThread.start();//Ejecuta el thread para adminostrar la conexión
    }
    /************************************************************************
     * onResume. Aquí cerramos la conexión
     ************************************************************************/
    public void onPause()
    {
        super.onPause();
        // Cuando se sale de la aplicación esta parte permite que no se deje abierto el socket
        miThread.desconectaBluetooth();//miSocket.close();
    }
    /************************************************************************
     * Fin de métodos del ciclo de vida de ActivityMain2
     ************************************************************************/
/****************************************************************************
 ****************************************************************************/
}
/* TODOS LOS MENSAJES POSIBLES
switch (msg.what)
                {
                    case Global.TipoRespuesta.FOTODIODO:
                        respuestaBase.setText((String) msg.obj);
                        //fotodiodo(msg.obj); //Gestiona los datos del fotodiodo
                    break;
                    case Global.TipoRespuesta.ACELEROMETRO:
                        respuestaBase.setText((String) msg.obj);
                        //Acelerometro(msg.obj)
                    break;
                    case Global.TipoRespuesta.LO_ENVIADO:
                        //comandoEnviado.setText((String) msg.obj);
                    break;
                    case Global.TipoRespuesta.TEMPERATURA_HUMEDAD:
                        respuestaBase.setText((String) msg.obj);
                        //temperaturaHumedad();
                    break;
                    case Global.TipoRespuesta.PASOS:
                        respuestaBase.setText((String) msg.obj);
                        //pasosPorDar();
                    break;
                    case Global.TipoRespuesta.SIN_FIRMA:
                        respuestaBase.setText((String) msg.obj);
                        //respuestaBase();
                    break;
                    case Global.TipoRespuesta.VARIABLES:
                        respuestaBase.setText((String) msg.obj);
                        //variablesEstado();
                    break;
                    case Global.TipoRespuesta.CONTADOR:
                        respuestaBase.setText((String) msg.obj);
                        //contadorBase();
                    break;
                    case Global.TipoRespuesta.MARCHA_PARO:
                        respuestaBase.setText((String) msg.obj);
                        //estadoMarchaParo();
                    break;
                    case Global.TipoRespuesta.SENTIDO:
                        respuestaBase.setText((String) msg.obj);
                        //sentidoBase();
                    break;
                    case Global.TipoRespuesta.MOTOR_ACTIVO:
                        respuestaBase.setText((String) msg.obj);
                        //motorActivo();
                    break;
                    case Global.TipoRespuesta.FRECUENCIA:
                        respuestaBase.setText((String) msg.obj);
                        //frecuencia();
                    break;
                    case Global.TipoRespuesta.RESOLUCION:
                        respuestaBase.setText((String) msg.obj);
                        //resolucion();
                    break;
                    case Global.TipoRespuesta.ESTADO:
                        respuestaBase.setText((String) msg.obj);
                        //estadoBase();
                    break;
                    case Global.TipoRespuesta.STOP:
                        respuestaBase.setText((String) msg.obj);
                        //stopRecibido();
                    break;
                    case Global.TipoRespuesta.ONDA:
                        respuestaBase.setText((String) msg.obj);
                        //onda();
                    break;
                }
 */