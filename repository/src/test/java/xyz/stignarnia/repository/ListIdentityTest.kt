package xyz.stignarnia.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ListIdentityTest {
  @Test
  fun `Should recognise the identities it creates, and never create the same one twice`() {
    val first = ListIdentity.create()
    val second = ListIdentity.create()

    assertThat(ListIdentity.isValid(first)).isTrue()
    assertThat(first).isNotEqualTo(second)
  }

  @Test
  fun `Should not take what the column held before identities for one`() {
    // An empty slug from a list created in the app, a name-derived slug from a Showly import, and a local row id.
    listOf("", "sugate", "1").forEach {
      assertThat(ListIdentity.isValid(it)).isFalse()
    }
  }
}
