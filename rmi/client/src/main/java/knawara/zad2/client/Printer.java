package knawara.zad2.client;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public class Printer {
    private final ConsoleIO io;

    public Printer(ConsoleIO io) {
        this.io = io;
    }

    public void gameStarted() {
        p("Game started!");
    }

    public void yourMove() {
        p("It's now your move!");
    }

    public void overWithoutWinner() {
        p("Game over, but there is no winner. All interesting things has already happend, so just type 'quit' when" +
                "you want to exit");
    }

    public void youWon() {
        p("You are the winner - congratulations!");
    }

    public void someoneElseWon(char winnerId) {
        p("Player %c won. Sorry :)", winnerId);
    }

    public void placeAlreadyOccupied() {
        p("This place is already occupied");
    }

    public void moveOutOfBoard() {
        p("Move outside of board!");
    }

    public void notYourTurnBuddy() {
        p("It is not your turn, sorry");
    }

    public void fatalError() {
        p("Fatal error occured, you can only quit");
    }

    public void any(String s) {
        p(s);
    }

    private void p(String f, Object... args) {
        io.enqueueAsyncMessage(String.format(f, args));
    }
}
