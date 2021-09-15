package tech.parasol.akka.workshop.model;

public class User {

    private String userId = null;
    private String userName = null;

    public User(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
