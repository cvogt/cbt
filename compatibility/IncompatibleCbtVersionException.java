package cbt;

public class IncompatibleCbtVersionException extends RuntimeException{
	public IncompatibleCbtVersionException( String msg, Throwable parent ){
		super( msg, parent );
	}
	public IncompatibleCbtVersionException( String msg ){
		super( msg );
	}
}
