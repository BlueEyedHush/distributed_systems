package knawara.zad2.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMain {
    private static final Logger LOGGER = LoggerFactory.getLogger("ClientMain");

    private static ClientMain app;
    public static ClientMain get() {
        return app;
    }

    private String username = "knawara@student.agh.edu.pl";

    public static void main(String[] args) {
        app = new ClientMain();
        app.run(args);
    }

    private void run(String[] args) {
        if(!parseArgs(args)) return;

        ConsoleIO io = new ConsoleIO();
        Communicator comms = new Communicator(username);

        UserInteractions ui = new UserInteractions(io, comms);
        io.setCommandHandler(ui);
        comms.setOnIncomingMessage(ui::onIncomingMessage);


        io.start();
    }

    private boolean parseArgs(String[] args) {
        if(args.length < 1) {
            System.out.println("You must pass username as first argument");
            return false;
        }

        username = args[0];

        return true;
    }
}
