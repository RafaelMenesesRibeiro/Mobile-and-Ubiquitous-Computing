package server.msgtypes;

public class YesNo {

    private final boolean response;

    public YesNo(boolean b) {
        this.response = b;
    }

    public boolean getResponse() {
        return response;
    }
}