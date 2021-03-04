/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.uioverrides.states;

import static android.view.View.VISIBLE;

import static com.android.launcher3.LauncherState.HINT_STATE;
import static com.android.launcher3.LauncherState.HOTSEAT_ICONS;
import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.LauncherState.OVERVIEW;
import static com.android.launcher3.WorkspaceStateTransitionAnimation.getSpringScaleAnimator;
import static com.android.launcher3.anim.Interpolators.ACCEL;
import static com.android.launcher3.anim.Interpolators.ACCEL_DEACCEL;
import static com.android.launcher3.anim.Interpolators.DEACCEL;
import static com.android.launcher3.anim.Interpolators.DEACCEL_1_7;
import static com.android.launcher3.anim.Interpolators.DEACCEL_3;
import static com.android.launcher3.anim.Interpolators.FINAL_FRAME;
import static com.android.launcher3.anim.Interpolators.INSTANT;
import static com.android.launcher3.anim.Interpolators.OVERSHOOT_1_2;
import static com.android.launcher3.anim.Interpolators.clampToProgress;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_ALL_APPS_FADE;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_DEPTH;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_HOTSEAT_FADE;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_OVERVIEW_FADE;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_OVERVIEW_SCALE;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_OVERVIEW_TRANSLATE_X;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_OVERVIEW_TRANSLATE_Y;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_TASKBAR_FADE;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_WORKSPACE_FADE;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_WORKSPACE_SCALE;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_WORKSPACE_TRANSLATE;
import static com.android.quickstep.SysUINavigationMode.Mode.NO_BUTTON;
import static com.android.quickstep.views.RecentsView.RECENTS_SCALE_PROPERTY;

import android.animation.ValueAnimator;
import android.view.View;

import com.android.launcher3.CellLayout;
import com.android.launcher3.Hotseat;
import com.android.launcher3.LauncherState;
import com.android.launcher3.Workspace;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.states.StateAnimationConfig;
import com.android.launcher3.uioverrides.QuickstepLauncher;
import com.android.quickstep.SysUINavigationMode;
import com.android.quickstep.util.RecentsAtomicAnimationFactory;
import com.android.quickstep.views.RecentsView;

/**
 * Animation factory for quickstep specific transitions
 */
public class QuickstepAtomicAnimationFactory extends
        RecentsAtomicAnimationFactory<QuickstepLauncher, LauncherState> {

    // Scale recents takes before animating in
    private static final float RECENTS_PREPARE_SCALE = 1.33f;

    // Due to use of physics, duration may differ between devices so we need to calculate and
    // cache the value.
    private int mHintToNormalDuration = -1;

    public QuickstepAtomicAnimationFactory(QuickstepLauncher activity) {
        super(activity);
    }

    @Override
    public void prepareForAtomicAnimation(LauncherState fromState, LauncherState toState,
            StateAnimationConfig config) {
        if (toState == NORMAL && fromState == OVERVIEW) {
            config.setInterpolator(ANIM_WORKSPACE_SCALE, DEACCEL);
            config.setInterpolator(ANIM_WORKSPACE_FADE, ACCEL);
            config.setInterpolator(ANIM_TASKBAR_FADE, ACCEL);
            config.setInterpolator(ANIM_ALL_APPS_FADE, ACCEL);
            config.setInterpolator(ANIM_OVERVIEW_SCALE, clampToProgress(ACCEL, 0, 0.9f));
            config.setInterpolator(ANIM_OVERVIEW_TRANSLATE_X, ACCEL_DEACCEL);

            if (SysUINavigationMode.getMode(mActivity) == NO_BUTTON) {
                config.setInterpolator(ANIM_OVERVIEW_FADE, FINAL_FRAME);
            } else {
                config.setInterpolator(ANIM_OVERVIEW_FADE, DEACCEL_1_7);
            }

            Workspace workspace = mActivity.getWorkspace();
            // Start from a higher workspace scale, but only if we're invisible so we don't jump.
            boolean isWorkspaceVisible = workspace.getVisibility() == VISIBLE;
            if (isWorkspaceVisible) {
                CellLayout currentChild = (CellLayout) workspace.getChildAt(
                        workspace.getCurrentPage());
                isWorkspaceVisible = currentChild.getVisibility() == VISIBLE
                        && currentChild.getShortcutsAndWidgets().getAlpha() > 0;
            }
            if (!isWorkspaceVisible) {
                workspace.setScaleX(0.92f);
                workspace.setScaleY(0.92f);
            }
            Hotseat hotseat = mActivity.getHotseat();
            boolean isHotseatVisible = hotseat.getVisibility() == VISIBLE && hotseat.getAlpha() > 0;
            if (!isHotseatVisible) {
                hotseat.setScaleX(0.92f);
                hotseat.setScaleY(0.92f);
                AllAppsContainerView qsbContainer = mActivity.getAppsView();
                View qsb = qsbContainer.getSearchView();
                boolean qsbVisible = qsb.getVisibility() == VISIBLE && qsb.getAlpha() > 0;
                if (!qsbVisible) {
                    qsbContainer.setScaleX(0.92f);
                    qsbContainer.setScaleY(0.92f);
                }
            }
        } else if ((fromState == NORMAL || fromState == HINT_STATE) && toState == OVERVIEW) {
            if (SysUINavigationMode.getMode(mActivity) == NO_BUTTON) {
                config.setInterpolator(ANIM_WORKSPACE_SCALE,
                        fromState == NORMAL ? ACCEL : OVERSHOOT_1_2);
                config.setInterpolator(ANIM_WORKSPACE_TRANSLATE, ACCEL);
                config.setInterpolator(ANIM_OVERVIEW_FADE, INSTANT);
            } else {
                config.setInterpolator(ANIM_WORKSPACE_SCALE, OVERSHOOT_1_2);
                config.setInterpolator(ANIM_OVERVIEW_FADE, OVERSHOOT_1_2);

                // Scale up the recents, if it is not coming from the side
                RecentsView overview = mActivity.getOverviewPanel();
                if (overview.getVisibility() != VISIBLE || overview.getContentAlpha() == 0) {
                    RECENTS_SCALE_PROPERTY.set(overview, RECENTS_PREPARE_SCALE);
                }
            }
            config.setInterpolator(ANIM_WORKSPACE_FADE, OVERSHOOT_1_2);
            config.setInterpolator(ANIM_ALL_APPS_FADE, OVERSHOOT_1_2);
            config.setInterpolator(ANIM_OVERVIEW_SCALE, OVERSHOOT_1_2);
            config.setInterpolator(ANIM_DEPTH, OVERSHOOT_1_2);
            config.setInterpolator(ANIM_OVERVIEW_TRANSLATE_X, OVERSHOOT_1_2);
            config.setInterpolator(ANIM_OVERVIEW_TRANSLATE_Y, OVERSHOOT_1_2);
            config.setInterpolator(ANIM_TASKBAR_FADE, OVERSHOOT_1_2);
        } else if (fromState == HINT_STATE && toState == NORMAL) {
            config.setInterpolator(ANIM_DEPTH, DEACCEL_3);
            if (mHintToNormalDuration == -1) {
                ValueAnimator va = getSpringScaleAnimator(mActivity, mActivity.getWorkspace(),
                        toState.getWorkspaceScaleAndTranslation(mActivity).scale);
                mHintToNormalDuration = (int) va.getDuration();
            }
            config.duration = Math.max(config.duration, mHintToNormalDuration);
        } else if (mActivity.getTaskbarController() != null)  {
            boolean wasHotseatVisible = fromState.areElementsVisible(mActivity, HOTSEAT_ICONS);
            boolean isHotseatVisible = toState.areElementsVisible(mActivity, HOTSEAT_ICONS);
            if (wasHotseatVisible || isHotseatVisible) {
                config.setInterpolator(ANIM_TASKBAR_FADE, INSTANT);
                config.setInterpolator(ANIM_HOTSEAT_FADE, INSTANT);

                if (isHotseatVisible) {
                    mActivity.getTaskbarController().alignRealHotseatWithTaskbar();
                }
            }
        }
    }
}
