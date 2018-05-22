import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ChessClient extends Application implements ChessConstants
{
    private boolean myTurn = false;

    private String myColour = null;
    private String otherColour = null;

    private Cell[][] cell = new Cell[8][8];

    private Label titel = new Label();
    private Label status = new Label();

    private int rowSelected;
    private int colomnSelected;
    private int pieceSelected;

    private DataInputStream fromServer;
    private DataOutputStream toServer;

    private boolean continueToPlay = true;
    private boolean waiting = true;

    private String host = "localhost";

    @Override
    public void start(Stage primaryStage)
    {
        GridPane pane = new GridPane();

        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                int piece;
                if (i == 1 || i == 6)
                {
                    piece = PAWN;
                }
                if (i == 0 || i == 7)
                {
                    if (j == 0 || j == 7)
                        piece = ROOK;
                    else if (j == 1 || j == 6)
                        piece = KNIGHT;
                    else if (j == 2 || j == 5)
                        piece = BISHOP;
                    else if (j == 3)
                        piece = QUEEN;
                    else
                        piece = KING;
                } else
                    piece = CLEAR;
                pane.add(cell[i][j] = new Cell(piece, i, j), j, i);
            }
        }
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(titel);
        borderPane.setCenter(pane);
        borderPane.setBottom(status);

        Scene scene = new Scene(borderPane, 900, 950);
        primaryStage.setTitle("ChessClient");
        primaryStage.setScene(scene);
        primaryStage.show();
        connectToServer();
    }

    private void connectToServer()
    {
        try
        {
            Socket socket = new Socket(host, 7000);

            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());
        }catch (IOException e)
        {
            e.printStackTrace();
        }

        new Thread(() -> {
            try
            {
                int player = fromServer.readInt();

                if(player == PLAYER1)
                {
                    myColour = "White";
                    otherColour = "Black";

                    fromServer.readInt();

                    Platform.runLater(() -> status.setText("Player 2 has joined. I start first"));

                    Platform.runLater(() -> titel.setText("Player 1 with colour White"));

                    myTurn = true;
                }
                else if(player == PLAYER2)
                {
                    Platform.runLater(() -> titel.setText("Player 2 with colour Black"));
                    myColour = "Black";
                    otherColour = "White";
                }

                while(continueToPlay)
                {
                    if(player == PLAYER1)
                    {
                        waitForPlayerAction();
                        sendMove();
                        receiveInfoFromServer();
                    }
                    else if (player == PLAYER2) {
                        receiveInfoFromServer();
                        waitForPlayerAction();
                        sendMove();
                    }
                }
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    private void waitForPlayerAction() throws InterruptedException
    {
        while(waiting)
        {
            Thread.sleep(100);
        }

        waiting = true;
    }

    private void sendMove() throws IOException
    {
        toServer.writeInt(pieceSelected);
        toServer.writeInt(rowSelected);
        toServer.writeInt(colomnSelected);
    }

    private void receiveInfoFromServer() throws IOException
    {
        int status = fromServer.readInt();

        receiveMove();
        myTurn = true;
    }

    private void receiveMove() throws IOException
    {
        int piece = fromServer.readInt();
        int row = fromServer.readInt();
        int column = fromServer.readInt();

        Platform.runLater(() -> cell[row][column].setPiece(piece));
    }

    public class Cell extends Pane
    {
        private int row;
        private int column;
        private int piece;

        public Cell(int piece, int row, int column)
        {
            this.row = row;
            this.column = column;
            this.piece = piece;
            this.setPrefSize(2000, 2000);
            setStyle("-fx-border-color: black");
            this.setOnMouseClicked(e -> handleMouseClick());
        }

        public void setPiece(int p)
        {
            piece = p;
            repaint();
        }

        protected void repaint()
        {
            switch(piece)
            {
                case BISHOP :
                    break;
                case KING :
                    break;
                case KNIGHT :
                    break;
                case PAWN :
                    break;
                case QUEEN :
                    break;
                case ROOK :
                    break;
            }
        }

        private void handleMouseClick()
        {
            if(myTurn)
            {

            }
        }
    }
}