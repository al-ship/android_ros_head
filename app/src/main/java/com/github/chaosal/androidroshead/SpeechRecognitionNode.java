package com.github.chaosal.androidroshead;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import android.widget.Toast;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;


public class SpeechRecognitionNode extends AbstractNodeMain implements RecognitionListener {
    private final static String CONTEXT = SpeechRecognitionNode.class.getSimpleName();
    public final static String TOPIC = "/speechRecognition";
    private final static String ASSETS_DIR = "ros/sphinx";
    private final static String KWS_SEARCH_NAME = "selfName";
    private final static String GRAMMAR_SEARCH = "grammar";
    private final Context context;
    private SpeechRecognizer recognizer;
    private ToneGenerator toneGenerator;

    public SpeechRecognitionNode(Context context) {
        this.context = context;
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(context.getString(R.string.nodes_prefix) + TOPIC);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        super.onStart(connectedNode);

        final File assetsDir = new File(context.getExternalFilesDir(null), ASSETS_DIR);

        try {
            recognizer = SpeechRecognizerSetup.defaultSetup().setAcousticModel(new File(assetsDir, "acc-model"))
                    .setDictionary(new File(assetsDir, "cmudict.dict"))
                            // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                    .setRawLogDir(assetsDir)
                            // Threshold to tune for keyphrase to balance between false alarms and misses
                    .setKeywordThreshold(1e-20f)
                            // Use context-independent phonetic search, context-dependent is too slow for mobile
                    .setBoolean("-allphone_ci", true)
                    .getRecognizer();

            recognizer.addListener(this);
            recognizer.addKeyphraseSearch(KWS_SEARCH_NAME, context.getString(R.string.recognitionName));
            recognizer.addGrammarSearch(GRAMMAR_SEARCH, new File(assetsDir, "grammar.gram"));

            startListen();
        } catch (IOException e) {
            Log.e("SpeechRecognitionNode", "Filed to run recognizer", e);
        }
    }

    private void startSearch()
    {
        Log.i(CONTEXT, "start search");
        recognizer.stop();
        recognizer.startListening(GRAMMAR_SEARCH, 10000);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
    }

    private void startListen()
    {
        recognizer.stop();
        recognizer.startListening(KWS_SEARCH_NAME);
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH_NAME))
            startListen();
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if(hypothesis == null)
            return;

        if(hypothesis.getHypstr().equals(context.getString(R.string.recognitionName)))
            startSearch();
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if(hypothesis == null || Math.abs(hypothesis.getBestScore()) > 2000)
            return;
        final String text = hypothesis != null ? hypothesis.getHypstr() : "null result";
        Log.i(CONTEXT, String.format("score: %d, probe: %d, result: %s",
                hypothesis.getBestScore(), hypothesis.getProb(), text));
    }

    @Override
    public void onError(Exception e) {
        Log.e(CONTEXT, e.getClass().getSimpleName() + " " + e.getMessage());
    }

    @Override
    public void onTimeout() {
        Log.i(CONTEXT, "timeout");
        startListen();
    }

    @Override
    public void onShutdown(Node node) {
        super.onShutdown(node);
        Log.i(CONTEXT, "shutdown");
        recognizer.cancel();
        recognizer.shutdown();
    }
}
