package application.controller;

import javafx.application.Platform;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

public class Server {
  static int NUM = 0;
  static ServerSocket serverSocket;
  static ArrayList<BufferedReader> bReaders = new ArrayList<BufferedReader>();
  static ArrayList<PrintWriter> pWriters = new ArrayList<PrintWriter>();
  static LinkedList<String> msgList = new LinkedList<String>();
  private static final int[][] chessBoard = new int[3][3];
  private static boolean checkWin(int side){
    side++;
    int cnt;
    // row
    for (int i = 0; i < 3; i++) {
      cnt = 0;
      for (int j = 0; j < 3; j++) {
        if(chessBoard[i][j]==side)cnt++;
        else break;
      }
      if(cnt==3) return true;
    }
    // col
    for (int i = 0; i < 3; i++) {
      cnt = 0;
      for (int j = 0; j < 3; j++) {
        if(chessBoard[j][i]==side)cnt++;
        else break;
      }
      if(cnt==3) return true;
    }
    cnt = 0;
    for (int i = 0; i < 3; i++) {
      if(chessBoard[i][i]==side)cnt++;
      else cnt = 0;
    }
    if(cnt==3) return true;
    cnt = 0;
    for (int i = 0; i < 3; i++) {
      if(chessBoard[i][2-i]==side)cnt++;
      else cnt = 0;
    }
    if(cnt==3) return true;
    return false;
  }
  public static void main(String[] args) {
    try {
      int port = 88;
      serverSocket = new ServerSocket(port);
      new AcceptSocketThread().start();
      new SendMsgToClient().start();
      System.out.println("当前客户端连接数: " + NUM);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static class AcceptSocketThread extends Thread {
    public void run() {
      while (this.isAlive()) {
        try {
          if(NUM==2) continue;
          Socket socket = serverSocket.accept();
          if (socket != null) {
            BufferedReader bReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            bReaders.add(bReader);
            // start a get message thread
            new GetMsgFromClient(socket,bReader).start();
            pWriters.add(new PrintWriter(socket.getOutputStream()));
            // Set ID for this Client
            pWriters.get(NUM).println(NUM);
            pWriters.get(NUM).flush();
            NUM++;
            System.out.println("当前客户端连接数: " + NUM);
            // inform to start
            if(NUM==2){
              for (int i = 0; i < pWriters.size(); i++) {
                pWriters.get(i).println(0+",1");
                pWriters.get(i).flush();
              }
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  static class GetMsgFromClient extends Thread {
    Socket socket;
    BufferedReader bReader;
    boolean disConnected = false;
    public GetMsgFromClient(Socket socket,BufferedReader bReader) {
      this.socket = socket;
      this.bReader = bReader;
    }


    public void run() {
      while (!disConnected) {
        try {
          // read the operation
          String strMsg = null;
          if(bReader!=null) strMsg = bReader.readLine();
          if(strMsg == null) continue;
          if(strMsg.equals("Disconnect")){
            NUM--;
            System.out.println("当前客户端连接数: "+NUM);
            if(NUM==0){
              pWriters.clear();
              bReaders.clear();
            }
            socket.close();
            socket = null;
            bReader.close();
            bReader = null;
            disConnected = true;
            return;
          }
          String[] ope = strMsg.split(",");
          int side = Integer.parseInt(ope[0]);
          int x = Integer.parseInt(ope[1]);
          int y = Integer.parseInt(ope[2]);
          // change the state of server
          chessBoard[x][y] = side+1;
          // change the state of each client
          msgList.addLast(strMsg);
          // check if game set
          if(checkWin(side)){
            msgList.addLast(side+",,1");
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  static class SendMsgToClient extends Thread {
    public void run() {
      while (this.isAlive()) {
        try {
          if (!msgList.isEmpty()) {
            String msg = msgList.removeFirst();
            for (int i = 0; i < pWriters.size(); i++) {
              pWriters.get(i).println(msg);
              pWriters.get(i).flush();
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
}
