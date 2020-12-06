package project.jaehyeok.chatchat.data;

public class Chat {
    public String masterUid;
    public long timestamp = System.currentTimeMillis();
    public String title;
    public int personnel;
    public int thumb;
    public long deleteAt;
    public String latestMessage = "";
    public String latestSender = "";

    public Chat() {
        // Default constructor required for calls to DataSnapshot.getValue
    }

    public Chat(String masterUid, String title, int personnel) {
        this.masterUid = masterUid;
        this.title = title;
        this.personnel = personnel;
    }

    public String getMasterUid() {
        return masterUid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTitle() {
        return title;
    }

    public int getPersonnel() {
        return personnel;
    }

    public int getThumb() {
        return thumb;
    }

    public long getDeleteAt() {
        return deleteAt;
    }

    public String getLatestMessage() {
        return latestMessage;
    }

    public String getLatestSender() {
        return latestSender;
    }
}
