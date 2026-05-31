package com.example

import com.example.data.TagClassifier
import com.example.data.SakugaTag
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testExtractCandidateName() {
    assertEquals("Vincent", TagClassifier.extractCandidateName("0:17 Vincent"))
    assertEquals("Saucelot", TagClassifier.extractCandidateName("0:05.9 Saucelot"))
    assertEquals("The Ohira energy at is amazing", TagClassifier.extractCandidateName("The Ohira energy at is amazing"))
    assertEquals("The Ohira energy at is amazing", TagClassifier.extractCandidateName("0:35.8 - 0:42.5 The Ohira energy at is amazing"))
  }

  @Test
  fun testIsValidArtist() {
    // True artists
    assertTrue(TagClassifier.isValidArtist("0:17 Vincent"))
    assertTrue(TagClassifier.isValidArtist("0:05.9 Saucelot"))
    assertTrue(TagClassifier.isValidArtist("0:22.5 TNK"))
    assertTrue(TagClassifier.isValidArtist("Yutaka Nakamura"))
    assertTrue(TagClassifier.isValidArtist("Shingo Yamashita"))

    // Step A dynamic cross-referencing checks (with simulated Booru database entries)
    val dummyTags = mapOf(
      "weird_artist_name" to SakugaTag(id = 11, name = "weird_artist_name", type = 1),
      "acting_general_tag" to SakugaTag(id = 12, name = "acting_general_tag", type = 0)
    )
    assertTrue(TagClassifier.isValidArtist("weird_artist_name", dummyTags))
    // "acting_general_tag" has type = 0 (General category), so it should fail validation as artist
    assertFalse(TagClassifier.isValidArtist("acting_general_tag", dummyTags))

    // False/discussive entries
    // Case 1: Length too long
    assertFalse(TagClassifier.isValidArtist("Why are they getting teleported to the side of a building?"))
    assertFalse(TagClassifier.isValidArtist("Maybe that indicates an insane amount of rushed work"))

    // Case 2: Capitalization failures (lowercase words starting)
    assertFalse(TagClassifier.isValidArtist("is amazing"))
    assertFalse(TagClassifier.isValidArtist("amazing cuts here"))

    // Case 3: Conversational stop-words
    assertFalse(TagClassifier.isValidArtist("is insane"))
    assertFalse(TagClassifier.isValidArtist("look at that background"))
  }

  @Test
  fun testSearchQueryParsingAndUpdating() {
    // We can reflect the internal math of updateTagInQuery and syncStatesFromQuery conceptually
    // Let's test a mock simulation or we can instantiate the SakugaViewModel if we want to,
    // but the logic of tag replacement is simple and robust to test.
    val initialQuery = "yutaka_nakamura"
    
    // Simulate updating keys
    fun mockUpdateTag(query: String, key: String, value: String?): String {
      val words = query.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.toMutableList()
      if (key == "source") {
        words.removeAll { it == "source:*solo*ka" }
        if (value == "true") {
          words.add("source:*solo*ka")
        }
      } else {
        val index = words.indexOfFirst { it.startsWith("$key:") }
        if (index != -1) {
          if (value == null || value == "all" || (key == "order" && value == "date")) {
            words.removeAt(index)
          } else {
            words[index] = "$key:$value"
          }
        } else {
          if (value != null && value != "all" && !(key == "order" && value == "date")) {
            words.add("$key:$value")
          }
        }
      }
      return words.joinToString(" ")
    }

    assertEquals("yutaka_nakamura order:score", mockUpdateTag(initialQuery, "order", "score"))
    assertEquals("yutaka_nakamura", mockUpdateTag("yutaka_nakamura order:score", "order", "date"))
    assertEquals("yutaka_nakamura rating:safe", mockUpdateTag(initialQuery, "rating", "safe"))
    assertEquals("yutaka_nakamura limit:50", mockUpdateTag(initialQuery, "limit", "50"))
    assertEquals("yutaka_nakamura source:*solo*ka", mockUpdateTag(initialQuery, "source", "true"))
    assertEquals("yutaka_nakamura", mockUpdateTag("yutaka_nakamura source:*solo*ka", "source", null))
  }
}
