package com.github.a2kaido.go.sgf

/**
 * Represents a node in an SGF (Smart Game Format) file.
 * Each node can contain various properties like board size, setup stones, or moves.
 *
 * @property size The board size (e.g., 19 for a 19x19 board). Typically found in the root node.
 * @property addBlack A list of 0-indexed (x,y) coordinates where black stones are to be added.
 * @property addWhite A list of 0-indexed (x,y) coordinates where white stones are to be added.
 * @property blackMove A 0-indexed (x,y) coordinate for a black move.
 * @property whiteMove A 0-indexed (x,y) coordinate for a white move.
 * @property comment A comment string associated with this node.
 * @property otherProperties A map to store any other properties encountered that are not explicitly handled.
 */
data class SgfNode(
    val properties: Map<String, List<String>> = emptyMap()
) {
    // Helper to convert SGF point (e.g., "pd") to 0-indexed (x, y)
    private fun sgfPointToCoords(sgfPoint: String): Pair<Int, Int> {
        if (sgfPoint.length != 2) {
            throw IllegalArgumentException("Invalid SGF point format: $sgfPoint (length must be 2)")
        }
        val colChar = sgfPoint[0]
        val rowChar = sgfPoint[1]

        if (colChar !in 'a'..'s' || rowChar !in 'a'..'s') { // Assuming max 19x19 board, 's' is the max coordinate
            throw IllegalArgumentException("Invalid SGF point format: $sgfPoint (coordinates must be a-s)")
        }
        
        val x = colChar - 'a'
        val y = rowChar - 'a'
        return Pair(x, y)
    }

    val size: Int? by lazy {
        properties["SZ"]?.firstOrNull()?.toIntOrNull()
    }

    val addBlack: List<Pair<Int, Int>> by lazy {
        properties["AB"]?.map { sgfPointToCoords(it) } ?: emptyList()
    }

    val addWhite: List<Pair<Int, Int>> by lazy {
        properties["AW"]?.map { sgfPointToCoords(it) } ?: emptyList()
    }

    val blackMove: Pair<Int, Int>? by lazy {
        properties["B"]?.firstOrNull()?.let { sgfPointToCoords(it) }
    }

    val whiteMove: Pair<Int, Int>? by lazy {
        properties["W"]?.firstOrNull()?.let { sgfPointToCoords(it) }
    }

    val comment: String? by lazy {
        properties["C"]?.firstOrNull()
    }
}

/**
 * Parses a string in SGF (Smart Game Format) and returns a list of SgfNode objects
 * representing the main variation of the game.
 *
 * This parser is basic and focuses on SZ, AB, AW, B, W properties.
 * It expects a single game tree and does not handle variations deeply.
 *
 * @param sgfString The raw SGF string.
 * @return A list of SgfNode objects from the main game sequence.
 * @throws IllegalArgumentException if the SGF string is malformed.
 */
fun parseSgf(sgfString: String): List<SgfNode> {
    val nodes = mutableListOf<SgfNode>()
    var currentIndex = 0

    // Find the start of the first game tree and move past '('
    currentIndex = sgfString.indexOf('(')
    if (currentIndex == -1) throw IllegalArgumentException("SGF string must start with '('.")
    currentIndex++

    // Main loop to parse nodes in the main variation
    while (currentIndex < sgfString.length && sgfString[currentIndex] != ')') {
        // Find the start of a node
        currentIndex = sgfString.indexOf(';', currentIndex)
        if (currentIndex == -1) {
            // No more nodes in this sequence, or malformed SGF
            // Could be end of main variation if variations follow, e.g. (;B[aa](;W[bb]))
            break 
        }
        currentIndex++ // Move past ';'

        val properties = mutableMapOf<String, MutableList<String>>()
        // Parse properties for the current node
        while (currentIndex < sgfString.length) {
            // Skip whitespace before property identifier or next control character
            while (currentIndex < sgfString.length && sgfString[currentIndex].isWhitespace()) {
                currentIndex++
            }

            // Check for end of node properties (start of next node, variation, or end of tree)
            if (currentIndex >= sgfString.length || sgfString[currentIndex] == ';' || sgfString[currentIndex] == '(' || sgfString[currentIndex] == ')') {
                break
            }

            // Parse Property Identifier
            val propIdentStart = currentIndex
            while (currentIndex < sgfString.length && sgfString[currentIndex].isUpperCase()) {
                currentIndex++
            }
            val propIdent = sgfString.substring(propIdentStart, currentIndex)
            if (propIdent.isEmpty()) {
                // This can happen if there's unexpected char, e.g., ";;" or "; C[comment]" (space before C)
                // Or if we hit control chars like '(', ')', ';' after whitespace.
                // The outer loop condition or the whitespace skip should handle this.
                // If we are here, it might be a malformed SGF or end of properties for this node.
                break // Break from property parsing loop for this node.
            }

            // Parse Property Values
            val currentPropValues = mutableListOf<String>()
            while (currentIndex < sgfString.length && sgfString[currentIndex] == '[') {
                currentIndex++ // Skip '['
                val valueBuilder = StringBuilder()
                var escape = false
                while (currentIndex < sgfString.length) {
                    val char = sgfString[currentIndex]
                    if (escape) {
                        valueBuilder.append(char)
                        escape = false
                    } else {
                        when (char) {
                            '\\' -> escape = true
                            ']' -> break // End of value
                            '[' -> throw IllegalArgumentException("Malformed SGF: Unexpected '[' inside property value for $propIdent")
                            else -> valueBuilder.append(char)
                        }
                    }
                    currentIndex++
                }

                if (currentIndex >= sgfString.length || sgfString[currentIndex] != ']') {
                     // This condition implies the loop terminated due to end of string before finding ']' without escape.
                    throw IllegalArgumentException("Malformed SGF: Property value not closed with ']' for $propIdent")
                }
                // Loop broke on ']', so current char is ']'
                currentPropValues.add(valueBuilder.toString())
                currentIndex++ // Skip ']' (move past the character that broke the loop)
            }
            
            if (currentPropValues.isNotEmpty()) {
                 properties.getOrPut(propIdent) { mutableListOf() }.addAll(currentPropValues)
            } else if (propIdent.isNotEmpty()) {
                // Property exists but has no value, e.g. "N[]" is not valid, but "TESUME" might be seen as valueless by a naive parser.
                // Standard SGF properties always have values within []. If not, it's a format violation or a property without value (e.g. GM, not GM[1])
                // For now, we only add properties that had values.
                // According to SGF spec, PropValue is mandatory: Property = PropIdent PropValue { PropValue }
                // So, if propIdent is not empty, currentPropValues should not be empty.
                // This case implies a parsing error or non-standard SGF.
                // We can choose to be strict and throw error, or lenient and ignore such props.
                // For now, ignoring, as we only add if currentPropValues.isNotEmpty().
            }
        }

        // A node is defined by a ';', even if it has no properties.
        nodes.add(SgfNode(properties.mapValues { it.value.toList() }))

        // After parsing properties of a node, check for variations.
        // If one or more variations `(...) (...)` are found, skip them to stay on the main line.
        var nextTokenIndex = currentIndex
        while(nextTokenIndex < sgfString.length && sgfString[nextTokenIndex].isWhitespace()){
            nextTokenIndex++
        }

        while (nextTokenIndex < sgfString.length && sgfString[nextTokenIndex] == '(') {
            currentIndex = nextTokenIndex // Current index is at the start of a variation block '('
            var nestingLevel = 0
            do {
                val char = sgfString[currentIndex]
                if (char == '(') nestingLevel++
                else if (char == ')') nestingLevel--
                currentIndex++ // Consume char
                if (currentIndex >= sgfString.length && nestingLevel > 0) { // Reached EOF while inside a variation
                    throw IllegalArgumentException("Malformed SGF: Unbalanced parentheses, EOF reached within variation.")
                }
            } while (nestingLevel > 0) // Loop until this variation block is closed

            // After skipping one variation, check if the *next* char (after potential whitespace) is also '('.
            nextTokenIndex = currentIndex
            while(nextTokenIndex < sgfString.length && sgfString[nextTokenIndex].isWhitespace()){
                nextTokenIndex++
            }
            // The outer while loop will re-evaluate if sgfString[nextTokenIndex] is '('.
        }
        // The main loop `while (currentIndex < sgfString.length && sgfString[currentIndex] != ')')`
        // will then continue, looking for the next ';' or the final ')' of the game tree.
    }

    if (currentIndex >= sgfString.length || sgfString[currentIndex] != ')') {
        // If the loop finished but we are not at a ')' (e.g. end of string reached before finding ')')
        throw IllegalArgumentException("Malformed SGF: Game tree not properly closed with ')'")
    }
    return nodes
}

// Example Usage (for testing purposes, can be removed or moved to a test file)
fun main() {
    val sgfExamples = listOf(
        "(;FF[4]GM[1]SZ[19];B[pd];W[dp];B[qp])" to "Simple 19x19 game",
        "(;FF[4]GM[1]SZ[9]AB[aa][bb][cc]AW[pd][qd];B[pe];W[df])" to "9x9 with setup stones",
        "(;FF[4]SZ[19];B[aa];W[bb](;B[cc];W[dd];B[ad])(;B[ee]))" to "Game with variations",
        "(;FF[4]C[Root comment];B[ab]C[Black's move comment])" to "Game with comments",
        "(;GM[1];B[aa];W[bb];B[\\(\\\\)cc\\]])" to "Game with escaped characters in comment/value (B value is not standard here, but testing escape)"
    )

    sgfExamples.forEach { (sgf, description) ->
        println("-----\nParsing SGF: $description\n$sgf")
        try {
            val parsedNodes = parseSgf(sgf)
            if (parsedNodes.isEmpty()) {
                println("  No nodes parsed.")
            }
            parsedNodes.forEachIndexed { index, node ->
                println("  Node ${index + 1}:")
                node.size?.let { println("    Size: $it") }
                if (node.addBlack.isNotEmpty()) println("    Add Black: ${node.addBlack}")
                if (node.addWhite.isNotEmpty()) println("    Add White: ${node.addWhite}")
                node.blackMove?.let { println("    Black Move: $it") }
                node.whiteMove?.let { println("    White Move: $it") }
                node.comment?.let { println("    Comment: ${node.comment}")}
                // println("    All Props: ${node.properties}") // Can be verbose
            }
        } catch (e: Exception) {
            println("  Error parsing SGF: ${e.message}")
            // e.printStackTrace() // For detailed debugging
        }
        println()
    }

    // Test for malformed SGF
    val malformedSgf = "(;B[aa];W[bb" // Missing closing bracket for value
    println("-----\nParsing SGF: Malformed SGF\n$malformedSgf")
    try {
        parseSgf(malformedSgf)
    } catch (e: Exception) {
        println("  Error parsing SGF: ${e.message} (Expected)")
    }
}
