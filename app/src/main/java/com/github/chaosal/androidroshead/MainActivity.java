package com.github.chaosal.androidroshead;

import android.hardware.Camera;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TimingLogger;
import android.widget.Toast;

import org.ros.android.RosActivity;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class MainActivity extends RosActivity {

    private RosCameraPreviewView rosCameraPreviewView;
    private SpeakNode speakNode;
    private SensorsNode sensorsNode;
    private SpeechRecognitionNode speechRecognitionNode;
    private GlobalState globalState;

    public MainActivity() {
        super("android_ros_head", "android_ros_head");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        globalState = new GlobalState();
        sensorsNode = new SensorsNode(this.getApplicationContext(), (SensorManager) getSystemService(SENSOR_SERVICE), globalState);
        speakNode = new SpeakNode(this.getApplicationContext(), globalState);
        speechRecognitionNode = new SpeechRecognitionNode(this.getApplicationContext(), globalState);
        rosCameraPreviewView = (RosCameraPreviewView) findViewById(R.id.ros_camera_preview_view);
    }

    @Override
    public URI getMasterUri() {
        final String uri = this.getPreferences(0).getString("URI_KEY", null);
        try {
            return uri != null ? new URI(uri) : null;
        } catch (URISyntaxException e) {
            Log.e("MainActivity", e.getMessage(), e);
        }
        return null;
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
            nodeMainExecutor.execute(speechRecognitionNode, nodeConfiguration);

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
