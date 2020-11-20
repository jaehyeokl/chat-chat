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
}
