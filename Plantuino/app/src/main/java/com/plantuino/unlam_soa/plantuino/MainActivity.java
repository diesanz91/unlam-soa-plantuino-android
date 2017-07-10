package com.plantuino.unlam_soa.plantuino;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.RecognizerIntent;
import android.content.ActivityNotFoundException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import android.media.AudioManager;

import android.bluetooth.*;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int RECOGNIZE_SPEECH_ACTIVITY = 1;
    private static final int LIGHT_SENSOR_ACTIVITY = 2;
    private String voz;
    private TextToSpeech speech;
    private String direccion = null;
    private boolean sensorMovil;
    private boolean ledApagado = true;
    private float luz;

    //Variables para comunicación Bluetooth
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket bluetoothSocket = null;
    private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private boolean isBtConnected = false;
    private ThreadConectar bluetoothConnection = null;
    private ThreadConectado bluetoothConectado = null;
    private boolean conexionExitosa = true;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private Button btnDesconectarBluetooth = null;
    //Fin variables para comunicación Bluetooth

    private Button btnSetearLimites = null;
    private EditText editTextLimiteLuz = null;
    private EditText editTextLimiteTemperatura = null;
    private EditText editTextLimiteHumedad = null;
    private Button btnVentilacion = null;
    private String txtBtnVentilacion = null;
    private Button btnIluminacion = null;
    private String txtBtnIluminacion = null;
    private Button btnHidratacion = null;
    private String txtBtnHidratacion = null;
    private Button btnObtenerDatos = null;
    private String txtBtnObtenerDatos = null;
    private Button btnMensajeVoz = null;
    private TextView lblMensajeVoz = null;
    private EditText editTextDatosSensores = null;
    private String contentTxtDatosSensores = null;
    private Button btnLimpiarDatos = null;
    //Boton Configuracion
    private Button btnConfig = null;
    private Button btnLuzMobile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        /*
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(true);
*/
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(false);


        //Inicialización de objetos para sensor de proximidad
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if(sensorManager != null){
            Sensor sensorProximidad = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            //Registro listener al sensor de proximidad
            sensorManager.registerListener(this,sensorProximidad,SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getApplicationContext(),"Ocurrio un error al obtener sensor de proximidad",Toast.LENGTH_SHORT);
        }

        //Inicialización de objetos para sensor acelerómetro
        if (sensorManager != null){
            Sensor sensorAcelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            //Registro listener al sensor acelerómetro
            sensorManager.registerListener(this,sensorAcelerometro,SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getApplicationContext(),"Ocurrio un error al obtener sensor acelerometro",Toast.LENGTH_SHORT);
        }

        if(sensorManager != null){
            Sensor sensorLuz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            //Registro listener al sensor de luz
            sensorManager.registerListener(this,sensorLuz,SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getApplicationContext(),"Ocurrio un error al obtener sensor de luz",Toast.LENGTH_SHORT);
        }

        //Inicialización de objetos para speech de datos de sensores obtenidos
        speech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
            public void onInit(int status){
                if(status != TextToSpeech.ERROR){
                    //Seteo lenguaje
                    speech.setLanguage(Locale.UK);
                }
            }
        });

        //Obtengo los diversos componentes de la vista
        btnSetearLimites = (Button)findViewById(R.id.btnSetearLimites);
        editTextLimiteLuz = (EditText)findViewById(R.id.editTextLimiteLuz);
        editTextLimiteTemperatura = (EditText)findViewById(R.id.editTextLimiteTemperatura);
        editTextLimiteHumedad = (EditText)findViewById(R.id.editTextLimiteHumedad);
        btnDesconectarBluetooth = (Button)findViewById(R.id.btnDesconectarBluetooth);
        btnVentilacion = (Button)findViewById(R.id.btnVentilacion);
        txtBtnVentilacion = (String)btnVentilacion.getText();
        btnIluminacion = (Button)findViewById(R.id.btnIluminacion);
        txtBtnIluminacion = (String)btnIluminacion.getText();
        btnHidratacion = (Button)findViewById(R.id.btnHidratacion);
        txtBtnHidratacion = (String)btnHidratacion.getText();
        btnObtenerDatos = (Button)findViewById(R.id.btnObtenerDatos);
        txtBtnObtenerDatos = (String)btnObtenerDatos.getText();
        btnMensajeVoz = (Button)findViewById(R.id.btnMensajeVoz);
        lblMensajeVoz = (TextView)findViewById(R.id.lblMensajeVoz);
        editTextDatosSensores = (EditText) findViewById(R.id.editTextDatosSensores);
        contentTxtDatosSensores = editTextDatosSensores.getEditableText().toString();
        btnLimpiarDatos = (Button)findViewById(R.id.btnLimpiarDatos);
        //Boton Configuracion
        btnConfig = (Button)findViewById(R.id.btnConfig);
        btnLuzMobile = (Button)findViewById(R.id.btnLuzMobile);
        btnLuzMobile.setVisibility(View.INVISIBLE);

        //Seteo listener a todos los botones
        btnSetearLimites.setOnClickListener(botonesListener);
        btnVentilacion.setOnClickListener(botonesListener);
        btnIluminacion.setOnClickListener(botonesListener);
        btnHidratacion.setOnClickListener(botonesListener);
        btnObtenerDatos.setOnClickListener(botonesListener);
        btnMensajeVoz.setOnClickListener(botonesListener);
        btnLimpiarDatos.setOnClickListener(botonesListener);
        btnDesconectarBluetooth.setOnClickListener(botonesListener);
        //Boton Configuracion
        btnConfig.setOnClickListener(botonesListener);

        //Region bluetooth
        Intent intent = getIntent();
        //Obtengo direccion MAC del dispositivo bluetooth seleccionado de la lista
        direccion = intent.getStringExtra(ManejadorBluetoothActivity.EXTRA_ADDRESS);

        if(bluetoothSocket == null || !isBtConnected){
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            //Realizamos conexion con el dispositivo y verificamos si esta disponible, debemos realizarla en otro thread para no bloquear el UIThread (thread principal)
            BluetoothDevice dispositivo = bluetoothAdapter.getRemoteDevice(direccion);
            if(dispositivo != null){
                bluetoothConnection = new ThreadConectar(dispositivo);
                //Una vez instanciada la conexion bluetooth en otro thread diferente al principal, ejecuto su funcionalidad
                bluetoothConnection.start();
            }
        }

    }

    //Clase para iniciar la conexion bluetooth con la placa
        private class ThreadConectar extends Thread {

            //Atributos clase ThreadConectar
            private BluetoothSocket mmSocket = null;
            private BluetoothDevice mmDevice = null;
            private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            //Constructor clase ThreadConectar
            public ThreadConectar(BluetoothDevice device){

                mmDevice = device;
                //Variable temporal tipo socket para setear el valor obtenido desde createRfcommSocketToServiceRecord
                BluetoothSocket tmpSocket = null;
                try{
                    tmpSocket = device.createRfcommSocketToServiceRecord(mUUID);
                } catch (IOException ioExc){
                    conexionExitosa = false;
                }
                mmSocket = tmpSocket;
            }

            //Métodos clase ThreadConectar
            public void run(){

                bluetoothAdapter.cancelDiscovery();
                try{
                    mmSocket.connect();
                }catch (IOException ioExc){
                    try{
                        mmSocket.close();
                    } catch(IOException ioException){
                        return;
                    }
                }
                bluetoothConectado = new ThreadConectado(mmSocket);
                bluetoothConectado.start();
                //Solicito datos de los limites a la placa
                bluetoothConectado.write("S\n".toString().getBytes());
            }

            public void cancel(){

                try{
                    bluetoothSocket.close();
                }catch(IOException ioException){

                }

            }
            //Fin métodos clase ThreadConectar

        }

        //Clase para manejar la conexion bluetooth con la placa
        private class ThreadConectado extends Thread{

            //Atributos privados de la clase ThreadConectado
            private BluetoothSocket mmSocket = null;
            private InputStream mmInputStream = new InputStream() {
                @Override
                public int read() throws IOException {
                    return 0;
                }
            };
            private OutputStream mmOutputStream = new OutputStream() {
                @Override
                public void write(int b) throws IOException {

                }
            };
            public String contenido = "";

            //Constructor clase ThreadConectado
            public ThreadConectado(BluetoothSocket socket){

                mmSocket = socket;
                //Creo un input y output steam temporales para manejar flujos de entrada y salida bluetooth y luego setear lo obtenido en los atributos mmInputStream y mmOutputStream de la clase ThreadConctado
                InputStream tmpIn = null;
                OutputStream tmpOut = null;
                try{
                    if(socket != null){
                        tmpIn = socket.getInputStream();
                        tmpOut = socket.getOutputStream();
                    }
                } catch (IOException ioException){}

                mmInputStream = tmpIn;
                mmOutputStream = tmpOut;

            }

        //Métodos clase ThreadConectado
        public void run(){
            //Debemos aplicar la funcionalidad correspondiente para que cuando se reciba informacion (datos sensores) desde la placa la escriba en los componentes correspondientes
            while(true){
                try{
                    BufferedReader buffReader = new BufferedReader(new InputStreamReader(mmInputStream));
                    String linea;

                    while((linea = buffReader.readLine()) != null){
                         this.contenido += linea.replace('|','\n');
                    }

                }catch(IOException ioException){
                    //Error al leer datos enviados por la placa
                    contentTxtDatosSensores = "Error al leer datos enviados por la placa\n";
                    editTextDatosSensores.setText(contentTxtDatosSensores);
                }
            }
        }

        //Método para escribir en flujo de salida (hacia la placa)
        public void write(byte[] bytes){
            try{
                mmOutputStream.write(bytes);
            }catch(IOException ioException){

            }
        }

        public void cancel(){
            try{
                mmSocket.close();
            }catch (IOException ioException){}
        }
        //Fin métodos clase ThreadConectado

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        switch(requestCode){
            case RECOGNIZE_SPEECH_ACTIVITY:
                if(resultCode == RESULT_OK && null != data){

                    ArrayList<String> speech = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    String strSpeechText = speech.get(0);
                    lblMensajeVoz.setText(strSpeechText);
                    voz = strSpeechText;

                    if(voz.compareToIgnoreCase("sensor") == 0 || voz.compareToIgnoreCase("datos") == 0){
                        if(bluetoothConectado != null){
                            //contentTxtDatosSensores = "Luz: 54612 lx\nTemperatura: 24.70 °C\nPresión Absoluta: 1017.34 mb (milibares)\nPresión Relativa: 1242.51 mb (milibares)\nHumedad: 51.70 %\n";
                            bluetoothConectado.write("A".toString().getBytes());

                            editTextDatosSensores.setText(bluetoothConectado.contenido);
                            bluetoothConectado.contenido = "";
                            Toast.makeText(getApplicationContext(), "Datos sensados recibidos", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(getApplicationContext(), "Comando de voz inválido", Toast.LENGTH_SHORT).show();
                    }

                }
                break;
            case LIGHT_SENSOR_ACTIVITY:
                if(resultCode == RESULT_OK && null != data) {
                    sensorMovil = data.getBooleanExtra("SensorMovil",false);
                   if (sensorMovil) {
                        btnLuzMobile.setVisibility(View.VISIBLE);
                        bluetoothConectado.write("M\n".toString().getBytes());
                        Toast.makeText(getApplicationContext(), "Sensor de Luz del Movil Activado", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        btnLuzMobile.setVisibility(View.INVISIBLE);
                        bluetoothConectado.write("m\n".toString().getBytes());
                        Toast.makeText(getApplicationContext(), "Sensor de Luz del Movil Desactivado", Toast.LENGTH_SHORT).show();
                    }
                }
            default:
                break;
        }
    }

    private View.OnClickListener botonesListener= new View.OnClickListener(){

        public void onClick(View v){

            switch (v.getId()){
                case R.id.btnSetearLimites:

                    String limiteLuz = editTextLimiteLuz.getText().toString();
                    String limiteTemperatura = editTextLimiteTemperatura.getText().toString();
                    String limiteHumedad = editTextLimiteHumedad.getText().toString();
                    StringBuilder parametros = new StringBuilder();
                    parametros.append('P').append('-');
                    /*
                    if(limiteLuz != null && limiteLuz.length() != 0)
                        parametros.append(limiteLuz).append('-');
                    else
                        parametros.append(0).append('-');
                    */

                    int longLimiteLuz = limiteLuz.length();
                    String ceros = "";

                    for(int i = 0; i < 5 - longLimiteLuz; i++){
                        ceros += "0";
                    }

                    parametros.append("" + ceros + limiteLuz).append('-');
                    ceros = "";

                    if(limiteTemperatura != null && limiteTemperatura.length() != 0)
                        parametros.append(limiteTemperatura).append('-');
                    else
                        parametros.append(0).append('-');

                    if(limiteHumedad != null && limiteHumedad.length() != 0)
                        parametros.append(limiteHumedad).append('\n');
                    else
                        parametros.append(0).append('\n');

                    if(bluetoothConectado != null)
                        bluetoothConectado.write(parametros.toString().getBytes());
                    Toast.makeText(getApplicationContext(), "Límites Parámetros Enviados", Toast.LENGTH_SHORT).show();
                    break;
                case  R.id.btnDesconectarBluetooth:
                    bluetoothConectado.cancel();
                    bluetoothConnection.cancel();
                    Toast.makeText(getApplicationContext(),"Conexión Bluetooth cerrada",Toast.LENGTH_LONG).show();
                    break;
                case R.id.btnVentilacion:
                    if(txtBtnVentilacion.compareToIgnoreCase("ENCENDER") == 0){
                        if(bluetoothConectado != null){
                            //Envio indicacion a placa para operar con actuador
                            bluetoothConectado.write("V\n".toString().getBytes());
                            txtBtnVentilacion = "APAGAR";
                            btnVentilacion.setText(txtBtnVentilacion);
                            Toast.makeText(getApplicationContext(),"Ventilación Encendida",Toast.LENGTH_LONG).show();
                        }
                    } else {
                        if(bluetoothConectado != null){
                            //Envio indicacion a placa para operar con actuador
                            bluetoothConectado.write("v\n".toString().getBytes());
                            txtBtnVentilacion = "ENCENDER";
                            btnVentilacion.setText(txtBtnVentilacion);
                            Toast.makeText(getApplicationContext(),"Ventilación Apagada",Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case R.id.btnIluminacion:
                    if(txtBtnIluminacion.compareToIgnoreCase("ENCENDER") == 0){
                        if(bluetoothConectado != null){
                            //Envio indicacion a placa para operar con actuador
                            bluetoothConectado.write("I\n".toString().getBytes());
                            txtBtnIluminacion = "APAGAR";
                            btnIluminacion.setText(txtBtnIluminacion);
                            Toast.makeText(getApplicationContext(),"Iluminación Encendida",Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if(bluetoothConectado != null){
                            //Envio indicacion a placa para operar con actuador
                            bluetoothConectado.write("i\n".toString().getBytes());
                            txtBtnIluminacion = "ENCENDER";
                            btnIluminacion.setText(txtBtnIluminacion);
                            Toast.makeText(getApplicationContext(), "Iluminación Apagada", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case R.id.btnHidratacion:
                    if(txtBtnHidratacion.compareToIgnoreCase("ENCENDER") == 0){
                        if(bluetoothConectado != null){
                            //Envio indicacion a placa para operar con actuador
                            bluetoothConectado.write("B\n".toString().getBytes());
                            txtBtnHidratacion = "APAGAR";
                            btnHidratacion.setText(txtBtnHidratacion);
                            Toast.makeText(getApplicationContext(),"Hidratación Encendida",Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if(bluetoothConectado != null){
                            //Envio indicacion a placa para operar con actuador
                            bluetoothConectado.write("b\n".toString().getBytes());
                            txtBtnHidratacion = "ENCENDER";
                            btnHidratacion.setText(txtBtnHidratacion);
                            Toast.makeText(getApplicationContext(),"Hidratación Apagada",Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case R.id.btnObtenerDatos:
                    if(bluetoothConectado != null){
                        contentTxtDatosSensores = "";
                        //Envio indicacion a placa para operar con actuador
                        bluetoothConectado.write("A\n".toString().getBytes());
                        //contentTxtDatosSensores = "Luz: 54612 lx\nTemperatura: 24.70 °C\nPresión Absoluta: 1017.34 mb (milibares)\nPresión Relativa: 1242.51 mb (milibares)\nHumedad: 51.70 %\n";
                        while(bluetoothConectado.contenido == ""){}
                        editTextDatosSensores.setText(bluetoothConectado.contenido);
                        bluetoothConectado.contenido = "";
                        Toast.makeText(getApplicationContext(), "Datos Sensados Recibidos", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.btnMensajeVoz:
                    onClickImgBtnHablar();
                    Toast.makeText(getApplicationContext(),"Micrófono presionado",Toast.LENGTH_SHORT).show();
                    break;
                case  R.id.btnLimpiarDatos:
                    editTextDatosSensores.setText("");
                    Toast.makeText(getApplicationContext(),"Se han limpiado los datos de sensores",Toast.LENGTH_SHORT).show();
                    break;
                case R.id.btnConfig:
                    Intent i = new Intent(MainActivity.this, CalibracionLuz.class);
                    if(sensorMovil)
                        i.putExtra("SensorMovil", "S");
                    else
                        i.putExtra("SensorMovil", "N");
                    startActivityForResult(i,LIGHT_SENSOR_ACTIVITY);
                    break;
                default:
                    Toast.makeText(getApplicationContext(),"Error ocurrido en Listener de botones",Toast.LENGTH_LONG).show();
            }
        }

    };

    //Creo intent para ingresar comando de voz
    private void onClickImgBtnHablar(){
        Intent intentActionRecognizeSpeech = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //Configuramos el lenguaje
        intentActionRecognizeSpeech.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        try{
            startActivityForResult(intentActionRecognizeSpeech,RECOGNIZE_SPEECH_ACTIVITY);
        }catch(ActivityNotFoundException ae){
            Toast.makeText(getApplicationContext(),"Este dispositivo no soporta reconocimiento de voz",Toast.LENGTH_LONG).show();
        }
    }

    //Métodos encargados de manejar los sensores
    //Implemento métodos de la clase SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int precision) {}

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onSensorChanged(SensorEvent evento){
        //Cada sensor puede provocar que un thread principal pase por este bloque de codigo asi que sincronizamos el acceso
        synchronized (this){
            switch (evento.sensor.getType()){
                case Sensor.TYPE_PROXIMITY:
                    if(evento.values[0] != 0){
                        //contentTxtDatosSensores = "Sin deteccion de proximidad";
                        speech.stop();
                    } else {
                        //Se detecto objeto
                        //Solicito datos de sensores a la placa y reproduzco audio con los datos
                        //contentTxtDatosSensores = "Light: 54612 lx\nTemperature: 24.70 °C\nAbsolute Pressure: 1017.34 mb\nRelative Pressure: 1242.51 mb\nHumidity: 51.70 %\n";
                        if(bluetoothConectado != null) {
                            bluetoothConectado.write("A\n".toString().getBytes());
                            /*
                            while (bluetoothConectado.contenido == "") {
                            }
                            */

                            speech.stop();
                            speech.setSpeechRate(1.0f);
                            speech.setPitch(1.0f);
                            speech.speak(bluetoothConectado.contenido, TextToSpeech.QUEUE_FLUSH, null, "");

                            editTextDatosSensores.setText(bluetoothConectado.contenido);
                            bluetoothConectado.contenido = "";
                        }
                    }
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    if(Math.abs(evento.values[0]) > 30|| Math.abs(evento.values[1]) > 30 || Math.abs(evento.values[2]) > 30){
                        if(bluetoothConectado != null) {
                            //En este caso al detectar shake solicito datos sensores a la placa y los muestro en el textarea
                            bluetoothConectado.write("A\n".toString().getBytes());
                            while (bluetoothConectado.contenido == "") {
                            }
                            editTextDatosSensores.setText(bluetoothConectado.contenido);
                            bluetoothConectado.contenido = "";
                        }
                    }
                    break;
                case Sensor.TYPE_LIGHT:

                    if(sensorMovil && bluetoothConectado != null)
                    {
                        luz = evento.values[0];
                        if(luz<Float.valueOf(editTextLimiteLuz.getText().toString())){
                            if(ledApagado) {
                                //Enciende Led
                                bluetoothConectado.write("Z\n".toString().getBytes());
                                ledApagado = false;
                            }
                        }else{
                            if(!ledApagado) {
                                //Apaga Led
                                bluetoothConectado.write("z\n".toString().getBytes());
                                ledApagado = true;
                            }
                        }
                    }

                    break;
                default:
                    break;
            }
        }
    }

    public void onPause(){
        if(speech !=null){
            speech.stop();
        }
        super.onPause();
    }

    public void onDestoy(){
        if(speech !=null){
            speech.stop();
            speech.shutdown();
        }
        super.onDestroy();
    }
}
