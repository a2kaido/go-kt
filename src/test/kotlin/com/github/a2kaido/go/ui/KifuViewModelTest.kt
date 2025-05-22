package com.github.a2kaido.go.ui

import com.github.a2kaido.go.model.Board
import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.sgf.SgfNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KifuViewModelTest {

    // Helper to create SgfNode for testing moves easily
    private fun createMoveNode(player: Player, x: Int, y: Int, comment: String? = null): SgfNode {
        val coord = ('a' + x).toString() + ('a' + y).toString()
        val properties = mutableMapOf<String, List<String>>()
        if (player == Player.BLACK) {
            properties["B"] = listOf(coord)
        } else {
            properties["W"] = listOf(coord)
        }
        comment?.let { properties["C"] = listOf(it) }
        return SgfNode(properties)
    }

    // Helper for setup nodes
    private fun createSetupNode(
        size: Int?,
        addBlack: List<Pair<Int, Int>> = emptyList(),
        addWhite: List<Pair<Int, Int>> = emptyList(),
        comment: String? = null
    ): SgfNode {
        val properties = mutableMapOf<String, MutableList<String>>()
        size?.let { properties.getOrPut("SZ") { mutableListOf() }.add(it.toString()) }
        if (addBlack.isNotEmpty()) {
            properties.getOrPut("AB") { mutableListOf() }
                .addAll(addBlack.map { (x, y) -> ('a' + x).toString() + ('a' + y).toString() })
        }
        if (addWhite.isNotEmpty()) {
            properties.getOrPut("AW") { mutableListOf() }
                .addAll(addWhite.map { (x, y) -> ('a' + x).toString() + ('a' + y).toString() })
        }
        comment?.let { properties.getOrPut("C") { mutableListOf() }.add(it) }
        return SgfNode(properties.mapValues { it.value.toList() })
    }

    @Test
    fun testInitialBoardSetup_WithSizeAndStones() {
        val nodes = listOf(
            createSetupNode(size = 9, addBlack = listOf(Pair(0, 0), Pair(1, 1)), addWhite = listOf(Pair(2, 2)))
        )
        val viewModel = KifuViewModel(nodes)

        assertEquals(9, viewModel.currentBoard.size)
        assertEquals(Player.BLACK, viewModel.currentBoard.getStone(0, 0))
        assertEquals(Player.BLACK, viewModel.currentBoard.getStone(1, 1))
        assertEquals(Player.WHITE, viewModel.currentBoard.getStone(2, 2))
        assertEquals(0, viewModel.currentMoveDisplayNumber, "Move number should be 0 for setup")
        assertEquals(0, viewModel.currentSgfNodeIndex, "SGF node index should be 0 for setup")
        assertTrue(viewModel.hasSgfNodes)
    }
    
    @Test
    fun testInitialBoardSetup_DefaultSize() {
        val nodes = listOf(createSetupNode(size = null)) // No SZ property
        val viewModel = KifuViewModel(nodes)
        assertEquals(19, viewModel.currentBoard.size, "Default board size should be 19")
        assertEquals(0, viewModel.currentMoveDisplayNumber)
    }

    @Test
    fun testInitialBoardSetup_EmptySgf() {
        val viewModel = KifuViewModel(emptyList())
        assertEquals(19, viewModel.currentBoard.size) // Default size
        assertTrue(viewModel.currentBoard.stones.isEmpty())
        assertEquals(0, viewModel.currentMoveDisplayNumber)
        assertEquals(-1, viewModel.currentSgfNodeIndex)
        assertFalse(viewModel.hasSgfNodes)
        assertTrue(viewModel.isAtStartOfKifu) // Considered at start
        assertTrue(viewModel.isAtEndOfKifu)   // And also at end
    }


    @Test
    fun testNextMove() {
        val nodes = listOf(
            createSetupNode(size = 19, comment = "Setup complete"), // Node 0 (Setup)
            createMoveNode(Player.BLACK, 3, 3, "B move 1"),      // Node 1 (Move 1)
            createMoveNode(Player.WHITE, 4, 4, "W move 2")       // Node 2 (Move 2)
        )
        val viewModel = KifuViewModel(nodes) // Starts at setup (node 0, move 0)

        // Initial state (Setup)
        assertEquals(0, viewModel.currentMoveDisplayNumber)
        assertEquals(0, viewModel.currentSgfNodeIndex)
        assertEquals("Setup complete", viewModel.currentComment)

        // After 1st nextMove (to Move 1)
        viewModel.nextMove()
        assertEquals(Player.BLACK, viewModel.currentBoard.getStone(3, 3))
        assertEquals(1, viewModel.currentMoveDisplayNumber, "Should be move 1")
        assertEquals(1, viewModel.currentSgfNodeIndex, "SGF node index should be 1")
        assertEquals("B move 1", viewModel.currentComment)

        // After 2nd nextMove (to Move 2)
        viewModel.nextMove()
        assertEquals(Player.WHITE, viewModel.currentBoard.getStone(4, 4))
        assertEquals(2, viewModel.currentMoveDisplayNumber, "Should be move 2")
        assertEquals(2, viewModel.currentSgfNodeIndex, "SGF node index should be 2")
        assertEquals("W move 2", viewModel.currentComment)

        // Try nextMove at the end
        viewModel.nextMove() // Should do nothing
        assertEquals(2, viewModel.currentMoveDisplayNumber, "Should remain at last move")
        assertEquals(2, viewModel.currentSgfNodeIndex, "Should remain at last SGF node index")
    }
    
    @Test
    fun testNextMove_RootNodeIsAlsoAMove() {
        val nodes = listOf(
            SgfNode(mapOf("SZ" to listOf("9"), "B" to listOf("aa"), "C" to listOf("Root is B move"))), // Node 0, is Move 1
            createMoveNode(Player.WHITE, 1,1, "W move 2") // Node 1, is Move 2
        )
        val viewModel = KifuViewModel(nodes) // init calls goToBoardSetup, which calls goToMove(0)
        
        // Initial state should be after root node's move
        assertEquals(1, viewModel.currentMoveDisplayNumber, "Initial display number for root move")
        assertEquals(0, viewModel.currentSgfNodeIndex, "Initial SGF index for root move")
        assertEquals(Player.BLACK, viewModel.currentBoard.getStone(0,0))
        assertEquals("Root is B move", viewModel.currentComment)

        viewModel.nextMove()
        assertEquals(2, viewModel.currentMoveDisplayNumber)
        assertEquals(1, viewModel.currentSgfNodeIndex)
        assertEquals(Player.WHITE, viewModel.currentBoard.getStone(1,1))
        assertEquals("W move 2", viewModel.currentComment)
    }


    @Test
    fun testPreviousMove() {
        val nodes = listOf(
            createSetupNode(size = 19, comment = "Setup"),         // Node 0
            createMoveNode(Player.BLACK, 3, 3, "B move 1"),   // Node 1
            createMoveNode(Player.WHITE, 4, 4, "W move 2")    // Node 2
        )
        val viewModel = KifuViewModel(nodes)
        
        viewModel.goToLastMove() // Go to W move at (4,4), move 2, node 2
        assertEquals(2, viewModel.currentMoveDisplayNumber)
        assertEquals(2, viewModel.currentSgfNodeIndex)
        assertEquals(Player.WHITE, viewModel.currentBoard.getStone(4,4))

        // Previous to B move 1 (node 1)
        viewModel.previousMove()
        assertEquals(1, viewModel.currentMoveDisplayNumber)
        assertEquals(1, viewModel.currentSgfNodeIndex)
        assertNull(viewModel.currentBoard.getStone(4,4), "W stone should be gone")
        assertEquals(Player.BLACK, viewModel.currentBoard.getStone(3,3))
        assertEquals("B move 1", viewModel.currentComment)

        // Previous to Setup (node 0)
        viewModel.previousMove()
        assertEquals(0, viewModel.currentMoveDisplayNumber, "Should be at setup (move 0)")
        assertEquals(0, viewModel.currentSgfNodeIndex, "SGF node index should be 0 for setup")
        assertNull(viewModel.currentBoard.getStone(3,3), "B stone should be gone")
        assertEquals("Setup", viewModel.currentComment)

        // Previous from Setup (should go to empty board, index -1)
        viewModel.previousMove()
        assertEquals(0, viewModel.currentMoveDisplayNumber, "Display number for empty board")
        assertEquals(-1, viewModel.currentSgfNodeIndex, "SGF node index for empty board")
        assertTrue(viewModel.currentBoard.stones.isEmpty(), "Board should be empty")
        assertNull(viewModel.currentComment, "Comment should be null for empty board")

        // Try previousMove at the very beginning
        viewModel.previousMove() // Should do nothing
        assertEquals(-1, viewModel.currentSgfNodeIndex)
    }

    @Test
    fun testGoToMove() {
        val nodes = listOf(
            createSetupNode(size = 19, addBlack = listOf(Pair(0,0))), // Node 0 (Setup)
            createMoveNode(Player.BLACK, 3, 3),                      // Node 1 (Move 1)
            createMoveNode(Player.WHITE, 4, 4),                      // Node 2 (Move 2)
            createMoveNode(Player.BLACK, 5, 5)                       // Node 3 (Move 3)
        )
        val viewModel = KifuViewModel(nodes)

        // Go to Move 2 (SGF Node 2)
        viewModel.goToMove(2)
        assertEquals(Player.WHITE, viewModel.currentBoard.getStone(4, 4))
        assertEquals(Player.BLACK, viewModel.currentBoard.getStone(0,0)) // from setup
        assertEquals(2, viewModel.currentMoveDisplayNumber)
        assertEquals(2, viewModel.currentSgfNodeIndex)

        // Go to Setup (SGF Node 0)
        viewModel.goToMove(0)
        assertTrue(viewModel.currentBoard.stones.size == 1 && viewModel.currentBoard.getStone(0,0) == Player.BLACK)
        assertEquals(0, viewModel.currentMoveDisplayNumber)
        assertEquals(0, viewModel.currentSgfNodeIndex)
        
        // Go to Empty board state (before setup)
        viewModel.goToMove(-1)
        assertTrue(viewModel.currentBoard.stones.isEmpty())
        assertEquals(0, viewModel.currentMoveDisplayNumber) // Display number is 0 for empty state
        assertEquals(-1, viewModel.currentSgfNodeIndex)

        // Go to last move (SGF Node 3)
        viewModel.goToMove(3)
        assertEquals(Player.BLACK, viewModel.currentBoard.getStone(5,5))
        assertEquals(3, viewModel.currentMoveDisplayNumber)
        assertEquals(3, viewModel.currentSgfNodeIndex)
    }
    
    @Test
    fun testGoToMove_WithRootNodeAsMove() {
         val nodes = listOf(
            SgfNode(mapOf("SZ" to listOf("9"), "B" to listOf("aa"))), // Node 0, Move 1
            createMoveNode(Player.WHITE, 1,1)  // Node 1, Move 2
        )
        val viewModel = KifuViewModel(nodes)

        // After init, should be at Node 0, Move 1
        assertEquals(1, viewModel.currentMoveDisplayNumber)
        assertEquals(0, viewModel.currentSgfNodeIndex)

        // Go to Node 1 (Move 2)
        viewModel.goToMove(1)
        assertEquals(2, viewModel.currentMoveDisplayNumber)
        assertEquals(1, viewModel.currentSgfNodeIndex)
        assertEquals(Player.WHITE, viewModel.currentBoard.getStone(1,1))

        // Go back to Node 0 (Move 1)
        viewModel.goToMove(0)
        assertEquals(1, viewModel.currentMoveDisplayNumber)
        assertEquals(0, viewModel.currentSgfNodeIndex)
        assertEquals(Player.BLACK, viewModel.currentBoard.getStone(0,0))
        assertNull(viewModel.currentBoard.getStone(1,1))
    }


    @Test
    fun testButtonEnablementStates() {
        val nodes = listOf(
            createSetupNode(size = 19),         // Node 0
            createMoveNode(Player.BLACK, 3, 3),   // Node 1
            createMoveNode(Player.WHITE, 4, 4)    // Node 2
        )
        val viewModel = KifuViewModel(nodes) // Starts at setup (node 0, move 0)

        // Initial state (Setup)
        assertTrue(viewModel.isAtStartOfKifu, "Should be at start when at setup display")
        assertFalse(viewModel.isAtEndOfKifu)

        // Next move (Move 1)
        viewModel.nextMove()
        assertFalse(viewModel.isAtStartOfKifu)
        assertFalse(viewModel.isAtEndOfKifu)

        // Next move (Move 2 - End of Kifu)
        viewModel.nextMove()
        assertFalse(viewModel.isAtStartOfKifu)
        assertTrue(viewModel.isAtEndOfKifu)

        // Previous move (Move 1)
        viewModel.previousMove()
        assertFalse(viewModel.isAtStartOfKifu)
        assertFalse(viewModel.isAtEndOfKifu)
        
        // Previous move (Setup)
        viewModel.previousMove()
        assertTrue(viewModel.isAtStartOfKifu) // Back to setup
        assertFalse(viewModel.isAtEndOfKifu)

        // Previous move (Empty board state)
        viewModel.previousMove()
        assertTrue(viewModel.isAtStartOfKifu) // At -1, display 0
        assertFalse(viewModel.isAtEndOfKifu) // Not at end unless SGF was empty
    }
    
    @Test
    fun testButtonEnablement_EmptySgf() {
        val viewModel = KifuViewModel(emptyList())
        assertTrue(viewModel.isAtStartOfKifu)
        assertTrue(viewModel.isAtEndOfKifu)
        assertFalse(viewModel.hasSgfNodes)
    }


    @Test
    fun testTotalPlayableMoves() {
        // SGF with setup and 2 moves
        val nodes1 = listOf(
            createSetupNode(size = 19),
            createMoveNode(Player.BLACK, 3, 3),
            createMoveNode(Player.WHITE, 4, 4)
        )
        val viewModel1 = KifuViewModel(nodes1)
        assertEquals(2, viewModel1.totalPlayableMoves)

        // SGF with only setup (no B/W properties)
        val nodes2 = listOf(createSetupNode(size = 9, addBlack = listOf(Pair(0,0))))
        val viewModel2 = KifuViewModel(nodes2)
        assertEquals(0, viewModel2.totalPlayableMoves)

        // SGF where root node is also a move
        val nodes3 = listOf(SgfNode(mapOf("SZ" to listOf("9"), "B" to listOf("aa"))))
        val viewModel3 = KifuViewModel(nodes3)
        assertEquals(1, viewModel3.totalPlayableMoves)
        
        // Empty SGF
        val viewModel4 = KifuViewModel(emptyList())
        assertEquals(0, viewModel4.totalPlayableMoves)
    }

    @Test
    fun testNavigationToFirstAndLastPlayableMove() {
        val nodes = listOf(
            createSetupNode(size = 19, comment = "Setup"),         // Node 0
            createMoveNode(Player.BLACK, 3, 3, "B move 1"),   // Node 1
            createMoveNode(Player.WHITE, 4, 4, "W move 2")    // Node 2
        )
        val viewModel = KifuViewModel(nodes)

        viewModel.goToFirstPlayableMove()
        assertEquals(1, viewModel.currentMoveDisplayNumber)
        assertEquals(1, viewModel.currentSgfNodeIndex)
        assertEquals("B move 1", viewModel.currentComment)

        viewModel.goToLastMove()
        assertEquals(2, viewModel.currentMoveDisplayNumber)
        assertEquals(2, viewModel.currentSgfNodeIndex)
        assertEquals("W move 2", viewModel.currentComment)
    }

    @Test
    fun testNavigationToFirstAndLast_WhenNoMoves() {
        val nodes = listOf(createSetupNode(size = 9, comment = "Only Setup"))
        val viewModel = KifuViewModel(nodes)

        viewModel.goToFirstPlayableMove() // Should go to setup as no moves
        assertEquals(0, viewModel.currentMoveDisplayNumber)
        assertEquals(0, viewModel.currentSgfNodeIndex)
        assertEquals("Only Setup", viewModel.currentComment)

        viewModel.goToLastMove() // Should also go to setup
        assertEquals(0, viewModel.currentMoveDisplayNumber)
        assertEquals(0, viewModel.currentSgfNodeIndex)
    }
    
    @Test
    fun testGoToBoardSetup_WhenRootIsAlsoAMove() {
        // Root node has setup (SZ) and a move (B).
        // initialBoardState will be empty 9x9.
        // goToBoardSetup() should show this initialBoardState (empty 9x9), move number 0.
        val nodes = listOf(
            SgfNode(mapOf("SZ" to listOf("9"), "B" to listOf("aa"), "C" to listOf("Root B move")))
        )
        val viewModel = KifuViewModel(nodes)
        
        // Initially, after constructor, it calls goToBoardSetup(), then processes node 0.
        // If node 0 is a move, it becomes move 1.
        assertEquals(1, viewModel.currentMoveDisplayNumber, "After init, should be at move 1")
        assertEquals(0, viewModel.currentSgfNodeIndex)
        assertEquals(Player.BLACK, viewModel.currentBoard.getStone(0,0))

        // Now explicitly call goToBoardSetup()
        viewModel.goToBoardSetup()
        assertEquals(0, viewModel.currentMoveDisplayNumber, "Display number after goToBoardSetup")
        assertEquals(0, viewModel.currentSgfNodeIndex, "SGF index after goToBoardSetup")
        assertTrue(viewModel.currentBoard.stones.isEmpty(), "Board should be empty (initial setup state) after goToBoardSetup")
        assertEquals("Root B move", viewModel.currentComment, "Comment should be from root node even at setup")
    }
}
