package com.github.chaosal.androidroshead;

import android.hardware.Camera;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.ros.android.RosActivity;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.IOException;

public class MainActivity extends RosActivity {

    private RosCameraPreviewView rosCameraPreviewView;
    private SpeakNode speakNode;
    private SensorsNode sensorsNode;

    public MainActivity() {
        super("android_ros_head", "android_ros_head");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorsNode = new SensorsNode(this.getApplicationContext(), (SensorManager) getSystemService(SENSOR_SERVICE));
        speakNode = new SpeakNode(this.getApplicationContext());
        rosCameraPreviewView = (RosCameraPreviewView) findViewById(R.id.ros_camera_preview_view);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        if (Camera.getNumberOfCameras() > 0)
            rosCameraPreviewView.setCamera(Camera.open());
        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());

            nodeMainExecutor.execute(sensorsNode, nodeConfiguration);
            nodeMainExecutor.execute(speakNode, nodeConfiguration);
            nodeMainExecutor.execute(rosCameraPreviewView, nodeConfiguration);
        } catch (IOException e) {
            // Socket problem
            Log.e("MainActivity", getString(R.string.socket_error));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
