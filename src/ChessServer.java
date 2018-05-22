import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChessServer extends Application implements ChessConstants
{
    private int sessionNo = 1;

    private int curPlayer = -1;

    @Override
    public void start(Stage primaryStage)
    {
        TextArea ta = new TextArea();

        Scene scene = new Scene(new ScrollPane(ta), 450, 200);
        primaryStage.setTitle("ChessServer");
        primaryStage.setScene(scene);
        primaryStage.show();

        new Thread(() ->
        {
            try
            {
                ServerSocket serverSocket = new ServerSocket(7000);
                Platform.runLater(() -> ta.appendText("Server is gestart mijn jongen" + '\n'));

                while (true)
                {
                    Platform.runLater(() -> ta.appendText("Waiting for players to join session: " + sessionNo + '\n'));

                    Socket player1 = serverSocket.accept();

                    Platform.runLater(() ->
                    {
                        ta.appendText("Player 1 joined" + '\n');
                        ta.appendText("Player 1's IP: " + player1.getInetAddress().getHostAddress() + '\n');
                    });

                    new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);

                    Socket player2 = serverSocket.accept();

                    Platform.runLater(() ->
                    {
                        ta.appendText("Player 2 joined" + '\n');
                        ta.appendText("Player 2's IP: " + player2.getInetAddress().getHostAddress());
                    });

                    new DataOutputStream(player2.getOutputStream()).writeInt(PLAYER2);

                    new Thread(new HandleASession(player1, player2)).start();
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }).start();
    }

    class HandleASession implements Runnable, ChessConstants
    {
        private Socket player1;
        private Socket player2;

        private int[][][] cell = new int[8][8][2];

        private DataInputStream fromPlayer1;
        private DataOutputStream toPlayer1;
        private DataInputStream fromPlayer2;
        private DataOutputStream toPlayer2;

        private boolean continueToPlay = true;

        public HandleASession(Socket player1, Socket player2)
        {
            this.player1 = player1;
            this.player2 = player2;
            for (int k = 0; k < 2; k++)
            {
                for (int i = 0; i < 8; i++)
                {
                    for (int j = 0; j < 8; j++)
                    {
                        if (i == 1 && k == 0)
                            cell[i][j][k] = PAWN;

                        if (i == 6 && k == 1)
                            cell[i][j][k] = PAWN;

                        if (i == 0 && k == 0)
                        {
                            if (j == 0 || j == 7)
                                cell[i][j][k] = ROOK;
                            else if (j == 1 || j == 6)
                                cell[i][j][k] = KNIGHT;
                            else if (j == 2 || j == 5)
                                cell[i][j][k] = BISHOP;
                            else if (j == 3)
                                cell[i][j][k] = QUEEN;
                            else
                                cell[i][j][k] = KING;
                        } else if (i == 7 && k == 1)
                        {
                            if (j == 0 || j == 7)
                                cell[i][j][k] = ROOK;
                            else if (j == 1 || j == 6)
                                cell[i][j][k] = KNIGHT;
                            else if (j == 2 || j == 5)
                                cell[i][j][k] = BISHOP;
                            else if (j == 3)
                                cell[i][j][k] = QUEEN;
                            else
                                cell[i][j][k] = KING;
                        } else
                            cell[i][j][k] = CLEAR;
                    }
                }
            }
        }

        public void run()
        {
            try
            {
                // Create data input and output streams
                DataInputStream fromPlayer1 = new DataInputStream(
                        player1.getInputStream());
                DataOutputStream toPlayer1 = new DataOutputStream(
                        player1.getOutputStream());
                DataInputStream fromPlayer2 = new DataInputStream(
                        player2.getInputStream());
                DataOutputStream toPlayer2 = new DataOutputStream(
                        player2.getOutputStream());

                // Write anything to notify player 1 to start
                // This is just to let player 1 know to start
                toPlayer1.writeInt(1);

                // Continuously serve the players and determine and report
                // the game status to the players
                while (true)
                {
                    // Receive a move from player 1
                    curPlayer = 0;
                    int piece = fromPlayer1.readInt();
                    int row = fromPlayer1.readInt();
                    int column = fromPlayer1.readInt();
                    int oldRow = fromPlayer1.readInt();
                    int oldColumn = fromPlayer1.readInt();

                    if (checkMove(piece, oldRow, oldColumn, row, column))
                        cell[row][column][0] = piece;

                    // Check if Player 1 wins
                    if (isWon('X'))
                    {
                        toPlayer1.writeInt(PLAYER1_WON);
                        toPlayer2.writeInt(PLAYER1_WON);
                        sendMove(toPlayer2, piece, row, column);
                        break; // Break the loop
                    } else
                    {
                        // Notify player 2 to take the turn
                        toPlayer2.writeInt(CONTINUE);

                        // Send player 1's selected row and column to player 2
                        sendMove(toPlayer2, piece, row, column);
                    }

                    // Receive a move from Player 2
                    row = fromPlayer2.readInt();
                    column = fromPlayer2.readInt();
                    cell[row][column][0] = 'O';

                    // Check if Player 2 wins
                    if (isWon('O'))
                    {
                        toPlayer1.writeInt(PLAYER2_WON);
                        toPlayer2.writeInt(PLAYER2_WON);
                        sendMove(toPlayer1, piece, row, column);
                        break;
                    } else
                    {
                        // Notify player 1 to take the turn
                        toPlayer1.writeInt(CONTINUE);

                        // Send player 2's selected row and column to player 1
                        sendMove(toPlayer1, piece, row, column);
                    }
                }
            } catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }

        /**
         * Check the move
         */
//        private boolean checkMove(int piece, int oldRow, int oldColumn, int newRow, int newColumn)
//        {
//            boolean valid = false;
//            switch (piece)
//            {
//                case BISHOP:
//                    if(cell[newRow][newColumn][curPlayer] != CLEAR)
//                        break;
//                    if(Math.abs((newRow-oldRow)/(newColumn-oldColumn)) != 1)
//                        break;
//                    int rowDiff = newRow - oldRow;
//                    int columnDiff = newColumn - oldColumn;
//                    for(int i = newRow; i != oldRow; i -= rowDiff/-rowDiff)
//                    {
//                        for(int j = newColumn; j != oldColumn; j += columnDiff/-columnDiff)
//                        {
//
//                        }
//                    }
//                    break;
//                case KING:
//                    break;
//                case KNIGHT:
//                    break;
//                case PAWN:
//                    break;
//                case QUEEN:
//                    break;
//                case ROOK:
//                    break;
//            }
//            return valid;
//        }

        /**
         * Send the move to other player
         */
        private void sendMove(DataOutputStream out, int piece, int row, int column)
                throws IOException
        {
            out.writeInt(piece);
            out.writeInt(row); // Send row index
            out.writeInt(column); // Send column index
        }

        /**
         * Determine if the cells are all occupied
         */
        private boolean isFull()
        {
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    if (cell[i][j][0] == ' ')
                        return false; // At least one cell is not filled

            // All cells are filled
            return true;
        }

        /**
         * Determine if the player with the specified token wins
         */
        private boolean isWon(char token)
        {
            // Check all rows
//            for (int i = 0; i < 3; i++)
//                if ((cell[i][0] == token)
//                        && (cell[i][1] == token)
//                        && (cell[i][2] == token))
//                {
//                    return true;
//                }
//
//            /** Check all columns */
//            for (int j = 0; j < 3; j++)
//                if ((cell[0][j] == token)
//                        && (cell[1][j] == token)
//                        && (cell[2][j] == token))
//                {
//                    return true;
//                }
//
//            /** Check major diagonal */
//            if ((cell[0][0] == token)
//                    && (cell[1][1] == token)
//                    && (cell[2][2] == token))
//            {
//                return true;
//            }
//
//            /** Check subdiagonal */
//            if ((cell[0][2] == token)
//                    && (cell[1][1] == token)
//                    && (cell[2][0] == token))
//            {
//                return true;
//            }
//
//            /** All checked, but no winner */
            return false;
        }
    }

    /**
     * The main method is only needed for the IDE with limited
     * JavaFX support. Not needed for running from the command line.
     */
    public static void main(String[] args)
    {
        launch(args);
    }
}