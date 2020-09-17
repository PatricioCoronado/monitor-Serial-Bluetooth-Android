package com.segainvex.mibluetooth;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import android.widget.Button;
import android.widget.CompoundButton;
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
import java.util.UUID;
public class BaseActivity extends AppCompatActivity
{
    private static final String TAG = "DispositivosVinculados";
    SharedPreferences preferencias;


    private Vibrator vibrator;
    //Componentes comunes
    private TextView comando;
    private TextView respuesta;
    //Componentes de la cabeza
    ConstraintLayout componentesCabeza;
    LinearLayout graficoXY;
    Grafico graficoCabeza;
    private TextView fuerzaNormal;
    private TextView fuerzaLateral;
    private TextView suma;
    private ToggleButton fotodiodoUp,fotodiodoDown,fotodiodoRight,fotodiodoLeft;
    private ToggleButton laserUp,laserDown,laserRight,laserLeft;
    private SeekBar velocidadCabeza;
    //Componentes de la base
    ConstraintLayout componentesBase;
    LinearLayout graficoZ;
    Grafico graficoBase;
    private TextView GX;//Para mostrar las coordenadas del acelerómetro
    private TextView GY;
    private Button subir, bajar, parar;
    private SeekBar velocidadBase;
    private Switch z1;
    private Switch z2;
    private Switch z3;
    //Variables
    private int motorActivo;
    private int velocidadMotorBase;
    private int velocidadMotorCabeza;
    private boolean  movimientoDiscreto;
    private boolean baseActiva=true;
    private int pasosMovimientoDiscreto;
    //Componentes Bluetooth
    BluetoothDevice miDevice = null;//Bluetooth device que representa el dispositivo remoto
    BluetoothSocket miSocket = null;//Para enchufar el Bluetooth device a la radio Bluetooth
    BluetoothAdapter miBluetoothAdapter =null;//La radio Bluetooth
    private ConnectedThread miThread;//Hilo que lee y escribe la entrada del bluetooth
    Handler miHandler;//Para administrar los datos recibidos en un handler
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
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
        //Preferencias
        preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        //Vibración de los botones
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        //Componentes comunes
        comando = (TextView) findViewById(R.id.comando);
        respuesta = (TextView) findViewById(R.id.respuesta);
        //Componentes de la cabeza
        componentesCabeza = findViewById(R.id.componentes_cabeza);
        fuerzaNormal= (TextView) findViewById(R.id.fuerza_normal);
        fuerzaLateral= (TextView) findViewById(R.id.fuerza_lateral);
        suma= (TextView) findViewById(R.id.suma);
        fotodiodoUp = (ToggleButton) findViewById(R.id.fotodiodo_up);
        fotodiodoDown = (ToggleButton) findViewById(R.id.fotodiodo_down);
        fotodiodoRight = (ToggleButton) findViewById(R.id.fotodiodo_right);
        fotodiodoLeft = (ToggleButton) findViewById(R.id.fotodiodo_left);
        laserUp = (ToggleButton) findViewById(R.id.laser_up);
        laserDown = (ToggleButton) findViewById(R.id.laser_down);
        laserRight = (ToggleButton) findViewById(R.id.laser_right);
        laserLeft = (ToggleButton) findViewById(R.id.laser_left);
        velocidadCabeza = (SeekBar) findViewById(R.id.velocidad_cabeza);
        //Gráfico de la cabeza
        graficoXY = (LinearLayout) findViewById(R.id.grafico_xy);
        graficoCabeza = new Grafico(this);
        graficoXY.addView(graficoCabeza);
        //Componentes de la base
        componentesBase = findViewById(R.id.componentes_base);
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
        //Velocidad de la base seekbar y métodos asociados
        velocidadMotorBase=Global.VELOCIDAD_INICIAL;//El seekBar debe estar en esa posición
        velocidadBase = (SeekBar) findViewById(R.id.velocidad_base);
        velocidadBase.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                progress+=1;//Para que con 0 sea 10, con 10, 20...y con 50, 60
                velocidadMotorBase = progress*10;
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar){}
        });
        //Velocidad seekbar y listeners asociados
        velocidadMotorCabeza=Global.VELOCIDAD_INICIAL;//El seekBar debe estar en esa posición
        velocidadCabeza = (SeekBar) findViewById(R.id.velocidad_cabeza);
        velocidadCabeza.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                progress+=1;//Para que con 0 sea 10, con 10, 20...y con 50, 60
                velocidadMotorCabeza = progress*10;
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar){}
        });

        // Grafica de la base
        graficoZ = (LinearLayout) findViewById(R.id.grafico_z);
        graficoBase = new Grafico(this);
        graficoZ.addView(graficoBase);
        //Listeners de los botones de la cabeza
        fotodiodoUp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    moverCabeza(fotodiodoUp);} else {
                    pararCabeza();} }});
        fotodiodoDown.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    moverCabeza(fotodiodoDown);} else {
                    pararCabeza();} }});
        fotodiodoRight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    moverCabeza(fotodiodoRight);} else {
                    pararCabeza();} }});
        fotodiodoLeft.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    moverCabeza(fotodiodoLeft);} else {
                    pararCabeza();} }});
        laserUp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    moverCabeza(laserUp);} else {
                    pararCabeza();} }});
        laserDown.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    moverCabeza(laserDown);} else {
                    pararCabeza();} }});
        laserRight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    moverCabeza(laserRight);} else {
                    pararCabeza();} }});
        laserLeft.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    moverCabeza(laserLeft);} else {
                    pararCabeza();} }});
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
        if (id == R.id.fotodiodo) {fotodiodo(null);return true;}
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
                graficoBase.punto(x0,y0);
                graficoZ.removeView(graficoBase);//Borra el punto anterior (borra el gráfico completo)
                graficoZ.addView(graficoBase); //Pinta el gráfico con el nuevo punto
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
    public void moverBase(View view)
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
                comand = "MOT:MMP " + motorActivo + " 256 " + velocidadMotorBase + " "+ sentido +" " + pasosMovimientoDiscreto +" \r";
            }
            else //Movimiento continuo
            {
                comand = "MOT:MM " + motorActivo + " 256 " + velocidadMotorBase + " "+ sentido +"\r";
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
     * Metodo para para motores.  Envía a la base el comando "MOT:MP 0"
     *********************************************************************/
    public void pararCabeza()
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
     * Método que atiende a los botones para mover motores de la cabeza
     *********************************************************************/
    public void moverCabeza(ToggleButton view)
    {
        int sentidoCabeza=0;
        pararCabeza();//Primero detiene el movimiento de cualquier motor
        view.setChecked(true);
        //Selección de motor y sentido
        if(view.equals(fotodiodoUp)){sentidoCabeza=1;motorActivo=Global.fotodiodoY;}
        else if(view.equals(fotodiodoDown)){sentidoCabeza=0;motorActivo=Global.fotodiodoY;}
        else if(view.equals(fotodiodoLeft)){sentidoCabeza=1;motorActivo=Global.fotodiodoX;}
        else if(view.equals(fotodiodoRight)){sentidoCabeza=0;motorActivo=Global.fotodiodoX;}
        else if(view.equals(laserUp)){sentidoCabeza=1;motorActivo=Global.laserY;}
        else if(view.equals(laserDown)){sentidoCabeza=0;motorActivo=Global.laserY;}
        else if(view.equals(laserRight)){sentidoCabeza=1;motorActivo=Global.laserX;}
        else if(view.equals(laserLeft)){sentidoCabeza=0;motorActivo=Global.laserX;}
        //Preferencias. Tipo de movimiento y pasos a dar
        movimientoDiscreto = preferencias.getBoolean("movimiento",false);
        String comand;
        if(movimientoDiscreto)
        {
            String pasosDiscretos =  preferencias.getString("pasos","10000");
            pasosMovimientoDiscreto = Integer.parseInt(pasosDiscretos);
            comand = "MOT:MMP " + motorActivo + " 256 " + velocidadMotorCabeza + " "+ sentidoCabeza +" " + pasosMovimientoDiscreto +" \r";
        }
        else //Movimiento continuo
        {
            comand = "MOT:MM " + motorActivo + " 256 " + velocidadMotorCabeza + " "+ sentidoCabeza +"\r";
        }
        comando.setText(comand);
        miThread.write(comand.getBytes());
        vibrator.vibrate(Global.TIEMPO_VIBRACION);
    }
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
        if(baseActiva) {
            componentesCabeza.setVisibility(View.VISIBLE);
            componentesBase.setVisibility(View.INVISIBLE);
            baseActiva=false;
        }
        else {
            componentesCabeza.setVisibility(View.INVISIBLE);
            componentesBase.setVisibility(View.VISIBLE);
            baseActiva=true;
        }
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
     * Item del menú: fotodiodo: Pide las señales del fotodiodo
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
     * Item del menú busca_bluetooth. Devuelve el control a la
     * actividad del Bluetooth
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

