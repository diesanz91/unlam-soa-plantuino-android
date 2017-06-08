package com.plantuino.unlam_soa.plantuino;

import android.app.ProgressDialog;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import android.bluetooth.*;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int RECOGNIZE_SPEECH_ACTIVITY = 1;
    private String voz;
    private TextToSpeech speech;
    private String direccion = null;
    private ProgressDialog progreso;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket bluetoothSocket = null;
    private boolean isBtConnected = false;
    private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Button btnDesconectarBluetooth = null;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Inicialización de objetos para sensor de proximidad
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if(sensorManager != null){
            Sensor sensorProximidad = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            //Registro listener al sensor de proximidad
            sensorManager.registerListener(this,sensorProximidad,SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getApplicationContext(),"Ocurrio un error al obtener sensor de proximidad",Toast.LENGTH_SHORT);
        }

        //Inicialización de objetos para sensor acelerometro
        if (sensorManager != null){
            Sensor sensorAcelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            //Registro listener al sensor acelerometro
            sensorManager.registerListener(this,sensorAcelerometro,SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getApplicationContext(),"Ocurrio un error al obtener sensor acelerometro",Toast.LENGTH_SHORT);
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

        Intent intent = getIntent();

        //Obtengo direccion MAC del dispositivo Bluetooth seleccionado de la lista
        direccion = intent.getStringExtra(ManejadorBluetoothActivity.EXTRA_ADDRESS);

        //Obtengo los diversos componentes de la vista
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

        //Seteo listener a todos los botones
        btnVentilacion.setOnClickListener(botonesListener);
        btnIluminacion.setOnClickListener(botonesListener);
        btnHidratacion.setOnClickListener(botonesListener);
        btnObtenerDatos.setOnClickListener(botonesListener);
        btnMensajeVoz.setOnClickListener(botonesListener);
        btnLimpiarDatos.setOnClickListener(botonesListener);

        //Ejecutamos conexion Bluetooth
        //new conexionBluetooth().execute();

    }

    //Implemento métodos de la clase SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int precision) {}

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onSensorChanged(SensorEvent evento){
        //Cada sensor puede provocar que un thread principal pase por este bloque de codigo asi que sincronizamos el acceso
        synchronized (this){
            switch (evento.sensor.getType()){
                case Sensor.TYPE_PROXIMITY:
                    if(evento.values[0] != 0){
                        contentTxtDatosSensores = "Detección proximidad";
                        editTextDatosSensores.setText(contentTxtDatosSensores);
                    } else{
                        contentTxtDatosSensores = "Sin detección de proximidad";
                        editTextDatosSensores.setText(contentTxtDatosSensores);
                        //En este caso solicito datos sensores a la placa y reproduzco audio con los datos
                        contentTxtDatosSensores = "Light: 54612 lx\nTemperature: 24.70 °C\nAbsolute Pressure: 1017.34 mb\nRelative Pressure: 1242.51 mb\nHumidity: 51.70 %\n";
                        speech.speak(contentTxtDatosSensores, TextToSpeech.QUEUE_FLUSH,null,"Datos Sensores");
                    }
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    if(Math.abs(evento.values[0]) > 30|| Math.abs(evento.values[1]) > 30 || Math.abs(evento.values[2]) > 30){
                        //En este caso al detectar shake solicito datos sensores a la placa y los muestro en el textarea
                        contentTxtDatosSensores = "Shake detectado";
                        editTextDatosSensores.setText(contentTxtDatosSensores);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private class conexionBluetooth extends AsyncTask<Void, Void, Void>{

        private boolean conexionExitosa = true;

        protected void onPreExecute(){
            progreso = ProgressDialog.show(MainActivity.this,"Conectando...", "Espere por favor!");
        }

        protected Void doInBackground(Void... dispositivos){
            try{
                if(bluetoothSocket == null || !isBtConnected){
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    //Realizamos conexion con el dispositivo y verificamos si esta disponible
                    BluetoothDevice dispositivo = bluetoothAdapter.getRemoteDevice(direccion);
                    bluetoothSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(mUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    bluetoothSocket.connect();
                }

            }catch (IOException ioExc){
                conexionExitosa = false;
            }
            return null;
        }

        protected void onPostExecute(Void result){
            super.onPostExecute(result);

            if(!conexionExitosa){
                Toast.makeText(getApplicationContext(),"Falló la conexión",Toast.LENGTH_SHORT).show();
                finish();
            } else {
               Toast.makeText(getApplicationContext(),"Conexión exitosa",Toast.LENGTH_SHORT).show();
                isBtConnected = true;
            }
            progreso.dismiss();
        }

    }

    private void desconexionBluetooth(){
        if(bluetoothSocket != null){
            try{
                bluetoothSocket.close();
            }catch (IOException ioExc){
                Toast.makeText(getApplicationContext(),"Ocurrió un error al desconectar",Toast.LENGTH_SHORT).show();
            }
        }
        finish();
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

                    if(voz.compareToIgnoreCase("sensor") == 0){
                        contentTxtDatosSensores = "Luz: 54612 lx\nTemperatura: 24.70 °C\nPresión Absoluta: 1017.34 mb (milibares)\nPresión Relativa: 1242.51 mb (milibares)\nHumedad: 51.70 %\n";
                        editTextDatosSensores.setText(contentTxtDatosSensores);
                        Toast.makeText(getApplicationContext(), "Datos Sensados Recibidos", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(getApplicationContext(), "Comando de voz inválido", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
                break;
        }
    }

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

    private View.OnClickListener botonesListener= new View.OnClickListener(){

        public void onClick(View v){

            switch (v.getId()){
                case  R.id.btnDesconectarBluetooth:
                    desconexionBluetooth();
                    break;
                case R.id.btnVentilacion:
                    if(txtBtnVentilacion.compareToIgnoreCase("ENCENDER") == 0){
                        if(bluetoothSocket != null){
                            try{
                                bluetoothSocket.getOutputStream().write("V".toString().getBytes());
                                txtBtnVentilacion = "APAGAR";
                                btnVentilacion.setText(txtBtnVentilacion);
                                Toast.makeText(getApplicationContext(),"Ventilación Encendida",Toast.LENGTH_LONG).show();
                            } catch (IOException ioExc){
                                Toast.makeText(getApplicationContext(),"Error al encender ventilación",Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        if(bluetoothSocket != null){
                            try{
                                bluetoothSocket.getOutputStream().write("v".toString().getBytes());
                                txtBtnVentilacion = "ENCENDER";
                                btnVentilacion.setText(txtBtnVentilacion);
                                Toast.makeText(getApplicationContext(),"Ventilación Apagada",Toast.LENGTH_SHORT).show();
                            } catch(IOException ioExc){
                                Toast.makeText(getApplicationContext(),"Error al apagar ventilación",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    break;
                case R.id.btnIluminacion:
                    if(txtBtnIluminacion.compareToIgnoreCase("ENCENDER") == 0){
                        if(bluetoothSocket != null){
                            try{
                                bluetoothSocket.getOutputStream().write("I".toString().getBytes());
                                txtBtnIluminacion = "APAGAR";
                                btnIluminacion.setText(txtBtnIluminacion);
                                Toast.makeText(getApplicationContext(),"Iluminación Encendida",Toast.LENGTH_SHORT).show();
                            } catch(IOException ioExc){
                                Toast.makeText(getApplicationContext(),"Error al encender iluminación",Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        if(bluetoothSocket != null){
                            try{
                                bluetoothSocket.getOutputStream().write("i".toString().getBytes());
                                txtBtnIluminacion = "ENCENDER";
                                btnIluminacion.setText(txtBtnIluminacion);
                                Toast.makeText(getApplicationContext(), "Iluminación Apagada", Toast.LENGTH_SHORT).show();
                            } catch (IOException ioExc){
                                Toast.makeText(getApplicationContext(),"Error al encender iluminación",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    break;
                case R.id.btnHidratacion:
                    if(txtBtnHidratacion.compareToIgnoreCase("ENCENDER") == 0){
                        try{
                            bluetoothSocket.getOutputStream().write("H".toString().getBytes());
                            txtBtnHidratacion = "APAGAR";
                            btnHidratacion.setText(txtBtnHidratacion);
                            Toast.makeText(getApplicationContext(),"Hidratación Encendida",Toast.LENGTH_SHORT).show();
                        } catch (IOException ioExc){
                            Toast.makeText(getApplicationContext(),"Error al encender hidratación",Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        try{
                            bluetoothSocket.getOutputStream().write("h".toString().getBytes());
                            txtBtnHidratacion = "ENCENDER";
                            btnHidratacion.setText(txtBtnHidratacion);
                            Toast.makeText(getApplicationContext(),"Hidratación Apagada",Toast.LENGTH_SHORT).show();
                        } catch (IOException ioExc){
                            Toast.makeText(getApplicationContext(),"Error al apagar hidratación",Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case R.id.btnObtenerDatos:
                    contentTxtDatosSensores = "Luz: 54612 lx\nTemperatura: 24.70 °C\nPresión Absoluta: 1017.34 mb (milibares)\nPresión Relativa: 1242.51 mb (milibares)\nHumedad: 51.70 %\n";
                    editTextDatosSensores.setText(contentTxtDatosSensores);
                    Toast.makeText(getApplicationContext(), "Datos Sensados Recibidos", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.btnMensajeVoz:
                    onClickImgBtnHablar();
                    Toast.makeText(getApplicationContext(),"Micrófono presionado",Toast.LENGTH_SHORT).show();
                    break;
                case  R.id.btnLimpiarDatos:
                    editTextDatosSensores.setText("");
                    Toast.makeText(getApplicationContext(),"Se han limpiado los datos de sensores",Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(getApplicationContext(),"Error ocurrido en Listener de botones",Toast.LENGTH_LONG).show();
            }
        }

    };

}
