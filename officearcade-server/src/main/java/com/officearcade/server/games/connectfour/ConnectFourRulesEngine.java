package com.officearcade.server.games.connectfour;

import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ConnectFourRulesEngine {

    public static final int ROWS = 6;
    public static final int COLUMNS = 7;
    public static final int EMPTY = 0;
    public static final int PLAYER_ONE_TOKEN = 1;
    public static final int PLAYER_TWO_TOKEN = 2;

    private ConnectFourRulesEngine() {
    }

    public static int[][] emptyBoard() {
        return new int[ROWS][COLUMNS];
    }

    public static int[][] decodeBoard(String encodedBoard) {
        if (encodedBoard == null || encodedBoard.length() != ROWS * COLUMNS) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Connect Four board encoding is invalid.");
        }

        int[][] board = new int[ROWS][COLUMNS];
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = row * COLUMNS + col;
                char symbol = encodedBoard.charAt(index);
                if (symbol == '0') {
                    board[row][col] = EMPTY;
                } else if (symbol == '1') {
                    board[row][col] = PLAYER_ONE_TOKEN;
                } else if (symbol == '2') {
                    board[row][col] = PLAYER_TWO_TOKEN;
                } else {
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Connect Four board encoding contains invalid token value."
                    );
                }
            }
        }
        return board;
    }

    public static String encodeBoard(int[][] board) {
        validateBoardShape(board);

        StringBuilder builder = new StringBuilder(ROWS * COLUMNS);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int cell = board[row][col];
                if (cell != EMPTY && cell != PLAYER_ONE_TOKEN && cell != PLAYER_TWO_TOKEN) {
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Connect Four board contains invalid token values."
                    );
                }
                builder.append(cell);
            }
        }
        return builder.toString();
    }

    public static int dropToken(int[][] board, int column, int token) {
        validateBoardShape(board);
        if (column < 0 || column >= COLUMNS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Column must be between 0 and 6.");
        }
        if (token != PLAYER_ONE_TOKEN && token != PLAYER_TWO_TOKEN) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid Connect Four token value.");
        }

        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][column] == EMPTY) {
                board[row][column] = token;
                return row;
            }
        }
        return -1;
    }

    public static boolean hasWinningConnection(int[][] board, int row, int col, int token) {
        validateBoardShape(board);
        return countDirection(board, row, col, 0, 1, token) + countDirection(board, row, col, 0, -1, token) + 1 >= 4
                || countDirection(board, row, col, 1, 0, token) + countDirection(board, row, col, -1, 0, token) + 1 >= 4
                || countDirection(board, row, col, 1, 1, token) + countDirection(board, row, col, -1, -1, token) + 1 >= 4
                || countDirection(board, row, col, 1, -1, token) + countDirection(board, row, col, -1, 1, token) + 1 >= 4;
    }

    public static boolean isBoardFull(int[][] board) {
        validateBoardShape(board);
        for (int col = 0; col < COLUMNS; col++) {
            if (board[0][col] == EMPTY) {
                return false;
            }
        }
        return true;
    }

    public static boolean isColumnFull(int[][] board, int column) {
        validateBoardShape(board);
        if (column < 0 || column >= COLUMNS) {
            return true;
        }
        return board[0][column] != EMPTY;
    }

    public static int[][] copyBoard(int[][] board) {
        validateBoardShape(board);
        int[][] copy = new int[ROWS][COLUMNS];
        for (int row = 0; row < ROWS; row++) {
            copy[row] = Arrays.copyOf(board[row], COLUMNS);
        }
        return copy;
    }

    private static int countDirection(int[][] board, int startRow, int startCol, int rowDelta, int colDelta, int token) {
        int count = 0;
        int row = startRow + rowDelta;
        int col = startCol + colDelta;

        while (row >= 0 && row < ROWS && col >= 0 && col < COLUMNS && board[row][col] == token) {
            count++;
            row += rowDelta;
            col += colDelta;
        }
        return count;
    }

    private static void validateBoardShape(int[][] board) {
        if (board == null || board.length != ROWS) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Connect Four board shape is invalid.");
        }
        for (int row = 0; row < ROWS; row++) {
            if (board[row] == null || board[row].length != COLUMNS) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Connect Four board shape is invalid.");
            }
        }
    }
}
