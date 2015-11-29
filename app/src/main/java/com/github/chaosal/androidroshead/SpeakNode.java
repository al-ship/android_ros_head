package com.github.chaosal.androidroshead;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

import java.util.Locale;


public class SpeakNode extends AbstractNodeMain implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private Subscriber<std_msgs.String> speakSubscriber;
    private Context context;

    public SpeakNode(Context applicationContext) {
        this.context = applicationContext;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android_ros_head/speak");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        super.onStart(connectedNode);
        speakSubscriber =
                connectedNode.newSubscriber("android_ros_head/speak", std_msgs.String._TYPE);
        tts = new TextToSpeech(context, this);
        tts.setLanguage(new Locale("ru"));

    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) {
            tts.speak("Привет", TextToSpeech.QUEUE_FLUSH, null);
            speakSubscriber.addMessageListener(new MessageListener<std_msgs.String>() {
                @Override
                public void onNewMessage(std_msgs.String string) {
                    tts.speak(string.getData(), TextToSpeech.QUEUE_FLUSH, null);
                }
            });
        }
        else
            Toast.makeText(context, "error tts init", Toast.LENGTH_SHORT);
    }

    @Override
    public void onShutdown(Node node) {
        super.onShutdown(node);
        speakSubscriber.shutdown();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        super.onError(node, throwable);
        Toast.makeText(context, throwable.getMessage(), Toast.LENGTH_SHORT);
    }
}
