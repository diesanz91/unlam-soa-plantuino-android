package com.plantuino.unlam_soa.plantuino;

import android.content.Intent;
import android.os.Bundle;
import android.bluetooth.*;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Set;

public class ManejadorBluetoothActivity extends AppCompatActivity{

    private BluetoothAdapter bluetoothAdapter = null;
    private Button btnDispositivosBluetooth = null;
    private ListView listaDispositivosBluetooth = null;
    public  static String EXTRA_ADDRESS = "direccionMAC_dispositivo";

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manejador_bluetooth_activity);

        btnDispositivosBluetooth = (Button)findViewById(R.id.btnDispositivosBluetooth);
        listaDispositivosBluetooth=(ListView) findViewById(R.id.listViewDispositivos);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null){
            //Significa que el dispositivo no soporta Bluetooth
            Toast.makeText(getApplicationContext(), "Bluetooth no soportado", Toast.LENGTH_SHORT).show();
        } else {
            //Si dispositivo soporta Bluetooth, verificamos si se encuentra habilitado
            if(!bluetoothAdapter.isEnabled()){
                //Si no se encuentra habilitado solicitamos al usuario si desea hacerlo
                Intent habilitarBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(habilitarBluetoothIntent,1);
            }
        }

        btnDispositivosBluetooth.setOnClickListener(botonesListener);
    }

    private View.OnClickListener botonesListener= new View.OnClickListener(){

        public void onClick(View v){
            switch (v.getId()){
                case R.id.btnDispositivosBluetooth:
                    listarDispositivosBluetooth();
                    break;
                default:
                    Toast.makeText(getApplicationContext(),"Error ocurrido en Listener de botones",Toast.LENGTH_LONG).show();
            }
        }

    };

    private void listarDispositivosBluetooth(){
        //Verificamos si existe algun dispositivo sincronizado
        Set<BluetoothDevice> dispositivosBluetoothSincronizados = bluetoothAdapter.getBondedDevices();
        ArrayList listaDispositivosBluetoothSincronizados = new ArrayList();
        if(dispositivosBluetoothSincronizados.size() > 0){
            for(BluetoothDevice dispositivo : dispositivosBluetoothSincronizados){
                //A cada dispositivo sincronizado lo agrego a mi lista de dispositivos Bluetooth mediante nombre y direccion MAC del dispositivo
                listaDispositivosBluetoothSincronizados.add(dispositivo.getName() + "\n" + dispositivo.getAddress());
            }
        } else {
            Toast.makeText(getApplicationContext(),"No se encontraron dispositivos Bluetooth sincronizados",Toast.LENGTH_LONG).show();
        }

        //Armo lista de dispositivos Bluetooth sincronizados
        final ArrayAdapter dispositivosBluetoothAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, listaDispositivosBluetoothSincronizados);

        if (!dispositivosBluetoothSincronizados.isEmpty()){
            //listaDispositivosBluetooth = new ListView(getApplicationContext());
            listaDispositivosBluetooth.setAdapter(dispositivosBluetoothAdapter);
            listaDispositivosBluetooth.setOnItemClickListener(listViewListener);
        }else{
            Toast.makeText(getApplicationContext(),"No se encontraron dispositivos bluetooth sincronizados",Toast.LENGTH_LONG).show();
        }


    }

    private AdapterView.OnItemClickListener listViewListener = new AdapterView.OnItemClickListener(){
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3){
            //Obtengo la direccion MAC del item seleccionado, los ultimos 17 caracteres del item corresponden a la direccion MAC
            String info = ((TextView) v).getText().toString();
            String direccion = info.substring(info.length()-17);

            //Creamos un intent para iniciar la siguiente Activity
            Intent intent = new Intent(ManejadorBluetoothActivity.this, MainActivity.class);

            //Cambiamos de Activity
            intent.putExtra(EXTRA_ADDRESS, direccion);
            startActivity(intent);
        }
    };

}
