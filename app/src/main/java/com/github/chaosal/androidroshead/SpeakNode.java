package com.github.chaosal.androidroshead;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

import std_msgs.String;


public class SpeakNode extends AbstractNodeMain implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private Subscriber<std_msgs.String> speakSubscriber;
    private Context context;

    public SpeakNode(Context applicationContext) {
        this.context = applicationContext;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("Speak");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        super.onStart(connectedNode);
        speakSubscriber =
                connectedNode.newSubscriber("android_ros_head/speak", String._TYPE);
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        speakSubscriber.addMessageListener(new MessageListener<String>() {
            @Override
            public void onNewMessage(String string) {
                tts.speak(string.getData(), TextToSpeech.QUEUE_FLUSH, null);
            }
        });
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
}
