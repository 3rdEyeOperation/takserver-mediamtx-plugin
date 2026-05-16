package tak.server.plugins;

/** Compile-only stub. */
public interface MessageSender {
    // The real SDK exposes overloads accepting a protobuf Message and an
    // optional feed UUID. This plugin extends MessageSenderBase only so the
    // Plugin Manager recognises it as a sender; it never actually invokes
    // these methods, so we leave the interface intentionally empty.
}
