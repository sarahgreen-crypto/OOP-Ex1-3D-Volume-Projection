package io;

public final class ReadOptions {

    private final boolean signeAllowed;

    private ReadOptions(boolean signeAllowed){
        this.signeAllowed = signeAllowed;
    }
    
    public static ReadOptions defaults(){
        return new ReadOptions(false);
    }

    public static ReadOptions allowSigned(){
        return new ReadOptions(true);
    }

    public boolean isSignedAllowed() {
        return signeAllowed;
    }
}
