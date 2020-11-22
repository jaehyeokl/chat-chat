package project.jaehyeok.chatchat;

public class Message {
    public boolean broadcast = false;
    public String name;
    public String message;
    public long timestamp = System.currentTimeMillis();

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue
    }

    // 유저 메세지 생성
    public Message(String name, String message) {
        this.name = name;
        this.message = message;
    }

    // 전체 안내메세지 생성
    public Message(boolean broadcast, String message) {
        this.broadcast = broadcast;
        this.message = message;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
