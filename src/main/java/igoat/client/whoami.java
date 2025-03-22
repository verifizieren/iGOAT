package igoat.client;

public class whoami{

    public static String returnUserName() {
        String userName = System.getProperty("user.name");
        return userName;
    }
    public static void main(String[] args){
        System.out.println(returnUserName());
    }



}
