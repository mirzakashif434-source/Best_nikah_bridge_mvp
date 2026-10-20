package com.nikahbridge;

import android.app.Activity;
import android.app.Application;
import android.app.AlertDialog;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

/** Audit copy: JVM smoke, no emulator, no Azure login, no billing calls. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33, application=Application.class)
public class WelcomeOfflineSmokeTest {
  @Test public void welcomeLanguageDialogOpensAndBackReturnsToWelcome() {
    Activity activity = Robolectric.buildActivity(WelcomeActivity.class).setup().get();
    View start = activity.findViewById(R.id.welcome_get_started);
    View login = activity.findViewById(R.id.welcome_sign_in);
    View language = activity.findViewById(R.id.welcome_language);
    assertNotNull("Get Started button", start);
    assertNotNull("Sign In button", login);
    assertNotNull("Language button", language);
    assertTrue("Language must be clickable", language.isClickable());

    assertNull("No dialog initially", ShadowAlertDialog.getLatestAlertDialog());
    assertTrue("Language click must be dispatched", language.performClick());
    AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
    assertNotNull("Language dialog must open", dialog);
    assertTrue("Language dialog must be visible", dialog.isShowing());
    assertNotNull("Language options must be present", dialog.getListView());
    assertEquals("All 14 languages available", LanguageManager.NAMES.length, dialog.getListView().getAdapter().getCount());
    assertNotNull("Cancel button", dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
    dialog.dismiss();
    assertFalse("Dialog must close", dialog.isShowing());
    assertFalse("Welcome activity must remain active", activity.isFinishing());
    assertNotNull("Sign In control remains", activity.findViewById(R.id.welcome_sign_in));
    assertNotNull("Language control remains", activity.findViewById(R.id.welcome_language));
  }
}
