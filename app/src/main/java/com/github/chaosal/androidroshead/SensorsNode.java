package com.github.chaosal.androidroshead;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

import diagnostic_msgs.DiagnosticStatus;
import geometry_msgs.PoseStamped;

/**
 * Node for controlling and publishing messages from internal android sensors
 */
public class SensorsNode extends AbstractNodeMain {

    public final static String ORIENTATION_TOPIC = "/orientation";
    public final static String BATTERY_TOPIC = "/battery";
    public final static String BATTERY_CHARGING = "charging";
    public final static String BATTERY_DISCHARGING = "discharging";

    private final Context context;
    private final SensorManager sensorManager;
    private Publisher<PoseStamped> orientationPublisher;
    private Publisher<DiagnosticStatus> batteryPublisher;
    private OrientationListener orientationListener;
    private BatteryListener batteryListener;

    public SensorsNode(Context applicaContext, SensorManager sensorManager) {
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
        if (sensor != null) {
            orientationPublisher = connectedNode.newPublisher(GraphName.of(context.getString(R.string.nodes_prefix) + ORIENTATION_TOPIC), PoseStamped._TYPE);
            orientationListener = new OrientationListener(connectedNode, orientationPublisher);
            sensorManager.registerListener(orientationListener, sensor, 1000000);
        }
        //Battery
        batteryPublisher = connectedNode.newPublisher(GraphName.of(context.getString(R.string.nodes_prefix) + BATTERY_TOPIC), DiagnosticStatus._TYPE);
        batteryListener = new BatteryListener(batteryPublisher);
        final Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        batteryPublisher.publish(batteryListener.convertIntent(batteryIntent));//publish current
        context.registerReceiver(batteryListener, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onShutdown(Node node) {
        super.onShutdown(node);
        if (orientationListener != null)
            sensorManager.unregisterListener(orientationListener);
        if (orientationPublisher != null)
            orientationPublisher.shutdown();
        if (batteryListener != null)
            context.unregisterReceiver(batteryListener);
        if (batteryPublisher != null)
            batteryPublisher.shutdown();

    }

    private final class OrientationListener implements SensorEventListener {

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
                if (publisher == null)
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

    private final class BatteryListener extends BroadcastReceiver {

        private Publisher<DiagnosticStatus> batteryPublisher;

        public BatteryListener(Publisher<DiagnosticStatus> batteryPublisher) {

            this.batteryPublisher = batteryPublisher;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            final DiagnosticStatus message = convertIntent(intent);
            batteryPublisher.publish(message);
        }

        public DiagnosticStatus convertIntent(Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            byte batteryPct = (byte) (level / (float) scale);

            final DiagnosticStatus message = batteryPublisher.newMessage();
            message.setName(isCharging ? BATTERY_CHARGING : BATTERY_DISCHARGING);
            message.setLevel(batteryPct);

            return message;
        }
    }
}
