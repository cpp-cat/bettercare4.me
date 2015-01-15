import java.util.*;

public class JmxTest { 

   public static void main(String[] args) throws InterruptedException { 
	   int sz=0;
	   List l = new ArrayList();
	   while(true) {
		   sz++;
		   l.add("bla bla");
          System.out.println("Hello, JMX World! ("+sz+")");
		  Thread.sleep(50);

		  if(sz % 10000 == 0) {
              System.out.println("OK, cleaning up!");
			  l.clear();
			  l = new ArrayList();
			  sz = 0;
		      Thread.sleep(100);
		  }
	   }
   }
}