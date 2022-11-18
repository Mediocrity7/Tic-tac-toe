package application.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    // chess position
    private static final int PLAY_1 = 1;
    private static final int PLAY_2 = 2;
    private static final int EMPTY = 0;
    // drawing parameter
    private static final int BOUND = 90;
    private static final int OFFSET = 15;

    static Socket socket;
    static PrintWriter pWriter;
    static BufferedReader bReader;
    InetAddress ip;
    // gaming
    private static int ID = 0;
    private static int CurrentTurn = -1;
    private static int win = -1;
    @FXML
    public Pane base_square;
    @FXML
    private Button button_connect;
    @FXML
    private TextArea textArea_listener;
    @FXML
    private Rectangle game_panel;


    private static final int[][] chessBoard = new int[3][3];
    private static final boolean[][] flag = new boolean[3][3];

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        game_panel.setOnMouseClicked(event -> {
            if(ID!=CurrentTurn) return;
            int x = (int) (event.getX() / BOUND);
            int y = (int) (event.getY() / BOUND);
            if (chessBoard[x][y] != EMPTY) return;
            // send the operation to the server
            pWriter.println(String.format("%d,%d,%d,1",ID,x,y));
            pWriter.flush();
        });
    }

    private void refreshBoard (int turn,int x, int y) {
            chessBoard[x][y] = turn+1;
            if(turn==0){
                drawCircle(x,y);
            }else{
                drawLine(x,y);
            }
    }

    private void drawChess () {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (flag[i][j]) {
                    // This square has been drawing, ignore.
                    continue;
                }
                switch (chessBoard[i][j]) {
                    case PLAY_1:
                        drawCircle(i, j);
                        break;
                    case PLAY_2:
                        drawLine(i, j);
                        break;
                    case EMPTY:
                        // do nothing
                        break;
                    default:
                        System.err.println("Invalid value!");
                }
            }
        }
    }

    private void drawCircle (int i, int j) {
        Circle circle = new Circle();
        try{
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    base_square.getChildren().add(circle);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        circle.setCenterX(i * BOUND + BOUND / 2.0 + OFFSET);
        circle.setCenterY(j * BOUND + BOUND / 2.0 + OFFSET);
        circle.setRadius(BOUND / 2.0 - OFFSET / 2.0);
        circle.setStroke(Color.RED);
        circle.setFill(Color.TRANSPARENT);
        flag[i][j] = true;
    }

    private void drawLine (int i, int j) {
        Line line_a = new Line();
        Line line_b = new Line();
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                base_square.getChildren().add(line_a);
                base_square.getChildren().add(line_b);
            }
        });
        line_a.setStartX(i * BOUND + OFFSET * 1.5);
        line_a.setStartY(j * BOUND + OFFSET * 1.5);
        line_a.setEndX((i + 1) * BOUND + OFFSET * 0.5);
        line_a.setEndY((j + 1) * BOUND + OFFSET * 0.5);
        line_a.setStroke(Color.BLUE);

        line_b.setStartX((i + 1) * BOUND + OFFSET * 0.5);
        line_b.setStartY(j * BOUND + OFFSET * 1.5);
        line_b.setEndX(i * BOUND + OFFSET * 1.5);
        line_b.setEndY((j + 1) * BOUND + OFFSET * 0.5);
        line_b.setStroke(Color.BLUE);
        flag[i][j] = true;
    }

    public void connectToServer(ActionEvent actionEvent) {
        try {
            int port = 88;
            socket = new Socket(InetAddress.getByName(null), port);
            pWriter = new PrintWriter(socket.getOutputStream());
            bReader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            ip = InetAddress.getLocalHost();
            // get message
            new GetMsgFromServer().start();
            button_connect.setDisable(true);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    class GetMsgFromServer extends Thread {
        public void run() {
            while (this.isAlive()) {
                try {
                    String strMsg = bReader.readLine();
                    String[] ope = strMsg.split(",");
                    // set ID of each Client
                    if(ope.length==1){
                        ID = Integer.parseInt(ope[0]);
                        System.out.println("Your ID is "+ID);
                    }
                    // begin the game
                    else if(ope.length==2){
                        CurrentTurn = Integer.parseInt(ope[0]);
                        System.out.println("Game Start!");
                    }
                    // announce the winner
                    else if(ope.length==3){
                        win = Integer.parseInt(ope[0]);
                        if(win==ID){
                            System.out.println("You win!");
                            textArea_listener.clear();
                            textArea_listener.appendText("You win!");
                            CurrentTurn = -1;
                            return;
                        }else if((win+1)%2==ID){
                            System.out.println("You lose!");
                            textArea_listener.clear();
                            textArea_listener.appendText("You lose!");
                            CurrentTurn = -1;
                            return;
                        }
                    }
                    // operations on chess
                    else{
                        int lastTurn = Integer.parseInt(ope[0]);
                        refreshBoard(lastTurn,Integer.parseInt(ope[1]),Integer.parseInt(ope[2]));
                        CurrentTurn = (lastTurn+1)%2;
                    }
                    if(CurrentTurn==ID){
                        System.out.println("It's your turn!");
                        textArea_listener.clear();
                        textArea_listener.appendText("It's your turn!");
                    }else{
                        System.out.println("Waiting!");
                        textArea_listener.clear();
                        textArea_listener.appendText("Waiting!");
                    }
                    Thread.sleep(50);
                } catch (Exception e) {
//                    e.printStackTrace();
                }
            }
        }
    }
}
