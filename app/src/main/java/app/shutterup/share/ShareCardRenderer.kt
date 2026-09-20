package app.shutterup.share

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.shutterup.R
import app.shutterup.domain.share.ShareCardContent
import app.shutterup.ui.share.ShareCard
import app.shutterup.ui.share.ShareCardMetrics
import app.shutterup.ui.theme.ProvideThemeTint
import app.shutterup.ui.theme.ShutterUpTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders [ShareCard] with the real design-system components to a bitmap.
 * Composition runs on the main thread (ComposeView requires it); callers
 * should hop off main before PNG encoding.
 */
@Singleton
class ShareCardRenderer @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    suspend fun render(
        content: ShareCardContent,
        photo: ImageBitmap,
        darkTheme: Boolean,
    ): Bitmap {
        val draw = { renderSync(content, photo, darkTheme) }
        return if (Looper.myLooper() == Looper.getMainLooper()) {
            draw()
        } else {
            withContext(Dispatchers.Main) { draw() }
        }
    }

    private fun renderSync(
        content: ShareCardContent,
        photo: ImageBitmap,
        darkTheme: Boolean,
    ): Bitmap {
        val context = ContextThemeWrapper(appContext, R.style.Theme_ShutterUp)
        val density = Density(
            density = ShareCardMetrics.DensityScale,
            fontScale = 1f,
        )
        val owner = ShareRenderOwner()
        val composeView = ComposeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ShareCardMetrics.WidthPx,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        composeView.setViewTreeLifecycleOwner(owner)
        composeView.setViewTreeSavedStateRegistryOwner(owner)
        composeView.setViewTreeViewModelStoreOwner(owner)
        composeView.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                ShutterUpTheme(darkTheme = darkTheme) {
                    ProvideThemeTint(theme = content.theme, darkTheme = darkTheme) {
                        ShareCard(
                            content = content,
                            photo = photo,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        composeView.createComposition()
        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            ShareCardMetrics.WidthPx,
            View.MeasureSpec.EXACTLY,
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        composeView.measure(widthSpec, heightSpec)
        val heightPx = composeView.measuredHeight
        check(heightPx > 0) { "Share card measured empty" }
        composeView.layout(0, 0, ShareCardMetrics.WidthPx, heightPx)
        val bitmap = Bitmap.createBitmap(
            ShareCardMetrics.WidthPx,
            heightPx,
            Bitmap.Config.ARGB_8888,
        )
        composeView.draw(android.graphics.Canvas(bitmap))
        composeView.disposeComposition()
        owner.destroy()
        return bitmap
    }
}

private class ShareRenderOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store
}
