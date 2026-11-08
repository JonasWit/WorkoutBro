package com.wtk_apps.workoutbro.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wtk_apps.workoutbro.BuildConfig
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The version on screen must be the version of the installed build, and the version must
 * stay a three-part semantic version. Both are easy to break by editing one number.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AppVersionTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun versionNameIsSemantic() {
    assertTrue(
      "versionName should be major.minor.patch, was ${BuildConfig.VERSION_NAME}",
      BuildConfig.VERSION_NAME.matches(Regex("""\d+\.\d+\.\d+""")),
    )
  }

  @Test
  fun versionIsShownUnderTheTitle() {
    compose.setContent { AppVersion() }

    compose.onNodeWithText("v${BuildConfig.VERSION_NAME}").assertIsDisplayed()
  }
}
