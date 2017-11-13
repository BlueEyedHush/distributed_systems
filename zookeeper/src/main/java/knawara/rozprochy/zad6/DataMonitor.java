package knawara.rozprochy.zad6;

import java.io.IOException;
import java.util.List;

import org.apache.zookeeper.*;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataMonitor implements Watcher {

    private static final Logger logger = LoggerFactory.getLogger("DataMonitor");

    ZooKeeper zk;
    String znode;
    boolean dead;
    NodeStatusListener listener;

    private final StatCallback existsCb = (int rc, String path, Object ctx, Stat stat) -> {
        if(rc == Code.Ok && stat != null) {
            nodeCreated();
        } else if(rc == Code.NoNode) {
            nodeDeleted();
        } else {
            dead = true;
            listener.closing(rc);
        }
    };

    private final ChildrenCallback childrenCb = (int rc, String path, Object ctx, List<String> children) -> {
        logger.debug("[ChildrenCallback] {}", KeeperException.Code.get(rc).toString());
    };

    public DataMonitor(String hostPort, String znode, NodeStatusListener listener)
            throws IOException {
        this.zk = new ZooKeeper(hostPort, 20000, this);
        this.znode = znode;
        this.listener = listener;
        this.dead = false;
    }

    public void start() {
        // Get things started by checking if the node exists. We are going
        // to be completely event driven
        checkIfExistsAndCreateWatcher();
        printTreeAndCreateWatchers();
    }

    public void process(WatchedEvent event) {
        logger.debug("[EVENT]: {} | {} | {}", event.getPath(), event.getState().toString(), event.getType().toString());

        String path = event.getPath();
        if (event.getType() == Event.EventType.None) {
            // We are are being told that the state of the
            // connection has changed
            switch (event.getState()) {
                case SyncConnected:
                    // In this particular example we don't need to do anything
                    // here - watches are automatically re-registered with
                    // server and any watches triggered while the client was
                    // disconnected will be delivered (in order of course)
                    break;
                case Expired:
                    // It's all over
                    dead = true;
                    listener.closing(KeeperException.Code.SessionExpired);
                    break;
            }
        } else if (path != null) {
            /* for the znode only */
            if (path.equals(znode)) {
                /* event regarding top-level node
                * I have to do two things:
                * 1. create new watch
                * 2. handle the event
                * I could do 2 right here (using event.getType()), but I'll defer it to exists callback
                * (first check can be handled only there, so I do it to keep all of the logic in one place)
                * There is yet another advantage of doing that - if I relied only on watches, I could miss some changes
                * which occured between previous watch triggering on server and setting a new watch */

                if(event.getType() == Event.EventType.NodeCreated) {
                    checkIfExistsAndCreateWatcher();
                    printTreeAndCreateWatchers();
                } else if(event.getType() == Event.EventType.NodeDeleted) {
                    checkIfExistsAndCreateWatcher();
                }
            }

            /* for the whole subtree */
            if(path.startsWith(znode)) {
                if(event.getType() == Event.EventType.NodeChildrenChanged) {
                    printTreeAndCreateWatchers();
                }
            }
        }en
    }

    private void checkIfExistsAndCreateWatcher() {
        zk.exists(znode, true, existsCb, null);
    }

    private void printTreeAndCreateWatchers() {
        System.out.print("\nSubtree: \n");
        int size = printTreeAndCreateWatchersWalker(znode, " ");
        System.out.print("\nFound: " + size + " nodes");
    }

    private int printTreeAndCreateWatchersWalker(String searchRoot, String prefix) {
        try {
            int size = 0;
            for(String name: zk.getChildren(searchRoot, true)) {
                System.out.println(prefix + name);
                size += printTreeAndCreateWatchersWalker(searchRoot + '/' + name, prefix + ' ') + 1;
            }

            return size;
        } catch(KeeperException.NoNodeException e) {
            return 0;
        } catch (KeeperException | InterruptedException e) {
            logger.error("exception thrown while prining tree", e);
            return -10;
        }
    }

    private void nodeCreated() {
        listener.exists(new byte[0]);
    }

    private void nodeDeleted() {
        listener.exists(null);
    }
}
