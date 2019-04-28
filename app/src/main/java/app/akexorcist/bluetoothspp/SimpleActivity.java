/*
 * Copyright 2014 Akexorcist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.akexorcist.bluetoothspp;

import android.annotation.TargetApi;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.BluetoothConnectionListener;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.OnDataReceivedListener;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class SimpleActivity extends Activity implements SensorEventListener {
	BluetoothSPP bt;

	///////
	int angle;
	ImageView compass_img;
	TextView txt_compass;
	Sensor gyroscope,rotationVectorSensor;
	private SensorManager mSensorManager;

	float[] rotationMatrix = new float[16];
	float[] remappedRotationMatrix = new float[16];
	float[] orientations = new float[3];
	//////////
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_simple);

		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		rotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		
		bt = new BluetoothSPP(this);
		compass_img = (ImageView) findViewById(R.id.img_compass);
		txt_compass = (TextView) findViewById(R.id.txt_azimuth);

		if(!bt.isBluetoothAvailable()) {
			Toast.makeText(getApplicationContext()
					, "Bluetooth is not available"
					, Toast.LENGTH_SHORT).show();
            finish();
		}
		
		bt.setOnDataReceivedListener(new OnDataReceivedListener() {
			public void onDataReceived(byte[] data, String message) {
				Toast.makeText(SimpleActivity.this, message, Toast.LENGTH_SHORT).show();
			}
		});
		
		bt.setBluetoothConnectionListener(new BluetoothConnectionListener() {
			public void onDeviceConnected(String name, String address) {
				Toast.makeText(getApplicationContext()
						, "Connected to " + name + "\n" + address
						, Toast.LENGTH_SHORT).show();
			}

			public void onDeviceDisconnected() {
				Toast.makeText(getApplicationContext()
						, "Connection lost", Toast.LENGTH_SHORT).show();
			}

			public void onDeviceConnectionFailed() {
				Toast.makeText(getApplicationContext()
						, "Unable to connect", Toast.LENGTH_SHORT).show();
			}
		});
		
		Button btnConnect = (Button)findViewById(R.id.btnConnect);
		btnConnect.setOnClickListener(new OnClickListener(){
        	public void onClick(View v){
        		if(bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
        			bt.disconnect();
        		} else {
                    Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE); 
        		}
        	}
        });






	}
	
	public void onDestroy() {
        super.onDestroy();
        bt.stopService();
    }
	
	public void onStart() {
        super.onStart();
        if (!bt.isBluetoothEnabled()) {
        	Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if(!bt.isServiceAvailable()) { 
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                setup();
            }
        }
    }
	
	public void setup() {
		Button btnSend = (Button)findViewById(R.id.btnSend);
		btnSend.setOnClickListener(new OnClickListener(){
        	public void onClick(View v){
        		bt.send("Text", true);
        	}
        });
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
			if(resultCode == Activity.RESULT_OK)
                bt.connect(data);
		} else if(requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                setup();
            } else {
                Toast.makeText(getApplicationContext()
                		, "Bluetooth was not enabled."
                		, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    ///////////////

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
			SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
			SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedRotationMatrix);
			SensorManager.getOrientation(remappedRotationMatrix, orientations);
			orientations[2] = (float)(Math.toDegrees(orientations[2]));
			//android:background="@drawable/bg1"

			//bt.send( String.valueOf((int) orientations[2]+180), true);
			angle = ((int) orientations[2])+180;
			bt.send( String.valueOf(angle), true);
			txt_compass.setText(angle+"°");

			//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				//compass_img.setRotation(-angle);
			//}
			//txt_compass.setText("");
		}


		//texto.setText((int)orientations[2]);
	}
	public void onAccuracyChanged(Sensor sensor, int accuracy ) {}

	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
	}

	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}




	///////



 }
