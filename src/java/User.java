package java;

// NO import needed for UserRole — same folder, same package

public abstract class User {

    private int userID;
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private UserRole role;      // works without import — same package
    private boolean isActive;

    public User(int userID, String email, String password,
                String fullName, String phone, UserRole role) {
        this.userID   = userID;
        this.email    = email;
        this.password = password;
        this.fullName = fullName;
        this.phone    = phone;
        this.role     = role;
        this.isActive = true;
    }

    public abstract void accessDashboard();

    public String getProfileSummary() {
        return "ID: " + userID + " | Name: " + fullName +
               " | Role: " + role + " | Email: " + email;
    }

    public int getUserID()                { return userID; }
    public void setUserID(int id)         { this.userID = id; }
    public String getEmail()              { return email; }
    public void setEmail(String e)        { this.email = e; }
    public String getPassword()           { return password; }
    public void setPassword(String p)     { this.password = p; }
    public String getFullName()           { return fullName; }
    public void setFullName(String n)     { this.fullName = n; }
    public String getPhone()              { return phone; }
    public void setPhone(String p)        { this.phone = p; }
    public UserRole getRole()             { return role; }
    public void setRole(UserRole r)       { this.role = r; }
    public boolean isActive()             { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
}