package com.plantuino.unlam_soa.plantuino;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;



public class CalibracionLuz extends AppCompatActivity implements SensorEventListener {

    private EditText txtLuz = null;
    private Switch swtLuz = null;
    private Button btnOk = null;
    private boolean SensorMovil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibracion_luz);

        Intent intent = getIntent();

        txtLuz = (EditText) findViewById(R.id.txtLuz);
        swtLuz = (Switch) findViewById(R.id.swtLuz);
        btnOk = (Button) findViewById(R.id.btnOk);

        //Si el sensor luz del movil esta activado para sensar pone el Switch en True
        swtLuz.setChecked(intent.getStringExtra("SensorMovil").equals("S"));

        swtLuz.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SensorMovil = isChecked;
            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent resultado = new Intent(CalibracionLuz.this, MainActivity.class);
                resultado.putExtra("SensorMovil",SensorMovil);
                setResult(RESULT_OK, resultado);
                finish();
            }
        });

        //Inicialización de objeto para sensor de luz
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if(sensorManager != null){
            Sensor sensorLuz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            //Registro listener al sensor de luz
            sensorManager.registerListener(this,sensorLuz,SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getApplicationContext(),"Ocurrio un error al obtener sensor de luz",Toast.LENGTH_SHORT);
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
                case Sensor.TYPE_LIGHT:

                    float luz = evento.values[0];
                    txtLuz.setText(luz + " lx");

                    break;
                default:
                    break;
            }
        }
    }



}
