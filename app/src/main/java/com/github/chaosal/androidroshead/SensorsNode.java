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

import org.ros.concurrent.CancellableLoop;
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
    public final static byte BATTERY_LOW = 15;

    private final Context context;
    private final SensorManager sensorManager;
    private float[] orientationQ = new float[4];
    private Object orientationLock = new Object();
    private Publisher<PoseStamped> orientationPublisher;
    private Publisher<DiagnosticStatus> batteryPublisher;
    private OrientationListener orientationListener;
    private BatteryListener batteryListener;
    private CancellableLoop orientationLoop;
    private Publisher<std_msgs.String> speakPublisher;

    public SensorsNode(Context applicaContext, SensorManager sensorManager) {
        this.context = applicaContext;
        this.sensorManager = sensorManager;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(context.getString(R.string.nodes_prefix) + "/sensors");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        super.onStart(connectedNode);
        //Orientation
        final Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (sensor != null) {
            orientationPublisher = connectedNode.newPublisher(GraphName.of(context.getString(R.string.nodes_prefix) + ORIENTATION_TOPIC), PoseStamped._TYPE);
            orientationListener = new OrientationListener(connectedNode, orientationPublisher);
            sensorManager.registerListener(orientationListener, sensor, SensorManager.SENSOR_DELAY_UI);
            orientationLoop = new CancellableLoop() {//publishes not so fast
                @Override
                protected void loop() throws InterruptedException {

                    Thread.sleep(1000);
                    if (orientationPublisher == null)
                        return;
                    final PoseStamped pose = orientationPublisher.newMessage();
                    pose.getHeader().setFrameId("/map");
                    pose.getHeader().setStamp(connectedNode.getCurrentTime());
                    synchronized (orientationLock) {
                        pose.getPose().getOrientation().setW(orientationQ[0]);
                        pose.getPose().getOrientation().setX(orientationQ[1]);
                        pose.getPose().getOrientation().setY(orientationQ[2]);
                        pose.getPose().getOrientation().setZ(orientationQ[3]);
                    }
                    orientationPublisher.publish(pose);
                }
            };
            connectedNode.executeCancellableLoop(orientationLoop);
        }
        //Battery
        batteryPublisher = connectedNode.newPublisher(GraphName.of(context.getString(R.string.nodes_prefix) + BATTERY_TOPIC), DiagnosticStatus._TYPE);
        batteryListener = new BatteryListener(batteryPublisher);
        final Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        batteryPublisher.publish(batteryListener.convertIntent(batteryIntent));//publish current
        context.registerReceiver(batteryListener, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        speakPublisher = connectedNode.newPublisher(GraphName.of(context.getString(R.string.nodes_prefix) + SpeakNode.TOPIC), std_msgs.String._TYPE);

    }

    @Override
    public void onShutdown(Node node) {
        super.onShutdown(node);

        if(orientationLoop != null)
            orientationLoop.cancel();
        if (orientationListener != null)
            sensorManager.unregisterListener(orientationListener);
        if (orientationPublisher != null)
            orientationPublisher.shutdown();
        if (batteryListener != null)
            context.unregisterReceiver(batteryListener);
        if (batteryPublisher != null)
            batteryPublisher.shutdown();

    }

    private void setOrientationQ(float[] q) {
        synchronized (orientationLock) {
            this.orientationQ = q;
        }
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
                setOrientationQ(quaternion);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    private final class BatteryListener extends BroadcastReceiver {

        private final Publisher<DiagnosticStatus> batteryPublisher;


        public BatteryListener(Publisher<DiagnosticStatus> batteryPublisher) {

            this.batteryPublisher = batteryPublisher;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            final DiagnosticStatus message = convertIntent(intent);
            batteryPublisher.publish(message);

            if (message.getLevel() < BATTERY_LOW) {
                final std_msgs.String messageLow = speakPublisher.newMessage();
                messageLow.setData(context.getString(R.string.charge_me));
                speakPublisher.publish(messageLow);
            }
        }

        public DiagnosticStatus convertIntent(Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            byte batteryPct = (byte) Math.round(level * 100 / (float) scale);

            final DiagnosticStatus message = batteryPublisher.newMessage();
            message.setName(isCharging ? BATTERY_CHARGING : BATTERY_DISCHARGING);
            message.setLevel(batteryPct);

            return message;
        }
    }
}
