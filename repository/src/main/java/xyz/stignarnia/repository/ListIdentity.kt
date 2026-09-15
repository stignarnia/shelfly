package xyz.stignarnia.repository

import java.util.UUID

/**
 * The identity a custom list keeps across devices, backups and sync, stored in its `id_slug` column.
 *
 * The local row id cannot be it: it is an autoincrement counter, so two devices - or one device before and after a reinstall - hand the same number to unrelated lists.
 * Anything matched on that number merges lists that merely occupied the same slot.
 */
object ListIdentity {
  private val format = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

  fun create(): String = UUID.randomUUID().toString()

  /**
   * Whether [idSlug] is an identity rather than what the column held before: an empty string for a list created in the app, or a slug derived from the name for one imported from Showly.
   */
  fun isValid(idSlug: String): Boolean = format.matches(idSlug)
}
