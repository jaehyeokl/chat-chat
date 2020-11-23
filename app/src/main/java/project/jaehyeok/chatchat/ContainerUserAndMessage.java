package project.jaehyeok.chatchat;

public class ContainerUserAndMessage {

    public UserData userData;
    public Message message;

    public ContainerUserAndMessage(UserData userData, Message message) {
        this.userData = userData;
        this.message = message;
    }

    public UserData getUserData() {
        return userData;
    }

    public void setUserData(UserData userData) {
        this.userData = userData;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
