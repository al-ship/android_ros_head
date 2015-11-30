package com.github.chaosal.androidroshead;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

import geometry_msgs.PoseStamped;

/**
 * Node for controlling and publishing messages from internal android sensors
 */
public class SensorsNode extends AbstractNodeMain{

    public final static String ORIENTATION_TOPIC = "/orientation";

    private final Context context;
    private final SensorManager sensorManager;
    private Publisher<geometry_msgs.PoseStamped> orientationPublisher;
    private OrientationListener orientationListener;

    public SensorsNode(Context applicaContext, SensorManager sensorManager){
        this.context = applicaContext;
        this.sensorManager = sensorManager;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(context.getString(R.string.nodes_prefix) + "/sensors");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        super.onStart(connectedNode);
        //Orientation
        final Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if(sensor != null) {
            orientationPublisher = connectedNode.newPublisher(GraphName.of(context.getString(R.string.nodes_prefix) + ORIENTATION_TOPIC), PoseStamped._TYPE);
            orientationListener = new OrientationListener(connectedNode, orientationPublisher);
            sensorManager.registerListener(orientationListener, sensor, 1000000);
        }
    }

    @Override
    public void onShutdown(Node node) {
        super.onShutdown(node);
        if(orientationPublisher != null)
            orientationPublisher.shutdown();
        if(orientationListener != null)
            sensorManager.unregisterListener(orientationListener);
    }

    private final class OrientationListener implements SensorEventListener{

        private ConnectedNode node;
        private Publisher<geometry_msgs.PoseStamped> publisher;

        public OrientationListener(ConnectedNode node, Publisher<geometry_msgs.PoseStamped> publisher) {
            this.node = node;
            this.publisher = publisher;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (Sensor.TYPE_ROTATION_VECTOR == event.sensor.getType()) {
                float[] quaternion = new float[4];
                SensorManager.getQuaternionFromVector(quaternion, event.values);
                if(publisher == null)
                    return;
                PoseStamped pose = publisher.newMessage();
                pose.getHeader().setFrameId("/map");
                pose.getHeader().setStamp(node.getCurrentTime());
                pose.getPose().getOrientation().setW(quaternion[0]);
                pose.getPose().getOrientation().setX(quaternion[1]);
                pose.getPose().getOrientation().setY(quaternion[2]);
                pose.getPose().getOrientation().setZ(quaternion[3]);
                publisher.publish(pose);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}
