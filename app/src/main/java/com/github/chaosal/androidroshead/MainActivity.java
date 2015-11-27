package com.github.chaosal.androidroshead;

import android.hardware.Camera;
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

    public MainActivity(String notificationTicker, String notificationTitle) {
        super(notificationTicker, notificationTitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rosCameraPreviewView = (RosCameraPreviewView) findViewById(R.id.ros_camera_preview_view);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        if(Camera.getNumberOfCameras() > 0)
            rosCameraPreviewView.setCamera(Camera.open());
        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
            nodeMainExecutor.execute(rosCameraPreviewView, nodeConfiguration);
        } catch (IOException e) {
            // Socket problem
            Log.e("android_ros_head", "socket error trying to get networking information from the master uri");
        }
    }
}
