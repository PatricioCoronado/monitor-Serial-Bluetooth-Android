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
        import android.widget.CompoundButton;
        import android.widget.EditText;
        import android.widget.LinearLayout;
        import android.widget.SeekBar;
        import android.widget.Switch;
        import android.widget.TextView;
        import android.os.Vibrator;
        import android.widget.Toast;
        import android.widget.ToggleButton;

        import java.io.IOException;
        import java.io.UnsupportedEncodingException;
        import java.util.Arrays;
        import java.util.Set;
        import java.util.UUID;

public class CabezaActivity extends AppCompatActivity
{
    private static final String TAG = "DispositivosVinculados";
    SharedPreferences preferencias;
    //Grafico
    LinearLayout graficoXY;
    Grafico grafico;//Clase para dibujar
    //Vibrador para los botones
    private Vibrator vibrator;
    //Componentes gráficos
    //Para mostrar las señales del fotodiodo
    private TextView fuerzaNormal;
    private TextView fuerzaLateral;
    private TextView suma;
    //Texto con el comando y la respuesta
    private TextView respuesta;
    private TextView comando;
    //private TextView baseConectada;
    // Botones para mover motores
    private ToggleButton fotodiodoUp,fotodiodoDown,fotodiodoRight,fotodiodoLeft;
    private ToggleButton laserUp,laserDown,laserRight,laserLeft;
    //Seekbar para la velocidad
    private SeekBar velocidad;
    //Variables
    private int velocidadMotor;
    private int motorActivo;
    private boolean  movimientoDiscreto;
    private int pasosMovimientoDiscreto;
    //Componentes Bluetooth
    BluetoothDevice miDevice = null;//Bluetooth device que representa el dispositivo remoto
    BluetoothSocket miSocket = null;//Para enchufar el Bluetooth device a la radio Bluetooth
    BluetoothAdapter miBluetoothAdapter =null;//La radio Bluetooth
    //Hilo para recibir por el Bluetooth y handler para procesar la respuesta
    private ConnectedThread miThread;//Hilo que lee y escribe la entrada del bluetooth
    Handler miHandler;//Para administrar los datos recibidos en un handler
    //UUID para utilizar el Bluetooth como serial
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
        setContentView(R.layout.activity_cabeza);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        //Textos de comando y respuesta
        comando = (TextView) findViewById(R.id.comando);
        respuesta = (TextView) findViewById(R.id.respuesta);
        //Señales del fotodiodo
        fuerzaNormal= (TextView) findViewById(R.id.fuerza_normal);
        fuerzaLateral= (TextView) findViewById(R.id.fuerza_lateral);
        suma= (TextView) findViewById(R.id.suma);
        //Botones de motores
        fotodiodoUp = (ToggleButton) findViewById(R.id.fotodiodo_up);
        fotodiodoDown = (ToggleButton) findViewById(R.id.fotodiodo_down);
        fotodiodoRight = (ToggleButton) findViewById(R.id.fotodiodo_right);
        fotodiodoLeft = (ToggleButton) findViewById(R.id.fotodiodo_left);
        laserUp = (ToggleButton) findViewById(R.id.laser_up);
        laserDown = (ToggleButton) findViewById(R.id.laser_down);
        laserRight = (ToggleButton) findViewById(R.id.laser_right);
        laserLeft = (ToggleButton) findViewById(R.id.laser_left);
        //Listeners de los botones
        fotodiodoUp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {mover(fotodiodoUp);} else {parar();} }});
        fotodiodoDown.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {mover(fotodiodoDown);} else {parar();} }});
        fotodiodoRight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {mover(fotodiodoRight);} else {parar();} }});
        fotodiodoLeft.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {mover(fotodiodoLeft);} else {parar();} }});
        laserUp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {mover(laserUp);} else {parar();} }});
        laserDown.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {mover(laserDown);} else {parar();} }});
        laserRight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {mover(laserRight);} else {parar();} }});
        laserLeft.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {mover(laserLeft);} else {parar();} }});
        //Borra el texto de los botones
        /*
        fotodiodoUp.setTextOff(" ");
        fotodiodoDown.setTextOff(" ");
        fotodiodoRight.setTextOff(" ");
        fotodiodoLeft.setTextOff(" ");
        fotodiodoUp.setTextOn(" ");
        fotodiodoDown.setTextOn(" ");
        fotodiodoRight.setTextOn(" ");
        fotodiodoLeft.setTextOn(" ");
        laserUp.setTextOff(" ");
        laserDown.setTextOff(" ");
        laserRight.setTextOff(" ");
        laserLeft.setTextOff(" ");
        laserUp.setTextOn(" ");
        laserDown.setTextOn(" ");
        laserRight.setTextOn(" ");
        laserLeft.setTextOn(" ");

         */

        //parar();
        //Motor a mover
        motorActivo=0;
        //Velocidad seekbar y listeners asociados
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
        //Inicializa el gráfico y pinta el fondo
        graficoXY = (LinearLayout) findViewById(R.id.grafico_xy);
        grafico = new Grafico(this);
        graficoXY.addView(grafico);
        //Preferencias
        preferencias = PreferenceManager.getDefaultSharedPreferences(this);

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
                        //acelerometro(respuestaTrim,longRespuestaTrim,GX,GY);
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
        getMenuInflater().inflate(R.menu.menu_cabeza, menu);
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
        if (id == R.id.fotodiodo) {fotodiodo(null);return true;}
        if (id == R.id.preferencias){lanzarPreferencias();return true;}
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
                graficoXY.removeView(grafico);//Borra el punto anterior (borra el gráfico completo)
                graficoXY.addView(grafico); //Pinta el gráfico con el nuevo punto
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
    public void enviar(View view)
    {
        String msg = comando.getText().toString();
        //Si no termina con retorno de carro lo pone
        int posicion = msg.indexOf("\r");
        if (posicion ==-1) msg += "\r";
        miThread.write(msg.getBytes());
        vibrator.vibrate(Global.TIEMPO_VIBRACION);
    }
    /*********************************************************************
     * Botón parar: Envía a la base el comando "MOT:MP 0"
     *********************************************************************/
    public void parar()
    {
        String comand = "MOT:MP 0\r";
        comando.setText(comand);
        miThread.write(comand.getBytes());
        vibrator.vibrate(Global.TIEMPO_VIBRACION);
        //Desactiva todos los botones

        fotodiodoUp.setChecked(false);
        fotodiodoDown.setChecked(false);
        fotodiodoRight.setChecked(false);
        fotodiodoLeft.setChecked(false);
        laserUp.setChecked(false);
        laserDown.setChecked(false);
        laserRight.setChecked(false);
        laserLeft.setChecked(false);


    }
    /*********************************************************************
     * Botón subir y bajar: Envía a la base el comando..
     * "MOT:MMP <MotorActivo Resolucion Frecuencia Sentido Pasos>"
     *********************************************************************/
    public void mover(ToggleButton view)
    {
        int sentido=0;
        parar();//Primero detiene el movimiento de cualquier motor
        view.setChecked(true);
        //Selección de motor y sentido
        if(view.equals(fotodiodoUp)){sentido=1;motorActivo=Global.fotodiodoY;}
        else if(view.equals(fotodiodoDown)){sentido=0;motorActivo=Global.fotodiodoY;}
        else if(view.equals(fotodiodoLeft)){sentido=1;motorActivo=Global.fotodiodoX;}
        else if(view.equals(fotodiodoRight)){sentido=0;motorActivo=Global.fotodiodoX;}
        else if(view.equals(laserUp)){sentido=1;motorActivo=Global.laserY;}
        else if(view.equals(laserDown)){sentido=0;motorActivo=Global.laserY;}
        else if(view.equals(laserRight)){sentido=1;motorActivo=Global.laserX;}
        else if(view.equals(laserLeft)){sentido=0;motorActivo=Global.laserX;}
        //Preferencias. Tipo de movimiento y pasos a dar
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
     /*********************************************************************
     *                  Métodos del menú
     ********************************************************************/
        /*********************************************************************
     * Item del menú: preferencias: carga el fragment de preferencias
     *********************************************************************/
    public void lanzarPreferencias()
    {
        Intent intentPreferencias;
        intentPreferencias = new Intent(this, PreferenciasActivity.class);
        startActivity(intentPreferencias);
    }
    /*********************************************************************
     * Item del menú: error: Pide error a la base
     *********************************************************************/
    public void pideError(View view)
    {
        String comand =new String("ERR?\r");
        comando.setText(comand);
        miThread.write(comand.getBytes());
        vibrator.vibrate(Global.TIEMPO_VIBRACION);
    }
    /*********************************************************************
     * Item del menú: version: Pide la versión a la base
     *********************************************************************/
    public void version(View view)
    {
        String comand =new String("MOT:VER?\r");
        comando.setText(comand);
        miThread.write(comand.getBytes());
        vibrator.vibrate(Global.TIEMPO_VIBRACION);
    }
    /*********************************************************************
     * Item del menú: fotodiodo: Pide las coordendas del fotodiodo
     *********************************************************************/
    private void fotodiodo(View view)
    {
        String muestrasString = preferencias.getString("muestrasFotodiodo","100");
        int muestrasFotodiodo = Integer.parseInt(muestrasString);
        String comand =new String("MOT:IFO "+ muestrasFotodiodo +" \r");
        comando.setText(comand);
        miThread.write(comand.getBytes());//Envía el comando
        vibrator.vibrate(Global.TIEMPO_VIBRACION);
    }
    /*********************************************************************
     * Item del menú: busca_bluetooth
     * Resetea la MAC, y devuelve el control a la actividad de bluetooth
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
     * Item del menú: Cambio entre activity de control de base o cabeza
     *********************************************************************/
      public void cambio (View view)
    {
        /*
        Intent intentCabeza =  new Intent(this, CabezaActivity.class);
        startActivity(intentCabeza);
        */
        finish();
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
 ***************************************************************************/
}//class

