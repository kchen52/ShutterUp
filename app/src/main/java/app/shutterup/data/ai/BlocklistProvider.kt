package app.shutterup.data.ai

import android.content.Context
import app.shutterup.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BlocklistProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun blockedTerms(): Set<String> =
        context.resources.getStringArray(R.array.blocklist_terms).toSet()

    fun gearTerms(): Set<String> =
        context.resources.getStringArray(R.array.gear_terms).toSet()
}
