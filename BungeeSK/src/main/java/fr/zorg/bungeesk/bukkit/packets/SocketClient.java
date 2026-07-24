package fr.zorg.bungeesk.bukkit.packets;

import fr.zorg.bungeesk.bukkit.BungeeSK;
import fr.zorg.bungeesk.bukkit.skript.events.bukkit.ClientDisconnectEvent;
import fr.zorg.bungeesk.common.packets.AuthCompletePacket;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.utils.EncryptionUtils;
import fr.zorg.bungeesk.common.utils.PacketUtils;
import fr.zorg.bungeesk.common.utils.SafeSerialization;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SocketClient {

    private final Socket socket;
    private ObjectOutputStream writer;
    private ObjectInputStream reader;
    private Thread readThread;
    private boolean encrypting;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Opens the object streams over {@code socket}. This performs blocking handshake I/O (the
     * {@link ObjectInputStream} constructor reads the peer's stream header), so it may throw if the
     * proxy drops the connection mid-handshake — the caller is expected to catch that and reconnect.
     * The read loop is NOT started here; call {@link #startReading()} once this client has been
     * published to {@link PacketClient} so a fast failure can't notify a not-yet-assigned client.
     */
    public SocketClient(Socket socket) throws IOException {
        this.socket = socket;
        this.encrypting = false;
        // The output stream must be created (and its header flushed) before the input stream,
        // otherwise both peers block in the ObjectInputStream constructor. The input stream is
        // wrapped with a deserialization whitelist (see SafeSerialization) because the first
        // reads happen before authentication.
        this.writer = new ObjectOutputStream(socket.getOutputStream());
        this.writer.flush();
        this.reader = SafeSerialization.createFilteredStream(socket.getInputStream());
        this.readThread = new Thread(this::read, "BungeeSK-SocketReader");
        this.readThread.setDaemon(true);
    }

    /**
     * Starts the background read loop. Must be called exactly once, after this client is assigned to
     * {@link PacketClient#getClient()} — otherwise a read failure could call
     * {@link PacketClient#notifyDisconnected(SocketClient)} before the assignment and be ignored,
     * stalling auto-reconnect.
     */
    public void startReading() {
        this.readThread.start();
    }

    public void read() {
        while (this.isConnected()) {
            try {
                final Object dataRaw = this.reader.readObject();
                final AtomicReference<?> dataAtomic = new AtomicReference<>(dataRaw);
                BungeeSK.runAsync(() -> {
                    Object data = dataAtomic.get();
                    if (this.encrypting) {
                        data = EncryptionUtils.decryptPacket((byte[]) data, PacketClient.getBuilder().getPassword());
                        if (data == null)
                            return;

                        data = PacketUtils.packetFromBytes((byte[]) data);
                        if (data == null)
                            return;
                    }
                    if (!(data instanceof BungeeSKPacket)) {
                        this.disconnect();
                        return;
                    }
                    final BungeeSKPacket packet = (BungeeSKPacket) data;
                    this.handleReceiveListeners(packet);
                });
            } catch (IOException | ClassNotFoundException ex) {
                this.disconnect();
                return;
            }
        }
    }

    public void sendPacket(BungeeSKPacket packet) {
        BungeeSK.runAsync(() -> {
            if (!this.isConnected())
                return;
            this.handleSendListeners(packet);

            Object toSend = packet;
            if (this.encrypting) {
                toSend = EncryptionUtils.encryptPacket(PacketUtils.packetToBytes(packet), PacketClient.getBuilder().getPassword());
                if (toSend == null)
                    return;
            }
            try {
                // ObjectOutputStream is not thread-safe and several async tasks may send at once.
                synchronized (this.writer) {
                    this.writer.writeObject(toSend);
                    this.writer.flush();
                }
            } catch (IOException ex) {
                this.disconnect();
                return;
            }

            if (packet instanceof AuthCompletePacket) {
                setEncrypting(((AuthCompletePacket) packet).isEncrypting());
            }
        });
    }

    public void disconnect() {
        if (!this.closed.compareAndSet(false, true))
            return;
        try {
            if (this.reader != null)
                this.reader.close();
        } catch (IOException ignored) {
        }
        try {
            if (this.writer != null)
                this.writer.close();
        } catch (IOException ignored) {
        }
        if (this.readThread != null)
            this.readThread.interrupt();
        try {
            this.socket.close();
        } catch (IOException ignored) {
        }
        PacketClient.notifyDisconnected(this);
        BungeeSK.callEvent(new ClientDisconnectEvent());
    }

    public boolean isConnected() {
        return !this.closed.get() && this.socket != null && this.socket.isConnected() && !this.socket.isClosed();
    }

    private void handleSendListeners(BungeeSKPacket packet) {
        BungeeSK.runAsync(() -> {
            BungeeSK.getApi().getListeners().forEach(listener -> {
                try {
                    listener.getClass().getMethod("onSend", BungeeSKPacket.class);
                    listener.onSend(packet);
                } catch (Exception ignored) {
                }
            });
        });
    }

    private void handleReceiveListeners(BungeeSKPacket packet) {
        BungeeSK.runAsync(() -> {
            BungeeSK.getApi().getListeners().forEach(listener -> {
                try {
                    listener.getClass().getMethod("onReceive", BungeeSKPacket.class);
                    listener.onReceive(packet);
                } catch (Exception ignored) {
                }
            });
        });
    }

    public void setEncrypting(boolean encrypting) {
        this.encrypting = encrypting;
    }

    public boolean isEncrypting() {
        return this.encrypting;
    }

}
