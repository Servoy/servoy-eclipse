/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.ui.internal.workbench.swt;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.swt.widgets.Display;

/**
 * This job creates an Animation Engine that uses an Animation Feedback to
 * render the animation. To begin the animation, instantiate this object then
 * call schedule().
 *
 * @since 3.3
 *
 */
public class AnimationEngine extends Job {
	public static final int TICK_TIMER = 1;
	public static final int FRAME_COUNT = 2;
	public static final int unlimitedDuration = -1;

	private boolean enableAnimations;
	private long startTime;
	private long curTime;
	private long prevTime;
	private int timingStyle = TICK_TIMER;
	private long frameCount;
	private boolean animationCanceled = false;
	private long sleepAmount;

	private Display display;
	private AnimationFeedbackBase feedbackRenderer;
	private int duration;

	public AnimationEngine(IEclipseContext context,
			AnimationFeedbackBase animationFeedback, int durationIn) {
		this(context, animationFeedback, durationIn, 0);
	}

	/**
	 * Creates an Animation that will run for the given number of milliseconds.
	 *
	 * @param animationFeedback
	 *            provides renderStep(), initialize() and jobInit() methods
	 * @param durationIn
	 *            number of milliseconds over which the animation will run
	 * @param sleepAmountIn
	 *            number of milliseconds to slow/delay the animation
	 */
	public AnimationEngine(IEclipseContext context,
			AnimationFeedbackBase animationFeedback, int durationIn,
			long sleepAmountIn) {
		super("E4 Animation");
		sleepAmount = sleepAmountIn;
		feedbackRenderer = animationFeedback;
		duration = durationIn;

		// if animations aren't on this is a NO-OP
		enableAnimations = false;
		if (context.get(IPresentationEngine.ANIMATIONS_ENABLED) != null)
			enableAnimations = (Boolean) context
					.get(IPresentationEngine.ANIMATIONS_ENABLED);
		if (!enableAnimations) {
			return;
		}

		animationCanceled = false;

		// Capture parameters
		display = feedbackRenderer.getAnimationShell().getDisplay();

		animationFeedback.getAnimationShell().addDisposeListener(
				e -> cancelAnimation());

		// Don't show the job in monitors
		setSystem(true);

		// Set it up
		feedbackRenderer.initialize(this);

		// Set the animation's initial state
		curTime = startTime = System.currentTimeMillis();

	}

	/**
	 * @return The current renderer
	 */
	public AnimationFeedbackBase getFeedback() {
		return feedbackRenderer;
	}

	private Runnable animationStep = () -> {
		if (animationCanceled)
			return;

		// Capture time
		prevTime = curTime;
		curTime = System.currentTimeMillis();

		if (isUpdateStep()) {
			updateDisplay();
			frameCount++;
		}
	};

	protected void updateDisplay() {
		if (animationCanceled)
			return;

		feedbackRenderer.renderStep(this);
	}

	protected boolean isUpdateStep() {
		if (duration == unlimitedDuration || timingStyle == FRAME_COUNT) {
			return true;
		}

		// Default to 'TICK_TIMER', update when the system timer changes
		return prevTime != curTime;
	}

	private boolean done() {
		return animationCanceled || amount() >= 1.0;
	}

	public double amount() {
		if (duration == unlimitedDuration) {
			return 0;
		}
		double amount = 0.0;
		switch (timingStyle) {
		case TICK_TIMER:
			amount = (double) (curTime - startTime) / (double) duration;
			break;

		// For testing purposes
		case FRAME_COUNT:
			amount = (double) frameCount / (double) duration;
		}

		if (amount > 1.0)
			amount = 1.0;

		return amount;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		// We use preference value to indicate that the animation should be
		// skipped on this platform.
		if (!enableAnimations) {
			return Status.OK_STATUS;
		}

		// We're starting, initialize
		display.syncExec(() -> {
			// 'jobInit' returns 'false' if it doesn't want to run...
			if (!animationCanceled)
				animationCanceled = !feedbackRenderer
						.jobInit(AnimationEngine.this);
		});

		if (animationCanceled)
			return Status.CANCEL_STATUS;

		// Only start the animation timer -after- we've initialized
		curTime = startTime = System.currentTimeMillis();

		while (!done() && !animationCanceled) {
			display.syncExec(animationStep);

			// Don't pin the CPU
			try {
				Thread.sleep(sleepAmount);
			} catch (InterruptedException e) {
			}
		}

		if (animationCanceled)
			return Status.CANCEL_STATUS;

		// We're done, clean up
		display.syncExec(feedbackRenderer::dispose);

		return Status.OK_STATUS;
	}

	public void cancelAnimation() {
		animationCanceled = true;
		feedbackRenderer.dispose();
		cancel();
	}

	public long getFrameCount() {
		return frameCount;
	}
}
