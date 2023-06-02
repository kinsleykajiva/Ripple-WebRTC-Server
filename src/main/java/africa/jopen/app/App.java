package africa.jopen.app;


import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class App {
    public static void main(String... args) {
        System.out.println("Running main method");
        Quarkus.run(Main.class,args);
        //Quarkus.run(args);
    }


}
