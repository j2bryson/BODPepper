package com.storm.pepper;

import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.ListenBuilder;
import com.aldebaran.qi.sdk.builder.PhraseSetBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.conversation.BodyLanguageOption;
import com.aldebaran.qi.sdk.object.conversation.Listen;
import com.aldebaran.qi.sdk.object.conversation.ListenResult;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.aldebaran.qi.sdk.object.conversation.PhraseSet;
import com.aldebaran.qi.sdk.object.conversation.QiChatVariable;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.aldebaran.qi.sdk.util.FutureUtils;
import com.storm.posh.BaseBehaviourLibrary;
import com.storm.posh.plan.planelements.Sense;
import com.storm.posh.plan.planelements.action.ActionEvent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PepperIntroductionLibrary extends BaseBehaviourLibrary {
    private static final String TAG = PepperIntroductionLibrary.class.getSimpleName();

    private boolean humanReady;
    private boolean okToStart;


    @Override
    public void reset() {
        super.reset();

        humanReady = false;
        okToStart = false;
    }

    @Override
    public boolean getBooleanSense(Sense sense) {
        //        pepperLog.appendLog(TAG, String.format("Getting boolean sense: %s", sense));
        boolean senseValue;

        switch (sense.getNameOfElement()) {
            case "HumanReady":
                senseValue = humanReady;
                break;
            case "OkToStart":
                senseValue = okToStart;
                break;

            default:
                senseValue = super.getBooleanSense(sense);
                break;
        }

        pepperLog.checkedBooleanSense(TAG, sense, senseValue);

        return senseValue;
    }

    @Override
    public void executeAction(ActionEvent action) {
        pepperLog.appendLog(TAG, "Performing action: " + action);

        if (action.getNameOfElement() == currentAction) {
            // already performing this action
            pepperLog.appendLog(TAG, String.format("Action still in progress: %s", currentAction));
            return;
        }

        switch (action.getNameOfElement()) {

            case "IntroduceToHuman":
                introduceToHuman();
                break;

            case "ApproachTable":
                approachTable();
                break;

            default:
                super.executeAction(action);
                break;
        }
    }


    public void approachTable() {
        pepperLog.appendLog("APPROACH TABLE");
        goToLocation(1);
        okToStart = false;
    }

    public void introduceToHuman() {
        pepperLog.appendLog(TAG, "Starts introducing to human");
        //Turning off autonomous abilities (JACK DID THIS BIT JUST FOR TESTING BTW)
        holdAwareness();
        if (!humanPresent) {
            pepperLog.appendLog(TAG, "Cannot approach when no human present");
            return;
        } else {
            FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
                Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                        .withText("Hello, I am Pepper!") // Set the text to say.
                        .withBodyLanguageOption(BodyLanguageOption.DISABLED)
                        .build(); // Build the say action.

                say.run();

                // Create a topic.
                Topic topic = TopicBuilder.with(qiContext) // Create the builder using the QiContext.
                        .withResource(R.raw.greet_participant) // Set the topic resource.
                        .build(); // Build the topic.

                // Create a new QiChatbot.
                QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext)
                        .withTopic(topic)
                        .build();

                //Turn off animations when speaking JACK
                qiChatbot.setSpeakingBodyLanguage(BodyLanguageOption.DISABLED);

                // Create a new Chat action.
                chat = ChatBuilder.with(qiContext)
                        .withChatbot(qiChatbot)
                        .build();

                //Turn off animations when listening JACK
                chat.setListeningBodyLanguage(BodyLanguageOption.DISABLED);

                // Add an on started listener to the Chat action.
                chat.addOnStartedListener(() -> Log.d(TAG, "Chat started."));

                // Run the Chat action asynchronously.
                Future<Void> chatFuture = chat.async().run();

                // Stop the chat when done
                qiChatbot.addOnEndedListener(endReason -> {
                    pepperLog.appendLog(TAG, String.format("Chat ended: %s", endReason));
                    chatFuture.requestCancellation();
                });

                // Add a lambda to the action execution.
                chatFuture.thenConsume(future -> {
                    pepperLog.appendLog(TAG, "Chat completed?");
                    this.talking = false;
                    this.listening = false;
                    okToStart = true;
                    if (future.hasError()) {
                        Log.d(TAG, "Discussion finished with error.", future.getError());
                    }
                });
            });
        }
    }
}
