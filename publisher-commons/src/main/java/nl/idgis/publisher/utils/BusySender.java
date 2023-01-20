package nl.idgis.publisher.utils;

import java.util.concurrent.TimeUnit;

import akka.actor.UntypedActor;
import akka.actor.Props;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.FiniteDuration;

public class BusySender extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final FiniteDuration interval;

    private final ActorRef target, sender;

    public BusySender(FiniteDuration interval, ActorRef target, ActorRef sender) {
        this.interval = interval;
        this.target = target;
        this.sender = sender;
    }

    public static Props props(FiniteDuration interval, ActorRef target, ActorRef sender) {
        return Props.create(BusySender.class, interval, target, sender);
    }

    private void scheduleBusy() {
        getContext().system().scheduler().scheduleOnce(
                interval,
                getSelf(),
                new Busy(),
                getContext().dispatcher(),
                getSelf());
    }

    @Override
    public void preStart() {
        scheduleBusy();
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        log.debug("message received, {}", msg.getClass().getCanonicalName());
        target.tell(msg, sender);

        if (msg instanceof Busy) {
            log.debug("busy sent");
            scheduleBusy();
        } else {
            log.debug("stopped");
            getContext().stop(getSelf());
        }
    }
}
