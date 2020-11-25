package project.jaehyeok.chatchat;

public class Chat {
    public String masterUid;
    public long timestamp = System.currentTimeMillis();
    public String title;
    public int personnel;
    public int thumb;

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

    public void setMasterUid(String masterUid) {
        this.masterUid = masterUid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPersonnel() {
        return personnel;
    }

    public void setPersonnel(int personnel) {
        this.personnel = personnel;
    }

    public int getThumb() {
        return thumb;
    }

    public void setThumb(int thumb) {
        this.thumb = thumb;
    }
}
