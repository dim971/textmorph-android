package io.github.dim971.textmorph.core

// A port of torph's packages/torph/src/lib/utils/lcs.ts.

/**
 * The longest common subsequence of two sequences, as paired indices.
 *
 * Returns one list of indices into [a] and one into [b], of equal length,
 * pairing the elements that survive from [a] into [b]. Everything not paired is
 * an element that leaves [a] or arrives in [b].
 *
 * The table is filled backwards and walked forwards, so a tie goes to the
 * earliest match. That is not an implementation detail: walked backwards, a word
 * that appears twice in a value pairs with the later occurrence, and the segment
 * then flies the width of the block to reach it.
 */
internal fun <T> lcsIndices(
    a: List<T>,
    b: List<T>,
): Pair<List<Int>, List<Int>> {
    val m = a.size
    val n = b.size
    if (m == 0 || n == 0) return emptyList<Int>() to emptyList()

    // dp[i][j] is the length of the longest common subsequence of a[i...] and
    // b[j...], stored flat because the tables get large and a nested array
    // costs an allocation per row.
    val stride = n + 1
    val dp = IntArray((m + 1) * stride)

    for (i in m - 1 downTo 0) {
        for (j in n - 1 downTo 0) {
            dp[i * stride + j] =
                if (a[i] == b[j]) {
                    dp[(i + 1) * stride + j + 1] + 1
                } else {
                    maxOf(dp[(i + 1) * stride + j], dp[i * stride + j + 1])
                }
        }
    }

    val aIndices = ArrayList<Int>()
    val bIndices = ArrayList<Int>()
    var i = 0
    var j = 0
    while (i < m && j < n) {
        if (a[i] == b[j]) {
            aIndices.add(i)
            bIndices.add(j)
            i += 1
            j += 1
        } else if (dp[(i + 1) * stride + j] >= dp[i * stride + j + 1]) {
            i += 1
        } else {
            j += 1
        }
    }

    return aIndices to bIndices
}
