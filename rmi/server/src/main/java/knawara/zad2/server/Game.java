package knawara.zad2.server;

import knawara.zad2.common.exceptions.AlreadyOccupiedException;
import knawara.zad2.common.exceptions.NotYourTurnException;
import knawara.zad2.common.exceptions.OutOfBoardException;
import knawara.zad2.common.requests.*;
import knawara.zad2.common.utils.Board;
import knawara.zad2.server.events.GameEvent;
import knawara.zad2.server.events.LeaveEvent;
import knawara.zad2.server.events.MoveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by blueeyedhush on 20.03.16.
 */
public class Game {
    private static final int EVENT_QUEUE_SIZE = 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger("Game");

    private List<PlayerImpl> players = null;
    private final Board board;
    private final int winningChainLength;

    private BlockingQueue<GameEvent> eventQueue = new LinkedBlockingQueue<>(EVENT_QUEUE_SIZE);
    private volatile AtomicInteger currentlyRequestingMoveFrom = new AtomicInteger(-1);

    public Game(Board board, int winningChainLength) {
        this.board = board;
        this.winningChainLength = winningChainLength;
    }

    private boolean processInitialLeaves() {
        Set<GameEvent> events = new HashSet<>();
        eventQueue.drainTo(events);

        Set<Character> idsWhoLeft = new HashSet<>();
        events.forEach(e -> {
            if(e instanceof LeaveEvent) {
                idsWhoLeft.add(((LeaveEvent) e).getPlayerId());
            } else {
                LOGGER.error("Events different than Leave found in eventQueue. Somebody is not respecting the rules.");
            }
        });

        players = players.stream()
                .filter(p -> !idsWhoLeft.contains(p.getId()))
                .collect(Collectors.toList());

        if(players.size() > 1) {
            return true;
        } else if(players.size() == 1) {
            PlayerImpl winner = players.iterator().next();
            winner.getQueue().add(new Won(winner.getId()));
            return false;
        } else {
            return false;
        }
    }

    public void start() {
        if(players == null) throw new IllegalStateException("Players not injected!");

        /* processing leaves() which were submitted before game started */
        if(!processInitialLeaves()) {
            LOGGER.info("Too much players has left game during initialization. Cannot continue.");
            return;
        }

        broadcastToAll(new GameStarted());

        boolean stop = false;
        while(!stop) {
            Set<PlayerImpl> removed = new HashSet<>();

            forEachPlayer:
            for (Iterator<PlayerImpl> it = players.iterator(); it.hasNext() && !stop; ) {
                PlayerImpl currentPlayer = it.next();

                if(removed.contains(currentPlayer)) continue;

                if(trySubmitRequest(currentPlayer, new MoveRequest())) {
                    /* submitted successfully, wait for client to make move */
                    currentlyRequestingMoveFrom.set(currentPlayer.getId());
                    MoveEvent moveEvent = null;
                    while (moveEvent == null) {
                        GameEvent event = waitForEvent();
                        if (event instanceof MoveEvent) {
                            moveEvent = (MoveEvent) event;
                        } else if (event instanceof LeaveEvent) {
                            /* handle leave event */
                            char leaverId = ((LeaveEvent) event).getPlayerId();
                            LOGGER.info("Player {} has left", leaverId);

                            if(leaverId == currentPlayer.getId()) {
                                /* cancel move request, remove him from under iterator, skip this loop iteration */
                                currentlyRequestingMoveFrom.set(-1);
                                it.remove();

                                if(players.size() > 1) {
                                    continue forEachPlayer;
                                } else {
                                    won(players.iterator().next());
                                    stop = true;
                                }
                            } else {
                                Optional<PlayerImpl> correspondingPlayerImpl = players.stream()
                                        .filter(p -> p.getId() == leaverId)
                                        .findFirst();

                                if(correspondingPlayerImpl.isPresent()) {
                                    removed.add(correspondingPlayerImpl.get());

                                    if(players.size() - removed.size() <= 1) {
                                        won(players.stream().filter(p -> !removed.contains(p)).findAny().get());
                                    }
                                } else {
                                    LOGGER.error("Cannot find PlayerImpl corresponding to leaverId. Cannot cleanup properly!");
                                }
                            }
                        }
                    }

                    /* handle move event */
                    board.occupy(moveEvent.getX(), moveEvent.getY(), currentPlayer.getId());
                    broadcastToAllBut(currentPlayer, new MovePerformed(currentPlayer.getId(), moveEvent.getX(), moveEvent.getY()));

                    if(isMoveWinning(moveEvent, currentPlayer.getId())) {
                        won(currentPlayer);
                        stop = true;
                    } else if(board.howMuchUnoccupied() < 1) {
                        overWithoutWinner();
                        stop = true;
                    }
                } else {
                    /* player's queue probably full, kick him out */
                    LOGGER.info("Kicking out player {}, probably due to full request queue", currentPlayer.getId());
                    kickOut(it, currentPlayer);
                }
            }

            if(!removed.isEmpty()) {
                players.removeIf(removed::contains);
            }
        }
    }

    public void submitMove(char callerId, int x, int y)
            throws AlreadyOccupiedException, NotYourTurnException, OutOfBoardException {
        if(!board.isWithinBounds(x,y)) throw new OutOfBoardException();
        if(board.isOccupied(x,y)) throw new AlreadyOccupiedException();

        if(currentlyRequestingMoveFrom.compareAndSet(callerId, -1)) {
            /* updated successfully! */
            enqueueEvent(new MoveEvent(x,y));
        } else {
            /* update failed, someone else was somehow first or request was unnecessary */
            throw new NotYourTurnException();
        };
    }

    public void submitLeave(char callerId) {
        enqueueEvent(new LeaveEvent(callerId));
    }

    private void won(PlayerImpl winner) {
        broadcastToAll(new Won(winner.getId()));
        LOGGER.info("{} won the game!", winner.getId());
    }

    private void overWithoutWinner() {
        broadcastToAll(new OverWithoutWinner());
        LOGGER.info("Game ended without winner...");
    }

    private void enqueueEvent(GameEvent event) {
        boolean succeeded = false;
        while(!succeeded) {
            try {
                eventQueue.put(event);
                succeeded = true;
            } catch (InterruptedException e) {
                LOGGER.warn("Unexpectedly interrupted when waiting to enqueue GameEvent to server");
            }
        }
    }

    private GameEvent waitForEvent() {
        GameEvent event = null;
        while(event == null) {
            try {
                event = eventQueue.take();
            } catch (InterruptedException e) {
                LOGGER.warn("Unexpected interruption while waiting for event. More:", e);
            }
        }
        return event;
    }

    private boolean trySubmitRequest(PlayerImpl player, Request request) {
        boolean succeeded = false;
        try {
            succeeded = player.getQueue().offer(request, 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("Unexpected interruption while waiting for event. More:", e);
        }

        return succeeded;
    }

    private void kickOut(Iterator<PlayerImpl> playerIt, PlayerImpl player) {
        playerIt.remove();
        unexport(player);
    }

    private void unexport(Remote obj) {
        try {
            UnicastRemoteObject.unexportObject(obj, true);
        } catch (NoSuchObjectException e) {
            LOGGER.warn("failed to unexport PlayerImpl while kicking out - this shouldn't happen!", e);
        }
    }

    /* basic dirs - TOP LEFT, TOP, TOP RIGHT, RIGHT  */
    private int[] basicDirsX = {-1, 0, 1, 1};
    private int[] basicDirsY = {1, 1, 1, 0};

    private boolean isMoveWinning(MoveEvent move, char playersSymbol) {
        assert basicDirsX.length == basicDirsY.length;
        for(int i = 0; i < basicDirsX.length; i++) {
            int bIncX = basicDirsX[i];
            int bIncY = basicDirsY[i];
            if(calculateCharsInDirection(move.getX() + bIncX, move.getY() + bIncY, bIncX, bIncY, playersSymbol)
                    + 1
                    + calculateCharsInDirection(move.getX() + (-1)*bIncX, move.getY() + (-1)*bIncY, (-1)*bIncX, (-1)*bIncY, playersSymbol)
                    >= winningChainLength) {
                return true;
            }
        }
        return false;
    }

    private int calculateCharsInDirection(int x, int y, int incX, int incY, char symbol) {
        if(!board.isWithinBounds(x,y) || !board.isOccupied(x,y) || !board.at(x,y).get().equals(symbol)) {
            return 0;
        }

        return 1 + calculateCharsInDirection(x + incX, y + incY, incX, incY, symbol);
    }

    private void broadcastToAll(Request request) {
        players.forEach(p -> trySubmitRequest(p, request));
    }

    private void broadcastToAllBut(PlayerImpl player, Request request) {
        for(PlayerImpl p: players) {
            if(!p.equals(player)) {
                trySubmitRequest(player, request);
            }
        }
    }

    public void setPlayers(List<PlayerImpl> players) {
        this.players = players;
    }
}
