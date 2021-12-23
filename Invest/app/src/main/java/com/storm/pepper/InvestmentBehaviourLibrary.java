package com.storm.pepper;

import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.ListenBuilder;
import com.aldebaran.qi.sdk.builder.LookAtBuilder;
import com.aldebaran.qi.sdk.builder.PhraseSetBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.builder.TransformBuilder;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.FreeFrame;
import com.aldebaran.qi.sdk.object.actuation.LookAt;
import com.aldebaran.qi.sdk.object.actuation.LookAtMovementPolicy;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
import com.aldebaran.qi.sdk.object.conversation.BodyLanguageOption;
import com.aldebaran.qi.sdk.object.conversation.Listen;
import com.aldebaran.qi.sdk.object.conversation.ListenResult;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.aldebaran.qi.sdk.object.conversation.PhraseSet;
import com.aldebaran.qi.sdk.object.conversation.QiChatVariable;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.aldebaran.qi.sdk.object.geometry.Quaternion;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.util.FutureUtils;
import com.storm.posh.BaseBehaviourLibrary;
import com.storm.posh.plan.planelements.Sense;
import com.storm.posh.plan.planelements.action.ActionEvent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class InvestmentBehaviourLibrary extends BaseBehaviourLibrary {
	//Code written by Jack Sharples
    private static final String TAG = InvestmentBehaviourLibrary.class.getSimpleName();

    private boolean atStart;
    private boolean okToStart;
    private boolean atTable;
    private boolean humanReady;
    private boolean dieRolled;
    private double rollResult;
    private boolean explained;
    private boolean understood;
    private int pounds;
    private int pence;
    private boolean played;
    private boolean goodbye;
    private boolean anthropomorphic;
    private BodyLanguageOption gestures;
    private LookAt lookAt;
    private String gameIntro;
    private String rulesIntro;
    private String experimenterString;
    private String goodbyeString;
    private Topic introTopic;
    private Topic gameTopic;

    @Override
    public void reset() {
        super.reset();
        atStart = false;
        atTable = false;
        okToStart = false;
        humanReady = false;
        dieRolled = false;
        rollResult = 0;
        explained = false;
        understood = false;
        pounds = 0;
        pence = 0;
        played = false;
        goodbye = false;

        anthropomorphic = true; //CHANGE THIS TO/FROM TRUE/FALSE (along with names in strings.xml and app gradle) TO SWITCH BETWEEN VERSIONS

        if (!anthropomorphic){
            holdAwareness();
            gestures = BodyLanguageOption.DISABLED;
            rulesIntro = "\\vct=100\\ Alright, for the second part, \\pau=500\\ you will be given 5" +
                    "euros, \\pau=500\\ and you will decide \\pau=500\\ how much" +
                    " you want to pass over to me. \\pau=500\\ The amount \\pau=500\\ is typed" +
                    "into the interface. \\pau=500\\The experimenter will triple the amount " +
                    "\\pau=500\\and i will return anything between zero euros \\pau=500\\and the tripled amount back to you." +
                    " \\pau=500\\You will find out \\pau=500\\at the end of the session \\pau=500\\how much you got. Is that clear?";
            gameIntro = "\\vct=100\\ i will keep quiet \\pau=500\\until you have made your decision. \\pau=500\\ " +
                    "Once the experimenter \\pau=500\\has left the room, \\pau=500\\just" +
                    " input the amount \\pau=500\\on the laptop, and tell me \\pau=500\\when you have finished.";
            experimenterString = "\\vct=100\\ Thank you i shall take it \\pau=500\\from here.";
            goodbyeString = "\\vct=100\\ Okay \\pau=500\\the experimenter will come \\pau=500\\back now.";
            introTopic = TopicBuilder.with(qiContext) // Create the builder using the QiContext.
                    .withResource(R.raw.investment_rules_na) // Set the topic resource.
                    .build(); // Build the topic.
            gameTopic = TopicBuilder.with(qiContext) // Create the builder using the QiContext.
                    .withResource(R.raw.investment_listen_na) // Set the topic resource.
                    .build(); // Build the topic.
        }
        else{
            gestures = BodyLanguageOption.NEUTRAL;
            rulesIntro = "Alright, for the second part, \\pau=200\\ you will be given 5" +
                    "euros, \\pau=200\\  and you'll decide how much you want to pass over to me." +
                    "\\pau=200\\ The amount is typed into the interface. \\pau=200\\The experimenter will triple the amount" +
                    "\\pau=200\\ and i'll return anything between zero euros \\pau=200\\ and the tripled amount back to you." +
                    " \\pau=200\\You'll find out at the end of the session how much you got. \\pau=200\\ Is that clear?";
            gameIntro = "I'll keep quiet until you have made your decision. \\pau=200\\ Once the experimenter has left the room" +
                    "\\pau=200\\just input the amount on the laptop, and" +
                    "tell me when you have finished.";
            experimenterString = "Thanks, I'll take it from here!";
            goodbyeString = "Okay.\\pau=200\\ The experimenter will come back now!";
            introTopic = TopicBuilder.with(qiContext) // Create the builder using the QiContext.
                    .withResource(R.raw.investment_rules) // Set the topic resource.
                    .build(); // Build the topic.
            gameTopic = TopicBuilder.with(qiContext) // Create the builder using the QiContext.
                    .withResource(R.raw.investment_listen) // Set the topic resource.
                    .build(); // Build the topic.

        }
    }

    @Override
    public boolean getBooleanSense(Sense sense) {
        //        pepperLog.appendLog(TAG, String.format("Getting boolean sense: %s", sense));
        boolean senseValue;

        switch (sense.getNameOfElement()) {
            case "AtStart":
                senseValue = atStart;
                break;
            case "OkToStart":
                senseValue = okToStart;
                break;
            case "AtTable":
                senseValue = atTable;
                break;
            case "HumanReady":
                senseValue = humanReady;
                break;
            case "DieRolled":
                senseValue = dieRolled;
                break;
            case "Explained":
                senseValue = explained;
                break;
            case "Understood":
                senseValue = understood;
                break;
            case "Played":
                senseValue = played;
                break;
            case "Goodbye":
                senseValue = goodbye;
                break;

            default:
                senseValue = super.getBooleanSense(sense);
                break;
        }

        pepperLog.checkedBooleanSense(TAG, sense, senseValue);

        return senseValue;
    }

    @Override
    public double getDoubleSense(Sense sense) {
        double senseValue;

        switch(sense.getNameOfElement()) {
            case "RollResult":
                senseValue = rollResult;
                break;

            default:
                senseValue = super.getDoubleSense(sense);
                break;
        }

        pepperLog.checkedDoubleSense(TAG, sense, senseValue);

        return senseValue;
    }

    @Override
    public void executeAction(ActionEvent action) {
		//Select the function to run based on the action pattern selected
        pepperLog.appendLog(TAG, "Performing action: " + action);

        if (action.getNameOfElement() == currentAction) {
            // already performing this action
            pepperLog.appendLog(TAG, String.format("Action still in progress: %s", currentAction));
            return;
        }

        switch (action.getNameOfElement()) {
            case "AskAboutRules":
                askAboutRules();
                break;
            case "PlayGame":
                playGame();
                break;
            case "Goodbye":
                goodbye();
                break;
            case "LookAtExperimenter":
                lookAtExperimenter();
                break;
            default:
                super.executeAction(action);
                break;
        }
    }

    public void goToStart() {
        pepperLog.appendLog("Go To Start");
        goToLocation(0);
    }

    public void approachTable() {
        pepperLog.appendLog("APPROACH TABLE");
        goToLocation(1);
    }

    public void askAboutRules() {
        pepperLog.appendLog("Ask about rules");
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

        FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
            Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                    .withText(rulesIntro) // Set the text to say.
                    .withBodyLanguageOption(gestures)
                    .build(); // Build the say action.

            say.run();


            // Create a new QiChatbot.
            QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext)
                    .withTopic(introTopic)
                    .build();

            qiChatbot.setSpeakingBodyLanguage(gestures);

            QiChatVariable chatRollResult = qiChatbot.variable("understood");

            chatRollResult.addOnValueChangedListener(currentValue -> {
                Log.i(TAG, "chatUnderstood: " + String.valueOf(currentValue));
                this.understood = Boolean.valueOf(currentValue);
                //this.reset();
            });


            // Create a new Chat action.
            chat = ChatBuilder.with(qiContext)
                    .withChatbot(qiChatbot)
                    .build();

            // Add an on started listener to the Chat action.
            chat.addOnStartedListener(() -> Log.d(TAG, "Chat started."));

            chat.setListeningBodyLanguage(gestures);

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
                pepperLog.appendLog(TAG, String.format("Understood: %s", this.understood));
                if (future.hasError()) {
                    Log.d(TAG, "Discussion finished with error.", future.getError());
                }
            });
        });
        explained = true;
    }

    public void playGame() {
        pepperLog.appendLog("Playing the investment game");
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

        FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
            Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                    .withText(gameIntro) // Set the text to say.
                    .withBodyLanguageOption(gestures)
                    .build(); // Build the say action.

            say.run();
            /*
            // Create a topic.
            Topic topic = TopicBuilder.with(qiContext) // Create the builder using the QiContext.
                    .withResource(R.raw.investment_input) // Set the topic resource.
                    .build(); // Build the topic.

            // Create a new QiChatbot.
            QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext)
                    .withTopic(topic)
                    .build();

            qiChatbot.setSpeakingBodyLanguage(gestures);

            QiChatVariable chatPounds = qiChatbot.variable("pounds");
            QiChatVariable chatPence = qiChatbot.variable("pence");

            chatPounds.addOnValueChangedListener(currentValue -> {
                Log.i(TAG, "chatPounds: " + String.valueOf(currentValue));
                this.pounds = Integer.valueOf(currentValue);
                //this.reset();
            });

            chatPence.addOnValueChangedListener(currentValue -> {
                Log.i(TAG, "chatPence: " + String.valueOf(currentValue));
                this.pence = Integer.valueOf(currentValue);
                //this.reset();
            });


            // Create a new Chat action.
            chat = ChatBuilder.with(qiContext)
                    .withChatbot(qiChatbot)
                    .build();

            chat.setListeningBodyLanguage(gestures);

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
                this.played = true;
                pepperLog.appendLog(TAG, String.format("Pounds: %s", this.pounds));
                pepperLog.appendLog(TAG, String.format("Pence: %s", this.pence));
                if (future.hasError()) {
                    Log.d(TAG, "Discussion finished with error.", future.getError());
                }
            });

             */
            // Create a topic.
            /*
            Topic topic = TopicBuilder.with(qiContext) // Create the builder using the QiContext.
                    .withResource(R.raw.investment_listen) // Set the topic resource.
                    .build(); // Build the topic.

             */

            // Create a new QiChatbot.
            QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext)
                    .withTopic(gameTopic)
                    .build();

            qiChatbot.setSpeakingBodyLanguage(gestures);
            // Create a new Chat action.
            chat = ChatBuilder.with(qiContext)
                    .withChatbot(qiChatbot)
                    .build();

            chat.setListeningBodyLanguage(gestures);

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
                this.played = true;
                if (future.hasError()) {
                    Log.d(TAG, "Discussion finished with error.", future.getError());
                }
            });
        });
    }

    public void goodbye(){
        pepperLog.appendLog("Saying goodbye");
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

        FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
            Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                    .withText(goodbyeString) // Set the text to say.
                    .withBodyLanguageOption(gestures)
                    .build(); // Build the say action.

            say.run();
        });
        this.talking = false;
        this.listening = false;
        this.goodbye = true;
    }

    public void lookAtExperimenter(){
        pepperLog.appendLog("Looking at the experimenter");
        if (talking) {
            pepperLog.appendLog(TAG,"Cannot lookAtExperimenter as already talking");
            return;
        } else if (listening) {
            pepperLog.appendLog(TAG, "Cannot lookAtExperimenter as already listening");
            return;
        }
        setActive();
        this.talking = true;
        this.listening = true;

        if (lookAtFuture != null) {
            pepperLog.appendLog(TAG, "Stop looking");
            lookAtFuture.requestCancellation();
        }
        FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
            Actuation actuation = qiContext.getActuation();
            Frame gazeFrame = actuation.gazeFrame();
            Transform transform = TransformBuilder.create().from2DTranslation(1, 0);

            // Get the Mapping service from the QiContext.
            Mapping mapping = qiContext.getMapping();

            // Create a FreeFrame with the Mapping service.
            FreeFrame targetFrame = mapping.makeFreeFrame();

            targetFrame.update(gazeFrame, transform, 0L);

            // Create a LookAt action.
            lookAt = LookAtBuilder.with(qiContext) // Create the builder with the context.
                    .withFrame(targetFrame.frame()) // Set the target frame.
                    .build(); // Build the LookAt action.
            lookAt.setPolicy(LookAtMovementPolicy.HEAD_ONLY);

            // Add an on started listener on the LookAt action.
            lookAt.addOnStartedListener(() -> Log.i(TAG, "LookAt action started."));

            if (lookAt != null) {
                lookAt.removeAllOnStartedListeners();
            }

            if (anthropomorphic){
                lookAtFuture = lookAt.async().run();
            }


            PhraseSet phraseSet = PhraseSetBuilder.with(qiContext)
                    .withTexts("Please continue")
                    .build();

            Listen listen = ListenBuilder.with(qiContext)
                    .withPhraseSet(phraseSet)
                    .withBodyLanguageOption(BodyLanguageOption.DISABLED)
                    .build();

            listen.run();

            understood = true;

            if (lookAtFuture != null && anthropomorphic) {
                lookAtFuture.requestCancellation();
            }

            Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                    .withText(experimenterString) // Set the text to say.
                    .withBodyLanguageOption(gestures)
                    .build(); // Build the say action.

            say.run();

            this.talking = false;
            this.listening = false;

        });
    }

    @Override
    protected void locationReached(int id) {
        pepperLog.appendLog(TAG, String.format("Reached: %d", id));
        if (id == 0) {
            reachedStart();
        } else if (id == 1) {
            reachedTable();
        } else {
            pepperLog.appendLog(TAG, "Unknown location reached");
        }
    }

    private void reachedStart() {
        this.atStart = true;
        pepperLog.appendLog(TAG, String.format("Reached: Start"));
    }

    private void reachedTable() {
        this.atTable = true;
        pepperLog.appendLog(TAG, String.format("Reached: Table"));
        lookAtHuman();
    }

    public void checkReady() {
        pepperLog.appendLog("CHECK READY");
        if (talking) {
            pepperLog.appendLog(TAG,"Cannot checkReady as already talking");
            return;
        } else if (listening) {
            pepperLog.appendLog(TAG, "Cannot checkReady as already listening");
            return;
        }

        setActive();

        this.talking = true;
        this.listening = true;

        FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
            Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                    .withText("Are you ready to continue?") // Set the text to say.
                    .build(); // Build the say action.
//                    .withBodyLanguageOption(BodyLanguageOption.DISABLED)

            say.run();

            this.talking = false;

            FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore2) -> {
                // TODO: Only init this once
                PhraseSet phraseSet = PhraseSetBuilder.with(qiContext).withTexts("Yes", "No").build();
                Listen listen = ListenBuilder.with(qiContext).withPhraseSet(phraseSet).build();

                this.listenFuture = listen.async().run();

                listenFuture.thenConsume(future -> {
                    this.listening = false;

                    try {
                        ListenResult result = future.get();

                        Phrase heardPhrase = result.getHeardPhrase();

                        if (heardPhrase.getText().equals("Yes")) {
                            this.humanReady = true;
                        }

                        listenFuture.requestCancellation();

                    } catch (ExecutionException e) {
                        pepperLog.appendLog(TAG, "Error occurred when listening for answer");
                    } catch (CancellationException e) {
                        pepperLog.appendLog(TAG, "Listening for answer was cancelled");
                    }
                });
            });
        });
    }
}
