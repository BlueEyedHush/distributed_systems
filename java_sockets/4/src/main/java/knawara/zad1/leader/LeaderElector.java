package knawara.zad1.leader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by blueeyedhush on 22.03.16.
 */
public class LeaderElector {

    private static final byte ELECTION_STARTED = 0;
    private static final byte CANDIDATE_SUBMISSION = 1;
    private static final byte ELECTION_FINISHED = 2;

    private static final int ELECTION_TIME = 2000; // in ms

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderElector.class.getSimpleName());
    private Consumer<ByteBuffer> networkSink;
    private Consumer<String> consoleSink;

    private final String id = UUID.randomUUID().toString();
    private final byte[] binaryId = id.getBytes(StandardCharsets.US_ASCII);
    private boolean electionDirector = false;
    private boolean electionInProgress = false;
    private List<String> candidates = new ArrayList<>();
    private final Timer electionTimer = new Timer("election_timer", true);
    private final Random random = new Random();

    public void passArgs(Consumer<ByteBuffer> networkSink, Consumer<String> consoleSink) {
        this.networkSink = networkSink;
        this.consoleSink = consoleSink;
    }

    public void handleMessage(ByteBuffer bb) {
        byte type = bb.get();
        switch (type) {
            case ELECTION_STARTED:
                electionDirector = false;
                electionInProgress = true;
                sendCandidateSubmission();
                LOGGER.info("Someone else started election. Your id: {}", id);
                break;
            case CANDIDATE_SUBMISSION:
                if(electionDirector) {
                    String candidateId = decodeBinaryId(bb);
                    candidates.add(candidateId);
                    LOGGER.info("Received candidate submission, id: {}", candidateId);
                }
                break;
            case ELECTION_FINISHED:
                electionDirector = false;
                electionInProgress = false;
                String leaderId = decodeBinaryId(bb);
                if(leaderId.equals(id)) {
                    consoleSink.accept("You became the leader!");
                }
                break;
        }
    }

    public void startElection() {
        if(!electionInProgress) {
            consoleSink.accept("Starting election! Your id: " + id);
            candidates.clear();
            candidates.add(id);
            electionDirector = true;
            electionInProgress = true;
            sendElectionStarted();
            scheduleElectionEnd();
        } else {
            consoleSink.accept("Election already in progress");
        }
    }

    private void scheduleElectionEnd() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MILLISECOND, ELECTION_TIME);
        electionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                endElection();
            }
        }, cal.getTime());
    }

    private void endElection() {
        consoleSink.accept("Ending election");
        String leaderId = candidates.get(random.nextInt(candidates.size()));
        if(leaderId.equals(id)) {
            consoleSink.accept("You became the leader!");
        } else {
            consoleSink.accept("Chosen " + leaderId + " as leader!");
        }
        sendElectionFinished(leaderId);
        electionDirector = false;
        electionInProgress = false;
    }

    private void sendElectionStarted() {
        ByteBuffer bb = ByteBuffer.allocate(1);
        bb.put(ELECTION_STARTED);
        bb.flip();
        networkSink.accept(bb);
    }

    private void sendCandidateSubmission() {
        ByteBuffer bb = ByteBuffer.allocate(1 + binaryId.length);
        bb.put(CANDIDATE_SUBMISSION);
        bb.put(binaryId);
        bb.flip();
        networkSink.accept(bb);
    }

    private String decodeBinaryId(ByteBuffer bb) {
        byte[] binaryIdBytes = new byte[binaryId.length];
        bb.get(binaryIdBytes);
        return new String(binaryIdBytes, StandardCharsets.US_ASCII);
    }

    private void sendElectionFinished(String leader) {
        byte[] leaderBinaryId = leader.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer bb = ByteBuffer.allocate(1 + leaderBinaryId.length);
        bb.put(ELECTION_FINISHED);
        bb.put(leaderBinaryId);
        bb.flip();
        networkSink.accept(bb);
    }
}
