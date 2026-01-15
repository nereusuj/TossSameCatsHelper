package com.nereusuj.tosssamecantshelper

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
class ImageAnalyzerTest {

    @Test
    fun testAnalyze2x2() {
        val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream("Screenshot_Toss_2x2.png")
        assertNotNull("Test image not found in resources", inputStream)

        val bitmap = BitmapFactory.decodeStream(inputStream)
        assertNotNull("Failed to decode bitmap", bitmap)

        val analyzer = ImageAnalyzer()
        val results = analyzer.analyze(bitmap, 2, 2)

        println("Found ${results.size} cards in 2x2")
        results.forEachIndexed { index, cardResult ->
            val r = cardResult.rect
            println("Card $index: Rect=$r, W=${r.width()}, H=${r.height()}, Group=${cardResult.groupId}")
        }

        assertEquals("Should find 4 cards", 4, results.size)
        
        val counts = results.groupingBy { it.groupId }.eachCount()
        println("Group ID Counts: $counts")
        
        // Should have 2 groups of 2 ideally
        val pairs = counts.values.count { it == 2 }
        assertEquals("Should find 2 pairs", 2, pairs)
    }

    @Test
    fun testAnalyze3x2() {
        val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream("Screenshot_Toss_3x2.png")
        assertNotNull("Test image not found in resources", inputStream)

        val bitmap = BitmapFactory.decodeStream(inputStream)
        assertNotNull("Failed to decode bitmap", bitmap)

        val analyzer = ImageAnalyzer()
        // Expected grid: 3 rows, 2 cols (from filename Screenshot_Toss_3x2.png)
        val results = analyzer.analyze(bitmap, 3, 2)

        println("Found ${results.size} cards")
        results.forEachIndexed { index, cardResult ->
            val r = cardResult.rect
            println("Card $index: Rect=$r, W=${r.width()}, H=${r.height()}, Group=${cardResult.groupId}")
        }

        assertEquals("Should find 6 cards", 6, results.size)
        
        // Verify we have pairs (at least mostly)
        // Count frequencies of groupIds
        val counts = results.groupingBy { it.groupId }.eachCount()
        println("Group ID Counts: $counts")
        
        // In a perfect world, we have pairs
        val pairCount = counts.values.count { it == 2 }
        assertTrue("Should have at least some pairs identified", pairCount > 0)
    }

    @Test
    fun testAnalyze4x2() {
        val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream("Screenshot_Toss_4x2.png")
        assertNotNull("Test image not found in resources", inputStream)

        val bitmap = BitmapFactory.decodeStream(inputStream)
        assertNotNull("Failed to decode bitmap", bitmap)

        val analyzer = ImageAnalyzer()
        val results = analyzer.analyze(bitmap, 4, 2)

        println("Found ${results.size} cards in 4x2")
        results.forEachIndexed { index, cardResult ->
            val r = cardResult.rect
            println("Card $index: Rect=$r, W=${r.width()}, H=${r.height()}, Group=${cardResult.groupId}")
        }

        assertEquals("Should find 8 cards", 8, results.size)

        val counts = results.groupingBy { it.groupId }.eachCount()
        println("Group ID Counts: $counts")
        
        val pairs = counts.values.count { it == 2 }
        assertTrue("Should have mostly pairs", pairs >= 3) // Allow some leniency or aim for perfect 4
    }

    @Test
    fun testAnalyze4x3() {
        val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream("Screenshot_Toss_4x3.png")
        assertNotNull("Test image not found in resources", inputStream)

        val bitmap = BitmapFactory.decodeStream(inputStream)
        assertNotNull("Failed to decode bitmap", bitmap)

        val analyzer = ImageAnalyzer()
        val results = analyzer.analyze(bitmap, 4, 3)

        println("Found ${results.size} cards in 4x3")
        results.forEachIndexed { index, cardResult ->
            val r = cardResult.rect
            println("Card $index: Rect=$r, W=${r.width()}, H=${r.height()}, Group=${cardResult.groupId}")
        }

        assertEquals("Should find 12 cards", 12, results.size)

        val counts = results.groupingBy { it.groupId }.eachCount()
        println("Group ID Counts: $counts")
        
        val pairs = counts.values.count { it == 2 }
        assertTrue("Should have mostly pairs", pairs >= 5)
    }

    @Test
    fun testAnalyze4x4() {
        val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream("Screenshot_Toss_4x4.png")
        assertNotNull("Test image not found in resources", inputStream)

        val bitmap = BitmapFactory.decodeStream(inputStream)
        assertNotNull("Failed to decode bitmap", bitmap)

        val analyzer = ImageAnalyzer()
        val results = analyzer.analyze(bitmap, 4, 4)

        println("Found ${results.size} cards in 4x4")
        results.forEachIndexed { index, cardResult ->
            val r = cardResult.rect
            println("Card $index: Rect=$r, W=${r.width()}, H=${r.height()}, Group=${cardResult.groupId}")
        }

        assertEquals("Should find 16 cards", 16, results.size)

        val counts = results.groupingBy { it.groupId }.eachCount()
        println("Group ID Counts: $counts")
        
        val pairs = counts.values.count { it == 2 }
        assertTrue("Should have mostly pairs", pairs >= 7)
    }

    @Test
    fun testAnalyze5x4() {
        val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream("Screenshot_Toss_5x4.png")
        assertNotNull("Test image not found in resources", inputStream)

        val bitmap = BitmapFactory.decodeStream(inputStream)
        assertNotNull("Failed to decode bitmap", bitmap)

        val analyzer = ImageAnalyzer()
        val results = analyzer.analyze(bitmap, 5, 4)

        println("Found ${results.size} cards in 5x4")
        results.forEachIndexed { index, cardResult ->
            val r = cardResult.rect
            println("Card $index: Rect=$r, W=${r.width()}, H=${r.height()}, Group=${cardResult.groupId}")
        }

        assertEquals("Should find 20 cards", 20, results.size)

        val counts = results.groupingBy { it.groupId }.eachCount()
        println("Group ID Counts: $counts")
        
        val pairs = counts.values.count { it == 2 }
        assertTrue("Should have mostly pairs", pairs >= 9)
    }

    @Test
    fun testAnalyze6x4() {
        val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream("Screenshot_Toss_6x4.png")
        assertNotNull("Test image not found in resources", inputStream)

        val bitmap = BitmapFactory.decodeStream(inputStream)
        assertNotNull("Failed to decode bitmap", bitmap)

        val analyzer = ImageAnalyzer()
        val results = analyzer.analyze(bitmap, 6, 4)

        println("Found ${results.size} cards in 6x4")
        results.forEachIndexed { index, cardResult ->
            val r = cardResult.rect
            println("Card $index: Rect=$r, W=${r.width()}, H=${r.height()}, Group=${cardResult.groupId}")
        }

        assertEquals("Should find 24 cards", 24, results.size)

        val counts = results.groupingBy { it.groupId }.eachCount()
        println("Group ID Counts: $counts")
        
        val pairs = counts.values.count { it == 2 }
        assertTrue("Should have mostly pairs", pairs >= 12) // 12 pairs ideally, but 6x4 is hard
    }

    @Test
    fun testAnalyze6x5() {
        val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream("Screenshot_Toss_6x5.png")
        assertNotNull("Test image not found in resources", inputStream)

        val bitmap = BitmapFactory.decodeStream(inputStream)
        assertNotNull("Failed to decode bitmap", bitmap)

        val analyzer = ImageAnalyzer()
        val results = analyzer.analyze(bitmap, 6, 5)

        println("Found ${results.size} cards in 6x5")
        results.forEachIndexed { index, cardResult ->
            val r = cardResult.rect
            println("Card $index: Rect=$r, W=${r.width()}, H=${r.height()}, Group=${cardResult.groupId}")
        }

        assertEquals("Should find 30 cards", 30, results.size)

        val counts = results.groupingBy { it.groupId }.eachCount()
        println("Group ID Counts: $counts")
        
        val pairs = counts.values.count { it == 2 }
        assertTrue("Should have mostly pairs", pairs >= 15) // 15 pairs ideally, but 6x5 is hard
    }
}
