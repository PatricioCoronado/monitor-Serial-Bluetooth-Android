package com.segainvex.mibluetooth;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.os.Vibrator;
import android.widget.Toast;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
public class BaseActivity extends AppCompatActivity
{
    private static final String TAG = "DispositivosVinculados";
    SharedPreferences preferencias;
    LinearLayout graficoZ;
    Grafico grafico;
    private Vibrator vibrator;
    //Componentes gráficos
    private TextView respuesta;
    private TextView GX;//Para mostrar las coordenadas del acelerómetro
    private TextView GY;
    private TextView comando;
    private Button subir, bajar, parar;
    private SeekBar velocidad;
    private int velocidadMotor;
    private Switch z1;
    private Switch z2;
    private Switch z3;
    private int motorActivo;
    private boolean  movimientoDiscreto;
    private int pasosMovimientoDiscreto;
    //Componentes Bluetooth
    BluetoothDevice miDevice = null;//Bluetooth device que representa el dispositivo remoto
    BluetoothSocket miSocket = null;//Para enchufar el Bluetooth device a la radio Bluetooth
    BluetoothAdapter miBluetoothAdapter =null;//La radio Bluetooth
    private ConnectedThread miThread;//Hilo que lee y escribe la entrada del bluetooth
    Handler miHandler;//Para administrar los datos recibidos en un handler
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    /************************************************************
     * onActivityResult
     ************************************************************/
/*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);
        //Si no se habilita el Bluetooth por el usuario salimos

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) //Si no se activa el bluetooth salimos
            {
                Toast.makeText(this, "No se ha activado el Bluetooth", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }//onActivityResult
*/
    /************************************************************
     *          onCreate
    * ***********************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        comando = (TextView) findViewById(R.id.comando);
        respuesta = (TextView) findViewById(R.id.respuesta);
        GX= (TextView) findViewById(R.id.textViewX);
        GY= (TextView) findViewById(R.id.textViewY);
        //enviar = (Button) findViewById(R.id.enviar);
        subir = (Button) findViewById(R.id.subir);
        parar = (Button) findViewById(R.id.parar);
        bajar = (Button) findViewById(R.id.bajar);
        z1 = (Switch) findViewById(R.id.swZ1);
        z2 = (Switch) findViewById(R.id.swZ2);
        z3 = (Switch) findViewById(R.id.swZ3);
        motorActivo=7;
        //Velocidad seekbar y métodos asociados
        velocidadMotor=Global.VELOCIDAD_INICIAL;//El seekBar debe estar en esa posición
        velocidad = (SeekBar) findViewById(R.id.velocidad);
        velocidad.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                progress+=1;//Para que con 0 sea 10, con 10, 20...y con 50, 60
                velocidadMotor = progress*10;
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar){}
        });
        //Preferencias
        preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        //Inicializa el gráfico y pinta el fondo
        graficoZ = (LinearLayout) findViewById(R.id.grafico_z);
        grafico = new Grafico(this);
        graficoZ.addView(grafico);
        /*****************************************************************
         * Gestión del mensaje recibido
         * Handler para recibir mensajes del thread del bluetooth
        **************************************************************** */
        miHandler = new Handler()
        {
            public void handleMessage(android.os.Message msg)
            {
                String strRespuesta = "";//Para alojar la respuesta en un string
                byte[] bytesRespuesta = (byte[]) msg.obj;//bytesRespuesta es un array de bits con la respuesta de Arduino
                if (msg.what==Global.TipoRespuesta.SIN_FIRMA) {//Si es una respuesta sin firma solo la muestra y sale
                    try {
                        strRespuesta = new String(bytesRespuesta, "US-ASCII");
                        respuesta.setText(strRespuesta);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    return;//Muestra la respuesta sin firma y sale
                }//if
                //Si la respuesta es con firma la procesa
                int longResp = bytesRespuesta.length;//Longitud de la respuesta
                //Quitamos la firma de 2 caracteres a la respuesta "respuestaTrim"
                byte[] respuestaTrim = Arrays.copyOfRange(bytesRespuesta, 3, longResp);
                int longRespuestaTrim = longResp-3;//Longitud de la respuesta trimada
                strRespuesta = new String(respuestaTrim);//Muestra la respuesta sin firma
                respuesta.setText(strRespuesta);
                //Procesa la cadena recibida en función de la firma leida
                switch (msg.what)//Posibles respuestas
                {
                    case Global.TipoRespuesta.ACELEROMETRO:
                        acelerometro(respuestaTrim,longRespuestaTrim,GX,GY);
                        break;
                    case Global.TipoRespuesta.LO_ENVIADO:
                        //comandoEnviado.setText((String) msg.obj);
                        break;
                    case Global.TipoRespuesta.TEMPERATURA_HUMEDAD:
                        //respuestaBase.setText((String) msg.obj);
                        //temperaturaHumedad();
                        break;
                    case Global.TipoRespuesta.PASOS:
                        //respuestaBase.setText((String) msg.obj);
                        //pasosPorDar();
                        break;
                    case Global.TipoRespuesta.SIN_FIRMA:
                        respuesta.setText(strRespuesta);
                        break;
                    case Global.TipoRespuesta.VARIABLES:
                        //respuestaBase.setText((String) msg.obj);
                        //variablesEstado();
                        break;
                    case Global.TipoRespuesta.VERSION:
                        longResp=bytesRespuesta.length;
                        if(longResp>Global.MAX_LON_STRING) return;//Evitamos cadenas muy largas
                        //Quitamos la firma
                        respuestaTrim = Arrays.copyOfRange(bytesRespuesta, 3, longResp-1);
                        strRespuesta = new String(respuestaTrim);
                        respuesta.setText(strRespuesta);
                        //respuesta.variablesEstadoBase();
                        break;
                }
            }
        };
    }//onCreate
    /**********************************************************************
     *                        Inflado del menú
     * *******************************************************************/
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_base, menu);
        return true; /** true -> el menú ya está visible */
    }
    /**********************************************************************
     * Gestión de items del menú
     **********************************************************************/
    @Override public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.error) {pideError(null); return true;}
        if (id == R.id.cambio) {cambio(null); return true;}
        if (id == R.id.version) {version(null);return true;}
        if (id == R.id.acelerometro) {acelerometro(null);return true;}
        if (id == R.id.preferencias){lanzarPreferencias();return true;}
        if (id == R.id.busca_bluetooth){cambiaDeviceBluetooth(Global.NUEVO_BLUETOOTH);return true;}
        return super.onOptionsItemSelected(item);
    }

    /**********************************************************************
     * Métodos de ActivityMain
    **********************************************************************/

    /*********************************************************************
     * Procesa la respuesta del acelerómetro
     * *******************************************************************/
    public void acelerometro(byte[] respuesta, int largoRespuesta, TextView GX, TextView GY)
    {
        for(int indice=0;indice < largoRespuesta-1;indice++)//Recorre el array de la respuesta...
        {
            if(respuesta[indice]==32)//Buscando el caracter espacio
            {   //Cuando lo encuentra ya puede leer el valor de x
                String gx=new String(Arrays.copyOfRange(respuesta, 0, indice));
                String gy=new String(Arrays.copyOfRange(respuesta, indice+1, largoRespuesta-1));
                GY.setText("y= "+gy);
                GX.setText("x= "+gx);
                float x0 = Float.parseFloat(gx);
                float y0 = Float.parseFloat(gy);
                grafico.punto(x0,y0);
                graficoZ.removeView(grafico);//Borra el punto anterior (borra el gráfico completo)
                graficoZ.addView(grafico); //Pinta el gráfico con el nuevo punto
                break;//Salimos del for
            }
        }
    }
    /**********************************************************************
     * onResume. Aquí conectamos el bluetooth y
     * arrancamos el thred que administra el tráfico de datos
    **********************************************************************/
    @Override
    public void onResume()
    {
        super.onResume();
        //Aquí se llega con un device de los que están vinculados desde la BluetoothActivity
        //O desde onPause
        VerificarEstadoBT();//Si desde onPause se ha desactivado el bluetooth hay que verificarlo
        miDevice=Global.deviceBase;


        // Cancela cualquier  discovery en proceso
        miBluetoothAdapter.cancelDiscovery();//Cancel cualquier busqueda en proceso
        //Ahora hay que conectar
        try {//Como sin bluetooth no podemos seguir no hace falta un tread secundario
            miSocket = miDevice.createRfcommSocketToServiceRecord(BTMODULEUUID);//Creamos el socket
        } catch (IOException e) {e.printStackTrace();}
        try {
            miSocket.connect();//Conectamos el socket. Esto puede llevar un tiempo
        } catch (IOException e)
        {
            e.printStackTrace();
            cambiaDeviceBluetooth(Global.FALLO_CONEXION);//Resetea las preferencias y solicita un nuevo dispositivo
        }
        miThread = new ConnectedThread(miSocket,this,miHandler);//Tread para manejar el Bluetooth
        miThread.start();//Ejecuta el thread para administrar la conexión
    }
    /*********************************************************************
     * onPause. Aquí cerramos la conexión
     *********************************************************************/
    public void onPause()
    {
        super.onPause();
       // Cuando se sale de la aplicación esta parte permite que no se deje abierto el socket
        miThread.desconectaBluetooth();//Desenchufa el bluetooth
    }
    /*********************************************************************
    * MÉTODOS DE LA APLICACION
    **********************************************************************/

    /*********************************************************************
     * Botón enviar: Envía un comando a la base
    *********************************************************************/
   /*
    public void enviar(View view)
    {
        String msg = comando.getText().toString();
        //Si no termina con retorno de carro lo pone
        int posicion = msg.indexOf("\r");
        if (posicion ==-1) msg += "\r";
        miThread.write(msg.getBytes());
        vibrator.vibrate(Global.TIEMPO_VIBRACION);
    }
    */

    /*********************************************************************
     * Botón parar: Envía a la base el comando "MOT:MP 0"
     *********************************************************************/
    public void parar(View view)
    {
        String comand = "MOT:MP 0\r";
        comando.setText(comand);
        miThread.write(comand.getBytes());
        vibrator.vibrate(Global.TIEMPO_VIBRACION);
    }
    /*********************************************************************
     * Botón subir y bajar: Envía a la base el comando..
     * "MOT:MMP <MotorActivo Resolucion Frecuencia Sentido Pasos>"
     *********************************************************************/
    public void mover(View view)
    {
            int sentido=0;
            //El  sentido depende del botón pulsado
            if(view.equals(bajar)) sentido=0; else sentido = 1;
            //Busca el motor activo
            motorActivo = motorActivo();//Lee el motor seleccionado
            if(motorActivo==0) {
                Toast.makeText(this, "no hay motor seleccionado", Toast.LENGTH_SHORT).show();
                return;
            }
            //Preferencias. Tipo de movimiento y pasos discretos
            movimientoDiscreto = preferencias.getBoolean("movimiento",false);
            String comand;
            if(movimientoDiscreto)
            {
                String pasosDiscretos =  preferencias.getString("pasos","10000");
                pasosMovimientoDiscreto = Integer.parseInt(pasosDiscretos);
                comand = "MOT:MMP " + motorActivo + " 256 " + velocidadMotor + " "+ sentido +" " + pasosMovimientoDiscreto +" \r";
            }
            else //Movimiento continuo
            {
                comand = "MOT:MM " + motorActivo + " 256 " + velocidadMotor + " "+ sentido +"\r";
            }
            comando.setText(comand);
            miThread.write(comand.getBytes());
            vibrator.vibrate(Global.TIEMPO_VIBRACION);
    }
    /********************************************************************
     * Método para seleccionar el motor activo a partir de los switches
    ******************************************************************** */
    private int motorActivo()
    {
        //Lee el estado de los switeches
        Boolean z1On =  z1.isChecked();
        Boolean z2On =  z2.isChecked();
        Boolean z3On =  z3.isChecked();
        //Analiza el estado y devuelve el motor activo
        if(z1On && z2On && z3On) return 7;
        if(z1On && !z2On && !z3On) return 1;
        if(!z1On && z2On && !z3On) return 2;
        if(!z1On && !z2On && z3On) return 3;
        if(z1On && z2On && !z3On) return 4;
        if(z1On && !z2On && z3On) return 5;
        if(!z1On && z2On && z3On) return 6;
        if(!z1On && !z2On && !z3On) return 0;
        return 0;
    }
    /*********************************************************************
     * Botón cabeza: Cambia a la activity de la cabeza
     *********************************************************************/
    /*
    //Botón para cambiar a la activity de la cabeza
    public void cambio (View view)
    {
        Intent intentCabeza =  new Intent(this, CabezaActivity.class);
        startActivity(intentCabeza);
    }
    */
    /*********************************************************************
     *                  Métodos del menú
    ******************************************************************* */

    /*********************************************************************
     * Item del menú preferencias: carga el fragment de preferencias
     *********************************************************************/
    public void lanzarPreferencias()
    {
        Intent intentPreferencias;
        intentPreferencias = new Intent(this, PreferenciasActivity.class);
        startActivity(intentPreferencias);
    }
    /*********************************************************************
     * Item del menú: Cambio entre activity de control de base o cabeza
     *********************************************************************/
    public void cambio (View view)
    {
        Intent intentCabeza =  new Intent(this, CabezaActivity.class);
        startActivity(intentCabeza);
    }
    /*********************************************************************
     * Item del menú error: Pide error a la base
     *********************************************************************/
    public void pideError(View view)
    {
        String comand =new String("ERR?\r");
        comando.setText(comand);
        miThread.write(comand.getBytes());
        vibrator.vibrate(Global.TIEMPO_VIBRACION);
    }
    /*********************************************************************
     * Item del menú version: Pide la versión a la base
     *********************************************************************/
    public void version(View view)
    {
        String comand =new String("MOT:VER?\r");
        comando.setText(comand);
        miThread.write(comand.getBytes());
        vibrator.vibrate(Global.TIEMPO_VIBRACION);
    }
    /*********************************************************************
     * Item del menú acelerometro: Pide las coordendas del acelerómetro
     *********************************************************************/
    private void acelerometro(View view)
    {
        String muestrasString = preferencias.getString("muestrasAcelerometro","100");
        int muestrasAcelerometro = Integer.parseInt(muestrasString);
        String comand =new String("MOT:IAC "+ muestrasAcelerometro +" \r");
        comando.setText(comand);
        miThread.write(comand.getBytes());
        vibrator.vibrate(Global.TIEMPO_VIBRACION);
    }
    /*********************************************************************
     * Item del menú busca_bluetooth
     ********************************************************************/
    private void cambiaDeviceBluetooth(int motivoCambio) {
        SharedPreferences.Editor editor = preferencias.edit();
        editor.putString("mac", "00:00:00:00:00:00");//Resetea la mac en preferencia
        editor.apply();
        //Devuelve el control a BluetoothActivity
        Intent intent = new Intent();
        setResult(motivoCambio, intent);
        finish();//Regresa al BluetoothActivity para que busque un nuevo device bluetooth
    }
    /*********************************************************************
     * Estado del Bluetooth
     * Comprueba que el  Bluetooth está activado. Si no, regresa
     * a BluetoothActivity
     *********************************************************************/
    private void VerificarEstadoBT()
    {
        miBluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
       if (miBluetoothAdapter.isEnabled())
       {
                Log.d(TAG, "...Bluetooth Activado...");//Está correcto
       }
       else //Si el bluetooth se ha desactivado vamos a  BluetoothActivity
       {
                finish();//Regresa a BluetoothActivity
       }

    }
/***************************************************************************
****************************************************************************/
}//class

