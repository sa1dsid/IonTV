package com.ionica.iontv.screenshot

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.composeTestRule
import com.github.takahirom.roborazzi.inspectionMode
import com.github.takahirom.roborazzi.toRoborazziComposeOptions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TV_1080P)
class PreviewScreenshotTest(
    private val preview: ComposablePreview<AndroidPreviewInfo>,
) {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun snapshot() {
        val options = preview.toRoborazziComposeOptions()
            .builder()
            .composeTestRule(composeTestRule.asScenarioRule())
            .inspectionMode(true)
            .build()

        preview.captureRoboImage(
            filePath = "$SCREENSHOTS_DIR/${preview.fileName()}.png",
            roborazziComposeOptions = options,
        )
    }

    companion object {

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun previews(): List<ComposablePreview<AndroidPreviewInfo>> =
            AndroidComposablePreviewScanner()
                .scanPackageTrees("com.ionica.iontv")
                .includePrivatePreviews()
                .getPreviews()
    }
}

private typealias ActivityComposeRule =
    AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>

@Suppress("UNCHECKED_CAST")
private fun ActivityComposeRule.asScenarioRule() =
    this as AndroidComposeTestRule<ActivityScenarioRule<out ComponentActivity>, *>

private fun ComposablePreview<AndroidPreviewInfo>.fileName(): String =
    "${declaringClass.removeSuffix("Kt")}.$methodName"

private const val SCREENSHOTS_DIR = "src/test/screenshots"

private const val TV_1080P = "w960dp-h540dp-xlarge-land-television-xhdpi-keyshidden-nonav"
