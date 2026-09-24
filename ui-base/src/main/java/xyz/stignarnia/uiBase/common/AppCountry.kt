package xyz.stignarnia.uiBase.common

import android.content.Context
import androidx.core.os.ConfigurationCompat
import java.util.Locale

enum class AppCountry(
  val code: String,
  val justWatchQuery: String = "search",
) {
  ARGENTINA("ar", "buscar"),
  AUSTRALIA("au"),
  AUSTRIA("at", "Suche"),
  BELGIUM("be", "recherche"),
  BRAZIL("br", "busca"),
  BULGARIA("bg"),
  CANADA("ca"),
  CHILE("cl", "buscar"),
  COLOMBIA("co", "buscar"),
  CZECH_REP("cz", "vyhledání"),
  DENMARK("dk"),
  ECUADOR("ec", "buscar"),
  ESTONIA("ee", "otsing"),
  FINLAND("fi", "etsi"),
  FRANCE("fr", "recherche"),
  GERMANY("de", "Suche"),
  GREECE("gr"),
  HONGKONG("hk"),
  HUNGARY("hu"),
  INDIA("in"),
  INDONESIA("id"),
  IRELAND("ie"),
  ITALY("it", "cerca"),
  JAPAN("jp", "検索"),
  LATVIA("lv"),
  LITHUANIA("lt"),
  MALAYSIA("my"),
  MEXICO("mx", "buscar"),
  NETHERLANDS("nl"),
  NEW_ZEALAND("nz"),
  NORWAY("no"),
  PERU("pe", "buscar"),
  PHILIPPINES("ph"),
  POLAND("pl"),
  PORTUGAL("pt", "busca"),
  ROMANIA("ro"),
  RUSSIA("ru", "поиск"),
  SINGAPORE("sg"),
  SOUTH_AFRICA("za"),
  SOUTH_KOREA("kr", "검색"),
  SPAIN("es", "buscar"),
  SWEDEN("se"),
  SWITZERLAND("ch", "Suche"),
  THAILAND("th"),
  TAIWAN("tw"),
  TURKEY("tr", "arama"),
  UKRAINE("ua", "пошук"),
  UNITED_KINGDOM("uk"),
  UNITED_STATES("us"),
  VENEZUELA("ve", "buscar"),
  ;

  /**
   * The country's name in the language the app is displayed in, taken from the platform's locale data rather than translated here.
   * [code] is the app's own key, which differs from ISO 3166 for the United Kingdom.
   */
  fun displayName(context: Context): String {
    val region = if (this == UNITED_KINGDOM) "GB" else code.uppercase(Locale.ROOT)
    val displayLocale = ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault()
    return Locale
      .Builder()
      .setRegion(region)
      .build()
      .getDisplayCountry(displayLocale)
  }

  companion object {
    fun fromCode(code: String) = values().first { it.code == code }
  }
}
