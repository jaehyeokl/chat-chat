package project.jaehyeok.chatchat;

public class UserData {
    public String provider;
    public String name;
    public String email;
    public long timestamp = System.currentTimeMillis();

    public UserData() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public UserData(String provider, String name, String email) {
        this.provider = provider;
        this.name = name;
        this.email = email;
    }

}
