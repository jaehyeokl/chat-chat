package project.jaehyeok.chatchat;

public class Chat {
    public String masterUid;
    public long createAt;
    public String title;
    public int personnel;

    public Chat() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Chat(String masterUid, long createAt, String title, int personnel) {
        this.masterUid = masterUid;
        this.createAt = createAt;
        this.title = title;
        this.personnel = personnel;
    }

    public String getMasterUid() {
        return masterUid;
    }

    public void setMasterUid(String masterUid) {
        this.masterUid = masterUid;
    }

    public long getCreateAt() {
        return createAt;
    }

    public void setCreateAt(long createAt) {
        this.createAt = createAt;
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
}
