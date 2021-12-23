package com.storm.pepper;

import android.util.Log;

import com.aldebaran.qi.Future;
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

public class HonestyBehaviourLibrary extends BaseBehaviourLibrary {
    private static final String TAG = HonestyBehaviourLibrary.class.getSimpleName();

    //booleans used for the senses
    private boolean humanReady;
    private boolean rules;
    private boolean transition;


    @Override
    public void reset() {
        super.reset();

        humanReady = false;
        rules = false;
        transition = false;
    }

    @Override
    public boolean getBooleanSense(Sense sense) {
        //        pepperLog.appendLog(TAG, String.format("Getting boolean sense: %s", sense));
        boolean senseValue;

        switch (sense.getNameOfElement()) {
            //cases being triggered by senses of Pepper
            case "HumanReady":
                senseValue = humanReady;
                break;
            case "Rules":
                senseValue = rules;
                break;
            case "End":
                senseValue = transition;
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
            //method calls for each of the cases representing the drives
            case "Introduction":
                rules();
                break;
            case "Dice":
                honestyGame();
                break;
            case "Transition":
                transition();
                break;

            default:
                super.executeAction(action);
                break;
        }
    }

    public void rules() {
        pepperLog.appendLog(TAG, "starting introduction");
        if (talking) {
            pepperLog.appendLog(TAG,"Cannot askForResult as already talking");
            return;
        } else if (listening) {
            pepperLog.appendLog(TAG, "Cannot askForResult as already listening");
            return;
        }

        setActive();
        this.talking = true;
        this.listening = true;

        if (lookAtFuture != null) {
            pepperLog.appendLog(TAG, "Stop looking");
            lookAtFuture.requestCancellation();
        }

        if (!humanPresent && humanPresent) {
            pepperLog.appendLog(TAG, "no human present");
            return;
        } else {
            FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
                Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                        .withText("Alright, let's get started. \\pau=200\\ For the" +
                                "first part, you'll just roll the dice \\pau=200\\ and type the number into" +
                                "the interface on the screen. \\pau=200\\  The interface also shows you" +
                                "the payoff rates. \\pau=200\\ You can roll the dice more than once \\pau=200\\if you" +
                                "want to check that the dice is fair,\\pau=200\\ but only the number of your first" +
                                "throw counts. \\pau=200\\ Is this clear?") // Set the text to say.
                        .build(); // Build the say action.
                    //engaging body language set to default level by QiSDK
                say.run();

                // creates the topic.
                Topic topic = TopicBuilder.with(qiContext) // Create the builder using the QiContext.
                        .withResource(R.raw.honesty_rules) // Set the topic resource.
                        .build(); // Build the topic.

                // Create a new QiChatbot.
                QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext)
                        .withTopic(topic)
                        .build();
                //creates a Qi variable to use during the conversation
                QiChatVariable rulesBool = qiChatbot.variable("rules");

                rulesBool.addOnValueChangedListener(currentValue -> {
                    Log.i(TAG, "chatRules: " + String.valueOf(currentValue));
                    this.rules = Boolean.valueOf(currentValue);
                });

                // Create a new Chat action.
                chat = ChatBuilder.with(qiContext)
                        .withChatbot(qiChatbot)
                        .build();

                //listener
                chat.addOnStartedListener(() -> Log.d(TAG, "chat started"));

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
                    //append to log result of flag for error testing
                    pepperLog.appendLog(TAG, String.format("Rules: %s", this.rules));
                    if (future.hasError()) {
                        Log.d(TAG, "Discussion finished with error.", future.getError());
                    }
                });
            });
        }
    }

    public void honestyGame() {
        //in case unexpected input occurs
        pepperLog.appendLog("Playing the honesty game");
        if (talking) {
            pepperLog.appendLog(TAG,"Cannot askForResult as already talking");
            return;
        } else if (listening) {
            pepperLog.appendLog(TAG, "Cannot askForResult as already listening");
            return;
        }

        setActive();
        this.talking = true;
        this.listening = true;

        //for asynchronous chat
        if (lookAtFuture != null) {
            pepperLog.appendLog(TAG, "Stop looking");
            lookAtFuture.requestCancellation();
        }

        //future and use of say from Base library to interact with participant
        FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
            Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                    .withText("Let me know when you are done.") // Set the text to say.
                    .build(); // Build the say action.

            say.run();

            // Create a topic.
            Topic topic = TopicBuilder.with(qiContext) // Create the builder using the QiContext.
                    .withResource(R.raw.game_rules) // Set the topic resource.
                    .build(); // Build the topic.

            // Create a new QiChatbot.
            QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext)
                    .withTopic(topic)
                    .build();

            //same as above drive
            QiChatVariable transitionBool = qiChatbot.variable("transition");

            transitionBool.addOnValueChangedListener(currentValue -> {
                Log.i(TAG, "chatTransition: " + String.valueOf(currentValue));
                this.transition = Boolean.valueOf(currentValue);
                //this.reset();
            });


            // Create a new Chat action.
            chat = ChatBuilder.with(qiContext)
                    .withChatbot(qiChatbot)
                    .build();

            // add a listener to the Chat action from when the chat starts
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
                //both set to false for next drive
                this.talking = false;
                this.listening = false;
                //append to log for error testing
                pepperLog.appendLog(TAG, String.format("End: %s", this.transition));
                if (future.hasError()) {
                    Log.d(TAG, "Discussion finished with error.", future.getError());
                }
            });
        });
    }
    public void transition(){
        if (lookAtFuture != null) {
            pepperLog.appendLog(TAG, "Stop looking");
            lookAtFuture.requestCancellation();
        }
        //make sure Pepper has stopped talking from previous one and there is no lag from previous drive
        pepperLog.appendLog("Transitioning to next experiment");
        FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
            Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                    .withText("The experimenter will now explain the next part.") // Set the text to say.
                    .build(); // Build the say action.
            say.run();
            //end
        });
        //set the flags to false for potential reuse in a paradigm following it
        transition = false;
        humanReady = false;
        rules = true;
    }





}
