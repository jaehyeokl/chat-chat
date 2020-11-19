package project.jaehyeok.chatchat;

public class UserData {
    public String name;
    public String email;

    public UserData() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public UserData(String name, String email) {
        this.name = name;
        this.email = email;
    }

}
