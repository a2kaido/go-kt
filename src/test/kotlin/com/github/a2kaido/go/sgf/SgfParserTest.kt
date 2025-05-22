package com.github.a2kaido.go.sgf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SgfParserTest {

    @Test
    fun testParseSimpleGame() {
        val sgf = "(;FF[4]GM[1]SZ[19];B[pd];W[dp];B[qp])"
        val nodes = parseSgf(sgf)
        assertEquals(4, nodes.size) // Root node + 3 move nodes

        // Root node
        assertEquals(19, nodes[0].size)
        assertEquals("4", nodes[0].properties["FF"]?.first())
        assertEquals("1", nodes[0].properties["GM"]?.first())
        assertNull(nodes[0].blackMove)

        // First move node (B[pd])
        assertNull(nodes[1].size) // Size is usually only in root
        assertEquals(Pair(15, 3), nodes[1].blackMove) // p=15, d=3
        assertNull(nodes[1].whiteMove)

        // Second move node (W[dp])
        assertEquals(Pair(3, 15), nodes[2].whiteMove) // d=3, p=15
        assertNull(nodes[2].blackMove)

        // Third move node (B[qp])
        assertEquals(Pair(16, 15), nodes[3].blackMove) // q=16, p=15
    }

    @Test
    fun testParseSetupStones() {
        val sgf = "(;FF[4]SZ[9]AB[aa][bb][cc]AW[pd][qd])"
        val nodes = parseSgf(sgf)
        assertEquals(1, nodes.size) // Only one node (root node with setup)

        val rootNode = nodes[0]
        assertEquals(9, rootNode.size)
        assertEquals(listOf(Pair(0,0), Pair(1,1), Pair(2,2)), rootNode.addBlack)
        assertEquals(listOf(Pair(15,3), Pair(16,3)), rootNode.addWhite) // p=15, q=16, d=3
    }

    @Test
    fun testCoordinateConversion() {
        // Test relies on the internal helper in SgfNode, which is fine for this structure
        // If it were public, we'd test it directly. Here, we test through node property access.
        val sgfNode = SgfNode(mapOf("B" to listOf("aa"), "W" to listOf("pd")))
        assertEquals(Pair(0,0), sgfNode.blackMove)
        assertEquals(Pair(15,3), sgfNode.whiteMove)

        val sgfNodeMax = SgfNode(mapOf("B" to listOf("ss"))) // s is 18 for 19x19
        assertEquals(Pair(18,18), sgfNodeMax.blackMove)

        assertFailsWith<IllegalArgumentException>("Invalid SGF point format: a") {
            SgfNode(mapOf("B" to listOf("a"))).blackMove
        }
        assertFailsWith<IllegalArgumentException>("Invalid SGF point format: abc") {
            SgfNode(mapOf("B" to listOf("abc"))).blackMove
        }
         assertFailsWith<IllegalArgumentException>("Invalid SGF point format: A") {
            SgfNode(mapOf("B" to listOf("Aa"))).blackMove
        }
    }

    @Test
    fun testParseComments() {
        val sgf = "(;FF[4]C[Root comment here.];B[aa]C[Move comment.])"
        val nodes = parseSgf(sgf)
        assertEquals(2, nodes.size)

        assertEquals("Root comment here.", nodes[0].comment)
        assertNotNull(nodes[0].properties["C"])

        assertEquals("Move comment.", nodes[1].comment)
        assertEquals(Pair(0,0), nodes[1].blackMove)
    }

    @Test
    fun testParseVariationsSkipped() {
        val sgf = "(;FF[4]SZ[19];B[aa](;W[bb]C[Variation];B[bc])(;W[dd];B[de]);B[cc]C[Main line])"
        // Expected main line: Node0(SZ), Node1(B[aa]), Node2(B[cc])
        val nodes = parseSgf(sgf)
        
        assertEquals(3, nodes.size, "Should only parse main variation nodes.")

        assertNotNull(nodes[0].size)
        assertNull(nodes[0].blackMove)

        assertEquals(Pair(0,0), nodes[1].blackMove) // B[aa]
        assertNull(nodes[1].comment) // Comment is in variation

        assertEquals(Pair(2,2), nodes[2].blackMove) // B[cc]
        assertEquals("Main line", nodes[2].comment)

        // Check that variation moves are not present
        nodes.forEach { node ->
            assertNull(node.whiteMove, "White moves from variations should not be present.")
            if(node.blackMove != null) {
                 assert(node.blackMove == Pair(0,0) || node.blackMove == Pair(2,2)) {
                    "Black move ${node.blackMove} from variation should not be present."
                 }
            }
        }
    }
    
    @Test
    fun testParseEscapedCharactersInComment() {
        val sgf = "(;C[This is a comment with an escaped bracket \\] and a backslash \\\\.])"
        val nodes = parseSgf(sgf)
        assertEquals(1, nodes.size)
        assertEquals("This is a comment with an escaped bracket ] and a backslash \\.", nodes[0].comment)
    }

    @Test
    fun testMalformedSgf_UnclosedNode() {
        val sgf = "(;B[aa]" // Missing closing ')' for game tree
        assertFailsWith<IllegalArgumentException>("Malformed SGF: Unbalanced parentheses in variation.") {
            // The current parser might throw this because it tries to skip to end of variation ')' which is never found
            // Or it might parse one node and then fail if it expects more structure.
            // The exact error message might vary based on parser's internal state machine.
            // The key is that it *fails* for unclosed structures.
             parseSgf(sgf)
        }
    }
    
    @Test
    fun testMalformedSgf_UnclosedProperty() {
        val sgf = "(;B[aa;W[bb])" // Missing ']' for B[aa property value
         assertFailsWith<IllegalArgumentException>("Malformed SGF: Property value not closed with ']' for B") {
            parseSgf(sgf)
        }
    }

    @Test
    fun testMalformedSgf_NoOpeningParenthesis() {
        val sgf = ";B[aa];W[bb])"
        assertFailsWith<IllegalArgumentException>("SGF string must start with '('.") {
            parseSgf(sgf)
        }
    }

    @Test
    fun testMalformedSgf_NoNodes() {
        val sgf = "()"
         // This will result in an empty list, not an exception, which is acceptable.
         // If strictness is required (at least one node ';'), the parser would need adjustment.
        val nodes = parseSgf(sgf)
        assertEquals(0, nodes.size)
    }
    
    @Test
    fun testParseMultiplePropertiesInRoot() {
        val sgf = "(;FF[4]GM[1]SZ[19]AP[MyTestApp:1.0]C[Root Comment])"
        val nodes = parseSgf(sgf)
        assertEquals(1, nodes.size)
        val rootNode = nodes[0]
        assertEquals(19, rootNode.size)
        assertEquals("4", rootNode.properties["FF"]?.first())
        assertEquals("1", rootNode.properties["GM"]?.first())
        assertEquals("MyTestApp:1.0", rootNode.properties["AP"]?.first())
        assertEquals("Root Comment", rootNode.comment)
    }

    @Test
    fun testParseNodeWithNoProperties() {
        // While typically nodes have properties, a semicolon alone is technically a node.
        val sgf = "(;;)" // Two nodes, root node has no properties, second node has no properties
        val nodes = parseSgf(sgf)
        assertEquals(2, nodes.size)
        assert(nodes[0].properties.isEmpty())
        assert(nodes[1].properties.isEmpty())
    }
}
