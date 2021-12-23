package com.storm.pepper;

import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.object.conversation.BodyLanguageOption;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.aldebaran.qi.sdk.util.FutureUtils;
import com.storm.posh.BaseBehaviourLibrary;
import com.storm.posh.plan.planelements.Sense;
import com.storm.posh.plan.planelements.action.ActionEvent;

import java.util.concurrent.TimeUnit;

/**
 * This class provides the behaviours that make Pepper seem less anthropomorphic.
 *
 * @author Vasile Perciuleac
 */

public class LessAnthropomorphicWelcomeLibrary extends BaseBehaviourLibrary {
    private static final String TAG = MoreAnthropomorphicWelcomeLibrary.class.getSimpleName();

    private boolean humanReady;
    private boolean introductionHappened;
    private boolean atTable;


    @Override
    public void reset() {
        super.reset();

        humanReady = false;
        introductionHappened = false; //used to know when the introductory conversation has finished
        atTable = false;
    }

    @Override
    public boolean getBooleanSense(Sense sense) {
        //pepperLog.appendLog(TAG, String.format("Getting boolean sense: %s", sense));
        boolean senseValue;

        switch (sense.getNameOfElement()) {
            case "HumanReady":
                senseValue = humanReady;
                break;

            case "IntroductionHappened":
                senseValue = introductionHappened;
                break;

            default:
                senseValue = super.getBooleanSense(sense);
                break;
        }

        pepperLog.checkedBooleanSense(TAG, sense, senseValue);

        return senseValue;
    }

    @Override
    protected void locationReached(int id) {
        pepperLog.appendLog(TAG, String.format("Reached: %d", id));
        if (id == 0) {
            reachedTable();
        } else {
            pepperLog.appendLog(TAG, "Unknown location reached");
        }
    }

    private void reachedTable() {
        this.atTable = true;
        pepperLog.appendLog(TAG, String.format("Reached: Table"));
        lookAtHuman();
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

            case "GoToTable":
                goToTable();
                break;

            default:
                super.executeAction(action);
                break;
        }
    }

    public void goToTable() {
        pepperLog.appendLog("Started going to table");
        goToLocation(0);
    }

    /** Instructs Pepper to start introducing itself to the study participant in a
     * less anthropomorphic way
     * **/
    public void introduceToHuman() {

        pepperLog.appendLog(TAG, "Starts introducing to human");
        if (!humanPresent && humanPresent) {
            pepperLog.appendLog(TAG, "Cannot approach when no human present");
            return;
        } else {
            FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
                holdAwareness();
                Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                        .withText("\\vct=100\\ \\readmode=word\\ Hello.") // Set the text to say.
                        .withBodyLanguageOption(BodyLanguageOption.DISABLED)  //text too small
                        .build(); // Build the say action.

                say.run();

                // Create a topic.
                Topic topic = TopicBuilder.with(qiContext) // Create the builder using the QiContext.
                        .withResource(R.raw.greetparticipantlessanthropomorphically) // Set the topic resource.
                        .build(); // Build the topic.

                // Create a new QiChatbot.
                QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext)
                        .withTopic(topic)
                        .build();

                qiChatbot.setSpeakingBodyLanguage(BodyLanguageOption.DISABLED);

                // Create a new Chat action.
                chat = ChatBuilder.with(qiContext)
                        .withChatbot(qiChatbot)
                        .build();

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
                    this.introductionHappened = true;
                    if (future.hasError()) {
                        Log.d(TAG, "Discussion finished with error.", future.getError());
                    }
                });
            });

        }
    }
}
