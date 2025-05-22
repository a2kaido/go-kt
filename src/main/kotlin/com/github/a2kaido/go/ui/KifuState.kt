package com.github.a2kaido.go.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.github.a2kaido.go.model.Board
import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.sgf.SgfNode

class KifuViewModel(private val sgfNodes: List<SgfNode>) {
    private var initialBoardSetupDone = false
    private lateinit var initialBoardState: Board // Board after SZ, AB, AW from root node
    private var boardSize: Int = 19 

    // currentSgfNodeIndex refers to the index in sgfNodes.
    // -1: Before any node is processed (empty board, before setup from root node)
    //  0: Root node processed. If it's only setup (AB/AW), this is the "setup state". If it also contains B/W, it's move 1.
    //  k: sgfNodes[k] has been processed.
    var currentSgfNodeIndex by mutableStateOf(-1)
        private set

    var currentBoard by mutableStateOf(Board.empty(boardSize))
        private set

    // Represents the displayed move number. 0 for setup, 1 for first move, etc.
    var currentMoveDisplayNumber by mutableStateOf(0)
        private set

    val totalPlayableMoves: Int by lazy {
        // Count nodes that contain a B or W move property.
        // Typically, the root node (index 0) is setup. Moves start from index 1.
        // However, a root node *can* contain a move.
        // This counts how many actual B/W moves are in the sequence.
        sgfNodes.count { it.blackMove != null || it.whiteMove != null }
    }
    
    val currentComment: String?
        get() = if (currentSgfNodeIndex >= 0 && currentSgfNodeIndex < sgfNodes.size) {
            sgfNodes[currentSgfNodeIndex].comment
        } else {
            null
        }

    // Index of the first SGF node that contains a playable move (B or W)
    private val firstPlayableMoveSgfIndex: Int by lazy {
        sgfNodes.indexOfFirst { it.blackMove != null || it.whiteMove != null }.let { if (it == -1) 0 else it }
    }
    
    // Index of the last SGF node (could be setup or move)
    private val lastSgfNodeIndex: Int by lazy {
        if (sgfNodes.isEmpty()) -1 else sgfNodes.size - 1
    }

    val hasSgfNodes: Boolean get() = sgfNodes.isNotEmpty()

    val isAtStartOfKifu: Boolean get() {
        if (!hasSgfNodes) return true // No nodes, so effectively at start and end
        // True if at -1 (empty board) or at index 0 and it's a setup-only node (display num 0)
        // or if at the first playable move and that's also index 0.
        return currentSgfNodeIndex <= 0 && currentMoveDisplayNumber == 0 
    }
    
    val isAtRealStartOfKifu: Boolean get() = currentSgfNodeIndex <= firstPlayableMoveSgfIndex && currentMoveDisplayNumber <=1 && !(currentSgfNodeIndex == 0 && currentMoveDisplayNumber == 0 && totalPlayableMoves > 0)


    val isAtEndOfKifu: Boolean get() {
        if (!hasSgfNodes) return true
        return currentSgfNodeIndex == lastSgfNodeIndex
    }


    init {
        if (hasSgfNodes) {
            val rootNode = sgfNodes.first()
            boardSize = rootNode.size ?: 19
            currentBoard = Board.empty(boardSize) // Ensure board is initialized with correct size
            setupInitialBoardState() // Populates initialBoardState
            goToMove(0) // Initialize to the state after processing the root node (incl. its move if any)
        } else {
            // Handle empty SGF or provide a default state
            currentBoard = Board.empty(boardSize) 
            currentSgfNodeIndex = -1 // No nodes to process
            currentMoveDisplayNumber = 0
        }
    }

    private fun setupInitialBoardState() {
        val rootNode = sgfNodes.first() // Should only be called if sgfNodes is not empty
        // boardSize is already set from rootNode.size or default
        var board = Board.empty(boardSize)

        rootNode.addBlack.forEach { (x, y) ->
            if (board.isOnBoard(x,y) && board.getStone(x,y) == null) {
                board = board.placeStone(Player.BLACK, x, y)
            }
        }
        rootNode.addWhite.forEach { (x, y) ->
            if (board.isOnBoard(x,y) && board.getStone(x,y) == null) {
                board = board.placeStone(Player.WHITE, x, y)
            }
        }
        initialBoardState = board // This is the state after AB/AW from root.
        initialBoardSetupDone = true
    }
    
    // Applies a B or W move from the given node to the board
    private fun applyPlayMove(node: SgfNode, board: Board): Board {
        var newBoard = board
        // Note: SGF standard is one move (B or W) per node.
        // If a node has both, behavior might be parser-dependent or based on game type.
        // This KifuViewModel assumes B or W, prioritizing B if somehow both are present.
        val moveNode = node.blackMove ?: node.whiteMove
        val player = if (node.blackMove != null) Player.BLACK else Player.WHITE

        moveNode?.let { (x,y) ->
            if (newBoard.isOnBoard(x,y) && newBoard.getStone(x,y) == null) {
                 newBoard = newBoard.placeStone(player, x, y)
            } else {
                // Illegal move in SGF (e.g. occupied point). For now, attempt and let Board handle/throw.
                // A more robust viewer might log/display SGF inconsistencies.
                try { newBoard = newBoard.placeStone(player, x, y) } catch (e: Exception) { /* log e.message */ }
            }
        }
        return newBoard
    }

    fun nextMove() {
        if (!initialBoardSetupDone && sgfNodes.isNotEmpty()) setupInitialBoardState()
        if (sgfNodes.isEmpty() || currentSgfNodeIndex >= lastSgfNodeIndex) return

        val nextSgfNodeToProcessIndex = currentSgfNodeIndex + 1
        goToMove(nextSgfNodeToProcessIndex)
    }

    fun previousMove() {
        if (!initialBoardSetupDone && hasSgfNodes) setupInitialBoardState()
        if (!hasSgfNodes || (currentSgfNodeIndex < 0)) return // currentSgfNodeIndex check might be redundant with isAtRealStartOfKifu logic

        // If at the first actual move, previous should go to setup (index 0, display 0)
        // If at setup (index 0, display 0), previous should go to empty (index -1)
        if (currentSgfNodeIndex == firstPlayableMoveSgfIndex && currentMoveDisplayNumber == 1) {
             goToBoardSetup() // Go to setup before first move
        } else if (currentSgfNodeIndex == 0 && currentMoveDisplayNumber == 0) {
             goToMove(-1) // Go to empty board state
        }
        else {
            val targetSgfNodeIndex = currentSgfNodeIndex -1
            goToMove(targetSgfNodeIndex)
        }
    }
    
    fun goToBoardSetup() { 
        if (!hasSgfNodes) { goToMove(-1); return } // Go to empty if no nodes
        // This should always show the board after AB/AW, before any B/W moves from root.
        // So, target SGF node is 0, but ensure display number is 0.
        // goToMove(0) will try to calculate move numbers.
        // A more direct way:
        if (!initialBoardSetupDone && hasSgfNodes) setupInitialBoardState()
        currentBoard = initialBoardState
        currentSgfNodeIndex = 0
        currentMoveDisplayNumber = 0
    }

    fun goToFirstPlayableMove() {
        if (!hasSgfNodes || totalPlayableMoves == 0) {
            // If no playable moves, go to setup or empty.
            goToBoardSetup()
            return
        }
        goToMove(firstPlayableMoveSgfIndex)
    }
    
    fun goToLastMove() {
        if (!hasSgfNodes || totalPlayableMoves == 0) {
            goToBoardSetup()
            return
        }
        // This should go to the SGF node that contains the last B/W move.
        var lastMoveNodeIndex = -1
        for(i in sgfNodes.indices.reversed()) {
            if (sgfNodes[i].blackMove != null || sgfNodes[i].whiteMove != null) {
                lastMoveNodeIndex = i
                break
            }
        }
        
        if (lastMoveNodeIndex != -1) {
            goToMove(lastMoveNodeIndex)
        } else { // No moves found, just setup
            goToBoardSetup()
        }
    }

    // targetSgfNodeIndex: -1 for empty board, 0 for root node (setup), 1+ for subsequent nodes.
    fun goToMove(targetSgfNodeIndex: Int) {
        if (!initialBoardSetupDone && hasSgfNodes) setupInitialBoardState()
        if (!hasSgfNodes && targetSgfNodeIndex != -1) { // Can only go to -1 (empty) if no nodes
             goToMove(-1) // Ensure clean empty state
             return
        }
        if (!hasSgfNodes && targetSgfNodeIndex == -1) { // No nodes and going to -1
            currentBoard = Board.empty(boardSize)
            currentSgfNodeIndex = -1
            currentMoveDisplayNumber = 0
            return
        }


        val effectiveTargetIndex = targetSgfNodeIndex.coerceIn(-1, lastSgfNodeIndex)

        if (effectiveTargetIndex == -1) { // Empty board state (before any setup or for empty SGF)
            currentBoard = Board.empty(boardSize) // Use the determined or default boardSize
            currentSgfNodeIndex = -1
            currentMoveDisplayNumber = 0 
            return
        }
        
        // Start from the initialBoardState (which is after root AB/AW)
        var newBoard = initialBoardState 
        var moveCounter = 0

        // Iterate through SGF nodes from the beginning up to the effectiveTargetIndex
        // The root node (index 0) might contain a move OR just setup.
        // sgfNodes must be non-empty here due to earlier checks if effectiveTargetIndex >=0
        for (i in 0..effectiveTargetIndex) { 
            val node = sgfNodes[i] // Safe access
            if (node.blackMove != null || node.whiteMove != null) {
                // If i == 0 (root node) and it's a move, initialBoardState was just setup. Apply this move to it.
                // Otherwise, apply to the progressively built newBoard.
                newBoard = applyPlayMove(node, if (i == 0 && newBoard == initialBoardState) initialBoardState else newBoard)
                moveCounter++
            } else if (i == 0) { // Root node with no B/W move, only setup.
                // newBoard is already initialBoardState (from the start of this block), no further action for node 0 here.
                // moveCounter remains 0 for this setup-only node.
            }
        }
        
        currentBoard = newBoard
        currentSgfNodeIndex = effectiveTargetIndex
        
        // Update display number
        // If target is root (0) and it's just setup (no B/W move in root node), display 0. Else, use moveCounter.
        if (effectiveTargetIndex == 0 && sgfNodes[0].blackMove == null && sgfNodes[0].whiteMove == null) {
            currentMoveDisplayNumber = 0
        } else {
            currentMoveDisplayNumber = moveCounter
        }
    }
}
