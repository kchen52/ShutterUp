package app.shutterup.capture

import androidx.activity.result.contract.ActivityResultContracts

/** Still capture into a [androidx.core.content.FileProvider] URI (SPEC §2 camera). */
class TakePrivatePicture : ActivityResultContracts.TakePicture()
