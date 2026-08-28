package xyz.stignarnia.uiSettings.helpers

import androidx.annotation.StringRes
import timber.log.Timber
import xyz.stignarnia.uiSettings.R

enum class AppLanguage(
  val code: String,
  val displayNameRaw: String,
  @get:StringRes val displayName: Int,
) {
  ENGLISH("en", "English", R.string.textLanguageEnglish),
  GERMAN("de", "German", R.string.textLanguageGerman),
  FRENCH("fr", "French", R.string.textLanguageFrench),
  DANISH("da", "Danish", R.string.textLanguageDanish),
  ITALIAN("it", "Italian", R.string.textLanguageItalian),
  SPANISH("es", "Spanish", R.string.textLanguageSpanish),
  PORTUGAL_BRASIL("pt", "Portuguese", R.string.textLanguagePortugalBrasil),
  ROMANIAN("ro", "Romanian", R.string.textLanguageRomanian),
  POLISH("pl", "Polish", R.string.textLanguagePolish),
  RUSSIAN("ru", "Russian", R.string.textLanguageRussian),
  UKRAINIAN("uk", "Ukrainian", R.string.textLanguageUkrainian),
  FINNISH("fi", "Finnish", R.string.textLanguageFinnish),
  TURKISH("tr", "Turkish", R.string.textLanguageTurkish),
  ARABIC("ar", "Arabic", R.string.textLanguageArabic),
  CHINESE("zh", "Chinese Simplified", R.string.textLanguageChinese),
  ;

  companion object {
    fun fromCode(code: String): AppLanguage {
      Timber.d("Looking for AppLanguage with code: $code")
      return entries.firstOrNull { it.code == code } ?: ENGLISH
    }
  }
}
