package com.nereusuj.tosssamecantshelper

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import java.util.LinkedList
import java.util.Queue
import kotlin.math.abs

class ImageAnalyzer {
    
    companion object {
        private const val TAG = "ImageAnalyzer"
    }

    // Tuning parameters
    // Tuning parameters
    private val MATCH_THRESHOLD = 1500.0 // MSE threshold
    private val DOWNSCALE_SIZE = 32 // For matching

    fun analyze(originalBitmap: Bitmap, rows: Int, cols: Int): List<CardResult> {
        val width = originalBitmap.width
        val height = originalBitmap.height
        
        val scale = 2
        val smallW = width / scale
        val smallH = height / scale
        val smallPixels = IntArray(smallW * smallH)
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, smallW, smallH, false)
        scaledBitmap.getPixels(smallPixels, 0, smallW, 0, 0, smallW, smallH)
        
        // 2. Find Blobs using Flood Fill Background Separation
        var blobs = findBlobs(smallPixels, smallW, smallH)
        Log.d(TAG, "Initial Blobs found: ${blobs.size}")

        // Check for Frame/Container: If we found few blobs but one is huge (Main Game Area)
        val expectedCount = rows * cols
        
        // Count "significant" blobs to avoid being fooled by noise
        // Noise blobs detected were ~3000-6000. Cards should be ~25000+.
        // In small coords, card is ~6000. So threshold 2000.
        val significantBlobs = blobs.count { it.width() * it.height() > 2000 } 

        val giantBlob = blobs.maxByOrNull { it.width() * it.height() }
        if (giantBlob != null) {
             // println("Largest Blob Area: ${giantBlob.width() * giantBlob.height()}")
        }

        // Heuristic: If we found significantly fewer blobs than expected, or just 1-2 blobs
        if (significantBlobs < expectedCount) {
            if (giantBlob != null) {
                val area = giantBlob.width() * giantBlob.height()
                // If blob > 50% of screen
                if (area > smallW * smallH * 0.4) {
                    Log.d(TAG, "Giant Blob detected (Frame?): $giantBlob. Refining ROI...")
                    // Extract ROI and re-process
                    val roiW = giantBlob.width()
                    val roiH = giantBlob.height()
                    val roiPixels = cropPixels(smallPixels, smallW, giantBlob)
                    
                    val subBlobs = findBlobs(roiPixels, roiW, roiH)
                    Log.d(TAG, "ROI Blobs found: ${subBlobs.size}")
                    
                    // Maps subBlobs back to small coordinates
                    blobs = subBlobs.map { 
                        Rect(it.left + giantBlob.left, it.top + giantBlob.top, 
                             it.right + giantBlob.left, it.bottom + giantBlob.top)
                    }
                }
            }
        }

        // 3. Filter Blobs to find Cards
        val cardRects = filterBlobs(blobs, expectedCount, scale) // Scale rects back up
        Log.d(TAG, "Filtered Cards: ${cardRects.size}")

        // 4. Sort (Row-major)
        val sortedRects = sortRects(cardRects, rows, cols)

        // 5. Match Cards
        val groups = matchCards(originalBitmap, sortedRects)

        // 6. Build Result
        return sortedRects.mapIndexed { index, rect ->
             CardResult(rect, groups[index])
        }
    }

    private fun findBlobs(pixels: IntArray, width: Int, height: Int): List<Rect> {
        val visited = BooleanArray(pixels.size)
        val queue: Queue<Int> = LinkedList()
        
        // Phase 1: Flood Fill Background from edges
        // Seeds: Left/Right edges (skipping top/bottom 10%)
        val marginY = height / 10
        // Ensure margin is valid
        val startY = marginY.coerceIn(0, height/2)
        val endY = (height - marginY).coerceIn(startY, height)
        
        for (y in startY until endY) {
            val left = y * width
            val right = y * width + width - 1
            if (!visited[left]) {
                queue.add(left)
                visited[left] = true
            }
            if (!visited[right]) {
                queue.add(right)
                visited[right] = true
            }
        }
        
        var bgPixels = 0
        while (!queue.isEmpty()) {
            val curr = queue.remove()
            bgPixels++
            
            val cx = curr % width
            val cy = curr / width
            
            val neighbors = intArrayOf(curr - 1, curr + 1, curr - width, curr + width)
            
            for (n in neighbors) {
                if (n in pixels.indices && !visited[n]) {
                    val nx = n % width
                    val ny = n / width
                    if (abs(nx - cx) + abs(ny - cy) == 1) {
                         if (colorDiff(pixels[curr], pixels[n]) < 9) { 
                             visited[n] = true
                             queue.add(n)
                         }
                    }
                }
            }
        }
        // println("Phase 1: Marked $bgPixels pixels as Background")

        // Phase 2: Find Connected Components in remaining (Foreground) pixels
        val blobs = mutableListOf<Rect>()
        val step = 2 
        
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val idx = y * width + x
                if (!visited[idx]) {
                    // New Component
                    var minX = x
                    var maxX = x
                    var minY = y
                    var maxY = y
                    var count = 0
                    
                    queue.add(idx)
                    visited[idx] = true
                    
                    while (!queue.isEmpty()) {
                        val curr = queue.remove()
                        count++
                        
                        val cx = curr % width
                        val cy = curr / width
                        
                        if (cx < minX) minX = cx
                        if (cx > maxX) maxX = cx
                        if (cy < minY) minY = cy
                        if (cy > maxY) maxY = cy
                        
                        val neighbors = intArrayOf(curr - 1, curr + 1, curr - width, curr + width)
                        for (n in neighbors) {
                            if (n in pixels.indices && !visited[n]) {
                                val nx = n % width
                                val ny = n / width
                                if (abs(nx - cx) + abs(ny - cy) == 1) {
                                    visited[n] = true
                                    queue.add(n)
                                }
                            }
                        }
                    }
                    if (count > 50) {
                        blobs.add(Rect(minX, minY, maxX + 1, maxY + 1))
                    }
                }
            }
        }
        return blobs
    }
    
    private fun cropPixels(source: IntArray, srcW: Int, rect: Rect): IntArray {
        val w = rect.width()
        val h = rect.height()
        val dest = IntArray(w * h)
        // Check bounds
        val startX = rect.left.coerceAtLeast(0)
        val startY = rect.top.coerceAtLeast(0)
        
        for (y in 0 until h) {
            val srcIdx = (startY + y) * srcW + startX
            if (srcIdx < source.size && (y * w + w) <= dest.size) {
                 System.arraycopy(source, srcIdx, dest, y * w, w)
            }
        }
        return dest
    }
    
    private fun colorDiff(c1: Int, c2: Int): Int {
        val r = abs(Color.red(c1) - Color.red(c2))
        val g = abs(Color.green(c1) - Color.green(c2))
        val b = abs(Color.blue(c1) - Color.blue(c2))
        return r + g + b
    }

    private fun filterBlobs(blobs: List<Rect>, expectedCount: Int, scale: Int): List<Rect> {
        if (blobs.isEmpty()) return emptyList()

        val scaledBlobs = blobs.map { 
            Rect(it.left * scale, it.top * scale, it.right * scale, it.bottom * scale) 
        }

        val validBlobs = scaledBlobs.filter { rect ->
            val w = rect.width().toFloat()
            val h = rect.height().toFloat()
            val ar = w / h
            val area = w * h
            
            if (ar > 1.8 || ar < 0.5) {
                return@filter false
            }
            if (area < 5000) {
                 return@filter false
            }
            true
        }
        return validBlobs.sortedByDescending { it.width() * it.height() }
            .take(expectedCount)
    }

    private fun sortRects(rects: List<Rect>, rows: Int, columns: Int): List<Rect> {
        // Sort by Y (Center)
        // Group into Rows (using threshold)
        // Sort each Row by X
        
        if (rects.isEmpty()) return rects
        
        val sortedByY = rects.sortedBy { it.centerY() }
        
        // Determine row height to group
        // Simple approach: K-Means or just splitting?
        // Since we know 'rows', we can expect 'rows' groups.
        
        // Naive row grouping:
        val result = mutableListOf<Rect>()
        if (sortedByY.size != rows * columns) {
             // If detection failed to find exact count, just XY sort
             // This might mix rows if skew is high, but standard grid is fine.
             // We can just sort by (Y / rowHeight), then X.
             // But we don't know rowHeight perfectly.
             
             // Just general sort: Y-major.
             // return sortedByY // Wrong, needs X sorting within rows.
             
             // Let's stick to naive XY sort logic with "fuzzy Y":
             return rects.sortedWith(Comparator { r1, r2 ->
                 if (abs(r1.centerY() - r2.centerY()) < r1.height() / 2) {
                     r1.centerX() - r2.centerX()
                 } else {
                     r1.centerY() - r2.centerY()
                 }
             })
        }

        // If we have exact count, chunks of 'cols' is safer after Y sort?
        // Only if rows are perfectly separated.
        
        // Robust Sort:
        val rowGroups = mutableListOf<MutableList<Rect>>()
        var currentRow = mutableListOf<Rect>()
        var lastY = sortedByY[0].centerY()
        
        sortedByY.forEach { rect ->
            if (abs(rect.centerY() - lastY) > rect.height() / 2) {
                // New Row
                rowGroups.add(currentRow)
                currentRow = mutableListOf()
                lastY = rect.centerY()
            }
            currentRow.add(rect)
        }
        rowGroups.add(currentRow)
        
        // Sort each row by X and flatten
        return rowGroups.flatMap { row -> row.sortedBy { it.centerX() } }
    }

    private data class MatchEdge(val i: Int, val j: Int, val mse: Double)

    private fun matchCards(bitmap: Bitmap, rects: List<Rect>): List<Int> {
        val samples = rects.map { rect ->
            // Crop and resize
            // Add safety check for bounds
            val safeRect = Rect(
                rect.left.coerceAtLeast(0),
                rect.top.coerceAtLeast(0),
                rect.right.coerceAtMost(bitmap.width),
                rect.bottom.coerceAtMost(bitmap.height)
            )
            val crop = Bitmap.createBitmap(bitmap, safeRect.left, safeRect.top, safeRect.width(), safeRect.height())
            
            // Center crop (15% margin) to remove borders
            val marginX = (safeRect.width() * 0.15).toInt()
            val marginY = (safeRect.height() * 0.15).toInt() 
            
            val finalCrop = if (safeRect.width() > 2 * marginX && safeRect.height() > 2 * marginY) {
                 Bitmap.createBitmap(crop, marginX, marginY, 
                     safeRect.width() - 2 * marginX, safeRect.height() - 2 * marginY)
            } else {
                 crop
            }

            Bitmap.createScaledBitmap(finalCrop, DOWNSCALE_SIZE, DOWNSCALE_SIZE, true)
        }

        val n = rects.size
        val labels = IntArray(n) { 0 }
        var nextLabel = 1

        val edges = mutableListOf<MatchEdge>()

        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val mse = calculateMSE(samples[i], samples[j])
                if (mse < MATCH_THRESHOLD) {
                    edges.add(MatchEdge(i, j, mse))
                }
            }
        }

        // Sort by best match first
        edges.sortBy { it.mse }

        val used = BooleanArray(n)
        
        // First pass: Link best pairs
        for (edge in edges) {
            if (!used[edge.i] && !used[edge.j]) {
                labels[edge.i] = nextLabel
                labels[edge.j] = nextLabel
                used[edge.i] = true
                used[edge.j] = true
                nextLabel++
                Log.d(TAG, "Matched ${edge.i} and ${edge.j} with MSE ${edge.mse}")
            }
        }
        
        // Assign distinct labels to remaining
        for (i in 0 until n) {
            if (labels[i] == 0) {
                labels[i] = nextLabel++
            }
        }
        
        return labels.toList()
    }

    private fun calculateMSE(b1: Bitmap, b2: Bitmap): Double {
        var mse = 0.0
        val w = b1.width
        val h = b1.height
        val p1 = IntArray(w * h)
        val p2 = IntArray(w * h)
        
        b1.getPixels(p1, 0, w, 0, 0, w, h)
        b2.getPixels(p2, 0, w, 0, 0, w, h)
        
        for (i in p1.indices) {
            val c1 = p1[i]
            val c2 = p2[i]
            val rDiff = Color.red(c1) - Color.red(c2)
            val gDiff = Color.green(c1) - Color.green(c2)
            val bDiff = Color.blue(c1) - Color.blue(c2)
            mse += (rDiff * rDiff + gDiff * gDiff + bDiff * bDiff)
        }
        
        return mse / (w * h * 3)
    }
}
