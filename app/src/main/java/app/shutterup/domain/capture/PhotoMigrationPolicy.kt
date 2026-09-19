package app.shutterup.domain.capture

/** What to do with one [app.shutterup.domain.model.Entry] during the one-time MediaStore copy. */
sealed class MigrationDecision {
    data object AlreadyMigrated : MigrationDecision()
    data object CopyToMediaStore : MigrationDecision()
    data object KeepThumbnailFallback : MigrationDecision()
}

object PhotoMigrationPolicy {
    fun decide(mediaUri: String, sourceReadable: Boolean): MigrationDecision {
        if (!PhotoUriClassifier.needsMigration(mediaUri)) {
            return MigrationDecision.AlreadyMigrated
        }
        return if (sourceReadable) {
            MigrationDecision.CopyToMediaStore
        } else {
            MigrationDecision.KeepThumbnailFallback
        }
    }
}
