package com.officearcade.server.games.connectfour;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConnectFourRulesEngineTest {

    @Test
    void shouldEncodeAndDecodeBoardWithoutLosingState() {
        int[][] board = ConnectFourRulesEngine.emptyBoard();
        ConnectFourRulesEngine.dropToken(board, 0, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 1, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 0, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);

        String encoded = ConnectFourRulesEngine.encodeBoard(board);
        int[][] decoded = ConnectFourRulesEngine.decodeBoard(encoded);

        assertThat(decoded).isEqualTo(board);
    }

    @Test
    void shouldDetectHorizontalWin() {
        int[][] board = ConnectFourRulesEngine.emptyBoard();
        ConnectFourRulesEngine.dropToken(board, 0, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 1, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 2, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);
        int row = ConnectFourRulesEngine.dropToken(board, 3, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);

        assertThat(ConnectFourRulesEngine.hasWinningConnection(board, row, 3, ConnectFourRulesEngine.PLAYER_ONE_TOKEN))
                .isTrue();
    }

    @Test
    void shouldDetectVerticalWin() {
        int[][] board = ConnectFourRulesEngine.emptyBoard();
        ConnectFourRulesEngine.dropToken(board, 0, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 0, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 0, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);
        int row = ConnectFourRulesEngine.dropToken(board, 0, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);

        assertThat(ConnectFourRulesEngine.hasWinningConnection(board, row, 0, ConnectFourRulesEngine.PLAYER_ONE_TOKEN))
                .isTrue();
    }

    @Test
    void shouldDetectDiagonalWinBottomLeftToTopRight() {
        int[][] board = ConnectFourRulesEngine.emptyBoard();

        ConnectFourRulesEngine.dropToken(board, 0, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);

        ConnectFourRulesEngine.dropToken(board, 1, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 1, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);

        ConnectFourRulesEngine.dropToken(board, 2, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 2, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 2, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);

        ConnectFourRulesEngine.dropToken(board, 3, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 3, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 3, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        int row = ConnectFourRulesEngine.dropToken(board, 3, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);

        assertThat(ConnectFourRulesEngine.hasWinningConnection(board, row, 3, ConnectFourRulesEngine.PLAYER_ONE_TOKEN))
                .isTrue();
    }

    @Test
    void shouldDetectDiagonalWinBottomRightToTopLeft() {
        int[][] board = ConnectFourRulesEngine.emptyBoard();

        ConnectFourRulesEngine.dropToken(board, 3, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);

        ConnectFourRulesEngine.dropToken(board, 2, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 2, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);

        ConnectFourRulesEngine.dropToken(board, 1, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 1, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 1, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);

        ConnectFourRulesEngine.dropToken(board, 0, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 0, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        ConnectFourRulesEngine.dropToken(board, 0, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        int row = ConnectFourRulesEngine.dropToken(board, 0, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);

        assertThat(ConnectFourRulesEngine.hasWinningConnection(board, row, 0, ConnectFourRulesEngine.PLAYER_ONE_TOKEN))
                .isTrue();
    }

    @Test
    void shouldMarkColumnAsFullAndRejectAdditionalDrop() {
        int[][] board = ConnectFourRulesEngine.emptyBoard();
        for (int row = 0; row < ConnectFourRulesEngine.ROWS; row++) {
            ConnectFourRulesEngine.dropToken(board, 4, ConnectFourRulesEngine.PLAYER_ONE_TOKEN);
        }

        int result = ConnectFourRulesEngine.dropToken(board, 4, ConnectFourRulesEngine.PLAYER_TWO_TOKEN);
        assertThat(result).isEqualTo(-1);
        assertThat(ConnectFourRulesEngine.isColumnFull(board, 4)).isTrue();
    }

    @Test
    void shouldDetectFullBoard() {
        int[][] board = ConnectFourRulesEngine.emptyBoard();
        int token = ConnectFourRulesEngine.PLAYER_ONE_TOKEN;

        for (int column = 0; column < ConnectFourRulesEngine.COLUMNS; column++) {
            for (int row = 0; row < ConnectFourRulesEngine.ROWS; row++) {
                ConnectFourRulesEngine.dropToken(board, column, token);
                token = token == ConnectFourRulesEngine.PLAYER_ONE_TOKEN
                        ? ConnectFourRulesEngine.PLAYER_TWO_TOKEN
                        : ConnectFourRulesEngine.PLAYER_ONE_TOKEN;
            }
        }

        assertThat(ConnectFourRulesEngine.isBoardFull(board)).isTrue();
    }
}
