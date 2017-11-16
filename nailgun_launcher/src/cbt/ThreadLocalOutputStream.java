package cbt;
import java.io.*;

public class ThreadLocalOutputStream extends OutputStream{
  final public ThreadLocal<OutputStream> threadLocal; 
  final private OutputStream initialValue;

  public ThreadLocalOutputStream( OutputStream initialValue ){
    this.initialValue = initialValue;
    threadLocal = new ThreadLocal<OutputStream>() {
      @Override protected OutputStream initialValue() {
        return ThreadLocalOutputStream.this.initialValue;
      }
    };
  }
  
  public OutputStream get(){
    return threadLocal.get();
  }
  
  public void set( OutputStream outputStream ){
    threadLocal.set( outputStream );
  }
  
  public void write( int b ) throws IOException{
    // after implementing this I realized NailgunLauncher uses the same hack,
    // so probably this is not a problem performance
    get().write(b);
  }

  public void flush() throws IOException{
    get().flush();
  }
}
