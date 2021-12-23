...
public void holdAwareness() {
	// Build and store the holder for the abilities.
	// Modified by Jack to limit additional anthropomorhic behaviours
	holder = HolderBuilder.with(qiContext)
			.withAutonomousAbilities(
					AutonomousAbilitiesType.BACKGROUND_MOVEMENT,
					AutonomousAbilitiesType.BASIC_AWARENESS,
					AutonomousAbilitiesType.AUTONOMOUS_BLINKING
			)
		.build();

	// Hold the abilities asynchronously.
	Future<Void> holdFuture = holder.async().hold();

	// Chain the hold with a lambda on the UI thread.
	holdFuture.andThenConsume(Qi.onUiThread((Consumer<Void>) ignore -> {
		// Store the abilities status.
		abilitiesHeld = true;
	}));
}
...