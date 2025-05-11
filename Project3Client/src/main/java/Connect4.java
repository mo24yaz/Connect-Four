import java.util.Random;
import java.util.Vector;

public class Connect4 {
    public static final int numRows = 6;
    public static final int numCols = 7;

    public static Vector<Vector<Character>> board;
    public static char currPlayer;
    public boolean gameEnded;
    public static int numPlays = 0;

    public Connect4() {
        reset();
    }

    // Reset to new empty board. (Beginning of new game)
    public void reset() {
        board = new Vector<>();
        for (int row = 0; row < numRows; row++) {
            Vector<Character> temp = new Vector<>();
            for (int col = 0; col < numCols; col++) {
                temp.add('-');
            }
            board.add(temp);
        }
        currPlayer = 'X';
        gameEnded     = false;
        numPlays      = 0;
    }

    // Return the top most open slot for the chosen column.
    public int getNextOpenRow(int col) {
        for (int row = numRows - 1; row >= 0; row--) {
            if (board.get(row).get(col) == '-') return row;
        }
        return -1;
    }

    // Create a deep copy of the board.
    private static Vector<Vector<Character>> copyBoard(Vector<Vector<Character>> original) {
        Vector<Vector<Character>> copy = new Vector<>();
        for (Vector<Character> row : original) {
            copy.add(new Vector<>(row));
        }
        return copy;
    }

    // Update the board with the new move.
    public Vector<Vector<Character>> updateBoard(int col, char player) {
        Vector<Vector<Character>> copy = copyBoard(board);
        for (int row = numRows - 1; row >= 0; row--) {
            if (copy.get(row).get(col) == '-') {
                copy.get(row).set(col, player);
                break;
            }
        }
        return copy;
    }

    // Drop a piece for the given player.
    public void drop(char player, int col) {
        board = updateBoard(col, player);
        numPlays++;
    }

    // Check if the column is full.
    public boolean isColumnFull(int col) {
        return board.getFirst().get(col) != '-';
    }

    // Check if the board is full.
    public boolean isBoardFull() {
        for (int col = 0; col < numCols; col++) {
            if (!isColumnFull(col)) return false;
        }
        return true;
    }

    // Switch the player.
    public void switchPlayer() {
        currPlayer = (currPlayer == 'X' ? 'O' : 'X');
    }

    // Check for win.
    public static boolean checkForWin() {
        return checkForWin(board, currPlayer);
    }

    // Check 4 in a row in all directions.
    public static boolean checkForWin(Vector<Vector<Character>> Board, char player) {
        // horizontal
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col <= numCols - 4; col++) {
                if (Board.get(row).get(col) == player &&
                        Board.get(row).get(col+1) == player &&
                        Board.get(row).get(col+2) == player &&
                        Board.get(row).get(col+3) == player) {
                    return true;
                }
            }
        }
        // vertical
        for (int col = 0; col < numCols; col++) {
            for (int row = 0; row <= numRows - 4; row++) {
                if (Board.get(row).get(col)   == player &&
                        Board.get(row+1).get(col) == player &&
                        Board.get(row+2).get(col) == player &&
                        Board.get(row+3).get(col) == player) {
                    return true;
                }
            }
        }
        // diagonal Top left -> Bottom right
        for (int row = 0; row <= numRows - 4; row++) {
            for (int col = 0; col <= numCols - 4; col++) {
                if (Board.get(row).get(col)     == player &&
                        Board.get(row+1).get(col+1) == player &&
                        Board.get(row+2).get(col+2) == player &&
                        Board.get(row+3).get(col+3) == player) {
                    return true;
                }
            }
        }
        // diagonal Bottom left -> Top right
        for (int row = 3; row < numRows; row++) {
            for (int col = 0; col <= numCols - 4; col++) {
                if (Board.get(row).get(col)     == player &&
                        Board.get(row-1).get(col+1) == player &&
                        Board.get(row-2).get(col+2) == player &&
                        Board.get(row-3).get(col+3) == player) {
                    return true;
                }
            }
        }
        return false;
    }

    // AI Logic
    public int AI_chooseCol() {
        // Firstly, check if AI can win.
        for (int col = 0; col < numCols; col++) {
            if (!isColumnFull(col)) {
                Vector<Vector<Character>> sim = updateBoard(col, currPlayer);
                if (checkForWin(sim, currPlayer)) {
                    return col;
                }
            }
        }
        // If not, check if player can win and block the move.
        char opp = (currPlayer == 'X' ? 'O' : 'X');
        for (int col = 0; col < numCols; col++) {
            if (!isColumnFull(col)) {
                Vector<Vector<Character>> sim = updateBoard(col, opp);
                if (checkForWin(sim, opp)) {
                    return col;
                }
            }
        }
        // Otherwise,
        Random random = new Random();
        int choice;
        do {
            choice = random.nextInt(numCols);
        } while (isColumnFull(choice));
        return choice;
    }
}