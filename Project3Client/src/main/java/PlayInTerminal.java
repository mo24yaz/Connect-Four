/**
 Testing class for the game before GUI implementation.
 Based on logic from previous c++ implementation of the game. (CS 141)
 **/

import java.util.Scanner;
import java.util.Vector;

public class PlayInTerminal {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Connect 4!");
        System.out.println("Press 1 to play single player against AI!");
        System.out.println("Press 2 to play multi-player!");

        int choice = scanner.nextInt();
        scanner.nextLine();

        if (choice == 1) {
            playGameAI(scanner);
        }
        else if (choice == 2) {
            playGameMultiplayer(scanner);
        }

        scanner.close();
    }

    private static void playGameAI(Scanner scanner) {
        Connect4 game = new Connect4();

        while (!game.gameEnded) {
            if (Connect4.currPlayer == 'X') {
                printBoard();

                System.out.println("Number of plays: " + Connect4.numPlays);
                System.out.print("Player X, enter column (0-6): ");

                int col = scanner.nextInt();
                while (col < 0 || col >= Connect4.numCols || game.isColumnFull(col)) {
                    System.out.print("Invalid. Try again: ");
                    col = scanner.nextInt();
                }
                game.drop(Connect4.currPlayer, col);
            }
            else {
                int aiCol = game.AI_chooseCol();
                System.out.println("*** AI plays at column: " + aiCol + " ***");
                game.drop(Connect4.currPlayer, aiCol);
            }

            if (Connect4.checkForWin(Connect4.board, Connect4.currPlayer)) {
                printBoard();
                System.out.println("Player " + Connect4.currPlayer + " wins!");
                game.gameEnded = true;
            }
            else if (game.isBoardFull()) {
                printBoard();
                System.out.println("It's a draw!");
                game.gameEnded = true;
            }
            else {
                game.switchPlayer();
            }
        }
    }

    private static void playGameMultiplayer(Scanner scanner) {
        Connect4 game    = new Connect4();
        LinkedList history = new LinkedList();

        while (!game.gameEnded) {
            printBoard();

            System.out.println("Number of plays: " + Connect4.numPlays);
            System.out.println("It is " + Connect4.currPlayer + "'s turn.");
            System.out.print("Enter 0–6, U=undo, P=print all, Q=quit: ");

            String in = scanner.nextLine().trim();
            if (in.equalsIgnoreCase("q")) {
                break;
            }
            else if (in.equalsIgnoreCase("u")) {
                Vector<Vector<Character>> prev = history.undoMove();
                if (prev != null) {
                    Connect4.board = prev;
                    game.switchPlayer();
                }
                else {
                    System.out.println("Nothing to undo.");
                }
                continue;
            }
            else if (in.equalsIgnoreCase("p")) {
                printAllBoards(history.head);
                continue;
            }

            int col;
            try {
                col = Integer.parseInt(in);
                if (col < 0 || col >= Connect4.numCols) {
                    System.out.println("Invalid column number.");
                    continue;
                }
                if (game.isColumnFull(col)) {
                    System.out.println("Column is full.");
                    continue;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
                continue;
            }

            history.addBoard(Connect4.board);
            game.drop(Connect4.currPlayer, col);

            if (Connect4.checkForWin(Connect4.board, Connect4.currPlayer)) {
                printBoard();
                System.out.println("Player " + Connect4.currPlayer + " wins!");
                game.gameEnded = true;
            }
            else if (game.isBoardFull()) {
                printBoard();
                System.out.println("It's a draw!");
                game.gameEnded = true;
            }
            else {
                game.switchPlayer();
            }
        }
    }

    private static void printBoard() {
        System.out.println("0 1 2 3 4 5 6");
        for (Vector<Character> row : Connect4.board) {
            for (char cell : row) System.out.print(cell + " ");
            System.out.println();
        }
    }

    // LinkedList for undoMove and board history (From CS 141 Project)
    // Might implement into the project or not.
    public static class Node {
        public Vector<Vector<Character>> board;
        public Node next;
        public Node(Vector<Vector<Character>> b) {
            this.board = copyBoard(b);
            this.next  = null;
        }
        private static Vector<Vector<Character>> copyBoard(Vector<Vector<Character>> orig) {
            Vector<Vector<Character>> c = new Vector<>();
            for (Vector<Character> row : orig) c.add(new Vector<>(row));
            return c;
        }
    }
    public static class LinkedList {
        public Node head;
        public LinkedList() { head = null; }
        public synchronized void addBoard(Vector<Vector<Character>> b) {
            Node n = new Node(b);
            n.next = head;
            head  = n;
        }
        public synchronized Vector<Vector<Character>> undoMove() {
            if (head == null) return null;
            Vector<Vector<Character>> prev = head.board;
            head = head.next;
            Connect4.numPlays--;
            return prev;
        }
    }
    private static void printAllBoards(Node node) {
        if (node == null) return;
        printAllBoards(node.next);
        System.out.println("0 1 2 3 4 5 6");
        for (Vector<Character> row : node.board) {
            for (char cell : row) System.out.print(cell + " ");
            System.out.println();
        }
        System.out.println();
    }
}